import { useState, type MutableRefObject } from "react";
import type { CoffeeRow, UserRow } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { Button, ComposerInputShell, IconButton, Input, SheetCard, SheetHandle, SheetOverlay, Topbar } from "../../ui/components";

export function CreatePostSheet({
  open,
  onClose,
  imageFile,
  text,
  setText,
  onPublish,
  imageInputRef,
  onAppendFiles,
  imagePreviewUrl,
  onRemoveSelectedImage,
  activeUser,
  showEmojiPanel,
  setShowEmojiPanel,
  mentionSuggestions,
  resolveMentionUser,
  onOpenCoffeePicker,
  selectedCoffee,
  showCoffeeSheet,
  onCloseCoffeeSheet,
  coffeeQuery,
  setCoffeeQuery,
  filteredCoffees,
  selectedCoffeeId,
  showBarcodeButton,
  onBarcodeClick,
  onSelectCoffee
}: {
  open: boolean;
  onClose: () => void;
  imageFile: File | null;
  text: string;
  setText: (value: string) => void;
  onPublish: () => void;
  imageInputRef: MutableRefObject<HTMLInputElement | null>;
  onAppendFiles: (files: File[]) => void;
  imagePreviewUrl: string;
  onRemoveSelectedImage: () => void;
  activeUser: UserRow | null;
  showEmojiPanel: boolean;
  setShowEmojiPanel: (value: boolean) => void;
  mentionSuggestions: UserRow[];
  resolveMentionUser?: (username: string) => { username: string; avatarUrl?: string | null } | null | undefined;
  onOpenCoffeePicker: () => void;
  selectedCoffee: CoffeeRow | null;
  showCoffeeSheet: boolean;
  onCloseCoffeeSheet: () => void;
  coffeeQuery: string;
  setCoffeeQuery: (value: string) => void;
  filteredCoffees: CoffeeRow[];
  selectedCoffeeId: string;
  showBarcodeButton?: boolean;
  onBarcodeClick?: () => void;
  onSelectCoffee: (coffeeId: string) => void;
}) {
  const [coffeeSearchFocus, setCoffeeSearchFocus] = useState(false);
  const showCoffeeSearchCancel = Boolean(coffeeQuery || coffeeSearchFocus);

  return (
    <>
      {open ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Crear publicación" onDismiss={onClose} onClick={onClose}>
          <SheetCard className="create-post-sheet" onClick={(event) => event.stopPropagation()}>
            <Topbar centered className="topbar-timeline create-post-header">
              <div className="topbar-slot">
                <IconButton
                  type="button"
                  tone="topbar"
                  className="create-post-close"
                  aria-label="Cerrar"
                  onClick={onClose}
                >
                  <UiIcon name="close" className="ui-icon" />
                </IconButton>
              </div>
              <h2 className="title title-upper topbar-title-center topbar-brand-title create-post-title">NUEVO POST</h2>
              <div className="topbar-slot topbar-slot-end">
                <Button variant="text" type="button" className="text-button create-post-publish" onClick={onPublish} disabled={!text.trim() && !imageFile}>
                  PUBLICAR
                </Button>
              </div>
            </Topbar>
            <div className="create-post-body create-post-flow create-post-step-1">
              <Input
                ref={imageInputRef}
                type="file"
                accept="image/*"
                multiple
                className="file-input-hidden"
                onChange={(event) => {
                  onAppendFiles(Array.from(event.target.files ?? []));
                  event.currentTarget.value = "";
                }}
              />
              {activeUser ? (
                <div className="create-post-user-row">
                  {activeUser.avatar_url ? <img src={activeUser.avatar_url} alt={activeUser.username} loading="lazy" decoding="async" referrerPolicy="no-referrer" crossOrigin="anonymous" /> : <div className="create-post-user-fallback">{activeUser.username.slice(0, 1).toUpperCase()}</div>}
                  <div>
                    <p>{activeUser.full_name}</p>
                    <span>@{activeUser.username}</span>
                  </div>
                </div>
              ) : null}
              <ComposerInputShell
                value={text}
                onChange={setText}
                textareaId="new-post-text"
                placeholder="¿Qué estás pensando?"
                rows={4}
                shellClassName="create-post-composer-card"
                textareaClassName="create-post-textarea"
                showEmojiPanel={showEmojiPanel}
                emojis={["☕", "😍", "🔥", "👏", "🤎", "✨"]}
                onEmojiSelect={(emoji) => setText(`${text}${emoji}`)}
                mentionSuggestions={!text.trim().endsWith("@") ? mentionSuggestions : []}
                onInsertMention={(username) => {
                  const parts = text.split(/\s+/);
                  parts[parts.length - 1] = `@${username}`;
                  setText(`${parts.join(" ")} `);
                }}
                resolveMentionUser={resolveMentionUser}
                emojiPanelClassName="create-post-inline-panel"
                mentionPanelClassName="create-post-inline-panel create-post-mention-suggestions"
                bottomClassName="create-post-composer-tools"
                toolsContent={
                  <>
                    <IconButton type="button" tone="default" onClick={() => { setText(`${text}@`); setShowEmojiPanel(false); }} aria-label="Mencionar">
                      <UiIcon name="at" className="ui-icon" />
                    </IconButton>
                    <IconButton type="button" tone="default" className={showEmojiPanel ? "is-active" : ""} onClick={() => setShowEmojiPanel(!showEmojiPanel)} aria-label="Emojis">
                      <UiIcon name="smile" className="ui-icon" />
                    </IconButton>
                  </>
                }
              />
              {imagePreviewUrl ? (
                <div className="create-post-image-detail-wrap">
                  <img className="create-post-preview create-post-preview-detail" src={imagePreviewUrl} alt="Previsualizacion" loading="lazy" decoding="async" />
                  <Button variant="plain" type="button" className="create-post-image-remove" onClick={onRemoveSelectedImage} aria-label="Quitar foto">
                    <svg className="create-post-image-remove-icon" viewBox="0 0 24 24" aria-hidden="true">
                      <path d="M6 6l12 12M18 6L6 18" />
                    </svg>
                  </Button>
                </div>
              ) : (
                <Button variant="plain" type="button" className="create-post-add-photo-row" onClick={() => imageInputRef.current?.click()}>
                  <UiIcon name="camera-filled" className="ui-icon" />
                  <span>Añadir foto</span>
                  <UiIcon name="chevron-right" className="ui-icon" />
                </Button>
              )}
              <Button variant="plain" type="button" className="create-post-coffee-row" onClick={onOpenCoffeePicker}>
                <UiIcon name="coffee-filled" className="ui-icon" />
                <span>Añadir café</span>
                <strong>{selectedCoffee ? selectedCoffee.nombre : "Seleccionar café"}</strong>
                <UiIcon name="chevron-right" className="ui-icon" />
              </Button>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showCoffeeSheet ? (
        <SheetOverlay className="create-post-coffee-overlay" role="dialog" aria-modal="true" aria-label="Seleccionar café" onDismiss={onCloseCoffeeSheet} onClick={onCloseCoffeeSheet}>
          <SheetCard className="create-post-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">SELECCIONAR CAFÉ</strong>
            </header>
            <div className="create-post-coffee-body">
              <div className={`search-row-with-cancel ${showCoffeeSearchCancel ? "has-cancel" : ""}`.trim()}>
                <div className="search-coffee-field">
                  <UiIcon name="search" className="ui-icon search-coffee-leading-icon" aria-hidden="true" />
                  <Input
                    variant="search"
                    className="search-wide search-coffee-input"
                    placeholder="Buscar café"
                    value={coffeeQuery}
                    onChange={(event) => setCoffeeQuery(event.target.value)}
                    onFocus={() => setCoffeeSearchFocus(true)}
                    onBlur={() => setCoffeeSearchFocus(false)}
                    aria-label="Buscar café"
                  />
                  {showBarcodeButton && onBarcodeClick ? (
                    <Button
                      variant="plain"
                      type="button"
                      className="search-coffee-trailing-button"
                      aria-label="Escanear código de barras"
                      onClick={() => onBarcodeClick()}
                    >
                      <UiIcon name="barcode" className="ui-icon" />
                    </Button>
                  ) : null}
                </div>
                <Button
                  variant="text"
                  type="button"
                  className={`search-cancel-button ${showCoffeeSearchCancel ? "is-visible" : ""}`}
                  onClick={() => {
                    setCoffeeQuery("");
                    setCoffeeSearchFocus(false);
                    const el = document.activeElement;
                    if (el instanceof HTMLElement) el.blur();
                  }}
                  aria-hidden={!showCoffeeSearchCancel}
                  tabIndex={showCoffeeSearchCancel ? 0 : -1}
                >
                  Cancelar
                </Button>
              </div>
              <ul className="create-post-coffee-list">
                {filteredCoffees.map((coffee) => (
                  <li key={coffee.id}>
                    <Button variant="plain" type="button" className={`create-post-coffee-item ${coffee.id === selectedCoffeeId ? "is-selected" : ""}`} onClick={() => onSelectCoffee(coffee.id)}>
                      {coffee.image_url ? <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" /> : <span className="create-post-coffee-fallback">{coffee.nombre.slice(0, 1).toUpperCase()}</span>}
                      <div>
                        <p>{coffee.nombre}</p>
                        <span>{coffee.marca}</span>
                      </div>
                    </Button>
                  </li>
                ))}
              </ul>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </>
  );
}




