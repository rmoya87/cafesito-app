// Edge Function: trigger-coffees-build
// Invocada por Database Webhook en la tabla public.coffees (Insert, Update, Delete).
// Dispara el workflow de GitHub Actions para regenerar páginas estáticas y desplegar la web.
//
// Secrets en Supabase Edge Function:
// - GITHUB_PAT: token con repo (o workflow)
// - GITHUB_REPO: "owner/repo"

const GITHUB_API = "https://api.github.com";
const EVENT_TYPE = "supabase-coffees-changed";
const DEFAULT_BRANCH = "Beta";

Deno.serve(async (req: Request) => {
  const pat = Deno.env.get("GITHUB_PAT");
  const repo = Deno.env.get("GITHUB_REPO");

  if (!pat || !repo) {
    console.error("Missing GITHUB_PAT or GITHUB_REPO");
    return new Response(
      JSON.stringify({ error: "Server misconfiguration" }),
      { status: 500, headers: { "Content-Type": "application/json" } }
    );
  }

  let branch = DEFAULT_BRANCH;
  try {
    const body = await req.json().catch(() => ({}));
    if (body?.payload?.branch) {
      branch = body.payload.branch;
    }
  } catch {
    // ignore
  }

  const url = `${GITHUB_API}/repos/${repo}/dispatches`;
  const res = await fetch(url, {
    method: "POST",
    headers: {
      Authorization: `token ${pat}`,
      Accept: "application/vnd.github.v3+json",
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      event_type: EVENT_TYPE,
      client_payload: { branch },
    }),
  });

  if (!res.ok) {
    const text = await res.text();
    console.error("GitHub API error:", res.status, text);
    return new Response(
      JSON.stringify({ error: "Failed to trigger workflow", details: text }),
      { status: 502, headers: { "Content-Type": "application/json" } }
    );
  }

  return new Response(
    JSON.stringify({ ok: true, branch }),
    { status: 200, headers: { "Content-Type": "application/json" } }
  );
});
