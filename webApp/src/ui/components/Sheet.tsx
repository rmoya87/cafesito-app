import { forwardRef, type HTMLAttributes, useEffect, useRef } from "react";
import { cn } from "./cn";

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
  { className, onDismiss, closeOnEscape = true, trapFocus = true, onKeyDown, ...props },
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

  return (
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
    />
  );
});

export function SheetCard({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("sheet-card", className)} {...props} />;
}

export function SheetHandle({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("sheet-handle", className)} {...props} />;
}

export function SheetHeader({ className, ...props }: HTMLAttributes<HTMLElement>) {
  return <header className={cn("sheet-header", className)} {...props} />;
}
