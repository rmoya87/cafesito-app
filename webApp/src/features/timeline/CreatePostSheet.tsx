import { useState, type MutableRefObject } from "react";
import type { CoffeeRow, UserRow } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { Button, IconButton, Input, SheetCard, SheetHandle, SheetOverlay, Textarea, Topbar } from "../../ui/components";

export function CreatePostSheet({
  open,
  onClose,
  step,
  setStep,
  imageFile,
  text,
  setText,
  onPublish,
  imageInputRef,
  cameraInputRef,
  onAppendFiles,
  imagePreviewUrl,
  isDesktopComposer,
  galleryItems,
  selectedImageId,
  onSelectGalleryItem,
  onRemoveSelectedImage,
  activeUser,
  showEmojiPanel,
  setShowEmojiPanel,
  mentionSuggestions,
  onOpenCoffeePicker,
  selectedCoffee,
  showCoffeeSheet,
  onCloseCoffeeSheet,
  coffeeQuery,
  setCoffeeQuery,
  filteredCoffees,
  selectedCoffeeId,
  onSelectCoffee
}: {
  open: boolean;
  onClose: () => void;
  step: 0 | 1;
  setStep: (value: 0 | 1) => void;
  imageFile: File | null;
  text: string;
  setText: (value: string) => void;
  onPublish: () => void;
  imageInputRef: MutableRefObject<HTMLInputElement | null>;
  cameraInputRef: MutableRefObject<HTMLInputElement | null>;
  onAppendFiles: (files: File[]) => void;
  imagePreviewUrl: string;
  isDesktopComposer: boolean;
  galleryItems: Array<{ id: string; file: File; previewUrl: string }>;
  selectedImageId: string | null;
  onSelectGalleryItem: (itemId: string) => void;
  onRemoveSelectedImage: () => void;
  activeUser: UserRow | null;
  showEmojiPanel: boolean;
  setShowEmojiPanel: (value: boolean) => void;
  mentionSuggestions: UserRow[];
  onOpenCoffeePicker: () => void;
  selectedCoffee: CoffeeRow | null;
  showCoffeeSheet: boolean;
  onCloseCoffeeSheet: () => void;
  coffeeQuery: string;
  setCoffeeQuery: (value: string) => void;
  filteredCoffees: CoffeeRow[];
  selectedCoffeeId: string;
  onSelectCoffee: (coffeeId: string) => void;
}) {
  const [coffeeSearchFocus, setCoffeeSearchFocus] = useState(false);
  const showCoffeeSearchCancel = Boolean(coffeeQuery || coffeeSearchFocus);

  return (
    <>
      {open ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Crear publicacion" onDismiss={onClose} onClick={onClose}>
          <SheetCard className="create-post-sheet" onClick={(event) => event.stopPropagation()}>
            <Topbar centered className="topbar-timeline create-post-header">
              <div className="topbar-slot">
                <IconButton
                  type="button"
                  tone="topbar"
                  aria-label={step === 0 ? "Cerrar" : "Atras"}
                  onClick={() => {
                    if (step === 0) {
                      onClose();
                      return;
                    }
                    setStep(0);
                  }}
                >
                  <UiIcon name={step === 0 ? "close" : "arrow-left"} className="ui-icon" />
                </IconButton>
              </div>
              <h2 className="title title-upper topbar-title-center topbar-brand-title create-post-title">{step === 0 ? "NUEVO POST" : "DETALLES"}</h2>
              <div className="topbar-slot topbar-slot-end">
                {step === 0 ? (
                  <IconButton type="button" tone="topbar" aria-label="Siguiente" onClick={() => setStep(1)} disabled={!imageFile}>
                    <UiIcon name="arrow-right" className="ui-icon" />
                  </IconButton>
                ) : (
                  <Button variant="text" type="button" className="text-button create-post-publish" onClick={onPublish} disabled={!text.trim() && !imageFile}>
                    PUBLICAR
                  </Button>
                )}
              </div>
            </Topbar>
            <div className={`create-post-body create-post-flow ${step === 0 ? "create-post-step-0" : "create-post-step-1"}`}>
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
              <Input
                ref={cameraInputRef}
                type="file"
                accept="image/*"
                capture="environment"
                className="file-input-hidden"
                onChange={(event) => {
                  onAppendFiles(Array.from(event.target.files ?? []));
                  event.currentTarget.value = "";
                }}
              />
              {step === 0 ? (
                <>
                  <Button variant="plain" type="button" className="create-post-image-stage" onClick={() => imageInputRef.current?.click()} aria-label="Seleccionar imagen">
                    {imagePreviewUrl ? (
                      <img className="create-post-preview" src={imagePreviewUrl} alt="Previsualizacion" loading="lazy" />
                    ) : (
                      <span className="create-post-image-placeholder">Selecciona una foto</span>
                    )}
                  </Button>
                  {!isDesktopComposer ? (
                    <>
                      <div className="create-post-source-row">
                        <strong>Galeria</strong>
                        <div className="create-post-source-actions">
                          {imagePreviewUrl ? (
                          <Button variant="text" type="button" className="text-button create-post-secondary" onClick={onRemoveSelectedImage}>
                            Quitar
                          </Button>
                        ) : null}
                          <IconButton type="button" tone="topbar" className="create-post-camera" onClick={() => cameraInputRef.current?.click()} aria-label="Abrir camara">
                            <UiIcon name="camera" className="ui-icon" />
                          </IconButton>
                        </div>
                      </div>
                      <div className="create-post-gallery-grid" role="list" aria-label="Galeria">
                        {galleryItems.length ? (
                          galleryItems.map((item) => {
                            const isSelected = item.id === selectedImageId;
                            return (
                              <Button variant="plain" key={item.id} type="button" role="listitem" className={`create-post-gallery-item ${isSelected ? "is-selected" : ""}`} onClick={() => onSelectGalleryItem(item.id)}>
                                <img src={item.previewUrl} alt="Miniatura galeria" loading="lazy" />
                                {isSelected ? <UiIcon name="check-circle-filled" className="create-post-gallery-check" /> : null}
                              </Button>
                            );
                          })
                        ) : (
                          <div className="create-post-gallery-empty" role="listitem">No hay imagenes para mostrar</div>
                        )}
                      </div>
                    </>
                  ) : null}
                </>
              ) : (
                <>
                  {activeUser ? (
                    <div className="create-post-user-row">
                      {activeUser.avatar_url ? <img src={activeUser.avatar_url} alt={activeUser.username} loading="lazy" /> : <div className="create-post-user-fallback">{activeUser.username.slice(0, 1).toUpperCase()}</div>}
                      <div>
                        <p>{activeUser.full_name}</p>
                        <span>@{activeUser.username}</span>
                      </div>
                    </div>
                  ) : null}
                  <div className="create-post-composer-card sheet-input-shell">
                    <Textarea id="new-post-text" className="search-wide sheet-input create-post-textarea" placeholder="¿Qué estás pensando?" rows={4} value={text} onChange={(event) => setText(event.target.value)} />
                    {showEmojiPanel ? (
                      <div className="create-post-inline-panel">
                        {["☕", "😍", "🔥", "👏", "🤎", "✨"].map((emoji) => (
                          <Button variant="plain" key={emoji} type="button" className="emoji-chip" onClick={() => setText(`${text}${emoji}`)}>
                            {emoji}
                          </Button>
                        ))}
                      </div>
                    ) : mentionSuggestions.length && !text.trim().endsWith("@") ? (
                      <div className="create-post-inline-panel create-post-mention-suggestions">
                        {mentionSuggestions.map((user) => (
                          <Button variant="plain"
                            key={user.id}
                            type="button"
                            className="mention-chip"
                            onClick={() => {
                              const parts = text.split(/\s+/);
                              parts[parts.length - 1] = `@${user.username}`;
                              setText(`${parts.join(" ")} `);
                            }}
                          >
                            {user.avatar_url ? <img className="mention-chip-avatar" src={user.avatar_url} alt={user.username} loading="lazy" /> : <span className="mention-chip-fallback">{user.username.slice(0, 1).toUpperCase()}</span>}
                            <span>@{user.username}</span>
                          </Button>
                        ))}
                      </div>
                    ) : null}
                    <div className="create-post-composer-tools">
                      <IconButton type="button" tone="default" onClick={() => { setText(`${text}@`); setShowEmojiPanel(false); }} aria-label="Mencionar">
                        <UiIcon name="at" className="ui-icon" />
                      </IconButton>
                      <IconButton type="button" tone="default" className={showEmojiPanel ? "is-active" : ""} onClick={() => setShowEmojiPanel(!showEmojiPanel)} aria-label="Emojis">
                        <UiIcon name="smile" className="ui-icon" />
                      </IconButton>
                    </div>
                  </div>
                  <Button variant="plain" type="button" className="create-post-coffee-row" onClick={onOpenCoffeePicker}>
                    <UiIcon name="coffee" className="ui-icon" />
                    <span>Anadir cafe</span>
                    <strong>{selectedCoffee ? selectedCoffee.nombre : "Seleccionar cafe"}</strong>
                    <UiIcon name="chevron-right" className="ui-icon" />
                  </Button>
                  {imagePreviewUrl ? (
                    <div className="create-post-image-detail-wrap">
                      <img className="create-post-preview create-post-preview-detail" src={imagePreviewUrl} alt="Previsualizacion" loading="lazy" />
                      <Button variant="plain" type="button" className="create-post-image-remove" onClick={onRemoveSelectedImage} aria-label="Quitar foto">
                        <UiIcon name="close" className="ui-icon" />
                      </Button>
                    </div>
                  ) : (
                    <Button variant="plain" type="button" className="create-post-add-photo-row" onClick={() => imageInputRef.current?.click()}>
                      <UiIcon name="camera" className="ui-icon" />
                      <span>Anadir foto</span>
                      <UiIcon name="chevron-right" className="ui-icon" />
                    </Button>
                  )}
                </>
              )}
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showCoffeeSheet ? (
        <SheetOverlay className="create-post-coffee-overlay" role="dialog" aria-modal="true" aria-label="Seleccionar cafe" onDismiss={onCloseCoffeeSheet} onClick={onCloseCoffeeSheet}>
          <SheetCard className="create-post-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">SELECCIONAR CAFE</strong>
            </header>
            <div className="create-post-coffee-body">
              <div className={`search-row-with-cancel ${showCoffeeSearchCancel ? "has-cancel" : ""}`.trim()}>
                <Input
                  variant="search"
                  className="search-wide"
                  placeholder="Buscar cafe"
                  value={coffeeQuery}
                  onChange={(event) => setCoffeeQuery(event.target.value)}
                  onFocus={() => setCoffeeSearchFocus(true)}
                  onBlur={() => setCoffeeSearchFocus(false)}
                  aria-label="Buscar cafe"
                />
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
                      {coffee.image_url ? <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" /> : <span className="create-post-coffee-fallback">{coffee.nombre.slice(0, 1).toUpperCase()}</span>}
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




