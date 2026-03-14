/// <reference types="vite/client" />
/// <reference types="vite-plugin-pwa/client" />

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
