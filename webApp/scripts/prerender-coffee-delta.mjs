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

function upsertTag(html, tagPattern, replacement) {
  if (tagPattern.test(html)) return html.replace(tagPattern, replacement);
  return html.replace("</head>", `  ${replacement}\n  </head>`);
}

function ensureLeadingSlash(value) {
  if (!value) return "/";
  return value.startsWith("/") ? value : `/${value}`;
}

function toAbsoluteAppAssetPath(appBasePath, assetRelativePath) {
  const cleanBase = appBasePath === "/" ? "" : appBasePath.replace(/\/+$/, "");
  const cleanAsset = assetRelativePath.replace(/^\.?\//, "");
  return ensureLeadingSlash(`${cleanBase}/${cleanAsset}`.replace(/\/+/g, "/"));
}

function rewriteRelativeAssetUrls(html, appBasePath) {
  const rewrites = [
    ["./assets/", toAbsoluteAppAssetPath(appBasePath, "assets/")],
    ["./manifest.webmanifest", toAbsoluteAppAssetPath(appBasePath, "manifest.webmanifest")],
    ["./registerSW.js", toAbsoluteAppAssetPath(appBasePath, "registerSW.js")],
    ["./logo.png", toAbsoluteAppAssetPath(appBasePath, "logo.png")]
  ];
  let output = html;
  for (const [from, to] of rewrites) output = output.split(from).join(to);
  return output;
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

function chunk(items, size) {
  const result = [];
  for (let i = 0; i < items.length; i += size) result.push(items.slice(i, i + size));
  return result;
}

function unique(items) {
  return Array.from(new Set(items.filter(Boolean)));
}

async function fetchJson(endpoint, anonKey) {
  const res = await fetch(endpoint, {
    headers: {
      apikey: anonKey,
      Authorization: `Bearer ${anonKey}`
    }
  });
  if (!res.ok) throw new Error(`Supabase request failed: ${res.status} ${res.statusText}`);
  return res.json();
}

async function main() {
  const changesPath = process.argv[2] ?? ".coffee-changes.json";
  const distDir = path.resolve(process.cwd(), "dist");
  const indexPath = path.join(distDir, "index.html");
  const deleteListPath = path.join(distDir, "coffee-deletes.txt");
  const supabaseUrl = await readEnvFallback("VITE_SUPABASE_URL");
  const supabaseAnonKey = await readEnvFallback("VITE_SUPABASE_ANON_KEY");
  const siteUrlRaw = (await readEnvFallback("VITE_SITE_URL")) || "https://cafesitoapp.com";
  const siteUrl = siteUrlRaw.replace(/\/+$/, "");
  const siteUrlObject = new URL(siteUrl);
  const appBasePath = (() => {
    const p = siteUrlObject.pathname.replace(/\/+$/, "");
    return p && p !== "/" ? ensureLeadingSlash(p) : "/";
  })();

  if (!supabaseUrl || !supabaseAnonKey) {
    throw new Error("Missing VITE_SUPABASE_URL/VITE_SUPABASE_ANON_KEY");
  }

  const payloadRaw = await fs.readFile(path.resolve(process.cwd(), changesPath), "utf8");
  const payload = JSON.parse(payloadRaw);
  const changes = Array.isArray(payload?.changes) ? payload.changes : [];
  if (changes.length === 0) {
    await fs.writeFile(deleteListPath, "", "utf8");
    console.log("[prerender-coffee-delta] No changes in payload.");
    return;
  }

  const indexHtml = await fs.readFile(indexPath, "utf8");
  const catalogEndpoint = `${supabaseUrl}/rest/v1/coffees?select=id,nombre,marca&order=nombre.asc,id.asc&limit=5000`;
  const catalog = await fetchJson(catalogEndpoint, supabaseAnonKey);

  catalog.sort((a, b) => {
    const nameCmp = String(a.nombre ?? "").localeCompare(String(b.nombre ?? ""));
    if (nameCmp !== 0) return nameCmp;
    return String(a.id ?? "").localeCompare(String(b.id ?? ""));
  });
  const nameCounts = new Map();
  for (const coffee of catalog) {
    const key = normalizeText(coffee.nombre);
    nameCounts.set(key, (nameCounts.get(key) ?? 0) + 1);
  }
  const slugCounts = new Map();
  const slugById = new Map();
  for (const coffee of catalog) {
    const hasDuplicatedName = (nameCounts.get(normalizeText(coffee.nombre)) ?? 0) > 1;
    const base = toCoffeeSlug(coffee.nombre, coffee.marca, hasDuplicatedName);
    const count = (slugCounts.get(base) ?? 0) + 1;
    slugCounts.set(base, count);
    const slug = count > 1 ? `${base}-${count}` : base;
    slugById.set(String(coffee.id), slug);
  }

  const idsToRender = unique(changes
    .filter((item) => String(item.operation ?? "").toUpperCase() !== "DELETE")
    .map((item) => String(item.coffee_id ?? "").trim()));

  const renderRows = [];
  for (const idChunk of chunk(idsToRender, 150)) {
    if (idChunk.length === 0) continue;
    const inFilter = idChunk.map((id) => encodeURIComponent(id)).join(",");
    const endpoint = `${supabaseUrl}/rest/v1/coffees?select=id,nombre,marca,descripcion,image_url&id=in.(${inFilter})`;
    const rows = await fetchJson(endpoint, supabaseAnonKey);
    renderRows.push(...rows);
  }

  const renderById = new Map(renderRows.map((row) => [String(row.id), row]));
  let rendered = 0;
  const deleteSlugs = new Set();

  for (const change of changes) {
    const operation = String(change.operation ?? "UNKNOWN").toUpperCase();
    const coffeeId = String(change.coffee_id ?? "").trim();
    const newSlugFromCatalog = coffeeId ? slugById.get(coffeeId) ?? null : null;
    const newSlug = String(change.new_slug ?? "").trim() || newSlugFromCatalog;
    const oldSlugExplicit = String(change.old_slug ?? "").trim();
    const oldName = String(change.old_nombre ?? "").trim();
    const oldBrand = String(change.old_marca ?? "").trim();
    const oldSlugDerived = oldName
      ? toCoffeeSlug(oldName, oldBrand || null)
      : "";

    if (operation === "DELETE") {
      const target = oldSlugExplicit || oldSlugDerived;
      if (target) deleteSlugs.add(target);
      continue;
    }

    const row = coffeeId ? renderById.get(coffeeId) : null;
    if (!row || !newSlug) continue;

    const coffeePath = `/coffee/${newSlug}/`;
    const canonical = `${siteUrl}${coffeePath}`;
    const title = `${row.nombre} | Cafesito`;
    const fallbackDescription = `${row.nombre} ${row.marca ?? ""}`.trim() || "Detalle de cafe en Cafesito";
    const description = (row.descripcion ?? fallbackDescription).slice(0, 160);

    let html = rewriteRelativeAssetUrls(indexHtml, appBasePath);
    html = html.replace(/<title>[^<]*<\/title>/i, `<title>${title}</title>`);
    html = upsertTag(html, /<meta\s+name=["']description["'][^>]*>/i, `<meta name="description" content="${description}">`);
    html = upsertTag(html, /<meta\s+name=["']robots["'][^>]*>/i, `<meta name="robots" content="index, follow">`);
    html = upsertTag(html, /<link\s+rel=["']canonical["'][^>]*>/i, `<link rel="canonical" href="${canonical}">`);
    html = upsertTag(html, /<meta\s+property=["']og:title["'][^>]*>/i, `<meta property="og:title" content="${title}">`);
    html = upsertTag(html, /<meta\s+property=["']og:type["'][^>]*>/i, `<meta property="og:type" content="product">`);
    html = upsertTag(html, /<meta\s+property=["']og:url["'][^>]*>/i, `<meta property="og:url" content="${canonical}">`);

    const jsonLd = {
      "@context": "https://schema.org",
      "@type": "WebPage",
      "@id": `${canonical}#webpage`,
      name: title,
      url: canonical,
      description,
      isPartOf: {
        "@type": "WebSite",
        name: "Cafesito",
        url: siteUrl
      },
      about: {
        "@type": "Thing",
        name: row.nombre,
        brand: row.marca || undefined,
        image: row.image_url || undefined
      }
    };
    html = html.replace("</head>", `  <script type="application/ld+json">${JSON.stringify(jsonLd)}</script>\n  </head>`);

    const outDir = path.join(distDir, "coffee", newSlug);
    await fs.mkdir(outDir, { recursive: true });
    await fs.writeFile(path.join(outDir, "index.html"), html, "utf8");
    rendered += 1;

    const previousSlug = oldSlugExplicit || oldSlugDerived;
    if (operation === "UPDATE" && previousSlug && previousSlug !== newSlug) {
      deleteSlugs.add(previousSlug);
    }
  }

  const deleteDirs = Array.from(deleteSlugs).map((slug) => `coffee/${slug}`);
  await fs.writeFile(deleteListPath, deleteDirs.join("\n"), "utf8");

  console.log(`[prerender-coffee-delta] Rendered ${rendered} coffee pages. Delete candidates: ${deleteDirs.length}.`);
}

main().catch((error) => {
  console.error("[prerender-coffee-delta]", error);
  process.exitCode = 1;
});
