import { expect, test } from "@playwright/test";

test.describe("login ui", () => {
  test("renderiza video y CTA principal", async ({ page }) => {
    await page.goto("/");
    await expect(page.locator("video.login-background-video")).toBeVisible();
    await expect(page.getByRole("button", { name: /continuar con google|empezar ahora/i })).toBeVisible();
  });

  test("boton de login es accesible con teclado", async ({ page }) => {
    await page.goto("/");
    await page.keyboard.press("Tab");
    await page.keyboard.press("Tab");
    const google = page.getByRole("button", { name: /continuar con google|empezar ahora/i });
    await expect(google).toBeVisible();
    await expect(google).toBeEnabled();
  });
});
