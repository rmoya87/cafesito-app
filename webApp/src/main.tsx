import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import { Button } from "./ui/components";
import { initGa4 } from "./core/ga4";
import { getConsent } from "./core/consent";
import { registerSW } from "virtual:pwa-register";
import { applyThemeToDocument, getThemeMode } from "./core/theme";
import { I18nProvider } from "./i18n";
import "./fonts-material-symbols.css";
import "./styles.css";

function appendHeadHint(rel: "preconnect" | "dns-prefetch", href: string, crossOrigin = false): void {
  if (typeof document === "undefined") return;
  if (document.head.querySelector(`link[rel='${rel}'][href='${href}']`)) return;
  const link = document.createElement("link");
  link.rel = rel;
  link.href = href;
  if (crossOrigin) link.crossOrigin = "anonymous";
  document.head.appendChild(link);
}

function configureNetworkHints(consent: "all" | "essential" | "none" | null): void {
  // Supabase es tráfico funcional de la app, permitido siempre.
  const supabaseUrl = (window as Window & { __SUPABASE_CONFIG__?: { url?: string } }).__SUPABASE_CONFIG__?.url;
  if (supabaseUrl) {
    try {
      const origin = new URL(supabaseUrl).origin;
      appendHeadHint("preconnect", origin, true);
      appendHeadHint("dns-prefetch", origin);
    } catch {
      // ignore malformed URL
    }
  }
  // GA/GTM solo con consentimiento total.
  if (consent === "all") {
    appendHeadHint("preconnect", "https://www.googletagmanager.com", true);
    appendHeadHint("dns-prefetch", "https://region1.google-analytics.com");
    appendHeadHint("preconnect", "https://accounts.google.com", true);
  }
}

const consent = getConsent();
configureNetworkHints(consent);
if (consent === "all") {
  initGa4();
}
applyThemeToDocument(getThemeMode());

// Solo en dev: ?safe-area=1 simula el notch de iOS (topbar con espacio superior) para validar sin iPhone
if (import.meta.env.DEV && typeof window !== "undefined" && window.location.search.includes("safe-area=1")) {
  document.documentElement.classList.add("dev-safe-area-sim");
}
// PWA "Añadir a escritorio" (iOS/Android): respetar notch/barra de estado para que el topbar no quede debajo
function detectPwaStandalone(): void {
  if (typeof document === "undefined" || !document.documentElement) return;
  const isStandalone =
    (typeof window !== "undefined" &&
      window.matchMedia("(display-mode: standalone)").matches) ||
    (typeof (navigator as unknown as { standalone?: boolean }).standalone === "boolean" &&
      (navigator as unknown as { standalone?: boolean }).standalone);
  if (isStandalone) {
    document.documentElement.classList.add("pwa-standalone");
  } else {
    document.documentElement.classList.remove("pwa-standalone");
  }
}

function saveLastUserAccess(): void {
  if (typeof window === "undefined" || !localStorage) return;
  const user = localStorage.getItem("currentUser");
  if (user) {
    const lastAccessData = {
      user,
      timestamp: new Date().toISOString()
    };
    localStorage.setItem("lastUserAccess", JSON.stringify(lastAccessData));
  }
}

if (typeof window !== "undefined") {
  detectPwaStandalone();
  // Solo en dev: ?pwa=1 simula modo PWA (standalone) para probar estilos sin instalar la app
  if (import.meta.env.DEV && window.location.search.includes("pwa=1")) {
    document.documentElement.classList.add("pwa-standalone");
  }
  saveLastUserAccess();
  window.matchMedia("(display-mode: standalone)").addEventListener("change", detectPwaStandalone);
}

class RootErrorBoundary extends React.Component<
  React.PropsWithChildren,
  { hasError: boolean; message: string }
> {
  constructor(props: React.PropsWithChildren) {
    super(props);
    this.state = { hasError: false, message: "" };
  }

  static getDerivedStateFromError(error: unknown) {
    const message = error instanceof Error ? error.message : "Error inesperado";
    return { hasError: true, message };
  }

  componentDidCatch(error: unknown) {
    // Preserve the stack trace in the console for debugging.
    // eslint-disable-next-line no-console
    console.error("[RootErrorBoundary]", error);
  }

  render() {
    if (!this.state.hasError) return this.props.children;
    return (
      <div className="coffee-detail-empty coffee-detail-empty-full is-full-viewport">
        <h1 className="title">Se produjo un error</h1>
        <p className="coffee-sub">{this.state.message || "No se pudo renderizar la aplicación."}</p>
        <Button variant="primary" onClick={() => window.location.reload()}>
          Recargar
        </Button>
      </div>
    );
  }
}

if (import.meta.env.PROD && "serviceWorker" in navigator) {
  registerSW({
    immediate: true,
    onRegisteredSW(_swUrl, registration) {
      if (!registration) return;
      const checkUpdate = () => {
        if (registration.installing || !navigator.onLine) return;
        void registration.update();
      };
      document.addEventListener("visibilitychange", () => {
        if (document.visibilityState === "visible") checkUpdate();
      });
      setInterval(checkUpdate, 5 * 60 * 1000);
    }
  });
}

if (import.meta.env.DEV && "serviceWorker" in navigator) {
  void navigator.serviceWorker.getRegistrations().then((registrations) => {
    registrations.forEach((registration) => {
      void registration.unregister();
    });
  });
  if ("caches" in window) {
    void caches.keys().then((keys) => {
      keys.forEach((key) => {
        void caches.delete(key);
      });
    });
  }
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <I18nProvider>
      <RootErrorBoundary>
        <App />
      </RootErrorBoundary>
    </I18nProvider>
  </React.StrictMode>
);