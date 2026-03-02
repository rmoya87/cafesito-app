import { Button, IconButton, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";

export function AuthPromptOverlay({
  open,
  authBusy,
  authError,
  onClose,
  onGoogleLogin
}: {
  open: boolean;
  authBusy: boolean;
  authError: string | null;
  onClose: () => void;
  onGoogleLogin: () => void;
}) {
  if (!open) return null;

  return (
    <SheetOverlay className="auth-prompt-overlay" role="dialog" aria-modal="true" aria-label="Acceso" onDismiss={onClose} onClick={onClose}>
      <SheetCard className="auth-prompt-card" onClick={(event) => event.stopPropagation()}>
        <SheetHandle aria-hidden="true" />
        <IconButton className="auth-prompt-close" aria-label="Cerrar" onClick={onClose}>
          <UiIcon name="close" className="ui-icon" />
        </IconButton>
        <div className="auth-prompt-avatar" aria-hidden="true">
          <img src="/logo.png" alt="" loading="lazy" decoding="async" />
        </div>
        <p className="auth-prompt-copy">
          Únete a la comunidad del café para descubrir, elaborar y compartir tu pasión.
        </p>
        <Button
          variant="primary"
          className="auth-prompt-primary"
          disabled={authBusy}
          onClick={onGoogleLogin}
        >
          <span className="auth-prompt-google-g" aria-hidden="true">G</span>
          <span>{authBusy ? "Conectando..." : "Continuar con Google"}</span>
        </Button>
        {authError ? <p className="auth-prompt-error">{authError}</p> : null}
      </SheetCard>
    </SheetOverlay>
  );
}



