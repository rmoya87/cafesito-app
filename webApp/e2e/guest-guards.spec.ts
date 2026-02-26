import { expect, test } from "@playwright/test";

test.describe("guest guards", () => {
  test("rutas protegidas muestran login gate", async ({ page }) => {
    for (const route of ["/timeline", "/search", "/search/users", "/brewlab", "/diary", "/profile", "/profile/usuario-demo"]) {
      await page.goto(route);
      await expect(page.getByRole("button", { name: /continuar con google|empezar ahora/i })).toBeVisible();
    }
  });

  test("ruta publica de cafe no fuerza login", async ({ page }) => {
    await page.goto("/coffee/slug-inexistente");
    await expect(page.getByText(/café no encontrado|cafe no encontrado/i)).toBeVisible();
    await expect(page.getByRole("button", { name: /continuar con google/i })).toHaveCount(0);
  });

  test("ruta protegida se normaliza a /timeline para invitado", async ({ page }) => {
    await page.goto("/profile/privado");
    await expect(page.getByRole("button", { name: /continuar con google|empezar ahora/i })).toBeVisible();
    await expect(page).toHaveURL(/\/timeline$/);
  });
});
