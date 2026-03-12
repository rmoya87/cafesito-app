import { useCallback } from "react";
import {
  deleteCoffeeReview,
  toggleFavoriteCoffee,
  upsertCoffeeReview,
  upsertCoffeeSensoryProfile,
  insertPantryItem,
  updatePantryItem,
  uploadImageFile
} from "../../data/supabaseApi";
import type { CoffeeReviewRow, CoffeeSensoryProfileRow, FavoriteRow, PantryItemRow, UserRow } from "../../types";

export function useCoffeeDetailActions({
  activeUser,
  detailCoffeeId,
  detailPantryStock,
  detailIsFavorite,
  detailCurrentUserReviewImageUrl,
  detailReviewImageFile,
  detailReviewRating,
  detailReviewText,
  detailSensoryDraft,
  detailStockDraft,
  setFavorites,
  setCoffeeReviews,
  setCoffeeSensoryProfiles,
  setPantryItems,
  setDetailStockDraft
}: {
  activeUser: UserRow | null;
  detailCoffeeId: string | null;
  detailPantryStock: PantryItemRow | null;
  detailIsFavorite: boolean;
  detailCurrentUserReviewImageUrl: string;
  detailReviewImageFile: File | null;
  detailReviewRating: number;
  detailReviewText: string;
  detailSensoryDraft: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
  detailStockDraft: { total: number; remaining: number };
  setFavorites: React.Dispatch<React.SetStateAction<FavoriteRow[]>>;
  setCoffeeReviews: React.Dispatch<React.SetStateAction<CoffeeReviewRow[]>>;
  setCoffeeSensoryProfiles: React.Dispatch<React.SetStateAction<CoffeeSensoryProfileRow[]>>;
  setPantryItems: React.Dispatch<React.SetStateAction<PantryItemRow[]>>;
  setDetailStockDraft: (value: { total: number; remaining: number }) => void;
}) {
  const saveDetailFavorite = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const next = !detailIsFavorite;
    const result = await toggleFavoriteCoffee(activeUser.id, detailCoffeeId, detailIsFavorite);
    setFavorites((prev) => {
      if (next && result) return [result, ...prev.filter((item) => !(item.user_id === result.user_id && item.coffee_id === result.coffee_id))];
      return prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === detailCoffeeId));
    });
  }, [activeUser, detailCoffeeId, detailIsFavorite, setFavorites]);

  const saveDetailReview = useCallback(async () => {
    if (!activeUser || !detailCoffeeId || detailReviewRating <= 0) return;
    let imageUrl = detailCurrentUserReviewImageUrl;
    if (detailReviewImageFile) {
      imageUrl = await uploadImageFile("reviews", detailReviewImageFile);
    }
    const row = await upsertCoffeeReview({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      rating: detailReviewRating,
      comment: detailReviewText.trim(),
      imageUrl
    });
    setCoffeeReviews((prev) => [row, ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))]);
  }, [
    activeUser,
    detailCoffeeId,
    detailCurrentUserReviewImageUrl,
    detailReviewImageFile,
    detailReviewRating,
    detailReviewText,
    setCoffeeReviews
  ]);

  const removeDetailReview = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    await deleteCoffeeReview(detailCoffeeId, activeUser.id);
    setCoffeeReviews((prev) => prev.filter((item) => !(item.coffee_id === detailCoffeeId && item.user_id === activeUser.id)));
  }, [activeUser, detailCoffeeId, setCoffeeReviews]);

  const saveDetailSensory = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const row = await upsertCoffeeSensoryProfile({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      aroma: detailSensoryDraft.aroma,
      sabor: detailSensoryDraft.sabor,
      cuerpo: detailSensoryDraft.cuerpo,
      acidez: detailSensoryDraft.acidez,
      dulzura: detailSensoryDraft.dulzura
    });
    setCoffeeSensoryProfiles((prev) => [row, ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))]);
  }, [activeUser, detailCoffeeId, detailSensoryDraft, setCoffeeSensoryProfiles]);

  const saveDetailStock = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const total = Math.max(0, Number.isFinite(detailStockDraft.total) ? detailStockDraft.total : 0);
    const remaining = Math.min(total, Math.max(0, Number.isFinite(detailStockDraft.remaining) ? detailStockDraft.remaining : 0));
    if (detailPantryStock?.id) {
      const updated = await updatePantryItem(detailPantryStock.id, { totalGrams: total, gramsRemaining: remaining });
      setPantryItems((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
    } else {
      const row = await insertPantryItem({
        coffeeId: detailCoffeeId,
        userId: activeUser.id,
        totalGrams: total,
        gramsRemaining: remaining
      });
      setPantryItems((prev) => [row, ...prev]);
    }
    setDetailStockDraft({ total, remaining });
  }, [activeUser, detailCoffeeId, detailPantryStock, detailStockDraft, setDetailStockDraft, setPantryItems]);

  return {
    saveDetailFavorite,
    saveDetailReview,
    removeDetailReview,
    saveDetailSensory,
    saveDetailStock
  };
}

