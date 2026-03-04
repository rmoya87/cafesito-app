import { useCallback, useRef, useState } from "react";

export function useNotificationsDomain() {
  const [showNotificationsPanel, setShowNotificationsPanel] = useState(false);
  const [notificationsLastSeenAt, setNotificationsLastSeenAt] = useState(() => {
    try {
      if (typeof localStorage === "undefined") return 0;
      const getItem = localStorage.getItem;
      if (typeof getItem !== "function") return 0;
      const saved = getItem.call(localStorage, "notifications_last_seen_at");
      return saved != null ? Number(saved) : 0;
    } catch {
      return 0;
    }
  });
  const [dismissedNotificationIds, setDismissedNotificationIds] = useState<Set<string>>(new Set());
  const [dismissingNotificationIds, setDismissingNotificationIds] = useState<Set<string>>(new Set());
  const dismissNotificationTimersRef = useRef<number[]>([]);

  const closeNotificationsPanel = useCallback(() => {
    const now = Date.now();
    setNotificationsLastSeenAt(now);
    try {
      if (typeof localStorage !== "undefined" && typeof localStorage.setItem === "function") {
        localStorage.setItem("notifications_last_seen_at", String(now));
      }
    } catch { /* ignore */ }
    setShowNotificationsPanel(false);
  }, []);

  const dismissNotification = useCallback((id: string, onDismissed?: (id: string) => void) => {
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
      if (onDismissed) onDismissed(id);
      dismissNotificationTimersRef.current = dismissNotificationTimersRef.current.filter((value) => value !== timer);
    }, 220);
    dismissNotificationTimersRef.current.push(timer);
  }, []);

  return {
    showNotificationsPanel,
    setShowNotificationsPanel,
    notificationsLastSeenAt,
    setNotificationsLastSeenAt,
    dismissedNotificationIds,
    dismissingNotificationIds,
    dismissNotificationTimersRef,
    closeNotificationsPanel,
    dismissNotification
  };
}

