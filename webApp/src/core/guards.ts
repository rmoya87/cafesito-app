import type { TabId } from "../types";

export function canAccessTabAsGuest(tab: TabId): boolean {
  return tab === "coffee";
}

export function shouldPromptAuthForInteraction(isAuthenticated: boolean): boolean {
  return !isAuthenticated;
}
