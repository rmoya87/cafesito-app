import { useCallback, useEffect, useRef, useState } from "react";

type GalleryItem = { id: string; file: File; previewUrl: string };

export function useTimelineComposerDomain() {
  const [commentSheetPostId, setCommentSheetPostId] = useState<string | null>(null);
  const [commentDraft, setCommentDraft] = useState("");
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [commentMenuId, setCommentMenuId] = useState<number | null>(null);
  const [highlightedCommentId, setHighlightedCommentId] = useState<number | null>(null);
  const [showCommentEmojiPanel, setShowCommentEmojiPanel] = useState(false);
  const [commentImageFile, setCommentImageFile] = useState<File | null>(null);
  const [commentImageName, setCommentImageName] = useState("");
  const [commentImagePreviewError, setCommentImagePreviewError] = useState(false);
  const [commentImagePreviewUrl, setCommentImagePreviewUrl] = useState("");

  const [showCreatePost, setShowCreatePost] = useState(false);
  const [newPostStep, setNewPostStep] = useState<0 | 1>(0);
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
  const newPostCameraInputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setEditingCommentId(null);
    setCommentMenuId(null);
    setCommentDraft("");
    setShowCommentEmojiPanel(false);
  }, [commentSheetPostId]);

  useEffect(() => {
    if (!showCreatePost) return;
    setNewPostCoffeeId((prev) => prev || "");
  }, [showCreatePost]);

  useEffect(() => {
    if (!showCreatePost || newPostStep !== 1) return;
    const id = window.requestAnimationFrame(() => {
      document.getElementById("new-post-text")?.focus();
    });
    return () => window.cancelAnimationFrame(id);
  }, [newPostStep, showCreatePost]);

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
    setNewPostStep(0);
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
    setNewPostStep(0);
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
    commentSheetPostId,
    setCommentSheetPostId,
    commentDraft,
    setCommentDraft,
    editingCommentId,
    setEditingCommentId,
    commentMenuId,
    setCommentMenuId,
    highlightedCommentId,
    setHighlightedCommentId,
    showCommentEmojiPanel,
    setShowCommentEmojiPanel,
    commentImageFile,
    setCommentImageFile,
    commentImageName,
    setCommentImageName,
    commentImagePreviewError,
    setCommentImagePreviewError,
    commentImagePreviewUrl,
    setCommentImagePreviewUrl,
    showCreatePost,
    setShowCreatePost,
    newPostStep,
    setNewPostStep,
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
    newPostCameraInputRef,
    resetCreatePostComposer,
    openCreatePostComposer,
    appendNewPostFiles
  };
}

