import { useEffect, useMemo, useRef, useState } from "react";
/* Iconos Material, banner cookies y sheet móvil: estilos en features.css (no solo auth.css). */
import "../../styles/features.css";
import { getAppAssetBase } from "../../core/appAssets";
import { sendEvent } from "../../core/ga4";
import { getGoogleClientId, showGoogleOneTap } from "../../core/googleGsi";
import { UiIcon } from "../../ui/iconography";
import { Button, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
import { CookieConsentBanner } from "../consent/CookieConsentBanner";
import { GoogleSignInButton } from "./GoogleSignInButton";
import { useI18n } from "../../i18n";

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
  const { t } = useI18n();
  const [showMobileSheet, setShowMobileSheet] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const assetBase = useMemo(() => getAppAssetBase(), []);
  const googleClientId = useMemo(() => getGoogleClientId(), []);
  const loginVideoSrc = assetBase + "login_bg.mp4";
  const logoSrc = assetBase + "logo.png";
  const isLocalDevLoginEnabled =
    import.meta.env.DEV &&
    typeof window !== "undefined" &&
    window.location.hostname !== "cafesitoapp.com";
  const DEV_LOCAL_EMAIL_KEY = "cafesito_dev_session_email";
  const DEV_LOCAL_USER_ID_KEY = "cafesito_dev_internal_user_id";

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
        {t("login.joinCommunity")}
      </p>
      <div className="login-google-button-container">
        <GoogleSignInButton
          label={t("login.signUpGoogle")}
          loading={loading}
          disabled={!onGoogleLogin}
          onClick={onGoogleLogin ?? undefined}
        />
        {isLocalDevLoginEnabled ? (
          <Button
            variant="ghost"
            className="login-dev-button"
            type="button"
            onClick={() => {
              try {
                window.localStorage.setItem(DEV_LOCAL_EMAIL_KEY, "dev-local-1924119502@cafesito.local");
                window.localStorage.setItem(DEV_LOCAL_USER_ID_KEY, "1924119502");
              } catch {
                // ignore localStorage failures in private mode
              }
              window.location.href = "/home";
            }}
          >
            Entrar como usuario local (1924119502)
          </Button>
        ) : null}
      </div>
      <p className="login-sheet-terms">
        {t("login.termsPrefix")}{" "}
        <a href={assetBase + "legal/privacidad.html"} target="_blank" rel="noopener noreferrer" className="login-legal-link">{t("login.privacy")}</a>
        , las{" "}
        <a href={assetBase + "legal/condiciones.html"} target="_blank" rel="noopener noreferrer" className="login-legal-link">{t("login.terms")}</a>
        {" "}y{" "}
        <a href={assetBase + "legal/eliminacion-cuenta.html"} target="_blank" rel="noopener noreferrer" className="login-legal-link">{t("login.dataDeletion")}</a>.
      </p>
      {message ? <p className="login-hint">{message}</p> : null}
      {errorMessage ? <p className="login-error">{errorMessage}</p> : null}
    </>
  );

  return (
    <main className="login-gate" aria-label={t("login.mainAria")}>
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
              <p className="login-overline">{t("login.welcome")}</p>
              <h1 className="login-brand">CAFESITO</h1>
              <p className="login-subtitle">{t("login.subtitle")}</p>
            </header>

            <section className="login-feature-list" aria-label={t("login.benefits")}>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="checklist" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">{t("login.manageTitle")}</p>
                  <p className="login-feature-desc">{t("login.manageDesc")}</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="explore-filled" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">{t("login.exploreTitle")}</p>
                  <p className="login-feature-desc">{t("login.exploreDesc")}</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="coffee-filled" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">{t("login.brewTitle")}</p>
                  <p className="login-feature-desc">{t("login.brewDesc")}</p>
                </div>
              </article>
              <article className="login-feature-row">
                <span className="login-feature-icon" aria-hidden="true">
                  <UiIcon name="auto_graph" className="ui-icon" />
                </span>
                <div className="login-feature-copy">
                  <p className="login-feature-title">{t("login.trackTitle")}</p>
                  <p className="login-feature-desc">{t("login.trackDesc")}</p>
                </div>
              </article>
            </section>

            <Button variant="primary" className="login-start-button action-button--primary" onClick={() => { sendEvent("modal_open", { modal_id: "login_sheet" }); setShowMobileSheet(true); }}>
              {t("login.startNow")}
            </Button>
          </div>
        </section>

        <section className="login-desktop-auth" aria-label={t("login.desktopAccess")}>
          <img src={logoSrc} alt={t("login.logoAlt")} className="login-desktop-logo" loading="eager" decoding="async" />
          <div className="login-desktop-auth-content">{renderAuthContent()}</div>
        </section>
      </section>

      {showMobileSheet ? (
        <SheetOverlay className="login-sheet-overlay" onDismiss={() => { sendEvent("modal_close", { modal_id: "login_sheet" }); setShowMobileSheet(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "login_sheet" }); setShowMobileSheet(false); }}>
          <SheetCard className="login-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle />
            <div className="login-sheet-head">
              <button
                type="button"
                className="login-sheet-close"
                aria-label={t("common.close")}
                onClick={() => {
                  sendEvent("modal_close", { modal_id: "login_sheet" });
                  setShowMobileSheet(false);
                }}
              >
                <UiIcon name="close" className="ui-icon" />
              </button>
              <img src={logoSrc} alt="" aria-hidden="true" className="login-sheet-logo" />
            </div>
            {renderAuthContent()}
          </SheetCard>
        </SheetOverlay>
      ) : null}
      <CookieConsentBanner isAuthenticated={false} />
    </main>
  );
}
