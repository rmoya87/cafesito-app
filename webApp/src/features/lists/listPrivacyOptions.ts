import type { ListPrivacy } from "../../types";
import type { I18nKey } from "../../i18n/messages";

/** Opciones de privacidad de lista (igual en crear, editar y compartir). */
export function getListPrivacyOptions(t: (key: I18nKey) => string): { value: ListPrivacy; label: string; description: string }[] {
  return [
    {
      value: "public",
      label: t("lists.privacy.public.label"),
      description: t("lists.privacy.public.desc")
    },
    {
      value: "invitation",
      label: t("lists.privacy.invitation.label"),
      description: t("lists.privacy.invitation.desc")
    },
    {
      value: "private",
      label: t("lists.privacy.private.label"),
      description: t("lists.privacy.private.desc")
    }
  ];
}
