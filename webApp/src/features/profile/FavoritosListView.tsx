import { useCallback, useMemo, useRef, useState } from "react";
import type { CoffeeRow } from "../../types";
import { UiIcon } from "../../ui/iconography";

const SWIPE_THRESHOLD_PX = 3;
const VERTICAL_LOCK_PX = 2;
const SWIPE_THRESHOLD = -72;
const MAX_LEFT = -110;

function FavoritosListRow({
  coffee,
  onOpenCoffee,
  onRemoveFavorite
}: {
  coffee: CoffeeRow;
  onOpenCoffee: (coffeeId: string) => void;
  onRemoveFavorite?: (coffeeId: string) => Promise<void>;
}) {
  const canRemove = typeof onRemoveFavorite === "function";
  const [isRemoving, setIsRemoving] = useState(false);
  const [offsetX, setOffsetX] = useState(0);
  const [swipeActive, setSwipeActive] = useState(false);
  const startXRef = useRef<number | null>(null);
  const startYRef = useRef<number | null>(null);
  const pointerIdRef = useRef<number | null>(null);
  const offsetRef = useRef(0);
  const contentRef = useRef<HTMLDivElement | null>(null);
  const rafIdRef = useRef<number | null>(null);
  const movedRef = useRef(false);

  const resetSwipe = useCallback(() => {
    setOffsetX(0);
    setSwipeActive(false);
    startXRef.current = null;
    startYRef.current = null;
    pointerIdRef.current = null;
    offsetRef.current = 0;
    movedRef.current = false;
    const el = contentRef.current;
    if (el) el.style.transform = "";
    if (rafIdRef.current != null) {
      cancelAnimationFrame(rafIdRef.current);
      rafIdRef.current = null;
    }
  }, []);

  return (
    <li className="profile-favorite-item favoritos-list-item">
      {canRemove ? (
        <div className="profile-favorite-swipe-bg" aria-hidden="true">
          <UiIcon name="trash" className="ui-icon" />
        </div>
      ) : null}
      <div
        ref={contentRef}
        className={`favoritos-list-card profile-favorite-row ${swipeActive || offsetX < -1 ? "is-swiping" : ""}`.trim()}
        style={{ transform: `translateX(${offsetX}px)` }}
        role="button"
        tabIndex={0}
        aria-label={`Ver detalle de ${coffee.nombre}`}
        onClick={(event) => {
          if (movedRef.current || Math.abs(offsetX) > 4) {
            event.preventDefault();
            return;
          }
          onOpenCoffee(coffee.id);
        }}
        onPointerDown={(event) => {
          if (!canRemove || isRemoving) return;
          pointerIdRef.current = event.pointerId;
          startXRef.current = event.clientX;
          startYRef.current = event.clientY;
          movedRef.current = false;
        }}
        onPointerMove={(event) => {
          if (!canRemove || startXRef.current == null || startYRef.current == null || pointerIdRef.current !== event.pointerId) return;
          const dx = event.clientX - startXRef.current;
          const dy = event.clientY - startYRef.current;
          const absDx = Math.abs(dx);
          const absDy = Math.abs(dy);
          if (!swipeActive) {
            if (absDx < SWIPE_THRESHOLD_PX && absDy < VERTICAL_LOCK_PX) return;
            if (absDy > absDx + VERTICAL_LOCK_PX) return;
            if (dx < 0 && absDx > absDy) {
              setSwipeActive(true);
              movedRef.current = true;
              event.currentTarget.setPointerCapture(event.pointerId);
            } else return;
          }
          if (dx >= 0) {
            offsetRef.current = 0;
            setOffsetX(0);
            const el = contentRef.current;
            if (el) el.style.transform = "translateX(0px)";
            return;
          }
          const raw = dx;
          const withResistance = raw <= MAX_LEFT ? MAX_LEFT + (raw - MAX_LEFT) * 0.25 : raw;
          const offset = Math.max(MAX_LEFT, Math.min(0, withResistance));
          offsetRef.current = offset;
          const el = contentRef.current;
          if (el) el.style.transform = `translateX(${offset}px)`;
          if (rafIdRef.current == null) {
            rafIdRef.current = requestAnimationFrame(() => {
              setOffsetX(offsetRef.current);
              rafIdRef.current = null;
            });
          }
        }}
        onPointerUp={async (event) => {
          if (pointerIdRef.current !== event.pointerId) return;
          if (rafIdRef.current != null) {
            cancelAnimationFrame(rafIdRef.current);
            rafIdRef.current = null;
          }
          pointerIdRef.current = null;
          startXRef.current = null;
          startYRef.current = null;
          setSwipeActive(false);
          if (event.currentTarget.hasPointerCapture(event.pointerId)) {
            event.currentTarget.releasePointerCapture(event.pointerId);
          }
          const finalOffset = offsetRef.current;
          const el = contentRef.current;
          if (el) el.style.transform = "";
          if (finalOffset <= SWIPE_THRESHOLD && !isRemoving && onRemoveFavorite) {
            setOffsetX(0);
            setIsRemoving(true);
            try {
              await onRemoveFavorite(coffee.id);
            } finally {
              setIsRemoving(false);
            }
            resetSwipe();
            return;
          }
          setOffsetX(finalOffset);
          requestAnimationFrame(() => setOffsetX(0));
          window.setTimeout(() => { movedRef.current = false; }, 120);
        }}
        onPointerCancel={() => resetSwipe()}
        onKeyDown={(e) => {
          if (e.key === "Enter" || e.key === " ") {
            e.preventDefault();
            onOpenCoffee(coffee.id);
          }
        }}
      >
        <span className="profile-favorite-media">
          {coffee.image_url ? (
            <img className="profile-favorite-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
          ) : (
            <span className="profile-favorite-image profile-favorite-fallback" aria-hidden="true">
              {(coffee.nombre ?? "C").slice(0, 1).toUpperCase()}
            </span>
          )}
        </span>
        <div className="profile-favorite-copy">
          <span className="profile-favorite-name">{coffee.nombre}</span>
          {coffee.marca ? <span className="profile-favorite-marca">{coffee.marca}</span> : null}
        </div>
      </div>
    </li>
  );
}

export function FavoritosListView({
  favoriteCoffees,
  onOpenCoffee,
  onRemoveFavorite
}: {
  favoriteCoffees: CoffeeRow[];
  onOpenCoffee: (coffeeId: string) => void;
  onRemoveFavorite?: (coffeeId: string) => Promise<void>;
}) {
  const sorted = useMemo(
    () => [...favoriteCoffees].sort((a, b) => (a.nombre ?? "").localeCompare(b.nombre ?? "")),
    [favoriteCoffees]
  );

  return (
    <section className="favoritos-list-view" aria-label="Lista de favoritos">
      {sorted.length === 0 ? (
        <p className="favoritos-list-empty">No hay cafés favoritos</p>
      ) : (
        <ul className="coffee-list profile-favorite-list favoritos-list-list">
          {sorted.map((coffee) => (
            <FavoritosListRow
              key={coffee.id}
              coffee={coffee}
              onOpenCoffee={onOpenCoffee}
              onRemoveFavorite={onRemoveFavorite}
            />
          ))}
        </ul>
      )}
    </section>
  );
}
