import { useState } from "react";
import { Input, Select, SheetCard, SheetHandle } from "../../ui/components";

export function CreateListSheet({
  onDismiss,
  onCreate
}: {
  onDismiss: () => void;
  onCreate: (name: string, isPublic: boolean) => void | Promise<void>;
}) {
  const [name, setName] = useState("");
  const [privacy, setPrivacy] = useState<"public" | "private">("private");
  const [creating, setCreating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const handleCreate = async () => {
    if (!name.trim()) return;
    setError(null);
    setCreating(true);
    try {
      const result = onCreate(name.trim(), privacy === "public");
      await (typeof (result as Promise<unknown>)?.then === "function" ? result : Promise.resolve());
      onDismiss();
    } catch (err) {
      setError((err as Error)?.message ?? "Error al crear la lista");
    } finally {
      setCreating(false);
    }
  };
  return (
    <SheetCard className="create-list-sheet diary-edit-entry-sheet" onClick={(e) => e.stopPropagation()}>
      <SheetHandle aria-hidden="true" />
      <header className="sheet-header diary-edit-entry-header create-list-sheet-header">
        <span className="diary-edit-entry-header-spacer" aria-hidden="true" />
        <strong className="sheet-title">Nueva lista</strong>
        <button
          type="button"
          className="diary-edit-entry-header-action is-save create-list-sheet-submit"
          disabled={!name.trim() || creating}
          onClick={() => void handleCreate()}
        >
          {creating ? "Creando…" : "Crear lista"}
        </button>
      </header>
      <div className="sheet-body create-list-sheet-body">
        <label className="diary-edit-entry-metric-field is-caffeine create-list-field">
          <span>Nombre de la lista</span>
          <div className="diary-edit-entry-metric-value">
            <Input
              type="text"
              className="diary-edit-entry-metric-input create-list-name-input"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Ej: Para probar"
            />
          </div>
        </label>
        <label className="diary-edit-entry-metric-field is-caffeine create-list-field">
          <span>Privacidad</span>
          <div className="diary-edit-entry-metric-value">
            <Select
              className="diary-edit-entry-metric-input create-list-privacy-select"
              value={privacy}
              onChange={(e) => setPrivacy(e.target.value === "public" ? "public" : "private")}
              aria-label="Privacidad"
            >
              <option value="public">Público</option>
              <option value="private">Privado</option>
            </Select>
          </div>
        </label>
        <p className="create-list-hint">Público: visible en la pestaña de seguidores. Privado: solo tú.</p>
        {error && <p className="create-list-error" role="alert">{error}</p>}
      </div>
    </SheetCard>
  );
}
