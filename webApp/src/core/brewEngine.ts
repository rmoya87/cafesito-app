import { normalizeLookupText } from "./text";

export type BrewSource = "brewlab" | "diary";

export type BrewCaffeineInput = {
  source: BrewSource;
  methodOrPreparation: string;
  coffeeGrams: number;
  hasCaffeine: boolean;
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

export function estimateCaffeineMg(input: BrewCaffeineInput): number {
  const grams = Math.max(0, Number(input.coffeeGrams) || 0);
  if (grams <= 0) return 0;
  const normalizedMethod = normalizeLookupText(input.methodOrPreparation || "");
  const mgPerGram = input.hasCaffeine ? BASE_MG_PER_GRAM_REGULAR : BASE_MG_PER_GRAM_DECAF;
  return Math.max(0, Math.round(grams * mgPerGram * methodFactor(normalizedMethod)));
}

export function cupSizeLabelForAmountMl(amountMl: number): string {
  const options: Array<{ ml: number; label: string }> = [
    { ml: 30, label: "Espresso" },
    { ml: 180, label: "Pequeno" },
    { ml: 275, label: "Mediano" },
    { ml: 375, label: "Grande" },
    { ml: 475, label: "Tazon XL" }
  ];
  const safeAmount = Math.max(0, Math.round(Number(amountMl) || 0));
  return options.reduce((best, current) => {
    const bestDiff = Math.abs(best.ml - safeAmount);
    const currentDiff = Math.abs(current.ml - safeAmount);
    return currentDiff < bestDiff ? current : best;
  }).label;
}
