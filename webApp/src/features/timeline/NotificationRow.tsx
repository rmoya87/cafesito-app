import { type PointerEvent as ReactPointerEvent, useRef, useState } from "react";
import { UiIcon } from "../../ui/iconography";
import { Button } from "../../ui/components";
import type { UserRow } from "../../types";

export type TimelineNotificationItem = {
  id: string;
  type: "follow" | "comment";
  userId: number;
  text: string;
  timestamp: number;
  postId?: string;
  commentId?: number;
};

export function NotificationRow({
  item,
  user,
  isUnread,
  isFollowing,
  isDismissing,
  onDismiss,
  onOpen,
  onToggleFollow,
  onOpenUserProfile,
  onReply
}: {
  item: TimelineNotificationItem;
  user?: UserRow;
  isUnread: boolean;
  isFollowing: boolean;
  isDismissing: boolean;
  onDismiss: (id: string) => void;
  onOpen: () => void;
  onToggleFollow: (userId: number) => void;
  onOpenUserProfile: () => void;
  onReply: () => void;
}) {
  const [offsetX, setOffsetX] = useState(0);
  const pointerIdRef = useRef<number | null>(null);
  const startXRef = useRef(0);
  const movedRef = useRef(false);
  const threshold = -76;
  const maxDrag = -124;
  const isActionTarget = (target: EventTarget | null) =>
    target instanceof Element && Boolean(target.closest(".notifications-action"));

  const handlePointerDown = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target)) return;
    if (pointerIdRef.current != null) return;
    pointerIdRef.current = event.pointerId;
    startXRef.current = event.clientX;
    movedRef.current = false;
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target)) return;
    if (pointerIdRef.current !== event.pointerId) return;
    const dx = event.clientX - startXRef.current;
    if (Math.abs(dx) > 6) movedRef.current = true;
    if (dx >= 0) {
      setOffsetX(0);
      return;
    }
    setOffsetX(Math.max(maxDrag, dx));
  };

  const endDrag = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target)) return;
    if (pointerIdRef.current !== event.pointerId) return;
    pointerIdRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    if (offsetX <= threshold) {
      onDismiss(item.id);
      setOffsetX(0);
      return;
    }
    setOffsetX(0);
    window.setTimeout(() => {
      movedRef.current = false;
    }, 60);
  };

  return (
    <li
      className={`sheet-item notifications-item ${isUnread ? "is-unread" : ""} ${isDismissing ? "is-dismissing" : ""} ${offsetX < -1 ? "is-swiping" : ""}`}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={endDrag}
      onPointerCancel={endDrag}
      onClick={(event) => {
        if (isActionTarget(event.target)) return;
        if (movedRef.current || isDismissing) return;
        onOpen();
      }}
    >
      <div className="notifications-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div className="notifications-swipe-content" style={{ transform: `translateX(${offsetX}px)` }}>
        <div className="sheet-item-head notifications-item-head">
          <Button
            variant="plain"
            className="notifications-user-link"
            onClick={(event) => {
              event.stopPropagation();
              if (movedRef.current || isDismissing) {
                event.preventDefault();
                return;
              }
              onOpenUserProfile();
            }}
          >
            <div className="notifications-avatar-wrap">
              {user?.avatar_url ? (
                <img className="avatar avatar-photo notifications-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
              ) : (
                <div className="avatar notifications-avatar" aria-hidden="true">
                  {(user?.username ?? "us").slice(0, 2).toUpperCase()}
                </div>
              )}
              {isUnread ? <span className="sheet-unread-dot notifications-unread-dot" aria-hidden="true" /> : null}
            </div>
            <div className="notifications-copy">
              <p className="feed-user notifications-user">@{user?.username ?? `user${item.userId}`}</p>
              <p className="feed-meta notifications-subtitle">{item.text}</p>
            </div>
          </Button>
          {item.type === "follow" ? (
            <Button
              variant="primary"
              className={`action-button notification-follow-button notifications-action ${isFollowing ? "is-following" : ""}`}
              onClick={(event) => {
                event.stopPropagation();
                onToggleFollow(item.userId);
              }}
            >
              {isFollowing ? "SIGUIENDO" : "SEGUIR"}
            </Button>
          ) : null}
          {item.type === "comment" ? (
            <Button
              variant="primary"
              className="notifications-action notifications-reply"
              onClick={(event) => {
                event.stopPropagation();
                onReply();
              }}
            >
              RESPONDER
            </Button>
          ) : null}
        </div>
      </div>
    </li>
  );
}
