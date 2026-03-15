import type { ListPrivacy } from "../../types";

/** Opciones de privacidad de lista (igual en crear, editar y compartir). */
export const LIST_PRIVACY_OPTIONS: { value: ListPrivacy; label: string; description: string }[] = [
  {
    value: "public",
    label: "Pública",
    description: "Cualquier persona puede suscribirse. Visible en actividad."
  },
  {
    value: "invitation",
    label: "Por invitación",
    description: "Solo quienes invites podrán ver la lista. No visible en actividad."
  },
  {
    value: "private",
    label: "Privada",
    description: "Solo tú. No visible en actividad."
  }
];
