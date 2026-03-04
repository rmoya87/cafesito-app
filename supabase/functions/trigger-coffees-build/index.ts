// Edge Function: trigger-coffees-build
// Webhook de cafes: registra eventos (INSERT/UPDATE/DELETE) para consumo nocturno.
// No dispara deploy inmediato; el workflow nocturno consume esta cola.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.1";

const DEFAULT_BRANCH = "Beta";
const supabaseUrl = Deno.env.get("SUPABASE_URL");
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

Deno.serve(async (req: Request) => {
  let branch = Deno.env.get("NIGHTLY_DEPLOY_BRANCH") || DEFAULT_BRANCH;
  let operation = "UNKNOWN";
  let payload: Record<string, unknown> = {};

  try {
    const body = await req.json().catch(() => ({} as Record<string, unknown>));
    payload = body;
    operation = String(
      (body as Record<string, unknown>)?.type ??
      (body as Record<string, unknown>)?.eventType ??
      "UNKNOWN"
    ).toUpperCase();
    if (body?.payload?.branch && typeof body.payload.branch === "string") {
      branch = body.payload.branch;
    }
  } catch {
    // ignore malformed payload
  }

  let queued = false;
  let queueError: string | null = null;
  if (supabaseUrl && supabaseServiceKey) {
    try {
      const supabase = createClient(supabaseUrl, supabaseServiceKey);
      const { error } = await supabase.from("deploy_change_events").insert({
        resource: "coffees",
        operation,
        payload,
      });
      if (error) {
        queueError = error.message;
      } else {
        queued = true;
      }
    } catch (e) {
      queueError = String(e);
    }
  } else {
    queueError = "Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY";
  }

  return new Response(
    JSON.stringify({
      ok: true,
      mode: "scheduled",
      message: "Evento en cola para deploy nocturno.",
      branch,
      resource: "coffees",
      operation,
      queued,
      queueError,
    }),
    { status: 202, headers: { "Content-Type": "application/json" } }
  );
});
