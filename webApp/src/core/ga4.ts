/**
 * Analíticas vía Google Tag Manager (GTM).
 * La WebApp envía todos los eventos al dataLayer; GA4 se configura y recibe los datos desde el contenedor GTM.
 * GTM solo se carga si el usuario ha aceptado todas las cookies (cookie consent). Si no, no se cargan analíticas.
 */

import { canLoadAnalytics } from "./consent";
import {
  initGtm,
  isGtmEnabled,
  pushEvent as gtmPushEvent,
  pushPageView as gtmPushPageView,
  pushUserId as gtmPushUserId
} from "./gtm";

/** Inicializa analíticas: solo GTM. Llamar solo cuando el usuario ha aceptado todas las cookies. */
export function initGa4(): void {
  if (typeof window === "undefined") return;
  if (!canLoadAnalytics()) return;
  initGtm();
}

/**
 * Asocia todas las sesiones y eventos al mismo usuario cuando está logueado.
 * Envía set_user_id al dataLayer; GTM debe tener etiqueta que configure GA4 con user_id.
 */
export function setGa4UserId(userId: string | null): void {
  if (typeof window === "undefined") return;
  if (!canLoadAnalytics() || !isGtmEnabled()) return;
  gtmPushUserId(userId);
}

/**
 * Envía un page_view al dataLayer. GTM debe tener disparador y etiquetas GA4 (page_view y screen_view).
 */
export function sendPageView(pagePath: string, pageTitle?: string): void {
  if (typeof window === "undefined") return;
  if (!canLoadAnalytics() || !isGtmEnabled()) return;
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
  if (!canLoadAnalytics() || !isGtmEnabled()) return;
  gtmPushEvent(name, params);
}
