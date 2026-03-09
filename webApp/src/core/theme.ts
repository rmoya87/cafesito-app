/**
 * Preferencia de tema: automático (sistema), claro u oscuro.
 * Misma semántica que Android (ThemeMode).
 */
export const THEME_STORAGE_KEY = "cafesito:theme";

export type ThemeModeValue = "auto" | "light" | "dark";

export function getThemeMode(): ThemeModeValue {
  if (typeof window === "undefined" || !localStorage) return "auto";
  const raw = localStorage.getItem(THEME_STORAGE_KEY);
  if (raw === "light" || raw === "dark" || raw === "auto") return raw;
  return "auto";
}

export function setThemeMode(mode: ThemeModeValue): void {
  if (typeof window === "undefined" || !localStorage) return;
  localStorage.setItem(THEME_STORAGE_KEY, mode);
  applyThemeToDocument(mode);
}

const THEME_COLOR_DARK = "#212121";
const THEME_COLOR_LIGHT = "#f7f7f7";

function updateThemeColorMeta(mode: ThemeModeValue): void {
  if (typeof document === "undefined") return;
  let meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"][data-forced]');
  if (mode === "auto") {
    if (meta) meta.remove();
    return;
  }
  if (!meta) {
    meta = document.createElement("meta");
    meta.name = "theme-color";
    meta.setAttribute("data-forced", "true");
    document.head.appendChild(meta);
  }
  meta.content = mode === "dark" ? THEME_COLOR_DARK : THEME_COLOR_LIGHT;
}

/**
 * Aplica la preferencia de tema al documento (html).
 * - auto: quita clases y deja que @media (prefers-color-scheme) actúe.
 * - light / dark: añade clase theme-light o theme-dark para forzar el tema.
 * Actualiza también theme-color para que la barra de estado del móvil coincida.
 */
export function applyThemeToDocument(mode: ThemeModeValue): void {
  if (typeof document === "undefined" || !document.documentElement) return;
  const root = document.documentElement;
  root.classList.remove("theme-light", "theme-dark");
  if (mode === "light") root.classList.add("theme-light");
  else if (mode === "dark") root.classList.add("theme-dark");
  updateThemeColorMeta(mode);
}
