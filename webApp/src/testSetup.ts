import "@testing-library/jest-dom/vitest";

// Asegurar localStorage en jsdom para que getItem/setItem existan (evita "getItem is not a function" en CI)
const storage: Record<string, string> = {};
if (typeof globalThis.localStorage === "undefined" || typeof globalThis.localStorage.getItem !== "function") {
  Object.defineProperty(globalThis, "localStorage", {
    value: {
      getItem: (key: string) => storage[key] ?? null,
      setItem: (key: string, value: string) => { storage[key] = value; },
      removeItem: (key: string) => { delete storage[key]; },
      clear: () => { for (const k of Object.keys(storage)) delete storage[k]; },
      key: (i: number) => Object.keys(storage)[i] ?? null,
      length: 0
    },
    writable: true
  });
}
