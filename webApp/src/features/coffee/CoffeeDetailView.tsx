import { type CSSProperties, useEffect, useMemo, useState } from "react";
import { createPortal } from "react-dom";
import { EMPTY } from "../../core/emptyErrorStrings";
import { sendEvent } from "../../core/ga4";
import { toRelativeMinutes } from "../../core/time";
import type { CoffeeReviewRow, CoffeeRow, ListPrivacy, PantryItemRow, UserListRow, UserRow } from "../../types";
import { Button, ComposerInputShell, IconButton, Input, SheetCard, SheetHandle, SheetHeader, SheetOverlay } from "../../ui/components";
import { UiIcon, type IconName } from "../../ui/iconography";
import { CreateListSheet } from "../lists/CreateListSheet";
export function CoffeeDetailView({
  coffee,
  reviews,
  currentUser,
  currentUserReview,
  avgRating,
  isFavorite,
  isListActive,
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
  externalOpenStockSignal,
  userLists = [],
  onCreateList,
  onAddCoffeeToList
}: {
  coffee: CoffeeRow;
  reviews: Array<CoffeeReviewRow & { user: UserRow | null }>;
  currentUser: UserRow | null;
  currentUserReview: (CoffeeReviewRow & { user: UserRow | null }) | null;
  avgRating: number;
  isFavorite: boolean;
  /** true si está en favoritos o en alguna lista creada (icono lista activo). */
  isListActive?: boolean;
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
  userLists?: UserListRow[];
  onCreateList?: (name: string, privacy: ListPrivacy) => Promise<void>;
  onAddCoffeeToList?: (listId: string) => Promise<void>;
}) {
  const ADD_TO_LIST_FAVORITES_ID = "__favorites";
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
  const [currentUserAvatarFailed, setCurrentUserAvatarFailed] = useState(false);
  const [failedReviewAvatarUrls, setFailedReviewAvatarUrls] = useState<Set<string>>(new Set());
  const [showAddToListModal, setShowAddToListModal] = useState(false);
  const [showCreateListInModal, setShowCreateListInModal] = useState(false);
  const [addToListSelectedIds, setAddToListSelectedIds] = useState<Set<string>>(new Set());
  const [addToListSaving, setAddToListSaving] = useState(false);
  useEffect(() => {
    if (showAddToListModal) setAddToListSelectedIds(new Set());
  }, [showAddToListModal]);

  useEffect(() => {
    setCurrentUserAvatarFailed(false);
  }, [currentUser?.avatar_url]);

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
    sendEvent("modal_open", { modal_id: "stock_edit" });
    setShowStockSheet(true);
  }, [externalOpenStockSignal, fullPage]);

  return (
    <article className={`coffee-detail ${fullPage ? "is-full-page" : "is-side-panel"}`.trim()}>
      <header className="coffee-detail-hero">
        <div className="coffee-detail-hero-top-actions">
          <IconButton tone="topbar" className="coffee-detail-topbar-icon" aria-label={fullPage ? "Volver" : "Cerrar detalle"} onClick={onClose}>
            <UiIcon name={fullPage ? "arrow-left" : "close"} className="ui-icon" />
          </IconButton>
          <div className="coffee-detail-topbar-actions">
            <IconButton
              tone="topbar"
              className={`coffee-detail-topbar-icon coffee-topbar-favorite ${(isListActive ?? isFavorite) ? "is-active" : ""}`.trim()}
              aria-label={(isListActive ?? isFavorite) ? "Quitar de listas" : "Añadir a listas"}
              onClick={() => {
                if (isGuest) {
                  onRequireAuth();
                  return;
                }
                sendEvent("modal_open", { modal_id: "add_to_list" });
                setShowAddToListModal(true);
              }}
            >
              <UiIcon name={(isListActive ?? isFavorite) ? "list-alt-check" : "list-alt-add"} className="ui-icon" />
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
                sendEvent("modal_open", { modal_id: "stock_edit" });
                setShowStockSheet(true);
              }}
            >
              <UiIcon name="stock" className="ui-icon" />
            </IconButton>
            <IconButton
              tone="topbar"
              className="coffee-detail-topbar-icon"
              aria-label="Compartir café"
              onClick={() => {
                const shareBase = "https://cafesitoapp.com";
                const url = typeof window !== "undefined" ? `${shareBase}${window.location.pathname}` : "";
                const text = `${coffee.marca ?? ""} ${coffee.nombre} – Cafesito: ${url}`.trim();
                if (typeof navigator !== "undefined" && navigator.share) {
                  void navigator.share({ title: `${coffee.marca ?? ""} ${coffee.nombre}`.trim(), url, text });
                } else if (navigator.clipboard?.writeText) {
                  void navigator.clipboard.writeText(text);
                }
              }}
            >
              <UiIcon name="share" className="ui-icon" />
            </IconButton>
          </div>
        </div>

        <div className="coffee-detail-hero-image-block">
          {coffee.image_url ? <img className="coffee-detail-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" /> : null}
          <div className="coffee-detail-overlay" />
          <div className="coffee-detail-headline">
            <p className="coffee-origin">{(coffee.marca ?? "Marca").toUpperCase()}</p>
            <h1 className="coffee-detail-title">{coffee.nombre}</h1>
          </div>
          {avgRating > 0 && reviews.length > 0 ? (
            <div className="coffee-detail-nota-block" aria-label="Nota del café">
              <p className="coffee-detail-nota-title">NOTA</p>
              <p className="coffee-detail-nota-value">{avgRating.toFixed(1)}</p>
            </div>
          ) : null}
        </div>
      </header>

      <section className="coffee-detail-section coffee-detail-section-first">
        {coffee.descripcion ? <p className="feed-text coffee-detail-description">{coffee.descripcion}</p> : <p className="coffee-sub coffee-detail-opinion-empty">Sin descripción.</p>}
      </section>

      {techRows.length > 0 ? (
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
      ) : null}

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
              sendEvent("modal_open", { modal_id: "sensory_profile" });
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
          {!currentUserReview ? (
            <Button variant="plain"
              className="coffee-detail-opinions-cta"
              onClick={() => {
                if (isGuest) {
                  onRequireAuth();
                  return;
                }
                setReviewSheetError(null);
                sendEvent("modal_open", { modal_id: "review" });
                setShowReviewSheet(true);
              }}
            >
              + AÑADIR
            </Button>
          ) : null}
        </div>
        {!hasAnyOpinions ? (
          <p className="coffee-detail-opinions-empty">{EMPTY.OPINIONS}</p>
        ) : null}
        {currentUserReview ? (
          <article className="coffee-card coffee-detail-opinion-preview coffee-detail-opinion-card">
            <div className="coffee-detail-opinion-head">
              <div className="coffee-detail-opinion-avatar-wrap">
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
                    {currentUser.avatar_url && !currentUserAvatarFailed ? (
                      <img
                        className="coffee-detail-opinion-avatar"
                        src={currentUser.avatar_url}
                        alt={currentUser.username}
                        loading="lazy"
                        decoding="async"
                        referrerPolicy="no-referrer"
                        crossOrigin="anonymous"
                        onError={() => setCurrentUserAvatarFailed(true)}
                      />
                    ) : (
                      <span className="coffee-detail-opinion-avatar" aria-hidden="true">
                        {(currentUser.username ?? "tu").slice(0, 2).toUpperCase()}
                      </span>
                    )}
                  </Button>
                ) : (
                  <div className="coffee-detail-opinion-avatar" aria-hidden="true">TU</div>
                )}
              </div>
              <div className="coffee-detail-opinion-body">
                {currentUser?.id ? (
                  <Button variant="plain"
                    className="coffee-detail-opinion-user-link coffee-detail-opinion-copy-link"
                    onClick={() => {
                      if (isGuest) {
                        onRequireAuth();
                        return;
                      }
                      onOpenUserProfile(currentUser.id);
                    }}
                  >
                    <span className="coffee-detail-opinion-copy">
                      <span className="feed-user">@{currentUser.username}</span>
                      <span className="feed-meta">{toRelativeMinutes(currentUserReview.timestamp ?? 0)}</span>
                    </span>
                  </Button>
                ) : (
                  <div className="coffee-detail-opinion-copy">
                    <p className="feed-user">@tu_usuario</p>
                    <p className="feed-meta">{toRelativeMinutes(currentUserReview.timestamp ?? 0)}</p>
                  </div>
                )}
                <span className="coffee-detail-opinion-rating-chip" aria-label="Nota">
                  <UiIcon name="star-filled" className="ui-icon coffee-detail-opinion-chip-star" />{Math.round(currentUserReview.rating)} / 5
                </span>
                {currentUserReview.comment ? <p className="feed-text coffee-detail-opinion-comment">{currentUserReview.comment}</p> : null}
                {currentUserReview.image_url ? <img className="coffee-detail-review-image" src={currentUserReview.image_url} alt="Tu reseña" loading="lazy" decoding="async" /> : null}
              </div>
              <Button variant="plain"
                className="coffee-detail-opinions-cta coffee-detail-opinion-editar"
                onClick={() => {
                  if (isGuest) {
                    onRequireAuth();
                    return;
                  }
                  setReviewSheetError(null);
                  sendEvent("modal_open", { modal_id: "review" });
                  setShowReviewSheet(true);
                }}
              >
                Editar
              </Button>
            </div>
          </article>
        ) : null}
        <ul className="coffee-list">
          {otherReviews.map((review) => (
            <li key={`${review.user_id}-${review.id ?? review.timestamp ?? 0}`} className="coffee-card coffee-detail-opinion-item coffee-detail-opinion-card">
              <div className="coffee-detail-opinion-head">
                <div className="coffee-detail-opinion-avatar-wrap">
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
                      {review.user.avatar_url && !failedReviewAvatarUrls.has(review.user.avatar_url) ? (
                        <img
                          className="coffee-detail-opinion-avatar"
                          src={review.user.avatar_url}
                          alt={review.user.username}
                          loading="lazy"
                          decoding="async"
                          referrerPolicy="no-referrer"
                          crossOrigin="anonymous"
                          onError={() => setFailedReviewAvatarUrls((prev) => new Set(prev).add(review.user!.avatar_url))}
                        />
                      ) : (
                        <span className="coffee-detail-opinion-avatar" aria-hidden="true">
                          {(review.user.username ?? "us").slice(0, 2).toUpperCase()}
                        </span>
                      )}
                    </Button>
                  ) : (
                    <div className="coffee-detail-opinion-avatar" aria-hidden="true">US</div>
                  )}
                </div>
                <div className="coffee-detail-opinion-body">
                  {review.user?.id ? (
                    <Button variant="plain"
                      className="coffee-detail-opinion-user-link coffee-detail-opinion-copy-link"
                      onClick={() => {
                        if (isGuest) {
                          onRequireAuth();
                          return;
                        }
                        onOpenUserProfile(review.user!.id);
                      }}
                    >
                      <span className="coffee-detail-opinion-copy">
                        <span className="feed-user">@{review.user.username}</span>
                        <span className="feed-meta">{toRelativeMinutes(review.timestamp ?? 0)}</span>
                      </span>
                    </Button>
                  ) : (
                    <div className="coffee-detail-opinion-copy">
                      <p className="feed-user">@usuario</p>
                      <p className="feed-meta">{toRelativeMinutes(review.timestamp ?? 0)}</p>
                    </div>
                  )}
                  <span className="coffee-detail-opinion-rating-chip" aria-label="Nota">
                    <UiIcon name="star-filled" className="ui-icon coffee-detail-opinion-chip-star" />{Math.round(review.rating)} / 5
                  </span>
                  {review.comment ? <p className="feed-text coffee-detail-opinion-comment">{review.comment}</p> : null}
                  {review.image_url ? <img className="coffee-detail-review-image" src={review.image_url} alt="Imagen reseña" loading="lazy" decoding="async" /> : null}
                </div>
              </div>
            </li>
          ))}
        </ul>
      </section>

      {showSensorySheet && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Editar perfil sensorial"
              onDismiss={() => {
                if (savingSensory) return;
                sendEvent("modal_close", { modal_id: "sensory_profile" });
                setSensorySheetError(null);
                setShowSensorySheet(false);
              }}
              onClick={() => {
                if (savingSensory) return;
                sendEvent("modal_close", { modal_id: "sensory_profile" });
                setSensorySheetError(null);
                setShowSensorySheet(false);
              }}
            >
              <SheetCard className="coffee-detail-sheet coffee-detail-sensory-sheet" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <header className="coffee-detail-sensory-sheet-head">
                  <div className="coffee-detail-sensory-sheet-head-slot" />
                  <h3 className="coffee-detail-sensory-sheet-title">Perfil sensorial</h3>
                  <div className="coffee-detail-sensory-sheet-head-slot coffee-detail-sensory-sheet-head-actions">
                    <Button variant="plain"
                      className="action-button coffee-detail-sensory-submit coffee-detail-sensory-submit-topbar"
                      disabled={savingSensory}
                      onClick={async () => {
                        setSavingSensory(true);
                        try {
                          await onSaveSensory();
                          sendEvent("modal_close", { modal_id: "sensory_profile" });
                          setSensorySheetError(null);
                          setShowSensorySheet(false);
                        } catch {
                          setSensorySheetError("No se pudo guardar el perfil sensorial.");
                        } finally {
                          setSavingSensory(false);
                        }
                      }}
                    >
                      {savingSensory ? "Guardando..." : "Guardar"}
                    </Button>
                  </div>
                </header>
                <p className="coffee-detail-sensory-sheet-copy">Tu opinión se unirá a la media de todas las valoraciones</p>
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
                {sensorySheetError ? <p className="coffee-detail-sheet-error">{sensorySheetError}</p> : null}
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}

      {showStockSheet && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Editar stock"
              onDismiss={() => {
                if (savingStock) return;
                sendEvent("modal_close", { modal_id: "stock_edit" });
                setStockSheetError(null);
                setShowStockSheet(false);
              }}
              onClick={() => {
                if (savingStock) return;
                sendEvent("modal_close", { modal_id: "stock_edit" });
                setStockSheetError(null);
                setShowStockSheet(false);
              }}
            >
              <SheetCard className="coffee-detail-sheet" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <SheetHeader>
                  <strong className="sheet-title coffee-detail-stock-title">Editar Stock</strong>
                </SheetHeader>
                <div className="coffee-detail-sheet-body coffee-detail-stock">
              <section className="coffee-detail-stock-field">
                <p className="coffee-detail-stock-label">Cantidad de cafe total (g)</p>
                <Input
                  className="coffee-detail-stock-value-input search-wide"
                  type="text"
                  inputMode="numeric"
                  value={String(stockDraft.total)}
                  onChange={(event) => {
                    const nextTotal = Number(event.target.value.replace(/[^0-9]/g, "") || 0);
                    onStockDraftChange({
                      total: nextTotal,
                      remaining: Math.min(Number.isFinite(stockDraft.remaining) ? Math.max(0, stockDraft.remaining) : 0, nextTotal)
                    });
                    if (stockSheetError) setStockSheetError(null);
                  }}
                  aria-label="Cantidad de cafe total"
                />
                <Input
                  className="coffee-detail-stock-slider app-range app-range--caramel search-wide"
                  type="range"
                  min={0}
                  max={1000}
                  step={1}
                  value={Math.max(0, Math.min(1000, stockDraft.total))}
                  style={
                    {
                      "--range-progress": `${Math.max(0, Math.min(100, (Math.max(0, Math.min(1000, stockDraft.total)) / 1000) * 100))}%`
                    } as CSSProperties
                  }
                  onChange={(event) => {
                    const nextTotal = Number(event.target.value);
                    onStockDraftChange({
                      total: nextTotal,
                      remaining: Math.min(Number.isFinite(stockDraft.remaining) ? Math.max(0, stockDraft.remaining) : 0, nextTotal)
                    });
                    if (stockSheetError) setStockSheetError(null);
                  }}
                  aria-label="Deslizar cantidad total"
                />
              </section>

              <section className="coffee-detail-stock-field">
                <p className="coffee-detail-stock-label">Cantidad de cafe restante (g)</p>
                <Input
                  className="coffee-detail-stock-value-input search-wide"
                  type="text"
                  inputMode="numeric"
                  value={String(stockDraft.remaining)}
                  onChange={(event) => {
                    const nextRemaining = Number(event.target.value.replace(/[^0-9]/g, "") || 0);
                    onStockDraftChange({
                      ...stockDraft,
                      remaining: Math.min(nextRemaining, Math.max(0, stockDraft.total))
                    });
                    if (stockSheetError) setStockSheetError(null);
                  }}
                  aria-label="Cantidad de cafe restante"
                />
                <Input
                  className="coffee-detail-stock-slider app-range app-range--caramel search-wide"
                  type="range"
                  min={0}
                  max={Math.max(1, stockDraft.total)}
                  step={1}
                  value={Math.max(0, Math.min(stockDraft.remaining, Math.max(1, stockDraft.total)))}
                  style={
                    {
                      "--range-progress": `${Math.max(
                        0,
                        Math.min(100, (Math.max(0, Math.min(stockDraft.remaining, Math.max(1, stockDraft.total))) / Math.max(1, stockDraft.total)) * 100)
                      )}%`
                    } as CSSProperties
                  }
                  onChange={(event) => {
                    onStockDraftChange({ ...stockDraft, remaining: Number(event.target.value) });
                    if (stockSheetError) setStockSheetError(null);
                  }}
                  aria-label="Deslizar cantidad restante"
                />
              </section>
              {stockSheetError ? <p className="coffee-detail-sheet-error">{stockSheetError}</p> : null}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions coffee-detail-stock-actions">
              <Button variant="plain"
                className="action-button coffee-detail-stock-cancel"
                disabled={savingStock}
                onClick={() => {
                  if (savingStock) return;
                  sendEvent("modal_close", { modal_id: "stock_edit" });
                  setStockSheetError(null);
                  setShowStockSheet(false);
                }}
              >
                CANCELAR
              </Button>
              <Button variant="plain"
                className="action-button coffee-detail-stock-save"
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
                    sendEvent("modal_close", { modal_id: "stock_edit" });
                    setStockSheetError(null);
                    setShowStockSheet(false);
                  } catch {
                    setStockSheetError("No se pudo guardar el stock.");
                  } finally {
                    setSavingStock(false);
                  }
                }}
              >
                {savingStock ? "GUARDANDO..." : "GUARDAR"}
              </Button>
            </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}

      {showReviewSheet && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              role="dialog"
              aria-modal="true"
              aria-label="Tu opinión"
              onDismiss={() => {
                sendEvent("modal_close", { modal_id: "review" });
                setReviewSheetError(null);
                setShowReviewSheet(false);
              }}
              onClick={() => {
                sendEvent("modal_close", { modal_id: "review" });
                setReviewSheetError(null);
                setShowReviewSheet(false);
              }}
            >
              <SheetCard className="coffee-detail-sheet coffee-detail-review-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <SheetHeader>
              <strong className="sheet-title">TU OPINIÓN</strong>
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
              <ComposerInputShell
                value={reviewDraftText}
                onChange={(value) => {
                  onReviewTextChange(value);
                  if (reviewSheetError) setReviewSheetError(null);
                }}
                placeholder="Escribe tu reseña"
                rows={3}
                shellClassName="coffee-detail-review-input-shell"
                textareaClassName="coffee-detail-review-textarea"
                bottomClassName="coffee-detail-review-input-tools"
                toolsContent={
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
                }
                extraContent={
                  reviewDraftImagePreviewUrl ? (
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
                  ) : null
                }
              />
              {reviewSheetError ? <p className="coffee-detail-sheet-error">{reviewSheetError}</p> : null}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions coffee-detail-review-actions">
              {canDeleteReview ? (
                <Button
                  variant="text"
                  className="action-button text-button coffee-detail-review-delete coffee-detail-review-left"
                  disabled={savingReview || deletingReview}
                  onClick={async () => {
                    if (savingReview || deletingReview) return;
                    setDeletingReview(true);
                    try {
                      await onDeleteReview();
                      sendEvent("modal_close", { modal_id: "review" });
                      setReviewSheetError(null);
                      setShowReviewSheet(false);
                    } finally {
                      setDeletingReview(false);
                    }
                  }}
                >
                  {deletingReview ? "Borrando..." : "Eliminar"}
                </Button>
              ) : (
                <Button
                  variant="ghost"
                  className="action-button action-button-ghost coffee-detail-review-cancel"
                  disabled={savingReview || deletingReview}
                  onClick={() => {
                    if (savingReview || deletingReview) return;
                    sendEvent("modal_close", { modal_id: "review" });
                    setReviewSheetError(null);
                    setShowReviewSheet(false);
                  }}
                >
                  Cancelar
                </Button>
              )}
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
                    sendEvent("modal_close", { modal_id: "review" });
                    setShowReviewSheet(false);
                  } finally {
                    setSavingReview(false);
                  }
                }}
              >
                {savingReview ? "Publicando..." : "Publicar"}
              </Button>
            </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {showAddToListModal && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay
              className="profile-topbar-options-overlay"
              role="dialog"
              aria-modal="true"
              aria-label="Añadir a lista"
              onDismiss={() => {
                sendEvent("modal_close", { modal_id: "add_to_list" });
                setShowAddToListModal(false);
              }}
              onClick={() => {
                sendEvent("modal_close", { modal_id: "add_to_list" });
                setShowAddToListModal(false);
              }}
            >
              <SheetCard
                className="diary-sheet diary-sheet-pantry-options profile-topbar-options-sheet add-to-list-sheet"
                onClick={(e) => e.stopPropagation()}
              >
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header sheet-header-with-action">
                  <strong className="sheet-title">Añadir a lista</strong>
                  <Button
                    variant="plain"
                    className="modal-action-btn"
                    disabled={addToListSelectedIds.size === 0 || addToListSaving}
                    onClick={async () => {
                      if (addToListSelectedIds.size === 0) return;
                      setAddToListSaving(true);
                      try {
                        for (const id of addToListSelectedIds) {
                          if (id === ADD_TO_LIST_FAVORITES_ID) {
                            if (!isFavorite) onToggleFavorite();
                          } else {
                            await (onAddCoffeeToList?.(id) ?? Promise.resolve());
                          }
                        }
                        setShowAddToListModal(false);
                      } finally {
                        setAddToListSaving(false);
                      }
                    }}
                  >
                    {addToListSaving ? "Añadiendo…" : "Añadir"}
                  </Button>
                </header>
                <div className="diary-sheet-list">
                  <Button
                    variant="plain"
                    className="diary-sheet-action diary-sheet-action-pantry"
                    onClick={() => {
                    sendEvent("modal_open", { modal_id: "create_list" });
                    setShowCreateListInModal(true);
                  }}
                  >
                    <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">add</span>
                    <span>Crear una lista</span>
                    <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                  </Button>
                  {userLists.map((list) => {
                    const checked = addToListSelectedIds.has(list.id);
                    return (
                      <Button
                        key={list.id}
                        variant="plain"
                        className={`diary-sheet-action diary-sheet-action-pantry add-to-list-row ${checked ? "is-checked" : ""}`.trim()}
                        onClick={() => {
                          setAddToListSelectedIds((prev) => {
                            const next = new Set(prev);
                            if (next.has(list.id)) next.delete(list.id);
                            else next.add(list.id);
                            return next;
                          });
                        }}
                      >
                        <span className="add-to-list-checkbox" aria-hidden="true">
                          {checked ? (
                            <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">check_box</span>
                          ) : (
                            <span className="ui-icon material-symbol-icon" aria-hidden="true">check_box_outline_blank</span>
                          )}
                        </span>
                        <UiIcon name="list-alt" className="ui-icon" />
                        <span>{list.name}</span>
                      </Button>
                    );
                  })}
                  <Button
                    variant="plain"
                    className={`diary-sheet-action diary-sheet-action-pantry add-to-list-row ${isFavorite ? "is-active" : ""} ${addToListSelectedIds.has(ADD_TO_LIST_FAVORITES_ID) ? "is-checked" : ""}`.trim()}
                    onClick={() => {
                      setAddToListSelectedIds((prev) => {
                        const next = new Set(prev);
                        if (next.has(ADD_TO_LIST_FAVORITES_ID)) next.delete(ADD_TO_LIST_FAVORITES_ID);
                        else next.add(ADD_TO_LIST_FAVORITES_ID);
                        return next;
                      });
                    }}
                  >
                    <span className="add-to-list-checkbox" aria-hidden="true">
                      {addToListSelectedIds.has(ADD_TO_LIST_FAVORITES_ID) ? (
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">check_box</span>
                      ) : (
                        <span className="ui-icon material-symbol-icon" aria-hidden="true">check_box_outline_blank</span>
                      )}
                    </span>
                    <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">favorite</span>
                    <span>Favoritos</span>
                  </Button>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {showCreateListInModal && showAddToListModal && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Nueva lista" onDismiss={() => { sendEvent("modal_close", { modal_id: "create_list" }); setShowCreateListInModal(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "create_list" }); setShowCreateListInModal(false); }}>
              <CreateListSheet
                onDismiss={() => { sendEvent("modal_close", { modal_id: "create_list" }); setShowCreateListInModal(false); }}
                onCreate={(name, privacy) =>
                  (onCreateList?.(name, privacy) ?? Promise.resolve()).then(() => {
                    sendEvent("modal_close", { modal_id: "create_list" });
                    setShowCreateListInModal(false);
                  })
                }
              />
            </SheetOverlay>,
            document.body
          )
        : null}
    </article>
  );
}







