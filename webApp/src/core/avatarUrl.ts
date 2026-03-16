import { getSupabaseClient } from "../supabase";

/**
 * Devuelve URL absoluta del avatar.
 * Si ya es http(s) se devuelve tal cual.
 * Si es ruta relativa se resuelve con Supabase Storage (bucket avatars).
 */
export function resolveAvatarUrl(avatarUrl: string | null | undefined): string | null {
  const raw = avatarUrl && String(avatarUrl).trim();
  if (!raw) return null;
  if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
  try {
    const path = raw.startsWith("/") ? raw.slice(1) : raw;
    const { data } = getSupabaseClient().storage.from("avatars").getPublicUrl(path);
    return data?.publicUrl ?? raw;
  } catch {
    return raw;
  }
}
