import { useEffect } from "react";
import { fetchUserData } from "../../data/supabaseApi";
import type { CoffeeRow, DiaryEntryRow, FavoriteRow, PantryItemRow, UserRow } from "../../types";

type Params = {
  activeUser: UserRow | null;
  setDiaryEntries: (value: DiaryEntryRow[]) => void;
  setPantryItems: (value: PantryItemRow[]) => void;
  setFavorites: (value: FavoriteRow[]) => void;
  setCustomCoffees: (value: CoffeeRow[]) => void;
  setGlobalStatus: (value: string) => void;
};

export function useUserDataLoader({
  activeUser,
  setDiaryEntries,
  setPantryItems,
  setFavorites,
  setCustomCoffees,
  setGlobalStatus
}: Params): void {
  useEffect(() => {
    if (!activeUser) return;
    (async () => {
      try {
        const data = await fetchUserData(activeUser.id);
        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
        setCustomCoffees(data.customCoffees);
      } catch (error) {
        setGlobalStatus(`Error: ${(error as Error).message}`);
      }
    })();
  }, [activeUser, setCustomCoffees, setDiaryEntries, setFavorites, setGlobalStatus, setPantryItems]);
}
