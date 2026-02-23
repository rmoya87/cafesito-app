import { Fragment, useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  addPostCoffeeTag,
  createPost,
  createComment,
  deleteComment,
  deletePost,
  fetchInitialData,
  fetchUserData,
  uploadImageFile,
  toggleFollow,
  toggleLike,
  updateComment,
  updatePost
} from "./data/supabaseApi";
import { getSupabaseClient, supabaseConfigError } from "./supabase";
import type {
  BrewStep,
  CoffeeRow,
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

type IconName =
  | "home"
  | "search"
  | "science"
  | "book"
  | "person"
  | "notifications"
  | "settings"
  | "add"
  | "arrow-left"
  | "arrow-right"
  | "more"
  | "coffee"
  | "camera"
  | "chat"
  | "at"
  | "smile"
  | "chevron-right"
  | "send"
  | "edit"
  | "trash";

const NAV_ITEMS: Array<{ id: TabId; label: string; icon: IconName }> = [
  { id: "timeline", label: "Inicio", icon: "home" },
  { id: "search", label: "Explorar", icon: "search" },
  { id: "brewlab", label: "Elabora", icon: "science" },
  { id: "diary", label: "Diario", icon: "book" },
  { id: "profile", label: "Perfil", icon: "person" }
];

const BREW_METHODS = ["V60", "AeroPress", "French Press", "Chemex", "Espresso"];
const COMMENT_EMOJIS = ["😀", "😍", "🤎", "☕", "🔥", "🙌", "👏", "😋", "🥳", "😎"];

function toRelativeMinutes(timestamp: number): string {
  const diffMs = Math.max(0, Date.now() - timestamp);
  const mins = Math.max(1, Math.floor(diffMs / 60000));
  if (mins < 60) return `${mins} min`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs} h`;
  const days = Math.floor(hrs / 24);
  return `${days} d`;
}

function withinDays(timestamp: number, days: number): boolean {
  const ms = days * 24 * 60 * 60 * 1000;
  return Date.now() - timestamp <= ms;
}

function UiIcon({ name, className }: { name: IconName; className?: string }) {
  if (name === "home") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 11.5L12 5l8 6.5V20h-5.5v-5h-5v5H4z" />
      </svg>
    );
  }
  if (name === "search") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="11" cy="11" r="6.5" />
        <path d="M16 16l4 4" />
      </svg>
    );
  }
  if (name === "science") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M9 3h6M10 3v4l-5.2 8.8A3 3 0 007.4 20h9.2a3 3 0 002.6-4.2L14 7V3" />
        <path d="M9 14h6" />
      </svg>
    );
  }
  if (name === "book") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M5 4h6.5A2.5 2.5 0 0114 6.5V20H7a2 2 0 01-2-2z" />
        <path d="M19 4h-6.5A2.5 2.5 0 0010 6.5V20h7a2 2 0 002-2z" />
      </svg>
    );
  }
  if (name === "person") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="12" cy="8" r="3.5" />
        <path d="M5 20c0-3.8 3.1-6 7-6s7 2.2 7 6" />
      </svg>
    );
  }
  if (name === "notifications") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 4a5 5 0 00-5 5v3.5L5 15v1h14v-1l-2-2.5V9a5 5 0 00-5-5z" />
        <path d="M10 18.5a2 2 0 004 0" />
      </svg>
    );
  }
  if (name === "settings") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="12" cy="12" r="3.2" />
        <path d="M12 4.2v2.2M12 17.6v2.2M4.2 12h2.2M17.6 12h2.2M6.4 6.4l1.6 1.6M16 16l1.6 1.6M17.6 6.4L16 8M8 16l-1.6 1.6" />
      </svg>
    );
  }
  if (name === "add") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 5v14M5 12h14" />
      </svg>
    );
  }
  if (name === "arrow-left") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M14.5 5.5L8 12l6.5 6.5M8 12h11" />
      </svg>
    );
  }
  if (name === "arrow-right") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M9.5 5.5L16 12l-6.5 6.5M16 12H5" />
      </svg>
    );
  }
  if (name === "more") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="6" cy="12" r="1.8" />
        <circle cx="12" cy="12" r="1.8" />
        <circle cx="18" cy="12" r="1.8" />
      </svg>
    );
  }
  if (name === "coffee") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M3.5 7.5h12v5.2A4.3 4.3 0 0111.2 17H7.8a4.3 4.3 0 01-4.3-4.3z" />
        <path d="M15.4 8.8h2a2.8 2.8 0 010 5.6h-2" />
        <path d="M6.2 19.4h7.6" />
      </svg>
    );
  }
  if (name === "camera") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4.5 7.5h4l1.3-2h4.4l1.3 2h4a1.8 1.8 0 011.8 1.8v8.7a1.8 1.8 0 01-1.8 1.8h-15A1.8 1.8 0 012.7 18V9.3A1.8 1.8 0 014.5 7.5z" />
        <circle cx="12" cy="13" r="3.2" />
      </svg>
    );
  }
  if (name === "chat") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4.5 6.2h15v8.8H9.2l-4.7 3.2z" />
      </svg>
    );
  }
  if (name === "at") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M16.5 12a4.5 4.5 0 10-1.1 2.9c.5.6 1.6.8 2.5.3 1.1-.6 1.6-1.8 1.6-3.2A7.5 7.5 0 1012 19.5" />
      </svg>
    );
  }
  if (name === "smile") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="12" cy="12" r="8.5" />
        <circle cx="9" cy="10" r="1" />
        <circle cx="15" cy="10" r="1" />
        <path d="M8.5 14c.8 1.4 2 2 3.5 2s2.7-.6 3.5-2" />
      </svg>
    );
  }
  if (name === "chevron-right") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M9.5 5.5L16 12l-6.5 6.5" />
      </svg>
    );
  }
  if (name === "send") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 11.5L20 4l-4.8 16-3.4-5.2L4 11.5z" />
        <path d="M20 4L11.8 14.8" />
      </svg>
    );
  }
  if (name === "edit") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 16.8V20h3.2L18 9.2l-3.2-3.2L4 16.8z" />
        <path d="M13.8 6.9l3.2 3.2" />
      </svg>
    );
  }
  if (name === "trash") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M5.5 7.5h13" />
        <path d="M9.5 7.5V5.8h5V7.5" />
        <path d="M8 7.5l.8 11h6.4l.8-11" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <path d="M9.5 5.5L16 12l-6.5 6.5M16 12H5" />
    </svg>
  );
}

function getBrewStepTitle(step: BrewStep): string {
  if (step === "method") return "ELIGE METODO";
  if (step === "coffee") return "ELIGE CAFE";
  if (step === "config") return "CONFIGURA";
  if (step === "brewing") return "PREPARANDO";
  return "RESULTADO";
}

function MentionText({
  text,
  onMentionClick
}: {
  text: string;
  onMentionClick?: (username: string) => void;
}) {
  const mentionRegex = /@([A-Za-z0-9._-]{2,30})/g;
  const parts: Array<{ value: string; mention: boolean; key: string }> = [];
  let lastIndex = 0;
  let matchIndex = 0;
  for (const match of text.matchAll(mentionRegex)) {
    const index = match.index ?? 0;
    if (index > lastIndex) {
      const chunk = text.slice(lastIndex, index);
      parts.push({ value: chunk, mention: false, key: `t-${matchIndex}-${index}` });
    }
    const username = match[1] ?? "";
    parts.push({ value: `@${username}`, mention: true, key: `m-${matchIndex}-${username}` });
    lastIndex = index + match[0].length;
    matchIndex += 1;
  }
  if (lastIndex < text.length) {
    parts.push({ value: text.slice(lastIndex), mention: false, key: `t-end-${lastIndex}` });
  }

  if (!parts.length) return <>{text}</>;

  return (
    <>
      {parts.map((part) =>
        part.mention ? (
          <button
            key={part.key}
            type="button"
            className="mention-button"
            onClick={() => onMentionClick?.(part.value.slice(1))}
          >
            {part.value}
          </button>
        ) : (
          <span key={part.key}>{part.value}</span>
        )
      )}
    </>
  );
}

function vibrateTap(duration = 10): void {
  if (typeof navigator === "undefined" || typeof navigator.vibrate !== "function") return;
  navigator.vibrate(duration);
}

export function App() {
  const [mode, setMode] = useState<ViewMode>(window.innerWidth < 900 ? "mobile" : "desktop");
  const [activeTab, setActiveTab] = useState<TabId>("timeline");

  const [globalStatus, setGlobalStatus] = useState("Cargando datos...");

  const [users, setUsers] = useState<UserRow[]>([]);
  const [coffees, setCoffees] = useState<CoffeeRow[]>([]);
  const [posts, setPosts] = useState<PostRow[]>([]);
  const [likes, setLikes] = useState<LikeRow[]>([]);
  const [comments, setComments] = useState<CommentRow[]>([]);
  const [postCoffeeTags, setPostCoffeeTags] = useState<PostCoffeeTagRow[]>([]);

  const [diaryEntries, setDiaryEntries] = useState<DiaryEntryRow[]>([]);
  const [pantryItems, setPantryItems] = useState<PantryItemRow[]>([]);
  const [favorites, setFavorites] = useState<FavoriteRow[]>([]);
  const [follows, setFollows] = useState<FollowRow[]>([]);

  const [searchQuery, setSearchQuery] = useState("");
  const [searchFilter, setSearchFilter] = useState<"todo" | "origen" | "nombre">("todo");

  const [brewStep, setBrewStep] = useState<BrewStep>("method");
  const [brewMethod, setBrewMethod] = useState("V60");
  const [brewCoffeeId, setBrewCoffeeId] = useState<string>("");
  const [waterMl, setWaterMl] = useState(300);
  const [ratio, setRatio] = useState(16);
  const [timerSeconds, setTimerSeconds] = useState(150);
  const [brewRunning, setBrewRunning] = useState(false);

  const [diaryTab, setDiaryTab] = useState<"actividad" | "despensa">("actividad");
  const [profileTab, setProfileTab] = useState<"posts" | "adn" | "favoritos">("posts");

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
  const [showCommentEmojiPanel, setShowCommentEmojiPanel] = useState(false);
  const [commentImageFile, setCommentImageFile] = useState<File | null>(null);
  const [commentImageName, setCommentImageName] = useState("");
  const [commentImagePreviewError, setCommentImagePreviewError] = useState(false);
  const [commentImagePreviewUrl, setCommentImagePreviewUrl] = useState("");
  const [showCreatePost, setShowCreatePost] = useState(false);
  const [newPostText, setNewPostText] = useState("");
  const [newPostImageUrl, setNewPostImageUrl] = useState("");
  const [newPostCoffeeId, setNewPostCoffeeId] = useState<string>("");
  const [authReady, setAuthReady] = useState(false);
  const [sessionEmail, setSessionEmail] = useState<string | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);

  const activeUser = useMemo(() => {
    if (!users.length) return null;
    if (sessionEmail) {
      const found = users.find((user) => user.email.toLowerCase() === sessionEmail.toLowerCase());
      if (found) return found;
    }
    return users[0] ?? null;
  }, [sessionEmail, users]);

  useEffect(() => {
    const onResize = () => setMode(window.innerWidth < 900 ? "mobile" : "desktop");
    const onShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setActiveTab("search");
        document.getElementById("quick-search")?.focus();
      }
    };

    window.addEventListener("resize", onResize);
    window.addEventListener("keydown", onShortcut);
    const onScroll = () => setTopbarScrolled(window.scrollY > 18);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      window.removeEventListener("resize", onResize);
      window.removeEventListener("keydown", onShortcut);
      window.removeEventListener("scroll", onScroll);
    };
  }, []);

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
    setNewPostCoffeeId((prev) => prev || coffees[0]?.id || "");
  }, [coffees, showCreatePost]);

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

  const handleSignOut = useCallback(async () => {
    if (supabaseConfigError) return;
    try {
      const supabase = getSupabaseClient();
      await supabase.auth.signOut();
      setUsers([]);
      setCoffees([]);
      setPosts([]);
      setLikes([]);
      setComments([]);
      setPostCoffeeTags([]);
      setFollows([]);
      setDiaryEntries([]);
      setPantryItems([]);
      setFavorites([]);
      setGlobalStatus("Listo");
    } catch (error) {
      setAuthError((error as Error).message);
    }
  }, []);

  useEffect(() => {
    const onEscSheet = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      if (showNotificationsPanel) {
        setNotificationsLastSeenAt(Date.now());
        setShowNotificationsPanel(false);
      }
      if (showCreatePost) {
        setShowCreatePost(false);
        setNewPostText("");
        setNewPostImageUrl("");
        setNewPostCoffeeId("");
      }
      if (commentSheetPostId) {
        setCommentSheetPostId(null);
        setCommentDraft("");
        setHighlightedCommentId(null);
      }
    };
    window.addEventListener("keydown", onEscSheet);
    return () => window.removeEventListener("keydown", onEscSheet);
  }, [commentSheetPostId, showCreatePost, showNotificationsPanel]);

  useEffect(() => {
    const hasModal = Boolean(commentSheetPostId || showCreatePost || showNotificationsPanel);
    const prev = document.body.style.overflow;
    if (hasModal) document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [commentSheetPostId, showCreatePost, showNotificationsPanel]);

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
    setActiveTab("timeline");
    setCommentSheetPostId(postId);
    if (commentId != null && Number.isFinite(commentId)) setHighlightedCommentId(commentId);
    setHandledTimelineDeepLink(true);
    const url = new URL(window.location.href);
    url.searchParams.delete("postId");
    url.searchParams.delete("commentId");
    window.history.replaceState({}, "", url.toString());
  }, [handledTimelineDeepLink, posts]);

  const loadInitialData = useCallback(async () => {
    if (!sessionEmail) return;
    try {
      const data = await fetchInitialData();
      setUsers(data.users);
      setCoffees(data.coffees);
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
  }, [sessionEmail]);

  useEffect(() => {
    if (!authReady || !sessionEmail) return;
    void loadInitialData();
  }, [authReady, loadInitialData, sessionEmail]);

  useEffect(() => {
    if (!activeUser) return;
    (async () => {
      try {
        const data = await fetchUserData(activeUser.id);
        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
      } catch (error) {
        setGlobalStatus(`Error: ${(error as Error).message}`);
      }
    })();
  }, [activeUser]);

  useEffect(() => {
    if (!brewRunning || brewStep !== "brewing") return;
    if (timerSeconds <= 0) {
      setBrewRunning(false);
      setBrewStep("result");
      return;
    }
    const id = window.setTimeout(() => setTimerSeconds((prev) => prev - 1), 1000);
    return () => window.clearTimeout(id);
  }, [brewRunning, brewStep, timerSeconds]);

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
    coffees.forEach((coffee) => {
      const key = `${(coffee.marca ?? "").trim().toLowerCase()}|${(coffee.nombre ?? "").trim().toLowerCase()}`;
      map.set(key, coffee.id);
    });
    return map;
  }, [coffees]);
  const coffeesById = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    coffees.forEach((coffee) => map.set(coffee.id, coffee));
    return map;
  }, [coffees]);

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
        const coffeeKey = tag ? `${(tag.coffee_brand ?? "").trim().toLowerCase()}|${(tag.coffee_name ?? "").trim().toLowerCase()}` : "";
        const coffeeId = coffeeKey ? coffeeIdByNameBrand.get(coffeeKey) ?? null : null;
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
    if (!searchQuery.trim()) return coffees;
    const q = searchQuery.trim().toLowerCase();
    return coffees.filter((coffee) => {
      const byName = coffee.nombre.toLowerCase().includes(q);
      const byOrigin = (coffee.pais_origen ?? "").toLowerCase().includes(q);
      if (searchFilter === "nombre") return byName;
      if (searchFilter === "origen") return byOrigin;
      return byName || byOrigin;
    });
  }, [coffees, searchFilter, searchQuery]);

  const selectedCoffee = coffees.find((coffee) => coffee.id === brewCoffeeId) ?? coffees[0];
  const coffeeGrams = Math.max(1, Math.round(waterMl / ratio));

  const diaryEntriesActivity = diaryEntries.slice(0, 80);
  const pantryCoffeeRows = pantryItems
    .map((item) => ({ item, coffee: coffees.find((coffee) => coffee.id === item.coffee_id) }))
    .filter((row) => row.coffee);

  const profilePosts = timelineCards.filter((card) => {
    if (!activeUser) return false;
    const post = posts.find((entry) => entry.id === card.id);
    return post?.user_id === activeUser.id;
  });

  const favoriteCoffees = favorites.map((favorite) => coffees.find((coffee) => coffee.id === favorite.coffee_id)).filter((coffee): coffee is CoffeeRow => Boolean(coffee));

  const followersCount = activeUser ? follows.filter((follow) => follow.followed_id === activeUser.id).length : 0;
  const followingCount = activeUser ? follows.filter((follow) => follow.follower_id === activeUser.id).length : 0;
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

  const timelineRecommendations = useMemo(() => coffees.slice(0, 8), [coffees]);
  const timelineSuggestions = useMemo(
    () =>
      users
        .filter((user) => user.id !== activeUser?.id && !followingIds.has(user.id))
        .slice(0, 8),
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
    const query = token.slice(1).toLowerCase();
    if (!query) return users.slice(0, 8);
    return users.filter((user) => user.username.toLowerCase().includes(query)).slice(0, 8);
  }, [commentDraft, users]);
  const commentListRef = useRef<HTMLUListElement | null>(null);
  const commentImageInputRef = useRef<HTMLInputElement | null>(null);
  const timelineNotifications = useMemo(() => {
    if (!activeUser) return [];

    const followEvents = follows
      .filter((follow) => follow.followed_id === activeUser.id && follow.follower_id !== activeUser.id)
      .map((follow) => ({
        id: `follow-${follow.follower_id}-${follow.followed_id}`,
        type: "follow" as const,
        userId: follow.follower_id,
        text: "empezo a seguirte",
        timestamp: typeof follow.created_at === "number" ? follow.created_at : Date.now()
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
          text: "comento tu publicacion",
          timestamp: comment.timestamp
        };
      })
      .filter(
        (event): event is { id: string; type: "comment"; userId: number; text: string; timestamp: number } =>
          Boolean(event)
      );

    return [...followEvents, ...commentEvents]
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, 24);
  }, [activeUser, comments, follows, posts]);
  const showNotificationsBadge = useMemo(
    () => timelineNotifications.some((item) => item.timestamp > notificationsLastSeenAt),
    [notificationsLastSeenAt, timelineNotifications]
  );

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
    const imageUrl = newPostImageUrl.trim();
    if (!text && !imageUrl) return;

    setTimelineBusyMessage("Publicando tu contenido...");
    vibrateTap(10);
    try {
      const post = await createPost(activeUser.id, text, imageUrl);
      setPosts((prev) => [post, ...prev]);

      if (newPostCoffeeId) {
        const selectedCoffee = coffees.find((coffee) => coffee.id === newPostCoffeeId);
        if (selectedCoffee) {
          const tag: PostCoffeeTagRow = {
            post_id: post.id,
            coffee_name: selectedCoffee.nombre,
            coffee_brand: selectedCoffee.marca
          };
          await addPostCoffeeTag(tag);
          setPostCoffeeTags((prev) => [tag, ...prev]);
        }
      }

      setShowCreatePost(false);
      setNewPostText("");
      setNewPostImageUrl("");
      setNewPostCoffeeId("");
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
    setActiveTab("search");
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

  if (!authReady) {
    return <LoginGate loading message="Verificando sesion..." />;
  }

  if (!sessionEmail) {
    return (
      <LoginGate
        loading={authBusy}
        message={authBusy ? "Redirigiendo a Google..." : undefined}
        errorMessage={authError}
        onGoogleLogin={handleGoogleLogin}
      />
    );
  }

  const nav = (
    <nav aria-label="Navegacion principal" className="nav nav-mobile">
      {NAV_ITEMS.map((item) => (
        <button
          key={item.id}
          type="button"
          className={`nav-item ${activeTab === item.id ? "is-active" : ""}`}
          onClick={() => setActiveTab(item.id)}
          aria-current={activeTab === item.id ? "page" : undefined}
        >
          <span className="nav-glyph" aria-hidden="true">
            <UiIcon name={item.icon} className="ui-icon" />
          </span>
          <span className="nav-label">{item.label}</span>
        </button>
      ))}
    </nav>
  );

  return (
    <div className={`layout ${mode}`}>
      <main className="main-shell">
        <TopBar
          activeTab={activeTab}
          searchQuery={searchQuery}
          onSearchQueryChange={setSearchQuery}
          onSearchCancel={() => setSearchQuery("")}
          showNotificationsBadge={showNotificationsBadge}
          onTimelineSearchUsers={() => setActiveTab("search")}
          onTimelineNotifications={() => {
            setShowNotificationsPanel(true);
            setNotificationsLastSeenAt(Date.now());
          }}
          scrolled={topbarScrolled}
          brewStep={brewStep}
          onBrewBack={() => {
            if (brewStep === "coffee") setBrewStep("method");
            else if (brewStep === "config") setBrewStep("coffee");
            else if (brewStep === "brewing") setBrewStep("config");
            else if (brewStep === "result") setBrewStep("method");
          }}
          onBrewForward={() => {
            if (brewStep === "config") {
              setTimerSeconds(150);
              setBrewRunning(true);
              setBrewStep("brewing");
            }
          }}
          onProfileSignOut={handleSignOut}
        />

        <section aria-live="polite" className="content">
          {activeTab === "timeline" ? (
            <TimelineView
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
              onOpenCoffee={(coffeeId) => {
                setSearchQuery("");
                setSearchFilter("todo");
                setActiveTab("search");
                const selected = coffees.find((item) => item.id === coffeeId);
                if (selected) setSearchQuery(selected.nombre);
              }}
            />
          ) : null}
          {activeTab === "search" ? <SearchView searchQuery={searchQuery} onSearchQueryChange={setSearchQuery} searchFilter={searchFilter} onSearchFilterChange={setSearchFilter} coffees={filteredCoffees} /> : null}
          {activeTab === "brewlab" ? <BrewLabView brewStep={brewStep} setBrewStep={setBrewStep} brewMethod={brewMethod} setBrewMethod={setBrewMethod} brewCoffeeId={brewCoffeeId} setBrewCoffeeId={setBrewCoffeeId} coffees={coffees} waterMl={waterMl} setWaterMl={setWaterMl} ratio={ratio} setRatio={setRatio} timerSeconds={timerSeconds} brewRunning={brewRunning} setBrewRunning={setBrewRunning} selectedCoffee={selectedCoffee} coffeeGrams={coffeeGrams} /> : null}
          {activeTab === "diary" ? <DiaryView tab={diaryTab} setTab={setDiaryTab} entries={diaryEntriesActivity} pantryRows={pantryCoffeeRows} /> : null}
          {activeTab === "profile" && activeUser ? <ProfileView user={activeUser} tab={profileTab} setTab={setProfileTab} posts={profilePosts} favoriteCoffees={favoriteCoffees} followers={followersCount} following={followingCount} /> : null}
        </section>

        {activeTab === "timeline" ? (
          <button className="fab" type="button" aria-label="Nuevo Post" onClick={() => setShowCreatePost(true)}>
            <UiIcon name="add" className="ui-icon" />
          </button>
        ) : null}
      </main>

      <footer className="bottom-tabs">{nav}</footer>

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
          onClick={() => {
            setShowCreatePost(false);
            setNewPostText("");
            setNewPostImageUrl("");
            setNewPostCoffeeId("");
          }}
        >
          <div className="sheet-card" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">NUEVA PUBLICACION</strong>
              <button
                type="button"
                className="text-button"
                onClick={() => {
                  setShowCreatePost(false);
                  setNewPostText("");
                  setNewPostImageUrl("");
                  setNewPostCoffeeId("");
                }}
              >
                Cerrar
              </button>
            </header>
            <div className="create-post-body">
              <textarea
                className="search-wide sheet-input"
                placeholder="Que estas preparando?"
                rows={4}
                value={newPostText}
                onChange={(event) => setNewPostText(event.target.value)}
              />
              <input
                className="search-wide"
                placeholder="URL de imagen (opcional)"
                value={newPostImageUrl}
                onChange={(event) => setNewPostImageUrl(event.target.value)}
              />
              {newPostImageUrl.trim() ? (
                <img className="create-post-preview" src={newPostImageUrl.trim()} alt="Previsualizacion" loading="lazy" />
              ) : null}
              <select
                className="search-wide"
                value={newPostCoffeeId}
                onChange={(event) => setNewPostCoffeeId(event.target.value)}
              >
                <option value="">Sin cafe etiquetado</option>
                {coffees.slice(0, 80).map((coffee) => (
                  <option key={coffee.id} value={coffee.id}>
                    {coffee.marca} {coffee.nombre}
                  </option>
                ))}
              </select>
              <div className="create-post-actions">
                <button
                  type="button"
                  className="action-button action-button-ghost"
                  onClick={() => {
                    setShowCreatePost(false);
                    setNewPostText("");
                    setNewPostImageUrl("");
                    setNewPostCoffeeId("");
                  }}
                >
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  onClick={handleCreatePost}
                  disabled={!newPostText.trim() && !newPostImageUrl.trim()}
                >
                  Publicar
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {showNotificationsPanel ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Notificaciones"
          onClick={() => {
            setNotificationsLastSeenAt(Date.now());
            setShowNotificationsPanel(false);
          }}
        >
          <div className="sheet-card" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">NOTIFICACIONES</strong>
              <button
                type="button"
                className="text-button"
                onClick={() => {
                  setNotificationsLastSeenAt(Date.now());
                  setShowNotificationsPanel(false);
                }}
              >
                Cerrar
              </button>
            </header>
            <ul className="sheet-list">
              {timelineNotifications.length ? (
                timelineNotifications.map((item) => {
                  const user = usersById.get(item.userId);
                  const isUnread = item.timestamp > notificationsLastSeenAt;
                  return (
                    <li key={item.id} className={`sheet-item ${isUnread ? "is-unread" : ""}`}>
                      <div className="sheet-item-head">
                        {user?.avatar_url ? (
                          <img className="avatar avatar-photo" src={user.avatar_url} alt={user.username} loading="lazy" />
                        ) : (
                          <div className="avatar" aria-hidden="true">
                            {(user?.username ?? "us").slice(0, 2).toUpperCase()}
                          </div>
                        )}
                        <div>
                          <p className="feed-user">@{user?.username ?? `user${item.userId}`}</p>
                          <p className="feed-meta">
                            {item.text} - {toRelativeMinutes(item.timestamp)}
                          </p>
                        </div>
                        {item.type === "follow" && activeUser ? (
                          <button
                            type="button"
                            className={`action-button notification-follow-button ${followingIds.has(item.userId) ? "is-following" : ""}`}
                            onClick={() => handleToggleFollow(item.userId)}
                          >
                            {followingIds.has(item.userId) ? "SIGUIENDO" : "SEGUIR"}
                          </button>
                        ) : null}
                        {isUnread ? <span className="sheet-unread-dot" aria-hidden="true" /> : null}
                      </div>
                    </li>
                  );
                })
              ) : (
                <li className="sheet-item">No hay notificaciones</li>
              )}
            </ul>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function TopBar({
  activeTab,
  searchQuery,
  onSearchQueryChange,
  onSearchCancel,
  showNotificationsBadge,
  onTimelineSearchUsers,
  onTimelineNotifications,
  scrolled,
  brewStep,
  onBrewBack,
  onBrewForward,
  onProfileSignOut
}: {
  activeTab: TabId;
  searchQuery: string;
  onSearchQueryChange: (value: string) => void;
  onSearchCancel: () => void;
  showNotificationsBadge: boolean;
  onTimelineSearchUsers: () => void;
  onTimelineNotifications: () => void;
  scrolled: boolean;
  brewStep: BrewStep;
  onBrewBack: () => void;
  onBrewForward: () => void;
  onProfileSignOut: () => void;
}) {
  const [searchFocus, setSearchFocus] = useState(false);
  const [searchHintWord, setSearchHintWord] = useState<"marca" | "cafe">("marca");
  const [notificationPop, setNotificationPop] = useState(false);

  useEffect(() => {
    if (activeTab !== "search") return;
    const interval = window.setInterval(() => {
      setSearchHintWord((prev) => (prev === "marca" ? "cafe" : "marca"));
    }, 2200);
    return () => window.clearInterval(interval);
  }, [activeTab]);

  useEffect(() => {
    if (!showNotificationsBadge || activeTab !== "timeline") return;
    setNotificationPop(true);
    const id = window.setTimeout(() => setNotificationPop(false), 340);
    return () => window.clearTimeout(id);
  }, [activeTab, showNotificationsBadge]);

  if (activeTab === "search") {
    return (
      <header className={`topbar topbar-search ${scrolled ? "topbar-scrolled" : ""}`}>
        <input
          id="quick-search"
          className="search-wide"
          placeholder={`Busca ${searchHintWord}`}
          value={searchQuery}
          onFocus={() => setSearchFocus(true)}
          onBlur={() => setSearchFocus(false)}
          onChange={(event) => onSearchQueryChange(event.target.value)}
          aria-label="Busqueda"
        />
        {searchQuery || searchFocus ? <button className="text-button" type="button" onClick={onSearchCancel}>Cancelar</button> : null}
      </header>
    );
  }

  if (activeTab === "brewlab") {
    return (
      <header className={`topbar ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-inline">
          {brewStep !== "method" ? <button className="icon-button" type="button" onClick={onBrewBack} aria-label="Atras"><UiIcon name="arrow-left" className="ui-icon" /></button> : null}
          <h1 className="title title-upper">{getBrewStepTitle(brewStep)}</h1>
        </div>
        {brewStep === "config" ? <button className="icon-button" type="button" onClick={onBrewForward} aria-label="Empezar"><UiIcon name="arrow-right" className="ui-icon" /></button> : null}
      </header>
    );
  }

  if (activeTab === "diary") {
    return (
      <header className={`topbar ${scrolled ? "topbar-scrolled" : ""}`}>
        <h1 className="title title-upper">MI DIARIO</h1>
        <div className="topbar-actions">
          <button className="icon-button" type="button" aria-label="Agregar"><UiIcon name="add" className="ui-icon" /></button>
          <button className="chip-button" type="button">HOY</button>
        </div>
      </header>
    );
  }

  if (activeTab === "profile") {
    return (
      <header className={`topbar ${scrolled ? "topbar-scrolled" : ""}`}>
        <h1 className="title title-upper">PERFIL</h1>
        <button className="icon-button" type="button" aria-label="Cerrar sesion" onClick={onProfileSignOut}><UiIcon name="settings" className="ui-icon" /></button>
      </header>
    );
  }

  return (
    <header className={`topbar topbar-centered ${scrolled ? "topbar-scrolled" : ""}`}>
      <div className="topbar-slot">
        <button className="icon-button" type="button" aria-label="Buscar Usuarios" onClick={onTimelineSearchUsers}>
          <UiIcon name="search" className="ui-icon" />
        </button>
      </div>
      <h1 className="title title-upper topbar-title-center">CAFESITO</h1>
      <div className="topbar-slot topbar-slot-end">
        <button
          className={`icon-button ${notificationPop ? "notification-pop" : ""}`}
          type="button"
          aria-label="Notificaciones"
          onClick={onTimelineNotifications}
        >
          <UiIcon name="notifications" className="ui-icon" />
          {showNotificationsBadge ? <span className="badge-dot" aria-hidden="true" /> : null}
        </button>
      </div>
    </header>
  );
}

function LoginGate({
  loading,
  message,
  errorMessage,
  onGoogleLogin
}: {
  loading: boolean;
  message?: string;
  errorMessage?: string | null;
  onGoogleLogin?: () => void;
}) {
  return (
    <main className="login-gate" aria-label="Inicio de sesion">
      <section className="login-card">
        <p className="login-brand">CAFESITO</p>
        <h1 className="login-title">Inicia sesion</h1>
        <p className="login-copy">Accede con Google para continuar.</p>
        <button
          type="button"
          className="google-login-button"
          onClick={onGoogleLogin}
          disabled={loading || !onGoogleLogin}
        >
          <span className="google-dot" aria-hidden="true" />
          {loading ? "Conectando..." : "Continuar con Google"}
        </button>
        {message ? <p className="login-hint">{message}</p> : null}
        {errorMessage ? <p className="login-error">{errorMessage}</p> : null}
      </section>
    </main>
  );
}

function TimelineView({
  cards,
  recommendations,
  suggestions,
  suggestionIndices,
  followingIds,
  followerCounts,
  loading,
  errorMessage,
  refreshing,
  activeUserId,
  onOpenComments,
  onToggleLike,
  onToggleFollow,
  onEditPost,
  onDeletePost,
  onRefresh,
  onMentionClick,
  onOpenCoffee
}: {
  cards: TimelineCard[];
  recommendations: CoffeeRow[];
  suggestions: UserRow[];
  suggestionIndices: number[];
  followingIds: Set<number>;
  followerCounts: Map<number, number>;
  loading: boolean;
  errorMessage: string | null;
  refreshing: boolean;
  activeUserId: number | null;
  onOpenComments: (postId: string) => void;
  onToggleLike: (postId: string) => void;
  onToggleFollow: (userId: number) => void;
  onEditPost: (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => Promise<void>;
  onDeletePost: (postId: string) => Promise<void>;
  onRefresh: () => Promise<void>;
  onMentionClick: (username: string) => void;
  onOpenCoffee: (coffeeId: string) => void;
}) {
  const [menuPostId, setMenuPostId] = useState<string | null>(null);
  const [editingPostId, setEditingPostId] = useState<string | null>(null);
  const [editingText, setEditingText] = useState("");
  const [editingImageUrl, setEditingImageUrl] = useState("");
  const [editingImageFile, setEditingImageFile] = useState<File | null>(null);
  const [editingImagePreviewUrl, setEditingImagePreviewUrl] = useState("");
  const [likeBurstPostId, setLikeBurstPostId] = useState<string | null>(null);
  const [pullDistance, setPullDistance] = useState(0);
  const editImageInputRef = useRef<HTMLInputElement | null>(null);
  const touchStartY = useRef<number | null>(null);
  const pullActive = useRef(false);
  const likeBurstTimerRef = useRef<number | null>(null);
  const visibleCards = cards;
  const activeMenuPost = useMemo(
    () => visibleCards.find((card) => card.id === menuPostId) ?? null,
    [menuPostId, visibleCards]
  );

  const closeEditModal = useCallback(() => {
    if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
    setEditingPostId(null);
    setEditingText("");
    setEditingImageUrl("");
    setEditingImageFile(null);
    setEditingImagePreviewUrl("");
  }, [editingImagePreviewUrl]);

  const isMobileLike = typeof window !== "undefined" ? window.innerWidth < 900 : false;
  const pullProgress = Math.min(1, pullDistance / 84);

  const handleTouchStart = (event: React.TouchEvent<HTMLElement>) => {
    if (!isMobileLike) return;
    if (window.scrollY > 0) return;
    touchStartY.current = event.touches[0]?.clientY ?? null;
    pullActive.current = true;
  };

  const handleTouchMove = (event: React.TouchEvent<HTMLElement>) => {
    if (!pullActive.current) return;
    const start = touchStartY.current;
    const current = event.touches[0]?.clientY;
    if (start == null || current == null) return;
    const delta = Math.max(0, current - start);
    if (delta <= 0) return;
    setPullDistance(Math.min(120, delta * 0.55));
  };

  const handleTouchEnd = () => {
    if (!pullActive.current) return;
    pullActive.current = false;
    touchStartY.current = null;
    const shouldRefresh = pullDistance >= 76 && !refreshing;
    setPullDistance(0);
    if (shouldRefresh) void onRefresh();
  };

  useEffect(() => {
    const onEsc = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setMenuPostId(null);
      if (editingPostId) closeEditModal();
    };
    window.addEventListener("keydown", onEsc);
    return () => window.removeEventListener("keydown", onEsc);
  }, [closeEditModal, editingPostId]);

  useEffect(() => {
    if (!editingPostId) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [editingPostId]);

  useEffect(() => {
    return () => {
      if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
    };
  }, [editingImagePreviewUrl]);

  useEffect(() => {
    return () => {
      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
    };
  }, []);

  if (loading) {
    return (
      <ul className="feed-list">
        {[1, 2, 3, 4, 5].map((item) => (
          <li key={item} className="feed-card shimmer-card" />
        ))}
      </ul>
    );
  }

  if (errorMessage) {
    return (
      <article className="timeline-empty timeline-error">
        <h3>No pudimos cargar el timeline</h3>
        <p>{errorMessage}</p>
      </article>
    );
  }

  const recommendationSection = recommendations.length ? (
    <section className="suggestion-strip">
      <p className="section-title">Recomendados para tu paladar</p>
      <div className="horizontal-cards">
        {recommendations.map((coffee) => (
          <article key={coffee.id} className="mini-card mini-coffee-card">
            {coffee.image_url ? <img className="mini-cover" src={coffee.image_url} alt={coffee.nombre} loading="lazy" /> : null}
            <p className="coffee-origin">{coffee.pais_origen ?? "Origen"}</p>
            <p className="feed-user">{coffee.nombre}</p>
            <p className="coffee-sub">{coffee.marca}</p>
          </article>
        ))}
      </div>
    </section>
  ) : null;

  const userSuggestionsSection = suggestions.length ? (
    <section className="suggestion-strip">
      <p className="section-title">Personas que podrias seguir</p>
      <div className="horizontal-cards">
        {suggestions.map((user) => (
          <article key={user.id} className="mini-card mini-user-card">
            {user.avatar_url ? <img className="mini-avatar" src={user.avatar_url} alt={user.username} loading="lazy" /> : <div className="avatar mini-avatar-fallback">{user.username.slice(0, 2).toUpperCase()}</div>}
            <p className="feed-user">{user.full_name}</p>
            <p className="feed-meta">@{user.username}</p>
            <p className="coffee-sub">{followerCounts.get(user.id) ?? 0} seguidores</p>
            <button
              className={`action-button ${followingIds.has(user.id) ? "action-button-following" : "action-button-ghost"}`}
              type="button"
              onClick={() => onToggleFollow(user.id)}
            >
              {followingIds.has(user.id) ? "Siguiendo" : "Seguir"}
            </button>
          </article>
        ))}
      </div>
    </section>
  ) : null;

  if (!cards.length) {
    return (
      <>
        <article className="timeline-empty">
          <h3>Tu timeline esta vacio</h3>
          <p>Empieza siguiendo personas o publicando tu primer cafe.</p>
          <button className="action-button" type="button">
            Publica tu primer cafe
          </button>
        </article>
        {userSuggestionsSection}
        {recommendationSection}
      </>
    );
  }

  return (
    <div
      className="timeline-shell"
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      <div
        className={`pull-indicator ${pullDistance > 2 ? "is-visible" : ""}`}
        style={{ ["--pull-distance" as string]: `${pullDistance}px`, ["--pull-progress" as string]: `${pullProgress}` }}
        aria-hidden="true"
      >
        <span className={`pull-dot ${pullProgress >= 1 ? "is-ready" : ""}`} />
      </div>
      {refreshing ? <div className="timeline-refresh-bar" aria-hidden="true" /> : null}

      {visibleCards.length ? (
        <ul className="feed-list">
          {visibleCards.map((card, index) => (
            <Fragment key={card.id}>
              {recommendationSection && suggestionIndices[0] != null && index === suggestionIndices[0] ? (
                <li className="feed-inline-item">{recommendationSection}</li>
              ) : null}
              {userSuggestionsSection && suggestionIndices[1] != null && index === suggestionIndices[1] ? (
                <li className="feed-inline-item">{userSuggestionsSection}</li>
              ) : null}
              <li className="feed-card feed-card-premium feed-entry" style={{ ["--feed-index" as string]: index }}>
                <article>
              <header className="feed-head">
                {card.avatarUrl ? (
                  <img className="avatar avatar-photo" src={card.avatarUrl} alt={card.username} loading="lazy" />
                ) : (
                  <div className="avatar" aria-hidden="true">
                    {card.userName
                      .split(" ")
                      .map((part) => part[0])
                      .join("")
                      .slice(0, 2)
                      .toUpperCase()}
                  </div>
                )}
                <div>
                  <p className="feed-user">{card.userName}</p>
                  <p className="feed-meta">{card.minsAgoLabel.toUpperCase()}</p>
                </div>
                {activeUserId === card.userId ? (
                  <button
                    type="button"
                    className="icon-button post-menu-trigger"
                    onClick={() => setMenuPostId(card.id)}
                    aria-label="Opciones"
                  >
                    <UiIcon name="more" className="ui-icon" />
                  </button>
                ) : null}
              </header>

              {card.text ? (
                <p className="feed-text">
                  <MentionText text={card.text} onMentionClick={onMentionClick} />
                </p>
              ) : null}

              {card.imageUrl ? <img className={`feed-image ${card.text ? "" : "feed-image-no-text"}`.trim()} src={card.imageUrl} alt="Publicacion" loading="lazy" /> : null}

              {card.coffeeTagName ? (
                <button type="button" className="coffee-tag-card" onClick={() => card.coffeeId && onOpenCoffee(card.coffeeId)} disabled={!card.coffeeId}>
                  <div className="coffee-tag-card-media">
                    {card.coffeeImageUrl ? (
                      <img className="coffee-tag-image" src={card.coffeeImageUrl} alt={card.coffeeTagName} loading="lazy" />
                    ) : (
                      <div className="coffee-tag-image coffee-tag-image-fallback" aria-hidden="true">
                        <UiIcon name="coffee" className="ui-icon" />
                      </div>
                    )}
                  </div>
                  <div className="coffee-tag-copy">
                    <p className="coffee-origin">CAFE ETIQUETADO</p>
                    <p className="coffee-tag-name">{card.coffeeTagName}</p>
                    {card.coffeeTagBrand ? <p className="coffee-tag-brand">{card.coffeeTagBrand.toUpperCase()}</p> : null}
                  </div>
                  <UiIcon name="chevron-right" className="ui-icon" />
                </button>
              ) : null}

              <footer className="feed-stats">
                <button
                  type="button"
                  className={`inline-action action-like ${card.likedByActiveUser ? "is-liked" : ""} ${likeBurstPostId === card.id ? "is-bursting" : ""}`}
                  onClick={() => {
                    if (!card.likedByActiveUser) {
                      setLikeBurstPostId(card.id);
                      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
                      likeBurstTimerRef.current = window.setTimeout(() => {
                        setLikeBurstPostId(null);
                        likeBurstTimerRef.current = null;
                      }, 520);
                    }
                    onToggleLike(card.id);
                  }}
                >
                  <span className="like-icon-wrap">
                    <UiIcon name="coffee" className="ui-icon" />
                    <span className="like-burst" aria-hidden="true">
                      <span />
                      <span />
                      <span />
                      <span />
                      <span />
                      <span />
                    </span>
                  </span>
                  {card.likes > 0 ? <span>{card.likes}</span> : null}
                </button>
                <button type="button" className="inline-action" onClick={() => onOpenComments(card.id)}>
                  <UiIcon name="chat" className="ui-icon" />
                  {card.comments > 0 ? <span>{card.comments}</span> : null}
                </button>
              </footer>
                </article>
              </li>
            </Fragment>
          ))}
        </ul>
      ) : (
        <article className="timeline-empty">
          <h3>No hay publicaciones disponibles</h3>
          <p>Publica tu primer cafe o sigue a mas personas.</p>
        </article>
      )}
      {activeMenuPost ? (
        <div className="sheet-overlay comment-action-overlay" onClick={() => setMenuPostId(null)}>
          <div className="sheet-card comment-action-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <div className="comment-action-list">
              <p className="comment-action-title">OPCIONES</p>
              <button
                type="button"
                className="comment-action-button"
                onClick={() => {
                  setEditingPostId(activeMenuPost.id);
                  setEditingText(activeMenuPost.text);
                  const initialImage = activeMenuPost.imageUrl ?? "";
                  setEditingImageUrl(initialImage);
                  setEditingImageFile(null);
                  setEditingImagePreviewUrl(initialImage);
                  setMenuPostId(null);
                }}
              >
                <UiIcon name="edit" className="ui-icon" />
                <span>Editar</span>
                <UiIcon name="chevron-right" className="ui-icon trailing" />
              </button>
              <button
                type="button"
                className="comment-action-button is-danger"
                onClick={async () => {
                  const confirmed = window.confirm("Borrar post definitivamente?");
                  if (!confirmed) return;
                  await onDeletePost(activeMenuPost.id);
                  setMenuPostId(null);
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
      {editingPostId ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Editar publicacion"
          onClick={closeEditModal}
        >
          <div className="sheet-card" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <div className="create-post-body edit-post-sheet">
              <h3 className="edit-post-title">Editar</h3>
              <input
                ref={editImageInputRef}
                type="file"
                accept="image/*"
                className="file-input-hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  if (!file) return;
                  if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
                  const preview = URL.createObjectURL(file);
                  setEditingImageFile(file);
                  setEditingImagePreviewUrl(preview);
                }}
              />
              <button
                type="button"
                className="edit-image-picker"
                onClick={() => editImageInputRef.current?.click()}
              >
                {editingImagePreviewUrl.trim() ? (
                  <img className="create-post-preview" src={editingImagePreviewUrl.trim()} alt="Previsualizacion" loading="lazy" />
                ) : (
                  <span className="edit-image-placeholder">Seleccionar imagen</span>
                )}
              </button>
              <textarea
                className="search-wide sheet-input"
                placeholder="Descripcion"
                value={editingText}
                rows={4}
                onChange={(event) => setEditingText(event.target.value)}
              />
              <div className="create-post-actions edit-post-actions-native">
                <button
                  type="button"
                  className="action-button action-button-ghost edit-post-cancel"
                  onClick={closeEditModal}
                >
                  CANCELAR
                </button>
                <button
                  type="button"
                  className="action-button edit-post-save"
                  disabled={!editingText.trim() && !editingImageUrl.trim() && !editingImageFile}
                  onClick={async () => {
                    await onEditPost(editingPostId, editingText.trim(), editingImageUrl.trim(), editingImageFile);
                    closeEditModal();
                  }}
                >
                  GUARDAR
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function SearchView({
  searchQuery,
  onSearchQueryChange,
  searchFilter,
  onSearchFilterChange,
  coffees
}: {
  searchQuery: string;
  onSearchQueryChange: (value: string) => void;
  searchFilter: "todo" | "origen" | "nombre";
  onSearchFilterChange: (value: "todo" | "origen" | "nombre") => void;
  coffees: CoffeeRow[];
}) {
  return (
    <>
      <div className="tab-filters" role="tablist" aria-label="Filtros de busqueda">
        {(["todo", "nombre", "origen"] as const).map((filter) => (
          <button key={filter} className={`filter-chip ${searchFilter === filter ? "is-active" : ""}`} type="button" role="tab" aria-selected={searchFilter === filter} onClick={() => onSearchFilterChange(filter)}>
            {filter.toUpperCase()}
          </button>
        ))}
      </div>

      <input className="search-wide" placeholder="Buscar cafe" value={searchQuery} onChange={(event) => onSearchQueryChange(event.target.value)} aria-label="Buscar cafe" />

      <ul className="coffee-list">
        {coffees.map((coffee) => (
          <li key={coffee.id} className="coffee-card">
            <p className="coffee-origin">{coffee.pais_origen ?? "Origen desconocido"}</p>
            <strong>{coffee.nombre}</strong>
            <p className="coffee-sub">{coffee.marca || "Marca"}</p>
          </li>
        ))}
      </ul>
    </>
  );
}

function BrewLabView({
  brewStep,
  setBrewStep,
  brewMethod,
  setBrewMethod,
  brewCoffeeId,
  setBrewCoffeeId,
  coffees,
  waterMl,
  setWaterMl,
  ratio,
  setRatio,
  timerSeconds,
  brewRunning,
  setBrewRunning,
  selectedCoffee,
  coffeeGrams
}: {
  brewStep: BrewStep;
  setBrewStep: (step: BrewStep) => void;
  brewMethod: string;
  setBrewMethod: (value: string) => void;
  brewCoffeeId: string;
  setBrewCoffeeId: (value: string) => void;
  coffees: CoffeeRow[];
  waterMl: number;
  setWaterMl: (value: number) => void;
  ratio: number;
  setRatio: (value: number) => void;
  timerSeconds: number;
  brewRunning: boolean;
  setBrewRunning: (value: boolean) => void;
  selectedCoffee?: CoffeeRow;
  coffeeGrams: number;
}) {
  return (
    <>
      <ol className="stepper" aria-label="Pasos de elabora">
        {["method", "coffee", "config", "brewing", "result"].map((id) => (
          <li key={id} className={`step ${brewStep === id ? "is-active" : ""}`}>{id.toUpperCase()}</li>
        ))}
      </ol>

      {brewStep === "method" ? (
        <div className="method-grid">
          {BREW_METHODS.map((method) => (
            <button key={method} type="button" className={`method-card ${brewMethod === method ? "is-active" : ""}`} onClick={() => {
              setBrewMethod(method);
              setBrewStep("coffee");
            }}>
              {method}
            </button>
          ))}
        </div>
      ) : null}

      {brewStep === "coffee" ? (
        <ul className="coffee-list">
          {coffees.slice(0, 20).map((coffee) => (
            <li key={coffee.id}>
              <button type="button" className={`coffee-select ${brewCoffeeId === coffee.id ? "is-active" : ""}`} onClick={() => {
                setBrewCoffeeId(coffee.id);
                setBrewStep("config");
              }}>
                <span>{coffee.nombre}</span>
                <small>{coffee.marca}</small>
              </button>
            </li>
          ))}
        </ul>
      ) : null}

      {brewStep === "config" ? (
        <section className="config-card">
          <p className="section-title">Configuracion</p>
          <p className="feed-meta">Metodo: {brewMethod}</p>
          <p className="feed-meta">Cafe: {selectedCoffee?.nombre ?? "No seleccionado"}</p>

          <label className="slider-row">
            Agua {waterMl} ml
            <input type="range" min={150} max={600} step={10} value={waterMl} onChange={(event) => setWaterMl(Number(event.target.value))} />
          </label>

          <label className="slider-row">
            Ratio 1:{ratio}
            <input type="range" min={12} max={20} step={1} value={ratio} onChange={(event) => setRatio(Number(event.target.value))} />
          </label>

          <p className="metric-pill">Cafe recomendado: {coffeeGrams} g</p>
          <button className="action-button" type="button" onClick={() => setBrewStep("brewing")}>Empezar preparacion</button>
        </section>
      ) : null}

      {brewStep === "brewing" ? (
        <section className="brew-timer">
          <p className="section-title">Preparando {brewMethod}</p>
          <strong className="timer-value">{timerSeconds}s</strong>
          <div className="timer-actions">
            <button className="action-button" type="button" onClick={() => setBrewRunning(!brewRunning)}>{brewRunning ? "Pausar" : "Iniciar"}</button>
            <button className="action-button action-button-ghost" type="button" onClick={() => {
              setBrewRunning(false);
              setBrewStep("result");
            }}>
              Finalizar
            </button>
          </div>
        </section>
      ) : null}

      {brewStep === "result" ? (
        <section className="config-card">
          <p className="section-title">Resultado</p>
          <p className="feed-meta">Metodo: {brewMethod}</p>
          <p className="feed-meta">Cafe: {selectedCoffee?.nombre ?? "-"}</p>
          <p className="metric-pill">Recomendacion: sube 1 click molienda para mas dulzor</p>
          <button className="action-button" type="button" onClick={() => setBrewStep("method")}>Nueva preparacion</button>
        </section>
      ) : null}
    </>
  );
}

function DiaryView({
  tab,
  setTab,
  entries,
  pantryRows
}: {
  tab: "actividad" | "despensa";
  setTab: (value: "actividad" | "despensa") => void;
  entries: DiaryEntryRow[];
  pantryRows: Array<{ item: PantryItemRow; coffee?: CoffeeRow }>;
}) {
  const [period, setPeriod] = useState<"hoy" | "7d" | "30d">("hoy");
  const visibleEntries = useMemo(() => {
    if (period === "hoy") return entries.filter((entry) => withinDays(entry.timestamp, 1));
    if (period === "7d") return entries.filter((entry) => withinDays(entry.timestamp, 7));
    return entries.filter((entry) => withinDays(entry.timestamp, 30));
  }, [entries, period]);

  const analytics = useMemo(() => {
    const caffeine = visibleEntries.reduce((acc, entry) => acc + Math.max(0, entry.caffeine_mg || 0), 0);
    const coffeeCups = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() !== "WATER").length;
    const waterEntries = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() === "WATER").length;
    return { caffeine, coffeeCups, waterEntries };
  }, [visibleEntries]);

  return (
    <>
      <article className="diary-analytics-card">
        <p className="section-title">Consumo de cafeina</p>
        <div className="analytics-grid">
          <div>
            <p className="metric-label">Total</p>
            <p className="analytics-value">{analytics.caffeine} mg</p>
          </div>
          <div>
            <p className="metric-label">Cafe</p>
            <p className="analytics-value">{analytics.coffeeCups}</p>
          </div>
          <div>
            <p className="metric-label">Agua</p>
            <p className="analytics-value">{analytics.waterEntries}</p>
          </div>
        </div>
      </article>

      <div className="period-row" role="tablist" aria-label="Periodo diario">
        {(["hoy", "7d", "30d"] as const).map((item) => (
          <button
            key={item}
            type="button"
            className={`chip-button period-chip ${period === item ? "is-active" : ""}`}
            role="tab"
            aria-selected={period === item}
            onClick={() => setPeriod(item)}
          >
            {item.toUpperCase()}
          </button>
        ))}
      </div>

      <div className="premium-tabs" role="tablist" aria-label="Tabs diario">
        <button type="button" className={`premium-tab ${tab === "actividad" ? "is-active" : ""}`} role="tab" aria-selected={tab === "actividad"} onClick={() => setTab("actividad")}>ACTIVIDAD</button>
        <button type="button" className={`premium-tab ${tab === "despensa" ? "is-active" : ""}`} role="tab" aria-selected={tab === "despensa"} onClick={() => setTab("despensa")}>DESPENSA</button>
      </div>

      {tab === "actividad" ? (
        <ul className="diary-list">
          {visibleEntries.length ? visibleEntries.map((entry) => (
            <li key={entry.id} className="diary-card">
              <div>
                <p className="feed-user">{entry.coffee_name || "Entrada"}</p>
                <p className="feed-meta">{entry.preparation_type || entry.type}</p>
              </div>
              <div className="score-pill">{entry.caffeine_mg} mg</div>
              <p className="feed-meta">{toRelativeMinutes(entry.timestamp)}</p>
            </li>
          )) : <li className="diary-card">Sin actividad en el periodo</li>}
        </ul>
      ) : null}

      {tab === "despensa" ? (
        <ul className="coffee-list">
          {pantryRows.length ? pantryRows.map((row) => (
            <li key={`${row.item.user_id}-${row.item.coffee_id}`} className="coffee-card">
              <p className="coffee-origin">{row.coffee?.pais_origen ?? "Origen"}</p>
              <strong>{row.coffee?.nombre ?? row.item.coffee_id}</strong>
              <p className="coffee-sub">Stock {row.item.total_grams}g | restante {row.item.grams_remaining}g</p>
            </li>
          )) : <li className="coffee-card">Sin items en despensa</li>}
        </ul>
      ) : null}
    </>
  );
}

function ProfileView({
  user,
  tab,
  setTab,
  posts,
  favoriteCoffees,
  followers,
  following
}: {
  user: UserRow;
  tab: "posts" | "adn" | "favoritos";
  setTab: (value: "posts" | "adn" | "favoritos") => void;
  posts: TimelineCard[];
  favoriteCoffees: CoffeeRow[];
  followers: number;
  following: number;
}) {
  return (
    <>
      <article className="profile-hero">
        <div className="avatar avatar-lg" aria-hidden="true">{user.full_name.split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase()}</div>
        <div>
          <p className="feed-user">{user.full_name}</p>
          <p className="feed-meta">@{user.username}</p>
        </div>
        <button className="action-button action-button-ghost" type="button">Editar</button>
      </article>

      <section className="metric-grid">
        <article className="metric-card"><p className="metric-label">Posts</p><strong className="metric-value">{posts.length}</strong></article>
        <article className="metric-card"><p className="metric-label">Seguidores</p><strong className="metric-value">{followers}</strong></article>
        <article className="metric-card"><p className="metric-label">Siguiendo</p><strong className="metric-value">{following}</strong></article>
      </section>

      <div className="premium-tabs" role="tablist" aria-label="Tabs perfil">
        <button type="button" className={`premium-tab ${tab === "posts" ? "is-active" : ""}`} role="tab" aria-selected={tab === "posts"} onClick={() => setTab("posts")}>POSTS</button>
        <button type="button" className={`premium-tab ${tab === "adn" ? "is-active" : ""}`} role="tab" aria-selected={tab === "adn"} onClick={() => setTab("adn")}>ADN</button>
        <button type="button" className={`premium-tab ${tab === "favoritos" ? "is-active" : ""}`} role="tab" aria-selected={tab === "favoritos"} onClick={() => setTab("favoritos")}>FAVORITOS</button>
      </div>

      {tab === "posts" ? (
        <ul className="feed-list">
          {posts.length ? posts.map((post) => (
            <li key={post.id} className="feed-card">
              <p className="feed-text">{post.text || "Sin texto"}</p>
              <p className="feed-meta">hace {post.minsAgoLabel}</p>
            </li>
          )) : <li className="feed-card">Aun no hay publicaciones</li>}
        </ul>
      ) : null}

      {tab === "adn" ? (
        <article className="config-card">
          <p className="section-title">Perfil sensorial</p>
          <p className="metric-pill">Dulzor 0 | Acidez 0 | Cuerpo 0 | Amargor 0</p>
          <p className="feed-meta">Calculado desde favoritos y resenas.</p>
        </article>
      ) : null}

      {tab === "favoritos" ? (
        <ul className="coffee-list">
          {favoriteCoffees.length ? favoriteCoffees.map((coffee) => (
            <li key={coffee.id} className="coffee-card">
              <p className="coffee-origin">{coffee.pais_origen ?? "Origen"}</p>
              <strong>{coffee.nombre}</strong>
              <p className="coffee-sub">{coffee.marca}</p>
            </li>
          )) : <li className="coffee-card">No hay cafes favoritos</li>}
        </ul>
      ) : null}
    </>
  );
}



