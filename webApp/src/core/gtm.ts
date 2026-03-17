/**
 * Google Tag Manager (GTM) — dataLayer para WebApp.
 * Cuando VITE_GTM_CONTAINER_ID está definido, la app envía todos los datos a dataLayer
 * y GTM se encarga de cargar GA4 y disparar eventos/audiencias desde el contenedor.
 * Estructura alineada con Android para que el mismo contenedor (o lógica) sirva en web y app.
 */

import { normalizePathToScreenName } from "./routing";

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

function isLocalhost(): boolean {
  if (typeof window === "undefined") return false;
  const h = window.location?.hostname ?? "";
  return h === "localhost" || h === "127.0.0.1" || h === "";
}

declare global {
  interface Window {
    dataLayer: unknown[];
  }
}

const GTM_CONTAINER_ID = typeof import.meta !== "undefined" ? String(import.meta.env?.VITE_GTM_CONTAINER_ID ?? "").trim() : "";

function ensureDataLayer(): void {
  if (typeof window === "undefined") return;
  window.dataLayer = window.dataLayer || [];
}

/**
 * Inyecta el script de GTM y deja dataLayer listo. Llamar al arranque si hay container ID.
 * No carga en localhost ni sin red.
 */
export function initGtm(): void {
  if (typeof window === "undefined") return;
  if (!GTM_CONTAINER_ID) return;
  if (isLocalhost()) return;
  if (typeof navigator !== "undefined" && !navigator.onLine) return;
  ensureDataLayer();
  window.dataLayer.push({
    platform: "web",
    event: "gtm_platform_ready"
  });
  const id = GTM_CONTAINER_ID;
  const script = document.createElement("script");
  script.async = true;
  script.src = `https://www.googletagmanager.com/gtm.js?id=${encodeURIComponent(id)}`;
  if (document.head) {
    document.head.appendChild(script);
  } else {
    document.addEventListener("DOMContentLoaded", () => document.head?.appendChild(script));
  }
}

/**
 * Envía page_view al dataLayer. GTM debe tener un disparador para este evento y una etiqueta GA4.
 * Incluye screen_name normalizado (paridad con Android) para la etiqueta GA4 - screen_view.
 */
export function pushPageView(pagePath: string, pageTitle?: string): void {
  if (typeof window === "undefined" || !window.dataLayer) return;
  let pageLocation = `${window.location.origin}${window.location.pathname}`;
  let path = pagePath.startsWith("/") ? pagePath : `/${pagePath}`;
  if (isPwaStandalone()) {
    pageLocation = appendPwaParam(pageLocation);
    path = appendPwaParam(path);
  }
  const payload: Record<string, unknown> = {
    event: "page_view",
    page_path: path,
    page_location: pageLocation,
    screen_name: normalizePathToScreenName(path)
  };
  if (pageTitle) payload.page_title = pageTitle;
  window.dataLayer.push(payload);
}

/**
 * Envía user_id al dataLayer. GTM debe tener variable/etiqueta que lea este evento y configure GA4.
 */
export function pushUserId(userId: string | null): void {
  if (typeof window === "undefined" || !window.dataLayer) return;
  window.dataLayer.push({
    event: "set_user_id",
    user_id: userId ?? undefined
  });
}

/**
 * Envía un evento personalizado al dataLayer (botones, carruseles, modales, etc.).
 * GTM puede crear un disparador "Evento personalizado" con este nombre y una etiqueta GA4 que envíe el evento con los mismos parámetros.
 * @param eventName Nombre del evento (snake_case recomendado, ej. carousel_nav, modal_open, button_click).
 * @param params Parámetros opcionales (ej. component_id, direction, modal_id). Valores deben ser string, number o boolean para GA4.
 */
export function pushEvent(eventName: string, params?: Record<string, string | number | boolean>): void {
  if (typeof window === "undefined" || !window.dataLayer) return;
  const payload: Record<string, unknown> = { event: eventName };
  if (params && Object.keys(params).length > 0) {
    Object.assign(payload, params);
  }
  window.dataLayer.push(payload);
}

export function getGtmContainerId(): string {
  return GTM_CONTAINER_ID;
}

export function isGtmEnabled(): boolean {
  return Boolean(GTM_CONTAINER_ID);
}
