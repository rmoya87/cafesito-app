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
const HIGH_PRIORITY_TYPES = new Set(["FOLLOW", "MENTION", "COMMENT"]);

Deno.serve(async (req) => {
  try {
    const payload = await req.json();
    const record = payload?.record ?? payload;

    console.log("send-notification invoked", {
      notificationId: record?.id ?? null,
      userId: record?.user_id ?? null,
      type: record?.type ?? null,
    });

    if (!record?.user_id) {
      return new Response(JSON.stringify({ error: "Missing user_id" }), {
        status: 400,
        headers: { "Content-Type": "application/json" },
      });
    }

    if (!supabaseUrl || !supabaseServiceKey || !fcmServerKey) {
      return new Response(
        JSON.stringify({ error: "Server misconfigured: missing secrets" }),
        {
          status: 500,
          headers: { "Content-Type": "application/json" },
        }
      );
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

    const tokenList = Array.from(
      new Set(
        (tokens ?? [])
          .map((row) => row.fcm_token)
          .filter((token) => typeof token === "string" && token.length > 0)
      )
    );

    console.log("send-notification tokens", {
      userId: record.user_id,
      totalRows: tokens?.length ?? 0,
      uniqueTokens: tokenList.length,
    });

    if (tokenList.length === 0) {
      return new Response(JSON.stringify({ ok: true, sent: 0 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }

    const fromUsername = record.from_username ?? "";
    const { data: fromUser } = await supabase
      .from("users_db")
      .select("avatar_url, username")
      .eq("username", fromUsername)
      .maybeSingle();

    const title =
      record.type === "FOLLOW"
        ? `${fromUsername} te siguió`
        : record.type === "MENTION"
        ? fromUsername
        : "Cafesito";
    const body =
      record.type === "MENTION"
        ? "te ha mencionado"
        : record.message || "Nueva notificación";

    const notificationType = String(record.type ?? "").toUpperCase();
    const fcmPriority = HIGH_PRIORITY_TYPES.has(notificationType) ? "high" : "normal";

    const fcmPayload = {
      registration_ids: tokenList,
      priority: fcmPriority,
      notification: {
        title,
        body,
        image: fromUser?.avatar_url ?? undefined,
      },
      data: {
        type: notificationType,
        targetId: record.related_id ?? "",
        post_id: record.related_id ?? "",
        action_label: notificationType === "MENTION" ? "Ver" : "",
        avatar_url: fromUser?.avatar_url ?? "",
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

    const fcmText = await fcmResponse.text();
    let fcmResult: unknown = fcmText;
    try {
      fcmResult = JSON.parse(fcmText);
    } catch {
      // keep raw text
    }

    if (!fcmResponse.ok) {
      console.error("FCM error:", fcmResponse.status, fcmResult);
      return new Response(
        JSON.stringify({ ok: false, status: fcmResponse.status, result: fcmResult }),
        {
          status: 502,
          headers: { "Content-Type": "application/json" },
        }
      );
    }

    console.log("FCM sent", {
      status: fcmResponse.status,
      userId: record.user_id,
      type: notificationType,
      priority: fcmPriority,
      tokenCount: tokenList.length,
    });

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
