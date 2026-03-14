/**
 * Coordenadas aproximadas por país (origen de café) para posicionar marcadores en el mapa.
 * Claves normalizadas a minúsculas sin acentos para matching con pais_origen.
 */

function normalizeCountryKey(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "");
}

const COORDS: Record<string, { lat: number; lng: number }> = {
  colombia: { lat: 4.57, lng: -74.29 },
  brasil: { lat: -14.24, lng: -51.92 },
  etiopia: { lat: 9.03, lng: 38.74 },
  etiopía: { lat: 9.03, lng: 38.74 },
  mexico: { lat: 23.63, lng: -102.55 },
  méxico: { lat: 23.63, lng: -102.55 },
  guatemala: { lat: 15.78, lng: -90.23 },
  honduras: { lat: 15.2, lng: -86.24 },
  peru: { lat: -9.19, lng: -75.02 },
  perú: { lat: -9.19, lng: -75.02 },
  nicaragua: { lat: 12.87, lng: -85.21 },
  costa_rica: { lat: 9.75, lng: -83.75 },
  "costa rica": { lat: 9.75, lng: -83.75 },
  kenya: { lat: -0.02, lng: 37.91 },
  kenia: { lat: -0.02, lng: 37.91 },
  indonesia: { lat: -0.79, lng: 113.92 },
  vietnam: { lat: 14.06, lng: 108.28 },
  viet_nam: { lat: 14.06, lng: 108.28 },
  india: { lat: 20.59, lng: 78.96 },
  rwanda: { lat: -1.94, lng: 29.87 },
  tanzania: { lat: -6.37, lng: 34.89 },
  uganda: { lat: 1.37, lng: 32.29 },
  papua_nueva_guinea: { lat: -6.31, lng: 143.96 },
  "papua nueva guinea": { lat: -6.31, lng: 143.96 },
  ecuador: { lat: -1.83, lng: -78.18 },
  bolivia: { lat: -16.29, lng: -63.59 },
  el_salvador: { lat: 13.79, lng: -88.9 },
  "el salvador": { lat: 13.79, lng: -88.9 },
  yemen: { lat: 15.55, lng: 48.52 },
  jamaica: { lat: 18.11, lng: -77.3 },
  haiti: { lat: 18.97, lng: -72.29 },
  haití: { lat: 18.97, lng: -72.29 },
  republica_dominicana: { lat: 18.74, lng: -70.16 },
  "república dominicana": { lat: 18.74, lng: -70.16 },
  camerun: { lat: 6.37, lng: 12.35 },
  camerún: { lat: 6.37, lng: 12.35 },
  burundi: { lat: -3.37, lng: 29.92 },
  china: { lat: 35.86, lng: 104.2 },
  tailandia: { lat: 15.87, lng: 100.99 },
  thailand: { lat: 15.87, lng: 100.99 },
  filipinas: { lat: 12.88, lng: 121.77 },
  laos: { lat: 19.86, lng: 102.5 },
  myanmar: { lat: 21.91, lng: 95.96 },
  timor_leste: { lat: -8.87, lng: 125.73 },
  "timor oriental": { lat: -8.87, lng: 125.73 },
  madagascar: { lat: -18.77, lng: 46.87 },
  zambia: { lat: -13.13, lng: 27.85 },
  malawi: { lat: -13.25, lng: 34.3 },
  zimbabwe: { lat: -19.02, lng: 29.15 },
  venezuela: { lat: 6.42, lng: -66.59 },
  panama: { lat: 8.54, lng: -80.78 },
  panamá: { lat: 8.54, lng: -80.78 },
  cuba: { lat: 21.52, lng: -77.78 },
  puerto_rico: { lat: 18.22, lng: -66.59 },
  "puerto rico": { lat: 18.22, lng: -66.59 },
  espana: { lat: 40.46, lng: -3.75 },
  españa: { lat: 40.46, lng: -3.75 },
  italia: { lat: 41.87, lng: 12.57 },
  portugal: { lat: 39.4, lng: -8.22 },
  francia: { lat: 46.23, lng: 2.21 },
  alemania: { lat: 51.17, lng: 10.45 },
  reino_unido: { lat: 55.38, lng: -3.44 },
  "reino unido": { lat: 55.38, lng: -3.44 },
  usa: { lat: 37.09, lng: -95.71 },
  eeuu: { lat: 37.09, lng: -95.71 },
  "estados unidos": { lat: 37.09, lng: -95.71 },
  australia: { lat: -25.27, lng: 133.78 },
  japon: { lat: 36.2, lng: 138.25 },
  japón: { lat: 36.2, lng: 138.25 },
  corea_del_sur: { lat: 35.91, lng: 127.77 },
  "corea del sur": { lat: 35.91, lng: 127.77 },
  taiwan: { lat: 23.7, lng: 120.96 },
  taiwán: { lat: 23.7, lng: 120.96 },
  nueva_zelanda: { lat: -40.9, lng: 174.89 },
  "nueva zelanda": { lat: -40.9, lng: 174.89 },
  hawai: { lat: 19.9, lng: -155.58 },
  hawái: { lat: 19.9, lng: -155.58 },
  blend: { lat: 20, lng: 0 },
  blends: { lat: 20, lng: 0 },
  varios: { lat: 20, lng: 0 },
  "varios origenes": { lat: 20, lng: 0 },
  "varios orígenes": { lat: 20, lng: 0 },
};

export function getCountryCoords(countryName: string): { lat: number; lng: number } | null {
  if (!countryName || !countryName.trim()) return null;
  const key = normalizeCountryKey(countryName);
  return COORDS[key] ?? null;
}

export function getCountryKey(countryName: string): string {
  return normalizeCountryKey(countryName);
}
