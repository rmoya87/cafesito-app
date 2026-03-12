/**
 * Opciones compartidas entre "Añadir café" (Mi diario) y Elaboración: tipo de café y tamaños.
 */

export const COFFEE_TIPO_OPTIONS: Array<{ label: string; drawable: string }> = [
  { label: "Espresso", drawable: "espresso.png" },
  { label: "Americano", drawable: "americano.png" },
  { label: "Capuchino", drawable: "capuchino.png" },
  { label: "Latte", drawable: "latte.png" },
  { label: "Macchiato", drawable: "macchiato.png" },
  { label: "Moca", drawable: "moca.png" },
  { label: "Vienés", drawable: "vienes.png" },
  { label: "Irlandés", drawable: "irlandes.png" },
  { label: "Frappuccino", drawable: "frappuccino.png" },
  { label: "Caramelo macchiato", drawable: "caramel_macchiato.png" },
  { label: "Corretto", drawable: "corretto.png" },
  { label: "Freddo", drawable: "freddo.png" },
  { label: "Latte macchiato", drawable: "latte_macchiato.png" },
  { label: "Leche con chocolate", drawable: "leche_con_chocolate.png" },
  { label: "Marroquí", drawable: "marroqui.png" },
  { label: "Romano", drawable: "romano.png" },
  { label: "Descafeinado", drawable: "descafeinado.png" }
];

export const COFFEE_SIZE_OPTIONS: Array<{ label: string; rangeLabel: string; ml: number; drawable: string }> = [
  { label: "Espresso", rangeLabel: "25–30 ml", ml: 30, drawable: "taza_espresso.png" },
  { label: "Pequeño", rangeLabel: "150–200 ml", ml: 180, drawable: "taza_pequeno.png" },
  { label: "Mediano", rangeLabel: "250–300 ml", ml: 275, drawable: "taza_mediano.png" },
  { label: "Grande", rangeLabel: "350–400 ml", ml: 375, drawable: "taza_grande.png" },
  { label: "Tazón XL", rangeLabel: "450–500 ml", ml: 475, drawable: "taza_xl.png" }
];
