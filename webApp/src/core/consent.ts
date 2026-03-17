/**
 * Consentimiento de cookies para la WebApp.
 * - null: primera visita, no hay decisión → mostrar banner.
 * - "essential": solo cookies esenciales → no cargar GTM/analíticas.
 * - "all": aceptar todo → cargar GTM y enviar analíticas.
 */

const STORAGE_KEY = "cafesito_cookie_consent";

export type CookieConsent = "essential" | "all" | null;

export function getConsent(): CookieConsent {
  if (typeof window === "undefined" || !window.localStorage) return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw === "essential" || raw === "all") return raw;
    return null;
  } catch {
    return null;
  }
}

export function setConsent(value: "essential" | "all"): void {
  if (typeof window === "undefined" || !window.localStorage) return;
  try {
    window.localStorage.setItem(STORAGE_KEY, value);
  } catch {
    // ignore
  }
}

/** true solo cuando el usuario ha aceptado todas las cookies (analíticas/GTM). */
export function canLoadAnalytics(): boolean {
  return getConsent() === "all";
}
