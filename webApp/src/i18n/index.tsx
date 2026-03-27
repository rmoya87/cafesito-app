import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { MESSAGES, type I18nKey, type Locale } from "./messages";

const LOCALE_KEY = "cafesito.locale";
const LEGACY_LOCALE_KEY = "cafesito.locale.value";
const LOCALE_USER_SELECTED_KEY = "cafesito.locale.user_selected";
export const SUPPORTED_LOCALES: Locale[] = ["es", "en", "fr", "pt", "de"];

export type LocalePreference = "system" | Locale;

function detectSystemLocale(): Locale {
  if (typeof window === "undefined") return "en";
  const browser = window.navigator.language.toLowerCase();
  const detected = SUPPORTED_LOCALES.find((l) => browser.startsWith(l));
  return detected ?? "en";
}

function getInitialLocalePreference(): LocalePreference {
  if (typeof window === "undefined") return "system";
  const wasUserSelected = window.localStorage.getItem(LOCALE_USER_SELECTED_KEY) === "1";
  const saved = window.localStorage.getItem(LOCALE_KEY);
  if (saved === "system") return "system";
  if (saved && SUPPORTED_LOCALES.includes(saved as Locale)) {
    return wasUserSelected ? (saved as Locale) : "system";
  }
  // Compatibilidad con versiones antiguas si existiera otra clave.
  const legacy = window.localStorage.getItem(LEGACY_LOCALE_KEY);
  if (legacy && SUPPORTED_LOCALES.includes(legacy as Locale)) {
    return wasUserSelected ? (legacy as Locale) : "system";
  }
  return "system";
}

type I18nContextValue = {
  locale: Locale;
  localePreference: LocalePreference;
  setLocalePreference: (preference: LocalePreference) => void;
  setLocale: (locale: Locale) => void;
  t: (key: I18nKey, vars?: Record<string, string | number>) => string;
};

const I18nContext = createContext<I18nContextValue | null>(null);

export function I18nProvider({ children }: { children: React.ReactNode }) {
  const [localePreference, setLocalePreferenceState] = useState<LocalePreference>(() => getInitialLocalePreference());
  const resolvedLocale = localePreference === "system" ? detectSystemLocale() : localePreference;

  useEffect(() => {
    if (typeof document !== "undefined") {
      document.documentElement.lang = resolvedLocale;
    }
  }, [resolvedLocale]);

  const value = useMemo<I18nContextValue>(
    () => ({
      locale: resolvedLocale,
      localePreference,
      setLocalePreference: (next) => {
        setLocalePreferenceState(next);
        if (typeof window !== "undefined") {
          window.localStorage.setItem(LOCALE_KEY, next);
          window.localStorage.setItem(LOCALE_USER_SELECTED_KEY, "1");
        }
      },
      // Mantener API anterior para llamadas existentes.
      setLocale: (next) => {
        setLocalePreferenceState(next);
        if (typeof window !== "undefined") {
          window.localStorage.setItem(LOCALE_KEY, next);
          window.localStorage.setItem(LOCALE_USER_SELECTED_KEY, "1");
        }
      },
      t: (key, vars) => {
        const base = MESSAGES[resolvedLocale][key] ?? MESSAGES.es[key] ?? key;
        if (!vars) return base;
        return Object.entries(vars).reduce(
          (acc, [varKey, varValue]) => acc.split(`{${varKey}}`).join(String(varValue)),
          base
        );
      }
    }),
    [resolvedLocale, localePreference]
  );

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const ctx = useContext(I18nContext);
  if (!ctx) throw new Error("useI18n must be used inside I18nProvider");
  return ctx;
}
