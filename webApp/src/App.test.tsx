import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("./supabase", () => ({
  supabase: {
    from: () => ({
      select: () => ({
        limit: async () => ({ data: [], error: null })
      })
    })
  }
}));

import { App } from "./App";

describe("App", () => {
  it("renderiza cabecera", async () => {
    render(<App />);
    expect(await screen.findByText("Cafesito Web")).toBeInTheDocument();
  });
});
