import os

# Content from Step Id 797 (1-800)
# (I'll truncate it here but I'll paste the real thing in the actual tool call)
# Wait, I cannot paste 800 lines easily. I'll use PowerShell to read the previous conversation if I had access, but I don't.
# I have to use the content I received in previous turns.

# Actually, I'll just write the whole thing.
# Since I'm an AI, I have the previous context.

# Restored content of DiaryView.tsx
restored_content = """import { type PointerEvent as ReactPointerEvent, useEffect, useMemo, useRef, useState } from "react";
import { MentionText } from "../../ui/MentionText";
import { normalizeLookupText } from "../../core/text";
import { UiIcon, type IconName } from "../../ui/iconography";
import { Button, Input, SheetCard, SheetHandle, SheetOverlay, TabButton, Tabs } from "../../ui/components";
import type { CoffeeRow, DiaryEntryRow, PantryItemRow } from "../../types";

export function DiaryView({
  mode,
  tab,
  setTab,
  period,
  entries,
  coffeeCatalog,
  pantryRows,
  onDeleteEntry,
  onEditEntry,
  onUpdatePantryStock,
  onRemovePantryItem,
  onOpenCoffee,
  onOpenQuickActions
}: {
  mode: "mobile" | "desktop";
  tab: "actividad" | "despensa";
  setTab: (value: "actividad" | "despensa") => void;
  period: "hoy" | "7d" | "30d";
  entries: DiaryEntryRow[];
  coffeeCatalog: CoffeeRow[];
  pantryRows: Array<{ item: PantryItemRow; coffee?: CoffeeRow }>;
  onDeleteEntry: (entryId: number) => Promise<void>;
  onEditEntry: (entryId: number, amountMl: number, caffeineMg: number, preparationType: string) => Promise<void>;
  onUpdatePantryStock: (coffeeId: string, totalGrams: number, gramsRemaining: number) => Promise<void>;
  onRemovePantryItem: (coffeeId: string) => Promise<void>;
  onOpenCoffee: (coffeeId: string) => void;
  onOpenQuickActions: () => void;
}) {
  const [deletingEntryId, setDeletingEntryId] = useState<number | null>(null);
  const [editEntryId, setEditEntryId] = useState<number | null>(null);
  const [editAmountMl, setEditAmountMl] = useState("");
  const [editCaffeineMg, setEditCaffeineMg] = useState("");
  const [editPreparationType, setEditPreparationType] = useState("");
  const [savingEditEntry, setSavingEditEntry] = useState(false);
  const [pantryOptionsCoffeeId, setPantryOptionsCoffeeId] = useState<string | null>(null);
  const [pantryDeleteConfirmCoffeeId, setPantryDeleteConfirmCoffeeId] = useState<string | null>(null);
  const [stockEditCoffeeId, setStockEditCoffeeId] = useState<string | null>(null);
  const [stockEditTotal, setStockEditTotal] = useState("");
  const [stockEditRemaining, setStockEditRemaining] = useState("");
  const [savingStock, setSavingStock] = useState(false);
  const [removingStock, setRemovingStock] = useState(false);
  const [showCaffeineInfo, setShowCaffeineInfo] = useState(false);
  const editPrepScrollRef = useRef<HTMLDivElement | null>(null);
  const editSizeScrollRef = useRef<HTMLDivElement | null>(null);
  const editScrollPointerIdRef = useRef<number | null>(null);
  const editScrollStartXRef = useRef(0);
  const editScrollStartLeftRef = useRef(0);
  const editScrollTargetRef = useRef<HTMLDivElement | null>(null);
  const editScrollActiveRef = useRef(false);
  const editScrollRafRef = useRef<number | null>(null);
  const editScrollPendingLeftRef = useRef(0);
  const [editChipsDragging, setEditChipsDragging] = useState(false);
  const visibleEntries = useMemo(() => {
    const now = new Date();
    const start = new Date(now);
    if (period === "hoy") {
      start.setHours(0, 0, 0, 0);
    } else if (period === "7d") {
      const day = start.getDay(); // Sunday=0
      const diffToMonday = day === 0 ? 6 : day - 1;
      start.setDate(start.getDate() - diffToMonday);
      start.setHours(0, 0, 0, 0);
    } else {
      start.setDate(1);
      start.setHours(0, 0, 0, 0);
    }
    const startMs = start.getTime();
    return entries
      .filter((entry) => Number(entry.timestamp) >= startMs)
      .sort((a, b) => Number(b.timestamp) - Number(a.timestamp));
  }, [entries, period]);

  const analytics = useMemo(() => {
    const caffeine = visibleEntries.reduce((acc, entry) => acc + Math.max(0, entry.caffeine_mg || 0), 0);
    const hydrationMl = visibleEntries
      .filter((entry) => (entry.type || "").toUpperCase() === "WATER")
      .reduce((acc, entry) => acc + Math.max(0, entry.amount_ml || 0), 0);
    const coffeeCups = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() !== "WATER").length;
    const waterEntries = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() === "WATER").length;
    const avgCaffeine = coffeeCups > 0 ? Math.round(caffeine / coffeeCups) : 0;
    return { caffeine, hydrationMl, coffeeCups, waterEntries, avgCaffeine };
  }, [visibleEntries]);
  const hydrationTargetMl = period === "hoy" ? 2000 : period === "7d" ? 14000 : 60000;
  const caffeineTargetMg = period === "hoy" ? 160 : period === "7d" ? 1120 : 4800;
  const hydrationProgressPct = Math.max(0, Math.min(100, Math.round((analytics.hydrationMl / hydrationTargetMl) * 100)));
  const caffeineTrendPct = Math.round(((analytics.caffeine - caffeineTargetMg) / Math.max(1, caffeineTargetMg)) * 100);
  const hydrationTrendPct = Math.round(((analytics.hydrationMl - hydrationTargetMl) / Math.max(1, hydrationTargetMl)) * 100);
  const chartData = useMemo(() => {
    if (period === "hoy") {
      const byHour = new Map<number, { caffeine: number; water: number }>();
      visibleEntries.forEach((entry) => {
        const hour = new Date(entry.timestamp).getHours();
        const prev = byHour.get(hour) ?? { caffeine: 0, water: 0 };
        if ((entry.type || "").toUpperCase() === "WATER") {
          prev.water += Math.max(0, entry.amount_ml || 0);
        } else {
          prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
        }
        byHour.set(hour, prev);
      });
      return Array.from({ length: 24 }).map((_, idx) => {
        const values = byHour.get(idx) ?? { caffeine: 0, water: 0 };
        return { label: `${String(idx).padStart(2, "0")}`, caffeine: values.caffeine, water: values.water };
      });
    }

    if (period === "7d") {
      const dayLabels = ["L", "M", "X", "J", "V", "S", "D"];
      const byDay = new Map<number, { caffeine: number; water: number }>();
      visibleEntries.forEach((entry) => {
        const jsDay = new Date(entry.timestamp).getDay(); // Sun=0..Sat=6
        const mondayStartIndex = jsDay === 0 ? 6 : jsDay - 1;
        const prev = byDay.get(mondayStartIndex) ?? { caffeine: 0, water: 0 };
        if ((entry.type || "").toUpperCase() === "WATER") {
          prev.water += Math.max(0, entry.amount_ml || 0);
        } else {
          prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
        }
        byDay.set(mondayStartIndex, prev);
      });
      return dayLabels.map((label, idx) => {
        const values = byDay.get(idx) ?? { caffeine: 0, water: 0 };
        return { label, caffeine: values.caffeine, water: values.water };
      });
    }

    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const byDay = new Map<number, { caffeine: number; water: number }>();
    visibleEntries.forEach((entry) => {
      const d = new Date(entry.timestamp);
      const day = d.getDate();
      const prev = byDay.get(day) ?? { caffeine: 0, water: 0 };
      if ((entry.type || "").toUpperCase() === "WATER") {
        prev.water += Math.max(0, entry.amount_ml || 0);
      } else {
        prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
      }
      byDay.set(day, prev);
    });
    return Array.from({ length: daysInMonth }).map((_, idx) => {
      const day = idx + 1;
      const values = byDay.get(day) ?? { caffeine: 0, water: 0 };
      return { label: String(day), caffeine: values.caffeine, water: values.water };
    });
  }, [period, visibleEntries]);
  const chartScrollRef = useRef<HTMLDivElement | null>(null);
  const chartPointerIdRef = useRef<number | null>(null);
  const chartDragStartXRef = useRef(0);
  const chartDragStartScrollRef = useRef(0);
  const [chartDragging, setChartDragging] = useState(false);

  useEffect(() => {
    const node = chartScrollRef.current;
    if (!node) return;
    node.scrollLeft = 0;
  }, [period, chartData.length]);
  const sortedPantryRows = useMemo(
    () => [...pantryRows].sort((a, b) => Number(b.item.last_updated || 0) - Number(a.item.last_updated || 0)),
    [pantryRows]
  );
  const entryImageByCoffeeId = useMemo(() => {
    const map = new Map<string, string>();
    coffeeCatalog.forEach((coffee) => {
      const id = String(coffee.id || "");
      const image = String(coffee.image_url || "");
      if (id && image && !map.has(id)) map.set(id, image);
    });
    sortedPantryRows.forEach((row) => {
      const id = String(row.item.coffee_id || "");
      const image = row.coffee?.image_url || "";
      if (id && image && !map.has(id)) map.set(id, image);
    });
    return map;
  }, [coffeeCatalog, sortedPantryRows]);
  const entryBrandByCoffeeId = useMemo(() => {
    const map = new Map<string, string>();
    coffeeCatalog.forEach((coffee) => {
      const id = String(coffee.id || "");
      const brand = String((coffee.marca || "").trim());
      if (id && brand && !map.has(id)) map.set(id, brand);
    });
    sortedPantryRows.forEach((row) => {
      const id = String(row.item.coffee_id || "");
      const brand = (row.coffee?.marca || "").trim();
      if (id && brand && !map.has(id)) map.set(id, brand);
    });
    return map;
  }, [coffeeCatalog, sortedPantryRows]);
  const diaryRows = useMemo(() => {
    const rows: Array<{ type: "header"; key: string; label: string } | { type: "entry"; key: string; entry: DiaryEntryRow }> = [];
    let lastDayKey = "";
    visibleEntries.forEach((entry) => {
      const date = new Date(entry.timestamp);
      const dayKey = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
      if (period !== "hoy" && dayKey !== lastDayKey) {
        rows.push({
          type: "header",
          key: `header-${dayKey}`,
          label: date.toLocaleDateString("es-ES", { day: "numeric", month: "long" })
        });
        lastDayKey = dayKey;
      }
      rows.push({ type: "entry", key: `entry-${entry.id}`, entry });
    });
    return rows;
  }, [period, visibleEntries]);
  const stockEditTarget = useMemo(
    () => sortedPantryRows.find((row) => row.item.coffee_id === stockEditCoffeeId) ?? null,
    [sortedPantryRows, stockEditCoffeeId]
  );
  const editEntryTarget = useMemo(
    () => visibleEntries.find((entry) => entry.id === editEntryId) ?? null,
    [editEntryId, visibleEntries]
  );
  const editEntryIsWater = (editEntryTarget?.type || "").toUpperCase() === "WATER";
  const editEntryTime = editEntryTarget
    ? new Date(editEntryTarget.timestamp).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" })
    : "";
  const editMethodOptions = [
    { label: "Espresso", drawable: "maq_espresso.png" },
    { label: "Americano", drawable: "maq_manual.png" },
    { label: "Cappuccino", drawable: "maq_espresso.png" },
    { label: "V60", drawable: "maq_hario_v60.png" },
    { label: "Aeropress", drawable: "maq_aeropress.png" },
    { label: "Chemex", drawable: "maq_chemex.png" },
    { label: "Prensa francesa", drawable: "maq_prensa_francesa.png" },
    { label: "Moka", drawable: "maq_italiana.png" },
    { label: "Goteo", drawable: "maq_goteo.png" },
    { label: "Sifón", drawable: "maq_sifon.png" },
    { label: "Turco", drawable: "maq_turco.png" },
    { label: "Manual", drawable: "maq_manual.png" }
  ];
  const editSizeOptions = [
    { label: "Espresso", range: "25-30 ml", ml: 30, drawable: "taza_espresso.png" },
    { label: "Pequeño", range: "150-200 ml", ml: 175, drawable: "taza_pequeno.png" },
    { label: "Mediano", range: "250-300 ml", ml: 275, drawable: "taza_mediano.png" },
    { label: "Grande", range: "320-400 ml", ml: 360, drawable: "taza_grande.png" }
  ];
  const parsedStockTotal = Number(stockEditTotal || 0);
  const parsedStockRemaining = Number(stockEditRemaining || 0);
  const canSaveStock =
    Number.isFinite(parsedStockTotal) &&
    Number.isFinite(parsedStockRemaining) &&
    parsedStockTotal >= 1 &&
    parsedStockRemaining >= 0 &&
    parsedStockRemaining <= parsedStockTotal;
  const stockValidationMessage =
    (!Number.isFinite(parsedStockTotal) || !Number.isFinite(parsedStockRemaining))
      ? "Introduce valores válidos."
      : parsedStockTotal < 1
      ? "El total debe ser mayor que 0."
      : parsedStockRemaining < 0
        ? "El restante no puede ser negativo."
        : parsedStockRemaining > parsedStockTotal
          ? "El restante no puede superar el total."
          : "";
  const parsedEditAmount = Number(editAmountMl || 0);
  const parsedEditCaffeine = Number(editCaffeineMg || 0);
  const parsedEditDose = ((editPreparationType || "").match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1] || "15").replace(",", ".");
  const canSaveEditEntry =
    Number.isFinite(parsedEditAmount) &&
    parsedEditAmount > 0 &&
    (editEntryIsWater || (Number.isFinite(parsedEditCaffeine) && parsedEditCaffeine >= 0));
  const editValidationMessage =
    !Number.isFinite(parsedEditAmount)
      ? "Introduce una cantidad válida."
      : parsedEditAmount <= 0
      ? "La cantidad debe ser mayor que 0 ml."
      : (!editEntryIsWater && !Number.isFinite(parsedEditCaffeine))
        ? "Introduce una cafeína válida."
      : (!editEntryIsWater && parsedEditCaffeine < 0)
        ? "La cafeína no puede ser negativa."
        : "";
  const handleEditScrollPointerDown = (event: ReactPointerEvent<HTMLDivElement>, section: "prep" | "size") => {
    const node = section === "prep" ? editPrepScrollRef.current : editSizeScrollRef.current;
    if (!node) return;
    if (event.pointerType !== "mouse") return;
    if (node.scrollWidth <= node.clientWidth) return;
    editScrollPointerIdRef.current = event.pointerId;
    editScrollTargetRef.current = node;
    editScrollStartXRef.current = event.clientX;
    editScrollStartLeftRef.current = node.scrollLeft;
    editScrollActiveRef.current = false;
  };
  const handleEditScrollPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const node = editScrollTargetRef.current;
    if (!node) return;
    if (editScrollPointerIdRef.current !== event.pointerId) return;
    const delta = event.clientX - editScrollStartXRef.current;
    if (!editScrollActiveRef.current) {
      if (Math.abs(delta) < 6) return;
      editScrollActiveRef.current = true;
      setEditChipsDragging(true);
      if (!node.hasPointerCapture(event.pointerId)) {
        node.setPointerCapture(event.pointerId);
      }
    }
    event.preventDefault();
    editScrollPendingLeftRef.current = editScrollStartLeftRef.current - delta;
    if (editScrollRafRef.current != null) return;
    editScrollRafRef.current = window.requestAnimationFrame(() => {
      const target = editScrollTargetRef.current;
      if (target) target.scrollLeft = editScrollPendingLeftRef.current;
      editScrollRafRef.current = null;
    });
  };
  const handleEditScrollPointerEnd = (event: ReactPointerEvent<HTMLDivElement>) => {
    const node = editScrollTargetRef.current;
    if (!node) return;
    if (editScrollPointerIdRef.current !== event.pointerId) return;
    editScrollPointerIdRef.current = null;
    editScrollTargetRef.current = null;
    editScrollActiveRef.current = false;
    setEditChipsDragging(false);
    if (editScrollRafRef.current != null) {
      window.cancelAnimationFrame(editScrollRafRef.current);
      editScrollRafRef.current = null;
    }
    if (node.hasPointerCapture(event.pointerId)) {
      node.releasePointerCapture(event.pointerId);
    }
  };
  useEffect(() => {
    return () => {
      if (editScrollRafRef.current != null) {
        window.cancelAnimationFrame(editScrollRafRef.current);
        editScrollRafRef.current = null;
      }
    };
  }, []);
  const activityList = (
    <ul className="diary-list">
      {diaryRows.length ? diaryRows.map((row) => {
        if (row.type === "header") {
          return <li key={row.key} className="diary-day-header">{row.label}</li>;
        }
        const entry = row.entry;
        return (
          <DiaryActivityRow
            key={row.key}
            entry={entry}
            imageUrl={entry.coffee_id ? entryImageByCoffeeId.get(String(entry.coffee_id)) ?? "" : ""}
            brand={entry.coffee_id ? entryBrandByCoffeeId.get(String(entry.coffee_id)) ?? "" : ""}
            deleting={deletingEntryId === entry.id}
            onOpenEdit={() => {
              const isWater = (entry.type || "").toUpperCase() === "WATER";
              setEditEntryId(entry.id);
              setEditAmountMl(String(Math.max(1, entry.amount_ml || 1)));
              setEditCaffeineMg(String(Math.max(0, entry.caffeine_mg || 0)));
              setEditPreparationType((entry.preparation_type || "").trim() || (isWater ? "Agua" : "Sin método"));
            }}
            onDelete={async () => {
              if (deletingEntryId === entry.id) return;
              setDeletingEntryId(entry.id);
              try {
                await new Promise((resolve) => window.setTimeout(resolve, 170));
                await onDeleteEntry(entry.id);
              } finally {
                setDeletingEntryId(null);
              }
            }}
          />
        );
      }) : <li className="diary-empty-card">Sin café o agua registrada</li>}
    </ul>
  );
  const pantryList = (
    <ul className="diary-pantry-grid">
      {sortedPantryRows.length ? sortedPantryRows.map((row) => (
        <li
          key={`${row.item.user_id}-${row.item.coffee_id}`}
          className="diary-pantry-card"
          onClick={() => {
            if (!row.coffee?.id) return;
            onOpenCoffee(row.coffee.id);
          }}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              if (!row.coffee?.id) return;
              onOpenCoffee(row.coffee.id);
            }
          }}
        >
          <div className="diary-pantry-media">
            {row.coffee?.image_url ? (
              <img src={row.coffee.image_url} alt={row.coffee.nombre} loading="lazy" />
            ) : (
              <span className="diary-pantry-fallback" aria-hidden="true">{(row.coffee?.nombre || "C").slice(0, 1).toUpperCase()}</span>
            )}
            <div className="diary-pantry-overlay" />
            <div className="diary-pantry-copy">
              <small>{(row.coffee?.marca || "").toUpperCase()}</small>
              <strong>{row.coffee?.nombre ?? row.item.coffee_id}</strong>
            </div>
            <Button variant="plain"
              type="button"
              className="diary-pantry-options"
              aria-label="Opciones"
              onClick={(event) => {
                event.stopPropagation();
                setPantryOptionsCoffeeId(row.item.coffee_id);
              }}
            >
              <UiIcon name="more" className="ui-icon" />
            </Button>
          </div>
          <div className="diary-pantry-foot">
            <div className="diary-pantry-values">
              <span>{row.item.grams_remaining}g</span>
              <span>{row.item.total_grams > 0 ? Math.round((row.item.grams_remaining / row.item.total_grams) * 100) : 0}%</span>
            </div>
            <div className="diary-pantry-progress" aria-hidden="true">
              <i style={{ width: `${Math.max(0, Math.min(100, row.item.total_grams > 0 ? (row.item.grams_remaining / row.item.total_grams) * 100 : 0))}%` }} />
            </div>
          </div>
        </li>
      )) : <li className="diary-empty-card">No hay café en tu despensa</li>}
    </ul>
  );

  return (
    <>
      <article className="diary-analytics-card">
        <div className="diary-analytics-head">
          <strong className="diary-analytics-card-title">ESTADO ACTUAL</strong>
          <Button variant="plain" className="diary-analytics-add" onClick={onOpenQuickActions} aria-label="Nuevo registro">
            <UiIcon name="add" className="ui-icon" />
          </Button>
        </div>
        <div className="diary-analytics-top">
          <div className="diary-analytics-head-block">
            <p className="metric-label diary-analytics-label">
              CAFEÍNA ESTIMADA
              <span className="diary-analytics-info-wrap">
                <Button variant="plain"
                  type="button"
                  className="diary-analytics-info"
                  aria-label="Información de cafeína estimada"
                  aria-expanded={showCaffeineInfo}
                  onClick={() => setShowCaffeineInfo((value) => !value)}
                  onMouseEnter={() => setShowCaffeineInfo(true)}
                  onMouseLeave={() => setShowCaffeineInfo(false)}
                  onBlur={() => setShowCaffeineInfo(false)}
                >
                  i
                </Button>
                {showCaffeineInfo ? (
                  <span className="diary-analytics-tooltip" role="tooltip">
                    Estimación basada en tus registros de consumo en el periodo seleccionado.
                  </span>
                ) : null}
              </span>
            </p>
            <p className="analytics-value diary-analytics-main-value">{analytics.caffeine} mg</p>
            <span className={`diary-analytics-trend ${caffeineTrendPct >= 0 ? "is-up" : "is-down"}`.trim()}>
              {caffeineTrendPct >= 0 ? "▲" : "▼"} {Math.abs(caffeineTrendPct)}%
            </span>
          </div>
          <div className="diary-analytics-head-block diary-analytics-hydration">
            <p className="metric-label diary-analytics-label">HIDRATACIÓN</p>
            <p className="analytics-value diary-analytics-main-value">{analytics.hydrationMl} ml</p>
            <span className={`diary-analytics-trend is-water ${hydrationTrendPct >= 0 ? "is-up" : "is-down"}`.trim()}>
              {hydrationTrendPct >= 0 ? "▲" : "▼"} {Math.abs(hydrationTrendPct)}%
            </span>
          </div>
        </div>
        <div
          ref={chartScrollRef}
          className={`diary-chart-scroll ${chartDragging ? "is-dragging" : ""}`.trim()}
          aria-label="Gráfico de consumo"
          onPointerDown={(event) => {
            const node = chartScrollRef.current;
            if (!node) return;
            if (node.scrollWidth <= node.clientWidth) return;
            event.preventDefault();
            chartPointerIdRef.current = event.pointerId;
            chartDragStartXRef.current = event.clientX;
            chartDragStartScrollRef.current = node.scrollLeft;
            setChartDragging(true);
            node.setPointerCapture(event.pointerId);
          }}
          onPointerMove={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            event.preventDefault();
            const delta = event.clientX - chartDragStartXRef.current;
            node.scrollLeft = chartDragStartScrollRef.current - delta;
          }}
          onPointerUp={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            chartPointerIdRef.current = null;
            setChartDragging(false);
            if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
          }}
          onPointerCancel={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            chartPointerIdRef.current = null;
            setChartDragging(false);
            if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
          }}
          onPointerLeave={() => {
            chartPointerIdRef.current = null;
            setChartDragging(false);
          }}
        >
          <div className={`diary-chart ${period === "7d" ? "is-week" : ""}`.trim()}>
            {chartData.map((item) => {
              const caffeineRatio = Math.min(1, item.caffeine / 400);
              const waterRatio = Math.min(1, item.water / 2000);
              const hasCaffeine = item.caffeine > 0;
              const hasWater = item.water > 0;
              const maxBarHeight = 136;
              const minActiveBarHeight = 14;
              const emptyBarHeight = 4;
              const caffeineHeight = hasCaffeine
                ? Math.max(minActiveBarHeight, Math.round(caffeineRatio * maxBarHeight))
                : emptyBarHeight;
              const waterHeight = hasWater
                ? Math.max(minActiveBarHeight, Math.round(waterRatio * maxBarHeight))
                : emptyBarHeight;
              return (
                <div key={item.label} className="diary-chart-col">
                  <div className="diary-chart-bars">
                    <div className="diary-chart-bar-wrap">
                      {hasCaffeine ? <small className="diary-chart-bar-value">{Math.round(item.caffeine)}</small> : null}
                      <span
                        className={`diary-chart-bar caffeine ${hasCaffeine ? "is-active" : ""}`.trim()}
                        style={{ height: `${caffeineHeight}px` }}
                        title={`Cafeína: ${Math.round(item.caffeine)} mg`}
                        aria-label={`Cafeína ${Math.round(item.caffeine)} miligramos`}
                      />
                    </div>
                    <div className="diary-chart-bar-wrap">
                      {hasWater ? <small className="diary-chart-bar-value">{Math.round(item.water)}</small> : null}
                      <span
                        className={`diary-chart-bar water ${hasWater ? "is-active" : ""}`.trim()}
                        style={{ height: `${waterHeight}px` }}
                        title={`Agua: ${Math.round(item.water)} ml`}
                        aria-label={`Agua ${Math.round(item.water)} mililitros`}
                      />
                    </div>
                  </div>
                  <small>{item.label}</small>
                </div>
              );
            })}
          </div>
        </div>
        <div className="analytics-grid">
          <div className="diary-metric-box">
            <UiIcon name="star" className="ui-icon" />
            <p className="analytics-value">{analytics.avgCaffeine} mg</p>
            <p className="metric-label">MEDIA</p>
          </div>
          <div className="diary-metric-box">
            <UiIcon name="coffee" className="ui-icon" />
            <p className="analytics-value">{analytics.coffeeCups}</p>
            <p className="metric-label">TAZAS</p>
          </div>
          <div className="diary-metric-box">
            <UiIcon name="taste-watery" className="ui-icon" />
            <p className="analytics-value">{hydrationProgressPct}%</p>
            <p className="metric-label">PROGRESO</p>
          </div>
        </div>
      </article>

      {mode !== "desktop" ? (
        <>
          <Tabs className="diary-tabs" aria-label="Tabs diario">
            <TabButton active={tab === "actividad"} role="tab" aria-selected={tab === "actividad"} onClick={() => setTab("actividad")}>ACTIVIDAD</TabButton>
            <TabButton active={tab === "despensa"} role="tab" aria-selected={tab === "despensa"} onClick={() => setTab("despensa")}>DESPENSA</TabButton>
          </Tabs>
          {tab === "actividad" ? activityList : null}
          {tab === "despensa" ? pantryList : null}
        </>
      ) : (
        <section className="diary-desktop-columns" aria-label="Actividad y despensa">
          <div className="diary-section-block">
            <h3 className="diary-section-title">Actividad</h3>
            {activityList}
          </div>
          <div className="diary-section-block">
            <h3 className="diary-section-title">Despensa</h3>
            {pantryList}
          </div>
        </section>
      )}

      {pantryOptionsCoffeeId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Opciones despensa" onDismiss={() => setPantryOptionsCoffeeId(null)} onClick={() => setPantryOptionsCoffeeId(null)}>
          <SheetCard className="diary-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">OPCIONES DESPENSA</strong>
            </header>
            <div className="diary-sheet-list">
              <Button variant="plain"
                type="button"
                className="diary-sheet-action"
                onClick={() => {
                  const row = sortedPantryRows.find((item) => item.item.coffee_id === pantryOptionsCoffeeId);
                  if (!row) return;
                  setStockEditCoffeeId(row.item.coffee_id);
                  setStockEditTotal(String(Math.max(1, row.item.total_grams)));
                  setStockEditRemaining(String(Math.max(0, row.item.grams_remaining)));
                  setPantryOptionsCoffeeId(null);
                }}
              >
                <UiIcon name="edit" className="ui-icon" />
                <span>Editar stock</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </Button>
              <Button variant="plain"
                type="button"
                className="diary-sheet-action"
                disabled={removingStock}
                onClick={() => {
                  setPantryDeleteConfirmCoffeeId(pantryOptionsCoffeeId);
                  setPantryOptionsCoffeeId(null);
                }}
              >
                <UiIcon name="trash" className="ui-icon" />
                <span>Eliminar de despensa</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </Button>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {pantryDeleteConfirmCoffeeId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Confirmar eliminación" onDismiss={() => setPantryDeleteConfirmCoffeeId(null)} onClick={() => setPantryDeleteConfirmCoffeeId(null)}>
          <SheetCard className="diary-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">ELIMINAR DE DESPENSA</strong>
            </header>
            <div className="diary-sheet-form">
              <p className="feed-meta">¿Seguro que quieres eliminar este café de tu despensa?</p>
              <div className="diary-sheet-form-actions">
                <Button variant="plain" type="button" className="action-button action-button-ghost" onClick={() => setPantryDeleteConfirmCoffeeId(null)} disabled={removingStock}>
                  Cancelar
                </Button>
                <Button variant="plain"
                  type="button"
                  className="action-button"
                  disabled={removingStock}
                  onClick={async () => {
                    if (removingStock) return;
                    setRemovingStock(true);
                    try {
                      await onRemovePantryItem(pantryDeleteConfirmCoffeeId);
                      setPantryDeleteConfirmCoffeeId(null);
                    } finally {
                      setRemovingStock(false);
                    }
                  }}
                >
                  {removingStock ? "Eliminando..." : "Eliminar"}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {stockEditTarget ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Editar stock" onDismiss={() => setStockEditCoffeeId(null)} onClick={() => setStockEditCoffeeId(null)}>
          <SheetCard className="diary-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">EDITAR STOCK</strong>
            </header>
            <div className="diary-sheet-form">
              <label>
                <span>Total (g)</span>
                <Input
                  className="search-wide"
                  type="number"
                  min={1}
                  step={1}
                  value={stockEditTotal}
                  onChange={(event) => {
                    const nextTotal = event.target.value;
                    setStockEditTotal(nextTotal);
                    const parsedNextTotal = Number(nextTotal || 0);
                    const parsedCurrentRemaining = Number(stockEditRemaining || 0);
                    if (
                      Number.isFinite(parsedNextTotal) &&
                      parsedNextTotal >= 0 &&
                      Number.isFinite(parsedCurrentRemaining) &&
                      parsedCurrentRemaining > parsedNextTotal
                    ) {
                      setStockEditRemaining(String(parsedNextTotal));
                    }
                  }}
                />
              </label>
              <label>
                <span>Restante (g)</span>
                <Input
                  className="search-wide"
                  type="number"
                  min={0}
                  max={Math.max(0, parsedStockTotal || 0)}
                  step={1}
                  value={stockEditRemaining}
                  onChange={(event) => setStockEditRemaining(event.target.value)}
                />
              </label>
              {!canSaveStock && stockValidationMessage ? <p className="diary-inline-error">{stockValidationMessage}</p> : null}
              <div className="diary-sheet-form-actions">
                <Button variant="plain" type="button" className="action-button action-button-ghost" onClick={() => setStockEditCoffeeId(null)} disabled={savingStock}>
                  Cancelar
                </Button>
                <Button variant="plain"
                  type="button"
                  className="action-button"
                  disabled={savingStock || !canSaveStock}
                  onClick={async () => {
                    if (savingStock || !canSaveStock) return;
                    setSavingStock(true);
                    try {
                      await onUpdatePantryStock(
                        stockEditTarget.item.coffee_id,
                        parsedStockTotal,
                        parsedStockRemaining
                      );
                      setStockEditCoffeeId(null);
                    } finally {
                      setSavingStock(false);
                    }
                  }}
                >
                  {savingStock ? "Guardando..." : "Guardar"}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {editEntryTarget ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Editar entrada" onDismiss={() => setEditEntryId(null)} onClick={() => setEditEntryId(null)}>
          <SheetCard className="diary-sheet diary-edit-entry-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">Editar registro de café</strong>
            </header>
            <div className="diary-sheet-form">
              {editEntryIsWater ? (
                <div className="diary-water-presets diary-edit-entry-presets is-water">
                  {[250, 500, 750].map((value) => (
                    <Button variant="chip"
                      key={value}
                      className={`chip-button period-chip ${Number(editAmountMl) === value ? "is-active" : ""}`.trim()}
                      onClick={() => setEditAmountMl(String(value))}
                    >
                      {value} ml
                    </Button>
                  ))}
                </div>
              ) : (
                <div className="diary-edit-entry-coffee-layout">
                  <section className="diary-edit-entry-block">
                    <h4 className="diary-edit-entry-block-title">Preparación</h4>
                    <div
                      ref={editPrepScrollRef}
                      className={`diary-edit-entry-presets is-coffee ${editChipsDragging ? "is-dragging" : ""}`.trim()}
                      onPointerDown={(event) => handleEditScrollPointerDown(event, "prep")}
                      onPointerMove={handleEditScrollPointerMove}
                      onPointerUp={handleEditScrollPointerEnd}
                      onPointerCancel={handleEditScrollPointerEnd}
                    >
                      {editMethodOptions.map((method) => (
                        <Button variant="chip"
                          key={method.label}
                          className={`chip-button period-chip diary-edit-entry-method-chip ${normalizeLookupText(editPreparationType) === normalizeLookupText(method.label) ? "is-active" : ""}`.trim()}
                          onClick={() => setEditPreparationType(method.label)}
                        >
                          <img src={`/android-drawable/${method.drawable}`} alt="" aria-hidden="true" />
                          <span>{method.label}</span>
                        </Button>
                      ))}
                    </div>
                  </section>

                  <section className="diary-edit-entry-metrics-grid">
                    <label className="diary-edit-entry-metric-field">
                      <span>Cafeína (mg)</span>
                      <div className="diary-edit-entry-metric-value">
                        <UiIcon name="caffeine" className="ui-icon" />
                        <Input
                          className="diary-edit-entry-metric-input"
                          type="number"
                          inputMode="numeric"
                          min={0}
                          step={1}
                          value={editCaffeineMg}
                          placeholder="0"
                          onChange={(event) => setEditCaffeineMg(event.target.value)}
                        />
                      </div>
                    </label>
                    <div className="diary-edit-entry-metric-field is-readonly">
                      <span>Dosis (g)</span>
                      <div className="diary-edit-entry-metric-value">
                        <UiIcon name="grind" className="ui-icon" />
                        <strong>{Number(parsedEditDose).toFixed(1).replace(".", ",")}</strong>
                      </div>
                    </div>
                  </section>

                  <section className="diary-edit-entry-block">
                    <h4 className="diary-edit-entry-block-title">Tamaño</h4>
                    <div
                      ref={editSizeScrollRef}
                      className={`diary-coffee-size-presets diary-edit-entry-size-presets ${editChipsDragging ? "is-dragging" : ""}`.trim()}
                      onPointerDown={(event) => handleEditScrollPointerDown(event, "size")}
                      onPointerMove={handleEditScrollPointerMove}
                      onPointerUp={handleEditScrollPointerEnd}
                      onPointerCancel={handleEditScrollPointerEnd}
                    >
                      {editSizeOptions.map((size) => (
                        <Button variant="chip"
                          key={size.label}
                          className={`chip-button period-chip diary-coffee-size-chip ${Math.abs(Number(editAmountMl || 0) - size.ml) <= 20 ? "is-active" : ""}`.trim()}
                          onClick={() => setEditAmountMl(String(size.ml))}
                        >
                          <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" />
                          <span>{size.label}</span>
                          <small>{size.range}</small>
                        </Button>
                      ))}
                    </div>
                  </section>

                  <section className="diary-edit-entry-metric-field is-readonly is-time">
                    <span>Tiempo (HH:mm)</span>
                    <div className="diary-edit-entry-metric-value">
                      <UiIcon name="clock" className="ui-icon" />
                      <strong>{editEntryTime}</strong>
                    </div>
                  </section>
                </div>
              )}
              {!canSaveEditEntry && editValidationMessage ? <p className="diary-inline-error">{editValidationMessage}</p> : null}
              <div className="diary-sheet-form-actions">
                <Button variant="plain"
                  type="button"
                  className="action-button diary-edit-entry-save"
                  disabled={savingEditEntry || !canSaveEditEntry}
                  onClick={async () => {
                    if (savingEditEntry || !canSaveEditEntry) return;
                    setSavingEditEntry(true);
                    try {
                      await onEditEntry(
                        editEntryTarget.id,
                        parsedEditAmount,
                        editEntryIsWater ? 0 : parsedEditCaffeine,
                        editPreparationType
                      );
                      setEditEntryId(null);
                    } finally {
                      setSavingEditEntry(false);
                    }
                  }}
                >
                  {savingEditEntry ? "Guardando..." : "Guardar cambios"}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </>
  );
}

function DiaryActivityRow({
  entry,
  imageUrl,
  brand,
  deleting,
  onOpenEdit,
  onDelete
}: {
  entry: DiaryEntryRow;
  imageUrl: string;
  brand?: string;
  deleting: boolean;
  onOpenEdit: () => void;
  onDelete: () => Promise<void>;
}) {
  const isWaterEntry = (entry.type || "").toUpperCase() === "WATER";
  const entryTitle = entry.coffee_name || (isWaterEntry ? "Agua" : "Entrada");
  const entrySubtitle = (brand || (isWaterEntry ? "Agua" : "Café")).toUpperCase();
  const prepValue = (entry.preparation_type || "").trim() || (isWaterEntry ? "Agua" : "-");
  const doseFromPrep = ((entry.preparation_type || "").match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1] || "").replace(",", ".");
  const doseValue = doseFromPrep ? `${Math.round(Number(doseFromPrep))} g` : (isWaterEntry ? "-" : "15 g");
  const sizeValue = isWaterEntry
    ? `${Math.max(0, entry.amount_ml || 0)} ml`
    : (() => {
      const ml = Math.max(0, entry.amount_ml || 0);
      if (ml <= 45) return "Espresso";
      if (ml <= 120) return "Corto";
      if (ml <= 220) return "Mediano";
      return "Largo";
    })();
  const timeValue = new Date(entry.timestamp).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" });
  const prepDrawable = (() => {
    const normalized = normalizeLookupText(prepValue);
    if (normalized.includes("espresso")) return "maq_espresso.png";
    if (normalized.includes("v60") || normalized.includes("hario")) return "maq_hario_v60.png";
    if (normalized.includes("aero")) return "maq_aeropress.png";
    if (normalized.includes("moka") || normalized.includes("italiana")) return "maq_italiana.png";
    if (normalized.includes("chemex")) return "maq_chemex.png";
    if (normalized.includes("prensa")) return "maq_prensa_francesa.png";
    if (normalized.includes("goteo")) return "maq_goteo.png";
    if (normalized.includes("sifon")) return "maq_sifon.png";
    if (normalized.includes("turco")) return "maq_turco.png";
    return "maq_manual.png";
  })();
  const sizeDrawable = (() => {
    if (isWaterEntry) return null;
    if (sizeValue === "Espresso") return "taza_espresso.png";
    if (sizeValue === "Corto") return "taza_pequeno.png";
    if (sizeValue === "Mediano") return "taza_mediano.png";
    return "taza_grande.png";
  })();
  const metaItems: Array<{ key: string; icon?: IconName; drawable?: string; label: string; value: string }> = [
    { key: "caffeine", icon: "caffeine", label: "Cafeína", value: `${Math.max(0, entry.caffeine_mg || 0)} mg` },
    { key: "prep", drawable: prepDrawable, label: "Preparación", value: prepValue },
    { key: "dose", icon: "grind", label: "Dosis", value: doseValue },
    { key: "size", drawable: sizeDrawable ?? undefined, icon: sizeDrawable ? undefined : "stock", label: "Tamaño", value: sizeValue }
  ];
  const metaScrollRef = useRef<HTMLDivElement | null>(null);
  const metaPointerIdRef = useRef<number | null>(null);
  const metaDragStartXRef = useRef(0);
  const metaDragStartScrollRef = useRef(0);
  const metaRafRef = useRef<number | null>(null);
  const metaPendingScrollRef = useRef(0);
  const metaInteractingRef = useRef(false);
  const [metaDragging, setMetaDragging] = useState(false);
  const [offsetX, setOffsetX] = useState(0);
  const pointerIdRef = useRef<number | null>(null);
  const startXRef = useRef(0);
  const startYRef = useRef(0);
  const swipeActiveRef = useRef(false);
  const movedRef = useRef(false);
  const threshold = -76;
  const maxDrag = -124;

  const handlePointerDown = (event: ReactPointerEvent<HTMLLIElement>) => {
    const target = event.target as HTMLElement | null;
    if (metaInteractingRef.current || target?.closest(".diary-entry-meta-scroll")) return;
    if (deleting) return;
    if (pointerIdRef.current != null) return;
    pointerIdRef.current = event.pointerId;
    startXRef.current = event.clientX;
    startYRef.current = event.clientY;
    swipeActiveRef.current = false;
    movedRef.current = false;
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (metaInteractingRef.current) return;
    if (pointerIdRef.current !== event.pointerId) return;
    const dx = event.clientX - startXRef.current;
    const dy = event.clientY - startYRef.current;
    const absDx = Math.abs(dx);
    const absDy = Math.abs(dy);
    if (!swipeActiveRef.current) {
      if (absDx < 7 && absDy < 7) return;
      if (absDy > absDx + 4) {
        pointerIdRef.current = null;
        setOffsetX(0);
        return;
      }
      if (dx < 0 && absDx > absDy) {
        swipeActiveRef.current = true;
        movedRef.current = true;
        event.currentTarget.setPointerCapture(event.pointerId);
      } else {
        pointerIdRef.current = null;
        setOffsetX(0);
        return;
      }
    }
    if (dx >= 0) {
      setOffsetX(0);
      return;
    }
    setOffsetX(Math.max(maxDrag, dx));
  };

  const handlePointerEnd = async (event: ReactPointerEvent<HTMLLIElement>) => {
    if (metaInteractingRef.current) return;
    if (pointerIdRef.current !== event.pointerId) return;
    pointerIdRef.current = null;
    swipeActiveRef.current = false;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    if (offsetX <= threshold) {
      setOffsetX(0);
      await onDelete();
      return;
    }
    setOffsetX(0);
    window.setTimeout(() => {
      movedRef.current = false;
    }, 80);
  };

  return (
    <li
      className={`diary-card-swipe-wrap ${offsetX < -1 ? "is-swiping" : ""} ${deleting ? "is-dismissing" : ""}`.trim()}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={(event) => {
        void handlePointerEnd(event);
      }}
      onPointerCancel={(event) => {
        void handlePointerEnd(event);
      }}
      onClick={(event) => {
        const target = event.target as HTMLElement | null;
        if (metaInteractingRef.current || target?.closest(".diary-entry-meta-scroll")) return;
        if (movedRef.current || deleting) return;
        onOpenEdit();
      }}
    >
      <div className="diary-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div className="diary-swipe-content" style={{ transform: `translateX(${offsetX}px)` }}>
        <div className="diary-card">
          <div className="diary-entry-head">
            <div className="diary-entry-media">
              {imageUrl ? (
                <img src={imageUrl} alt={entryTitle} loading="lazy" />
              ) : (
                <span className="diary-entry-fallback" aria-hidden="true">
                  <UiIcon name={isWaterEntry ? "stock" : "coffee"} className="ui-icon" />
                </span>
              )}
            </div>
            <div className="diary-entry-copy">
              <p className="feed-user">{entryTitle}</p>
              <p className="feed-meta diary-entry-brand">{entrySubtitle.toUpperCase()}</p>
            </div>
            <div className="diary-entry-time-pill">
              <UiIcon name="clock" className="ui-icon" />
              <span>{timeValue}</span>
            </div>
          </div>
          <div
            ref={metaScrollRef}
            className={`diary-entry-meta-scroll ${metaDragging ? "is-dragging" : ""}`.trim()}
            onPointerDown={(event) => {
              event.stopPropagation();
              metaInteractingRef.current = true;
              const node = metaScrollRef.current;
              if (!node) return;
              if (event.pointerType !== "mouse") return;
              if (node.scrollWidth <= node.clientWidth) return;
              event.preventDefault();
              metaPointerIdRef.current = event.pointerId;
              metaDragStartXRef.current = event.clientX;
              metaDragStartScrollRef.current = node.scrollLeft;
              setMetaDragging(true);
              node.setPointerCapture(event.pointerId);
            }}
            onPointerMove={(event) => {
              event.stopPropagation();
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              event.preventDefault();
              const delta = event.clientX - metaDragStartXRef.current;
              metaPendingScrollRef.current = metaDragStartScrollRef.current - delta;
              if (metaRafRef.current != null) return;
              metaRafRef.current = window.requestAnimationFrame(() => {
                const target = metaScrollRef.current;
                if (target) target.scrollLeft = metaPendingScrollRef.current;
                metaRafRef.current = null;
              });
            }}
            onPointerUp={(event) => {
              event.stopPropagation();
              metaInteractingRef.current = false;
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
              if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
            }}
            onPointerCancel={(event) => {
              event.stopPropagation();
              metaInteractingRef.current = false;
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
              if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
            }}
            onPointerLeave={() => {
              metaInteractingRef.current = false;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
            }}
          >
            <div className="diary-entry-meta-grid">
              {metaItems.map((item) => (
                <div key={item.key} className="diary-entry-meta-item">
                  {item.drawable ? (
                    <img className="diary-entry-meta-drawable" src={`/android-drawable/${item.drawable}`} alt="" aria-hidden="true" />
                  ) : item.icon ? (
                    <UiIcon name={item.icon} className="ui-icon" />
                  ) : null}
                  <span className="diary-entry-meta-label">{item.label}</span>
                  <strong className="diary-entry-meta-value">{item.value}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </li>
  );
}
"""

file_path = r'c:\Users\ramon.demoya\.gemini\antigravity\scratch\cafesito-app-android\webApp\src\features\diary\DiaryView.tsx'

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(restored_content)

print("Restoration complete.")
