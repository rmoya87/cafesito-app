import { startTransition, useCallback, useEffect, useMemo, useState } from "react";
import { insertPantryItem, upsertCustomCoffee, uploadImageFile } from "../../data/supabaseApi";
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
  totalGrams: 250,
  descripcion: "",
  proceso: "",
  codigo_barras: "",
  molienda_recomendada: "",
  product_url: "",
  aroma: null as number | null,
  sabor: null as number | null,
  cuerpo: null as number | null,
  acidez: null as number | null,
  dulzura: null as number | null
};

export type CreateCoffeeDraft = typeof initialDraft;

type Draft = CreateCoffeeDraft;

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
    startTransition(() => {
      setShowCreateCoffeeComposer(true);
    });
  }, []);

  const saveCreateCoffee = useCallback(async (options?: { fromDiarySheet?: boolean; fromPantrySheet?: boolean; fromBrewChooser?: boolean }): Promise<{ id: string; name: string } | null> => {
    if (!activeUser) return null;
    if (!createCoffeeDraft.name.trim() || !createCoffeeDraft.brand.trim()) {
      setCreateCoffeeError("Nombre y marca son obligatorios.");
      return null;
    }
    if (!createCoffeeDraft.specialty.trim() || !createCoffeeDraft.country.trim() || !createCoffeeDraft.format.trim()) {
      setCreateCoffeeError("Completa especialidad, país y formato.");
      return null;
    }
    if (!(createCoffeeDraft.totalGrams > 0)) {
      setCreateCoffeeError("Indica la cantidad (tamaño de la bolsa en gramos) para darlo de alta en la despensa.");
      return null;
    }
    if (createCoffeeDraft.codigo_barras.trim()) {
      const raw = createCoffeeDraft.codigo_barras.replace(/\s+/g, "");
      if (!/^[0-9]{6,}$/.test(raw)) {
        setCreateCoffeeError("Revisa el código de barras (solo números, al menos 6 dígitos).");
        return null;
      }
    }
    if (createCoffeeDraft.product_url.trim()) {
      const url = createCoffeeDraft.product_url.trim();
      if (!/^https?:\/\/.+/i.test(url)) {
        setCreateCoffeeError("Revisa el enlace del producto (debe empezar por http:// o https://).");
        return null;
      }
    }
    const fromDiarySheet = options?.fromDiarySheet === true;
    const fromPantrySheet = options?.fromPantrySheet === true;
    const fromBrewChooser = options?.fromBrewChooser === true;
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
        totalGrams: createCoffeeDraft.totalGrams,
        descripcion: createCoffeeDraft.descripcion?.trim() || null,
        proceso: createCoffeeDraft.proceso?.trim() || null,
        codigo_barras: createCoffeeDraft.codigo_barras?.trim() || null,
        molienda_recomendada: createCoffeeDraft.molienda_recomendada?.trim() || null,
        product_url: createCoffeeDraft.product_url?.trim() || null,
        aroma: (createCoffeeDraft.aroma != null && createCoffeeDraft.aroma > 0) ? createCoffeeDraft.aroma : null,
        sabor: (createCoffeeDraft.sabor != null && createCoffeeDraft.sabor > 0) ? createCoffeeDraft.sabor : null,
        cuerpo: (createCoffeeDraft.cuerpo != null && createCoffeeDraft.cuerpo > 0) ? createCoffeeDraft.cuerpo : null,
        acidez: (createCoffeeDraft.acidez != null && createCoffeeDraft.acidez > 0) ? createCoffeeDraft.acidez : null,
        dulzura: (createCoffeeDraft.dulzura != null && createCoffeeDraft.dulzura > 0) ? createCoffeeDraft.dulzura : null
      });

      if (!fromPantrySheet) {
        const pantryRow = await insertPantryItem({
          coffeeId: created.id,
          userId: activeUser.id,
          totalGrams: Math.max(1, createCoffeeDraft.totalGrams || 250),
          gramsRemaining: Math.max(1, createCoffeeDraft.totalGrams || 250)
        });
        setPantryItems((prev) => [pantryRow, ...prev]);
      }

      setCustomCoffees((prev) => [created, ...prev.filter((item) => item.id !== created.id)]);
      if (!fromDiarySheet && !fromPantrySheet) {
        setShowCreateCoffeeComposer(false);
        if (fromBrewChooser) {
          setBrewCoffeeId(created.id);
          setBrewStep("method");
        } else {
          setBrewCoffeeId(created.id);
          setBrewStep("method");
        }
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
