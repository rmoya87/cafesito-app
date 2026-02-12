// Edge Function: sync-openfoodfacts-coffees
// Sincroniza productos de Open Food Facts (café + España) en public.coffees.
//
// Variables requeridas:
// - SUPABASE_URL
// - SUPABASE_SERVICE_ROLE_KEY
//
// Mapeo aplicado:
// - code -> id
// - product_name_es (fallback product_name) -> nombre
// - brands (fallback brand, brands_tags[0]) -> marca
// - categories -> formato (una sola: grano | molido | capsula | soluble)
// - manufacturing_places -> pais_origen
// - image_url -> image_url
// - code -> codigo_barras
// - categories -> especialidad (arábica | mezcla)
// - url -> product_url
//
// Filtros obligatorios en llamada a OFF:
// - categories: cafe
// - countries: espana

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.1";

type OpenFoodProduct = {
  code?: string;
  product_name_es?: string;
  product_name?: string;
  brands?: string;
  brand?: string;
  brands_tags?: string[];
  categories?: string;
  categories_tags?: string[];
  manufacturing_places?: string;
  image_url?: string;
  url?: string;
};

type OpenFoodResponse = {
  count?: number;
  page?: number;
  page_count?: number;
  page_size?: number;
  products?: OpenFoodProduct[];
};

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

const PAGE_SIZE = 100;

function normalize(value: string): string {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

function mapFormato(categories: string | undefined, categoriesTags: string[] | undefined): string {
  const joined = [categories ?? "", (categoriesTags ?? []).join(",")].join(",");
  const text = normalize(joined);

  if (text.includes("grano") || text.includes("beans") || text.includes("en:coffee-beans")) return "grano";
  if (text.includes("molido") || text.includes("ground")) return "molido";
  if (text.includes("capsula") || text.includes("capsulas") || text.includes("capsule") || text.includes("capsules")) return "capsula";
  if (text.includes("soluble") || text.includes("instant")) return "soluble";

  return "";
}

function mapEspecialidad(categories: string | undefined, categoriesTags: string[] | undefined): string {
  const joined = [categories ?? "", (categoriesTags ?? []).join(",")].join(",");
  const text = normalize(joined);

  if (text.includes("arabica")) return "arábica";
  if (text.includes("torrefacto") || text.includes("mezcla")) return "mezcla";

  return "";
}

function mapMarca(product: OpenFoodProduct): string {
  if (product.brands && product.brands.trim().length > 0) return product.brands.trim();
  if (product.brand && product.brand.trim().length > 0) return product.brand.trim();
  if (Array.isArray(product.brands_tags) && product.brands_tags.length > 0) return product.brands_tags[0] ?? "";
  return "";
}

function toCoffeeRow(product: OpenFoodProduct) {
  const code = (product.code ?? "").trim();
  if (!code) return null;

  return {
    id: code,
    // Campos mapeados solicitados
    nombre: (product.product_name_es ?? product.product_name ?? "").trim(),
    marca: mapMarca(product),
    formato: mapFormato(product.categories, product.categories_tags),
    pais_origen: (product.manufacturing_places ?? "").trim() || null,
    image_url: (product.image_url ?? "").trim(),
    codigo_barras: code,
    especialidad: mapEspecialidad(product.categories, product.categories_tags),
    product_url: (product.url ?? "").trim(),

    // Campos con valores por defecto para evitar NOT NULL en esquemas estrictos
    variedad_tipo: null,
    descripcion: "",
    fuente_puntuacion: null,
    puntuacion_oficial: null,
    notas_cata: "",
    cafeina: "",
    tueste: "",
    proceso: "",
    ratio_recomendado: null,
    molienda_recomendada: "",
    aroma: 0,
    sabor: 0,
    retrogusto: 0,
    acidez: 0,
    cuerpo: 0,
    uniformidad: 0,
    dulzura: 0,
    puntuacion_total: 0,
    is_custom: false,
    user_id: null,

    // opcional de trazabilidad (solo si existe columna):
    // off_raw_json: product,
  };
}

async function fetchPage(page: number): Promise<OpenFoodResponse> {
  const url = new URL("https://world.openfoodfacts.org/cgi/search.pl");
  url.searchParams.set("search_simple", "1");
  url.searchParams.set("action", "process");
  url.searchParams.set("json", "1");

  // Filtros solicitados (café + España)
  url.searchParams.set("tagtype_0", "categories");
  url.searchParams.set("tag_contains_0", "contains");
  url.searchParams.set("tag_0", "cafe");

  url.searchParams.set("tagtype_1", "countries");
  url.searchParams.set("tag_contains_1", "contains");
  url.searchParams.set("tag_1", "espana");

  url.searchParams.set("page", String(page));
  url.searchParams.set("page_size", String(PAGE_SIZE));
  url.searchParams.set(
    "fields",
    [
      "code",
      "product_name_es",
      "product_name",
      "brands",
      "brands_tags",
      "categories",
      "categories_tags",
      "manufacturing_places",
      "image_url",
      "url",
    ].join(",")
  );

  const response = await fetch(url.toString());
  if (!response.ok) {
    const text = await response.text();
    throw new Error(`OpenFoodFacts request failed (${response.status}): ${text}`);
  }

  return (await response.json()) as OpenFoodResponse;
}

Deno.serve(async (req) => {
  if (!supabaseUrl || !supabaseServiceRoleKey) {
    return new Response(
      JSON.stringify({ ok: false, error: "Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY" }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }

  const supabase = createClient(supabaseUrl, supabaseServiceRoleKey);

  let maxPages: number | null = null;
  try {
    const body = await req.json();
    if (typeof body?.max_pages === "number" && body.max_pages > 0) {
      maxPages = Math.floor(body.max_pages);
    }
  } catch {
    // body opcional
  }

  let page = 1;
  let totalFetched = 0;
  let totalUpserted = 0;

  while (true) {
    const payload = await fetchPage(page);
    const products = payload.products ?? [];

    if (products.length === 0) break;

    const rows = products
      .map((product) => toCoffeeRow(product))
      .filter((row): row is NonNullable<ReturnType<typeof toCoffeeRow>> => row !== null);

    totalFetched += products.length;

    if (rows.length > 0) {
      const { error } = await supabase
        .from("coffees")
        .upsert(rows, { onConflict: "id", ignoreDuplicates: false });

      if (error) {
        return new Response(
          JSON.stringify({
            ok: false,
            error: "Supabase upsert failed",
            details: error,
            page,
          }),
          { status: 500, headers: { "Content-Type": "application/json" } }
        );
      }
      totalUpserted += rows.length;
    }

    page += 1;
    if (maxPages !== null && page > maxPages) {
      break;
    }
  }

  return new Response(
    JSON.stringify({
      ok: true,
      fetched: totalFetched,
      upserted: totalUpserted,
      pages_processed: page - 1,
      max_pages: maxPages,
      filters: {
        categories: "cafe",
        countries: "espana",
      },
    }),
    { status: 200, headers: { "Content-Type": "application/json" } }
  );
});
