import { useCallback } from "react";
import { buildRoute, toCoffeeSlug } from "../../core/routing";
import type { CoffeeRow, TabId } from "../../types";

export function useCoffeeDetailNavigation({
  coffeesById,
  coffeeSlugById,
  mode,
  searchMode,
  profileUsername,
  activeTab,
  setDetailCoffeeId,
  setDetailHostTab,
  setActiveTab
}: {
  coffeesById: Map<string, CoffeeRow>;
  coffeeSlugById: Map<string, string>;
  mode: "mobile" | "desktop";
  searchMode: "users" | "coffees";
  profileUsername: string | null;
  activeTab: TabId;
  setDetailCoffeeId: (value: string | null) => void;
  setDetailHostTab: (value: "timeline" | "search" | "profile" | null) => void;
  setActiveTab: (value: TabId) => void;
}) {
  const openCoffeeDetail = useCallback(
    (coffeeId: string, sourceTab: "timeline" | "search" | "profile" | "diary") => {
      const coffee = coffeesById.get(coffeeId);
      if (!coffee) return;
      const slug = coffeeSlugById.get(coffeeId) ?? toCoffeeSlug(coffee.nombre, coffee.marca);
      setDetailCoffeeId(coffeeId);
      if (sourceTab === "diary") {
        setDetailHostTab(null);
        setActiveTab("coffee");
      } else if (mode === "desktop") {
        setDetailHostTab(sourceTab);
      } else {
        setDetailHostTab(null);
        setActiveTab("coffee");
      }
      const nextPath = buildRoute("coffee", searchMode, profileUsername, slug);
      if (window.location.pathname !== nextPath) {
        window.history.pushState({}, "", `${nextPath}${window.location.search}${window.location.hash}`);
      }
    },
    [coffeeSlugById, coffeesById, mode, profileUsername, searchMode, setActiveTab, setDetailCoffeeId, setDetailHostTab]
  );

  const closeCoffeePanel = useCallback(() => {
    setDetailCoffeeId(null);
    setDetailHostTab(null);
    if (mode === "desktop") {
      const backPath = buildRoute(activeTab, searchMode, profileUsername, null);
      if (window.location.pathname !== backPath) {
        window.history.replaceState({}, "", `${backPath}${window.location.search}${window.location.hash}`);
      }
    }
  }, [activeTab, mode, profileUsername, searchMode, setDetailCoffeeId, setDetailHostTab]);

  return { openCoffeeDetail, closeCoffeePanel };
}

