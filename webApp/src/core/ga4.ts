/**
 * Google Analytics 4 (GA4) y Google Tag Manager (GTM).
 * - Si VITE_GTM_CONTAINER_ID está definido: se usa GTM (dataLayer); init/config/eventos se gestionan en el contenedor.
 * - Si no: se usa gtag directamente con VITE_GA4_MEASUREMENT_ID (por defecto G-BMZEQNRKR4).
 * - En PWA se añade ?pwa=1 a page_path y page_location para segmentar.
 */

import {
  getGtmContainerId,
  initGtm,
  isGtmEnabled,
  pushEvent as gtmPushEvent,
  pushPageView as gtmPushPageView,
  pushUserId as gtmPushUserId
} from "./gtm";

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

/** True si estamos en localhost (evitar cargar GA en dev y evitar ERR_NAME_NOT_RESOLVED si GA está bloqueado). */
function isLocalhost(): boolean {
  if (typeof window === "undefined") return false;
  const h = window.location?.hostname ?? "";
  return h === "localhost" || h === "127.0.0.1" || h === "";
}

/** Inicializa analíticas: GTM si hay VITE_GTM_CONTAINER_ID, si no gtag/GA4. Llamar al arranque de la app. */
export function initGa4(): void {
  if (typeof window === "undefined") return;
  if (isLocalhost()) return;
  if (typeof navigator !== "undefined" && !navigator.onLine) return;
  if (getGtmContainerId()) {
    initGtm();
    return;
  }
  const id = GA4_MEASUREMENT_ID;
  if (!id) return;
  if (!window.gtag) {
    window.dataLayer = window.dataLayer || [];
    window.gtag = function gtag(...args: unknown[]) {
      window.dataLayer.push(args);
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
  window.gtag("config", id, {
    send_page_view: false,
    user_properties: { platform: "web" }
  });
}

/**
 * Asocia todas las sesiones y eventos al mismo usuario cuando está logueado.
 * Con GTM: push a dataLayer (evento set_user_id). Sin GTM: gtag config user_id.
 */
export function setGa4UserId(userId: string | null): void {
  if (typeof window === "undefined") return;
  if (isGtmEnabled()) {
    gtmPushUserId(userId);
    return;
  }
  if (!GA4_MEASUREMENT_ID || typeof window.gtag !== "function") return;
  window.gtag("config", GA4_MEASUREMENT_ID, { user_id: userId ?? undefined });
}

/**
 * Envía un page_view. Con GTM: push a dataLayer. Sin GTM: gtag event page_view.
 */
export function sendPageView(pagePath: string, pageTitle?: string): void {
  if (typeof window === "undefined") return;
  if (isGtmEnabled()) {
    gtmPushPageView(pagePath, pageTitle);
    return;
  }
  if (!GA4_MEASUREMENT_ID || typeof window.gtag !== "function") return;
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

/**
 * Envía un evento personalizado (botones, carruseles, modales, etc.). Con GTM: push al dataLayer; sin GTM: gtag event.
 * @param name Nombre del evento (snake_case, ej. carousel_nav, modal_open, button_click).
 * @param params Parámetros opcionales (string, number o boolean).
 */
export function sendEvent(name: string, params?: Record<string, string | number | boolean>): void {
  if (typeof window === "undefined") return;
  if (isGtmEnabled()) {
    gtmPushEvent(name, params);
    return;
  }
  if (!GA4_MEASUREMENT_ID || typeof window.gtag !== "function") return;
  const safeParams = params ?? {};
  const gtagParams: Record<string, string | number | boolean> = {};
  for (const [k, v] of Object.entries(safeParams)) {
    if (typeof v === "string" || typeof v === "number" || typeof v === "boolean") gtagParams[k] = v;
  }
  window.gtag("event", name, gtagParams);
}
