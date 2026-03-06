import { useCallback, useEffect } from "react";
import { buildRoute, getAppRootPath, parseRoute, toCoffeeSlug } from "../../core/routing";
import { canNavigateToTab } from "../../core/guards";
import { normalizeLookupText } from "../../core/text";
import type { CoffeeRow, TabId, UserRow } from "../../types";

export function useAppNavigationDomain({
  searchMode,
  setSearchMode,
  profileUsername,
  setProfileUsername,
  users,
  setActiveTab,
  activeUserUsername,
  coffees,
  setDetailCoffeeId,
  setDetailHostTab,
  setShowCreateCoffeeComposer,
  setSearchSelectedCoffeeId,
  setSearchFocusCoffeeProfile,
  isAuthenticated,
  onRequireAuth
}: {
  searchMode: "coffees" | "users";
  setSearchMode: (value: "coffees" | "users") => void;
  profileUsername: string | null;
  setProfileUsername: (value: string | null) => void;
  users: UserRow[];
  setActiveTab: (value: TabId) => void;
  activeUserUsername: string | null;
  coffees: CoffeeRow[];
  setDetailCoffeeId: (value: string | null) => void;
  setDetailHostTab: (value: "timeline" | "search" | "profile" | "diary" | null) => void;
  setShowCreateCoffeeComposer: (value: boolean) => void;
  setSearchSelectedCoffeeId: (value: string | null) => void;
  setSearchFocusCoffeeProfile: (value: boolean) => void;
  isAuthenticated: boolean;
  onRequireAuth: () => void;
}) {
  const navigateToTab = useCallback(
    (
      tab: TabId,
      options?: {
        searchMode?: "coffees" | "users";
        profileUserId?: number | null;
        profileUsername?: string | null;
        coffeeSlug?: string | null;
        replace?: boolean;
      }
    ) => {
      const nextSearchMode = options?.searchMode ?? searchMode;
      if (!canNavigateToTab(tab, isAuthenticated, tab === "search" ? nextSearchMode : undefined)) {
        onRequireAuth();
        return;
      }
      const userById = options?.profileUserId != null ? users.find((item) => item.id === options.profileUserId) ?? null : null;
      const nextProfileUsername = options?.profileUsername ?? userById?.username ?? (tab === "profile" ? profileUsername : null);

      setActiveTab(tab);
      if (tab === "search") setSearchMode(nextSearchMode);
      if (tab === "profile") setProfileUsername(nextProfileUsername ?? null);

      const routePath = buildRoute(tab, nextSearchMode, nextProfileUsername ?? null, options?.coffeeSlug ?? null);
      const base = (getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || "";
      const fullPath = base === "" || base === "/" ? routePath : `${base}${routePath}`;
      if (window.location.pathname === fullPath) return;
      const method = options?.replace ? "replaceState" : "pushState";
      window.history[method]({}, "", `${fullPath}${window.location.search}${window.location.hash}`);
    },
    [isAuthenticated, onRequireAuth, profileUsername, searchMode, setActiveTab, setProfileUsername, setSearchMode, users]
  );

  useEffect(() => {
    const onPopState = () => {
      const route = parseRoute(window.location.pathname);
      const guardedTab = canNavigateToTab(route.tab, isAuthenticated, route.tab === "search" ? route.searchMode : undefined) ? route.tab : "timeline";
      setActiveTab(guardedTab);
      setSearchMode(route.searchMode);
      setProfileUsername(route.profileUsername);
      if (guardedTab === "coffee") {
        setDetailHostTab(null);
        if (!route.coffeeSlug) {
          setDetailCoffeeId(null);
        } else {
          const counts = new Map<string, number>();
          const nameCounts = new Map<string, number>();
          const sorted = [...coffees].sort((a, b) => {
            const nameCmp = a.nombre.localeCompare(b.nombre);
            if (nameCmp !== 0) return nameCmp;
            return a.id.localeCompare(b.id);
          });
          sorted.forEach((item) => {
            const key = normalizeLookupText(item.nombre);
            nameCounts.set(key, (nameCounts.get(key) ?? 0) + 1);
          });
          let found: string | null = null;
          for (const item of sorted) {
            const hasDuplicatedName = (nameCounts.get(normalizeLookupText(item.nombre)) ?? 0) > 1;
            const base = toCoffeeSlug(item.nombre, item.marca, hasDuplicatedName);
            const count = (counts.get(base) ?? 0) + 1;
            counts.set(base, count);
            const slug = count > 1 ? `${base}-${count}` : base;
            if (slug === route.coffeeSlug) {
              found = item.id;
              break;
            }
          }
          setDetailCoffeeId(found);
        }
      } else {
        setDetailCoffeeId(null);
        setDetailHostTab(null);
      }
    };
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, [coffees, isAuthenticated, setActiveTab, setDetailCoffeeId, setDetailHostTab, setProfileUsername, setSearchMode]);

  const handleNavClick = useCallback(
    (tabId: TabId) => {
      setDetailCoffeeId(null);
      setDetailHostTab(null);
      setShowCreateCoffeeComposer(false);
      if (tabId === "search") {
        setSearchSelectedCoffeeId(null);
        setSearchFocusCoffeeProfile(false);
        navigateToTab("search", { searchMode: "coffees" });
        return;
      }
      if (tabId === "profile") {
        navigateToTab("profile", { profileUsername: activeUserUsername ?? null });
        return;
      }
      navigateToTab(tabId);
    },
    [
      activeUserUsername,
      navigateToTab,
      setDetailCoffeeId,
      setDetailHostTab,
      setSearchFocusCoffeeProfile,
      setSearchSelectedCoffeeId,
      setShowCreateCoffeeComposer
    ]
  );

  return { navigateToTab, handleNavClick };
}
