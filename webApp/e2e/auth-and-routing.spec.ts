import { expect, test } from "@playwright/test";

test("muestra login en home para invitado", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("button", { name: /continuar con google/i })).toBeVisible();
});

test("detalle de cafe es accesible por URL publica", async ({ page }) => {
  await page.goto("/coffee/slug-inexistente");
  await expect(page.getByText(/café no encontrado|cafe no encontrado/i)).toBeVisible();
  await expect(page.getByRole("button", { name: /continuar con google/i })).toHaveCount(0);
});

test("acciones protegidas abren popup de auth en detalle", async ({ page }) => {
  await page.goto("/coffee/slug-inexistente");
  const topbarButton = page.locator(".topbar .icon-button, .coffee-top-actions .icon-button").first();
  await topbarButton.click();
  await expect(page.getByRole("button", { name: /continuar con google/i })).toBeVisible();
});
