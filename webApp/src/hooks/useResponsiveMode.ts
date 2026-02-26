import { useEffect, useState } from "react";
import type { ViewMode } from "../types";

export function useResponsiveMode() {
  const [mode, setMode] = useState<ViewMode>(window.innerWidth < 900 ? "mobile" : "desktop");
  const [viewportWidth, setViewportWidth] = useState<number>(window.innerWidth);

  useEffect(() => {
    const onResize = () => {
      setViewportWidth(window.innerWidth);
      setMode(window.innerWidth < 900 ? "mobile" : "desktop");
    };
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
  }, []);

  return { mode, viewportWidth };
}
