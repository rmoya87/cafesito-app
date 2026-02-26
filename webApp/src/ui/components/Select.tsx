import { forwardRef, type SelectHTMLAttributes } from "react";
import { cn } from "./cn";

export type SelectVariant = "default" | "sheet";

export type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & {
  variant?: SelectVariant;
};

const variantClass: Record<SelectVariant, string> = {
  default: "search-wide",
  sheet: "search-wide sheet-input"
};

export const Select = forwardRef<HTMLSelectElement, SelectProps>(function Select(
  { variant = "default", className, ...props },
  ref
) {
  return <select ref={ref} className={cn(variantClass[variant], className)} {...props} />;
});

