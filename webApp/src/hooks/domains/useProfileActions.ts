import { useCallback } from "react";
import { updateUserProfile, uploadImageFile } from "../../data/supabaseApi";
import type { UserRow } from "../../types";

export function useProfileActions({
  setUsers,
  setTimelineBusyMessage,
  setTimelineActionBanner,
  setGlobalStatus
}: {
  setUsers: React.Dispatch<React.SetStateAction<UserRow[]>>;
  setTimelineBusyMessage: (value: string | null) => void;
  setTimelineActionBanner: (value: string | null) => void;
  setGlobalStatus: (value: string) => void;
}) {
  const handleUpdateProfile = useCallback(
    async (
      userId: number,
      fullName: string,
      bio: string,
      avatarFile?: File | null,
      removeAvatar?: boolean
    ) => {
      const trimmedName = fullName.trim();
      const normalizedBio = bio.trim();
      if (!trimmedName) return;
      setTimelineBusyMessage("Actualizando perfil...");
      try {
        const avatarUrl = removeAvatar ? null : avatarFile ? await uploadImageFile("avatars", avatarFile) : undefined;
        await updateUserProfile(userId, {
          full_name: trimmedName,
          bio: normalizedBio ? normalizedBio : null,
          avatar_url: avatarUrl
        });
        setUsers((prev) =>
          prev.map((row) =>
            row.id === userId
              ? {
                  ...row,
                  full_name: trimmedName,
                  bio: normalizedBio ? normalizedBio : null,
                  avatar_url: avatarUrl === undefined ? row.avatar_url : avatarUrl ?? ""
                }
              : row
          )
        );
        setTimelineActionBanner("Perfil actualizado");
      } catch (error) {
        setGlobalStatus(`Error actualizando perfil: ${(error as Error).message}`);
      } finally {
        setTimelineBusyMessage(null);
      }
    },
    [setGlobalStatus, setTimelineActionBanner, setTimelineBusyMessage, setUsers]
  );

  return { handleUpdateProfile };
}

