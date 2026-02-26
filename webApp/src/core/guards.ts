import type { TabId } from "../types";

export function canAccessTabAsGuest(tab: TabId): boolean {
  return tab === "coffee";
}

export function canNavigateToTab(tab: TabId, isAuthenticated: boolean): boolean {
  if (isAuthenticated) return true;
  return canAccessTabAsGuest(tab);
}

export function resolveGuardedTab(tab: TabId, isAuthenticated: boolean): TabId {
  return canNavigateToTab(tab, isAuthenticated) ? tab : "timeline";
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
