/**
 * Analíticas vía Google Tag Manager (GTM).
 * La WebApp envía todos los eventos al dataLayer; GA4 se configura y recibe los datos desde el contenedor GTM.
 * No se usa gtag ni GA4 directo; el snippet de GTM está en index.html (head + noscript en body).
 */

import {
  initGtm,
  isGtmEnabled,
  pushEvent as gtmPushEvent,
  pushPageView as gtmPushPageView,
  pushUserId as gtmPushUserId
} from "./gtm";

/** Inicializa analíticas: solo GTM. Llamar al arranque de la app. */
export function initGa4(): void {
  if (typeof window === "undefined") return;
  initGtm();
}

/**
 * Asocia todas las sesiones y eventos al mismo usuario cuando está logueado.
 * Envía set_user_id al dataLayer; GTM debe tener etiqueta que configure GA4 con user_id.
 */
export function setGa4UserId(userId: string | null): void {
  if (typeof window === "undefined") return;
  if (!isGtmEnabled()) return;
  gtmPushUserId(userId);
}

/**
 * Envía un page_view al dataLayer. GTM debe tener disparador y etiquetas GA4 (page_view y screen_view).
 */
export function sendPageView(pagePath: string, pageTitle?: string): void {
  if (typeof window === "undefined") return;
  if (!isGtmEnabled()) return;
  gtmPushPageView(pagePath, pageTitle);
}

/**
 * Envía un evento personalizado al dataLayer (botones, carruseles, modales, etc.).
 * GTM debe tener disparadores y etiquetas GA4 para cada tipo de evento.
 * @param name Nombre del evento (snake_case, ej. carousel_nav, modal_open, button_click).
 * @param params Parámetros opcionales (string, number o boolean).
 */
export function sendEvent(name: string, params?: Record<string, string | number | boolean>): void {
  if (typeof window === "undefined") return;
  if (!isGtmEnabled()) return;
  gtmPushEvent(name, params);
}
