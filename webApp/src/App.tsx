import { Fragment, type CSSProperties, type PointerEvent as ReactPointerEvent, type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  addPostCoffeeTag,
  createDiaryEntry,
  createPost,
  createComment,
  deleteCoffeeReview,
  deleteComment,
  deleteDiaryEntry,
  deletePantryItem,
  deletePost,
  fetchInitialData,
  fetchUserData,
  toggleFavoriteCoffee,
  upsertCoffeeReview,
  upsertCoffeeSensoryProfile,
  upsertCustomCoffee,
  upsertPantryStock,
  toggleFollow,
  toggleLike,
  uploadImageFile,
  updateComment,
  updateDiaryEntry,
  updatePost,
  updateUserProfile
} from "./data/supabaseApi";
import { BREW_METHODS, COMMENT_EMOJIS } from "./config/brew";
import { NAV_ITEMS } from "./config/navigation";
import { buildRoute, parseRoute, toCoffeeSlug } from "./core/routing";
import { getBrewStepTitle, getBrewTimelineForMethod } from "./core/brew";
import { buildNormalizedOptions, normalizeLookupText, toEventTimestamp, toUiOptionValue } from "./core/text";
import { toRelativeMinutes, withinDays } from "./core/time";
import { useGlobalUiEvents } from "./hooks/useGlobalUiEvents";
import { useResponsiveMode } from "./hooks/useResponsiveMode";
import { getSupabaseClient, supabaseConfigError } from "./supabase";
import { TopBar } from "./features/topbar/TopBar";
import { TimelineView } from "./features/timeline/TimelineView";
import { NotificationRow, type TimelineNotificationItem } from "./features/timeline/NotificationRow";
import { SearchView } from "./features/search/SearchView";
import { MobileBarcodeScannerSheet } from "./features/search/MobileBarcodeScannerSheet";
import { LoginGate } from "./features/auth/LoginGate";
import { CoffeeDetailView } from "./features/coffee/CoffeeDetailView";
import { BrewLabView, CreateCoffeeView } from "./features/brew/BrewViews";
import { DiaryView } from "./features/diary/DiaryView";
import { ProfileView } from "./features/profile/ProfileView";

import { MentionText } from "./ui/MentionText";
import { MaterialSymbolIcon, UiIcon, type IconName } from "./ui/iconography";
import type {
  BrewStep,
  CoffeeRow,
  CoffeeReviewRow,
  CoffeeSensoryProfileRow,
  CommentRow,
  DiaryEntryRow,
  FavoriteRow,
  FollowRow,
  LikeRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  TabId,
  TimelineCard,
  UserRow,
  ViewMode
} from "./types";

function vibrateTap(duration = 10): void {
  if (typeof navigator === "undefined" || typeof navigator.vibrate !== "function") return;
  navigator.vibrate(duration);
}

export function App() {
  const initialRoute = parseRoute(window.location.pathname);
  const { mode, viewportWidth } = useResponsiveMode();
  const [activeTab, setActiveTab] = useState<TabId>(initialRoute.tab);

  const [globalStatus, setGlobalStatus] = useState("Cargando datos...");

  const [users, setUsers] = useState<UserRow[]>([]);
  const [coffees, setCoffees] = useState<CoffeeRow[]>([]);
  const [customCoffees, setCustomCoffees] = useState<CoffeeRow[]>([]);
  const [coffeeReviews, setCoffeeReviews] = useState<CoffeeReviewRow[]>([]);
  const [coffeeSensoryProfiles, setCoffeeSensoryProfiles] = useState<CoffeeSensoryProfileRow[]>([]);
  const [posts, setPosts] = useState<PostRow[]>([]);
  const [likes, setLikes] = useState<LikeRow[]>([]);
  const [comments, setComments] = useState<CommentRow[]>([]);
  const [postCoffeeTags, setPostCoffeeTags] = useState<PostCoffeeTagRow[]>([]);

  const [diaryEntries, setDiaryEntries] = useState<DiaryEntryRow[]>([]);
  const [pantryItems, setPantryItems] = useState<PantryItemRow[]>([]);
  const [favorites, setFavorites] = useState<FavoriteRow[]>([]);
  const [follows, setFollows] = useState<FollowRow[]>([]);

  const [searchQuery, setSearchQuery] = useState("");
  const [searchMode, setSearchMode] = useState<"coffees" | "users">(initialRoute.searchMode);
  const [searchFilter, setSearchFilter] = useState<"todo" | "origen" | "nombre">("todo");
  const [searchSelectedOrigins, setSearchSelectedOrigins] = useState<Set<string>>(new Set());
  const [searchSelectedRoasts, setSearchSelectedRoasts] = useState<Set<string>>(new Set());
  const [searchSelectedSpecialties, setSearchSelectedSpecialties] = useState<Set<string>>(new Set());
  const [searchSelectedFormats, setSearchSelectedFormats] = useState<Set<string>>(new Set());
  const [searchMinRating, setSearchMinRating] = useState(0);
  const [searchActiveFilterType, setSearchActiveFilterType] = useState<"origen" | "tueste" | "especialidad" | "formato" | "nota" | null>(null);
  const [searchSelectedCoffeeId, setSearchSelectedCoffeeId] = useState<string | null>(null);
  const [searchFocusCoffeeProfile, setSearchFocusCoffeeProfile] = useState(false);
  const [detailCoffeeId, setDetailCoffeeId] = useState<string | null>(null);
  const [detailHostTab, setDetailHostTab] = useState<"timeline" | "search" | "profile" | "diary" | null>(null);
  const [detailReviewText, setDetailReviewText] = useState("");
  const [detailReviewRating, setDetailReviewRating] = useState(0);
  const [detailReviewImageFile, setDetailReviewImageFile] = useState<File | null>(null);
  const [detailReviewImagePreviewUrl, setDetailReviewImagePreviewUrl] = useState("");
  const [detailSensoryDraft, setDetailSensoryDraft] = useState({ aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 });
  const [detailStockDraft, setDetailStockDraft] = useState({ total: 0, remaining: 0 });
  const [detailOpenStockSignal, setDetailOpenStockSignal] = useState(0);
  const [showBarcodeScannerSheet, setShowBarcodeScannerSheet] = useState(false);

  const [brewStep, setBrewStep] = useState<BrewStep>("method");
  const [brewMethod, setBrewMethod] = useState("");
  const [brewCoffeeId, setBrewCoffeeId] = useState<string>("");
  const [waterMl, setWaterMl] = useState(300);
  const [ratio, setRatio] = useState(16);
  const [timerSeconds, setTimerSeconds] = useState(150);
  const [brewRunning, setBrewRunning] = useState(false);
  const [showCreateCoffeeComposer, setShowCreateCoffeeComposer] = useState(false);
  const [createCoffeeSaving, setCreateCoffeeSaving] = useState(false);
  const [createCoffeeError, setCreateCoffeeError] = useState<string | null>(null);
  const [createCoffeeDraft, setCreateCoffeeDraft] = useState({
    name: "",
    brand: "",
    specialty: "",
    country: "",
    format: "",
    roast: "",
    variety: "",
    hasCaffeine: true,
    totalGrams: 250
  });
  const [createCoffeeImageFile, setCreateCoffeeImageFile] = useState<File | null>(null);
  const [createCoffeeImagePreviewUrl, setCreateCoffeeImagePreviewUrl] = useState("");

  const [diaryTab, setDiaryTab] = useState<"actividad" | "despensa">("actividad");
  const [diaryPeriod, setDiaryPeriod] = useState<"hoy" | "7d" | "30d">("hoy");
  const [showDiaryQuickActions, setShowDiaryQuickActions] = useState(false);
  const [showDiaryPeriodSheet, setShowDiaryPeriodSheet] = useState(false);
  const [showDiaryWaterSheet, setShowDiaryWaterSheet] = useState(false);
  const [diaryWaterMlDraft, setDiaryWaterMlDraft] = useState("250");
  const [showDiaryCoffeeSheet, setShowDiaryCoffeeSheet] = useState(false);
  const [diaryCoffeeIdDraft, setDiaryCoffeeIdDraft] = useState("");
  const [diaryCoffeeMlDraft, setDiaryCoffeeMlDraft] = useState("250");
  const [diaryCoffeeCaffeineDraft, setDiaryCoffeeCaffeineDraft] = useState("95");
  const [diaryCoffeePreparationDraft, setDiaryCoffeePreparationDraft] = useState("Manual");
  const [showDiaryAddPantrySheet, setShowDiaryAddPantrySheet] = useState(false);
  const [diaryPantryCoffeeIdDraft, setDiaryPantryCoffeeIdDraft] = useState("");
  const [diaryPantryGramsDraft, setDiaryPantryGramsDraft] = useState("250");
  const [profileTab, setProfileTab] = useState<"posts" | "adn" | "favoritos">("posts");
  const [profileUsername, setProfileUsername] = useState<string | null>(initialRoute.profileUsername);
  const [profileEditSignal, setProfileEditSignal] = useState(0);
  useEffect(() => {
    if (activeTab === "profile") return;
    if (profileEditSignal === 0) return;
    setProfileEditSignal(0);
  }, [activeTab, profileEditSignal]);

  const [commentSheetPostId, setCommentSheetPostId] = useState<string | null>(null);
  const [commentDraft, setCommentDraft] = useState("");
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [commentMenuId, setCommentMenuId] = useState<number | null>(null);
  const [highlightedCommentId, setHighlightedCommentId] = useState<number | null>(null);
  const [handledTimelineDeepLink, setHandledTimelineDeepLink] = useState(false);
  const [topbarScrolled, setTopbarScrolled] = useState(false);
  const [timelineActionBanner, setTimelineActionBanner] = useState<string | null>(null);
  const [timelineRefreshing, setTimelineRefreshing] = useState(false);
  const [timelineBusyMessage, setTimelineBusyMessage] = useState<string | null>(null);
  const [showNotificationsPanel, setShowNotificationsPanel] = useState(false);
  const [notificationsLastSeenAt, setNotificationsLastSeenAt] = useState(0);
  const [dismissedNotificationIds, setDismissedNotificationIds] = useState<Set<string>>(new Set());
  const [dismissingNotificationIds, setDismissingNotificationIds] = useState<Set<string>>(new Set());
  const dismissNotificationTimersRef = useRef<number[]>([]);
  const [showCommentEmojiPanel, setShowCommentEmojiPanel] = useState(false);
  const [commentImageFile, setCommentImageFile] = useState<File | null>(null);
  const [commentImageName, setCommentImageName] = useState("");
  const [commentImagePreviewError, setCommentImagePreviewError] = useState(false);
  const [commentImagePreviewUrl, setCommentImagePreviewUrl] = useState("");
  const [showCreatePost, setShowCreatePost] = useState(false);
  const [newPostStep, setNewPostStep] = useState<0 | 1>(0);
  const [newPostText, setNewPostText] = useState("");
  const [newPostImageFile, setNewPostImageFile] = useState<File | null>(null);
  const [newPostImagePreviewUrl, setNewPostImagePreviewUrl] = useState("");
  const [newPostGalleryItems, setNewPostGalleryItems] = useState<Array<{ id: string; file: File; previewUrl: string }>>([]);
  const [newPostSelectedImageId, setNewPostSelectedImageId] = useState<string | null>(null);
  const [newPostCoffeeId, setNewPostCoffeeId] = useState<string>("");
  const [showCreatePostCoffeeSheet, setShowCreatePostCoffeeSheet] = useState(false);
  const [createPostCoffeeQuery, setCreatePostCoffeeQuery] = useState("");
  const [showCreatePostEmojiPanel, setShowCreatePostEmojiPanel] = useState(false);
  const newPostImageInputRef = useRef<HTMLInputElement | null>(null);
  const newPostCameraInputRef = useRef<HTMLInputElement | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [sessionEmail, setSessionEmail] = useState<string | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [showAuthPrompt, setShowAuthPrompt] = useState(false);
  const isMobileOsDevice = useMemo(() => /Android|iPhone|iPad|iPod/i.test(window.navigator.userAgent), []);

  const activeUser = useMemo(() => {
    if (!users.length) return null;
    if (sessionEmail) {
      const sessionEmailNormalized = normalizeLookupText(sessionEmail);
      const found = users.find((user) => normalizeLookupText(user.email) === sessionEmailNormalized);
      if (found) return found;
    }
    return users[0] ?? null;
  }, [sessionEmail, users]);

  const navigateToTab = useCallback(
    (
      tab: TabId,
      options?: {
        searchMode?: "coffees" | "users";
        profileUserId?: number | null;
        profileUsername?: string | null;
        coffeeSlug?: string | null;
        replace?: boolean;
      }
    ) => {
      const nextSearchMode = options?.searchMode ?? searchMode;
      const userById =
        options?.profileUserId != null
          ? users.find((item) => item.id === options.profileUserId) ?? null
          : null;
      const nextProfileUsername =
        options?.profileUsername ??
        userById?.username ??
        (tab === "profile" ? profileUsername : null);

      setActiveTab(tab);
      if (tab === "search") setSearchMode(nextSearchMode);
      if (tab === "profile") setProfileUsername(nextProfileUsername ?? null);

      const nextPath = buildRoute(tab, nextSearchMode, nextProfileUsername ?? null, options?.coffeeSlug ?? null);
      if (window.location.pathname === nextPath) return;
      const method = options?.replace ? "replaceState" : "pushState";
      window.history[method]({}, "", `${nextPath}${window.location.search}${window.location.hash}`);
    },
    [profileUsername, searchMode, users]
  );

  useEffect(() => {
    const onPopState = () => {
      const route = parseRoute(window.location.pathname);
      setActiveTab(route.tab);
      setSearchMode(route.searchMode);
      setProfileUsername(route.profileUsername);
      if (route.tab === "coffee") {
        setDetailHostTab(null);
        if (!route.coffeeSlug) {
          setDetailCoffeeId(null);
        } else {
          const counts = new Map<string, number>();
          const sorted = [...coffees].sort((a, b) => {
            const nameCmp = a.nombre.localeCompare(b.nombre);
            if (nameCmp !== 0) return nameCmp;
            return a.id.localeCompare(b.id);
          });
          let found: string | null = null;
          for (const item of sorted) {
            const base = toCoffeeSlug(item.nombre);
            const count = (counts.get(base) ?? 0) + 1;
            counts.set(base, count);
            const slug = count > 1 ? `${base}-${count}` : base;
            if (slug === route.coffeeSlug) {
              found = item.id;
              break;
            }
          }
          setDetailCoffeeId(found);
        }
      } else {
        setDetailCoffeeId(null);
        setDetailHostTab(null);
      }
    };
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, [coffees]);

  useEffect(() => {
    const route = parseRoute(window.location.pathname);
    const normalized = buildRoute(route.tab, route.searchMode, route.profileUsername, route.coffeeSlug);
    if (window.location.pathname !== normalized) {
      window.history.replaceState({}, "", `${normalized}${window.location.search}${window.location.hash}`);
    }
  }, []);

  useGlobalUiEvents({
    onOpenSearch: () => {
      navigateToTab("search", { searchMode: "coffees" });
      document.getElementById("quick-search")?.focus();
    },
    onTopbarScroll: setTopbarScrolled
  });

  useEffect(() => {
    if (!timelineActionBanner) return;
    const id = window.setTimeout(() => setTimelineActionBanner(null), 1800);
    return () => window.clearTimeout(id);
  }, [timelineActionBanner]);

  useEffect(() => {
    setEditingCommentId(null);
    setCommentMenuId(null);
    setCommentDraft("");
    setShowCommentEmojiPanel(false);
  }, [commentSheetPostId]);

  useEffect(() => {
    if (!showCreatePost) return;
    setNewPostCoffeeId((prev) => prev || "");
  }, [showCreatePost]);

  useEffect(() => {
    if (!showCreatePost || newPostStep !== 1) return;
    const id = window.requestAnimationFrame(() => {
      document.getElementById("new-post-text")?.focus();
    });
    return () => window.cancelAnimationFrame(id);
  }, [newPostStep, showCreatePost]);

  useEffect(() => {
    if (activeTab !== "search" || searchMode !== "coffees" || searchFocusCoffeeProfile) {
      setSearchActiveFilterType(null);
    }
  }, [activeTab, searchFocusCoffeeProfile, searchMode]);

  useEffect(() => {
    if (showCreatePost) return;
    newPostGalleryItems.forEach((item) => {
      if (item.previewUrl.startsWith("blob:")) URL.revokeObjectURL(item.previewUrl);
    });
  }, [newPostGalleryItems, newPostImagePreviewUrl, showCreatePost]);

  useEffect(() => {
    if (supabaseConfigError) {
      setAuthError(supabaseConfigError);
      setAuthReady(true);
      return;
    }

    const supabase = getSupabaseClient();
    let mounted = true;
    void supabase.auth.getSession().then(({ data, error }) => {
      if (!mounted) return;
      if (error) setAuthError(error.message);
      setSessionEmail(data.session?.user?.email ?? null);
      setAuthReady(true);
    });

    const { data } = supabase.auth.onAuthStateChange((_event, session) => {
      setSessionEmail(session?.user?.email ?? null);
      setAuthReady(true);
    });

    return () => {
      mounted = false;
      data.subscription.unsubscribe();
    };
  }, []);

  const handleGoogleLogin = useCallback(async () => {
    if (supabaseConfigError) return;
    setAuthBusy(true);
    setAuthError(null);
    try {
      const supabase = getSupabaseClient();
      const { error } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: {
          redirectTo: `${window.location.origin}${window.location.pathname}`
        }
      });
      if (error) throw error;
    } catch (error) {
      setAuthError((error as Error).message);
      setAuthBusy(false);
      return;
    }
  }, []);

  const requestLogin = useCallback(() => {
    setAuthError(null);
    setShowAuthPrompt(true);
  }, []);

  useEffect(() => {
    if (sessionEmail) setShowAuthPrompt(false);
  }, [sessionEmail]);

  const handleSignOut = useCallback(async () => {
    if (supabaseConfigError) return;
    try {
      const supabase = getSupabaseClient();
      await supabase.auth.signOut();
      setUsers([]);
      setCoffees([]);
      setCustomCoffees([]);
      setCoffeeReviews([]);
      setCoffeeSensoryProfiles([]);
      setPosts([]);
      setLikes([]);
      setComments([]);
      setPostCoffeeTags([]);
      setFollows([]);
      setDiaryEntries([]);
      setPantryItems([]);
      setFavorites([]);
      setShowCreateCoffeeComposer(false);
      setCreateCoffeeSaving(false);
      setCreateCoffeeError(null);
      setCreateCoffeeDraft({
        name: "",
        brand: "",
        specialty: "",
        country: "",
        format: "",
        roast: "",
        variety: "",
        hasCaffeine: true,
        totalGrams: 250
      });
      setCreateCoffeeImageFile(null);
      setCreateCoffeeImagePreviewUrl("");
      setDetailCoffeeId(null);
      setDetailHostTab(null);
      setGlobalStatus("Listo");
    } catch (error) {
      setAuthError((error as Error).message);
    }
  }, []);

  const resetCreatePostComposer = useCallback(() => {
    newPostGalleryItems.forEach((item) => {
      if (item.previewUrl.startsWith("blob:")) URL.revokeObjectURL(item.previewUrl);
    });
    setShowCreatePost(false);
    setNewPostStep(0);
    setNewPostText("");
    setNewPostImageFile(null);
    setNewPostImagePreviewUrl("");
    setNewPostGalleryItems([]);
    setNewPostSelectedImageId(null);
    setNewPostCoffeeId("");
    setCreatePostCoffeeQuery("");
    setShowCreatePostCoffeeSheet(false);
    setShowCreatePostEmojiPanel(false);
  }, [newPostGalleryItems, newPostImagePreviewUrl]);

  const openCreatePostComposer = useCallback(() => {
    setShowCreatePost(true);
    setNewPostStep(0);
  }, []);

  const appendNewPostFiles = useCallback(
    (files: File[]) => {
      if (!files.length) return;
      const added = files.map((file, index) => ({
        id: `${Date.now()}-${index}-${Math.random().toString(36).slice(2, 8)}`,
        file,
        previewUrl: URL.createObjectURL(file)
      }));
      setNewPostGalleryItems((prev) => [...added, ...prev].slice(0, 24));
      const first = added[0];
      if (first) {
        setNewPostSelectedImageId(first.id);
        setNewPostImageFile(first.file);
        setNewPostImagePreviewUrl(first.previewUrl);
      }
    },
    []
  );

  useEffect(() => {
    return () => {
      if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
    };
  }, [createCoffeeImagePreviewUrl]);

  useEffect(() => {
    const onEscSheet = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      if (showNotificationsPanel) {
        setShowNotificationsPanel(false);
      }
      if (showCreatePost) {
        resetCreatePostComposer();
      }
      if (showCreatePostCoffeeSheet) {
        setShowCreatePostCoffeeSheet(false);
      }
      if (commentSheetPostId) {
        setCommentSheetPostId(null);
        setCommentDraft("");
        setHighlightedCommentId(null);
      }
      if (showCreateCoffeeComposer) {
        setShowCreateCoffeeComposer(false);
      }
    };
    window.addEventListener("keydown", onEscSheet);
    return () => window.removeEventListener("keydown", onEscSheet);
  }, [commentSheetPostId, resetCreatePostComposer, showCreateCoffeeComposer, showCreatePost, showCreatePostCoffeeSheet, showNotificationsPanel]);

  useEffect(() => {
    const hasModal = Boolean(commentSheetPostId || showCreatePost || showNotificationsPanel || showCreatePostCoffeeSheet);
    const prev = document.body.style.overflow;
    if (hasModal) document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [commentSheetPostId, showCreatePost, showCreatePostCoffeeSheet, showNotificationsPanel]);

  useEffect(() => {
    const onEsc = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setCommentMenuId(null);
      if (editingCommentId) {
        setEditingCommentId(null);
        setCommentDraft("");
      }
    };
    window.addEventListener("keydown", onEsc);
    return () => window.removeEventListener("keydown", onEsc);
  }, [editingCommentId]);

  useEffect(() => {
    if (activeTab !== "search" || searchMode !== "users") return;
    const id = window.setTimeout(() => {
      document.getElementById("quick-search")?.focus();
    }, 40);
    return () => window.clearTimeout(id);
  }, [activeTab, searchMode]);

  useEffect(() => {
    if (!commentSheetPostId) return;
    const list = commentListRef.current;
    if (!list) return;
    list.scrollTop = list.scrollHeight;
  }, [commentSheetPostId, comments, editingCommentId]);

  useEffect(() => {
    return () => {
      if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
    };
  }, [commentImagePreviewUrl]);

  useEffect(() => {
    if (!commentSheetPostId || !highlightedCommentId) return;
    const list = commentListRef.current;
    if (!list) return;
    const target = list.querySelector<HTMLElement>(`[data-comment-id="${highlightedCommentId}"]`);
    if (!target) return;
    target.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [commentSheetPostId, comments, highlightedCommentId]);

  useEffect(() => {
    if (handledTimelineDeepLink) return;
    const params = new URLSearchParams(window.location.search);
    const postId = params.get("postId");
    const commentIdRaw = params.get("commentId");
    const commentId = commentIdRaw ? Number(commentIdRaw) : null;
    if (!postId) return;
    if (!posts.some((post) => post.id === postId)) return;
    navigateToTab("timeline", { replace: true });
    setCommentSheetPostId(postId);
    if (commentId != null && Number.isFinite(commentId)) setHighlightedCommentId(commentId);
    setHandledTimelineDeepLink(true);
    const url = new URL(window.location.href);
    url.searchParams.delete("postId");
    url.searchParams.delete("commentId");
    window.history.replaceState({}, "", url.toString());
  }, [handledTimelineDeepLink, navigateToTab, posts]);

  useEffect(
    () => () => {
      dismissNotificationTimersRef.current.forEach((id) => window.clearTimeout(id));
      dismissNotificationTimersRef.current = [];
    },
    []
  );

  const loadInitialData = useCallback(async () => {
    try {
      const data = await fetchInitialData();
      setUsers(data.users);
      setCoffees(data.coffees);
      setCoffeeReviews(data.reviews);
      setCoffeeSensoryProfiles(data.sensoryProfiles);
      setPosts(data.posts);
      setLikes(data.likes);
      setComments(data.comments);
      setPostCoffeeTags(data.postCoffeeTags);
      setFollows(data.follows);
      setBrewCoffeeId((prev) => prev || data.coffees[0]?.id || "");
      setGlobalStatus("Listo");
    } catch (error) {
      setGlobalStatus(`Error: ${(error as Error).message}`);
    }
  }, []);

  useEffect(() => {
    if (!authReady) return;
    void loadInitialData();
  }, [authReady, loadInitialData]);

  useEffect(() => {
    if (activeTab === "diary") return;
    setShowDiaryQuickActions(false);
    setShowDiaryPeriodSheet(false);
    setShowDiaryWaterSheet(false);
    setShowDiaryCoffeeSheet(false);
    setShowDiaryAddPantrySheet(false);
  }, [activeTab]);

  useEffect(() => {
    if (!activeUser) return;
    (async () => {
      try {
        const data = await fetchUserData(activeUser.id);
        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
        setCustomCoffees(data.customCoffees);
      } catch (error) {
        setGlobalStatus(`Error: ${(error as Error).message}`);
      }
    })();
  }, [activeUser]);

  const brewCoffeeCatalog = useMemo(() => {
    if (!customCoffees.length) return coffees;
    const byId = new Map<string, CoffeeRow>();
    coffees.forEach((coffee) => byId.set(String(coffee.id), coffee));
    customCoffees.forEach((coffee) => byId.set(String(coffee.id), coffee));
    return Array.from(byId.values());
  }, [coffees, customCoffees]);

  useEffect(() => {
    if (!brewRunning || brewStep !== "brewing") return;
    const totalDuration = getBrewTimelineForMethod(brewMethod, waterMl).reduce((acc, phase) => acc + phase.durationSeconds, 0);
    if (totalDuration > 0 && timerSeconds >= totalDuration) {
      setBrewRunning(false);
      setBrewStep("result");
      return;
    }
    const id = window.setTimeout(() => setTimerSeconds((prev) => prev + 1), 1000);
    return () => window.clearTimeout(id);
  }, [brewMethod, brewRunning, brewStep, timerSeconds, waterMl]);

  const usersById = useMemo(() => {
    const map = new Map<number, UserRow>();
    users.forEach((user) => map.set(user.id, user));
    return map;
  }, [users]);

  const tagsByPostId = useMemo(() => {
    const map = new Map<string, PostCoffeeTagRow>();
    postCoffeeTags.forEach((tag) => {
      if (!map.has(tag.post_id)) map.set(tag.post_id, tag);
    });
    return map;
  }, [postCoffeeTags]);

  const likesByPostId = useMemo(() => {
    const map = new Map<string, number>();
    likes.forEach((like) => {
      map.set(like.post_id, (map.get(like.post_id) ?? 0) + 1);
    });
    return map;
  }, [likes]);

  const commentsByPostId = useMemo(() => {
    const map = new Map<string, CommentRow[]>();
    comments.forEach((comment) => {
      const bucket = map.get(comment.post_id);
      if (bucket) {
        bucket.push(comment);
      } else {
        map.set(comment.post_id, [comment]);
      }
    });
    return map;
  }, [comments]);

  const coffeeIdByNameBrand = useMemo(() => {
    const map = new Map<string, string>();
    const nameOnlyMap = new Map<string, string>();
    coffees.forEach((coffee) => {
      const normalizedBrand = normalizeLookupText(coffee.marca);
      const normalizedName = normalizeLookupText(coffee.nombre);
      const key = `${normalizedBrand}|${normalizedName}`;
      map.set(key, coffee.id);
      if (normalizedName && !nameOnlyMap.has(normalizedName)) {
        nameOnlyMap.set(normalizedName, coffee.id);
      }
    });
    return { byNameBrand: map, byName: nameOnlyMap };
  }, [coffees]);
  const coffeesById = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    coffees.forEach((coffee) => map.set(coffee.id, coffee));
    return map;
  }, [coffees]);
  const coffeeSlugIndex = useMemo(() => {
    const bySlug = new Map<string, string>();
    const byId = new Map<string, string>();
    const counts = new Map<string, number>();
    const sorted = [...coffees].sort((a, b) => {
      const nameCmp = a.nombre.localeCompare(b.nombre);
      if (nameCmp !== 0) return nameCmp;
      return a.id.localeCompare(b.id);
    });
    sorted.forEach((coffee) => {
      const base = toCoffeeSlug(coffee.nombre);
      const count = (counts.get(base) ?? 0) + 1;
      counts.set(base, count);
      const slug = count > 1 ? `${base}-${count}` : base;
      bySlug.set(slug, coffee.id);
      byId.set(coffee.id, slug);
    });
    return { bySlug, byId };
  }, [coffees]);

  useEffect(() => {
    const route = parseRoute(window.location.pathname);
    if (route.tab !== "coffee") return;
    if (!route.coffeeSlug) return;
    const coffeeId = coffeeSlugIndex.bySlug.get(route.coffeeSlug) ?? null;
    if (coffeeId) {
      setDetailCoffeeId(coffeeId);
      setDetailHostTab(null);
    }
  }, [coffeeSlugIndex]);


  const timelineCards: TimelineCard[] = useMemo(
    () =>
      posts.map((post) => {
        const user = usersById.get(post.user_id);
        const postLikes = likesByPostId.get(post.id) ?? 0;
        const postComments = commentsByPostId.get(post.id)?.length ?? 0;
        const likedByActiveUser =
          activeUser != null &&
          likes.some((like) => like.post_id === post.id && like.user_id === activeUser.id);
        const tag = tagsByPostId.get(post.id);
        const normalizedTagBrand = normalizeLookupText(tag?.coffee_brand);
        const normalizedTagName = normalizeLookupText(tag?.coffee_name);
        const coffeeKey = tag ? `${normalizedTagBrand}|${normalizedTagName}` : "";
        const coffeeId =
          (tag?.coffee_id ? tag.coffee_id : null) ??
          (coffeeKey ? coffeeIdByNameBrand.byNameBrand.get(coffeeKey) : null) ??
          (normalizedTagName ? coffeeIdByNameBrand.byName.get(normalizedTagName) : null) ??
          null;
        const coffee = coffeeId ? coffeesById.get(coffeeId) ?? null : null;

        return {
          id: post.id,
          userId: post.user_id,
          userName: user?.full_name ?? `Usuario ${post.user_id}`,
          username: user?.username ?? `user${post.user_id}`,
          avatarUrl: user?.avatar_url ?? "",
          text: post.comment,
          imageUrl: post.image_url,
          minsAgoLabel: toRelativeMinutes(post.timestamp),
          likes: postLikes,
          comments: postComments,
          coffeeId,
          coffeeTagName: tag?.coffee_name ?? null,
          coffeeTagBrand: tag?.coffee_brand ?? null,
          coffeeImageUrl: coffee?.image_url ?? null,
          likedByActiveUser
        };
      }),
    [activeUser, coffeeIdByNameBrand, coffeesById, commentsByPostId, likes, likesByPostId, posts, tagsByPostId, usersById]
  );

  const filteredCoffees = useMemo(() => {
    const q = normalizeLookupText(searchQuery);
    const ratingByCoffee = new Map<string, { total: number; count: number }>();
    coffeeReviews.forEach((review) => {
      const bucket = ratingByCoffee.get(review.coffee_id) ?? { total: 0, count: 0 };
      bucket.total += review.rating;
      bucket.count += 1;
      ratingByCoffee.set(review.coffee_id, bucket);
    });
    return coffees.filter((coffee) => {
      const byName = normalizeLookupText(coffee.nombre).includes(q);
      const byBrand = normalizeLookupText(coffee.marca).includes(q);
      const byOrigin = normalizeLookupText(coffee.pais_origen).includes(q);
      const queryMatch = !q || byName || byBrand || byOrigin;
      if (!queryMatch) return false;

      const origin = toUiOptionValue(coffee.pais_origen ?? "");
      const originMatch =
        !searchSelectedOrigins.size || (origin ? searchSelectedOrigins.has(origin) : false);
      if (!originMatch) return false;

      const roast = toUiOptionValue(coffee.tueste ?? "");
      const roastMatch = !searchSelectedRoasts.size || (roast ? searchSelectedRoasts.has(roast) : false);
      if (!roastMatch) return false;

      const specialty = toUiOptionValue(coffee.especialidad ?? "");
      const specialtyMatch = !searchSelectedSpecialties.size || (specialty ? searchSelectedSpecialties.has(specialty) : false);
      if (!specialtyMatch) return false;

      const format = toUiOptionValue(coffee.formato ?? "");
      const formatMatch = !searchSelectedFormats.size || (format ? searchSelectedFormats.has(format) : false);
      if (!formatMatch) return false;

      if (searchMinRating > 0) {
        const stats = ratingByCoffee.get(coffee.id);
        const avg = stats && stats.count > 0 ? stats.total / stats.count : 0;
        if (avg < searchMinRating) return false;
      }

      return true;
    });
  }, [
    coffeeReviews,
    coffees,
    searchMinRating,
    searchQuery,
    searchSelectedFormats,
    searchSelectedOrigins,
    searchSelectedRoasts,
    searchSelectedSpecialties
  ]);

  const searchOriginOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.pais_origen)),
    [coffees]
  );

  const searchRoastOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.tueste)),
    [coffees]
  );

  const searchSpecialtyOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.especialidad)),
    [coffees]
  );

  const searchFormatOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.formato)),
    [coffees]
  );
  const selectedCoffee = brewCoffeeCatalog.find((coffee) => coffee.id === brewCoffeeId) ?? brewCoffeeCatalog[0];
  const coffeeGrams = Math.max(1, Math.round(waterMl / ratio));
  const saveBrewToDiary = useCallback(
    async (taste: string) => {
      if (!activeUser || !selectedCoffee) return;
      const decaf = normalizeLookupText(selectedCoffee.cafeina ?? "").includes("sin");
      const caffeineMg = Math.max(0, Math.round(coffeeGrams * (decaf ? 2 : 9)));
      const preparationType = `Lab: ${brewMethod || "Metodo"} (${taste})`;
      const created = await createDiaryEntry({
        userId: activeUser.id,
        coffeeId: selectedCoffee.id,
        coffeeName: selectedCoffee.nombre,
        amountMl: waterMl,
        caffeineMg,
        preparationType,
        type: "CUP"
      });
      setDiaryEntries((prev) => [created, ...prev]);
      setBrewRunning(false);
      setTimerSeconds(0);
      setBrewStep("method");
      navigateToTab("diary");
    },
    [activeUser, brewMethod, coffeeGrams, navigateToTab, selectedCoffee, setBrewStep, waterMl]
  );
  const brewPantryItems = useMemo(() => {
    const coffeeById = new Map<string, CoffeeRow>();
    brewCoffeeCatalog.forEach((coffee) => {
      coffeeById.set(String(coffee.id), coffee);
    });

    const latestByCoffee = new Map<string, PantryItemRow>();
    pantryItems.forEach((item) => {
      const coffeeId = String(item.coffee_id);
      if (!coffeeId) return;
      const prev = latestByCoffee.get(coffeeId);
      if (!prev || Number(item.last_updated ?? 0) > Number(prev.last_updated ?? 0)) {
        latestByCoffee.set(coffeeId, item);
      }
    });

    return Array.from(latestByCoffee.values())
      .map((item) => {
        const coffeeId = String(item.coffee_id);
        const coffee = coffeeById.get(coffeeId);
        if (!coffee) return null;
        const total = Math.max(0, Number(item.total_grams ?? 0));
        const remaining = Math.max(0, Math.min(total, Number(item.grams_remaining ?? 0)));
        const progress = total > 0 ? remaining / total : 0;
        return { item, coffee, total, remaining, progress };
      })
      .filter((row): row is { item: PantryItemRow; coffee: CoffeeRow; total: number; remaining: number; progress: number } => Boolean(row))
      .sort((a, b) => Number(b.item.last_updated ?? 0) - Number(a.item.last_updated ?? 0));
  }, [brewCoffeeCatalog, pantryItems]);

  const diaryEntriesActivity = diaryEntries.slice(0, 80);
  const pantryCoffeeRows = pantryItems
    .map((item) => ({ item, coffee: brewCoffeeCatalog.find((coffee) => coffee.id === item.coffee_id) }))
    .filter((row) => row.coffee);
  const handleDeleteDiaryEntry = useCallback(
    async (entryId: number) => {
      if (!activeUser) return;
      await deleteDiaryEntry(entryId, activeUser.id);
      setDiaryEntries((prev) => prev.filter((entry) => entry.id !== entryId));
    },
    [activeUser]
  );
  const handleUpdateDiaryEntry = useCallback(
    async (entryId: number, amountMl: number, caffeineMg: number, preparationType: string) => {
      if (!activeUser) return;
      const updated = await updateDiaryEntry({
        entryId,
        userId: activeUser.id,
        amountMl,
        caffeineMg,
        preparationType
      });
      setDiaryEntries((prev) => prev.map((entry) => (entry.id === updated.id ? { ...entry, ...updated } : entry)));
    },
    [activeUser]
  );
  const handleUpdatePantryStock = useCallback(
    async (coffeeId: string, totalGrams: number, gramsRemaining: number) => {
      if (!activeUser) return;
      const total = Math.max(1, Math.round(totalGrams));
      const remaining = Math.max(0, Math.min(total, Math.round(gramsRemaining)));
      const updated = await upsertPantryStock({
        coffeeId,
        userId: activeUser.id,
        totalGrams: total,
        gramsRemaining: remaining
      });
      setPantryItems((prev) =>
        [updated, ...prev.filter((row) => !(row.user_id === updated.user_id && row.coffee_id === updated.coffee_id))]
      );
    },
    [activeUser]
  );
  const handleRemovePantryItem = useCallback(
    async (coffeeId: string) => {
      if (!activeUser) return;
      await deletePantryItem(coffeeId, activeUser.id);
      setPantryItems((prev) => prev.filter((row) => !(row.user_id === activeUser.id && row.coffee_id === coffeeId)));
    },
    [activeUser]
  );
  const diaryCoffeeOptions = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    pantryCoffeeRows.forEach((row) => {
      if (row.coffee?.id) map.set(row.coffee.id, row.coffee);
    });
    brewCoffeeCatalog.forEach((coffee) => {
      if (!map.has(coffee.id)) map.set(coffee.id, coffee);
    });
    return Array.from(map.values()).slice(0, 500);
  }, [brewCoffeeCatalog, pantryCoffeeRows]);
  const selectedDiaryCoffee = useMemo(
    () => diaryCoffeeOptions.find((coffee) => coffee.id === diaryCoffeeIdDraft) ?? diaryCoffeeOptions[0] ?? null,
    [diaryCoffeeIdDraft, diaryCoffeeOptions]
  );
  const selectedDiaryPantryCoffee = useMemo(
    () => diaryCoffeeOptions.find((coffee) => coffee.id === diaryPantryCoffeeIdDraft) ?? diaryCoffeeOptions[0] ?? null,
    [diaryCoffeeOptions, diaryPantryCoffeeIdDraft]
  );
  const bumpPantryGrams = (delta: number) => {
    const next = Math.max(1, Math.round((Number(diaryPantryGramsDraft || 0) || 0) + delta));
    setDiaryPantryGramsDraft(String(next));
  };

  const profileUser = useMemo(() => {
    if (profileUsername) {
      const routeUsername = normalizeLookupText(profileUsername);
      const fromRoute = users.find((user) => normalizeLookupText(user.username) === routeUsername);
      if (fromRoute) return fromRoute;
    }
    return usersById.get(activeUser?.id ?? -1) ?? null;
  }, [activeUser?.id, profileUsername, users, usersById]);
  const profilePosts = timelineCards.filter((card) => card.userId === (profileUser?.id ?? -1));

  const favoriteCoffees = favorites.map((favorite) => coffees.find((coffee) => coffee.id === favorite.coffee_id)).filter((coffee): coffee is CoffeeRow => Boolean(coffee));
  const detailCoffee = detailCoffeeId ? coffeesById.get(detailCoffeeId) ?? null : null;
  const detailCoffeeReviews = useMemo(() => {
    if (!detailCoffeeId) return [] as Array<CoffeeReviewRow & { user: UserRow | null }>;
    return coffeeReviews
      .filter((item) => item.coffee_id === detailCoffeeId && typeof item.user_id === "number")
      .map((item) => ({
        ...item,
        user: typeof item.user_id === "number" ? usersById.get(item.user_id) ?? null : null
      }))
      .sort((a, b) => (b.timestamp ?? 0) - (a.timestamp ?? 0));
  }, [coffeeReviews, detailCoffeeId, usersById]);
  const detailCoffeeAverageRating = useMemo(() => {
    if (!detailCoffeeReviews.length) return 0;
    const sum = detailCoffeeReviews.reduce((acc, item) => acc + item.rating, 0);
    return Number((sum / detailCoffeeReviews.length).toFixed(1));
  }, [detailCoffeeReviews]);
  const detailCurrentUserReview = useMemo(() => {
    if (!activeUser || !detailCoffeeId) return null;
    return coffeeReviews.find((item) => item.coffee_id === detailCoffeeId && item.user_id === activeUser.id) ?? null;
  }, [activeUser, coffeeReviews, detailCoffeeId]);
  const detailCurrentUser = useMemo(() => {
    if (!activeUser) return null;
    return usersById.get(activeUser.id) ?? null;
  }, [activeUser, usersById]);
  const detailCurrentUserReviewWithUser = useMemo(() => {
    if (!detailCurrentUserReview) return null;
    return {
      ...detailCurrentUserReview,
      user: detailCurrentUser
    };
  }, [detailCurrentUser, detailCurrentUserReview]);
  const detailIsFavorite = useMemo(() => {
    if (!activeUser || !detailCoffeeId) return false;
    return favorites.some((item) => item.user_id === activeUser.id && item.coffee_id === detailCoffeeId);
  }, [activeUser, detailCoffeeId, favorites]);
  const detailPantryStock = useMemo(() => {
    if (!activeUser || !detailCoffeeId) return null;
    return pantryItems.find((item) => item.user_id === activeUser.id && item.coffee_id === detailCoffeeId) ?? null;
  }, [activeUser, detailCoffeeId, pantryItems]);
  const detailSensoryAverages = useMemo(() => {
    if (!detailCoffeeId || !detailCoffee) return { aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 };
    const rows = coffeeSensoryProfiles.filter((item) => item.coffee_id === detailCoffeeId);
    const base = {
      aroma: Number(detailCoffee.aroma ?? 0),
      sabor: Number(detailCoffee.sabor ?? 0),
      cuerpo: Number(detailCoffee.cuerpo ?? 0),
      acidez: Number(detailCoffee.acidez ?? 0),
      dulzura: Number(detailCoffee.dulzura ?? 0)
    };
    if (!rows.length) return base;
    const avg = (key: keyof CoffeeSensoryProfileRow) => {
      const total = rows.reduce((acc, row) => acc + Number(row[key]), 0);
      return Number((total / rows.length).toFixed(1));
    };
    return {
      aroma: avg("aroma"),
      sabor: avg("sabor"),
      cuerpo: avg("cuerpo"),
      acidez: avg("acidez"),
      dulzura: avg("dulzura")
    };
  }, [coffeeSensoryProfiles, detailCoffee, detailCoffeeId]);

  useEffect(() => {
    if (!detailCoffee) return;
    if (detailCurrentUserReview) {
      setDetailReviewText(detailCurrentUserReview.comment ?? "");
      setDetailReviewRating(detailCurrentUserReview.rating);
      setDetailReviewImagePreviewUrl(detailCurrentUserReview.image_url ?? "");
    } else {
      setDetailReviewText("");
      setDetailReviewRating(0);
      setDetailReviewImagePreviewUrl("");
    }
    setDetailReviewImageFile(null);
    setDetailStockDraft({
      total: detailPantryStock?.total_grams ?? 0,
      remaining: detailPantryStock?.grams_remaining ?? 0
    });
    setDetailSensoryDraft({
      aroma: detailSensoryAverages.aroma,
      sabor: detailSensoryAverages.sabor,
      cuerpo: detailSensoryAverages.cuerpo,
      acidez: detailSensoryAverages.acidez,
      dulzura: detailSensoryAverages.dulzura
    });
  }, [detailCoffee, detailCurrentUserReview, detailPantryStock, detailSensoryAverages]);

  useEffect(() => {
    if (!detailReviewImagePreviewUrl.startsWith("blob:")) return;
    const blobUrl = detailReviewImagePreviewUrl;
    return () => {
      URL.revokeObjectURL(blobUrl);
    };
  }, [detailReviewImagePreviewUrl]);

  const followersCount = profileUser ? follows.filter((follow) => follow.followed_id === profileUser.id).length : 0;
  const followingCount = profileUser ? follows.filter((follow) => follow.follower_id === profileUser.id).length : 0;
  const followingIds = useMemo(() => {
    if (!activeUser) return new Set<number>();
    return new Set(follows.filter((follow) => follow.follower_id === activeUser.id).map((follow) => follow.followed_id));
  }, [activeUser, follows]);
  const followerCounts = useMemo(() => {
    const map = new Map<number, number>();
    follows.forEach((follow) => {
      map.set(follow.followed_id, (map.get(follow.followed_id) ?? 0) + 1);
    });
    return map;
  }, [follows]);
  const filteredSearchUsers = useMemo(() => {
    const q = normalizeLookupText(searchQuery);
    const base = users.filter((user) => user.id !== activeUser?.id);
    if (!q) return base.filter((user) => !followingIds.has(user.id)).slice(0, 80);
    return base
      .filter((user) => normalizeLookupText(user.username).includes(q) || normalizeLookupText(user.full_name).includes(q))
      .slice(0, 120);
  }, [activeUser?.id, followingIds, searchQuery, users]);
  const selectedCreatePostCoffee = useMemo(
    () => coffees.find((coffee) => coffee.id === newPostCoffeeId) ?? null,
    [coffees, newPostCoffeeId]
  );
  const filteredCreatePostCoffees = useMemo(() => {
    const query = normalizeLookupText(createPostCoffeeQuery);
    if (!query) return coffees.slice(0, 30);
    return coffees
      .filter((coffee) => normalizeLookupText(coffee.nombre).includes(query) || normalizeLookupText(coffee.marca).includes(query))
      .slice(0, 40);
  }, [coffees, createPostCoffeeQuery]);
  const createPostMentionSuggestions = useMemo(() => {
    const draftParts = newPostText.split(/\s+/);
    const token = draftParts[draftParts.length - 1] ?? "";
    if (!token.startsWith("@")) return [] as UserRow[];
    const query = normalizeLookupText(token.slice(1));
    if (!query) return users.slice(0, 6);
    return users.filter((user) => normalizeLookupText(user.username).includes(query)).slice(0, 6);
  }, [newPostText, users]);

  const timelineRecommendations = useMemo(() => {
    if (!coffees.length) return [] as CoffeeRow[];
    if (!activeUser) return coffees.slice(0, 5);

    const scoreByCoffeeId = new Map<string, number>();
    const addScore = (coffeeId: string | null | undefined, points: number) => {
      if (!coffeeId) return;
      scoreByCoffeeId.set(coffeeId, (scoreByCoffeeId.get(coffeeId) ?? 0) + points);
    };

    favorites
      .filter((favorite) => favorite.user_id === activeUser.id)
      .forEach((favorite) => addScore(favorite.coffee_id, 120));

    pantryItems
      .filter((item) => item.user_id === activeUser.id)
      .forEach((item) => addScore(item.coffee_id, 70 + Math.min(20, Math.round((item.grams_remaining ?? 0) / 40))));

    diaryEntries
      .filter((entry) => entry.user_id === activeUser.id)
      .forEach((entry) => addScore(entry.coffee_id, 16));

    const followedIds = new Set(
      follows.filter((follow) => follow.follower_id === activeUser.id).map((follow) => follow.followed_id)
    );
    timelineCards
      .filter((card) => followedIds.has(card.userId))
      .forEach((card) => addScore(card.coffeeId, 18));

    const coffeeByPostId = new Map<string, string | null>();
    timelineCards.forEach((card) => coffeeByPostId.set(card.id, card.coffeeId));

    likes
      .filter((like) => like.user_id === activeUser.id)
      .forEach((like) => addScore(coffeeByPostId.get(like.post_id) ?? null, 14));

    comments
      .filter((comment) => comment.user_id === activeUser.id)
      .forEach((comment) => addScore(coffeeByPostId.get(comment.post_id) ?? null, 8));

    const scored = coffees
      .map((coffee) => ({ coffee, score: scoreByCoffeeId.get(coffee.id) ?? 0 }))
      .sort((a, b) => {
        if (b.score !== a.score) return b.score - a.score;
        return a.coffee.nombre.localeCompare(b.coffee.nombre);
      })
      .map((entry) => entry.coffee);

    return scored.slice(0, 5);
  }, [activeUser, coffees, comments, diaryEntries, favorites, follows, likes, pantryItems, timelineCards]);
  const timelineSuggestions = useMemo(
    () =>
      users
        .filter((user) => user.id !== activeUser?.id && !followingIds.has(user.id))
        .slice(0, 5),
    [activeUser?.id, followingIds, users]
  );
  const timelineSuggestionIndices = useMemo(() => {
    if (timelineCards.length < 3) return [] as number[];
    const seed = Math.max(timelineCards.length, 1) + (activeUser?.id ?? 0);
    const first = Math.max(1, seed % timelineCards.length);
    let second = Math.max(1, (seed * 7) % timelineCards.length);
    if (second === first) second = Math.min(timelineCards.length - 1, second + 1);
    return [first, second];
  }, [activeUser?.id, timelineCards.length]);

  const commentSheetRows = useMemo(
    () =>
      [...(commentsByPostId.get(commentSheetPostId ?? "") ?? [])]
        .sort((a, b) => a.timestamp - b.timestamp)
        .slice(-120),
    [commentSheetPostId, commentsByPostId]
  );
  const activeCommentMenuRow = useMemo(
    () => commentSheetRows.find((row) => row.id === commentMenuId) ?? null,
    [commentMenuId, commentSheetRows]
  );
  const commentMentionSuggestions = useMemo(() => {
    const draftParts = commentDraft.split(/\s+/);
    const token = draftParts[draftParts.length - 1] ?? "";
    if (!token.startsWith("@")) return [];
    const query = normalizeLookupText(token.slice(1));
    if (!query) return users.slice(0, 8);
    return users.filter((user) => normalizeLookupText(user.username).includes(query)).slice(0, 8);
  }, [commentDraft, users]);
  const commentListRef = useRef<HTMLUListElement | null>(null);
  const commentImageInputRef = useRef<HTMLInputElement | null>(null);
  const timelineNotifications = useMemo<TimelineNotificationItem[]>(() => {
    if (!activeUser) return [];

    const followEvents = follows
      .filter((follow) => follow.followed_id === activeUser.id && follow.follower_id !== activeUser.id)
      .map((follow) => ({
        id: `follow-${follow.follower_id}-${follow.followed_id}`,
        type: "follow" as const,
        userId: follow.follower_id,
        text: "ha comenzado a seguirte",
        timestamp: toEventTimestamp(follow.created_at)
      }));

    const commentEvents = comments
      .filter((comment) => comment.user_id !== activeUser.id)
      .slice(0, 40)
      .map((comment) => {
        const post = posts.find((entry) => entry.id === comment.post_id);
        if (!post || post.user_id !== activeUser.id) return null;
        return {
          id: `comment-${comment.id}`,
          type: "comment" as const,
          userId: comment.user_id,
          text: "ha comentado en tu publicacion",
          timestamp: toEventTimestamp(comment.timestamp),
          postId: comment.post_id,
          commentId: comment.id
        };
      })
      .filter(
        (event): event is { id: string; type: "comment"; userId: number; text: string; timestamp: number; postId: string; commentId: number } =>
          Boolean(event)
      );

    return [...followEvents, ...commentEvents]
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, 24);
  }, [activeUser, comments, follows, posts]);
  const visibleTimelineNotifications = useMemo(
    () => timelineNotifications.filter((item) => !dismissedNotificationIds.has(item.id)),
    [dismissedNotificationIds, timelineNotifications]
  );
  const showNotificationsBadge = useMemo(
    () => visibleTimelineNotifications.some((item) => item.timestamp > notificationsLastSeenAt),
    [notificationsLastSeenAt, visibleTimelineNotifications]
  );

  useEffect(() => {
    if (!showNotificationsPanel) return;
    const latestSeen = visibleTimelineNotifications.reduce((max, item) => Math.max(max, item.timestamp), 0);
    if (latestSeen > notificationsLastSeenAt) setNotificationsLastSeenAt(latestSeen);
  }, [notificationsLastSeenAt, showNotificationsPanel, visibleTimelineNotifications]);

  const handleToggleLike = async (postId: string) => {
    if (!activeUser) return;

    const alreadyLiked = likes.some((like) => like.post_id === postId && like.user_id === activeUser.id);
    setTimelineBusyMessage(alreadyLiked ? "Quitando like..." : "Enviando like...");
    vibrateTap(8);
    try {
      const payload = await toggleLike(postId, activeUser.id, alreadyLiked);
      if (!payload) {
        setLikes((prev) => prev.filter((like) => !(like.post_id === postId && like.user_id === activeUser.id)));
        setTimelineBusyMessage(null);
        return;
      }
      setLikes((prev) => [...prev, payload]);
    } catch (error) {
      setGlobalStatus(`Error like: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
    setTimelineActionBanner(alreadyLiked ? "Like eliminado" : "Like enviado");
  };

  const handleAddComment = async () => {
    if (!activeUser || !commentSheetPostId) return;
    const text = commentDraft.trim();
    if (!text) return;

    setTimelineBusyMessage("Publicando comentario...");
    vibrateTap(8);
    try {
      const newComment = await createComment(commentSheetPostId, activeUser.id, text);
      setComments((prev) => [newComment, ...prev]);
      setCommentDraft("");
      if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
      setCommentImageFile(null);
      setCommentImageName("");
      setCommentImagePreviewError(false);
      setCommentImagePreviewUrl("");
      setTimelineActionBanner("Comentario publicado");
    } catch (error) {
      setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleUpdateComment = async () => {
    if (!editingCommentId) return;
    const text = commentDraft.trim();
    if (!text) return;
    setTimelineBusyMessage("Actualizando comentario...");
    vibrateTap(8);
    try {
      await updateComment(editingCommentId, text);
      setComments((prev) =>
        prev.map((entry) => (entry.id === editingCommentId ? { ...entry, text } : entry))
      );
      setEditingCommentId(null);
      setCommentDraft("");
      if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
      setCommentImageFile(null);
      setCommentImageName("");
      setCommentImagePreviewError(false);
      setCommentImagePreviewUrl("");
      setTimelineActionBanner("Comentario actualizado");
    } catch (error) {
      setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleDeleteComment = async (commentId: number) => {
    setTimelineBusyMessage("Eliminando comentario...");
    vibrateTap(8);
    try {
      await deleteComment(commentId);
      setComments((prev) => prev.filter((entry) => entry.id !== commentId));
      setCommentMenuId(null);
      if (editingCommentId === commentId) {
        setEditingCommentId(null);
        setCommentDraft("");
      }
      setTimelineActionBanner("Comentario eliminado");
    } catch (error) {
      setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleToggleFollow = async (targetUserId: number) => {
    if (!activeUser || targetUserId === activeUser.id) return;
    const alreadyFollowing = follows.some(
      (follow) => follow.follower_id === activeUser.id && follow.followed_id === targetUserId
    );
    setTimelineBusyMessage(alreadyFollowing ? "Actualizando seguimiento..." : "Siguiendo usuario...");
    vibrateTap(8);
    try {
      const payload = await toggleFollow(activeUser.id, targetUserId, alreadyFollowing);
      if (!payload) {
        setFollows((prev) =>
          prev.filter(
            (follow) => !(follow.follower_id === activeUser.id && follow.followed_id === targetUserId)
          )
        );
        setTimelineActionBanner("Dejaste de seguir");
        setTimelineBusyMessage(null);
        return;
      }
      setFollows((prev) => [...prev, payload]);
      setTimelineActionBanner("Ahora sigues a este usuario");
    } catch (error) {
      setGlobalStatus(`Error follow: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleEditPost = async (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => {
    setTimelineBusyMessage("Actualizando publicacion...");
    vibrateTap(8);
    try {
      const imageToPersist = imageFile ? await uploadImageFile("posts", imageFile) : newImageUrl;
      await updatePost(postId, newText, imageToPersist);
      setPosts((prev) =>
        prev.map((post) =>
          post.id === postId ? { ...post, comment: newText, image_url: imageToPersist } : post
        )
      );
      setTimelineActionBanner("Publicacion actualizada");
    } catch (error) {
      setGlobalStatus(`Error editar post: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleDeletePost = async (postId: string) => {
    setTimelineBusyMessage("Eliminando publicacion...");
    vibrateTap(10);
    try {
      await deletePost(postId);
      setPosts((prev) => prev.filter((post) => post.id !== postId));
      setLikes((prev) => prev.filter((like) => like.post_id !== postId));
      setComments((prev) => prev.filter((comment) => comment.post_id !== postId));
      setPostCoffeeTags((prev) => prev.filter((tag) => tag.post_id !== postId));
      setTimelineActionBanner("Publicacion eliminada");
    } catch (error) {
      setGlobalStatus(`Error borrar post: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };
  const handleUpdateProfile = async (
    userId: number,
    fullName: string,
    bio: string,
    avatarFile?: File | null,
    removeAvatar?: boolean
  ) => {
    const trimmedName = fullName.trim();
    const normalizedBio = bio.trim();
    if (!trimmedName) return;
    setTimelineBusyMessage("Actualizando perfil...");
    try {
      const avatarUrl = removeAvatar ? null : avatarFile ? await uploadImageFile("avatars", avatarFile) : undefined;
      await updateUserProfile(userId, {
        full_name: trimmedName,
        bio: normalizedBio ? normalizedBio : null,
        avatar_url: avatarUrl
      });
      setUsers((prev) =>
        prev.map((row) =>
          row.id === userId
            ? {
                ...row,
                full_name: trimmedName,
                bio: normalizedBio ? normalizedBio : null,
                avatar_url: avatarUrl === undefined ? row.avatar_url : avatarUrl ?? ""
              }
            : row
        )
      );
      setTimelineActionBanner("Perfil actualizado");
    } catch (error) {
      setGlobalStatus(`Error actualizando perfil: ${(error as Error).message}`);
    } finally {
      setTimelineBusyMessage(null);
    }
  };

  const handleRefreshTimeline = async () => {
    setTimelineRefreshing(true);
    setTimelineBusyMessage("Actualizando timeline...");
    await loadInitialData();
    setTimelineRefreshing(false);
    setTimelineBusyMessage(null);
    setTimelineActionBanner("Timeline actualizado");
  };

  const handleCreatePost = async () => {
    if (!activeUser) return;
    const text = newPostText.trim();
    if (!text && !newPostImageFile) return;

    setTimelineBusyMessage("Publicando tu contenido...");
    vibrateTap(10);
    try {
      const imageUrl = newPostImageFile ? await uploadImageFile("posts", newPostImageFile) : "";
      const post = await createPost(activeUser.id, text, imageUrl);
      setPosts((prev) => [post, ...prev]);

      if (newPostCoffeeId) {
        const selectedCoffee = coffees.find((coffee) => coffee.id === newPostCoffeeId);
        if (selectedCoffee) {
          const tag: PostCoffeeTagRow = {
            post_id: post.id,
            coffee_id: selectedCoffee.id,
            coffee_name: selectedCoffee.nombre,
            coffee_brand: selectedCoffee.marca ?? ""
          };
          await addPostCoffeeTag(tag);
          setPostCoffeeTags((prev) => [tag, ...prev]);
        }
      }

      resetCreatePostComposer();
      setTimelineActionBanner("Publicacion creada");
    } catch (error) {
      setGlobalStatus(`Error publicar: ${(error as Error).message}`);
      return;
    } finally {
      setTimelineBusyMessage(null);
    }
  };

  const handleMentionNavigation = (username: string) => {
    if (!username) return;
    navigateToTab("search", { searchMode: "users" });
    setSearchQuery(username);
    window.requestAnimationFrame(() => {
      document.getElementById("quick-search")?.focus();
    });
  };

  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState !== "visible") return;
      if (activeTab !== "timeline") return;
      void handleRefreshTimeline();
    };
    window.addEventListener("focus", onVisible);
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.removeEventListener("focus", onVisible);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [activeTab]);

  const closeNotificationsPanel = () => {
    setNotificationsLastSeenAt(Date.now());
    setShowNotificationsPanel(false);
  };
  const dismissNotification = (id: string) => {
    setDismissingNotificationIds((prev) => {
      if (prev.has(id)) return prev;
      const next = new Set(prev);
      next.add(id);
      return next;
    });
    const timer = window.setTimeout(() => {
      setDismissedNotificationIds((prev) => {
        const next = new Set(prev);
        next.add(id);
        return next;
      });
      setDismissingNotificationIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
      dismissNotificationTimersRef.current = dismissNotificationTimersRef.current.filter((value) => value !== timer);
    }, 220);
    dismissNotificationTimersRef.current.push(timer);
  };

  const openCoffeeDetail = useCallback(
    (coffeeId: string, sourceTab: "timeline" | "search" | "profile" | "diary") => {
      const coffee = coffeesById.get(coffeeId);
      if (!coffee) return;
      const slug = coffeeSlugIndex.byId.get(coffeeId) ?? toCoffeeSlug(coffee.nombre);
      setDetailCoffeeId(coffeeId);
      if (sourceTab === "diary") {
        setDetailHostTab(null);
        setActiveTab("coffee");
      } else if (mode === "desktop") {
        setDetailHostTab(sourceTab);
      } else {
        setDetailHostTab(null);
        setActiveTab("coffee");
      }
      const nextPath = buildRoute("coffee", searchMode, profileUsername, slug);
      if (window.location.pathname !== nextPath) {
        window.history.pushState({}, "", `${nextPath}${window.location.search}${window.location.hash}`);
      }
    },
    [coffeeSlugIndex.byId, coffeesById, mode, profileUsername, searchMode]
  );

  const closeCoffeePanel = useCallback(() => {
    setDetailCoffeeId(null);
    setDetailHostTab(null);
    if (mode === "desktop") {
      const backPath = buildRoute(activeTab, searchMode, profileUsername, null);
      if (window.location.pathname !== backPath) {
        window.history.replaceState({}, "", `${backPath}${window.location.search}${window.location.hash}`);
      }
    }
  }, [activeTab, mode, profileUsername, searchMode]);

  const saveDetailFavorite = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const next = !detailIsFavorite;
    const result = await toggleFavoriteCoffee(activeUser.id, detailCoffeeId, detailIsFavorite);
    setFavorites((prev) => {
      if (next && result) return [result, ...prev.filter((item) => !(item.user_id === result.user_id && item.coffee_id === result.coffee_id))];
      return prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === detailCoffeeId));
    });
  }, [activeUser, detailCoffeeId, detailIsFavorite]);

  const saveDetailReview = useCallback(async () => {
    if (!activeUser || !detailCoffeeId || detailReviewRating <= 0) return;
    let imageUrl = detailCurrentUserReview?.image_url ?? "";
    if (detailReviewImageFile) {
      imageUrl = await uploadImageFile("reviews", detailReviewImageFile);
    }
    const row = await upsertCoffeeReview({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      rating: detailReviewRating,
      comment: detailReviewText.trim(),
      imageUrl
    });
    setCoffeeReviews((prev) => [
      row,
      ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))
    ]);
  }, [activeUser, detailCoffeeId, detailCurrentUserReview?.image_url, detailReviewImageFile, detailReviewRating, detailReviewText]);

  const removeDetailReview = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    await deleteCoffeeReview(detailCoffeeId, activeUser.id);
    setCoffeeReviews((prev) => prev.filter((item) => !(item.coffee_id === detailCoffeeId && item.user_id === activeUser.id)));
  }, [activeUser, detailCoffeeId]);

  const saveDetailSensory = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const row = await upsertCoffeeSensoryProfile({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      aroma: detailSensoryDraft.aroma,
      sabor: detailSensoryDraft.sabor,
      cuerpo: detailSensoryDraft.cuerpo,
      acidez: detailSensoryDraft.acidez,
      dulzura: detailSensoryDraft.dulzura
    });
    setCoffeeSensoryProfiles((prev) => [
      row,
      ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))
    ]);
  }, [activeUser, detailCoffeeId, detailSensoryDraft]);

  const saveDetailStock = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const total = Math.max(0, Number.isFinite(detailStockDraft.total) ? detailStockDraft.total : 0);
    const remaining = Math.min(total, Math.max(0, Number.isFinite(detailStockDraft.remaining) ? detailStockDraft.remaining : 0));
    const row = await upsertPantryStock({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      totalGrams: total,
      gramsRemaining: remaining
    });
    setPantryItems((prev) => [
      row,
      ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))
    ]);
    setDetailStockDraft({
      total,
      remaining
    });
  }, [activeUser, detailCoffeeId, detailStockDraft]);

  const closeCreateCoffeeComposer = useCallback(() => {
    if (createCoffeeSaving) return;
    setShowCreateCoffeeComposer(false);
    setCreateCoffeeError(null);
  }, [createCoffeeSaving]);

  const saveCreateCoffee = useCallback(async () => {
    if (!activeUser) return;
    if (!createCoffeeDraft.name.trim() || !createCoffeeDraft.brand.trim()) {
      setCreateCoffeeError("Nombre y marca son obligatorios.");
      return;
    }
    if (!createCoffeeDraft.specialty.trim() || !createCoffeeDraft.country.trim() || !createCoffeeDraft.format.trim()) {
      setCreateCoffeeError("Completa especialidad, país y formato.");
      return;
    }
    setCreateCoffeeSaving(true);
    setCreateCoffeeError(null);
    try {
      let imageUrl = "";
      if (createCoffeeImageFile) {
        imageUrl = await uploadImageFile("posts", createCoffeeImageFile);
      }
      const created = await upsertCustomCoffee({
        userId: activeUser.id,
        name: createCoffeeDraft.name,
        brand: createCoffeeDraft.brand,
        specialty: createCoffeeDraft.specialty,
        country: createCoffeeDraft.country,
        format: createCoffeeDraft.format,
        roast: createCoffeeDraft.roast || null,
        variety: createCoffeeDraft.variety || null,
        hasCaffeine: createCoffeeDraft.hasCaffeine,
        imageUrl,
        totalGrams: createCoffeeDraft.totalGrams
      });

      const pantryRow = await upsertPantryStock({
        coffeeId: created.id,
        userId: activeUser.id,
        totalGrams: Math.max(1, createCoffeeDraft.totalGrams || 250),
        gramsRemaining: Math.max(1, createCoffeeDraft.totalGrams || 250)
      });

      setCustomCoffees((prev) => [created, ...prev.filter((item) => item.id !== created.id)]);
      setPantryItems((prev) => [pantryRow, ...prev.filter((item) => !(item.coffee_id === pantryRow.coffee_id && item.user_id === pantryRow.user_id))]);
      setBrewCoffeeId(created.id);
      setBrewStep("config");
      setShowCreateCoffeeComposer(false);
      setCreateCoffeeDraft({
        name: "",
        brand: "",
        specialty: "",
        country: "",
        format: "",
        roast: "",
        variety: "",
        hasCaffeine: true,
        totalGrams: 250
      });
      if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
      setCreateCoffeeImageFile(null);
      setCreateCoffeeImagePreviewUrl("");
    } catch (error) {
      setCreateCoffeeError((error as Error).message);
    } finally {
      setCreateCoffeeSaving(false);
    }
  }, [activeUser, createCoffeeDraft, createCoffeeImageFile, createCoffeeImagePreviewUrl]);

  const detailPanel = detailCoffee ? (
    <CoffeeDetailView
      coffee={detailCoffee}
      reviews={detailCoffeeReviews}
      currentUser={detailCurrentUser}
      currentUserReview={detailCurrentUserReviewWithUser}
      avgRating={detailCoffeeAverageRating}
      isFavorite={detailIsFavorite}
      pantry={detailPantryStock}
      sensory={detailSensoryAverages}
      sensoryDraft={detailSensoryDraft}
      stockDraft={detailStockDraft}
      reviewDraftText={detailReviewText}
      reviewDraftRating={detailReviewRating}
      reviewDraftImagePreviewUrl={detailReviewImagePreviewUrl}
      onClose={closeCoffeePanel}
      onToggleFavorite={() => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        void saveDetailFavorite();
      }}
      onReviewTextChange={setDetailReviewText}
      onReviewRatingChange={setDetailReviewRating}
      onReviewImagePick={(file, previewUrl) => {
        setDetailReviewImageFile(file);
        setDetailReviewImagePreviewUrl(previewUrl);
      }}
      onSaveReview={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await saveDetailReview();
      }}
      onDeleteReview={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await removeDetailReview();
      }}
      canDeleteReview={Boolean(detailCurrentUserReview)}
      onSensoryDraftChange={setDetailSensoryDraft}
      onSaveSensory={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await saveDetailSensory();
      }}
      onStockDraftChange={setDetailStockDraft}
      onSaveStock={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await saveDetailStock();
      }}
      onOpenUserProfile={(userId) => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        navigateToTab("profile", { profileUserId: userId });
      }}
      isGuest={!sessionEmail}
      onRequireAuth={requestLogin}
      fullPage={false}
      externalOpenStockSignal={0}
    />
  ) : null;

  const createCoffeePanel = (
    <CreateCoffeeView
      draft={createCoffeeDraft}
      imagePreviewUrl={createCoffeeImagePreviewUrl}
      saving={createCoffeeSaving}
      error={createCoffeeError}
      countryOptions={searchOriginOptions}
      specialtyOptions={searchSpecialtyOptions}
      onChange={setCreateCoffeeDraft}
      onPickImage={(file, previewUrl) => {
        if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
        setCreateCoffeeImageFile(file);
        setCreateCoffeeImagePreviewUrl(previewUrl);
      }}
      onRemoveImage={() => {
        if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
        setCreateCoffeeImageFile(null);
        setCreateCoffeeImagePreviewUrl("");
      }}
      onClose={closeCreateCoffeeComposer}
      onSave={() => void saveCreateCoffee()}
      fullPage={mode !== "desktop"}
    />
  );

  useEffect(() => {
    const isCoffeeRoute = window.location.pathname.startsWith("/coffee/");
    const siteUrl = (import.meta.env.VITE_SITE_URL as string | undefined) ?? window.location.origin;
    const canonicalHref = `${siteUrl}${window.location.pathname}`;

    let canonical = document.querySelector("link[rel='canonical']") as HTMLLinkElement | null;
    if (!canonical) {
      canonical = document.createElement("link");
      canonical.rel = "canonical";
      document.head.appendChild(canonical);
    }
    canonical.href = canonicalHref;

    let descriptionMeta = document.querySelector("meta[name='description']") as HTMLMetaElement | null;
    if (!descriptionMeta) {
      descriptionMeta = document.createElement("meta");
      descriptionMeta.name = "description";
      document.head.appendChild(descriptionMeta);
    }

    if (!isCoffeeRoute) {
      document.title = "Cafesito Web";
      descriptionMeta.content = "Comunidad de cafe para compartir timeline, explorar cafes y seguir perfiles.";
      return;
    }
    const title = detailCoffee ? `${detailCoffee.nombre} | Cafesito` : "Café | Cafesito";
    const description = detailCoffee
      ? (detailCoffee.descripcion?.trim() || `${detailCoffee.nombre} ${detailCoffee.marca ?? ""}`.trim() || "Detalle de café en Cafesito")
      : "Detalle de café en Cafesito";
    document.title = title;
    descriptionMeta.content = description.slice(0, 160);

    let ogTitle = document.querySelector("meta[property='og:title']") as HTMLMetaElement | null;
    if (!ogTitle) {
      ogTitle = document.createElement("meta");
      ogTitle.setAttribute("property", "og:title");
      document.head.appendChild(ogTitle);
    }
    ogTitle.content = title;

    let ogType = document.querySelector("meta[property='og:type']") as HTMLMetaElement | null;
    if (!ogType) {
      ogType = document.createElement("meta");
      ogType.setAttribute("property", "og:type");
      document.head.appendChild(ogType);
    }
    ogType.content = "product";
  }, [detailCoffee]);

  const handleNavClick = (tabId: TabId) => {
    if (!sessionEmail) {
      requestLogin();
      return;
    }
    setDetailCoffeeId(null);
    setDetailHostTab(null);
    setShowCreateCoffeeComposer(false);
    if (tabId === "search") {
      setSearchSelectedCoffeeId(null);
      setSearchFocusCoffeeProfile(false);
      navigateToTab("search", { searchMode: "coffees" });
      return;
    }
    if (tabId === "profile") {
      navigateToTab("profile", { profileUsername: activeUser?.username ?? null });
      return;
    }
    navigateToTab(tabId);
  };

  const nav = (
    <nav aria-label="Navegacion principal" className="nav nav-mobile">
      {NAV_ITEMS.map((item) => (
        (() => {
          const isActive = activeTab === item.id;
          return (
        <button
          key={item.id}
          type="button"
          className={`nav-item ${isActive ? "is-active" : ""}`}
          onClick={() => handleNavClick(item.id)}
          aria-current={isActive ? "page" : undefined}
        >
          <span className="nav-glyph" aria-hidden="true">
            <UiIcon name={isActive ? item.activeIcon : item.icon} className="ui-icon" />
          </span>
          <span className="nav-label">{item.label}</span>
        </button>
          );
        })()
      ))}
    </nav>
  );

  const navRail = (
    <aside className="nav-rail" aria-label="Navegacion principal">
      <nav className="nav nav-desktop">
        {NAV_ITEMS.map((item) => (
          (() => {
            const isActive = activeTab === item.id;
            return (
          <button
            key={item.id}
            type="button"
            className={`nav-item ${isActive ? "is-active" : ""}`}
            onClick={() => handleNavClick(item.id)}
            aria-current={isActive ? "page" : undefined}
            aria-label={item.label}
            title={item.label}
          >
            <span className="nav-glyph" aria-hidden="true">
              <UiIcon name={isActive ? item.activeIcon : item.icon} className="ui-icon" />
            </span>
          </button>
            );
          })()
        ))}
      </nav>
    </aside>
  );

  if (!authReady) {
    return <LoginGate loading message="Verificando sesion..." />;
  }

  if (!sessionEmail && activeTab !== "coffee") {
    return (
      <LoginGate
        loading={authBusy}
        message={authBusy ? "Redirigiendo a Google..." : undefined}
        errorMessage={authError}
        onGoogleLogin={handleGoogleLogin}
      />
    );
  }

  const isWideDesktop = mode === "desktop" && viewportWidth >= 1520;
  const useRightRailDetail = Boolean(
    isWideDesktop &&
      detailPanel &&
      ((activeTab === "timeline" && detailHostTab === "timeline") ||
        (activeTab === "search" && detailHostTab === "search") ||
        (activeTab === "profile" && detailHostTab === "profile"))
  );
  const isDesktopComposer = mode === "desktop";

  return (
    <div className={`layout ${mode}`.trim()}>
      {mode === "desktop" ? navRail : null}
      <main className="main-shell">
        <TopBar
          activeTab={activeTab}
          searchQuery={searchQuery}
          searchMode={searchMode}
          onSearchQueryChange={(value) => {
            setSearchQuery(value);
            if (searchFocusCoffeeProfile) setSearchFocusCoffeeProfile(false);
          }}
          onSearchCancel={() => {
            setSearchQuery("");
            setSearchFocusCoffeeProfile(false);
          }}
          onSearchBarcodeClick={() => {
            if (!isMobileOsDevice) return;
            setShowBarcodeScannerSheet(true);
          }}
          showSearchBarcodeButton={isMobileOsDevice && searchMode === "coffees"}
          showSearchCoffeeFilterChips={searchMode === "coffees" && !searchFocusCoffeeProfile}
          searchOriginCount={searchSelectedOrigins.size}
          searchSpecialtyCount={searchSelectedSpecialties.size}
          searchRoastCount={searchSelectedRoasts.size}
          searchFormatCount={searchSelectedFormats.size}
          searchHasRatingFilter={searchMinRating > 0}
          onOpenSearchFilter={(filter) => setSearchActiveFilterType(filter)}
          onSearchBack={() => {
            if (searchMode === "users") {
              setSearchQuery("");
              setSearchFocusCoffeeProfile(false);
              navigateToTab("timeline");
              return;
            }
            setSearchQuery("");
          }}
          showNotificationsBadge={showNotificationsBadge}
          onTimelineSearchUsers={() => {
            setSearchQuery("");
            setSearchFocusCoffeeProfile(false);
            navigateToTab("search", { searchMode: "users" });
          }}
          onTimelineNotifications={() => {
            setShowNotificationsPanel(true);
            const latestSeen = visibleTimelineNotifications.reduce((max, item) => Math.max(max, item.timestamp), 0);
            setNotificationsLastSeenAt((prev) => Math.max(prev, latestSeen));
          }}
          diaryPeriod={diaryPeriod}
          onDiaryOpenQuickActions={() => setShowDiaryQuickActions(true)}
          onDiaryOpenPeriodSelector={() => setShowDiaryPeriodSheet(true)}
          scrolled={topbarScrolled}
          brewStep={brewStep}
          brewStepTitle={getBrewStepTitle(brewStep)}
          onBrewBack={() => {
            if (showCreateCoffeeComposer) {
              closeCreateCoffeeComposer();
              return;
            }
            if (brewStep === "coffee") setBrewStep("method");
            else if (brewStep === "config") setBrewStep("coffee");
            else if (brewStep === "brewing") setBrewStep("config");
            else if (brewStep === "result") setBrewStep("method");
          }}
          onBrewForward={() => {
            if (brewStep === "config") {
              setTimerSeconds(0);
              setBrewRunning(true);
              setBrewStep("brewing");
            }
          }}
          brewCreateCoffeeOpen={showCreateCoffeeComposer}
          onBrewCreateCoffeeBack={closeCreateCoffeeComposer}
          onProfileSignOut={handleSignOut}
          profileMenuEnabled={Boolean(profileUser && activeUser && profileUser.id === activeUser.id)}
          onProfileOpenEdit={() => setProfileEditSignal((prev) => prev + 1)}
          onCoffeeBack={() => {
            if (!sessionEmail) {
              requestLogin();
              return;
            }
            if (window.history.length > 1) {
              window.history.back();
              return;
            }
            navigateToTab("timeline", { replace: true });
          }}
          coffeeTopbarFavoriteActive={activeTab === "coffee" ? detailIsFavorite : false}
          coffeeTopbarStockActive={activeTab === "coffee" ? Boolean(detailPantryStock) : false}
          onCoffeeTopbarToggleFavorite={() => {
            if (!sessionEmail) {
              requestLogin();
              return;
            }
            void saveDetailFavorite();
          }}
          onCoffeeTopbarOpenStock={() => {
            if (!sessionEmail) {
              requestLogin();
              return;
            }
            setDetailOpenStockSignal((prev) => prev + 1);
          }}
        />

        <section aria-live="polite" className="content">
          {activeTab === "timeline" ? (
            <TimelineView
              mode={mode}
              cards={timelineCards}
              recommendations={timelineRecommendations}
              suggestions={timelineSuggestions}
              suggestionIndices={timelineSuggestionIndices}
              followingIds={followingIds}
              followerCounts={followerCounts}
              loading={globalStatus === "Cargando datos..."}
              errorMessage={globalStatus.startsWith("Error") ? globalStatus : null}
              refreshing={timelineRefreshing}
              activeUserId={activeUser?.id ?? null}
              onOpenComments={(postId) => {
                setHighlightedCommentId(null);
                setCommentSheetPostId(postId);
              }}
              onToggleLike={handleToggleLike}
              onToggleFollow={handleToggleFollow}
              onEditPost={handleEditPost}
              onDeletePost={handleDeletePost}
              onRefresh={handleRefreshTimeline}
              onMentionClick={handleMentionNavigation}
              onOpenUserProfile={(userId) => {
                navigateToTab("profile", { profileUserId: userId });
              }}
              onOpenCoffee={(coffeeId) => {
                openCoffeeDetail(coffeeId, "timeline");
              }}
              sidePanel={mode === "desktop" && !useRightRailDetail && detailHostTab === "timeline" ? detailPanel : null}
            />
          ) : null}
          {activeTab === "search" ? (
            <SearchView
              mode={searchMode}
              searchQuery={searchQuery}
              onSearchQueryChange={(value) => {
                setSearchQuery(value);
                if (searchFocusCoffeeProfile) setSearchFocusCoffeeProfile(false);
              }}
              searchFilter={searchFilter}
              onSearchFilterChange={(value) => {
                setSearchFilter(value);
                if (searchFocusCoffeeProfile) setSearchFocusCoffeeProfile(false);
              }}
              selectedOrigins={searchSelectedOrigins}
              selectedRoasts={searchSelectedRoasts}
              selectedSpecialties={searchSelectedSpecialties}
              selectedFormats={searchSelectedFormats}
              minRating={searchMinRating}
              activeFilterType={searchActiveFilterType}
              onSetActiveFilterType={setSearchActiveFilterType}
              originOptions={searchOriginOptions}
              roastOptions={searchRoastOptions}
              specialtyOptions={searchSpecialtyOptions}
              formatOptions={searchFormatOptions}
              onToggleOrigin={(value) =>
                setSearchSelectedOrigins((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onToggleRoast={(value) =>
                setSearchSelectedRoasts((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onToggleSpecialty={(value) =>
                setSearchSelectedSpecialties((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onToggleFormat={(value) =>
                setSearchSelectedFormats((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onSetMinRating={setSearchMinRating}
              onClearCoffeeFilters={() => {
                setSearchSelectedOrigins(new Set());
                setSearchSelectedRoasts(new Set());
                setSearchSelectedSpecialties(new Set());
                setSearchSelectedFormats(new Set());
                setSearchMinRating(0);
              }}
              coffees={filteredCoffees}
              users={filteredSearchUsers}
              followingIds={followingIds}
              selectedCoffee={coffees.find((item) => item.id === searchSelectedCoffeeId) ?? null}
              onSelectCoffee={(coffeeId) => {
                setSearchSelectedCoffeeId(coffeeId);
                setSearchFocusCoffeeProfile(false);
                openCoffeeDetail(coffeeId, "search");
              }}
              onSelectUser={(userId) => {
                navigateToTab("profile", { profileUserId: userId });
              }}
              onToggleFollow={handleToggleFollow}
              focusCoffeeProfile={searchFocusCoffeeProfile}
              onExitCoffeeFocus={() => setSearchFocusCoffeeProfile(false)}
              sidePanel={mode === "desktop" && !useRightRailDetail && detailHostTab === "search" ? detailPanel : null}
            />
          ) : null}
          {activeTab === "coffee" ? (
            detailCoffee ? (
              <CoffeeDetailView
                coffee={detailCoffee}
                reviews={detailCoffeeReviews}
                currentUser={detailCurrentUser}
                currentUserReview={detailCurrentUserReviewWithUser}
                avgRating={detailCoffeeAverageRating}
                isFavorite={detailIsFavorite}
                pantry={detailPantryStock}
                sensory={detailSensoryAverages}
                sensoryDraft={detailSensoryDraft}
                stockDraft={detailStockDraft}
                reviewDraftText={detailReviewText}
                reviewDraftRating={detailReviewRating}
                reviewDraftImagePreviewUrl={detailReviewImagePreviewUrl}
                onClose={() => {
                  if (window.history.length > 1) window.history.back();
                  else navigateToTab("timeline", { replace: true });
                }}
                onToggleFavorite={() => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  void saveDetailFavorite();
                }}
                onReviewTextChange={setDetailReviewText}
                onReviewRatingChange={setDetailReviewRating}
                onReviewImagePick={(file, previewUrl) => {
                  setDetailReviewImageFile(file);
                  setDetailReviewImagePreviewUrl(previewUrl);
                }}
                onSaveReview={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await saveDetailReview();
                }}
                onDeleteReview={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await removeDetailReview();
                }}
                canDeleteReview={Boolean(detailCurrentUserReview)}
                onSensoryDraftChange={setDetailSensoryDraft}
                onSaveSensory={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await saveDetailSensory();
                }}
                onStockDraftChange={setDetailStockDraft}
                onSaveStock={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await saveDetailStock();
                }}
                onOpenUserProfile={(userId) => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  navigateToTab("profile", { profileUserId: userId });
                }}
                isGuest={!sessionEmail}
                onRequireAuth={requestLogin}
                fullPage
                externalOpenStockSignal={detailOpenStockSignal}
              />
            ) : (
              <article className="coffee-detail-empty coffee-detail-empty-full">
                <h2 className="title">Café no encontrado</h2>
                <p className="coffee-sub">La URL no corresponde a ningún café disponible.</p>
                <button
                  type="button"
                  className="action-button"
                  onClick={() => {
                    navigateToTab("search", { searchMode: "coffees", replace: true });
                  }}
                >
                  Volver a Explorar
                </button>
              </article>
            )
          ) : null}
          {activeTab === "brewlab" ? (
            showCreateCoffeeComposer && mode !== "desktop" ? (
              <section className="create-coffee-mobile-screen">{createCoffeePanel}</section>
            ) : (
              <BrewLabView
                brewStep={brewStep}
                setBrewStep={setBrewStep}
                brewMethod={brewMethod}
                setBrewMethod={setBrewMethod}
                brewCoffeeId={brewCoffeeId}
                setBrewCoffeeId={setBrewCoffeeId}
                coffees={brewCoffeeCatalog}
                pantryItems={brewPantryItems}
                onAddNotFoundCoffee={() => {
                  setCreateCoffeeError(null);
                  setShowCreateCoffeeComposer(true);
                }}
                waterMl={waterMl}
                setWaterMl={setWaterMl}
                ratio={ratio}
                setRatio={setRatio}
                timerSeconds={timerSeconds}
                setTimerSeconds={setTimerSeconds}
                brewRunning={brewRunning}
                setBrewRunning={setBrewRunning}
                selectedCoffee={selectedCoffee}
                coffeeGrams={coffeeGrams}
                onSaveResultToDiary={saveBrewToDiary}
              />
            )
          ) : null}
          {activeTab === "diary" ? (
            <DiaryView
              mode={mode}
              tab={diaryTab}
              setTab={setDiaryTab}
              period={diaryPeriod}
              entries={diaryEntriesActivity}
              coffeeCatalog={brewCoffeeCatalog}
              pantryRows={pantryCoffeeRows}
              onDeleteEntry={handleDeleteDiaryEntry}
              onEditEntry={handleUpdateDiaryEntry}
              onUpdatePantryStock={handleUpdatePantryStock}
              onRemovePantryItem={handleRemovePantryItem}
              onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "diary")}
            />
          ) : null}
          {activeTab === "profile" && profileUser ? (
            <ProfileView
              user={profileUser}
              mode={mode}
              tab={profileTab}
              setTab={setProfileTab}
              posts={profilePosts}
              favoriteCoffees={profileUser.id === activeUser?.id ? favoriteCoffees : []}
              allCoffees={coffees}
              coffeeReviews={coffeeReviews}
              coffeeSensoryProfiles={coffeeSensoryProfiles}
              followers={followersCount}
              following={followingCount}
              onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "profile")}
              onOpenUserProfile={(userId) => navigateToTab("profile", { profileUserId: userId })}
              canEditProfile={profileUser.id === activeUser?.id}
              canFollowProfile={Boolean(activeUser && profileUser.id !== activeUser.id)}
              isFollowingProfile={Boolean(profileUser && followingIds.has(profileUser.id))}
              onToggleFollowProfile={async () => {
                if (!profileUser) return;
                await handleToggleFollow(profileUser.id);
              }}
              onSaveProfile={handleUpdateProfile}
              onRemoveFavorite={async (coffeeId) => {
                if (!activeUser) return;
                const exists = favorites.some((item) => item.user_id === activeUser.id && item.coffee_id === coffeeId);
                if (!exists) return;
                await toggleFavoriteCoffee(activeUser.id, coffeeId, true);
                setFavorites((prev) => prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === coffeeId)));
              }}
              onEditPost={handleEditPost}
              onDeletePost={handleDeletePost}
              onToggleLike={handleToggleLike}
              onOpenComments={(postId) => {
                setHighlightedCommentId(null);
                setCommentSheetPostId(postId);
              }}
              externalEditProfileSignal={profileEditSignal}
              sidePanel={mode === "desktop" && !useRightRailDetail && detailHostTab === "profile" ? detailPanel : null}
            />
          ) : null}
        </section>

        {activeTab === "timeline" && mode !== "desktop" ? (
          <button className="fab" type="button" aria-label="Nuevo Post" onClick={openCreatePostComposer}>
            <UiIcon name="add" className="ui-icon" />
          </button>
        ) : null}
      </main>

      {mode === "desktop" ? (
        <aside
          className={useRightRailDetail || (activeTab === "brewlab" && showCreateCoffeeComposer) ? "detail-rail-fixed" : "fab-rail"}
          aria-label={useRightRailDetail ? "Detalle cafe" : activeTab === "brewlab" && showCreateCoffeeComposer ? "Crear cafe" : "Acciones"}
        >
          {useRightRailDetail ? (
            <div className="desktop-detail-wrap">{detailPanel}</div>
          ) : activeTab === "brewlab" && showCreateCoffeeComposer ? (
            <div className="desktop-detail-wrap">{createCoffeePanel}</div>
          ) : activeTab === "timeline" ? (
            <button className="fab fab-desktop" type="button" aria-label="Nuevo Post" onClick={openCreatePostComposer}>
              <UiIcon name="add" className="ui-icon" />
            </button>
          ) : null}
        </aside>
      ) : null}

      {mode === "mobile" && !(activeTab === "brewlab" && showCreateCoffeeComposer) ? <footer className="bottom-tabs">{nav}</footer> : null}

      {showAuthPrompt ? (
        <div className="auth-prompt-overlay" role="dialog" aria-modal="true" aria-label="Acceso" onClick={() => setShowAuthPrompt(false)}>
          <div className="auth-prompt-card" onClick={(event) => event.stopPropagation()}>
            <button type="button" className="auth-prompt-close" aria-label="Cerrar" onClick={() => setShowAuthPrompt(false)}>
              <UiIcon name="close" className="ui-icon" />
            </button>
            <div className="auth-prompt-avatar" aria-hidden="true">
              <img src="/logo.png" alt="" loading="lazy" />
            </div>
            <p className="auth-prompt-copy">
              Únete a la comunidad del cafe para descrubir, elaborar y compartir tu pasión.
            </p>
            <button
              type="button"
              className="action-button auth-prompt-primary"
              disabled={authBusy}
              onClick={() => {
                void handleGoogleLogin();
              }}
            >
              <span className="auth-prompt-google-g" aria-hidden="true">G</span>
              <span>{authBusy ? "Conectando..." : "Continuar con Google"}</span>
            </button>
            {authError ? <p className="auth-prompt-error">{authError}</p> : null}
          </div>
        </div>
      ) : null}

      <MobileBarcodeScannerSheet
        open={showBarcodeScannerSheet}
        onClose={() => setShowBarcodeScannerSheet(false)}
        onDetected={(value) => {
          setSearchQuery(value);
          setSearchFocusCoffeeProfile(false);
          setShowBarcodeScannerSheet(false);
        }}
      />

      {commentSheetPostId ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Comentarios"
          onClick={() => {
            setCommentSheetPostId(null);
            setCommentDraft("");
            setCommentMenuId(null);
            setHighlightedCommentId(null);
            if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
            setCommentImageFile(null);
            setCommentImageName("");
            setCommentImagePreviewError(false);
            setCommentImagePreviewUrl("");
          }}
        >
          <div className="sheet-card" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">COMENTARIOS</strong>
            </header>
            <ul className="sheet-list comments-list" ref={commentListRef}>
              {commentSheetRows.length
                ? commentSheetRows.map((row) => {
                    const user = usersById.get(row.user_id);
                    const isOwnComment = activeUser?.id === row.user_id;
                    return (
                      <li
                        key={row.id}
                        data-comment-id={row.id}
                        className={`sheet-item ${highlightedCommentId === row.id ? "is-highlighted" : ""}`}
                      >
                        <div className="sheet-item-head">
                          {user?.avatar_url ? (
                            <img className="comment-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                          ) : (
                            <div className="comment-avatar comment-avatar-fallback" aria-hidden="true">
                              {(user?.username ?? "us").slice(0, 2).toUpperCase()}
                            </div>
                          )}
                          <div className="comment-copy">
                            <p className="comment-author">@{user?.username ?? `user${row.user_id}`}</p>
                          </div>
                          {isOwnComment ? (
                            <button
                              type="button"
                              className="icon-button post-menu-trigger"
                              onClick={() => setCommentMenuId(row.id)}
                            >
                              <UiIcon name="more" className="ui-icon" />
                            </button>
                          ) : null}
                        </div>
                        <p className="sheet-item-text">
                          <MentionText text={row.text} onMentionClick={handleMentionNavigation} />
                        </p>
                      </li>
                    );
                  })
                : <li className="sheet-item comments-empty">No hay comentarios todavia</li>}
            </ul>
            {activeCommentMenuRow ? (
              <div className="sheet-overlay comment-action-overlay" onClick={() => setCommentMenuId(null)}>
                <div className="sheet-card comment-action-sheet" onClick={(event) => event.stopPropagation()}>
                  <div className="sheet-handle" aria-hidden="true" />
                  <div className="comment-action-list">
                    <p className="comment-action-title">OPCIONES</p>
                    <button
                      type="button"
                      className="comment-action-button"
                      onClick={() => {
                        setEditingCommentId(activeCommentMenuRow.id);
                        setCommentDraft(activeCommentMenuRow.text);
                        setCommentMenuId(null);
                      }}
                    >
                      <UiIcon name="edit" className="ui-icon" />
                      <span>Editar</span>
                      <UiIcon name="chevron-right" className="ui-icon trailing" />
                    </button>
                    <button
                      type="button"
                      className="comment-action-button is-danger"
                      onClick={() => {
                        const confirmed = window.confirm("Borrar comentario?");
                        if (!confirmed) return;
                        setCommentMenuId(null);
                        void handleDeleteComment(activeCommentMenuRow.id);
                      }}
                    >
                      <UiIcon name="trash" className="ui-icon" />
                      <span>Borrar</span>
                      <UiIcon name="chevron-right" className="ui-icon trailing" />
                    </button>
                  </div>
                </div>
              </div>
            ) : null}
            {editingCommentId ? (
              <div className="edit-comment-banner">
                <span>Editando comentario</span>
                <button
                  type="button"
                  className="text-button"
                  onClick={() => {
                    setEditingCommentId(null);
                    setCommentDraft("");
                  }}
                >
                  Cancelar
                </button>
              </div>
            ) : null}
            <div className="sheet-composer">
              <div className="sheet-input-wrap">
                <input
                  ref={commentImageInputRef}
                  type="file"
                  accept="image/*"
                  className="file-input-hidden"
                  onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    if (!file) return;
                    if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
                    const preview = URL.createObjectURL(file);
                    setCommentImageFile(file);
                    setCommentImageName(file.name);
                    setCommentImagePreviewError(false);
                    setCommentImagePreviewUrl(preview);
                    event.currentTarget.value = "";
                  }}
                />
                <div className="sheet-input-shell">
                  {showCommentEmojiPanel ? (
                    <div className="comment-inline-panel emoji-panel">
                      {COMMENT_EMOJIS.map((emoji) => (
                        <button
                          key={emoji}
                          type="button"
                          className="emoji-chip"
                          onClick={() => setCommentDraft((prev) => `${prev}${emoji}`)}
                        >
                          {emoji}
                        </button>
                      ))}
                    </div>
                  ) : commentMentionSuggestions.length ? (
                    <div className="comment-inline-panel mention-suggestions">
                      {commentMentionSuggestions.map((user) => (
                        <button
                          key={user.id}
                          type="button"
                          className="mention-chip"
                          onClick={() => {
                            const parts = commentDraft.split(/\s+/);
                            parts[parts.length - 1] = `@${user.username}`;
                            setCommentDraft(`${parts.join(" ")} `);
                            setShowCommentEmojiPanel(false);
                          }}
                        >
                          {user.avatar_url ? (
                            <img className="mention-chip-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                          ) : (
                            <span className="mention-chip-fallback">{user.username.slice(0, 1).toUpperCase()}</span>
                          )}
                          <span>@{user.username}</span>
                        </button>
                      ))}
                    </div>
                  ) : null}
                  <textarea
                    className="search-wide sheet-input"
                    placeholder="Anade un comentario..."
                    value={commentDraft}
                    rows={2}
                    onChange={(event) => {
                      setCommentDraft(event.target.value);
                      if (event.target.value.endsWith("@")) setShowCommentEmojiPanel(false);
                    }}
                  />
                  <div className="sheet-composer-bottom">
                    <div className="sheet-composer-tools-inline">
                      <button
                        type="button"
                        className="icon-button"
                        onClick={() => commentImageInputRef.current?.click()}
                        aria-label="Agregar foto"
                      >
                        <UiIcon name="camera" className="ui-icon" />
                      </button>
                      <button type="button" className="icon-button" onClick={() => setCommentDraft((prev) => `${prev}@`)}>
                        <UiIcon name="at" className="ui-icon" />
                      </button>
                      <button
                        type="button"
                        className={`icon-button ${showCommentEmojiPanel ? "is-active" : ""}`}
                        onClick={() => setShowCommentEmojiPanel((prev) => !prev)}
                      >
                        <UiIcon name="smile" className="ui-icon" />
                      </button>
                    </div>
                    <button
                      type="button"
                      className="send-button"
                      onClick={editingCommentId ? handleUpdateComment : handleAddComment}
                    >
                      <UiIcon name="send" className="ui-icon" />
                      {editingCommentId ? "Guardar" : "Enviar"}
                    </button>
                  </div>
                  {commentImagePreviewUrl ? (
                    <div className="comment-image-thumb-wrap">
                      {commentImagePreviewError ? (
                        <div className="comment-image-thumb-fallback">{commentImageName || "Imagen seleccionada"}</div>
                      ) : (
                        <img
                          className="comment-image-thumb"
                          src={commentImagePreviewUrl}
                          alt="Miniatura comentario"
                          loading="lazy"
                          onError={() => setCommentImagePreviewError(true)}
                        />
                      )}
                      <button
                        type="button"
                        className="comment-image-remove"
                        onClick={() => {
                          if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
                          setCommentImageFile(null);
                          setCommentImageName("");
                          setCommentImagePreviewError(false);
                          setCommentImagePreviewUrl("");
                        }}
                        aria-label="Quitar imagen"
                      >
                        x
                      </button>
                    </div>
                  ) : null}
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {showCreatePost ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Crear publicacion"
          onClick={resetCreatePostComposer}
        >
          <div className="sheet-card create-post-sheet" onClick={(event) => event.stopPropagation()}>
            <header className="topbar topbar-centered topbar-timeline create-post-header">
              <div className="topbar-slot">
                <button
                  type="button"
                  className="icon-button topbar-icon-button"
                  aria-label={newPostStep === 0 ? "Cerrar" : "Atras"}
                  onClick={() => {
                    if (newPostStep === 0) {
                      resetCreatePostComposer();
                      return;
                    }
                    setNewPostStep(0);
                  }}
                >
                  <UiIcon name={newPostStep === 0 ? "close" : "arrow-left"} className="ui-icon" />
                </button>
              </div>
              <h2 className="title title-upper topbar-title-center topbar-brand-title create-post-title">
                {newPostStep === 0 ? "NUEVO POST" : "DETALLES"}
              </h2>
              <div className="topbar-slot topbar-slot-end">
                {newPostStep === 0 ? (
                  <button
                    type="button"
                    className="icon-button topbar-icon-button"
                    aria-label="Siguiente"
                    onClick={() => setNewPostStep(1)}
                    disabled={!newPostImageFile}
                  >
                    <UiIcon name="arrow-right" className="ui-icon" />
                  </button>
                ) : (
                  <button
                    type="button"
                    className="text-button create-post-publish"
                    onClick={handleCreatePost}
                    disabled={!newPostText.trim() && !newPostImageFile}
                  >
                    PUBLICAR
                  </button>
                )}
              </div>
            </header>
            <div className={`create-post-body create-post-flow ${newPostStep === 0 ? "create-post-step-0" : "create-post-step-1"}`}>
              <input
                ref={newPostImageInputRef}
                type="file"
                accept="image/*"
                multiple
                className="file-input-hidden"
                onChange={(event) => {
                  const files = Array.from(event.target.files ?? []);
                  appendNewPostFiles(files);
                  event.currentTarget.value = "";
                }}
              />
              <input
                ref={newPostCameraInputRef}
                type="file"
                accept="image/*"
                capture="environment"
                className="file-input-hidden"
                onChange={(event) => {
                  const files = Array.from(event.target.files ?? []);
                  appendNewPostFiles(files);
                  event.currentTarget.value = "";
                }}
              />
              {newPostStep === 0 ? (
                <>
                  <button
                    type="button"
                    className="create-post-image-stage"
                    onClick={() => newPostImageInputRef.current?.click()}
                    aria-label="Seleccionar imagen"
                  >
                    {newPostImagePreviewUrl ? (
                      <img className="create-post-preview" src={newPostImagePreviewUrl} alt="Previsualizacion" loading="lazy" />
                    ) : (
                      <span className="create-post-image-placeholder">Selecciona una foto</span>
                    )}
                  </button>
                  {!isDesktopComposer ? (
                    <>
                      <div className="create-post-source-row">
                        <strong>Galeria</strong>
                        <div className="create-post-source-actions">
                          {newPostImagePreviewUrl ? (
                            <button
                              type="button"
                              className="text-button create-post-secondary"
                              onClick={() => {
                                if (!newPostSelectedImageId) return;
                                setNewPostGalleryItems((prev) => {
                                  const target = prev.find((item) => item.id === newPostSelectedImageId);
                                  if (target?.previewUrl.startsWith("blob:")) URL.revokeObjectURL(target.previewUrl);
                                  const next = prev.filter((item) => item.id !== newPostSelectedImageId);
                                  const replacement = next[0] ?? null;
                                  setNewPostSelectedImageId(replacement?.id ?? null);
                                  setNewPostImageFile(replacement?.file ?? null);
                                  setNewPostImagePreviewUrl(replacement?.previewUrl ?? "");
                                  return next;
                                });
                              }}
                            >
                              Quitar
                            </button>
                          ) : null}
                          <button
                            type="button"
                            className="icon-button topbar-icon-button create-post-camera"
                            onClick={() => newPostCameraInputRef.current?.click()}
                            aria-label="Abrir camara"
                          >
                            <UiIcon name="camera" className="ui-icon" />
                          </button>
                        </div>
                      </div>
                      <div className="create-post-gallery-grid" role="list" aria-label="Galeria">
                        {newPostGalleryItems.length ? (
                          newPostGalleryItems.map((item) => {
                            const isSelected = item.id === newPostSelectedImageId;
                            return (
                              <button
                                key={item.id}
                                type="button"
                                role="listitem"
                                className={`create-post-gallery-item ${isSelected ? "is-selected" : ""}`}
                                onClick={() => {
                                  setNewPostSelectedImageId(item.id);
                                  setNewPostImageFile(item.file);
                                  setNewPostImagePreviewUrl(item.previewUrl);
                                }}
                              >
                                <img src={item.previewUrl} alt="Miniatura galeria" loading="lazy" />
                                {isSelected ? <span className="create-post-gallery-check material-symbol-icon is-filled" aria-hidden="true">check_circle</span> : null}
                              </button>
                            );
                          })
                        ) : (
                          <div className="create-post-gallery-empty" role="listitem">No hay imagenes para mostrar</div>
                        )}
                      </div>
                    </>
                  ) : null}
                </>
              ) : (
                <>
                  {activeUser ? (
                    <div className="create-post-user-row">
                      {activeUser.avatar_url ? (
                        <img src={activeUser.avatar_url} alt={activeUser.username} loading="lazy" />
                      ) : (
                        <div className="create-post-user-fallback">{activeUser.username.slice(0, 1).toUpperCase()}</div>
                      )}
                      <div>
                        <p>{activeUser.full_name}</p>
                        <span>@{activeUser.username}</span>
                      </div>
                    </div>
                  ) : null}
                  <div className="create-post-composer-card sheet-input-shell">
                    <textarea
                      id="new-post-text"
                      className="search-wide sheet-input create-post-textarea"
                      placeholder="¿Qué estás pensando?"
                      rows={4}
                      value={newPostText}
                      onChange={(event) => setNewPostText(event.target.value)}
                    />
                    {showCreatePostEmojiPanel ? (
                      <div className="create-post-inline-panel">
                        {COMMENT_EMOJIS.map((emoji) => (
                          <button
                            key={emoji}
                            type="button"
                            className="emoji-chip"
                            onClick={() => setNewPostText((prev) => `${prev}${emoji}`)}
                          >
                            {emoji}
                          </button>
                        ))}
                      </div>
                    ) : createPostMentionSuggestions.length && !newPostText.trim().endsWith("@") ? (
                      <div className="create-post-inline-panel create-post-mention-suggestions">
                        {createPostMentionSuggestions.map((user) => (
                          <button
                            key={user.id}
                            type="button"
                            className="mention-chip"
                            onClick={() => {
                              const parts = newPostText.split(/\s+/);
                              parts[parts.length - 1] = `@${user.username}`;
                              setNewPostText(`${parts.join(" ")} `);
                            }}
                          >
                            {user.avatar_url ? (
                              <img className="mention-chip-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                            ) : (
                              <span className="mention-chip-fallback">{user.username.slice(0, 1).toUpperCase()}</span>
                            )}
                            <span>@{user.username}</span>
                          </button>
                        ))}
                      </div>
                    ) : null}
                    <div className="create-post-composer-tools">
                      <button
                        type="button"
                        className="icon-button"
                        onClick={() => {
                          setNewPostText((prev) => `${prev}@`);
                          setShowCreatePostEmojiPanel(false);
                        }}
                        aria-label="Mencionar"
                      >
                        <UiIcon name="at" className="ui-icon" />
                      </button>
                      <button
                        type="button"
                        className={`icon-button ${showCreatePostEmojiPanel ? "is-active" : ""}`}
                        onClick={() => setShowCreatePostEmojiPanel((prev) => !prev)}
                        aria-label="Emojis"
                      >
                        <UiIcon name="smile" className="ui-icon" />
                      </button>
                    </div>
                  </div>
                  <button
                    type="button"
                    className="create-post-coffee-row"
                    onClick={() => setShowCreatePostCoffeeSheet(true)}
                  >
                    <UiIcon name="coffee" className="ui-icon" />
                    <span>Anadir cafe</span>
                    <strong>{selectedCreatePostCoffee ? selectedCreatePostCoffee.nombre : "Seleccionar cafe"}</strong>
                    <UiIcon name="chevron-right" className="ui-icon" />
                  </button>
                  {newPostImagePreviewUrl ? (
                    <div className="create-post-image-detail-wrap">
                      <img className="create-post-preview create-post-preview-detail" src={newPostImagePreviewUrl} alt="Previsualizacion" loading="lazy" />
                      <button
                        type="button"
                        className="create-post-image-remove"
                        onClick={() => {
                          if (!newPostSelectedImageId) return;
                          setNewPostGalleryItems((prev) => {
                            const target = prev.find((item) => item.id === newPostSelectedImageId);
                            if (target?.previewUrl.startsWith("blob:")) URL.revokeObjectURL(target.previewUrl);
                            const next = prev.filter((item) => item.id !== newPostSelectedImageId);
                            const replacement = next[0] ?? null;
                            setNewPostSelectedImageId(replacement?.id ?? null);
                            setNewPostImageFile(replacement?.file ?? null);
                            setNewPostImagePreviewUrl(replacement?.previewUrl ?? "");
                            return next;
                          });
                        }}
                        aria-label="Quitar foto"
                      >
                        <UiIcon name="close" className="ui-icon" />
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      className="create-post-add-photo-row"
                      onClick={() => newPostImageInputRef.current?.click()}
                    >
                      <UiIcon name="camera" className="ui-icon" />
                      <span>Anadir foto</span>
                      <UiIcon name="chevron-right" className="ui-icon" />
                    </button>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      ) : null}

      {showCreatePostCoffeeSheet ? (
        <div className="sheet-overlay create-post-coffee-overlay" role="dialog" aria-modal="true" aria-label="Seleccionar cafe" onClick={() => setShowCreatePostCoffeeSheet(false)}>
          <div className="sheet-card create-post-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">SELECCIONAR CAFE</strong>
            </header>
            <div className="create-post-coffee-body">
              <input
                className="search-wide"
                placeholder="Buscar cafe"
                value={createPostCoffeeQuery}
                onChange={(event) => setCreatePostCoffeeQuery(event.target.value)}
              />
              <ul className="create-post-coffee-list">
                {filteredCreatePostCoffees.map((coffee) => (
                  <li key={coffee.id}>
                    <button
                      type="button"
                      className={`create-post-coffee-item ${coffee.id === newPostCoffeeId ? "is-selected" : ""}`}
                      onClick={() => {
                        setNewPostCoffeeId(coffee.id);
                        setShowCreatePostCoffeeSheet(false);
                      }}
                    >
                      {coffee.image_url ? <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" /> : <span className="create-post-coffee-fallback">{coffee.nombre.slice(0, 1).toUpperCase()}</span>}
                      <div>
                        <p>{coffee.nombre}</p>
                        <span>{coffee.marca}</span>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      ) : null}

      {showNotificationsPanel ? (
        <div
          className="sheet-overlay notifications-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Notificaciones"
          onClick={closeNotificationsPanel}
        >
          <div className="sheet-card notifications-panel" onClick={(event) => event.stopPropagation()}>
            <header className="topbar topbar-centered topbar-timeline notifications-header">
              <div className="topbar-slot">
                <button
                  type="button"
                  className="icon-button topbar-icon-button notifications-back"
                  onClick={closeNotificationsPanel}
                  aria-label="Atras"
                >
                  <UiIcon name="arrow-left" className="ui-icon" />
                </button>
              </div>
              <h2 className="title title-upper topbar-title-center topbar-brand-title notifications-title">NOTIFICACIONES</h2>
              <div className="topbar-slot topbar-slot-end">
                <button type="button" className="icon-button topbar-icon-button notifications-header-spacer" aria-hidden="true" tabIndex={-1}>
                  <UiIcon name="notifications" className="ui-icon" />
                </button>
              </div>
            </header>
            <ul className="sheet-list notifications-list">
              {visibleTimelineNotifications.length ? (
                visibleTimelineNotifications.map((item) => {
                  const user = usersById.get(item.userId);
                  const isUnread = item.timestamp > notificationsLastSeenAt;
                  return (
                    <NotificationRow
                      key={item.id}
                      item={item}
                      user={user}
                      isUnread={isUnread}
                      isFollowing={followingIds.has(item.userId)}
                      isDismissing={dismissingNotificationIds.has(item.id)}
                      onDismiss={dismissNotification}
                      onOpen={() => {
                        if (item.type === "comment" && item.postId) {
                          setCommentSheetPostId(item.postId);
                          setHighlightedCommentId(item.commentId ?? null);
                          closeNotificationsPanel();
                          return;
                        }
                        closeNotificationsPanel();
                      }}
                      onToggleFollow={handleToggleFollow}
                      onOpenUserProfile={() => {
                        navigateToTab("profile", { profileUserId: item.userId });
                        closeNotificationsPanel();
                      }}
                      onReply={() => {
                        if (item.type !== "comment" || !item.postId) return;
                        setCommentSheetPostId(item.postId);
                        setHighlightedCommentId(item.commentId ?? null);
                        closeNotificationsPanel();
                      }}
                    />
                  );
                })
              ) : (
                <li className="sheet-item notifications-empty">No tienes notificaciones</li>
              )}
            </ul>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryQuickActions ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Nuevo registro" onClick={() => setShowDiaryQuickActions(false)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">NUEVO REGISTRO</strong>
            </header>
            <div className="diary-sheet-list">
              <button
                type="button"
                className="diary-sheet-action is-water"
                onClick={() => {
                  setDiaryWaterMlDraft("250");
                  setShowDiaryQuickActions(false);
                  setShowDiaryWaterSheet(true);
                }}
              >
                <UiIcon name="stock" className="ui-icon" />
                <span>Agua</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
              <button
                type="button"
                className="diary-sheet-action is-coffee"
                onClick={() => {
                  const defaultCoffee = diaryCoffeeOptions[0];
                  if (defaultCoffee) {
                    setDiaryCoffeeIdDraft(defaultCoffee.id);
                    const isDecaf = normalizeLookupText(defaultCoffee.cafeina ?? "").includes("sin");
                    setDiaryCoffeeCaffeineDraft(isDecaf ? "5" : "95");
                  } else {
                    setDiaryCoffeeIdDraft("");
                    setDiaryCoffeeCaffeineDraft("95");
                  }
                  setDiaryCoffeeMlDraft("250");
                  setDiaryCoffeePreparationDraft("Manual");
                  setShowDiaryQuickActions(false);
                  setShowDiaryCoffeeSheet(true);
                }}
              >
                <UiIcon name="coffee" className="ui-icon" />
                <span>Café</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
              <button
                type="button"
                className="diary-sheet-action is-pantry"
                onClick={() => {
                  const defaultCoffee = diaryCoffeeOptions[0];
                  setDiaryPantryCoffeeIdDraft(defaultCoffee?.id ?? "");
                  setDiaryPantryGramsDraft("250");
                  setShowDiaryQuickActions(false);
                  setShowDiaryAddPantrySheet(true);
                }}
              >
                <UiIcon name="add" className="ui-icon" />
                <span>Añadir a Despensa</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryPeriodSheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Seleccionar periodo" onClick={() => setShowDiaryPeriodSheet(false)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">SELECCIONAR PERIODO</strong>
            </header>
            <div className="diary-sheet-list">
              {([
                { value: "hoy", label: "HOY" },
                { value: "7d", label: "SEMANA" },
                { value: "30d", label: "MES" }
              ] as const).map((option) => (
                <button
                  key={option.value}
                  type="button"
                  className={`diary-sheet-action ${diaryPeriod === option.value ? "is-active" : ""}`.trim()}
                  onClick={() => {
                    setDiaryPeriod(option.value);
                    setShowDiaryPeriodSheet(false);
                  }}
                >
                  <span>{option.label}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryWaterSheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Registrar agua" onClick={() => setShowDiaryWaterSheet(false)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">REGISTRAR AGUA</strong>
            </header>
            <div className="diary-sheet-form">
              <div className="diary-water-presets">
                {[250, 500, 750].map((value) => (
                  <button
                    key={value}
                    type="button"
                    className={`chip-button period-chip ${Number(diaryWaterMlDraft) === value ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryWaterMlDraft(String(value))}
                  >
                    {value} ml
                  </button>
                ))}
              </div>
              <label>
                <span>Cantidad personalizada (ml)</span>
                <input
                  className="search-wide"
                  type="number"
                  min={1}
                  value={diaryWaterMlDraft}
                  onChange={(event) => setDiaryWaterMlDraft(event.target.value)}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={() => setShowDiaryWaterSheet(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  onClick={async () => {
                    if (!activeUser) return;
                    const amount = Math.max(1, Math.round(Number(diaryWaterMlDraft || 0)));
                    const created = await createDiaryEntry({
                      userId: activeUser.id,
                      coffeeId: null,
                      coffeeName: "Agua",
                      amountMl: amount,
                      caffeineMg: 0,
                      preparationType: "None",
                      type: "WATER"
                    });
                    setDiaryEntries((prev) => [created, ...prev]);
                    setShowDiaryWaterSheet(false);
                  }}
                >
                  Guardar
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryCoffeeSheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Registrar cafe" onClick={() => setShowDiaryCoffeeSheet(false)}>
          <div className="sheet-card diary-sheet diary-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">REGISTRAR CAFÉ</strong>
            </header>
            <div className="diary-sheet-form">
              <div className="diary-edit-entry-presets diary-coffee-method-presets">
                {[
                  { label: "Espresso", drawable: "maq_espresso.png" },
                  { label: "V60", drawable: "maq_hario_v60.png" },
                  { label: "Aeropress", drawable: "maq_aeropress.png" },
                  { label: "Moka", drawable: "maq_italiana.png" }
                ].map((method) => (
                  <button
                    key={method.label}
                    type="button"
                    className={`chip-button period-chip diary-edit-entry-method-chip ${normalizeLookupText(diaryCoffeePreparationDraft) === normalizeLookupText(method.label) ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryCoffeePreparationDraft(method.label)}
                  >
                    <img src={`/android-drawable/${method.drawable}`} alt="" aria-hidden="true" />
                    <span>{method.label}</span>
                  </button>
                ))}
              </div>
              <label>
                <span>Café</span>
                <div className="diary-coffee-picker" role="listbox" aria-label="Seleccionar café">
                  {diaryCoffeeOptions.map((coffee) => {
                    const selected = coffee.id === diaryCoffeeIdDraft;
                    return (
                      <button
                        key={coffee.id}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={`diary-coffee-picker-item ${selected ? "is-active" : ""}`.trim()}
                        onClick={() => {
                          setDiaryCoffeeIdDraft(coffee.id);
                          const isDecaf = normalizeLookupText(coffee.cafeina ?? "").includes("sin");
                          setDiaryCoffeeCaffeineDraft(isDecaf ? "5" : "95");
                        }}
                      >
                        <span className="diary-coffee-picker-media">
                          {coffee.image_url ? (
                            <img src={coffee.image_url} alt="" loading="lazy" />
                          ) : (
                            <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" />
                          )}
                        </span>
                        <span className="diary-coffee-picker-copy">
                          <strong>{coffee.nombre}</strong>
                          <em>{(coffee.marca || "CAFÉ").toUpperCase()}</em>
                        </span>
                      </button>
                    );
                  })}
                </div>
              </label>
              <label>
                <span>Preparación</span>
                <input
                  className="search-wide"
                  value={diaryCoffeePreparationDraft}
                  onChange={(event) => setDiaryCoffeePreparationDraft(event.target.value)}
                />
              </label>
              <label>
                <span>Cantidad (ml)</span>
                <input
                  className="search-wide diary-edit-entry-input"
                  type="number"
                  inputMode="numeric"
                  min={1}
                  value={diaryCoffeeMlDraft}
                  onChange={(event) => setDiaryCoffeeMlDraft(event.target.value)}
                />
                <input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={10}
                  max={700}
                  step={10}
                  value={Math.max(10, Number(diaryCoffeeMlDraft || 10))}
                  onChange={(event) => setDiaryCoffeeMlDraft(String(Math.max(10, Number(event.target.value || 10))))}
                />
              </label>
              <div className="diary-coffee-size-presets">
                {[
                  { label: "Espresso", ml: 30, drawable: "taza_espresso.png" },
                  { label: "Peq.", ml: 180, drawable: "taza_pequeno.png" },
                  { label: "Med.", ml: 275, drawable: "taza_mediano.png" },
                  { label: "Gra.", ml: 375, drawable: "taza_grande.png" },
                  { label: "XL", ml: 475, drawable: "taza_xl.png" }
                ].map((size) => (
                  <button
                    key={size.label}
                    type="button"
                    className={`chip-button period-chip diary-coffee-size-chip ${Math.abs(Number(diaryCoffeeMlDraft || 0) - size.ml) <= 10 ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryCoffeeMlDraft(String(size.ml))}
                  >
                    <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" />
                    <span>{size.label}</span>
                  </button>
                ))}
              </div>
              <label>
                <span>Cafeína (mg)</span>
                <input
                  className="search-wide diary-edit-entry-input"
                  type="number"
                  inputMode="numeric"
                  min={0}
                  value={diaryCoffeeCaffeineDraft}
                  onChange={(event) => setDiaryCoffeeCaffeineDraft(event.target.value)}
                />
                <input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={0}
                  max={350}
                  step={5}
                  value={Math.max(0, Number(diaryCoffeeCaffeineDraft || 0))}
                  onChange={(event) => setDiaryCoffeeCaffeineDraft(String(Math.max(0, Number(event.target.value || 0))))}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={() => setShowDiaryCoffeeSheet(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  onClick={async () => {
                    if (!activeUser) return;
                    if (!selectedDiaryCoffee) return;
                    const created = await createDiaryEntry({
                      userId: activeUser.id,
                      coffeeId: selectedDiaryCoffee.id,
                      coffeeName: selectedDiaryCoffee.nombre,
                      amountMl: Math.max(1, Number(diaryCoffeeMlDraft || 0)),
                      caffeineMg: Math.max(0, Number(diaryCoffeeCaffeineDraft || 0)),
                      preparationType: diaryCoffeePreparationDraft.trim() || "Manual",
                      type: "CUP"
                    });
                    setDiaryEntries((prev) => [created, ...prev]);
                    setShowDiaryCoffeeSheet(false);
                  }}
                >
                  Guardar
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryAddPantrySheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Añadir a despensa" onClick={() => setShowDiaryAddPantrySheet(false)}>
          <div className="sheet-card diary-sheet diary-pantry-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">AÑADIR A DESPENSA</strong>
            </header>
            <div className="diary-sheet-form">
              {selectedDiaryPantryCoffee ? (
                <div className="diary-edit-entry-preview" aria-hidden="true">
                  <div className="diary-entry-media">
                    {selectedDiaryPantryCoffee.image_url ? (
                      <img src={selectedDiaryPantryCoffee.image_url} alt={selectedDiaryPantryCoffee.nombre} loading="lazy" />
                    ) : (
                      <img className="diary-entry-fallback-drawable" src="/android-drawable/taza_mediano.png" alt="" aria-hidden="true" loading="lazy" />
                    )}
                  </div>
                  <div className="diary-entry-copy">
                    <p className="feed-user">{selectedDiaryPantryCoffee.nombre}</p>
                    <p className="feed-meta diary-entry-brand">{(selectedDiaryPantryCoffee.marca || "CAFÉ").toUpperCase()}</p>
                  </div>
                </div>
              ) : null}
              <label>
                <span>Café</span>
                <div className="diary-coffee-picker" role="listbox" aria-label="Seleccionar café para despensa">
                  {diaryCoffeeOptions.map((coffee) => {
                    const selected = coffee.id === diaryPantryCoffeeIdDraft;
                    return (
                      <button
                        key={coffee.id}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={`diary-coffee-picker-item ${selected ? "is-active" : ""}`.trim()}
                        onClick={() => setDiaryPantryCoffeeIdDraft(coffee.id)}
                      >
                        <span className="diary-coffee-picker-media">
                          {coffee.image_url ? (
                            <img src={coffee.image_url} alt="" loading="lazy" />
                          ) : (
                            <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" />
                          )}
                        </span>
                        <span className="diary-coffee-picker-copy">
                          <strong>{coffee.nombre}</strong>
                          <em>{(coffee.marca || "CAFÉ").toUpperCase()}</em>
                        </span>
                      </button>
                    );
                  })}
                </div>
              </label>
              <div className="diary-water-presets diary-edit-entry-presets is-water">
                {[100, 250, 500].map((value) => (
                  <button
                    key={value}
                    type="button"
                    className={`chip-button period-chip ${Number(diaryPantryGramsDraft) === value ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryPantryGramsDraft(String(value))}
                  >
                    {value} g
                  </button>
                ))}
              </div>
              <label>
                <span>Gramos a añadir</span>
                <div className="diary-edit-entry-measure">
                  <button
                    type="button"
                    className="diary-edit-entry-step"
                    aria-label="Reducir gramos"
                    onClick={() => bumpPantryGrams(-25)}
                  >
                    -
                  </button>
                  <input
                    className="search-wide diary-edit-entry-input"
                    type="number"
                    inputMode="numeric"
                    min={1}
                    value={diaryPantryGramsDraft}
                    onChange={(event) => setDiaryPantryGramsDraft(event.target.value)}
                  />
                  <span className="diary-edit-entry-unit" aria-hidden="true">g</span>
                  <button
                    type="button"
                    className="diary-edit-entry-step"
                    aria-label="Aumentar gramos"
                    onClick={() => bumpPantryGrams(25)}
                  >
                    +
                  </button>
                </div>
                <input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={1}
                  max={2000}
                  step={25}
                  value={Math.max(1, Number(diaryPantryGramsDraft || 1))}
                  onChange={(event) => setDiaryPantryGramsDraft(String(Math.max(1, Number(event.target.value || 1))))}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost diary-edit-entry-cancel" onClick={() => setShowDiaryAddPantrySheet(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button diary-edit-entry-save"
                  onClick={async () => {
                    if (!activeUser) return;
                    if (!selectedDiaryPantryCoffee) return;
                    const gramsToAdd = Math.max(1, Math.round(Number(diaryPantryGramsDraft || 0)));
                    const existing = pantryItems.find(
                      (item) => item.user_id === activeUser.id && item.coffee_id === selectedDiaryPantryCoffee.id
                    );
                    const total = Math.max(gramsToAdd, (existing?.total_grams ?? 0) + gramsToAdd);
                    const remaining = Math.max(gramsToAdd, (existing?.grams_remaining ?? 0) + gramsToAdd);
                    const updated = await upsertPantryStock({
                      coffeeId: selectedDiaryPantryCoffee.id,
                      userId: activeUser.id,
                      totalGrams: total,
                      gramsRemaining: remaining
                    });
                    setPantryItems((prev) =>
                      [updated, ...prev.filter((row) => !(row.user_id === updated.user_id && row.coffee_id === updated.coffee_id))]
                    );
                    setDiaryTab("despensa");
                    setShowDiaryAddPantrySheet(false);
                  }}
                >
                  Guardar
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}




