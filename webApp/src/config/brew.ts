export const BREW_METHODS: Array<{ name: string; icon: string }> = [
  { name: "Aeropress", icon: "/brew-methods/maq_aeropress.png" },
  { name: "Chemex", icon: "/brew-methods/maq_chemex.png" },
  { name: "Espresso", icon: "/brew-methods/maq_espresso.png" },
  { name: "Goteo", icon: "/brew-methods/maq_goteo.png" },
  { name: "Hario V60", icon: "/brew-methods/maq_hario_v60.png" },
  { name: "Italiana", icon: "/brew-methods/maq_italiana.png" },
  { name: "Manual", icon: "/brew-methods/maq_manual.png" },
  { name: "Prensa francesa", icon: "/brew-methods/maq_prensa_francesa.png" },
  { name: "Sifón", icon: "/brew-methods/maq_sifon.png" },
  { name: "Turco", icon: "/brew-methods/maq_turco.png" }
];

/** Método especial «Otros»: icono rayo, en elaboración no muestra Configura ni Temporizador. */
export const BREW_METHOD_OTROS: { name: string; icon: string } = { name: "Otros", icon: "bolt" };

/** Método «Agua»: icono gota; en elaboración solo muestra configuración de agua y registra en actividad como agua. */
export const BREW_METHOD_AGUA: { name: string; icon: string } = { name: "Agua", icon: "water" };

export type BrewMethodItem = { name: string; icon: string };

const ALL_METHODS_WITH_SPECIAL: Array<BrewMethodItem> = [...BREW_METHODS, BREW_METHOD_OTROS, BREW_METHOD_AGUA];

const normalizeForMatch = (s: string) => s.toLowerCase().normalize("NFD").replace(/\p{Diacritic}/gu, "");

/** Nombres normalizados (lowercase, sin acentos) de métodos de elaboración para distinguir método vs tipo de bebida. */
const BREW_METHOD_NAMES_NORMALIZED = new Set([
  ...ALL_METHODS_WITH_SPECIAL.map((m) => normalizeForMatch(m.name)),
  "v60"
]);

/** Indica si el texto corresponde a un método de elaboración (V60, Aeropress, Otros, Agua, etc.), no a un tipo de bebida. */
export function isBrewMethodName(text: string): boolean {
  const normalized = (text || "").trim().toLowerCase().normalize("NFD").replace(/\p{Diacritic}/gu, "");
  return normalized !== "" && BREW_METHOD_NAMES_NORMALIZED.has(normalized);
}

/**
 * Orden para mostrar métodos: el último utilizado a la izquierda (referencia: actividad del usuario en el diario), luego el anterior, etc.; los no usados alfabético; Otros y Agua al final si no se usaron.
 * @param diaryEntries entradas de diario (se usa el timestamp de cada entrada para calcular el último uso por método)
 */
export function getOrderedBrewMethods(diaryEntries: Array<{ preparation_type?: string; timestamp?: number; type?: string }>): Array<BrewMethodItem> {
  const byName = new Map(ALL_METHODS_WITH_SPECIAL.map((m) => [m.name, m]));
  const lastUsedTimestamp = new Map<string, number>();
  for (const entry of diaryEntries) {
    let methodName = "";
    if ((entry.type ?? "").toUpperCase() === "WATER") {
      methodName = "Agua";
    } else {
      const prep = (entry.preparation_type ?? "").trim();
      const match = prep.match(/^Lab:\s*([^(]+)/);
      const raw = match?.[1]?.trim() ?? "";
      if (raw && byName.has(raw)) methodName = raw;
      else if (raw?.toLowerCase() === "otros") methodName = "Otros";
    }
    if (methodName) {
      const ts = Number(entry.timestamp ?? 0);
      const cur = lastUsedTimestamp.get(methodName) ?? 0;
      if (ts > cur) lastUsedTimestamp.set(methodName, ts);
    }
  }
  const usedOrder = [...lastUsedTimestamp.entries()]
    .sort((a, b) => b[1] - a[1])
    .map(([name]) => name);
  const used = usedOrder.map((name) => byName.get(name)!).filter(Boolean);
  const unused = BREW_METHODS.filter((m) => !usedOrder.includes(m.name)).sort((a, b) => a.name.localeCompare(b.name, "es"));
  const result: BrewMethodItem[] = [...used, ...unused];
  if (!usedOrder.includes("Otros")) result.push(BREW_METHOD_OTROS);
  if (!usedOrder.includes("Agua")) result.push(BREW_METHOD_AGUA);
  return result;
}

export const COMMENT_EMOJIS = ["😀", "😍", "🤎", "☕", "🔥", "🙌", "👏", "😋", "🥳", "😎"];
