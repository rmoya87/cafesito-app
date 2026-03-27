import { useCallback, useEffect, useRef, useState } from "react";
import { Button, SheetCard, SheetHandle, SheetHeader, SheetOverlay } from "../../ui/components";
import { useI18n } from "../../i18n";

export function MobileBarcodeScannerSheet({
  open,
  onClose,
  onDetected
}: {
  open: boolean;
  onClose: () => void;
  onDetected: (value: string) => void;
}) {
  const { t } = useI18n();
  const containerRef = useRef<HTMLDivElement>(null);
  const quaggaRef = useRef<typeof import("@ericblade/quagga2").default | null>(null);
  const closedRef = useRef(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Confirmar el mismo código varias veces seguidas para evitar lecturas inventadas
  const REQUIRED_SAME_READS = 2;
  const lastCodeRef = useRef<string | null>(null);
  const sameReadCountRef = useRef(0);

  const handleDetected = useCallback(
    (data: { codeResult?: { code?: string | null } }) => {
      const code = data?.codeResult?.code?.trim();
      if (!code || closedRef.current) return;
      // Ignorar códigos demasiado cortos (ruido típico)
      if (code.length < 6) return;

      if (lastCodeRef.current === code) {
        sameReadCountRef.current += 1;
        if (sameReadCountRef.current >= REQUIRED_SAME_READS) {
          closedRef.current = true;
          onDetected(code);
          onClose();
        }
      } else {
        lastCodeRef.current = code;
        sameReadCountRef.current = 1;
      }
    },
    [onClose, onDetected]
  );

  useEffect(() => {
    if (!open) return;
    closedRef.current = false;
    lastCodeRef.current = null;
    sameReadCountRef.current = 0;
    setErrorMessage(null);
    const el = containerRef.current;
    if (!el) return;

    let cancelled = false;
    let startTimeoutId: ReturnType<typeof setTimeout> | null = null;

    const boot = async () => {
      try {
        const isLocalhost =
          window.location.hostname === "localhost" ||
          window.location.hostname === "127.0.0.1" ||
          window.location.hostname === "::1";
        if (!window.isSecureContext && !isLocalhost) {
          setErrorMessage(t("scanner.httpsRequired"));
          return;
        }
        if (!window.navigator.mediaDevices?.getUserMedia) {
          setErrorMessage(t("scanner.browserNoCamera"));
          return;
        }

        const Quagga = (await import("@ericblade/quagga2")).default;
        if (cancelled) return;
        quaggaRef.current = Quagga;

        Quagga.onDetected(handleDetected);

        Quagga.init(
          {
            inputStream: {
              type: "LiveStream",
              target: el,
              constraints: {
                width: { min: 320, ideal: 640 },
                height: { min: 240, ideal: 480 },
                facingMode: "environment"
              },
              area: {
                top: "25%",
                right: "5%",
                left: "5%",
                bottom: "25%"
              }
            },
            locator: {
              patchSize: "medium",
              halfSample: true
            },
            numOfWorkers: 2,
            frequency: 10,
            decoder: {
              readers: [
                "ean_reader",
                "ean_8_reader",
                "upc_reader",
                "upc_e_reader",
                "code_128_reader",
                "code_39_reader"
              ]
            },
            locate: true
          },
          (err: Error | null) => {
            if (cancelled || closedRef.current) return;
            if (err) {
              const msg = err?.message ?? String(err);
              if (/notallowed|permission|denied/i.test(msg)) {
                setErrorMessage(t("scanner.permissionDenied"));
                return;
              }
              if (/notfound|overconstrained|constraints|no devices/i.test(msg)) {
                setErrorMessage(t("scanner.noCompatibleCamera"));
                return;
              }
              setErrorMessage(t("scanner.openFailed"));
              return;
            }
            // Dar tiempo al stream de video a entregar frames antes de empezar a decodificar
            startTimeoutId = setTimeout(() => {
              startTimeoutId = null;
              if (cancelled || closedRef.current) return;
              const video = el.querySelector("video");
              if (video && typeof video.play === "function") {
                video.play().catch(() => {});
              }
              quaggaRef.current?.start();
            }, 250);
          }
        );
      } catch (e) {
        if (cancelled) return;
        const message = e instanceof Error ? e.message : String(e ?? "");
        if (/notallowed|permission|denied/i.test(message)) {
          setErrorMessage(t("scanner.permissionDenied"));
        } else if (/notfound|overconstrained|constraints/i.test(message)) {
          setErrorMessage(t("scanner.noCompatibleCamera"));
        } else {
          setErrorMessage(t("scanner.openFailed"));
        }
      }
    };

    // Esperar a que el sheet esté pintado antes de init (evita target sin tamaño)
    const rafId = requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        if (cancelled || !open) return;
        void boot();
      });
    });

    return () => {
      cancelled = true;
      closedRef.current = true;
      if (startTimeoutId != null) clearTimeout(startTimeoutId);
      cancelAnimationFrame(rafId);
      const q = quaggaRef.current;
      if (q) {
        try {
          const video = el.querySelector("video");
          if (video && typeof video.pause === "function") {
            video.pause();
          }
          q.offDetected(handleDetected);
          setTimeout(() => {
            try {
              q.stop();
            } catch {
              // ignore
            }
          }, 0);
        } catch {
          // ignore
        }
        quaggaRef.current = null;
      }
    };
  }, [open, handleDetected, t]);

  if (!open) return null;

  return (
    <SheetOverlay
      className="barcode-scanner-overlay"
      role="dialog"
      aria-modal="true"
      aria-label={t("scanner.aria")}
      onDismiss={onClose}
      onClick={onClose}
    >
      <SheetCard className="barcode-scanner-sheet" onClick={(event) => event.stopPropagation()}>
        <SheetHandle aria-hidden="true" />
        <SheetHeader>
          <strong className="sheet-title barcode-scanner-title">{t("scanner.title")}</strong>
          <Button variant="ghost" className="barcode-scanner-close" onClick={onClose}>
            {t("common.close")}
          </Button>
        </SheetHeader>
        <div className="barcode-scanner-body">
          {errorMessage ? (
            <p className="barcode-scanner-error">{errorMessage}</p>
          ) : (
            <div className="barcode-scanner-video-wrap">
              <div ref={containerRef} className="barcode-scanner-video barcode-scanner-quagga-viewport" />
              <div className="barcode-scanner-frame" aria-hidden="true" />
              <p className="barcode-scanner-hint">{t("scanner.hint")}</p>
            </div>
          )}
        </div>
      </SheetCard>
    </SheetOverlay>
  );
}
