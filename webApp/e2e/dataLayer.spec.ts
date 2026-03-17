import { expect, test } from "@playwright/test";

/**
 * Comprueba que el dataLayer recibe los eventos esperados (alineado con GTM Web).
 * Requiere VITE_GTM_CONTAINER_ID en el entorno del dev server para que la app haga push.
 *
 * Ejecutar: npm run test:e2e:dataLayer (o, con servidor ya en marcha en otro terminal:
 * npm run dev y luego npx playwright test dataLayer.spec.ts).
 */
test.describe("dataLayer (GTM)", () => {
  /** Devuelve los objetos del dataLayer que tengan la clave event (eventos que envía la app). */
  async function getDataLayerEvents(page: { evaluate: (fn: () => unknown) => Promise<unknown> }) {
    return page.evaluate(() => {
      const dl = (window as unknown as { dataLayer?: unknown[] }).dataLayer;
      if (!Array.isArray(dl)) return [];
      return dl.filter((item): item is Record<string, unknown> => typeof item === "object" && item !== null && "event" in item);
    }) as Promise<Record<string, unknown>[]>;
  }

  test("al cargar la raíz se hace push de page_view con page_path, screen_name y page_location", async ({ page }) => {
    await page.goto("/");
    await page.waitForLoadState("networkidle").catch(() => {});
    await page.waitForTimeout(800);

    const events = await getDataLayerEvents(page);
    const pageViews = events.filter((e) => e.event === "page_view");
    expect(pageViews.length).toBeGreaterThanOrEqual(1);

    const last = pageViews[pageViews.length - 1];
    expect(last).toHaveProperty("page_path");
    expect(last).toHaveProperty("screen_name");
    expect(last).toHaveProperty("page_location");
    expect(typeof last.page_path).toBe("string");
    expect(typeof last.screen_name).toBe("string");
  });

  test("al abrir el modal de login se hace push de modal_open con modal_id", async ({ page }) => {
    await page.goto("/");
    await page.waitForLoadState("domcontentloaded");
    await page.waitForTimeout(400);

    const before = await getDataLayerEvents(page);
    const beforeCount = before.filter((e) => e.event === "modal_open").length;

    await page.getByRole("button", { name: /continuar con google|empezar ahora/i }).click();
    await page.waitForTimeout(500);

    const after = await getDataLayerEvents(page);
    const modalOpens = after.filter((e) => e.event === "modal_open");
    expect(modalOpens.length).toBeGreaterThan(beforeCount);

    const loginOpen = modalOpens.find((e) => e.modal_id === "login_sheet");
    expect(loginOpen).toBeDefined();
    expect(loginOpen?.modal_id).toBe("login_sheet");
  });

  test("al cerrar el modal de login se hace push de modal_close con modal_id", async ({ page }) => {
    await page.goto("/");
    await page.waitForLoadState("domcontentloaded");
    await page.waitForTimeout(300);
    await page.getByRole("button", { name: /continuar con google|empezar ahora/i }).click();
    await page.waitForTimeout(300);
    await expect(page.getByRole("dialog").first()).toBeVisible();

    const before = await getDataLayerEvents(page);
    const closeCountBefore = before.filter((e) => e.event === "modal_close" && e.modal_id === "login_sheet").length;

    await page.keyboard.press("Escape");
    await page.waitForTimeout(400);

    const after = await getDataLayerEvents(page);
    const closeEvents = after.filter((e) => e.event === "modal_close" && e.modal_id === "login_sheet");
    expect(closeEvents.length).toBeGreaterThan(closeCountBefore);
  });
});
