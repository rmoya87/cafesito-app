/**
 * Google Analytics 4 (GA4).
 * - ID por defecto: G-BMZEQNRKR4. Se puede sobreescribir con VITE_GA4_MEASUREMENT_ID.
 * - Envío manual de page_view para SPA (send_page_view: false en config).
 * - page_location usa la URL real del navegador para que GA4 registre correctamente.
 */

const GA4_DEFAULT_MEASUREMENT_ID = "G-BMZEQNRKR4";
const rawId = (typeof import.meta !== "undefined" && import.meta.env?.VITE_GA4_MEASUREMENT_ID) || GA4_DEFAULT_MEASUREMENT_ID;

function normalizeMeasurementId(raw: string): string {
  const trimmed = String(raw).trim();
  if (!trimmed) return "";
  return trimmed.startsWith("G-") ? trimmed : `G-${trimmed}`;
}

export const GA4_MEASUREMENT_ID = normalizeMeasurementId(rawId);

declare global {
  interface Window {
    dataLayer: unknown[];
    gtag?: (...args: unknown[]) => void;
    __GA4_MEASUREMENT_ID__?: string;
  }
}

/** Inicializa gtag.js solo si VITE_GA4_MEASUREMENT_ID está definido. Llamar al arranque de la app. */
export function initGa4(): void {
  if (typeof window === "undefined" || !GA4_MEASUREMENT_ID) return;
  if (window.gtag) return; // ya inicializado
  window.dataLayer = window.dataLayer || [];
  window.gtag = function gtag() {
    window.dataLayer.push(arguments);
  };
  window.__GA4_MEASUREMENT_ID__ = GA4_MEASUREMENT_ID;
  window.gtag("js", new Date());
  window.gtag("config", GA4_MEASUREMENT_ID, { send_page_view: false });
  const script = document.createElement("script");
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(GA4_MEASUREMENT_ID)}`;
  document.head.appendChild(script);
}

/**
 * Envía un page_view a GA4.
 * Usa la URL real del navegador (origin + pathname) para page_location para que GA4 registre bien
 * aunque la app esté en un subdirectorio (ej. /cafesito-web/app/).
 */
export function sendPageView(pagePath: string, pageTitle?: string): void {
  if (typeof window === "undefined" || !GA4_MEASUREMENT_ID) return;
  if (typeof window.gtag !== "function") return;
  const pageLocation = `${window.location.origin}${window.location.pathname}`;
  const params: Record<string, string> = {
    page_location: pageLocation,
    page_path: pagePath.startsWith("/") ? pagePath : `/${pagePath}`
  };
  if (pageTitle) params.page_title = pageTitle;
  window.gtag("event", "page_view", params);
}
