import type { BrewStep } from "../types";
import { normalizeLookupText } from "./text";

/** Contrato compartido con Android/shared: mismo JSON que BrewSharePayload (Kotlin). */
export type BrewSharePayload = {
  method: string;
  coffeeId?: string | null;
  coffeeName?: string | null;
  waterMl: number;
  ratio: number;
  espresso_sec?: number | null;
};

const BREW_SHARE_BASE = "https://cafesitoapp.com/brewlab";

function base64UrlEncodeUtf8(str: string): string {
  if (typeof btoa === "undefined") return "";
  const binary = encodeURIComponent(str).replace(/%([0-9a-fA-F]{2})/g, (_, hex) =>
    String.fromCharCode(Number.parseInt(hex, 16))
  );
  return btoa(binary);
}

function base64UrlDecodeUtf8(b64: string): string {
  if (typeof atob === "undefined") return "";
  const binary = atob(b64);
  return decodeURIComponent(
    Array.from(binary, (c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2)).join("")
  );
}

export function buildBrewShareUrl(payload: BrewSharePayload): string {
  const json = JSON.stringify(payload);
  const encoded = base64UrlEncodeUtf8(json);
  return `${BREW_SHARE_BASE}?p=${encodeURIComponent(encoded)}`;
}

export function parseBrewSharePayloadFromSearchParams(searchParams: URLSearchParams): BrewSharePayload | null {
  const p = searchParams.get("p");
  if (!p) return null;
  try {
    const decoded = base64UrlDecodeUtf8(decodeURIComponent(p));
    const parsed = JSON.parse(decoded) as unknown;
    if (parsed && typeof parsed === "object" && "method" in parsed && "waterMl" in parsed && "ratio" in parsed) {
      return {
        method: String((parsed as BrewSharePayload).method),
        coffeeId: (parsed as BrewSharePayload).coffeeId ?? null,
        coffeeName: (parsed as BrewSharePayload).coffeeName ?? null,
        waterMl: Number((parsed as BrewSharePayload).waterMl),
        ratio: Number((parsed as BrewSharePayload).ratio),
        espresso_sec: (parsed as BrewSharePayload).espresso_sec ?? null
      };
    }
  } catch {
    // ignore
  }
  return null;
}

export type BrewPhaseInfo = {
  label: string;
  instruction: string;
  durationSeconds: number;
};

export type BrewBaristaTip = {
  label: string;
  value: string;
  icon: "grind" | "thermostat" | "water" | "clock" | "coffee";
};
export type BrewBaristaContext = {
  ratio?: number;
  waterMl?: number;
  coffeeGrams?: number;
  brewTimeSeconds?: number;
};

export type BrewMethodProfile = {
  waterMinMl: number;
  waterMaxMl: number;
  waterStepMl: number;
  defaultWaterMl: number;
  ratioMin: number;
  ratioMax: number;
  ratioStep: number;
  defaultRatio: number;
};

export type BrewTimeProfile = {
  minSeconds: number;
  maxSeconds: number;
  defaultSeconds: number;
};

const DEFAULT_METHOD_PROFILE: BrewMethodProfile = {
  waterMinMl: 150,
  waterMaxMl: 600,
  waterStepMl: 10,
  defaultWaterMl: 300,
  ratioMin: 14,
  ratioMax: 18,
  ratioStep: 0.5,
  defaultRatio: 16
};

const DEFAULT_TIME_PROFILE: BrewTimeProfile = {
  minSeconds: 90,
  maxSeconds: 360,
  defaultSeconds: 180
};

/** Límites libres: el usuario puede poner la cantidad de agua y café que quiera. Ratio y forma se calculan en base a ello. */
export const BREW_WATER_ABS_MIN_ML = 1;
export const BREW_WATER_ABS_MAX_ML = 5000;
export const BREW_COFFEE_ABS_MIN_G = 0.5;
export const BREW_COFFEE_ABS_MAX_G = 2000;

/** Máximos solo del slider; el input numérico sigue permitiendo hasta ABS_MAX. */
export const BREW_SLIDER_MAX_WATER_ML = 1000;
export const BREW_SLIDER_MAX_COFFEE_G = 250;
export const BREW_SLIDER_MAX_TIME_S = 60;

export function getBrewMethodProfile(method: string): BrewMethodProfile {
  const key = normalizeLookupText(method);
  if (key.includes("agua")) {
    return {
      waterMinMl: 50,
      waterMaxMl: 2000,
      waterStepMl: 10,
      defaultWaterMl: 250,
      ratioMin: 14,
      ratioMax: 18,
      ratioStep: 0.5,
      defaultRatio: 16
    };
  }
  if (key.includes("espresso")) {
    return {
      waterMinMl: 25,
      waterMaxMl: 60,
      waterStepMl: 1,
      defaultWaterMl: 36,
      ratioMin: 1.8,
      ratioMax: 2.8,
      ratioStep: 0.1,
      defaultRatio: 2.0
    };
  }
  if (key.includes("italiana")) {
    return {
      waterMinMl: 60,
      waterMaxMl: 320,
      waterStepMl: 5,
      defaultWaterMl: 150,
      ratioMin: 7.5,
      ratioMax: 12,
      ratioStep: 0.5,
      defaultRatio: 10
    };
  }
  if (key.includes("turco")) {
    return {
      waterMinMl: 60,
      waterMaxMl: 180,
      waterStepMl: 5,
      defaultWaterMl: 90,
      ratioMin: 8,
      ratioMax: 12,
      ratioStep: 0.5,
      defaultRatio: 10
    };
  }
  if (key.includes("aeropress")) {
    return {
      waterMinMl: 150,
      waterMaxMl: 300,
      waterStepMl: 5,
      defaultWaterMl: 220,
      ratioMin: 13,
      ratioMax: 17,
      ratioStep: 0.5,
      defaultRatio: 15
    };
  }
  if (key.includes("prensa")) {
    return {
      waterMinMl: 250,
      waterMaxMl: 1000,
      waterStepMl: 10,
      defaultWaterMl: 400,
      ratioMin: 12,
      ratioMax: 16,
      ratioStep: 0.5,
      defaultRatio: 14.5
    };
  }
  if (key.includes("chemex")) {
    return {
      waterMinMl: 300,
      waterMaxMl: 900,
      waterStepMl: 10,
      defaultWaterMl: 500,
      ratioMin: 14,
      ratioMax: 17,
      ratioStep: 0.5,
      defaultRatio: 15.5
    };
  }
  if (key.includes("goteo")) {
    return {
      waterMinMl: 200,
      waterMaxMl: 1200,
      waterStepMl: 10,
      defaultWaterMl: 400,
      ratioMin: 15,
      ratioMax: 18,
      ratioStep: 0.5,
      defaultRatio: 16.5
    };
  }
  if (key.includes("hario") || key.includes("v60")) {
    return {
      waterMinMl: 180,
      waterMaxMl: 500,
      waterStepMl: 5,
      defaultWaterMl: 300,
      ratioMin: 14,
      ratioMax: 17,
      ratioStep: 0.5,
      defaultRatio: 16
    };
  }
  if (key.includes("sifon")) {
    return {
      waterMinMl: 200,
      waterMaxMl: 700,
      waterStepMl: 10,
      defaultWaterMl: 350,
      ratioMin: 14,
      ratioMax: 17,
      ratioStep: 0.5,
      defaultRatio: 15
    };
  }
  if (key.includes("manual")) {
    return {
      waterMinMl: 150,
      waterMaxMl: 600,
      waterStepMl: 10,
      defaultWaterMl: 300,
      ratioMin: 14,
      ratioMax: 18,
      ratioStep: 0.5,
      defaultRatio: 16
    };
  }
  return DEFAULT_METHOD_PROFILE;
}

export function getBrewTimeProfile(method: string): BrewTimeProfile {
  const key = normalizeLookupText(method);
  if (key.includes("espresso")) return { minSeconds: 20, maxSeconds: 40, defaultSeconds: 27 };
  if (key.includes("italiana")) return { minSeconds: 120, maxSeconds: 360, defaultSeconds: 210 };
  if (key.includes("turco")) return { minSeconds: 90, maxSeconds: 240, defaultSeconds: 160 };
  if (key.includes("aeropress")) return { minSeconds: 90, maxSeconds: 240, defaultSeconds: 150 };
  if (key.includes("prensa")) return { minSeconds: 180, maxSeconds: 420, defaultSeconds: 240 };
  if (key.includes("chemex")) return { minSeconds: 180, maxSeconds: 360, defaultSeconds: 240 };
  if (key.includes("goteo")) return { minSeconds: 180, maxSeconds: 420, defaultSeconds: 300 };
  if (key.includes("hario") || key.includes("v60") || key.includes("manual")) return { minSeconds: 120, maxSeconds: 330, defaultSeconds: 195 };
  if (key.includes("sifon")) return { minSeconds: 120, maxSeconds: 300, defaultSeconds: 195 };
  return DEFAULT_TIME_PROFILE;
}

/** Valoración breve de la configuración actual; dinámica según método (y ratio/agua si aplica). */
export function getBrewConfigAdvice(method: string, ratio: number, waterMl: number): string {
  const key = normalizeLookupText(method);
  if (key.includes("agua")) {
    return "Configuración aplicada para agua; ajusta la cantidad con los consejos del barista.";
  }
  if (key.includes("espresso")) {
    return "Configuración aplicada para espresso; afina con los consejos del barista.";
  }
  if (key.includes("italiana") || key.includes("moka")) {
    return "Configuración aplicada para italiana; afina molienda y fuego con los consejos del barista.";
  }
  if (key.includes("aeropress")) {
    return "Configuración aplicada para Aeropress; afina molienda, tiempo y presión con los consejos del barista.";
  }
  if (key.includes("prensa") || key.includes("french")) {
    return "Configuración aplicada para prensa; afina molienda e inmersión con los consejos del barista.";
  }
  if (key.includes("v60") || key.includes("chemex") || key.includes("filtro") || key.includes("goteo") || key.includes("hario") || key.includes("manual")) {
    return "Configuración aplicada para filtro; afina molienda, vertido y cuerpo con los consejos del barista.";
  }
  if (key.includes("sifon")) {
    return "Configuración aplicada para sifón; afina molienda y tiempos con los consejos del barista.";
  }
  if (key.includes("turco")) {
    return "Configuración aplicada para turco; afina molienda y temperatura con los consejos del barista.";
  }
  if (key.includes("otros") || key.includes("rapido")) {
    return "Configuración aplicada para elaboración rápida; elige tipo de bebida y usa los consejos del barista.";
  }
  return "Configuración aplicada; afina molienda, vertido y cuerpo con los consejos del barista.";
}

export function getBrewStepTitle(step: BrewStep): string {
  if (step === "method") return "ELABORACION";
  if (step === "coffee") return "ELIGE TU CAFÉ";
  if (step === "config") return "CONFIGURA";
  if (step === "brewing") return "PROCESO EN CURSO";
  return "RESULTADO";
}

export function getBrewTimelineForMethod(
  method: string,
  waterMl: number,
  espressoSeconds?: number,
  targetTotalSeconds?: number
): BrewPhaseInfo[] {
  const key = normalizeLookupText(method);
  if (key.includes("espresso")) {
    const extractionSeconds = Math.max(20, Math.min(40, Math.round(Number(espressoSeconds) || 25)));
    return [
      {
        label: "Extraccion",
        instruction: "Aplica presión constante. Vigila el flujo: debe ser como un hilo de miel. Busca obtener unos 36-40g de líquido final.",
        durationSeconds: extractionSeconds
      }
    ];
  }
  if (key.includes("prensa")) {
    return scaleTimelineToTarget([
      {
        label: "Inmersión",
        instruction: "Vierte todo el agua caliente uniformemente sobre el café. Coloca la tapa sin presionar para mantener el calor.",
        durationSeconds: 240
      }
    ], targetTotalSeconds);
  }
  if (key.includes("aeropress")) {
    return scaleTimelineToTarget([
      {
        label: "Pre-infusión",
        instruction: "Vierte unos 50ml de agua para humedecer todo el café. Remueve suavemente 3 veces para asegurar una extracción uniforme.",
        durationSeconds: 30
      },
      {
        label: "Infusión",
        instruction: "Añade el resto del agua. Deja que el café repose e interactúe con el agua para extraer todos sus sabores.",
        durationSeconds: 90
      },
      {
        label: "Presión",
        instruction: "Presiona el émbolo hacia abajo con una fuerza firme y constante. Escucha el 'sssh' final y detente.",
        durationSeconds: 30
      }
    ], targetTotalSeconds);
  }
  if (key.includes("italiana")) {
    const boilTime = Math.round(120 + waterMl * 0.2);
    return scaleTimelineToTarget([
      {
        label: "Calentamiento",
        instruction: "Mantén el fuego medio-bajo. El agua en la base empezará a crear presión para subir por la chimenea.",
        durationSeconds: boilTime
      },
      {
        label: "Extraccion",
        instruction: "Cuando el café empiece a salir, baja el fuego o retíralo. Escucha el burbujeo suave y detente antes del chorro final.",
        durationSeconds: 40
      }
    ], targetTotalSeconds);
  }
  if (key.includes("turco")) {
    return scaleTimelineToTarget([
      {
        label: "Infusión",
        instruction: "Calienta a fuego muy lento hasta que veas que se forma una espuma densa y oscura en la superficie (crema).",
        durationSeconds: 120
      },
      {
        label: "Levantamiento 1",
        instruction: "Retira el cezve del fuego justo antes de que hierva. Deja que la espuma baje un poco y vuelve al fuego.",
        durationSeconds: 20
      },
      {
        label: "Levantamiento 2",
        instruction: "Repite el proceso: deja que suba la espuma por segunda vez para intensificar el cuerpo y sabor.",
        durationSeconds: 20
      },
      {
        label: "Toque Final",
        instruction: "Último ciclo de espuma. El café turco se caracteriza por su densidad y su sedimento único.",
        durationSeconds: 20
      }
    ], targetTotalSeconds);
  }
  if (key.includes("sifon")) {
    return scaleTimelineToTarget([
      {
        label: "Ascenso",
        instruction: "La presión enviará el agua a la cámara superior. Espera a que se estabilice antes de añadir el café.",
        durationSeconds: 90
      },
      {
        label: "Mezcla",
        instruction: "Añade el café molido y remueve en círculos suavemente. Asegúrate de que todo el café esté sumergido.",
        durationSeconds: 60
      },
      {
        label: "Efecto Vacío",
        instruction: "Retira la fuente de calor. El enfriamiento creará un vacío que filtrará el café hacia abajo a través del filtro.",
        durationSeconds: 45
      }
    ], targetTotalSeconds);
  }

  const totalPourTime = Math.max(90, Math.min(300, Math.round(120 + (waterMl - 250) * 0.18)));
  const bloomMl = Math.floor(waterMl / 10);
  const pourMl = waterMl - bloomMl;
  const phases = [
    {
      label: "Bloom",
      instruction: `Humedece el café con ${bloomMl}ml de agua y espera a que libere CO2.` ,
      durationSeconds: 30
    },
    {
      label: "Vertido Principal",
      instruction: `Vierte ${pourMl}ml en círculos lentos y controlados.` ,
      durationSeconds: totalPourTime
    },
    {
      label: "Drenado",
      instruction: "Deja que el lecho termine de drenar para completar la extracción.",
      durationSeconds: 35
    }
  ];
  return scaleTimelineToTarget(phases, targetTotalSeconds);
}

export function getBrewBaristaTipsForMethod(method: string, context?: BrewBaristaContext): BrewBaristaTip[] {
  const key = normalizeLookupText(method);
  const defaults: BrewBaristaTip[] = [
    { label: "MOLIENDA", value: "Media", icon: "grind" },
    { label: "TEMPERATURA", value: "92-96°C", icon: "thermostat" },
    { label: "RATIO", value: "1:15 a 1:17", icon: "coffee" },
    { label: "BLOOM", value: "30-45s con 2x de agua", icon: "water" },
    { label: "VERTIDO", value: "Constante y en espiral", icon: "water" },
    { label: "TIEMPO", value: "2:30-3:30", icon: "clock" },
    { label: "AJUSTE ACIDEZ", value: "Muele más fino", icon: "grind" },
    { label: "AJUSTE AMARGOR", value: "Muele más grueso", icon: "grind" }
  ];
  const baseTips: BrewBaristaTip[] = !key
    ? defaults
    : key.includes("espresso")
      ? [
        { label: "MOLIENDA", value: "Fina", icon: "grind" },
        { label: "TEMPERATURA", value: "90-94°C", icon: "thermostat" },
        { label: "RATIO", value: "1:2 (ej. 18g -> 36g)", icon: "coffee" },
        { label: "TIEMPO", value: "25-32s", icon: "clock" },
        { label: "DISTRIBUCIÓN", value: "Nivela antes del tamp", icon: "coffee" },
        { label: "PREINFUSIÓN", value: "Suave para evitar canalización", icon: "water" },
        { label: "AJUSTE RÁPIDO", value: "Si corre rápido, más fino", icon: "grind" },
        { label: "AJUSTE LENTO", value: "Si se ahoga, más grueso", icon: "grind" }
      ]
      : key.includes("italiana")
        ? [
          { label: "MOLIENDA", value: "Media-fina", icon: "grind" },
          { label: "AGUA", value: "Caliente en base", icon: "water" },
          { label: "FUEGO", value: "Medio-bajo", icon: "thermostat" },
          { label: "CORTE", value: "Retira al primer burbujeo", icon: "clock" },
          { label: "FILTRO", value: "No compactar café", icon: "coffee" },
          { label: "RATIO", value: "Más café para más cuerpo", icon: "coffee" },
          { label: "AMARGOR", value: "Evita fuego alto", icon: "thermostat" }
        ]
        : key.includes("aeropress")
          ? [
            { label: "MOLIENDA", value: "Fina-media", icon: "grind" },
            { label: "TEMPERATURA", value: "85-92°C", icon: "thermostat" },
            { label: "INFUSIÓN", value: "1:30-2:00", icon: "clock" },
            { label: "PRESION", value: "Suave y constante", icon: "coffee" },
            { label: "REMOVIDO", value: "1-2 agitaciones suaves", icon: "water" },
            { label: "PAPEL", value: "Más limpieza en taza", icon: "coffee" },
            { label: "METAL", value: "Más cuerpo y textura", icon: "coffee" }
          ]
          : key.includes("chemex")
            ? [
              { label: "MOLIENDA", value: "Media-gruesa", icon: "grind" },
              { label: "TEMPERATURA", value: "93-96°C", icon: "thermostat" },
              { label: "FILTRO", value: "Enjuague generoso", icon: "water" },
              { label: "TIEMPO", value: "3:30-4:30", icon: "clock" },
              { label: "VERTIDO", value: "Pausado, sin colapsar filtro", icon: "water" },
              { label: "RATIO", value: "1:15 a 1:16", icon: "coffee" },
              { label: "AJUSTE LENTO", value: "Si drena lento, más grueso", icon: "grind" }
            ]
            : key.includes("prensa")
              ? [
                { label: "MOLIENDA", value: "Gruesa y uniforme", icon: "grind" },
                { label: "TEMPERATURA", value: "93-96°C", icon: "thermostat" },
                { label: "INFUSIÓN", value: "4:00", icon: "clock" },
                { label: "PRENSADO", value: "Lento, sin golpear", icon: "coffee" },
                { label: "COSTRA", value: "Romper y retirar espuma", icon: "water" },
                { label: "RATIO", value: "1:14 a 1:16", icon: "coffee" },
                { label: "DECANTAR", value: "Servir al terminar", icon: "clock" }
              ]
              : key.includes("sifon")
                ? [
                  { label: "MOLIENDA", value: "Media", icon: "grind" },
                  { label: "TEMPERATURA", value: "91-94°C", icon: "thermostat" },
                  { label: "AGITACION", value: "Suave y breve", icon: "water" },
                  { label: "BAJADA", value: "45-60s al vacío", icon: "clock" },
                  { label: "HERVOR", value: "Controlado, no violento", icon: "thermostat" },
                  { label: "CONTACTO", value: "1:30-2:30 total", icon: "clock" },
                  { label: "FILTRO", value: "Limpio para evitar rancidez", icon: "coffee" }
                ]
                : key.includes("turco")
                  ? [
                    { label: "MOLIENDA", value: "Extra fina", icon: "grind" },
                    { label: "FUEGO", value: "Muy bajo", icon: "thermostat" },
                    { label: "ESPUMA", value: "3 levantamientos", icon: "coffee" },
                    { label: "AGUA", value: "Casi ebullición, no hervir", icon: "water" },
                    { label: "REMOVIDO", value: "Solo al inicio", icon: "water" },
                    { label: "DESCANSO", value: "Breve antes de servir", icon: "clock" },
                    { label: "DENSIDAD", value: "Taza corta y concentrada", icon: "coffee" }
                  ]
                  : key.includes("goteo")
                    ? [
                      { label: "MOLIENDA", value: "Media", icon: "grind" },
                      { label: "RATIO", value: "55-65g por litro", icon: "coffee" },
                      { label: "TEMPERATURA", value: "92-96°C", icon: "thermostat" },
                      { label: "SERVICIO", value: "Consumir recién hecho", icon: "clock" },
                      { label: "FILTRO", value: "Enjuagar antes de usar", icon: "water" },
                      { label: "CARGA", value: "Nivelar cama de café", icon: "coffee" },
                      { label: "PLACA", value: "Evitar sobrecalentamiento", icon: "thermostat" }
                    ]
                    : key.includes("hario") || key.includes("v60") || key.includes("manual")
                      ? [
                        { label: "MOLIENDA", value: "Media-fina", icon: "grind" },
                        { label: "BLOOM", value: "30-45s con 2x de agua", icon: "water" },
                        { label: "TEMPERATURA", value: "92-96°C", icon: "thermostat" },
                        { label: "TIEMPO", value: "2:30-3:15", icon: "clock" },
                        { label: "RATIO", value: "1:15 a 1:17", icon: "coffee" },
                        { label: "VERTIDO", value: "Pulsos cortos y constantes", icon: "water" },
                        { label: "AJUSTE ACIDEZ", value: "Muele más fino", icon: "grind" },
                        { label: "AJUSTE AMARGOR", value: "Muele más grueso", icon: "grind" }
                      ]
                      : defaults;

  const compactBaseTips = compactBaristaFixedTips(baseTips);
  const profile = getBrewMethodProfile(method);
  const hasConfig = Boolean(
    context &&
    typeof context.waterMl === "number" &&
    typeof context.ratio === "number"
  );
  if (!hasConfig) return compactBaseTips;

  const ratio = Number(context?.ratio ?? profile.defaultRatio);
  const waterMl = Math.max(profile.waterMinMl, Math.min(profile.waterMaxMl, Math.round(Number(context?.waterMl ?? profile.defaultWaterMl))));
  const coffeeGrams = Math.max(1, Number(context?.coffeeGrams ?? (waterMl / Math.max(0.1, ratio))));
  const ratioSpan = Math.max(0.1, profile.ratioMax - profile.ratioMin);
  const ratioNormalized = (ratio - profile.ratioMin) / ratioSpan;
  const waterSpan = Math.max(1, profile.waterMaxMl - profile.waterMinMl);
  const waterNormalized = (waterMl - profile.waterMinMl) / waterSpan;
  const isEspresso = key.includes("espresso");

  const dynamicTips: BrewBaristaTip[] = [
    ratioNormalized <= 0.3
      ? { label: "PERFIL ACTUAL", value: "Más concentrado; si amarga, abre punto de molienda.", icon: "grind" }
      : ratioNormalized >= 0.7
        ? { label: "PERFIL ACTUAL", value: "Más ligero; si queda acuoso, muele un poco más fino.", icon: "grind" }
        : { label: "PERFIL ACTUAL", value: "Equilibrado; mantén ritmo y distribución constantes.", icon: "coffee" },
    waterNormalized <= 0.33
      ? { label: "VOLUMEN", value: "Tramo corto del método: prioriza control y uniformidad.", icon: "water" }
      : waterNormalized >= 0.66
        ? { label: "VOLUMEN", value: "Tramo alto del método: evita dilución con vertido estable.", icon: "water" }
        : { label: "VOLUMEN", value: "Tramo medio: buen balance entre cuerpo y claridad.", icon: "water" }
  ];

  if (isEspresso) {
    const timeProfile = getBrewTimeProfile(method);
    const time = Math.max(timeProfile.minSeconds, Math.min(timeProfile.maxSeconds, Math.round(Number(context?.brewTimeSeconds ?? timeProfile.defaultSeconds))));
    dynamicTips.push(
      time < 25
        ? { label: "TIEMPO ACTUAL", value: "Corto: sube 1-2 s o afina molienda para más extracción.", icon: "clock" }
        : time > 32
          ? { label: "TIEMPO ACTUAL", value: "Largo: baja 1-2 s o abre molienda para evitar amargor.", icon: "clock" }
          : { label: "TIEMPO ACTUAL", value: "En ventana ideal: busca flujo continuo y crema uniforme.", icon: "clock" },
      coffeeGrams < 16
        ? { label: "DOSIS", value: "Baja para espresso; puedes subirla si buscas más cuerpo.", icon: "coffee" }
        : coffeeGrams > 20
          ? { label: "DOSIS", value: "Alta para espresso; cuida no sobre-extraer.", icon: "coffee" }
          : { label: "DOSIS", value: "Dentro de rango clásico para espresso.", icon: "coffee" }
    );
  } else {
    dynamicTips.push(
      { label: "RATIO ACTUAL", value: ratio <= profile.defaultRatio ? "Más intenso; vierte suave para mantener dulzor." : "Más limpio; si falta cuerpo, sube extracción.", icon: "coffee" }
    );
  }

  return [...dynamicTips, ...compactBaseTips];
}

function compactBaristaFixedTips(tips: BrewBaristaTip[]): BrewBaristaTip[] {
  if (!tips.length) return tips;
  const baseBlocks: string[] = [];
  const processBlocks: string[] = [];
  const adjustBlocks: string[] = [];
  const extraBlocks: string[] = [];

  for (const tip of tips) {
    const key = normalizeLookupText(tip.label);
    const block = `${tip.label}: ${tip.value}`;
    const isBase = key.includes("molienda") || key.includes("temperatura") || key.includes("ratio");
    const isAdjust = key.includes("ajuste") || key.includes("acidez") || key.includes("amargor");
    const isProcess =
      key.includes("tiempo") ||
      key.includes("bloom") ||
      key.includes("vertido") ||
      key.includes("infusion") ||
      key.includes("presion") ||
      key.includes("preinfusion") ||
      key.includes("filtro") ||
      key.includes("fuego") ||
      key.includes("agua") ||
      key.includes("removido") ||
      key.includes("contacto") ||
      key.includes("corte") ||
      key.includes("servicio");

    if (isBase) baseBlocks.push(block);
    else if (isAdjust) adjustBlocks.push(block);
    else if (isProcess) processBlocks.push(block);
    else extraBlocks.push(block);
  }

  const compact: BrewBaristaTip[] = [];
  if (baseBlocks.length) compact.push({ label: "BASE", value: baseBlocks.join(" · "), icon: "coffee" });
  if (processBlocks.length) compact.push({ label: "PROCESO", value: processBlocks.join(" · "), icon: "water" });
  if (adjustBlocks.length) compact.push({ label: "AJUSTE", value: adjustBlocks.join(" · "), icon: "grind" });
  if (extraBlocks.length) compact.push({ label: "DETALLE", value: extraBlocks.join(" · "), icon: "clock" });
  return compact.length ? compact : tips;
}

export function getBrewingProcessAdvice(
  method: string,
  ratio: number,
  waterMl: number,
  phaseLabel: string,
  remainingInPhaseSeconds: number,
  brewTimeSeconds?: number
): string {
  const key = normalizeLookupText(method);
  const phase = normalizeLookupText(phaseLabel);
  const profile = getBrewMethodProfile(method);
  const span = Math.max(0.1, profile.ratioMax - profile.ratioMin);
  const normalized = (ratio - profile.ratioMin) / span;

  const extractionTip = key.includes("espresso")
    ? (() => {
      const time = Math.max(20, Math.min(40, Math.round(Number(brewTimeSeconds) || 27)));
      if (time < 25) return "Extraccion corta: puede quedar acida; afina molienda o sube 1-2 s.";
      if (time <= 32) return "Extraccion en ventana ideal: mantén flujo estable y crema uniforme.";
      return "Extraccion larga: puede amargar; abre molienda o corta antes.";
    })()
    : normalized <= 0.3
      ? "Perfil concentrado: usa vertido suave y evita agitar de mas."
      : normalized >= 0.7
        ? "Perfil ligero: para mas cuerpo, aumenta un poco contacto o finura."
        : "Perfil equilibrado: manten ritmo y flujo constantes.";

  const phaseTip =
    phase.includes("bloom") || phase.includes("preinfusion")
      ? "Asegura saturacion completa del lecho antes de continuar."
      : phase.includes("vertido") || phase.includes("mezcla")
        ? "Mantiene altura corta de vertido para no canalizar."
        : phase.includes("extraccion") || phase.includes("presion")
          ? "Controla el flujo: si acelera demasiado, corrige mas fino."
          : phase.includes("inmersion") || phase.includes("infusion")
            ? "Mantiene temperatura estable, sin remover en exceso."
            : "Busca consistencia de flujo y lecho uniforme.";

  const timeTip =
    remainingInPhaseSeconds <= 5
      ? `Cierra esta fase en ${Math.max(0, Math.floor(remainingInPhaseSeconds))} s y prepara la transicion.`
      : remainingInPhaseSeconds <= 15
        ? "Queda poco de fase: prioriza precision sobre velocidad."
        : "Mantiene el patron actual para sostener la extraccion.";

  const methodTip = key.includes("espresso")
    ? "En espresso, corta al rubio claro para evitar amargor final."
    : key.includes("italiana")
      ? "En italiana, retira al primer burbujeo fuerte para no quemar."
      : key.includes("prensa")
        ? "En prensa, rompe costra suave y decanta al terminar."
        : key.includes("aeropress")
          ? "En aeropress, presion constante sin empujar de golpe."
        : "Cuida temperatura y distribucion para una taza limpia.";

  const timeProfile = getBrewTimeProfile(method);
  const timeConfigTip =
    key.includes("espresso") && typeof brewTimeSeconds === "number"
      ? brewTimeSeconds < timeProfile.defaultSeconds * 0.85
        ? "Tiempo corto para espresso: potencia acidez."
        : brewTimeSeconds > timeProfile.defaultSeconds * 1.15
          ? "Tiempo largo para espresso: aumenta cuerpo y riesgo de amargor."
          : "Tiempo de espresso en ventana recomendada."
      : "";

  const volumeTip = waterMl > profile.defaultWaterMl * 1.25
    ? "Receta larga: vigila no diluir en exceso."
    : waterMl < profile.defaultWaterMl * 0.75
      ? "Receta corta: evita sobre-extraer por exceso de contacto."
      : "Volumen dentro de rango recomendado para este metodo.";

  return `${phaseTip} ${extractionTip} ${timeTip} ${timeConfigTip} ${methodTip} ${volumeTip}`.replace(/\s+/g, " ").trim();
}

function scaleTimelineToTarget(phases: BrewPhaseInfo[], targetTotalSeconds?: number): BrewPhaseInfo[] {
  if (!phases.length || !targetTotalSeconds || targetTotalSeconds <= 0) return phases;
  const baseTotal = phases.reduce((sum, phase) => sum + phase.durationSeconds, 0) || 1;
  if (baseTotal === targetTotalSeconds) return phases;
  const scaled = phases.map((phase) => Math.max(1, Math.floor((phase.durationSeconds / baseTotal) * targetTotalSeconds)));
  const diff = targetTotalSeconds - scaled.reduce((sum, seconds) => sum + seconds, 0);
  if (diff !== 0) scaled[scaled.length - 1] = Math.max(1, scaled[scaled.length - 1] + diff);
  return phases.map((phase, index) => ({ ...phase, durationSeconds: scaled[index] }));
}

export function formatClock(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const mins = Math.floor(safe / 60)
    .toString()
    .padStart(2, "0");
  const secs = (safe % 60).toString().padStart(2, "0");
  return `${mins}:${secs}`;
}

export function getBrewDialRecommendation(taste: string): string {
  const key = normalizeLookupText(taste);
  if (key === "amargo") return "Reduce extracción: muele un punto más grueso o baja ligeramente el tiempo.";
  if (key === "acido") return "Aumenta extracción: muele más fino o sube temperatura.";
  if (key === "equilibrado") return "Excelente balance. Guarda este perfil como referencia.";
  if (key === "salado") return "Mejora distribución: revisa homogeneidad y nivelación del lecho.";
  if (key === "acuoso") return "Aumenta dosis o reduce ratio para ganar cuerpo.";
  if (key === "aspero") return "Astringencia: evita remover en exceso y asegura un buen lavado de filtro.";
  if (key === "dulce") return "Excelente extracción de azúcares naturales. Mantén esta configuración.";
  return "";
}




