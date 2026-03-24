export type SharePayload = {
  title?: string;
  text?: string;
  url?: string;
};

export type ShareResult = {
  ok: boolean;
  method: "native" | "clipboard" | "none";
};

export function supportsNativeShare(): boolean {
  return typeof navigator !== "undefined" && typeof navigator.share === "function";
}

export async function copyToClipboard(text: string): Promise<boolean> {
  if (!text.trim()) return false;
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    try {
      const input = document.createElement("input");
      input.value = text;
      document.body.appendChild(input);
      input.select();
      const copied = document.execCommand("copy");
      document.body.removeChild(input);
      return copied;
    } catch {
      return false;
    }
  }
}

/**
 * Comparte un enlace usando Web Share API cuando existe;
 * si no está disponible, intenta copiar al portapapeles.
 */
export async function shareOrCopyLink(payload: SharePayload): Promise<ShareResult> {
  const url = String(payload.url ?? "").trim();
  if (!url) return { ok: false, method: "none" };

  if (supportsNativeShare()) {
    try {
      await navigator.share({
        title: payload.title,
        text: payload.text,
        url
      });
      return { ok: true, method: "native" };
    } catch {
      // Si el usuario cancela o el share falla, intentamos fallback.
    }
  }

  const copied = await copyToClipboard(url);
  return copied ? { ok: true, method: "clipboard" } : { ok: false, method: "none" };
}
