import { useEffect, useState } from "react";
import type { CoffeeReviewRow, PantryItemRow } from "../../types";

export function useCoffeeDetailDraftSync({
  hasDetailCoffee,
  currentUserReview,
  pantryStock,
  sensoryAverages,
  setDetailReviewText,
  setDetailReviewRating,
  setDetailReviewImagePreviewUrl,
  setDetailReviewImageFile,
  setDetailStockDraft,
  setDetailSensoryDraft
}: {
  hasDetailCoffee: boolean;
  currentUserReview: CoffeeReviewRow | null;
  pantryStock: PantryItemRow | null;
  sensoryAverages: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
  setDetailReviewText: (value: string) => void;
  setDetailReviewRating: (value: number) => void;
  setDetailReviewImagePreviewUrl: (value: string) => void;
  setDetailReviewImageFile: (value: File | null) => void;
  setDetailStockDraft: (value: { total: number; remaining: number }) => void;
  setDetailSensoryDraft: (value: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number }) => void;
}) {
  useEffect(() => {
    if (!hasDetailCoffee) return;
    if (currentUserReview) {
      setDetailReviewText(currentUserReview.comment ?? "");
      setDetailReviewRating(currentUserReview.rating);
      setDetailReviewImagePreviewUrl(currentUserReview.image_url ?? "");
    } else {
      setDetailReviewText("");
      setDetailReviewRating(0);
      setDetailReviewImagePreviewUrl("");
    }
    setDetailReviewImageFile(null);
    setDetailStockDraft({
      total: pantryStock?.total_grams ?? 0,
      remaining: pantryStock?.grams_remaining ?? 0
    });
    setDetailSensoryDraft({
      aroma: sensoryAverages.aroma,
      sabor: sensoryAverages.sabor,
      cuerpo: sensoryAverages.cuerpo,
      acidez: sensoryAverages.acidez,
      dulzura: sensoryAverages.dulzura
    });
  }, [currentUserReview, hasDetailCoffee, pantryStock, sensoryAverages]);

}

export function useCoffeeDetailDomain() {
  const [detailCoffeeId, setDetailCoffeeId] = useState<string | null>(null);
  const [detailHostTab, setDetailHostTab] = useState<"timeline" | "search" | "profile" | "diary" | null>(null);
  const [detailReviewText, setDetailReviewText] = useState("");
  const [detailReviewRating, setDetailReviewRating] = useState(0);
  const [detailReviewImageFile, setDetailReviewImageFile] = useState<File | null>(null);
  const [detailReviewImagePreviewUrl, setDetailReviewImagePreviewUrl] = useState("");
  const [detailSensoryDraft, setDetailSensoryDraft] = useState({ aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 });
  const [detailStockDraft, setDetailStockDraft] = useState({ total: 0, remaining: 0 });
  const [detailOpenStockSignal, setDetailOpenStockSignal] = useState(0);

  useEffect(() => {
    if (!detailReviewImagePreviewUrl.startsWith("blob:")) return;
    const blobUrl = detailReviewImagePreviewUrl;
    return () => {
      URL.revokeObjectURL(blobUrl);
    };
  }, [detailReviewImagePreviewUrl]);

  return {
    detailCoffeeId,
    setDetailCoffeeId,
    detailHostTab,
    setDetailHostTab,
    detailReviewText,
    setDetailReviewText,
    detailReviewRating,
    setDetailReviewRating,
    detailReviewImageFile,
    setDetailReviewImageFile,
    detailReviewImagePreviewUrl,
    setDetailReviewImagePreviewUrl,
    detailSensoryDraft,
    setDetailSensoryDraft,
    detailStockDraft,
    setDetailStockDraft,
    detailOpenStockSignal,
    setDetailOpenStockSignal
  };
}
