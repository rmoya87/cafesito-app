import { useCallback } from "react";

type GalleryItem = {
  id: string;
  file: File;
  previewUrl: string;
};

export function useTimelineSheetActions(params: {
  commentImagePreviewUrl: string;
  setCommentSheetPostId: (value: string | null) => void;
  setCommentDraft: (value: string) => void;
  setCommentMenuId: (value: number | null) => void;
  setHighlightedCommentId: (value: number | null) => void;
  setCommentImageFile: (value: File | null) => void;
  setCommentImageName: (value: string) => void;
  setCommentImagePreviewError: (value: boolean) => void;
  setCommentImagePreviewUrl: (value: string) => void;
  newPostSelectedImageId: string | null;
  newPostGalleryItems: GalleryItem[];
  setNewPostSelectedImageId: (value: string | null) => void;
  setNewPostImageFile: (value: File | null) => void;
  setNewPostImagePreviewUrl: (value: string) => void;
  setNewPostGalleryItems: (updater: (prev: GalleryItem[]) => GalleryItem[]) => void;
}) {
  const {
    commentImagePreviewUrl,
    setCommentSheetPostId,
    setCommentDraft,
    setCommentMenuId,
    setHighlightedCommentId,
    setCommentImageFile,
    setCommentImageName,
    setCommentImagePreviewError,
    setCommentImagePreviewUrl,
    newPostSelectedImageId,
    newPostGalleryItems,
    setNewPostSelectedImageId,
    setNewPostImageFile,
    setNewPostImagePreviewUrl,
    setNewPostGalleryItems
  } = params;

  const closeCommentSheet = useCallback(() => {
    setCommentSheetPostId(null);
    setCommentDraft("");
    setCommentMenuId(null);
    setHighlightedCommentId(null);
    if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
    setCommentImageFile(null);
    setCommentImageName("");
    setCommentImagePreviewError(false);
    setCommentImagePreviewUrl("");
  }, [
    commentImagePreviewUrl,
    setCommentDraft,
    setCommentImageFile,
    setCommentImageName,
    setCommentImagePreviewError,
    setCommentImagePreviewUrl,
    setCommentMenuId,
    setCommentSheetPostId,
    setHighlightedCommentId
  ]);

  const pickCommentImage = useCallback(
    (file: File) => {
      if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
      const preview = URL.createObjectURL(file);
      setCommentImageFile(file);
      setCommentImageName(file.name);
      setCommentImagePreviewError(false);
      setCommentImagePreviewUrl(preview);
    },
    [commentImagePreviewUrl, setCommentImageFile, setCommentImageName, setCommentImagePreviewError, setCommentImagePreviewUrl]
  );

  const removeCommentImage = useCallback(() => {
    if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
    setCommentImageFile(null);
    setCommentImageName("");
    setCommentImagePreviewError(false);
    setCommentImagePreviewUrl("");
  }, [commentImagePreviewUrl, setCommentImageFile, setCommentImageName, setCommentImagePreviewError, setCommentImagePreviewUrl]);

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
    closeCommentSheet,
    pickCommentImage,
    removeCommentImage,
    selectCreatePostGalleryItem,
    removeSelectedCreatePostImage
  };
}

