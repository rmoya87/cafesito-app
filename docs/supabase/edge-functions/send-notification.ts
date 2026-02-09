// Edge Function: send-notification
// Env vars requeridas:
// - SUPABASE_URL
// - SUPABASE_SERVICE_ROLE_KEY
// - FCM_SERVER_KEY (Legacy) o configura el envío con HTTP v1 si lo prefieres.
//
// Esta función recibe un payload:
// {
//   "record": {
//     "id": 1,
//     "user_id": 123,
//     "type": "FOLLOW",
//     "from_username": "maria",
//     "message": "",
//     "timestamp": 1700000000000,
//     "related_id": "456"
//   }
// }
//
// Luego busca tokens en public.user_fcm_tokens y envía un push a cada token.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.1";

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const fcmServerKey = Deno.env.get("FCM_SERVER_KEY");

if (!supabaseUrl || !supabaseServiceKey || !fcmServerKey) {
  console.error("Missing required environment variables.");
}

const supabase = createClient(supabaseUrl ?? "", supabaseServiceKey ?? "");

Deno.serve(async (req) => {
  try {
    const payload = await req.json();
    const record = payload?.record;

    if (!record?.user_id) {
      return new Response(JSON.stringify({ error: "Missing user_id" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      });
    }

    const { data: tokens, error } = await supabase
      .from("user_fcm_tokens")
      .select("fcm_token")
      .eq("user_id", record.user_id);

    if (error) {
      console.error("Error fetching tokens:", error);
      return new Response(JSON.stringify({ error: "Token lookup failed" }), {
        status: 500,
        headers: { "Content-Type": "application/json" },
      });
    }

    const tokenList = (tokens ?? [])
      .map((row) => row.fcm_token)
      .filter((token) => typeof token === "string" && token.length > 0);

    if (tokenList.length === 0) {
      return new Response(JSON.stringify({ ok: true, sent: 0 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    const title =
      record.type === "FOLLOW"
        ? `${record.from_username} te siguió`
        : "Cafesito";
    const body =
      record.type === "MENTION"
        ? `${record.from_username} te mencionó`
        : record.message || "Nueva notificación";

    const fcmPayload = {
      registration_ids: tokenList,
      notification: {
        title,
        body,
      },
      data: {
        type: record.type,
        targetId: record.related_id ?? "",
      },
    };

    const fcmResponse = await fetch("https://fcm.googleapis.com/fcm/send", {
      method: "POST",
      headers: {
        Authorization: `key=${fcmServerKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify(fcmPayload),
    });

    const fcmResult = await fcmResponse.json();

    return new Response(JSON.stringify({ ok: true, result: fcmResult }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  } catch (error) {
    console.error("send-notification error:", error);
    return new Response(JSON.stringify({ error: "Unhandled error" }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }
});
