import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  timeout: 30_000,
  fullyParallel: true,
  retries: 0,
  use: {
    baseURL: "https://127.0.0.1:4173",
    trace: "on-first-retry",
    ignoreHTTPSErrors: true
  },
  webServer: {
    command: "npm run dev -- --host 127.0.0.1 --port 4173",
    url: "https://127.0.0.1:4173",
    reuseExistingServer: true,
    timeout: 180_000,
    env: {
      ...process.env,
      VITE_SUPABASE_URL: process.env.VITE_SUPABASE_URL ?? "https://127.0.0.1",
      VITE_SUPABASE_ANON_KEY:
        process.env.VITE_SUPABASE_ANON_KEY ??
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxvY2FsIiwicm9sZSI6ImFub24iLCJpYXQiOjE2MDAwMDAwMDB9.signature",
      VITE_GTM_CONTAINER_ID: process.env.VITE_GTM_CONTAINER_ID ?? "GTM-WLXN93VK"
    }
  },
  projects: [
    {
      name: "chromium",
      use: { ...devices["Desktop Chrome"] }
    },
    {
      name: "mobile-chrome",
      use: { ...devices["Pixel 7"] }
    }
  ]
});
