import { useCallback, useEffect, useRef, useState } from "react";

type GalleryItem = { id: string; file: File; previewUrl: string };

export function useTimelineComposerDomain() {
  const [showCreatePost, setShowCreatePost] = useState(false);
  const [newPostText, setNewPostText] = useState("");
  const [newPostImageFile, setNewPostImageFile] = useState<File | null>(null);
  const [newPostImagePreviewUrl, setNewPostImagePreviewUrl] = useState("");
  const [newPostGalleryItems, setNewPostGalleryItems] = useState<GalleryItem[]>([]);
  const [newPostSelectedImageId, setNewPostSelectedImageId] = useState<string | null>(null);
  const [newPostCoffeeId, setNewPostCoffeeId] = useState<string>("");
  const [showCreatePostCoffeeSheet, setShowCreatePostCoffeeSheet] = useState(false);
  const [createPostCoffeeQuery, setCreatePostCoffeeQuery] = useState("");
  const [showCreatePostEmojiPanel, setShowCreatePostEmojiPanel] = useState(false);
  const newPostImageInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    if (!showCreatePost) return;
    setNewPostCoffeeId((prev) => prev || "");
  }, [showCreatePost]);

  useEffect(() => {
    if (!showCreatePost) return;
    const id = window.requestAnimationFrame(() => {
      document.getElementById("new-post-text")?.focus();
    });
    return () => window.cancelAnimationFrame(id);
  }, [showCreatePost]);

  useEffect(() => {
    if (showCreatePost) return;
    newPostGalleryItems.forEach((item) => {
      if (item.previewUrl.startsWith("blob:")) URL.revokeObjectURL(item.previewUrl);
    });
  }, [newPostGalleryItems, showCreatePost]);

  const resetCreatePostComposer = useCallback(() => {
    newPostGalleryItems.forEach((item) => {
      if (item.previewUrl.startsWith("blob:")) URL.revokeObjectURL(item.previewUrl);
    });
    setShowCreatePost(false);
    setNewPostText("");
    setNewPostImageFile(null);
    setNewPostImagePreviewUrl("");
    setNewPostGalleryItems([]);
    setNewPostSelectedImageId(null);
    setNewPostCoffeeId("");
    setCreatePostCoffeeQuery("");
    setShowCreatePostCoffeeSheet(false);
    setShowCreatePostEmojiPanel(false);
  }, [newPostGalleryItems]);

  const openCreatePostComposer = useCallback(() => {
    setShowCreatePost(true);
  }, []);

  const appendNewPostFiles = useCallback((files: File[]) => {
    if (!files.length) return;
    const added = files.map((file, index) => ({
      id: `${Date.now()}-${index}-${Math.random().toString(36).slice(2, 8)}`,
      file,
      previewUrl: URL.createObjectURL(file)
    }));
    setNewPostGalleryItems((prev) => [...added, ...prev].slice(0, 24));
    const first = added[0];
    if (first) {
      setNewPostSelectedImageId(first.id);
      setNewPostImageFile(first.file);
      setNewPostImagePreviewUrl(first.previewUrl);
    }
  }, []);

  return {
    showCreatePost,
    setShowCreatePost,
    newPostText,
    setNewPostText,
    newPostImageFile,
    setNewPostImageFile,
    newPostImagePreviewUrl,
    setNewPostImagePreviewUrl,
    newPostGalleryItems,
    setNewPostGalleryItems,
    newPostSelectedImageId,
    setNewPostSelectedImageId,
    newPostCoffeeId,
    setNewPostCoffeeId,
    showCreatePostCoffeeSheet,
    setShowCreatePostCoffeeSheet,
    createPostCoffeeQuery,
    setCreatePostCoffeeQuery,
    showCreatePostEmojiPanel,
    setShowCreatePostEmojiPanel,
    newPostImageInputRef,
    resetCreatePostComposer,
    openCreatePostComposer,
    appendNewPostFiles
  };
}

