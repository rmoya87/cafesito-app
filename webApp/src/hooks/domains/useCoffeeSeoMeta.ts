import { useEffect } from "react";
import type { CoffeeRow } from "../../types";

export function useCoffeeSeoMeta(detailCoffee: CoffeeRow | null) {
  useEffect(() => {
    const isCoffeeRoute = window.location.pathname.startsWith("/coffee/");
    const siteUrl = (import.meta.env.VITE_SITE_URL as string | undefined) ?? window.location.origin;
    const canonicalHref = `${siteUrl}${window.location.pathname}`;

    let canonical = document.querySelector("link[rel='canonical']") as HTMLLinkElement | null;
    if (!canonical) {
      canonical = document.createElement("link");
      canonical.rel = "canonical";
      document.head.appendChild(canonical);
    }
    canonical.href = canonicalHref;

    let descriptionMeta = document.querySelector("meta[name='description']") as HTMLMetaElement | null;
    if (!descriptionMeta) {
      descriptionMeta = document.createElement("meta");
      descriptionMeta.name = "description";
      document.head.appendChild(descriptionMeta);
    }

    if (!isCoffeeRoute) {
      document.title = "Cafesito Web";
      descriptionMeta.content = "Comunidad de cafe para compartir timeline, explorar cafes y seguir perfiles.";
      return;
    }
    const title = detailCoffee ? `${detailCoffee.nombre} | Cafesito` : "Café | Cafesito";
    const description = detailCoffee
      ? (detailCoffee.descripcion?.trim() || `${detailCoffee.nombre} ${detailCoffee.marca ?? ""}`.trim() || "Detalle de café en Cafesito")
      : "Detalle de café en Cafesito";
    document.title = title;
    descriptionMeta.content = description.slice(0, 160);

    let ogTitle = document.querySelector("meta[property='og:title']") as HTMLMetaElement | null;
    if (!ogTitle) {
      ogTitle = document.createElement("meta");
      ogTitle.setAttribute("property", "og:title");
      document.head.appendChild(ogTitle);
    }
    ogTitle.content = title;

    let ogType = document.querySelector("meta[property='og:type']") as HTMLMetaElement | null;
    if (!ogType) {
      ogType = document.createElement("meta");
      ogType.setAttribute("property", "og:type");
      document.head.appendChild(ogType);
    }
    ogType.content = "product";
  }, [detailCoffee]);
}

