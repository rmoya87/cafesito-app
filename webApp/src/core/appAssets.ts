/**
 * Base URL para assets estáticos (logo, sw.js, legal, etc.).
 * Con base "./" (app en raíz del dominio) devuelve "/" para que las URLs
 * sean absolutas desde el origen y funcionen en cualquier ruta (/profile/xxx, /coffee/xxx).
 * Con base en subpath (ej. /cafesito-web/app/) devuelve ese path con / final.
 */
export function getAppAssetBase(): string {
  const base = import.meta.env.BASE_URL ?? "./";
  if (base === "./" || base === "." || !base.startsWith("/")) return "/";
  return base.endsWith("/") ? base : base + "/";
}
