import fs from "node:fs/promises";
import path from "node:path";
import sharp from "sharp";

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function fileExists(p) {
  try {
    await fs.access(p);
    return true;
  } catch {
    return false;
  }
}

function ogCanvas({ title, subtitle }) {
  const safeTitle = String(title ?? "").slice(0, 80);
  const safeSub = String(subtitle ?? "").slice(0, 120);
  // SVG -> PNG/JPG via sharp. 1200x630 recomendado.
  return Buffer.from(
    `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="1200" height="630">
  <defs>
    <linearGradient id="bg" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#6F4E37"/>
      <stop offset="1" stop-color="#212121"/>
    </linearGradient>
  </defs>
  <rect width="1200" height="630" fill="url(#bg)"/>
  <rect x="72" y="72" width="1056" height="486" rx="48" fill="rgba(255,255,255,0.08)"/>
  <text x="120" y="260" fill="#FFFFFF" font-family="Arial, Helvetica, sans-serif" font-size="72" font-weight="800">${escapeXml(
    safeTitle
  )}</text>
  <text x="120" y="340" fill="rgba(255,255,255,0.9)" font-family="Arial, Helvetica, sans-serif" font-size="36" font-weight="500">${escapeXml(
    safeSub
  )}</text>
  <text x="120" y="500" fill="rgba(255,255,255,0.9)" font-family="Arial, Helvetica, sans-serif" font-size="42" font-weight="700">cafesitoapp.com</text>
</svg>`
  );
}

function escapeXml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&apos;");
}

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

async function fetchBuffer(url) {
  const res = await fetch(url);
  if (!res.ok) throw new Error(`Fetch failed: ${res.status} ${res.statusText} (${url})`);
  const arr = await res.arrayBuffer();
  return Buffer.from(arr);
}

function fitImageToOg(input, opacity = 1) {
  // Área para imagen: 1200x630, dejamos padding y colocamos imagen a la derecha
  const targetW = 520;
  const targetH = 520;
  return sharp(input)
    .resize(targetW, targetH, { fit: "cover", position: "centre" })
    .modulate({ brightness: 1, saturation: 1.05 })
    .png()
    .toBuffer()
    .then((buf) => ({ input: buf, top: 55, left: 630, blend: "over", opacity }));
}

async function main() {
  const distDir = path.resolve(process.cwd(), "dist");
  const outDir = path.join(distDir, "og");
  await ensureDir(outDir);

  const searchSvg = ogCanvas({
    title: "Explorar cafés",
    subtitle: "Busca, filtra y descubre tu próximo café favorito"
  });
  await sharp(searchSvg).jpeg({ quality: 85 }).toFile(path.join(outDir, "search.jpg"));
  await sharp(searchSvg).webp({ quality: 82 }).toFile(path.join(outDir, "search.webp"));

  const loginSvg = ogCanvas({
    title: "Iniciar sesión",
    subtitle: "Accede para guardar favoritos, listas y tu diario"
  });
  await sharp(loginSvg).jpeg({ quality: 85 }).toFile(path.join(outDir, "login.jpg"));
  await sharp(loginSvg).webp({ quality: 82 }).toFile(path.join(outDir, "login.webp"));

  // Placeholder (opcional) por si se quiere usar en fallbacks
  const defaultSvg = ogCanvas({ title: "Cafesito", subtitle: "Comunidad de café" });
  await sharp(defaultSvg).jpeg({ quality: 85 }).toFile(path.join(outDir, "default.jpg"));
  await sharp(defaultSvg).webp({ quality: 82 }).toFile(path.join(outDir, "default.webp"));

  // Estructura para cafés
  const coffeeDir = path.join(outDir, "coffee");
  if (!(await fileExists(coffeeDir))) await ensureDir(coffeeDir);

  // Generar OG por café (best-effort). Si falla, no rompe el build.
  const supabaseUrl = await readEnvFallback("VITE_SUPABASE_URL");
  const supabaseAnonKey = await readEnvFallback("VITE_SUPABASE_ANON_KEY");
  const siteUrlRaw = (await readEnvFallback("VITE_SITE_URL")) || "https://cafesitoapp.com";
  const siteUrl = siteUrlRaw.replace(/\/+$/, "");

  if (supabaseUrl && supabaseAnonKey) {
    const endpoint = `${supabaseUrl}/rest/v1/coffees?select=id,nombre,marca,image_url&order=nombre.asc,id.asc&limit=5000`;
    const res = await fetch(endpoint, {
      headers: { apikey: supabaseAnonKey, Authorization: `Bearer ${supabaseAnonKey}` }
    });
    if (res.ok) {
      const coffees = await res.json();
      let generated = 0;
      for (const coffee of coffees) {
        const id = coffee?.id;
        const imageUrl = coffee?.image_url;
        if (!id || !imageUrl) continue;
        const outPath = path.join(coffeeDir, `${id}.jpg`);
        try {
          const imgBuf = await fetchBuffer(imageUrl.startsWith("http") ? imageUrl : `${siteUrl}${imageUrl}`);
          const base = ogCanvas({
            title: coffee?.nombre ? String(coffee.nombre).slice(0, 60) : "Café",
            subtitle: coffee?.marca ? String(coffee.marca).slice(0, 80) : "Cafesito"
          });
          const composed = await sharp(base)
            .composite([await fitImageToOg(imgBuf)])
            .jpeg({ quality: 84 })
            .toBuffer();
          await fs.writeFile(outPath, composed);
          const composedWebp = await sharp(base)
            .composite([await fitImageToOg(imgBuf)])
            .webp({ quality: 82 })
            .toBuffer();
          await fs.writeFile(path.join(coffeeDir, `${id}.webp`), composedWebp);
          generated++;
        } catch {
          // best-effort
        }
      }
      console.log(`[generate-og-images] Generated generic + ${generated} coffee OG images in dist/og/`);
      return;
    }
  }

  console.log("[generate-og-images] Generated generic OG images in dist/og/ (coffee OG skipped)");
}

main().catch((err) => {
  console.error("[generate-og-images]", err);
  process.exitCode = 1;
});

