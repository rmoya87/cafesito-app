import { useEffect, useMemo } from "react";
import { getGoogleClientId, showGoogleOneTap } from "../../core/googleGsi";
import { IconButton, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";
import { GoogleSignInButton } from "./GoogleSignInButton";

export function AuthPromptOverlay({
  open,
  authBusy,
  authError,
  onClose,
  onGoogleLogin,
  onGoogleCredential
}: {
  open: boolean;
  authBusy: boolean;
  authError: string | null;
  onClose: () => void;
  onGoogleLogin: () => void;
  onGoogleCredential?: (idToken: string) => void;
}) {
  const googleClientId = useMemo(() => getGoogleClientId(), []);

  // One Tap en el overlay: sugiere "Continuar como [email]" si hay sesión en Google (solo con red para evitar ERR_NAME_NOT_RESOLVED)
  useEffect(() => {
    if (!open || !googleClientId || !onGoogleCredential || typeof navigator !== "undefined" && !navigator.onLine) return;
    const cancel = showGoogleOneTap(googleClientId, (token) => onGoogleCredential(token));
    return cancel;
  }, [open, googleClientId, onGoogleCredential]);

  if (!open) return null;

  return (
    <SheetOverlay className="auth-prompt-overlay" role="dialog" aria-modal="true" aria-label="Acceso" onDismiss={onClose} onClick={onClose}>
      <SheetCard className="auth-prompt-card" onClick={(event) => event.stopPropagation()}>
        <SheetHandle aria-hidden="true" />
        <div className="auth-prompt-head">
          <span className="auth-prompt-head-spacer" aria-hidden="true" />
          <div className="auth-prompt-avatar" aria-hidden="true">
            <img src="/logo.png" alt="" loading="lazy" decoding="async" />
          </div>
          <IconButton className="auth-prompt-close" aria-label="Cerrar" onClick={onClose}>
            <UiIcon name="close" className="ui-icon" />
          </IconButton>
        </div>
        <p className="auth-prompt-copy">
          Únete a la comunidad del café para descubrir, elaborar y compartir tu pasión.
        </p>
        <div className="auth-prompt-google-slot">
          <GoogleSignInButton
            label="Registrarse con Google"
            loading={authBusy}
            onClick={onGoogleLogin}
          />
        </div>
        {authError ? <p className="auth-prompt-error">{authError}</p> : null}
      </SheetCard>
    </SheetOverlay>
  );
}



