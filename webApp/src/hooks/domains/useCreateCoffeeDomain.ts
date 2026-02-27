import { useCallback, useEffect, useMemo, useState } from "react";
import { upsertCustomCoffee, upsertPantryStock, uploadImageFile } from "../../data/supabaseApi";
import type { CoffeeRow, PantryItemRow, UserRow } from "../../types";

const initialDraft = {
  name: "",
  brand: "",
  specialty: "",
  country: "",
  format: "",
  roast: "",
  variety: "",
  hasCaffeine: true,
  totalGrams: 250
};

type Draft = typeof initialDraft;

export function useCreateCoffeeDomain({
  activeUser,
  setCustomCoffees,
  setPantryItems,
  setBrewCoffeeId,
  setBrewStep
}: {
  activeUser: UserRow | null;
  setCustomCoffees: React.Dispatch<React.SetStateAction<CoffeeRow[]>>;
  setPantryItems: React.Dispatch<React.SetStateAction<PantryItemRow[]>>;
  setBrewCoffeeId: (value: string) => void;
  setBrewStep: (value: "method" | "coffee" | "config" | "brewing" | "result") => void;
}) {
  const [showCreateCoffeeComposer, setShowCreateCoffeeComposer] = useState(false);
  const [createCoffeeSaving, setCreateCoffeeSaving] = useState(false);
  const [createCoffeeError, setCreateCoffeeError] = useState<string | null>(null);
  const [createCoffeeDraft, setCreateCoffeeDraft] = useState<Draft>(initialDraft);
  const [createCoffeeImageFile, setCreateCoffeeImageFile] = useState<File | null>(null);
  const [createCoffeeImagePreviewUrl, setCreateCoffeeImagePreviewUrl] = useState("");

  const closeCreateCoffeeComposer = useCallback(() => {
    if (createCoffeeSaving) return;
    setShowCreateCoffeeComposer(false);
    setCreateCoffeeError(null);
  }, [createCoffeeSaving]);

  const openCreateCoffeeComposer = useCallback(() => {
    setCreateCoffeeError(null);
    setShowCreateCoffeeComposer(true);
  }, []);

  const saveCreateCoffee = useCallback(async (options?: { fromDiarySheet?: boolean }): Promise<{ id: string; name: string } | null> => {
    if (!activeUser) return null;
    if (!createCoffeeDraft.name.trim() || !createCoffeeDraft.brand.trim()) {
      setCreateCoffeeError("Nombre y marca son obligatorios.");
      return null;
    }
    if (!createCoffeeDraft.specialty.trim() || !createCoffeeDraft.country.trim() || !createCoffeeDraft.format.trim()) {
      setCreateCoffeeError("Completa especialidad, país y formato.");
      return null;
    }
    const fromDiarySheet = options?.fromDiarySheet === true;
    setCreateCoffeeSaving(true);
    setCreateCoffeeError(null);
    try {
      let imageUrl = "";
      if (createCoffeeImageFile) imageUrl = await uploadImageFile("posts", createCoffeeImageFile);
      const created = await upsertCustomCoffee({
        userId: activeUser.id,
        name: createCoffeeDraft.name,
        brand: createCoffeeDraft.brand,
        specialty: createCoffeeDraft.specialty,
        country: createCoffeeDraft.country,
        format: createCoffeeDraft.format,
        roast: createCoffeeDraft.roast || null,
        variety: createCoffeeDraft.variety || null,
        hasCaffeine: createCoffeeDraft.hasCaffeine,
        imageUrl,
        totalGrams: createCoffeeDraft.totalGrams
      });

      const pantryRow = await upsertPantryStock({
        coffeeId: created.id,
        userId: activeUser.id,
        totalGrams: Math.max(1, createCoffeeDraft.totalGrams || 250),
        gramsRemaining: Math.max(1, createCoffeeDraft.totalGrams || 250)
      });

      setCustomCoffees((prev) => [created, ...prev.filter((item) => item.id !== created.id)]);
      setPantryItems((prev) => [pantryRow, ...prev.filter((item) => !(item.coffee_id === pantryRow.coffee_id && item.user_id === pantryRow.user_id))]);
      if (!fromDiarySheet) {
        setBrewCoffeeId(created.id);
        setBrewStep("config");
        setShowCreateCoffeeComposer(false);
      }
      setCreateCoffeeDraft(initialDraft);
      if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
      setCreateCoffeeImageFile(null);
      setCreateCoffeeImagePreviewUrl("");
      return { id: created.id, name: created.nombre };
    } catch (error) {
      setCreateCoffeeError((error as Error).message);
      return null;
    } finally {
      setCreateCoffeeSaving(false);
    }
  }, [
    activeUser,
    createCoffeeDraft,
    createCoffeeImageFile,
    createCoffeeImagePreviewUrl,
    setBrewCoffeeId,
    setBrewStep,
    setCustomCoffees,
    setPantryItems
  ]);

  const onPickImage = useCallback((file: File | null, previewUrl: string) => {
    if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
    setCreateCoffeeImageFile(file);
    setCreateCoffeeImagePreviewUrl(previewUrl);
  }, [createCoffeeImagePreviewUrl]);

  const onRemoveImage = useCallback(() => {
    if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
    setCreateCoffeeImageFile(null);
    setCreateCoffeeImagePreviewUrl("");
  }, [createCoffeeImagePreviewUrl]);

  useEffect(() => {
    return () => {
      if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
    };
  }, [createCoffeeImagePreviewUrl]);

  const resetCreateCoffeeDomain = useCallback(() => {
    setShowCreateCoffeeComposer(false);
    setCreateCoffeeSaving(false);
    setCreateCoffeeError(null);
    setCreateCoffeeDraft(initialDraft);
    if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
    setCreateCoffeeImageFile(null);
    setCreateCoffeeImagePreviewUrl("");
  }, [createCoffeeImagePreviewUrl]);

  return useMemo(() => ({
    showCreateCoffeeComposer,
    setShowCreateCoffeeComposer,
    createCoffeeSaving,
    createCoffeeError,
    createCoffeeDraft,
    setCreateCoffeeDraft,
    createCoffeeImagePreviewUrl,
    openCreateCoffeeComposer,
    closeCreateCoffeeComposer,
    saveCreateCoffee,
    onPickImage,
    onRemoveImage,
    resetCreateCoffeeDomain
  }), [
    closeCreateCoffeeComposer,
    createCoffeeDraft,
    createCoffeeError,
    createCoffeeImagePreviewUrl,
    createCoffeeSaving,
    openCreateCoffeeComposer,
    onPickImage,
    onRemoveImage,
    resetCreateCoffeeDomain,
    saveCreateCoffee,
    showCreateCoffeeComposer
  ]);
}
