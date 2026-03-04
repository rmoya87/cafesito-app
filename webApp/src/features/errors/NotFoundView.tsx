import { Button } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";

export function NotFoundView({
  onGoHome,
  onOpenRandomCoffee,
  randomCoffeeEnabled
}: {
  onGoHome: () => void;
  onOpenRandomCoffee: () => void;
  randomCoffeeEnabled: boolean;
}) {
  return (
    <section className="not-found-view" aria-label="Pagina no encontrada">
      <article className="not-found-card">
        <div className="not-found-illustration" aria-hidden="true">
          <span className="not-found-code">4 0 4</span>
          <span className="not-found-cup-wrap">
            <UiIcon name="coffee-filled" className="ui-icon not-found-cup-icon" />
            <span className="not-found-steam not-found-steam-left" />
            <span className="not-found-steam not-found-steam-right" />
          </span>
        </div>
        <p className="not-found-kicker">ERROR 404</p>
        <h1 className="not-found-title">Se nos fue el espresso por otro filtro</h1>
        <p className="not-found-copy">
          Esta pagina no existe o cambio de taza. Tranquilo: la cafetera sigue caliente y te podemos llevar a algo mejor.
        </p>
        <div className="not-found-actions">
          <Button variant="primary" onClick={onGoHome}>
            Volver al inicio
          </Button>
          <Button variant="ghost" onClick={onOpenRandomCoffee} disabled={!randomCoffeeEnabled}>
            Sorprendeme con un cafe
          </Button>
        </div>
      </article>
    </section>
  );
}
