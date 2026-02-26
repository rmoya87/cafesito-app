import { useCallback, useEffect, useState } from "react";
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
    if (supabaseConfigError) return;
    setAuthBusy(true);
    setAuthError(null);
    try {
      const supabase = getSupabaseClient();
      const { error } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: {
          redirectTo: `${window.location.origin}${window.location.pathname}`
        }
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
