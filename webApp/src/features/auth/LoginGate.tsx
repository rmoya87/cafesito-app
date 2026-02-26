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
  return (
    <main className="login-gate" aria-label="Inicio de sesion">
      <section className="login-card">
        <p className="login-brand">CAFESITO</p>
        <h1 className="login-title">Inicia sesion</h1>
        <p className="login-copy">Accede con Google para continuar.</p>
        <button
          type="button"
          className="google-login-button"
          onClick={onGoogleLogin}
          disabled={loading || !onGoogleLogin}
        >
          <span className="google-dot" aria-hidden="true" />
          {loading ? "Conectando..." : "Continuar con Google"}
        </button>
        {message ? <p className="login-hint">{message}</p> : null}
        {errorMessage ? <p className="login-error">{errorMessage}</p> : null}
      </section>
    </main>
  );
}



