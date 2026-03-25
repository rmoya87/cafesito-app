import { useCallback, useEffect, useState } from "react";
import { getAppRootPath } from "../../core/routing";
import { getSupabaseClient, supabaseConfigError } from "../../supabase";

const SESSION_ACTIVITY_TTL_MS = 5 * 24 * 60 * 60 * 1000; // 5 días sin acceso → cierre de sesión
const STORAGE_KEY_LAST_ACTIVITY = "cafesito_session_last_activity";

function getLastActivity(): number | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY_LAST_ACTIVITY);
    if (raw == null) return null;
    const t = parseInt(raw, 10);
    return Number.isFinite(t) ? t : null;
  } catch {
    return null;
  }
}

function setLastActivity(now: number): void {
  try {
    localStorage.setItem(STORAGE_KEY_LAST_ACTIVITY, String(now));
  } catch {
    /* ignore */
  }
}

function clearLastActivity(): void {
  try {
    localStorage.removeItem(STORAGE_KEY_LAST_ACTIVITY);
  } catch {
    /* ignore */
  }
}

function isSessionExpired(lastActivity: number | null, now: number): boolean {
  if (lastActivity == null) return false; // primera vez con sesión: no expirado, se renovará al guardar now
  return now - lastActivity > SESSION_ACTIVITY_TTL_MS;
}

type UseAuthSessionResult = {
  authReady: boolean;
  sessionEmail: string | null;
  authBusy: boolean;
  authError: string | null;
  showAuthPrompt: boolean;
  setAuthError: (value: string | null) => void;
  setShowAuthPrompt: (value: boolean) => void;
  handleGoogleLogin: () => Promise<void>;
  /** Recibe el ID token de Google (GIS) y firma en Supabase sin redirección. */
  handleGoogleCredential: (idToken: string) => Promise<void>;
  requestLogin: () => void;
};

export function useAuthSession(): UseAuthSessionResult {
  const [authReady, setAuthReady] = useState(false);
  const [sessionEmail, setSessionEmail] = useState<string | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [showAuthPrompt, setShowAuthPrompt] = useState(false);

  useEffect(() => {
    if (supabaseConfigError) {
      setAuthError(supabaseConfigError);
      setAuthReady(true);
      return;
    }

    // Si el callback OAuth devolvió error en el hash (ej. 500, server_error), mostrarlo para que el contenedor redirija a /home
    const hash = window.location.hash?.replace(/^#/, "") || "";
    const params = new URLSearchParams(hash);
    const hashError = params.get("error") || params.get("error_description");
    if (hashError) {
      setAuthError(decodeURIComponent(hashError));
      window.history.replaceState(null, "", window.location.pathname + window.location.search);
    }

    const supabase = getSupabaseClient();
    let mounted = true;
    let initialCheckDone = false;

    async function applySession(session: { user: { email?: string | null } } | null) {
      const now = Date.now();
      if (!session?.user?.email) {
        clearLastActivity();
        if (mounted) setSessionEmail(null);
        return;
      }
      const lastActivity = getLastActivity();
      if (isSessionExpired(lastActivity, now)) {
        await supabase.auth.signOut();
        clearLastActivity();
        if (mounted) setSessionEmail(null);
        return;
      }
      setLastActivity(now);
      if (mounted) setSessionEmail(session.user.email ?? null);
    }

    void supabase.auth.getSession().then(async ({ data, error }) => {
      if (!mounted) return;
      if (error) {
        setAuthError(error.message);
        initialCheckDone = true;
        setAuthReady(true);
        return;
      }
      await applySession(data.session);
      if (mounted) {
        initialCheckDone = true;
        setAuthReady(true);
      }
    });

    const { data } = supabase.auth.onAuthStateChange(async (_event, session) => {
      await applySession(session);
      if (mounted && initialCheckDone) {
        setAuthReady(true);
        setAuthBusy(false);
      }
    });

    return () => {
      mounted = false;
      data.subscription.unsubscribe();
    };
  }, []);

  const handleGoogleLogin = useCallback(async () => {
    if (supabaseConfigError) {
      setAuthError(supabaseConfigError);
      return;
    }
    setAuthBusy(true);
    setAuthError(null);
    try {
      const supabase = getSupabaseClient();
      // Redirect tras login: usar la raíz del sitio para evitar 500 en Apache (Ionos) con /home?code=...
      // En Supabase → Authentication → URL Configuration añade cada Redirect URL (raíz y, si pruebas en red local, esa IP).
      const hostname = typeof window !== "undefined" ? window.location.hostname : "";
      const origin = typeof window !== "undefined" ? window.location.origin : "";
      const base =
        hostname === "cafesitoapp.com"
          ? "https://cafesitoapp.com"
          : origin || "https://cafesitoapp.com";
      const redirectTo = base.replace(/\/+$/, "") + "/";
      const { error } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: { redirectTo }
      });
      if (error) throw error;
    } catch (error) {
      setAuthError((error as Error).message);
      setAuthBusy(false);
    }
  }, []);

  const handleGoogleCredential = useCallback(async (idToken: string) => {
    if (supabaseConfigError) {
      setAuthError(supabaseConfigError);
      return;
    }
    setAuthBusy(true);
    setAuthError(null);
    try {
      const supabase = getSupabaseClient();
      // Supabase → Google: con "Skip nonce checks" no hace falta enviar nonce.
      const { error } = await supabase.auth.signInWithIdToken({
        provider: "google",
        token: idToken
      });
      if (error) throw error;
    } catch (error) {
      setAuthError((error as Error).message);
    } finally {
      setAuthBusy(false);
    }
  }, []);

  const requestLogin = useCallback(() => {
    setAuthError(null);
    setShowAuthPrompt(true);
  }, []);

  useEffect(() => {
    if (sessionEmail) setShowAuthPrompt(false);
  }, [sessionEmail]);

  /**
   * OAuth PKCE: Supabase redirige a la URL de `redirectTo` con `?code=`.
   * Tras tener sesión, normalizamos a `{getAppRootPath}/home` (paridad con `AppContainer` ante `authError`;
   * soporta raíz `/` y despliegue con base path). One Tap (`signInWithIdToken`) no pasa por aquí.
   */
  useEffect(() => {
    if (!sessionEmail) return;
    const search = window.location.search || "";
    if (!search.includes("code=")) return;
    const pathnameRaw = window.location.pathname;
    const pathname = pathnameRaw.replace(/\/+$/, "") || "/";
    const base = (getAppRootPath(pathnameRaw) || "/").replace(/\/+$/, "") || "/";
    const atAppEntry = pathname === base || pathname === "/" || pathname === "";
    if (!atAppEntry) return;
    const homePath = (base === "/" ? "/home" : `${base}/home`).replace(/\/+/g, "/");
    window.history.replaceState(null, "", `${homePath}${window.location.hash}`);
  }, [sessionEmail]);

  return {
    authReady,
    sessionEmail,
    authBusy,
    authError,
    showAuthPrompt,
    setAuthError,
    setShowAuthPrompt,
    handleGoogleLogin,
    handleGoogleCredential,
    requestLogin
  };
}
