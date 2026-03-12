import { useCallback } from "react";
import { createDiaryEntry, deleteDiaryEntry, deletePantryItemById, updateDiaryEntry, updatePantryItem } from "../../data/supabaseApi";
import { cupSizeLabelForAmountMl, estimateCaffeineMg, hasCaffeineFromLabel } from "../../core/brewEngine";
import { normalizeLookupText } from "../../core/text";
import type { CoffeeRow, DiaryEntryRow, PantryItemRow, UserRow } from "../../types";

export function useDiaryActions({
  activeUser,
  selectedCoffee,
  coffeeGrams,
  brewMethod,
  waterMl,
  setDiaryEntries,
  setPantryItems,
  setBrewRunning,
  setTimerSeconds,
  setBrewStep,
  navigateToDiary
}: {
  activeUser: UserRow | null;
  selectedCoffee: CoffeeRow | undefined;
  coffeeGrams: number;
  brewMethod: string;
  waterMl: number;
  setDiaryEntries: React.Dispatch<React.SetStateAction<DiaryEntryRow[]>>;
  setPantryItems: React.Dispatch<React.SetStateAction<PantryItemRow[]>>;
  setBrewRunning: (value: boolean) => void;
  setTimerSeconds: (value: number) => void;
  setBrewStep: (value: "method" | "coffee" | "config" | "brewing" | "result") => void;
  navigateToDiary: () => void;
}) {
  const saveBrewToDiary = useCallback(
    async (taste: string, drinkType?: string) => {
      if (!activeUser) return;
      const normMethod = normalizeLookupText(brewMethod || "");
      const isRapido = normMethod === "rapido" || normMethod === "otros";
      const drinkTypeVal = drinkType?.trim() || "Espresso";
      const gramsToSave = Math.max(0, coffeeGrams);
      const caffeineMg = selectedCoffee
        ? estimateCaffeineMg({
            source: "brewlab",
            methodOrPreparation: brewMethod || "Metodo",
            coffeeGrams: gramsToSave,
            hasCaffeine: hasCaffeineFromLabel(selectedCoffee.cafeina),
            ...(isRapido && waterMl > 0 ? { amountMl: waterMl, drinkType: drinkTypeVal } : {})
          })
        : isRapido && waterMl > 0
          ? estimateCaffeineMg({
              source: "brewlab",
              methodOrPreparation: brewMethod || "Metodo",
              coffeeGrams: 0,
              hasCaffeine: true,
              amountMl: waterMl,
              drinkType: drinkTypeVal
            })
          : 0;
      const methodPart = `Lab: ${brewMethod || "Metodo"} (${taste})`;
      const preparationType = drinkType?.trim() ? `${methodPart}|${drinkType.trim()}` : methodPart;
      const sizeLabel = cupSizeLabelForAmountMl(waterMl);
      const created = await createDiaryEntry({
        userId: activeUser.id,
        coffeeId: selectedCoffee?.id ?? null,
        coffeeName: selectedCoffee?.nombre ?? "Café",
        amountMl: waterMl,
        caffeineMg,
        coffeeGrams: gramsToSave,
        preparationType,
        sizeLabel,
        type: "CUP"
      });
      setDiaryEntries((prev) => [created, ...prev]);
      setBrewRunning(false);
      setTimerSeconds(0);
      setBrewStep("method");
      navigateToDiary();
    },
    [activeUser, brewMethod, coffeeGrams, navigateToDiary, selectedCoffee, setBrewRunning, setBrewStep, setDiaryEntries, setTimerSeconds, waterMl]
  );

  /** Elimina solo la entrada del diario (actividad). No modifica la despensa: el café sigue en pantry si estaba. */
  const handleDeleteDiaryEntry = useCallback(
    async (entryId: number) => {
      if (!activeUser) return;
      await deleteDiaryEntry(entryId, activeUser.id);
      setDiaryEntries((prev) => prev.filter((entry) => entry.id !== entryId));
    },
    [activeUser, setDiaryEntries]
  );

  const handleUpdateDiaryEntry = useCallback(
    async (
      entryId: number,
      amountMl: number,
      caffeineMg: number,
      preparationType: string,
      coffeeGrams?: number,
      sizeLabel?: string | null,
      timestampMs?: number
    ) => {
      if (!activeUser) return;
      const updated = await updateDiaryEntry({
        entryId,
        userId: activeUser.id,
        amountMl,
        caffeineMg,
        preparationType,
        coffeeGrams,
        sizeLabel,
        timestampMs
      });
      setDiaryEntries((prev) => prev.map((entry) => (entry.id === updated.id ? { ...entry, ...updated } : entry)));
    },
    [activeUser, setDiaryEntries]
  );

  const handleUpdatePantryStock = useCallback(
    async (pantryItemId: string, totalGrams: number, gramsRemaining: number) => {
      if (!activeUser) return;
      const total = Math.max(1, Math.round(totalGrams));
      const remaining = Math.max(0, Math.min(total, Math.round(gramsRemaining)));
      const updated = await updatePantryItem(pantryItemId, { totalGrams: total, gramsRemaining: remaining });
      setPantryItems((prev) => prev.map((row) => (row.id === updated.id ? updated : row)));
    },
    [activeUser, setPantryItems]
  );

  const handleRemovePantryItem = useCallback(
    async (pantryItemId: string) => {
      if (!activeUser) return;
      await deletePantryItemById(pantryItemId);
      setPantryItems((prev) => prev.filter((row) => row.id !== pantryItemId));
    },
    [activeUser, setPantryItems]
  );

  return {
    saveBrewToDiary,
    handleDeleteDiaryEntry,
    handleUpdateDiaryEntry,
    handleUpdatePantryStock,
    handleRemovePantryItem
  };
}

