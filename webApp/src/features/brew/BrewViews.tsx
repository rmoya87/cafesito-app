import { type CSSProperties, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { getOrderedBrewMethods } from "../../config/brew";
import type { BrewMethodItem } from "../../config/brew";
import { COFFEE_SIZE_OPTIONS, COFFEE_TIPO_OPTIONS } from "../../data/diaryBrewOptions";
import { EMPTY } from "../../core/emptyErrorStrings";
import { sendEvent } from "../../core/ga4";
import { BREW_COFFEE_ABS_MAX_G, BREW_COFFEE_ABS_MIN_G, BREW_SLIDER_MAX_COFFEE_G, BREW_SLIDER_MIN_COFFEE_G, BREW_SLIDER_MAX_TIME_S, BREW_SLIDER_MAX_WATER_ML, BREW_SLIDER_MIN_WATER_ML, BREW_WATER_ABS_MAX_ML, BREW_WATER_ABS_MIN_ML, formatClock, getBrewingProcessAdvice, getBrewBaristaTipsForMethod, getBrewDialRecommendation, getBrewMethodProfile, getBrewTimeProfile, getBrewTimelineForMethod } from "../../core/brew";
import { normalizeLookupText } from "../../core/text";
import type { CreateCoffeeDraft } from "../../hooks/domains/useCreateCoffeeDomain";
import type { BrewStep, CoffeeRow, PantryItemRow } from "../../types";
import { convertImageToWebP, webPBlobToFile } from "../../utils/imageToWebP";
import { Button, Input, Select, SheetCard, SheetHandle, SheetOverlay, Switch } from "../../ui/components";
import { UiIcon, type IconName } from "../../ui/iconography";
import { useI18n } from "../../i18n";

const ROAST_OPTIONS = ["Ligero", "Medio", "Medio-oscuro"];
const FORMAT_OPTIONS = ["Grano", "Molido", "Capsula"];
const PROCESS_OPTIONS = ["Natural", "Lavado", "Honey", "Semi-lavado", "Otro"];
const VARIETY_OPTIONS = ["Geisha", "Caturra", "Arábica 100%", "Robusta", "Bourbon", "Typica", "Maragogype", "Pacamara", "Otro"];
const GRIND_OPTIONS = ["Molido fino", "Molido medio", "Molido grueso", "Grano entero"];

type PickerId = "specialty" | "roast" | "country" | "variety" | "format" | "process" | "grind";

function parseMultiValue(s: string): string[] {
  if (!s || !String(s).trim()) return [];
  return String(s)
    .split(/[,;|]+/)
    .map((x) => x.trim())
    .filter(Boolean);
}
function formatMultiValue(arr: string[]): string {
  return arr.filter(Boolean).join(", ");
}
/** Texto para mostrar en el campo: valores múltiples separados por comas */
function displayMultiValue(s: string | undefined): string {
  return formatMultiValue(parseMultiValue(s ?? ""));
}
export function BrewLabView({
  brewStep,
  setBrewStep,
  brewMethod,
  setBrewMethod,
  brewCoffeeId,
  setBrewCoffeeId,
  brewPantryItemId = "",
  setBrewPantryItemId,
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
  setBrewTimerEnabled,
  onTimerEndedGoToConsumption,
  onTimerEndedChange
}: {
  brewStep: BrewStep;
  setBrewStep: (step: BrewStep) => void;
  brewMethod: string;
  setBrewMethod: (value: string) => void;
  brewCoffeeId: string;
  setBrewCoffeeId: (value: string) => void;
  /** Ítem de despensa elegido (varios pueden ser del mismo café). Si está definido, se usa para mostrar stock y restar del ítem correcto. */
  brewPantryItemId?: string;
  setBrewPantryItemId?: (value: string) => void;
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
  /** Si está definido, al terminar el temporizador se llama y no se muestra el resultado debajo del timer (ir a consumo en diario). */
  onTimerEndedGoToConsumption?: () => void;
  /** Notifica cuando el temporizador termina (para mostrar botón Siguiente en topbar que va a consumo). */
  onTimerEndedChange?: (ended: boolean) => void;
}) {
  const { t, locale } = useI18n();
  const createCopy = useMemo(() => {
    if (locale === "en") {
      return {
        ariaCreateCoffee: "Create your coffee",
        subtitle: "Create your coffee and add the bag you will have in your pantry.",
        ready: "Ready to save",
        missing: (n: number) => `${n} required fields missing`,
        removeCoffeeImage: "Remove coffee image",
        addCoffeeImage: "Add coffee photo",
        addPhoto: "Add photo",
        coffeeNamePlaceholder: "Coffee name *",
        coffeeNameAria: "Coffee name (required)",
        roasterPlaceholder: "Roaster",
        roasterAria: "Roaster (required)",
        suggestedRoasters: "Suggested roasters",
        originProfile: "Origin and profile",
        selectSpecialty: "Select specialty",
        selectRoast: "Select roast",
        selectCountry: "Select country(ies)",
        selectVariety: "Select variety(ies)",
        presentation: "Presentation",
        hasCaffeine: "Has caffeine?",
        caffeineCoffee: "Coffee with caffeine",
        selectFormat: "Select format",
        quantityGrams: "Amount (g)",
        quantityAria: "Amount in grams, bag size (required)",
        optionalDetails: "Optional details",
        optionalBadge: "Optional",
        description: "Description",
        descriptionAria: "Coffee description",
        process: "Process",
        selectProcess: "Select process(es)",
        recommendedGrind: "Recommended grind",
        selectGrind: "Select grind",
        barcode: "Barcode",
        productLink: "Product link",
        sensoryProfile: "Sensory profile",
        sensoryHint: "Rating from 0 to 5 (0 = not rated)"
      };
    }
    return {
      ariaCreateCoffee: "Crea tu café",
      subtitle: "Crea tu café y añade la bolsa que tendrás en tu despensa.",
      ready: "Listo para guardar",
      missing: (n: number) => `Faltan ${n} campos obligatorios`,
      removeCoffeeImage: "Quitar imagen del café",
      addCoffeeImage: "Añadir foto del café",
      addPhoto: "Añadir foto",
      coffeeNamePlaceholder: "Nombre del café *",
      coffeeNameAria: "Nombre del café (obligatorio)",
      roasterPlaceholder: "Tostador",
      roasterAria: "Tostador (obligatorio)",
      suggestedRoasters: "Tostadores sugeridos",
      originProfile: "Origen y perfil",
      selectSpecialty: "Seleccionar especialidad",
      selectRoast: "Seleccionar tueste",
      selectCountry: "Seleccionar país(es)",
      selectVariety: "Seleccionar variedad(es)",
      presentation: "Presentación",
      hasCaffeine: "¿Tiene cafeína?",
      caffeineCoffee: "Café con cafeína",
      selectFormat: "Seleccionar formato",
      quantityGrams: "Cantidad (g)",
      quantityAria: "Cantidad en gramos, tamaño de la bolsa (obligatorio)",
      optionalDetails: "Detalles opcionales",
      optionalBadge: "Opcional",
      description: "Descripción",
      descriptionAria: "Descripción del café",
      process: "Proceso",
      selectProcess: "Seleccionar proceso(s)",
      recommendedGrind: "Molienda recomendada",
      selectGrind: "Seleccionar molienda",
      barcode: "Código de barras",
      productLink: "Enlace al producto",
      sensoryProfile: "Perfil sensorial",
      sensoryHint: "Valoración del 0 al 5 (0 = no valorar)"
    };
  }, [locale]);
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
  /* Handlers que aceptan evento nativo para usarlos también desde document (capture), así el drag funciona al arrastrar desde la imagen/chip */
  const doBrewCarouselMove = useCallback((e: { clientX: number; pointerId: number; preventDefault: () => void }) => {
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
  const doBrewCarouselUp = useCallback((e: { pointerId: number }) => {
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

  const docCarouselMoveRef = useRef<(e: PointerEvent) => void>(() => {});
  const docCarouselUpRef = useRef<(e: PointerEvent) => void>(() => {});

  const handleBrewCarouselPointerDown = useCallback(
    (e: React.PointerEvent, el: HTMLDivElement | null) => {
      if (!el || el.scrollWidth <= el.clientWidth) return;
      if (!el.contains(e.target as Node)) return;
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

      docCarouselMoveRef.current = (ev: PointerEvent) => doBrewCarouselMove(ev);
      docCarouselUpRef.current = (ev: PointerEvent) => {
        doBrewCarouselUp(ev);
        document.removeEventListener("pointermove", docCarouselMoveRef.current, true);
        document.removeEventListener("pointerup", docCarouselUpRef.current, true);
        document.removeEventListener("pointercancel", docCarouselUpRef.current, true);
      };
      document.addEventListener("pointermove", docCarouselMoveRef.current, true);
      document.addEventListener("pointerup", docCarouselUpRef.current, true);
      document.addEventListener("pointercancel", docCarouselUpRef.current, true);
    },
    [doBrewCarouselMove, doBrewCarouselUp]
  );
  const handleBrewCarouselPointerMove = useCallback(
    (e: React.PointerEvent) => doBrewCarouselMove(e.nativeEvent),
    [doBrewCarouselMove]
  );
  const handleBrewCarouselPointerUp = useCallback(
    (e: React.PointerEvent) => doBrewCarouselUp(e.nativeEvent),
    [doBrewCarouselUp]
  );
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
    brewDrinkType: "Espresso",
    selectedSizeMl: null as number | null
  });
  refs.current = { selectedCoffee, resultTaste, savingResult, onSaveResultToDiary, waterMl, onSaveWaterFromBrew, brewDrinkType, selectedSizeMl };
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
  const isCoffeeSliderShown = useMemo(
    () => (isWaterEditable && !isRatioEditable) || isEspressoMethod,
    [isWaterEditable, isRatioEditable, isEspressoMethod]
  );
  const coffeeGramsPrecise = useMemo(
    () => (isEspressoMethod ? Number((waterMl / 2).toFixed(1)) : Number(coffeeGramsProp.toFixed(1))),
    [isEspressoMethod, waterMl, coffeeGramsProp]
  );
  const coffeeGramsLabel = useMemo(() => coffeeGramsPrecise.toFixed(1).replace(".", ","), [coffeeGramsPrecise]);
  const [waterDraft, setWaterDraft] = useState(String(waterMl));
  const [coffeeDraft, setCoffeeDraft] = useState(coffeeGramsPrecise.toFixed(1));
  const waterProgress = useMemo(() => {
    const min = BREW_SLIDER_MIN_WATER_ML;
    const max = BREW_SLIDER_MAX_WATER_ML;
    return Math.max(0, Math.min(100, ((waterMl - min) / Math.max(1, max - min)) * 100));
  }, [waterMl]);
  const ratioProgress = useMemo(() => {
    const min = methodProfile.ratioMin;
    const max = methodProfile.ratioMax;
    return Math.max(0, Math.min(100, ((ratio - min) / Math.max(0.1, max - min)) * 100));
  }, [methodProfile.ratioMax, methodProfile.ratioMin, ratio]);
  const coffeeSliderRange = useMemo(() => ({ min: BREW_SLIDER_MIN_COFFEE_G, max: BREW_SLIDER_MAX_COFFEE_G }), []);
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
    if (locale === "es") {
      if (normalized <= 0.35) return "CONCENTRADO";
      if (normalized <= 0.7) return "EQUILIBRADO";
      return "LIGERO";
    }
    if (normalized <= 0.35) return "CONCENTRATED";
    if (normalized <= 0.7) return "BALANCED";
    return "LIGHT";
  }, [effectiveRatio, locale, methodProfile.ratioMax, methodProfile.ratioMin]);
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
  const timerEndedGoToConsumption = timerEnded && Boolean(brewTimerEnabled && onTimerEndedGoToConsumption);
  const isResultStep = brewStep === "result" || timerEnded;
  useEffect(() => {
    onTimerEndedChange?.(timerEnded);
    return () => onTimerEndedChange?.(false);
  }, [timerEnded, onTimerEndedChange]);
  // No auto-redirect: el usuario pulsa "Siguiente" en la topbar para ir a consumo.
  useEffect(() => {
    if (!onBrewResultSaveState) return;
    if (!isResultStep || timerEndedGoToConsumption) {
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
        sendEvent("button_click", { button_id: "brew_save_to_diary" });
        setSavingResult(true);
        try {
          // El sabor es opcional en Consumo; si no hay texto, se envía cadena vacía.
          await cur.onSaveResultToDiary(cur.resultTaste?.trim() || "", cur.brewDrinkType);
        } finally {
          setSavingResult(false);
        }
      },
      // Solo son obligatorios Tipo y Tamaño; el resultado (sabor) no bloquea el guardado.
      canSave: Boolean(brewDrinkType && selectedSizeMl != null),
      saving: savingResult,
      showGuardar: true
    });
  }, [isResultStep, onBrewResultSaveState, resultTaste, savingResult, brewMethod, waterMl, brewDrinkType, selectedSizeMl]);
  const currentPhaseIndex = useMemo(() => {
    if (!brewTimeline.length) return 0;
    let elapsed = 0;
    for (let i = 0; i < brewTimeline.length; i += 1) {
      elapsed += brewTimeline[i].durationSeconds;
      if (elapsedSeconds < elapsed) return i;
    }
    return Math.max(0, brewTimeline.length - 1);
  }, [brewTimeline, elapsedSeconds]);
  const currentPhase = brewTimeline[currentPhaseIndex] ?? { label: t("brew.finish"), instruction: t("brew.recommendation"), durationSeconds: 0 };
  const nextPhase = brewTimeline[currentPhaseIndex + 1];
  const translatePhaseLabel = useCallback(
    (label: string) => {
      if (locale === "es") return label;
      const key = normalizeLookupText(label);
      const map: Record<string, string> = {
        "calentamiento": "Heating",
        "extraccion": "Extraction",
        "inmersion": "Immersion",
        "infusion": "Infusion",
        "pre-infusion": "Pre-infusion",
        "preinfusion": "Pre-infusion",
        "presion": "Press",
        "vertido principal": "Main pour",
        "drenado": "Drawdown",
        "mezcla": "Mixing",
        "toque final": "Final touch",
        "ascenso": "Rise",
        "efecto vacio": "Vacuum draw"
      };
      return map[key] ?? label;
    },
    [locale]
  );
  const translatePhaseInstruction = useCallback(
    (instruction: string) => {
      if (locale === "es") return instruction;
      const key = normalizeLookupText(instruction);
      if (key.includes("fuego medio-bajo")) return "Keep medium-low heat. Water in the lower chamber builds pressure up the funnel.";
      if (key.includes("empiece a salir")) return "When coffee starts flowing, lower heat or remove from stove before the final sputter.";
      if (key.includes("presion constante")) return "Keep steady pressure and a honey-like flow.";
      if (key.includes("humedece el cafe")) return "Bloom the bed and let trapped CO2 release before continuing.";
      return instruction;
    },
    [locale]
  );
  const currentPhaseLabel = useMemo(() => translatePhaseLabel(currentPhase.label), [currentPhase.label, translatePhaseLabel]);
  const currentPhaseInstruction = useMemo(
    () => translatePhaseInstruction(currentPhase.instruction),
    [currentPhase.instruction, translatePhaseInstruction]
  );
  const nextPhaseLabel = useMemo(
    () => (nextPhase ? translatePhaseLabel(nextPhase.label) : t("brew.finish")),
    [nextPhase, t, translatePhaseLabel]
  );
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
  const processAdviceCards = useMemo(() => {
    if (locale !== "es") {
      return [
        currentPhaseInstruction,
        "Keep a steady flow and avoid abrupt changes in temperature or pour speed."
      ];
    }
    return [currentPhaseInstruction, ...brewingAdviceLines.map((line) => `${line}.`)];
  }, [brewingAdviceLines, currentPhaseInstruction, locale]);
  const resultRecommendation = useMemo(() => {
    if (locale === "es") return getBrewDialRecommendation(resultTaste);
    const key = normalizeLookupText(resultTaste);
    if (key === "amargo") return "Reduce extraction: grind a touch coarser or lower contact time slightly.";
    if (key === "acido") return "Increase extraction: grind finer or raise temperature slightly.";
    if (key === "equilibrado") return "Great balance. Save this profile as your reference point.";
    if (key === "salado") return "Improve bed prep: check distribution and leveling for better uniformity.";
    if (key === "acuoso") return "Increase coffee dose or lower ratio to gain body.";
    if (key === "aspero") return "Astringency detected: reduce agitation and rinse filter thoroughly.";
    if (key === "dulce") return "Excellent sugar extraction. Keep this setup.";
    return "";
  }, [locale, resultTaste]);
  const tasteOptions = useMemo(
    () => [
      { label: locale === "es" ? "Amargo" : "Bitter", value: "Amargo", icon: "taste-bitter" as IconName },
      { label: locale === "es" ? "Acido" : "Acidic", value: "Acido", icon: "taste-acid" as IconName },
      { label: locale === "es" ? "Equilibrado" : "Balanced", value: "Equilibrado", icon: "taste-balance" as IconName },
      { label: locale === "es" ? "Salado" : "Salty", value: "Salado", icon: "taste-salty" as IconName },
      { label: locale === "es" ? "Acuoso" : "Watery", value: "Acuoso", icon: "taste-watery" as IconName },
      { label: locale === "es" ? "Aspero" : "Rough", value: "Aspero", icon: "grind" as IconName },
      { label: locale === "es" ? "Dulce" : "Sweet", value: "Dulce", icon: "taste-sweet" as IconName }
    ],
    [locale]
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
    return `${locale === "es" ? "RATIO" : "RATIO"} 1:${value} - ${ratioProfile.toUpperCase()}`;
  }, [effectiveRatio, locale, methodProfile.ratioStep, ratioProfile]);

  const espressoCoffeeSliderLabel = useMemo(() => {
    const value = methodProfile.ratioStep < 1 ? effectiveRatio.toFixed(1) : String(Math.round(effectiveRatio));
    return `RATIO 1:${value} - ${ratioProfile}`;
  }, [effectiveRatio, methodProfile.ratioStep, ratioProfile]);

  const methodsToShow = useMemo(
    () => (orderedBrewMethods.length > 0 ? orderedBrewMethods : getOrderedBrewMethods([])),
    [orderedBrewMethods]
  );
  const localizedMethodName = useCallback(
    (name: string) => {
      if (locale === "es") return name;
      const key = normalizeLookupText(name);
      const map: Record<string, string> = {
        "goteo": "Drip",
        "prensa francesa": "French press",
        "italiana": "Moka",
        "otros": "Other",
        "agua": "Water"
      };
      return map[key] ?? name;
    },
    [locale]
  );
  const isOtrosMethod = brewMethod === "Otros";
  const isAguaMethod = brewMethod === "Agua";
  const translateBaristaLabel = useCallback(
    (label: string) => {
      if (locale === "es") return label;
      const key = normalizeLookupText(label);
      const map: Record<string, string> = {
        "perfil actual": "CURRENT PROFILE",
        "volumen": "VOLUME",
        "tiempo actual": "CURRENT TIME",
        "dosis": "DOSE",
        "base": "BASE",
        "proceso": "PROCESS",
        "ajuste": "ADJUSTMENT",
        "detalle": "DETAIL",
        "molienda": "GRIND",
        "temperatura": "TEMPERATURE",
        "ratio": "RATIO"
      };
      return map[key] ?? label;
    },
    [locale]
  );
  const translateBaristaValue = useCallback(
    (value: string) => {
      if (locale === "es") return value;
      const text = normalizeLookupText(value);
      if (text.includes("mas concentrado")) return "More concentrated profile; if bitter, grind one notch coarser.";
      if (text.includes("tramo medio")) return "Mid-volume range: good balance between body and clarity.";
      if (text.includes("ventana ideal")) return "Within ideal window: keep steady flow and consistent crema.";
      if (text.includes("dentro de rango clasico")) return "Within classic espresso dose range.";
      if (text.includes("molienda fina")) return "Fine grind";
      if (text.includes("si corre rapido")) return "Fast flow: grind finer. Slow choke: grind coarser.";
      if (text.includes("distribucion")) return "Distribution: level the bed before tamping.";
      return value;
    },
    [locale]
  );

  return (
    <>
      {brewStep === "method" ? (
        <div className="brew-select-step">
          <div className="brew-forma-card brew-tech-card">
            <section className="home-elaboration-methods brew-elaboration-methods brew-forma-methods" aria-label={t("brew.method")}>
              <div className="home-carousel-with-nav brew-method-carousel-wrap">
                <div
                  ref={brewMethodScrollRef}
                  className={`home-elaboration-methods-scroll brew-method-scroll${brewCarouselDragging ? " is-dragging" : ""}`.trim()}
                  role="listbox"
                  aria-label={t("brew.methodAria")}
                  onPointerDownCapture={(e) => handleBrewCarouselPointerDown(e, brewMethodScrollRef.current)}
                  onPointerMove={handleBrewCarouselPointerMove}
                  onPointerUp={handleBrewCarouselPointerUp}
                  onPointerLeave={handleBrewCarouselPointerUp}
                  onPointerCancel={handleBrewCarouselPointerUp}
                >
                  {methodsToShow.map((method) => {
                    const isActive = brewMethod === method.name;
                    const methodDisplayName = localizedMethodName(method.name);
                    const words = methodDisplayName.split(/\s+/);
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
                        aria-label={locale === "es" ? `Elaborar con ${methodDisplayName}` : `Brew with ${methodDisplayName}`}
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
            {!isAguaMethod ? (
              <>
                <div className="brew-config-option-divider" aria-hidden="true" />
                <button
                  type="button"
                  className="brew-select-coffee-row"
                  onClick={onAddToPantry ?? onAddNotFoundCoffee}
                  aria-label={
                    selectedCoffee
                      ? (() => {
                          const pantryRow = brewPantryItemId
                            ? pantryItems.find((r) => r.item.id === brewPantryItemId)
                            : brewCoffeeId
                              ? pantryItems.find((r) => r.coffee.id === brewCoffeeId)
                              : null;
                          if (pantryRow) {
                            return locale === "es"
                              ? `Selecciona café: ${selectedCoffee.nombre}, ${Math.round(pantryRow.remaining)} de ${Math.round(pantryRow.total)} g en bolsa`
                              : `Select coffee: ${selectedCoffee.nombre}, ${Math.round(pantryRow.remaining)} of ${Math.round(pantryRow.total)} g in bag`;
                          }
                          return locale === "es"
                            ? `Selecciona café: ${selectedCoffee.nombre}`
                            : `Select coffee: ${selectedCoffee.nombre}`;
                        })()
                      : t("brew.selectCoffee")
                  }
                >
                  {selectedCoffee ? (
                    <span className="brew-select-coffee-selected">
                      {selectedCoffee.image_url ? (
                        <img src={selectedCoffee.image_url} alt="" className="brew-select-coffee-img" loading="lazy" decoding="async" />
                      ) : null}
                      <span className="brew-select-coffee-selected-copy">
                        <span className="brew-select-coffee-name">{selectedCoffee.nombre}</span>
                        {(() => {
                          const pantryRow = brewPantryItemId
                            ? pantryItems.find((r) => r.item.id === brewPantryItemId)
                            : brewCoffeeId
                              ? pantryItems.find((r) => r.coffee.id === brewCoffeeId)
                              : null;
                          if (!pantryRow) return null;
                          return (
                            <span className="brew-select-coffee-stock" aria-hidden="true">
                              {Math.round(pantryRow.remaining)}/{Math.round(pantryRow.total)}g
                            </span>
                          );
                        })()}
                      </span>
                    </span>
                  ) : (
                    <span className="brew-select-coffee-row-label">{t("brew.selectCoffee")}</span>
                  )}
                  <UiIcon name="arrow-right" className="ui-icon brew-select-coffee-row-arrow" aria-hidden="true" />
                </button>
              </>
            ) : null}
            {!isOtrosMethod ? (
            <>
              <div className="brew-config-option-divider" aria-hidden="true" />
              <div className="brew-select-params-layout brew-forma-params">
                <div className="brew-select-params-col">
                  <div className="brew-tech-rows-wrap" aria-label={isAguaMethod ? t("brew.waterMl") : t("brew.paramsMethod")}>
                <div className="brew-tech-rows">
                  {(isWaterEditable || isAguaMethod) ? (
                    <div className="brew-tech-row">
                      <div className="brew-tech-field">
                        <span>{t("brew.waterMl")}</span>
                        <div className="brew-tech-value-field">
                          <Input
                            className="search-wide brew-tech-value-input is-water"
                            type="number"
                            inputMode="numeric"
                            min={BREW_SLIDER_MIN_WATER_ML}
                            max={BREW_WATER_ABS_MAX_ML}
                            step={methodProfile.waterStepMl}
                            value={waterDraft}
                            onChange={(event) => setWaterDraft(event.target.value)}
                            onBlur={() => {
                              const parsed = Number(waterDraft);
                              if (Number.isFinite(parsed) && parsed >= 0) {
                                setWaterMl(Math.max(BREW_SLIDER_MIN_WATER_ML, Math.min(BREW_WATER_ABS_MAX_ML, Math.round(parsed))));
                              } else {
                                setWaterDraft(String(waterMl));
                              }
                            }}
                          aria-label={t("brew.waterMl")}
                          />
                        </div>
                      </div>
                      <label className="brew-tech-slider">
                        <Input
                          className="app-range app-range--water"
                          style={{ "--range-progress": `${waterProgress}%` } as CSSProperties}
                          type="range"
                          min={BREW_SLIDER_MIN_WATER_ML}
                          max={BREW_SLIDER_MAX_WATER_ML}
                          step={methodProfile.waterStepMl}
                          value={Math.max(BREW_SLIDER_MIN_WATER_ML, Math.min(waterMl, BREW_SLIDER_MAX_WATER_ML))}
                          onChange={(event) => setWaterMl(Number(event.target.value))}
                        />
                      </label>
                    </div>
                  ) : null}

                  {!isAguaMethod ? (
                  <div className={`brew-tech-row ${isEspressoMethod ? "brew-tech-row-coffee-espresso" : ""}`.trim()}>
                    {isEspressoMethod ? (
                      <div className="brew-tech-coffee-espresso-titles" aria-hidden="true">
                        <span>{t("brew.coffeeG")}</span>
                        <span className="brew-tech-slider-label">{espressoCoffeeSliderLabel}</span>
                      </div>
                    ) : null}
                    <div className="brew-tech-field">
                      {!isEspressoMethod ? <span>{t("brew.coffeeG")}</span> : null}
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
                          aria-label={t("brew.coffeeG")}
                        />
                      </div>
                    </div>
                    {isCoffeeSliderShown ? (
                      <label className="brew-tech-slider">
                        {!isEspressoMethod ? <span className="brew-tech-slider-label">{ratioLabel}</span> : null}
                        <Input
                          className="app-range app-range--coffee"
                          style={{ "--range-progress": `${coffeeProgress}%` } as CSSProperties}
                          type="range"
                          min={coffeeSliderRange.min}
                          max={coffeeSliderRange.max}
                          step={0.5}
                          value={Math.max(BREW_SLIDER_MIN_COFFEE_G, Math.min(coffeeGramsPrecise, BREW_SLIDER_MAX_COFFEE_G))}
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
                          value={Math.max(BREW_SLIDER_MIN_COFFEE_G, Math.min(coffeeGramsPrecise, BREW_SLIDER_MAX_COFFEE_G))}
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
                        <span>{t("brew.timeS")}</span>
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
                            aria-label={t("brew.timeS")}
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
                    aria-label={t("brew.seeBaristaTips")}
                  >
                    <span className="brew-select-barista-cta-text">{t("brew.baristaTips")}</span>
                    <UiIcon name="chevron-right" className="ui-icon brew-select-barista-cta-arrow" aria-hidden="true" />
                  </Button>
                ) : null}
                {!isAguaMethod && (baristaTips.length > 0 || setBrewTimerEnabled) ? (
                  <div className="brew-config-option-divider" aria-hidden="true" />
                ) : null}
                {!isAguaMethod ? (
                  <div className="brew-config-option-row brew-config-timer-row" role="group" aria-label={t("brew.timerAria")}>
                    <span className="brew-config-option-label">{t("brew.timer")}</span>
                    {setBrewTimerEnabled ? (
                      <Switch
                        checked={brewTimerEnabled}
                        onClick={() => setBrewTimerEnabled(!brewTimerEnabled)}
                        aria-label={t("brew.timerEnableAria")}
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
        </div>
      ) : null}

      {brewStep === "brewing" ? (
        <section className="brew-prep-screen">
          <div className="brew-prep-layout">
            <div className="brew-prep-left-col">
            <article className="brew-prep-card brew-prep-card-timer">
              <div className="brew-prep-head">
                <p className="brew-prep-phase">{currentPhaseLabel}</p>
                <strong className={`brew-prep-clock ${remainingInPhase <= 5 && brewRunning ? "is-warning" : ""}`.trim()}>
                  {formatClock(remainingInPhase)}
                </strong>
                <p className="brew-prep-next">{t("brew.nextLabel", { label: nextPhaseLabel })}</p>
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
                  <span>{t("brew.total", { time: formatClock(brewTotalSeconds) })}</span>
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
                    {t("brew.restart")}
                  </Button>
                ) : null}
                <Button
                  variant="plain"
                  className={`action-button brew-prep-action-primary ${brewRunning ? "is-running" : ""}`.trim()}
                  onClick={() => setBrewRunning(!brewRunning)}
                >
                  {brewRunning ? t("brew.pause") : t("brew.start")}
                </Button>
              </div>
            ) : null}
            </div>
            <div className="brew-prep-tips-strip">
              {timerEnded && !timerEndedGoToConsumption ? (
                <article className="brew-result-card brew-result-card-inline">
                  <p className="brew-result-title">{t("brew.resultTasteTitle")}</p>
                  <div className="brew-result-grid">
                    {tasteOptions.map((taste) => (
                      <Button
                        variant="plain"
                        key={taste.value}
                        className={`brew-taste-chip ${resultTaste === taste.value ? "is-active" : ""}`.trim()}
                        onClick={() => setResultTaste(taste.value)}
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
                        <strong>{t("brew.recommendation")}</strong>
                      </div>
                      <p>{resultRecommendation}</p>
                    </div>
                  ) : null}
                </article>
              ) : !timerEnded ? (
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
              ) : null}
            </div>
          </div>
        </section>
      ) : null}

      {brewStep === "result" ? (
        <section className="brew-result-screen" aria-label={t("diary.consumption")}>
          {!isAguaMethod ? (
            <>
              <p className="section-title brew-select-section-title brew-config-cafe-title">{t("brew.configureCoffee")}</p>
              <div className="brew-config-cafe-card brew-tech-card">
                <section className="brew-tipo-strip-wrap brew-config-cafe-section" aria-label={t("brew.coffeeType")}>
                  <div className="brew-config-cafe-carousel-wrap">
                    <div
                      ref={brewTipoScrollRef}
                      className={`brew-tipo-strip${brewCarouselDragging ? " is-dragging" : ""}`.trim()}
                      role="listbox"
                      aria-label={t("brew.coffeeType")}
                      onPointerDownCapture={(e) => handleBrewCarouselPointerDown(e, brewTipoScrollRef.current)}
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
                <section className="brew-tamaño-strip-wrap brew-config-cafe-section" aria-label={t("brew.cupSize")}>
                  <div className="brew-config-cafe-carousel-wrap">
                    <div
                      ref={brewTamañoScrollRef}
                      className={`brew-tamaño-strip${brewCarouselDragging ? " is-dragging" : ""}`.trim()}
                      role="listbox"
                      aria-label={t("brew.cupSize")}
                      onPointerDownCapture={(e) => handleBrewCarouselPointerDown(e, brewTamañoScrollRef.current)}
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
                            onClick={() => setSelectedSizeMl(size.ml)}
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
          <p className="section-title brew-select-section-title brew-result-section-title">{t("brew.result")}</p>
          <article className="brew-result-card">
            {isAguaMethod ? (
              <p className="brew-result-title">{t("brew.registerWater", { ml: waterMl })}</p>
            ) : (
              <>
            <div className="brew-result-grid">
              {tasteOptions.map((taste) => (
                <Button
                  variant="plain"
                  key={taste.value}
                  className={`brew-taste-chip ${resultTaste === taste.value ? "is-active" : ""}`.trim()}
                  onClick={() => setResultTaste(taste.value)}
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
                  <strong>{t("brew.recommendation")}</strong>
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
          aria-label={t("brew.baristaTips")}
          onDismiss={() => setShowBaristaPopover(false)}
          onClick={() => setShowBaristaPopover(false)}
        >
          <SheetCard className="diary-sheet brew-barista-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header brew-barista-sheet-header">
              <span className="brew-barista-sheet-header-spacer" aria-hidden="true" />
              <strong className="sheet-title">{t("brew.baristaTips")}</strong>
              <Button
                variant="plain"
                type="button"
                className="brew-barista-sheet-close"
                aria-label={t("common.close")}
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
                    <strong>{translateBaristaLabel(tip.label)}</strong>
                    <em>{translateBaristaValue(tip.value)}</em>
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

const SENSORY_MIN = 0;
const SENSORY_MAX = 5;
const SENSORY_LABELS: Record<string, string> = {
  aroma: "Aroma",
  sabor: "Sabor",
  cuerpo: "Cuerpo",
  acidez: "Acidez",
  dulzura: "Dulzura"
};

function OptionPickerModal({
  open,
  title,
  options,
  selectedSingle,
  selectedMulti,
  multi,
  onSelectSingle,
  onConfirmMulti,
  onClose
}: {
  open: boolean;
  title: string;
  options: string[];
  selectedSingle: string;
  selectedMulti: string[];
  multi: boolean;
  onSelectSingle: (value: string) => void;
  onConfirmMulti: (values: string[]) => void;
  onClose: () => void;
}) {
  const { locale } = useI18n();
  const [tempMulti, setTempMulti] = useState<string[]>(selectedMulti);

  useEffect(() => {
    if (open) setTempMulti(selectedMulti);
  }, [open, selectedMulti]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, onClose]);

  if (!open) return null;

  const handleSingle = (value: string) => {
    onSelectSingle(value);
    onClose();
  };

  const handleConfirmMulti = () => {
    onConfirmMulti(tempMulti);
    onClose();
  };

  const toggleMulti = (value: string) => {
    setTempMulti((prev) =>
      prev.includes(value) ? prev.filter((x) => x !== value) : [...prev, value]
    );
  };

  const content = (
    <SheetOverlay
      role="dialog"
      aria-modal="true"
      aria-label={title}
      onDismiss={onClose}
      onClick={onClose}
    >
      <SheetCard className="diary-sheet create-coffee-picker-sheet" onClick={(e) => e.stopPropagation()}>
        <SheetHandle aria-hidden="true" />
        <header className="sheet-header create-coffee-picker-sheet-header">
          <span className="create-coffee-picker-header-spacer" aria-hidden="true" />
          <strong className="sheet-title">{title}</strong>
          {multi ? (
            <Button variant="plain" type="button" className="create-coffee-picker-apply" onClick={handleConfirmMulti}>
              {locale === "es" ? "Aplicar" : "Apply"}
            </Button>
          ) : (
            <span className="create-coffee-picker-header-spacer" aria-hidden="true" />
          )}
        </header>
        <div className="create-coffee-picker-list">
          {multi ? (
            options.map((opt) => (
              <label key={opt} className="create-coffee-picker-option create-coffee-picker-option-multi">
                <input
                  type="checkbox"
                  checked={tempMulti.includes(opt)}
                  onChange={() => toggleMulti(opt)}
                  className="create-coffee-picker-checkbox"
                />
                <span>{opt}</span>
              </label>
            ))
          ) : (
            options.map((opt) => (
              <button
                key={opt}
                type="button"
                className={`create-coffee-picker-option ${selectedSingle === opt ? "is-selected" : ""}`.trim()}
                onClick={() => handleSingle(opt)}
              >
                {opt}
              </button>
            ))
          )}
        </div>
      </SheetCard>
    </SheetOverlay>
  );

  return typeof document !== "undefined" ? createPortal(content, document.body) : null;
}

export function CreateCoffeeView({
  draft,
  imagePreviewUrl,
  saving,
  error,
  countryOptions = [],
  specialtyOptions = [],
  brandSuggestions = [],
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
  draft: CreateCoffeeDraft;
  imagePreviewUrl: string;
  saving: boolean;
  error: string | null;
  countryOptions?: string[];
  specialtyOptions?: string[];
  /** Marcas ya usadas en cafés creados; se muestran como sugerencias al escribir */
  brandSuggestions?: string[];
  onChange: (next: CreateCoffeeDraft) => void;
  onPickImage: (file: File | null, previewUrl: string) => void;
  onRemoveImage: () => void;
  onClose: () => void;
  onSave: () => void;
  fullPage: boolean;
  hideActions?: boolean;
  hideHead?: boolean;
  showQuantityField?: boolean;
}) {
  const { locale } = useI18n();
  const rootRef = useRef<HTMLElement | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const brandDropdownRef = useRef<HTMLDivElement | null>(null);
  const [attemptedSave, setAttemptedSave] = useState(false);
  const [pickerOpen, setPickerOpen] = useState<PickerId | null>(null);
  const [brandDropdownOpen, setBrandDropdownOpen] = useState(false);
  const brandBlurTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const requiredValues = [
    draft.name.trim(),
    draft.brand.trim(),
    draft.specialty.trim(),
    draft.country.trim(),
    draft.format.trim(),
    draft.totalGrams > 0
  ];
  const missingRequiredCount = requiredValues.filter((value) => !value).length;
  const canSave = missingRequiredCount === 0;
  const nameMissing = attemptedSave && !draft.name.trim();
  const brandMissing = attemptedSave && !draft.brand.trim();
  const specialtyMissing = attemptedSave && !draft.specialty.trim();
  const countryMissing = attemptedSave && !draft.country.trim();
  const formatMissing = attemptedSave && !draft.format.trim();
  const quantityMissing = attemptedSave && !(draft.totalGrams > 0);
  const barcodeInvalid =
    attemptedSave &&
    !!draft.codigo_barras &&
    !/^[0-9]{6,}$/.test(draft.codigo_barras.replace(/\s+/g, ""));
  const productUrlInvalid =
    attemptedSave &&
    !!draft.product_url &&
    !/^https?:\/\/.+/i.test(draft.product_url.trim());

  const filteredBrandSuggestions = useMemo(() => {
    const q = draft.brand.trim().toLowerCase();
    if (!q || !brandSuggestions.length) return [];
    return brandSuggestions.filter((b) => b.toLowerCase().includes(q)).slice(0, 8);
  }, [draft.brand, brandSuggestions]);

  const showBrandDropdown = brandDropdownOpen && filteredBrandSuggestions.length > 0;

  const handleBrandFocus = useCallback(() => {
    if (brandBlurTimeoutRef.current) {
      clearTimeout(brandBlurTimeoutRef.current);
      brandBlurTimeoutRef.current = null;
    }
    setBrandDropdownOpen(true);
  }, []);

  const handleBrandBlur = useCallback(() => {
    brandBlurTimeoutRef.current = setTimeout(() => setBrandDropdownOpen(false), 150);
  }, []);

  const handleSelectBrand = useCallback(
    (brand: string) => {
      onChange({ ...draft, brand });
      setBrandDropdownOpen(false);
    },
    [draft, onChange]
  );

  useEffect(() => {
    return () => {
      if (brandBlurTimeoutRef.current) clearTimeout(brandBlurTimeoutRef.current);
    };
  }, []);

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

  const getPickerConfig = useCallback(
    (id: PickerId) => {
      switch (id) {
        case "specialty":
          return { title: locale === "es" ? "Especialidad" : "Specialty", options: specialtyOptions, single: draft.specialty, multi: [] };
        case "roast":
          return { title: locale === "es" ? "Tueste" : "Roast", options: ROAST_OPTIONS, single: draft.roast, multi: [] };
        case "country":
          return { title: locale === "es" ? "País de origen" : "Origin country", options: countryOptions, single: "", multi: parseMultiValue(draft.country ?? "") };
        case "variety":
          return { title: locale === "es" ? "Variedad o tipo" : "Variety or type", options: VARIETY_OPTIONS, single: "", multi: parseMultiValue(draft.variety ?? "") };
        case "format":
          return { title: locale === "es" ? "Formato" : "Format", options: FORMAT_OPTIONS, single: draft.format, multi: [] };
        case "process":
          return { title: locale === "es" ? "Proceso" : "Process", options: PROCESS_OPTIONS, single: "", multi: parseMultiValue(draft.proceso ?? "") };
        case "grind":
          return { title: locale === "es" ? "Molienda recomendada" : "Recommended grind", options: GRIND_OPTIONS, single: draft.molienda_recomendada ?? "", multi: [] };
        default:
          return { title: "", options: [], single: "", multi: [] };
      }
    },
    [locale, specialtyOptions, countryOptions, draft.specialty, draft.roast, draft.country, draft.variety, draft.format, draft.proceso, draft.molienda_recomendada]
  );
  const createCopy = useMemo(() => {
    if (locale === "en") {
      return {
        ariaCreateCoffee: "Create your coffee",
        subtitle: "Create your coffee and add the bag you will have in your pantry.",
        ready: "Ready to save",
        missing: (n: number) => `${n} required fields missing`,
        removeCoffeeImage: "Remove coffee image",
        addCoffeeImage: "Add coffee photo",
        addPhoto: "Add photo",
        coffeeNamePlaceholder: "Coffee name *",
        coffeeNameAria: "Coffee name (required)",
        roasterPlaceholder: "Roaster",
        roasterAria: "Roaster (required)",
        suggestedRoasters: "Suggested roasters",
        originProfile: "Origin and profile",
        selectSpecialty: "Select specialty",
        selectRoast: "Select roast",
        selectCountry: "Select country(ies)",
        selectVariety: "Select variety(ies)",
        presentation: "Presentation",
        hasCaffeine: "Has caffeine?",
        caffeineCoffee: "Coffee with caffeine",
        selectFormat: "Select format",
        quantityGrams: "Amount (g)",
        quantityAria: "Amount in grams, bag size (required)",
        optionalDetails: "Optional details",
        optionalBadge: "Optional",
        description: "Description",
        descriptionAria: "Coffee description",
        process: "Process",
        selectProcess: "Select process(es)",
        recommendedGrind: "Recommended grind",
        selectGrind: "Select grind",
        barcode: "Barcode",
        productLink: "Product link",
        sensoryProfile: "Sensory profile",
        sensoryHint: "Rating from 0 to 5 (0 = not rated)",
        specialtyRequiredAria: "Specialty (required)",
        roastAria: "Roast",
        countryRequiredAria: "Origin country (required)",
        varietyAria: "Variety or type",
        apply: "Apply",
        cancel: "Cancel",
        saving: "Saving...",
        saveCoffee: "Save coffee",
        save: "Save",
        totalCoffeeAmount: "Total coffee amount (g)",
        remainingCoffeeAmount: "Remaining coffee amount (g)"
      };
    }
    return {
      ariaCreateCoffee: "Crea tu café",
      subtitle: "Crea tu café y añade la bolsa que tendrás en tu despensa.",
      ready: "Listo para guardar",
      missing: (n: number) => `Faltan ${n} campos obligatorios`,
      removeCoffeeImage: "Quitar imagen del café",
      addCoffeeImage: "Añadir foto del café",
      addPhoto: "Añadir foto",
      coffeeNamePlaceholder: "Nombre del café *",
      coffeeNameAria: "Nombre del café (obligatorio)",
      roasterPlaceholder: "Tostador",
      roasterAria: "Tostador (obligatorio)",
      suggestedRoasters: "Tostadores sugeridos",
      originProfile: "Origen y perfil",
      selectSpecialty: "Seleccionar especialidad",
      selectRoast: "Seleccionar tueste",
      selectCountry: "Seleccionar país(es)",
      selectVariety: "Seleccionar variedad(es)",
      presentation: "Presentación",
      hasCaffeine: "¿Tiene cafeína?",
      caffeineCoffee: "Café con cafeína",
      selectFormat: "Seleccionar formato",
      quantityGrams: "Cantidad (g)",
      quantityAria: "Cantidad en gramos, tamaño de la bolsa (obligatorio)",
      optionalDetails: "Detalles opcionales",
      optionalBadge: "Opcional",
      description: "Descripción",
      descriptionAria: "Descripción del café",
      process: "Proceso",
      selectProcess: "Seleccionar proceso(s)",
      recommendedGrind: "Molienda recomendada",
      selectGrind: "Seleccionar molienda",
      barcode: "Código de barras",
      productLink: "Enlace al producto",
      sensoryProfile: "Perfil sensorial",
      sensoryHint: "Valoración del 0 al 5 (0 = no valorar)",
      specialtyRequiredAria: "Especialidad (obligatorio)",
      roastAria: "Tueste",
      countryRequiredAria: "País de origen (obligatorio)",
      varietyAria: "Variedad o tipo",
      apply: "Aplicar",
      cancel: "Cancelar",
      saving: "Guardando...",
      saveCoffee: "Guardar café",
      save: "Guardar",
      totalCoffeeAmount: "Cantidad de café total (g)",
      remainingCoffeeAmount: "Cantidad de café restante (g)"
    };
  }, [locale]);

  return (
    <section
      ref={rootRef}
      className={`create-coffee-view create-coffee-form ${fullPage ? "is-full-page" : "is-side-panel"}`.trim()}
      aria-label={createCopy.ariaCreateCoffee}
    >
      {!hideHead ? (
        <header className="create-coffee-head">
          <p className="create-coffee-subtitle">{createCopy.subtitle}</p>
        </header>
      ) : null}

      {attemptedSave ? (
        <p className="create-coffee-status" role="status" aria-live="polite">
          {canSave ? createCopy.ready : createCopy.missing(missingRequiredCount)}
        </p>
      ) : null}

      <div className="create-coffee-form-body">
        {/* ——— Hero: foto + nombre + marca ——— */}
        <div className="create-coffee-card create-coffee-hero">
          <div className="create-coffee-photo-wrap">
            <Input
              ref={imageInputRef}
              type="file"
              accept="image/*"
              className="file-input-hidden"
              onChange={async (event) => {
                const file = event.target.files?.[0] ?? null;
                if (!file) return;
                try {
                  const webpBlob = await convertImageToWebP(file);
                  const webpFile = webPBlobToFile(webpBlob, file.name);
                  onPickImage(webpFile, URL.createObjectURL(webpBlob));
                } catch {
                  onPickImage(file, URL.createObjectURL(file));
                }
                event.currentTarget.value = "";
              }}
            />
            {imagePreviewUrl ? (
              <div className="create-coffee-photo-preview">
                <img src={imagePreviewUrl} alt="" loading="lazy" decoding="async" />
                <Button
                  variant="plain"
                  className="create-coffee-photo-remove"
                  onClick={onRemoveImage}
                  aria-label={createCopy.removeCoffeeImage}
                >
                  <UiIcon name="close" className="ui-icon" />
                </Button>
              </div>
            ) : (
              <button
                type="button"
                className="create-coffee-photo-placeholder"
                onClick={() => imageInputRef.current?.click()}
                aria-label={createCopy.addCoffeeImage}
              >
                <span className="material-symbols-outlined create-coffee-photo-icon" aria-hidden="true">photo_camera</span>
                <span className="create-coffee-photo-text">{createCopy.addPhoto}</span>
              </button>
            )}
          </div>
          <div className="create-coffee-hero-fields">
            <div className={`create-coffee-group ${nameMissing ? "is-invalid" : ""}`.trim()}>
              <Input
                variant="default"
                className="create-coffee-input search-wide"
                placeholder={createCopy.coffeeNamePlaceholder}
                value={draft.name}
                onChange={(e) => onChange({ ...draft, name: e.target.value })}
                aria-invalid={nameMissing}
                aria-label={createCopy.coffeeNameAria}
              />
            </div>
            <div className="create-coffee-field-divider" aria-hidden="true" />
            <div className={`create-coffee-group create-coffee-brand-group ${brandMissing ? "is-invalid" : ""}`.trim()} ref={brandDropdownRef}>
              <div className="create-coffee-brand-input-wrap">
                <Input
                  variant="default"
                  className="create-coffee-input search-wide"
                  placeholder={createCopy.roasterPlaceholder}
                  value={draft.brand}
                  onChange={(e) => onChange({ ...draft, brand: e.target.value })}
                  onFocus={handleBrandFocus}
                  onBlur={handleBrandBlur}
                  aria-invalid={brandMissing}
                  aria-label={createCopy.roasterAria}
                  aria-autocomplete="list"
                  aria-expanded={showBrandDropdown}
                  aria-controls={showBrandDropdown ? "create-coffee-brand-listbox" : undefined}
                />
                {showBrandDropdown ? (
                  <div
                    id="create-coffee-brand-listbox"
                    className="create-coffee-brand-suggestions-wrap"
                    role="listbox"
                    aria-label={createCopy.suggestedRoasters}
                    onMouseDown={(e) => e.preventDefault()}
                  >
                    <p className="create-coffee-brand-suggestions-title">{createCopy.suggestedRoasters}</p>
                    <ul className="create-coffee-brand-suggestions" role="group">
                      {filteredBrandSuggestions.map((brand) => (
                        <li key={brand} role="option">
                          <button
                            type="button"
                            className="create-coffee-brand-suggestion-item"
                            onMouseDown={() => handleSelectBrand(brand)}
                          >
                            {brand}
                          </button>
                        </li>
                      ))}
                    </ul>
                  </div>
                ) : null}
              </div>
            </div>
          </div>
        </div>

        {/* ——— Origen y perfil ——— */}
        <section className="create-coffee-form-section">
          <h3 className="create-coffee-section-heading">{createCopy.originProfile}</h3>
          <div className="create-coffee-card">
            <div className={`create-coffee-group ${specialtyMissing ? "is-invalid" : ""}`.trim()}>
              <button
                type="button"
                className="create-coffee-picker-trigger search-wide"
                onClick={() => setPickerOpen("specialty")}
                aria-haspopup="dialog"
                aria-expanded={pickerOpen === "specialty"}
                aria-label={createCopy.specialtyRequiredAria}
              >
                <span className={draft.specialty ? "" : "create-coffee-picker-placeholder"}>
                  {draft.specialty || createCopy.selectSpecialty}
                </span>
                <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
              </button>
            </div>
            <div className="create-coffee-field-divider" aria-hidden="true" />
            <div className="create-coffee-group">
              <button
                type="button"
                className="create-coffee-picker-trigger search-wide"
                onClick={() => setPickerOpen("roast")}
                aria-haspopup="dialog"
                aria-expanded={pickerOpen === "roast"}
                aria-label={createCopy.roastAria}
              >
                <span className={draft.roast ? "" : "create-coffee-picker-placeholder"}>{draft.roast || createCopy.selectRoast}</span>
                <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
              </button>
            </div>
            <div className="create-coffee-field-divider" aria-hidden="true" />
            <div className={`create-coffee-group ${countryMissing ? "is-invalid" : ""}`.trim()}>
              <button
                type="button"
                className="create-coffee-picker-trigger search-wide"
                onClick={() => setPickerOpen("country")}
                aria-haspopup="dialog"
                aria-expanded={pickerOpen === "country"}
                aria-label={createCopy.countryRequiredAria}
              >
                <span className={displayMultiValue(draft.country) ? "" : "create-coffee-picker-placeholder"}>
                  {displayMultiValue(draft.country) || createCopy.selectCountry}
                </span>
                <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
              </button>
            </div>
            <div className="create-coffee-field-divider" aria-hidden="true" />
            <div className="create-coffee-group">
              <button
                type="button"
                className="create-coffee-picker-trigger search-wide"
                onClick={() => setPickerOpen("variety")}
                aria-haspopup="dialog"
                aria-expanded={pickerOpen === "variety"}
                aria-label={createCopy.varietyAria}
              >
                <span className={displayMultiValue(draft.variety) ? "" : "create-coffee-picker-placeholder"}>
                  {displayMultiValue(draft.variety) || createCopy.selectVariety}
                </span>
                <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
              </button>
            </div>
          </div>
        </section>

        {/* ——— Presentación ——— */}
        <section className="create-coffee-form-section">
          <h3 className="create-coffee-section-heading">{createCopy.presentation}</h3>
          <div className={`create-coffee-card ${formatMissing || quantityMissing ? "is-invalid" : ""}`.trim()}>
            <div className="create-coffee-row create-coffee-row-caffeine">
              <span className="create-coffee-label-inline">{createCopy.hasCaffeine}</span>
              <Switch
                checked={draft.hasCaffeine}
                className="create-coffee-switch"
                onClick={() => onChange({ ...draft, hasCaffeine: !draft.hasCaffeine })}
                aria-label={createCopy.caffeineCoffee}
              />
            </div>
            <div className="create-coffee-field-divider" aria-hidden="true" />
            <div className="create-coffee-group">
              <button
                type="button"
                className="create-coffee-picker-trigger search-wide"
                onClick={() => setPickerOpen("format")}
                aria-haspopup="dialog"
                aria-expanded={pickerOpen === "format"}
                aria-label={locale === "es" ? `${createCopy.selectFormat} (obligatorio)` : `${createCopy.selectFormat} (required)`}
              >
                <span className={draft.format ? "" : "create-coffee-picker-placeholder"}>{draft.format || createCopy.selectFormat}</span>
                <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
              </button>
            </div>
            <div className="create-coffee-field-divider" aria-hidden="true" />
            <div className={`create-coffee-row create-coffee-quantity-row ${quantityMissing ? "is-invalid" : ""}`.trim()}>
              <span className="create-coffee-label-inline">{createCopy.quantityGrams}</span>
              <Input
                variant="default"
                className="create-coffee-input create-coffee-quantity-input search-wide"
                type="number"
                inputMode="numeric"
                min={1}
                max={5000}
                value={draft.totalGrams > 0 ? String(draft.totalGrams) : ""}
                onChange={(e) => {
                  const v = e.target.value;
                  const n = v === "" ? 0 : Math.max(0, Math.min(5000, parseInt(v, 10) || 0));
                  onChange({ ...draft, totalGrams: n });
                }}
                placeholder=""
                aria-label={createCopy.quantityAria}
                aria-invalid={quantityMissing}
              />
            </div>
          </div>
        </section>

        {/* ——— Detalles opcionales ——— */}
        <section className="create-coffee-form-section">
          <h3 className="create-coffee-section-heading">
            {createCopy.optionalDetails}
            <span className="create-coffee-badge">{createCopy.optionalBadge}</span>
          </h3>
          <div className="create-coffee-card create-coffee-card-optional">
            <div className="create-coffee-details-grid">
              <div className="create-coffee-group create-coffee-group-full">
                <textarea
                  className="create-coffee-textarea search-wide"
                  placeholder={createCopy.description}
                  value={draft.descripcion ?? ""}
                  onChange={(e) => onChange({ ...draft, descripcion: e.target.value })}
                  rows={3}
                  aria-label={createCopy.descriptionAria}
                />
              </div>
              <div className="create-coffee-field-divider" aria-hidden="true" />
              <div className="create-coffee-group create-coffee-group-full">
                <button
                  type="button"
                  className="create-coffee-picker-trigger search-wide"
                  onClick={() => setPickerOpen("process")}
                  aria-haspopup="dialog"
                  aria-expanded={pickerOpen === "process"}
                  aria-label={createCopy.process}
                >
                  <span className={`create-coffee-picker-trigger-text ${displayMultiValue(draft.proceso) ? "" : "create-coffee-picker-placeholder"}`.trim()}>
                    {displayMultiValue(draft.proceso) || createCopy.selectProcess}
                  </span>
                  <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron create-coffee-picker-chevron-right" aria-hidden />
                </button>
              </div>
              <div className="create-coffee-field-divider" aria-hidden="true" />
              <div className="create-coffee-group create-coffee-group-full">
                <button
                  type="button"
                  className="create-coffee-picker-trigger search-wide"
                  onClick={() => setPickerOpen("grind")}
                  aria-haspopup="dialog"
                  aria-expanded={pickerOpen === "grind"}
                  aria-label={createCopy.recommendedGrind}
                >
                  <span className={`create-coffee-picker-trigger-text ${draft.molienda_recomendada ? "" : "create-coffee-picker-placeholder"}`.trim()}>
                    {draft.molienda_recomendada || createCopy.selectGrind}
                  </span>
                  <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron create-coffee-picker-chevron-right" aria-hidden />
                </button>
              </div>
              <div className="create-coffee-field-divider" aria-hidden="true" />
              <div className={`create-coffee-group create-coffee-group-full ${barcodeInvalid ? "is-invalid" : ""}`.trim()}>
                <Input
                  variant="default"
                  className="create-coffee-input search-wide"
                  type="text"
                  inputMode="numeric"
                  placeholder={createCopy.barcode}
                  value={draft.codigo_barras ?? ""}
                  onChange={(e) => onChange({ ...draft, codigo_barras: e.target.value })}
                  aria-label={createCopy.barcode}
                  aria-invalid={barcodeInvalid}
                />
              </div>
              <div className="create-coffee-field-divider" aria-hidden="true" />
              <div className={`create-coffee-group create-coffee-group-full ${productUrlInvalid ? "is-invalid" : ""}`.trim()}>
                <Input
                  variant="default"
                  className="create-coffee-input search-wide"
                  type="url"
                  inputMode="url"
                  placeholder={createCopy.productLink}
                  value={draft.product_url ?? ""}
                  onChange={(e) => onChange({ ...draft, product_url: e.target.value })}
                  aria-label={createCopy.productLink}
                  aria-invalid={productUrlInvalid}
                />
              </div>
            </div>
          </div>
        </section>

        {/* ——— Perfil sensorial ——— */}
        <section className="create-coffee-form-section">
          <h3 className="create-coffee-section-heading">
            {createCopy.sensoryProfile}
            <span className="create-coffee-badge">{createCopy.optionalBadge}</span>
          </h3>
          <div className="create-coffee-card create-coffee-card-optional">
            <p className="create-coffee-hint-block">{createCopy.sensoryHint}</p>
          <div className="create-coffee-sensory-list">
            {(["aroma", "sabor", "cuerpo", "acidez", "dulzura"] as const).map((key) => {
              const val = draft[key] ?? null;
              const num = val != null && Number.isFinite(val) ? Math.max(SENSORY_MIN, Math.min(SENSORY_MAX, val)) : 0;
              return (
                <label key={key} className="create-coffee-sensory-item">
                  <span className="create-coffee-sensory-name">
                    <strong>{SENSORY_LABELS[key]}</strong>
                    <small>{num}</small>
                  </span>
                  <input
                    type="range"
                    className="app-range create-coffee-sensory-range search-wide"
                    min={SENSORY_MIN}
                    max={SENSORY_MAX}
                    step={1}
                    value={num}
                    style={{ "--range-progress": `${((num - SENSORY_MIN) / (SENSORY_MAX - SENSORY_MIN)) * 100}%` } as CSSProperties}
                    onChange={(e) => {
                      const v = parseInt(e.target.value, 10);
                      const n = Number.isFinite(v) ? Math.max(SENSORY_MIN, Math.min(SENSORY_MAX, v)) : 0;
                      onChange({ ...draft, [key]: n });
                    }}
                    aria-label={`${SENSORY_LABELS[key]} de 1 a 5`}
                  />
                </label>
              );
            })}
          </div>
          </div>
        </section>
      </div>

      {pickerOpen ? (
        (() => {
          const cfg = getPickerConfig(pickerOpen);
          const isMulti = pickerOpen === "country" || pickerOpen === "variety" || pickerOpen === "process";
          return (
            <OptionPickerModal
              open={true}
              title={cfg.title}
              options={cfg.options}
              selectedSingle={cfg.single}
              selectedMulti={cfg.multi}
              multi={isMulti}
              onSelectSingle={(v) => {
                if (pickerOpen === "specialty") onChange({ ...draft, specialty: v });
                else if (pickerOpen === "roast") onChange({ ...draft, roast: v });
                else if (pickerOpen === "format") onChange({ ...draft, format: v });
                else if (pickerOpen === "grind") onChange({ ...draft, molienda_recomendada: v });
              }}
              onConfirmMulti={(arr) => {
                const s = formatMultiValue(arr);
                if (pickerOpen === "country") onChange({ ...draft, country: s });
                else if (pickerOpen === "variety") onChange({ ...draft, variety: s });
                else if (pickerOpen === "process") onChange({ ...draft, proceso: s });
              }}
              onClose={() => setPickerOpen(null)}
            />
          );
        })()
      ) : null}

      {error ? (
        <div className="create-coffee-error-wrap" role="alert">
          <p className="create-coffee-error">{error}</p>
        </div>
      ) : null}

      {!hideActions && !fullPage ? (
        <footer className="create-coffee-actions">
          <Button variant="plain" className="create-coffee-btn create-coffee-btn-ghost" onClick={onClose} disabled={saving}>
            {createCopy.cancel}
          </Button>
          <Button variant="plain" className="create-coffee-btn create-coffee-btn-primary" onClick={handleSaveAttempt} disabled={saving}>
            {saving ? createCopy.saving : createCopy.saveCoffee}
          </Button>
        </footer>
      ) : null}
      {!hideActions && fullPage ? (
        <Button variant="plain" className="create-coffee-btn create-coffee-btn-primary create-coffee-mobile-save" onClick={handleSaveAttempt} disabled={saving}>
          {saving ? createCopy.saving : createCopy.saveCoffee}
        </Button>
      ) : null}
      {hideActions && !fullPage ? (
        <footer className="create-coffee-actions create-coffee-actions-desktop-save">
          <Button variant="plain" className="create-coffee-btn create-coffee-btn-primary" onClick={handleSaveAttempt} disabled={saving}>
            {saving ? createCopy.saving : createCopy.save}
          </Button>
        </footer>
      ) : null}
    </section>
  );
}












