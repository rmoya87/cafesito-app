import { createContext, forwardRef, useCallback, useContext, useEffect, useRef, useState, type HTMLAttributes } from "react";
import { createPortal } from "react-dom";
import { cn } from "./cn";

const DRAG_CLOSE_THRESHOLD_PX = 80;
const SNAP_DURATION_MS = 220;

const SheetDismissContext = createContext<(() => void) | null>(null);

type SheetOverlayProps = HTMLAttributes<HTMLDivElement> & {
  onDismiss?: () => void;
  closeOnEscape?: boolean;
  trapFocus?: boolean;
};

function getFocusable(container: HTMLElement) {
  return Array.from(
    container.querySelectorAll<HTMLElement>(
      "button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])"
    )
  ).filter((el) => !el.hasAttribute("aria-hidden"));
}

export const SheetOverlay = forwardRef<HTMLDivElement, SheetOverlayProps>(function SheetOverlay(
  { className, onDismiss, closeOnEscape = true, trapFocus = true, onKeyDown, children, ...props },
  ref
) {
  const localRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!trapFocus || !localRef.current) return;
    const node = localRef.current;
    const focusable = getFocusable(node);
    if (!focusable.length) {
      node.focus();
      return;
    }
    focusable[0].focus();
  }, [trapFocus]);

  const overlay = (
    <SheetDismissContext.Provider value={onDismiss ?? null}>
      <div
        ref={(node) => {
          localRef.current = node;
          if (typeof ref === "function") ref(node);
          else if (ref) ref.current = node;
        }}
        className={cn("sheet-overlay", className)}
        tabIndex={-1}
        onKeyDown={(event) => {
          onKeyDown?.(event);
          if (event.defaultPrevented || !localRef.current) return;
          if (closeOnEscape && event.key === "Escape" && onDismiss) {
            event.preventDefault();
            onDismiss();
            return;
          }
          if (!trapFocus || event.key !== "Tab") return;
          const focusable = getFocusable(localRef.current);
          if (!focusable.length) return;
          const first = focusable[0];
          const last = focusable[focusable.length - 1];
          const active = document.activeElement as HTMLElement | null;
          if (event.shiftKey && active === first) {
            event.preventDefault();
            last.focus();
            return;
          }
          if (!event.shiftKey && active === last) {
            event.preventDefault();
            first.focus();
          }
        }}
        {...props}
      >
        {children}
      </div>
    </SheetDismissContext.Provider>
  );

  if (typeof document !== "undefined" && document.body) {
    return createPortal(overlay, document.body);
  }
  return overlay;
});

export function SheetCard({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("sheet-card", className)} {...props} />;
}

export function SheetHandle({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  const onDismiss = useContext(SheetDismissContext);
  const handleRef = useRef<HTMLDivElement | null>(null);
  const startYRef = useRef(0);
  const startTranslateRef = useRef(0);
  const currentTranslateRef = useRef(0);
  const isDraggingRef = useRef(false);
  const didDragRef = useRef(false);
  const lastMoveTimeRef = useRef(0);
  const lastYRef = useRef(0);
  const [draggable] = useState(() => Boolean(onDismiss));

  const getCard = useCallback(() => {
    const el = handleRef.current;
    return el?.closest<HTMLElement>(".sheet-card") ?? null;
  }, []);

  useEffect(() => {
    if (!onDismiss || !handleRef.current) return;
    const handle = handleRef.current;

    const onPointerDown = (e: PointerEvent) => {
      if (e.button !== 0) return;
      const card = getCard();
      if (!card) return;
      const style = window.getComputedStyle(card);
      const matrix = new DOMMatrix(style.transform);
      const currentTy = matrix.m42;
      startYRef.current = e.clientY;
      startTranslateRef.current = currentTy;
      currentTranslateRef.current = currentTy;
      isDraggingRef.current = true;
      didDragRef.current = false;
      lastMoveTimeRef.current = Date.now();
      lastYRef.current = e.clientY;
      handle.setPointerCapture(e.pointerId);
    };

    const onPointerMove = (e: PointerEvent) => {
      if (!isDraggingRef.current) return;
      const card = getCard();
      if (!card) return;
      didDragRef.current = true;
      const dy = e.clientY - startYRef.current;
      const translate = Math.max(0, startTranslateRef.current + dy);
      currentTranslateRef.current = translate;
      lastYRef.current = e.clientY;
      lastMoveTimeRef.current = Date.now();
      card.style.transform = `translateY(${translate}px)`;
      card.style.transition = "none";
    };

    const onPointerUp = (e: PointerEvent) => {
      if (!isDraggingRef.current) return;
      const card = getCard();
      try {
        handle.releasePointerCapture(e.pointerId);
      } catch {
        /* ignore */
      }
      isDraggingRef.current = false;

      if (didDragRef.current) {
        e.preventDefault();
        e.stopPropagation();
      }

      if (!card) return;

      const ty = currentTranslateRef.current;
      const dt = Date.now() - lastMoveTimeRef.current;
      const velocity = dt > 0 ? (e.clientY - lastYRef.current) / dt : 0;
      const shouldClose = ty >= DRAG_CLOSE_THRESHOLD_PX || (ty > 20 && velocity > 0.15);

      if (shouldClose && onDismiss) {
        card.style.transition = "";
        card.style.transform = "";
        onDismiss();
        return;
      }

      card.style.transition = `transform ${SNAP_DURATION_MS}ms cubic-bezier(0.2, 0.9, 0.2, 1)`;
      card.style.transform = "translateY(0)";
      const onEnd = () => {
        card.style.transition = "";
        card.removeEventListener("transitionend", onEnd);
      };
      card.addEventListener("transitionend", onEnd);
    };

    handle.addEventListener("pointerdown", onPointerDown, { passive: true });
    document.addEventListener("pointermove", onPointerMove, { passive: true });
    document.addEventListener("pointerup", onPointerUp, { capture: true });
    document.addEventListener("pointercancel", onPointerUp, { capture: true });

    return () => {
      handle.removeEventListener("pointerdown", onPointerDown);
      document.removeEventListener("pointermove", onPointerMove);
      document.removeEventListener("pointerup", onPointerUp, { capture: true });
      document.removeEventListener("pointercancel", onPointerUp, { capture: true });
    };
  }, [onDismiss, getCard]);

  return (
    <div
      ref={(node) => {
        handleRef.current = node;
      }}
      className={cn("sheet-handle", draggable && "sheet-handle-draggable", className)}
      data-draggable={draggable ? "true" : undefined}
      aria-hidden={props["aria-hidden"] ?? true}
      {...props}
    >
      <span className="sheet-handle-line" aria-hidden="true" />
    </div>
  );
}

export function SheetHeader({ className, ...props }: HTMLAttributes<HTMLElement>) {
  return <header className={cn("sheet-header", className)} {...props} />;
}
