import React, { useState, useCallback, useEffect } from "react";
import { getConsent, setConsent, type CookieConsent } from "../../core/consent";
import { initGa4 } from "../../core/ga4";
import { getAppAssetBase } from "../../core/appAssets";
import { Button } from "../../ui/components";

const PRIVACY_URL = "legal/privacidad.html";

/**
 * Banner de consentimiento de cookies.
 * - En páginas públicas (no logado) se muestra la primera vez (consent === null).
 * - "Solo esenciales": no se cargan analíticas (GTM).
 * - "Aceptar todas": se cargan GTM y se envían analíticas.
 * - Si el usuario inicia sesión, se asume consentimiento completo (all) sin mostrar banner.
 */
export function CookieConsentBanner({ isAuthenticated }: { isAuthenticated: boolean }): React.ReactElement | null {
  const [consent, setConsentState] = useState<CookieConsent>(() => getConsent());
  const assetBase = getAppAssetBase();
  const privacyHref = assetBase + PRIVACY_URL;

  // Si el usuario está autenticado y aún no hay consentimiento completo, asumir "all" automáticamente.
  useEffect(() => {
    if (!isAuthenticated) return;
    if (consent === "all") return;
    setConsent("all");
    setConsentState("all");
    initGa4();
  }, [isAuthenticated, consent]);

  const handleEssential = useCallback(() => {
    setConsent("essential");
    setConsentState("essential");
  }, []);

  const handleAcceptAll = useCallback(() => {
    setConsent("all");
    setConsentState("all");
    initGa4();
  }, []);

  if (consent !== null) return null;

  return (
    <aside
      className="cookie-consent-banner"
      role="dialog"
      aria-label="Aviso de cookies"
      aria-describedby="cookie-consent-description"
    >
      <div className="cookie-consent-banner-inner">
        <h2 className="cookie-consent-banner-title">Cookies</h2>
        <p id="cookie-consent-description" className="cookie-consent-banner-text">
          Utilizamos las cookies necesarias para garantizar el correcto funcionamiento de la aplicación y las cookies
          de análisis para mejorar tu experiencia. Puedes aceptar solo las necesarias o permitir todas.
        </p>
        <p className="cookie-consent-banner-legal">
          <a href={privacyHref} target="_blank" rel="noopener noreferrer" className="cookie-consent-banner-link">
            Más información en la Política de Privacidad
          </a>
        </p>
        <div className="cookie-consent-banner-actions">
          <Button variant="ghost" type="button" onClick={handleEssential} className="cookie-consent-btn-secondary">
            Solo esenciales
          </Button>
          <Button variant="primary" type="button" onClick={handleAcceptAll}>
            Aceptar todas
          </Button>
        </div>
      </div>
    </aside>
  );
}
