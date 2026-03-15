import { useEffect } from "react";
import { buildRoute, getAppRootPath, isKnownRoute, parseRoute } from "../../core/routing";
import { canNavigateToTab } from "../../core/guards";

export function useRouteCanonicalSync(isAuthenticated?: boolean) {
  useEffect(() => {
    const pathname = window.location.pathname;
    if (!isKnownRoute(pathname)) return;
    const base = (getAppRootPath(pathname) || "/").replace(/\/+$/, "") || "/";
    if (!isAuthenticated) {
      const route = parseRoute(pathname);
      if (canNavigateToTab(route.tab, false, route.tab === "search" ? route.searchMode : undefined)) {
        return;
      }
      const current = pathname.replace(/\/+$/, "") || "/";
      if (current !== base) {
        window.history.replaceState({}, "", `${base}${window.location.search}${window.location.hash}`);
      }
      return;
    }
    const route = parseRoute(pathname);
    const routePath = buildRoute(route.tab, route.searchMode, route.profileUsername, route.coffeeSlug, route.profileSection, (route as { profileListId?: string }).profileListId, undefined, (route as { listOptionsView?: boolean }).listOptionsView);
    const normalized = base === "/" ? routePath : `${base}${routePath}`;
    if (pathname !== normalized) {
      window.history.replaceState({}, "", `${normalized}${window.location.search}${window.location.hash}`);
    }
  }, [isAuthenticated]);
}

const RETURN_AFTER_LOGIN_KEY = "cafesito_return_after_login";

/** Devuelve la URL guardada para restaurar tras login (p. ej. enlace compartido de lista). */
export function getReturnAfterLoginPath(): string | null {
  try {
    return sessionStorage.getItem(RETURN_AFTER_LOGIN_KEY);
  } catch {
    return null;
  }
}

/** Borra la URL guardada tras usarla. */
export function clearReturnAfterLoginPath(): void {
  try {
    sessionStorage.removeItem(RETURN_AFTER_LOGIN_KEY);
  } catch {
    /* ignore */
  }
}

export function useRouteGuardSync({
  authReady,
  isAuthenticated,
  onBlocked,
  fallbackPath = "/home"
}: {
  /** Si false, no se redirige: se espera a conocer el estado de auth (evita perder /profile/list/xxx al refrescar). */
  authReady: boolean;
  isAuthenticated: boolean;
  onBlocked: () => void;
  fallbackPath?: string;
}) {
  useEffect(() => {
    if (!authReady || !isKnownRoute(window.location.pathname)) return;
    const route = parseRoute(window.location.pathname);
    if (canNavigateToTab(route.tab, isAuthenticated, route.tab === "search" ? route.searchMode : undefined)) return;
    const pathname = window.location.pathname;
    const profileListId = (route as { profileListId?: string }).profileListId;
    if (route.tab === "profile" && route.profileSection === "list" && profileListId) {
      try {
        sessionStorage.setItem(RETURN_AFTER_LOGIN_KEY, pathname);
      } catch {
        /* ignore */
      }
    }
    if (pathname !== fallbackPath) {
      window.history.replaceState({}, "", `${fallbackPath}${window.location.search}${window.location.hash}`);
    }
    onBlocked();
  }, [authReady, fallbackPath, isAuthenticated, onBlocked]);
}

export function useCoffeeRouteSync({
  coffeeSlugToId,
  setDetailCoffeeId,
  setDetailHostTab
}: {
  coffeeSlugToId: Map<string, string>;
  setDetailCoffeeId: (value: string | null) => void;
  setDetailHostTab: (value: "home" | "search" | "profile" | null) => void;
}) {
  useEffect(() => {
    // Esperar a que haya catálogo cargado para resolver slug->id.
    if (coffeeSlugToId.size === 0) return;
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
