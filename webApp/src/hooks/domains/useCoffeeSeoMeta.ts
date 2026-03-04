import { useEffect } from "react";
import type { CoffeeRow } from "../../types";

const DEFAULT_TITLE = "Cafesito";
const DEFAULT_DESCRIPTION = "Comunidad de café para compartir timeline, explorar cafés y seguir perfiles.";
const SITE_NAME = "Cafesito";
const MAX_DESCRIPTION_LENGTH = 160;

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

export function useCoffeeSeoMeta(
  detailCoffee: CoffeeRow | null,
  options?: { avgRating?: number; reviewCount?: number }
) {
  useEffect(() => {
    const doc = document;
    const isCoffeeRoute = window.location.pathname.startsWith("/coffee/");
    const siteUrl = (import.meta.env.VITE_SITE_URL as string | undefined) ?? (typeof window !== "undefined" ? window.location.origin : "");
    const canonicalHref = `${siteUrl}${window.location.pathname}`;

    setOrCreateLink(doc, "canonical", canonicalHref);

    let descriptionMeta = doc.querySelector("meta[name='description']") as HTMLMetaElement | null;
    if (!descriptionMeta) {
      descriptionMeta = doc.createElement("meta");
      descriptionMeta.name = "description";
      doc.head.appendChild(descriptionMeta);
    }

    if (!isCoffeeRoute) {
      doc.title = DEFAULT_TITLE;
      descriptionMeta.content = DEFAULT_DESCRIPTION;
      setOrCreateMeta(doc, "meta", "property", "og:title", DEFAULT_TITLE);
      setOrCreateMeta(doc, "meta", "property", "og:description", DEFAULT_DESCRIPTION);
      setOrCreateMeta(doc, "meta", "property", "og:type", "website");
      setOrCreateMeta(doc, "meta", "property", "og:url", canonicalHref);
      setOrCreateMeta(doc, "meta", "property", "og:site_name", SITE_NAME);
      setOrCreateMeta(doc, "meta", "name", "twitter:card", "summary");
      setOrCreateMeta(doc, "meta", "name", "twitter:title", DEFAULT_TITLE);
      setOrCreateMeta(doc, "meta", "name", "twitter:description", DEFAULT_DESCRIPTION);
      removeJsonLd(doc, "coffee-product-ld");
      return;
    }

    const title = detailCoffee ? `${detailCoffee.nombre} | Cafesito` : "Café | Cafesito";
    const rawDesc = detailCoffee
      ? (detailCoffee.descripcion?.trim() || `${detailCoffee.nombre}${detailCoffee.marca ? ` · ${detailCoffee.marca}` : ""}${detailCoffee.pais_origen ? ` · ${detailCoffee.pais_origen}` : ""}`.trim() || "Detalle de café en Cafesito")
      : "Detalle de café en Cafesito";
    const description = rawDesc.slice(0, MAX_DESCRIPTION_LENGTH);
    const imageUrl =
      detailCoffee?.image_url
        ? (detailCoffee.image_url.startsWith("http") ? detailCoffee.image_url : `${siteUrl}${detailCoffee.image_url}`)
        : "";

    doc.title = title;
    descriptionMeta.content = description;

    setOrCreateMeta(doc, "meta", "property", "og:title", title);
    setOrCreateMeta(doc, "meta", "property", "og:description", description);
    setOrCreateMeta(doc, "meta", "property", "og:type", "product");
    setOrCreateMeta(doc, "meta", "property", "og:url", canonicalHref);
    setOrCreateMeta(doc, "meta", "property", "og:site_name", SITE_NAME);
    if (imageUrl) setOrCreateMeta(doc, "meta", "property", "og:image", imageUrl);

    setOrCreateMeta(doc, "meta", "name", "twitter:card", imageUrl ? "summary_large_image" : "summary");
    setOrCreateMeta(doc, "meta", "name", "twitter:title", title);
    setOrCreateMeta(doc, "meta", "name", "twitter:description", description);
    if (imageUrl) setOrCreateMeta(doc, "meta", "name", "twitter:image", imageUrl);

    if (detailCoffee) {
      const avgRating = options?.avgRating ?? 0;
      const reviewCount = options?.reviewCount ?? 0;
      const product: Record<string, unknown> = {
        "@context": "https://schema.org",
        "@type": "Product",
        "@id": `${canonicalHref}#product`,
        name: detailCoffee.nombre,
        description: description,
        ...(detailCoffee.marca ? { brand: { "@type": "Brand", name: detailCoffee.marca } } : {}),
        ...(imageUrl ? { image: imageUrl } : {}),
        ...(detailCoffee.pais_origen ? { countryOfOrigin: { "@type": "Country", name: detailCoffee.pais_origen } } : {})
      };
      if (reviewCount > 0 && avgRating > 0) {
        (product as Record<string, unknown>).aggregateRating = {
          "@type": "AggregateRating",
          ratingValue: avgRating,
          reviewCount,
          bestRating: 5,
          worstRating: 1
        };
      }
      removeJsonLd(doc, "coffee-product-ld");
      const script = doc.createElement("script");
      script.id = "coffee-product-ld";
      script.type = "application/ld+json";
      script.textContent = JSON.stringify(product);
      doc.head.appendChild(script);
      return () => removeJsonLd(doc, "coffee-product-ld");
    }

    removeJsonLd(doc, "coffee-product-ld");
  }, [detailCoffee, options?.avgRating, options?.reviewCount]);
}
