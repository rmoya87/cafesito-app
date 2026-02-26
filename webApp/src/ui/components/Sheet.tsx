import { type HTMLAttributes } from "react";
import { cn } from "./cn";

export function SheetOverlay({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("sheet-overlay", className)} {...props} />;
}

export function SheetCard({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("sheet-card", className)} {...props} />;
}

export function SheetHandle({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("sheet-handle", className)} {...props} />;
}

export function SheetHeader({ className, ...props }: HTMLAttributes<HTMLElement>) {
  return <header className={cn("sheet-header", className)} {...props} />;
}
