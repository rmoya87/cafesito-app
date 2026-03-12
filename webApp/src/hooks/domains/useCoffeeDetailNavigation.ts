import { useCallback, startTransition } from "react";
import { buildRoute, getAppRootPath, toCoffeeSlug } from "../../core/routing";
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
  setDetailHostTab: (value: "home" | "search" | "profile" | null) => void;
  setActiveTab: (value: TabId) => void;
}) {
  const openCoffeeDetail = useCallback(
    (coffeeId: string, sourceTab: "home" | "search" | "profile" | "diary") => {
      const coffee = coffeesById.get(coffeeId);
      if (!coffee) return;
      const slug = coffeeSlugById.get(coffeeId) ?? toCoffeeSlug(coffee.nombre, coffee.marca);
      const routePath = buildRoute("coffee", searchMode, profileUsername, slug);
      const base = (getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || "";
      const fullPath = base === "" || base === "/" ? routePath : `${base}${routePath}`;
      startTransition(() => {
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
        if (window.location.pathname !== fullPath) {
          window.history.pushState({}, "", `${fullPath}${window.location.search}${window.location.hash}`);
        }
      });
    },
    [coffeeSlugById, coffeesById, mode, profileUsername, searchMode, setActiveTab, setDetailCoffeeId, setDetailHostTab]
  );

  const closeCoffeePanel = useCallback(() => {
    setDetailCoffeeId(null);
    setDetailHostTab(null);
    if (mode === "desktop") {
      const routePath = buildRoute(activeTab, searchMode, profileUsername, null);
      const base = (getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || "";
      const fullPath = base === "" || base === "/" ? routePath : `${base}${routePath}`;
      if (window.location.pathname !== fullPath) {
        window.history.replaceState({}, "", `${fullPath}${window.location.search}${window.location.hash}`);
      }
    }
  }, [activeTab, mode, profileUsername, searchMode, setDetailCoffeeId, setDetailHostTab]);

  return { openCoffeeDetail, closeCoffeePanel };
}

