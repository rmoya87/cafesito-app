import { useCallback, useEffect } from "react";
import { fetchInitialData } from "../../data/supabaseApi";
import type {
  CoffeeReviewRow,
  CoffeeRow,
  FollowRow,
  UserRow
} from "../../types";

type Params = {
  authReady: boolean;
  setUsers: (value: UserRow[]) => void;
  setCoffees: (value: CoffeeRow[]) => void;
  setCoffeeReviews: (value: CoffeeReviewRow[]) => void;
  setCoffeeSensoryProfiles: (value: Array<{
    coffee_id: string;
    user_id: number;
    aroma: number;
    sabor: number;
    cuerpo: number;
    acidez: number;
    dulzura: number;
    updated_at: number;
  }>) => void;
  setFollows: (value: FollowRow[]) => void;
  setBrewCoffeeId: (updater: (prev: string) => string) => void;
  setGlobalStatus: (value: string) => void;
};

export function useInitialDataLoader(params: Params): () => Promise<void> {
  const {
    authReady,
    setUsers,
    setCoffees,
    setCoffeeReviews,
    setCoffeeSensoryProfiles,
    setFollows,
    setBrewCoffeeId,
    setGlobalStatus
  } = params;

  const loadInitialData = useCallback(async (forceRefresh = false) => {
    try {
      const data = await fetchInitialData(forceRefresh);
      setUsers(data.users);
      setCoffees(data.coffees);
      setCoffeeReviews(data.reviews);
      setCoffeeSensoryProfiles(data.sensoryProfiles);
      setFollows(data.follows);
      setGlobalStatus("Listo");
    } catch (error) {
      setGlobalStatus(`Error: ${(error as Error).message}`);
    }
  }, [
    setUsers,
    setCoffees,
    setCoffeeReviews,
    setCoffeeSensoryProfiles,
    setFollows,
    setBrewCoffeeId,
    setGlobalStatus
  ]);

  useEffect(() => {
    if (!authReady) return;
    void loadInitialData();
    // Sin refresh automático: los datos solo se cargan al montar (recarga de página).
  }, [authReady, loadInitialData]);

  return loadInitialData;
}
