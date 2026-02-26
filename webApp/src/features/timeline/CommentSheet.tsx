import type { MutableRefObject } from "react";
import type { CommentRow, UserRow } from "../../types";
import { MentionText } from "../../ui/MentionText";
import { UiIcon } from "../../ui/iconography";
import { Button, IconButton, Input, SheetCard, SheetHandle, SheetOverlay, Textarea } from "../../ui/components";

export function CommentSheet({
  open,
  rows,
  usersById,
  activeUserId,
  highlightedCommentId,
  onClose,
  onOpenMenu,
  activeMenuRow,
  onCloseMenu,
  onMenuEdit,
  onMenuDelete,
  editingCommentId,
  onCancelEdit,
  commentDraft,
  setCommentDraft,
  emojis,
  showEmojiPanel,
  setShowEmojiPanel,
  mentionSuggestions,
  onMentionNavigate,
  commentImageInputRef,
  commentListRef,
  onPickImage,
  onAddComment,
  onUpdateComment,
  commentImagePreviewUrl,
  commentImagePreviewError,
  setCommentImagePreviewError,
  commentImageName,
  onRemoveImage
}: {
  open: boolean;
  rows: CommentRow[];
  usersById: Map<number, UserRow>;
  activeUserId: number | null;
  highlightedCommentId: number | null;
  onClose: () => void;
  onOpenMenu: (id: number) => void;
  activeMenuRow: CommentRow | null;
  onCloseMenu: () => void;
  onMenuEdit: (row: CommentRow) => void;
  onMenuDelete: (row: CommentRow) => void;
  editingCommentId: number | null;
  onCancelEdit: () => void;
  commentDraft: string;
  setCommentDraft: (value: string) => void;
  emojis: string[];
  showEmojiPanel: boolean;
  setShowEmojiPanel: (value: boolean) => void;
  mentionSuggestions: UserRow[];
  onMentionNavigate: (username: string) => void;
  commentImageInputRef: MutableRefObject<HTMLInputElement | null>;
  commentListRef?: MutableRefObject<HTMLUListElement | null>;
  onPickImage: (file: File) => void;
  onAddComment: () => void;
  onUpdateComment: () => void;
  commentImagePreviewUrl: string;
  commentImagePreviewError: boolean;
  setCommentImagePreviewError: (value: boolean) => void;
  commentImageName: string;
  onRemoveImage: () => void;
}) {
  if (!open) return null;
  return (
    <SheetOverlay role="dialog" aria-modal="true" aria-label="Comentarios" onDismiss={onClose} onClick={onClose}>
      <SheetCard onClick={(event) => event.stopPropagation()}>
        <SheetHandle aria-hidden="true" />
        <header className="sheet-header">
          <strong className="sheet-title">COMENTARIOS</strong>
        </header>
        <ul className="sheet-list comments-list" ref={commentListRef}>
          {rows.length
            ? rows.map((row) => {
                const user = usersById.get(row.user_id);
                const isOwnComment = activeUserId === row.user_id;
                return (
                  <li key={row.id} data-comment-id={row.id} className={`sheet-item ${highlightedCommentId === row.id ? "is-highlighted" : ""}`}>
                    <div className="sheet-item-head">
                      {user?.avatar_url ? (
                        <img className="comment-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                      ) : (
                        <div className="comment-avatar comment-avatar-fallback" aria-hidden="true">
                          {(user?.username ?? "us").slice(0, 2).toUpperCase()}
                        </div>
                      )}
                      <div className="comment-copy">
                        <p className="comment-author">@{user?.username ?? `user${row.user_id}`}</p>
                      </div>
                      {isOwnComment ? (
                        <IconButton type="button" tone="default" className="post-menu-trigger" onClick={() => onOpenMenu(row.id)}>
                          <UiIcon name="more" className="ui-icon" />
                        </IconButton>
                      ) : null}
                    </div>
                    <p className="sheet-item-text">
                      <MentionText text={row.text} onMentionClick={onMentionNavigate} />
                    </p>
                  </li>
                );
              })
            : <li className="sheet-item comments-empty">No hay comentarios todavia</li>}
        </ul>

        {activeMenuRow ? (
          <SheetOverlay className="comment-action-overlay" onDismiss={onCloseMenu} onClick={onCloseMenu}>
            <SheetCard className="comment-action-sheet" onClick={(event) => event.stopPropagation()}>
              <SheetHandle aria-hidden="true" />
              <div className="comment-action-list">
                <p className="comment-action-title">OPCIONES</p>
                <Button variant="plain" type="button" className="comment-action-button" onClick={() => onMenuEdit(activeMenuRow)}>
                  <UiIcon name="edit" className="ui-icon" />
                  <span>Editar</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </Button>
                <Button variant="plain" type="button" className="comment-action-button is-danger" onClick={() => onMenuDelete(activeMenuRow)}>
                  <UiIcon name="trash" className="ui-icon" />
                  <span>Borrar</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </Button>
              </div>
            </SheetCard>
          </SheetOverlay>
        ) : null}

        {editingCommentId ? (
          <div className="edit-comment-banner">
            <span>Editando comentario</span>
            <Button variant="text" type="button" className="text-button" onClick={onCancelEdit}>
              Cancelar
            </Button>
          </div>
        ) : null}

        <div className="sheet-composer">
          <div className="sheet-input-wrap">
            <Input
              ref={commentImageInputRef}
              type="file"
              accept="image/*"
              className="file-input-hidden"
              onChange={(event) => {
                const file = event.target.files?.[0] ?? null;
                if (!file) return;
                onPickImage(file);
                event.currentTarget.value = "";
              }}
            />
            <div className="sheet-input-shell">
              {showEmojiPanel ? (
                <div className="comment-inline-panel emoji-panel">
                  {emojis.map((emoji) => (
                    <Button key={emoji} variant="plain" className="emoji-chip" onClick={() => setCommentDraft(`${commentDraft}${emoji}`)}>
                      {emoji}
                    </Button>
                  ))}
                </div>
              ) : mentionSuggestions.length ? (
                <div className="comment-inline-panel mention-suggestions">
                  {mentionSuggestions.map((user) => (
                    <Button
                      key={user.id}
                      variant="plain"
                      className="mention-chip"
                      onClick={() => {
                        const parts = commentDraft.split(/\s+/);
                        parts[parts.length - 1] = `@${user.username}`;
                        setCommentDraft(`${parts.join(" ")} `);
                        setShowEmojiPanel(false);
                      }}
                    >
                      {user.avatar_url ? (
                        <img className="mention-chip-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                      ) : (
                        <span className="mention-chip-fallback">{user.username.slice(0, 1).toUpperCase()}</span>
                      )}
                      <span>@{user.username}</span>
                    </Button>
                  ))}
                </div>
              ) : null}
              <Textarea
                className="search-wide sheet-input"
                placeholder="Anade un comentario..."
                value={commentDraft}
                rows={2}
                onChange={(event) => {
                  setCommentDraft(event.target.value);
                  if (event.target.value.endsWith("@")) setShowEmojiPanel(false);
                }}
              />
              <div className="sheet-composer-bottom">
                <div className="sheet-composer-tools-inline">
                  <IconButton type="button" tone="default" onClick={() => commentImageInputRef.current?.click()} aria-label="Agregar foto">
                    <UiIcon name="camera" className="ui-icon" />
                  </IconButton>
                  <IconButton type="button" tone="default" onClick={() => setCommentDraft(`${commentDraft}@`)}>
                    <UiIcon name="at" className="ui-icon" />
                  </IconButton>
                  <IconButton type="button" tone="default" className={showEmojiPanel ? "is-active" : ""} onClick={() => setShowEmojiPanel(!showEmojiPanel)}>
                    <UiIcon name="smile" className="ui-icon" />
                  </IconButton>
                </div>
                <Button variant="plain" type="button" className="send-button" onClick={editingCommentId ? onUpdateComment : onAddComment}>
                  <UiIcon name="send" className="ui-icon" />
                  {editingCommentId ? "Guardar" : "Enviar"}
                </Button>
              </div>
              {commentImagePreviewUrl ? (
                <div className="comment-image-thumb-wrap">
                  {commentImagePreviewError ? (
                    <div className="comment-image-thumb-fallback">{commentImageName || "Imagen seleccionada"}</div>
                  ) : (
                    <img
                      className="comment-image-thumb"
                      src={commentImagePreviewUrl}
                      alt="Miniatura comentario"
                      loading="lazy"
                      onError={() => setCommentImagePreviewError(true)}
                    />
                  )}
                  <Button variant="plain" className="comment-image-remove" onClick={onRemoveImage} aria-label="Quitar imagen">
                    x
                  </Button>
                </div>
              ) : null}
            </div>
          </div>
        </div>
      </SheetCard>
    </SheetOverlay>
  );
}
