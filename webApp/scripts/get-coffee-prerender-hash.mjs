/**
 * Obtiene un hash de la lista de cafés en Supabase para usar como clave de caché.
 * Solo stdout: el hash (ej. para GITHUB_OUTPUT).
 */
import crypto from "node:crypto";
import path from "node:path";
import fs from "node:fs/promises";

async function readEnvFallback(key) {
  if (process.env[key]) return process.env[key];
  try {
    const envPath = path.resolve(process.cwd(), ".env");
    const raw = await fs.readFile(envPath, "utf8");
    for (const line of raw.split(/\r?\n/)) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith("#")) continue;
      const idx = trimmed.indexOf("=");
      if (idx <= 0) continue;
      const k = trimmed.slice(0, idx).trim();
      if (k !== key) continue;
      return trimmed.slice(idx + 1).trim().replace(/^"|"$/g, "");
    }
  } catch {
    // noop
  }
  return "";
}

async function main() {
  const supabaseUrl = await readEnvFallback("VITE_SUPABASE_URL");
  const supabaseAnonKey = await readEnvFallback("VITE_SUPABASE_ANON_KEY");
  if (!supabaseUrl || !supabaseAnonKey) {
    console.log("no-supabase");
    return;
  }
  const endpoint = `${supabaseUrl}/rest/v1/coffees?select=id,nombre,marca&order=id.asc&limit=500`;
  const res = await fetch(endpoint, {
    headers: {
      apikey: supabaseAnonKey,
      Authorization: `Bearer ${supabaseAnonKey}`
    }
  });
  if (!res.ok) {
    console.log("fetch-error");
    return;
  }
  const coffees = await res.json();
  const payload = JSON.stringify(coffees.map((c) => [c.id, c.nombre, c.marca]));
  const hash = crypto.createHash("sha256").update(payload).digest("hex").slice(0, 16);
  console.log(hash);
}

main().catch(() => console.log("error"));
