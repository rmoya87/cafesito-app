import React, { useMemo, useRef, useState } from "react";
import { Button, cn, Input } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";
import type { UserRow } from "../../types";
import type { ListInvitationRow, ListMemberRow } from "../../data/supabaseApi";
import { normalizeLookupText } from "../../core/text";

function getDisplayName(users: UserRow[], userId: number, isCurrentUser: boolean): string {
  if (isCurrentUser) return "Tú";
  const u = users.find((x) => x.id === userId);
  return u?.full_name?.trim() || (u?.username ? `@${u.username}` : "") || `#${userId}`;
}

function getAvatar(users: UserRow[], userId: number): string | null {
  return users.find((x) => x.id === userId)?.avatar_url ?? null;
}

const SWIPE_THRESHOLD = 60;

export function ListOptionsMembersBlock({
  listOwnerId,
  members,
  memberUsers,
  users,
  currentUserId,
  shareUrl,
  invitingId,
  onInvite,
  onCopyLink,
  onRemoveMember,
  copyChipVisible,
  copyChipExiting = false,
  invitations = [],
  variant = "sheet"
}: {
  listOwnerId: number;
  members: ListMemberRow[];
  memberUsers: UserRow[];
  users: UserRow[];
  currentUserId: number;
  shareUrl: string;
  invitingId: number | null;
  invitations?: ListInvitationRow[];
  onInvite: (userId: number) => Promise<void>;
  onCopyLink: () => void;
  onRemoveMember: (userId: number) => Promise<void>;
  copyChipVisible: boolean;
  copyChipExiting?: boolean;
  /** "page" usa el mismo estilo que seguidores/siguiendo (search-users-row). */
  variant?: "sheet" | "page";
}) {
  const [search, setSearch] = useState("");
  const [swipedUserId, setSwipedUserId] = useState<number | null>(null);
  const touchStartX = useRef<number | null>(null);

  const allUsersForDisplay = useMemo(() => {
    const map = new Map(users.map((u) => [u.id, u]));
    memberUsers.forEach((u) => map.set(u.id, u));
    return Array.from(map.values());
  }, [users, memberUsers]);

  const searchNorm = normalizeLookupText(search);
  const memberIds = useMemo(() => new Set(members.map((m) => m.user_id)), [members]);
  const pendingInviteeIds = useMemo(
    () => new Set(invitations.filter((i) => i.status === "pending").map((i) => i.invitee_id)),
    [invitations]
  );
  const displayOrder = useMemo(() => {
    const ids = members.map((m) => m.user_id);
    if (!ids.includes(listOwnerId)) return [listOwnerId, ...ids];
    return [listOwnerId, ...ids.filter((id) => id !== listOwnerId)];
  }, [members, listOwnerId]);

  const canInviteUsers = useMemo(
    () =>
      users.filter(
        (u) =>
          u.id !== currentUserId &&
          !memberIds.has(u.id) &&
          !pendingInviteeIds.has(u.id) &&
          (!searchNorm ||
            normalizeLookupText(u.username).includes(searchNorm) ||
            (u.full_name && normalizeLookupText(u.full_name).includes(searchNorm)))
      ),
    [users, currentUserId, memberIds, pendingInviteeIds, searchNorm]
  );

  const resolveUser = (userId: number): UserRow | undefined =>
    allUsersForDisplay.find((u) => u.id === userId);

  const handleSwipeStart = (e: React.TouchEvent, userId: number) => {
    touchStartX.current = e.touches[0].clientX;
    if (userId !== currentUserId) setSwipedUserId(userId);
  };

  const handleSwipeMove = (e: React.TouchEvent, userId: number) => {
    if (userId === currentUserId || touchStartX.current === null) return;
    const dx = e.touches[0].clientX - touchStartX.current;
    if (dx < -SWIPE_THRESHOLD) setSwipedUserId(userId);
    else if (dx > SWIPE_THRESHOLD) setSwipedUserId(null);
  };

  const handleSwipeEnd = () => {
    touchStartX.current = null;
  };

  const isPage = variant === "page";

  return (
    <div className={cn("list-options-members", isPage && "list-options-members--page")}>
      <h3 className="create-list-privacy-subtitle">Miembros</h3>
      <div className="list-options-members-box">
        <div className="list-options-members-add">
          <UiIcon name="search" className="list-options-members-search-icon" aria-hidden="true" />
          <Input
            variant="search"
            type="search"
            className="list-options-members-search-input"
            placeholder="Añadir miembro"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Buscar usuarios para añadir"
          />
        </div>
        {search.trim() && (
          <div className="list-options-members-suggestions">
            {canInviteUsers.length === 0 ? (
              <p className="list-options-members-suggestions-empty">No hay usuarios que coincidan.</p>
            ) : (
              canInviteUsers.slice(0, 5).map((user) => (
                <div key={user.id} className="list-options-members-suggestion-row">
                  <div className="list-options-members-suggestion-user">
                    {user.avatar_url ? (
                      <img src={user.avatar_url} alt="" className="list-options-members-avatar" />
                    ) : (
                      <span className="list-options-members-avatar-placeholder">
                        {user.username.slice(0, 1).toUpperCase()}
                      </span>
                    )}
                    <span className="list-options-members-suggestion-name">
                      {user.full_name?.trim() || `@${user.username}`}
                    </span>
                  </div>
                  <Button
                    variant="plain"
                    type="button"
                    className="list-options-members-invite-btn"
                    disabled={invitingId !== null}
                    onClick={() => void onInvite(user.id)}
                    aria-label={`Invitar a ${user.username}`}
                  >
                    {invitingId === user.id ? "Enviando…" : "Invitar"}
                  </Button>
                </div>
              ))
            )}
          </div>
        )}
        <button
          type="button"
          className="list-options-members-copy-link"
          onClick={() => {
            onCopyLink();
          }}
          aria-label="Copiar enlace de invitación"
        >
          <UiIcon name="link" className="list-options-members-copy-icon" aria-hidden="true" />
          <span>Copiar enlace</span>
        </button>
        {isPage ? (
          <div className="search-users-container">
            <ul className="search-users-list">
              {displayOrder.map((userId) => {
                const isCurrentUser = userId === currentUserId;
                const isOwner = userId === listOwnerId;
                const user = resolveUser(userId);
                const avatar = getAvatar(allUsersForDisplay, userId) ?? user?.avatar_url ?? null;
                const displayName = getDisplayName(allUsersForDisplay, userId, isCurrentUser);
                return (
                  <li key={userId} className="search-users-row">
                    <div className="search-users-link" style={{ cursor: "default", flex: 1 }}>
                      {avatar ? (
                        <img
                          src={avatar}
                          alt=""
                          className="avatar avatar-photo search-users-avatar"
                          loading="lazy"
                          decoding="async"
                        />
                      ) : (
                        <div className="avatar search-users-avatar-fallback" aria-hidden="true">
                          {(displayName === "Tú" ? "T" : (user?.username ?? "?").slice(0, 2)).toUpperCase()}
                        </div>
                      )}
                      <div className="search-users-copy">
                        <p className="search-users-username">{displayName}</p>
                        <p className="search-users-fullname">
                          {isOwner ? "Admin." : "Miembro"}
                        </p>
                      </div>
                    </div>
                    {!isOwner && (
                      <Button
                        variant="plain"
                        type="button"
                        className="list-options-members-row-delete list-options-members-row-delete--page"
                        onClick={() => void onRemoveMember(userId)}
                        aria-label={`Eliminar a ${displayName} de la lista`}
                      >
                        Eliminar
                      </Button>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        ) : (
          <ul className="list-options-members-list">
            {displayOrder.map((userId) => {
              const isCurrentUser = userId === currentUserId;
              const isOwner = userId === listOwnerId;
              const user = resolveUser(userId);
              const avatar = getAvatar(allUsersForDisplay, userId) ?? user?.avatar_url ?? null;
              const displayName = getDisplayName(allUsersForDisplay, userId, isCurrentUser);
              return (
                <li
                  key={userId}
                  className={cn(
                    "list-options-members-row",
                    swipedUserId === userId && "is-swiped"
                  )}
                  onTouchStart={(e) => handleSwipeStart(e, userId)}
                  onTouchMove={(e) => handleSwipeMove(e, userId)}
                  onTouchEnd={handleSwipeEnd}
                  onTouchCancel={handleSwipeEnd}
                >
                  <div className="list-options-members-row-content">
                    {avatar ? (
                      <img src={avatar} alt="" className="list-options-members-avatar" />
                    ) : (
                      <span className="list-options-members-avatar-placeholder">
                        {displayName === "Tú" ? "T" : (user?.username ?? "?").slice(0, 1).toUpperCase()}
                      </span>
                    )}
                    <div className="list-options-members-row-info">
                      <span className="list-options-members-row-name">{displayName}</span>
                      {isOwner && (
                        <span className="list-options-members-row-role">Admin.</span>
                      )}
                    </div>
                    {!isOwner && (
                      <UiIcon name="chevron-right" className="list-options-members-row-chevron" aria-hidden="true" />
                    )}
                  </div>
                  {!isOwner && (
                    <button
                      type="button"
                      className="list-options-members-row-delete"
                      onClick={(e) => {
                        e.preventDefault();
                        void onRemoveMember(userId);
                        setSwipedUserId(null);
                      }}
                      aria-label={`Eliminar a ${displayName} de la lista`}
                    >
                      Eliminar
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        )}
      </div>
      {copyChipVisible && (
        <div
          className={cn("list-options-members-copy-chip", copyChipExiting && "hide")}
          role="status"
          aria-live="polite"
        >
          Enlace copiado
        </div>
      )}
    </div>
  );
}
