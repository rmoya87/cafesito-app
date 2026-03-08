import { type CSSProperties, type PointerEvent as ReactPointerEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { MentionText } from "../../ui/MentionText";
import {
  caffeineTargetMg,
  hydrationProgressPercent,
  hydrationTargetMl,
  last30DaysDailyAverages,
  trendPercent
} from "../../core/diaryAnalytics";
import { EMPTY } from "../../core/emptyErrorStrings";
import { normalizeLookupText } from "../../core/text";
import { UiIcon, type IconName } from "../../ui/iconography";
import { Button, Input, SheetCard, SheetHandle, SheetOverlay, TabButton, Tabs } from "../../ui/components";
import type { CoffeeRow, DiaryEntryRow, PantryItemRow } from "../../types";

// Strings con acentos generados en runtime para evitar problemas de encoding del archivo
const _c = (code: number) => String.fromCharCode(code);
const DIARY_STR = {
  CAFEINA_ESTIMADA: "CAFE" + _c(0x00CD) + "NA ESTIMADA",
  ARIA_CAFEINA: "Informaci" + _c(0x00F3) + "n de cafe" + _c(0x00ED) + "na estimada",
  TOOLTIP_ESTIMACION: "Estimaci" + _c(0x00F3) + "n basada en tus registros de consumo en el periodo seleccionado.",
  HIDRATACION: "HIDRATACI" + _c(0x00D3) + "N",
  GRAFICO_CONSUMO: "Gr" + _c(0x00E1) + "fico de consumo",
  CONFIRMAR_ELIMINACION: "Confirmar eliminaci" + _c(0x00F3) + "n",
  EDITAR_REGISTRO_CAFE: "Editar registro de caf" + _c(0x00E9),
  PREPARACION: "Preparaci" + _c(0x00F3) + "n",
  PREPARACION_UPPER: "PREPARACI" + _c(0x00D3) + "N",
  CAFEINA: "Cafe" + _c(0x00ED) + "na",
  CAFEINA_UPPER: "CAFE" + _c(0x00CD) + "NA",
  CAFEINA_MG: "Cafe" + _c(0x00ED) + "na (mg)",
  TAMANO: "Tama" + _c(0x00F1) + "o",
  TAMANO_UPPER: "TAMA" + _c(0x00D1) + "O",
  PEQUENO: "Peque" + _c(0x00F1) + "o",
  SIN_METODO: "Sin m" + _c(0x00E9) + "todo",
  SIN_CAFE_AGUA: "Sin caf" + _c(0x00E9) + " o agua registrada",
  NO_HAY_CAFE_DESPENSA: "No hay caf" + _c(0x00E9) + " en tu despensa",
  SIFON: "Sif" + _c(0x00F3) + "n",
  CAFE_LABEL: "Caf" + _c(0x00E9),
  REGISTRO_RAPIDO: "Registro r" + _c(0x00E1) + "pido",
  CAFE_BRAND: "CAF" + _c(0x00C9),
  CANTIDAD: "Cantidad",
  METODO_UPPER: "M" + _c(0x00C9) + "TODO",
} as const;

/** Formatea solo dígitos a hh:mm insertando ":" tras 2 dígitos (teclado numérico sin dos puntos). */
function formatTimeInputToHhMm(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 4);
  if (digits.length === 0) return "";
  if (digits.length === 1) return digits;
  if (digits.length === 2) return `${digits}:`;
  if (digits.length === 3) return `${digits.slice(0, 2)}:${digits.slice(2, 3)}`;
  return `${digits.slice(0, 2)}:${digits.slice(2, 4)}`;
}

export function DiaryView({
  mode,
  tab,
  setTab,
  period,
  selectedDiaryDate,
  entries,
  coffeeCatalog,
  pantryRows,
  onDeleteEntry,
  onEditEntry,
  onUpdatePantryStock,
  onRemovePantryItem,
  onOpenCoffee,
  onOpenQuickActions
}: {
  mode: "mobile" | "desktop";
  tab: "actividad" | "despensa";
  setTab: (value: "actividad" | "despensa") => void;
  period: "hoy" | "7d" | "30d";
  /** Cuando period === "hoy", fecha seleccionada en formato YYYY-MM-DD */
  selectedDiaryDate?: string;
  entries: DiaryEntryRow[];
  coffeeCatalog: CoffeeRow[];
  pantryRows: Array<{ item: PantryItemRow; coffee?: CoffeeRow }>;
  onDeleteEntry: (entryId: number) => Promise<void>;
  onEditEntry: (
    entryId: number,
    amountMl: number,
    caffeineMg: number,
    preparationType: string,
    coffeeGrams?: number,
    sizeLabel?: string | null,
    timestampMs?: number
  ) => Promise<void>;
  onUpdatePantryStock: (coffeeId: string, totalGrams: number, gramsRemaining: number) => Promise<void>;
  onRemovePantryItem: (coffeeId: string) => Promise<void>;
  onOpenCoffee: (coffeeId: string) => void;
  onOpenQuickActions: () => void;
}) {
  const diaryTabBarRef = useRef<HTMLDivElement>(null);
  const panelsWrapRef = useRef<HTMLDivElement>(null);
  const [dragOffsetPx, setDragOffsetPx] = useState<number | null>(null);
  const tabDragRefs = useRef({
    startX: 0,
    startY: 0,
    startOffsetPx: 0,
    wrapWidth: 0,
    isDragging: false,
    committed: false,
    lastOffsetPx: 0
  });
  const COMMIT_THRESHOLD_PX = 4;

  const onTabPanelsTouchStart = useCallback(
    (e: React.TouchEvent) => {
      if (e.touches.length !== 1) return;
      const wrap = panelsWrapRef.current;
      if (!wrap) return;
      const w = wrap.clientWidth;
      tabDragRefs.current.wrapWidth = w;
      tabDragRefs.current.startX = e.touches[0].clientX;
      tabDragRefs.current.startY = e.touches[0].clientY;
      tabDragRefs.current.startOffsetPx = tab === "actividad" ? 0 : -w;
      tabDragRefs.current.isDragging = false;
      tabDragRefs.current.committed = false;
      tabDragRefs.current.lastOffsetPx = tab === "actividad" ? 0 : -w;
    },
    [tab]
  );

  const onTabPanelsTouchMove = useCallback((e: React.TouchEvent) => {
    if (e.touches.length !== 1) return;
    const r = tabDragRefs.current;
    const deltaX = e.touches[0].clientX - r.startX;
    const deltaY = e.touches[0].clientY - r.startY;
    if (!r.committed) {
      if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > COMMIT_THRESHOLD_PX) {
        r.committed = true;
        r.isDragging = true;
        const next = Math.max(-r.wrapWidth, Math.min(0, r.startOffsetPx + deltaX));
        r.lastOffsetPx = next;
        setDragOffsetPx(next);
      } else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > COMMIT_THRESHOLD_PX) {
        r.committed = true;
      }
      return;
    }
    if (!r.isDragging) return;
    const next = Math.max(-r.wrapWidth, Math.min(0, r.startOffsetPx + deltaX));
    r.lastOffsetPx = next;
    setDragOffsetPx(next);
  }, []);

  const onTabPanelsTouchEnd = useCallback((e: React.TouchEvent) => {
    const r = tabDragRefs.current;
    if (!r.isDragging) return;
    e.preventDefault();
    e.stopPropagation();
    r.isDragging = false;
    r.committed = false;
    const current = r.lastOffsetPx;
    const threshold = -r.wrapWidth / 2;
    setTab(current > threshold ? "actividad" : "despensa");
    setDragOffsetPx(null);
  }, [setTab]);

  useEffect(() => {
    const el = diaryTabBarRef.current;
    if (!el) return;
    const onMove = (e: TouchEvent) => {
      if (tabDragRefs.current.isDragging && e.cancelable) e.preventDefault();
    };
    el.addEventListener("touchmove", onMove, { passive: false });
    return () => el.removeEventListener("touchmove", onMove);
  }, []);

  const [deletingEntryId, setDeletingEntryId] = useState<number | null>(null);
  const [editEntryId, setEditEntryId] = useState<number | null>(null);
  const [editAmountMl, setEditAmountMl] = useState("");
  const [editCaffeineMg, setEditCaffeineMg] = useState("");
  const [editPreparationType, setEditPreparationType] = useState("");
  const [editDoseGrams, setEditDoseGrams] = useState("");
  const [editTimeText, setEditTimeText] = useState("");
  const [savingEditEntry, setSavingEditEntry] = useState(false);
  const [pantryOptionsCoffeeId, setPantryOptionsCoffeeId] = useState<string | null>(null);
  const [pantryDeleteConfirmCoffeeId, setPantryDeleteConfirmCoffeeId] = useState<string | null>(null);
  const [stockEditCoffeeId, setStockEditCoffeeId] = useState<string | null>(null);
  const [stockEditTotal, setStockEditTotal] = useState("");
  const [stockEditRemaining, setStockEditRemaining] = useState("");
  const [savingStock, setSavingStock] = useState(false);
  const [removingStock, setRemovingStock] = useState(false);
  const [showCaffeineInfo, setShowCaffeineInfo] = useState(false);
  const editPrepScrollRef = useRef<HTMLDivElement | null>(null);
  const editSizeScrollRef = useRef<HTMLDivElement | null>(null);
  const editScrollPointerIdRef = useRef<number | null>(null);
  const editScrollStartXRef = useRef(0);
  const editScrollStartLeftRef = useRef(0);
  const editScrollTargetRef = useRef<HTMLDivElement | null>(null);
  const editScrollActiveRef = useRef(false);
  const editScrollRafRef = useRef<number | null>(null);
  const editScrollPendingLeftRef = useRef(0);
  const [editChipsDragging, setEditChipsDragging] = useState(false);
  const [prepLeftFade, setPrepLeftFade] = useState(false);
  const [prepRightFade, setPrepRightFade] = useState(false);
  const [sizeLeftFade, setSizeLeftFade] = useState(false);
  const [sizeRightFade, setSizeRightFade] = useState(false);
  const visibleEntries = useMemo(() => {
    const now = new Date();
    const start = new Date(now);
    if (period === "hoy") {
      if (selectedDiaryDate) {
        const [y, m, d] = selectedDiaryDate.split("-").map(Number);
        start.setFullYear(y, (m ?? 1) - 1, d ?? 1);
      }
      start.setHours(0, 0, 0, 0);
    } else if (period === "7d") {
      const day = start.getDay(); // Sunday=0
      const diffToMonday = day === 0 ? 6 : day - 1;
      start.setDate(start.getDate() - diffToMonday);
      start.setHours(0, 0, 0, 0);
    } else {
      start.setDate(1);
      start.setHours(0, 0, 0, 0);
    }
    const startMs = start.getTime();
    let filtered = entries.filter(
      (entry) => Number(entry.timestamp) >= startMs && (entry.type || "").toUpperCase() !== "NOTE"
    );
    if (period === "hoy" && selectedDiaryDate) {
      const [y, m, d] = selectedDiaryDate.split("-").map(Number);
      const dayEnd = new Date(y, (m ?? 1) - 1, (d ?? 1) + 1);
      dayEnd.setHours(0, 0, 0, 0);
      const dayEndMs = dayEnd.getTime();
      filtered = filtered.filter((entry) => Number(entry.timestamp) < dayEndMs);
    }
    return filtered.sort((a, b) => Number(b.timestamp) - Number(a.timestamp));
  }, [entries, period, selectedDiaryDate]);

  const analytics = useMemo(() => {
    const caffeine = visibleEntries.reduce((acc, entry) => acc + Math.max(0, entry.caffeine_mg || 0), 0);
    const hydrationMl = visibleEntries
      .filter((entry) => (entry.type || "").toUpperCase() === "WATER")
      .reduce((acc, entry) => acc + Math.max(0, entry.amount_ml || 0), 0);
    const coffeeCups = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() !== "WATER").length;
    const waterEntries = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() === "WATER").length;
    return { caffeine, hydrationMl, coffeeCups, waterEntries };
  }, [visibleEntries]);

  const last30Avg = useMemo(() => {
    const now = Date.now();
    const dayMs = 86400000;
    const cutoff = now - 30 * dayMs;
    const entriesLast30 = entries.filter((e) => Number(e.timestamp) >= cutoff);
    return last30DaysDailyAverages(entriesLast30);
  }, [entries]);
  const hydrationTarget = hydrationTargetMl(period);
  const caffeineTarget = caffeineTargetMg(period);
  const hydrationProgressPct = hydrationProgressPercent(analytics.hydrationMl, period);
  const caffeineTrendPct = trendPercent(analytics.caffeine, caffeineTarget);
  const hydrationTrendPct = trendPercent(analytics.hydrationMl, hydrationTarget);
  const chartData = useMemo(() => {
    if (period === "hoy") {
      const byHour = new Map<number, { caffeine: number; water: number }>();
      visibleEntries.forEach((entry) => {
        const hour = new Date(entry.timestamp).getHours();
        const prev = byHour.get(hour) ?? { caffeine: 0, water: 0 };
        if ((entry.type || "").toUpperCase() === "WATER") {
          prev.water += Math.max(0, entry.amount_ml || 0);
        } else {
          prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
        }
        byHour.set(hour, prev);
      });
      return Array.from({ length: 24 }).map((_, idx) => {
        const values = byHour.get(idx) ?? { caffeine: 0, water: 0 };
        return { label: `${String(idx).padStart(2, "0")}`, caffeine: values.caffeine, water: values.water };
      });
    }

    if (period === "7d") {
      const dayLabels = ["L", "M", "X", "J", "V", "S", "D"];
      const byDay = new Map<number, { caffeine: number; water: number }>();
      visibleEntries.forEach((entry) => {
        const jsDay = new Date(entry.timestamp).getDay(); // Sun=0..Sat=6
        const mondayStartIndex = jsDay === 0 ? 6 : jsDay - 1;
        const prev = byDay.get(mondayStartIndex) ?? { caffeine: 0, water: 0 };
        if ((entry.type || "").toUpperCase() === "WATER") {
          prev.water += Math.max(0, entry.amount_ml || 0);
        } else {
          prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
        }
        byDay.set(mondayStartIndex, prev);
      });
      return dayLabels.map((label, idx) => {
        const values = byDay.get(idx) ?? { caffeine: 0, water: 0 };
        return { label, caffeine: values.caffeine, water: values.water };
      });
    }

    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const byDay = new Map<number, { caffeine: number; water: number }>();
    visibleEntries.forEach((entry) => {
      const d = new Date(entry.timestamp);
      const day = d.getDate();
      const prev = byDay.get(day) ?? { caffeine: 0, water: 0 };
      if ((entry.type || "").toUpperCase() === "WATER") {
        prev.water += Math.max(0, entry.amount_ml || 0);
      } else {
        prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
      }
      byDay.set(day, prev);
    });
    return Array.from({ length: daysInMonth }).map((_, idx) => {
      const day = idx + 1;
      const values = byDay.get(day) ?? { caffeine: 0, water: 0 };
      return { label: String(day), caffeine: values.caffeine, water: values.water };
    });
  }, [period, visibleEntries]);
  const caffeineTargetPerSlot = useMemo(() => {
    if (period === "hoy") return caffeineTarget;
    if (period === "7d") return caffeineTarget / 7;
    return caffeineTarget / Math.max(1, chartData.length);
  }, [period, caffeineTarget, chartData.length]);
  const chartMaxCaffeine = useMemo(() => {
    const seriesMax = Math.max(1, ...chartData.map((item) => item.caffeine));
    const adjustedTarget = Math.max(1, Math.round(caffeineTargetPerSlot * 0.6));
    return Math.max(seriesMax, adjustedTarget);
  }, [caffeineTargetPerSlot, chartData]);
  const chartMaxWater = useMemo(() => Math.max(1, ...chartData.map((item) => item.water)), [chartData]);
  const chartScrollRef = useRef<HTMLDivElement | null>(null);
  const chartPointerIdRef = useRef<number | null>(null);
  const chartDragStartXRef = useRef(0);
  const chartDragStartScrollRef = useRef(0);
  const [chartDragging, setChartDragging] = useState(false);

  /** Índice del slot actual (hora, día de la semana o día del mes) para centrar el gráfico. */
  const currentSlotIndex = useMemo(() => {
    const now = new Date();
    if (period === "hoy") return now.getHours();
    if (period === "7d") {
      const d = now.getDay();
      return d === 0 ? 6 : d - 1;
    }
    return now.getDate() - 1;
  }, [period]);

  useEffect(() => {
    const node = chartScrollRef.current;
    if (!node || chartData.length === 0) return;
    const colWidth = period === "7d" ? node.scrollWidth / chartData.length : 47;
    const viewportWidth = node.clientWidth;
    const maxScroll = Math.max(0, node.scrollWidth - viewportWidth);
    const targetScroll = Math.max(0, Math.min(maxScroll, currentSlotIndex * colWidth - viewportWidth / 2 + colWidth / 2));
    node.scrollLeft = targetScroll;
  }, [period, chartData.length, currentSlotIndex]);
  const sortedPantryRows = useMemo(
    () => [...pantryRows].sort((a, b) => Number(b.item.last_updated || 0) - Number(a.item.last_updated || 0)),
    [pantryRows]
  );
  const entryImageByCoffeeId = useMemo(() => {
    const map = new Map<string, string>();
    coffeeCatalog.forEach((coffee) => {
      const id = String(coffee.id || "");
      const image = String(coffee.image_url || "");
      if (id && image && !map.has(id)) map.set(id, image);
    });
    sortedPantryRows.forEach((row) => {
      const id = String(row.item.coffee_id || "");
      const image = row.coffee?.image_url || "";
      if (id && image && !map.has(id)) map.set(id, image);
    });
    return map;
  }, [coffeeCatalog, sortedPantryRows]);
  const entryBrandByCoffeeId = useMemo(() => {
    const map = new Map<string, string>();
    coffeeCatalog.forEach((coffee) => {
      const id = String(coffee.id || "");
      const brand = String((coffee.marca || "").trim());
      if (id && brand && !map.has(id)) map.set(id, brand);
    });
    sortedPantryRows.forEach((row) => {
      const id = String(row.item.coffee_id || "");
      const brand = (row.coffee?.marca || "").trim();
      if (id && brand && !map.has(id)) map.set(id, brand);
    });
    return map;
  }, [coffeeCatalog, sortedPantryRows]);
  const diaryRows = useMemo(() => {
    const rows: Array<{ type: "header"; key: string; label: string } | { type: "entry"; key: string; entry: DiaryEntryRow }> = [];
    let lastDayKey = "";
    visibleEntries.forEach((entry) => {
      const date = new Date(entry.timestamp);
      const dayKey = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
      if (period !== "hoy" && dayKey !== lastDayKey) {
        rows.push({
          type: "header",
          key: `header-${dayKey}`,
          label: date.toLocaleDateString("es-ES", { day: "numeric", month: "long" })
        });
        lastDayKey = dayKey;
      }
      rows.push({ type: "entry", key: `entry-${entry.id}`, entry });
    });
    return rows;
  }, [period, visibleEntries]);
  const stockEditTarget = useMemo(
    () => sortedPantryRows.find((row) => row.item.coffee_id === stockEditCoffeeId) ?? null,
    [sortedPantryRows, stockEditCoffeeId]
  );
  const editEntryTarget = useMemo(
    () => visibleEntries.find((entry) => entry.id === editEntryId) ?? null,
    [editEntryId, visibleEntries]
  );
  const editEntryIsWater = (editEntryTarget?.type || "").toUpperCase() === "WATER";
  const editEntryTime = editEntryTarget
    ? new Date(editEntryTarget.timestamp).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" })
    : "";
  const stripDoseFromPreparation = (value: string) =>
    value.replace(/\s*(?:\(?\d+(?:[.,]\d+)?\s*g\)?)\s*$/i, "").trim();
  const editMethodOptions = [
    { label: "Espresso", drawable: "espresso.png" },
    { label: "Americano", drawable: "americano.png" },
    { label: "Capuchino", drawable: "capuchino.png" },
    { label: "Latte", drawable: "latte.png" },
    { label: "Macchiato", drawable: "macchiato.png" },
    { label: "Moca", drawable: "moca.png" },
    { label: "Vienés", drawable: "vienes.png" },
    { label: "Irlandés", drawable: "irlandes.png" },
    { label: "Frappuccino", drawable: "frappuccino.png" },
    { label: "Caramelo macchiato", drawable: "caramel_macchiato.png" },
    { label: "Corretto", drawable: "corretto.png" },
    { label: "Freddo", drawable: "freddo.png" },
    { label: "Latte macchiato", drawable: "latte_macchiato.png" },
    { label: "Leche con chocolate", drawable: "leche_con_chocolate.png" },
    { label: "Marroquí", drawable: "marroqui.png" },
    { label: "Romano", drawable: "romano.png" },
    { label: "Descafeinado", drawable: "descafeinado.png" }
  ];
  const editSizeOptions = [
    { label: "Espresso", range: "25-30 ml", ml: 30, drawable: "taza_espresso.png" },
    { label: DIARY_STR.PEQUENO, range: "150-200 ml", ml: 175, drawable: "taza_pequeno.png" },
    { label: "Mediano", range: "250-300 ml", ml: 275, drawable: "taza_mediano.png" },
    { label: "Grande", range: "320-400 ml", ml: 360, drawable: "taza_grande.png" },
    { label: "Tazón XL", range: "450-500 ml", ml: 475, drawable: "taza_xl.png" }
  ];
  const parsedStockTotal = Number(stockEditTotal || 0);
  const parsedStockRemaining = Number(stockEditRemaining || 0);
  const canSaveStock =
    Number.isFinite(parsedStockTotal) &&
    Number.isFinite(parsedStockRemaining) &&
    parsedStockTotal >= 1 &&
    parsedStockRemaining >= 0 &&
    parsedStockRemaining <= parsedStockTotal;
  const stockValidationMessage =
    (!Number.isFinite(parsedStockTotal) || !Number.isFinite(parsedStockRemaining))
      ? "Introduce valores v�lidos."
      : parsedStockTotal < 1
      ? "El total debe ser mayor que 0."
      : parsedStockRemaining < 0
        ? "El restante no puede ser negativo."
        : parsedStockRemaining > parsedStockTotal
          ? "El restante no puede superar el total."
          : "";
  const parsedEditAmount = Number(editAmountMl || 0);
  const parsedEditCaffeine = Number(editCaffeineMg || 0);
  const parsedEditDose = Number((editDoseGrams || "0").replace(",", "."));
  const parsedEditTime = (() => {
    if (!editEntryTarget) return null;
    const [hoursRaw, minutesRaw] = (editTimeText || "").split(":");
    const hours = Number(hoursRaw);
    const minutes = Number(minutesRaw);
    if (!Number.isFinite(hours) || !Number.isFinite(minutes)) return null;
    if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) return null;
    const base = new Date(editEntryTarget.timestamp);
    base.setHours(hours, minutes, 0, 0);
    return base.getTime();
  })();
  const canSaveEditEntry =
    Number.isFinite(parsedEditAmount) &&
    parsedEditAmount > 0 &&
    parsedEditTime != null &&
    (editEntryIsWater || (Number.isFinite(parsedEditCaffeine) && parsedEditCaffeine >= 0 && Number.isFinite(parsedEditDose) && parsedEditDose > 0));
  const editValidationMessage =
    !Number.isFinite(parsedEditAmount)
      ? "Introduce una cantidad v�lida."
      : parsedEditAmount <= 0
      ? "La cantidad debe ser mayor que 0 ml."
      : parsedEditTime == null
        ? "Introduce una hora válida (HH:mm)."
      : (!editEntryIsWater && !Number.isFinite(parsedEditCaffeine))
        ? "Introduce una cafe�na v�lida."
      : (!editEntryIsWater && parsedEditCaffeine < 0)
        ? "La cafe�na no puede ser negativa."
        : (!editEntryIsWater && (!Number.isFinite(parsedEditDose) || parsedEditDose <= 0))
          ? "Introduce una dosis válida."
        : "";
  const handleSaveEditEntry = async () => {
    if (!editEntryTarget) return;
    if (savingEditEntry || !canSaveEditEntry) return;
    setSavingEditEntry(true);
    try {
      await onEditEntry(
        editEntryTarget.id,
        parsedEditAmount,
        editEntryIsWater ? 0 : parsedEditCaffeine,
        editEntryIsWater ? "Agua" : editPreparationType.trim(),
        editEntryIsWater ? 0 : Math.max(1, Math.round(parsedEditDose)),
        editEntryIsWater
          ? null
          : (editSizeOptions.find((size) => Math.abs(parsedEditAmount - size.ml) <= 20)?.label ?? null),
        parsedEditTime ?? undefined
      );
      setEditEntryId(null);
    } finally {
      setSavingEditEntry(false);
    }
  };
  const handleEditScrollPointerDown = (event: ReactPointerEvent<HTMLDivElement>, section: "prep" | "size") => {
    const node = section === "prep" ? editPrepScrollRef.current : editSizeScrollRef.current;
    if (!node) return;
    if (node.scrollWidth <= node.clientWidth) return;
    editScrollPointerIdRef.current = event.pointerId;
    editScrollTargetRef.current = node;
    editScrollStartXRef.current = event.clientX;
    editScrollStartLeftRef.current = node.scrollLeft;
    editScrollActiveRef.current = false;
  };
  const handleEditScrollPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const node = editScrollTargetRef.current;
    if (!node) return;
    if (editScrollPointerIdRef.current !== event.pointerId) return;
    const delta = event.clientX - editScrollStartXRef.current;
    if (!editScrollActiveRef.current) {
      if (Math.abs(delta) < 4) return;
      editScrollActiveRef.current = true;
      setEditChipsDragging(true);
      if (!node.hasPointerCapture(event.pointerId)) {
        node.setPointerCapture(event.pointerId);
      }
    }
    event.preventDefault();
    editScrollPendingLeftRef.current = editScrollStartLeftRef.current - delta;
    if (editScrollRafRef.current != null) return;
    editScrollRafRef.current = window.requestAnimationFrame(() => {
      const target = editScrollTargetRef.current;
      if (target) target.scrollLeft = editScrollPendingLeftRef.current;
      editScrollRafRef.current = null;
    });
  };
  const handleEditScrollPointerEnd = (event: ReactPointerEvent<HTMLDivElement>) => {
    const node = editScrollTargetRef.current;
    if (!node) return;
    if (editScrollPointerIdRef.current !== event.pointerId) return;
    editScrollPointerIdRef.current = null;
    editScrollTargetRef.current = null;
    editScrollActiveRef.current = false;
    setEditChipsDragging(false);
    if (editScrollRafRef.current != null) {
      window.cancelAnimationFrame(editScrollRafRef.current);
      editScrollRafRef.current = null;
    }
    if (node.hasPointerCapture(event.pointerId)) {
      node.releasePointerCapture(event.pointerId);
    }
  };
  useEffect(() => {
    return () => {
      if (editScrollRafRef.current != null) {
        window.cancelAnimationFrame(editScrollRafRef.current);
        editScrollRafRef.current = null;
      }
    };
  }, []);
  useEffect(() => {
    const attach = (
      node: HTMLDivElement | null,
      setLeft: (value: boolean) => void,
      setRight: (value: boolean) => void
    ) => {
      if (!node) {
        setLeft(false);
        setRight(false);
        return () => undefined;
      }
      const update = () => {
        const maxScroll = Math.max(0, node.scrollWidth - node.clientWidth);
        setLeft(node.scrollLeft > 2);
        setRight(maxScroll - node.scrollLeft > 2);
      };
      update();
      node.addEventListener("scroll", update, { passive: true });
      const observer = typeof ResizeObserver !== "undefined" ? new ResizeObserver(update) : null;
      observer?.observe(node);
      return () => {
        node.removeEventListener("scroll", update);
        observer?.disconnect();
      };
    };
    const cleanupPrep = attach(editPrepScrollRef.current, setPrepLeftFade, setPrepRightFade);
    const cleanupSize = attach(editSizeScrollRef.current, setSizeLeftFade, setSizeRightFade);
    return () => {
      cleanupPrep();
      cleanupSize();
    };
  }, [editEntryId]);
  const activityList = (
    <ul className="diary-list">
      {diaryRows.length ? diaryRows.map((row) => {
        if (row.type === "header") {
          return <li key={row.key} className="diary-day-header">{row.label}</li>;
        }
        const entry = row.entry;
        return (
          <DiaryActivityRow
            key={row.key}
            entry={entry}
            imageUrl={entry.coffee_id ? entryImageByCoffeeId.get(String(entry.coffee_id)) ?? "" : ""}
            brand={entry.coffee_id ? entryBrandByCoffeeId.get(String(entry.coffee_id)) ?? "" : ""}
            deleting={deletingEntryId === entry.id}
            onOpenEdit={() => {
              const isWater = (entry.type || "").toUpperCase() === "WATER";
              const rawPreparation = (entry.preparation_type || "").trim();
              const doseMatch = rawPreparation.match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1];
              const grams = Math.max(0, Number(entry.coffee_grams || 0));
              setEditEntryId(entry.id);
              setEditAmountMl(String(Math.max(1, entry.amount_ml || 1)));
              setEditCaffeineMg(String(Math.max(0, entry.caffeine_mg || 0)));
              setEditDoseGrams((grams > 0 ? String(grams) : (doseMatch || "15")).replace(".", ","));
              setEditPreparationType(stripDoseFromPreparation(rawPreparation) || (isWater ? "Agua" : DIARY_STR.SIN_METODO));
              setEditTimeText(new Date(entry.timestamp).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" }));
            }}
            onDelete={async () => {
              if (deletingEntryId === entry.id) return;
              setDeletingEntryId(entry.id);
              try {
                await new Promise((resolve) => window.setTimeout(resolve, 170));
                await onDeleteEntry(entry.id);
              } finally {
                setDeletingEntryId(null);
              }
            }}
          />
        );
      }) : <li className="diary-empty-card">{EMPTY.DIARY_NO_ENTRIES}</li>}
    </ul>
  );
  const pantryList = (
    <ul className="diary-pantry-grid">
      {sortedPantryRows.length ? sortedPantryRows.map((row) => (
        <li
          key={`${row.item.user_id}-${row.item.coffee_id}`}
          className="diary-pantry-card"
          onClick={() => {
            if (!row.coffee?.id) return;
            onOpenCoffee(row.coffee.id);
          }}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              if (!row.coffee?.id) return;
              onOpenCoffee(row.coffee.id);
            }
          }}
        >
          <div className="diary-pantry-top">
            {row.coffee?.image_url ? (
              <img src={row.coffee.image_url} alt={row.coffee.nombre} loading="lazy" decoding="async" />
            ) : (
              <span className="diary-pantry-fallback" aria-hidden="true">{(row.coffee?.nombre || "C").slice(0, 1).toUpperCase()}</span>
            )}
            <Button variant="plain"
              type="button"
              className="diary-pantry-options"
              aria-label="Opciones"
              onClick={(event) => {
                event.stopPropagation();
                setPantryOptionsCoffeeId(row.item.coffee_id);
              }}
            >
              <UiIcon name="more" className="ui-icon" />
            </Button>
          </div>
          <div className="brew-pantry-body">
            <strong>{row.coffee?.nombre ?? row.item.coffee_id}</strong>
            <small>{row.item.grams_remaining}/{row.item.total_grams}g</small>
            <div className="brew-pantry-progress" aria-hidden="true">
              <span style={{ width: `${Math.max(0, Math.min(100, row.item.total_grams > 0 ? (row.item.grams_remaining / row.item.total_grams) * 100 : 0))}%` }} />
            </div>
          </div>
        </li>
      )) : <li className="diary-empty-card">{EMPTY.DIARY_NO_PANTRY}</li>}
    </ul>
  );

  return (
    <>
      <article className="diary-analytics-card">
        <div className="diary-analytics-top">
          <div className="diary-analytics-head-block">
            <p className="metric-label diary-analytics-label">
              {DIARY_STR.CAFEINA_ESTIMADA}
              <span className="diary-analytics-info-wrap">
                <Button variant="plain"
                  type="button"
                  className="diary-analytics-info"
                  aria-label={DIARY_STR.ARIA_CAFEINA}
                  aria-expanded={showCaffeineInfo}
                  onClick={() => setShowCaffeineInfo((value) => !value)}
                  onMouseEnter={() => setShowCaffeineInfo(true)}
                  onMouseLeave={() => setShowCaffeineInfo(false)}
                  onBlur={() => setShowCaffeineInfo(false)}
                >
                  i
                </Button>
                {showCaffeineInfo ? (
                  <span className="diary-analytics-tooltip" role="tooltip">
                    {DIARY_STR.TOOLTIP_ESTIMACION}
                  </span>
                ) : null}
              </span>
            </p>
            <p className="analytics-value diary-analytics-main-value">{analytics.caffeine} mg</p>
            <span className={`diary-analytics-trend ${caffeineTrendPct >= 0 ? "is-up" : "is-down"}`.trim()} aria-hidden="true">
              {caffeineTrendPct >= 0 ? "\u2191" : "\u2193"} {Math.abs(caffeineTrendPct)}%
            </span>
          </div>
          <div className="diary-analytics-head-block diary-analytics-hydration">
            <p className="metric-label diary-analytics-label">{DIARY_STR.HIDRATACION}</p>
            <p className="analytics-value diary-analytics-main-value">{analytics.hydrationMl} ml</p>
            <span className={`diary-analytics-trend is-water ${hydrationTrendPct >= 0 ? "is-up" : "is-down"}`.trim()} aria-hidden="true">
              {hydrationTrendPct >= 0 ? "\u2191" : "\u2193"} {Math.abs(hydrationTrendPct)}%
            </span>
          </div>
        </div>
        <div
          ref={chartScrollRef}
          className={`diary-chart-scroll ${chartDragging ? "is-dragging" : ""}`.trim()}
          aria-label={DIARY_STR.GRAFICO_CONSUMO}
          onPointerDown={(event) => {
            const node = chartScrollRef.current;
            if (!node) return;
            if (node.scrollWidth <= node.clientWidth) return;
            event.preventDefault();
            chartPointerIdRef.current = event.pointerId;
            chartDragStartXRef.current = event.clientX;
            chartDragStartScrollRef.current = node.scrollLeft;
            setChartDragging(true);
            node.setPointerCapture(event.pointerId);
          }}
          onPointerMove={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            event.preventDefault();
            const delta = event.clientX - chartDragStartXRef.current;
            const newLeft = chartDragStartScrollRef.current - delta;
            node.scrollLeft = Math.max(0, Math.min(node.scrollWidth - node.clientWidth, newLeft));
          }}
          onPointerUp={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            chartPointerIdRef.current = null;
            setChartDragging(false);
            if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
          }}
          onPointerCancel={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            chartPointerIdRef.current = null;
            setChartDragging(false);
            if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
          }}
          onPointerLeave={() => {
            chartPointerIdRef.current = null;
            setChartDragging(false);
          }}
        >
          <div className={`diary-chart ${period === "7d" ? "is-week" : ""}`.trim()}>
            {chartData.map((item, index) => {
              const caffeineRatio = Math.min(1, item.caffeine / chartMaxCaffeine);
              const waterRatio = Math.min(1, item.water / chartMaxWater);
              const hasCaffeine = item.caffeine > 0;
              const hasWater = item.water > 0;
              const maxBarHeight = 136;
              const minActiveBarHeight = 14;
              const emptyBarHeight = 4;
              const caffeineHeight = hasCaffeine
                ? Math.max(minActiveBarHeight, Math.round(caffeineRatio * maxBarHeight))
                : emptyBarHeight;
              const waterHeight = hasWater
                ? Math.max(minActiveBarHeight, Math.round(waterRatio * maxBarHeight))
                : emptyBarHeight;
              const isCurrent = index === currentSlotIndex;
              return (
                <div
                  key={item.label}
                  className={`diary-chart-col${isCurrent ? " is-current" : ""}`.trim()}
                  title={isCurrent ? (period === "hoy" ? "Hora actual" : "Día actual") : undefined}
                >
                  <div className="diary-chart-bars">
                    <div className="diary-chart-bar-wrap">
                      {hasCaffeine ? <small className="diary-chart-bar-value">{Math.round(item.caffeine)}</small> : null}
                      <span
                        className={`diary-chart-bar caffeine ${hasCaffeine ? "is-active" : ""}`.trim()}
                        style={{ height: `${caffeineHeight}px` }}
                        title={`${DIARY_STR.CAFEINA}: ${Math.round(item.caffeine)} mg`}
                        aria-label={`${DIARY_STR.CAFEINA} ${Math.round(item.caffeine)} miligramos`}
                      />
                    </div>
                    <div className="diary-chart-bar-wrap">
                      {hasWater ? <small className="diary-chart-bar-value">{Math.round(item.water)}</small> : null}
                      <span
                        className={`diary-chart-bar water ${hasWater ? "is-active" : ""}`.trim()}
                        style={{ height: `${waterHeight}px` }}
                        title={`Agua: ${Math.round(item.water)} ml`}
                        aria-label={`Agua ${Math.round(item.water)} mililitros`}
                      />
                    </div>
                  </div>
                  <small>
                    {item.label}
                    {isCurrent ? <span className="diary-chart-col-now" aria-hidden="true"> · Hoy</span> : null}
                  </small>
                </div>
              );
            })}
          </div>
        </div>
        <div className="analytics-grid">
          <div className={`diary-metric-box ${last30Avg.avgCaffeinePerDay >= 100 ? "has-long-value" : ""}`.trim()}>
            <UiIcon name="insights" className="ui-icon" />
            <p className="analytics-value">{last30Avg.avgCaffeinePerDay}<span className="diary-unit-suffix"> mg</span></p>
            <p className="metric-label">MEDIA</p>
          </div>
          <div className="diary-metric-box">
            <UiIcon name="coffee-filled" className="ui-icon" />
            <p className="analytics-value">{analytics.coffeeCups}</p>
            <p className="metric-label">TAZAS</p>
          </div>
          <div className="diary-metric-box">
            <UiIcon name="taste-watery" className="ui-icon" />
            <p className="analytics-value">{hydrationProgressPct}%</p>
            <p className="metric-label">PROGRESO</p>
          </div>
        </div>
      </article>

      {mode !== "desktop" ? (
        <div className="diary-tab-swipe">
          <div
            ref={diaryTabBarRef}
            onTouchStart={onTabPanelsTouchStart}
            onTouchMove={onTabPanelsTouchMove}
            onTouchEnd={onTabPanelsTouchEnd}
            onTouchCancel={onTabPanelsTouchEnd}
          >
          <Tabs className="diary-tabs" aria-label="Tabs diario">
            <span
              className="tab-sliding-indicator"
              style={{
                ["--indicator-pos" as string]:
                  dragOffsetPx !== null && tabDragRefs.current.wrapWidth > 0
                    ? -dragOffsetPx / tabDragRefs.current.wrapWidth
                    : tab === "despensa"
                      ? 1
                      : 0,
                transform: "translateX(calc(var(--indicator-pos, 0) * (100% + 6px)))",
                transition: dragOffsetPx !== null ? "none" : undefined
              }}
              aria-hidden="true"
            />
            <TabButton active={tab === "actividad"} role="tab" aria-selected={tab === "actividad"} onClick={() => setTab("actividad")}>ACTIVIDAD</TabButton>
            <TabButton active={tab === "despensa"} role="tab" aria-selected={tab === "despensa"} onClick={() => setTab("despensa")}>DESPENSA</TabButton>
          </Tabs>
          </div>
          <div ref={panelsWrapRef} className="diary-tab-panels-wrap" aria-hidden="false">
            <div
              className="diary-tab-panels"
              style={{
                transform: dragOffsetPx !== null ? `translateX(${dragOffsetPx}px)` : `translateX(${tab === "actividad" ? 0 : -50}%)`,
                transition: dragOffsetPx !== null ? "none" : undefined
              }}
            >
              <div className="diary-tab-panel">{activityList}</div>
              <div className="diary-tab-panel">{pantryList}</div>
            </div>
          </div>
        </div>
      ) : (
        <section className="diary-desktop-columns" aria-label="Actividad y despensa">
          <div className="diary-section-block">
            <h3 className="diary-section-title">Actividad</h3>
            {activityList}
          </div>
          <div className="diary-section-block">
            <h3 className="diary-section-title">Despensa</h3>
            {pantryList}
          </div>
        </section>
      )}

      {pantryOptionsCoffeeId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Opciones despensa" onDismiss={() => setPantryOptionsCoffeeId(null)} onClick={() => setPantryOptionsCoffeeId(null)}>
          <SheetCard className="diary-sheet diary-sheet-pantry-options" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="diary-sheet-list">
              <Button variant="plain"
                type="button"
                className="diary-sheet-action diary-sheet-action-pantry"
                onClick={() => {
                  const row = sortedPantryRows.find((item) => item.item.coffee_id === pantryOptionsCoffeeId);
                  if (!row) return;
                  setStockEditCoffeeId(row.item.coffee_id);
                  setStockEditTotal(String(Math.max(1, row.item.total_grams)));
                  setStockEditRemaining(String(Math.max(0, row.item.grams_remaining)));
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
              <Button variant="plain"
                type="button"
                className="diary-sheet-action diary-sheet-action-pantry"
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
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {pantryDeleteConfirmCoffeeId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Eliminar de la despensa" onDismiss={() => setPantryDeleteConfirmCoffeeId(null)} onClick={() => setPantryDeleteConfirmCoffeeId(null)}>
          <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="diary-delete-confirm-body">
              <h2 className="diary-delete-confirm-title">Eliminar de la despensa</h2>
              <p className="diary-delete-confirm-text">
                ¿Estás seguro de eliminar este café? Se borrará tu stock actual.
              </p>
              <div className="diary-delete-confirm-actions">
                <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => setPantryDeleteConfirmCoffeeId(null)} disabled={removingStock}>
                  Cancelar
                </Button>
                <Button variant="plain"
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
        </SheetOverlay>
      ) : null}

      {stockEditTarget ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Editar stock" onDismiss={() => setStockEditCoffeeId(null)} onClick={() => setStockEditCoffeeId(null)}>
          <SheetCard className="diary-sheet diary-stock-edit-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header diary-stock-edit-header">
              <strong className="sheet-title">Editar Stock</strong>
            </header>
            <div className="diary-sheet-form diary-stock-edit-form">
              <label className="diary-stock-edit-field">
                <span>Cantidad de café total (g)</span>
                <Input
                  className="diary-stock-edit-value"
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
                  className="diary-stock-edit-slider app-range"
                  type="range"
                  min={0}
                  max={1000}
                  step={1}
                  value={Math.max(0, Math.min(1000, Number(stockEditTotal || 0)))}
                  style={{ "--range-progress": `${Math.max(0, Math.min(100, (Math.max(0, Math.min(1000, Number(stockEditTotal || 0))) / 1000) * 100))}%` } as CSSProperties}
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
                  className="diary-stock-edit-value"
                  type="text"
                  inputMode="numeric"
                  value={String(Math.max(0, Number(stockEditRemaining || 0)))}
                  onChange={(event) => {
                    const digitsOnly = event.target.value.replace(/[^0-9]/g, "");
                    setStockEditRemaining(String(Number(digitsOnly || 0)));
                  }}
                />
                <Input
                  className="diary-stock-edit-slider app-range"
                  type="range"
                  min={0}
                  max={Math.max(1, Number(stockEditTotal || 0))}
                  step={1}
                  value={Math.max(0, Math.min(Number(stockEditRemaining || 0), Math.max(1, Number(stockEditTotal || 0))))}
                  style={{
                    "--range-progress": `${Math.max(
                      0,
                      Math.min(
                        100,
                        (Math.max(0, Math.min(Number(stockEditRemaining || 0), Math.max(1, Number(stockEditTotal || 0)))) / Math.max(1, Number(stockEditTotal || 0))) * 100
                      )
                    )}%`
                  } as CSSProperties}
                  onChange={(event) => setStockEditRemaining(event.target.value)}
                />
              </label>
              {!canSaveStock && stockValidationMessage ? <p className="diary-inline-error">{stockValidationMessage}</p> : null}
              <div className="diary-sheet-form-actions diary-stock-edit-actions">
                <Button variant="plain" type="button" className="action-button diary-stock-edit-cancel" onClick={() => setStockEditCoffeeId(null)} disabled={savingStock}>
                  CANCELAR
                </Button>
                <Button variant="plain"
                  type="button"
                  className="action-button diary-stock-edit-save"
                  disabled={savingStock || !canSaveStock}
                  onClick={async () => {
                    if (savingStock || !canSaveStock) return;
                    setSavingStock(true);
                    try {
                      await onUpdatePantryStock(
                        stockEditTarget.item.coffee_id,
                        parsedStockTotal,
                        parsedStockRemaining
                      );
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
        </SheetOverlay>
      ) : null}

      {editEntryTarget ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Editar entrada" onDismiss={() => setEditEntryId(null)} onClick={() => setEditEntryId(null)}>
          <SheetCard className="diary-sheet diary-edit-entry-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header diary-edit-entry-header">
              <Button
                variant="plain"
                type="button"
                className="diary-edit-entry-header-action diary-edit-entry-close"
                onClick={() => setEditEntryId(null)}
                disabled={savingEditEntry}
                aria-label="Cerrar"
              >
                <UiIcon name="close" className="ui-icon" />
              </Button>
              <strong className="sheet-title">Editar</strong>
              <Button
                variant="plain"
                type="button"
                className="diary-edit-entry-header-action is-save"
                onClick={() => void handleSaveEditEntry()}
                disabled={savingEditEntry || !canSaveEditEntry}
              >
                {savingEditEntry ? "Guardando..." : "Guardar"}
              </Button>
            </header>
            <div className="diary-sheet-form">
              {editEntryIsWater ? (
                <div className="diary-edit-entry-metrics-grid diary-edit-water-grid">
                  <label className="diary-edit-entry-metric-field is-caffeine">
                    <span>Cantidad (ml)</span>
                    <div className="diary-edit-entry-metric-value">
                      <UiIcon name="water" className="ui-icon diary-water-drop-icon" />
                      <Input
                        className="diary-edit-entry-metric-input"
                        type="number"
                        inputMode="numeric"
                        min={1}
                        step={1}
                        value={editAmountMl}
                        onChange={(event) => setEditAmountMl(event.target.value)}
                      />
                    </div>
                  </label>
                  <label className="diary-edit-entry-metric-field is-time">
                    <span>Tiempo (hh:mm)</span>
                    <div className="diary-edit-entry-metric-value is-time-input">
                      <UiIcon name="clock" className="ui-icon" />
                      <Input
                        className="diary-edit-entry-metric-input"
                        type="text"
                        inputMode="numeric"
                        placeholder="HH:mm"
                        value={editTimeText}
                        onChange={(event) => setEditTimeText(formatTimeInputToHhMm(event.target.value))}
                      />
                    </div>
                  </label>
                </div>
              ) : (
                <div className="diary-edit-entry-coffee-layout">
                  <section className="diary-edit-entry-block">
                    <h4 className="diary-edit-entry-block-title">{DIARY_STR.PREPARACION}</h4>
                    <div
                      ref={editPrepScrollRef}
                      className={`diary-edit-entry-presets is-coffee ${editChipsDragging ? "is-dragging" : ""} ${prepLeftFade ? "has-left-fade" : ""} ${prepRightFade ? "has-right-fade" : ""}`.trim()}
                      onPointerDown={(event) => handleEditScrollPointerDown(event, "prep")}
                      onPointerMove={handleEditScrollPointerMove}
                      onPointerUp={handleEditScrollPointerEnd}
                      onPointerCancel={handleEditScrollPointerEnd}
                    >
                      {editMethodOptions.map((method) => (
                        <Button variant="chip"
                          key={method.label}
                          className={`chip-button period-chip diary-edit-entry-method-chip ${normalizeLookupText(editPreparationType) === normalizeLookupText(method.label) ? "is-active" : ""}`.trim()}
                          onClick={() => setEditPreparationType(method.label)}
                        >
                          <img src={`/android-drawable/${method.drawable}`} alt="" aria-hidden="true" />
                          <span>{method.label}</span>
                        </Button>
                      ))}
                    </div>
                  </section>

                  <section className="diary-edit-entry-metrics-grid">
                    <label className="diary-edit-entry-metric-field is-caffeine">
                      <span>{DIARY_STR.CAFEINA_MG}</span>
                      <div className="diary-edit-entry-metric-value">
                        <UiIcon name="caffeine" className="ui-icon" />
                        <Input
                          className="diary-edit-entry-metric-input"
                          type="number"
                          inputMode="numeric"
                          min={0}
                          step={1}
                          value={editCaffeineMg}
                          placeholder="0"
                          onChange={(event) => setEditCaffeineMg(event.target.value)}
                        />
                      </div>
                    </label>
                    <div className="diary-edit-entry-metric-field is-readonly is-dose">
                      <span>Dosis (g)</span>
                      <div className="diary-edit-entry-metric-value">
                        <UiIcon name="dose" className="ui-icon" />
                        <Input
                          className="diary-edit-entry-metric-input"
                          type="text"
                          inputMode="decimal"
                          value={editDoseGrams}
                          onChange={(event) => setEditDoseGrams(event.target.value)}
                        />
                      </div>
                    </div>
                  </section>

                  <section className="diary-edit-entry-block">
                    <h4 className="diary-edit-entry-block-title">{DIARY_STR.TAMANO}</h4>
                    <div
                      ref={editSizeScrollRef}
                      className={`diary-coffee-size-presets diary-edit-entry-size-presets ${editChipsDragging ? "is-dragging" : ""} ${sizeLeftFade ? "has-left-fade" : ""} ${sizeRightFade ? "has-right-fade" : ""}`.trim()}
                      onPointerDown={(event) => handleEditScrollPointerDown(event, "size")}
                      onPointerMove={handleEditScrollPointerMove}
                      onPointerUp={handleEditScrollPointerEnd}
                      onPointerCancel={handleEditScrollPointerEnd}
                    >
                      {editSizeOptions.map((size) => (
                        <Button variant="chip"
                          key={size.label}
                          className={`chip-button period-chip diary-coffee-size-chip ${Math.abs(Number(editAmountMl || 0) - size.ml) <= 20 ? "is-active" : ""}`.trim()}
                          onClick={() => setEditAmountMl(String(size.ml))}
                        >
                          <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" />
                          <span>{size.label}</span>
                          <small>{size.range}</small>
                        </Button>
                      ))}
                    </div>
                  </section>

                  <label className="diary-edit-entry-metric-field is-readonly is-time">
                    <span>Tiempo (hh:mm)</span>
                    <div className="diary-edit-entry-metric-value is-time-input">
                      <UiIcon name="clock" className="ui-icon" />
                      <Input
                        className="diary-edit-entry-metric-input"
                        type="text"
                        inputMode="numeric"
                        placeholder="HH:mm"
                        value={editTimeText}
                        onChange={(event) => setEditTimeText(formatTimeInputToHhMm(event.target.value))}
                      />
                    </div>
                  </label>
                </div>
              )}
              {!canSaveEditEntry && editValidationMessage ? <p className="diary-inline-error">{editValidationMessage}</p> : null}
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </>
  );
}

function DiaryActivityRow({
  entry,
  imageUrl,
  brand,
  deleting,
  onOpenEdit,
  onDelete
}: {
  entry: DiaryEntryRow;
  imageUrl: string;
  brand?: string;
  deleting: boolean;
  onOpenEdit: () => void;
  onDelete: () => Promise<void>;
}) {
  const isWaterEntry = (entry.type || "").toUpperCase() === "WATER";
  const coffeeNameNorm = (entry.coffee_name || "").trim().toLowerCase();
  const isRegistroRapido = !isWaterEntry && (
    (entry.coffee_id == null || entry.coffee_id === "") ||
    coffeeNameNorm === DIARY_STR.REGISTRO_RAPIDO.toLowerCase() ||
    /registro\s*rapido/i.test(coffeeNameNorm)
  );
  const entryTitle = isRegistroRapido
    ? (entry.preparation_type || "").trim() || DIARY_STR.REGISTRO_RAPIDO
    : entry.coffee_name || (isWaterEntry ? "Agua" : "Entrada");
  const entrySubtitle = isWaterEntry
    ? `${Math.max(0, entry.amount_ml || 0)} ml`
    : isRegistroRapido
      ? DIARY_STR.CAFE_BRAND
      : (brand || DIARY_STR.CAFE_LABEL).toUpperCase();
  const rawPreparationValue = (entry.preparation_type || "").trim();
  const elaborationMethod = (() => {
    const match = rawPreparationValue.match(/(?:^lab:\s*|^elaboracion:\s*)([^()]+?)(?:\s*\(|$)/i);
    return match?.[1]?.trim() ?? "";
  })();
  const prepValue = rawPreparationValue || (isWaterEntry ? "Agua" : "-");
  const prepDisplayValue = elaborationMethod ? "BrewLab" : prepValue;
  const doseFromPrep = ((entry.preparation_type || "").match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1] || "").replace(",", ".");
  const gramsFromField = Math.max(0, Number(entry.coffee_grams || 0));
  const doseValue = gramsFromField > 0 ? `${Math.round(gramsFromField)} g` : doseFromPrep ? `${Math.round(Number(doseFromPrep))} g` : (isWaterEntry ? "-" : "15 g");
  const sizeValue = isWaterEntry
    ? `${Math.max(0, entry.amount_ml || 0)} ml`
    : (entry.size_label || "").trim()
      ? String(entry.size_label).trim()
    : (() => {
      const ml = Math.max(0, entry.amount_ml || 0);
      if (ml <= 45) return "Espresso";
      if (ml <= 120) return "Corto";
      if (ml <= 220) return "Mediano";
      return "Largo";
    })();
  const timeValue = new Date(entry.timestamp).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" });
  const prepDrawable = (() => {
    const normalized = normalizeLookupText(prepValue);
    if (normalized.includes("americano")) return "americano.png";
    if (normalized.includes("capuch")) return "capuchino.png";
    if (normalized.includes("caramelo") && normalized.includes("macchi")) return "caramel_macchiato.png";
    if (normalized.includes("corretto")) return "corretto.png";
    if (normalized.includes("descafe")) return "descafeinado.png";
    if (normalized.includes("espresso")) return "espresso.png";
    if (normalized.includes("frapp")) return "frappuccino.png";
    if (normalized.includes("freddo")) return "freddo.png";
    if (normalized.includes("irland")) return "irlandes.png";
    if (normalized.includes("latte macchi")) return "latte_macchiato.png";
    if (normalized.includes("latte")) return "latte.png";
    if (normalized.includes("chocolate")) return "leche_con_chocolate.png";
    if (normalized.includes("macchiato")) return "macchiato.png";
    if (normalized.includes("marro")) return "marroqui.png";
    if (normalized.includes("moca")) return "moca.png";
    if (normalized.includes("romano")) return "romano.png";
    if (normalized.includes("vien")) return "vienes.png";
    return "maq_manual.png";
  })();
  const elaborationDrawable = (() => {
    if (!elaborationMethod) return null;
    const normalized = normalizeLookupText(elaborationMethod);
    if (normalized.includes("espresso")) return "maq_espresso.png";
    if (normalized.includes("v60") || normalized.includes("hario")) return "maq_hario_v60.png";
    if (normalized.includes("aero")) return "maq_aeropress.png";
    if (normalized.includes("moka") || normalized.includes("italiana")) return "maq_italiana.png";
    if (normalized.includes("chemex")) return "maq_chemex.png";
    if (normalized.includes("prensa")) return "maq_prensa_francesa.png";
    if (normalized.includes("goteo")) return "maq_goteo.png";
    if (normalized.includes("sifon")) return "maq_sifon.png";
    if (normalized.includes("turco")) return "maq_turco.png";
    return "maq_manual.png";
  })();
  const sizeDrawable = (() => {
    if (isWaterEntry) return null;
    if (sizeValue === "Espresso") return "taza_espresso.png";
    if (sizeValue === "Corto") return "taza_pequeno.png";
    if (sizeValue === "Mediano") return "taza_mediano.png";
    return "taza_grande.png";
  })();
  const metaItemsBase: Array<{ key: string; icon?: IconName; drawable?: string; label: string; value: string }> = [
    { key: "caffeine", icon: "caffeine", label: "CAFE\u00CDNA", value: `${Math.max(0, entry.caffeine_mg || 0)} mg` },
    { key: "dose", icon: "dose", label: "DOSIS", value: doseValue },
    { key: "size", drawable: sizeDrawable ?? undefined, icon: isWaterEntry ? "bottle" : (sizeDrawable ? undefined : "stock"), label: "TAMA\u00D1O", value: sizeValue },
    { key: "prep", drawable: prepDrawable, label: "PREPARACI\u00D3N", value: prepDisplayValue },
    ...(elaborationMethod
      ? [{ key: "brew-method", drawable: elaborationDrawable ?? undefined, label: "M\u00C9TODO", value: elaborationMethod }]
      : [])
  ];
  const metaItems = isWaterEntry ? [] : metaItemsBase;
  const metaScrollRef = useRef<HTMLDivElement | null>(null);
  const metaPointerIdRef = useRef<number | null>(null);
  const metaDragStartXRef = useRef(0);
  const metaDragStartScrollRef = useRef(0);
  const metaRafRef = useRef<number | null>(null);
  const metaPendingScrollRef = useRef(0);
  const metaInteractingRef = useRef(false);
  const [metaDragging, setMetaDragging] = useState(false);
  const [metaHasOverflow, setMetaHasOverflow] = useState(false);
  const [metaScrollLeft, setMetaScrollLeft] = useState(false);
  const [metaScrollRight, setMetaScrollRight] = useState(false);
  const [offsetX, setOffsetX] = useState(0);
  const [swipeActive, setSwipeActive] = useState(false);
  const pointerIdRef = useRef<number | null>(null);
  const startXRef = useRef(0);
  const startYRef = useRef(0);
  const swipeActiveRef = useRef(false);
  const movedRef = useRef(false);
  const swipeContentRef = useRef<HTMLDivElement | null>(null);
  const offsetRef = useRef(0);
  const rafIdRef = useRef<number | null>(null);
  const threshold = -76;
  const maxDrag = -124;
  const SWIPE_THRESHOLD_PX = 3;
  const VERTICAL_LOCK_PX = 2;

  const handlePointerDown = (event: ReactPointerEvent<HTMLLIElement>) => {
    const target = event.target as HTMLElement | null;
    if (metaInteractingRef.current || (metaHasOverflow && target?.closest(".diary-entry-meta-scroll"))) return;
    if (deleting) return;
    if (pointerIdRef.current != null) return;
    pointerIdRef.current = event.pointerId;
    startXRef.current = event.clientX;
    startYRef.current = event.clientY;
    swipeActiveRef.current = false;
    movedRef.current = false;
    offsetRef.current = 0;
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (metaInteractingRef.current) return;
    if (pointerIdRef.current !== event.pointerId) return;
    const dx = event.clientX - startXRef.current;
    const dy = event.clientY - startYRef.current;
    const absDx = Math.abs(dx);
    const absDy = Math.abs(dy);
    if (!swipeActiveRef.current) {
      if (absDx < SWIPE_THRESHOLD_PX && absDy < SWIPE_THRESHOLD_PX) return;
      if (absDy > absDx + VERTICAL_LOCK_PX) {
        pointerIdRef.current = null;
        setOffsetX(0);
        return;
      }
      if (dx < 0 && absDx > absDy) {
        swipeActiveRef.current = true;
        movedRef.current = true;
        setSwipeActive(true);
        event.currentTarget.setPointerCapture(event.pointerId);
      } else {
        pointerIdRef.current = null;
        setOffsetX(0);
        return;
      }
    }
    if (dx >= 0) {
      offsetRef.current = 0;
      setOffsetX(0);
      const el = swipeContentRef.current;
      if (el) el.style.transform = "translateX(0px)";
      return;
    }
    const raw = dx;
    const withResistance = raw <= maxDrag ? maxDrag + (raw - maxDrag) * 0.25 : raw;
    const offset = Math.max(maxDrag, withResistance);
    offsetRef.current = offset;
    const el = swipeContentRef.current;
    if (el) el.style.transform = `translateX(${offset}px)`;
    if (rafIdRef.current == null) {
      rafIdRef.current = requestAnimationFrame(() => {
        setOffsetX(offsetRef.current);
        rafIdRef.current = null;
      });
    }
  };

  const handlePointerEnd = async (event: ReactPointerEvent<HTMLLIElement>) => {
    if (metaInteractingRef.current) return;
    if (pointerIdRef.current !== event.pointerId) return;
    if (rafIdRef.current != null) {
      cancelAnimationFrame(rafIdRef.current);
      rafIdRef.current = null;
    }
    pointerIdRef.current = null;
    swipeActiveRef.current = false;
    setSwipeActive(false);
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    const finalOffset = offsetRef.current;
    const el = swipeContentRef.current;
    if (finalOffset <= threshold) {
      if (el) el.style.transform = "";
      setOffsetX(0);
      await onDelete();
      return;
    }
    setOffsetX(finalOffset);
    requestAnimationFrame(() => {
      if (el) el.style.transform = "";
      setOffsetX(0);
    });
    window.setTimeout(() => {
      movedRef.current = false;
    }, 120);
  };

  const updateMetaScrollEdges = useCallback(() => {
    const node = metaScrollRef.current;
    if (!node || !metaItems.length) return;
    setMetaHasOverflow(node.scrollWidth - node.clientWidth > 1);
    setMetaScrollLeft(node.scrollLeft > 1);
    setMetaScrollRight(node.scrollLeft + node.clientWidth < node.scrollWidth - 1);
  }, [metaItems.length]);

  useEffect(() => {
    const node = metaScrollRef.current;
    if (!node || !metaItems.length) {
      setMetaHasOverflow(false);
      setMetaScrollLeft(false);
      setMetaScrollRight(false);
      return;
    }
    updateMetaScrollEdges();
    node.addEventListener("scroll", updateMetaScrollEdges);
    if (typeof ResizeObserver === "undefined") {
      return () => node.removeEventListener("scroll", updateMetaScrollEdges);
    }
    const observer = new ResizeObserver(updateMetaScrollEdges);
    observer.observe(node);
    return () => {
      node.removeEventListener("scroll", updateMetaScrollEdges);
      observer.disconnect();
    };
  }, [metaItems.length, updateMetaScrollEdges]);

  return (
    <li
      className={`diary-card-swipe-wrap ${swipeActive || offsetX < -1 ? "is-swiping" : ""} ${deleting ? "is-dismissing" : ""}`.trim()}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={(event) => {
        void handlePointerEnd(event);
      }}
      onPointerCancel={(event) => {
        void handlePointerEnd(event);
      }}
      onClick={(event) => {
        const target = event.target as HTMLElement | null;
        if (metaInteractingRef.current || (metaHasOverflow && target?.closest(".diary-entry-meta-scroll"))) return;
        if (movedRef.current || deleting) return;
        onOpenEdit();
      }}
    >
      <div className="diary-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div ref={swipeContentRef} className="diary-swipe-content" style={{ transform: `translateX(${offsetX}px)` }}>
        <div className="diary-card">
          <div className="diary-entry-head">
            <div className={`diary-entry-media ${isWaterEntry ? "is-water" : ""}`.trim()}>
              {imageUrl && !isRegistroRapido ? (
                <img src={imageUrl} alt={entryTitle} loading="lazy" decoding="async" />
              ) : (
                <span className="diary-entry-fallback" aria-hidden="true">
                  <UiIcon
                    name={isWaterEntry ? "water" : isRegistroRapido ? "coffee-filled" : "coffee"}
                    className={`ui-icon ${isWaterEntry ? "diary-water-icon-centered" : ""}`.trim()}
                  />
                </span>
              )}
            </div>
            <div className="diary-entry-copy">
              <p className="feed-user">{entryTitle}</p>
              <p className="feed-meta diary-entry-brand">{entrySubtitle.toUpperCase()}</p>
            </div>
            <div className="diary-entry-time-pill">
              <UiIcon name="clock" className="ui-icon" />
              <span>{timeValue}</span>
            </div>
          </div>
          {metaItems.length ? (
            <div
              ref={metaScrollRef}
              className={`diary-entry-meta-scroll ${metaDragging ? "is-dragging" : ""} ${metaHasOverflow ? "" : "is-static"} ${metaScrollLeft ? "has-scroll-left" : ""} ${metaScrollRight ? "has-scroll-right" : ""}`.trim()}
              onPointerDown={(event) => {
              if (!metaHasOverflow) return;
              event.stopPropagation();
              metaInteractingRef.current = true;
              const node = metaScrollRef.current;
              if (!node) return;
              if (node.scrollWidth <= node.clientWidth) return;
              event.preventDefault();
              metaPointerIdRef.current = event.pointerId;
              metaDragStartXRef.current = event.clientX;
              metaDragStartScrollRef.current = node.scrollLeft;
              setMetaDragging(true);
              node.setPointerCapture(event.pointerId);
            }}
              onPointerMove={(event) => {
              if (!metaHasOverflow) return;
              event.stopPropagation();
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              event.preventDefault();
              const delta = event.clientX - metaDragStartXRef.current;
              metaPendingScrollRef.current = metaDragStartScrollRef.current - delta;
              if (metaRafRef.current != null) return;
              metaRafRef.current = window.requestAnimationFrame(() => {
                const target = metaScrollRef.current;
                if (target) target.scrollLeft = metaPendingScrollRef.current;
                metaRafRef.current = null;
              });
            }}
              onPointerUp={(event) => {
              if (!metaHasOverflow) return;
              event.stopPropagation();
              metaInteractingRef.current = false;
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
              if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
            }}
              onPointerCancel={(event) => {
              if (!metaHasOverflow) return;
              event.stopPropagation();
              metaInteractingRef.current = false;
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
              if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
            }}
              onPointerLeave={() => {
              if (!metaHasOverflow) return;
              metaInteractingRef.current = false;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
              }}
            >
              <div className="diary-entry-meta-grid">
                {metaItems.map((item) => (
                  <div key={item.key} className="diary-entry-meta-item">
                    {item.drawable ? (
                      <img className="diary-entry-meta-drawable" src={`/android-drawable/${item.drawable}`} alt="" aria-hidden="true" />
                    ) : item.icon ? (
                      <UiIcon name={item.icon} className="ui-icon" />
                    ) : null}
                    <span className="diary-entry-meta-label">{item.label}</span>
                    <strong className="diary-entry-meta-value">{item.value}</strong>
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </li>
  );
}
