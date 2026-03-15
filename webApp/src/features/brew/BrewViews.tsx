import { type CSSProperties, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { getOrderedBrewMethods } from "../../config/brew";
import type { BrewMethodItem } from "../../config/brew";
import { COFFEE_SIZE_OPTIONS, COFFEE_TIPO_OPTIONS } from "../../data/diaryBrewOptions";
import { EMPTY } from "../../core/emptyErrorStrings";
import { BREW_COFFEE_ABS_MAX_G, BREW_COFFEE_ABS_MIN_G, BREW_SLIDER_MAX_COFFEE_G, BREW_SLIDER_MAX_TIME_S, BREW_SLIDER_MAX_WATER_ML, BREW_WATER_ABS_MAX_ML, BREW_WATER_ABS_MIN_ML, formatClock, getBrewingProcessAdvice, getBrewBaristaTipsForMethod, getBrewDialRecommendation, getBrewMethodProfile, getBrewTimeProfile, getBrewTimelineForMethod } from "../../core/brew";
import { normalizeLookupText } from "../../core/text";
import type { BrewStep, CoffeeRow, PantryItemRow } from "../../types";
import { Button, Input, Select, SheetCard, SheetHandle, SheetOverlay, Switch } from "../../ui/components";
import { UiIcon, type IconName } from "../../ui/iconography";
export function BrewLabView({
  brewStep,
  setBrewStep,
  brewMethod,
  setBrewMethod,
  brewCoffeeId,
  setBrewCoffeeId,
  brewDrinkType = "Espresso",
  setBrewDrinkType,
  coffees,
  orderedBrewMethods = [],
  pantryItems,
  onAddNotFoundCoffee,
  onAddToPantry,
  waterMl,
  setWaterMl,
  ratio,
  coffeeGrams: coffeeGramsProp,
  setCoffeeGrams,
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
  onSaveWaterFromBrew,
  showBarcodeButton,
  onBarcodeClick,
  barcodeDetectedValue,
  onClearBarcodeDetectedValue,
  brewTimerEnabled = false,
  setBrewTimerEnabled
}: {
  brewStep: BrewStep;
  setBrewStep: (step: BrewStep) => void;
  brewMethod: string;
  setBrewMethod: (value: string) => void;
  brewCoffeeId: string;
  setBrewCoffeeId: (value: string) => void;
  /** Tipo de bebida (Espresso, Americano, etc.); mismo conjunto que en Mi diario. */
  brewDrinkType?: string;
  setBrewDrinkType?: (value: string) => void;
  coffees: CoffeeRow[];
  /** Métodos ordenados (más usados primero, alfabético, Otros al final). Si vacío, se usa lista por defecto. */
  orderedBrewMethods?: BrewMethodItem[];
  pantryItems: Array<{ item: PantryItemRow; coffee: CoffeeRow; total: number; remaining: number; progress: number }>;
  onAddNotFoundCoffee: () => void;
  /** Abre la modal «Selecciona café» (seleccionar/crear café para esta elaboración). */
  onAddToPantry?: () => void;
  waterMl: number;
  setWaterMl: (value: number) => void;
  ratio: number;
  coffeeGrams: number;
  setCoffeeGrams?: (value: number) => void;
  espressoTimeSeconds: number;
  setEspressoTimeSeconds: (value: number) => void;
  timerSeconds: number;
  setTimerSeconds: (value: number) => void;
  brewRunning: boolean;
  setBrewRunning: (value: boolean) => void;
  selectedCoffee?: CoffeeRow;
  onSaveResultToDiary: (taste: string, drinkType?: string) => Promise<void>;
  onBrewResultSaveState?: (state: { save: () => Promise<void>; canSave: boolean; saving: boolean; showGuardar: boolean }) => void;
  /** Al guardar desde elaboración con método Agua, registra entrada de agua en actividad. */
  onSaveWaterFromBrew?: (amountMl: number) => Promise<void>;
  showBarcodeButton?: boolean;
  onBarcodeClick?: () => void;
  barcodeDetectedValue?: string | null;
  onClearBarcodeDetectedValue?: () => void;
  brewTimerEnabled?: boolean;
  setBrewTimerEnabled?: (value: boolean) => void;
}) {
  const [brewCoffeeQuery, setBrewCoffeeQuery] = useState("");
  const [brewSearchFocus, setBrewSearchFocus] = useState(false);
  const [resultTaste, setResultTaste] = useState("");
  const baristaCarouselRef = useRef<HTMLDivElement | null>(null);
  const prepAdviceCarouselRef = useRef<HTMLDivElement | null>(null);
  const brewMethodScrollRef = useRef<HTMLDivElement | null>(null);
  const brewTipoScrollRef = useRef<HTMLDivElement | null>(null);
  const brewTamañoScrollRef = useRef<HTMLDivElement | null>(null);
  const [brewCarouselHasScroll, setBrewCarouselHasScroll] = useState({ method: false, tipo: false });
  /* Drag-to-scroll: umbral para no robar el click a los chips; rAF para movimiento fluido */
  const CAROUSEL_DRAG_THRESHOLD = 8;
  const brewCarouselDragElRef = useRef<HTMLDivElement | null>(null);
  const brewCarouselDragStartXRef = useRef(0);
  const brewCarouselDragStartScrollRef = useRef(0);
  const brewCarouselDragPointerIdRef = useRef<number | null>(null);
  const brewCarouselDragActiveRef = useRef(false);
  const brewCarouselPendingScrollRef = useRef<number | null>(null);
  const brewCarouselRafRef = useRef<number | null>(null);
  const [brewCarouselDragging, setBrewCarouselDragging] = useState(false);
  const handleBrewCarouselPointerDown = useCallback((e: React.PointerEvent, el: HTMLDivElement | null) => {
    if (!el || el.scrollWidth <= el.clientWidth) return;
    if (brewCarouselRafRef.current != null) {
      cancelAnimationFrame(brewCarouselRafRef.current);
      brewCarouselRafRef.current = null;
    }
    brewCarouselDragElRef.current = el;
    brewCarouselDragStartXRef.current = e.clientX;
    brewCarouselDragStartScrollRef.current = el.scrollLeft;
    brewCarouselDragPointerIdRef.current = e.pointerId;
    brewCarouselDragActiveRef.current = false;
    brewCarouselPendingScrollRef.current = null;
    setBrewCarouselDragging(false);
  }, []);
  const handleBrewCarouselPointerMove = useCallback((e: React.PointerEvent) => {
    const el = brewCarouselDragElRef.current;
    if (!el || brewCarouselDragPointerIdRef.current !== e.pointerId) return;
    if (!brewCarouselDragActiveRef.current) {
      if (Math.abs(e.clientX - brewCarouselDragStartXRef.current) < CAROUSEL_DRAG_THRESHOLD) return;
      e.preventDefault();
      el.setPointerCapture(e.pointerId);
      brewCarouselDragActiveRef.current = true;
      setBrewCarouselDragging(true);
    }
    e.preventDefault();
    const delta = e.clientX - brewCarouselDragStartXRef.current;
    const newLeft = brewCarouselDragStartScrollRef.current - delta;
    const clamped = Math.max(0, Math.min(el.scrollWidth - el.clientWidth, newLeft));
    brewCarouselPendingScrollRef.current = clamped;
    if (brewCarouselRafRef.current == null) {
      brewCarouselRafRef.current = requestAnimationFrame(() => {
        brewCarouselRafRef.current = null;
        const target = brewCarouselDragElRef.current;
        const pending = brewCarouselPendingScrollRef.current;
        if (target != null && pending != null) {
          target.scrollLeft = pending;
        }
      });
    }
  }, []);
  const handleBrewCarouselPointerUp = useCallback((e: React.PointerEvent) => {
    if (brewCarouselDragPointerIdRef.current !== e.pointerId) return;
    const el = brewCarouselDragElRef.current;
    if (brewCarouselRafRef.current != null) {
      cancelAnimationFrame(brewCarouselRafRef.current);
      brewCarouselRafRef.current = null;
    }
    const pending = brewCarouselPendingScrollRef.current;
    if (el != null && pending != null) {
      el.scrollLeft = pending;
    }
    brewCarouselDragPointerIdRef.current = null;
    brewCarouselDragElRef.current = null;
    brewCarouselDragActiveRef.current = false;
    brewCarouselPendingScrollRef.current = null;
    setBrewCarouselDragging(false);
    if (el?.hasPointerCapture(e.pointerId)) el.releasePointerCapture(e.pointerId);
  }, []);
  const [showBaristaPopover, setShowBaristaPopover] = useState(false);
  /** Tamaño elegido por clic en la strip; hace que el borde marrón se mantenga hasta cambiar de método o elegir otro tamaño */
  const [selectedSizeMl, setSelectedSizeMl] = useState<number | null>(null);
  const checkBrewCarouselOverflow = useCallback(() => {
    const hasOverflow = (el: HTMLDivElement | null, threshold = 2): boolean => {
      if (!el) return false;
      const clientW = Math.round(el.getBoundingClientRect().width);
      if (clientW < 250) return false;
      const contentW = Math.round(el.scrollWidth);
      return contentW > clientW + threshold;
    };
    setBrewCarouselHasScroll({
      method: hasOverflow(brewMethodScrollRef.current),
      tipo: hasOverflow(brewTipoScrollRef.current)
    });
  }, []);
  useLayoutEffect(() => {
    if (brewStep !== "method") return;
    const runCheck = () => {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => checkBrewCarouselOverflow());
      });
    };
    runCheck();
    window.addEventListener("resize", runCheck);
    const refsToObserve = [brewMethodScrollRef, brewTipoScrollRef];
    const observers: ResizeObserver[] = [];
    if (typeof ResizeObserver !== "undefined") {
      refsToObserve.forEach((ref) => {
        const el = ref.current;
        if (!el) return;
        const ro = new ResizeObserver(runCheck);
        ro.observe(el);
        observers.push(ro);
        const firstChild = el.firstElementChild;
        if (firstChild) {
          const roChild = new ResizeObserver(runCheck);
          roChild.observe(firstChild);
          observers.push(roChild);
        }
      });
    }
    return () => {
      window.removeEventListener("resize", runCheck);
      observers.forEach((ro) => ro.disconnect());
    };
  }, [brewStep, checkBrewCarouselOverflow, pantryItems.length]);
  const CAROUSEL_SCROLL_PX = 280;
  const scrollBrewCarousel = useCallback((ref: React.RefObject<HTMLDivElement | null>, direction: "prev" | "next") => {
    const el = ref.current;
    if (!el) return;
    const delta = direction === "prev" ? -CAROUSEL_SCROLL_PX : CAROUSEL_SCROLL_PX;
    el.scrollBy({ left: delta, behavior: "smooth" });
  }, []);
  const [baristaFade, setBaristaFade] = useState({ left: false, right: false });
  const [prepFade, setPrepFade] = useState({ left: false, right: false });
  const showBrewSearchCancel = Boolean(brewCoffeeQuery || brewSearchFocus);
  const [savingResult, setSavingResult] = useState(false);
  const refs = useRef({
    selectedCoffee,
    resultTaste,
    savingResult,
    onSaveResultToDiary,
    waterMl: 0,
    onSaveWaterFromBrew: undefined as ((amountMl: number) => Promise<void>) | undefined,
    brewDrinkType: "Espresso"
  });
  refs.current = { selectedCoffee, resultTaste, savingResult, onSaveResultToDiary, waterMl, onSaveWaterFromBrew, brewDrinkType };
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
  /* Perfil, tiempos y consejos dependen del método seleccionado (brewMethod). */
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
    () => (isEspressoMethod ? Number((waterMl / 2).toFixed(1)) : Number(coffeeGramsProp.toFixed(1))),
    [isEspressoMethod, waterMl, coffeeGramsProp]
  );
  const coffeeGramsLabel = useMemo(() => coffeeGramsPrecise.toFixed(1).replace(".", ","), [coffeeGramsPrecise]);
  const [waterDraft, setWaterDraft] = useState(String(waterMl));
  const [coffeeDraft, setCoffeeDraft] = useState(coffeeGramsPrecise.toFixed(1));
  const waterProgress = useMemo(() => {
    const min = BREW_WATER_ABS_MIN_ML;
    const max = BREW_SLIDER_MAX_WATER_ML;
    return Math.max(0, Math.min(100, ((waterMl - min) / Math.max(1, max - min)) * 100));
  }, [waterMl]);
  const ratioProgress = useMemo(() => {
    const min = methodProfile.ratioMin;
    const max = methodProfile.ratioMax;
    return Math.max(0, Math.min(100, ((ratio - min) / Math.max(0.1, max - min)) * 100));
  }, [methodProfile.ratioMax, methodProfile.ratioMin, ratio]);
  const coffeeSliderRange = useMemo(() => ({ min: BREW_COFFEE_ABS_MIN_G, max: BREW_SLIDER_MAX_COFFEE_G }), []);
  const coffeeProgress = useMemo(() => {
    const { min, max } = coffeeSliderRange;
    return Math.max(0, Math.min(100, ((coffeeGramsPrecise - min) / Math.max(0.1, max - min)) * 100));
  }, [coffeeSliderRange, coffeeGramsPrecise]);
  const timeSliderMax = Math.min(BREW_SLIDER_MAX_TIME_S, timeProfile.maxSeconds);
  const timeSliderMin = Math.min(timeProfile.minSeconds, timeSliderMax);
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
  const isResultStep = brewStep === "result" || timerEnded;
  useEffect(() => {
    if (!onBrewResultSaveState) return;
    if (!isResultStep) {
      onBrewResultSaveState({ save: async () => {}, canSave: false, saving: false, showGuardar: false });
      return;
    }
    if (brewMethod === "Agua") {
      const cur = refs.current;
      onBrewResultSaveState({
        save: async () => {
          if (!cur.onSaveWaterFromBrew || cur.savingResult) return;
          setSavingResult(true);
          try {
            await cur.onSaveWaterFromBrew(cur.waterMl);
          } finally {
            setSavingResult(false);
          }
        },
        canSave: Boolean(onSaveWaterFromBrew && waterMl > 0),
        saving: savingResult,
        showGuardar: true
      });
      return;
    }
    onBrewResultSaveState({
      save: async () => {
        const cur = refs.current;
        if (cur.savingResult) return;
        setSavingResult(true);
        try {
          await cur.onSaveResultToDiary(cur.resultTaste?.trim() || "—", cur.brewDrinkType);
        } finally {
          setSavingResult(false);
        }
      },
      canSave: true,
      saving: savingResult,
      showGuardar: true
    });
  }, [isResultStep, onBrewResultSaveState, resultTaste, savingResult, brewMethod, waterMl]);
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
    if (brewStep === "method") {
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
    if (!brewMethod || !isEspressoMethod) return;
    const clampedTime = Math.max(timeProfile.minSeconds, Math.min(timeProfile.maxSeconds, Math.round(espressoTimeSeconds)));
    if (clampedTime !== espressoTimeSeconds) setEspressoTimeSeconds(clampedTime);
  }, [brewMethod, espressoTimeSeconds, isEspressoMethod, setEspressoTimeSeconds, timeProfile.maxSeconds, timeProfile.minSeconds]);
  useEffect(() => {
    setSelectedSizeMl(null);
  }, [brewMethod]);
  const ratioLabel = useMemo(() => {
    const value = methodProfile.ratioStep < 1 ? effectiveRatio.toFixed(1) : String(Math.round(effectiveRatio));
    return `RATIO 1:${value} - ${ratioProfile.toUpperCase()}`;
  }, [effectiveRatio, methodProfile.ratioStep, ratioProfile]);

  const methodsToShow = useMemo(
    () => (orderedBrewMethods.length > 0 ? orderedBrewMethods : getOrderedBrewMethods([])),
    [orderedBrewMethods]
  );
  const isOtrosMethod = brewMethod === "Otros";
  const isAguaMethod = brewMethod === "Agua";

  return (
    <>
      {brewStep === "method" ? (
        <div className="brew-select-step">
          <p className="section-title brew-select-section-title">Forma de elaboración</p>
          <div className="brew-forma-card brew-tech-card">
            <section className="home-elaboration-methods brew-elaboration-methods brew-forma-methods" aria-label="Método">
              <div className="home-carousel-with-nav brew-method-carousel-wrap">
                <div
                  ref={brewMethodScrollRef}
                  className={`home-elaboration-methods-scroll brew-method-scroll${brewCarouselDragging ? " is-dragging" : ""}`.trim()}
                  role="listbox"
                  aria-label="Método de elaboración"
                  onPointerDown={(e) => handleBrewCarouselPointerDown(e, brewMethodScrollRef.current)}
                  onPointerMove={handleBrewCarouselPointerMove}
                  onPointerUp={handleBrewCarouselPointerUp}
                  onPointerLeave={handleBrewCarouselPointerUp}
                  onPointerCancel={handleBrewCarouselPointerUp}
                >
                  {methodsToShow.map((method) => {
                    const isActive = brewMethod === method.name;
                    const words = method.name.split(/\s+/);
                    return (
                      <Button
                        key={method.name}
                        variant="plain"
                        type="button"
                        role="option"
                        aria-selected={isActive}
                        className={`brew-method-card ${isActive ? "is-active" : ""}`}
                        onClick={() => {
                          const profile = getBrewMethodProfile(method.name);
                          const methodTime = getBrewTimeProfile(method.name);
                          setBrewMethod(method.name);
                          setWaterMl(profile.defaultWaterMl);
                          setCoffeeGrams?.(Math.max(1, Math.round(profile.defaultWaterMl / Math.max(0.1, profile.defaultRatio))));
                          setEspressoTimeSeconds(methodTime.defaultSeconds);
                        }}
                        aria-label={`Elaborar con ${method.name}`}
                      >
                        <span className="brew-method-card-icon">
                          {method.icon === "bolt" ? (
                            <UiIcon name="bolt" className="ui-icon home-elaboration-method-icon-bolt" aria-hidden="true" />
                          ) : method.icon === "water" ? (
                            <UiIcon name="water" className="ui-icon home-elaboration-method-icon-water" aria-hidden="true" />
                          ) : (
                            <img src={method.icon} alt="" loading="lazy" decoding="async" />
                          )}
                        </span>
                        <span className="brew-method-card-copy">
                          <span className="brew-method-card-label">
                            {words.map((word, i) => (
                              <span key={i} className="brew-method-card-word">{word}</span>
                            ))}
                          </span>
                        </span>
                      </Button>
                    );
                  })}
                </div>
              </div>
            </section>
            {!isOtrosMethod ? (
            <>
              <div className="brew-config-option-divider" aria-hidden="true" />
              <div className="brew-select-params-layout brew-forma-params">
                <div className="brew-select-params-col">
                  <div className="brew-tech-rows-wrap" aria-label={isAguaMethod ? "Cantidad de agua" : "Parámetros del método"}>
                <div className="brew-tech-rows">
                  {(isWaterEditable || isAguaMethod) ? (
                    <div className="brew-tech-row">
                      <div className="brew-tech-field">
                        <span>Agua (ml)</span>
                        <div className="brew-tech-value-field">
                          <Input
                            className="search-wide brew-tech-value-input is-water"
                            type="number"
                            inputMode="numeric"
                            min={BREW_WATER_ABS_MIN_ML}
                            max={BREW_WATER_ABS_MAX_ML}
                            step={methodProfile.waterStepMl}
                            value={waterDraft}
                            onChange={(event) => setWaterDraft(event.target.value)}
                            onBlur={() => {
                              const parsed = Number(waterDraft);
                              if (Number.isFinite(parsed) && parsed > 0) {
                                setWaterMl(Math.max(BREW_WATER_ABS_MIN_ML, Math.min(BREW_WATER_ABS_MAX_ML, Math.round(parsed))));
                              } else {
                                setWaterDraft(String(waterMl));
                              }
                            }}
                            aria-label="Agua en ml"
                          />
                        </div>
                      </div>
                      <label className="brew-tech-slider">
                        <Input
                          className="app-range app-range--water"
                          style={{ "--range-progress": `${waterProgress}%` } as CSSProperties}
                          type="range"
                          min={BREW_WATER_ABS_MIN_ML}
                          max={BREW_SLIDER_MAX_WATER_ML}
                          step={methodProfile.waterStepMl}
                          value={Math.min(waterMl, BREW_SLIDER_MAX_WATER_ML)}
                          onChange={(event) => setWaterMl(Number(event.target.value))}
                        />
                      </label>
                    </div>
                  ) : null}

                  {!isAguaMethod ? (
                  <div className="brew-tech-row">
                    <div className="brew-tech-field">
                      <span>Café (g)</span>
                      <div className="brew-tech-value-field">
                        <Input
                          className="search-wide brew-tech-value-input is-coffee"
                          type="number"
                          inputMode="decimal"
                          min={BREW_COFFEE_ABS_MIN_G}
                          max={BREW_COFFEE_ABS_MAX_G}
                          step={0.1}
                          value={coffeeDraft}
                          onChange={(event) => setCoffeeDraft(event.target.value)}
                          onBlur={() => {
                            const parsed = Number(coffeeDraft.replace(",", "."));
                            if (Number.isFinite(parsed) && parsed > 0) {
                              if (isEspressoMethod) {
                                setCoffeeGrams?.(Math.max(BREW_COFFEE_ABS_MIN_G, Math.min(BREW_COFFEE_ABS_MAX_G, parsed)));
                                setWaterMl(Math.max(BREW_WATER_ABS_MIN_ML, Math.min(BREW_WATER_ABS_MAX_ML, Math.round(parsed * 2))));
                              } else if (setCoffeeGrams) {
                                setCoffeeGrams(Math.max(BREW_COFFEE_ABS_MIN_G, Math.min(BREW_COFFEE_ABS_MAX_G, parsed)));
                              }
                            } else {
                              setCoffeeDraft(coffeeGramsPrecise.toFixed(1));
                            }
                          }}
                          aria-label="Café en gramos"
                        />
                      </div>
                    </div>
                    {isCoffeeSliderShown ? (
                      <label className="brew-tech-slider">
                        <span className="brew-tech-slider-label">{ratioLabel}</span>
                        <Input
                          className="app-range app-range--coffee"
                          style={{ "--range-progress": `${coffeeProgress}%` } as CSSProperties}
                          type="range"
                          min={coffeeSliderRange.min}
                          max={coffeeSliderRange.max}
                          step={0.5}
                          value={Math.min(coffeeGramsPrecise, BREW_SLIDER_MAX_COFFEE_G)}
                          onChange={(event) => {
                            const v = Number(event.target.value);
                            if (!Number.isFinite(v)) return;
                            if (isEspressoMethod) {
                              setCoffeeGrams?.(v);
                              setWaterMl(Math.max(BREW_WATER_ABS_MIN_ML, Math.min(BREW_WATER_ABS_MAX_ML, Math.round(v * 2))));
                            } else if (setCoffeeGrams) {
                              setCoffeeGrams(v);
                            }
                          }}
                        />
                      </label>
                    ) : isRatioEditable && setCoffeeGrams ? (
                      <label className="brew-tech-slider">
                        <span className="brew-tech-slider-label">{ratioLabel}</span>
                        <Input
                          className="app-range app-range--coffee"
                          style={{ "--range-progress": `${coffeeProgress}%` } as CSSProperties}
                          type="range"
                          min={coffeeSliderRange.min}
                          max={coffeeSliderRange.max}
                          step={0.5}
                          value={Math.min(coffeeGramsPrecise, BREW_SLIDER_MAX_COFFEE_G)}
                          onChange={(event) => {
                            const v = Number(event.target.value);
                            if (setCoffeeGrams && Number.isFinite(v)) setCoffeeGrams(Math.max(BREW_COFFEE_ABS_MIN_G, Math.min(BREW_COFFEE_ABS_MAX_G, v)));
                          }}
                        />
                      </label>
                    ) : null}
                  </div>
                  ) : null}

                  {!isAguaMethod && isEspressoMethod ? (
                    <div className="brew-tech-row brew-tech-row-time">
                      <div className="brew-tech-field">
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
                      <label className="brew-tech-slider">
                        <Input
                          className="app-range app-range--time"
                          style={{ "--range-progress": `${Math.max(0, Math.min(100, ((espressoTimeSeconds - timeSliderMin) / Math.max(1, timeSliderMax - timeSliderMin)) * 100))}%` } as CSSProperties}
                          type="range"
                          min={timeSliderMin}
                          max={timeSliderMax}
                          step={1}
                          value={Math.min(espressoTimeSeconds, timeSliderMax)}
                          onChange={(event) => setEspressoTimeSeconds(Number(event.target.value))}
                        />
                      </label>
                    </div>
                  ) : null}
                </div>
                {!isAguaMethod && baristaTips.length > 0 ? (
                  <Button
                    variant="plain"
                    type="button"
                    className="brew-select-barista-cta"
                    onClick={() => setShowBaristaPopover(true)}
                    aria-label="Ver consejos del barista"
                  >
                    <span className="brew-select-barista-cta-text">Consejos del barista</span>
                    <UiIcon name="chevron-right" className="ui-icon brew-select-barista-cta-arrow" aria-hidden="true" />
                  </Button>
                ) : null}
                {!isAguaMethod && (baristaTips.length > 0 || setBrewTimerEnabled) ? (
                  <div className="brew-config-option-divider" aria-hidden="true" />
                ) : null}
                {!isAguaMethod ? (
                  <div className="brew-config-option-row brew-config-timer-row" role="group" aria-label="Temporizador">
                    <span className="brew-config-option-label">Temporizador</span>
                    {setBrewTimerEnabled ? (
                      <Switch
                        checked={brewTimerEnabled}
                        onClick={() => setBrewTimerEnabled(!brewTimerEnabled)}
                        aria-label="Activar temporizador para proceso en curso"
                      />
                    ) : null}
                  </div>
                ) : null}
                  </div>
                </div>
              </div>
            </>
            ) : null}
          </div>

          {!isAguaMethod ? (
          <>
            <p className="section-title brew-select-section-title brew-config-cafe-title">Configura tu café</p>
            <div className="brew-config-cafe-card brew-tech-card">
              <button
                type="button"
                className="brew-select-coffee-row"
                onClick={onAddToPantry ?? onAddNotFoundCoffee}
                aria-label={selectedCoffee ? `Selecciona café: ${selectedCoffee.nombre}` : "Selecciona café"}
              >
                {selectedCoffee ? (
                  <span className="brew-select-coffee-selected">
                    {selectedCoffee.image_url ? (
                      <img src={selectedCoffee.image_url} alt="" className="brew-select-coffee-img" loading="lazy" decoding="async" />
                    ) : null}
                    <span className="brew-select-coffee-name">{selectedCoffee.nombre}</span>
                  </span>
                ) : (
                  <span className="brew-select-coffee-row-label">Selecciona café</span>
                )}
                <UiIcon name="arrow-right" className="ui-icon brew-select-coffee-row-arrow" aria-hidden="true" />
              </button>
              <div className="brew-config-option-divider" aria-hidden="true" />
              <section className="brew-tipo-strip-wrap brew-config-cafe-section" aria-label="Tipo de café">
                <div className="brew-config-cafe-carousel-wrap">
              <div
                ref={brewTipoScrollRef}
                className={`brew-tipo-strip${brewCarouselDragging ? " is-dragging" : ""}`.trim()}
                role="listbox"
                aria-label="Tipo de café"
                onPointerDown={(e) => handleBrewCarouselPointerDown(e, brewTipoScrollRef.current)}
                onPointerMove={handleBrewCarouselPointerMove}
                onPointerUp={handleBrewCarouselPointerUp}
                onPointerLeave={handleBrewCarouselPointerUp}
                onPointerCancel={handleBrewCarouselPointerUp}
              >
                {COFFEE_TIPO_OPTIONS.map(({ label, drawable }) => {
                  const isSelected = brewDrinkType === label;
                  return (
                    <Button
                      variant="plain"
                      key={label}
                      type="button"
                      role="option"
                      aria-selected={isSelected}
                      className={`brew-tipo-card ${isSelected ? "is-active" : ""}`.trim()}
                      onClick={() => setBrewDrinkType?.(label)}
                    >
                      <span className="brew-tipo-card-icon">
                        <img src={`/android-drawable/${drawable}`} alt="" aria-hidden="true" loading="lazy" decoding="async" />
                      </span>
                      <span className="brew-tipo-card-copy">
                        <span className="brew-tipo-card-label">{label}</span>
                      </span>
                    </Button>
                  );
                })}
              </div>
                </div>
              </section>
              <div className="brew-config-option-divider" aria-hidden="true" />
              <section className="brew-tamaño-strip-wrap brew-config-cafe-section" aria-label="Tamaño">
                <div className="brew-config-cafe-carousel-wrap">
                <div
                ref={brewTamañoScrollRef}
                className={`brew-tamaño-strip${brewCarouselDragging ? " is-dragging" : ""}`.trim()}
                role="listbox"
                aria-label="Tamaño de la taza"
                onPointerDown={(e) => handleBrewCarouselPointerDown(e, brewTamañoScrollRef.current)}
                onPointerMove={handleBrewCarouselPointerMove}
                onPointerUp={handleBrewCarouselPointerUp}
                onPointerLeave={handleBrewCarouselPointerUp}
                onPointerCancel={handleBrewCarouselPointerUp}
              >
              {COFFEE_SIZE_OPTIONS.map((size) => {
                const isSelected = selectedSizeMl === size.ml;
                return (
                  <Button
                    variant="plain"
                    key={size.label}
                    type="button"
                    role="option"
                    aria-selected={isSelected}
                    className={`brew-tamaño-card ${isSelected ? "is-active" : ""}`.trim()}
                    onClick={() => {
                      setSelectedSizeMl(size.ml);
                    }}
                  >
                    <span className="brew-tamaño-card-icon">
                      <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" loading="lazy" decoding="async" />
                    </span>
                    <span className="brew-tamaño-card-copy">
                      <span className="brew-tamaño-card-label">{size.label}</span>
                      <span className="brew-tamaño-card-range">{size.rangeLabel}</span>
                    </span>
                    </Button>
                );
              })}
                </div>
                </div>
              </section>
            </div>
          </>
          ) : null}
        </div>
      ) : null}

      {brewStep === "brewing" ? (
        <section className="brew-prep-screen">
          <div className="brew-prep-layout">
            <div className="brew-prep-left-col">
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
          </div>
        </section>
      ) : null}

      {brewStep === "result" ? (
        <section className="brew-result-screen" aria-label="Resultado de la elaboración">
          <article className="brew-result-card">
            {isAguaMethod ? (
              <p className="brew-result-title">Registrar {waterMl} ml de agua</p>
            ) : (
              <>
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
              </>
            )}
          </article>
        </section>
      ) : null}

      {showBaristaPopover && baristaTips.length > 0 ? (
        <SheetOverlay
          role="dialog"
          aria-modal="true"
          aria-label="Consejos del barista"
          onDismiss={() => setShowBaristaPopover(false)}
          onClick={() => setShowBaristaPopover(false)}
        >
          <SheetCard className="diary-sheet brew-barista-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header brew-barista-sheet-header">
              <span className="brew-barista-sheet-header-spacer" aria-hidden="true" />
              <strong className="sheet-title">Consejos del barista</strong>
              <Button
                variant="plain"
                type="button"
                className="brew-barista-sheet-close"
                aria-label="Cerrar"
                onClick={() => setShowBaristaPopover(false)}
              >
                <UiIcon name="close" className="ui-icon" />
              </Button>
            </header>
            <div className="brew-barista-sheet-list">
              {baristaTips.map((tip) => (
                <article className="brew-barista-tip brew-select-barista-tip" key={`${tip.label}-${tip.value}`}>
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
          </SheetCard>
        </SheetOverlay>
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












