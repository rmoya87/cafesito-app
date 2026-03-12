import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { VitePWA } from "vite-plugin-pwa";
import mkcert from "vite-plugin-mkcert";
import { fileURLToPath } from "url";
import path from "path";
import fs from "fs";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function loadEnvFromWebApp(mode: string): Record<string, string> {
  const env: Record<string, string> = {};
  const tryPaths = [
    path.join(__dirname, ".env"),
    path.join(process.cwd(), ".env"),
    path.join(process.cwd(), "webApp", ".env")
  ];
  let raw = "";
  for (const envPath of tryPaths) {
    if (fs.existsSync(envPath)) {
      raw = fs.readFileSync(envPath, "utf8");
      break;
    }
  }
  if (raw) {
    raw = raw.replace(/^\uFEFF/, "");
    for (const line of raw.split(/\r?\n/)) {
      const trimmed = line.trim();
      if (!trimmed || trimmed.startsWith("#")) continue;
      const m = trimmed.match(/^([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$/);
      if (m) {
        const key = m[1].trim();
        let val = m[2].trim();
        if ((val.startsWith('"') && val.endsWith('"')) || (val.startsWith("'") && val.endsWith("'")))
          val = val.slice(1, -1).replace(/\\(.)/g, "$1");
        env[key] = val;
      }
    }
  }
  const fromVite = loadEnv(mode, __dirname, "");
  const merged = { ...env, ...fromVite };
  // No usar nunca el placeholder: si la URL es de ejemplo, tratarla como vacía
  if (merged.VITE_SUPABASE_URL && /your_project|YOUR_PROJECT|xxxxx/i.test(merged.VITE_SUPABASE_URL)) {
    merged.VITE_SUPABASE_URL = "";
    merged.VITE_SUPABASE_ANON_KEY = "";
  }
  return merged;
}

const PLACEHOLDER_URL = /your_project|YOUR_PROJECT|xxxxx/i;

export default defineConfig(({ mode }) => {
  const env = loadEnvFromWebApp(mode);
  let supabaseUrl = (env.VITE_SUPABASE_URL ?? "").trim() || undefined;
  let supabaseAnonKey = (env.VITE_SUPABASE_ANON_KEY ?? "").trim() || undefined;
  if (supabaseUrl && PLACEHOLDER_URL.test(supabaseUrl)) {
    supabaseUrl = undefined;
    supabaseAnonKey = undefined;
  }

  if (mode === "development") {
    const ok = Boolean(supabaseUrl && supabaseAnonKey);
    if (!ok && (env.VITE_SUPABASE_URL ?? "").trim()) {
      console.warn("[vite] Supabase: se ignoró un valor con placeholder (your_project/xxxxx). Usa la URL real en webApp/.env y asegúrate de no tener otro .env (p. ej. en la raíz) con el ejemplo.");
    }
    console.log(`[vite] Supabase env: ${ok ? "VITE_SUPABASE_URL y VITE_SUPABASE_ANON_KEY cargadas desde .env" : "FALTAN (revisa webApp/.env)"}`);
  }

  const supabaseConfig = { url: supabaseUrl ?? "", anonKey: supabaseAnonKey ?? "" };

  const injectSupabaseConfig = {
    name: "inject-supabase-config",
    enforce: "pre" as const,
    transformIndexHtml() {
      return [
        {
          tag: "script",
          injectTo: "head-prepend",
          children: `window.__SUPABASE_CONFIG__=${JSON.stringify(supabaseConfig)};`
        }
      ];
    }
  };

  return {
  base: "/",
  envDir: __dirname,
  build: {
    rollupOptions: {
      output: {
        manualChunks: (id) => {
          if (id.includes("node_modules/react/") || id.includes("node_modules/react-dom/")) return "react";
          if (id.includes("node_modules/@supabase/")) return "supabase";
        }
      }
    },
    cssCodeSplit: true,
    sourcemap: false
  },
  define: {
    __SUPABASE_URL__: JSON.stringify(supabaseConfig.url),
    __SUPABASE_ANON_KEY__: JSON.stringify(supabaseConfig.anonKey)
  },
  resolve: {
    extensions: [".tsx", ".ts", ".jsx", ".js", ".json"]
  },
  plugins: [
    injectSupabaseConfig,
    react(),
    mkcert(),
    VitePWA({
      registerType: "autoUpdate",
      injectRegister: "script-defer",
      includeAssets: ["favicon.svg", "logo.png", "splash.png", "splash-1170x2532.png", "splash-750x1294.png"],
      manifest: {
        name: "Cafesito",
        short_name: "Cafesito",
        description: "Cafesito web app instalable",
        theme_color: "#6f4e37",
        background_color: "#6f4e37",
        display: "standalone",
        orientation: "portrait",
        start_url: "/",
        icons: [
          { src: "/logo.png", sizes: "192x192", type: "image/png", purpose: "any" },
          { src: "/logo.png", sizes: "512x512", type: "image/png", purpose: "any maskable" }
        ]
      },
      workbox: {
        // Solo precache JS, CSS y HTML (shell + chunks). No precache todos los PNG/SVG (iconos, drawables)
        // para evitar muchas peticiones en segundo plano al instalar el SW. Esos assets se cachean en
        // runtime al usarse (ver runtimeCaching para same-origin).
        globPatterns: ["**/*.{js,css,html}"],
        runtimeCaching: [
          {
            urlPattern: /^https:\/\/.*\.supabase\.co\/rest\/v1\/.*/i,
            handler: "NetworkFirst",
            options: {
              cacheName: "supabase-rest-cache",
              networkTimeoutSeconds: 4
            }
          },
          {
            urlPattern: /^https:\/\/.*\.supabase\.co\/storage\/.*/i,
            handler: "CacheFirst",
            options: {
              cacheName: "supabase-storage-cache",
              expiration: { maxEntries: 100, maxAgeSeconds: 60 * 60 * 24 * 7 }
            }
          },
          // Iconos y drawables de la app (se cachean al usarse, no en precache)
          {
            urlPattern: /\/android-drawable\/|\/brew-methods\/|\/(logo|splash|favicon)\.(png|svg|ico)(\?|$)/i,
            handler: "CacheFirst",
            options: {
              cacheName: "app-static-assets",
              expiration: { maxEntries: 80, maxAgeSeconds: 60 * 60 * 24 * 30 }
            }
          }
        ]
      }
    })
  ],
  server: {
    https: mode === "development",
    host: "0.0.0.0",
    port: 4173
  },
  configureServer(server) {
    if (mode !== "development") return;
    const serviceRoleKey = (env.SUPABASE_SERVICE_ROLE_KEY ?? "").trim();
    const devLoginHandler: (req: any, res: any, next: () => void) => void = async (req, res, next) => {
      const url = req.url ?? "";
      if (!url.startsWith("/api/dev-login")) {
        next();
        return;
      }
      if (!serviceRoleKey || !supabaseUrl) {
        res.statusCode = 503;
        res.setHeader("Content-Type", "text/plain; charset=utf-8");
        res.end(
          "[Solo local] Añade SUPABASE_SERVICE_ROLE_KEY en webApp/.env (Supabase → Settings → API → service_role). Reinicia npm run dev."
        );
        return;
      }
      try {
        const parsed = new URL(url, "http://localhost");
        const userId = parsed.searchParams.get("userId") ?? "1924119502";
        const host = req.headers.host ?? "localhost:4173";
        const redirectTo = `https://${host}/timeline`;
        const { createClient } = await import("@supabase/supabase-js");
        const admin = createClient(supabaseUrl, serviceRoleKey, {
          auth: { autoRefreshToken: false, persistSession: false }
        });
        const { data: userData } = await admin.auth.admin.getUserById(userId);
        const email = userData?.user?.email;
        if (!email) {
          res.statusCode = 404;
          res.setHeader("Content-Type", "text/plain; charset=utf-8");
          res.end("Usuario no encontrado. Comprueba el userId.");
          return;
        }
        const { data: linkData } = await admin.auth.admin.generateLink({
          type: "magiclink",
          email,
          options: { redirectTo }
        });
        const actionLink = linkData?.properties?.action_link;
        if (!actionLink) {
          res.statusCode = 500;
          res.setHeader("Content-Type", "text/plain; charset=utf-8");
          res.end("No se pudo generar el enlace. Revisa SUPABASE_SERVICE_ROLE_KEY en .env.");
          return;
        }
        res.writeHead(302, { Location: actionLink });
        res.end();
      } catch (err) {
        res.statusCode = 500;
        res.setHeader("Content-Type", "text/plain; charset=utf-8");
        res.end(`Error: ${(err as Error).message}`);
      }
    };
    // Registrar primero para que /api/dev-login no lo atrape el SPA fallback de Vite
    const app = server.middlewares as { stack?: Array<{ route?: string; path?: string; handle?: (req: any, res: any, next: () => void) => void }> };
    if (Array.isArray(app.stack)) {
      app.stack.unshift({ route: "", handle: devLoginHandler });
    } else {
      server.middlewares.use(devLoginHandler);
    }
  }
  };
});
