import { type CSSProperties, type PointerEvent as ReactPointerEvent, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { MentionText } from "../../ui/MentionText";
import {
  caffeineTargetMg,
  getMondayOfWeek,
  getWeekStartEndMs,
  hydrationProgressPercent,
  hydrationTargetMl,
  last30DaysDailyAverages,
  trendPercent,
  type DiaryPeriod
} from "../../core/diaryAnalytics";
import { EMPTY } from "../../core/emptyErrorStrings";
import { sendEvent } from "../../core/ga4";
import { normalizeLookupText } from "../../core/text";
import { UiIcon, type IconName } from "../../ui/iconography";
import { Button, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
import type { CoffeeRow, DiaryEntryRow, PantryItemRow } from "../../types";
import { DiaryLineChart } from "./DiaryLineChart";
import { useI18n } from "../../i18n";

const CHART_COL_WIDTH = 42;

/** Mapa nombre de país (normalizado) a código ISO 3166-1 alpha-2 para bandera emoji. */
const COUNTRY_NAME_TO_ISO: Record<string, string> = {
  colombia: "CO", brasil: "BR", brazil: "BR", etiopía: "ET", ethiopia: "ET",
  guatemala: "GT", honduras: "HN", "costa rica": "CR", perú: "PE", peru: "PE",
  kenia: "KE", kenya: "KE", indonesia: "ID", méxico: "MX", mexico: "MX",
  nicaragua: "NI", "el salvador": "SV", india: "IN", vietnam: "VN",
  "papúa nueva guinea": "PG", "papua nueva guinea": "PG", uganda: "UG",
  tanzania: "TZ", ruanda: "RW", rwanda: "RW", ecuador: "EC",
  bolivia: "BO", venezuela: "VE", jamaica: "JM", "república dominicana": "DO",
  "republica dominicana": "DO", haití: "HT", haiti: "HT", yemen: "YE",
  china: "CN", panamá: "PA", panama: "PA", cuba: "CU", filipinas: "PH",
  tailandia: "TH", "timor oriental": "TL", laos: "LA", myanmar: "MM",
  burundi: "BI", camerún: "CM", camerun: "CM", madagascar: "MG",
  españa: "ES", spain: "ES", italia: "IT", italy: "IT", francia: "FR",
  alemania: "DE", germany: "DE", "estados unidos": "US", usa: "US",
};

/** Devuelve código ISO del país para mostrar bandera como imagen, o null si no hay mapa. */
function getCountryIso(countryName: string): string | null {
  if (countryName === "—" || !countryName.trim()) return null;
  return COUNTRY_NAME_TO_ISO[countryName.trim().toLowerCase()] ?? null;
}

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
  period,
  selectedDiaryDate,
  selectedDiaryMonth,
  entries,
  coffeeCatalog,
  pantryRows,
  onDeleteEntry,
  onEditEntry,
  onUpdatePantryStock,
  onRemovePantryItem,
  onMarkPantryCoffeeFinished,
  onOpenCoffee,
  onOpenCafesProbados,
  orderedBrewMethods = []
}: {
  mode: "mobile" | "desktop";
  period: DiaryPeriod;
  /** Cuando period === "hoy", fecha del día. Cuando period === "week", lunes de la semana (YYYY-MM-DD). */
  selectedDiaryDate?: string;
  /** Cuando period === "30d", mes seleccionado (YYYY-MM). */
  selectedDiaryMonth?: string;
  entries: DiaryEntryRow[];
  coffeeCatalog: CoffeeRow[];
  pantryRows: Array<{ item: PantryItemRow; coffee?: CoffeeRow }>;
  /** Métodos de elaboración ordenados (para selector de método al editar) */
  orderedBrewMethods?: Array<{ name: string; icon: string }>;
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
  onUpdatePantryStock: (pantryItemId: string, totalGrams: number, gramsRemaining: number) => Promise<void>;
  onRemovePantryItem: (pantryItemId: string) => Promise<void>;
  onMarkPantryCoffeeFinished?: (pantryItemId: string) => Promise<void>;
  onOpenCoffee: (coffeeId: string) => void;
  /** Al pulsar "Cafés probados": abrir página completa (mapa + listado). Si no se pasa, se abre el sheet actual. */
  onOpenCafesProbados?: () => void;
}) {
  const { t, locale } = useI18n();
  const localeTag = locale === "en" ? "en-US" : locale === "fr" ? "fr-FR" : locale === "pt" ? "pt-PT" : locale === "de" ? "de-DE" : "es-ES";
  const [deletingEntryId, setDeletingEntryId] = useState<number | null>(null);
  const [editEntryId, setEditEntryId] = useState<number | null>(null);
  const [editAmountMl, setEditAmountMl] = useState("");
  const [editCaffeineMg, setEditCaffeineMg] = useState("");
  const [editPreparationType, setEditPreparationType] = useState("");
  const [editBrewMethod, setEditBrewMethod] = useState("");
  const [editBrewTaste, setEditBrewTaste] = useState("");
  const [editDoseGrams, setEditDoseGrams] = useState("");
  const [editTimeText, setEditTimeText] = useState("");
  const [savingEditEntry, setSavingEditEntry] = useState(false);
  const [pantryOptionsPantryItemId, setPantryOptionsPantryItemId] = useState<string | null>(null);
  const [pantryDeleteConfirmPantryId, setPantryDeleteConfirmPantryId] = useState<string | null>(null);
  const [pantryFinishedConfirmPantryId, setPantryFinishedConfirmPantryId] = useState<string | null>(null);
  const [markingFinished, setMarkingFinished] = useState(false);
  const [stockEditPantryItemId, setStockEditPantryItemId] = useState<string | null>(null);
  const [stockEditTotal, setStockEditTotal] = useState("");
  const [stockEditRemaining, setStockEditRemaining] = useState("");
  const [savingStock, setSavingStock] = useState(false);
  const [removingStock, setRemovingStock] = useState(false);
  const [showCaffeineInfo, setShowCaffeineInfo] = useState(false);
  const [showBaristaCoffeeList, setShowBaristaCoffeeList] = useState(false);
  const editPrepScrollRef = useRef<HTMLDivElement | null>(null);
  const editMethodScrollRef = useRef<HTMLDivElement | null>(null);
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
  const [methodLeftFade, setMethodLeftFade] = useState(false);
  const [methodRightFade, setMethodRightFade] = useState(false);
  const [sizeLeftFade, setSizeLeftFade] = useState(false);
  const [sizeRightFade, setSizeRightFade] = useState(false);
  const visibleEntries = useMemo(() => {
    const now = new Date();
    const start = new Date(now);
    let startMs: number;
    let endMs: number | null = null;
    if (period === "week" && selectedDiaryDate) {
      const { startMs: s, endMs: e } = getWeekStartEndMs(selectedDiaryDate);
      startMs = s;
      const now = Date.now();
      if (e > now) {
        const endOfToday = new Date();
        endOfToday.setHours(23, 59, 59, 999);
        endMs = endOfToday.getTime() + 1;
      } else {
        endMs = e;
      }
    } else if (period === "hoy") {
      if (selectedDiaryDate) {
        const [y, m, d] = selectedDiaryDate.split("-").map(Number);
        start.setFullYear(y, (m ?? 1) - 1, d ?? 1);
      }
      start.setHours(0, 0, 0, 0);
      startMs = start.getTime();
      if (selectedDiaryDate) {
        const [y, m, d] = selectedDiaryDate.split("-").map(Number);
        const dayEnd = new Date(y, (m ?? 1) - 1, (d ?? 1) + 1);
        dayEnd.setHours(0, 0, 0, 0);
        endMs = dayEnd.getTime();
      }
    } else if (period === "7d") {
      const day = start.getDay();
      const diffToMonday = day === 0 ? 6 : day - 1;
      start.setDate(start.getDate() - diffToMonday);
      start.setHours(0, 0, 0, 0);
      startMs = start.getTime();
      endMs = startMs + 7 * 86400000;
    } else if (period === "30d") {
      if (selectedDiaryMonth) {
        const [y, m] = selectedDiaryMonth.split("-").map(Number);
        start.setFullYear(y, (m ?? 1) - 1, 1);
      }
      start.setHours(0, 0, 0, 0);
      startMs = start.getTime();
      const nextMonth = new Date(start);
      nextMonth.setMonth(nextMonth.getMonth() + 1);
      endMs = nextMonth.getTime();
    } else {
      start.setDate(1);
      start.setHours(0, 0, 0, 0);
      startMs = start.getTime();
    }
    let filtered = entries.filter(
      (entry) => Number(entry.timestamp) >= startMs && (entry.type || "").toUpperCase() !== "NOTE"
    );
    if (endMs != null) {
      filtered = filtered.filter((entry) => Number(entry.timestamp) < endMs!);
    }
    return filtered.sort((a, b) => Number(b.timestamp) - Number(a.timestamp));
  }, [entries, period, selectedDiaryDate, selectedDiaryMonth]);

  const analytics = useMemo(() => {
    const caffeine = visibleEntries.reduce((acc, entry) => acc + Math.max(0, entry.caffeine_mg || 0), 0);
    const hydrationMl = visibleEntries
      .filter((entry) => (entry.type || "").toUpperCase() === "WATER")
      .reduce((acc, entry) => acc + Math.max(0, entry.amount_ml || 0), 0);
    const coffeeCups = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() !== "WATER").length;
    const waterEntries = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() === "WATER").length;
    return { caffeine, hydrationMl, coffeeCups, waterEntries };
  }, [visibleEntries]);

  const periodDays = period === "hoy" ? 1 : period === "week" || period === "7d" ? 7 : 30;
  /** Días efectivos del periodo para hábitos (tazas): si es el periodo actual, no contar días futuros. */
  const effectivePeriodDays = useMemo(() => {
    if (period === "hoy") return 1;
    const now = new Date();
    const todayMs = Date.UTC(now.getFullYear(), now.getMonth(), now.getDate());
    if (period === "7d" || period === "week") {
      const weekStart = selectedDiaryDate
        ? (() => {
            const [y, m, d] = selectedDiaryDate.split("-").map(Number);
            return Date.UTC(y, (m ?? 1) - 1, d ?? 1);
          })()
        : (() => {
            const d = new Date();
            const day = d.getDay();
            const diff = day === 0 ? -6 : 1 - day;
            d.setDate(d.getDate() + diff);
            return Date.UTC(d.getFullYear(), d.getMonth(), d.getDate());
          })();
      const weekEnd = weekStart + 7 * 86400000;
      if (todayMs >= weekEnd) return 7;
      if (todayMs < weekStart) return 1;
      return Math.ceil((todayMs - weekStart) / 86400000) + 1;
    }
    if (period === "30d") {
      const [selY, selM] = (selectedDiaryMonth || "").split("-").map(Number);
      const isCurrentMonth =
        selY === now.getFullYear() && (selM ?? 0) === now.getMonth() + 1;
      if (!isCurrentMonth) return 30;
      return now.getDate();
    }
    return periodDays;
  }, [period, periodDays, selectedDiaryDate, selectedDiaryMonth]);

  const coffeeEntries = useMemo(
    () => visibleEntries.filter((e) => (e.type || "").toUpperCase() !== "WATER"),
    [visibleEntries]
  );
  /** Cafés de toda la cuenta (para barista: cafés probados, tostadores, origen favorito). */
  const allCoffeeEntries = useMemo(
    () => entries.filter((e) => (e.type || "").toUpperCase() !== "WATER"),
    [entries]
  );
  const coffeeById = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    coffeeCatalog.forEach((c) => map.set(String(c.id), c));
    return map;
  }, [coffeeCatalog]);

  const habitStats = useMemo(() => {
    const cups = coffeeEntries.length;
    const avgCups = effectivePeriodDays > 0 ? Math.round((cups / effectivePeriodDays) * 10) / 10 : 0;
    const sizeCount = new Map<string, number>();
    const methodCount = new Map<string, number>();
    const dayCount = new Map<number, number>();
    const stripLabPrefix = (s: string) => s.replace(/^(?:lab:\s*|elaboracion:\s*)/i, "").trim();
    coffeeEntries.forEach((entry) => {
      const size = (entry.size_label || "—").trim() || "—";
      sizeCount.set(size, (sizeCount.get(size) ?? 0) + 1);
      const prep = (entry.preparation_type || "").trim();
      const rawMethod = prep.includes("|") ? prep.split("|")[0].trim() : prep;
      const methodKey = stripLabPrefix(rawMethod) || t("diary.noMethod");
      methodCount.set(methodKey, (methodCount.get(methodKey) ?? 0) + 1);
      const d = new Date(entry.timestamp).getDay();
      dayCount.set(d, (dayCount.get(d) ?? 0) + 1);
    });
    const DAY_NAMES = Array.from({ length: 7 }).map((_, day) => {
      const d = new Date(Date.UTC(2026, 0, 4 + day)); // Sunday-based reference week
      return d.toLocaleDateString(localeTag, { weekday: "long" });
    });
    let mostSize = "—";
    let maxSize = 0;
    sizeCount.forEach((count, size) => {
      if (count > maxSize) {
        maxSize = count;
        mostSize = size;
      }
    });
    let mostMethod = t("diary.noMethod");
    let maxMethod = 0;
    methodCount.forEach((count, method) => {
      if (count > maxMethod) {
        maxMethod = count;
        mostMethod = method;
      }
    });
    let busiestDay = "—";
    let maxDay = 0;
    dayCount.forEach((count, day) => {
      if (count > maxDay) {
        maxDay = count;
        busiestDay = DAY_NAMES[day] ?? "—";
      }
    });
    return { avgCups, mostSize, mostMethod, busiestDay };
  }, [coffeeEntries, effectivePeriodDays]);

  /** Café consumido en los últimos 30 días (para previsión despensa: no depende de la semana seleccionada). */
  const coffeeEntriesLast30 = useMemo(() => {
    const now = Date.now();
    const thirtyDaysMs = 30 * 86400000;
    const from = now - thirtyDaysMs;
    return entries.filter(
      (e) => (e.type || "").toUpperCase() !== "WATER" && Number(e.timestamp) >= from
    );
  }, [entries]);

  const consumptionStats = useMemo(() => {
    const morning = [6, 12];
    const afternoon = [12, 20];
    const evening = [20, 24];
    let morn = 0;
    let after = 0;
    let even = 0;
    coffeeEntries.forEach((entry) => {
      const h = new Date(entry.timestamp).getHours();
      if (h >= morning[0] && h < morning[1]) morn++;
      else if (h >= afternoon[0] && h < afternoon[1]) after++;
      else even++;
    });
    const total = morn + after + even;
    const pct = (n: number) => (total > 0 ? Math.round((n / total) * 100) : 0);
    const avgCaffeine = coffeeEntries.length > 0
      ? Math.round(coffeeEntries.reduce((a, e) => a + (e.caffeine_mg || 0), 0) / coffeeEntries.length)
      : 0;
    const withDose = coffeeEntries.filter((e) => Number(e.coffee_grams) > 0);
    const avgDose = withDose.length > 0
      ? Math.round(withDose.reduce((a, e) => a + (e.coffee_grams || 0), 0) / withDose.length)
      : 0;
    const formatCount = new Map<string, number>();
    coffeeEntries.forEach((entry) => {
      if (!entry.coffee_id) return;
      const coffee = coffeeById.get(entry.coffee_id);
      const fmt = (coffee?.formato || "—").trim() || "—";
      formatCount.set(fmt, (formatCount.get(fmt) ?? 0) + 1);
    });
    let mostFormat = "—";
    let maxFmt = 0;
    formatCount.forEach((count, fmt) => {
      if (count > maxFmt) {
        maxFmt = count;
        mostFormat = fmt;
      }
    });
    const PANTRY_DAYS_BASE = 30;
    const totalGramsLast30 = coffeeEntriesLast30.reduce((a, e) => a + (e.coffee_grams || 0), 0);
    const avgGramsPerDayPantry = PANTRY_DAYS_BASE > 0 && totalGramsLast30 > 0
      ? totalGramsLast30 / PANTRY_DAYS_BASE
      : 0;
    const totalPantryGrams = pantryRows.reduce((a, r) => a + (r.item.grams_remaining || 0), 0);
    const pantryDaysLeft = avgGramsPerDayPantry > 0 && totalPantryGrams > 0
      ? Math.round(totalPantryGrams / avgGramsPerDayPantry)
      : null;
    return {
      momentPct: { morning: pct(morn), afternoon: pct(after), evening: pct(even) },
      avgCaffeine,
      avgDose,
      mostFormat,
      pantryDaysLeft
    };
  }, [coffeeEntries, periodDays, coffeeById, pantryRows, coffeeEntriesLast30]);

  const baristaStats = useMemo(() => {
    const byCoffeeId = new Map<string, number>();
    const roasterSet = new Set<string>();
    const originCount = new Map<string, number>();
    allCoffeeEntries.forEach((entry) => {
      if (entry.coffee_id) {
        const ts = Number(entry.timestamp);
        const prev = byCoffeeId.get(entry.coffee_id);
        if (prev == null || ts < prev) byCoffeeId.set(entry.coffee_id, ts);
        const coffee = coffeeById.get(entry.coffee_id);
        if (coffee?.marca) roasterSet.add(coffee.marca.trim());
        const origin = (coffee?.pais_origen || "").trim() || "—";
        if (origin !== "—") originCount.set(origin, (originCount.get(origin) ?? 0) + 1);
      }
    });
    let favoriteOrigin = "—";
    let maxOrig = 0;
    originCount.forEach((count, orig) => {
      if (count > maxOrig) {
        maxOrig = count;
        favoriteOrigin = orig;
      }
    });
    const coffeesWithFirstTried: Array<{ coffee: CoffeeRow; firstTriedTs: number }> = [];
    byCoffeeId.forEach((firstTriedTs, coffeeId) => {
      const coffee = coffeeById.get(coffeeId);
      if (coffee) coffeesWithFirstTried.push({ coffee, firstTriedTs });
    });
    coffeesWithFirstTried.sort((a, b) => a.firstTriedTs - b.firstTriedTs);
    return {
      distinctCoffees: coffeesWithFirstTried.length,
      coffeesWithFirstTried,
      distinctRoasters: roasterSet.size,
      favoriteOrigin
    };
  }, [allCoffeeEntries, coffeeById]);

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

    if (period === "7d" || period === "week") {
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
  const chartScrollRef = useRef<HTMLDivElement | null>(null);
  const chartPointerIdRef = useRef<number | null>(null);
  const chartDragStartXRef = useRef(0);
  const chartDragStartScrollRef = useRef(0);
  const [chartDragging, setChartDragging] = useState(false);

  /** Índice del slot actual (hora, día de la semana o día del mes) para centrar el gráfico. */
  const currentSlotIndex = useMemo(() => {
    const now = new Date();
    if (period === "hoy") return now.getHours();
    if (period === "7d" || period === "week") {
      const d = now.getDay();
      return d === 0 ? 6 : d - 1;
    }
    return now.getDate() - 1;
  }, [period]);

  useEffect(() => {
    const node = chartScrollRef.current;
    if (!node || chartData.length === 0) return;
    if (period === "7d" || period === "week") {
      node.scrollLeft = 0;
      return;
    }
    const colWidth = CHART_COL_WIDTH;
    const viewportWidth = node.clientWidth;
    const maxScroll = Math.max(0, node.scrollWidth - viewportWidth);
    const targetScroll = Math.max(0, Math.min(maxScroll, currentSlotIndex * colWidth - viewportWidth / 2 + colWidth / 2));
    node.scrollLeft = targetScroll;
  }, [period, chartData.length, currentSlotIndex]);
  /** Ordenar despensa por último uso (actividad del usuario): el café más recientemente usado en el diario a la izquierda. */
  const sortedPantryRows = useMemo(() => {
    const lastUsed = new Map<string, number>();
    entries.forEach((entry) => {
      const ts = Number(entry.timestamp ?? 0);
      if (entry.pantry_item_id) {
        const cur = lastUsed.get(entry.pantry_item_id) ?? 0;
        if (ts > cur) lastUsed.set(entry.pantry_item_id, ts);
      }
      if (entry.coffee_id) {
        const key = `coffee:${entry.coffee_id}`;
        const cur = lastUsed.get(key) ?? 0;
        if (ts > cur) lastUsed.set(key, ts);
      }
    });
    const lastUsedFor = (item: PantryItemRow) =>
      Math.max(
        lastUsed.get(item.id) ?? 0,
        lastUsed.get(`coffee:${item.coffee_id}`) ?? 0,
        Number(item.last_updated ?? 0)
      );
    return [...pantryRows].sort((a, b) => lastUsedFor(b.item) - lastUsedFor(a.item));
  }, [pantryRows, entries]);
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
  const todayKey = useMemo(() => {
    const now = new Date();
    return `${now.getFullYear()}-${now.getMonth()}-${now.getDate()}`;
  }, []);
  const diaryRows = useMemo(() => {
    const rows: Array<{ type: "header"; key: string; label: string; isToday?: boolean } | { type: "entry"; key: string; entry: DiaryEntryRow }> = [];
    let lastDayKey = "";
    visibleEntries.forEach((entry) => {
      const date = new Date(entry.timestamp);
      const dayKey = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
      if (period !== "hoy" && dayKey !== lastDayKey) {
        rows.push({
          type: "header",
          key: `header-${dayKey}`,
          label: date.toLocaleDateString(localeTag, { day: "numeric", month: "long" }),
          isToday: dayKey === todayKey
        });
        lastDayKey = dayKey;
      }
      rows.push({ type: "entry", key: `entry-${entry.id}`, entry });
    });
    return rows;
  }, [period, visibleEntries, todayKey]);
  const stockEditTarget = useMemo(
    () => sortedPantryRows.find((row) => row.item.id === stockEditPantryItemId) ?? null,
    [sortedPantryRows, stockEditPantryItemId]
  );
  const editEntryTarget = useMemo(
    () => visibleEntries.find((entry) => entry.id === editEntryId) ?? null,
    [editEntryId, visibleEntries]
  );
  const editEntryIsWater = (editEntryTarget?.type || "").toUpperCase() === "WATER";
  const editEntryTime = editEntryTarget
    ? new Date(editEntryTarget.timestamp).toLocaleTimeString(localeTag, { hour: "2-digit", minute: "2-digit" })
    : "";
  const stripDoseFromPreparation = (value: string) =>
    value.replace(/\s*(?:\(?\d+(?:[.,]\d+)?\s*g\)?)\s*$/i, "").trim();
  const localizedDiaryMethodChip = useCallback((name: string) => {
    if (locale === "es") return name;
    const key = normalizeLookupText(name);
    const en: Record<string, string> = {
      espresso: "Espresso",
      agua: "Water",
      italiana: "Moka",
      aeropress: "Aeropress",
      chemex: "Chemex",
      goteo: "Drip",
      "hario v60": "Hario V60"
    };
    const fr: Record<string, string> = {
      espresso: "Espresso",
      agua: "Eau",
      italiana: "Moka",
      aeropress: "Aeropress",
      chemex: "Chemex",
      goteo: "Filtre",
      "hario v60": "Hario V60"
    };
    const pt: Record<string, string> = {
      espresso: "Espresso",
      agua: "Agua",
      italiana: "Moka",
      aeropress: "Aeropress",
      chemex: "Chemex",
      goteo: "Coado",
      "hario v60": "Hario V60"
    };
    const de: Record<string, string> = {
      espresso: "Espresso",
      agua: "Wasser",
      italiana: "Moka",
      aeropress: "Aeropress",
      chemex: "Chemex",
      goteo: "Filter",
      "hario v60": "Hario V60"
    };
    const dictionary = locale === "fr" ? fr : locale === "pt" ? pt : locale === "de" ? de : en;
    return dictionary[key] ?? name;
  }, [locale]);
  const localizedDiaryPreparationChip = useCallback((name: string) => {
    if (locale === "es") return name;
    const key = normalizeLookupText(name);
    const en: Record<string, string> = {
      espresso: "Espresso",
      americano: "Americano",
      capuchino: "Cappuccino",
      latte: "Latte",
      macchiato: "Macchiato",
      moca: "Mocha",
      vienes: "Viennese",
      irlandes: "Irish",
      frappuccino: "Frappuccino",
      "caramelo macchiato": "Caramel macchiato",
      corretto: "Corretto",
      freddo: "Freddo",
      "latte macchiato": "Latte macchiato",
      "leche con chocolate": "Hot chocolate milk",
      marroqui: "Moroccan",
      romano: "Romano",
      descafeinado: "Decaf"
    };
    const fr: Record<string, string> = {
      espresso: "Espresso",
      americano: "Americano",
      capuchino: "Cappuccino",
      latte: "Latte",
      macchiato: "Macchiato",
      moca: "Moka",
      vienes: "Viennois",
      irlandes: "Irlandais",
      frappuccino: "Frappuccino",
      "caramelo macchiato": "Macchiato caramel",
      corretto: "Corretto",
      freddo: "Freddo",
      "latte macchiato": "Latte macchiato",
      "leche con chocolate": "Lait au chocolat",
      marroqui: "Marocain",
      romano: "Romano",
      descafeinado: "Decafeine"
    };
    const pt: Record<string, string> = {
      espresso: "Espresso",
      americano: "Americano",
      capuchino: "Cappuccino",
      latte: "Latte",
      macchiato: "Macchiato",
      moca: "Moca",
      vienes: "Vienense",
      irlandes: "Irlandes",
      frappuccino: "Frappuccino",
      "caramelo macchiato": "Macchiato de caramelo",
      corretto: "Corretto",
      freddo: "Freddo",
      "latte macchiato": "Latte macchiato",
      "leche con chocolate": "Leite com chocolate",
      marroqui: "Marroquino",
      romano: "Romano",
      descafeinado: "Descafeinado"
    };
    const de: Record<string, string> = {
      espresso: "Espresso",
      americano: "Americano",
      capuchino: "Cappuccino",
      latte: "Latte",
      macchiato: "Macchiato",
      moca: "Mokka",
      vienes: "Wiener",
      irlandes: "Irisch",
      frappuccino: "Frappuccino",
      "caramelo macchiato": "Karamell-Macchiato",
      corretto: "Corretto",
      freddo: "Freddo",
      "latte macchiato": "Latte macchiato",
      "leche con chocolate": "Milch mit Schokolade",
      marroqui: "Marokkanisch",
      romano: "Romano",
      descafeinado: "Entkoffeiniert"
    };
    const dictionary = locale === "fr" ? fr : locale === "pt" ? pt : locale === "de" ? de : en;
    return dictionary[key] ?? name;
  }, [locale]);
  const localizedDiarySizeChip = useCallback((name: string) => {
    if (locale === "es") return name;
    const key = normalizeLookupText(name);
    const en: Record<string, string> = { espresso: "Espresso", pequeno: "Small", mediano: "Medium", grande: "Large", "tazon xl": "XL mug" };
    const fr: Record<string, string> = { espresso: "Espresso", pequeno: "Petit", mediano: "Moyen", grande: "Grand", "tazon xl": "Tasse XL" };
    const pt: Record<string, string> = { espresso: "Espresso", pequeno: "Pequeno", mediano: "Medio", grande: "Grande", "tazon xl": "Caneca XL" };
    const de: Record<string, string> = { espresso: "Espresso", pequeno: "Klein", mediano: "Mittel", grande: "Gross", "tazon xl": "XL-Tasse" };
    const dictionary = locale === "fr" ? fr : locale === "pt" ? pt : locale === "de" ? de : en;
    return dictionary[key] ?? name;
  }, [locale]);
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
    { label: "Pequeño", range: "150-200 ml", ml: 175, drawable: "taza_pequeno.png" },
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
      ? t("diary.enterValidValues")
      : parsedStockTotal < 1
      ? t("diary.totalMustBeGreaterThanZero")
      : parsedStockRemaining < 0
        ? t("diary.remainingNotNegative")
        : parsedStockRemaining > parsedStockTotal
          ? t("diary.remainingNotExceedTotal")
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
      ? t("diary.enterValidAmount")
      : parsedEditAmount <= 0
      ? t("diary.amountMustBeGreaterThanZero")
      : parsedEditTime == null
        ? "Introduce una hora válida (HH:mm)."
      : (!editEntryIsWater && !Number.isFinite(parsedEditCaffeine))
        ? t("diary.enterValidCaffeine")
      : (!editEntryIsWater && parsedEditCaffeine < 0)
        ? t("diary.caffeineCannotBeNegative")
        : (!editEntryIsWater && (!Number.isFinite(parsedEditDose) || parsedEditDose <= 0))
          ? t("diary.enterValidDose")
        : "";
  const handleSaveEditEntry = async () => {
    if (!editEntryTarget) return;
    if (savingEditEntry || !canSaveEditEntry) return;
    setSavingEditEntry(true);
    try {
      const preparationTypeToSave = editEntryIsWater
        ? t("diary.water")
        : (() => {
            if (editBrewMethod.trim()) {
              const methodPart = editBrewTaste.trim()
                ? `Lab: ${editBrewMethod.trim()} (${editBrewTaste.trim()})`
                : editBrewMethod.trim();
              return editPreparationType.trim() ? `${methodPart}|${editPreparationType.trim()}` : methodPart;
            }
            return editPreparationType.trim();
          })();
      await onEditEntry(
        editEntryTarget.id,
        parsedEditAmount,
        editEntryIsWater ? 0 : parsedEditCaffeine,
        preparationTypeToSave,
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
  const handleEditScrollPointerDown = (event: ReactPointerEvent<HTMLDivElement>, section: "prep" | "method" | "size") => {
    const node = section === "prep" ? editPrepScrollRef.current : section === "method" ? editMethodScrollRef.current : editSizeScrollRef.current;
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
    const cleanupMethod = attach(editMethodScrollRef.current, setMethodLeftFade, setMethodRightFade);
    const cleanupSize = attach(editSizeScrollRef.current, setSizeLeftFade, setSizeRightFade);
    return () => {
      cleanupPrep();
      cleanupMethod();
      cleanupSize();
    };
  }, [editEntryId]);
  const activityList = (
    <ul className="diary-list">
      {diaryRows.length ? diaryRows.map((row) => {
        if (row.type === "header") {
          return (
            <li
              key={row.key}
              className={`diary-day-header${row.isToday ? " is-today" : ""}`.trim()}
            >
              {row.label}
            </li>
          );
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
              const pipeIdx = rawPreparation.indexOf("|");
              const methodPart = pipeIdx >= 0 ? rawPreparation.slice(0, pipeIdx).trim() : rawPreparation;
              const tipoPart = pipeIdx >= 0 ? rawPreparation.slice(pipeIdx + 1).trim() : "";
              const doseMatch = rawPreparation.match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1];
              const grams = Math.max(0, Number(entry.coffee_grams || 0));
              setEditEntryId(entry.id);
              setEditAmountMl(String(Math.max(1, entry.amount_ml || 1)));
              setEditCaffeineMg(String(Math.max(0, entry.caffeine_mg || 0)));
              setEditDoseGrams((grams > 0 ? String(grams) : (doseMatch || "15")).replace(".", ","));
              setEditTimeText(new Date(entry.timestamp).toLocaleTimeString(localeTag, { hour: "2-digit", minute: "2-digit" }));
              const labMatch = methodPart.match(/^Lab:\s*([^(]+?)\s*\(([^)]*)\)\s*$/);
              if (labMatch) {
                setEditBrewMethod(labMatch[1].trim());
                setEditBrewTaste(labMatch[2].trim());
                setEditPreparationType(tipoPart || (isWater ? t("diary.water") : ""));
              } else {
                const methodName = orderedBrewMethods.find((m) => normalizeLookupText(m.name) === normalizeLookupText(methodPart))?.name ?? "";
                setEditBrewMethod(methodName);
                setEditBrewTaste("");
                setEditPreparationType(pipeIdx >= 0 ? tipoPart : (stripDoseFromPreparation(rawPreparation) || (isWater ? t("diary.water") : t("diary.noMethod"))));
              }
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
      }) : <li className="card diary-empty-card">{EMPTY.DIARY_NO_ENTRIES}</li>}
    </ul>
  );

  return (
    <>
      <article className="card diary-analytics-card">
        <div className="diary-analytics-top">
          <div className="diary-analytics-head-block">
            <p className="metric-label diary-analytics-label">
                {t("diary.estimatedCaffeineUpper")}
              <span className="diary-analytics-info-wrap">
                <Button variant="plain"
                  type="button"
                  className="diary-analytics-info"
                  aria-label={t("diary.estimatedCaffeineAria")}
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
                    {t("diary.estimatedCaffeineTooltip")}
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
            <p className="metric-label diary-analytics-label">{t("diary.hydrationUpper")}</p>
            <p className="analytics-value diary-analytics-main-value">{analytics.hydrationMl} ml</p>
            <span className={`diary-analytics-trend is-water ${hydrationTrendPct >= 0 ? "is-up" : "is-down"}`.trim()} aria-hidden="true">
              {hydrationTrendPct >= 0 ? "\u2191" : "\u2193"} {Math.abs(hydrationTrendPct)}%
            </span>
          </div>
        </div>
        <div
          ref={chartScrollRef}
          className={`diary-chart-scroll ${chartDragging ? "is-dragging" : ""}`.trim()}
          aria-label={t("diary.consumptionChartAria")}
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
          <div
            className={`diary-chart diary-chart-lines diary-chart-chartjs ${period === "7d" || period === "week" ? "is-week" : ""}`.trim()}
            style={{ width: chartData.length * CHART_COL_WIDTH }}
          >
            <DiaryLineChart
              chartData={chartData}
              period={period}
              currentSlotIndex={currentSlotIndex}
              isCurrentWeek={
                (period === "7d" || period === "week") &&
                !!selectedDiaryDate &&
                selectedDiaryDate === getMondayOfWeek(new Date().toISOString().slice(0, 10))
              }
            />
          </div>
        </div>
      </article>

      <section className="diary-stats-section" aria-label={t("diary.habit")}>
        <h3 className="diary-section-title">{t("diary.habit")}</h3>
        <article className="card diary-stats-card diary-habit-card">
        <ul className="diary-stats-card-list">
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.cups")}</span>
            <span className="diary-stats-card-value">{habitStats.avgCups}</span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.cupSize")}</span>
            <span className="diary-stats-card-value">{habitStats.mostSize}</span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.method")}</span>
            <span className="diary-stats-card-value">{habitStats.mostMethod}</span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.coffeeDay")}</span>
            <span className="diary-stats-card-value">{habitStats.busiestDay}</span>
          </li>
        </ul>
      </article>
      </section>

      <section className="diary-stats-section" aria-label={t("diary.consumption")}>
        <h3 className="diary-section-title">{t("diary.consumption")}</h3>
        <article className="card diary-stats-card diary-consumption-card">
        <ul className="diary-stats-card-list">
          <li className="diary-stats-card-row diary-stats-card-row-momento">
            <span className="diary-stats-card-label">{t("diary.moment")}</span>
            <span className="diary-stats-card-value diary-stats-card-value-momento">
              {t("diary.morning")} {consumptionStats.momentPct.morning}% · {t("diary.afternoon")} {consumptionStats.momentPct.afternoon}% · {t("diary.night")} {consumptionStats.momentPct.evening}%
            </span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.caffeine")}</span>
            <span className="diary-stats-card-value">{consumptionStats.avgCaffeine} mg</span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.dosePerCoffee")}</span>
            <span className="diary-stats-card-value">{consumptionStats.avgDose} g</span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.format")}</span>
            <span className="diary-stats-card-value">{consumptionStats.mostFormat}</span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.pantryForecast")}</span>
            <span className="diary-stats-card-value">
              {consumptionStats.pantryDaysLeft != null ? t("diary.daysApprox", { days: consumptionStats.pantryDaysLeft }) : "—"}
            </span>
          </li>
        </ul>
      </article>
      </section>

      <section className="diary-stats-section" aria-label={t("diary.barista")}>
        <h3 className="diary-section-title">{t("diary.barista")}</h3>
        <article className="card diary-stats-card diary-barista-card">
        <ul className="diary-stats-card-list">
          <li className="diary-stats-card-row diary-stats-card-row-clickable">
            <span className="diary-stats-card-label">{t("diary.coffeesTried")}</span>
            <span className="diary-stats-card-value">
              {baristaStats.distinctCoffees}
              <UiIcon name="chevron-right" className="ui-icon diary-stats-card-arrow" aria-hidden="true" />
            </span>
            <button
              type="button"
              className="diary-stats-card-row-tap"
              aria-label={t("diary.viewCoffeesTried")}
              onClick={() => { if (onOpenCafesProbados) onOpenCafesProbados(); else { sendEvent("modal_open", { modal_id: "diary_barista_list" }); setShowBaristaCoffeeList(true); } }}
            />
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.roastersTried")}</span>
            <span className="diary-stats-card-value">{baristaStats.distinctRoasters}</span>
          </li>
          <li className="diary-stats-card-row">
            <span className="diary-stats-card-label">{t("diary.favoriteOrigin")}</span>
            <span className="diary-stats-card-value diary-stats-card-value-origin">
              {(() => {
                const iso = getCountryIso(baristaStats.favoriteOrigin);
                if (iso) {
                  return (
                    <>
                      <img
                        src={`https://flagcdn.com/w40/${iso.toLowerCase()}.png`}
                        alt=""
                        className="diary-origin-flag-img"
                        width={24}
                        height={18}
                        loading="lazy"
                      />
                      <span>{baristaStats.favoriteOrigin}</span>
                    </>
                  );
                }
                return baristaStats.favoriteOrigin;
              })()}
            </span>
          </li>
        </ul>
      </article>
      </section>

      {showBaristaCoffeeList ? (
        <SheetOverlay
          role="dialog"
          aria-modal="true"
          aria-label={t("diary.coffeesTried")}
          onDismiss={() => { sendEvent("modal_close", { modal_id: "diary_barista_list" }); setShowBaristaCoffeeList(false); }}
          onClick={() => { sendEvent("modal_close", { modal_id: "diary_barista_list" }); setShowBaristaCoffeeList(false); }}
        >
          <SheetCard className="diary-sheet diary-sheet-barista-list" onClick={(e) => e.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <span className="sheet-header-spacer" aria-hidden="true" />
              <h2 className="sheet-title">{t("diary.coffeesTried")}</h2>
              <Button variant="plain" type="button" className="sheet-header-close" onClick={() => { sendEvent("modal_close", { modal_id: "diary_barista_list" }); setShowBaristaCoffeeList(false); }} aria-label={t("diary.close")}>
                <UiIcon name="close" className="ui-icon" />
              </Button>
            </header>
            <ul className="diary-barista-coffee-list">
              {baristaStats.coffeesWithFirstTried.map(({ coffee, firstTriedTs }) => (
                <li key={coffee.id}>
                  <Button
                    variant="plain"
                    className="diary-barista-coffee-item"
                    onClick={() => {
                      sendEvent("modal_close", { modal_id: "diary_barista_list" });
                      setShowBaristaCoffeeList(false);
                      onOpenCoffee(coffee.id);
                    }}
                  >
                    {coffee.image_url ? (
                      <img src={coffee.image_url} alt="" className="diary-barista-coffee-img" loading="lazy" />
                    ) : (
                      <span className="diary-barista-coffee-img diary-barista-coffee-placeholder">{(coffee.nombre || "C").slice(0, 1)}</span>
                    )}
                    <div className="diary-barista-coffee-copy">
                      <span className="diary-barista-coffee-name">{coffee.nombre}</span>
                      <span className="diary-barista-coffee-meta">
                        {t("diary.firstTime", { date: new Date(firstTriedTs).toLocaleDateString(localeTag, { day: "numeric", month: "short", year: "numeric" }) })}
                      </span>
                    </div>
                    <UiIcon name="chevron-right" className="ui-icon" aria-hidden="true" />
                  </Button>
                </li>
              ))}
            </ul>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      <section className="diary-activity-section" aria-label={t("diary.activity")}>
        <h3 className="diary-section-title">{t("diary.activity")}</h3>
        {activityList}
      </section>

      {pantryOptionsPantryItemId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label={t("diary.pantryOptions")} onDismiss={() => { sendEvent("modal_close", { modal_id: "diary_pantry_options" }); setPantryOptionsPantryItemId(null); }} onClick={() => { sendEvent("modal_close", { modal_id: "diary_pantry_options" }); setPantryOptionsPantryItemId(null); }}>
          <SheetCard className="diary-sheet diary-sheet-pantry-options list-options-general-wrap" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="diary-sheet-list list-options-general-wrap">
              <div className="list-options-page-section">
                <h3 className="create-list-privacy-subtitle">{t("diary.organize")}</h3>
                <div className="list-options-general-card">
                  <Button variant="plain"
                    type="button"
                    className="list-options-page-action diary-sheet-action-pantry"
                    onClick={() => {
                      const row = sortedPantryRows.find((item) => item.item.id === pantryOptionsPantryItemId);
                      if (!row) return;
                      sendEvent("modal_close", { modal_id: "diary_pantry_options" });
                      sendEvent("modal_open", { modal_id: "diary_stock_edit" });
                      setStockEditPantryItemId(row.item.id);
                      setStockEditTotal(String(Math.max(1, row.item.total_grams)));
                      setStockEditRemaining(String(Math.max(0, row.item.grams_remaining)));
                      setPantryOptionsPantryItemId(null);
                    }}
                  >
                    <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">edit</span>
                    <span>{t("diary.editStock")}</span>
                    <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">chevron_right</span>
                  </Button>
                  {onMarkPantryCoffeeFinished ? (
                    <Button variant="plain"
                      type="button"
                      className="list-options-page-action diary-sheet-action-pantry"
                      onClick={() => {
                        sendEvent("modal_close", { modal_id: "diary_pantry_options" });
                        sendEvent("modal_open", { modal_id: "diary_finished_confirm" });
                        setPantryFinishedConfirmPantryId(pantryOptionsPantryItemId);
                        setPantryOptionsPantryItemId(null);
                      }}
                    >
                      <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">check_circle</span>
                      <span>{t("diary.coffeeFinished")}</span>
                      <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">chevron_right</span>
                    </Button>
                  ) : null}
                </div>
              </div>
              <div className="list-options-page-section list-options-section-spaced">
                <h3 className="create-list-privacy-subtitle">{t("diary.general")}</h3>
                <div className="list-options-general-card">
                  <Button variant="plain"
                    type="button"
                    className="list-options-page-action diary-sheet-action-pantry"
                    disabled={removingStock}
                    onClick={() => {
                      sendEvent("modal_close", { modal_id: "diary_pantry_options" });
                      sendEvent("modal_open", { modal_id: "diary_delete_confirm_pantry" });
                      setPantryDeleteConfirmPantryId(pantryOptionsPantryItemId);
                      setPantryOptionsPantryItemId(null);
                    }}
                  >
                    <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">delete</span>
                    <span>{t("diary.removeFromPantry")}</span>
                    <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">chevron_right</span>
                  </Button>
                </div>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {pantryFinishedConfirmPantryId && onMarkPantryCoffeeFinished ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label={t("diary.coffeeFinished")} onDismiss={() => { sendEvent("modal_close", { modal_id: "diary_finished_confirm" }); setPantryFinishedConfirmPantryId(null); }} onClick={() => { sendEvent("modal_close", { modal_id: "diary_finished_confirm" }); setPantryFinishedConfirmPantryId(null); }}>
          <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="diary-delete-confirm-body">
              <h2 className="diary-delete-confirm-title">Café terminado</h2>
              <p className="diary-delete-confirm-text">
                {t("diary.finishedCoffeeConfirmText")}
              </p>
              <div className="diary-delete-confirm-actions">
                <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => { sendEvent("modal_close", { modal_id: "diary_finished_confirm" }); setPantryFinishedConfirmPantryId(null); }} disabled={markingFinished}>
                  {t("diary.cancel")}
                </Button>
                <Button variant="plain"
                  type="button"
                  className="diary-delete-confirm-submit"
                  disabled={markingFinished}
                  onClick={async () => {
                    if (markingFinished) return;
                    setMarkingFinished(true);
                    try {
                      await onMarkPantryCoffeeFinished(pantryFinishedConfirmPantryId);
                      sendEvent("modal_close", { modal_id: "diary_finished_confirm" });
                      setPantryFinishedConfirmPantryId(null);
                    } finally {
                      setMarkingFinished(false);
                    }
                  }}
                >
                  {markingFinished ? t("profile.saving") : t("diary.confirm")}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {pantryDeleteConfirmPantryId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label={t("diary.removeFromPantry")} onDismiss={() => { sendEvent("modal_close", { modal_id: "diary_delete_confirm_pantry" }); setPantryDeleteConfirmPantryId(null); }} onClick={() => { sendEvent("modal_close", { modal_id: "diary_delete_confirm_pantry" }); setPantryDeleteConfirmPantryId(null); }}>
          <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="diary-delete-confirm-body">
              <h2 className="diary-delete-confirm-title">Eliminar de la despensa</h2>
              <p className="diary-delete-confirm-text">
                {t("diary.removePantryConfirmText")}
              </p>
              <div className="diary-delete-confirm-actions">
                <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => { sendEvent("modal_close", { modal_id: "diary_delete_confirm_pantry" }); setPantryDeleteConfirmPantryId(null); }} disabled={removingStock}>
                  {t("diary.cancel")}
                </Button>
                <Button variant="plain"
                  type="button"
                  className="diary-delete-confirm-submit"
                  disabled={removingStock}
                  onClick={async () => {
                    if (removingStock) return;
                    setRemovingStock(true);
                    try {
                      await onRemovePantryItem(pantryDeleteConfirmPantryId);
                      sendEvent("modal_close", { modal_id: "diary_delete_confirm_pantry" });
                      setPantryDeleteConfirmPantryId(null);
                    } finally {
                      setRemovingStock(false);
                    }
                  }}
                >
                  {removingStock ? t("diary.removing") : t("diary.delete")}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {stockEditTarget ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label={t("diary.editStock")} onDismiss={() => { sendEvent("modal_close", { modal_id: "diary_stock_edit" }); setStockEditPantryItemId(null); }} onClick={() => { sendEvent("modal_close", { modal_id: "diary_stock_edit" }); setStockEditPantryItemId(null); }}>
          <SheetCard className="diary-sheet diary-stock-edit-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header diary-stock-edit-header">
              <strong className="sheet-title">{t("diary.editStock")}</strong>
            </header>
            <div className="diary-sheet-form diary-stock-edit-form">
              <label className="diary-stock-edit-field">
                <span>{t("diary.totalCoffeeAmount")}</span>
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
                <span>{t("diary.remainingCoffeeAmount")}</span>
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
                <Button variant="plain" type="button" className="action-button diary-stock-edit-cancel" onClick={() => { sendEvent("modal_close", { modal_id: "diary_stock_edit" }); setStockEditPantryItemId(null); }} disabled={savingStock}>
                  {t("diary.cancel").toUpperCase()}
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
                        stockEditTarget.item.id,
                        parsedStockTotal,
                        parsedStockRemaining
                      );
                      sendEvent("modal_close", { modal_id: "diary_stock_edit" });
                      setStockEditPantryItemId(null);
                    } finally {
                      setSavingStock(false);
                    }
                  }}
                >
                  {savingStock ? t("profile.saving").toUpperCase() : t("profile.save").toUpperCase()}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {editEntryTarget ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label={t("diary.editEntry")} onDismiss={() => setEditEntryId(null)} onClick={() => setEditEntryId(null)}>
          <SheetCard className="diary-sheet diary-edit-entry-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header diary-edit-entry-header">
              <span className="diary-edit-entry-header-spacer" aria-hidden="true" />
              <strong className="sheet-title">{t("diary.edit")}</strong>
              <Button
                variant="plain"
                type="button"
                className="diary-edit-entry-header-action is-save"
                onClick={() => void handleSaveEditEntry()}
                disabled={savingEditEntry || !canSaveEditEntry}
              >
                {savingEditEntry ? t("profile.saving") : t("profile.save")}
              </Button>
            </header>
            <div className="diary-sheet-form">
              {editEntryIsWater ? (
                <div className="diary-edit-entry-metrics-grid diary-edit-water-grid">
                  <label className="diary-edit-entry-metric-field is-caffeine">
                    <span>{t("diary.quantityMl")}</span>
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
                    <span>{t("diary.timeHhMm")}</span>
                    <div className="diary-edit-entry-metric-value is-time-input">
                      <UiIcon name="clock" className="ui-icon" />
                      <Input
                        className="diary-edit-entry-metric-input"
                        type="text"
                        inputMode="numeric"
                        placeholder={t("diary.hhmmPlaceholder")}
                        value={editTimeText}
                        onChange={(event) => setEditTimeText(formatTimeInputToHhMm(event.target.value))}
                      />
                    </div>
                  </label>
                </div>
              ) : (
                <div className="diary-edit-entry-coffee-layout">
                  {orderedBrewMethods.length > 0 ? (
                    <section className="diary-edit-entry-block">
                      <h4 className="diary-edit-entry-block-title">{t("diary.methodUpper")}</h4>
                      <div
                        ref={editMethodScrollRef}
                        className={`diary-edit-entry-presets is-coffee ${editChipsDragging ? "is-dragging" : ""}`.trim()}
                        onPointerDown={(event) => handleEditScrollPointerDown(event, "method")}
                        onPointerMove={handleEditScrollPointerMove}
                        onPointerUp={handleEditScrollPointerEnd}
                        onPointerCancel={handleEditScrollPointerEnd}
                      >
                        {orderedBrewMethods.map((method) => (
                          <Button variant="chip"
                            key={method.name}
                            className={`chip-button period-chip diary-edit-entry-method-chip ${normalizeLookupText(editBrewMethod) === normalizeLookupText(method.name) ? "is-active" : ""}`.trim()}
                            onClick={() => setEditBrewMethod(method.name)}
                          >
                            {method.icon === "bolt" ? (
                              <UiIcon name="bolt" className="ui-icon home-elaboration-method-icon-bolt" aria-hidden />
                            ) : method.icon === "water" ? (
                              <UiIcon name="water" className="ui-icon home-elaboration-method-icon-water" aria-hidden />
                            ) : (
                              <img src={method.icon} alt="" aria-hidden="true" />
                            )}
                            <span>{localizedDiaryMethodChip(method.name)}</span>
                          </Button>
                        ))}
                      </div>
                    </section>
                  ) : null}
                  <section className="diary-edit-entry-block">
                    <h4 className="diary-edit-entry-block-title">{t("diary.type")}</h4>
                    <div
                      ref={editPrepScrollRef}
                      className={`diary-edit-entry-presets diary-edit-entry-tipo-presets is-coffee ${editChipsDragging ? "is-dragging" : ""}`.trim()}
                      onPointerDown={(event) => handleEditScrollPointerDown(event, "prep")}
                      onPointerMove={handleEditScrollPointerMove}
                      onPointerUp={handleEditScrollPointerEnd}
                      onPointerCancel={handleEditScrollPointerEnd}
                    >
                      {editMethodOptions.map((method) => {
                        const isActive = normalizeLookupText(editPreparationType) === normalizeLookupText(method.label);
                        return (
                          <Button
                            variant="plain"
                            key={method.label}
                            type="button"
                            className={`brew-tipo-card diary-edit-entry-tipo-chip ${isActive ? "is-active" : ""}`.trim()}
                            onClick={() => setEditPreparationType(method.label)}
                          >
                            <span className="brew-tipo-card-icon">
                              <img src={`/android-drawable/${method.drawable}`} alt="" aria-hidden="true" loading="lazy" decoding="async" />
                            </span>
                            <span className="brew-tipo-card-copy">
                              <span className="brew-tipo-card-label">{localizedDiaryPreparationChip(method.label)}</span>
                            </span>
                          </Button>
                        );
                      })}
                    </div>
                  </section>

                  <section className="diary-edit-entry-metrics-grid">
                    <label className="diary-edit-entry-metric-field is-caffeine">
                      <span>{t("diary.caffeineMg")}</span>
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
                      <span>{t("diary.doseGrams")}</span>
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
                    <h4 className="diary-edit-entry-block-title">{t("diary.size")}</h4>
                    <div
                      ref={editSizeScrollRef}
                      className={`diary-coffee-size-presets diary-edit-entry-size-presets ${editChipsDragging ? "is-dragging" : ""}`.trim()}
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
                          <span>{localizedDiarySizeChip(size.label)}</span>
                          <small>{size.range}</small>
                        </Button>
                      ))}
                    </div>
                  </section>

                  <label className="diary-edit-entry-metric-field is-readonly is-time">
                    <span>{t("diary.timeHhMm")}</span>
                    <div className="diary-edit-entry-metric-value is-time-input">
                      <UiIcon name="clock" className="ui-icon" />
                      <Input
                        className="diary-edit-entry-metric-input"
                        type="text"
                        inputMode="numeric"
                        placeholder={t("diary.hhmmPlaceholder")}
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
  const { t, locale } = useI18n();
  const localeTag = locale === "en" ? "en-US" : locale === "fr" ? "fr-FR" : locale === "pt" ? "pt-PT" : locale === "de" ? "de-DE" : "es-ES";
  const localizeMethodName = (value: string) => {
    if (locale === "es") return value;
    const key = normalizeLookupText(value);
    const en: Record<string, string> = { espresso: "Espresso", agua: "Water", italiana: "Moka", aeropress: "Aeropress", chemex: "Chemex", goteo: "Drip", "hario v60": "Hario V60" };
    const fr: Record<string, string> = { espresso: "Espresso", agua: "Eau", italiana: "Moka", aeropress: "Aeropress", chemex: "Chemex", goteo: "Filtre", "hario v60": "Hario V60" };
    const pt: Record<string, string> = { espresso: "Espresso", agua: "Agua", italiana: "Moka", aeropress: "Aeropress", chemex: "Chemex", goteo: "Coado", "hario v60": "Hario V60" };
    const de: Record<string, string> = { espresso: "Espresso", agua: "Wasser", italiana: "Moka", aeropress: "Aeropress", chemex: "Chemex", goteo: "Filter", "hario v60": "Hario V60" };
    const dict = locale === "fr" ? fr : locale === "pt" ? pt : locale === "de" ? de : en;
    return dict[key] ?? value;
  };
  const localizePrepName = (value: string) => {
    if (locale === "es") return value;
    const key = normalizeLookupText(value);
    const en: Record<string, string> = { espresso: "Espresso", americano: "Americano", capuchino: "Cappuccino", latte: "Latte", macchiato: "Macchiato", moca: "Mocha", vienes: "Viennese", irlandes: "Irish", frappuccino: "Frappuccino", "caramelo macchiato": "Caramel macchiato", corretto: "Corretto", freddo: "Freddo", "latte macchiato": "Latte macchiato", "leche con chocolate": "Hot chocolate milk", marroqui: "Moroccan", romano: "Romano", descafeinado: "Decaf" };
    const fr: Record<string, string> = { espresso: "Espresso", americano: "Americano", capuchino: "Cappuccino", latte: "Latte", macchiato: "Macchiato", moca: "Moka", vienes: "Viennois", irlandes: "Irlandais", frappuccino: "Frappuccino", "caramelo macchiato": "Macchiato caramel", corretto: "Corretto", freddo: "Freddo", "latte macchiato": "Latte macchiato", "leche con chocolate": "Lait au chocolat", marroqui: "Marocain", romano: "Romano", descafeinado: "Decafeine" };
    const pt: Record<string, string> = { espresso: "Espresso", americano: "Americano", capuchino: "Cappuccino", latte: "Latte", macchiato: "Macchiato", moca: "Moca", vienes: "Vienense", irlandes: "Irlandes", frappuccino: "Frappuccino", "caramelo macchiato": "Macchiato de caramelo", corretto: "Corretto", freddo: "Freddo", "latte macchiato": "Latte macchiato", "leche con chocolate": "Leite com chocolate", marroqui: "Marroquino", romano: "Romano", descafeinado: "Descafeinado" };
    const de: Record<string, string> = { espresso: "Espresso", americano: "Americano", capuchino: "Cappuccino", latte: "Latte", macchiato: "Macchiato", moca: "Mokka", vienes: "Wiener", irlandes: "Irisch", frappuccino: "Frappuccino", "caramelo macchiato": "Karamell-Macchiato", corretto: "Corretto", freddo: "Freddo", "latte macchiato": "Latte macchiato", "leche con chocolate": "Milch mit Schokolade", marroqui: "Marokkanisch", romano: "Romano", descafeinado: "Entkoffeiniert" };
    const dict = locale === "fr" ? fr : locale === "pt" ? pt : locale === "de" ? de : en;
    return dict[key] ?? value;
  };
  const localizeSizeName = (value: string) => {
    if (locale === "es") return value;
    const key = normalizeLookupText(value);
    const en: Record<string, string> = { espresso: "Espresso", pequeno: "Small", mediano: "Medium", grande: "Large", "tazon xl": "XL mug", corto: "Short", largo: "Large" };
    const fr: Record<string, string> = { espresso: "Espresso", pequeno: "Petit", mediano: "Moyen", grande: "Grand", "tazon xl": "Tasse XL", corto: "Court", largo: "Long" };
    const pt: Record<string, string> = { espresso: "Espresso", pequeno: "Pequeno", mediano: "Medio", grande: "Grande", "tazon xl": "Caneca XL", corto: "Curto", largo: "Longo" };
    const de: Record<string, string> = { espresso: "Espresso", pequeno: "Klein", mediano: "Mittel", grande: "Gross", "tazon xl": "XL-Tasse", corto: "Kurz", largo: "Lang" };
    const dict = locale === "fr" ? fr : locale === "pt" ? pt : locale === "de" ? de : en;
    return dict[key] ?? value;
  };
  const isWaterEntry = (entry.type || "").toUpperCase() === "WATER";
  const coffeeNameNorm = (entry.coffee_name || "").trim().toLowerCase();
  const isRegistroRapido = !isWaterEntry && (
    (entry.coffee_id == null || entry.coffee_id === "") ||
    coffeeNameNorm === t("diary.quickLog").toLowerCase() ||
    /registro\s*rapido/i.test(coffeeNameNorm)
  );
  const isBrewSinCafe = !isWaterEntry && (entry.coffee_id == null || entry.coffee_id === "") && (entry.coffee_name || "").trim() === "Café";
  const entryTitle = isBrewSinCafe
    ? t("top.coffee")
    : isRegistroRapido
      ? (entry.preparation_type || "").trim() || t("diary.quickLog")
      : entry.coffee_name || (isWaterEntry ? t("diary.water") : t("diary.entry"));
  const entrySubtitle = isWaterEntry
    ? `${Math.max(0, entry.amount_ml || 0)} ml`
    : isRegistroRapido
      ? t("diary.coffeeBrandUpper")
      : (brand || t("top.coffee")).toUpperCase();
  const rawPreparationValue = (entry.preparation_type || "").trim();
  const pipeIdx = rawPreparationValue.indexOf("|");
  const methodPart = pipeIdx >= 0 ? rawPreparationValue.slice(0, pipeIdx).trim() : rawPreparationValue;
  const tipoPart = pipeIdx >= 0 ? rawPreparationValue.slice(pipeIdx + 1).trim() : "";
  const elaborationMethod = (() => {
    const labMatch = methodPart.match(/(?:^lab:\s*|^elaboracion:\s*)([^()]+?)(?:\s*\(|$)/i);
    if (labMatch) return labMatch[1].trim();
    if (pipeIdx >= 0 && methodPart) return methodPart;
    return "";
  })();
  const hasLabFormat = /^Lab:\s*.+\s*\(/i.test(methodPart);
  const tipoDisplayValue = tipoPart || (!hasLabFormat && methodPart && pipeIdx < 0 ? methodPart : "");
  const prepValue = rawPreparationValue || (isWaterEntry ? t("diary.water") : "-");
  const doseFromPrep = ((entry.preparation_type || "").match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1] || "").replace(",", ".");
  const gramsFromField = Math.max(0, Number(entry.coffee_grams || 0));
  const doseValue = gramsFromField > 0 ? `${Math.round(gramsFromField)} g` : doseFromPrep ? `${Math.round(Number(doseFromPrep))} g` : (isWaterEntry ? "-" : "15 g");
  const sizeValueRaw = isWaterEntry
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
  const sizeValueEs = sizeValueRaw === "Pequeno" ? "Pequeño" : sizeValueRaw === "Tazon XL" ? "Tazón XL" : sizeValueRaw;
  const sizeValue = /\bml\b/i.test(sizeValueEs) ? sizeValueEs : localizeSizeName(sizeValueEs);
  const timeValue = new Date(entry.timestamp).toLocaleTimeString(localeTag, { hour: "2-digit", minute: "2-digit" });
  const tipoDrawable = (() => {
    const normalized = normalizeLookupText(tipoDisplayValue);
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
    if (normalized.includes("otros")) return null;
    return "maq_manual.png";
  })();
  const prepIconForElaboration = elaborationMethod && normalizeLookupText(elaborationMethod).includes("otros") ? ("bolt" as IconName) : undefined;
  const resultTasteFromPrep = (() => {
    if (!elaborationMethod) return null;
    const match = methodPart.match(/\(\s*([^)]+)\s*\)\s*$/);
    return match?.[1]?.trim() ?? null;
  })();
  const resultTasteIcon = ((): IconName | null => {
    if (!resultTasteFromPrep) return null;
    const n = normalizeLookupText(resultTasteFromPrep);
    if (n.includes("amargo")) return "taste-bitter";
    if (n.includes("acido")) return "taste-acid";
    if (n.includes("equilibrado")) return "taste-balance";
    if (n.includes("salado")) return "taste-salty";
    if (n.includes("acuoso")) return "taste-watery";
    if (n.includes("aspero")) return "grind";
    if (n.includes("dulce")) return "taste-sweet";
    return null;
  })();
  const sizeDrawable = (() => {
    if (isWaterEntry) return null;
    if (sizeValue === "Espresso") return "taza_espresso.png";
    if (sizeValue === "Corto") return "taza_pequeno.png";
    if (sizeValue === "Mediano") return "taza_mediano.png";
    return "taza_grande.png";
  })();
  const methodIcon: IconName | undefined = elaborationMethod && normalizeLookupText(elaborationMethod).includes("agua")
    ? "water"
    : elaborationMethod && normalizeLookupText(elaborationMethod).includes("otros")
      ? "bolt"
      : !elaborationMethod && normalizeLookupText(prepValue).includes("agua")
        ? "water"
        : !elaborationMethod && normalizeLookupText(prepValue).includes("otros")
          ? "bolt"
          : prepIconForElaboration;
  const methodDrawable = methodIcon
    ? undefined
    : (elaborationMethod ? elaborationDrawable : prepDrawable) ?? undefined;
  const methodMetaItem = elaborationMethod
    ? [{
        key: "method" as const,
        label: t("diary.methodUpper"),
        value: localizeMethodName(elaborationMethod),
        drawable: methodDrawable,
        icon: methodIcon
      }]
    : [];
  const tipoMetaItem = tipoDisplayValue
    ? [{
        key: "prep" as const,
        label: t("diary.type").toUpperCase(),
        value: localizePrepName(tipoDisplayValue),
        drawable: tipoDrawable,
        icon: undefined
      }]
    : [];
  const metaItemsBase: Array<{ key: string; icon?: IconName; drawable?: string; label: string; value: string }> = [
    { key: "caffeine", icon: "caffeine", label: t("diary.caffeine").toUpperCase(), value: `${Math.max(0, entry.caffeine_mg || 0)} mg` },
    { key: "dose", icon: "dose", label: t("diary.dosePerCoffee").toUpperCase(), value: doseValue },
    { key: "size", drawable: sizeDrawable ?? undefined, icon: isWaterEntry ? "bottle" : (sizeDrawable ? undefined : "stock"), label: t("diary.size").toUpperCase(), value: sizeValue },
    ...methodMetaItem,
    ...tipoMetaItem,
    ...(resultTasteFromPrep && resultTasteIcon
      ? [{ key: "result" as const, icon: resultTasteIcon, label: t("brew.result").toUpperCase(), value: resultTasteFromPrep }]
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
