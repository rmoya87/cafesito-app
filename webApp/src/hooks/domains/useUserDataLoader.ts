import { useEffect } from "react";
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

/** Carga datos del usuario una sola vez al montar; no hay Realtime ni refetch al volver a la pestaña. Para actualizar, recargar la página. */
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

    const loadInitialData = async (retryCount = 0) => {
      try {
        const data = await fetchUserData(activeUser.id);
        const {
          fetchNotifications,
          fetchUserLists,
          fetchSharedWithMeLists,
          fetchCoffeeIdsInLists,
          fetchAllListItemsForActivity
        } = await import("../../data/supabaseApi");
        const [notifications, ownedLists, sharedLists, listItemsForActivity] = await Promise.all([
          fetchNotifications(activeUser.id),
          fetchUserLists(activeUser.id),
          fetchSharedWithMeLists(activeUser.id),
          fetchAllListItemsForActivity(activeUser.id)
        ]);
        if (cancelled) return;

        const mergedLists = (() => {
          const byId = new Map<string, (typeof ownedLists)[0]>();
          [...ownedLists, ...sharedLists].forEach((l) => byId.set(l.id, l));
          return Array.from(byId.values());
        })();
        const coffeeIdsInLists = await fetchCoffeeIdsInLists(mergedLists.map((l) => l.id));
        if (cancelled) return;

        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
        setUserLists(mergedLists);
        setCoffeeIdsInUserLists(coffeeIdsInLists);
        setAllListItemsForActivity(listItemsForActivity);
        setCustomCoffees(data.customCoffees);
        setFinishedCoffees(data.finishedCoffees);
        setNotifications(notifications);
      } catch (error) {
        if (cancelled) return;
        if (retryCount < 1) {
          await new Promise((r) => setTimeout(r, 2000));
          void loadInitialData(retryCount + 1);
          return;
        }
        setGlobalStatus(`Error: ${(error as Error).message}`);
      }
    };

    void loadInitialData();

    return () => {
      cancelled = true;
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
