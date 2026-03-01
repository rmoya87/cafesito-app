import { type CSSProperties, useEffect, useMemo, useState } from "react";
import { toRelativeMinutes } from "../../core/time";
import type { CoffeeReviewRow, CoffeeRow, PantryItemRow, UserRow } from "../../types";
import { Button, IconButton, Input, SheetCard, SheetHandle, SheetHeader, SheetOverlay, Textarea } from "../../ui/components";
import { UiIcon, type IconName } from "../../ui/iconography";
export function CoffeeDetailView({
  coffee,
  reviews,
  currentUser,
  currentUserReview,
  avgRating,
  isFavorite,
  pantry,
  sensory,
  sensoryDraft,
  stockDraft,
  reviewDraftText,
  reviewDraftRating,
  reviewDraftImagePreviewUrl,
  onClose,
  onToggleFavorite,
  onReviewTextChange,
  onReviewRatingChange,
  onReviewImagePick,
  onSaveReview,
  onDeleteReview,
  canDeleteReview,
  onSensoryDraftChange,
  onSaveSensory,
  onStockDraftChange,
  onSaveStock,
  onOpenUserProfile,
  isGuest,
  onRequireAuth,
  fullPage,
  externalOpenStockSignal
}: {
  coffee: CoffeeRow;
  reviews: Array<CoffeeReviewRow & { user: UserRow | null }>;
  currentUser: UserRow | null;
  currentUserReview: (CoffeeReviewRow & { user: UserRow | null }) | null;
  avgRating: number;
  isFavorite: boolean;
  pantry: PantryItemRow | null;
  sensory: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
  sensoryDraft: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
  stockDraft: { total: number; remaining: number };
  reviewDraftText: string;
  reviewDraftRating: number;
  reviewDraftImagePreviewUrl: string;
  onClose: () => void;
  onToggleFavorite: () => void;
  onReviewTextChange: (value: string) => void;
  onReviewRatingChange: (value: number) => void;
  onReviewImagePick: (file: File | null, previewUrl: string) => void;
  onSaveReview: () => Promise<void>;
  onDeleteReview: () => Promise<void>;
  canDeleteReview: boolean;
  onSensoryDraftChange: (value: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number }) => void;
  onSaveSensory: () => Promise<void>;
  onStockDraftChange: (value: { total: number; remaining: number }) => void;
  onSaveStock: () => Promise<void>;
  onOpenUserProfile: (userId: number) => void;
  isGuest: boolean;
  onRequireAuth: () => void;
  fullPage: boolean;
  externalOpenStockSignal: number;
}) {
  const [showSensorySheet, setShowSensorySheet] = useState(false);
  const [showStockSheet, setShowStockSheet] = useState(false);
  const [showReviewSheet, setShowReviewSheet] = useState(false);
  const [reviewSheetError, setReviewSheetError] = useState<string | null>(null);
  const [stockSheetError, setStockSheetError] = useState<string | null>(null);
  const [sensorySheetError, setSensorySheetError] = useState<string | null>(null);
  const [savingSensory, setSavingSensory] = useState(false);
  const [savingStock, setSavingStock] = useState(false);
  const [savingReview, setSavingReview] = useState(false);
  const [deletingReview, setDeletingReview] = useState(false);

  const sensoryKeys: Array<keyof typeof sensoryDraft> = ["aroma", "sabor", "cuerpo", "acidez", "dulzura"];
  const sensoryLabels: Record<keyof typeof sensoryDraft, string> = {
    aroma: "Aroma",
    sabor: "Sabor",
    cuerpo: "Cuerpo",
    acidez: "Acidez",
    dulzura: "Dulzura"
  };
  const techRowsBase: Array<{ icon: IconName; label: string; value: string }> = [
    { icon: "origin", label: "Pais", value: coffee.pais_origen ?? "" },
    { icon: "specialty", label: "Especialidad", value: coffee.especialidad ?? "" },
    { icon: "variety", label: "Variedad", value: coffee.variedad_tipo ?? "" },
    { icon: "roast", label: "Tueste", value: coffee.tueste ?? "" },
    { icon: "process", label: "Proceso", value: coffee.proceso ?? "" },
    { icon: "grind", label: "Molienda", value: coffee.molienda_recomendada ?? "" }
  ];
  const techRows = techRowsBase.filter((row) => row.value.trim().length > 0);
  const otherReviews = reviews.filter((review) => !currentUser || review.user_id !== currentUser.id);
  const stockTotal = Number.isFinite(stockDraft.total) ? Math.max(0, stockDraft.total) : 0;
  const stockRemaining = Number.isFinite(stockDraft.remaining) ? Math.max(0, stockDraft.remaining) : 0;
  const isStockDraftInvalid = stockRemaining > stockTotal;
  const isSensoryDirty = sensoryKeys.some((key) => Math.abs(sensoryDraft[key] - sensory[key]) > 0.001);
  const pantryTotal = Math.max(0, Number(pantry?.total_grams ?? 0));
  const pantryRemaining = Math.min(pantryTotal, Math.max(0, Number(pantry?.grams_remaining ?? 0)));
  const isStockDirty = stockTotal !== pantryTotal || stockRemaining !== pantryRemaining;
  const canSaveStock = !isStockDraftInvalid && isStockDirty;
  const baseReviewText = (currentUserReview?.comment ?? "").trim();
  const draftReviewText = reviewDraftText.trim();
  const baseReviewRating = Number(currentUserReview?.rating ?? 0);
  const baseReviewImage = currentUserReview?.image_url ?? "";
  const draftReviewImage = reviewDraftImagePreviewUrl ?? "";
  const isReviewDirty =
    draftReviewText !== baseReviewText ||
    Math.abs(reviewDraftRating - baseReviewRating) > 0.001 ||
    draftReviewImage !== baseReviewImage;
  const canSaveReview = reviewDraftRating > 0 && draftReviewText.length > 0 && isReviewDirty;
  const sensoryEditorsCount = new Set(reviews.map((review) => review.user_id).filter((value): value is number => typeof value === "number")).size;
  const hasAnyOpinions = Boolean(currentUserReview) || otherReviews.length > 0;
  const acquireLabel = useMemo(() => {
    if (!coffee.product_url) return "";
    try {
      const host = new URL(coffee.product_url).hostname.replace(/^www\./i, "");
      return host.toUpperCase();
    } catch {
      return "ADQUIRIR";
    }
  }, [coffee.product_url]);

  useEffect(() => {
    if (!fullPage || externalOpenStockSignal <= 0) return;
    setStockSheetError(null);
    setShowStockSheet(true);
  }, [externalOpenStockSignal, fullPage]);

  return (
    <article className={`coffee-detail ${fullPage ? "is-full-page" : "is-side-panel"}`.trim()}>
      <header className="coffee-detail-hero">
        {coffee.image_url ? <img className="coffee-detail-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" /> : null}
        <div className="coffee-detail-overlay" />
        {!fullPage ? (
          <div className="coffee-detail-hero-top-actions">
            <IconButton tone="topbar" className="coffee-detail-topbar-icon" aria-label="Cerrar detalle" onClick={onClose}>
              <UiIcon name="close" className="ui-icon" />
            </IconButton>
            <div className="coffee-detail-topbar-actions">
              <IconButton
                tone="topbar"
                className={`coffee-detail-topbar-icon ${isFavorite ? "is-active" : ""}`.trim()}
                aria-label={isFavorite ? "Quitar de favoritos" : "Guardar en favoritos"}
                onClick={onToggleFavorite}
              >
                <UiIcon name={isFavorite ? "favorite-filled" : "favorite"} className="ui-icon" />
              </IconButton>
              <IconButton
                tone="topbar"
                className={`coffee-detail-topbar-icon ${pantry ? "is-active" : ""}`.trim()}
                aria-label="Añadir a stock"
                onClick={() => {
                  if (isGuest) {
                    onRequireAuth();
                    return;
                  }
                  setStockSheetError(null);
                  setShowStockSheet(true);
                }}
              >
                <UiIcon name="stock" className="ui-icon" />
              </IconButton>
            </div>
          </div>
        ) : null}

        <div className="coffee-detail-headline">
          <p className="coffee-origin">{coffee.marca ?? "Marca"}</p>
          <h2 className="coffee-detail-title">{coffee.nombre}</h2>
        </div>
        {avgRating > 0 ? (
          <span className="coffee-detail-rating-badge">
            <UiIcon name="star" className="ui-icon" />
            <strong>{avgRating.toFixed(1)}</strong>
          </span>
        ) : null}
      </header>

      <section className="coffee-detail-section coffee-detail-section-first">
        {coffee.descripcion ? <p className="feed-text coffee-detail-description">{coffee.descripcion}</p> : <p className="coffee-sub coffee-detail-opinion-empty">Sin descripción.</p>}
      </section>

      <section className="coffee-detail-section">
        <h3 className="section-title">Detalles técnicos</h3>
        <ul className="coffee-detail-tech-list">
          {techRows.map((row) => (
            <li key={row.label} className="coffee-detail-tech-item">
              <span className="coffee-detail-tech-icon-wrap" aria-hidden="true">
                <UiIcon name={row.icon} className="ui-icon coffee-detail-tech-icon" />
              </span>
              <span className="coffee-detail-tech-copy">
                <strong>{row.label}</strong>
                <em>{row.value}</em>
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section className="coffee-detail-section">
        <div className="coffee-detail-section-head">
          <h3 className="section-title">Perfil sensorial</h3>
          <Button variant="text"
            className="coffee-detail-inline-action"
            onClick={() => {
              if (isGuest) {
                onRequireAuth();
                return;
              }
              setSensorySheetError(null);
              setShowSensorySheet(true);
            }}
          >
            Editar
          </Button>
        </div>
        {sensoryEditorsCount > 0 ? (
          <p className="coffee-detail-sensory-note">
            Basado en los comentarios de {sensoryEditorsCount} usuarios. {sensoryEditorsCount} son las personas que lo han editado.
          </p>
        ) : null}
        <div className="coffee-detail-sensory-summary">
          {sensoryKeys.map((key) => (
            <div key={key} className="coffee-detail-sensory-row">
              <div className="coffee-detail-sensory-meta">
                <span>{sensoryLabels[key].toUpperCase()}</span>
                <strong>{`${sensory[key].toFixed(1).replace(".", ",")}/10`}</strong>
              </div>
              <div className="coffee-detail-sensory-track" aria-hidden="true">
                <div className="coffee-detail-sensory-fill" style={{ width: `${Math.max(0, Math.min(100, (sensory[key] / 10) * 100))}%` }} />
              </div>
            </div>
          ))}
        </div>
      </section>

      {coffee.product_url ? (
        <section className="coffee-detail-section">
          <div className="coffee-detail-acquire">
            <h3 className="section-title">Adquirir</h3>
            <a className="coffee-detail-acquire-row" href={coffee.product_url} target="_blank" rel="noreferrer">
              <span className="coffee-detail-acquire-main">
                <UiIcon name="shop" className="ui-icon coffee-detail-acquire-icon" />
                <strong>{acquireLabel}</strong>
              </span>
              <UiIcon name="chevron-right" className="ui-icon coffee-detail-acquire-chevron" />
            </a>
          </div>
        </section>
      ) : null}

      <section className="coffee-detail-section coffee-detail-opinions-section">
        <div className="coffee-detail-section-head">
          <h3 className="section-title">Opiniones</h3>
          <Button variant="plain"
            className="coffee-detail-opinions-cta"
            onClick={() => {
              if (isGuest) {
                onRequireAuth();
                return;
              }
              setReviewSheetError(null);
              setShowReviewSheet(true);
            }}
          >
            {currentUserReview ? "EDITAR" : "+ AÑADIR"}
          </Button>
        </div>
        {!hasAnyOpinions ? (
          <p className="coffee-detail-opinions-empty">No hay opiniones aún. ¡Sé el primero!</p>
        ) : null}
        {currentUserReview ? (
          <article className="coffee-card coffee-detail-opinion-preview">
            <p className="coffee-detail-opinion-label">Tu opinión</p>
            <div className="coffee-detail-opinion-head">
              {currentUser?.id ? (
                <Button variant="plain"
                  className="coffee-detail-opinion-user-link"
                  onClick={() => {
                    if (isGuest) {
                      onRequireAuth();
                      return;
                    }
                    onOpenUserProfile(currentUser.id);
                  }}
                >
                  <span className="coffee-detail-opinion-user">
                    {currentUser.avatar_url ? (
                      <img className="coffee-detail-opinion-avatar" src={currentUser.avatar_url} alt={currentUser.username} loading="lazy" decoding="async" />
                    ) : (
                      <span className="coffee-detail-opinion-avatar" aria-hidden="true">
                        {(currentUser.username ?? "tu").slice(0, 2).toUpperCase()}
                      </span>
                    )}
                    <span className="coffee-detail-opinion-copy">
                      <span className="feed-user">@{currentUser.username}</span>
                      <span className="feed-meta">{toRelativeMinutes(currentUserReview.timestamp ?? 0)}</span>
                    </span>
                  </span>
                </Button>
              ) : (
                <div className="coffee-detail-opinion-user">
                  <div className="coffee-detail-opinion-avatar" aria-hidden="true">TU</div>
                  <div className="coffee-detail-opinion-copy">
                    <p className="feed-user">@tu_usuario</p>
                    <p className="feed-meta">{toRelativeMinutes(currentUserReview.timestamp ?? 0)}</p>
                  </div>
                </div>
              )}
              <p className="feed-meta coffee-detail-opinion-rating"><UiIcon name="star" className="ui-icon" />{currentUserReview.rating.toFixed(1)} / 5</p>
            </div>
            {currentUserReview.comment ? <p className="feed-text">{currentUserReview.comment}</p> : null}
            {currentUserReview.image_url ? <img className="coffee-detail-review-image" src={currentUserReview.image_url} alt="Tu reseña" loading="lazy" decoding="async" /> : null}
          </article>
        ) : null}
        <ul className="coffee-list">
          {otherReviews.map((review) => (
            <li key={`${review.user_id}-${review.id ?? review.timestamp ?? 0}`} className="coffee-card coffee-detail-opinion-item">
              <div className="coffee-detail-opinion-head">
                {review.user?.id ? (
                  <Button variant="plain"
                    className="coffee-detail-opinion-user-link"
                    onClick={() => {
                      if (isGuest) {
                        onRequireAuth();
                        return;
                      }
                      onOpenUserProfile(review.user!.id);
                    }}
                  >
                    <span className="coffee-detail-opinion-user">
                      {review.user.avatar_url ? (
                        <img className="coffee-detail-opinion-avatar" src={review.user.avatar_url} alt={review.user.username} loading="lazy" decoding="async" />
                      ) : (
                        <span className="coffee-detail-opinion-avatar" aria-hidden="true">
                          {(review.user.username ?? "us").slice(0, 2).toUpperCase()}
                        </span>
                      )}
                      <span className="coffee-detail-opinion-copy">
                        <span className="feed-user">@{review.user.username}</span>
                        <span className="feed-meta">{toRelativeMinutes(review.timestamp ?? 0)}</span>
                      </span>
                    </span>
                  </Button>
                ) : (
                  <div className="coffee-detail-opinion-user">
                    <div className="coffee-detail-opinion-avatar" aria-hidden="true">US</div>
                    <div className="coffee-detail-opinion-copy">
                      <p className="feed-user">@usuario</p>
                      <p className="feed-meta">{toRelativeMinutes(review.timestamp ?? 0)}</p>
                    </div>
                  </div>
                )}
                <p className="feed-meta coffee-detail-opinion-rating"><UiIcon name="star" className="ui-icon" />{review.rating.toFixed(1)} / 5</p>
              </div>
              {review.comment ? <p className="feed-text">{review.comment}</p> : null}
              {review.image_url ? <img className="coffee-detail-review-image" src={review.image_url} alt="Imagen reseña" loading="lazy" decoding="async" /> : null}
            </li>
          ))}
        </ul>
      </section>

      {showSensorySheet ? (
        <SheetOverlay
          role="dialog"
          aria-modal="true"
          aria-label="Editar perfil sensorial"
          onClick={() => {
            if (savingSensory) return;
            setSensorySheetError(null);
            setShowSensorySheet(false);
          }}
        >
          <SheetCard className="coffee-detail-sheet coffee-detail-sensory-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="coffee-detail-sensory-sheet-head">
              <h3 className="coffee-detail-sensory-sheet-title">Perfil sensorial</h3>
              <p className="coffee-detail-sensory-sheet-copy">Tu opinión se unirá a la media de todas las valoraciones</p>
            </header>
            <div className="coffee-detail-sheet-body coffee-detail-sliders coffee-detail-sensory-sliders">
              {sensoryKeys.map((key) => (
                <label key={key} className="coffee-detail-sensory-control">
                  <span className="coffee-detail-slider-label">
                    {sensoryLabels[key]}
                    <strong>{sensoryDraft[key].toFixed(1).replace(".", ",")}</strong>
                  </span>
                  <span className="coffee-detail-sensory-slider-row">
                    <small>0</small>
                    <Input
                      className="app-range"
                      style={{ "--range-progress": `${Math.max(0, Math.min(100, (sensoryDraft[key] / 10) * 100))}%` } as CSSProperties}
                      type="range"
                      min={0}
                      max={10}
                      step={0.5}
                      value={sensoryDraft[key]}
                      onChange={(event) => onSensoryDraftChange({ ...sensoryDraft, [key]: Number(event.target.value) })}
                    />
                    <small>10</small>
                  </span>
                </label>
              ))}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions">
              <Button variant="plain"
                className="action-button coffee-detail-sensory-submit"
                disabled={savingSensory}
                onClick={async () => {
                  setSavingSensory(true);
                  try {
                    await onSaveSensory();
                    setSensorySheetError(null);
                    setShowSensorySheet(false);
                  } catch {
                    setSensorySheetError("No se pudo guardar el perfil sensorial.");
                  } finally {
                    setSavingSensory(false);
                  }
                }}
              >
                {savingSensory ? "Guardando..." : "Listo"}
              </Button>
            </div>
            {sensorySheetError ? <p className="coffee-detail-sheet-error">{sensorySheetError}</p> : null}
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showStockSheet ? (
        <SheetOverlay
          role="dialog"
          aria-modal="true"
          aria-label="Editar stock"
          onClick={() => {
            if (savingStock) return;
            setStockSheetError(null);
            setShowStockSheet(false);
          }}
        >
          <SheetCard className="coffee-detail-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <SheetHeader>
              <strong className="sheet-title">STOCK EN DESPENSA</strong>
            </SheetHeader>
            <div className="coffee-detail-sheet-body coffee-detail-stock">
              <Input
                variant="search"
                className="search-wide search-input-standard"
                type="number"
                min={0}
                value={stockDraft.total}
                onChange={(event) => {
                  onStockDraftChange({ ...stockDraft, total: Number(event.target.value) });
                  if (stockSheetError) setStockSheetError(null);
                }}
                placeholder="Total gramos"
              />
              <Input
                className="search-wide"
                type="number"
                min={0}
                value={stockDraft.remaining}
                onChange={(event) => {
                  onStockDraftChange({ ...stockDraft, remaining: Number(event.target.value) });
                  if (stockSheetError) setStockSheetError(null);
                }}
                placeholder="Restante gramos"
              />
              {stockSheetError ? <p className="coffee-detail-sheet-error">{stockSheetError}</p> : null}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions">
              <Button variant="plain"
                className="action-button action-button-ghost"
                disabled={savingStock}
                onClick={() => {
                  if (savingStock) return;
                  setStockSheetError(null);
                  setShowStockSheet(false);
                }}
              >
                Cancelar
              </Button>
              <Button variant="plain"
                className="action-button"
                disabled={!canSaveStock || savingStock}
                onClick={async () => {
                  if (isStockDraftInvalid) {
                    setStockSheetError("El restante no puede superar el total.");
                    return;
                  }
                  if (!isStockDirty) return;
                  setStockSheetError(null);
                  setSavingStock(true);
                  try {
                    await onSaveStock();
                    setStockSheetError(null);
                    setShowStockSheet(false);
                  } catch {
                    setStockSheetError("No se pudo guardar el stock.");
                  } finally {
                    setSavingStock(false);
                  }
                }}
              >
                {savingStock ? "Guardando..." : "Guardar stock"}
              </Button>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}

      {showReviewSheet ? (
        <SheetOverlay
          role="dialog"
          aria-modal="true"
          aria-label="Escribir reseña"
          onClick={() => {
            setReviewSheetError(null);
            setShowReviewSheet(false);
          }}
        >
          <SheetCard className="coffee-detail-sheet coffee-detail-review-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <SheetHeader>
              <strong className="sheet-title">TU RESEÑA</strong>
            </SheetHeader>
            <div className="coffee-detail-sheet-body coffee-detail-review-editor">
              <label className="coffee-detail-rating-field coffee-detail-review-rating-field">
                <div className="coffee-detail-rating-stars" role="radiogroup" aria-label="Seleccionar nota">
                  {[1, 2, 3, 4, 5].map((value) => (
                    <Button variant="plain"
                      key={value}
                      className={`coffee-detail-rating-star ${reviewDraftRating >= value ? "is-active" : ""}`.trim()}
                      onClick={() => {
                        onReviewRatingChange(value);
                        setReviewSheetError(null);
                      }}
                      onKeyDown={(event) => {
                        if (event.key === "ArrowRight" || event.key === "ArrowUp") {
                          event.preventDefault();
                          onReviewRatingChange(Math.min(5, Math.max(0, Math.round(reviewDraftRating)) + 1));
                          setReviewSheetError(null);
                          return;
                        }
                        if (event.key === "ArrowLeft" || event.key === "ArrowDown") {
                          event.preventDefault();
                          onReviewRatingChange(Math.max(0, Math.max(0, Math.round(reviewDraftRating)) - 1));
                          setReviewSheetError(null);
                          return;
                        }
                        if (event.key === "Home") {
                          event.preventDefault();
                          onReviewRatingChange(1);
                          setReviewSheetError(null);
                          return;
                        }
                        if (event.key === "End") {
                          event.preventDefault();
                          onReviewRatingChange(5);
                          setReviewSheetError(null);
                        }
                      }}
                      role="radio"
                      aria-checked={reviewDraftRating === value}
                      aria-label={`${value} estrellas`}
                    >
                      <UiIcon name={reviewDraftRating >= value ? "star-filled" : "star"} className="ui-icon" />
                    </Button>
                  ))}
                </div>
              </label>
              <div className="sheet-input-shell coffee-detail-review-input-shell">
                <Textarea
                  className="search-wide sheet-input coffee-detail-review-textarea"
                  rows={3}
                  value={reviewDraftText}
                  onChange={(event) => {
                    onReviewTextChange(event.target.value);
                    if (reviewSheetError) setReviewSheetError(null);
                  }}
                  placeholder="Escribe tu reseña"
                />
                <div className="sheet-composer-bottom coffee-detail-review-input-tools">
                  <div className="sheet-composer-tools-inline">
                    <label className="coffee-detail-file coffee-detail-review-camera" aria-label="Adjuntar imagen">
                      <UiIcon name="camera" className="ui-icon" />
                      <Input
                        type="file"
                        accept="image/*"
                        className="file-input-hidden"
                        onChange={(event) => {
                          const file = event.target.files?.[0] ?? null;
                          if (!file) {
                            onReviewImagePick(null, "");
                            return;
                          }
                          onReviewImagePick(file, URL.createObjectURL(file));
                          event.currentTarget.value = "";
                        }}
                      />
                    </label>
                  </div>
                </div>
                {reviewDraftImagePreviewUrl ? (
                  <div className="comment-image-thumb-wrap coffee-detail-review-thumb-wrap">
                    <img
                      className="comment-image-thumb"
                      src={reviewDraftImagePreviewUrl}
                      alt="Previsualización reseña"
                      loading="lazy"
                      decoding="async"
                    />
                    <Button
                      variant="plain"
                      className="comment-image-remove"
                      onClick={() => onReviewImagePick(null, "")}
                      aria-label="Quitar imagen"
                    >
                      x
                    </Button>
                  </div>
                ) : null}
              </div>
              {reviewSheetError ? <p className="coffee-detail-sheet-error">{reviewSheetError}</p> : null}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions coffee-detail-review-actions">
              <Button
                variant="ghost"
                className="action-button action-button-ghost coffee-detail-review-cancel"
                disabled={savingReview || deletingReview}
                onClick={() => {
                  if (savingReview || deletingReview) return;
                  setReviewSheetError(null);
                  setShowReviewSheet(false);
                }}
              >
                Cancelar
              </Button>
              <Button variant="primary"
                className="action-button coffee-detail-review-submit"
                disabled={!canSaveReview || savingReview || deletingReview}
                onClick={async () => {
                  if (reviewDraftRating <= 0) {
                    setReviewSheetError("Selecciona una nota para guardar.");
                    return;
                  }
                  if (draftReviewText.length === 0) {
                    setReviewSheetError("Escribe tu reseña antes de publicar.");
                    return;
                  }
                  if (!isReviewDirty) return;
                  setReviewSheetError(null);
                  setSavingReview(true);
                  try {
                    await onSaveReview();
                    setShowReviewSheet(false);
                  } finally {
                    setSavingReview(false);
                  }
                }}
              >
                {savingReview ? "Publicando..." : "Publicar"}
              </Button>
            </div>
            {canDeleteReview ? (
              <div className="coffee-detail-review-delete-wrap">
                <Button variant="text"
                  className="text-button coffee-detail-review-delete"
                  disabled={savingReview || deletingReview}
                  onClick={async () => {
                    setDeletingReview(true);
                    try {
                      await onDeleteReview();
                      setReviewSheetError(null);
                    } finally {
                      setDeletingReview(false);
                    }
                  }}
                >
                  {deletingReview ? "Borrando..." : "Borrar reseña"}
                </Button>
              </div>
            ) : null}
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </article>
  );
}







