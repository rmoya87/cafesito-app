import type { TabId } from "../types";

/** searchMode solo aplica cuando tab === "search". Buscar usuarios y crear café exigen login. */
export function canAccessTabAsGuest(tab: TabId, searchMode?: "coffees" | "users"): boolean {
  if (tab === "coffee") return true;
  if (tab === "search") return searchMode !== "users";
  if (tab === "crear-cafe" || tab === "selecciona-cafe") return false;
  return false;
}

export function canNavigateToTab(tab: TabId, isAuthenticated: boolean, searchMode?: "coffees" | "users"): boolean {
  if (isAuthenticated) return true;
  return canAccessTabAsGuest(tab, searchMode);
}

export function resolveGuardedTab(tab: TabId, isAuthenticated: boolean, searchMode?: "coffees" | "users"): TabId {
  return canNavigateToTab(tab, isAuthenticated, searchMode) ? tab : "home";
}

export function shouldPromptAuthForInteraction(isAuthenticated: boolean): boolean {
  return !isAuthenticated;
}

export type GuardedInteraction =
  | "open_profile"
  | "toggle_follow"
  | "toggle_favorite"
  | "save_review"
  | "save_sensory"
  | "save_stock"
  | "create_post"
  | "open_diary"
  | "open_brewlab";

export function requiresAuthForInteraction(_interaction: GuardedInteraction): boolean {
  return true;
}

export function canRunInteraction(interaction: GuardedInteraction, isAuthenticated: boolean): boolean {
  if (!requiresAuthForInteraction(interaction)) return true;
  return isAuthenticated;
}
