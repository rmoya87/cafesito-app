import { Button } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";

/**
 * Pantalla "Unirse a la lista" cuando el usuario abre un enlace de lista compartida
 * y aún no es miembro. Muestra nombre de la lista, dueño y botón para unirse.
 */
export function JoinListView({
  listName,
  ownerUsername,
  onJoin,
  onBack,
  isJoining
}: {
  listName: string;
  ownerUsername: string;
  onJoin: () => void;
  onBack: () => void;
  isJoining: boolean;
}) {
  return (
    <section className="join-list-view profile-users-list-view" aria-label="Unirse a la lista">
      <div className="join-list-view-card">
        <div className="join-list-view-icon" aria-hidden="true">
          <UiIcon name="link" />
        </div>
        <h2 className="join-list-view-title">Te han invitado a una lista</h2>
        <p className="join-list-view-name">{listName}</p>
        <p className="join-list-view-owner">de @{ownerUsername}</p>
        <Button
          variant="primary"
          className="join-list-view-join-btn"
          onClick={onJoin}
          disabled={isJoining}
          aria-busy={isJoining}
        >
          {isJoining ? "Uniendo…" : "Unirse a la lista"}
        </Button>
        <Button variant="plain" className="join-list-view-back" onClick={onBack}>
          <UiIcon name="arrow-left" aria-hidden="true" />
          Volver
        </Button>
      </div>
    </section>
  );
}
