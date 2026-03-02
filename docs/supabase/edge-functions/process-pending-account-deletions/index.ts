// Edge Function: process-pending-account-deletions
// Ejecuta la función SQL process_pending_account_deletions() para borrar
// cuentas con estado inactive_pending_deletion y scheduled_deletion_at ya pasado.
//
// Uso: programar desde Supabase Dashboard → Cron Jobs (o cron externo) una vez al día.
// Env: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY (ya inyectados por Supabase).

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.1";

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");

Deno.serve(async (req: Request) => {
  if (!supabaseUrl || !supabaseServiceKey) {
    return new Response(
      JSON.stringify({ error: "Missing Supabase env" }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }

  const supabase = createClient(supabaseUrl, supabaseServiceKey);

  try {
    const { data, error } = await supabase.rpc("process_pending_account_deletions");

    if (error) {
      console.error("RPC error:", error);
      return new Response(
        JSON.stringify({ error: error.message }),
        { status: 502, headers: { "Content-Type": "application/json" } }
      );
    }

    const deletedCount = typeof data === "number" ? data : 0;
    return new Response(
      JSON.stringify({ ok: true, deletedCount }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  } catch (e) {
    console.error(e);
    return new Response(
      JSON.stringify({ error: String(e) }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }
});
