import { useCallback } from "react";
import {
  addPostCoffeeTag,
  createComment,
  createPost,
  deleteComment,
  deletePost,
  toggleFollow,
  toggleLike,
  updateComment,
  updatePost,
  uploadImageFile
} from "../../data/supabaseApi";
import { normalizeLookupText } from "../../core/text";
import type { CommentRow, FollowRow, LikeRow, PostCoffeeTagRow, PostRow, TabId, UserRow, CoffeeRow } from "../../types";

export function useTimelineActions({
  activeUser,
  likes,
  follows,
  comments,
  coffees,
  newPostText,
  newPostImageFile,
  newPostCoffeeId,
  commentDraft,
  editingCommentId,
  commentImageFile,
  setComments,
  setLikes,
  setFollows,
  setPosts,
  setPostCoffeeTags,
  setTimelineBusyMessage,
  setTimelineActionBanner,
  setGlobalStatus,
  resetCreatePostComposer,
  setEditingCommentId,
  setCommentDraft,
  setCommentImageFile,
  setCommentImageName,
  setCommentImagePreviewError,
  setCommentImagePreviewUrl,
  setCommentMenuId,
  reloadInitialData,
  navigateToTab,
  setSearchQuery
}: {
  activeUser: UserRow | null;
  likes: LikeRow[];
  follows: FollowRow[];
  comments: CommentRow[];
  coffees: CoffeeRow[];
  newPostText: string;
  newPostImageFile: File | null;
  newPostCoffeeId: string;
  commentDraft: string;
  editingCommentId: number | null;
  commentImageFile: File | null;
  setComments: React.Dispatch<React.SetStateAction<CommentRow[]>>;
  setLikes: React.Dispatch<React.SetStateAction<LikeRow[]>>;
  setFollows: React.Dispatch<React.SetStateAction<FollowRow[]>>;
  setPosts: React.Dispatch<React.SetStateAction<PostRow[]>>;
  setPostCoffeeTags: React.Dispatch<React.SetStateAction<PostCoffeeTagRow[]>>;
  setTimelineBusyMessage: (value: string | null) => void;
  setTimelineActionBanner: (value: string | null) => void;
  setGlobalStatus: (value: string) => void;
  resetCreatePostComposer: () => void;
  setEditingCommentId: (value: number | null) => void;
  setCommentDraft: (value: string) => void;
  setCommentImageFile: (value: File | null) => void;
  setCommentImageName: (value: string) => void;
  setCommentImagePreviewError: (value: boolean) => void;
  setCommentImagePreviewUrl: (value: string) => void;
  setCommentMenuId: (value: number | null) => void;
  reloadInitialData: () => Promise<void>;
  navigateToTab: (tab: TabId, options?: { searchMode?: "coffees" | "users" }) => void;
  setSearchQuery: (value: string) => void;
}) {
  const handleToggleLike = useCallback(
    async (postId: string) => {
      if (!activeUser) return;
      const alreadyLiked = likes.some((like) => like.post_id === postId && like.user_id === activeUser.id);
      setTimelineBusyMessage(alreadyLiked ? "Actualizando me gusta..." : "Registrando me gusta...");
      try {
        const result = await toggleLike(postId, activeUser.id, alreadyLiked);
        if (result) {
          setLikes((prev) => [result, ...prev.filter((item) => !(item.post_id === result.post_id && item.user_id === result.user_id))]);
          setTimelineActionBanner("Te gusta esta publicación");
        } else {
          setLikes((prev) => prev.filter((item) => !(item.post_id === postId && item.user_id === activeUser.id)));
          setTimelineActionBanner("Quitaste tu me gusta");
        }
      } catch (error) {
        setGlobalStatus(`Error like: ${(error as Error).message}`);
      } finally {
        setTimelineBusyMessage(null);
      }
    },
    [activeUser, likes, setGlobalStatus, setLikes, setTimelineActionBanner, setTimelineBusyMessage]
  );

  const handleAddComment = useCallback(
    async (postId: string) => {
      if (!activeUser) return;
      const text = commentDraft.trim();
      if (!text) return;
      setTimelineBusyMessage("Enviando comentario...");
      try {
        const row = await createComment(postId, activeUser.id, text);
        setComments((prev) => [row, ...prev]);
        setCommentDraft("");
        setCommentImageFile(null);
        setCommentImageName("");
        setCommentImagePreviewError(false);
        setCommentImagePreviewUrl("");
        setTimelineActionBanner("Comentario enviado");
      } catch (error) {
        setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      } finally {
        setTimelineBusyMessage(null);
      }
    },
    [
      activeUser,
      commentDraft,
      commentImageFile,
      setCommentDraft,
      setCommentImageFile,
      setCommentImageName,
      setCommentImagePreviewError,
      setCommentImagePreviewUrl,
      setComments,
      setGlobalStatus,
      setTimelineActionBanner,
      setTimelineBusyMessage
    ]
  );

  const handleUpdateComment = useCallback(async () => {
    if (!editingCommentId) return;
    const text = commentDraft.trim();
    if (!text) return;
    setTimelineBusyMessage("Actualizando comentario...");
    try {
      await updateComment(editingCommentId, text);
      setComments((prev) => prev.map((entry) => (entry.id === editingCommentId ? { ...entry, text } : entry)));
      setEditingCommentId(null);
      setCommentDraft("");
      setCommentImageFile(null);
      setCommentImageName("");
      setCommentImagePreviewError(false);
      setCommentImagePreviewUrl("");
      setTimelineActionBanner("Comentario actualizado");
    } catch (error) {
      setGlobalStatus(`Error comentario: ${(error as Error).message}`);
    } finally {
      setTimelineBusyMessage(null);
    }
  }, [
    commentDraft,
    editingCommentId,
    setCommentDraft,
    setCommentImageFile,
    setCommentImageName,
    setCommentImagePreviewError,
    setCommentImagePreviewUrl,
    setComments,
    setEditingCommentId,
    setGlobalStatus,
    setTimelineActionBanner,
    setTimelineBusyMessage
  ]);

  const handleDeleteComment = useCallback(
    async (commentId: number) => {
      setTimelineBusyMessage("Eliminando comentario...");
      try {
        await deleteComment(commentId);
        setComments((prev) => prev.filter((entry) => entry.id !== commentId));
        setCommentMenuId(null);
        if (editingCommentId === commentId) {
          setEditingCommentId(null);
          setCommentDraft("");
        }
        setTimelineActionBanner("Comentario eliminado");
      } catch (error) {
        setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      } finally {
        setTimelineBusyMessage(null);
      }
    },
    [editingCommentId, setCommentDraft, setCommentMenuId, setComments, setEditingCommentId, setGlobalStatus, setTimelineActionBanner, setTimelineBusyMessage]
  );

  const handleToggleFollow = useCallback(
    async (targetUserId: number) => {
      if (!activeUser || targetUserId === activeUser.id) return;
      const alreadyFollowing = follows.some((follow) => follow.follower_id === activeUser.id && follow.followed_id === targetUserId);
      setTimelineBusyMessage(alreadyFollowing ? "Actualizando seguimiento..." : "Siguiendo usuario...");
      try {
        const payload = await toggleFollow(activeUser.id, targetUserId, alreadyFollowing);
        if (!payload) {
          setFollows((prev) => prev.filter((follow) => !(follow.follower_id === activeUser.id && follow.followed_id === targetUserId)));
          setTimelineActionBanner("Dejaste de seguir");
          return;
        }
        setFollows((prev) => [...prev, payload]);
        setTimelineActionBanner("Ahora sigues a este usuario");
      } catch (error) {
        setGlobalStatus(`Error follow: ${(error as Error).message}`);
      } finally {
        setTimelineBusyMessage(null);
      }
    },
    [activeUser, follows, setFollows, setGlobalStatus, setTimelineActionBanner, setTimelineBusyMessage]
  );

  const handleEditPost = useCallback(
    async (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => {
      setTimelineBusyMessage("Actualizando publicacion...");
      try {
        const imageToPersist = imageFile ? await uploadImageFile("posts", imageFile) : newImageUrl;
        await updatePost(postId, newText, imageToPersist);
        setPosts((prev) => prev.map((post) => (post.id === postId ? { ...post, comment: newText, image_url: imageToPersist } : post)));
        setTimelineActionBanner("Publicacion actualizada");
      } catch (error) {
        setGlobalStatus(`Error editar post: ${(error as Error).message}`);
      } finally {
        setTimelineBusyMessage(null);
      }
    },
    [setGlobalStatus, setPosts, setTimelineActionBanner, setTimelineBusyMessage]
  );

  const handleDeletePost = useCallback(
    async (postId: string) => {
      setTimelineBusyMessage("Eliminando publicacion...");
      try {
        await deletePost(postId);
        setPosts((prev) => prev.filter((post) => post.id !== postId));
        setLikes((prev) => prev.filter((like) => like.post_id !== postId));
        setComments((prev) => prev.filter((comment) => comment.post_id !== postId));
        setPostCoffeeTags((prev) => prev.filter((tag) => tag.post_id !== postId));
        setTimelineActionBanner("Publicacion eliminada");
      } catch (error) {
        setGlobalStatus(`Error borrar post: ${(error as Error).message}`);
      } finally {
        setTimelineBusyMessage(null);
      }
    },
    [setComments, setGlobalStatus, setLikes, setPostCoffeeTags, setPosts, setTimelineActionBanner, setTimelineBusyMessage]
  );

  const handleRefreshTimeline = useCallback(async () => {
    setTimelineBusyMessage("Actualizando timeline...");
    await reloadInitialData();
    setTimelineBusyMessage(null);
    setTimelineActionBanner("Timeline actualizado");
  }, [reloadInitialData, setTimelineActionBanner, setTimelineBusyMessage]);

  const handleCreatePost = useCallback(async () => {
    if (!activeUser) return;
    const text = newPostText.trim();
    if (!text && !newPostImageFile) return;

    setTimelineBusyMessage("Publicando tu contenido...");
    try {
      const imageUrl = newPostImageFile ? await uploadImageFile("posts", newPostImageFile) : "";
      const post = await createPost(activeUser.id, text, imageUrl);
      setPosts((prev) => [post, ...prev]);

      if (newPostCoffeeId) {
        const selectedCoffee = coffees.find((coffee) => coffee.id === newPostCoffeeId);
        if (selectedCoffee) {
          const tag: PostCoffeeTagRow = {
            post_id: post.id,
            coffee_id: selectedCoffee.id,
            coffee_name: selectedCoffee.nombre,
            coffee_brand: selectedCoffee.marca ?? ""
          };
          await addPostCoffeeTag(tag);
          setPostCoffeeTags((prev) => [tag, ...prev]);
        }
      }

      resetCreatePostComposer();
      setTimelineActionBanner("Publicacion creada");
    } catch (error) {
      setGlobalStatus(`Error publicar: ${(error as Error).message}`);
    } finally {
      setTimelineBusyMessage(null);
    }
  }, [
    activeUser,
    coffees,
    newPostCoffeeId,
    newPostImageFile,
    newPostText,
    resetCreatePostComposer,
    setGlobalStatus,
    setPostCoffeeTags,
    setPosts,
    setTimelineActionBanner,
    setTimelineBusyMessage
  ]);

  const handleMentionNavigation = useCallback(
    (username: string) => {
      if (!username) return;
      navigateToTab("search", { searchMode: "users" });
      setSearchQuery(username);
      window.requestAnimationFrame(() => {
        document.getElementById("quick-search")?.focus();
      });
    },
    [navigateToTab, setSearchQuery]
  );

  return {
    handleToggleLike,
    handleAddComment,
    handleUpdateComment,
    handleDeleteComment,
    handleToggleFollow,
    handleEditPost,
    handleDeletePost,
    handleRefreshTimeline,
    handleCreatePost,
    handleMentionNavigation
  };
}
