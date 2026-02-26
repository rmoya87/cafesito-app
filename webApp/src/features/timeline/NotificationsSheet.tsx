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
  return (
    <SheetOverlay
      className="notifications-overlay"
      role="dialog"
      aria-modal="true"
      aria-label="Notificaciones"
      onDismiss={onClose}
      onClick={onClose}
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
          {notifications.length ? (
            notifications.map((item) => {
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
            })
          ) : (
            <li className="sheet-item notifications-empty">No tienes notificaciones</li>
          )}
        </ul>
      </SheetCard>
    </SheetOverlay>
  );
}
