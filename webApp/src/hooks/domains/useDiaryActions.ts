import { useCallback } from "react";
import { createDiaryEntry, deleteDiaryEntry, deletePantryItem, updateDiaryEntry, upsertPantryStock } from "../../data/supabaseApi";
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
    async (taste: string) => {
      if (!activeUser || !selectedCoffee) return;
      const decaf = normalizeLookupText(selectedCoffee.cafeina ?? "").includes("sin");
      const caffeineMg = Math.max(0, Math.round(coffeeGrams * (decaf ? 2 : 9)));
      const preparationType = `Lab: ${brewMethod || "Metodo"} (${taste})`;
      const created = await createDiaryEntry({
        userId: activeUser.id,
        coffeeId: selectedCoffee.id,
        coffeeName: selectedCoffee.nombre,
        amountMl: waterMl,
        caffeineMg,
        preparationType,
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

  const handleDeleteDiaryEntry = useCallback(
    async (entryId: number) => {
      if (!activeUser) return;
      await deleteDiaryEntry(entryId, activeUser.id);
      setDiaryEntries((prev) => prev.filter((entry) => entry.id !== entryId));
    },
    [activeUser, setDiaryEntries]
  );

  const handleUpdateDiaryEntry = useCallback(
    async (entryId: number, amountMl: number, caffeineMg: number, preparationType: string, timestampMs?: number) => {
      if (!activeUser) return;
      const updated = await updateDiaryEntry({
        entryId,
        userId: activeUser.id,
        amountMl,
        caffeineMg,
        preparationType,
        timestampMs
      });
      setDiaryEntries((prev) => prev.map((entry) => (entry.id === updated.id ? { ...entry, ...updated } : entry)));
    },
    [activeUser, setDiaryEntries]
  );

  const handleUpdatePantryStock = useCallback(
    async (coffeeId: string, totalGrams: number, gramsRemaining: number) => {
      if (!activeUser) return;
      const total = Math.max(1, Math.round(totalGrams));
      const remaining = Math.max(0, Math.min(total, Math.round(gramsRemaining)));
      const updated = await upsertPantryStock({
        coffeeId,
        userId: activeUser.id,
        totalGrams: total,
        gramsRemaining: remaining
      });
      setPantryItems((prev) =>
        [updated, ...prev.filter((row) => !(row.user_id === updated.user_id && row.coffee_id === updated.coffee_id))]
      );
    },
    [activeUser, setPantryItems]
  );

  const handleRemovePantryItem = useCallback(
    async (coffeeId: string) => {
      if (!activeUser) return;
      await deletePantryItem(coffeeId, activeUser.id);
      setPantryItems((prev) => prev.filter((row) => !(row.user_id === activeUser.id && row.coffee_id === coffeeId)));
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
