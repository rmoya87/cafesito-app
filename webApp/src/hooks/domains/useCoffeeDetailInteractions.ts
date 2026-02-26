import { useCallback } from "react";
import { canRunInteraction } from "../../core/guards";

export function useCoffeeDetailInteractions({
  isAuthenticated,
  requestLogin,
  runWithAuth,
  saveDetailFavorite,
  saveDetailReview,
  removeDetailReview,
  saveDetailSensory,
  saveDetailStock,
  navigateToProfile
}: {
  isAuthenticated: boolean;
  requestLogin: () => void;
  runWithAuth: <T>(fn: () => Promise<T>) => Promise<T | null>;
  saveDetailFavorite: () => Promise<void>;
  saveDetailReview: () => Promise<void>;
  removeDetailReview: () => Promise<void>;
  saveDetailSensory: () => Promise<void>;
  saveDetailStock: () => Promise<void>;
  navigateToProfile: (userId: number) => void;
}) {
  const requireAuth = useCallback(
    async (
      interaction: "toggle_favorite" | "save_review" | "save_sensory" | "save_stock" | "open_profile",
      fn: () => void | Promise<void>
    ): Promise<void> => {
      if (!canRunInteraction(interaction, isAuthenticated)) {
        requestLogin();
        return;
      }
      await fn();
    },
    [isAuthenticated, requestLogin]
  );

  const sidePanel = {
    onToggleFavorite: async () => {
      await requireAuth("toggle_favorite", async () => {
        await saveDetailFavorite();
      });
    },
    onSaveReview: async () => {
      await requireAuth("save_review", async () => {
        await saveDetailReview();
      });
    },
    onDeleteReview: async () => {
      await requireAuth("save_review", async () => {
        await removeDetailReview();
      });
    },
    onSaveSensory: async () => {
      await requireAuth("save_sensory", async () => {
        await saveDetailSensory();
      });
    },
    onSaveStock: async () => {
      await requireAuth("save_stock", async () => {
        await saveDetailStock();
      });
    },
    onOpenUserProfile: async (userId: number) => {
      await requireAuth("open_profile", () => {
        navigateToProfile(userId);
      });
    }
  };

  const fullPage = {
    onToggleFavorite: async () => {
      await runWithAuth(async () => {
        await saveDetailFavorite();
      });
    },
    onSaveReview: async () => {
      await runWithAuth(async () => {
        await saveDetailReview();
      });
    },
    onDeleteReview: async () => {
      await runWithAuth(async () => {
        await removeDetailReview();
      });
    },
    onSaveSensory: async () => {
      await runWithAuth(async () => {
        await saveDetailSensory();
      });
    },
    onSaveStock: async () => {
      await runWithAuth(async () => {
        await saveDetailStock();
      });
    },
    onOpenUserProfile: async (userId: number) => {
      await runWithAuth(async () => {
        navigateToProfile(userId);
      });
    }
  };

  return { sidePanel, fullPage };
}
