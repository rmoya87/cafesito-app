import { useCallback, useEffect, useState } from "react";
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

    // Si el callback OAuth devolvió error en el hash (ej. 500, server_error), mostrarlo para que el contenedor redirija a /timeline
    const hash = window.location.hash?.replace(/^#/, "") || "";
    const params = new URLSearchParams(hash);
    const hashError = params.get("error") || params.get("error_description");
    if (hashError) {
      setAuthError(decodeURIComponent(hashError));
      window.history.replaceState(null, "", window.location.pathname + window.location.search);
    }

    const supabase = getSupabaseClient();
    let mounted = true;

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
        setAuthReady(true);
        return;
      }
      await applySession(data.session);
      if (mounted) setAuthReady(true);
    });

    const { data } = supabase.auth.onAuthStateChange(async (_event, session) => {
      await applySession(session);
      if (mounted) {
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
      // Redirect tras login: volver siempre a la misma URL donde el usuario abrió la app (nunca a producción si estás en local o IP).
      // Solo usar cafesitoapp.com cuando el host es exactamente ese; en cualquier otro caso (localhost, 192.168.x.x, etc.) usar origin actual.
      // En Supabase → Authentication → URL Configuration añade cada Redirect URL que uses (ej. https://192.168.1.123:4173/timeline para probar en red local).
      const hostname = typeof window !== "undefined" ? window.location.hostname : "";
      const origin = typeof window !== "undefined" ? window.location.origin : "";
      const base =
        hostname === "cafesitoapp.com"
          ? "https://cafesitoapp.com"
          : origin || "https://cafesitoapp.com";
      const redirectTo = `${base.replace(/\/+$/, "")}/timeline`;
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

  const requestLogin = useCallback(() => {
    setAuthError(null);
    setShowAuthPrompt(true);
  }, []);

  useEffect(() => {
    if (sessionEmail) setShowAuthPrompt(false);
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
    requestLogin
  };
}
