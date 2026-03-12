import { useCallback } from "react";

type GalleryItem = {
  id: string;
  file: File;
  previewUrl: string;
};

export function useTimelineSheetActions(params: {
  newPostSelectedImageId: string | null;
  newPostGalleryItems: GalleryItem[];
  setNewPostSelectedImageId: (value: string | null) => void;
  setNewPostImageFile: (value: File | null) => void;
  setNewPostImagePreviewUrl: (value: string) => void;
  setNewPostGalleryItems: (updater: (prev: GalleryItem[]) => GalleryItem[]) => void;
}) {
  const {
    newPostSelectedImageId,
    newPostGalleryItems,
    setNewPostSelectedImageId,
    setNewPostImageFile,
    setNewPostImagePreviewUrl,
    setNewPostGalleryItems
  } = params;

  const selectCreatePostGalleryItem = useCallback(
    (itemId: string) => {
      const item = newPostGalleryItems.find((entry) => entry.id === itemId);
      if (!item) return;
      setNewPostSelectedImageId(item.id);
      setNewPostImageFile(item.file);
      setNewPostImagePreviewUrl(item.previewUrl);
    },
    [newPostGalleryItems, setNewPostImageFile, setNewPostImagePreviewUrl, setNewPostSelectedImageId]
  );

  const removeSelectedCreatePostImage = useCallback(() => {
    if (!newPostSelectedImageId) return;
    setNewPostGalleryItems((prev) => {
      const target = prev.find((item) => item.id === newPostSelectedImageId);
      if (target?.previewUrl.startsWith("blob:")) URL.revokeObjectURL(target.previewUrl);
      const next = prev.filter((item) => item.id !== newPostSelectedImageId);
      const replacement = next[0] ?? null;
      setNewPostSelectedImageId(replacement?.id ?? null);
      setNewPostImageFile(replacement?.file ?? null);
      setNewPostImagePreviewUrl(replacement?.previewUrl ?? "");
      return next;
    });
  }, [newPostSelectedImageId, setNewPostGalleryItems, setNewPostImageFile, setNewPostImagePreviewUrl, setNewPostSelectedImageId]);

  return {
    selectCreatePostGalleryItem,
    removeSelectedCreatePostImage
  };
}

