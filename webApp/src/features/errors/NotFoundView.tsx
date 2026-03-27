import { Button } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";
import { useI18n } from "../../i18n";

export function NotFoundView({
  onGoHome,
  onOpenRandomCoffee,
  randomCoffeeEnabled
}: {
  onGoHome: () => void;
  onOpenRandomCoffee: () => void;
  randomCoffeeEnabled: boolean;
}) {
  const { t } = useI18n();
  return (
    <section className="not-found-view" aria-label={t("notFound.aria")}>
      <article className="not-found-card">
        <div className="not-found-illustration" aria-hidden="true">
          <span className="not-found-code">4 0 4</span>
          <span className="not-found-cup-wrap">
            <UiIcon name="coffee-filled" className="ui-icon not-found-cup-icon" />
            <span className="not-found-steam not-found-steam-left" />
            <span className="not-found-steam not-found-steam-right" />
          </span>
        </div>
        <p className="not-found-kicker">{t("notFound.kicker")}</p>
        <h1 className="not-found-title">{t("notFound.title")}</h1>
        <p className="not-found-copy">{t("notFound.body")}</p>
        <div className="not-found-actions">
          <Button variant="primary" onClick={onGoHome}>
            {t("notFound.goHome")}
          </Button>
          <Button variant="ghost" onClick={onOpenRandomCoffee} disabled={!randomCoffeeEnabled}>
            {t("notFound.randomCoffee")}
          </Button>
        </div>
      </article>
    </section>
  );
}
