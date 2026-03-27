import React, { useMemo, useRef, useState } from "react";
import { Button, cn, Input } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";
import type { UserRow } from "../../types";
import type { ListInvitationRow, ListMemberRow } from "../../data/supabaseApi";
import { resolveAvatarUrl } from "../../core/avatarUrl";
import { normalizeLookupText } from "../../core/text";
import { useI18n } from "../../i18n";

function getDisplayName(users: UserRow[], userId: number, isCurrentUser: boolean, selfLabel: string): string {
  if (isCurrentUser) return selfLabel;
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
  onQuickShare,
  onCopyLink,
  onRemoveMember,
  copyChipVisible,
  copyChipExiting = false,
  invitations = [],
  variant = "sheet",
  hideMemberList = false,
  canRemoveMember = true,
  visibleMemberIds
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
  onQuickShare?: () => void;
  onCopyLink: () => void;
  onRemoveMember: (userId: number) => Promise<void>;
  copyChipVisible: boolean;
  copyChipExiting?: boolean;
  variant?: "sheet" | "page";
  hideMemberList?: boolean;
  /** Si false, no se muestra el botón Eliminar en la lista de miembros (para terceros). */
  canRemoveMember?: boolean;
  /** Si se define, solo se muestran en la lista los miembros cuyo user_id está en este Set (p. ej. mutual follow en listas públicas). */
  visibleMemberIds?: Set<number>;
}) {
  const { t, locale } = useI18n();
  const [search, setSearch] = useState("");
  const [swipedUserId, setSwipedUserId] = useState<number | null>(null);
  /** IDs de miembros cuyo avatar falló al cargar; se muestra inicial en su lugar. */
  const [failedMemberAvatarIds, setFailedMemberAvatarIds] = useState<Set<number>>(new Set());
  /** IDs de usuarios en sugerencias cuyo avatar falló al cargar. */
  const [failedSuggestionAvatarIds, setFailedSuggestionAvatarIds] = useState<Set<number>>(new Set());
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
    const withOwner = !ids.includes(listOwnerId) ? [listOwnerId, ...ids] : [listOwnerId, ...ids.filter((id) => id !== listOwnerId)];
    if (!visibleMemberIds) return withOwner;
    return withOwner.filter((id) => visibleMemberIds.has(id) || id === currentUserId);
  }, [members, listOwnerId, visibleMemberIds, currentUserId]);

  const searchResultUsers = useMemo(
    () =>
      users.filter(
        (u) =>
          u.id !== currentUserId &&
          (!searchNorm ||
            normalizeLookupText(u.username).includes(searchNorm) ||
            (u.full_name && normalizeLookupText(u.full_name).includes(searchNorm)))
      ),
    [users, currentUserId, searchNorm]
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
  const l =
    locale === "es"
      ? {
          me: "Tú",
          members: "Miembros",
          searchUsers: "Buscar usuarios...",
          searchUsersAria: "Buscar usuarios para añadir",
          noUsers: "No hay usuarios que coincidan.",
          alreadyInList: "Ya está en la lista",
          invitationSent: "Invitación enviada",
          inviteTo: (u: string) => `Invitar a ${u}`,
          sending: "Enviando…",
          invite: "Invitar",
          copyInviteLink: "Copiar enlace de invitación",
          copyLink: "Copiar enlace",
          shareInviteLink: "Compartir enlace de invitación",
          share: "Compartir",
          admin: "Admin.",
          member: "Miembro",
          removeFromList: (u: string) => `Eliminar a ${u} de la lista`,
          remove: "Eliminar",
          linkCopied: "Enlace copiado"
        }
      : {
          me: "You",
          members: "Members",
          searchUsers: "Search users...",
          searchUsersAria: "Search users to add",
          noUsers: "No matching users.",
          alreadyInList: "Already in list",
          invitationSent: "Invitation sent",
          inviteTo: (u: string) => `Invite ${u}`,
          sending: "Sending…",
          invite: "Invite",
          copyInviteLink: "Copy invite link",
          copyLink: "Copy link",
          shareInviteLink: "Share invite link",
          share: "Share",
          admin: "Admin",
          member: "Member",
          removeFromList: (u: string) => `Remove ${u} from list`,
          remove: "Remove",
          linkCopied: "Link copied"
        };

  return (
    <div className={cn("list-options-members", isPage && "list-options-members--page")}>
      <h3 className="create-list-privacy-subtitle">{l.members}</h3>
      <div className="list-options-members-box">
        <div className="list-options-members-add">
          <UiIcon name="search" className="list-options-members-search-icon" aria-hidden="true" />
          <Input
            variant="search"
            type="search"
            className="list-options-members-search-input"
            placeholder={l.searchUsers}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label={l.searchUsersAria}
          />
        </div>
        {search.trim() && (
          <div className="list-options-members-suggestions">
            {searchResultUsers.length === 0 ? (
              <p className="list-options-members-suggestions-empty">{l.noUsers}</p>
            ) : (
              searchResultUsers.slice(0, 5).map((user) => {
                const alreadyMember = memberIds.has(user.id);
                const pendingInvite = pendingInviteeIds.has(user.id);
                const suggestionAvatarUrl = resolveAvatarUrl(user.avatar_url);
                const showSuggestionAvatar =
                  suggestionAvatarUrl && !failedSuggestionAvatarIds.has(user.id);
                return (
                <div key={user.id} className="list-options-members-suggestion-row">
                  <div className="list-options-members-suggestion-user">
                    {showSuggestionAvatar ? (
                      <img
                        src={suggestionAvatarUrl}
                        alt=""
                        className="list-options-members-avatar"
                        width={36}
                        height={36}
                        loading="lazy"
                        decoding="async"
                        referrerPolicy="no-referrer"
                        crossOrigin="anonymous"
                        onError={() => setFailedSuggestionAvatarIds((prev) => new Set(prev).add(user.id))}
                      />
                    ) : (
                      <span className="list-options-members-avatar-placeholder" aria-hidden="true">
                        {user.username.slice(0, 1).toUpperCase()}
                      </span>
                    )}
                    <span className="list-options-members-suggestion-name">
                      {user.full_name?.trim() || `@${user.username}`}
                    </span>
                  </div>
                  {alreadyMember ? (
                    <span className="list-options-members-already-in-list" aria-live="polite">{l.alreadyInList}</span>
                  ) : pendingInvite ? (
                    <span className="list-options-members-pending-label" aria-live="polite">{l.invitationSent}</span>
                  ) : (
                    <Button
                      variant="plain"
                      type="button"
                      className="list-options-members-invite-btn"
                      disabled={invitingId !== null}
                      onClick={() => void onInvite(user.id)}
                      aria-label={l.inviteTo(user.username)}
                    >
                      {invitingId === user.id ? l.sending : l.invite}
                    </Button>
                  )}
                </div>
              );
              })
            )}
          </div>
        )}
        <div className="list-options-members-share-actions">
          <button
            type="button"
            className="list-options-members-copy-link"
            onClick={() => {
              onCopyLink();
            }}
            aria-label={l.copyInviteLink}
          >
            <UiIcon name="link" className="list-options-members-copy-icon" aria-hidden="true" />
            <span>{l.copyLink}</span>
          </button>
          <button
            type="button"
            className="list-options-members-copy-link"
            onClick={() => {
              onQuickShare?.();
            }}
            aria-label={l.shareInviteLink}
          >
            <UiIcon name="share" className="list-options-members-copy-icon" aria-hidden="true" />
            <span>{l.share}</span>
          </button>
        </div>
        {!hideMemberList && isPage ? (
          <div className="search-users-container">
            <ul className="search-users-list">
              {displayOrder.map((userId) => {
                const isCurrentUser = userId === currentUserId;
                const isOwner = userId === listOwnerId;
                const user = resolveUser(userId);
                const rawAvatar = getAvatar(allUsersForDisplay, userId) ?? user?.avatar_url ?? null;
                const avatarUrl = resolveAvatarUrl(rawAvatar);
                const showAvatar = avatarUrl && !failedMemberAvatarIds.has(userId);
                const displayName = getDisplayName(allUsersForDisplay, userId, isCurrentUser, l.me);
                return (
                  <li key={userId} className="search-users-row">
                    <div className="search-users-link" style={{ cursor: "default", flex: 1 }}>
                      {showAvatar ? (
                        <img
                          src={avatarUrl}
                          alt=""
                          className="avatar avatar-photo search-users-avatar"
                          width={36}
                          height={36}
                          loading="lazy"
                          decoding="async"
                          referrerPolicy="no-referrer"
                          crossOrigin="anonymous"
                          onError={() => setFailedMemberAvatarIds((prev) => new Set(prev).add(userId))}
                        />
                      ) : (
                        <div className="avatar search-users-avatar-fallback" aria-hidden="true">
                          {(displayName === l.me ? "T" : (user?.username ?? "?").slice(0, 2)).toUpperCase()}
                        </div>
                      )}
                      <div className="search-users-copy">
                        <p className="search-users-username">{displayName}</p>
                        <p className="search-users-fullname">
                          {isOwner ? l.admin : l.member}
                        </p>
                      </div>
                    </div>
                    {!isOwner && canRemoveMember && (
                      <Button
                        variant="plain"
                        type="button"
                        className="list-options-members-row-delete list-options-members-row-delete--page"
                        onClick={() => void onRemoveMember(userId)}
                        aria-label={l.removeFromList(displayName)}
                      >
                        {l.remove}
                      </Button>
                    )}
                  </li>
                );
              })}
            </ul>
          </div>
        ) : !hideMemberList ? (
          <ul className="list-options-members-list">
            {displayOrder.map((userId) => {
              const isCurrentUser = userId === currentUserId;
              const isOwner = userId === listOwnerId;
              const user = resolveUser(userId);
              const rawAvatar = getAvatar(allUsersForDisplay, userId) ?? user?.avatar_url ?? null;
              const avatarUrl = resolveAvatarUrl(rawAvatar);
              const showImage = avatarUrl && !failedMemberAvatarIds.has(userId);
              const displayName = getDisplayName(allUsersForDisplay, userId, isCurrentUser, l.me);
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
                    {showImage ? (
                      <img
                        src={avatarUrl}
                        alt=""
                        className="list-options-members-avatar"
                        width={36}
                        height={36}
                        loading="lazy"
                        decoding="async"
                        referrerPolicy="no-referrer"
                        crossOrigin="anonymous"
                        onError={() => setFailedMemberAvatarIds((prev) => new Set(prev).add(userId))}
                      />
                    ) : (
                      <span className="list-options-members-avatar-placeholder" aria-hidden="true">
                        {displayName === l.me ? "T" : (user?.username ?? "?").slice(0, 1).toUpperCase()}
                      </span>
                    )}
                    <div className="list-options-members-row-info">
                      <span className="list-options-members-row-name">{displayName}</span>
                      {isOwner && (
                        <span className="list-options-members-row-role">{l.admin}</span>
                      )}
                    </div>
                    {!isOwner && (
                      <UiIcon name="chevron-right" className="list-options-members-row-chevron" aria-hidden="true" />
                    )}
                  </div>
                  {!isOwner && canRemoveMember && (
                    <button
                      type="button"
                      className="list-options-members-row-delete"
                      onClick={(e) => {
                        e.preventDefault();
                        void onRemoveMember(userId);
                        setSwipedUserId(null);
                      }}
                      aria-label={l.removeFromList(displayName)}
                    >
                      {l.remove}
                    </button>
                  )}
                </li>
              );
            })}
          </ul>
        ) : null}
      </div>
      {copyChipVisible && (
        <div
          className={cn("list-options-members-copy-chip", copyChipExiting && "hide")}
          role="status"
          aria-live="polite"
        >
          {l.linkCopied}
        </div>
      )}
    </div>
  );
}
