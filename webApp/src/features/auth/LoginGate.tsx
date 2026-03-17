import { useEffect, useMemo, useRef, useState } from "react";
import { getAppAssetBase } from "../../core/appAssets";
import { sendEvent } from "../../core/ga4";
import { getGoogleClientId, showGoogleOneTap } from "../../core/googleGsi";
import { UiIcon } from "../../ui/iconography";
import { Button, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
import { GoogleSignInButton } from "./GoogleSignInButton";

export function LoginGate({
  loading,
  message,
  errorMessage,
  onGoogleLogin,
  onGoogleCredential
}: {
  loading: boolean;
  message?: string;
  errorMessage?: string | null;
  onGoogleLogin?: () => void;
  onGoogleCredential?: (idToken: string) => void;
}) {
  const [showMobileSheet, setShowMobileSheet] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const assetBase = useMemo(() => getAppAssetBase(), []);
  const googleClientId = useMemo(() => getGoogleClientId(), []);
  const loginVideoSrc = assetBase + "login_bg.mp4";
  const logoSrc = assetBase + "logo.png";

  // Móvil: evitar que se pueda arrastrar la página de login hacia arriba/abajo
  useEffect(() => {
    document.body.classList.add("is-login-gate");
    return () => document.body.classList.remove("is-login-gate");
  }, []);

  // One Tap: sugiere "Continuar como [email]" si el usuario tiene sesión en Google
  useEffect(() => {
    if (!googleClientId || !onGoogleCredential) return;
    const cancel = showGoogleOneTap(googleClientId, (token) => onGoogleCredential(token));
    return cancel;
  }, [googleClientId, onGoogleCredential]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    video.setAttribute("webkit-playsinline", "true");
    video.muted = true;
    const play = () => {
      video.muted = true;
      const p = video.play();
      if (p != null && typeof p.catch === "function") p.catch(() => {});
    };
    const tryPlay = () => {
      requestAnimationFrame(() => play());
    };
    tryPlay();
    video.addEventListener("loadeddata", tryPlay);
    video.addEventListener("canplay", tryPlay);
    video.addEventListener("canplaythrough", tryPlay);
    return () => {
      video.removeEventListener("loadeddata", tryPlay);
      video.removeEventListener("canplay", tryPlay);
      video.removeEventListener("canplaythrough", tryPlay);
    };
  }, []);

  const renderAuthContent = () => (
    <>
      <p className="login-sheet-description">
        Únete a la comunidad del café para descubrir, elaborar y compartir tu pasión.
      </p>
      <div className="login-google-button-container">
        <GoogleSignInButton
          label="Registrarse con Google"
          loading={loading}
          disabled={!onGoogleLogin}
          onClick={onGoogleLogin ?? undefined}
        />
      </div>
      <p className="login-sheet-terms">
        Al continuar, aceptas nuestra{" "}
        <a href={assetBase + "legal/privacidad.html"} target="_blank" rel="noopener noreferrer" className="login-legal-link">Política de Privacidad</a>
        , las{" "}
        <a href={assetBase + "legal/condiciones.html"} target="_blank" rel="noopener noreferrer" className="login-legal-link">Condiciones del Servicio</a>
        {" "}y{" "}
        <a href={assetBase + "legal/eliminacion-cuenta.html"} target="_blank" rel="noopener noreferrer" className="login-legal-link">Eliminación de datos</a>.
      </p>
      {message ? <p className="login-hint">{message}</p> : null}
      {errorMessage ? <p className="login-error">{errorMessage}</p> : null}
    </>
  );

  return (
    <main className="login-gate" aria-label="Inicio de sesión">
      <section className="login-shell">
        <section className="login-left-pane">
          <video
            ref={videoRef}
            className="login-background-video"
            autoPlay
            muted
            loop
            playsInline
            preload="metadata"
            src={loginVideoSrc}
          />
          <div className="login-background-layer" />
          <div className="login-content">
            <header className="login-hero">
              <p className="login-overline">BIENVENIDO A</p>
              <h1 className="login-brand">CAFESITO</h1>
              <p className="login-subtitle">La comunidad para los amantes del café.</p>
            </header>

            <section className="login-feature-list" aria-label="Beneficios">
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="camera-filled" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Comparte</p>
                  <p className="login-feature-desc">Publica tus momentos cafeteros.</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="coffee-filled" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Explora</p>
                  <p className="login-feature-desc">Descubre nuevos granos y baristas.</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="nav-add-circle-filled" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Elabora</p>
                  <p className="login-feature-desc">Prepara recetas como un profesional.</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="auto_graph" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Registra</p>
                  <p className="login-feature-desc">Crea tu propio perfil sensorial.</p>
                </div>
              </article>
            </section>

            <Button variant="primary" className="login-start-button action-button--primary" onClick={() => { sendEvent("modal_open", { modal_id: "login_sheet" }); setShowMobileSheet(true); }}>
              EMPEZAR AHORA
            </Button>
          </div>
        </section>

        <section className="login-desktop-auth" aria-label="Acceso desktop">
          <img src={logoSrc} alt="Logo Cafesito" className="login-desktop-logo" loading="eager" decoding="async" />
          <div className="login-desktop-auth-content">{renderAuthContent()}</div>
        </section>
      </section>

      {showMobileSheet ? (
        <SheetOverlay className="login-sheet-overlay" onDismiss={() => { sendEvent("modal_close", { modal_id: "login_sheet" }); setShowMobileSheet(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "login_sheet" }); setShowMobileSheet(false); }}>
          <SheetCard className="login-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle />
            {renderAuthContent()}
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </main>
  );
}
