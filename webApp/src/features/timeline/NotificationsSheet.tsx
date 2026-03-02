import { Fragment, useMemo } from "react";
import type { TimelineNotificationItem } from "./NotificationRow";
import { NotificationRow } from "./NotificationRow";
import type { UserRow } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { IconButton, SheetCard, SheetOverlay, Topbar } from "../../ui/components";

export function NotificationsSheet({
  open,
  notifications,
  usersById,
  notificationsLastSeenAt,
  followingIds,
  dismissingNotificationIds,
  onClose,
  onDismiss,
  onToggleFollow,
  onOpenCommentThread,
  onOpenUserProfile
}: {
  open: boolean;
  notifications: TimelineNotificationItem[];
  usersById: Map<number, UserRow>;
  notificationsLastSeenAt: number;
  followingIds: Set<number>;
  dismissingNotificationIds: Set<string>;
  onClose: () => void;
  onDismiss: (id: string) => void;
  onToggleFollow: (userId: number) => void;
  onOpenCommentThread: (postId: string, commentId: number | null) => void;
  onOpenUserProfile: (userId: number) => void;
}) {
  if (!open) return null;

  const groupedNotifications = useMemo(() => {
    const now = Date.now();
    const startOfToday = new Date(now);
    startOfToday.setHours(0, 0, 0, 0);
    const dayMs = 24 * 60 * 60 * 1000;
    const todayStartMs = startOfToday.getTime();
    const yesterdayStartMs = todayStartMs - dayMs;
    const last7StartMs = todayStartMs - dayMs * 7;
    const last30StartMs = todayStartMs - dayMs * 30;

    const buckets = [
      { key: "today", title: "Hoy", items: [] as TimelineNotificationItem[] },
      { key: "yesterday", title: "Ayer", items: [] as TimelineNotificationItem[] },
      { key: "last7", title: "Últimos 7 días", items: [] as TimelineNotificationItem[] },
      { key: "last30", title: "Últimos 30 días", items: [] as TimelineNotificationItem[] }
    ];

    notifications.forEach((item) => {
      if (item.timestamp >= todayStartMs) {
        buckets[0].items.push(item);
      } else if (item.timestamp >= yesterdayStartMs) {
        buckets[1].items.push(item);
      } else if (item.timestamp >= last7StartMs) {
        buckets[2].items.push(item);
      } else if (item.timestamp >= last30StartMs) {
        buckets[3].items.push(item);
      } else {
        buckets[3].items.push(item);
      }
    });

    return buckets.filter((bucket) => bucket.items.length > 0);
  }, [notifications]);

  return (
    <SheetOverlay
      className="notifications-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Notificaciones"
      onDismiss={onClose}
      onClick={() => {
        const isDesktop = typeof window !== "undefined" && window.matchMedia("(min-width: 1024px)").matches;
        if (isDesktop) return;
        onClose();
      }}
    >
      <SheetCard className="notifications-panel" onClick={(event) => event.stopPropagation()}>
        <Topbar centered className="topbar-timeline notifications-header">
          <div className="topbar-slot">
            <IconButton
              tone="topbar"
              className="icon-button topbar-icon-button notifications-back"
              onClick={onClose}
              aria-label="Atras"
            >
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <h2 className="title title-upper topbar-title-center topbar-brand-title notifications-title">NOTIFICACIONES</h2>
          <div className="topbar-slot topbar-slot-end">
            <IconButton tone="topbar" className="icon-button topbar-icon-button notifications-header-spacer" aria-hidden="true" tabIndex={-1}>
              <UiIcon name="notifications" className="ui-icon" />
            </IconButton>
          </div>
        </Topbar>
        <ul className="sheet-list notifications-list">
          {groupedNotifications.length ? (
            groupedNotifications.map((group, index) => (
              <Fragment key={group.key}>
                <li className={`notifications-group-heading ${index === 0 ? "is-first" : ""}`}>{group.title}</li>
                {group.items.map((item) => {
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
                      onDismiss={onDismiss}
                      onOpen={() => {
                        if (item.type === "comment" && item.postId) {
                          onOpenCommentThread(item.postId, item.commentId ?? null);
                          return;
                        }
                        onClose();
                      }}
                      onToggleFollow={onToggleFollow}
                      onOpenUserProfile={() => {
                        onOpenUserProfile(item.userId);
                      }}
                      onReply={() => {
                        if (item.type !== "comment" || !item.postId) return;
                        onOpenCommentThread(item.postId, item.commentId ?? null);
                      }}
                    />
                  );
                })}
              </Fragment>
            ))
          ) : (
            <li className="sheet-item notifications-empty">No tienes notificaciones</li>
          )}
        </ul>
      </SheetCard>
    </SheetOverlay>
  );
}
