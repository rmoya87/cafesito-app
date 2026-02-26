import { expect, test } from "@playwright/test";

test("login mantiene boton principal accesible por teclado", async ({ page }) => {
  await page.goto("/");
  await page.keyboard.press("Tab");
  await page.keyboard.press("Tab");
  const googleButton = page.getByRole("button", { name: /continuar con google/i });
  await expect(googleButton).toBeVisible();
});

test("popup auth se renderiza con rol dialog", async ({ page }) => {
  await page.goto("/coffee/slug-inexistente");
  await page.locator(".topbar .icon-button, .coffee-top-actions .icon-button").first().click();
  await expect(page.getByRole("dialog", { name: /acceso/i })).toBeVisible();
});
