import { useState } from "react";
import { cn, Input, SheetCard, SheetHandle, Switch } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";
import type { ListPrivacy } from "../../types";
import { getListPrivacyOptions } from "./listPrivacyOptions";
import { useI18n } from "../../i18n";

export function EditListSheet({
  listId,
  initialName,
  initialPrivacy,
  initialMembersCanEdit = false,
  onDismiss,
  onSave
}: {
  listId: string;
  initialName: string;
  initialPrivacy: ListPrivacy;
  initialMembersCanEdit?: boolean;
  onDismiss: () => void;
  onSave: (listId: string, name: string, privacy: ListPrivacy, membersCanEdit?: boolean) => void | Promise<void>;
}) {
  const { t } = useI18n();
  const [name, setName] = useState(initialName);
  const [privacy, setPrivacy] = useState<ListPrivacy>(initialPrivacy);
  const [membersCanEdit, setMembersCanEdit] = useState(initialMembersCanEdit);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const privacyOptions = getListPrivacyOptions(t);

  const handleSave = async () => {
    if (!name.trim()) return;
    setError(null);
    setSaving(true);
    try {
      const result = onSave(
        listId,
        name.trim(),
        privacy,
        privacy === "public" || privacy === "invitation" ? membersCanEdit : undefined
      );
      await (typeof (result as Promise<unknown>)?.then === "function" ? result : Promise.resolve());
      onDismiss();
    } catch (err) {
      setError((err as Error)?.message ?? t("lists.errorSave"));
    } finally {
      setSaving(false);
    }
  };

  return (
    <SheetCard className="create-list-sheet diary-edit-entry-sheet edit-list-sheet" onClick={(e) => e.stopPropagation()}>
      <SheetHandle aria-hidden="true" />
      <header className="sheet-header diary-edit-entry-header create-list-sheet-header">
        <span className="diary-edit-entry-header-spacer" aria-hidden="true" />
        <strong className="sheet-title">{t("lists.editList")}</strong>
        <button
          type="button"
          className="diary-edit-entry-header-action is-save create-list-sheet-submit"
          disabled={!name.trim() || saving}
          onClick={() => void handleSave()}
        >
          {saving ? t("common.saving") : t("common.save")}
        </button>
      </header>
      <div className="sheet-body create-list-sheet-body">
        <label className="diary-edit-entry-metric-field is-caffeine create-list-field">
          <span>{t("lists.listName")}</span>
          <div className="diary-edit-entry-metric-value">
            <Input
              type="text"
              className="diary-edit-entry-metric-input create-list-name-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder={t("lists.listNamePlaceholder")}
            />
          </div>
        </label>
        <h3 className="create-list-privacy-subtitle">{t("lists.privacy")}</h3>
        <div className="create-list-privacy-card">
          <div className="create-list-privacy-options">
            {privacyOptions.map((opt) => (
              <button
                key={opt.value}
                type="button"
                className={cn("create-list-privacy-option-btn", privacy === opt.value && "is-selected")}
                onClick={() => setPrivacy(opt.value)}
                aria-pressed={privacy === opt.value}
              >
                <span className="create-list-privacy-option-check">
                  {privacy === opt.value ? (
                    <UiIcon name="check-circle-filled" aria-hidden="true" />
                  ) : (
                    <span className="create-list-privacy-option-check-empty" aria-hidden="true" />
                  )}
                </span>
                <div className="create-list-privacy-option-text">
                  <span className="create-list-privacy-option-label">{opt.label}</span>
                  <span className="create-list-privacy-option-desc">{opt.description}</span>
                </div>
              </button>
            ))}
          </div>
          {(privacy === "public" || privacy === "invitation") && (
            <div
              className="share-list-privacy-option-switch-row create-list-privacy-option-switch-row"
              onClick={(e) => e.stopPropagation()}
            >
              <span className="share-list-privacy-option-switch-label">{t("lists.allowEdit")}</span>
              <Switch
                checked={membersCanEdit}
                onClick={(e) => {
                  e.stopPropagation();
                  setMembersCanEdit((prev) => !prev);
                }}
                aria-label={t("lists.allowEditAria")}
              />
            </div>
          )}
        </div>
        {error && <p className="create-list-error" role="alert">{error}</p>}
      </div>
    </SheetCard>
  );
}
