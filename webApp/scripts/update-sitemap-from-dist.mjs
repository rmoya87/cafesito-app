/**
 * Actualiza solo sitemap.xml, sitemap-pages.xml, sitemap-coffee.xml y robots.txt
 * leyendo los slugs desde dist/coffee/ (ya generados). No llama a Supabase ni regenera HTMLs.
 * Útil cuando hay cache hit en CI: se restaura dist/coffee y solo se refrescan los sitemaps.
 */
import fs from "node:fs/promises";
import path from "node:path";

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
  const coffeeDir = path.join(distDir, "coffee");
  const siteUrlRaw = (await readEnvFallback("VITE_SITE_URL")) || "https://cafesitoapp.com";
  const siteUrl = siteUrlRaw.replace(/\/+$/, "");
  const siteUrlObject = new URL(siteUrl);
  const appBasePath = (() => {
    const p = siteUrlObject.pathname.replace(/\/+$/, "");
    return p && p !== "/" ? ensureLeadingSlash(p) : "/";
  })();
  const staticRoot = appBasePath === "/" ? "/" : ensureTrailingSlash(appBasePath);

  let slugs = [];
  try {
    const entries = await fs.readdir(coffeeDir, { withFileTypes: true });
    slugs = entries.filter((e) => e.isDirectory()).map((e) => e.name);
  } catch (err) {
    if (err.code !== "ENOENT") throw err;
    console.warn("[update-sitemap-from-dist] No dist/coffee folder, writing sitemaps with 0 coffee URLs.");
  }

  const coffeeUrls = slugs.map((slug) => `${siteUrl}/coffee/${slug}/`);
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
  if (coffeeUrls.length) {
    const coffeePartLocs = [];
    for (let i = 0; i < coffeeUrls.length; i += COFFEE_SITEMAP_MAX_URLS) {
      const chunk = coffeeUrls.slice(i, i + COFFEE_SITEMAP_MAX_URLS);
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

  console.log(`[update-sitemap-from-dist] Updated sitemaps (${coffeeUrls.length} coffee URLs from dist/coffee).`);
}

main().catch((err) => {
  console.error("[update-sitemap-from-dist]", err);
  process.exitCode = 1;
});
