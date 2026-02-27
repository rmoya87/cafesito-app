/**
 * Objetivos y cálculo de tendencia del diario.
 * Misma lógica de negocio que shared (Android/iOS): DiaryAnalyticsTargets.
 */

export type DiaryPeriod = "hoy" | "7d" | "30d";

const HYDRATION_TARGET_ML: Record<DiaryPeriod, number> = {
  hoy: 2000,
  "7d": 14_000,
  "30d": 60_000,
};

const CAFFEINE_TARGET_MG: Record<DiaryPeriod, number> = {
  hoy: 160,
  "7d": 1_120,
  "30d": 4_800,
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
