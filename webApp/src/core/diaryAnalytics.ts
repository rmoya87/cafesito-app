/**
 * Objetivos y cálculo de tendencia del diario.
 * Misma lógica de negocio que shared (Android/iOS): DiaryAnalyticsTargets.
 */

export type DiaryPeriod = "hoy" | "7d" | "30d" | "week";

/** Devuelve el lunes (YYYY-MM-DD) de la semana que contiene la fecha dada (YYYY-MM-DD). */
export function getMondayOfWeek(dateStr: string): string {
  const [y, m, d] = dateStr.split("-").map(Number);
  const date = new Date(y, (m ?? 1) - 1, d ?? 1);
  const day = date.getDay();
  const diff = day === 0 ? -6 : 1 - day;
  date.setDate(date.getDate() + diff);
  return date.getFullYear() + "-" + String(date.getMonth() + 1).padStart(2, "0") + "-" + String(date.getDate()).padStart(2, "0");
}

/** Formato "d/m - d/m" para el rango de la semana (lunes a domingo). */
export function formatWeekRange(mondayStr: string): string {
  const [y, m, d] = mondayStr.split("-").map(Number);
  const mon = new Date(y, (m ?? 1) - 1, d ?? 1);
  const sun = new Date(mon);
  sun.setDate(sun.getDate() + 6);
  const d1 = mon.getDate();
  const m1 = mon.getMonth() + 1;
  const d2 = sun.getDate();
  const m2 = sun.getMonth() + 1;
  return `${d1}/${m1} - ${d2}/${m2}`;
}

/** Formato "marzo 2025" para el mes (clave YYYY-MM). */
export function formatMonthYear(monthKey: string): string {
  const [y, m] = monthKey.split("-").map(Number);
  const date = new Date(y, (m ?? 1) - 1, 1);
  return date.toLocaleDateString("es-ES", { month: "long", year: "numeric" });
}

/** Inicio (00:00) y fin (23:59:59.999) de la semana en ms. */
export function getWeekStartEndMs(mondayStr: string): { startMs: number; endMs: number } {
  const [y, m, d] = mondayStr.split("-").map(Number);
  const start = new Date(y, (m ?? 1) - 1, d ?? 1);
  start.setHours(0, 0, 0, 0);
  const end = new Date(start);
  end.setDate(end.getDate() + 7);
  end.setMilliseconds(-1);
  return { startMs: start.getTime(), endMs: end.getTime() };
}

export type DiaryPeriodLegacy = "hoy" | "7d" | "30d";

const HYDRATION_TARGET_ML: Record<DiaryPeriod, number> = {
  hoy: 2000,
  "7d": 14_000,
  "30d": 60_000,
  week: 14_000,
};

const CAFFEINE_TARGET_MG: Record<DiaryPeriod, number> = {
  hoy: 160,
  "7d": 1_120,
  "30d": 4_800,
  week: 1_120,
};

/** Objetivo de hidratación (ml) para el período. */
export function hydrationTargetMl(period: DiaryPeriod): number {
  return HYDRATION_TARGET_ML[period];
}

/** Objetivo de cafeína (mg) para el período. */
export function caffeineTargetMg(period: DiaryPeriod): number {
  return CAFFEINE_TARGET_MG[period];
}

/**
 * Porcentaje de tendencia vs objetivo: (actual - objetivo) / objetivo * 100.
 * Positivo = por encima del objetivo, negativo = por debajo.
 */
export function trendPercent(actual: number, target: number): number {
  if (target <= 0) return 0;
  return Math.round(((actual - target) / target) * 100);
}

/** Progreso de hidratación 0..100. */
export function hydrationProgressPercent(actualMl: number, period: DiaryPeriod): number {
  const target = hydrationTargetMl(period);
  if (target <= 0) return 0;
  return Math.max(0, Math.min(100, Math.round((Math.max(0, actualMl) / target) * 100)));
}

const HYDRATION_TARGET_ML_DAY = 2000;
const LAST_30_DAYS = 30;

export interface DiaryEntryForAvg {
  timestamp: number;
  type?: string;
  caffeine_mg?: number;
  amount_ml?: number;
}

/**
 * Media de un día en los últimos 30 días (común Android/iOS/webapp).
 * Para cada uno de los últimos 30 días: cafeína total, tazas y % hidratación del día.
 * Devuelve el promedio de esas 30 cifras (días sin datos = 0).
 */
export function last30DaysDailyAverages(entries: DiaryEntryForAvg[]): {
  avgCaffeinePerDay: number;
  avgCupsPerDay: number;
  avgHydrationPctPerDay: number;
} {
  const now = new Date();
  const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const dayMs = 86400000;
  const daily: Array<{ caffeine: number; cups: number; waterMl: number; hydrationPct: number }> = [];
  for (let i = 0; i < LAST_30_DAYS; i++) {
    const dayStart = todayStart - (LAST_30_DAYS - 1 - i) * dayMs;
    const dayEnd = dayStart + dayMs;
    const dayEntries = entries.filter((e) => {
      const t = Number(e.timestamp);
      return t >= dayStart && t < dayEnd;
    });
    const caffeine = dayEntries
      .filter((e) => (e.type || "").toUpperCase() !== "WATER")
      .reduce((acc, e) => acc + Math.max(0, e.caffeine_mg || 0), 0);
    const cups = dayEntries.filter((e) => (e.type || "").toUpperCase() !== "WATER").length;
    const waterMl = dayEntries
      .filter((e) => (e.type || "").toUpperCase() === "WATER")
      .reduce((acc, e) => acc + Math.max(0, e.amount_ml || 0), 0);
    const hydrationPct = Math.min(100, Math.round((waterMl / HYDRATION_TARGET_ML_DAY) * 100));
    daily.push({ caffeine, cups, waterMl, hydrationPct });
  }
  const sumCaffeine = daily.reduce((a, d) => a + d.caffeine, 0);
  const sumCups = daily.reduce((a, d) => a + d.cups, 0);
  const sumHydrationPct = daily.reduce((a, d) => a + d.hydrationPct, 0);
  return {
    avgCaffeinePerDay: Math.round(sumCaffeine / LAST_30_DAYS),
    avgCupsPerDay: Math.round((sumCups / LAST_30_DAYS) * 10) / 10,
    avgHydrationPctPerDay: Math.round(sumHydrationPct / LAST_30_DAYS),
  };
}
