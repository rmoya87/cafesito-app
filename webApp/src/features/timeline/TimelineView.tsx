import { Fragment, type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { CTA, EMPTY, ERROR } from "../../core/emptyErrorStrings";
import type { CoffeeRow, TimelineCard, UserRow } from "../../types";
import { MentionText } from "../../ui/MentionText";
import { UiIcon } from "../../ui/iconography";
import { Button, IconButton, Input, SheetCard, SheetHandle, SheetOverlay, Textarea } from "../../ui/components";

function SuggestionAvatar({
  avatarUrl,
  username
}: {
  avatarUrl: string | null | undefined;
  username: string;
}) {
  const [loadFailed, setLoadFailed] = useState(false);
  if (!avatarUrl || loadFailed) {
    return <div className="avatar mini-avatar-fallback">{username.slice(0, 2).toUpperCase()}</div>;
  }
  return (
    <img
      className="mini-avatar"
      src={avatarUrl}
      alt={username}
      loading="lazy"
      decoding="async"
      referrerPolicy="no-referrer"
      crossOrigin="anonymous"
      onError={() => setLoadFailed(true)}
    />
  );
}

export function TimelineView({
  mode,
  cards,
  recommendations,
  suggestions,
  suggestionIndices,
  followingIds,
  followerCounts,
  loading,
  errorMessage,
  refreshing,
  activeUserId,
  onOpenComments,
  onToggleLike,
  onToggleFollow,
  onEditPost,
  onDeletePost,
  onRefresh,
  onMentionClick,
  resolveMentionUser,
  onOpenUserProfile,
  onOpenCoffee,
  onOpenCreatePost,
  sidePanel
}: {
  mode: "mobile" | "desktop";
  cards: TimelineCard[];
  recommendations: CoffeeRow[];
  suggestions: UserRow[];
  suggestionIndices: number[];
  followingIds: Set<number>;
  followerCounts: Map<number, number>;
  loading: boolean;
  errorMessage: string | null;
  refreshing: boolean;
  activeUserId: number | null;
  onOpenComments: (postId: string) => void;
  onToggleLike: (postId: string) => void;
  onToggleFollow: (userId: number) => void;
  onEditPost: (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => Promise<void>;
  onDeletePost: (postId: string) => Promise<void>;
  onRefresh: () => Promise<void>;
  onMentionClick: (username: string) => void;
  resolveMentionUser?: (username: string) => { username: string; avatarUrl?: string | null } | null | undefined;
  onOpenUserProfile: (userId: number) => void;
  onOpenCoffee: (coffeeId: string) => void;
  onOpenCreatePost?: () => void;
  sidePanel?: ReactNode;
}) {
  const [menuPostId, setMenuPostId] = useState<string | null>(null);
  const [deletePostConfirmId, setDeletePostConfirmId] = useState<string | null>(null);
  const [deletingPost, setDeletingPost] = useState(false);
  const [editingPostId, setEditingPostId] = useState<string | null>(null);
  const [editingText, setEditingText] = useState("");
  const [editingImageUrl, setEditingImageUrl] = useState("");
  const [editingImageFile, setEditingImageFile] = useState<File | null>(null);
  const [editingImagePreviewUrl, setEditingImagePreviewUrl] = useState("");
  const [likeBurstPostId, setLikeBurstPostId] = useState<string | null>(null);
  const [dismissingSuggestionIds, setDismissingSuggestionIds] = useState<Set<number>>(new Set());
  const [pendingSuggestionFollowIds, setPendingSuggestionFollowIds] = useState<Set<number>>(new Set());
  const [failedAvatarUrls, setFailedAvatarUrls] = useState<Set<string>>(new Set());
  const [pullDistance, setPullDistance] = useState(0);
  const editImageInputRef = useRef<HTMLInputElement | null>(null);
  const touchStartY = useRef<number | null>(null);
  const pullActive = useRef(false);
  const likeBurstTimerRef = useRef<number | null>(null);
  const visibleCards = cards;
  const isDesktopTimeline = mode === "desktop";
  const activeMenuPost = useMemo(
    () => visibleCards.find((card) => card.id === menuPostId) ?? null,
    [menuPostId, visibleCards]
  );

  const closeEditModal = useCallback(() => {
    if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
    setEditingPostId(null);
    setEditingText("");
    setEditingImageUrl("");
    setEditingImageFile(null);
    setEditingImagePreviewUrl("");
  }, [editingImagePreviewUrl]);

  const isMobileLike = typeof window !== "undefined" ? window.innerWidth < 900 : false;
  const pullProgress = Math.min(1, pullDistance / 84);

  const handleTouchStart = (event: React.TouchEvent<HTMLElement>) => {
    if (!isMobileLike) return;
    if (window.scrollY > 0) return;
    touchStartY.current = event.touches[0]?.clientY ?? null;
    pullActive.current = true;
  };

  const handleTouchMove = (event: React.TouchEvent<HTMLElement>) => {
    if (!pullActive.current) return;
    const start = touchStartY.current;
    const current = event.touches[0]?.clientY;
    if (start == null || current == null) return;
    const delta = Math.max(0, current - start);
    if (delta <= 0) return;
    setPullDistance(Math.min(120, delta * 0.55));
  };

  const handleTouchEnd = () => {
    if (!pullActive.current) return;
    pullActive.current = false;
    touchStartY.current = null;
    const shouldRefresh = pullDistance >= 76 && !refreshing;
    setPullDistance(0);
    if (shouldRefresh) void onRefresh();
  };

  useEffect(() => {
    const onEsc = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setMenuPostId(null);
      if (editingPostId) closeEditModal();
    };
    window.addEventListener("keydown", onEsc);
    return () => window.removeEventListener("keydown", onEsc);
  }, [closeEditModal, editingPostId]);

  useEffect(() => {
    if (!editingPostId) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [editingPostId]);

  useEffect(() => {
    return () => {
      if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
    };
  }, [editingImagePreviewUrl]);

  useEffect(() => {
    return () => {
      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
    };
  }, []);

  if (loading) {
    return (
      <ul className="feed-list">
        {[1, 2, 3, 4, 5].map((item) => (
          <li key={item} className="feed-card shimmer-card" />
        ))}
      </ul>
    );
  }

  if (errorMessage) {
    return (
      <article className="timeline-empty timeline-error" role="alert">
        <h3>{ERROR.LOAD_DATA}</h3>
        <p>{errorMessage}</p>
        <Button variant="secondary" className="action-button" onClick={() => void onRefresh()} aria-label={`${ERROR.RETRY} carga`}>
          {ERROR.RETRY}
        </Button>
      </article>
    );
  }

  const recommendationSection = recommendations.length ? (
    <section className="suggestion-strip">
      <p className="section-title">Recomendados para tu paladar</p>
      <div className="horizontal-cards">
        {recommendations.map((coffee) => (
          <Button
            key={coffee.id}
            variant="plain"
            className="mini-card mini-coffee-card mini-coffee-link"
            onClick={() => onOpenCoffee(coffee.id)}
          >
            {coffee.image_url ? <img className="mini-cover" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" /> : null}
            <p className="coffee-origin">{coffee.pais_origen ?? "Origen"}</p>
            <p className="feed-user">{coffee.nombre}</p>
            <p className="coffee-sub">{(coffee.marca || "").toUpperCase()}</p>
          </Button>
        ))}
      </div>
    </section>
  ) : null;

  const userSuggestionsSection = suggestions.length ? (
    <section className="suggestion-strip">
      <p className="section-title">Personas que podrias seguir</p>
      <div className="horizontal-cards">
        {suggestions.map((user) => {
          const isDismissing = dismissingSuggestionIds.has(user.id);
          const isPendingFollow = pendingSuggestionFollowIds.has(user.id);
          return (
            <article key={user.id} className={`mini-card mini-user-card suggestion-user-card ${isDismissing ? "is-removing" : ""}`.trim()}>
              <Button variant="plain" type="button" className="mini-user-link" onClick={() => onOpenUserProfile(user.id)}>
                <SuggestionAvatar avatarUrl={user.avatar_url} username={user.username} />
                <div className="mini-user-copy">
                  <p className="feed-user">{user.full_name}</p>
                  <p className="feed-meta suggestion-subtitle">{followerCounts.get(user.id) ?? 0} seguidores</p>
                </div>
              </Button>
              <Button variant="plain"
                className={`action-button search-users-follow suggestion-follow-btn ${followingIds.has(user.id) ? "action-button-following" : ""}`}
                disabled={isPendingFollow}
                onClick={async () => {
                  if (isPendingFollow) return;
                  setPendingSuggestionFollowIds((prev) => new Set(prev).add(user.id));
                  setDismissingSuggestionIds((prev) => new Set(prev).add(user.id));
                  await new Promise((resolve) => window.setTimeout(resolve, 190));
                  await onToggleFollow(user.id);
                  setPendingSuggestionFollowIds((prev) => {
                    const next = new Set(prev);
                    next.delete(user.id);
                    return next;
                  });
                  setDismissingSuggestionIds((prev) => {
                    const next = new Set(prev);
                    next.delete(user.id);
                    return next;
                  });
                }}
              >
                {followingIds.has(user.id) ? "Siguiendo" : "Seguir"}
              </Button>
            </article>
          );
        })}
      </div>
    </section>
  ) : null;

  const desktopSideContent = sidePanel ? (
    <aside className="timeline-side-column">{sidePanel}</aside>
  ) : (userSuggestionsSection || recommendationSection) ? (
    <aside className="timeline-side-column">
      {userSuggestionsSection}
      {recommendationSection}
    </aside>
  ) : null;

  if (!cards.length) {
    const emptyTitle = EMPTY.TIMELINE_TITLE;
    const emptySubtitle = EMPTY.TIMELINE_SUBTITLE;
    const ctaLabel = CTA.PUBLISH_FIRST;
    if (isDesktopTimeline) {
      return (
        <div className="timeline-shell timeline-shell-desktop">
          <div className="timeline-desktop-columns">
            <div className="timeline-main-column">
              <article className="timeline-empty">
                <h3>{emptyTitle}</h3>
                <p>{emptySubtitle}</p>
                {onOpenCreatePost ? (
                  <Button variant="primary" className="action-button" onClick={onOpenCreatePost} aria-label={ctaLabel}>
                    {ctaLabel}
                  </Button>
                ) : (
                  <Button variant="primary" className="action-button" aria-label={ctaLabel}>
                    {ctaLabel}
                  </Button>
                )}
              </article>
            </div>
            {desktopSideContent}
          </div>
        </div>
      );
    }
    return (
      <>
        <article className="timeline-empty">
          <h3>{emptyTitle}</h3>
          <p>{emptySubtitle}</p>
          {onOpenCreatePost ? (
            <Button variant="primary" className="action-button" onClick={onOpenCreatePost} aria-label={ctaLabel}>
              {ctaLabel}
            </Button>
          ) : (
            <Button variant="primary" className="action-button" aria-label={ctaLabel}>
              {ctaLabel}
            </Button>
          )}
        </article>
        {userSuggestionsSection}
        {recommendationSection}
      </>
    );
  }

  return (
    <div
      className={`timeline-shell ${isDesktopTimeline ? "timeline-shell-desktop" : ""}`.trim()}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      <div
        className={`pull-indicator ${pullDistance > 2 ? "is-visible" : ""}`}
        style={{ ["--pull-distance" as string]: `${pullDistance}px`, ["--pull-progress" as string]: `${pullProgress}` }}
        aria-hidden="true"
      >
        <span className={`pull-dot ${pullProgress >= 1 ? "is-ready" : ""}`} />
      </div>

      {visibleCards.length ? (
        <div className="timeline-desktop-columns">
          <div className="timeline-main-column">
            <ul className="feed-list">
              {visibleCards.map((card, index) => (
                <Fragment key={card.id}>
                  {!isDesktopTimeline && recommendationSection && suggestionIndices[0] != null && index === suggestionIndices[0] ? (
                    <li className="feed-inline-item">{recommendationSection}</li>
                  ) : null}
                  {!isDesktopTimeline && userSuggestionsSection && suggestionIndices[1] != null && index === suggestionIndices[1] ? (
                    <li className="feed-inline-item">{userSuggestionsSection}</li>
                  ) : null}
                  <li className="feed-card feed-card-premium feed-entry" style={{ ["--feed-index" as string]: index }}>
                    <article>
              <header className="feed-head">
                <Button variant="plain"
                  type="button"
                  className="feed-user-link"
                  onClick={() => onOpenUserProfile(card.userId)}
                >
                  {card.avatarUrl && !failedAvatarUrls.has(card.avatarUrl) ? (
                    <img
                      className="avatar avatar-photo"
                      src={card.avatarUrl}
                      alt={card.username}
                      loading="lazy"
                      decoding="async"
                      referrerPolicy="no-referrer"
                      crossOrigin="anonymous"
                      onError={() => setFailedAvatarUrls((prev) => new Set(prev).add(card.avatarUrl!))}
                    />
                  ) : (
                    <div className="avatar" aria-hidden="true">
                      {card.userName
                        .split(" ")
                        .map((part) => part[0])
                        .join("")
                        .slice(0, 2)
                        .toUpperCase()}
                    </div>
                  )}
                  <div>
                    <p className="feed-user">{card.userName}</p>
                    <div className="feed-meta-row" style={{ display: "flex", alignItems: "center", gap: "8px" }}><p className="feed-meta">{card.minsAgoLabel.toUpperCase()}</p>{card.rating ? (<span className="feed-card-rating" style={{ display: "inline-flex", alignItems: "center", gap: "2px", color: "var(--caramel-soft)", fontSize: "var(--font-size-xs)", fontWeight: "bold" }}><UiIcon name="star" className="ui-icon" />{card.rating.toFixed(1)}</span>) : null}</div>
                  </div>
                </Button>
                {activeUserId === card.userId ? (
                  <IconButton
                    tone="default"
                    className="post-menu-trigger"
                    onClick={() => setMenuPostId(card.id)}
                    aria-label="Opciones"
                  >
                    <UiIcon name="more" className="ui-icon" />
                  </IconButton>
                ) : null}
              </header>

              {card.text ? (
                <p className="feed-text">
                  <MentionText text={card.text} onMentionClick={onMentionClick} resolveMentionUser={resolveMentionUser} />
                </p>
              ) : null}

              {card.imageUrl ? <img className={`feed-image ${card.text ? "" : "feed-image-no-text"}`.trim()} src={card.imageUrl} alt="Publicacion" loading="lazy" decoding="async" /> : null}

              {card.coffeeTagName ? (
                <Button variant="plain" type="button" className="coffee-tag-card" onClick={() => card.coffeeId && onOpenCoffee(card.coffeeId)} disabled={!card.coffeeId}>
                  <div className="coffee-tag-card-media">
                    {card.coffeeImageUrl ? (
                      <img className="coffee-tag-image" src={card.coffeeImageUrl} alt={card.coffeeTagName} loading="lazy" decoding="async" />
                    ) : (
                      <div className="coffee-tag-image coffee-tag-image-fallback" aria-hidden="true">
                        <UiIcon name="coffee" className="ui-icon" />
                      </div>
                    )}
                  </div>
                  <div className="coffee-tag-copy">
                    <p className="coffee-origin">CAFE ETIQUETADO</p>
                    <p className="coffee-tag-name">{card.coffeeTagName}</p>
                    {card.coffeeTagBrand ? <p className="coffee-tag-brand">{card.coffeeTagBrand.toUpperCase()}</p> : null}
                  </div>
                  <UiIcon name="chevron-right" className="ui-icon" />
                </Button>
              ) : null}

              <footer className="feed-stats">
                <Button variant="plain"
                  type="button"
                  className={`inline-action action-like ${card.likedByActiveUser ? "is-liked" : ""} ${likeBurstPostId === card.id ? "is-bursting" : ""}`}
                  onClick={() => {
                    if (!card.likedByActiveUser) {
                      setLikeBurstPostId(card.id);
                      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
                      likeBurstTimerRef.current = window.setTimeout(() => {
                        setLikeBurstPostId(null);
                        likeBurstTimerRef.current = null;
                      }, 520);
                    }
                    onToggleLike(card.id);
                  }}
                >
                  <span className="like-icon-wrap">
                    <UiIcon name={card.likedByActiveUser ? "coffee-filled" : "coffee"} className="ui-icon" />
                    <span className="like-burst" aria-hidden="true">
                      <span />
                      <span />
                      <span />
                      <span />
                      <span />
                      <span />
                    </span>
                  </span>
                  {card.likes > 0 ? <span>{card.likes}</span> : null}
                </Button>
                <Button variant="plain" type="button" className="inline-action" onClick={() => onOpenComments(card.id)}>
                  <UiIcon name="chat" className="ui-icon" />
                  {card.comments > 0 ? <span>{card.comments}</span> : null}
                </Button>
              </footer>
                    </article>
                  </li>
                </Fragment>
              ))}
            </ul>
          </div>
          {isDesktopTimeline ? desktopSideContent : null}
        </div>
      ) : (
        <article className="timeline-empty">
          <h3>{EMPTY.TIMELINE_NO_POSTS}</h3>
          <p>{EMPTY.TIMELINE_NO_POSTS_SUB}</p>
          {onOpenCreatePost ? (
            <Button variant="primary" className="action-button" onClick={onOpenCreatePost} aria-label={CTA.PUBLISH_FIRST}>
              {CTA.PUBLISH_FIRST}
            </Button>
          ) : null}
        </article>
      )}
      {activeMenuPost && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Opciones publicación" onDismiss={() => setMenuPostId(null)} onClick={() => setMenuPostId(null)}>
              <SheetCard className="diary-sheet diary-sheet-pantry-options profile-post-menu-sheet" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-sheet-list">
                  <Button variant="plain"
                    className="diary-sheet-action diary-sheet-action-pantry"
                    onClick={() => {
                      setDeletePostConfirmId(activeMenuPost.id);
                      setMenuPostId(null);
                    }}
                  >
                    <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">delete</span>
                    <span>Borrar</span>
                    <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                  </Button>
                  <Button variant="plain"
                    className="diary-sheet-action diary-sheet-action-pantry"
                    onClick={() => {
                      setEditingPostId(activeMenuPost.id);
                      setEditingText(activeMenuPost.text);
                      const initialImage = activeMenuPost.imageUrl ?? "";
                      setEditingImageUrl(initialImage);
                      setEditingImageFile(null);
                      setEditingImagePreviewUrl(initialImage);
                      setMenuPostId(null);
                    }}
                  >
                    <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">edit</span>
                    <span>Editar</span>
                    <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                  </Button>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {deletePostConfirmId && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Eliminar publicación" onDismiss={() => setDeletePostConfirmId(null)} onClick={() => setDeletePostConfirmId(null)}>
              <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-delete-confirm-body">
                  <h2 className="diary-delete-confirm-title">Eliminar publicación</h2>
                  <p className="diary-delete-confirm-text">
                    ¿Estás seguro de eliminar esta publicación? Esta acción no se puede deshacer.
                  </p>
                  <div className="diary-delete-confirm-actions">
                    <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => setDeletePostConfirmId(null)} disabled={deletingPost}>
                      Cancelar
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-submit"
                      disabled={deletingPost}
                      onClick={async () => {
                        if (deletingPost) return;
                        setDeletingPost(true);
                        try {
                          await onDeletePost(deletePostConfirmId);
                          setDeletePostConfirmId(null);
                        } finally {
                          setDeletingPost(false);
                        }
                      }}
                    >
                      {deletingPost ? "Eliminando..." : "Eliminar"}
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {editingPostId && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Editar publicacion"
              onDismiss={closeEditModal}
              onClick={closeEditModal}
            >
              <SheetCard onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="create-post-body edit-post-sheet">
              <h3 className="edit-post-title">Editar</h3>
              <Input
                ref={editImageInputRef}
                type="file"
                accept="image/*"
                className="file-input-hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  if (!file) return;
                  if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
                  const preview = URL.createObjectURL(file);
                  setEditingImageFile(file);
                  setEditingImagePreviewUrl(preview);
                }}
              />
              <Button variant="plain"
                type="button"
                className="edit-image-picker"
                onClick={() => editImageInputRef.current?.click()}
              >
                {editingImagePreviewUrl.trim() ? (
                  <img className="create-post-preview" src={editingImagePreviewUrl.trim()} alt="Previsualizacion" loading="lazy" decoding="async" />
                ) : (
                  <span className="edit-image-placeholder">Seleccionar imagen</span>
                )}
              </Button>
              <Textarea
                className="search-wide sheet-input"
                placeholder="Descripción"
                value={editingText}
                rows={4}
                onChange={(event) => setEditingText(event.target.value)}
              />
              <div className="create-post-actions edit-post-actions-native">
                <Button variant="ghost"
                  className="action-button action-button-ghost edit-post-cancel"
                  onClick={closeEditModal}
                >
                  CANCELAR
                </Button>
                <Button variant="primary"
                  className="action-button edit-post-save"
                  disabled={!editingText.trim() && !editingImageUrl.trim() && !editingImageFile}
                  onClick={async () => {
                    await onEditPost(editingPostId, editingText.trim(), editingImageUrl.trim(), editingImageFile);
                    closeEditModal();
                  }}
                >
                  GUARDAR
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>,
            document.body
          )
        : null}
    </div>
  );
}





