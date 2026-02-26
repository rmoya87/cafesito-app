import { useState } from "react";
import { UiIcon } from "../../ui/iconography";
import { Button, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";

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

  const authContent = (
    <>
      <p className="login-sheet-description">
        Únete a la comunidad del café para descubrir, elaborar y compartir tu pasión.
      </p>
      <p className="login-sheet-terms">
        Al continuar aceptas el uso de tu cuenta Google para iniciar sesión en Cafesito.
      </p>
      <Button
        variant="primary"
        className="google-login-button"
        onClick={onGoogleLogin}
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
          <video className="login-background-video" autoPlay muted loop playsInline preload="metadata">
            <source src="/login_bg.mp4" type="video/mp4" />
          </video>
          <div className="login-background-layer" />
          <div className="login-content">
            <header className="login-hero">
              <p className="login-overline">CAFESITO</p>
              <h1 className="login-brand">COMUNIDAD CAFETERA</h1>
              <p className="login-subtitle">Descubre cafés, guarda rituales y comparte tu pasión.</p>
            </header>

            <section className="login-feature-list" aria-label="Beneficios">
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="nav-explore-filled" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Explora</p>
                  <p className="login-feature-desc">Encuentra cafés y recomendaciones para tu paladar.</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="science" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Elabora</p>
                  <p className="login-feature-desc">Perfecciona tus preparaciones con pasos guiados.</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="book" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">Comparte</p>
                  <p className="login-feature-desc">Publica tu actividad y conecta con la comunidad.</p>
                </div>
              </article>
            </section>

            <Button variant="plain" className="login-start-button" onClick={() => setShowMobileSheet(true)}>
              EMPEZAR AHORA
            </Button>
          </div>
        </section>

        <section className="login-desktop-auth" aria-label="Acceso desktop">
          <img src="/logo.png" alt="Logo Cafesito" className="login-desktop-logo" loading="lazy" />
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
