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
  diaryCoffeeGramsDraft,
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
  setPantrySheetStep,
  setDiaryWaterMlDraft,
  setDiaryCoffeeIdDraft,
  setDiaryCoffeeCaffeineDraft,
  setDiaryCoffeeMlDraft,
  setDiaryCoffeePreparationDraft,
  setDiaryPantryCoffeeIdDraft,
  setDiaryPantryGramsDraft,
  setLastCreatedCoffeeNameForSheet
}: {
  activeUser: UserRow | null;
  diaryCoffeeOptions: CoffeeRow[];
  selectedDiaryCoffee: CoffeeRow | null;
  selectedDiaryPantryCoffee: CoffeeRow | null;
  diaryWaterMlDraft: string;
  diaryCoffeeMlDraft: string;
  diaryCoffeeGramsDraft: string;
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
  setPantrySheetStep: (value: "select" | "form" | "createCoffee") => void;
  setDiaryWaterMlDraft: (value: string) => void;
  setDiaryCoffeeIdDraft: (value: string) => void;
  setDiaryCoffeeCaffeineDraft: (value: string) => void;
  setDiaryCoffeeMlDraft: (value: string) => void;
  setDiaryCoffeePreparationDraft: (value: string) => void;
  setDiaryPantryCoffeeIdDraft: (value: string) => void;
  setDiaryPantryGramsDraft: (value: string) => void;
  setLastCreatedCoffeeNameForSheet?: ((value: string | null) => void) | null;
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
    setDiaryPantryCoffeeIdDraft("");
    setDiaryPantryGramsDraft("250");
    setPantrySheetStep("select");
    setShowDiaryQuickActions(false);
    setShowDiaryAddPantrySheet(true);
  }, [setDiaryPantryCoffeeIdDraft, setDiaryPantryGramsDraft, setPantrySheetStep, setShowDiaryAddPantrySheet, setShowDiaryQuickActions]);

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

  const saveCoffee = useCallback(
    async (payload?: {
      coffeeId: string | null;
      coffeeName: string;
      coffeeBrand?: string;
      amountMl: number;
      caffeineMg: number;
      coffeeGrams?: number;
      preparationType: string;
      sizeLabel?: string | null;
    }) => {
      if (!activeUser) return;
      const coffeeId = payload?.coffeeId ?? selectedDiaryCoffee?.id ?? null;
      const coffeeName = payload?.coffeeName ?? selectedDiaryCoffee?.nombre ?? "Registro rápido";
      const coffeeBrand = payload?.coffeeBrand ?? selectedDiaryCoffee?.marca ?? "";
      const amountMl = payload?.amountMl ?? Math.max(1, Number(diaryCoffeeMlDraft || 0));
      const caffeineMg = payload?.caffeineMg ?? Math.max(0, Number(diaryCoffeeCaffeineDraft || 0));
      const coffeeGrams = payload?.coffeeGrams ?? Math.max(0, Math.round(Number(diaryCoffeeGramsDraft || 0) || 0));
      const preparationType = payload?.preparationType ?? (diaryCoffeePreparationDraft.trim() || "Manual");
      const sizeLabel = payload?.sizeLabel ?? null;
      const created = await createDiaryEntry({
        userId: activeUser.id,
        coffeeId,
        coffeeName,
        coffeeBrand,
        amountMl,
        caffeineMg,
        coffeeGrams,
        preparationType,
        sizeLabel,
        type: "CUP"
      });
      setDiaryEntries((prev) => [created, ...prev]);
      setLastCreatedCoffeeNameForSheet?.(null);
      setShowDiaryCoffeeSheet(false);
    },
    [
      activeUser,
      diaryCoffeeCaffeineDraft,
      diaryCoffeeGramsDraft,
      diaryCoffeeMlDraft,
      diaryCoffeePreparationDraft,
      selectedDiaryCoffee,
      setDiaryEntries,
      setLastCreatedCoffeeNameForSheet,
      setShowDiaryCoffeeSheet
    ]
  );

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
