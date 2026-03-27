import { useState } from "react";
import type { UserRow } from "../../types";
import { Button } from "../../ui/components";
import { useI18n } from "../../i18n";

/**
 * Lista de usuarios (seguidores o siguiendo) con el mismo diseño que la búsqueda de usuarios.
 * El título y botón atrás se muestran en el TopBar.
 */
export function ProfileUsersListView({
  title,
  emptyState,
  users,
  followerCounts,
  followingCounts,
  followingIds,
  onSelectUser,
  onToggleFollow
}: {
  title: string;
  emptyState: "followers" | "following";
  users: UserRow[];
  followerCounts: Map<number, number>;
  followingCounts: Map<number, number>;
  followingIds: Set<number>;
  onSelectUser: (userId: number) => void;
  onToggleFollow: (userId: number) => void;
}) {
  const { t } = useI18n();
  const [failedAvatarUrls, setFailedAvatarUrls] = useState<Set<string>>(new Set());

  return (
    <section className="profile-users-list-view" aria-label={title}>
      <div className="search-users-container">
        <ul className="search-users-list">
          {users.length ? (
            users.map((user) => (
              <li key={user.id} className="search-users-row">
                <Button variant="plain" className="search-users-link" onClick={() => onSelectUser(user.id)}>
                  {user.avatar_url && !failedAvatarUrls.has(user.avatar_url) ? (
                    <img
                      className="avatar avatar-photo search-users-avatar"
                      src={user.avatar_url}
                      alt={user.username}
                      loading="lazy"
                      decoding="async"
                      referrerPolicy="no-referrer"
                      crossOrigin="anonymous"
                      onError={() => setFailedAvatarUrls((prev) => new Set(prev).add(user.avatar_url))}
                    />
                  ) : (
                    <div className="avatar search-users-avatar-fallback" aria-hidden="true">
                      {user.username.slice(0, 2).toUpperCase()}
                    </div>
                  )}
                  <div className="search-users-copy">
                    <p className="search-users-username">{user.username}</p>
                    <p className="search-users-fullname">
                      {t("search.followersFollowing", { followers: followerCounts.get(user.id) ?? 0, following: followingCounts.get(user.id) ?? 0 })}
                    </p>
                  </div>
                </Button>
                <Button
                  variant="plain"
                  className={`action-button search-users-follow ${followingIds.has(user.id) ? "action-button-following" : "action-button-ghost"}`}
                  onClick={() => onToggleFollow(user.id)}
                >
                  {followingIds.has(user.id) ? t("search.following") : t("search.follow")}
                </Button>
              </li>
            ))
          ) : (
            <li className="search-users-empty">
              {emptyState === "followers" ? t("profile.noFollowers") : t("profile.notFollowingAnyone")}
            </li>
          )}
        </ul>
      </div>
    </section>
  );
}
