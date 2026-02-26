import { forwardRef, type HTMLAttributes } from "react";
import { cn } from "./cn";

export type CardTone = "default" | "coffee" | "config";

export type CardProps = HTMLAttributes<HTMLDivElement> & {
  tone?: CardTone;
};

const toneClass: Record<CardTone, string> = {
  default: "card",
  coffee: "coffee-card",
  config: "config-card"
};

export const Card = forwardRef<HTMLDivElement, CardProps>(function Card(
  { tone = "default", className, ...props },
  ref
) {
  return <div ref={ref} className={cn(toneClass[tone], className)} {...props} />;
});
