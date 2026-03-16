import { type Dispatch, type MutableRefObject, type SetStateAction, useEffect } from "react";

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
  showCreateCoffeeComposer,
  resetCreatePostComposer,
  setShowNotificationsPanel,
  setShowCreatePostCoffeeSheet,
  setShowCreateCoffeeComposer,
  showAuthPrompt,
  setShowAuthPrompt,
  handledTimelineDeepLink,
  setHandledTimelineDeepLink,
  posts,
  navigateToHomeReplace,
  setNotificationsLastSeenAt,
  notificationsLastSeenAt,
  visibleTimelineNotifications,
  dismissNotificationTimersRef,
  closeDiarySheets,
  handleRefreshTimeline,
  onCloseCreateCoffee
}: {
  timelineActionBanner: string | null;
  setTimelineActionBanner: (value: string | null) => void;
  activeTab: "home" | "search" | "coffee" | "brewlab" | "diary" | "profile" | "crear-cafe" | "selecciona-cafe";
  searchMode: "users" | "coffees";
  searchFocusCoffeeProfile: boolean;
  setSearchActiveFilterType: (value: "origen" | "especialidad" | "tueste" | "formato" | "nota" | null) => void;
  showNotificationsPanel: boolean;
  showCreatePost: boolean;
  showCreatePostCoffeeSheet: boolean;
  showCreateCoffeeComposer: boolean;
  resetCreatePostComposer: () => void;
  setShowNotificationsPanel: (value: boolean) => void;
  setShowCreatePostCoffeeSheet: (value: boolean) => void;
  setShowCreateCoffeeComposer: (value: boolean) => void;
  showAuthPrompt: boolean;
  setShowAuthPrompt: (value: boolean) => void;
  handledTimelineDeepLink: boolean;
  setHandledTimelineDeepLink: (value: boolean) => void;
  posts: Array<{ id: string }>;
  navigateToHomeReplace: () => void;
  setNotificationsLastSeenAt: Dispatch<SetStateAction<number>>;
  notificationsLastSeenAt: number;
  visibleTimelineNotifications: Array<{ timestamp: number }>;
  dismissNotificationTimersRef: MutableRefObject<number[]>;
  closeDiarySheets: () => void;
  handleRefreshTimeline: () => Promise<void>;
  onCloseCreateCoffee?: () => void;
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
      if (showCreateCoffeeComposer || activeTab === "crear-cafe") {
        if (onCloseCreateCoffee) onCloseCreateCoffee();
        else setShowCreateCoffeeComposer(false);
      }
    };
    window.addEventListener("keydown", onEscSheet);
    return () => window.removeEventListener("keydown", onEscSheet);
  }, [
    activeTab,
    onCloseCreateCoffee,
    resetCreatePostComposer,
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
    const hasModal = Boolean(showCreatePost || showNotificationsPanel || showCreatePostCoffeeSheet);
    const prev = document.body.style.overflow;
    if (hasModal) document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [showCreatePost, showCreatePostCoffeeSheet, showNotificationsPanel]);

  useEffect(() => {
    if (activeTab !== "search" || searchMode !== "users") return;
    const id = window.setTimeout(() => {
      document.getElementById("quick-search")?.focus();
    }, 40);
    return () => window.clearTimeout(id);
  }, [activeTab, searchMode]);

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
      if (activeTab !== "home") return;
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
