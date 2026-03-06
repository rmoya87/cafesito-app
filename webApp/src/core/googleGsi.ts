/**
 * Google Identity Services (GIS) para el botón "Sign in with Google" personalizado.
 * Muestra nombre/email del usuario cuando tiene sesión en Google (como en X.com).
 * Requiere VITE_GOOGLE_CLIENT_ID (mismo Client ID de tipo "Web" que en Supabase → Google).
 */

const GSI_SCRIPT_URL = "https://accounts.google.com/gsi/client";

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: IdConfiguration) => void;
          prompt: (momentListener?: (moment: OneTapMoment) => void) => void;
          renderButton: (
            parent: HTMLElement,
            options: GsiButtonConfiguration
          ) => void;
        };
      };
    };
  }
}

export interface OneTapMoment {
  getDismissedReason: () => string;
  getMomentType: () => string;
  isDisplayed: () => boolean;
  isNotDisplayed: () => boolean;
  isSkippedMoment: () => boolean;
}

export interface IdConfiguration {
  client_id: string;
  callback: (response: CredentialResponse) => void;
  auto_select?: boolean;
  context?: "signin" | "signup" | "use";
  use_fedcm_for_button?: boolean;
  button_auto_select?: boolean;
}

export interface CredentialResponse {
  credential: string;
  select_by?: string;
  clientId?: string;
}

export interface GsiButtonConfiguration {
  type?: "standard" | "icon";
  theme?: "outline" | "filled_blue" | "filled_black";
  size?: "large" | "medium" | "small";
  text?: "signin_with" | "signup_with" | "continue_with" | "signin";
  width?: number;
  use_fedcm_for_button?: boolean;
  button_auto_select?: boolean;
  click_listener?: () => void;
}

let scriptLoadPromise: Promise<boolean> | null = null;

/**
 * Carga el script de Google Identity Services si no está cargado.
 * Devuelve true si window.google.accounts.id está disponible.
 */
export function loadGoogleGsiScript(): Promise<boolean> {
  if (typeof window === "undefined") return Promise.resolve(false);
  if (window.google?.accounts?.id) return Promise.resolve(true);
  if (scriptLoadPromise) return scriptLoadPromise;

  scriptLoadPromise = new Promise<boolean>((resolve) => {
    const existing = document.querySelector(
      `script[src="${GSI_SCRIPT_URL}"]`
    );
    if (existing) {
      const check = () => {
        if (window.google?.accounts?.id) {
          resolve(true);
          return true;
        }
        return false;
      };
      if (check()) return;
      const interval = setInterval(() => {
        if (check()) {
          clearInterval(interval);
        }
      }, 50);
      return;
    }

    const script = document.createElement("script");
    script.src = GSI_SCRIPT_URL;
    script.async = true;
    script.onload = () => {
      // La API puede tardar un frame en estar disponible
      const tryResolve = () => {
        if (window.google?.accounts?.id) {
          resolve(true);
          return true;
        }
        return false;
      };
      if (!tryResolve()) {
        requestAnimationFrame(() => {
          tryResolve() || resolve(!!window.google?.accounts?.id);
        });
      }
    };
    script.onerror = () => resolve(false);
    document.head.appendChild(script);
  });

  return scriptLoadPromise;
}

/**
 * Indica si el Client ID de Google está configurado (env).
 */
export function getGoogleClientId(): string | undefined {
  const id = (import.meta.env?.VITE_GOOGLE_CLIENT_ID as string | undefined)?.trim();
  return id || undefined;
}

/**
 * Inicializa el cliente GIS y renderiza el botón en el elemento dado.
 * Si no hay clientId o el script no carga, no hace nada (usar botón fallback).
 */
export function renderGoogleButton(
  container: HTMLElement,
  clientId: string,
  onCredential: (credential: string) => void
): () => void {
  if (!container || !clientId) return () => {};

  let cancelled = false;

  void loadGoogleGsiScript().then((ok) => {
    if (cancelled || !ok || !window.google?.accounts?.id) return;

    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: (response: CredentialResponse) => {
        if (response.credential) {
          onCredential(response.credential);
        }
      },
      context: "signin",
      use_fedcm_for_button: true,
      button_auto_select: false
    });

    if (cancelled) return;
    // Vaciar el contenedor por si había un fallback
    container.innerHTML = "";
    // type "standard" + size "large" para que se muestre el botón personalizado (nombre/email)
    window.google.accounts.id.renderButton(container, {
      type: "standard",
      theme: "outline",
      size: "large",
      text: "signin_with",
      width: Math.min(container.offsetWidth || 320, 400),
      use_fedcm_for_button: true
    });
  });

  return () => {
    cancelled = true;
  };
}

/**
 * Muestra el One Tap de Google: sugiere "Continuar como [email]" si el usuario
 * tiene sesión en el navegador. Al aceptar, llama a onCredential con el ID token.
 * No pinta ningún botón; solo el popup flotante de Google. Úsalo junto al botón custom.
 */
export function showGoogleOneTap(
  clientId: string,
  onCredential: (credential: string) => void
): () => void {
  if (!clientId) return () => {};

  let cancelled = false;

  void loadGoogleGsiScript().then((ok) => {
    if (cancelled || !ok || !window.google?.accounts?.id) return;

    window.google.accounts.id.initialize({
      client_id: clientId,
      callback: (response: CredentialResponse) => {
        if (!cancelled && response.credential) {
          onCredential(response.credential);
        }
      },
      context: "signin",
      use_fedcm_for_button: false,
      button_auto_select: false
    });

    if (cancelled) return;
    window.google.accounts.id.prompt();
  });

  return () => {
    cancelled = true;
  };
}
