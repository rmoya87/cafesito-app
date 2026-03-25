import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
import { UiIcon } from "../../ui/iconography";
import { Button, IconButton } from "../../ui/components";
import { getCountryCoords } from "../../core/countryCoords";
import { getCountryFlagImageUrl } from "../../core/countryFlags";
import type { CoffeeRow } from "../../types";

/** Separa pais_origen en tokens (coma, barra, etc.) */
function splitOrigins(paisOrigen: string | null): string[] {
  if (!paisOrigen || !paisOrigen.trim()) return [];
  return paisOrigen
    .split(/[,/|]/)
    .map((s) => s.trim())
    .filter(Boolean);
}

const OSM_ATTRIBUTION = '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
const OSM_TILE = "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png";

const MAPTILER_OMT_STYLE = "basic-v2";
const MAPTILER_ATTRIBUTION = '&copy; <a href="https://www.maptiler.com/copyright/" target="_blank">MapTiler</a>';

function getMapTileUrl(): { url: string; attribution: string; options?: L.TileLayerOptions } {
  const key = (import.meta.env.VITE_MAPTILER_API_KEY as string)?.trim();
  if (key) {
    return {
      url: `https://api.maptiler.com/maps/${MAPTILER_OMT_STYLE}/{z}/{x}/{y}.png?key=${key}`,
      attribution: MAPTILER_ATTRIBUTION,
      options: { tileSize: 512, zoomOffset: -1, minZoom: 0 }
    };
  }
  return { url: OSM_TILE, attribution: OSM_ATTRIBUTION };
}

export function CafesProbadosView({
  coffeesWithFirstTried,
  onBack,
  onOpenCoffee
}: {
  coffeesWithFirstTried: Array<{ coffee: CoffeeRow; firstTriedTs: number }>;
  onBack: () => void;
  onOpenCoffee: (coffeeId: string) => void;
}) {
  const [selectedCountry, setSelectedCountry] = useState<string | null>(null);
  const [listScrolled, setListScrolled] = useState(false);
  const mapRef = useRef<HTMLDivElement>(null);
  const mapWrapRef = useRef<HTMLDivElement>(null);
  const listWrapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<L.Map | null>(null);
  const markersRef = useRef<L.Marker[]>([]);

  const countriesWithCoords = useMemo(() => {
    const seen = new Set<string>();
    const result: Array<{ country: string; lat: number; lng: number }> = [];
    coffeesWithFirstTried.forEach(({ coffee }) => {
      const origins = splitOrigins(coffee.pais_origen);
      origins.forEach((country) => {
        const key = country.trim().toLowerCase();
        if (!key || seen.has(key)) return;
        const coords = getCountryCoords(country);
        if (coords) {
          seen.add(key);
          result.push({ country, ...coords });
        }
      });
    });
    return result;
  }, [coffeesWithFirstTried]);

  const filteredCoffees = useMemo(() => {
    if (!selectedCountry) return coffeesWithFirstTried;
    const key = selectedCountry.trim().toLowerCase();
    return coffeesWithFirstTried.filter(({ coffee }) => {
      const origins = splitOrigins(coffee.pais_origen);
      return origins.some((o) => o.trim().toLowerCase() === key);
    });
  }, [coffeesWithFirstTried, selectedCountry]);

  /** Agrupa cafés por país (subtítulo = país). Orden: por país, luego por firstTriedTs. */
  const coffeesByCountry = useMemo(() => {
    const map = new Map<string, Array<{ coffee: CoffeeRow; firstTriedTs: number }>>();
    const noOrigin = "—";
    filteredCoffees.forEach((item) => {
      const origins = splitOrigins(item.coffee.pais_origen);
      const country = origins[0]?.trim() || noOrigin;
      if (!map.has(country)) map.set(country, []);
      map.get(country)!.push(item);
    });
    map.forEach((list) => list.sort((a, b) => a.firstTriedTs - b.firstTriedTs));
    return Array.from(map.entries()).sort(([a], [b]) => (a === noOrigin ? 1 : b === noOrigin ? -1 : a.localeCompare(b)));
  }, [filteredCoffees]);

  const handleClear = useCallback(() => {
    setSelectedCountry(null);
  }, []);

  const handleListScroll = useCallback(() => {
    const el = listWrapRef.current;
    if (!el) return;
    setListScrolled(el.scrollTop > 8);
  }, []);

  useEffect(() => {
    if (!mapRef.current || countriesWithCoords.length === 0) return;
    const el = mapRef.current;
    markersRef.current.forEach((m) => m.remove());
    markersRef.current = [];
    if (mapInstanceRef.current) {
      mapInstanceRef.current.remove();
      mapInstanceRef.current = null;
    }
    const center = countriesWithCoords[0];
    const { url, attribution, options } = getMapTileUrl();
    const map = L.map(el, {
      center: [center.lat, center.lng],
      zoom: countriesWithCoords.length === 1 ? 4 : 2,
      scrollWheelZoom: true,
      zoomControl: false
    });
    L.tileLayer(url, { attribution, ...options }).addTo(map);
    L.control.zoom({ position: "bottomleft" }).addTo(map);
    mapInstanceRef.current = map;

    const bounds = L.latLngBounds(
      countriesWithCoords.map((c) => [c.lat, c.lng] as L.LatLngTuple)
    );
    countriesWithCoords.forEach(({ country, lat, lng }) => {
      const flagImgUrl = getCountryFlagImageUrl(country);
      const escapedTitle = country.replace(/"/g, "&quot;").replace(/</g, "&lt;");
      const escapedUrl = flagImgUrl.replace(/"/g, "&quot;");
      const marker = L.marker([lat, lng], {
        title: country,
        icon: L.divIcon({
          className: "cafes-probados-marker",
          html: `<span class="cafes-probados-marker-emoji" aria-hidden="true" title="${escapedTitle}"><img src="${escapedUrl}" alt="" width="24" height="24" loading="lazy" /></span>`,
          iconSize: [32, 32],
          iconAnchor: [16, 16]
        })
      })
        .addTo(map)
        .on("click", () => {
          setSelectedCountry(country);
          map.setView([lat, lng], 6);
        });
      markersRef.current.push(marker);
    });
    if (countriesWithCoords.length === 1) {
      map.setView([center.lat, center.lng], 4);
    } else {
      map.fitBounds(bounds, { padding: [40, 40] });
    }

    return () => {
      markersRef.current.forEach((m) => m.remove());
      markersRef.current = [];
      map.remove();
      mapInstanceRef.current = null;
    };
  }, [countriesWithCoords]);

  const hasMap = countriesWithCoords.length > 0;

  useEffect(() => {
    const el = mapWrapRef.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      (entries) => {
        const ratio = entries[0]?.intersectionRatio ?? 1;
        setListScrolled(ratio < 0.8);
      },
      { threshold: [0.25, 0.5, 0.8, 1], rootMargin: "0px" }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  return (
    <div className="cafes-probados-page">
      <header className="cafes-probados-hero" aria-label="Mapa de países de origen">
        <div className="cafes-probados-hero-top-actions" aria-label="Navegación">
          <IconButton
            tone="topbar"
            className="cafes-probados-topbar-icon"
            aria-label="Volver a Mi diario"
            onClick={onBack}
          >
            <UiIcon name="arrow-left" className="ui-icon" />
          </IconButton>
          {selectedCountry ? (
            <IconButton
              tone="topbar"
              className="cafes-probados-topbar-icon"
              aria-label="Limpiar filtro de país"
              onClick={handleClear}
            >
              <UiIcon name="close" className="ui-icon" />
            </IconButton>
          ) : null}
        </div>
        <div
          ref={mapWrapRef}
          className={`cafes-probados-hero-map-block${listScrolled ? " cafes-probados-hero-map-block--collapsed" : ""}`}
        >
          {hasMap ? (
            <div ref={mapRef} className="cafes-probados-map" />
          ) : (
            <div className="cafes-probados-map-placeholder">
              <p>Mapa de países</p>
              <p className="cafes-probados-map-placeholder-hint">Añade cafés a tu diario para ver los países en el mapa.</p>
            </div>
          )}
          <div className="cafes-probados-map-overlay" aria-hidden="true" />
        </div>
      </header>

      <section
        ref={listWrapRef}
        className="cafes-probados-list-wrap"
        aria-label="Listado de cafés"
        onScroll={handleListScroll}
      >
        <div className="cafes-probados-list">
          {coffeesByCountry.map(([country, items]) => (
            <div key={country} className="cafes-probados-country-group">
              <h2 className="cafes-probados-country-subtitle">{country}</h2>
              <ul className="coffee-list cafes-probados-coffee-list">
                {items.map(({ coffee, firstTriedTs }) => (
                  <li key={coffee.id}>
                    <Button
                      variant="plain"
                      type="button"
                      className="coffee-card coffee-card-row coffee-card-interactive"
                      onClick={() => onOpenCoffee(coffee.id)}
                      aria-label={`Ver detalle de ${coffee.nombre}`}
                    >
                      {coffee.image_url ? (
                        <img className="search-coffee-thumb" src={coffee.image_url} alt={`Imagen de ${coffee.nombre}`} loading="lazy" decoding="async" />
                      ) : (
                        <div className="search-coffee-thumb search-coffee-thumb-fallback" aria-hidden="true">
                          <UiIcon name="coffee" className="ui-icon" />
                        </div>
                      )}
                      <div className="search-coffee-copy">
                        <strong>{coffee.nombre}</strong>
                        <p className="coffee-sub">{country}</p>
                        <p className="cafes-probados-first-date">
                          Primera vez: {new Date(firstTriedTs).toLocaleDateString("es-ES", { day: "numeric", month: "short", year: "numeric" })}
                        </p>
                      </div>
                      <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" aria-hidden="true" />
                    </Button>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
