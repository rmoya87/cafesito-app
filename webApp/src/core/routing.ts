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
      profileSection: null,
      profileListId: undefined
    };
  }
  if (first === "brewlab") return { tab: "brewlab" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null, profileSection: null, profileListId: undefined };
  if (first === "diary") {
    const diarySubView = second === "cafes-probados" ? ("cafes-probados" as const) : undefined;
    return { tab: "diary" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null, profileSection: null, profileListId: undefined, diarySubView };
  }
  if (first === "profile") {
    const third = routeSegments[2] ?? "";
    const fourth = routeSegments[3] ?? "";
    const isListSection = second === "list" && third.length > 0;
    const listOptionsView = isListSection && fourth === "options";
    const profileSectionFromSecond = second === "historial" || second === "followers" || second === "following" || second === "favorites" ? second : isListSection ? "list" : null;
    const profileSectionFromThird = third === "followers" || third === "following" || third === "favorites" || third === "historial" ? third : null;
    const profileSection = profileSectionFromThird ?? (isListSection ? "list" : profileSectionFromSecond);
    const profileUsername = profileSectionFromSecond && !isListSection
      ? null
      : second && !isListSection
        ? decodeURIComponent(second)
        : null;
    const profileListId = isListSection ? third : null;
    return {
      tab: "profile" as TabId,
      searchMode: "coffees" as const,
      profileUsername,
      coffeeSlug: null,
      profileSection: profileSection as "historial" | "followers" | "following" | "favorites" | "list" | null,
      profileListId: profileListId ?? undefined,
      listOptionsView: listOptionsView || undefined
    };
  }
  if (first === "coffee") {
    return {
      tab: "coffee" as TabId,
      searchMode: "coffees" as const,
      profileUsername: null,
      coffeeSlug: second ? decodeURIComponent(second) : null,
      profileSection: null,
      profileListId: undefined
    };
  }
  return { tab: "home" as TabId, searchMode: "coffees" as const, profileUsername: null, coffeeSlug: null, profileSection: null, profileListId: undefined };
}

const TAB_SEGMENTS = ["home", "search", "brewlab", "diary", "profile", "coffee"];

/** Pathname de la raíz de la app (para mostrar login en URL raíz, no /home). */
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

  if (first === "home" || first === "brewlab") return routeSegments.length === 1;
  if (first === "diary") return routeSegments.length === 1 || (routeSegments.length === 2 && second === "cafes-probados");
  if (first === "search") return routeSegments.length <= 2 && (second === "" || second === "users");
  if (first === "profile") {
    if (routeSegments.length === 1) return true;
    if (routeSegments.length === 2) return true;
    if (routeSegments.length === 3) {
      const third = routeSegments[2] ?? "";
      return third === "followers" || third === "following" || third === "favorites" || third === "historial" || (second === "list" && third.length > 0);
    }
    if (routeSegments.length === 4 && second === "list") {
      const third = routeSegments[2] ?? "";
      const fourth = routeSegments[3] ?? "";
      return third.length > 0 && fourth === "options";
    }
    return false;
  }
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

export type ProfileSection = "historial" | "followers" | "following" | "favorites" | "list" | null;

export type DiarySubView = "cafes-probados" | undefined;

export function buildRoute(
  tab: TabId,
  searchMode: "coffees" | "users",
  profileUsername: string | null,
  coffeeSlug?: string | null,
  profileSection?: ProfileSection,
  profileListId?: string | null,
  diarySubView?: DiarySubView,
  listOptionsView?: boolean
): string {
  if (tab === "search") return searchMode === "users" ? "/search/users" : "/search";
  if (tab === "brewlab") return "/brewlab";
  if (tab === "diary") return diarySubView === "cafes-probados" ? "/diary/cafes-probados" : "/diary";
  if (tab === "profile") {
    if (profileSection === "historial") return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}/historial` : "/profile/historial";
    if (profileSection === "favorites") return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}/favorites` : "/profile/favorites";
    if (profileSection === "list" && profileListId)
      return listOptionsView ? `/profile/list/${encodeURIComponent(profileListId)}/options` : `/profile/list/${encodeURIComponent(profileListId)}`;
    if (profileSection === "followers") return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}/followers` : "/profile/followers";
    if (profileSection === "following") return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}/following` : "/profile/following";
    return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}` : "/profile";
  }
  if (tab === "coffee") return coffeeSlug ? `/coffee/${encodeURIComponent(coffeeSlug)}/` : "/home";
  return "/home";
}
