import { describe, expect, it } from "vitest";
import { approximateCoffeeGramsForRapido, cupSizeLabelForAmountMl, estimateCaffeineMg, hasCaffeineFromLabel } from "./brewEngine";
import { getBrewingProcessAdvice, getBrewTimelineForMethod } from "./brew";
import { normalizeLookupText } from "./text";

describe("brewEngine", () => {
  it("keeps caffeine total stable when grams are equal", () => {
    const small = estimateCaffeineMg({
      source: "diary",
      methodOrPreparation: "Espresso",
      coffeeGrams: 18,
      hasCaffeine: true
    });
    const large = estimateCaffeineMg({
      source: "diary",
      methodOrPreparation: "Espresso",
      coffeeGrams: 18,
      hasCaffeine: true
    });
    expect(large).toBe(small);
  });

  it("detects decaf variants from label", () => {
    expect(hasCaffeineFromLabel("No")).toBe(false);
    expect(hasCaffeineFromLabel("Sin cafeina")).toBe(false);
    expect(hasCaffeineFromLabel("Si")).toBe(true);
  });

  it("matches contract vectors for caffeine outputs", () => {
    expect(
      estimateCaffeineMg({
        source: "diary",
        methodOrPreparation: "Espresso",
        coffeeGrams: 18,
        hasCaffeine: true
      })
    ).toBe(113);

    expect(
      estimateCaffeineMg({
        source: "brewlab",
        methodOrPreparation: "Hario V60",
        coffeeGrams: 15,
        hasCaffeine: true
      })
    ).toBe(91);

    expect(
      estimateCaffeineMg({
        source: "brewlab",
        methodOrPreparation: "Hario V60",
        coffeeGrams: 15,
        hasCaffeine: false
      })
    ).toBe(6);

    expect(
      estimateCaffeineMg({
        source: "diary",
        methodOrPreparation: "Moca",
        coffeeGrams: 18,
        hasCaffeine: true
      })
    ).toBe(125);
  });

  it("matches contract vectors for timeline outputs", () => {
    const pourover = getBrewTimelineForMethod("Hario V60", 300);
    expect(pourover).toHaveLength(3);
    expect(pourover[0]?.label).toBe("Bloom");

    const espresso = getBrewTimelineForMethod("Espresso", 36);
    expect(espresso).toHaveLength(1);
    expect(normalizeLookupText(espresso[0]?.label ?? "")).toBe("extraccion");
  });

  it("maps cup size by nearest volume", () => {
    expect(cupSizeLabelForAmountMl(36)).toBe("Espresso");
    expect(cupSizeLabelForAmountMl(260)).toBe("Mediano");
    expect(cupSizeLabelForAmountMl(410)).toBe("Grande");
  });

  it("Rápido: approximate coffee grams by type and size", () => {
    expect(approximateCoffeeGramsForRapido(30, "Espresso")).toBe(18);
    expect(approximateCoffeeGramsForRapido(60, "Espresso")).toBeGreaterThanOrEqual(35);
    expect(approximateCoffeeGramsForRapido(60, "Espresso")).toBeLessThanOrEqual(37);
    expect(approximateCoffeeGramsForRapido(275, "Americano")).toBeGreaterThanOrEqual(15);
    expect(approximateCoffeeGramsForRapido(275, "Americano")).toBeLessThanOrEqual(17);
    expect(approximateCoffeeGramsForRapido(180, "Filter")).toBeGreaterThanOrEqual(11);
    expect(approximateCoffeeGramsForRapido(180, "Filter")).toBeLessThanOrEqual(13);
  });

  it("Rápido: approximate caffeine by type and size", () => {
    const espresso30 = estimateCaffeineMg({
      source: "brewlab",
      methodOrPreparation: "Rápido",
      coffeeGrams: 0,
      hasCaffeine: true,
      amountMl: 30,
      drinkType: "Espresso"
    });
    expect(espresso30).toBeGreaterThanOrEqual(60);
    expect(espresso30).toBeLessThanOrEqual(70);
    const mediano = estimateCaffeineMg({
      source: "brewlab",
      methodOrPreparation: "Rápido",
      coffeeGrams: 0,
      hasCaffeine: true,
      amountMl: 275,
      drinkType: "Americano"
    });
    expect(mediano).toBeGreaterThanOrEqual(140);
    expect(mediano).toBeLessThanOrEqual(150);
    const decaf = estimateCaffeineMg({
      source: "brewlab",
      methodOrPreparation: "Rápido",
      coffeeGrams: 0,
      hasCaffeine: false,
      amountMl: 180,
      drinkType: "Filter"
    });
    expect(decaf).toBeGreaterThanOrEqual(0);
    expect(decaf).toBeLessThanOrEqual(10);
  });

  it("returns dynamic brewing advice by phase/time", () => {
    const advice = getBrewingProcessAdvice("Hario V60", 16, 300, "Bloom", 4);
    expect(advice).toContain("Asegura saturacion completa");
    expect(advice).toContain("4 s");
  });
});
