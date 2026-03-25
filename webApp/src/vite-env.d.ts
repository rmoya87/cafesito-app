/// <reference types="vite/client" />
/// <reference types="vite-plugin-pwa/client" />

interface ImportMetaEnv {
  /** Tour por pestañas; "false" u "0" desactiva (paridad con APP_TAB_TOUR_V1_ENABLED en Android). */
  readonly VITE_APP_TAB_TOUR_V1_ENABLED?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

declare global {
  interface Window {
    google?: {
      maps: {
        Map: new (el: HTMLElement, opts?: object) => { fitBounds: (b: unknown, p?: number) => void };
        Marker: new (opts?: object) => { setMap: (m: unknown) => void; addListener: (e: string, fn: () => void) => void };
        LatLngBounds: new () => { extend: (p: { lat: number; lng: number }) => void };
      };
    };
  }
}
