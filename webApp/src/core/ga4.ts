/**
 * Google Analytics 4 (GA4).
 * - ID por defecto: G-BMZEQNRKR4. Se puede sobreescribir con VITE_GA4_MEASUREMENT_ID.
 * - Envío manual de page_view para SPA (send_page_view: false en config).
 * - page_location usa la URL real del navegador para que GA4 registre correctamente.
 * - En PWA (app añadida al escritorio): se añade ?pwa=1 a page_path y page_location para segmentar en GA.
 */

/** True si la app se está ejecutando como PWA (añadida a la pantalla de inicio). */
function isPwaStandalone(): boolean {
  if (typeof window === "undefined" || typeof document === "undefined") return false;
  if (document.documentElement.classList.contains("pwa-standalone")) return true;
  if (typeof window.matchMedia !== "undefined" && window.matchMedia("(display-mode: standalone)").matches) return true;
  const nav = window.navigator as unknown as { standalone?: boolean };
  return typeof nav.standalone === "boolean" && nav.standalone;
}

function appendPwaParam(urlOrPath: string): string {
  const sep = urlOrPath.includes("?") ? "&" : "?";
  return `${urlOrPath}${sep}pwa=1`;
}

const GA4_DEFAULT_MEASUREMENT_ID = "G-BMZEQNRKR4";
const envId = typeof import.meta !== "undefined" ? String(import.meta.env?.VITE_GA4_MEASUREMENT_ID ?? "").trim() : "";
const rawId = envId || GA4_DEFAULT_MEASUREMENT_ID;

function normalizeMeasurementId(raw: string): string {
  const trimmed = String(raw).trim();
  if (!trimmed) return "";
  return trimmed.startsWith("G-") ? trimmed : `G-${trimmed}`;
}

export const GA4_MEASUREMENT_ID = normalizeMeasurementId(rawId) || GA4_DEFAULT_MEASUREMENT_ID;

declare global {
  interface Window {
    dataLayer: unknown[];
    gtag?: (...args: unknown[]) => void;
    __GA4_MEASUREMENT_ID__?: string;
  }
}

/** Inicializa gtag.js con el measurement ID (por defecto G-BMZEQNRKR4). Llamar al arranque de la app. */
export function initGa4(): void {
  if (typeof window === "undefined") return;
  const id = GA4_MEASUREMENT_ID;
  if (!id) return;
  if (!window.gtag) {
    window.dataLayer = window.dataLayer || [];
    window.gtag = function gtag() {
      window.dataLayer.push(arguments);
    };
    window.gtag("js", new Date());
    const script = document.createElement("script");
    script.async = true;
    script.src = `https://www.googletagmanager.com/gtag/js?id=${encodeURIComponent(id)}`;
    if (document.head) {
      document.head.appendChild(script);
    } else {
      document.addEventListener("DOMContentLoaded", () => document.head?.appendChild(script));
    }
  }
  window.__GA4_MEASUREMENT_ID__ = id;
  window.gtag("config", id, { send_page_view: false });
}

/**
 * Envía un page_view a GA4.
 * Usa la URL real del navegador (origin + pathname) para page_location para que GA4 registre bien
 * aunque la app esté en un subdirectorio (ej. /cafesito-web/app/).
 * En PWA se añade ?pwa=1 a page_path y page_location para poder segmentar tráfico PWA en GA.
 */
export function sendPageView(pagePath: string, pageTitle?: string): void {
  if (typeof window === "undefined" || !GA4_MEASUREMENT_ID) return;
  if (typeof window.gtag !== "function") return;
  let pageLocation = `${window.location.origin}${window.location.pathname}`;
  let path = pagePath.startsWith("/") ? pagePath : `/${pagePath}`;
  if (isPwaStandalone()) {
    pageLocation = appendPwaParam(pageLocation);
    path = appendPwaParam(path);
  }
  const params: Record<string, string> = {
    page_location: pageLocation,
    page_path: path
  };
  if (pageTitle) params.page_title = pageTitle;
  window.gtag("event", "page_view", params);
}
