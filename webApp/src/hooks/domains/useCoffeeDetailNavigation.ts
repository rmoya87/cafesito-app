import { useCallback, useRef, startTransition } from "react";
import { buildRoute, getAppRootPath, toCoffeeSlug } from "../../core/routing";
import type { CoffeeRow, TabId } from "../../types";

/** Al cerrar el detalle, si veníamos de "Cafés probados", volver a esa página. */
type ReturnDiarySubView = "cafes-probados" | null;

export function useCoffeeDetailNavigation({
  coffeesById,
  coffeeSlugById,
  mode,
  searchMode,
  profileUsername,
  activeTab,
  setDetailCoffeeId,
  setDetailHostTab,
  setActiveTab,
  setDiarySubView
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
  setDiarySubView: (value: "cafes-probados" | null) => void;
}) {
  const returnDiarySubViewRef = useRef<ReturnDiarySubView>(null);

  const openCoffeeDetail = useCallback(
    (
      coffeeId: string,
      sourceTab: "home" | "search" | "profile" | "diary",
      options?: { diarySubView?: "cafes-probados" }
    ) => {
      const coffee = coffeesById.get(coffeeId);
      if (!coffee) return;
      const slug = coffeeSlugById.get(coffeeId) ?? toCoffeeSlug(coffee.nombre, coffee.marca);
      const routePath = buildRoute("coffee", searchMode, profileUsername, slug);
      const base = (getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || "";
      const fullPath = base === "" || base === "/" ? routePath : `${base}${routePath}`;
      startTransition(() => {
        if (sourceTab === "diary" && options?.diarySubView === "cafes-probados") {
          returnDiarySubViewRef.current = "cafes-probados";
        } else {
          returnDiarySubViewRef.current = null;
        }
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
    const returnTo = returnDiarySubViewRef.current;
    returnDiarySubViewRef.current = null;
    setDetailCoffeeId(null);
    setDetailHostTab(null);

    if (returnTo === "cafes-probados") {
      setActiveTab("diary");
      setDiarySubView("cafes-probados");
      const routePath = buildRoute("diary", searchMode, null, null, undefined, undefined, "cafes-probados");
      const base = (getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || "";
      const fullPath = base === "" || base === "/" ? routePath : `${base}${routePath}`;
      if (window.location.pathname !== fullPath) {
        window.history.pushState({}, "", `${fullPath}${window.location.search}${window.location.hash}`);
      }
    } else if (mode === "desktop") {
      const routePath = buildRoute(activeTab, searchMode, profileUsername, null);
      const base = (getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || "";
      const fullPath = base === "" || base === "/" ? routePath : `${base}${routePath}`;
      if (window.location.pathname !== fullPath) {
        window.history.replaceState({}, "", `${fullPath}${window.location.search}${window.location.hash}`);
      }
    }
  }, [activeTab, mode, profileUsername, searchMode, setActiveTab, setDiarySubView, setDetailCoffeeId, setDetailHostTab]);

  return { openCoffeeDetail, closeCoffeePanel };
}

