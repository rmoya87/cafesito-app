import React from "react";
import ReactDOM from "react-dom/client";
import { App } from "./App";
import "@fontsource-variable/material-symbols-outlined/fill.css";
import "./styles.css";

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
        <button type="button" className="action-button" onClick={() => window.location.reload()}>
          Recargar
        </button>
      </div>
    );
  }
}

if (import.meta.env.PROD && "serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/sw.js").catch((error) => {
      // eslint-disable-next-line no-console
      console.error("[PWA] service worker registration failed", error);
    });
  });
}

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <RootErrorBoundary>
      <App />
    </RootErrorBoundary>
  </React.StrictMode>
);
