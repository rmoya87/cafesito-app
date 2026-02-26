import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "./cn";

export type IconButtonTone = "default" | "topbar" | "menu";

export type IconButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  tone?: IconButtonTone;
};

const toneClass: Record<IconButtonTone, string> = {
  default: "icon-button",
  topbar: "icon-button topbar-icon-button",
  menu: "icon-button profile-topbar-menu-trigger"
};

export const IconButton = forwardRef<HTMLButtonElement, IconButtonProps>(function IconButton(
  { tone = "default", type = "button", className, ...props },
  ref
) {
  return <button ref={ref} type={type} className={cn(toneClass[tone], className)} {...props} />;
});
