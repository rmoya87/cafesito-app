import { type PointerEvent as ReactPointerEvent, useRef, useState } from "react";
import { UiIcon } from "../../ui/iconography";
import { Button } from "../../ui/components";
import type { UserRow } from "../../types";
import { useI18n } from "../../i18n";

export type TimelineNotificationItem = {
  id: string;
  type: "follow" | "comment" | "list_invite";
  userId: number;
  text: string;
  timestamp: number;
  postId?: string;
  commentId?: number;
  /** Para type "list_invite": id de la invitación (related_id en notifications_db). */
  invitationId?: string;
  /** Desde Supabase notifications_db: false = no leída, mostrar bolita */
  is_read?: boolean;
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
  onReply,
  onAcceptListInvite,
  onDeclineListInvite
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
  onAcceptListInvite?: (invitationId: string) => void;
  onDeclineListInvite?: (invitationId: string) => void;
}) {
  const { t } = useI18n();
  const [offsetX, setOffsetX] = useState(0);
  const [swipeActive, setSwipeActive] = useState(false);
  const [avatarError, setAvatarError] = useState(false);
  const pointerIdRef = useRef<number | null>(null);
  const startXRef = useRef(0);
  const startYRef = useRef(0);
  const movedRef = useRef(false);
  const contentRef = useRef<HTMLDivElement | null>(null);
  const offsetRef = useRef(0);
  const rafIdRef = useRef<number | null>(null);
  const swipeActiveRef = useRef(false);
  const threshold = -76;
  const maxDrag = -124;
  const SWIPE_THRESHOLD_PX = 3;
  const VERTICAL_LOCK_PX = 2;
  const isActionTarget = (target: EventTarget | null) =>
    target instanceof Element && Boolean(target.closest(".notifications-action"));

  const handlePointerDown = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target)) return;
    if (pointerIdRef.current != null) return;
    pointerIdRef.current = event.pointerId;
    startXRef.current = event.clientX;
    startYRef.current = event.clientY;
    movedRef.current = false;
    swipeActiveRef.current = false;
    offsetRef.current = 0;
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target)) return;
    if (pointerIdRef.current !== event.pointerId) return;
    const dx = event.clientX - startXRef.current;
    const dy = event.clientY - startYRef.current;
    const absDx = Math.abs(dx);
    const absDy = Math.abs(dy);
    if (!swipeActiveRef.current) {
      if (absDx < SWIPE_THRESHOLD_PX && absDy < SWIPE_THRESHOLD_PX) return;
      if (absDy > absDx + VERTICAL_LOCK_PX) {
        pointerIdRef.current = null;
        setOffsetX(0);
        return;
      }
      if (dx < 0 && absDx > absDy) {
        swipeActiveRef.current = true;
        movedRef.current = true;
        setSwipeActive(true);
        event.currentTarget.setPointerCapture(event.pointerId);
      } else return;
    }
    if (dx >= 0) {
      offsetRef.current = 0;
      setOffsetX(0);
      const el = contentRef.current;
      if (el) el.style.transform = "translateX(0px)";
      return;
    }
    const raw = dx;
    const withResistance = raw <= maxDrag ? maxDrag + (raw - maxDrag) * 0.25 : raw;
    const offset = Math.max(maxDrag, withResistance);
    offsetRef.current = offset;
    const el = contentRef.current;
    if (el) el.style.transform = `translateX(${offset}px)`;
    if (rafIdRef.current == null) {
      rafIdRef.current = requestAnimationFrame(() => {
        setOffsetX(offsetRef.current);
        rafIdRef.current = null;
      });
    }
  };

  const endDrag = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target)) return;
    if (pointerIdRef.current !== event.pointerId) return;
    if (rafIdRef.current != null) {
      cancelAnimationFrame(rafIdRef.current);
      rafIdRef.current = null;
    }
    pointerIdRef.current = null;
    swipeActiveRef.current = false;
    setSwipeActive(false);
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    const finalOffset = offsetRef.current;
    const el = contentRef.current;
    if (el) el.style.transform = "";
    if (finalOffset <= threshold) {
      onDismiss(item.id);
      setOffsetX(0);
      return;
    }
    setOffsetX(finalOffset);
    requestAnimationFrame(() => setOffsetX(0));
    window.setTimeout(() => { movedRef.current = false; }, 120);
  };

  return (
    <li
      className={`sheet-item notifications-item ${isUnread ? "is-unread" : ""} ${isDismissing ? "is-dismissing" : ""} ${swipeActive || offsetX < -1 ? "is-swiping" : ""}`}
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
      <div ref={contentRef} className="notifications-swipe-content" style={{ transform: `translateX(${offsetX}px)` }}>
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
              {user?.avatar_url && !avatarError ? (
                <img
                  className="avatar avatar-photo notifications-avatar"
                  src={user.avatar_url}
                  alt={user.username}
                  loading="lazy"
                  decoding="async"
                  referrerPolicy="no-referrer"
                  crossOrigin="anonymous"
                  onError={() => setAvatarError(true)}
                />
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
              {isFollowing ? t("notifications.following") : t("notifications.follow")}
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
              {t("notifications.reply")}
            </Button>
          ) : null}
          {item.type === "list_invite" && item.invitationId ? (
            <span className="notifications-action notifications-list-invite-actions" role="group" aria-label={t("notifications.inviteGroup")}>
              <Button
                variant="plain"
                className="notifications-action"
                onClick={(event) => {
                  event.stopPropagation();
                  onDeclineListInvite?.(item.invitationId!);
                }}
                aria-label={t("notifications.declineInvite")}
              >
                {t("notifications.declineInvite")}
              </Button>
              <Button
                variant="primary"
                className="notifications-action"
                onClick={(event) => {
                  event.stopPropagation();
                  onAcceptListInvite?.(item.invitationId!);
                }}
                aria-label={t("notifications.addList")}
              >
                {t("notifications.addList")}
              </Button>
            </span>
          ) : null}
        </div>
      </div>
    </li>
  );
}
