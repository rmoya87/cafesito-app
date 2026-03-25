/**
 * Flags de producto (Vite: prefijo VITE_). Valores "false" o "0" desactivan.
 */
export function isAppTabTourV1Enabled(): boolean {
  const v = import.meta.env.VITE_APP_TAB_TOUR_V1_ENABLED;
  if (v === "false" || v === "0") return false;
  return true;
}
