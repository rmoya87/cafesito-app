/**
 * Banderas emoji por país (ISO 3166-1 alpha-2) para marcadores en el mapa.
 * Mismas claves normalizadas que countryCoords.
 */

function normalizeCountryKey(name: string): string {
  return name
    .trim()
    .toLowerCase()
    .normalize("NFD")
    .replace(/\p{Diacritic}/gu, "")
    .replace(/\s+/g, " ");
}

const ISO2: Record<string, string> = {
  colombia: "CO",
  brasil: "BR",
  etiopia: "ET",
  mexico: "MX",
  guatemala: "GT",
  honduras: "HN",
  peru: "PE",
  nicaragua: "NI",
  costa_rica: "CR",
  "costa rica": "CR",
  kenya: "KE",
  kenia: "KE",
  indonesia: "ID",
  vietnam: "VN",
  viet_nam: "VN",
  india: "IN",
  rwanda: "RW",
  tanzania: "TZ",
  uganda: "UG",
  papua_nueva_guinea: "PG",
  "papua nueva guinea": "PG",
  ecuador: "EC",
  bolivia: "BO",
  el_salvador: "SV",
  "el salvador": "SV",
  yemen: "YE",
  jamaica: "JM",
  haiti: "HT",
  republica_dominicana: "DO",
  "república dominicana": "DO",
  camerun: "CM",
  china: "CN",
  tailandia: "TH",
  thailand: "TH",
  filipinas: "PH",
  laos: "LA",
  myanmar: "MM",
  timor_leste: "TL",
  "timor oriental": "TL",
  madagascar: "MG",
  zambia: "ZM",
  malawi: "MW",
  zimbabwe: "ZW",
  venezuela: "VE",
  panama: "PA",
  panamá: "PA",
  cuba: "CU",
  puerto_rico: "PR",
  "puerto rico": "PR",
  espana: "ES",
  españa: "ES",
  italia: "IT",
  portugal: "PT",
  francia: "FR",
  alemania: "DE",
  reino_unido: "GB",
  "reino unido": "GB",
  usa: "US",
  eeuu: "US",
  "estados unidos": "US",
  australia: "AU",
  japon: "JP",
  japón: "JP",
  corea_del_sur: "KR",
  "corea del sur": "KR",
  taiwan: "TW",
  taiwán: "TW",
  nueva_zelanda: "NZ",
  "nueva zelanda": "NZ",
  hawai: "US",
  hawái: "US",
  burundi: "BI",
};

/** Convierte código ISO 3166-1 alpha-2 a emoji de bandera (regional indicators). */
function isoToFlagEmoji(iso: string): string {
  if (iso.length !== 2) return "🌍";
  const a = 0x1f1e6 + (iso.charCodeAt(0) - 65);
  const b = 0x1f1e6 + (iso.charCodeAt(1) - 65);
  return String.fromCodePoint(a, b);
}

/**
 * Devuelve el emoji de bandera para un nombre de país (p. ej. "Colombia" → "🇨🇴").
 * Si no hay código, devuelve "🌍".
 */
export function getCountryFlagEmoji(countryName: string): string {
  if (!countryName?.trim()) return "🌍";
  const key = normalizeCountryKey(countryName);
  const code = ISO2[key];
  return code ? isoToFlagEmoji(code) : "🌍";
}

const TWEMOJI_BASE = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72";

/**
 * Devuelve la URL de la imagen Twemoji para la bandera del país.
 * Así la bandera se ve igual en todos los navegadores sin depender de fuentes del sistema.
 */
export function getCountryFlagImageUrl(countryName: string): string {
  const emoji = getCountryFlagEmoji(countryName);
  const codepoints = [...emoji].map((c) => (c.codePointAt(0) ?? 0).toString(16));
  const filename = codepoints.join("-") + ".png";
  return `${TWEMOJI_BASE}/${filename}`;
}
