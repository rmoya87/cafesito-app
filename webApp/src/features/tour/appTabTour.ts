import type { TabId } from "../../types";
import type { UserRow } from "../../types";

/** Paridad con Android (`AppTabTour`). */
export const APP_TAB_TOUR_STEP_HOME = "home";
export const APP_TAB_TOUR_STEP_SEARCH = "search";
export const APP_TAB_TOUR_STEP_BREWLAB = "brewlab";
export const APP_TAB_TOUR_STEP_DIARY = "diary";
export const APP_TAB_TOUR_STEP_PROFILE = "profile";

export const APP_TAB_TOUR_STEP_CONTENT: Record<
  string,
  { title: string; body: string }
> = {
  [APP_TAB_TOUR_STEP_HOME]: {
    title: "Inicio",
    body:
      "Bienvenido a Cafesito, aquí puedes elaborar, gestionar tu despensa para saber cuánto café te queda y descubrir nuevas personas."
  },
  [APP_TAB_TOUR_STEP_SEARCH]: {
    title: "Explorar",
    body:
      "Busca cafés y tostadores por nombre o por código de barras. Abre cualquier café para ver sus detalles, reseñas y notas."
  },
  [APP_TAB_TOUR_STEP_BREWLAB]: {
    title: "Elaborar",
    body:
      "Elige método y café, ajusta los parámetros y activa el temporizador. Te guiará paso a paso durante la elaboración y podrás guardar el resultado en tu diario."
  },
  [APP_TAB_TOUR_STEP_DIARY]: {
    title: "Diario",
    body:
      "Registra tus consumos de café y agua. Consulta tu historial y entiende tus hábitos con métricas y resúmenes."
  },
  [APP_TAB_TOUR_STEP_PROFILE]: {
    title: "Perfil",
    body:
      "Personaliza tu experiencia, en Actividad ves lo que prueban las personas que sigues, en ADN descubres tu perfil de sabor en base a los cafés que has probado, y en Listas creas o compartes colecciones de cafés."
  }
};

export function parseAppTourDismissedSteps(raw: string | null | undefined): Set<string> {
  if (raw == null || String(raw).trim() === "") return new Set();
  try {
    const v = JSON.parse(String(raw)) as unknown;
    if (!Array.isArray(v)) return new Set();
    return new Set(v.filter((x): x is string => typeof x === "string"));
  } catch {
    return new Set();
  }
}

export function isAppTourGloballySkipped(user: UserRow | null | undefined): boolean {
  const t = user?.app_tour_skipped_at;
  return t != null && Number(t) > 0;
}

export function shouldShowAppTabTourStep(user: UserRow, stepId: string): boolean {
  if (isAppTourGloballySkipped(user)) return false;
  return !parseAppTourDismissedSteps(user.app_tour_dismissed_steps).has(stepId);
}

export function resolveAppTabTourStepFromTab(
  activeTab: TabId,
  profileUsername: string | null | undefined,
  activeUsername: string | null | undefined
): string | null {
  const pu = (profileUsername ?? "").trim();
  const au = (activeUsername ?? "").trim();
  switch (activeTab) {
    case "home":
      return APP_TAB_TOUR_STEP_HOME;
    case "search":
      return APP_TAB_TOUR_STEP_SEARCH;
    case "brewlab":
    case "crear-cafe":
    case "selecciona-cafe":
      return APP_TAB_TOUR_STEP_BREWLAB;
    case "diary":
      return APP_TAB_TOUR_STEP_DIARY;
    case "profile":
      if (pu && au && pu === au) return APP_TAB_TOUR_STEP_PROFILE;
      return null;
    default:
      return null;
  }
}
