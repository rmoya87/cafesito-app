import { useEffect, useState } from "react";

/**
 * Banner fijo que se muestra cuando no hay conexión (PWA / modo nativo).
 * Solo se renderiza si el usuario está offline.
 */
export function OfflineBanner() {
  const [isOnline, setIsOnline] = useState(
    typeof navigator !== "undefined" ? navigator.onLine : true
  );

  useEffect(() => {
    const handleOnline = () => setIsOnline(true);
    const handleOffline = () => setIsOnline(false);
    window.addEventListener("online", handleOnline);
    window.addEventListener("offline", handleOffline);
    return () => {
      window.removeEventListener("online", handleOnline);
      window.removeEventListener("offline", handleOffline);
    };
  }, []);

  if (isOnline) return null;

  return (
    <div
      className="offline-banner"
      role="status"
      aria-live="polite"
      aria-label="Sin conexión"
    >
      <span className="offline-banner-text">Sin conexión</span>
    </div>
  );
}
