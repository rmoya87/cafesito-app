import { useCallback, useEffect } from "react";
import { buildRoute, parseRoute, toCoffeeSlug } from "../../core/routing";
import { canNavigateToTab } from "../../core/guards";
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
      if (!canNavigateToTab(tab, isAuthenticated)) {
        onRequireAuth();
        return;
      }
      const nextSearchMode = options?.searchMode ?? searchMode;
      const userById = options?.profileUserId != null ? users.find((item) => item.id === options.profileUserId) ?? null : null;
      const nextProfileUsername = options?.profileUsername ?? userById?.username ?? (tab === "profile" ? profileUsername : null);

      setActiveTab(tab);
      if (tab === "search") setSearchMode(nextSearchMode);
      if (tab === "profile") setProfileUsername(nextProfileUsername ?? null);

      const nextPath = buildRoute(tab, nextSearchMode, nextProfileUsername ?? null, options?.coffeeSlug ?? null);
      if (window.location.pathname === nextPath) return;
      const method = options?.replace ? "replaceState" : "pushState";
      window.history[method]({}, "", `${nextPath}${window.location.search}${window.location.hash}`);
    },
    [isAuthenticated, onRequireAuth, profileUsername, searchMode, setActiveTab, setProfileUsername, setSearchMode, users]
  );

  useEffect(() => {
    const onPopState = () => {
      const route = parseRoute(window.location.pathname);
      const guardedTab = canNavigateToTab(route.tab, isAuthenticated) ? route.tab : "timeline";
      setActiveTab(guardedTab);
      setSearchMode(route.searchMode);
      setProfileUsername(route.profileUsername);
      if (guardedTab === "coffee") {
        setDetailHostTab(null);
        if (!route.coffeeSlug) {
          setDetailCoffeeId(null);
        } else {
          const counts = new Map<string, number>();
          const sorted = [...coffees].sort((a, b) => {
            const nameCmp = a.nombre.localeCompare(b.nombre);
            if (nameCmp !== 0) return nameCmp;
            return a.id.localeCompare(b.id);
          });
          let found: string | null = null;
          for (const item of sorted) {
            const base = toCoffeeSlug(item.nombre);
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
