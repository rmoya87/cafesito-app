import type { TabId } from "../types";
import type { IconName } from "../ui/iconography";

export const NAV_ITEMS: Array<{ id: TabId; label: string; icon: IconName; activeIcon: IconName }> = [
  { id: "home", label: "Inicio", icon: "nav-home-outline", activeIcon: "nav-home-filled" },
  { id: "search", label: "Explorar", icon: "nav-search-outline", activeIcon: "nav-search-filled" },
  { id: "brewlab", label: "Elabora", icon: "nav-add-circle-outline", activeIcon: "nav-add-circle-filled" },
  { id: "diary", label: "Diario", icon: "nav-book-outline", activeIcon: "nav-book-filled" },
  { id: "profile", label: "Perfil", icon: "nav-person-outline", activeIcon: "nav-person-filled" }
];
