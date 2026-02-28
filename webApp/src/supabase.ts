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
let supabaseUrl = (fromEnv.url || fromWindow?.url)?.trim() || undefined;
let supabaseAnonKey = (fromEnv.anonKey || fromWindow?.anonKey)?.trim() || undefined;

if (supabaseUrl && /your_project|YOUR_PROJECT/i.test(supabaseUrl)) {
  console.error("Supabase: la URL contiene el placeholder. Revisa webApp/.env y usa la URL real de tu proyecto (ej. https://xxxxx.supabase.co). Reinicia el servidor de desarrollo (npm run dev).");
  supabaseUrl = undefined;
}

const isDev = import.meta.env.DEV;
export const supabaseConfigError =
  !supabaseUrl || !supabaseAnonKey
    ? isDev
      ? "Faltan VITE_SUPABASE_URL y/o VITE_SUPABASE_ANON_KEY. Crea o revisa webApp/.env (con la URL y anon key de Supabase), guarda y reinicia el servidor: desde webApp/ ejecuta npm run dev."
      : "Faltan VITE_SUPABASE_URL y/o VITE_SUPABASE_ANON_KEY. En producción: añade esas variables en el pipeline de build (p. ej. GitHub Actions) o inyecta window.__SUPABASE_CONFIG__ en el index.html (ver DEPLOY-IONOS.md)."
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
