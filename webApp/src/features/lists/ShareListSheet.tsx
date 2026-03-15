import { cn, SheetCard, SheetHandle } from "../../ui/components";
import type { UserRow } from "../../types";

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
  return (
    <SheetCard className="diary-sheet diary-sheet-pantry-options share-list-sheet" onClick={(e) => e.stopPropagation()}>
      <SheetHandle aria-hidden="true" />
      <h2 className="share-list-sheet-title">Invitar a la lista</h2>
      <p className="share-list-sheet-hint">Elige un usuario para enviarle una invitación. Podrá ver y editar la lista cuando la acepte.</p>
      <div className="diary-sheet-list share-list-sheet-list">
        {users.length === 0 ? (
          <p className="share-list-sheet-empty">No hay otros usuarios para invitar.</p>
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
                aria-label={`Invitar a @${user.username}`}
              >
                {invitingId === user.id ? "Enviando…" : "Invitar"}
              </button>
            </div>
          ))
        )}
      </div>
    </SheetCard>
  );
}
