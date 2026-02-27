import { useEffect, useRef, useState } from "react";
import { UiIcon } from "../../ui/iconography";
import { Button, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";

const loginVideoSrc = `${import.meta.env.BASE_URL}login_bg.mp4`;
const logoSrc = `${import.meta.env.BASE_URL}logo.png`;

export function LoginGate({
  loading,
  message,
  errorMessage,
  onGoogleLogin
}: {
  loading: boolean;
  message?: string;
  errorMessage?: string | null;
  onGoogleLogin?: () => void;
}) {
  const [showMobileSheet, setShowMobileSheet] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;
    video.setAttribute("webkit-playsinline", "true");
    video.muted = true;
    const play = () => {
      video.muted = true;
      video.play().catch(() => {});
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

  const authContent = (
    <>
      <p className="login-sheet-description">
        Únete a la comunidad del café para descubrir, elaborar y compartir tu pasión.
      </p>
      <p className="login-sheet-terms">
        Al continuar, aceptas nuestros Términos y Condiciones.
      </p>
      <Button
        type="button"
        variant="primary"
        className="google-login-button"
        onClick={onGoogleLogin ? () => onGoogleLogin() : undefined}
        disabled={loading || !onGoogleLogin}
      >
        <span className="auth-prompt-google-g" aria-hidden="true">
          G
        </span>
        {loading ? "Conectando..." : "Continuar con Google"}
      </Button>
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
            preload="auto"
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
                  <UiIcon name="camera" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Comparte</p>
                  <p className="login-feature-desc">Publica tus momentos cafeteros.</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="coffee" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Explora</p>
                  <p className="login-feature-desc">Descubre nuevos granos y baristas.</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="science" className="ui-icon" />
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

            <Button variant="plain" className="login-start-button" onClick={() => setShowMobileSheet(true)}>
              EMPEZAR AHORA
            </Button>
          </div>
        </section>

        <section className="login-desktop-auth" aria-label="Acceso desktop">
          <img src={logoSrc} alt="Logo Cafesito" className="login-desktop-logo" loading="lazy" />
          <div className="login-desktop-auth-content">{authContent}</div>
        </section>
      </section>

      {showMobileSheet ? (
        <SheetOverlay className="login-sheet-overlay" onDismiss={() => setShowMobileSheet(false)} onClick={() => setShowMobileSheet(false)}>
          <SheetCard className="login-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle />
            {authContent}
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </main>
  );
}
