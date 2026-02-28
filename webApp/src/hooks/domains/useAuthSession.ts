import { useCallback, useEffect, useState } from "react";
import { getAppRootPath } from "../../core/routing";
import { getSupabaseClient, supabaseConfigError } from "../../supabase";

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
    void supabase.auth.getSession().then(({ data, error }) => {
      if (!mounted) return;
      if (error) setAuthError(error.message);
      setSessionEmail(data.session?.user?.email ?? null);
      setAuthReady(true);
    });

    const { data } = supabase.auth.onAuthStateChange((_event, session) => {
      setSessionEmail(session?.user?.email ?? null);
      setAuthReady(true);
      setAuthBusy(false);
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
      // En producción usar VITE_SITE_URL (definida en CI) como URL base completa (incl. path) para no depender de
      // window.location: en PWA "Añadir a página de inicio" (iOS) el pathname puede ser "/" y el redirect iría
      // a /timeline en la raíz → 404/500. VITE_SITE_URL debe ser la URL pública de la app (ej. https://cafesitoapp.com/cafesito-web/app).
      const siteUrl = (import.meta.env.VITE_SITE_URL as string | undefined)?.trim();
      const base = siteUrl
        ? siteUrl.replace(/\/+$/, "")
        : `${window.location.origin}${(getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || ""}`.replace(/\/+$/, "");
      const redirectTo = `${base}/timeline`;
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
