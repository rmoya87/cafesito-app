import { forwardRef, type ButtonHTMLAttributes } from "react";
import { cn } from "./cn";

export type SwitchProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, "role"> & {
  checked: boolean;
};

export const Switch = forwardRef<HTMLButtonElement, SwitchProps>(function Switch(
  { checked, className, type = "button", ...props },
  ref
) {
  return (
    <button
      ref={ref}
      type={type}
      role="switch"
      aria-checked={checked}
      className={cn("ui-switch", checked && "is-on", className)}
      {...props}
    >
      <span className="ui-switch-thumb" />
    </button>
  );
});

