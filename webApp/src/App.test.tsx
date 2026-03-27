import { fireEvent, render, screen } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const createQueryBuilder = () => {
  const builder: any = {
    select: () => builder,
    order: () => builder,
    eq: () => builder,
    limit: () => builder,
    delete: () => builder,
    insert: () => builder,
    single: async () => ({ data: { id: 1, post_id: "p1", user_id: 1, text: "ok", timestamp: Date.now() }, error: null }),
    then: (resolve: (value: { data: []; error: null }) => void) => resolve({ data: [], error: null })
  };
  return builder;
};

vi.mock("./supabase", () => ({
  supabaseConfigError: null,
  getSupabaseClient: () => ({
    auth: {
      getSession: async () => ({ data: { session: { user: { email: "test@example.com" } } }, error: null }),
      onAuthStateChange: () => ({ data: { subscription: { unsubscribe: () => {} } } }),
      signInWithOAuth: async () => ({ error: null }),
      signOut: async () => ({ error: null })
    },
    from: () => createQueryBuilder()
  })
}));

import { I18nProvider } from "./i18n";
import { App } from "./App";

function renderApp() {
  return render(
    <I18nProvider>
      <App />
    </I18nProvider>
  );
}

describe("App", () => {
  beforeEach(() => {
    // Locale fija en tests: en CI suele resolverse a inglés y cambian aria-label de la navegación.
    try {
      window.localStorage.setItem("cafesito.locale", "es");
      window.localStorage.setItem("cafesito.locale.user_selected", "1");
      window.localStorage.removeItem("cafesito.locale.value");
    } catch {
      /* ignore */
    }
  });
  afterEach(() => {
    document.body.innerHTML = "";
  });
  it("renderiza cabecera", async () => {
    const { unmount } = renderApp();
    expect((await screen.findAllByText("CAFESITO")).length).toBeGreaterThan(0);
    unmount();
  });

  it("renderiza navegacion principal", async () => {
    const { unmount } = renderApp();
    expect((await screen.findAllByRole("button", { name: "Inicio" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Explorar" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Elabora" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Diario" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Perfil" })).length).toBeGreaterThan(0);
    unmount();
  });

  it("muestra vista de diario al navegar a Diario (humo)", async () => {
    const { unmount } = renderApp();
    const diarioButtons = await screen.findAllByRole("button", { name: "Diario" });
    expect(diarioButtons.length).toBeGreaterThan(0);
    fireEvent.click(diarioButtons[0]);
    await expect(screen.findByText(/mi diario|sin café o agua registrada/i)).resolves.toBeInTheDocument();
    unmount();
  });

  it("muestra vista de explorar al navegar a Explorar (humo)", async () => {
    const { unmount } = renderApp();
    const explorarButtons = await screen.findAllByRole("button", { name: "Explorar" });
    expect(explorarButtons.length).toBeGreaterThan(0);
    fireEvent.click(explorarButtons[0]);
    await expect(screen.findByRole("textbox", { name: "Busqueda" })).resolves.toBeInTheDocument();
    unmount();
  });
});
