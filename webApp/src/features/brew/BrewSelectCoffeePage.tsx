import type { CSSProperties } from "react";
import { useCallback, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { sendEvent } from "../../core/ga4";
import { normalizeLookupText } from "../../core/text";
import type { CoffeeRow, PantryItemRow } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { Button, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";

export type BrewSelectCoffeePantryRow = {
  item: PantryItemRow;
  coffee: CoffeeRow;
  total: number;
  remaining: number;
  progress: number;
};

export function BrewSelectCoffeePage({
  pantryItems,
  coffeeOptions,
  brewCoffeeGrams,
  onBack,
  onSelectCoffee,
  onAddToPantry,
  onCreateCoffee,
  onUpdatePantryStock,
  onRemovePantryItem,
  onMarkPantryCoffeeFinished,
  showBarcodeButton = false,
  onBarcodeClick
}: {
  /** Ítems de despensa; debe estar ordenado por último uso (actividad del usuario), el más recientemente usado primero (a la izquierda). Mismo origen que Home: brewPantryItems. */
  pantryItems: BrewSelectCoffeePantryRow[];
  /** Opciones de café para la sección Sugerencias (catálogo + despensa). */
  coffeeOptions: CoffeeRow[];
  /** Gramos de café que el usuario va a usar en esta elaboración; si se pasa, en cada ítem de despensa se muestra restante/total (restante = stock actual − brewCoffeeGrams) en tiempo real. */
  brewCoffeeGrams?: number;
  onBack: () => void;
  /** Al seleccionar un café, se usa para elaboración y se cierra la página. Si viene de despensa, pasar pantryItemId para identificar el ítem concreto (varios pueden ser del mismo café). */
  onSelectCoffee: (coffeeId: string, pantryItemId?: string) => void;
  onAddToPantry?: () => void;
  /** Abre el flujo Crear mi café. */
  onCreateCoffee?: () => void;
  onUpdatePantryStock?: (pantryItemId: string, totalGrams: number, gramsRemaining: number) => Promise<void>;
  onRemovePantryItem?: (pantryItemId: string) => Promise<void>;
  onMarkPantryCoffeeFinished?: (pantryItemId: string) => Promise<void>;
  showBarcodeButton?: boolean;
  onBarcodeClick?: () => void;
}) {
  const [coffeeSearchQuery, setCoffeeSearchQuery] = useState("");
  const [coffeeSearchFocus, setCoffeeSearchFocus] = useState(false);
  const [pantryOptionsCoffeeId, setPantryOptionsCoffeeId] = useState<string | null>(null);
  const [pantryDeleteConfirmCoffeeId, setPantryDeleteConfirmCoffeeId] = useState<string | null>(null);
  const [pantryFinishedConfirmCoffeeId, setPantryFinishedConfirmCoffeeId] = useState<string | null>(null);
  const [stockEditCoffeeId, setStockEditCoffeeId] = useState<string | null>(null);
  const [stockEditTotal, setStockEditTotal] = useState("");
  const [stockEditRemaining, setStockEditRemaining] = useState("");
  const [savingStock, setSavingStock] = useState(false);
  const [removingStock, setRemovingStock] = useState(false);
  const [markingFinished, setMarkingFinished] = useState(false);

  const despensaScrollRef = useRef<HTMLDivElement | null>(null);
  const [carouselHasScroll, setCarouselHasScroll] = useState(false);

  const checkCarouselOverflow = useCallback(() => {
    const hasOverflow = (el: HTMLDivElement | null, threshold = 20): boolean => {
      if (!el) return false;
      const clientW = Math.round(el.getBoundingClientRect().width);
      if (clientW < 250) return false;
      const contentW = Math.round(el.scrollWidth);
      return contentW > clientW + threshold;
    };
    setCarouselHasScroll(hasOverflow(despensaScrollRef.current));
  }, []);

  useLayoutEffect(() => {
    const runCheck = () => {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => checkCarouselOverflow());
      });
    };
    runCheck();
    window.addEventListener("resize", runCheck);
    const el = despensaScrollRef.current;
    if (typeof ResizeObserver !== "undefined" && el) {
      const ro = new ResizeObserver(runCheck);
      ro.observe(el);
      const firstChild = el.firstElementChild;
      if (firstChild) {
        const roChild = new ResizeObserver(runCheck);
        roChild.observe(firstChild);
        return () => {
          ro.disconnect();
          roChild.disconnect();
          window.removeEventListener("resize", runCheck);
        };
      }
      return () => {
        ro.disconnect();
        window.removeEventListener("resize", runCheck);
      };
    }
    return () => window.removeEventListener("resize", runCheck);
  }, [checkCarouselOverflow, pantryItems.length]);

  const CAROUSEL_SCROLL_PX = 280;
  const scrollCarousel = useCallback((direction: "prev" | "next") => {
    const el = despensaScrollRef.current;
    if (!el) return;
    sendEvent("carousel_nav", { carousel_id: "brew_despensa", direction });
    const delta = direction === "prev" ? -CAROUSEL_SCROLL_PX : CAROUSEL_SCROLL_PX;
    el.scrollBy({ left: delta, behavior: "smooth" });
  }, []);

  const stockEditTarget = pantryItems.find((r) => r.item.id === stockEditCoffeeId) ?? null;
  const parsedStockTotal = Number(stockEditTotal || 0);
  const parsedStockRemaining = Number(stockEditRemaining || 0);
  const canSaveStock =
    Number.isFinite(parsedStockTotal) &&
    Number.isFinite(parsedStockRemaining) &&
    parsedStockTotal >= 1 &&
    parsedStockRemaining >= 0 &&
    parsedStockRemaining <= parsedStockTotal;
  const stockValidationMessage =
    !Number.isFinite(parsedStockTotal) || !Number.isFinite(parsedStockRemaining)
      ? "Introduce valores válidos."
      : parsedStockTotal < 1
        ? "El total debe ser mayor que 0."
        : parsedStockRemaining < 0
          ? "El restante no puede ser negativo."
          : parsedStockRemaining > parsedStockTotal
            ? "El restante no puede superar el total."
            : "";

  const hasPantryOptions =
    onUpdatePantryStock != null || onRemovePantryItem != null || onMarkPantryCoffeeFinished != null;

  const showSearchCancel = coffeeSearchQuery !== "" || coffeeSearchFocus;
  const filteredSuggestions = useMemo(() => {
    const q = normalizeLookupText(coffeeSearchQuery);
    const filtered = q
      ? coffeeOptions.filter(
          (c) => normalizeLookupText(c.nombre).includes(q) || normalizeLookupText(c.marca).includes(q)
        )
      : coffeeOptions;
    return filtered.slice(0, 10);
  }, [coffeeOptions, coffeeSearchQuery]);

  return (
    <div className="brew-select-coffee-page">
      <div className="brew-select-coffee-page-content">
        <section className="home-despensa suggestion-strip brew-select-coffee-despensa" aria-label="Tu despensa">
          <p className="section-title">TU DESPENSA</p>
          <div className="home-carousel-with-nav">
            <div ref={despensaScrollRef} className="home-despensa-scroll">
              <div className="brew-pantry-row">
                {pantryItems.map((row) => {
                  const gramsForBrew = Number(brewCoffeeGrams) || 0;
                  const remainingAfterBrew = Math.max(0, row.remaining - gramsForBrew);
                  const displayProgress = row.total > 0 ? row.remaining / row.total : 0;
                  return (
                    <div
                      key={row.item.id}
                      className="brew-pantry-card"
                      role="button"
                      tabIndex={0}
                      onClick={() => { sendEvent("button_click", { button_id: "brew_select_coffee" }); onSelectCoffee(row.coffee.id, row.item.id); }}
                      onKeyDown={(e) => {
                        if (e.key === "Enter" || e.key === " ") {
                          e.preventDefault();
                          onSelectCoffee(row.coffee.id, row.item.id);
                        }
                      }}
                    >
                      <div className="diary-pantry-top">
                        {row.coffee.image_url ? (
                          <img src={row.coffee.image_url} alt={row.coffee.nombre} loading="lazy" decoding="async" />
                        ) : (
                          <span className="brew-pantry-fallback" aria-hidden="true">
                            {row.coffee.nombre.slice(0, 1).toUpperCase()}
                          </span>
                        )}
                        {hasPantryOptions ? (
                          <Button
                            variant="plain"
                            type="button"
                            className="diary-pantry-options"
                            aria-label="Opciones"
                            onClick={(e) => {
                              e.stopPropagation();
                              setPantryOptionsCoffeeId(row.item.id);
                            }}
                          >
                            <UiIcon name="more" className="ui-icon" />
                          </Button>
                        ) : null}
                      </div>
                      <div className="brew-pantry-body">
                        <strong>{row.coffee.nombre}</strong>
                        <small
                          title={gramsForBrew > 0 ? `Stock actual ${Math.round(row.remaining)}/${Math.round(row.total)} g. Quedarían ${Math.round(remainingAfterBrew)} g tras esta elaboración` : undefined}
                          aria-label={`Stock ${Math.round(row.remaining)} de ${Math.round(row.total)} gramos${gramsForBrew > 0 ? `. Tras esta elaboración quedarían ${Math.round(remainingAfterBrew)} g` : ""}`}
                        >
                          {Math.round(row.remaining)}/{Math.round(row.total)}g
                        </small>
                        <div className="brew-pantry-progress" aria-hidden="true">
                          <span style={{ width: `${Math.max(0, Math.min(100, displayProgress * 100))}%` }} />
                        </div>
                      </div>
                    </div>
                  );
                })}
                {onAddToPantry ? (
                  <Button
                    variant="plain"
                    type="button"
                    className="brew-pantry-add-card"
                    aria-label="Añadir café a despensa"
                    onClick={() => { sendEvent("button_click", { button_id: "brew_add_to_pantry" }); onAddToPantry(); }}
                  >
                    <span className="brew-pantry-add-main" aria-hidden="true">
                      <span className="brew-pantry-add-icon-wrap">
                        <UiIcon name="add" className="ui-icon" />
                      </span>
                    </span>
                    <span className="brew-pantry-add-footer" aria-hidden="true" />
                  </Button>
                ) : null}
              </div>
            </div>
            {carouselHasScroll ? (
              <div className="home-carousel-nav">
                <button
                  type="button"
                  className="home-carousel-nav-btn home-carousel-nav-prev"
                  aria-label="Anterior"
                  onClick={() => scrollCarousel("prev")}
                >
                  <UiIcon name="arrow-left" className="ui-icon" />
                </button>
                <button
                  type="button"
                  className="home-carousel-nav-btn home-carousel-nav-next"
                  aria-label="Siguiente"
                  onClick={() => scrollCarousel("next")}
                >
                  <UiIcon name="arrow-right" className="ui-icon" />
                </button>
              </div>
            ) : null}
          </div>
        </section>

        {/* Buscador */}
        <div className={`search-row-with-cancel diary-coffee-select-search-row brew-select-coffee-search-row ${showSearchCancel ? "has-cancel" : ""}`.trim()}>
          <div className="search-coffee-field">
            <UiIcon name="search" className="ui-icon search-coffee-leading-icon" aria-hidden="true" />
            <Input
              variant="search"
              type="search"
              className="search-coffee-input"
              placeholder="Busca un café o marca"
              value={coffeeSearchQuery}
              onFocus={() => setCoffeeSearchFocus(true)}
              onBlur={() => setCoffeeSearchFocus(false)}
              onChange={(event) => setCoffeeSearchQuery(event.target.value)}
              aria-label="Buscar café o marca"
            />
            {showBarcodeButton && onBarcodeClick ? (
              <Button
                variant="plain"
                type="button"
                className="search-coffee-trailing-button"
                aria-label="Escanear código de barras"
                onClick={() => onBarcodeClick()}
              >
                <UiIcon name="barcode" className="ui-icon" />
              </Button>
            ) : null}
          </div>
          <Button
            variant="text"
            type="button"
            className={`search-cancel-button ${showSearchCancel ? "is-visible" : ""}`.trim()}
            onClick={() => {
              setCoffeeSearchQuery("");
              setCoffeeSearchFocus(false);
              const active = document.activeElement;
              if (active instanceof HTMLElement) active.blur();
            }}
            aria-hidden={!showSearchCancel}
            tabIndex={showSearchCancel ? 0 : -1}
          >
            Cancelar
          </Button>
        </div>

        {/* Sugerencias + Crear mi café */}
        <section className="diary-coffee-select-section brew-select-coffee-suggestions">
          <div className="diary-coffee-select-section-head">
            <h3 className="diary-coffee-select-section-title">SUGERENCIAS</h3>
            {onCreateCoffee ? (
              <Button
                variant="plain"
                type="button"
                className="diary-coffee-select-create-btn"
                onClick={() => { sendEvent("button_click", { button_id: "brew_create_coffee" }); onCreateCoffee(); }}
              >
                <UiIcon name="add" className="ui-icon" />
                <span>Crear mi café</span>
              </Button>
            ) : null}
          </div>
          <ul className="diary-coffee-select-list" role="listbox" aria-label="Sugerencias de café">
            {filteredSuggestions.map((coffee) => (
              <li key={coffee.id}>
                <Button
                  variant="plain"
                  type="button"
                  role="option"
                  className="diary-coffee-select-item"
                  onClick={() => { sendEvent("button_click", { button_id: "brew_select_coffee" }); onSelectCoffee(coffee.id); }}
                >
                  {coffee.image_url ? (
                    <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
                  ) : (
                    <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" decoding="async" aria-hidden="true" />
                  )}
                  <div className="diary-coffee-select-item-copy">
                    <strong>{coffee.nombre}</strong>
                    <span>{(coffee.marca || "CAFÉ").toUpperCase()}</span>
                  </div>
                </Button>
              </li>
            ))}
          </ul>
        </section>
      </div>

      {/* Modal opciones despensa */}
      {pantryOptionsCoffeeId && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Opciones despensa"
              onDismiss={() => setPantryOptionsCoffeeId(null)}
              onClick={() => setPantryOptionsCoffeeId(null)}
            >
              <SheetCard
                className="diary-sheet diary-sheet-pantry-options list-options-general-wrap"
                onClick={(event) => event.stopPropagation()}
              >
                <SheetHandle aria-hidden="true" />
                <div className="diary-sheet-list list-options-general-wrap">
                  <div className="list-options-page-section">
                    <h3 className="create-list-privacy-subtitle">Organiza</h3>
                    <div className="list-options-general-card">
                      {onUpdatePantryStock ? (
                        <Button
                          variant="plain"
                          type="button"
                          className="list-options-page-action diary-sheet-action-pantry"
                          onClick={() => {
                            const row = pantryItems.find((r) => r.item.id === pantryOptionsCoffeeId);
                            if (!row) return;
                            setStockEditTotal(String(Math.max(1, row.total)));
                            setStockEditRemaining(String(Math.max(0, row.remaining)));
                            setStockEditCoffeeId(pantryOptionsCoffeeId);
                            setPantryOptionsCoffeeId(null);
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">
                            edit
                          </span>
                          <span>Editar stock</span>
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">
                            chevron_right
                          </span>
                        </Button>
                      ) : null}
                      {onMarkPantryCoffeeFinished ? (
                        <Button
                          variant="plain"
                          type="button"
                          className="list-options-page-action diary-sheet-action-pantry"
                          onClick={() => {
                            setPantryFinishedConfirmCoffeeId(pantryOptionsCoffeeId);
                            setPantryOptionsCoffeeId(null);
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">
                            check_circle
                          </span>
                          <span>Café terminado</span>
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">
                            chevron_right
                          </span>
                        </Button>
                      ) : null}
                    </div>
                  </div>
                  <div className="list-options-page-section list-options-section-spaced">
                    <h3 className="create-list-privacy-subtitle">General</h3>
                    <div className="list-options-general-card">
                      {onRemovePantryItem ? (
                        <Button
                          variant="plain"
                          type="button"
                          className="list-options-page-action diary-sheet-action-pantry"
                          disabled={removingStock}
                          onClick={() => {
                            setPantryDeleteConfirmCoffeeId(pantryOptionsCoffeeId);
                            setPantryOptionsCoffeeId(null);
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">
                            delete
                          </span>
                          <span>Eliminar de la despensa</span>
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">
                            chevron_right
                          </span>
                        </Button>
                      ) : null}
                    </div>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}

      {/* Modal café terminado */}
      {pantryFinishedConfirmCoffeeId && onMarkPantryCoffeeFinished && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Café terminado"
              onDismiss={() => setPantryFinishedConfirmCoffeeId(null)}
              onClick={() => setPantryFinishedConfirmCoffeeId(null)}
            >
              <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(e) => e.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-delete-confirm-body">
                  <h2 className="diary-delete-confirm-title">Café terminado</h2>
                  <p className="diary-delete-confirm-text">
                    ¿Marcar este café como terminado? Se quitará de tu despensa y se guardará en Historial.
                  </p>
                  <div className="diary-delete-confirm-actions">
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-cancel"
                      onClick={() => setPantryFinishedConfirmCoffeeId(null)}
                      disabled={markingFinished}
                    >
                      Cancelar
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-submit"
                      disabled={markingFinished}
                      onClick={async () => {
                        if (markingFinished) return;
                        setMarkingFinished(true);
                        try {
                          await onMarkPantryCoffeeFinished(pantryFinishedConfirmCoffeeId);
                          setPantryFinishedConfirmCoffeeId(null);
                        } finally {
                          setMarkingFinished(false);
                        }
                      }}
                    >
                      {markingFinished ? "Guardando..." : "Confirmar"}
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}

      {/* Modal eliminar despensa */}
      {pantryDeleteConfirmCoffeeId && onRemovePantryItem && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Eliminar de la despensa"
              onDismiss={() => setPantryDeleteConfirmCoffeeId(null)}
              onClick={() => setPantryDeleteConfirmCoffeeId(null)}
            >
              <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(e) => e.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-delete-confirm-body">
                  <h2 className="diary-delete-confirm-title">Eliminar de la despensa</h2>
                  <p className="diary-delete-confirm-text">
                    ¿Estás seguro de eliminar este café? Se borrará tu stock actual.
                  </p>
                  <div className="diary-delete-confirm-actions">
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-cancel"
                      onClick={() => setPantryDeleteConfirmCoffeeId(null)}
                      disabled={removingStock}
                    >
                      Cancelar
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-submit"
                      disabled={removingStock}
                      onClick={async () => {
                        if (removingStock) return;
                        setRemovingStock(true);
                        try {
                          await onRemovePantryItem(pantryDeleteConfirmCoffeeId);
                          setPantryDeleteConfirmCoffeeId(null);
                        } finally {
                          setRemovingStock(false);
                        }
                      }}
                    >
                      {removingStock ? "Eliminando..." : "Eliminar"}
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}

      {/* Modal editar stock */}
      {stockEditTarget && onUpdatePantryStock && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Editar stock"
              onDismiss={() => setStockEditCoffeeId(null)}
              onClick={() => setStockEditCoffeeId(null)}
            >
              <SheetCard className="diary-sheet diary-stock-edit-sheet" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-stock-edit-header">
                  <strong className="sheet-title">Editar Stock</strong>
                </header>
                <div className="diary-sheet-form diary-stock-edit-form">
                  <label className="diary-stock-edit-field">
                    <span>Cantidad de café total (g)</span>
                    <Input
                      className="diary-stock-edit-value search-wide"
                      type="text"
                      inputMode="numeric"
                      value={String(Math.max(0, Number(stockEditTotal || 0)))}
                      onChange={(event) => {
                        const digitsOnly = event.target.value.replace(/[^0-9]/g, "");
                        const nextTotal = String(Number(digitsOnly || 0));
                        setStockEditTotal(nextTotal);
                        const parsedNextTotal = Number(nextTotal || 0);
                        const parsedCurrentRemaining = Number(stockEditRemaining || 0);
                        if (
                          Number.isFinite(parsedNextTotal) &&
                          parsedNextTotal >= 0 &&
                          Number.isFinite(parsedCurrentRemaining) &&
                          parsedCurrentRemaining > parsedNextTotal
                        ) {
                          setStockEditRemaining(String(parsedNextTotal));
                        }
                      }}
                    />
                    <Input
                      className="diary-stock-edit-slider app-range search-wide"
                      type="range"
                      min={0}
                      max={1000}
                      step={1}
                      value={Math.max(0, Math.min(1000, Number(stockEditTotal || 0)))}
                      style={
                        {
                          "--range-progress": `${Math.max(
                            0,
                            Math.min(100, (Math.max(0, Math.min(1000, Number(stockEditTotal || 0))) / 1000) * 100)
                          )}%`
                        } as CSSProperties
                      }
                      onChange={(event) => {
                        const nextTotal = Number(event.target.value);
                        setStockEditTotal(String(nextTotal));
                        const currentRemaining = Number(stockEditRemaining || 0);
                        if (currentRemaining > nextTotal) setStockEditRemaining(String(nextTotal));
                      }}
                    />
                  </label>
                  <label className="diary-stock-edit-field">
                    <span>Cantidad de café restante (g)</span>
                    <Input
                      className="diary-stock-edit-value search-wide"
                      type="text"
                      inputMode="numeric"
                      value={String(Math.max(0, Number(stockEditRemaining || 0)))}
                      onChange={(event) => {
                        const digitsOnly = event.target.value.replace(/[^0-9]/g, "");
                        setStockEditRemaining(String(Number(digitsOnly || 0)));
                      }}
                    />
                    <Input
                      className="diary-stock-edit-slider app-range search-wide"
                      type="range"
                      min={0}
                      max={Math.max(1, Number(stockEditTotal || 0))}
                      step={1}
                      value={Math.max(
                        0,
                        Math.min(Number(stockEditRemaining || 0), Math.max(1, Number(stockEditTotal || 0)))
                      )}
                      style={{
                        "--range-progress": `${Math.max(
                          0,
                          Math.min(
                            100,
                            (Math.max(0, Math.min(Number(stockEditRemaining || 0), Math.max(1, Number(stockEditTotal || 0)))) /
                              Math.max(1, Number(stockEditTotal || 0))) *
                              100
                          )
                        )}%`
                      } as CSSProperties}
                      onChange={(event) => setStockEditRemaining(event.target.value)}
                    />
                  </label>
                  {!canSaveStock && stockValidationMessage ? (
                    <p className="diary-inline-error">{stockValidationMessage}</p>
                  ) : null}
                  <div className="diary-sheet-form-actions diary-stock-edit-actions">
                    <Button
                      variant="plain"
                      type="button"
                      className="action-button diary-stock-edit-cancel"
                      onClick={() => setStockEditCoffeeId(null)}
                      disabled={savingStock}
                    >
                      CANCELAR
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="action-button diary-stock-edit-save"
                      disabled={savingStock || !canSaveStock}
                      onClick={async () => {
                        if (savingStock || !canSaveStock) return;
                        setSavingStock(true);
                        try {
                          await onUpdatePantryStock(stockEditTarget.item.id, parsedStockTotal, parsedStockRemaining);
                          setStockEditCoffeeId(null);
                        } finally {
                          setSavingStock(false);
                        }
                      }}
                    >
                      {savingStock ? "GUARDANDO..." : "GUARDAR"}
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
    </div>
  );
}
