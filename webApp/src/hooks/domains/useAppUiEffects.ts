import { type Dispatch, type MutableRefObject, type RefObject, type SetStateAction, useEffect } from "react";

export function useAppUiEffects({
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
  navigateToTimelineReplace,
  setNotificationsLastSeenAt,
  notificationsLastSeenAt,
  visibleTimelineNotifications,
  dismissNotificationTimersRef,
  closeDiarySheets,
  handleRefreshTimeline
}: {
  timelineActionBanner: string | null;
  setTimelineActionBanner: (value: string | null) => void;
  activeTab: "timeline" | "search" | "coffee" | "brewlab" | "diary" | "profile";
  searchMode: "users" | "coffees";
  searchFocusCoffeeProfile: boolean;
  setSearchActiveFilterType: (value: "origen" | "especialidad" | "tueste" | "formato" | "nota" | null) => void;
  showNotificationsPanel: boolean;
  showCreatePost: boolean;
  showCreatePostCoffeeSheet: boolean;
  commentSheetPostId: string | null;
  showCreateCoffeeComposer: boolean;
  resetCreatePostComposer: () => void;
  setShowNotificationsPanel: (value: boolean) => void;
  setShowCreatePostCoffeeSheet: (value: boolean) => void;
  setCommentSheetPostId: (value: string | null) => void;
  setCommentDraft: (value: string) => void;
  setHighlightedCommentId: (value: number | null) => void;
  setShowCreateCoffeeComposer: (value: boolean) => void;
  editingCommentId: number | null;
  setCommentMenuId: (value: number | null) => void;
  setEditingCommentId: (value: number | null) => void;
  showAuthPrompt: boolean;
  setShowAuthPrompt: (value: boolean) => void;
  commentListRef: RefObject<HTMLUListElement | null>;
  comments: Array<{ id: number; post_id: string; timestamp: number }>;
  commentImagePreviewUrl: string;
  highlightedCommentId: number | null;
  handledTimelineDeepLink: boolean;
  setHandledTimelineDeepLink: (value: boolean) => void;
  posts: Array<{ id: string }>;
  navigateToTimelineReplace: () => void;
  setNotificationsLastSeenAt: Dispatch<SetStateAction<number>>;
  notificationsLastSeenAt: number;
  visibleTimelineNotifications: Array<{ timestamp: number }>;
  dismissNotificationTimersRef: MutableRefObject<number[]>;
  closeDiarySheets: () => void;
  handleRefreshTimeline: () => Promise<void>;
}) {
  useEffect(() => {
    if (!timelineActionBanner) return;
    const id = window.setTimeout(() => setTimelineActionBanner(null), 1800);
    return () => window.clearTimeout(id);
  }, [timelineActionBanner, setTimelineActionBanner]);

  useEffect(() => {
    if (activeTab !== "search" || searchMode !== "coffees" || searchFocusCoffeeProfile) {
      setSearchActiveFilterType(null);
    }
  }, [activeTab, searchFocusCoffeeProfile, searchMode, setSearchActiveFilterType]);

  useEffect(() => {
    const onEscSheet = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      if (showAuthPrompt) setShowAuthPrompt(false);
      if (showNotificationsPanel) setShowNotificationsPanel(false);
      if (showCreatePost) resetCreatePostComposer();
      if (showCreatePostCoffeeSheet) setShowCreatePostCoffeeSheet(false);
      if (commentSheetPostId) {
        setCommentSheetPostId(null);
        setCommentDraft("");
        setHighlightedCommentId(null);
      }
      if (showCreateCoffeeComposer) setShowCreateCoffeeComposer(false);
    };
    window.addEventListener("keydown", onEscSheet);
    return () => window.removeEventListener("keydown", onEscSheet);
  }, [
    commentSheetPostId,
    resetCreatePostComposer,
    setCommentDraft,
    setCommentSheetPostId,
    setHighlightedCommentId,
    setShowCreateCoffeeComposer,
    setShowCreatePostCoffeeSheet,
    setShowNotificationsPanel,
    showAuthPrompt,
    setShowAuthPrompt,
    showCreateCoffeeComposer,
    showCreatePost,
    showCreatePostCoffeeSheet,
    showNotificationsPanel
  ]);

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
  }, [editingCommentId, setCommentDraft, setCommentMenuId, setEditingCommentId]);

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
  }, [commentSheetPostId, commentListRef, comments, editingCommentId]);

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
  }, [commentListRef, commentSheetPostId, comments, highlightedCommentId]);

  useEffect(() => {
    if (handledTimelineDeepLink) return;
    const params = new URLSearchParams(window.location.search);
    const postId = params.get("postId");
    const commentIdRaw = params.get("commentId");
    const commentId = commentIdRaw ? Number(commentIdRaw) : null;
    if (!postId) return;
    if (!posts.some((post) => post.id === postId)) return;
    navigateToTimelineReplace();
    setCommentSheetPostId(postId);
    if (commentId != null && Number.isFinite(commentId)) setHighlightedCommentId(commentId);
    setHandledTimelineDeepLink(true);
    const url = new URL(window.location.href);
    url.searchParams.delete("postId");
    url.searchParams.delete("commentId");
    window.history.replaceState({}, "", url.toString());
  }, [
    handledTimelineDeepLink,
    navigateToTimelineReplace,
    posts,
    setCommentSheetPostId,
    setHandledTimelineDeepLink,
    setHighlightedCommentId
  ]);

  useEffect(
    () => () => {
      dismissNotificationTimersRef.current.forEach((id) => window.clearTimeout(id));
      dismissNotificationTimersRef.current = [];
    },
    [dismissNotificationTimersRef]
  );

  useEffect(() => {
    if (activeTab !== "diary") closeDiarySheets();
  }, [activeTab, closeDiarySheets]);

  useEffect(() => {
    if (!showNotificationsPanel) return;
    const latestSeen = visibleTimelineNotifications.reduce((max, item) => Math.max(max, item.timestamp), 0);
    if (latestSeen > notificationsLastSeenAt) {
      setNotificationsLastSeenAt(latestSeen);
      localStorage.setItem("notifications_last_seen_at", String(latestSeen));
    }
  }, [notificationsLastSeenAt, setNotificationsLastSeenAt, showNotificationsPanel, visibleTimelineNotifications]);

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
  }, [activeTab, handleRefreshTimeline]);
}
