import { forwardRef, type HTMLAttributes } from "react";
import { cn } from "./cn";

export type TopbarProps = HTMLAttributes<HTMLElement> & {
  centered?: boolean;
};

export const Topbar = forwardRef<HTMLElement, TopbarProps>(function Topbar(
  { centered = false, className, ...props },
  ref
) {
  return <header ref={ref} className={cn("topbar", centered && "topbar-centered", className)} {...props} />;
});

