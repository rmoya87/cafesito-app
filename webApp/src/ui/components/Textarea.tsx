import { forwardRef, type TextareaHTMLAttributes } from "react";
import { cn } from "./cn";

export type TextareaVariant = "default" | "sheet";

export type TextareaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  variant?: TextareaVariant;
};

const variantClass: Record<TextareaVariant, string> = {
  default: "search-wide",
  sheet: "search-wide sheet-input"
};

export const Textarea = forwardRef<HTMLTextAreaElement, TextareaProps>(function Textarea(
  { variant = "default", className, ...props },
  ref
) {
  return <textarea ref={ref} className={cn(variantClass[variant], className)} {...props} />;
});

