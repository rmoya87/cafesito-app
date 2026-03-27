import fs from "node:fs/promises";
import path from "node:path";
import sharp from "sharp";

const OG_CACHE_VERSION = 1;

async function ensureDir(dir) {
  await fs.mkdir(dir, { recursive: true });
}

async function fileExists(filePath) {
  try {
    await fs.access(filePath);
    return true;
  } catch {
    return false;
  }
}

async function readJsonFile(filePath, fallback) {
  try {
    const raw = await fs.readFile(filePath, "utf8");
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

function ogCanvas({ title, subtitle }) {
  const safeTitle = String(title ?? "").slice(0, 80);
  const safeSubtitle = String(subtitle ?? "").slice(0, 120);
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
    safeSubtitle
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
      const currentKey = trimmed.slice(0, idx).trim();
      if (currentKey !== key) continue;
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
  const targetWidth = 520;
  const targetHeight = 520;
  return sharp(input)
    .resize(targetWidth, targetHeight, { fit: "cover", position: "centre" })
    .modulate({ brightness: 1, saturation: 1.05 })
    .png()
    .toBuffer()
    .then((buffer) => ({ input: buffer, top: 55, left: 630, blend: "over", opacity }));
}

function createCoffeeSignature(coffee) {
  return JSON.stringify({
    version: OG_CACHE_VERSION,
    id: String(coffee?.id ?? ""),
    nombre: String(coffee?.nombre ?? ""),
    marca: String(coffee?.marca ?? ""),
    image_url: String(coffee?.image_url ?? "")
  });
}

async function copyIfExists(sourcePath, targetPath) {
  if (!(await fileExists(sourcePath))) return false;
  await ensureDir(path.dirname(targetPath));
  await fs.copyFile(sourcePath, targetPath);
  return true;
}

async function writeGenericOgImages(outDir) {
  const searchSvg = ogCanvas({
    title: "Explorar cafes",
    subtitle: "Busca, filtra y descubre tu proximo cafe favorito"
  });
  await sharp(searchSvg).jpeg({ quality: 85 }).toFile(path.join(outDir, "search.jpg"));
  await sharp(searchSvg).webp({ quality: 82 }).toFile(path.join(outDir, "search.webp"));

  const loginSvg = ogCanvas({
    title: "Iniciar sesion",
    subtitle: "Accede para guardar favoritos, listas y tu diario"
  });
  await sharp(loginSvg).jpeg({ quality: 85 }).toFile(path.join(outDir, "login.jpg"));
  await sharp(loginSvg).webp({ quality: 82 }).toFile(path.join(outDir, "login.webp"));

  const defaultSvg = ogCanvas({ title: "Cafesito", subtitle: "Comunidad de cafe" });
  await sharp(defaultSvg).jpeg({ quality: 85 }).toFile(path.join(outDir, "default.jpg"));
  await sharp(defaultSvg).webp({ quality: 82 }).toFile(path.join(outDir, "default.webp"));
}

async function main() {
  const distDir = path.resolve(process.cwd(), "dist");
  const outDir = path.join(distDir, "og");
  const coffeeOutDir = path.join(outDir, "coffee");
  const cacheDir = path.resolve(process.cwd(), ".cache", "og-images");
  const cacheCoffeeDir = path.join(cacheDir, "coffee");
  const cacheManifestPath = path.join(cacheDir, "manifest.json");

  await ensureDir(outDir);
  await ensureDir(coffeeOutDir);
  await ensureDir(cacheCoffeeDir);
  await writeGenericOgImages(outDir);

  const cacheManifest = await readJsonFile(cacheManifestPath, { version: OG_CACHE_VERSION, coffees: {} });
  const cacheEntries = cacheManifest.version === OG_CACHE_VERSION && cacheManifest.coffees ? cacheManifest.coffees : {};
  const nextCacheEntries = {};

  const supabaseUrl = await readEnvFallback("VITE_SUPABASE_URL");
  const supabaseAnonKey = await readEnvFallback("VITE_SUPABASE_ANON_KEY");
  const siteUrlRaw = (await readEnvFallback("VITE_SITE_URL")) || "https://cafesitoapp.com";
  const siteUrl = siteUrlRaw.replace(/\/+$/, "");

  if (supabaseUrl && supabaseAnonKey) {
    const endpoint = `${supabaseUrl}/rest/v1/coffees?select=id,nombre,marca,image_url&order=nombre.asc,id.asc&limit=5000`;
    const res = await fetch(endpoint, {
      headers: {
        apikey: supabaseAnonKey,
        Authorization: `Bearer ${supabaseAnonKey}`
      }
    });

    if (res.ok) {
      const coffees = await res.json();
      let generated = 0;
      let restored = 0;

      for (const coffee of coffees) {
        const id = String(coffee?.id ?? "").trim();
        const imageUrl = String(coffee?.image_url ?? "").trim();
        if (!id || !imageUrl) continue;

        const signature = createCoffeeSignature(coffee);
        const cacheEntry = cacheEntries[id] ?? null;
        const cacheJpgPath = path.join(cacheCoffeeDir, `${id}.jpg`);
        const cacheWebpPath = path.join(cacheCoffeeDir, `${id}.webp`);
        const outJpgPath = path.join(coffeeOutDir, `${id}.jpg`);
        const outWebpPath = path.join(coffeeOutDir, `${id}.webp`);

        if (
          cacheEntry?.signature === signature &&
          (await copyIfExists(cacheJpgPath, outJpgPath)) &&
          (await copyIfExists(cacheWebpPath, outWebpPath))
        ) {
          nextCacheEntries[id] = cacheEntry;
          restored++;
          continue;
        }

        try {
          const imgBuf = await fetchBuffer(imageUrl.startsWith("http") ? imageUrl : `${siteUrl}${imageUrl}`);
          const base = ogCanvas({
            title: coffee?.nombre ? String(coffee.nombre).slice(0, 60) : "Cafe",
            subtitle: coffee?.marca ? String(coffee.marca).slice(0, 80) : "Cafesito"
          });
          const overlay = await fitImageToOg(imgBuf);
          const jpgBuffer = await sharp(base).composite([overlay]).jpeg({ quality: 84 }).toBuffer();
          const webpBuffer = await sharp(base).composite([overlay]).webp({ quality: 82 }).toBuffer();

          await fs.writeFile(outJpgPath, jpgBuffer);
          await fs.writeFile(outWebpPath, webpBuffer);
          await fs.writeFile(cacheJpgPath, jpgBuffer);
          await fs.writeFile(cacheWebpPath, webpBuffer);

          nextCacheEntries[id] = { signature };
          generated++;
        } catch {
          // best-effort
        }
      }

      await fs.writeFile(
        cacheManifestPath,
        JSON.stringify({ version: OG_CACHE_VERSION, coffees: nextCacheEntries }, null, 2),
        "utf8"
      );

      console.log(
        `[generate-og-images] Generated generic + ${generated} coffee OG images in dist/og/ (restored from cache: ${restored}).`
      );
      return;
    }
  }

  console.log("[generate-og-images] Generated generic OG images in dist/og/ (coffee OG skipped)");
}

main().catch((err) => {
  console.error("[generate-og-images]", err);
  process.exitCode = 1;
});
