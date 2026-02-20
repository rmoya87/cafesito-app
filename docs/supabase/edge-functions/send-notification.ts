// Edge Function: send-notification
// Env vars requeridas:
// - SUPABASE_URL
// - SUPABASE_SERVICE_ROLE_KEY
// - FCM_PROJECT_ID
// - FCM_CLIENT_EMAIL
// - FCM_PRIVATE_KEY (PEM; si tiene \n escapados, se normaliza)
//
// Payload esperado:
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

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.45.1";

const supabaseUrl = Deno.env.get("SUPABASE_URL");
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
const fcmProjectId = Deno.env.get("FCM_PROJECT_ID");
const fcmClientEmail = Deno.env.get("FCM_CLIENT_EMAIL");
const fcmPrivateKey = Deno.env.get("FCM_PRIVATE_KEY")?.replace(/\\n/g, "\n");

if (!supabaseUrl || !supabaseServiceKey) {
  console.error("Missing SUPABASE_URL or SUPABASE_SERVICE_ROLE_KEY.");
}
if (!fcmProjectId || !fcmClientEmail || !fcmPrivateKey) {
  console.error("Missing one or more FCM HTTP v1 credentials.");
}

const supabase = createClient(supabaseUrl ?? "", supabaseServiceKey ?? "");
const HIGH_PRIORITY_TYPES = new Set(["FOLLOW", "MENTION", "COMMENT"]);
const GOOGLE_TOKEN_URI = "https://oauth2.googleapis.com/token";
const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";

let accessTokenCache: { token: string; expiresAtMs: number } | null = null;

const corsHeaders = {
  "Content-Type": "application/json",
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type",
};

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function base64UrlEncode(input: Uint8Array): string {
  let binary = "";
  for (const b of input) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function stripPemMarkers(pem: string): string {
  return pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
}

async function importPrivateKey(privateKeyPem: string): Promise<CryptoKey> {
  const binary = Uint8Array.from(atob(stripPemMarkers(privateKeyPem)), (c) => c.charCodeAt(0));
  return await crypto.subtle.importKey(
    "pkcs8",
    binary.buffer,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );
}

async function createGoogleAccessToken(): Promise<{ accessToken: string; expiresAtMs: number }> {
  if (!fcmClientEmail || !fcmPrivateKey) {
    throw new Error("Missing FCM service-account credentials.");
  }

  const nowSeconds = Math.floor(Date.now() / 1000);
  const jwtHeader = { alg: "RS256", typ: "JWT" };
  const jwtPayload = {
    iss: fcmClientEmail,
    scope: FCM_SCOPE,
    aud: GOOGLE_TOKEN_URI,
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  };

  const encoder = new TextEncoder();
  const headerB64 = base64UrlEncode(encoder.encode(JSON.stringify(jwtHeader)));
  const payloadB64 = base64UrlEncode(encoder.encode(JSON.stringify(jwtPayload)));
  const unsignedJwt = `${headerB64}.${payloadB64}`;

  const privateKey = await importPrivateKey(fcmPrivateKey);
  const signature = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    encoder.encode(unsignedJwt)
  );
  const signedJwt = `${unsignedJwt}.${base64UrlEncode(new Uint8Array(signature))}`;

  const tokenResp = await fetch(GOOGLE_TOKEN_URI, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: signedJwt,
    }),
  });

  const tokenBody = await tokenResp.json();
  if (!tokenResp.ok || !tokenBody?.access_token) {
    throw new Error(`OAuth token request failed: ${JSON.stringify(tokenBody)}`);
  }

  const expiresIn = Number(tokenBody.expires_in ?? 3600);
  return {
    accessToken: String(tokenBody.access_token),
    expiresAtMs: Date.now() + Math.max(0, expiresIn - 60) * 1000,
  };
}

async function getGoogleAccessToken(): Promise<string> {
  if (accessTokenCache && Date.now() < accessTokenCache.expiresAtMs) {
    return accessTokenCache.token;
  }
  const token = await createGoogleAccessToken();
  accessTokenCache = { token: token.accessToken, expiresAtMs: token.expiresAtMs };
  return token.accessToken;
}

function parseRelatedTarget(relatedId: string | null | undefined): {
  related: string;
  postId: string;
  commentId: string;
  targetUserId: string;
} {
  const related = String(relatedId ?? "");
  const parts = related.split(":");
  if (parts.length === 2) {
    const [postId, commentId] = parts;
    return {
      related,
      postId: postId || "",
      commentId: commentId || "",
      targetUserId: "",
    };
  }
  return {
    related,
    postId: "",
    commentId: "",
    targetUserId: /^\d+$/.test(related) ? related : "",
  };
}

async function deleteTokenIfInvalid(token: string): Promise<void> {
  const { error } = await supabase.from("user_fcm_tokens").delete().eq("fcm_token", token);
  if (error) {
    console.warn("Could not delete invalid token", { token: token.slice(0, 12), error });
  }
}

function isInvalidTokenError(responseText: string): boolean {
  const text = responseText.toUpperCase();
  return (
    text.includes("UNREGISTERED") ||
    text.includes("INVALID_ARGUMENT") ||
    text.includes("INVALID REGISTRATION TOKEN") ||
    text.includes("NOT A VALID FCM REGISTRATION TOKEN")
  );
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return jsonResponse({ error: "Method not allowed" }, 405);

  try {
    const payload = await req.json();
    const record = payload?.record ?? payload;

    console.log("send-notification invoked", {
      notificationId: record?.id ?? null,
      userId: record?.user_id ?? null,
      type: record?.type ?? null,
    });

    if (!record?.user_id) return jsonResponse({ error: "Missing user_id" }, 400);
    if (!supabaseUrl || !supabaseServiceKey || !fcmProjectId || !fcmClientEmail || !fcmPrivateKey) {
      return jsonResponse({ error: "Server misconfigured: missing secrets" }, 500);
    }

    const { data: tokens, error } = await supabase
      .from("user_fcm_tokens")
      .select("fcm_token")
      .eq("user_id", record.user_id);

    if (error) {
      console.error("Error fetching tokens:", error);
      return jsonResponse({ error: "Token lookup failed" }, 500);
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
      return jsonResponse({ ok: true, sent: 0, failed: 0, invalidTokensRemoved: 0 }, 200);
    }

    const fromUsername = String(record.from_username ?? "");
    const { data: fromUser } = await supabase
      .from("users_db")
      .select("avatar_url, username")
      .eq("username", fromUsername)
      .maybeSingle();

    const rawType = String(record.type ?? "");
    const notificationType = rawType.toUpperCase();
    const isMention = notificationType === "MENTION";

    const title =
      notificationType === "FOLLOW"
        ? `${fromUsername} te siguio`
        : isMention
        ? fromUsername || "Cafesito"
        : "Cafesito";
    const body = isMention ? "te ha mencionado" : String(record.message || "Nueva notificacion");

    const fcmPriority = HIGH_PRIORITY_TYPES.has(notificationType) ? "HIGH" : "NORMAL";
    const target = parseRelatedTarget(record.related_id);
    const accessToken = await getGoogleAccessToken();

    const sendResults = await Promise.all(
      tokenList.map(async (token) => {
        const messagePayload = {
          message: {
            token,
            notification: {
              title,
              body,
              ...(fromUser?.avatar_url ? { image: fromUser.avatar_url } : {}),
            },
            android: {
              priority: fcmPriority,
              notification: {
                sound: "default",
              },
            },
            data: {
              type: notificationType,
              targetId: target.related,
              related_id: target.related,
              post_id: target.postId,
              comment_id: target.commentId,
              target_user_id: target.targetUserId,
              action_label: isMention ? "Ver" : "",
              avatar_url: String(fromUser?.avatar_url ?? ""),
            },
          },
        };

        const fcmResp = await fetch(
          `https://fcm.googleapis.com/v1/projects/${fcmProjectId}/messages:send`,
          {
            method: "POST",
            headers: {
              Authorization: `Bearer ${accessToken}`,
              "Content-Type": "application/json",
            },
            body: JSON.stringify(messagePayload),
          }
        );

        const text = await fcmResp.text();
        if (!fcmResp.ok && isInvalidTokenError(text)) {
          await deleteTokenIfInvalid(token);
          return { ok: false, tokenInvalid: true, status: fcmResp.status, response: text };
        }
        return { ok: fcmResp.ok, tokenInvalid: false, status: fcmResp.status, response: text };
      })
    );

    const sent = sendResults.filter((r) => r.ok).length;
    const failed = sendResults.length - sent;
    const invalidTokensRemoved = sendResults.filter((r) => r.tokenInvalid).length;

    console.log("FCM send summary", {
      userId: record.user_id,
      type: notificationType,
      sent,
      failed,
      invalidTokensRemoved,
    });

    const sampleError = sendResults.find((r) => !r.ok);
    if (sent === 0) {
      return jsonResponse(
        {
          ok: false,
          sent,
          failed,
          invalidTokensRemoved,
          sampleError: sampleError
            ? { status: sampleError.status, response: sampleError.response }
            : null,
        },
        502
      );
    }

    return jsonResponse(
      {
        ok: true,
        sent,
        failed,
        invalidTokensRemoved,
      },
      200
    );
  } catch (error) {
    console.error("send-notification error:", error);
    return jsonResponse({ error: "Unhandled error" }, 500);
  }
});
