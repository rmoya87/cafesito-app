import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

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

import { App } from "./App";

describe("App", () => {
  it("renderiza cabecera", async () => {
    render(<App />);
    expect((await screen.findAllByText("CAFESITO")).length).toBeGreaterThan(0);
  });

  it("renderiza navegacion principal", async () => {
    render(<App />);
    expect((await screen.findAllByRole("button", { name: "Inicio" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Explorar" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Elabora" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Diario" })).length).toBeGreaterThan(0);
    expect((await screen.findAllByRole("button", { name: "Perfil" })).length).toBeGreaterThan(0);
  });
});
