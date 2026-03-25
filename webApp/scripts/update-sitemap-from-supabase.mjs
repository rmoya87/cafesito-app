import fs from "node:fs/promises";
import path from "node:path";
import { gzipSync } from "node:zlib";

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

function splitAtomizedList(value) {
  if (!value) return [];
  // Normaliza separadores comunes: coma, barra, punto y coma, " y "
  return String(value)
    .split(/[,/;|]/g)
    .map((t) => t.trim())
    .filter(Boolean);
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
  const buildDateIso = new Date().toISOString();

  if (!supabaseUrl || !supabaseAnonKey) {
    throw new Error("Missing VITE_SUPABASE_URL/VITE_SUPABASE_ANON_KEY");
  }

  async function fetchCoffees(select) {
    const endpoint = `${supabaseUrl}/rest/v1/coffees?select=${encodeURIComponent(select)}&order=nombre.asc,id.asc&limit=5000`;
    const res = await fetch(endpoint, {
      headers: {
        apikey: supabaseAnonKey,
        Authorization: `Bearer ${supabaseAnonKey}`
      }
    });
    return res;
  }

  // `updated_at` no está garantizado en coffees. Intentar y hacer fallback.
  const candidates = [
    "id,nombre,marca,image_url,pais_origen,tueste,especialidad,formato,updated_at",
    "id,nombre,marca,image_url,pais_origen,tueste,especialidad,formato,created_at",
    "id,nombre,marca,image_url,pais_origen,tueste,especialidad,formato"
  ];
  let coffees = null;
  let ok = false;
  for (const select of candidates) {
    const res = await fetchCoffees(select);
    if (!res.ok) continue;
    coffees = await res.json();
    ok = true;
    break;
  }
  if (!ok || !coffees) throw new Error("Failed to fetch coffees for sitemap (all select variants failed).");

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
  /** @type {{ loc: string, lastmod?: string }[]} */
  const coffeeUrls = [];
  /** @type {{ pageLoc: string, imageLoc: string, caption?: string }[]} */
  const coffeeImageUrls = [];
  /** @type {{ loc: string, lastmod?: string }[]} */
  const facetUrls = [];
  const facetSeen = new Set();
  for (const coffee of coffees) {
    const hasDuplicatedName = (nameCounts.get(normalizeText(coffee.nombre)) ?? 0) > 1;
    const base = toCoffeeSlug(coffee.nombre, coffee.marca, hasDuplicatedName);
    const count = (slugCounts.get(base) ?? 0) + 1;
    slugCounts.set(base, count);
    const slug = count > 1 ? `${base}-${count}` : base;
    const updatedAt = coffee.updated_at ? new Date(coffee.updated_at).toISOString() : undefined;
    const pageLoc = `${siteUrl}/coffee/${slug}`;
    coffeeUrls.push({ loc: pageLoc, lastmod: updatedAt });
    const ogImage = `${siteUrl}/og/coffee/${coffee.id}.jpg`;
    coffeeImageUrls.push({
      pageLoc,
      imageLoc: ogImage,
      caption: `${coffee.nombre ?? "Cafe"}${coffee.marca ? ` · ${coffee.marca}` : ""}`
    });

    // Facetas indexables (rutas limpias, sin query params)
    const facetPairs = [
      ["origen", ...(splitAtomizedList(coffee.pais_origen))],
      ["tueste", ...(splitAtomizedList(coffee.tueste))],
      ["especialidad", ...(splitAtomizedList(coffee.especialidad))],
      ["formato", ...(splitAtomizedList(coffee.formato))]
    ];
    for (const [facetType, ...vals] of facetPairs) {
      for (const v of vals) {
        const key = `${facetType}:${normalizeText(v)}`;
        if (facetSeen.has(key)) continue;
        facetSeen.add(key);
        facetUrls.push({
          loc: `${siteUrl}/search/${facetType}/${encodeURIComponent(v)}`,
          lastmod: buildDateIso
        });
      }
    }
  }

  const siteUrlObject = new URL(siteUrl);
  const appBasePath = (() => {
    const p = siteUrlObject.pathname.replace(/\/+$/, "");
    return p && p !== "/" ? ensureLeadingSlash(p) : "/";
  })();
  const staticRoot = appBasePath === "/" ? "/" : ensureTrailingSlash(appBasePath);
  const staticPaths = [
    staticRoot,
    `${staticRoot}search`,
    `${staticRoot}login`,
    `${staticRoot}legal/privacidad.html`,
    `${staticRoot}legal/condiciones.html`,
    `${staticRoot}legal/eliminacion-cuenta.html`
  ];
  /** @type {{ loc: string, lastmod?: string }[]} */
  const staticUrls = staticPaths.map((p) => ({ loc: `${siteUrl}${p.replace(/\/+$/, "") || "/"}`, lastmod: buildDateIso }));

  /** Máximo de URLs por sitemap de cafés; Google permite 50.000 pero dividir en 500 mejora indexación en Search Console. */
  const COFFEE_SITEMAP_MAX_URLS = 500;

  const urlSetXml = (entries) =>
    `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${entries
      .map((e) => `  <url><loc>${e.loc}</loc>${e.lastmod ? `<lastmod>${e.lastmod}</lastmod>` : ""}</url>`)
      .join("\n")}\n</urlset>\n`;

  const imageUrlSetXml = (entries) =>
    `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:image="http://www.google.com/schemas/sitemap-image/1.1">\n${entries
      .map(
        (e) =>
          `  <url><loc>${e.pageLoc}</loc><image:image><image:loc>${e.imageLoc}</image:loc>${
            e.caption ? `<image:caption>${String(e.caption).replace(/&/g, "&amp;").replace(/</g, "&lt;")}</image:caption>` : ""
          }</image:image></url>`
      )
      .join("\n")}\n</urlset>\n`;

  async function writeXmlAndGz(filename, xml) {
    const xmlPath = path.join(distDir, filename);
    await fs.writeFile(xmlPath, xml, "utf8");
    await fs.writeFile(`${xmlPath}.gz`, gzipSync(Buffer.from(xml, "utf8")));
    return `${siteUrl}/${filename}.gz`;
  }

  const sitemapLocations = [await writeXmlAndGz("sitemap-pages.xml", urlSetXml(staticUrls))];

  if (facetUrls.length) {
    sitemapLocations.push(await writeXmlAndGz("sitemap-facets.xml", urlSetXml(facetUrls)));
  }
  if (coffeeImageUrls.length) {
    sitemapLocations.push(await writeXmlAndGz("image-sitemap.xml", imageUrlSetXml(coffeeImageUrls)));
  }
  if (coffeeUrls.length) {
    const coffeePartLocs = [];
    for (let i = 0; i < coffeeUrls.length; i += COFFEE_SITEMAP_MAX_URLS) {
      const chunk = coffeeUrls.slice(i, i + COFFEE_SITEMAP_MAX_URLS);
      const partIndex = Math.floor(i / COFFEE_SITEMAP_MAX_URLS) + 1;
      const filename = `sitemap-coffee-${partIndex}.xml`;
      coffeePartLocs.push(await writeXmlAndGz(filename, urlSetXml(chunk)));
    }
    const coffeeIndexXml = `<?xml version="1.0" encoding="UTF-8"?>\n<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${coffeePartLocs
      .map((loc) => `  <sitemap><loc>${loc}</loc></sitemap>`)
      .join("\n")}\n</sitemapindex>\n`;
    sitemapLocations.push(await writeXmlAndGz("sitemap-coffee.xml", coffeeIndexXml));
  }
  const sitemapIndex = `<?xml version="1.0" encoding="UTF-8"?>\n<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${sitemapLocations
    .map((loc) => `  <sitemap><loc>${loc}</loc></sitemap>`)
    .join("\n")}\n</sitemapindex>\n`;
  await fs.writeFile(path.join(distDir, "sitemap.xml"), sitemapIndex, "utf8");
  await fs.writeFile(path.join(distDir, "sitemap.xml.gz"), gzipSync(Buffer.from(sitemapIndex, "utf8")));

  const robots = `User-agent: *\nAllow: /\nDisallow: /index.html\nSitemap: ${siteUrl}/sitemap.xml.gz\n`;
  await fs.writeFile(path.join(distDir, "robots.txt"), robots, "utf8");

  console.log(`[update-sitemap-from-supabase] Updated sitemaps (${coffeeUrls.length} coffee URLs).`);
}

main().catch((error) => {
  console.error("[update-sitemap-from-supabase]", error);
  process.exitCode = 1;
});
