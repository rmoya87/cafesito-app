import { type Dispatch, type SetStateAction, useCallback } from "react";
import { createDiaryEntry, insertPantryItem, updatePantryItem } from "../../data/supabaseApi";
import { estimateCaffeineMg, hasCaffeineFromLabel } from "../../core/brewEngine";
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
      const estimated = estimateCaffeineMg({
        source: "diary",
        methodOrPreparation: "Espresso",
        coffeeGrams: 15,
        hasCaffeine: hasCaffeineFromLabel(defaultCoffee.cafeina)
      });
      setDiaryCoffeeCaffeineDraft(String(estimated));
    } else {
      setDiaryCoffeeIdDraft("");
      setDiaryCoffeeCaffeineDraft("0");
    }
    setDiaryCoffeeMlDraft("250");
    setDiaryCoffeePreparationDraft("Espresso");
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
      if (selectedCoffee) {
        const estimated = estimateCaffeineMg({
          source: "diary",
          methodOrPreparation: diaryCoffeePreparationDraft.trim() || "Espresso",
          coffeeGrams: Math.max(0, Number(diaryCoffeeGramsDraft) || 15),
          hasCaffeine: hasCaffeineFromLabel(selectedCoffee.cafeina)
        });
        setDiaryCoffeeCaffeineDraft(String(estimated));
      }
    },
    [diaryCoffeeGramsDraft, diaryCoffeeOptions, diaryCoffeePreparationDraft, setDiaryCoffeeCaffeineDraft, setDiaryCoffeeIdDraft]
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

  /** Registra una entrada de agua en actividad (p. ej. desde elaboración método Agua). No abre/cierra sheet. */
  const saveWaterWithAmount = useCallback(
    async (amountMl: number) => {
      if (!activeUser) return;
      const amount = Math.max(1, Math.round(amountMl));
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
    },
    [activeUser, setDiaryEntries]
  );

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
      const coffeeGrams = payload?.coffeeGrams ?? Math.max(0, Math.round(Number(diaryCoffeeGramsDraft || 0) || 0));
      const preparationType = payload?.preparationType ?? (diaryCoffeePreparationDraft.trim() || "Espresso");
      const coffeeForCalc = coffeeId ? diaryCoffeeOptions.find((coffee) => coffee.id === coffeeId) ?? selectedDiaryCoffee : selectedDiaryCoffee;
      const caffeineMg = estimateCaffeineMg({
        source: "diary",
        methodOrPreparation: preparationType,
        coffeeGrams,
        hasCaffeine: coffeeForCalc ? hasCaffeineFromLabel(coffeeForCalc.cafeina) : true
      });
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

      // Restar de la despensa si el café está en despensa (puede haber varios ítems del mismo café)
      if (coffeeId && coffeeGrams > 0 && pantryItems.length > 0) {
        const toReduce = pantryItems.find((p) => p.coffee_id === coffeeId && p.grams_remaining >= coffeeGrams)
          ?? pantryItems.find((p) => p.coffee_id === coffeeId);
        if (toReduce) {
          const newRemaining = Math.max(0, toReduce.grams_remaining - coffeeGrams);
          const updated = await updatePantryItem(toReduce.id, {
            totalGrams: toReduce.total_grams,
            gramsRemaining: newRemaining
          });
          setPantryItems((prev) =>
            prev.map((p) => (p.id === toReduce.id ? updated : p))
          );
        }
      }

      setLastCreatedCoffeeNameForSheet?.(null);
      setShowDiaryCoffeeSheet(false);
    },
    [
      activeUser,
      diaryCoffeeCaffeineDraft,
      diaryCoffeeGramsDraft,
      diaryCoffeeMlDraft,
      diaryCoffeePreparationDraft,
      pantryItems,
      selectedDiaryCoffee,
      setDiaryEntries,
      setPantryItems,
      setLastCreatedCoffeeNameForSheet,
      setShowDiaryCoffeeSheet
    ]
  );

  const savePantry = useCallback(async () => {
    if (!activeUser || !selectedDiaryPantryCoffee) return;
    const gramsToAdd = Math.max(1, Math.round(Number(diaryPantryGramsDraft || 0)));
    const newRow = await insertPantryItem({
      coffeeId: selectedDiaryPantryCoffee.id,
      userId: activeUser.id,
      totalGrams: gramsToAdd,
      gramsRemaining: gramsToAdd
    });
    setPantryItems((prev) => [newRow, ...prev]);
    setDiaryTab("despensa");
    setShowDiaryAddPantrySheet(false);
  }, [
    activeUser,
    diaryPantryGramsDraft,
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
    saveWaterWithAmount,
    saveCoffee,
    savePantry
  };
}
