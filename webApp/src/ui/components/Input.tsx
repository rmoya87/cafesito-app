import { forwardRef, type InputHTMLAttributes } from "react";
import { cn } from "./cn";

export type InputVariant = "search" | "default";

export type InputProps = InputHTMLAttributes<HTMLInputElement> & {
  variant?: InputVariant;
};

const variantClass: Record<InputVariant, string> = {
  default: "search-wide",
  search: "search-wide search-input-standard"
};

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { variant = "default", className, ...props },
  ref
) {
  return <input ref={ref} className={cn(variantClass[variant], className)} {...props} />;
});
