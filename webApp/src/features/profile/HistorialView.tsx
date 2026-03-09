import { useMemo } from "react";
import type { CoffeeRow, FinishedCoffeeRow } from "../../types";
import { Button } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";

export function HistorialView({
  finishedCoffees,
  coffeeCatalog,
  onBack,
  onOpenCoffee
}: {
  finishedCoffees: FinishedCoffeeRow[];
  coffeeCatalog: CoffeeRow[];
  onBack: () => void;
  onOpenCoffee: (coffeeId: string) => void;
}) {
  const sortedWithCoffee = useMemo(() => {
    const byId = new Map<string, CoffeeRow>();
    coffeeCatalog.forEach((c) => byId.set(c.id, c));
    return [...finishedCoffees]
      .sort((a, b) => b.finished_at - a.finished_at)
      .map((row) => ({ row, coffee: byId.get(row.coffee_id) }))
      .filter((x): x is { row: FinishedCoffeeRow; coffee: CoffeeRow } => Boolean(x.coffee));
  }, [finishedCoffees, coffeeCatalog]);

  const groupedByDate = useMemo(() => {
    const groups = new Map<string, { row: FinishedCoffeeRow; coffee: CoffeeRow }[]>();
    sortedWithCoffee.forEach((entry) => {
      const key = new Date(entry.row.finished_at).toLocaleDateString("es-ES", {
        day: "numeric",
        month: "long",
        year: "numeric"
      });
      const list = groups.get(key) ?? [];
      list.push(entry);
      groups.set(key, list);
    });
    return Array.from(groups.entries()).sort((a, b) => {
      const tA = a[1][0]?.row.finished_at ?? 0;
      const tB = b[1][0]?.row.finished_at ?? 0;
      return tB - tA;
    });
  }, [sortedWithCoffee]);

  if (sortedWithCoffee.length === 0) {
    return (
      <section className="historial-view historial-view-empty" aria-label="Historial de cafés terminados">
        <p className="historial-empty-text">No hay cafés terminados</p>
        <Button variant="plain" type="button" className="historial-back" onClick={onBack}>
          Volver
        </Button>
      </section>
    );
  }

  return (
    <section className="historial-view" aria-label="Historial de cafés terminados">
      {groupedByDate.map(([dateLabel, entries]) => (
        <div key={dateLabel} className="historial-section">
          <h2 className="historial-section-title">{dateLabel}</h2>
          <ul className="coffee-list profile-favorite-list">
            {entries.map(({ row, coffee }) => (
              <li key={`${row.coffee_id}-${row.finished_at}`} className="profile-favorite-item">
                <div
                  className="coffee-card coffee-card-interactive profile-favorite-row historial-row"
                  role="button"
                  tabIndex={0}
                  onClick={() => onOpenCoffee(row.coffee_id)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" || e.key === " ") {
                      e.preventDefault();
                      onOpenCoffee(row.coffee_id);
                    }
                  }}
                >
                  <span className="profile-favorite-media">
                    {coffee.image_url ? (
                      <img className="profile-favorite-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
                    ) : (
                      <span className="profile-favorite-image profile-favorite-fallback" aria-hidden="true">{(coffee.nombre || "C").slice(0, 1).toUpperCase()}</span>
                    )}
                  </span>
                  <span className="profile-favorite-copy">
                    <strong>{coffee.nombre ?? row.coffee_id}</strong>
                    <em>{(coffee.marca || "").toUpperCase()}</em>
                  </span>
                  <span className="historial-item-chevron" aria-hidden="true">
                    <UiIcon name="chevron-right" className="ui-icon" />
                  </span>
                </div>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  );
}
