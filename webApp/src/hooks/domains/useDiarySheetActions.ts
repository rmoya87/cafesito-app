import { type Dispatch, type SetStateAction, useCallback } from "react";
import { createDiaryEntry, upsertPantryStock } from "../../data/supabaseApi";
import { normalizeLookupText } from "../../core/text";
import type { CoffeeRow, DiaryEntryRow, PantryItemRow, UserRow } from "../../types";

export function useDiarySheetActions({
  activeUser,
  diaryCoffeeOptions,
  selectedDiaryCoffee,
  selectedDiaryPantryCoffee,
  diaryWaterMlDraft,
  diaryCoffeeMlDraft,
  diaryCoffeeCaffeineDraft,
  diaryCoffeePreparationDraft,
  diaryPantryGramsDraft,
  pantryItems,
  setDiaryEntries,
  setPantryItems,
  setDiaryTab,
  setShowDiaryQuickActions,
  setShowDiaryWaterSheet,
  setShowDiaryCoffeeSheet,
  setShowDiaryAddPantrySheet,
  setDiaryWaterMlDraft,
  setDiaryCoffeeIdDraft,
  setDiaryCoffeeCaffeineDraft,
  setDiaryCoffeeMlDraft,
  setDiaryCoffeePreparationDraft,
  setDiaryPantryCoffeeIdDraft,
  setDiaryPantryGramsDraft
}: {
  activeUser: UserRow | null;
  diaryCoffeeOptions: CoffeeRow[];
  selectedDiaryCoffee: CoffeeRow | null;
  selectedDiaryPantryCoffee: CoffeeRow | null;
  diaryWaterMlDraft: string;
  diaryCoffeeMlDraft: string;
  diaryCoffeeCaffeineDraft: string;
  diaryCoffeePreparationDraft: string;
  diaryPantryGramsDraft: string;
  pantryItems: PantryItemRow[];
  setDiaryEntries: Dispatch<SetStateAction<DiaryEntryRow[]>>;
  setPantryItems: Dispatch<SetStateAction<PantryItemRow[]>>;
  setDiaryTab: (value: "actividad" | "despensa") => void;
  setShowDiaryQuickActions: (value: boolean) => void;
  setShowDiaryWaterSheet: (value: boolean) => void;
  setShowDiaryCoffeeSheet: (value: boolean) => void;
  setShowDiaryAddPantrySheet: (value: boolean) => void;
  setDiaryWaterMlDraft: (value: string) => void;
  setDiaryCoffeeIdDraft: (value: string) => void;
  setDiaryCoffeeCaffeineDraft: (value: string) => void;
  setDiaryCoffeeMlDraft: (value: string) => void;
  setDiaryCoffeePreparationDraft: (value: string) => void;
  setDiaryPantryCoffeeIdDraft: (value: string) => void;
  setDiaryPantryGramsDraft: (value: string) => void;
}) {
  const openWaterSheet = useCallback(() => {
    setDiaryWaterMlDraft("250");
    setShowDiaryQuickActions(false);
    setShowDiaryWaterSheet(true);
  }, [setDiaryWaterMlDraft, setShowDiaryQuickActions, setShowDiaryWaterSheet]);

  const openCoffeeSheet = useCallback(() => {
    const defaultCoffee = diaryCoffeeOptions[0];
    if (defaultCoffee) {
      setDiaryCoffeeIdDraft(defaultCoffee.id);
      const isDecaf = normalizeLookupText(defaultCoffee.cafeina ?? "").includes("sin");
      setDiaryCoffeeCaffeineDraft(isDecaf ? "5" : "95");
    } else {
      setDiaryCoffeeIdDraft("");
      setDiaryCoffeeCaffeineDraft("95");
    }
    setDiaryCoffeeMlDraft("250");
    setDiaryCoffeePreparationDraft("Manual");
    setShowDiaryQuickActions(false);
    setShowDiaryCoffeeSheet(true);
  }, [
    diaryCoffeeOptions,
    setDiaryCoffeeCaffeineDraft,
    setDiaryCoffeeIdDraft,
    setDiaryCoffeeMlDraft,
    setDiaryCoffeePreparationDraft,
    setShowDiaryCoffeeSheet,
    setShowDiaryQuickActions
  ]);

  const openAddPantrySheet = useCallback(() => {
    const defaultCoffee = diaryCoffeeOptions[0];
    setDiaryPantryCoffeeIdDraft(defaultCoffee?.id ?? "");
    setDiaryPantryGramsDraft("250");
    setShowDiaryQuickActions(false);
    setShowDiaryAddPantrySheet(true);
  }, [diaryCoffeeOptions, setDiaryPantryCoffeeIdDraft, setDiaryPantryGramsDraft, setShowDiaryAddPantrySheet, setShowDiaryQuickActions]);

  const setDiaryCoffeeDraftWithCaffeine = useCallback(
    (coffeeId: string) => {
      setDiaryCoffeeIdDraft(coffeeId);
      const selectedCoffee = diaryCoffeeOptions.find((coffee) => coffee.id === coffeeId);
      const isDecaf = normalizeLookupText(selectedCoffee?.cafeina ?? "").includes("sin");
      if (selectedCoffee) setDiaryCoffeeCaffeineDraft(isDecaf ? "5" : "95");
    },
    [diaryCoffeeOptions, setDiaryCoffeeCaffeineDraft, setDiaryCoffeeIdDraft]
  );

  const saveWater = useCallback(async () => {
    if (!activeUser) return;
    const amount = Math.max(1, Math.round(Number(diaryWaterMlDraft || 0)));
    const created = await createDiaryEntry({
      userId: activeUser.id,
      coffeeId: null,
      coffeeName: "Agua",
      amountMl: amount,
      caffeineMg: 0,
      preparationType: "None",
      type: "WATER"
    });
    setDiaryEntries((prev) => [created, ...prev]);
    setShowDiaryWaterSheet(false);
  }, [activeUser, diaryWaterMlDraft, setDiaryEntries, setShowDiaryWaterSheet]);

  const saveCoffee = useCallback(async () => {
    if (!activeUser || !selectedDiaryCoffee) return;
    const created = await createDiaryEntry({
      userId: activeUser.id,
      coffeeId: selectedDiaryCoffee.id,
      coffeeName: selectedDiaryCoffee.nombre,
      amountMl: Math.max(1, Number(diaryCoffeeMlDraft || 0)),
      caffeineMg: Math.max(0, Number(diaryCoffeeCaffeineDraft || 0)),
      preparationType: diaryCoffeePreparationDraft.trim() || "Manual",
      type: "CUP"
    });
    setDiaryEntries((prev) => [created, ...prev]);
    setShowDiaryCoffeeSheet(false);
  }, [
    activeUser,
    diaryCoffeeCaffeineDraft,
    diaryCoffeeMlDraft,
    diaryCoffeePreparationDraft,
    selectedDiaryCoffee,
    setDiaryEntries,
    setShowDiaryCoffeeSheet
  ]);

  const savePantry = useCallback(async () => {
    if (!activeUser || !selectedDiaryPantryCoffee) return;
    const gramsToAdd = Math.max(1, Math.round(Number(diaryPantryGramsDraft || 0)));
    const existing = pantryItems.find((item) => item.user_id === activeUser.id && item.coffee_id === selectedDiaryPantryCoffee.id);
    const total = Math.max(gramsToAdd, (existing?.total_grams ?? 0) + gramsToAdd);
    const remaining = Math.max(gramsToAdd, (existing?.grams_remaining ?? 0) + gramsToAdd);
    const updated = await upsertPantryStock({
      coffeeId: selectedDiaryPantryCoffee.id,
      userId: activeUser.id,
      totalGrams: total,
      gramsRemaining: remaining
    });
    setPantryItems((prev) => [updated, ...prev.filter((row) => !(row.user_id === updated.user_id && row.coffee_id === updated.coffee_id))]);
    setDiaryTab("despensa");
    setShowDiaryAddPantrySheet(false);
  }, [
    activeUser,
    diaryPantryGramsDraft,
    pantryItems,
    selectedDiaryPantryCoffee,
    setDiaryTab,
    setPantryItems,
    setShowDiaryAddPantrySheet
  ]);

  return {
    openWaterSheet,
    openCoffeeSheet,
    openAddPantrySheet,
    setDiaryCoffeeDraftWithCaffeine,
    saveWater,
    saveCoffee,
    savePantry
  };
}
