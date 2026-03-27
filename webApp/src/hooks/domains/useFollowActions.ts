import { useCallback, type Dispatch, type SetStateAction } from "react";
import { toggleFollow } from "../../data/supabaseApi";
import type { FollowRow, UserRow } from "../../types";

export function useFollowActions({
  activeUser,
  follows,
  setFollows,
  setBusyMessage,
  setActionBanner,
  setGlobalStatus
}: {
  activeUser: UserRow | null;
  follows: FollowRow[];
  setFollows: Dispatch<SetStateAction<FollowRow[]>>;
  setBusyMessage: (value: string | null) => void;
  setActionBanner: (value: string | null) => void;
  setGlobalStatus: (value: string) => void;
}) {
  const handleToggleFollow = useCallback(
    async (targetUserId: number) => {
      if (!activeUser || targetUserId === activeUser.id) return;
      const alreadyFollowing = follows.some((follow) => follow.follower_id === activeUser.id && follow.followed_id === targetUserId);
      setBusyMessage(alreadyFollowing ? "Actualizando seguimiento..." : "Siguiendo usuario...");
      try {
        const payload = await toggleFollow(activeUser.id, targetUserId, alreadyFollowing);
        if (!payload) {
          setFollows((prev) => prev.filter((follow) => !(follow.follower_id === activeUser.id && follow.followed_id === targetUserId)));
          setActionBanner("Dejaste de seguir");
          return;
        }
        setFollows((prev) => [...prev, payload]);
        setActionBanner("Ahora sigues a este usuario");
      } catch (error) {
        setGlobalStatus(`Error follow: ${(error as Error).message}`);
      } finally {
        setBusyMessage(null);
      }
    },
    [activeUser, follows, setFollows, setGlobalStatus, setActionBanner, setBusyMessage]
  );

  return { handleToggleFollow };
}
