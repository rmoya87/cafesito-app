import { useEffect, useMemo, useState } from "react";
import { supabase } from "./supabase";

type Coffee = { id: string; nombre: string; origen: string };

type ViewMode = "mobile" | "desktop";

export function App() {
  const [mode, setMode] = useState<ViewMode>(window.innerWidth < 900 ? "mobile" : "desktop");
  const [coffees, setCoffees] = useState<Coffee[]>([]);
  const [status, setStatus] = useState("Cargando cafés...");

  useEffect(() => {
    const onResize = () => setMode(window.innerWidth < 900 ? "mobile" : "desktop");
    const onShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        document.getElementById("quick-search")?.focus();
      }
    };
    window.addEventListener("resize", onResize);
    window.addEventListener("keydown", onShortcut);
    return () => {
      window.removeEventListener("resize", onResize);
      window.removeEventListener("keydown", onShortcut);
    };
  }, []);

  useEffect(() => {
    (async () => {
      const { data, error } = await supabase
        .from("custom_coffees")
        .select("id,nombre,origen")
        .limit(20);
      if (error) {
        setStatus(`Error: ${error.message}`);
        return;
      }
      setCoffees((data as Coffee[]) ?? []);
      setStatus("Listo");
    })();
  }, []);

  const nav = useMemo(
    () => (
      <nav aria-label="Navegación principal" className="nav">
        <button>Inicio</button>
        <button>Cafés</button>
        <button>Diario</button>
        <button>Perfil</button>
      </nav>
    ),
    []
  );

  return (
    <div className={`layout ${mode}`}>
      {mode === "desktop" ? <aside className="sidebar">{nav}</aside> : null}
      <main>
        <header className="topbar">
          <h1>Cafesito Web</h1>
          <input id="quick-search" placeholder="Buscar (Ctrl/Cmd + K)" aria-label="Búsqueda rápida" />
        </header>

        <section aria-live="polite">
          <p>{status}</p>
          <ul>
            {coffees.map((coffee) => (
              <li key={coffee.id}>
                <strong>{coffee.nombre}</strong> · {coffee.origen}
              </li>
            ))}
          </ul>
        </section>
      </main>
      {mode === "mobile" ? <footer className="bottom-tabs">{nav}</footer> : null}
    </div>
  );
}
