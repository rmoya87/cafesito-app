import React, { Suspense, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { requestAccountDeletion, syncAccountLifecycleAfterLogin, toggleFavoriteCoffee } from "../data/supabaseApi";
import { BREW_METHODS, COMMENT_EMOJIS } from "../config/brew";
import { buildRoute, getAppRootPath, isKnownRoute, parseRoute } from "../core/routing";
import { sendPageView } from "../core/ga4";
import { shouldUseRightRailDetail, sidePanelForTab } from "../core/layouts";
import { canAccessTabAsGuest, resolveGuardedTab } from "../core/guards";
import { getBrewStepTitle } from "../core/brew";
import { normalizeLookupText } from "../core/text";

import { useGlobalUiEvents } from "../hooks/useGlobalUiEvents";
import { useResponsiveMode } from "../hooks/useResponsiveMode";
import { useAuthSession } from "../hooks/domains/useAuthSession";
import { useInitialDataLoader } from "../hooks/domains/useInitialDataLoader";
import { useUserDataLoader } from "../hooks/domains/useUserDataLoader";
import { useBrewTimer } from "../hooks/domains/useBrewTimer";
import { useAuthActionGuard } from "../hooks/domains/useAuthActionGuard";
import { useSearchDomain } from "../hooks/domains/useSearchDomain";
import { useProfileDomain } from "../hooks/domains/useProfileDomain";
import { useCoffeeDetailDomain, useCoffeeDetailDraftSync } from "../hooks/domains/useCoffeeDetailDomain";
import { useCoffeeDetailActions } from "../hooks/domains/useCoffeeDetailActions";
import { useTimelineComposerDomain } from "../hooks/domains/useTimelineComposerDomain";
import { useAppNavigationDomain } from "../hooks/domains/useAppNavigationDomain";
import { useTimelineActions } from "../hooks/domains/useTimelineActions";
import { useCreateCoffeeDomain } from "../hooks/domains/useCreateCoffeeDomain";
import { useProfileActions } from "../hooks/domains/useProfileActions";
import { useDiaryActions } from "../hooks/domains/useDiaryActions";
import { useDiarySheetActions } from "../hooks/domains/useDiarySheetActions";
import { useTimelineSheetActions } from "../hooks/domains/useTimelineSheetActions";
import { useNotificationsDomain } from "../hooks/domains/useNotificationsDomain";
import { useCoffeeDetailNavigation } from "../hooks/domains/useCoffeeDetailNavigation";
import { useCoffeeDetailInteractions } from "../hooks/domains/useCoffeeDetailInteractions";
import { useTopBarActions } from "../hooks/domains/useTopBarActions";
import { useAppDerivedData } from "../hooks/domains/useAppDerivedData";
import { useAppUiEffects } from "../hooks/domains/useAppUiEffects";
import { useCoffeeSeoMeta } from "../hooks/domains/useCoffeeSeoMeta";
import { useCoffeeRouteSync, useRouteCanonicalSync, useRouteGuardSync } from "../hooks/domains/useRouteSync";
import { getSupabaseClient, supabaseConfigError } from "../supabase";
import { TopBar } from "../features/topbar/TopBar";
import {
  LazyTimelineView,
  LazySearchView,
  LazyCoffeeDetailView,
  LazyBrewLabView,
  LazyCreateCoffeeView,
  LazyDiaryView,
  LazyProfileView,
  LazyNotFoundView
} from "./lazyViews";
import { NotificationsSheet } from "../features/timeline/NotificationsSheet";
import { CommentSheet } from "../features/timeline/CommentSheet";
import { CreatePostSheet } from "../features/timeline/CreatePostSheet";
import { MobileBarcodeScannerSheet } from "../features/search/MobileBarcodeScannerSheet";
import { LoginGate } from "../features/auth/LoginGate";
import { AuthPromptOverlay } from "../features/auth/AuthPromptOverlay";
import { DiarySheets } from "../features/diary/DiarySheets";
import { BottomNav, DesktopNavRail } from "../features/navigation/NavControls";
import { AppContentRouter } from "./AppContentRouter";
import { AppOverlayLayers } from "./AppOverlayLayers";

import { UiIcon } from "../ui/iconography";
import { Button, IconButton } from "../ui/components";
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
  NotificationRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  TabId,
  UserRow
} from "../types";

export function AppContainer() {
  const initialRoute = parseRoute(window.location.pathname);
  const isNotFoundRoute = !isKnownRoute(window.location.pathname);
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
  const [notifications, setNotifications] = useState<NotificationRow[]>([]);

  const {
    searchQuery,
    setSearchQuery,
    searchMode,
    setSearchMode,
    searchFilter,
    searchSelectedOrigins,
    searchSelectedRoasts,
    searchSelectedSpecialties,
    searchSelectedFormats,
    searchMinRating,
    setSearchMinRating,
    searchActiveFilterType,
    setSearchActiveFilterType,
    searchSelectedCoffeeId,
    setSearchSelectedCoffeeId,
    searchFocusCoffeeProfile,
    setSearchFocusCoffeeProfile,
    onSearchQueryChange,
    onSearchFilterChange,
    onToggleOrigin,
    onToggleRoast,
    onToggleSpecialty,
    onToggleFormat,
    onClearCoffeeFilters,
    onSelectCoffee,
    resetSearchUi
  } = useSearchDomain(initialRoute.searchMode);
  const {
    detailCoffeeId,
    setDetailCoffeeId,
    detailHostTab,
    setDetailHostTab,
    detailReviewText,
    setDetailReviewText,
    detailReviewRating,
    setDetailReviewRating,
    detailReviewImageFile,
    setDetailReviewImageFile,
    detailReviewImagePreviewUrl,
    setDetailReviewImagePreviewUrl,
    detailSensoryDraft,
    setDetailSensoryDraft,
    detailStockDraft,
    setDetailStockDraft,
    detailOpenStockSignal,
    setDetailOpenStockSignal
  } = useCoffeeDetailDomain();
  const [showBarcodeScannerSheet, setShowBarcodeScannerSheet] = useState(false);
  const [barcodeOrigin, setBarcodeOrigin] = useState<"search" | "diary" | "brew" | "createPostCoffee" | null>(null);
  const [barcodeDetectedValueForDiary, setBarcodeDetectedValueForDiary] = useState<string | null>(null);
  const [barcodeDetectedValueForBrew, setBarcodeDetectedValueForBrew] = useState<string | null>(null);

  const [brewStep, setBrewStep] = useState<BrewStep>("method");
  const [brewMethod, setBrewMethod] = useState("");
  const [brewCoffeeId, setBrewCoffeeId] = useState<string>("");
  const [waterMl, setWaterMl] = useState(300);
  const [ratio, setRatio] = useState(16);
  const [timerSeconds, setTimerSeconds] = useState(150);
  const [brewRunning, setBrewRunning] = useState(false);
  const [diaryTab, setDiaryTab] = useState<"actividad" | "despensa">("actividad");
  const [diaryPeriod, setDiaryPeriod] = useState<"hoy" | "7d" | "30d">("hoy");
  const [showDiaryQuickActions, setShowDiaryQuickActions] = useState(false);
  const [showDiaryPeriodSheet, setShowDiaryPeriodSheet] = useState(false);
  const [showDiaryWaterSheet, setShowDiaryWaterSheet] = useState(false);
  const [diaryWaterMlDraft, setDiaryWaterMlDraft] = useState("250");
  const [showDiaryCoffeeSheet, setShowDiaryCoffeeSheet] = useState(false);
  const [coffeeSheetStep, setCoffeeSheetStep] = useState<"select" | "dose" | "tipo" | "tamaño" | "createCoffee">("select");
  const [diaryCoffeeIdDraft, setDiaryCoffeeIdDraft] = useState("");
  const [diaryCoffeeGramsDraft, setDiaryCoffeeGramsDraft] = useState("15");
  const [diaryCoffeeMlDraft, setDiaryCoffeeMlDraft] = useState("250");
  const [diaryCoffeeCaffeineDraft, setDiaryCoffeeCaffeineDraft] = useState("95");
  const [diaryCoffeePreparationDraft, setDiaryCoffeePreparationDraft] = useState("Manual");
  const [showDiaryAddPantrySheet, setShowDiaryAddPantrySheet] = useState(false);
  const [pantrySheetStep, setPantrySheetStep] = useState<"select" | "form" | "createCoffee">("select");
  const [diaryPantryCoffeeIdDraft, setDiaryPantryCoffeeIdDraft] = useState("");
  const [diaryPantryGramsDraft, setDiaryPantryGramsDraft] = useState("250");
  const [lastCreatedCoffeeNameForSheet, setLastCreatedCoffeeNameForSheet] = useState<string | null>(null);
  const {
    profileTab,
    setProfileTab,
    profileUsername,
    setProfileUsername,
    profileEditSignal,
    triggerProfileEdit
  } = useProfileDomain(initialRoute.profileUsername, activeTab);

  const {
    commentSheetPostId,
    setCommentSheetPostId,
    commentDraft,
    setCommentDraft,
    editingCommentId,
    setEditingCommentId,
    commentMenuId,
    setCommentMenuId,
    highlightedCommentId,
    setHighlightedCommentId,
    showCommentEmojiPanel,
    setShowCommentEmojiPanel,
    commentImageFile,
    setCommentImageFile,
    commentImageName,
    setCommentImageName,
    commentImagePreviewError,
    setCommentImagePreviewError,
    commentImagePreviewUrl,
    setCommentImagePreviewUrl,
    showCreatePost,
    newPostText,
    setNewPostText,
    newPostImageFile,
    setNewPostImageFile,
    newPostImagePreviewUrl,
    setNewPostImagePreviewUrl,
    newPostGalleryItems,
    setNewPostGalleryItems,
    newPostSelectedImageId,
    setNewPostSelectedImageId,
    newPostCoffeeId,
    setNewPostCoffeeId,
    showCreatePostCoffeeSheet,
    setShowCreatePostCoffeeSheet,
    createPostCoffeeQuery,
    setCreatePostCoffeeQuery,
    showCreatePostEmojiPanel,
    setShowCreatePostEmojiPanel,
    newPostImageInputRef,
    resetCreatePostComposer,
    openCreatePostComposer,
    appendNewPostFiles
  } = useTimelineComposerDomain();
  const [handledTimelineDeepLink, setHandledTimelineDeepLink] = useState(false);
  const [topbarScrolled, setTopbarScrolled] = useState(false);
  const mainScrollRef = useRef<HTMLDivElement>(null);
  const TOPBAR_HIDE_SCROLL_PX = 100;
  const [timelineActionBanner, setTimelineActionBanner] = useState<string | null>(null);
  const [timelineRefreshing, setTimelineRefreshing] = useState(false);
  const [timelineBusyMessage, setTimelineBusyMessage] = useState<string | null>(null);
  const {
    showNotificationsPanel,
    setShowNotificationsPanel,
    notificationsLastSeenAt,
    setNotificationsLastSeenAt,
    dismissedNotificationIds,
    dismissingNotificationIds,
    dismissNotificationTimersRef,
    closeNotificationsPanel,
    dismissNotification
  } = useNotificationsDomain();
  const {
    authReady,
    sessionEmail,
    authBusy,
    authError,
    showAuthPrompt,
    setAuthError,
    setShowAuthPrompt,
    handleGoogleLogin,
    requestLogin
  } = useAuthSession();
  const isMobileOsDevice = useMemo(() => /Android|iPhone|iPad|iPod/i.test(window.navigator.userAgent), []);

  const activeUser = useMemo(() => {
    if (!users.length) return null;
    if (sessionEmail) {
      const sessionEmailNormalized = normalizeLookupText(sessionEmail);
      const found = users.find((user) => normalizeLookupText(user.email) === sessionEmailNormalized);
      return found ?? null;
    }
    return users[0] ?? null;
  }, [sessionEmail, users]);
  const { runWithAuth } = useAuthActionGuard({ sessionEmail, onRequireAuth: requestLogin });

  const reloadInitialData = useInitialDataLoader({
    authReady,
    setUsers,
    setCoffees,
    setCoffeeReviews,
    setCoffeeSensoryProfiles,
    setPosts,
    setLikes,
    setComments,
    setPostCoffeeTags,
    setFollows,
    setBrewCoffeeId,
    setGlobalStatus
  });

  useUserDataLoader({
    activeUser,
    setDiaryEntries,
    setPantryItems,
    setFavorites,
    setCustomCoffees,
    setNotifications,
    setGlobalStatus
  });

  const {
    showCreateCoffeeComposer,
    setShowCreateCoffeeComposer,
    createCoffeeSaving,
    createCoffeeError,
    createCoffeeDraft,
    setCreateCoffeeDraft,
    createCoffeeImagePreviewUrl,
    openCreateCoffeeComposer,
    closeCreateCoffeeComposer,
    saveCreateCoffee,
    onPickImage: onPickCreateCoffeeImage,
    onRemoveImage: onRemoveCreateCoffeeImage,
    resetCreateCoffeeDomain
  } = useCreateCoffeeDomain({
    activeUser,
    setCustomCoffees,
    setPantryItems,
    setBrewCoffeeId,
    setBrewStep
  });

  useBrewTimer({
    brewRunning,
    brewStep,
    brewMethod,
    waterMl,
    timerSeconds,
    setTimerSeconds,
    setBrewRunning,
    setBrewStep
  });

  const { navigateToTab, handleNavClick: handleNavClickBase } = useAppNavigationDomain({
    searchMode,
    setSearchMode,
    profileUsername,
    setProfileUsername,
    users,
    setActiveTab,
    activeUserUsername: activeUser?.username ?? null,
    coffees,
    setDetailCoffeeId,
    setDetailHostTab,
    setShowCreateCoffeeComposer,
    setSearchSelectedCoffeeId,
    setSearchFocusCoffeeProfile,
    isAuthenticated: Boolean(sessionEmail),
    onRequireAuth: requestLogin
  });

  useRouteCanonicalSync(Boolean(sessionEmail));
  const loginRootPath = useMemo(() => getAppRootPath(window.location.pathname) || "/", []);
  useRouteGuardSync({
    isAuthenticated: Boolean(sessionEmail),
    onBlocked: requestLogin,
    fallbackPath: loginRootPath
  });

  // Si hay error de auth (login fallido, 500, callback con error), llevar a /timeline para no dejar en URL de error
  useEffect(() => {
    if (!authError) return;
    const pathname = window.location.pathname;
    const base = (getAppRootPath(pathname) || "/").replace(/\/+$/, "") || "";
    const timelinePath = (base ? `${base}/timeline` : "/timeline").replace(/\/+/g, "/");
    if (pathname.replace(/\/+$/, "") !== timelinePath.replace(/\/+$/, "")) {
      window.history.replaceState(null, "", `${timelinePath}${window.location.search}${window.location.hash}`);
      setActiveTab("timeline");
    }
  }, [authError]);

  useGlobalUiEvents({
    onOpenSearch: () => {
      navigateToTab("search", { searchMode: "coffees" });
      document.getElementById("quick-search")?.focus();
    },
    onTopbarScroll: setTopbarScrolled
  });

  const lastScrollTopRef = useRef(0);

  const applyScrollState = useCallback((scrollTop: number) => {
    setTopbarScrolled(scrollTop > 18);
    const mainShell = mainScrollRef.current?.parentElement;
    if (mainShell) {
      const y = Math.min(scrollTop, TOPBAR_HIDE_SCROLL_PX);
      mainShell.style.setProperty("--topbar-translate-y", `${y}px`);
    }
  }, []);

  useEffect(() => {
    const el = mainScrollRef.current;
    if (!el) return;
    lastScrollTopRef.current = el.scrollTop;
    applyScrollState(el.scrollTop);

    let scrollEndTimer: ReturnType<typeof setTimeout> | null = null;
    const SCROLL_END_MS = 100;

    const syncFromScroll = () => {
      const st = el.scrollTop;
      lastScrollTopRef.current = st;
      applyScrollState(st);
    };

    const onScroll = () => {
      syncFromScroll();
      if (scrollEndTimer != null) clearTimeout(scrollEndTimer);
      scrollEndTimer = setTimeout(() => {
        scrollEndTimer = null;
        syncFromScroll();
      }, SCROLL_END_MS);
    };

    const onScrollEnd = () => {
      if (scrollEndTimer != null) {
        clearTimeout(scrollEndTimer);
        scrollEndTimer = null;
      }
      syncFromScroll();
    };

    el.addEventListener("scroll", onScroll, { passive: true });
    el.addEventListener("scrollend", onScrollEnd);
    return () => {
      el.removeEventListener("scroll", onScroll);
      el.removeEventListener("scrollend", onScrollEnd);
      if (scrollEndTimer != null) clearTimeout(scrollEndTimer);
    };
  }, [applyScrollState, sessionEmail]);

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
      resetCreateCoffeeDomain();
      setDetailCoffeeId(null);
      setDetailHostTab(null);
      setGlobalStatus("Listo");
    } catch (error) {
      setAuthError((error as Error).message);
    }
  }, [resetCreateCoffeeDomain, setAuthError]);

  const handleDeleteAccount = useCallback(async () => {
    if (!activeUser) return;
    try {
      await requestAccountDeletion(activeUser.id);
      await handleSignOut();
    } catch (error) {
      setAuthError((error as Error).message);
    }
  }, [activeUser, handleSignOut, setAuthError]);

  useEffect(() => {
    if (!sessionEmail || !activeUser) return;
    let cancelled = false;
    void (async () => {
      try {
        const outcome = await syncAccountLifecycleAfterLogin(activeUser.id);
        if (cancelled) return;
        if (outcome === "reactivated") {
          setTimelineActionBanner("Se canceló la eliminación de tu cuenta porque volviste a iniciar sesión.");
        } else if (outcome === "deleted") {
          await handleSignOut();
          if (!cancelled) setGlobalStatus("Tu cuenta se eliminó por haber superado el plazo de 30 días.");
        }
      } catch {
        // Best effort: no bloquea la sesión si el backend aún no tiene estas columnas.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeUser, handleSignOut, sessionEmail, setTimelineActionBanner]);

  const {
    brewCoffeeCatalog,
    usersById,
    coffeesById,
    coffeeSlugIndex,
    timelineCards,
    filteredCoffees,
    searchOriginOptions,
    createCoffeeCountryOptions,
    searchRoastOptions,
    searchSpecialtyOptions,
    searchFormatOptions,
    selectedCreatePostCoffee,
    filteredCreatePostCoffees,
    createPostMentionSuggestions,
    selectedCoffeeForBrew,
    brewPantryItems,
    diaryEntriesActivity,
    pantryCoffeeRows,
    diaryCoffeeOptions,
    profileUser,
    profilePosts,
    favoriteCoffees,
    detailCoffee,
    detailCoffeeReviews,
    detailCoffeeAverageRating,
    detailCurrentUserReview,
    detailCurrentUser,
    detailCurrentUserReviewWithUser,
    detailIsFavorite,
    detailPantryStock,
    detailSensoryAverages,
    followersCount,
    followingCount,
    followingIds,
    followerCounts,
    filteredSearchUsers,
    timelineRecommendations,
    timelineSuggestions,
    timelineSuggestionIndices,
    commentSheetRows,
    activeCommentMenuRow,
    commentMentionSuggestions,
    visibleTimelineNotifications,
    showNotificationsBadge
  } = useAppDerivedData({
    users,
    coffees,
    customCoffees,
    coffeeReviews,
    coffeeSensoryProfiles,
    posts,
    likes,
    comments,
    postCoffeeTags,
    diaryEntries,
    pantryItems,
    favorites,
    follows,
    activeUser,
    profileUsername,
    searchQuery,
    searchSelectedOrigins,
    searchSelectedRoasts,
    searchSelectedSpecialties,
    searchSelectedFormats,
    searchMinRating,
    newPostCoffeeId,
    createPostCoffeeQuery,
    brewCoffeeId,
    newPostText,
    commentDraft,
    commentSheetPostId,
    commentMenuId,
    dismissedNotificationIds,
    notificationsLastSeenAt,
    detailCoffeeId,
    notifications
  });

  useCoffeeRouteSync({
    coffeeSlugToId: coffeeSlugIndex.bySlug,
    setDetailCoffeeId,
    setDetailHostTab
  });

  const selectedCoffee = selectedCoffeeForBrew ?? undefined;
  const coffeeGrams = Math.max(1, Math.round(waterMl / ratio));
  const {
    saveBrewToDiary,
    handleDeleteDiaryEntry,
    handleUpdateDiaryEntry,
    handleUpdatePantryStock,
    handleRemovePantryItem
  } = useDiaryActions({
    activeUser,
    selectedCoffee,
    coffeeGrams,
    brewMethod,
    waterMl,
    setDiaryEntries,
    setPantryItems,
    setBrewRunning,
    setTimerSeconds,
    setBrewStep,
    navigateToDiary: () => navigateToTab("diary")
  });
  const selectedDiaryCoffee = useMemo(
    () => diaryCoffeeOptions.find((coffee) => coffee.id === diaryCoffeeIdDraft) ?? diaryCoffeeOptions[0] ?? null,
    [diaryCoffeeIdDraft, diaryCoffeeOptions]
  );
  const selectedDiaryPantryCoffee = useMemo(
    () =>
      diaryPantryCoffeeIdDraft
        ? diaryCoffeeOptions.find((coffee) => coffee.id === diaryPantryCoffeeIdDraft) ?? null
        : null,
    [diaryCoffeeOptions, diaryPantryCoffeeIdDraft]
  );
  const {
    openWaterSheet,
    openCoffeeSheet,
    openAddPantrySheet,
    setDiaryCoffeeDraftWithCaffeine,
    saveWater,
    saveCoffee,
    savePantry
  } = useDiarySheetActions({
    activeUser,
    diaryCoffeeOptions,
    selectedDiaryCoffee,
    selectedDiaryPantryCoffee,
    diaryWaterMlDraft,
    diaryCoffeeMlDraft,
    diaryCoffeeGramsDraft,
    diaryCoffeeCaffeineDraft,
    diaryCoffeePreparationDraft,
    diaryPantryGramsDraft,
    pantryItems,
    setDiaryEntries,
    setPantryItems,
    setDiaryTab,
    setShowDiaryQuickActions,
    setShowDiaryWaterSheet,
    setShowDiaryCoffeeSheet,
    setShowDiaryAddPantrySheet,
    setPantrySheetStep,
    setDiaryWaterMlDraft,
    setDiaryCoffeeIdDraft,
    setDiaryCoffeeCaffeineDraft,
    setDiaryCoffeeMlDraft,
    setDiaryCoffeePreparationDraft,
    setDiaryPantryCoffeeIdDraft,
    setDiaryPantryGramsDraft,
    setLastCreatedCoffeeNameForSheet
  });

  useCoffeeDetailDraftSync({
    hasDetailCoffee: Boolean(detailCoffee),
    currentUserReview: detailCurrentUserReview,
    pantryStock: detailPantryStock,
    sensoryAverages: detailSensoryAverages,
    setDetailReviewText,
    setDetailReviewRating,
    setDetailReviewImagePreviewUrl,
    setDetailReviewImageFile,
    setDetailStockDraft,
    setDetailSensoryDraft
  });


  const commentListRef = useRef<HTMLUListElement | null>(null);
  const commentImageInputRef = useRef<HTMLInputElement | null>(null);

  const {
    handleToggleLike,
    handleAddComment: handleAddCommentBase,
    handleUpdateComment,
    handleDeleteComment,
    handleToggleFollow,
    handleEditPost,
    handleDeletePost,
    handleRefreshTimeline: handleRefreshTimelineBase,
    handleCreatePost,
    handleMentionNavigation
  } = useTimelineActions({
    activeUser,
    likes,
    follows,
    comments,
    coffees,
    newPostText,
    newPostImageFile,
    newPostCoffeeId,
    commentDraft,
    editingCommentId,
    commentImageFile,
    setComments,
    setLikes,
    setFollows,
    setPosts,
    setPostCoffeeTags,
    setTimelineBusyMessage,
    setTimelineActionBanner,
    setGlobalStatus,
    resetCreatePostComposer,
    setEditingCommentId,
    setCommentDraft,
    setCommentImageFile,
    setCommentImageName,
    setCommentImagePreviewError,
    setCommentImagePreviewUrl,
    setCommentMenuId,
    reloadInitialData,
    navigateToTab,
    setSearchQuery
  });

  const handleAddComment = useCallback(() => {
    if (!commentSheetPostId) return;
    void handleAddCommentBase(commentSheetPostId);
  }, [commentSheetPostId, handleAddCommentBase]);

  const handleRefreshTimeline = useCallback(async () => {
    setTimelineRefreshing(true);
    await handleRefreshTimelineBase();
    setTimelineRefreshing(false);
  }, [handleRefreshTimelineBase]);
  const { handleUpdateProfile } = useProfileActions({
    setUsers,
    setTimelineBusyMessage,
    setTimelineActionBanner,
    setGlobalStatus
  });

  const closeDiarySheets = useCallback(() => {
    setShowDiaryQuickActions(false);
    setShowDiaryPeriodSheet(false);
    setShowDiaryWaterSheet(false);
    setShowDiaryCoffeeSheet(false);
    setShowDiaryAddPantrySheet(false);
  }, []);
  const {
    closeCommentSheet,
    pickCommentImage,
    removeCommentImage,
    removeSelectedCreatePostImage
  } = useTimelineSheetActions({
    commentImagePreviewUrl,
    setCommentSheetPostId,
    setCommentDraft,
    setCommentMenuId,
    setHighlightedCommentId,
    setCommentImageFile,
    setCommentImageName,
    setCommentImagePreviewError,
    setCommentImagePreviewUrl,
    newPostSelectedImageId,
    newPostGalleryItems,
    setNewPostSelectedImageId,
    setNewPostImageFile,
    setNewPostImagePreviewUrl,
    setNewPostGalleryItems
  });

  useAppUiEffects({
    timelineActionBanner,
    setTimelineActionBanner,
    activeTab,
    searchMode,
    searchFocusCoffeeProfile,
    setSearchActiveFilterType,
    showNotificationsPanel,
    showCreatePost,
    showCreatePostCoffeeSheet,
    commentSheetPostId,
    showCreateCoffeeComposer,
    resetCreatePostComposer,
    setShowNotificationsPanel,
    setShowCreatePostCoffeeSheet,
    setCommentSheetPostId,
    setCommentDraft,
    setHighlightedCommentId,
    setShowCreateCoffeeComposer,
    editingCommentId,
    setCommentMenuId,
    setEditingCommentId,
    showAuthPrompt,
    setShowAuthPrompt,
    commentListRef,
    comments,
    commentImagePreviewUrl,
    highlightedCommentId,
    handledTimelineDeepLink,
    setHandledTimelineDeepLink,
    posts,
    navigateToTimelineReplace: () => navigateToTab("timeline", { replace: true }),
    setNotificationsLastSeenAt,
    notificationsLastSeenAt,
    visibleTimelineNotifications,
    dismissNotificationTimersRef,
    closeDiarySheets,
    handleRefreshTimeline
  });

  const { openCoffeeDetail, closeCoffeePanel } = useCoffeeDetailNavigation({
    coffeesById,
    coffeeSlugById: coffeeSlugIndex.byId,
    mode,
    searchMode,
    profileUsername,
    activeTab,
    setDetailCoffeeId,
    setDetailHostTab,
    setActiveTab
  });

  const { saveDetailFavorite, saveDetailReview, removeDetailReview, saveDetailSensory, saveDetailStock } = useCoffeeDetailActions({
    activeUser,
    detailCoffeeId,
    detailIsFavorite,
    detailCurrentUserReviewImageUrl: detailCurrentUserReview?.image_url ?? "",
    detailReviewImageFile,
    detailReviewRating,
    detailReviewText,
    detailSensoryDraft,
    detailStockDraft,
    setFavorites,
    setCoffeeReviews,
    setCoffeeSensoryProfiles,
    setPantryItems,
    setDetailStockDraft
  });
  const { sidePanel: sidePanelDetailActions, fullPage: fullPageDetailActions } = useCoffeeDetailInteractions({
    isAuthenticated: Boolean(sessionEmail),
    requestLogin,
    runWithAuth,
    saveDetailFavorite,
    saveDetailReview,
    removeDetailReview,
    saveDetailSensory,
    saveDetailStock,
    navigateToProfile: (userId) => navigateToTab("profile", { profileUserId: userId })
  });

  const detailPanel = detailCoffee ? (
    <LazyCoffeeDetailView
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
      onToggleFavorite={sidePanelDetailActions.onToggleFavorite}
      onReviewTextChange={setDetailReviewText}
      onReviewRatingChange={setDetailReviewRating}
      onReviewImagePick={(file: File | null, previewUrl: string) => {
        setDetailReviewImageFile(file);
        setDetailReviewImagePreviewUrl(previewUrl);
      }}
      onSaveReview={sidePanelDetailActions.onSaveReview}
      onDeleteReview={sidePanelDetailActions.onDeleteReview}
      canDeleteReview={Boolean(detailCurrentUserReview)}
      onSensoryDraftChange={setDetailSensoryDraft}
      onSaveSensory={sidePanelDetailActions.onSaveSensory}
      onStockDraftChange={setDetailStockDraft}
      onSaveStock={sidePanelDetailActions.onSaveStock}
      onOpenUserProfile={sidePanelDetailActions.onOpenUserProfile}
      isGuest={!sessionEmail}
      onRequireAuth={requestLogin}
      fullPage={false}
      externalOpenStockSignal={0}
    />
  ) : null;

  const createCoffeePanel = (
    <LazyCreateCoffeeView
      draft={createCoffeeDraft}
      imagePreviewUrl={createCoffeeImagePreviewUrl}
      saving={createCoffeeSaving}
      error={createCoffeeError}
      countryOptions={searchOriginOptions}
      specialtyOptions={searchSpecialtyOptions}
      onChange={setCreateCoffeeDraft}
      onPickImage={onPickCreateCoffeeImage}
      onRemoveImage={onRemoveCreateCoffeeImage}
      onClose={closeCreateCoffeeComposer}
      onSave={() => void saveCreateCoffee({ fromBrewChooser: true })}
      fullPage={mode !== "desktop"}
    />
  );

  const createCoffeeFormForSheet = (
    <LazyCreateCoffeeView
      draft={createCoffeeDraft}
      imagePreviewUrl={createCoffeeImagePreviewUrl}
      saving={createCoffeeSaving}
      error={createCoffeeError}
      countryOptions={searchOriginOptions}
      specialtyOptions={searchSpecialtyOptions}
      onChange={setCreateCoffeeDraft}
      onPickImage={onPickCreateCoffeeImage}
      onRemoveImage={onRemoveCreateCoffeeImage}
      onClose={() => setCoffeeSheetStep("select")}
      onSave={async () => {
        const result = await saveCreateCoffee({ fromDiarySheet: true });
        if (result) {
          setDiaryCoffeeDraftWithCaffeine(result.id);
          setLastCreatedCoffeeNameForSheet(result.name);
          setCoffeeSheetStep("dose");
        }
      }}
      fullPage={false}
      hideActions={true}
      hideHead={true}
    />
  );

  const isCreateCoffeeFormValid =
    createCoffeeDraft.name.trim() !== "" &&
    createCoffeeDraft.brand.trim() !== "" &&
    createCoffeeDraft.specialty.trim() !== "" &&
    createCoffeeDraft.country.trim() !== "" &&
    createCoffeeDraft.format.trim() !== "";

  const createCoffeeFormForPantrySheet = (
    <LazyCreateCoffeeView
      draft={createCoffeeDraft}
      imagePreviewUrl={createCoffeeImagePreviewUrl}
      saving={createCoffeeSaving}
      error={createCoffeeError}
      countryOptions={createCoffeeCountryOptions}
      specialtyOptions={searchSpecialtyOptions}
      onChange={setCreateCoffeeDraft}
      onPickImage={onPickCreateCoffeeImage}
      onRemoveImage={onRemoveCreateCoffeeImage}
      onClose={() => setPantrySheetStep("select")}
      onSave={async () => {
        await handleCreateCoffeeNextForPantry();
      }}
      fullPage={false}
      hideActions={true}
      hideHead={true}
      showQuantityField={true}
    />
  );

  const handleCreateCoffeeNext = useCallback(async () => {
    const result = await saveCreateCoffee({ fromDiarySheet: true });
    if (result) {
      setDiaryCoffeeDraftWithCaffeine(result.id);
      setLastCreatedCoffeeNameForSheet(result.name);
      setCoffeeSheetStep("dose");
    }
  }, [saveCreateCoffee, setDiaryCoffeeDraftWithCaffeine, setCoffeeSheetStep]);

  const handleCreateCoffeeNextForPantry = useCallback(async () => {
    const result = await saveCreateCoffee({ fromPantrySheet: true });
    if (result) {
      setDiaryPantryCoffeeIdDraft(result.id);
      const suggestedGrams = Math.max(1, Math.round(Number(createCoffeeDraft.totalGrams || 250)));
      setDiaryPantryGramsDraft(String(suggestedGrams));
      setPantrySheetStep("form");
    }
  }, [createCoffeeDraft.totalGrams, saveCreateCoffee, setDiaryPantryCoffeeIdDraft, setDiaryPantryGramsDraft, setPantrySheetStep]);

  useCoffeeSeoMeta(detailCoffee, {
    avgRating: detailCoffeeAverageRating,
    reviewCount: detailCoffeeReviews.length
  });

  const handleNavClick = (tabId: TabId) => {
    handleNavClickBase(tabId);
  };

  const guardedActiveTab = resolveGuardedTab(activeTab, Boolean(sessionEmail));

  // GA4: enviar page_view en cada cambio de ruta (SPA)
  const coffeeSlugForGa =
    guardedActiveTab === "coffee" && detailCoffeeId ? coffeeSlugIndex.byId.get(detailCoffeeId) ?? null : null;
  const gaPagePath = buildRoute(guardedActiveTab, searchMode, profileUsername, coffeeSlugForGa);
  useEffect(() => {
    const titles: Partial<Record<TabId, string>> = {
      timeline: "Inicio",
      search: "Explorar",
      brewlab: "Elabora",
      diary: "Diario",
      profile: "Perfil",
      coffee: detailCoffee?.nombre ? `Café · ${detailCoffee.nombre}` : "Café"
    };
    const pageTitle = titles[guardedActiveTab] ?? "Cafesito";
    sendPageView(gaPagePath, `Cafesito - ${pageTitle}`);
  }, [gaPagePath, guardedActiveTab]);

  // Al cambiar de vista: reiniciar scroll y mostrar topbar para que en timeline, perfil y resto se comporte igual
  useEffect(() => {
    const el = mainScrollRef.current;
    if (!el) return;
    el.scrollTop = 0;
    lastScrollTopRef.current = 0;
    setTopbarScrolled(false);
    const mainShell = el.parentElement;
    if (mainShell) mainShell.style.setProperty("--topbar-translate-y", "0px");
  }, [guardedActiveTab]);

  const guestCanAccessCurrentTab = canAccessTabAsGuest(guardedActiveTab);
  const showingLogin = (!authReady && !guestCanAccessCurrentTab) || (!sessionEmail && !guestCanAccessCurrentTab);
  useLayoutEffect(() => {
    if (isNotFoundRoute) return;
    if (!showingLogin) return;
    const pathname = window.location.pathname;
    const root = (getAppRootPath(pathname) || "/").replace(/\/+$/, "") || "/";
    const current = pathname.replace(/\/+$/, "") || "/";
    if (current !== root) {
      window.history.replaceState({}, "", `${root}${window.location.search}${window.location.hash}`);
    }
  }, [isNotFoundRoute, showingLogin]);
  const mentionUsersByUsername = useMemo(() => {
    const map = new Map<string, UserRow>();
    users.forEach((user) => {
      const key = String(user.username || "").trim().toLowerCase();
      if (!key) return;
      if (!map.has(key)) map.set(key, user);
    });
    return map;
  }, [users]);
  const resolveMentionUser = useCallback((username: string) => {
    const key = String(username || "").trim().toLowerCase();
    const user = mentionUsersByUsername.get(key);
    if (!user) return null;
    return { username: user.username, avatarUrl: user.avatar_url };
  }, [mentionUsersByUsername]);
  const nav = <BottomNav activeTab={guardedActiveTab} onNavClick={handleNavClick} avatarUrl={activeUser?.avatar_url ?? null} />;
  const navRail = <DesktopNavRail activeTab={guardedActiveTab} onNavClick={handleNavClick} avatarUrl={activeUser?.avatar_url ?? null} />;
  const topbarActions = useTopBarActions({
    searchMode,
    resetSearchUi,
    navigateToTab,
    setShowBarcodeScannerSheet,
    isMobileOsDevice,
    setShowNotificationsPanel,
    visibleTimelineNotifications,
    setNotificationsLastSeenAt,
    setShowDiaryQuickActions,
    setShowDiaryPeriodSheet,
    showCreateCoffeeComposer,
    closeCreateCoffeeComposer,
    brewStep,
    setBrewStep,
    setTimerSeconds,
    setBrewRunning,
    runWithAuth,
    setDetailOpenStockSignal,
    saveDetailFavorite,
    activeUserId: activeUser?.id ?? null
  });

  if (isNotFoundRoute) {
    const openRandomCoffee = () => {
      if (!coffees.length) {
        window.location.replace(getAppRootPath(window.location.pathname) || "/");
        return;
      }
      const randomCoffee = coffees[Math.floor(Math.random() * coffees.length)];
      const slug = coffeeSlugIndex.byId.get(randomCoffee.id) ?? null;
      const nextPath = buildRoute("coffee", "coffees", null, slug);
      window.location.assign(nextPath);
    };
    return (
      <div className={`layout ${mode}`.trim()}>
        {mode === "desktop" ? navRail : null}
        <main className="main-shell">
          <Suspense fallback={<div className="app-content-loading" aria-hidden="true" />}>
            <LazyNotFoundView
              onGoHome={() => window.location.replace(getAppRootPath(window.location.pathname) || "/")}
              onOpenRandomCoffee={openRandomCoffee}
              randomCoffeeEnabled={coffees.length > 0}
            />
          </Suspense>
        </main>
        {mode === "mobile" ? <footer className="bottom-tabs">{nav}</footer> : null}
        <AuthPromptOverlay
          open={showAuthPrompt}
          authBusy={authBusy}
          authError={authError}
          onClose={() => setShowAuthPrompt(false)}
          onGoogleLogin={() => {
            void handleGoogleLogin();
          }}
        />
      </div>
    );
  }

  if (!authReady && !guestCanAccessCurrentTab) {
    return <LoginGate loading message="Verificando sesion..." />;
  }

  if (!sessionEmail && !guestCanAccessCurrentTab) {
    return (
      <LoginGate
        loading={authBusy}
        message={authBusy ? "Redirigiendo a Google..." : undefined}
        errorMessage={authError}
        onGoogleLogin={handleGoogleLogin}
      />
    );
  }

  const useRightRailDetail = shouldUseRightRailDetail({
    mode,
    viewportWidth,
    hasDetailPanel: Boolean(detailPanel),
    activeTab: guardedActiveTab,
    detailHostTab
  });
  const activeSidePanelTarget = sidePanelForTab({
    mode,
    activeTab: guardedActiveTab,
    detailHostTab,
    useRightRailDetail
  });
  const isDesktopComposer = mode === "desktop";
  const timelineContent =
    guardedActiveTab === "timeline" ? (
      <LazyTimelineView
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
        resolveMentionUser={resolveMentionUser}
        onOpenUserProfile={(userId) => {
          navigateToTab("profile", { profileUserId: userId });
        }}
        onOpenCoffee={(coffeeId) => {
          openCoffeeDetail(coffeeId, "timeline");
        }}
        sidePanel={activeSidePanelTarget === "timeline" ? detailPanel : null}
      />
    ) : null;
  const searchContent =
    guardedActiveTab === "search" ? (
      <LazySearchView
        mode={searchMode}
        searchQuery={searchQuery}
        onSearchQueryChange={onSearchQueryChange}
        searchFilter={searchFilter}
        onSearchFilterChange={onSearchFilterChange}
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
        onToggleOrigin={onToggleOrigin}
        onToggleRoast={onToggleRoast}
        onToggleSpecialty={onToggleSpecialty}
        onToggleFormat={onToggleFormat}
        onSetMinRating={setSearchMinRating}
        onClearCoffeeFilters={onClearCoffeeFilters}
        coffees={filteredCoffees}
        users={filteredSearchUsers}
        followingIds={followingIds}
        selectedCoffee={coffees.find((item) => item.id === searchSelectedCoffeeId) ?? null}
        onSelectCoffee={(coffeeId) => {
          onSelectCoffee(coffeeId);
          openCoffeeDetail(coffeeId, "search");
        }}
        onSelectUser={(userId) => {
          navigateToTab("profile", { profileUserId: userId });
        }}
        onToggleFollow={handleToggleFollow}
        focusCoffeeProfile={searchFocusCoffeeProfile}
        onExitCoffeeFocus={() => setSearchFocusCoffeeProfile(false)}
        sidePanel={activeSidePanelTarget === "search" ? detailPanel : null}
      />
    ) : null;
  const coffeeContent =
    guardedActiveTab === "coffee" ? (
      detailCoffee ? (
        <LazyCoffeeDetailView
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
          onToggleFavorite={fullPageDetailActions.onToggleFavorite}
          onReviewTextChange={setDetailReviewText}
          onReviewRatingChange={setDetailReviewRating}
          onReviewImagePick={(file, previewUrl) => {
            setDetailReviewImageFile(file);
            setDetailReviewImagePreviewUrl(previewUrl);
          }}
          onSaveReview={fullPageDetailActions.onSaveReview}
          onDeleteReview={fullPageDetailActions.onDeleteReview}
          canDeleteReview={Boolean(detailCurrentUserReview)}
          onSensoryDraftChange={setDetailSensoryDraft}
          onSaveSensory={fullPageDetailActions.onSaveSensory}
          onStockDraftChange={setDetailStockDraft}
          onSaveStock={fullPageDetailActions.onSaveStock}
          onOpenUserProfile={fullPageDetailActions.onOpenUserProfile}
          isGuest={!sessionEmail}
          onRequireAuth={requestLogin}
          fullPage
          externalOpenStockSignal={detailOpenStockSignal}
        />
      ) : (
        <article className="coffee-detail-empty coffee-detail-empty-full">
          <h2 className="title">Café no encontrado</h2>
          <p className="coffee-sub">La URL no corresponde a ningún café disponible.</p>
          <Button
            variant="primary"
            onClick={() => {
              navigateToTab("search", { searchMode: "coffees", replace: true });
            }}
          >
            Volver a Explorar
          </Button>
        </article>
      )
    ) : null;
  const brewContent =
    guardedActiveTab === "brewlab" ? (
      showCreateCoffeeComposer && mode !== "desktop" ? (
        <section className="create-coffee-mobile-screen">{createCoffeePanel}</section>
      ) : (
        <LazyBrewLabView
          brewStep={brewStep}
          setBrewStep={setBrewStep}
          brewMethod={brewMethod}
          setBrewMethod={setBrewMethod}
          brewCoffeeId={brewCoffeeId}
          setBrewCoffeeId={setBrewCoffeeId}
          coffees={brewCoffeeCatalog}
          pantryItems={brewPantryItems}
          onAddNotFoundCoffee={openAddPantrySheet}
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
          showBarcodeButton={isMobileOsDevice}
          onBarcodeClick={() => {
            setBarcodeOrigin("brew");
            setShowBarcodeScannerSheet(true);
          }}
          barcodeDetectedValue={barcodeDetectedValueForBrew}
          onClearBarcodeDetectedValue={() => setBarcodeDetectedValueForBrew(null)}
        />
      )
    ) : null;
  const diaryContent =
    guardedActiveTab === "diary" ? (
      <LazyDiaryView
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
        onOpenQuickActions={() => setShowDiaryQuickActions(true)}
      />
    ) : null;
  const profileContent =
    guardedActiveTab === "profile" && profileUser ? (
      <LazyProfileView
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
          const result = await toggleFavoriteCoffee(activeUser.id, coffeeId, exists);
          setFavorites((prev) => {
            if (exists) {
              return prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === coffeeId));
            }
            if (!result) return prev;
            return [
              result,
              ...prev.filter((item) => !(item.user_id === result.user_id && item.coffee_id === result.coffee_id))
            ];
          });
        }}
        onEditPost={handleEditPost}
        onDeletePost={handleDeletePost}
        onToggleLike={handleToggleLike}
        onOpenComments={(postId) => {
          setHighlightedCommentId(null);
          setCommentSheetPostId(postId);
        }}
        resolveMentionUser={resolveMentionUser}
        externalEditProfileSignal={profileEditSignal}
        sidePanel={activeSidePanelTarget === "profile" ? detailPanel : null}
      />
    ) : null;
  const authPromptOverlay = (
    <AuthPromptOverlay
      open={showAuthPrompt}
      authBusy={authBusy}
      authError={authError}
      onClose={() => setShowAuthPrompt(false)}
      onGoogleLogin={() => {
        void handleGoogleLogin();
      }}
    />
  );
  const barcodeScannerOverlay = (
    <MobileBarcodeScannerSheet
      open={showBarcodeScannerSheet}
      onClose={() => {
        setShowBarcodeScannerSheet(false);
        setBarcodeOrigin(null);
      }}
      onDetected={(value) => {
        setShowBarcodeScannerSheet(false);
        if (barcodeOrigin === "diary") {
          setBarcodeDetectedValueForDiary(value);
        } else if (barcodeOrigin === "brew") {
          setBarcodeDetectedValueForBrew(value);
        } else if (barcodeOrigin === "createPostCoffee") {
          setCreatePostCoffeeQuery(value);
        } else {
          onSearchQueryChange(value);
        }
        setBarcodeOrigin(null);
      }}
    />
  );
  const commentOverlay = (
    <CommentSheet
      open={Boolean(commentSheetPostId)}
      rows={commentSheetRows}
      usersById={usersById}
      activeUserId={activeUser?.id ?? null}
      highlightedCommentId={highlightedCommentId}
      onClose={closeCommentSheet}
      onOpenMenu={(id) => setCommentMenuId(id)}
      activeMenuRow={activeCommentMenuRow}
      onCloseMenu={() => setCommentMenuId(null)}
      onMenuEdit={(row) => {
        setEditingCommentId(row.id);
        setCommentDraft(row.text);
        setCommentMenuId(null);
      }}
      onMenuDelete={(row) => {
        const confirmed = window.confirm("Borrar comentario?");
        if (!confirmed) return;
        setCommentMenuId(null);
        void handleDeleteComment(row.id);
      }}
      editingCommentId={editingCommentId}
      onCancelEdit={() => {
        setEditingCommentId(null);
        setCommentDraft("");
      }}
      commentDraft={commentDraft}
      setCommentDraft={setCommentDraft}
      emojis={COMMENT_EMOJIS}
      showEmojiPanel={showCommentEmojiPanel}
      setShowEmojiPanel={setShowCommentEmojiPanel}
      mentionSuggestions={commentMentionSuggestions}
      onMentionNavigate={handleMentionNavigation}
      resolveMentionUser={resolveMentionUser}
      commentImageInputRef={commentImageInputRef}
      commentListRef={commentListRef}
      onPickImage={pickCommentImage}
      onAddComment={handleAddComment}
      onUpdateComment={handleUpdateComment}
      commentImagePreviewUrl={commentImagePreviewUrl}
      commentImagePreviewError={commentImagePreviewError}
      setCommentImagePreviewError={setCommentImagePreviewError}
      commentImageName={commentImageName}
      onRemoveImage={removeCommentImage}
    />
  );
  const createPostOverlay = (
    <CreatePostSheet
      open={showCreatePost}
      onClose={resetCreatePostComposer}
      imageFile={newPostImageFile}
      text={newPostText}
      setText={setNewPostText}
      onPublish={handleCreatePost}
      imageInputRef={newPostImageInputRef}
      onAppendFiles={appendNewPostFiles}
      imagePreviewUrl={newPostImagePreviewUrl}
      onRemoveSelectedImage={removeSelectedCreatePostImage}
      activeUser={activeUser}
      showEmojiPanel={showCreatePostEmojiPanel}
      setShowEmojiPanel={setShowCreatePostEmojiPanel}
      mentionSuggestions={createPostMentionSuggestions}
      resolveMentionUser={resolveMentionUser}
      onOpenCoffeePicker={() => setShowCreatePostCoffeeSheet(true)}
      selectedCoffee={selectedCreatePostCoffee}
      showCoffeeSheet={showCreatePostCoffeeSheet}
      onCloseCoffeeSheet={() => setShowCreatePostCoffeeSheet(false)}
      coffeeQuery={createPostCoffeeQuery}
      setCoffeeQuery={setCreatePostCoffeeQuery}
      filteredCoffees={filteredCreatePostCoffees}
      selectedCoffeeId={newPostCoffeeId}
      showBarcodeButton={isMobileOsDevice}
      onBarcodeClick={() => {
        setBarcodeOrigin("createPostCoffee");
        setShowBarcodeScannerSheet(true);
      }}
      onSelectCoffee={(coffeeId) => {
        setNewPostCoffeeId(coffeeId);
        setShowCreatePostCoffeeSheet(false);
      }}
    />
  );
  const notificationsOverlay = (
    <NotificationsSheet
      open={showNotificationsPanel}
      notifications={visibleTimelineNotifications}
      usersById={usersById}
      notificationsLastSeenAt={notificationsLastSeenAt}
      followingIds={followingIds}
      dismissingNotificationIds={dismissingNotificationIds}
      onClose={closeNotificationsPanel}
      onDismiss={(id) => {
        dismissNotification(id, async (dismissedId) => {
          try {
            const { deleteNotification: apiDelete } = await import("../data/supabaseApi");
            await apiDelete(Number(dismissedId));
            setNotifications((prev) => prev.filter((n) => String(n.id) !== dismissedId));
          } catch (err) {
            console.error("Failed to delete notification:", err);
          }
        });
      }}
      onToggleFollow={handleToggleFollow}
      onOpenCommentThread={(postId, commentId) => {
        setCommentSheetPostId(postId);
        setHighlightedCommentId(commentId);
        closeNotificationsPanel();
      }}
      onOpenUserProfile={(userId) => {
        navigateToTab("profile", { profileUserId: userId });
        closeNotificationsPanel();
      }}
    />
  );
  const diarySheetsOverlay = (
    <DiarySheets
      isActive={guardedActiveTab === "diary" || guardedActiveTab === "brewlab"}
      showQuickActions={showDiaryQuickActions}
      showPeriodSheet={showDiaryPeriodSheet}
      showWaterSheet={showDiaryWaterSheet}
      showCoffeeSheet={showDiaryCoffeeSheet}
      showAddPantrySheet={showDiaryAddPantrySheet}
      onCloseQuickActions={() => setShowDiaryQuickActions(false)}
      onClosePeriodSheet={() => setShowDiaryPeriodSheet(false)}
      onCloseWaterSheet={() => setShowDiaryWaterSheet(false)}
      onCloseCoffeeSheet={() => {
        setLastCreatedCoffeeNameForSheet(null);
        setShowDiaryCoffeeSheet(false);
      }}
      onCloseAddPantrySheet={() => {
        setPantrySheetStep("select");
        setShowDiaryAddPantrySheet(false);
      }}
      onOpenWaterSheet={openWaterSheet}
      onOpenCoffeeSheet={() => {
        setCoffeeSheetStep("select");
        openCoffeeSheet();
      }}
      onOpenAddPantrySheet={openAddPantrySheet}
      diaryPeriod={diaryPeriod}
      setDiaryPeriod={setDiaryPeriod}
      diaryWaterMlDraft={diaryWaterMlDraft}
      setDiaryWaterMlDraft={setDiaryWaterMlDraft}
      onSaveWater={saveWater}
      pantryCoffeeRows={pantryCoffeeRows}
      activeUserId={activeUser?.id ?? null}
      diaryCoffeeOptions={diaryCoffeeOptions}
      coffeeSheetStep={coffeeSheetStep}
      setCoffeeSheetStep={setCoffeeSheetStep}
      diaryCoffeeIdDraft={diaryCoffeeIdDraft}
      setDiaryCoffeeIdDraft={setDiaryCoffeeDraftWithCaffeine}
      diaryCoffeeGramsDraft={diaryCoffeeGramsDraft}
      setDiaryCoffeeGramsDraft={setDiaryCoffeeGramsDraft}
      createCoffeeFormContent={createCoffeeFormForSheet}
      onCreateCoffeeNext={handleCreateCoffeeNext}
      createCoffeeFormForPantrySheet={createCoffeeFormForPantrySheet}
      onCreateCoffeeNextForPantry={handleCreateCoffeeNextForPantry}
      isCreateCoffeeFormValid={isCreateCoffeeFormValid}
      diaryCoffeePreparationDraft={diaryCoffeePreparationDraft}
      setDiaryCoffeePreparationDraft={setDiaryCoffeePreparationDraft}
      diaryCoffeeMlDraft={diaryCoffeeMlDraft}
      setDiaryCoffeeMlDraft={setDiaryCoffeeMlDraft}
      diaryCoffeeCaffeineDraft={diaryCoffeeCaffeineDraft}
      setDiaryCoffeeCaffeineDraft={setDiaryCoffeeCaffeineDraft}
      onSaveCoffee={saveCoffee}
      lastCreatedCoffeeNameForSheet={lastCreatedCoffeeNameForSheet}
      pantrySheetStep={pantrySheetStep}
      setPantrySheetStep={setPantrySheetStep}
      diaryPantryCoffeeIdDraft={diaryPantryCoffeeIdDraft}
      setDiaryPantryCoffeeIdDraft={setDiaryPantryCoffeeIdDraft}
      diaryPantryGramsDraft={diaryPantryGramsDraft}
      setDiaryPantryGramsDraft={setDiaryPantryGramsDraft}
      onSavePantry={savePantry}
      selectedDiaryPantryCoffee={selectedDiaryPantryCoffee}
      showBarcodeButton={isMobileOsDevice}
      onBarcodeClick={() => {
        setBarcodeOrigin("diary");
        setShowBarcodeScannerSheet(true);
      }}
      barcodeDetectedValue={barcodeDetectedValueForDiary}
      onClearBarcodeDetectedValue={() => setBarcodeDetectedValueForDiary(null)}
    />
  );
  return (
    <div className={`layout ${mode}`.trim()}>
      {mode === "desktop" ? navRail : null}
      <main className="main-shell">
        <TopBar
          activeTab={guardedActiveTab}
          searchQuery={searchQuery}
          searchMode={searchMode}
          onSearchQueryChange={onSearchQueryChange}
          onSearchCancel={resetSearchUi}
          onSearchBarcodeClick={() => {
            setBarcodeOrigin("search");
            topbarActions.onSearchBarcodeClick();
          }}
          showSearchBarcodeButton={isMobileOsDevice && searchMode === "coffees"}
          showSearchCoffeeFilterChips={searchMode === "coffees" && !searchFocusCoffeeProfile}
          searchOriginCount={searchSelectedOrigins.size}
          searchSpecialtyCount={searchSelectedSpecialties.size}
          searchRoastCount={searchSelectedRoasts.size}
          searchFormatCount={searchSelectedFormats.size}
          searchHasRatingFilter={searchMinRating > 0}
          onOpenSearchFilter={(filter) => setSearchActiveFilterType(filter)}
          onSearchBack={topbarActions.onSearchBack}
          showNotificationsBadge={showNotificationsBadge}
          onTimelineSearchUsers={topbarActions.onTimelineSearchUsers}
          onTimelineNotifications={topbarActions.onTimelineNotifications}
          diaryPeriod={diaryPeriod}
          onDiaryOpenQuickActions={topbarActions.onDiaryOpenQuickActions}
          onDiaryOpenPeriodSelector={topbarActions.onDiaryOpenPeriodSelector}
          scrolled={topbarScrolled}
          hidden={false}
          brewStep={brewStep}
          brewStepTitle={getBrewStepTitle(brewStep)}
          onBrewBack={topbarActions.onBrewBack}
          onBrewForward={topbarActions.onBrewForward}
          brewCreateCoffeeOpen={showCreateCoffeeComposer}
          onBrewCreateCoffeeBack={closeCreateCoffeeComposer}
          onProfileSignOut={handleSignOut}
          onProfileDeleteAccount={handleDeleteAccount}
          profileMenuEnabled={Boolean(profileUser && activeUser && profileUser.id === activeUser.id)}
          onProfileOpenEdit={triggerProfileEdit}
          onCoffeeBack={() => {
            void runWithAuth(async () => {
              if (window.history.length > 1) {
                window.history.back();
                return;
              }
              navigateToTab("timeline", { replace: true });
            });
          }}
          coffeeTopbarFavoriteActive={guardedActiveTab === "coffee" ? detailIsFavorite : false}
          coffeeTopbarStockActive={guardedActiveTab === "coffee" ? Boolean(detailPantryStock) : false}
          onCoffeeTopbarToggleFavorite={topbarActions.onCoffeeTopbarToggleFavorite}
          onCoffeeTopbarOpenStock={topbarActions.onCoffeeTopbarOpenStock}
        />
        <div ref={mainScrollRef} className={`main-shell-scroll ${guardedActiveTab === "coffee" ? "is-coffee" : ""}`.trim()}>
          <Suspense fallback={<div className="app-content-loading" aria-hidden="true" />}>
            <AppContentRouter
              activeTab={guardedActiveTab}
              mode={mode}
              timelineContent={timelineContent}
              searchContent={searchContent}
              coffeeContent={coffeeContent}
              brewContent={brewContent}
              diaryContent={diaryContent}
              profileContent={profileContent}
              onOpenCreatePost={openCreatePostComposer}
            />
          </Suspense>
        </div>
      </main>

      {mode === "desktop" ? (
        <aside
          className={useRightRailDetail || (guardedActiveTab === "brewlab" && showCreateCoffeeComposer) ? "detail-rail-fixed" : "fab-rail"}
          aria-label={useRightRailDetail ? "Detalle cafe" : guardedActiveTab === "brewlab" && showCreateCoffeeComposer ? "Crear cafe" : "Acciones"}
        >
          {useRightRailDetail ? (
            <div className="desktop-detail-wrap">{detailPanel}</div>
          ) : guardedActiveTab === "brewlab" && showCreateCoffeeComposer ? (
            <div className="desktop-detail-wrap">{createCoffeePanel}</div>
          ) : guardedActiveTab === "timeline" ? (
            <IconButton className="fab fab-desktop" aria-label="Nuevo Post" onClick={openCreatePostComposer}>
              <UiIcon name="add" className="ui-icon" />
            </IconButton>
          ) : null}
        </aside>
      ) : null}

      {mode === "mobile" && !(guardedActiveTab === "brewlab" && showCreateCoffeeComposer) ? <footer className="bottom-tabs">{nav}</footer> : null}

      <AppOverlayLayers
        authPrompt={authPromptOverlay}
        barcodeScanner={barcodeScannerOverlay}
        commentSheet={commentOverlay}
        createPostSheet={createPostOverlay}
        notificationsSheet={notificationsOverlay}
        diarySheets={diarySheetsOverlay}
      />
    </div>
  );
}












