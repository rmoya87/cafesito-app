import { useEffect } from "react";
import { getSupabaseClient } from "../../supabase";
import { fetchUserData } from "../../data/supabaseApi";
import type { UserListItemActivityRow } from "../../data/supabaseApi";
import type {
  CoffeeRow,
  DiaryEntryRow,
  FavoriteRow,
  FinishedCoffeeRow,
  NotificationRow,
  PantryItemRow,
  UserListRow,
  UserRow
} from "../../types";

type Params = {
  activeUser: UserRow | null;
  setDiaryEntries: (value: DiaryEntryRow[]) => void;
  setPantryItems: (value: PantryItemRow[]) => void;
  setFavorites: (value: FavoriteRow[]) => void;
  setUserLists: (value: UserListRow[]) => void;
  setCoffeeIdsInUserLists: (value: string[]) => void;
  setAllListItemsForActivity: (value: UserListItemActivityRow[]) => void;
  setCustomCoffees: (value: CoffeeRow[]) => void;
  setFinishedCoffees: (value: FinishedCoffeeRow[]) => void;
  setNotifications: (value: NotificationRow[]) => void;
  setGlobalStatus: (value: string) => void;
};

export function useUserDataLoader({
  activeUser,
  setDiaryEntries,
  setPantryItems,
  setFavorites,
  setUserLists,
  setCoffeeIdsInUserLists,
  setAllListItemsForActivity,
  setCustomCoffees,
  setFinishedCoffees,
  setNotifications,
  setGlobalStatus
}: Params): void {
  useEffect(() => {
    if (!activeUser) return;
    let cancelled = false;

    const loadInitialData = async () => {
      try {
        const data = await fetchUserData(activeUser.id);
        const { fetchNotifications, fetchUserLists, fetchCoffeeIdsInUserLists, fetchAllListItemsForActivity } = await import("../../data/supabaseApi");
        const [notifications, userLists, coffeeIdsInLists, listItemsForActivity] = await Promise.all([
          fetchNotifications(activeUser.id),
          fetchUserLists(activeUser.id),
          fetchCoffeeIdsInUserLists(activeUser.id),
          fetchAllListItemsForActivity(activeUser.id)
        ]);
        if (cancelled) return;

        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
        setUserLists(userLists);
        setCoffeeIdsInUserLists(coffeeIdsInLists);
        setAllListItemsForActivity(listItemsForActivity);
        setCustomCoffees(data.customCoffees);
        setFinishedCoffees(data.finishedCoffees);
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

    // Realtime: suscripción a cambios en notifications_db para este usuario (evita polling cada 5s)
    const supabase = getSupabaseClient();
    const channel = supabase
      .channel(`notifications:${activeUser.id}`)
      .on(
        "postgres_changes",
        {
          event: "*",
          schema: "public",
          table: "notifications_db",
          filter: `user_id=eq.${activeUser.id}`
        },
        () => {
          if (!cancelled) void refreshNotifications();
        }
      )
      .subscribe();

    const handleFocus = () => {
      void refreshNotifications();
    };
    window.addEventListener("focus", handleFocus);

    return () => {
      cancelled = true;
      void channel.unsubscribe();
      window.removeEventListener("focus", handleFocus);
    };
  }, [
    activeUser,
    setCustomCoffees,
    setDiaryEntries,
    setFinishedCoffees,
    setFavorites,
    setGlobalStatus,
    setNotifications,
    setPantryItems,
    setCoffeeIdsInUserLists,
    setAllListItemsForActivity,
    setUserLists
  ]);
}
