import type { BrewStep } from "../types";
import { normalizeLookupText } from "./text";

export type BrewPhaseInfo = {
  label: string;
  instruction: string;
  durationSeconds: number;
};

export function getBrewStepTitle(step: BrewStep): string {
  if (step === "method") return "ELABORACION";
  if (step === "coffee") return "ELIGE CAFE";
  if (step === "config") return "CONFIGURA";
  if (step === "brewing") return "PROCESO EN CURSO";
  return "RESULTADO";
}

export function getBrewTimelineForMethod(method: string, waterMl: number): BrewPhaseInfo[] {
  const key = normalizeLookupText(method);
  if (key.includes("espresso")) {
    return [
      {
        label: "Extracción",
        instruction: "Aplica presión constante. Vigila el flujo: debe ser como un hilo de miel. Busca obtener unos 36-40g de líquido final.",
        durationSeconds: 25
      }
    ];
  }
  if (key.includes("prensa")) {
    return [
      {
        label: "Inmersión",
        instruction: "Vierte todo el agua caliente uniformemente sobre el café. Coloca la tapa sin presionar para mantener el calor.",
        durationSeconds: 240
      }
    ];
  }
  if (key.includes("aeropress")) {
    return [
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
    ];
  }
  if (key.includes("italiana")) {
    const boilTime = Math.round(120 + waterMl * 0.2);
    return [
      {
        label: "Calentamiento",
        instruction: "Mantén el fuego medio-bajo. El agua en la base empezará a crear presión para subir por la chimenea.",
        durationSeconds: boilTime
      },
      {
        label: "Extracción",
        instruction: "Cuando el café empiece a salir, baja el fuego o retíralo. Escucha el burbujeo suave y detente antes del chorro final.",
        durationSeconds: 40
      }
    ];
  }
  if (key.includes("turco")) {
    return [
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
    ];
  }
  if (key.includes("sifon")) {
    return [
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
    ];
  }

  const totalPourTime = Math.max(90, Math.min(300, Math.round(120 + (waterMl - 250) * 0.18)));
  const bloomMl = Math.floor(waterMl / 10);
  const pourMl = waterMl - bloomMl;
  return [
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
