import { useEffect, useRef, useState } from "react";
import { Button, SheetCard, SheetHandle, SheetHeader, SheetOverlay } from "../../ui/components";

export function MobileBarcodeScannerSheet({
  open,
  onClose,
  onDetected
}: {
  open: boolean;
  onClose: () => void;
  onDetected: (value: string) => void;
}) {
  const scannerHostIdRef = useRef(`barcode-scanner-${Math.random().toString(36).slice(2)}`);
  const scannerRef = useRef<{
    stop: () => Promise<void>;
    clear: () => void;
  } | null>(null);
  const closedRef = useRef(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    closedRef.current = false;
    setErrorMessage(null);

    const stopScanner = async () => {
      const scanner = scannerRef.current;
      if (!scanner) return;
      try {
        await scanner.stop();
      } catch {
        // scanner could already be stopped
      }
      try {
        scanner.clear();
      } catch {
        // noop
      }
      scannerRef.current = null;
    };

    const boot = async () => {
      try {
        const isLocalhost =
          window.location.hostname === "localhost" ||
          window.location.hostname === "127.0.0.1" ||
          window.location.hostname === "::1";
        if (!window.isSecureContext && !isLocalhost) {
          setErrorMessage("La cámara en web requiere HTTPS.");
          return;
        }
        if (!window.navigator.mediaDevices?.getUserMedia) {
          setErrorMessage("Este navegador no permite acceso a cámara.");
          return;
        }

        const { Html5Qrcode, Html5QrcodeSupportedFormats } = await import("html5-qrcode");
        const scanner = new Html5Qrcode(scannerHostIdRef.current, {
          verbose: false,
          useBarCodeDetectorIfSupported: true,
          formatsToSupport: [
            Html5QrcodeSupportedFormats.EAN_13,
            Html5QrcodeSupportedFormats.EAN_8,
            Html5QrcodeSupportedFormats.UPC_A,
            Html5QrcodeSupportedFormats.UPC_E,
            Html5QrcodeSupportedFormats.CODE_128,
            Html5QrcodeSupportedFormats.CODE_39,
            Html5QrcodeSupportedFormats.QR_CODE
          ]
        });
        if (closedRef.current) {
          scanner.clear();
          return;
        }
        scannerRef.current = scanner;

        const startScanner = async (cameraConfig: string | MediaTrackConstraints) => {
          await scanner.start(
            cameraConfig,
            {
              fps: 12,
              disableFlip: true,
              qrbox: { width: 320, height: 180 }
            },
            (decodedText) => {
              const value = decodedText.trim();
              if (!value || closedRef.current) return;
              closedRef.current = true;
              onDetected(value);
              onClose();
            },
            () => {
              // ignore per-frame decode errors
            }
          );
        };

        // Estrategia robusta: intentar primero constraints directas y luego IDs de cámara.
        try {
          await startScanner({ facingMode: { exact: "environment" } });
          return;
        } catch {
          // fallback
        }

        try {
          await startScanner({ facingMode: { ideal: "environment" } });
          return;
        } catch {
          // fallback
        }

        let cameras: Array<{ id: string; label: string }> = [];
        try {
          cameras = await Html5Qrcode.getCameras();
        } catch {
          cameras = [];
        }
        const preferredBackCamera =
          cameras.find((camera) => /back|rear|environment|trasera/i.test(camera.label))?.id ??
          cameras[0]?.id ??
          null;

        if (preferredBackCamera) {
          await startScanner(preferredBackCamera);
          return;
        }

        // Último intento.
        await startScanner({ facingMode: "user" });
      } catch (error) {
        const message = error instanceof Error ? error.message : String(error ?? "");
        if (/notallowed|permission|denied/i.test(message)) {
          setErrorMessage("No tenemos permiso para usar la cámara. Revísalo en el navegador.");
          return;
        }
        if (/notfound|overconstrained|constraints/i.test(message)) {
          setErrorMessage("No encontramos una cámara compatible en este dispositivo.");
          return;
        }
        setErrorMessage("No pudimos abrir la cámara. Cierra otras apps que la estén usando y vuelve a intentar.");
      }
    };

    void boot();
    return () => {
      closedRef.current = true;
      void stopScanner();
    };
  }, [onClose, onDetected, open]);

  if (!open) return null;

  return (
    <SheetOverlay className="barcode-scanner-overlay" role="dialog" aria-modal="true" aria-label="Escanear codigo" onClick={onClose}>
      <SheetCard className="barcode-scanner-sheet" onClick={(event) => event.stopPropagation()}>
        <SheetHandle aria-hidden="true" />
        <SheetHeader>
          <strong className="sheet-title barcode-scanner-title">ESCANEAR CODIGO</strong>
          <Button variant="ghost" className="barcode-scanner-close" onClick={onClose}>
            Cerrar
          </Button>
        </SheetHeader>
        <div className="barcode-scanner-body">
          {errorMessage ? (
            <p className="barcode-scanner-error">{errorMessage}</p>
          ) : (
            <div className="barcode-scanner-video-wrap">
              <div id={scannerHostIdRef.current} className="barcode-scanner-video" />
              <div className="barcode-scanner-frame" aria-hidden="true" />
            </div>
          )}
        </div>
      </SheetCard>
    </SheetOverlay>
  );
}
