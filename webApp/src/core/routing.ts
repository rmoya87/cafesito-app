import type { TabId } from "../types";
import { normalizeLookupText } from "./text";

export function parseRoute(pathname: string) {
  const clean = pathname.replace(/\/+$/, "") || "/";
  const segments = clean.split("/").filter(Boolean);
  const first = segments[0] ?? "";
  const second = segments[1] ?? "";

  if (first === "search") {
    return {
      tab: "search" as TabId,
      searchMode: (second === "users" ? "users" : "coffees") as "coffees" | "users",
      profileUsername: null,
      coffeeSlug: null
    };
  }
  if (first === "brewlab") return { tab: "brewlab" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null };
  if (first === "diary") return { tab: "diary" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null };
  if (first === "profile") {
    return {
      tab: "profile" as TabId,
      searchMode: "coffees" as const,
      profileUsername: second ? decodeURIComponent(second) : null,
      coffeeSlug: null
    };
  }
  if (first === "coffee") {
    return {
      tab: "coffee" as TabId,
      searchMode: "coffees" as const,
      profileUsername: null,
      coffeeSlug: second ? decodeURIComponent(second) : null
    };
  }
  return { tab: "timeline" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null };
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

export function toCoffeeSlug(name: string): string {
  const base = normalizeLookupText(name)
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
  return base || "cafe";
}

export function buildRoute(
  tab: TabId,
  searchMode: "coffees" | "users",
  profileUsername: string | null,
  coffeeSlug?: string | null
): string {
  if (tab === "search") return searchMode === "users" ? "/search/users" : "/search";
  if (tab === "brewlab") return "/brewlab";
  if (tab === "diary") return "/diary";
  if (tab === "profile") return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}` : "/profile";
  if (tab === "coffee") return coffeeSlug ? `/coffee/${encodeURIComponent(coffeeSlug)}` : "/timeline";
  return "/timeline";
}
