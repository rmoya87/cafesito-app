import { useEffect } from "react";
import { fetchUserData } from "../../data/supabaseApi";
import type { CoffeeRow, DiaryEntryRow, FavoriteRow, NotificationRow, PantryItemRow, UserRow } from "../../types";

type Params = {
  activeUser: UserRow | null;
  setDiaryEntries: (value: DiaryEntryRow[]) => void;
  setPantryItems: (value: PantryItemRow[]) => void;
  setFavorites: (value: FavoriteRow[]) => void;
  setCustomCoffees: (value: CoffeeRow[]) => void;
  setNotifications: (value: NotificationRow[]) => void;
  setGlobalStatus: (value: string) => void;
};

export function useUserDataLoader({
  activeUser,
  setDiaryEntries,
  setPantryItems,
  setFavorites,
  setCustomCoffees,
  setNotifications,
  setGlobalStatus
}: Params): void {
  useEffect(() => {
    if (!activeUser) return;
    let cancelled = false;
    let notificationsInterval: number | null = null;

    const loadInitialData = async () => {
      try {
        const data = await fetchUserData(activeUser.id);
        const { fetchNotifications } = await import("../../data/supabaseApi");
        const notifications = await fetchNotifications(activeUser.id);
        if (cancelled) return;

        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
        setCustomCoffees(data.customCoffees);
        setNotifications(notifications);
      } catch (error) {
        if (cancelled) return;
        setGlobalStatus(`Error: ${(error as Error).message}`);
      }
    };

    const refreshNotifications = async () => {
      try {
        const { fetchNotifications } = await import("../../data/supabaseApi");
        const notifications = await fetchNotifications(activeUser.id);
        if (cancelled) return;
        setNotifications(notifications);
      } catch (error) {
        if (cancelled) return;
        console.warn("No se pudieron refrescar notificaciones:", error);
      }
    };

    void loadInitialData();
    notificationsInterval = window.setInterval(() => {
      void refreshNotifications();
    }, 5000);
    const handleFocus = () => {
      void refreshNotifications();
    };
    window.addEventListener("focus", handleFocus);

    return () => {
      cancelled = true;
      if (notificationsInterval != null) window.clearInterval(notificationsInterval);
      window.removeEventListener("focus", handleFocus);
    };
  }, [activeUser, setCustomCoffees, setDiaryEntries, setFavorites, setGlobalStatus, setNotifications, setPantryItems]);
}
