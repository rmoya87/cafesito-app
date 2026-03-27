type Params = {
  newPostSelectedImageId: string | null;
  newPostGalleryItems: Array<{ id: string }>;
  setNewPostSelectedImageId: (value: string | null) => void;
  setNewPostImageFile: (value: File | null) => void;
  setNewPostImagePreviewUrl: (value: string | null) => void;
  setNewPostGalleryItems: (value: any[] | ((prev: any[]) => any[])) => void;
};

export function useTimelineSheetActions({
  newPostSelectedImageId,
  newPostGalleryItems,
  setNewPostSelectedImageId,
  setNewPostImageFile,
  setNewPostImagePreviewUrl,
  setNewPostGalleryItems
}: Params) {
  const removeSelectedCreatePostImage = () => {
    if (!newPostSelectedImageId) return;
    setNewPostGalleryItems(
      newPostGalleryItems.filter((item) => item.id !== newPostSelectedImageId)
    );
    setNewPostSelectedImageId(null);
    setNewPostImageFile(null);
    setNewPostImagePreviewUrl(null);
  };

  return { removeSelectedCreatePostImage };
}

