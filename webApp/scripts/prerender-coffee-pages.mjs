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

function toCoffeeSlug(name) {
  const base = normalizeText(name)
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
  return base || "cafe";
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

function upsertTag(html, tagPattern, replacement) {
  if (tagPattern.test(html)) return html.replace(tagPattern, replacement);
  return html.replace("</head>", `  ${replacement}\n  </head>`);
}

async function main() {
  const distDir = path.resolve(process.cwd(), "dist");
  const indexPath = path.join(distDir, "index.html");
  const supabaseUrl = await readEnvFallback("VITE_SUPABASE_URL");
  const supabaseAnonKey = await readEnvFallback("VITE_SUPABASE_ANON_KEY");
  const siteUrl = (await readEnvFallback("VITE_SITE_URL")) || "https://cafesito.app";

  if (!supabaseUrl || !supabaseAnonKey) {
    console.warn("[prerender-coffee-pages] Missing VITE_SUPABASE_URL/VITE_SUPABASE_ANON_KEY. Skipping prerender.");
    return;
  }

  const indexHtml = await fs.readFile(indexPath, "utf8");
  const endpoint = `${supabaseUrl}/rest/v1/coffees?select=id,nombre,marca,descripcion,image_url&order=nombre.asc,id.asc&limit=500`;
  const res = await fetch(endpoint, {
    headers: {
      apikey: supabaseAnonKey,
      Authorization: `Bearer ${supabaseAnonKey}`
    }
  });

  if (!res.ok) {
    throw new Error(`Failed to fetch coffees for prerender: ${res.status} ${res.statusText}`);
  }

  const coffees = await res.json();
  coffees.sort((a, b) => {
    const nameCmp = String(a.nombre ?? "").localeCompare(String(b.nombre ?? ""));
    if (nameCmp !== 0) return nameCmp;
    return String(a.id ?? "").localeCompare(String(b.id ?? ""));
  });
  const slugCounts = new Map();
  const urls = [];

  for (const coffee of coffees) {
    const base = toCoffeeSlug(coffee.nombre);
    const count = (slugCounts.get(base) ?? 0) + 1;
    slugCounts.set(base, count);
    const slug = count > 1 ? `${base}-${count}` : base;

    const coffeePath = `/coffee/${slug}`;
    const canonical = `${siteUrl}${coffeePath}`;
    const title = `${coffee.nombre} | Cafesito`;
    const fallbackDescription = `${coffee.nombre} ${coffee.marca ?? ""}`.trim() || "Detalle de cafe en Cafesito";
    const description = (coffee.descripcion ?? fallbackDescription).slice(0, 160);

    let html = indexHtml;
    html = html.replace(/<title>[^<]*<\/title>/i, `<title>${title}</title>`);
    html = upsertTag(html, /<meta\s+name=["']description["'][^>]*>/i, `<meta name="description" content="${description}">`);
    html = upsertTag(html, /<link\s+rel=["']canonical["'][^>]*>/i, `<link rel="canonical" href="${canonical}">`);
    html = upsertTag(html, /<meta\s+property=["']og:title["'][^>]*>/i, `<meta property="og:title" content="${title}">`);
    html = upsertTag(html, /<meta\s+property=["']og:type["'][^>]*>/i, `<meta property="og:type" content="product">`);
    html = upsertTag(html, /<meta\s+property=["']og:url["'][^>]*>/i, `<meta property="og:url" content="${canonical}">`);

    const jsonLd = {
      "@context": "https://schema.org",
      "@type": "Product",
      name: coffee.nombre,
      brand: coffee.marca || undefined,
      image: coffee.image_url || undefined,
      description
    };
    html = html.replace("</head>", `  <script type="application/ld+json">${JSON.stringify(jsonLd)}</script>\n  </head>`);

    const outDir = path.join(distDir, "coffee", slug);
    await fs.mkdir(outDir, { recursive: true });
    await fs.writeFile(path.join(outDir, "index.html"), html, "utf8");
    urls.push(canonical);
  }

  if (urls.length) {
    const sitemap = `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls
      .map((url) => `  <url><loc>${url}</loc></url>`)
      .join("\n")}\n</urlset>\n`;
    await fs.writeFile(path.join(distDir, "sitemap-coffee.xml"), sitemap, "utf8");

    const sitemapIndex = `<?xml version="1.0" encoding="UTF-8"?>\n<sitemapindex xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n  <sitemap><loc>${siteUrl}/sitemap-coffee.xml</loc></sitemap>\n</sitemapindex>\n`;
    await fs.writeFile(path.join(distDir, "sitemap.xml"), sitemapIndex, "utf8");

    const robots = `User-agent: *\nAllow: /\nSitemap: ${siteUrl}/sitemap.xml\n`;
    await fs.writeFile(path.join(distDir, "robots.txt"), robots, "utf8");
  }

  console.log(`[prerender-coffee-pages] Generated ${urls.length} coffee detail pages.`);
}

main().catch((error) => {
  console.error("[prerender-coffee-pages]", error);
  process.exitCode = 1;
});
