import { type CSSProperties, useEffect, useMemo, useRef, useState } from "react";
import { BREW_METHODS } from "../../config/brew";
import { formatClock, getBrewingProcessAdvice, getBrewBaristaTipsForMethod, getBrewDialRecommendation, getBrewMethodProfile, getBrewTimeProfile, getBrewTimelineForMethod } from "../../core/brew";
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
  onAddToPantry,
  waterMl,
  setWaterMl,
  ratio,
  setRatio,
  espressoTimeSeconds,
  setEspressoTimeSeconds,
  timerSeconds,
  setTimerSeconds,
  brewRunning,
  setBrewRunning,
  selectedCoffee,
  coffeeGrams,
  onSaveResultToDiary,
  onBrewResultSaveState,
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
  /** Abre el flujo «Añadir a despensa» (elegir café existente + gramos). Si no se pasa, el + usa onAddNotFoundCoffee. */
  onAddToPantry?: () => void;
  waterMl: number;
  setWaterMl: (value: number) => void;
  ratio: number;
  setRatio: (value: number) => void;
  espressoTimeSeconds: number;
  setEspressoTimeSeconds: (value: number) => void;
  timerSeconds: number;
  setTimerSeconds: (value: number) => void;
  brewRunning: boolean;
  setBrewRunning: (value: boolean) => void;
  selectedCoffee?: CoffeeRow;
  coffeeGrams: number;
  onSaveResultToDiary: (taste: string) => Promise<void>;
  onBrewResultSaveState?: (state: { save: () => Promise<void>; canSave: boolean; saving: boolean; showGuardar: boolean }) => void;
  showBarcodeButton?: boolean;
  onBarcodeClick?: () => void;
  barcodeDetectedValue?: string | null;
  onClearBarcodeDetectedValue?: () => void;
}) {
  const [brewCoffeeQuery, setBrewCoffeeQuery] = useState("");
  const [brewSearchFocus, setBrewSearchFocus] = useState(false);
  const [resultTaste, setResultTaste] = useState("");
  const baristaCarouselRef = useRef<HTMLDivElement | null>(null);
  const prepAdviceCarouselRef = useRef<HTMLDivElement | null>(null);
  const [baristaFade, setBaristaFade] = useState({ left: false, right: false });
  const [prepFade, setPrepFade] = useState({ left: false, right: false });
  const showBrewSearchCancel = Boolean(brewCoffeeQuery || brewSearchFocus);
  const [savingResult, setSavingResult] = useState(false);
  const refs = useRef({
    selectedCoffee,
    resultTaste,
    savingResult,
    onSaveResultToDiary
  });
  refs.current = { selectedCoffee, resultTaste, savingResult, onSaveResultToDiary };
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
  const methodProfile = useMemo(() => getBrewMethodProfile(brewMethod), [brewMethod]);
  const timeProfile = useMemo(() => getBrewTimeProfile(brewMethod), [brewMethod]);
  const isEspressoMethod = useMemo(() => normalizeLookupText(brewMethod).includes("espresso"), [brewMethod]);
  const isRatioEditable = useMemo(() => {
    const key = normalizeLookupText(brewMethod);
    return !key.includes("espresso") && !key.includes("italiana") && !key.includes("turco");
  }, [brewMethod]);
  const isWaterEditable = useMemo(() => !isEspressoMethod, [isEspressoMethod]);
  const isCoffeeSliderShown = useMemo(() => isWaterEditable && !isRatioEditable, [isWaterEditable, isRatioEditable]);
  const coffeeGramsPrecise = useMemo(
    () => Number((isEspressoMethod ? waterMl / 2 : waterMl / Math.max(0.1, ratio)).toFixed(1)),
    [isEspressoMethod, ratio, waterMl]
  );
  const coffeeGramsLabel = useMemo(() => coffeeGramsPrecise.toFixed(1).replace(".", ","), [coffeeGramsPrecise]);
  const [waterDraft, setWaterDraft] = useState(String(waterMl));
  const [coffeeDraft, setCoffeeDraft] = useState(coffeeGramsPrecise.toFixed(1));
  const waterProgress = useMemo(() => {
    const min = methodProfile.waterMinMl;
    const max = methodProfile.waterMaxMl;
    return Math.max(0, Math.min(100, ((waterMl - min) / Math.max(1, max - min)) * 100));
  }, [methodProfile.waterMaxMl, methodProfile.waterMinMl, waterMl]);
  const ratioProgress = useMemo(() => {
    const min = methodProfile.ratioMin;
    const max = methodProfile.ratioMax;
    return Math.max(0, Math.min(100, ((ratio - min) / Math.max(0.1, max - min)) * 100));
  }, [methodProfile.ratioMax, methodProfile.ratioMin, ratio]);
  const coffeeSliderRange = useMemo(() => {
    if (!isCoffeeSliderShown) return { min: 1, max: 250 };
    const min = Math.max(1, methodProfile.waterMinMl / methodProfile.ratioMax);
    const max = Math.min(250, methodProfile.waterMaxMl / methodProfile.ratioMin);
    return { min: Math.ceil(min), max: Math.floor(max) };
  }, [isCoffeeSliderShown, methodProfile.waterMinMl, methodProfile.waterMaxMl, methodProfile.ratioMin, methodProfile.ratioMax]);
  const coffeeProgress = useMemo(() => {
    const { min, max } = coffeeSliderRange;
    return Math.max(0, Math.min(100, ((coffeeGramsPrecise - min) / Math.max(0.1, max - min)) * 100));
  }, [coffeeSliderRange, coffeeGramsPrecise]);
  const effectiveRatio = useMemo(() => {
    if (coffeeGramsPrecise <= 0) return ratio;
    return waterMl / coffeeGramsPrecise;
  }, [coffeeGramsPrecise, ratio, waterMl]);
  const ratioProfile = useMemo(() => {
    const span = Math.max(0.1, methodProfile.ratioMax - methodProfile.ratioMin);
    const normalized = (effectiveRatio - methodProfile.ratioMin) / span;
    if (normalized <= 0.35) return "CONCENTRADO";
    if (normalized <= 0.7) return "EQUILIBRADO";
    return "LIGERO";
  }, [effectiveRatio, methodProfile.ratioMax, methodProfile.ratioMin]);
  const technicalSummary = useMemo(() => {
    if (isEspressoMethod) {
      return "Configuración aplicada para espresso; afina con los consejos del barista.";
    }
    return "Configuración aplicada; afina molienda, vertido y cuerpo con los consejos del barista.";
  }, [isEspressoMethod]);
  const baristaTips = useMemo(
    () =>
      getBrewBaristaTipsForMethod(brewMethod, {
        ratio: effectiveRatio,
        waterMl,
        coffeeGrams: coffeeGramsPrecise,
        brewTimeSeconds: isEspressoMethod ? espressoTimeSeconds : undefined
      }),
    [brewMethod, coffeeGramsPrecise, effectiveRatio, espressoTimeSeconds, isEspressoMethod, waterMl]
  );
  const brewTimeline = useMemo(
    () => getBrewTimelineForMethod(brewMethod, waterMl, isEspressoMethod ? espressoTimeSeconds : undefined),
    [brewMethod, espressoTimeSeconds, isEspressoMethod, waterMl]
  );
  const brewTotalSeconds = useMemo(() => brewTimeline.reduce((acc, phase) => acc + phase.durationSeconds, 0), [brewTimeline]);
  const elapsedSeconds = useMemo(() => Math.max(0, Math.min(timerSeconds, brewTotalSeconds || timerSeconds)), [brewTotalSeconds, timerSeconds]);
  const timerEnded = brewStep === "brewing" && brewTotalSeconds > 0 && elapsedSeconds >= brewTotalSeconds;
  useEffect(() => {
    if (!onBrewResultSaveState) return;
    if (!timerEnded) {
      onBrewResultSaveState({ save: async () => {}, canSave: false, saving: false, showGuardar: false });
      return;
    }
    onBrewResultSaveState({
      save: async () => {
        const cur = refs.current;
        if (!cur.selectedCoffee || !cur.resultTaste || cur.savingResult) return;
        setSavingResult(true);
        try {
          await cur.onSaveResultToDiary(cur.resultTaste);
        } finally {
          setSavingResult(false);
        }
      },
      canSave: Boolean(selectedCoffee && resultTaste),
      saving: savingResult,
      showGuardar: true
    });
  }, [timerEnded, onBrewResultSaveState, selectedCoffee, resultTaste, savingResult]);
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
  const brewingProcessAdvice = useMemo(
    () => getBrewingProcessAdvice(brewMethod, isEspressoMethod ? 2 : ratio, waterMl, currentPhase.label, remainingInPhase, isEspressoMethod ? espressoTimeSeconds : undefined),
    [brewMethod, currentPhase.label, espressoTimeSeconds, isEspressoMethod, ratio, remainingInPhase, waterMl]
  );
  const brewingAdviceLines = useMemo(
    () =>
      brewingProcessAdvice
        .split(".")
        .map((line) => line.trim())
        .filter(Boolean),
    [brewingProcessAdvice]
  );
  const processAdviceCards = useMemo(
    () => [currentPhase.instruction, ...brewingAdviceLines.map((line) => `${line}.`)],
    [brewingAdviceLines, currentPhase.instruction]
  );
  const configAdviceLines = useMemo(
    () =>
      technicalSummary
        .split(".")
        .map((line) => line.trim())
        .filter(Boolean),
    [technicalSummary]
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
    if (brewStep !== "brewing") {
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
  useEffect(() => {
    const el = baristaCarouselRef.current;
    if (!el) return;
    const updateFade = () => {
      const hasOverflow = el.scrollWidth - el.clientWidth > 1;
      if (!hasOverflow) {
        setBaristaFade({ left: false, right: false });
        return;
      }
      const left = el.scrollLeft > 1;
      const right = el.scrollLeft + el.clientWidth < el.scrollWidth - 1;
      setBaristaFade({ left, right });
    };
    updateFade();
    const resizeObserver = typeof ResizeObserver !== "undefined" ? new ResizeObserver(updateFade) : null;
    resizeObserver?.observe(el);
    el.addEventListener("scroll", updateFade, { passive: true });
    window.addEventListener("resize", updateFade);
    return () => {
      resizeObserver?.disconnect();
      el.removeEventListener("scroll", updateFade);
      window.removeEventListener("resize", updateFade);
    };
  }, [baristaTips.length, brewStep]);
  useEffect(() => {
    const el = prepAdviceCarouselRef.current;
    if (!el) return;
    const updateFade = () => {
      const hasOverflow = el.scrollWidth - el.clientWidth > 1;
      if (!hasOverflow) {
        setPrepFade({ left: false, right: false });
        return;
      }
      const left = el.scrollLeft > 1;
      const right = el.scrollLeft + el.clientWidth < el.scrollWidth - 1;
      setPrepFade({ left, right });
    };
    updateFade();
    const resizeObserver = typeof ResizeObserver !== "undefined" ? new ResizeObserver(updateFade) : null;
    resizeObserver?.observe(el);
    el.addEventListener("scroll", updateFade, { passive: true });
    window.addEventListener("resize", updateFade);
    return () => {
      resizeObserver?.disconnect();
      el.removeEventListener("scroll", updateFade);
      window.removeEventListener("resize", updateFade);
    };
  }, [processAdviceCards.length, brewStep]);
  useEffect(() => {
    if (!brewMethod) return;
    const clampedWater = Math.max(methodProfile.waterMinMl, Math.min(methodProfile.waterMaxMl, Math.round(waterMl)));
    if (clampedWater !== waterMl) setWaterMl(clampedWater);
    const clampedRatio = Math.max(methodProfile.ratioMin, Math.min(methodProfile.ratioMax, ratio));
    if (isRatioEditable && Math.abs(clampedRatio - ratio) > 0.0001) setRatio(clampedRatio);
    if (!isRatioEditable && Math.abs(ratio - methodProfile.defaultRatio) > 0.0001) setRatio(methodProfile.defaultRatio);
    if (isEspressoMethod) {
      const clampedTime = Math.max(timeProfile.minSeconds, Math.min(timeProfile.maxSeconds, Math.round(espressoTimeSeconds)));
      if (clampedTime !== espressoTimeSeconds) setEspressoTimeSeconds(clampedTime);
    }
  }, [brewMethod, espressoTimeSeconds, isRatioEditable, methodProfile.defaultRatio, methodProfile.ratioMax, methodProfile.ratioMin, methodProfile.waterMaxMl, methodProfile.waterMinMl, ratio, setEspressoTimeSeconds, setRatio, setWaterMl, timeProfile.maxSeconds, timeProfile.minSeconds, waterMl]);
  const ratioLabel = useMemo(() => {
    const value = methodProfile.ratioStep < 1 ? effectiveRatio.toFixed(1) : String(Math.round(effectiveRatio));
    return `RATIO 1:${value} - ${ratioProfile.toUpperCase()}`;
  }, [effectiveRatio, methodProfile.ratioStep, ratioProfile]);

  return (
    <>
      {brewStep === "method" ? (
        <div className="brew-method-grid-native">
          {BREW_METHODS.map((method) => (
            <Button variant="plain" key={method.name} className={`brew-method-card-native ${brewMethod === method.name ? "is-active" : ""}`} onClick={() => {
              const profile = getBrewMethodProfile(method.name);
              const methodTime = getBrewTimeProfile(method.name);
              setBrewMethod(method.name);
              setWaterMl(profile.defaultWaterMl);
              setRatio(profile.defaultRatio);
              setEspressoTimeSeconds(methodTime.defaultSeconds);
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
                  onClick={onAddToPantry ?? onAddNotFoundCoffee}
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
                  {isWaterEditable ? (
                    <div>
                      <span>Agua (ml)</span>
                      <div className="brew-tech-value-field">
                        <Input
                          className="search-wide brew-tech-value-input is-water"
                          type="number"
                          inputMode="numeric"
                          min={methodProfile.waterMinMl}
                          max={methodProfile.waterMaxMl}
                          step={methodProfile.waterStepMl}
                          value={waterDraft}
                          onChange={(event) => setWaterDraft(event.target.value)}
                          onBlur={() => {
                            const parsed = Number(waterDraft);
                            if (Number.isFinite(parsed) && parsed > 0) {
                              setWaterMl(
                                Math.max(
                                  methodProfile.waterMinMl,
                                  Math.min(methodProfile.waterMaxMl, Math.round(parsed))
                                )
                              );
                            } else {
                              setWaterDraft(String(waterMl));
                            }
                          }}
                          aria-label="Agua en ml"
                        />
                      </div>
                    </div>
                  ) : null}
                  <div>
                    <span>Café (g)</span>
                    <div className="brew-tech-value-field">
                      <Input
                        className="search-wide brew-tech-value-input is-coffee"
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
                            if (isEspressoMethod) {
                              setWaterMl(
                                Math.max(
                                  methodProfile.waterMinMl,
                                  Math.min(methodProfile.waterMaxMl, Math.round(parsed * 2))
                                )
                              );
                            } else if (!isRatioEditable) {
                              const nextWater = Math.round(parsed * methodProfile.defaultRatio);
                              setWaterMl(
                                Math.max(
                                  methodProfile.waterMinMl,
                                  Math.min(methodProfile.waterMaxMl, nextWater)
                                )
                              );
                            } else {
                              const nextRatio = waterMl / parsed;
                              const normalized = methodProfile.ratioStep < 1
                                ? Math.round(nextRatio / methodProfile.ratioStep) * methodProfile.ratioStep
                                : Math.round(nextRatio);
                              setRatio(Math.max(methodProfile.ratioMin, Math.min(methodProfile.ratioMax, normalized)));
                            }
                          } else {
                            setCoffeeDraft(coffeeGramsPrecise.toFixed(1));
                          }
                        }}
                        aria-label="Café en gramos"
                      />
                    </div>
                  </div>
                  {isEspressoMethod ? (
                    <div>
                      <span>Tiempo (s)</span>
                      <div className="brew-tech-value-field">
                        <Input
                          className="search-wide brew-tech-value-input is-time"
                          type="number"
                          inputMode="numeric"
                          min={timeProfile.minSeconds}
                          max={timeProfile.maxSeconds}
                          step={1}
                          value={String(espressoTimeSeconds)}
                          onChange={(event) => {
                            const next = Number(event.target.value);
                            if (Number.isFinite(next)) setEspressoTimeSeconds(Math.max(timeProfile.minSeconds, Math.min(timeProfile.maxSeconds, Math.round(next))));
                          }}
                          aria-label="Tiempo de extracción en segundos"
                        />
                      </div>
                    </div>
                  ) : null}
                </div>

                {isWaterEditable ? (
                  <label className="brew-tech-slider">
                    <span>CANTIDAD DE AGUA</span>
                    <Input
                      className="app-range app-range--water"
                      style={{ "--range-progress": `${waterProgress}%` } as CSSProperties}
                      type="range"
                      min={methodProfile.waterMinMl}
                      max={methodProfile.waterMaxMl}
                      step={methodProfile.waterStepMl}
                      value={waterMl}
                      onChange={(event) => setWaterMl(Number(event.target.value))}
                    />
                  </label>
                ) : null}

                {isCoffeeSliderShown ? (
                  <label className="brew-tech-slider">
                    <span>{`CANTIDAD DE CAFÉ (${coffeeGramsPrecise.toFixed(1)}g)`}</span>
                    <Input
                      className="app-range app-range--coffee"
                      style={{ "--range-progress": `${coffeeProgress}%` } as CSSProperties}
                      type="range"
                      min={coffeeSliderRange.min}
                      max={coffeeSliderRange.max}
                      step={0.5}
                      value={coffeeGramsPrecise}
                      onChange={(event) => {
                        const v = Number(event.target.value);
                        if (!Number.isFinite(v)) return;
                        if (isEspressoMethod) {
                          setWaterMl(Math.max(methodProfile.waterMinMl, Math.min(methodProfile.waterMaxMl, Math.round(v * 2))));
                        } else {
                          const nextWater = Math.round(v * methodProfile.defaultRatio);
                          setWaterMl(Math.max(methodProfile.waterMinMl, Math.min(methodProfile.waterMaxMl, nextWater)));
                        }
                      }}
                    />
                  </label>
                ) : null}

                {isRatioEditable ? (
                  <label className="brew-tech-slider">
                    <span>{ratioLabel}</span>
                    <Input
                      className="app-range"
                      style={{ "--range-progress": `${ratioProgress}%` } as CSSProperties}
                      type="range"
                      min={methodProfile.ratioMin}
                      max={methodProfile.ratioMax}
                      step={methodProfile.ratioStep}
                      value={ratio}
                      onChange={(event) => setRatio(Number(event.target.value))}
                    />
                  </label>
                ) : null}

                {isEspressoMethod ? (
                  <label className="brew-tech-slider">
                    <span>{`TIEMPO DE EXTRACCIÓN (${espressoTimeSeconds}s)`}</span>
                    <Input
                      className="app-range app-range--time"
                      style={{ "--range-progress": `${Math.max(0, Math.min(100, ((espressoTimeSeconds - timeProfile.minSeconds) / Math.max(1, timeProfile.maxSeconds - timeProfile.minSeconds)) * 100))}%` } as CSSProperties}
                      type="range"
                      min={timeProfile.minSeconds}
                      max={timeProfile.maxSeconds}
                      step={1}
                      value={espressoTimeSeconds}
                      onChange={(event) => setEspressoTimeSeconds(Number(event.target.value))}
                    />
                  </label>
                ) : null}
                <div className="brew-tech-advice">
                  <UiIcon name="sparkles" className="ui-icon" />
                  <ul className="brew-tech-advice-list">
                    {configAdviceLines.map((line) => (
                      <li key={line}>{line}.</li>
                    ))}
                  </ul>
                </div>
              </article>
            </div>

            <aside className="brew-barista-block">
              <p className="section-title brew-config-heading">CONSEJOS DEL BARISTA</p>
              <div
                ref={baristaCarouselRef}
                className={`brew-barista-grid ${baristaFade.left ? "has-left-fade" : ""} ${baristaFade.right ? "has-right-fade" : ""}`.trim()}
              >
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
          <div className="brew-prep-layout">
            <article className="brew-prep-card brew-prep-card-timer">
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
            </article>
            <div className="brew-prep-tips-strip">
              {timerEnded ? (
                <article className="brew-result-card brew-result-card-inline">
                  <p className="brew-result-title">¿QUÉ SABOR HAS OBTENIDO?</p>
                  <div className="brew-result-grid">
                    {tasteOptions.map((taste) => (
                      <Button
                        variant="plain"
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
                        <strong>Recomendación</strong>
                      </div>
                      <p>{resultRecommendation}</p>
                    </div>
                  ) : null}
                </article>
              ) : (
                <>
                  <div className="brew-prep-advice-list-desktop">
                    {processAdviceCards.map((card, index) => (
                      <article className="brew-prep-advice-card" key={`desk-${index}`}>
                        {card}
                      </article>
                    ))}
                  </div>
                  <div
                    ref={prepAdviceCarouselRef}
                    className={`brew-prep-advice-carousel ${prepFade.left ? "has-left-fade" : ""} ${prepFade.right ? "has-right-fade" : ""}`.trim()}
                  >
                    {(() => {
                      const columns: string[][] = [];
                      for (let i = 0; i < processAdviceCards.length; i += 3) columns.push(processAdviceCards.slice(i, i + 3));
                      return columns.map((column, colIndex) => (
                        <div className="brew-prep-advice-page" key={`mob-${colIndex}`}>
                          {column.map((card, cardIndex) => (
                            <article className="brew-prep-advice-card" key={`mob-${colIndex}-${cardIndex}`}>
                              {card}
                            </article>
                          ))}
                        </div>
                      ));
                    })()}
                  </div>
                </>
              )}
            </div>
            {!timerEnded ? (
              <div className={`brew-prep-actions ${elapsedSeconds === 0 ? "is-single" : ""}`.trim()}>
                {elapsedSeconds > 0 ? (
                  <Button
                    variant="plain"
                    className="action-button action-button-ghost brew-prep-action-secondary"
                    onClick={() => {
                      setBrewRunning(false);
                      setTimerSeconds(0);
                    }}
                  >
                    REINICIAR
                  </Button>
                ) : null}
                <Button
                  variant="plain"
                  className={`action-button brew-prep-action-primary ${brewRunning ? "is-running" : ""}`.trim()}
                  onClick={() => setBrewRunning(!brewRunning)}
                >
                  {brewRunning ? "PAUSAR" : "INICIAR"}
                </Button>
              </div>
            ) : null}
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
  countryOptions = [],
  specialtyOptions = [],
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
  countryOptions?: string[];
  specialtyOptions?: string[];
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
      {hideActions && !fullPage ? (
        <div className="create-coffee-actions create-coffee-actions-desktop-save">
          <Button variant="plain" className="action-button" onClick={handleSaveAttempt} disabled={saving}>
            {saving ? "Guardando..." : "Guardar"}
          </Button>
        </div>
      ) : null}
    </section>
  );
}












