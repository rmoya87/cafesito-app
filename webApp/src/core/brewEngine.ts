import { normalizeLookupText } from "./text";

export type BrewSource = "brewlab" | "diary";

export type BrewCaffeineInput = {
  source: BrewSource;
  methodOrPreparation: string;
  coffeeGrams: number;
  hasCaffeine: boolean;
  /** Para método Rápido: volumen en ml (tamaño) para estimar cafeína de forma aproximada. */
  amountMl?: number;
  /** Para método Rápido: tipo de bebida (ej. Espresso, Americano) para estimar cafeína de forma aproximada. */
  drinkType?: string;
};

export const BREW_CALCULATION_VERSION = "2026.03.v1";

const BASE_MG_PER_GRAM_REGULAR = 6.3;
const BASE_MG_PER_GRAM_DECAF = 0.45;

function methodFactor(normalized: string): number {
  if (normalized.includes("moca")) return 1.1;
  if (normalized.includes("turco")) return 1.1;
  if (normalized.includes("italiana")) return 1.08;
  if (normalized.includes("romano")) return 0.92;
  if (normalized.includes("frappuccino")) return 0.88;
  if (normalized.includes("vienes")) return 0.9;
  if (normalized.includes("chemex")) return 0.95;
  if (normalized.includes("hario") || normalized.includes("v60") || normalized.includes("manual")) return 0.96;
  if (normalized.includes("prensa")) return 0.92;
  if (normalized.includes("sifon")) return 0.95;
  return 1;
}

export function hasCaffeineFromLabel(caffeineLabel?: string | null): boolean {
  const key = normalizeLookupText(caffeineLabel ?? "");
  if (!key) return true;
  if (key.includes("descaf")) return false;
  if (key === "no") return false;
  if (key.includes("sin cafe")) return false;
  if (key.includes("sincafe")) return false;
  return true;
}

function approximateCaffeineForRapido(amountMl: number, drinkType: string, hasCaffeine: boolean): number {
  const typeNorm = normalizeLookupText(drinkType);
  let regularMg: number;
  if (typeNorm.includes("espresso")) {
    const shots = amountMl / 30;
    regularMg = Math.max(0, Math.min(260, Math.round(shots * 65)));
  } else {
    const sizeTable: Array<[number, number]> = [[30, 65], [180, 95], [275, 145], [375, 195], [475, 250]];
    const prev = sizeTable.filter(([ml]) => ml <= amountMl).sort((a, b) => b[0] - a[0])[0];
    const next = sizeTable.filter(([ml]) => ml >= amountMl).sort((a, b) => a[0] - b[0])[0];
    if (!prev && next) regularMg = next[1];
    else if (prev && !next) regularMg = prev[1];
    else if (prev && next && prev[0] !== next[0]) {
      const t = (amountMl - prev[0]) / (next[0] - prev[0]);
      regularMg = Math.max(0, Math.round(prev[1] + t * (next[1] - prev[1])));
    } else {
      const nearest = sizeTable.reduce((best, cur) =>
        Math.abs(cur[0] - amountMl) < Math.abs(best[0] - amountMl) ? cur : best
      );
      regularMg = nearest[1];
    }
  }
  return hasCaffeine ? regularMg : Math.max(0, Math.round(regularMg * 0.05));
}

export function estimateCaffeineMg(input: BrewCaffeineInput): number {
  const normalizedMethod = normalizeLookupText(input.methodOrPreparation || "");
  const useRapidoApprox =
    (normalizedMethod === "rapido" || normalizedMethod === "otros") &&
    input.amountMl != null &&
    input.amountMl > 0 &&
    (input.drinkType ?? "").trim() !== "";
  if (useRapidoApprox) {
    return approximateCaffeineForRapido(input.amountMl!, input.drinkType!, input.hasCaffeine);
  }
  const grams = Math.max(0, Number(input.coffeeGrams) || 0);
  if (grams <= 0) return 0;
  const mgPerGram = input.hasCaffeine ? BASE_MG_PER_GRAM_REGULAR : BASE_MG_PER_GRAM_DECAF;
  return Math.max(0, Math.round(grams * mgPerGram * methodFactor(normalizedMethod)));
}

export function cupSizeLabelForAmountMl(amountMl: number): string {
  const options: Array<{ ml: number; label: string }> = [
    { ml: 30, label: "Espresso" },
    { ml: 180, label: "Pequeño" },
    { ml: 275, label: "Mediano" },
    { ml: 375, label: "Grande" },
    { ml: 475, label: "Tazón XL" }
  ];
  const safeAmount = Math.max(0, Math.round(Number(amountMl) || 0));
  return options.reduce((best, current) => {
    const bestDiff = Math.abs(best.ml - safeAmount);
    const currentDiff = Math.abs(current.ml - safeAmount);
    return currentDiff < bestDiff ? current : best;
  }).label;
}

/**
 * Dosis aproximada de café (gramos) para método Rápido según tipo de bebida y tamaño (ml).
 * Espresso: ~18 g por shot (30 ml); filtro/otros: por tamaño típico (~12–24 g).
 */
export function approximateCoffeeGramsForRapido(amountMl: number, drinkType: string): number {
  const typeNorm = normalizeLookupText(drinkType);
  if (typeNorm.includes("espresso")) {
    const shots = amountMl / 30;
    return Math.max(1, Math.min(100, Math.round(shots * 18)));
  }
  const sizeTable: Array<[number, number]> = [[30, 18], [180, 12], [275, 16], [375, 20], [475, 24]];
  const prev = sizeTable.filter(([ml]) => ml <= amountMl).sort((a, b) => b[0] - a[0])[0];
  const next = sizeTable.filter(([ml]) => ml >= amountMl).sort((a, b) => a[0] - b[0])[0];
  if (!prev && next) return next[1];
  if (prev && !next) return prev[1];
  if (prev && next && prev[0] !== next[0]) {
    const t = (amountMl - prev[0]) / (next[0] - prev[0]);
    return Math.max(1, Math.round(prev[1] + t * (next[1] - prev[1])));
  }
  const nearest = sizeTable.reduce((best, cur) =>
    Math.abs(cur[0] - amountMl) < Math.abs(best[0] - amountMl) ? cur : best
  );
  return nearest[1];
}
