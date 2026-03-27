import { useEffect, useState } from "react";
import { useI18n } from "../i18n";

/**
 * Banner fijo que se muestra cuando no hay conexión (PWA / modo nativo).
 * Solo se renderiza si el usuario está offline.
 */
export function OfflineBanner() {
  const { locale } = useI18n();
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
  const label = locale === "es" ? "Sin conexión" : "Offline";

  return (
    <div
      className="offline-banner"
      role="status"
      aria-live="polite"
      aria-label={label}
    >
      <span className="offline-banner-text">{label}</span>
    </div>
  );
}
