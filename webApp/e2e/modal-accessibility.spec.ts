import { expect, test } from "@playwright/test";

test("focus queda atrapado dentro del dialog de auth", async ({ page }) => {
  await page.goto("/coffee/slug-inexistente");
  await page.locator(".topbar .icon-button, .coffee-top-actions .icon-button").first().click();
  const dialog = page.getByRole("dialog", { name: /acceso/i });
  await expect(dialog).toBeVisible();

  for (let i = 0; i < 6; i += 1) {
    await page.keyboard.press("Tab");
  }

  const focusedInsideDialog = await page.evaluate(() => {
    const dialogEl = document.querySelector('[role="dialog"][aria-label="Acceso"]');
    return Boolean(dialogEl && dialogEl.contains(document.activeElement));
  });
  expect(focusedInsideDialog).toBeTruthy();
});

