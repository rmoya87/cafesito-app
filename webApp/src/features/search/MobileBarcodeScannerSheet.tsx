import { useEffect, useRef, useState } from "react";
import { Button, SheetCard, SheetHandle, SheetHeader, SheetOverlay } from "../../ui/components";

type BarcodeDetectorResult = { rawValue?: string };
type BarcodeDetectorInstance = {
  detect: (source: CanvasImageSource) => Promise<BarcodeDetectorResult[]>;
};
type BarcodeDetectorConstructor = {
  new (options?: { formats?: string[] }): BarcodeDetectorInstance;
};

declare global {
  interface Window {
    BarcodeDetector?: BarcodeDetectorConstructor;
  }
}

export function MobileBarcodeScannerSheet({
  open,
  onClose,
  onDetected
}: {
  open: boolean;
  onClose: () => void;
  onDetected: (value: string) => void;
}) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const rafRef = useRef<number | null>(null);
  const closedRef = useRef(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    closedRef.current = false;
    setErrorMessage(null);

    const stop = () => {
      if (rafRef.current != null) {
        window.cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
        streamRef.current = null;
      }
    };

    const boot = async () => {
      try {
        if (!window.BarcodeDetector) {
          setErrorMessage("Tu navegador no soporta escaneo de codigo en web.");
          return;
        }
        const stream = await window.navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: { ideal: "environment" },
            width: { ideal: 1280 },
            height: { ideal: 720 }
          },
          audio: false
        });
        if (closedRef.current) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        streamRef.current = stream;
        const video = videoRef.current;
        if (!video) return;
        video.srcObject = stream;
        await video.play();
        const detector = new window.BarcodeDetector({
          formats: ["ean_13", "ean_8", "upc_a", "upc_e", "code_128", "code_39", "qr_code"]
        });

        const scanLoop = async () => {
          if (closedRef.current) return;
          const currentVideo = videoRef.current;
          if (!currentVideo || currentVideo.readyState < 2) {
            rafRef.current = window.requestAnimationFrame(scanLoop);
            return;
          }
          try {
            const hits = await detector.detect(currentVideo);
            const value = hits.find((hit) => Boolean(hit.rawValue?.trim()))?.rawValue?.trim();
            if (value) {
              onDetected(value);
              onClose();
              return;
            }
          } catch {
            // continue scanning
          }
          rafRef.current = window.requestAnimationFrame(scanLoop);
        };

        rafRef.current = window.requestAnimationFrame(scanLoop);
      } catch {
        setErrorMessage("No pudimos acceder a la camara del dispositivo.");
      }
    };

    void boot();
    return () => {
      closedRef.current = true;
      stop();
    };
  }, [onClose, onDetected, open]);

  if (!open) return null;

  return (
    <SheetOverlay className="barcode-scanner-overlay" role="dialog" aria-modal="true" aria-label="Escanear codigo" onClick={onClose}>
      <SheetCard className="barcode-scanner-sheet" onClick={(event) => event.stopPropagation()}>
        <SheetHandle aria-hidden="true" />
        <SheetHeader>
          <strong className="sheet-title">ESCANEAR CODIGO</strong>
        </SheetHeader>
        <div className="barcode-scanner-body">
          {errorMessage ? (
            <p className="barcode-scanner-error">{errorMessage}</p>
          ) : (
            <div className="barcode-scanner-video-wrap">
              <video ref={videoRef} className="barcode-scanner-video" playsInline muted autoPlay />
              <div className="barcode-scanner-frame" aria-hidden="true" />
            </div>
          )}
          <Button variant="ghost" className="barcode-scanner-close" onClick={onClose}>
            Cerrar
          </Button>
        </div>
      </SheetCard>
    </SheetOverlay>
  );
}
