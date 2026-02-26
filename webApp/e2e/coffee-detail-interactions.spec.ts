import { expect, test } from "@playwright/test";

test.describe("coffee detail public + guarded interactions", () => {
  test.beforeEach(async ({ page }) => {
    await page.goto("/coffee/slug-inexistente");
  });

  test("acciones del topbar abren popup auth en invitado", async ({ page }) => {
    const actionable = page.locator(".topbar .icon-button, .coffee-top-actions .icon-button").first();
    await actionable.click();
    await expect(page.getByRole("dialog", { name: /acceso/i })).toBeVisible();
    await expect(page.getByRole("button", { name: /continuar con google/i })).toBeVisible();
  });

  test("esc cierra popup de auth", async ({ page }) => {
    await page.locator(".topbar .icon-button, .coffee-top-actions .icon-button").first().click();
    await expect(page.getByRole("dialog", { name: /acceso/i })).toBeVisible();
    await page.keyboard.press("Escape");
    await expect(page.getByRole("dialog", { name: /acceso/i })).toHaveCount(0);
  });
});

