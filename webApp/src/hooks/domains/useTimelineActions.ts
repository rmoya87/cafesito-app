import { useCallback } from "react";

type Params = {
  activeUser: { id: number } | null;
  likes: any[];
  follows: any[];
  comments: any[];
  coffees: any[];
  newPostText: string;
  newPostImageFile: File | null;
  newPostCoffeeId: string | null;
  setComments: (value: any[] | ((prev: any[]) => any[])) => void;
  setLikes: (value: any[] | ((prev: any[]) => any[])) => void;
  setFollows: (value: any[] | ((prev: any[]) => any[])) => void;
  setPosts: (value: any[] | ((prev: any[]) => any[])) => void;
  setPostCoffeeTags: (value: any[] | ((prev: any[]) => any[])) => void;
  setTimelineBusyMessage: (value: string | null) => void;
  setTimelineActionBanner: (value: string | null) => void;
  setGlobalStatus: (value: string) => void;
  resetCreatePostComposer: () => void;
  reloadInitialData: () => Promise<void> | void;
  navigateToTab: (...args: any[]) => void;
  setSearchQuery: (value: string) => void;
};

export function useTimelineActions({
  reloadInitialData
}: Params) {
  const handleToggleLike = useCallback((postId?: string) => {
    void postId;
    // No-op minimal stub for local/dev usage
  }, []);

  const handleToggleFollow = useCallback(async (targetUserId?: number) => {
    void targetUserId;
    // No-op minimal stub for local/dev usage
  }, []);

  const handleEditPost = useCallback((postId?: string, newText?: string, newImageUrl?: string, imageFile?: File | null) => {
    void postId;
    void newText;
    void newImageUrl;
    void imageFile;
    // No-op minimal stub for local/dev usage
  }, []);

  const handleDeletePost = useCallback((postId?: string) => {
    void postId;
    // No-op minimal stub for local/dev usage
  }, []);

  const handleRefreshHome = useCallback(async () => {
    await reloadInitialData();
  }, [reloadInitialData]);

  const handleCreatePost = useCallback(async () => {
    // No-op minimal stub for local/dev usage
  }, []);

  const handleMentionNavigation = useCallback((username?: string) => {
    void username;
    // No-op minimal stub for local/dev usage
  }, []);

  return {
    handleToggleLike,
    handleToggleFollow,
    handleEditPost,
    handleDeletePost,
    handleRefreshHome,
    handleCreatePost,
    handleMentionNavigation
  };
}

