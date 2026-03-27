import { useEffect } from "react";
import type { CoffeeRow } from "../../types";

const ROBOTS_INDEX_FOLLOW = "index, follow";
const ROBOTS_NOINDEX_FOLLOW = "noindex, follow";

const DEFAULT_TITLE = "Cafesito";
const DEFAULT_DESCRIPTION = "Comunidad de café para compartir timeline, explorar cafés y seguir perfiles.";
const SITE_NAME = "Cafesito";
const MAX_DESCRIPTION_LENGTH = 160;

const SEARCH_COFFEES_TITLE = "Explorar cafés | Cafesito";
const SEARCH_COFFEES_DESCRIPTION =
  "Explora y descubre cafés, marcas y orígenes. Busca por nombre, filtra por tipo o valoración y encuentra tu próximo café favorito en la comunidad Cafesito.";

const DEFAULT_OG_IMAGE = "https://cafesitoapp.com/logo.png";
const SEARCH_OG_IMAGE = "https://cafesitoapp.com/og/search.jpg";
const LOGIN_OG_IMAGE = "https://cafesitoapp.com/og/login.jpg";
const OG_IMAGE_WIDTH = "1200";
const OG_IMAGE_HEIGHT = "630";
const OG_LOCALE = "es_ES";
const TWITTER_SITE = "@cafesitoapp";
const TWITTER_CREATOR = "@cafesitoapp";

function stripTrailingSlash(pathname: string): string {
  const p = (pathname ?? "").replace(/\/+$/, "") || "/";
  return p === "" ? "/" : p;
}

function upsertWebSiteSearchActionJsonLd(doc: Document, siteUrl: string) {
  const id = "website-searchaction-ld";
  const existing = doc.getElementById(id) as HTMLScriptElement | null;
  const base = siteUrl.replace(/\/+$/, "");
  const payload = {
    "@context": "https://schema.org",
    "@type": "WebSite",
    name: SITE_NAME,
    url: base,
    potentialAction: {
      "@type": "SearchAction",
      target: `${base}/search?q={search_term_string}`,
      "query-input": "required name=search_term_string"
    }
  };
  const script = existing ?? doc.createElement("script");
  script.id = id;
  script.type = "application/ld+json";
  script.textContent = JSON.stringify(payload);
  if (!existing) doc.head.appendChild(script);
}

function setOrCreateMeta(
  doc: Document,
  selector: string,
  attr: "name" | "property",
  key: string,
  content: string
): void {
  let el = doc.querySelector(`${selector}[${attr}='${key}']`) as HTMLMetaElement | null;
  if (!el) {
    el = doc.createElement("meta");
    el.setAttribute(attr, key);
    doc.head.appendChild(el);
  }
  el.content = content;
}

function removeMeta(doc: Document, selector: string, attr: string, key: string): void {
  const el = doc.querySelector(`${selector}[${attr}='${key}']`);
  if (el) el.remove();
}

function setOrCreateLink(doc: Document, rel: string, href: string): void {
  let el = doc.querySelector(`link[rel='${rel}']`) as HTMLLinkElement | null;
  if (!el) {
    el = doc.createElement("link");
    el.rel = rel;
    doc.head.appendChild(el);
  }
  el.href = href;
}

function removeJsonLd(doc: Document, id: string): void {
  const el = doc.getElementById(id);
  if (el) el.remove();
}

function upsertJsonLd(doc: Document, id: string, payload: Record<string, unknown>): void {
  removeJsonLd(doc, id);
  const script = doc.createElement("script");
  script.id = id;
  script.type = "application/ld+json";
  script.textContent = JSON.stringify(payload);
  doc.head.appendChild(script);
}

function setOgImageMeta(doc: Document, url: string) {
  setOrCreateMeta(doc, "meta", "property", "og:image", url);
  setOrCreateMeta(doc, "meta", "property", "og:image:width", OG_IMAGE_WIDTH);
  setOrCreateMeta(doc, "meta", "property", "og:image:height", OG_IMAGE_HEIGHT);
  setOrCreateMeta(doc, "meta", "name", "twitter:image", url);
}

export function useCoffeeSeoMeta(
  detailCoffee: CoffeeRow | null,
  options?: { avgRating?: number; reviewCount?: number },
  pathnameOverride?: string
) {
  useEffect(() => {
    const doc = document;
    const rawPathname = pathnameOverride ?? (typeof window !== "undefined" ? window.location.pathname : "");
    const pathname = stripTrailingSlash(rawPathname);
    const isCoffeeRoute = pathname.includes("/coffee/");
    const isSearchRoute = /\/search(\/|$)/.test(pathname);
    const isSearchCoffeesOnly = isSearchRoute && !/\/search\/users/.test(pathname);
    const isLoginRoute = /\/login(\/|$)/.test(pathname);
    const isSearchFacetRoute = /\/search\/(origen|tueste|especialidad|formato|nota)\/[^/]+$/.test(pathname);
    const siteUrl = (import.meta.env.VITE_SITE_URL as string | undefined) ?? (typeof window !== "undefined" ? window.location.origin : "");
    const canonicalHref = `${siteUrl.replace(/\/+$/, "")}${pathname}`;

    setOrCreateLink(doc, "canonical", canonicalHref);
    upsertWebSiteSearchActionJsonLd(doc, siteUrl);
    setOrCreateMeta(doc, "meta", "property", "og:locale", OG_LOCALE);
    setOrCreateMeta(doc, "meta", "name", "twitter:site", TWITTER_SITE);
    setOrCreateMeta(doc, "meta", "name", "twitter:creator", TWITTER_CREATOR);

    let descriptionMeta = doc.querySelector("meta[name='description']") as HTMLMetaElement | null;
    if (!descriptionMeta) {
      descriptionMeta = doc.createElement("meta");
      descriptionMeta.name = "description";
      doc.head.appendChild(descriptionMeta);
    }

    if (isSearchCoffeesOnly || isSearchFacetRoute) {
      const facetMatch = pathname.match(/\/search\/(origen|tueste|especialidad|formato|nota)\/([^/]+)$/);
      const facetType = facetMatch?.[1] ?? null;
      const facetValue = facetMatch?.[2] ? decodeURIComponent(facetMatch[2]) : null;
      const title = SEARCH_COFFEES_TITLE;
      const description = facetType && facetValue
        ? `${SEARCH_COFFEES_DESCRIPTION} Filtrado por ${facetType}: ${facetValue}.`
        : SEARCH_COFFEES_DESCRIPTION;
      const descTruncated = description.slice(0, MAX_DESCRIPTION_LENGTH);
      doc.title = title;
      descriptionMeta.content = descTruncated;
      setOrCreateMeta(doc, "meta", "property", "og:title", title);
      setOrCreateMeta(doc, "meta", "property", "og:description", descTruncated);
      setOrCreateMeta(doc, "meta", "property", "og:type", "website");
      setOrCreateMeta(doc, "meta", "property", "og:url", canonicalHref);
      setOrCreateMeta(doc, "meta", "property", "og:site_name", SITE_NAME);
      setOgImageMeta(doc, SEARCH_OG_IMAGE);
      setOrCreateMeta(doc, "meta", "name", "twitter:card", "summary_large_image");
      setOrCreateMeta(doc, "meta", "name", "twitter:title", title);
      setOrCreateMeta(doc, "meta", "name", "twitter:description", descTruncated);
      setOrCreateMeta(doc, "meta", "name", "robots", ROBOTS_INDEX_FOLLOW);
      removeJsonLd(doc, "coffee-product-ld");
      removeJsonLd(doc, "coffee-detail-page-ld");
      removeJsonLd(doc, "search-page-ld");
      const webPage = {
        "@context": "https://schema.org",
        "@type": "WebPage",
        "@id": `${canonicalHref}#webpage`,
        name: title,
        description: descTruncated,
        url: canonicalHref,
        isPartOf: { "@type": "WebSite", name: SITE_NAME, url: siteUrl }
      };
      const script = doc.createElement("script");
      script.id = "search-page-ld";
      script.type = "application/ld+json";
      script.textContent = JSON.stringify(webPage);
      doc.head.appendChild(script);
      return () => removeJsonLd(doc, "search-page-ld");
    }

    if (!isCoffeeRoute) {
      // Política SEO SPA:
      // - Indexar: /search (cafés), /coffee/* y /login
      // - El resto de rutas SPA se marcan como noindex
      setOrCreateMeta(doc, "meta", "name", "robots", isLoginRoute ? ROBOTS_INDEX_FOLLOW : ROBOTS_NOINDEX_FOLLOW);
      doc.title = DEFAULT_TITLE;
      descriptionMeta.content = DEFAULT_DESCRIPTION;
      setOrCreateMeta(doc, "meta", "property", "og:title", DEFAULT_TITLE);
      setOrCreateMeta(doc, "meta", "property", "og:description", DEFAULT_DESCRIPTION);
      setOrCreateMeta(doc, "meta", "property", "og:type", "website");
      setOrCreateMeta(doc, "meta", "property", "og:url", canonicalHref);
      setOrCreateMeta(doc, "meta", "property", "og:site_name", SITE_NAME);
      if (isLoginRoute) {
        setOgImageMeta(doc, LOGIN_OG_IMAGE);
        setOrCreateMeta(doc, "meta", "name", "twitter:card", "summary_large_image");
      } else {
        setOgImageMeta(doc, DEFAULT_OG_IMAGE);
        setOrCreateMeta(doc, "meta", "name", "twitter:card", "summary");
      }
      setOrCreateMeta(doc, "meta", "name", "twitter:title", DEFAULT_TITLE);
      setOrCreateMeta(doc, "meta", "name", "twitter:description", DEFAULT_DESCRIPTION);
      removeJsonLd(doc, "coffee-product-ld");
      removeJsonLd(doc, "coffee-detail-page-ld");
      removeJsonLd(doc, "search-page-ld");
      return;
    }

    removeJsonLd(doc, "search-page-ld");
    const title = detailCoffee ? `${detailCoffee.nombre} | Cafesito` : "Café | Cafesito";
    const rawDesc = detailCoffee
      ? (detailCoffee.descripcion?.trim() || `${detailCoffee.nombre}${detailCoffee.marca ? ` · ${detailCoffee.marca}` : ""}${detailCoffee.pais_origen ? ` · ${detailCoffee.pais_origen}` : ""}`.trim() || "Detalle de café en Cafesito")
      : "Detalle de café en Cafesito";
    const description = rawDesc.slice(0, MAX_DESCRIPTION_LENGTH);
    // Preferimos una OG image 1200x630 generada en build (si existe). Fallback: image_url del café.
    // OG images se generan y sirven en producción bajo cafesitoapp.com
    const ogGeneratedUrl = detailCoffee ? `https://cafesitoapp.com/og/coffee/${encodeURIComponent(detailCoffee.id)}.jpg` : "";
    const imageUrl =
      detailCoffee?.image_url
        ? (detailCoffee.image_url.startsWith("http") ? detailCoffee.image_url : `${siteUrl}${detailCoffee.image_url}`)
        : "";

    doc.title = title;
    descriptionMeta.content = description;
    setOrCreateMeta(doc, "meta", "name", "robots", ROBOTS_INDEX_FOLLOW);

    setOrCreateMeta(doc, "meta", "property", "og:title", title);
    setOrCreateMeta(doc, "meta", "property", "og:description", description);
    setOrCreateMeta(doc, "meta", "property", "og:type", "product");
    setOrCreateMeta(doc, "meta", "property", "og:url", canonicalHref);
    setOrCreateMeta(doc, "meta", "property", "og:site_name", SITE_NAME);
    setOgImageMeta(doc, ogGeneratedUrl || imageUrl || DEFAULT_OG_IMAGE);

    setOrCreateMeta(doc, "meta", "name", "twitter:card", "summary_large_image");
    setOrCreateMeta(doc, "meta", "name", "twitter:title", title);
    setOrCreateMeta(doc, "meta", "name", "twitter:description", description);

    if (detailCoffee) {
      const avgRating = options?.avgRating ?? 0;
      const reviewCount = options?.reviewCount ?? 0;
      const hasAggregateRating = reviewCount > 0 && avgRating > 0;

      if (hasAggregateRating) {
        const product: Record<string, unknown> = {
          "@context": "https://schema.org",
          "@type": "Product",
          "@id": `${canonicalHref}#product`,
          name: detailCoffee.nombre,
          description: description,
          ...(detailCoffee.marca ? { brand: { "@type": "Brand", name: detailCoffee.marca } } : {}),
          ...(imageUrl ? { image: imageUrl } : {}),
          ...(detailCoffee.pais_origen ? { countryOfOrigin: { "@type": "Country", name: detailCoffee.pais_origen } } : {}),
          aggregateRating: {
            "@type": "AggregateRating",
            ratingValue: avgRating,
            reviewCount,
            bestRating: 5,
            worstRating: 1
          }
        };
        removeJsonLd(doc, "coffee-detail-page-ld");
        upsertJsonLd(doc, "coffee-product-ld", product);
        return () => removeJsonLd(doc, "coffee-product-ld");
      }

      // Si no hay datos de oferta/reviews/rating, evitamos Product para no invalidar rich results.
      const coffeeWebPage: Record<string, unknown> = {
        "@context": "https://schema.org",
        "@type": "WebPage",
        "@id": `${canonicalHref}#webpage`,
        name: title,
        description,
        url: canonicalHref,
        isPartOf: { "@type": "WebSite", name: SITE_NAME, url: siteUrl },
        about: {
          "@type": "Thing",
          name: detailCoffee.nombre,
          ...(detailCoffee.marca ? { brand: detailCoffee.marca } : {})
        }
      };
      removeJsonLd(doc, "coffee-product-ld");
      upsertJsonLd(doc, "coffee-detail-page-ld", coffeeWebPage);
      return () => removeJsonLd(doc, "coffee-detail-page-ld");
    }

    removeJsonLd(doc, "coffee-product-ld");
    removeJsonLd(doc, "coffee-detail-page-ld");
  }, [detailCoffee, options?.avgRating, options?.reviewCount, pathnameOverride]);
}
