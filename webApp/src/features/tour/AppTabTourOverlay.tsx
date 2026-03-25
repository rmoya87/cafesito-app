import React, { useEffect, useId, useRef } from "react";
import { Button } from "../../ui/components";
import { APP_TAB_TOUR_STEP_CONTENT, APP_TAB_TOUR_STEP_HOME } from "./appTabTour";

type Props = {
  stepId: string;
  onDismissStep: () => void | Promise<void>;
  onSkipAll: () => void | Promise<void>;
  errorMessage?: string | null;
};

/** Tour contextual por pestaña; estado sincronizado en `users_db` (paridad Android). */
export function AppTabTourOverlay({ stepId, onDismissStep, onSkipAll, errorMessage }: Props) {
  const titleId = useId();
  const descId = useId();
  const overlayRef = useRef<HTMLDivElement | null>(null);
  const dismissRef = useRef<HTMLButtonElement | null>(null);
  const copy = APP_TAB_TOUR_STEP_CONTENT[stepId] ?? APP_TAB_TOUR_STEP_CONTENT[APP_TAB_TOUR_STEP_HOME];

  useEffect(() => {
    const t = window.setTimeout(() => dismissRef.current?.focus(), 50);
    return () => window.clearTimeout(t);
  }, [stepId]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") {
        e.preventDefault();
        void onDismissStep();
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onDismissStep]);

  // Bloquear scroll del contenido detrás (el scroll principal suele estar en .main-shell-scroll).
  useEffect(() => {
    const html = document.documentElement;
    const body = document.body;
    const scrollRoot = document.querySelector<HTMLElement>(".main-shell-scroll");
    const prevHtml = html.style.overflow;
    const prevBody = body.style.overflow;
    const prevScroll = scrollRoot?.style.overflow ?? "";
    html.style.overflow = "hidden";
    body.style.overflow = "hidden";
    if (scrollRoot) scrollRoot.style.overflow = "hidden";
    return () => {
      html.style.overflow = prevHtml;
      body.style.overflow = prevBody;
      if (scrollRoot) scrollRoot.style.overflow = prevScroll;
    };
  }, []);

  // Wheel/touch con passive:false para que preventDefault bloquee el scroll detrás (p. ej. iOS).
  useEffect(() => {
    const el = overlayRef.current;
    if (!el) return;
    const blockScroll = (e: Event) => e.preventDefault();
    el.addEventListener("wheel", blockScroll, { passive: false });
    el.addEventListener("touchmove", blockScroll, { passive: false });
    return () => {
      el.removeEventListener("wheel", blockScroll);
      el.removeEventListener("touchmove", blockScroll);
    };
  }, []);

  return (
    <div
      ref={overlayRef}
      className="app-tab-tour-overlay-root"
      role="dialog"
      aria-modal="true"
      aria-labelledby={titleId}
      aria-describedby={descId}
      style={{
        position: "fixed",
        inset: 0,
        zIndex: 9999,
        display: "flex",
        alignItems: "flex-end",
        justifyContent: "center",
        padding: "24px",
        paddingBottom: "max(24px, env(safe-area-inset-bottom))",
        background: "rgba(0,0,0,0.55)",
        backdropFilter: "blur(4px)",
        touchAction: "none",
        overscrollBehavior: "none"
      }}
    >
      <div
        style={{
          maxWidth: 480,
          width: "100%",
          borderRadius: 16,
          padding: "24px",
          background: "var(--color-surface, #fff)",
          color: "var(--color-on-surface, #111)",
          boxShadow: "0 8px 32px rgba(0,0,0,0.2)"
        }}
      >
        <h2 id={titleId} style={{ margin: "0 0 12px", fontSize: "1.25rem" }}>
          {copy.title}
        </h2>
        <p id={descId} style={{ margin: "0 0 20px", lineHeight: 1.5, opacity: 0.85 }}>
          {copy.body}
        </p>
        {errorMessage ? (
          <p
            role="alert"
            style={{ margin: "0 0 16px", color: "var(--color-error, #b3261e)", fontSize: "0.9rem" }}
          >
            {errorMessage}
          </p>
        ) : null}
        <div style={{ display: "flex", flexWrap: "wrap", gap: 12, justifyContent: "flex-end" }}>
          <Button type="button" variant="plain" size="md" onClick={() => void onSkipAll()}>
            Omitir todo
          </Button>
          <Button ref={dismissRef} type="button" variant="primary" size="md" onClick={() => void onDismissStep()}>
            Entendido
          </Button>
        </div>
      </div>
    </div>
  );
}
