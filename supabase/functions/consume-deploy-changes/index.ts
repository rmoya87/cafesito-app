// Edge Function: consume-deploy-changes
// Devuelve si hay cambios pendientes para un recurso y, opcionalmente, los marca como procesados.
//
// Seguridad opcional:
// - Si existe DEPLOY_GATE_TOKEN, se exige cabecera x-deploy-token con ese valor.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.1";

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const deployGateToken = Deno.env.get("DEPLOY_GATE_TOKEN");

type JsonObject = Record<string, unknown>;

function asObject(value: unknown): JsonObject | null {
  if (!value || typeof value !== "object" || Array.isArray(value)) return null;
  return value as JsonObject;
}

function asText(value: unknown): string | null {
  if (value === null || value === undefined) return null;
  const text = String(value).trim();
  return text.length > 0 ? text : null;
}

function pickFirstText(...values: unknown[]): string | null {
  for (const value of values) {
    const text = asText(value);
    if (text) return text;
  }
  return null;
}

function extractCoffeeChange(
  operation: string,
  payloadValue: unknown,
): {
  coffeeId: string | null;
  newNombre: string | null;
  newMarca: string | null;
  newSlug: string | null;
  oldNombre: string | null;
  oldMarca: string | null;
  oldSlug: string | null;
} {
  const payload = asObject(payloadValue) ?? {};
  const nested = asObject(payload.payload) ?? {};
  const currentRecord = asObject(payload.record) ??
    asObject(payload.new) ??
    asObject(payload.row) ??
    asObject(nested.record) ??
    asObject(nested.new) ??
    asObject(nested.row);
  const oldRecord = asObject(payload.old_record) ??
    asObject(payload.old) ??
    asObject(nested.old_record) ??
    asObject(nested.old);

  const activeRecord = operation === "DELETE" ? (oldRecord ?? currentRecord) : (currentRecord ?? oldRecord);
  const fallbackRecord = operation === "DELETE" ? currentRecord : oldRecord;

  const coffeeId = pickFirstText(
    activeRecord?.id,
    activeRecord?.coffee_id,
    fallbackRecord?.id,
    fallbackRecord?.coffee_id,
    payload.id,
    payload.coffee_id,
    nested.id,
    nested.coffee_id,
  );

  const newNombre = pickFirstText(
    currentRecord?.nombre,
    currentRecord?.name,
    payload.nombre,
    nested.nombre,
  );
  const newMarca = pickFirstText(
    currentRecord?.marca,
    currentRecord?.brand,
    payload.marca,
    nested.marca,
  );
  const newSlug = pickFirstText(
    currentRecord?.slug,
    payload.slug,
    nested.slug,
  );

  const oldNombre = pickFirstText(
    oldRecord?.nombre,
    oldRecord?.name,
    payload.old_nombre,
    nested.old_nombre,
  );
  const oldMarca = pickFirstText(
    oldRecord?.marca,
    oldRecord?.brand,
    payload.old_marca,
    nested.old_marca,
  );
  const oldSlug = pickFirstText(
    oldRecord?.slug,
    payload.old_slug,
    nested.old_slug,
  );

  return { coffeeId, newNombre, newMarca, newSlug, oldNombre, oldMarca, oldSlug };
}

Deno.serve(async (req: Request) => {
  if (!supabaseUrl || !supabaseServiceKey) {
    return new Response(
      JSON.stringify({ error: "Missing Supabase env" }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }

  if (deployGateToken) {
    const token = req.headers.get("x-deploy-token");
    if (token != deployGateToken) {
      return new Response(
        JSON.stringify({ error: "Unauthorized" }),
        { status: 401, headers: { "Content-Type": "application/json" } },
      );
    }
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey);

  try {
    const body = await req.json().catch(() => ({} as Record<string, unknown>));
    const resource = String((body?.resource as string | undefined) ?? "coffees");
    const consume = Boolean(body?.consume ?? true);

    const { data, error } = await supabase
      .from("deploy_change_events")
      .select("id, operation, payload")
      .eq("resource", resource)
      .is("processed_at", null)
      .order("id", { ascending: true });

    if (error) {
      return new Response(
        JSON.stringify({ error: error.message }),
        { status: 502, headers: { "Content-Type": "application/json" } },
      );
    }

    const rows = data ?? [];
    const total = rows.length;
    const pending = total > 0;
    const ids = rows.map((r) => r.id as number);

    const counts = rows.reduce(
      (acc, row) => {
        const op = String(row.operation ?? "UNKNOWN").toUpperCase();
        if (op == "INSERT") acc.insert += 1;
        else if (op == "UPDATE") acc.update += 1;
        else if (op == "DELETE") acc.delete += 1;
        else acc.unknown += 1;
        return acc;
      },
      { insert: 0, update: 0, delete: 0, unknown: 0 },
    );

    const changes = rows.map((row) => {
      const operation = String(row.operation ?? "UNKNOWN").toUpperCase();
      const details = extractCoffeeChange(operation, row.payload);
      return {
        id: Number(row.id),
        operation,
        coffee_id: details.coffeeId,
        new_nombre: details.newNombre,
        new_marca: details.newMarca,
        new_slug: details.newSlug,
        old_nombre: details.oldNombre,
        old_marca: details.oldMarca,
        old_slug: details.oldSlug,
      };
    });

    let consumed = false;
    if (consume && ids.length > 0) {
      const { error: updateError } = await supabase
        .from("deploy_change_events")
        .update({ processed_at: new Date().toISOString() })
        .in("id", ids);
      if (updateError) {
        return new Response(
          JSON.stringify({ error: updateError.message }),
          { status: 502, headers: { "Content-Type": "application/json" } },
        );
      }
      consumed = true;
    }

    return new Response(
      JSON.stringify({
        ok: true,
        resource,
        pending,
        total,
        counts,
        changes,
        consumed,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } },
    );
  } catch (e) {
    return new Response(
      JSON.stringify({ error: String(e) }),
      { status: 500, headers: { "Content-Type": "application/json" } },
    );
  }
});
