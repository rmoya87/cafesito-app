import type { MutableRefObject } from "react";
import { toRelativeMinutes } from "../../core/time";
import type { CommentRow, UserRow } from "../../types";
import { MentionText } from "../../ui/MentionText";
import { UiIcon } from "../../ui/iconography";
import { Button, ComposerInputShell, IconButton, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";

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
  resolveMentionUser,
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
  resolveMentionUser?: (username: string) => { username: string; avatarUrl?: string | null } | null | undefined;
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
  const canSubmitComment = commentDraft.trim().length > 0 || Boolean(commentImagePreviewUrl);
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
                        <img className="comment-avatar" src={user.avatar_url} alt={user.username} loading="lazy" decoding="async" referrerPolicy="no-referrer" crossOrigin="anonymous" />
                      ) : (
                        <div className="comment-avatar comment-avatar-fallback" aria-hidden="true">
                          {(user?.username ?? "us").slice(0, 2).toUpperCase()}
                        </div>
                      )}
                      <div className="comment-copy">
                        {user?.full_name ? <p className="comment-author-name">{user.full_name}</p> : null}
                        <p className="comment-author comment-time">{toRelativeMinutes(row.timestamp ?? 0).toUpperCase()}</p>
                      </div>
                      {isOwnComment ? (
                        <IconButton type="button" tone="default" className="post-menu-trigger" onClick={() => onOpenMenu(row.id)}>
                          <UiIcon name="more" className="ui-icon" />
                        </IconButton>
                      ) : null}
                    </div>
                    <p className="sheet-item-text">
                      <MentionText text={row.text} onMentionClick={onMentionNavigate} resolveMentionUser={resolveMentionUser} />
                    </p>
                  </li>
                );
              })
            : <li className="sheet-item comments-empty">No hay comentarios todavía</li>}
        </ul>

        {activeMenuRow ? (
          <SheetOverlay className="comment-action-overlay" onDismiss={onCloseMenu} onClick={onCloseMenu}>
            <SheetCard className="diary-sheet diary-sheet-pantry-options comment-options-sheet" onClick={(event) => event.stopPropagation()}>
              <SheetHandle aria-hidden="true" />
              <div className="diary-sheet-list">
                <Button variant="plain"
                  type="button"
                  className="diary-sheet-action diary-sheet-action-pantry"
                  onClick={() => onMenuEdit(activeMenuRow)}
                >
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">edit</span>
                  <span>Editar</span>
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                </Button>
                <Button variant="plain"
                  type="button"
                  className="diary-sheet-action diary-sheet-action-pantry"
                  onClick={() => onMenuDelete(activeMenuRow)}
                >
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">delete</span>
                  <span>Borrar</span>
                  <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
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
            <ComposerInputShell
              value={commentDraft}
              onChange={(value) => {
                setCommentDraft(value);
                if (value.endsWith("@")) setShowEmojiPanel(false);
              }}
              placeholder="Añade un comentario..."
              rows={2}
              showEmojiPanel={showEmojiPanel}
              emojis={emojis}
              onEmojiSelect={(emoji) => setCommentDraft(`${commentDraft}${emoji}`)}
              mentionSuggestions={mentionSuggestions}
              onInsertMention={(username) => {
                const parts = commentDraft.split(/\s+/);
                parts[parts.length - 1] = `@${username}`;
                setCommentDraft(`${parts.join(" ")} `);
                setShowEmojiPanel(false);
              }}
              resolveMentionUser={resolveMentionUser}
              toolsContent={
                <>
                  <IconButton type="button" tone="default" onClick={() => commentImageInputRef.current?.click()} aria-label="Agregar foto">
                    <UiIcon name="camera" className="ui-icon" />
                  </IconButton>
                  <IconButton type="button" tone="default" onClick={() => setCommentDraft(`${commentDraft}@`)}>
                    <UiIcon name="at" className="ui-icon" />
                  </IconButton>
                  <IconButton type="button" tone="default" className={showEmojiPanel ? "is-active" : ""} onClick={() => setShowEmojiPanel(!showEmojiPanel)}>
                    <UiIcon name="smile" className="ui-icon" />
                  </IconButton>
                </>
              }
              actionContent={
                <Button
                  variant="plain"
                  type="button"
                  className="send-button"
                  disabled={!canSubmitComment}
                  onClick={editingCommentId ? onUpdateComment : onAddComment}
                >
                  <UiIcon name="send" className="ui-icon" />
                  {editingCommentId ? "Guardar" : "Enviar"}
                </Button>
              }
              extraContent={
                commentImagePreviewUrl ? (
                  <div className="comment-image-thumb-wrap">
                    {commentImagePreviewError ? (
                      <div className="comment-image-thumb-fallback">{commentImageName || "Imagen seleccionada"}</div>
                    ) : (
                      <img
                        className="comment-image-thumb"
                        src={commentImagePreviewUrl}
                        alt="Miniatura comentario"
                        loading="lazy"
                        decoding="async"
                        onError={() => setCommentImagePreviewError(true)}
                      />
                    )}
                    <Button variant="plain" className="comment-image-remove" onClick={onRemoveImage} aria-label="Quitar imagen">
                      x
                    </Button>
                  </div>
                ) : null
              }
            />
          </div>
        </div>
      </SheetCard>
    </SheetOverlay>
  );
}
