import { describe, expect, it } from "vitest";
import { cupSizeLabelForAmountMl, estimateCaffeineMg, hasCaffeineFromLabel } from "./brewEngine";
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

  it("returns dynamic brewing advice by phase/time", () => {
    const advice = getBrewingProcessAdvice("Hario V60", 16, 300, "Bloom", 4);
    expect(advice).toContain("Asegura saturacion completa");
    expect(advice).toContain("4 s");
  });
});
