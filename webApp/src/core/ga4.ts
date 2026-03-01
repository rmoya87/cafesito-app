/**
 * Google Analytics 4 (GA4) – propiedad 521879463.
 * Envío manual de page_view para SPA (send_page_view: false en config).
 * GA4 requiere page_location (URL completa) para que el hit aparezca en informes.
 */

const GA4_PROPERTY_ID = "521879463";

function normalizeMeasurementId(rawId: string): string {
  const trimmed = rawId.trim();
  return trimmed.startsWith("G-") ? trimmed : `G-${trimmed}`;
}

export const GA4_MEASUREMENT_ID = normalizeMeasurementId(GA4_PROPERTY_ID);

declare global {
  interface Window {
    dataLayer: unknown[];
    gtag: (...args: unknown[]) => void;
    __GA4_MEASUREMENT_ID__?: string;
  }
}

export function sendPageView(pagePath: string, pageTitle?: string): void {
  if (typeof window === "undefined" || typeof window.gtag !== "function") return;
  const origin = window.location.origin;
  const pageLocation = `${origin}${pagePath.startsWith("/") ? pagePath : `/${pagePath}`}`;
  const params: Record<string, string> = {
    page_location: pageLocation,
    page_path: pagePath
  };
  if (pageTitle) params.page_title = pageTitle;
  window.gtag("event", "page_view", params);
}
