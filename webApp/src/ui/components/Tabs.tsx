import { type ButtonHTMLAttributes, type HTMLAttributes } from "react";
import { cn } from "./cn";

export function Tabs({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("premium-tabs", className)} role="tablist" {...props} />;
}

export type TabButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  active?: boolean;
};

export function TabButton({ active = false, className, type = "button", ...props }: TabButtonProps) {
  return <button type={type} className={cn("premium-tab", active && "is-active", className)} {...props} />;
}
