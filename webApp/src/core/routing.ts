import type { TabId } from "../types";
import { normalizeLookupText } from "./text";

export function parseRoute(pathname: string) {
  const clean = pathname.replace(/\/+$/, "") || "/";
  const segments = clean.split("/").filter(Boolean);
  const tabIdx = segments.findIndex((segment) => TAB_SEGMENTS.includes(segment));
  const routeSegments = tabIdx >= 0 ? segments.slice(tabIdx) : segments;
  const first = routeSegments[0] ?? "";
  const second = routeSegments[1] ?? "";

  if (first === "search") {
    return {
      tab: "search" as TabId,
      searchMode: (second === "users" ? "users" : "coffees") as "coffees" | "users",
      profileUsername: null,
      coffeeSlug: null,
      profileSection: null
    };
  }
  if (first === "brewlab") return { tab: "brewlab" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null, profileSection: null };
  if (first === "diary") return { tab: "diary" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null, profileSection: null };
  if (first === "profile") {
    const profileSection = second === "historial" ? ("historial" as const) : null;
    return {
      tab: "profile" as TabId,
      searchMode: "coffees" as const,
      profileUsername: profileSection ? null : (second ? decodeURIComponent(second) : null),
      coffeeSlug: null,
      profileSection
    };
  }
  if (first === "coffee") {
    return {
      tab: "coffee" as TabId,
      searchMode: "coffees" as const,
      profileUsername: null,
      coffeeSlug: second ? decodeURIComponent(second) : null,
      profileSection: null
    };
  }
  return { tab: "timeline" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null, profileSection: null };
}

const TAB_SEGMENTS = ["timeline", "search", "brewlab", "diary", "profile", "coffee"];

/** Pathname de la raíz de la app (para mostrar login en URL raíz, no /timeline). */
export function getAppRootPath(pathname: string): string {
  const segments = pathname.replace(/^\/|\/$/g, "").split("/").filter(Boolean);
  const idx = segments.findIndex((s) => TAB_SEGMENTS.includes(s));
  if (idx < 0) return pathname || "/";
  if (idx === 0) return "/";
  return "/" + segments.slice(0, idx).join("/");
}

export function isKnownRoute(pathname: string): boolean {
  const clean = pathname.replace(/\/+$/, "") || "/";
  if (clean === "/") return true;
  const segments = clean.split("/").filter(Boolean);
  const tabIdx = segments.findIndex((segment) => TAB_SEGMENTS.includes(segment));
  if (tabIdx < 0) return false;

  const routeSegments = segments.slice(tabIdx);
  const first = routeSegments[0] ?? "";
  const second = routeSegments[1] ?? "";

  if (first === "timeline" || first === "brewlab" || first === "diary") return routeSegments.length === 1;
  if (first === "search") return routeSegments.length <= 2 && (second === "" || second === "users");
  if (first === "profile") return routeSegments.length <= 2;
  if (first === "coffee") return routeSegments.length === 2 && second.length > 0;
  return false;
}

function slugifyText(value: string): string {
  return normalizeLookupText(value)
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

export function toCoffeeSlug(name: string, brand?: string | null, forceBrand = false): string {
  const baseFromName = slugifyText(name);
  if (forceBrand && brand) {
    const forced = slugifyText(`${name} ${brand}`);
    return forced || baseFromName || "cafe";
  }
  if (baseFromName.length > 10) return baseFromName;

  const baseWithBrand = slugifyText(`${name} ${brand ?? ""}`);
  return baseWithBrand || baseFromName || "cafe";
}

export type ProfileSection = "historial" | null;

export function buildRoute(
  tab: TabId,
  searchMode: "coffees" | "users",
  profileUsername: string | null,
  coffeeSlug?: string | null,
  profileSection?: ProfileSection
): string {
  if (tab === "search") return searchMode === "users" ? "/search/users" : "/search";
  if (tab === "brewlab") return "/brewlab";
  if (tab === "diary") return "/diary";
  if (tab === "profile") {
    if (profileSection === "historial") return "/profile/historial";
    return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}` : "/profile";
  }
  if (tab === "coffee") return coffeeSlug ? `/coffee/${encodeURIComponent(coffeeSlug)}/` : "/timeline";
  return "/timeline";
}
