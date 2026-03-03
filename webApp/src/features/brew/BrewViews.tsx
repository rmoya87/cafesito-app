import { type CSSProperties, useEffect, useMemo, useRef, useState } from "react";
import { BREW_METHODS } from "../../config/brew";
import { formatClock, getBrewBaristaTipsForMethod, getBrewDialRecommendation, getBrewTimelineForMethod } from "../../core/brew";
import { normalizeLookupText } from "../../core/text";
import type { BrewStep, CoffeeRow, PantryItemRow } from "../../types";
import { Button, Input, Select, Switch } from "../../ui/components";
import { UiIcon, type IconName } from "../../ui/iconography";
export function BrewLabView({
  brewStep,
  setBrewStep,
  brewMethod,
  setBrewMethod,
  brewCoffeeId,
  setBrewCoffeeId,
  coffees,
  pantryItems,
  onAddNotFoundCoffee,
  waterMl,
  setWaterMl,
  ratio,
  setRatio,
  timerSeconds,
  setTimerSeconds,
  brewRunning,
  setBrewRunning,
  selectedCoffee,
  coffeeGrams,
  onSaveResultToDiary,
  showBarcodeButton,
  onBarcodeClick,
  barcodeDetectedValue,
  onClearBarcodeDetectedValue
}: {
  brewStep: BrewStep;
  setBrewStep: (step: BrewStep) => void;
  brewMethod: string;
  setBrewMethod: (value: string) => void;
  brewCoffeeId: string;
  setBrewCoffeeId: (value: string) => void;
  coffees: CoffeeRow[];
  pantryItems: Array<{ item: PantryItemRow; coffee: CoffeeRow; total: number; remaining: number; progress: number }>;
  onAddNotFoundCoffee: () => void;
  waterMl: number;
  setWaterMl: (value: number) => void;
  ratio: number;
  setRatio: (value: number) => void;
  timerSeconds: number;
  setTimerSeconds: (value: number) => void;
  brewRunning: boolean;
  setBrewRunning: (value: boolean) => void;
  selectedCoffee?: CoffeeRow;
  coffeeGrams: number;
  onSaveResultToDiary: (taste: string) => Promise<void>;
  showBarcodeButton?: boolean;
  onBarcodeClick?: () => void;
  barcodeDetectedValue?: string | null;
  onClearBarcodeDetectedValue?: () => void;
}) {
  const [brewCoffeeQuery, setBrewCoffeeQuery] = useState("");
  const [brewSearchFocus, setBrewSearchFocus] = useState(false);
  const [resultTaste, setResultTaste] = useState("");
  const showBrewSearchCancel = Boolean(brewCoffeeQuery || brewSearchFocus);
  const [savingResult, setSavingResult] = useState(false);
  const q = normalizeLookupText(brewCoffeeQuery);
  const filteredPantry = useMemo(
    () =>
      pantryItems.filter((row) => {
        if (!q) return true;
        return normalizeLookupText(row.coffee.nombre).includes(q) || normalizeLookupText(row.coffee.marca).includes(q);
      }),
    [pantryItems, q]
  );
  const filteredSuggestions = useMemo(
    () =>
      coffees
        .filter((coffee) => {
          if (!q) return true;
          return normalizeLookupText(coffee.nombre).includes(q) || normalizeLookupText(coffee.marca).includes(q);
        })
        .slice(0, 24),
    [coffees, q]
  );
  const coffeeGramsPrecise = useMemo(() => Number((waterMl / ratio).toFixed(1)), [ratio, waterMl]);
  const coffeeGramsLabel = useMemo(() => coffeeGramsPrecise.toFixed(1).replace(".", ","), [coffeeGramsPrecise]);
  const [waterDraft, setWaterDraft] = useState(String(waterMl));
  const [coffeeDraft, setCoffeeDraft] = useState(coffeeGramsPrecise.toFixed(1));
  const waterProgress = useMemo(() => {
    const min = 50;
    const max = 1000;
    return Math.max(0, Math.min(100, ((waterMl - min) / (max - min)) * 100));
  }, [waterMl]);
  const ratioProgress = useMemo(() => {
    const min = 12;
    const max = 20;
    return Math.max(0, Math.min(100, ((ratio - min) / (max - min)) * 100));
  }, [ratio]);
  const effectiveRatio = useMemo(() => {
    if (coffeeGramsPrecise <= 0) return ratio;
    return waterMl / coffeeGramsPrecise;
  }, [coffeeGramsPrecise, ratio, waterMl]);
  const ratioProfile = useMemo(() => {
    if (effectiveRatio <= 13) return "INTENSO";
    if (effectiveRatio <= 15) return "INTENSO";
    if (effectiveRatio <= 17) return "EQUILIBRADO";
    if (effectiveRatio <= 19) return "LIGERO";
    return "Muy ligero";
  }, [effectiveRatio]);
  const brewAdvice = useMemo(() => {
    const methodKey = normalizeLookupText(brewMethod);
    if (!methodKey) return "";

    let intensity = "";
    if (methodKey.includes("espresso")) {
      if (ratio < 16) intensity = "Perfil sedoso y cuerpo ligero";
      else if (ratio < 20) intensity = "Equilibrio perfecto de dulzor";
      else intensity = "Cuerpo intenso y textura densa";
    } else if (methodKey.includes("italiana")) {
      if (ratio < 18) intensity = "Sabor suave y retrogusto limpio";
      else if (ratio < 21) intensity = "Intensidad clásica con mucho cuerpo";
      else intensity = "Textura muy robusta y concentrada";
    } else {
      if (ratio < 13) intensity = "Cuerpo muy pesado y sabores potentes";
      else if (ratio < 15) intensity = "Notas dulces muy marcadas y untuosas";
      else if (ratio < 17.5) intensity = "Extracción equilibrada y balanceada";
      else if (ratio < 19) intensity = "Mayor claridad aromática y cuerpo sutil";
      else intensity = "Perfil muy ligero con notas acuosas";
    }

    let volumeDesc = "";
    if (waterMl < 150) volumeDesc = "en formato de taza corta e intensa.";
    else if (waterMl < 300) volumeDesc = "en una taza estándar ideal para el día a día.";
    else volumeDesc = "en formato diseñado para compartir.";

    return `${intensity} ${volumeDesc}`;
  }, [brewMethod, ratio, waterMl]);
  const baristaTips = useMemo(() => getBrewBaristaTipsForMethod(brewMethod), [brewMethod]);
  const brewTimeline = useMemo(() => getBrewTimelineForMethod(brewMethod, waterMl), [brewMethod, waterMl]);
  const brewTotalSeconds = useMemo(() => brewTimeline.reduce((acc, phase) => acc + phase.durationSeconds, 0), [brewTimeline]);
  const elapsedSeconds = useMemo(() => Math.max(0, Math.min(timerSeconds, brewTotalSeconds || timerSeconds)), [brewTotalSeconds, timerSeconds]);
  const currentPhaseIndex = useMemo(() => {
    if (!brewTimeline.length) return 0;
    let elapsed = 0;
    for (let i = 0; i < brewTimeline.length; i += 1) {
      elapsed += brewTimeline[i].durationSeconds;
      if (elapsedSeconds < elapsed) return i;
    }
    return Math.max(0, brewTimeline.length - 1);
  }, [brewTimeline, elapsedSeconds]);
  const currentPhase = brewTimeline[currentPhaseIndex] ?? { label: "Listo", instruction: "Proceso completado.", durationSeconds: 0 };
  const nextPhase = brewTimeline[currentPhaseIndex + 1];
  const elapsedBeforeCurrentPhase = useMemo(() => {
    if (!brewTimeline.length) return 0;
    return brewTimeline.slice(0, currentPhaseIndex).reduce((acc, phase) => acc + phase.durationSeconds, 0);
  }, [brewTimeline, currentPhaseIndex]);
  const remainingInPhase = useMemo(
    () => Math.max(0, currentPhase.durationSeconds - Math.max(0, elapsedSeconds - elapsedBeforeCurrentPhase)),
    [currentPhase.durationSeconds, elapsedBeforeCurrentPhase, elapsedSeconds]
  );
  const resultRecommendation = useMemo(() => getBrewDialRecommendation(resultTaste), [resultTaste]);
  const tasteOptions = useMemo(
    () => [
      { label: "Amargo", icon: "taste-bitter" as IconName },
      { label: "Acido", icon: "taste-acid" as IconName },
      { label: "Equilibrado", icon: "taste-balance" as IconName },
      { label: "Salado", icon: "taste-salty" as IconName },
      { label: "Acuoso", icon: "taste-watery" as IconName },
      { label: "Aspero", icon: "grind" as IconName },
      { label: "Dulce", icon: "taste-sweet" as IconName }
    ],
    []
  );

  useEffect(() => {
    if (brewStep !== "result") {
      setResultTaste("");
      setSavingResult(false);
    }
  }, [brewStep]);
  useEffect(() => {
    if (barcodeDetectedValue != null && barcodeDetectedValue !== "") {
      setBrewCoffeeQuery(barcodeDetectedValue);
      onClearBarcodeDetectedValue?.();
    }
  }, [barcodeDetectedValue, onClearBarcodeDetectedValue]);
  useEffect(() => {
    setWaterDraft(String(waterMl));
  }, [waterMl]);
  useEffect(() => {
    setCoffeeDraft(coffeeGramsPrecise.toFixed(1));
  }, [coffeeGramsPrecise]);

  return (
    <>
      {brewStep === "method" ? (
        <div className="brew-method-grid-native">
          {BREW_METHODS.map((method) => (
            <Button variant="plain" key={method.name} className={`brew-method-card-native ${brewMethod === method.name ? "is-active" : ""}`} onClick={() => {
              setBrewMethod(method.name);
              setBrewCoffeeId("");
              setBrewStep("coffee");
            }}>
              <img src={method.icon} alt={method.name} loading="lazy" decoding="async" />
              <strong>{method.name.toUpperCase()}</strong>
            </Button>
          ))}
        </div>
      ) : null}

      {brewStep === "coffee" ? (
        <section className="brew-choose-coffee">
          <div className="brew-coffee-block">
            <p className="section-title">Tu despensa</p>
            {filteredPantry.length ? (
              <div className="brew-pantry-row">
                {filteredPantry.map((row) => (
                  <Button variant="plain"
                    key={`${row.coffee.id}-pantry`}
                    className="brew-pantry-card"
                    onClick={() => {
                      setBrewCoffeeId(row.coffee.id);
                      setBrewStep("config");
                    }}
                  >
                    {row.coffee.image_url ? (
                      <img src={row.coffee.image_url} alt={row.coffee.nombre} loading="lazy" decoding="async" />
                    ) : (
                      <span className="brew-pantry-fallback" aria-hidden="true">{row.coffee.nombre.slice(0, 1).toUpperCase()}</span>
                    )}
                    <div className="brew-pantry-body">
                      <strong>{row.coffee.nombre}</strong>
                      <small>{Math.round(row.remaining)}/{Math.round(row.total)}g</small>
                      <div className="brew-pantry-progress" aria-hidden="true">
                        <span style={{ width: `${Math.max(0, Math.min(100, row.progress * 100))}%` }} />
                      </div>
                    </div>
                  </Button>
                ))}
                <Button
                  variant="plain"
                  type="button"
                  className="brew-pantry-add-card"
                  aria-label="Añadir café a despensa"
                  onClick={onAddNotFoundCoffee}
                >
                  <span className="brew-pantry-add-main" aria-hidden="true">
                    <span className="brew-pantry-add-icon-wrap">
                      <UiIcon name="add" className="ui-icon" />
                    </span>
                  </span>
                  <span className="brew-pantry-add-footer" aria-hidden="true" />
                </Button>
              </div>
            ) : (
              <p className="coffee-sub">{q ? "No hay coincidencias en tu despensa." : "Tu despensa está vacía."}</p>
            )}
          </div>

          <div className="brew-coffee-block-head">
            <p className="section-title">Sugerencias</p>
            <Button variant="text" className="brew-add-coffee-link" onClick={onAddNotFoundCoffee}>
              <UiIcon name="add" className="ui-icon" />
              Crear mi café
            </Button>
          </div>

          <div className={`search-row-with-cancel ${showBrewSearchCancel ? "has-cancel" : ""}`.trim()}>
            <div className="search-coffee-field">
              <UiIcon name="search" className="ui-icon search-coffee-leading-icon" aria-hidden="true" />
              <Input
                variant="search"
                className="search-wide search-input-standard search-coffee-input brew-coffee-search"
                value={brewCoffeeQuery}
                onChange={(event) => setBrewCoffeeQuery(event.target.value)}
                onFocus={() => setBrewSearchFocus(true)}
                onBlur={() => setBrewSearchFocus(false)}
                placeholder="Buscar café..."
                aria-label="Buscar café"
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
              className={`search-cancel-button ${showBrewSearchCancel ? "is-visible" : ""}`}
              onClick={() => {
                setBrewCoffeeQuery("");
                setBrewSearchFocus(false);
                const el = document.activeElement;
                if (el instanceof HTMLElement) el.blur();
              }}
              aria-hidden={!showBrewSearchCancel}
              tabIndex={showBrewSearchCancel ? 0 : -1}
            >
              Cancelar
            </Button>
          </div>

          {filteredSuggestions.length ? (
            <ul className="brew-suggestions-list">
              {filteredSuggestions.map((coffee) => (
                <li key={coffee.id}>
                  <Button variant="plain"
                    type="button"
                    className={`brew-suggestion-card ${brewCoffeeId === coffee.id ? "is-active" : ""}`}
                    onClick={() => {
                      setBrewCoffeeId(coffee.id);
                      setBrewStep("config");
                    }}
                  >
                    {coffee.image_url ? (
                      <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
                    ) : (
                      <span className="brew-suggestion-fallback" aria-hidden="true">{coffee.nombre.slice(0, 1).toUpperCase()}</span>
                    )}
                    <span>
                      <strong>{coffee.nombre}</strong>
                      <small>{(coffee.marca || "").toUpperCase()}</small>
                    </span>
                    <UiIcon name="add" className="ui-icon" />
                  </Button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="coffee-sub">No hay sugerencias disponibles.</p>
          )}
        </section>
      ) : null}

      {brewStep === "config" ? (
        <section className="brew-config-native">
          <div className="brew-config-layout">
            <div className="brew-config-main">
              <p className="section-title brew-config-heading">AJUSTES TÉCNICOS</p>
              <article className="brew-tech-card">
                <div className="brew-tech-top">
                  <div>
                    <span>Agua (ml)</span>
                    <div className="brew-tech-value-field">
                      <Input
                        className="search-wide brew-tech-value-input is-water"
                        type="number"
                        inputMode="numeric"
                        min={50}
                        max={1000}
                        step={10}
                        value={waterDraft}
                        onChange={(event) => setWaterDraft(event.target.value)}
                        onBlur={() => {
                          const parsed = Number(waterDraft);
                          if (Number.isFinite(parsed) && parsed > 0) {
                            setWaterMl(Math.max(50, Math.min(1000, Math.round(parsed))));
                          } else {
                            setWaterDraft(String(waterMl));
                          }
                        }}
                        aria-label="Agua en ml"
                      />
                    </div>
                  </div>
                  <div>
                    <span>Café (g)</span>
                    <div className="brew-tech-value-field">
                      <Input
                        className="search-wide brew-tech-value-input"
                        type="number"
                        inputMode="decimal"
                        min={1}
                        max={250}
                        step={0.1}
                        value={coffeeDraft}
                        onChange={(event) => setCoffeeDraft(event.target.value)}
                        onBlur={() => {
                          const parsed = Number(coffeeDraft.replace(",", "."));
                          if (Number.isFinite(parsed) && parsed > 0) {
                            const nextRatio = waterMl / parsed;
                            setRatio(Math.max(12, Math.min(20, Math.round(nextRatio))));
                          } else {
                            setCoffeeDraft(coffeeGramsPrecise.toFixed(1));
                          }
                        }}
                        aria-label="Café en gramos"
                      />
                    </div>
                  </div>
                </div>

                <label className="brew-tech-slider">
                  <span>CANTIDAD DE AGUA</span>
                  <Input
                    className="app-range app-range--water"
                    style={{ "--range-progress": `${waterProgress}%` } as CSSProperties}
                    type="range"
                    min={50}
                    max={1000}
                    step={10}
                    value={waterMl}
                    onChange={(event) => setWaterMl(Number(event.target.value))}
                  />
                </label>

                <label className="brew-tech-slider">
                  <span>{`RATIO 1:${Math.round(effectiveRatio)} · ${ratioProfile.toUpperCase()}`}</span>
                  <Input
                    className="app-range"
                    style={{ "--range-progress": `${ratioProgress}%` } as CSSProperties}
                    type="range"
                    min={12}
                    max={20}
                    step={1}
                    value={ratio}
                    onChange={(event) => setRatio(Number(event.target.value))}
                  />
                </label>

                <div className="brew-tech-advice">
                  <UiIcon name="sparkles" className="ui-icon" />
                  <p>{brewAdvice}</p>
                </div>
              </article>
            </div>

            <aside className="brew-barista-block">
              <p className="section-title brew-config-heading">CONSEJOS DEL BARISTA</p>
              <div className="brew-barista-grid">
                {baristaTips.map((tip) => (
                  <article className="brew-barista-tip" key={`${tip.label}-${tip.value}`}>
                    <span className="brew-barista-icon" aria-hidden="true">
                      <UiIcon name={tip.icon} className={`ui-icon brew-barista-glyph brew-barista-glyph-${tip.icon}`} />
                    </span>
                    <div className="brew-barista-copy">
                      <strong>{tip.label}</strong>
                      <em>{tip.value}</em>
                    </div>
                  </article>
                ))}
              </div>
            </aside>
          </div>
        </section>
      ) : null}

      {brewStep === "brewing" ? (
        <section className="brew-prep-screen">
          <article className="brew-prep-card">
            <div className="brew-prep-head">
              <p className="brew-prep-phase">{currentPhase.label}</p>
              <strong className={`brew-prep-clock ${remainingInPhase <= 5 && brewRunning ? "is-warning" : ""}`.trim()}>
                {formatClock(remainingInPhase)}
              </strong>
              <p className="brew-prep-next">Siguiente: {nextPhase?.label ?? "Finalizar"}</p>
            </div>

            <div className="brew-prep-timeline">
              <div className="brew-prep-time-labels" aria-hidden="true">
                {brewTimeline.map((phase) => {
                  const widthWeight = Math.max(phase.durationSeconds / Math.max(1, brewTotalSeconds), 0.08);
                  return (
                    <small key={`label-${phase.label}`} style={{ flexGrow: widthWeight }}>
                      {phase.durationSeconds}s
                    </small>
                  );
                })}
              </div>
              <div className="brew-prep-bars" aria-hidden="true">
                {brewTimeline.map((phase, index) => {
                  const widthWeight = Math.max(phase.durationSeconds / Math.max(1, brewTotalSeconds), 0.08);
                  const elapsedBefore = brewTimeline.slice(0, index).reduce((acc, item) => acc + item.durationSeconds, 0);
                  const progress = elapsedSeconds <= elapsedBefore
                    ? 0
                    : elapsedSeconds >= elapsedBefore + phase.durationSeconds
                      ? 1
                      : (elapsedSeconds - elapsedBefore) / phase.durationSeconds;
                  return (
                    <span key={`bar-${phase.label}-${index}`} className="brew-prep-bar" style={{ flexGrow: widthWeight }}>
                      <i style={{ width: `${Math.max(0, Math.min(100, progress * 100))}%` }} />
                    </span>
                  );
                })}
              </div>
              <div className="brew-prep-total">
                <span>TOTAL {formatClock(brewTotalSeconds)}</span>
                <strong>{formatClock(elapsedSeconds)}</strong>
              </div>
            </div>

            <div className="brew-prep-instruction">
              <p>{currentPhase.instruction}</p>
            </div>
          </article>

          <div className={`brew-prep-actions ${elapsedSeconds > 0 ? "" : "is-single"}`.trim()}>
            {elapsedSeconds > 0 ? (
              <Button variant="plain"
                className="action-button action-button-ghost brew-prep-action-secondary"
                onClick={() => {
                  setBrewRunning(false);
                  setTimerSeconds(0);
                }}
              >
                REINICIAR
              </Button>
            ) : null}
            <Button variant="plain"
              className={`action-button brew-prep-action-primary ${brewRunning ? "is-running" : ""}`.trim()}
              onClick={() => setBrewRunning(!brewRunning)}
            >
              {brewRunning ? "PAUSAR" : "INICIAR"}
            </Button>
          </div>
        </section>
      ) : null}

      {brewStep === "result" ? (
        <section className="brew-result-screen">
          <article className="brew-result-card">
            <p className="brew-result-title">QUE SABOR HAS OBTENIDO?</p>
            <div className="brew-result-grid">
              {tasteOptions.map((taste) => (
                <Button variant="plain"
                  key={taste.label}
                  className={`brew-taste-chip ${resultTaste === taste.label ? "is-active" : ""}`.trim()}
                  onClick={() => setResultTaste(taste.label)}
                >
                  <UiIcon name={taste.icon} className="ui-icon" />
                  <span>{taste.label.toUpperCase()}</span>
                </Button>
              ))}
            </div>
            {resultRecommendation ? (
              <div className="brew-result-reco">
                <div className="brew-result-reco-head">
                  <UiIcon name="sparkles" className="ui-icon" />
                  <strong>Recomendacion</strong>
                </div>
                <p>{resultRecommendation}</p>
              </div>
            ) : null}
          </article>

          <div className="brew-result-actions">
            <Button variant="plain"
              className="action-button action-button-ghost brew-result-action-secondary"
              onClick={() => {
                setBrewRunning(false);
                setTimerSeconds(0);
                setBrewStep("method");
              }}
            >
              REINICIAR
            </Button>
            <Button variant="plain"
              className="action-button brew-result-action-primary"
              disabled={!selectedCoffee || !resultTaste || savingResult}
              onClick={async () => {
                if (!selectedCoffee || !resultTaste || savingResult) return;
                setSavingResult(true);
                try {
                  await onSaveResultToDiary(resultTaste);
                } finally {
                  setSavingResult(false);
                }
              }}
            >
              {savingResult ? "GUARDANDO..." : "GUARDAR EN DIARIO"}
            </Button>
          </div>
        </section>
      ) : null}
    </>
  );
}

export function CreateCoffeeView({
  draft,
  imagePreviewUrl,
  saving,
  error,
  countryOptions,
  specialtyOptions,
  onChange,
  onPickImage,
  onRemoveImage,
  onClose,
  onSave,
  fullPage,
  hideActions,
  hideHead,
  showQuantityField = false
}: {
  draft: {
    name: string;
    brand: string;
    specialty: string;
    country: string;
    format: string;
    roast: string;
    variety: string;
    hasCaffeine: boolean;
    totalGrams: number;
  };
  imagePreviewUrl: string;
  saving: boolean;
  error: string | null;
  countryOptions: string[];
  specialtyOptions: string[];
  onChange: (next: {
    name: string;
    brand: string;
    specialty: string;
    country: string;
    format: string;
    roast: string;
    variety: string;
    hasCaffeine: boolean;
    totalGrams: number;
  }) => void;
  onPickImage: (file: File | null, previewUrl: string) => void;
  onRemoveImage: () => void;
  onClose: () => void;
  onSave: () => void;
  fullPage: boolean;
  hideActions?: boolean;
  hideHead?: boolean;
  showQuantityField?: boolean;
}) {
  const rootRef = useRef<HTMLElement | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const [attemptedSave, setAttemptedSave] = useState(false);
  const requiredValues = [
    draft.name.trim(),
    draft.brand.trim(),
    draft.specialty.trim(),
    draft.country.trim(),
    draft.format.trim()
  ];
  const missingRequiredCount = requiredValues.filter((value) => !value).length;
  const canSave = missingRequiredCount === 0;
  const nameMissing = attemptedSave && !draft.name.trim();
  const brandMissing = attemptedSave && !draft.brand.trim();
  const specialtyMissing = attemptedSave && !draft.specialty.trim();
  const countryMissing = attemptedSave && !draft.country.trim();
  const formatMissing = attemptedSave && !draft.format.trim();

  const handleSaveAttempt = () => {
    setAttemptedSave(true);
    if (!canSave) {
      const firstInvalid = rootRef.current?.querySelector<HTMLInputElement | HTMLSelectElement>(".is-invalid .search-wide");
      firstInvalid?.focus();
      return;
    }
    onSave();
  };

  const getSpecialtyIcon = (value: string): IconName => {
    const key = normalizeLookupText(value);
    if (key.includes("arabica")) return "leaf";
    if (key.includes("mezcla") || key.includes("blend")) return "blend";
    return "specialty";
  };

  const getRoastIcon = (value: string): IconName => {
    const key = normalizeLookupText(value);
    if (key.includes("ligero") || key.includes("claro") || key.includes("light")) return "roast-light";
    if (key.includes("medio-oscuro") || key.includes("oscuro") || key.includes("dark")) return "roast-dark";
    return "roast-medium";
  };

  const roastChoices = useMemo(
    () => [
      { label: "Ligero", value: "Ligero" },
      { label: "Medio", value: "Medio" },
      { label: "Medio-oscuro", value: "Medio-oscuro" }
    ],
    []
  );

  const formatChoices = useMemo(
    () => [
      { label: "Grano", value: "Grano" },
      { label: "Molido", value: "Molido" },
      { label: "Capsula", value: "Capsula" }
    ],
    []
  );

  return (
    <section ref={rootRef} className={`create-coffee-view ${fullPage ? "is-full-page" : "is-side-panel"}`.trim()}>
      {!hideHead ? (
        <div className="create-coffee-head">
          <p className="section-title">Datos del café</p>
        </div>
      ) : null}
      <p className="create-coffee-hint">{attemptedSave ? (canSave ? "Listo para guardar" : `Faltan ${missingRequiredCount} campos obligatorios`) : ""}</p>
      <div className="create-coffee-image create-coffee-image-top create-coffee-image-card">
        <Input
          ref={imageInputRef}
          type="file"
          accept="image/*"
          className="file-input-hidden"
          onChange={(event) => {
            const file = event.target.files?.[0] ?? null;
            if (!file) return;
            const previewUrl = URL.createObjectURL(file);
            onPickImage(file, previewUrl);
            event.currentTarget.value = "";
          }}
        />
        {imagePreviewUrl ? (
          <div className="create-coffee-image-preview-wrap">
            <img src={imagePreviewUrl} alt="Previsualización café" loading="lazy" decoding="async" />
            <Button variant="plain" className="icon-button" onClick={onRemoveImage} aria-label="Quitar imagen">
              <UiIcon name="close" className="ui-icon" />
            </Button>
          </div>
        ) : (
          <Button variant="plain" className="create-coffee-image-empty" onClick={() => imageInputRef.current?.click()}>
            <span className="create-coffee-image-empty-icon" aria-hidden="true">
              <UiIcon name="camera" className="ui-icon" />
              <span className="create-coffee-image-empty-plus">+</span>
            </span>
            <span>Añadir foto</span>
          </Button>
        )}
        <div className="create-coffee-image-fields">
          <div className={`create-coffee-inline-field ${nameMissing ? "is-invalid" : ""}`.trim()}>
            <Input
              variant="default"
              className="search-wide"
              placeholder="Nombre del café"
              value={draft.name}
              onChange={(event) => onChange({ ...draft, name: event.target.value })}
              aria-invalid={nameMissing}
            />
          </div>
          <div className={`create-coffee-inline-field ${brandMissing ? "is-invalid" : ""}`.trim()}>
            <Input
              variant="default"
              className="search-wide"
              placeholder="Marca"
              value={draft.brand}
              onChange={(event) => onChange({ ...draft, brand: event.target.value })}
              aria-invalid={brandMissing}
            />
          </div>
        </div>
      </div>
      <div className="create-coffee-profile-origin">
        <p className="create-coffee-block-title">Perfil y Origen</p>
        <p className="create-coffee-block-subtitle">Especialidad</p>
        <div className={`create-coffee-choice-grid create-coffee-choice-grid-specialty ${specialtyMissing ? "is-invalid" : ""}`.trim()}>
          {specialtyOptions.length ? specialtyOptions.map((value) => (
            <Button variant="plain"
              key={value}
              className={`create-coffee-choice ${draft.specialty === value ? "is-selected" : ""}`.trim()}
              onClick={() => onChange({ ...draft, specialty: value })}
              aria-pressed={draft.specialty === value}
            >
              <UiIcon name={getSpecialtyIcon(value)} className="ui-icon" />
              <span>{value}</span>
            </Button>
          )) : (
            <p className="coffee-sub">Sin opciones</p>
          )}
        </div>
        <p className="create-coffee-block-subtitle">Tueste</p>
        <div className="create-coffee-choice-grid create-coffee-choice-grid-roast">
          {roastChoices.map((choice) => (
            <Button variant="plain"
              key={choice.value}
              className={`create-coffee-choice ${draft.roast === choice.value ? "is-selected" : ""}`.trim()}
              onClick={() => onChange({ ...draft, roast: draft.roast === choice.value ? "" : choice.value })}
              aria-pressed={draft.roast === choice.value}
            >
              <UiIcon name={getRoastIcon(choice.value)} className="ui-icon" />
              <span>{choice.label}</span>
            </Button>
          ))}
        </div>
        <label className={`create-coffee-country ${countryMissing ? "is-invalid" : ""}`.trim()}>
          <span>País</span>
          <Select
            className="search-wide"
            value={draft.country}
            onChange={(event) => onChange({ ...draft, country: event.target.value })}
            aria-invalid={countryMissing}
          >
            <option value="">Selecciona</option>
            {countryOptions.map((value) => (
              <option key={value} value={value}>{value}</option>
            ))}
          </Select>
        </label>
      </div>
      <div className={`create-coffee-format-block ${formatMissing ? "is-invalid" : ""}`.trim()}>
        <p className="create-coffee-block-title">Formato</p>
        <label className="create-coffee-caffeine-row">
          <span>¿Tiene cafeína?</span>
          <Switch
            checked={draft.hasCaffeine}
            className="create-coffee-caffeine-switch"
            onClick={() => onChange({ ...draft, hasCaffeine: !draft.hasCaffeine })}
          />
        </label>
        <p className="create-coffee-block-subtitle">Presentación</p>
        <div className="create-coffee-choice-grid create-coffee-choice-grid-format">
          {formatChoices.map((choice) => (
            <Button variant="plain"
              key={choice.value}
              className={`create-coffee-choice ${draft.format === choice.value ? "is-selected" : ""}`.trim()}
              onClick={() => onChange({ ...draft, format: choice.value })}
              aria-pressed={draft.format === choice.value}
            >
              <UiIcon
                name={
                  choice.value === "Grano"
                    ? "rugby"
                    : choice.value === "Molido"
                      ? "blend"
                      : "recycle"
                }
                className="ui-icon"
              />
              <span>{choice.label}</span>
            </Button>
          ))}
        </div>
        {showQuantityField ? (
          <>
            <p className="create-coffee-block-subtitle">Cantidad a añadir (g)</p>
            <label className="create-coffee-quantity-row">
              <Input
                variant="default"
                className="search-wide create-coffee-quantity-input"
                type="number"
                inputMode="numeric"
                min={1}
                max={5000}
                value={draft.totalGrams > 0 ? String(draft.totalGrams) : ""}
                onChange={(event) => {
                  const v = event.target.value;
                  const n = v === "" ? 0 : Math.max(0, Math.min(5000, parseInt(v, 10) || 0));
                  onChange({ ...draft, totalGrams: n });
                }}
                placeholder="250"
                aria-label="Gramos a añadir a despensa"
              />
            </label>
          </>
        ) : null}
      </div>
      {error ? <p className="create-coffee-error">{error}</p> : null}
      {!hideActions && !fullPage ? (
        <div className="create-coffee-actions">
          <Button variant="plain" className="action-button action-button-ghost" onClick={onClose} disabled={saving}>
            Cancelar
          </Button>
          <Button variant="plain" className="action-button" onClick={handleSaveAttempt} disabled={saving}>
            {saving ? "Guardando..." : "Guardar"}
          </Button>
        </div>
      ) : null}
      {!hideActions && fullPage ? (
        <Button variant="plain" className="action-button create-coffee-mobile-save" onClick={handleSaveAttempt} disabled={saving}>
          {saving ? "Guardando..." : "Guardar café"}
        </Button>
      ) : null}
    </section>
  );
}





