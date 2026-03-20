export function normalizeLookupText(value: string | null | undefined): string {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/['’`´ʼ]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

export function toUiOptionValue(value: string | null | undefined): string {
  const compact = (value ?? "").replace(/\s+/g, " ").trim();
  if (!compact) return "";
  return compact
    .split(" ")
    .map((chunk) => {
      if (!chunk) return chunk;
      const lower = chunk.toLocaleLowerCase("es");
      return lower.charAt(0).toLocaleUpperCase("es") + lower.slice(1);
    })
    .join(" ");
}

export function buildNormalizedOptions(values: Array<string | null | undefined>): string[] {
  const map = new Map<string, string>();
  values.forEach((raw) => {
    splitAtomizedList(raw).forEach((token) => {
      const normalizedKey = normalizeLookupText(token);
      if (!normalizedKey) return;
      if (!map.has(normalizedKey)) {
        map.set(normalizedKey, toUiOptionValue(token));
      }
    });
  });
  return Array.from(map.values()).sort((a, b) => a.localeCompare(b, "es"));
}

export function splitAtomizedList(value: string | null | undefined): string[] {
  return (value ?? "")
    .split(",")
    .map((token) => token.replace(/\s+/g, " ").trim())
    .filter((token) => token.length > 0);
}

export function toEventTimestamp(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const asNumber = Number(value);
    if (Number.isFinite(asNumber) && asNumber > 0) return asNumber;
    const parsed = Date.parse(value);
    if (Number.isFinite(parsed) && parsed > 0) return parsed;
  }
  return 0;
}
