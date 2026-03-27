import { Button } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";
import { useI18n } from "../../i18n";

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
  const { t } = useI18n();
  return (
    <section className="join-list-view profile-users-list-view" aria-label={t("lists.join.aria")}>
      <div className="join-list-view-card">
        <div className="join-list-view-icon" aria-hidden="true">
          <UiIcon name="link" />
        </div>
        <h2 className="join-list-view-title">{t("lists.join.invited")}</h2>
        <p className="join-list-view-name">{listName}</p>
        <p className="join-list-view-owner">{t("lists.join.ownerBy", { owner: ownerUsername })}</p>
        <Button
          variant="primary"
          className="join-list-view-join-btn"
          onClick={onJoin}
          disabled={isJoining}
          aria-busy={isJoining}
        >
          {isJoining ? t("lists.join.joining") : t("lists.join.join")}
        </Button>
        <Button variant="plain" className="join-list-view-back" onClick={onBack}>
          <UiIcon name="arrow-left" aria-hidden="true" />
          {t("common.back")}
        </Button>
      </div>
    </section>
  );
}
