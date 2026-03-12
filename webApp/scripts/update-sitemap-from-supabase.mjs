import fs from "node:fs/promises";
import path from "node:path";

function normalizeText(value) {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

function slugifyText(value) {
  return normalizeText(value)
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
}

function toCoffeeSlug(name, brand, forceBrand = false) {
  const baseFromName = slugifyText(name);
  if (forceBrand && brand) {
    const forced = slugifyText(`${name} ${brand}`);
    return forced || baseFromName || "cafe";
  }
  if (baseFromName.length > 10) return baseFromName;
  const baseWithBrand = slugifyText(`${name} ${brand ?? ""}`);
  return baseWithBrand || baseFromName || "cafe";
}

function ensureTrailingSlash(value) {
  return value.endsWith("/") ? value : `${value}/`;
}

function ensureLeadingSlash(value) {
  if (!value) return "/";
  return value.startsWith("/") ? value : `/${value}`;
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

async function main() {
  const distDir = path.resolve(process.cwd(), "dist");
  const supabaseUrl = await readEnvFallback("VITE_SUPABASE_URL");
  const supabaseAnonKey = await readEnvFallback("VITE_SUPABASE_ANON_KEY");
  const siteUrlRaw = (await readEnvFallback("VITE_SITE_URL")) || "https://cafesitoapp.com";
  const siteUrl = siteUrlRaw.replace(/\/+$/, "");

  if (!supabaseUrl || !supabaseAnonKey) {
    throw new Error("Missing VITE_SUPABASE_URL/VITE_SUPABASE_ANON_KEY");
  }

  const endpoint = `${supabaseUrl}/rest/v1/coffees?select=id,nombre,marca&order=nombre.asc,id.asc&limit=5000`;
  const res = await fetch(endpoint, {
    headers: {
      apikey: supabaseAnonKey,
      Authorization: `Bearer ${supabaseAnonKey}`
    }
  });
  if (!res.ok) throw new Error(`Failed to fetch coffees for sitemap: ${res.status} ${res.statusText}`);
  const coffees = await res.json();

  coffees.sort((a, b) => {
    const nameCmp = String(a.nombre ?? "").localeCompare(String(b.nombre ?? ""));
    if (nameCmp !== 0) return nameCmp;
    return String(a.id ?? "").localeCompare(String(b.id ?? ""));
  });
  const nameCounts = new Map();
  for (const coffee of coffees) {
    const key = normalizeText(coffee.nombre);
    nameCounts.set(key, (nameCounts.get(key) ?? 0) + 1);
  }
  const slugCounts = new Map();
  const urls = [];
  for (const coffee of coffees) {
    const hasDuplicatedName = (nameCounts.get(normalizeText(coffee.nombre)) ?? 0) > 1;
    const base = toCoffeeSlug(coffee.nombre, coffee.marca, hasDuplicatedName);
    const count = (slugCounts.get(base) ?? 0) + 1;
    slugCounts.set(base, count);
    const slug = count > 1 ? `${base}-${count}` : base;
    urls.push(`${siteUrl}/coffee/${slug}/`);
  }

  const siteUrlObject = new URL(siteUrl);
  const appBasePath = (() => {
    const p = siteUrlObject.pathname.replace(/\/+$/, "");
    return p && p !== "/" ? ensureLeadingSlash(p) : "/";
  })();
  const staticRoot = appBasePath === "/" ? "/" : ensureTrailingSlash(appBasePath);
  const staticPaths = [
    staticRoot,
    `${staticRoot}legal/privacidad.html`,
    `${staticRoot}legal/condiciones.html`,
    `${staticRoot}legal/eliminacion-cuenta.html`
  ];
  const staticUrls = staticPaths.map((p) => `${siteUrl}${p}`);
  staticUrls.splice(1, 0, `${new URL(siteUrl).origin}/search`);

  /** Máximo de URLs por sitemap de cafés; Google permite 50.000 pero dividir en 500 mejora indexación en Search Console. */
  const COFFEE_SITEMAP_MAX_URLS = 500;

  const urlSetXml = (allUrls) =>
    `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${allUrls
      .map((url) => `  <url><loc>${url}</loc></url>`)
      .join("\n")}\n</urlset>\n`;

  await fs.writeFile(path.join(distDir, "sitemap-pages.xml"), urlSetXml(staticUrls), "utf8");
  const sitemapLocations = [`${siteUrl}/sitemap-pages.xml`];
  if (urls.length) {
    const coffeePartLocs = [];
    for (let i = 0; i < urls.length; i += COFFEE_SITEMAP_MAX_URLS) {
      const chunk = urls.slice(i, i + COFFEE_SITEMAP_MAX_URLS);
      const partIndex = Math.floor(i / COFFEE_SITEMAP_MAX_URLS) + 1;
      const filename = `sitemap-coffee-${partIndex}.xml`;
      await fs.writeFile(path.join(distDir, filename), urlSetXml(chunk), "utf8");
      coffeePartLocs.push(`${siteUrl}/${filename}`);
    }
    const coffeeIndexXml = `<?xml version="1.0" encoding="UTF-8"?>\n<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${coffeePartLocs
      .map((loc) => `  <sitemap><loc>${loc}</loc></sitemap>`)
      .join("\n")}\n</sitemapindex>\n`;
    await fs.writeFile(path.join(distDir, "sitemap-coffee.xml"), coffeeIndexXml, "utf8");
    sitemapLocations.push(`${siteUrl}/sitemap-coffee.xml`);
  }
  const sitemapIndex = `<?xml version="1.0" encoding="UTF-8"?>\n<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${sitemapLocations
    .map((loc) => `  <sitemap><loc>${loc}</loc></sitemap>`)
    .join("\n")}\n</sitemapindex>\n`;
  await fs.writeFile(path.join(distDir, "sitemap.xml"), sitemapIndex, "utf8");

  const robots = `User-agent: *\nAllow: /\nDisallow: /index.html\nSitemap: ${siteUrl}/sitemap.xml\n`;
  await fs.writeFile(path.join(distDir, "robots.txt"), robots, "utf8");

  console.log(`[update-sitemap-from-supabase] Updated sitemaps (${urls.length} coffee URLs).`);
}

main().catch((error) => {
  console.error("[update-sitemap-from-supabase]", error);
  process.exitCode = 1;
});
