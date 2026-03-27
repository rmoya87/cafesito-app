import { cn, SheetCard, SheetHandle } from "../../ui/components";
import type { UserRow } from "../../types";
import { useI18n } from "../../i18n";

export function ShareListSheet({
  users,
  onDismiss,
  onInvite,
  invitingId
}: {
  users: UserRow[];
  onDismiss: () => void;
  onInvite: (inviteeId: number) => Promise<void>;
  invitingId: number | null;
}) {
  const { locale } = useI18n();
  const l =
    locale === "es"
      ? {
          title: "Invitar a la lista",
          hint: "Elige un usuario para enviarle una invitación. Podrá ver y editar la lista cuando la acepte.",
          empty: "No hay otros usuarios para invitar.",
          inviteTo: (u: string) => `Invitar a @${u}`,
          sending: "Enviando…",
          invite: "Invitar"
        }
      : {
          title: "Invite to list",
          hint: "Choose a user to send an invitation. They will be able to view and edit the list after accepting.",
          empty: "There are no other users to invite.",
          inviteTo: (u: string) => `Invite @${u}`,
          sending: "Sending…",
          invite: "Invite"
        };
  return (
    <SheetCard className="diary-sheet diary-sheet-pantry-options share-list-sheet" onClick={(e) => e.stopPropagation()}>
      <SheetHandle aria-hidden="true" />
      <h2 className="share-list-sheet-title">{l.title}</h2>
      <p className="share-list-sheet-hint">{l.hint}</p>
      <div className="diary-sheet-list share-list-sheet-list">
        {users.length === 0 ? (
          <p className="share-list-sheet-empty">{l.empty}</p>
        ) : (
          users.map((user) => (
            <div key={user.id} className="share-list-sheet-row">
              <div className="share-list-sheet-user">
                {user.avatar_url ? (
                  <img src={user.avatar_url} alt="" className="share-list-sheet-avatar" />
                ) : (
                  <span className="share-list-sheet-avatar-placeholder" aria-hidden="true">
                    {user.username.slice(0, 1).toUpperCase()}
                  </span>
                )}
                <div className="share-list-sheet-user-info">
                  <span className="share-list-sheet-username">@{user.username}</span>
                  {user.full_name && user.full_name.trim() ? (
                    <span className="share-list-sheet-fullname">{user.full_name}</span>
                  ) : null}
                </div>
              </div>
              <button
                type="button"
                className={cn("share-list-sheet-invite-btn", invitingId === user.id && "is-busy")}
                disabled={invitingId !== null}
                onClick={() => void onInvite(user.id)}
                aria-label={l.inviteTo(user.username)}
              >
                {invitingId === user.id ? l.sending : l.invite}
              </button>
            </div>
          ))
        )}
      </div>
    </SheetCard>
  );
}
