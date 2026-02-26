import { useEffect } from "react";
import { buildRoute, parseRoute } from "../../core/routing";
import { canNavigateToTab } from "../../core/guards";

export function useRouteCanonicalSync() {
  useEffect(() => {
    const route = parseRoute(window.location.pathname);
    const normalized = buildRoute(route.tab, route.searchMode, route.profileUsername, route.coffeeSlug);
    if (window.location.pathname !== normalized) {
      window.history.replaceState({}, "", `${normalized}${window.location.search}${window.location.hash}`);
    }
  }, []);
}

export function useRouteGuardSync({
  isAuthenticated,
  onBlocked,
  fallbackPath = "/timeline"
}: {
  isAuthenticated: boolean;
  onBlocked: () => void;
  fallbackPath?: string;
}) {
  useEffect(() => {
    const route = parseRoute(window.location.pathname);
    if (canNavigateToTab(route.tab, isAuthenticated)) return;
    if (window.location.pathname !== fallbackPath) {
      window.history.replaceState({}, "", `${fallbackPath}${window.location.search}${window.location.hash}`);
    }
    onBlocked();
  }, [fallbackPath, isAuthenticated, onBlocked]);
}

export function useCoffeeRouteSync({
  coffeeSlugToId,
  setDetailCoffeeId,
  setDetailHostTab
}: {
  coffeeSlugToId: Map<string, string>;
  setDetailCoffeeId: (value: string | null) => void;
  setDetailHostTab: (value: "timeline" | "search" | "profile" | null) => void;
}) {
  useEffect(() => {
    const route = parseRoute(window.location.pathname);
    if (route.tab !== "coffee") return;
    if (!route.coffeeSlug) return;

    const coffeeId = coffeeSlugToId.get(route.coffeeSlug) ?? null;
    if (coffeeId) {
      setDetailCoffeeId(coffeeId);
      setDetailHostTab(null);
    }
  }, [coffeeSlugToId, setDetailCoffeeId, setDetailHostTab]);
}
