import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import { Button } from "./ui/components";
import { getAppAssetBase } from "./core/appAssets";
import { initGa4 } from "./core/ga4";
import "@fontsource-variable/material-symbols-outlined/fill.css";
import "./styles.css";

initGa4();

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
      <div className="coffee-detail-empty coffee-detail-empty-full" style={{ minHeight: "100vh", paddingTop: 28 }}>
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
  window.addEventListener("load", () => {
    const assetBase = getAppAssetBase();
    const swPath = `${assetBase.replace(/\/$/, "")}/sw.js`;
    const swUrl = swPath.startsWith("/") ? `${window.location.origin}${swPath}` : swPath;
    navigator.serviceWorker.register(swUrl).catch((error) => {
      // eslint-disable-next-line no-console
      console.error("[PWA] service worker registration failed", error);
    });
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
    <RootErrorBoundary>
      <App />
    </RootErrorBoundary>
  </React.StrictMode>
);