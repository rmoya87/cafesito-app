// Edge Function: consume-deploy-changes
// Devuelve si hay cambios pendientes para un recurso y, opcionalmente, los marca como procesados.
//
// Seguridad opcional:
// - Si existe DEPLOY_GATE_TOKEN, se exige cabecera x-deploy-token con ese valor.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.1";

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const deployGateToken = Deno.env.get("DEPLOY_GATE_TOKEN");

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
      .select("id, operation")
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
