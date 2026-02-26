import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "./cn";

export type ChipProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  active?: boolean;
};

export const Chip = forwardRef<HTMLButtonElement, ChipProps>(function Chip(
  { active = false, className, type = "button", ...props },
  ref
) {
  return <button ref={ref} type={type} className={cn("filter-chip", active && "is-active", className)} {...props} />;
});
