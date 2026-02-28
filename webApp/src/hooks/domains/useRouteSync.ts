import { useEffect } from "react";
import { buildRoute, getAppRootPath, parseRoute } from "../../core/routing";
import { canNavigateToTab } from "../../core/guards";

export function useRouteCanonicalSync(isAuthenticated?: boolean) {
  useEffect(() => {
    const pathname = window.location.pathname;
    const base = (getAppRootPath(pathname) || "/").replace(/\/+$/, "") || "/";
    if (!isAuthenticated) {
      const current = pathname.replace(/\/+$/, "") || "/";
      if (current !== base) {
        window.history.replaceState({}, "", `${base}${window.location.search}${window.location.hash}`);
      }
      return;
    }
    const route = parseRoute(pathname);
    const routePath = buildRoute(route.tab, route.searchMode, route.profileUsername, route.coffeeSlug);
    const normalized = base === "/" ? routePath : `${base}${routePath}`;
    if (pathname !== normalized) {
      window.history.replaceState({}, "", `${normalized}${window.location.search}${window.location.hash}`);
    }
  }, [isAuthenticated]);
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
    const pathname = window.location.pathname;
    const route = parseRoute(pathname);
    if (route.tab !== "coffee") return;
    if (!route.coffeeSlug) return;

    const coffeeId = coffeeSlugToId.get(route.coffeeSlug) ?? null;
    if (coffeeId) {
      setDetailCoffeeId(coffeeId);
      setDetailHostTab(null);
      return;
    }
    const base = (getAppRootPath(pathname) || "/").replace(/\/+$/, "") || "/";
    const searchPath = base === "/" ? "/search" : `${base}/search`;
    window.location.replace(`${searchPath}${window.location.search}${window.location.hash}`);
  }, [coffeeSlugToId, setDetailCoffeeId, setDetailHostTab]);
}
