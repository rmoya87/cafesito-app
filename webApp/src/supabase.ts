import { createClient, type SupabaseClient } from "@supabase/supabase-js";

declare global {
  interface Window {
    __SUPABASE_CONFIG__?: { url: string; anonKey: string };
  }
}

const fromEnv = {
  url: import.meta.env.VITE_SUPABASE_URL as string | undefined,
  anonKey: import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined
};
const fromWindow = typeof window !== "undefined" ? window.__SUPABASE_CONFIG__ : undefined;
const supabaseUrl = (fromEnv.url || fromWindow?.url)?.trim() || undefined;
const supabaseAnonKey = (fromEnv.anonKey || fromWindow?.anonKey)?.trim() || undefined;

export const supabaseConfigError =
  !supabaseUrl || !supabaseAnonKey
    ? "Faltan VITE_SUPABASE_URL y/o VITE_SUPABASE_ANON_KEY. En producción el build suele hacerse sin .env (p. ej. en CI). Añade las variables en el pipeline de build o inyecta window.__SUPABASE_CONFIG__ en el index.html (ver DEPLOY-IONOS.md)."
    : null;

let supabaseClient: SupabaseClient | null = null;

if (supabaseUrl && supabaseAnonKey) {
  supabaseClient = createClient(supabaseUrl, supabaseAnonKey, {
    auth: {
      flowType: "pkce",
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true
    }
  });
}

export function getSupabaseClient(): SupabaseClient {
  if (!supabaseClient) {
    throw new Error(supabaseConfigError ?? "No se pudo inicializar Supabase");
  }
  return supabaseClient;
}
