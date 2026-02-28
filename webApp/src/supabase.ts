import { createClient, type SupabaseClient } from "@supabase/supabase-js";

declare global {
  interface Window {
    __SUPABASE_CONFIG__?: { url: string; anonKey: string };
  }
}

declare const __SUPABASE_URL__: string;
declare const __SUPABASE_ANON_KEY__: string;

const fromDefine = { url: typeof __SUPABASE_URL__ !== "undefined" ? __SUPABASE_URL__ : "", anonKey: typeof __SUPABASE_ANON_KEY__ !== "undefined" ? __SUPABASE_ANON_KEY__ : "" };
const fromEnv = {
  url: import.meta.env.VITE_SUPABASE_URL as string | undefined,
  anonKey: import.meta.env.VITE_SUPABASE_ANON_KEY as string | undefined
};
const fromWindow = typeof window !== "undefined" ? window.__SUPABASE_CONFIG__ : undefined;
// Prioridad: define (inyectado por Vite desde .env) > window > import.meta.env
let supabaseUrl = (fromDefine.url?.trim() || fromWindow?.url || fromEnv.url)?.trim() || undefined;
let supabaseAnonKey = (fromDefine.anonKey?.trim() || fromWindow?.anonKey || fromEnv.anonKey)?.trim() || undefined;

if (supabaseUrl && /your_project|YOUR_PROJECT|xxxxx/i.test(supabaseUrl)) {
  console.error("Supabase: URL con placeholder detectada. Usa la URL real en webApp/.env y reinicia npm run dev.");
  supabaseUrl = undefined;
}

const isDev = import.meta.env.DEV;
export const supabaseConfigError =
  !supabaseUrl || !supabaseAnonKey
    ? isDev
      ? "Faltan VITE_SUPABASE_URL y/o VITE_SUPABASE_ANON_KEY. Revisa webApp/.env y reinicia el servidor (npm run dev)."
      : "Faltan VITE_SUPABASE_URL y/o VITE_SUPABASE_ANON_KEY. Configúralas en el pipeline de build o inyecta window.__SUPABASE_CONFIG__ (ver DEPLOY-IONOS.md)."
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
