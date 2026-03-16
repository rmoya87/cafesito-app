import { Fragment, type CSSProperties, type ReactNode, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { getOrderedBrewMethods, type BrewMethodItem } from "../../config/brew";
import { EMPTY, ERROR } from "../../core/emptyErrorStrings";
import type { CoffeeRow, HomeCard, UserRow } from "../../types";
import { MentionText } from "../../ui/MentionText";
import { UiIcon } from "../../ui/iconography";
import { Button, EmptyState, ErrorState, IconButton, Input, SheetCard, SheetHandle, SheetOverlay, Textarea } from "../../ui/components";

export type HomeHeroUser = { fullName: string; username: string; avatarUrl: string | null };

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

export function HomeView({
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
  onToggleFollow,
  onRefresh,
  onMentionClick,
  resolveMentionUser,
  onOpenUserProfile,
  onOpenCoffee,
  onOpenSearch,
  activeUserDisplay,
  sidePanel,
  onOpenBrewToMethod,
  orderedBrewMethods = [],
  pantryItems = [],
  onPantryCoffeeClick,
  onAddToPantry,
  onUpdatePantryStock,
  onRemovePantryItem,
  onMarkPantryCoffeeFinished
}: {
  mode: "mobile" | "desktop";
  cards: HomeCard[];
  recommendations: CoffeeRow[];
  suggestions: UserRow[];
  suggestionIndices: number[];
  followingIds: Set<number>;
  followerCounts: Map<number, number>;
  loading: boolean;
  errorMessage: string | null;
  refreshing: boolean;
  activeUserId: number | null;
  onToggleFollow: (userId: number) => void;
  onRefresh: () => Promise<void>;
  onMentionClick: (username: string) => void;
  resolveMentionUser?: (username: string) => { username: string; avatarUrl?: string | null } | null | undefined;
  onOpenUserProfile: (userId: number) => void;
  onOpenCoffee: (coffeeId: string) => void;
  onOpenSearch?: () => void;
  activeUserDisplay?: HomeHeroUser | null;
  sidePanel?: ReactNode;
  /** Al pulsar un método de elaboración: ir a pestaña Elabora en el paso de elegir café con ese método */
  onOpenBrewToMethod?: (methodName: string) => void;
  /** Métodos de elaboración ordenados (más usados primero, luego alfabético, Otros al final). */
  orderedBrewMethods?: BrewMethodItem[];
  /** Filas de despensa (mismo formato que en Elige tu café). Si no se pasa, no se muestra el bloque. */
  pantryItems?: Array<{ item: { id: string; coffee_id: string }; coffee: CoffeeRow; total: number; remaining: number; progress: number }>;
  /** Al pulsar un café de despensa: ir a elaboración dosis (CONFIGURA) y al terminar ir a Mi diario */
  onPantryCoffeeClick?: (coffeeId: string) => void;
  /** Al pulsar el + de despensa: flujo añadir a despensa; al terminar volver a home */
  onAddToPantry?: () => void;
  /** Opciones despensa: editar stock, café terminado, eliminar */
  onUpdatePantryStock?: (pantryItemId: string, totalGrams: number, gramsRemaining: number) => Promise<void>;
  onRemovePantryItem?: (pantryItemId: string) => Promise<void>;
  onMarkPantryCoffeeFinished?: (pantryItemId: string) => Promise<void>;
}) {
  const [pantryOptionsCoffeeId, setPantryOptionsCoffeeId] = useState<string | null>(null);
  const [pantryDeleteConfirmCoffeeId, setPantryDeleteConfirmCoffeeId] = useState<string | null>(null);
  const [pantryFinishedConfirmCoffeeId, setPantryFinishedConfirmCoffeeId] = useState<string | null>(null);
  const [stockEditCoffeeId, setStockEditCoffeeId] = useState<string | null>(null);
  const [stockEditTotal, setStockEditTotal] = useState("");
  const [stockEditRemaining, setStockEditRemaining] = useState("");
  const [savingStock, setSavingStock] = useState(false);
  const [removingStock, setRemovingStock] = useState(false);
  const [markingFinished, setMarkingFinished] = useState(false);

  const [dismissingSuggestionIds, setDismissingSuggestionIds] = useState<Set<number>>(new Set());
  const [pendingSuggestionFollowIds, setPendingSuggestionFollowIds] = useState<Set<number>>(new Set());
  const [failedAvatarUrls, setFailedAvatarUrls] = useState<Set<string>>(new Set());
  const [pullDistance, setPullDistance] = useState(0);
  const touchStartY = useRef<number | null>(null);
  const pullActive = useRef(false);
  const elaborationScrollRef = useRef<HTMLDivElement | null>(null);
  const despensaScrollRef = useRef<HTMLDivElement | null>(null);
  const recommendationsScrollRef = useRef<HTMLDivElement | null>(null);
  const suggestionsScrollRef = useRef<HTMLDivElement | null>(null);

  /* Drag-to-scroll para carrusel de método (home): mismo comportamiento que en elaboración */
  const HOME_CAROUSEL_DRAG_THRESHOLD = 8;
  const homeCarouselDragElRef = useRef<HTMLDivElement | null>(null);
  const homeCarouselDragStartXRef = useRef(0);
  const homeCarouselDragStartScrollRef = useRef(0);
  const homeCarouselDragPointerIdRef = useRef<number | null>(null);
  const homeCarouselDragActiveRef = useRef(false);
  const homeCarouselPendingScrollRef = useRef<number | null>(null);
  const homeCarouselRafRef = useRef<number | null>(null);
  const [homeCarouselDragging, setHomeCarouselDragging] = useState(false);

  const handleHomeCarouselPointerDown = useCallback((e: React.PointerEvent, el: HTMLDivElement | null) => {
    if (!el || el.scrollWidth <= el.clientWidth) return;
    if (homeCarouselRafRef.current != null) {
      cancelAnimationFrame(homeCarouselRafRef.current);
      homeCarouselRafRef.current = null;
    }
    homeCarouselDragElRef.current = el;
    homeCarouselDragStartXRef.current = e.clientX;
    homeCarouselDragStartScrollRef.current = el.scrollLeft;
    homeCarouselDragPointerIdRef.current = e.pointerId;
    homeCarouselDragActiveRef.current = false;
    homeCarouselPendingScrollRef.current = null;
    setHomeCarouselDragging(false);
  }, []);

  const handleHomeCarouselPointerMove = useCallback((e: React.PointerEvent) => {
    const el = homeCarouselDragElRef.current;
    if (!el || homeCarouselDragPointerIdRef.current !== e.pointerId) return;
    if (!homeCarouselDragActiveRef.current) {
      if (Math.abs(e.clientX - homeCarouselDragStartXRef.current) < HOME_CAROUSEL_DRAG_THRESHOLD) return;
      e.preventDefault();
      el.setPointerCapture(e.pointerId);
      homeCarouselDragActiveRef.current = true;
      setHomeCarouselDragging(true);
    }
    e.preventDefault();
    const delta = e.clientX - homeCarouselDragStartXRef.current;
    const newLeft = homeCarouselDragStartScrollRef.current - delta;
    const clamped = Math.max(0, Math.min(el.scrollWidth - el.clientWidth, newLeft));
    homeCarouselPendingScrollRef.current = clamped;
    if (homeCarouselRafRef.current == null) {
      homeCarouselRafRef.current = requestAnimationFrame(() => {
        homeCarouselRafRef.current = null;
        const target = homeCarouselDragElRef.current;
        const pending = homeCarouselPendingScrollRef.current;
        if (target != null && pending != null) {
          target.scrollLeft = pending;
        }
      });
    }
  }, []);

  const handleHomeCarouselPointerUp = useCallback((e: React.PointerEvent) => {
    if (homeCarouselDragPointerIdRef.current !== e.pointerId) return;
    const el = homeCarouselDragElRef.current;
    if (homeCarouselRafRef.current != null) {
      cancelAnimationFrame(homeCarouselRafRef.current);
      homeCarouselRafRef.current = null;
    }
    const pending = homeCarouselPendingScrollRef.current;
    if (el != null && pending != null) {
      el.scrollLeft = pending;
    }
    homeCarouselDragPointerIdRef.current = null;
    homeCarouselDragElRef.current = null;
    homeCarouselDragActiveRef.current = false;
    homeCarouselPendingScrollRef.current = null;
    setHomeCarouselDragging(false);
    if (el?.hasPointerCapture(e.pointerId)) el.releasePointerCapture(e.pointerId);
  }, []);

  const [carouselHasScroll, setCarouselHasScroll] = useState({
    elaboration: false,
    despensa: false,
    recommendations: false,
    suggestions: false
  });

  const checkCarouselOverflow = useCallback(() => {
    const hasOverflow = (el: HTMLDivElement | null, threshold = 2): boolean => {
      if (!el) return false;
      const clientW = Math.round(el.getBoundingClientRect().width);
      if (clientW < 250) return false;
      const contentW = Math.round(el.scrollWidth);
      return contentW > clientW + threshold;
    };

    setCarouselHasScroll({
      elaboration: hasOverflow(elaborationScrollRef.current),
      despensa: hasOverflow(despensaScrollRef.current, 20),
      recommendations: hasOverflow(recommendationsScrollRef.current),
      suggestions: hasOverflow(suggestionsScrollRef.current)
    });
  }, []);

  useLayoutEffect(() => {
    const runCheck = () => {
      requestAnimationFrame(() => {
        requestAnimationFrame(() => checkCarouselOverflow());
      });
    };

    runCheck();
    window.addEventListener("resize", runCheck);

    const refs = [elaborationScrollRef, despensaScrollRef, recommendationsScrollRef, suggestionsScrollRef];
    const observers: ResizeObserver[] = [];

    if (typeof ResizeObserver !== "undefined") {
      refs.forEach((ref) => {
        const el = ref.current;
        if (!el) return;
        const ro = new ResizeObserver(runCheck);
        ro.observe(el);
        observers.push(ro);
        const firstChild = el.firstElementChild;
        if (firstChild) {
          const roChild = new ResizeObserver(runCheck);
          roChild.observe(firstChild);
          observers.push(roChild);
        }
      });
    }

    return () => {
      window.removeEventListener("resize", runCheck);
      observers.forEach((ro) => ro.disconnect());
    };
  }, [checkCarouselOverflow, mode, pantryItems.length, recommendations.length, suggestions.length]);

  const CAROUSEL_SCROLL_PX = 280;
  const scrollCarousel = useCallback((ref: { current: HTMLDivElement | null }, direction: "prev" | "next") => {
    const el = ref.current;
    if (!el) return;
    const delta = direction === "prev" ? -CAROUSEL_SCROLL_PX : CAROUSEL_SCROLL_PX;
    el.scrollBy({ left: delta, behavior: "smooth" });
  }, []);
  const visibleCards = cards;
  const isDesktopHome = mode === "desktop";

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

  const stockEditTarget = useMemo(
    () => (stockEditCoffeeId ? pantryItems.find((r) => r.item.id === stockEditCoffeeId) ?? null : null),
    [pantryItems, stockEditCoffeeId]
  );

  const recommendationChunks = useMemo(() => {
    const size = 3;
    const chunks: typeof recommendations[] = [];
    for (let i = 0; i < recommendations.length; i += size) {
      chunks.push(recommendations.slice(i, i + size));
    }
    return chunks;
  }, [recommendations]);

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
      <ErrorState
        detail={errorMessage}
        onRetry={() => void onRefresh()}
      />
    );
  }

  const elaborationMethodsToShow = orderedBrewMethods.length > 0 ? orderedBrewMethods : getOrderedBrewMethods([]);
  const elaborationMethodsBlock = onOpenBrewToMethod ? (
    <section className="home-elaboration-methods" aria-label="Formas de elaboración">
      <div className="home-carousel-with-nav">
        <div
          ref={elaborationScrollRef}
          className={`home-elaboration-methods-scroll${homeCarouselDragging ? " is-dragging" : ""}`.trim()}
          onPointerDown={(e) => handleHomeCarouselPointerDown(e, elaborationScrollRef.current)}
          onPointerMove={handleHomeCarouselPointerMove}
          onPointerUp={handleHomeCarouselPointerUp}
          onPointerLeave={handleHomeCarouselPointerUp}
          onPointerCancel={handleHomeCarouselPointerUp}
        >
          {elaborationMethodsToShow.map((method) => (
            <Button
              key={method.name}
              variant="plain"
              type="button"
              className="home-elaboration-method-circle"
              onClick={() => onOpenBrewToMethod(method.name)}
              aria-label={`Elaborar con ${method.name}`}
            >
              <span className="home-elaboration-method-circle-inner">
                {method.icon === "bolt" ? (
                  <UiIcon name="bolt" className="ui-icon home-elaboration-method-icon-bolt" aria-hidden="true" />
                ) : method.icon === "water" ? (
                  <UiIcon name="water" className="ui-icon home-elaboration-method-icon-water" aria-hidden="true" />
                ) : (
                  <img src={method.icon} alt="" loading="lazy" decoding="async" />
                )}
              </span>
              <span className="home-elaboration-method-label">{method.name}</span>
            </Button>
          ))}
        </div>
        {carouselHasScroll.elaboration ? (
          <div className="home-carousel-nav">
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-prev" aria-label="Anterior" onClick={() => scrollCarousel(elaborationScrollRef, "prev")}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-next" aria-label="Siguiente" onClick={() => scrollCarousel(elaborationScrollRef, "next")}>
              <UiIcon name="arrow-right" className="ui-icon" />
            </button>
          </div>
        ) : null}
      </div>
    </section>
  ) : null;

  const hasPantryOptions = onPantryCoffeeClick != null || onAddToPantry != null || onUpdatePantryStock != null || onRemovePantryItem != null || onMarkPantryCoffeeFinished != null;
  const parsedStockTotal = Number(stockEditTotal || 0);
  const parsedStockRemaining = Number(stockEditRemaining || 0);
  const canSaveStock =
    Number.isFinite(parsedStockTotal) &&
    Number.isFinite(parsedStockRemaining) &&
    parsedStockTotal >= 1 &&
    parsedStockRemaining >= 0 &&
    parsedStockRemaining <= parsedStockTotal;
  const stockValidationMessage =
    (!Number.isFinite(parsedStockTotal) || !Number.isFinite(parsedStockRemaining))
      ? "Introduce valores válidos."
      : parsedStockTotal < 1
        ? "El total debe ser mayor que 0."
        : parsedStockRemaining < 0
          ? "El restante no puede ser negativo."
          : parsedStockRemaining > parsedStockTotal
            ? "El restante no puede superar el total."
            : "";

  const pantryBlock = hasPantryOptions ? (
    <section className="home-despensa suggestion-strip" aria-label="Tu despensa">
      <p className="section-title">TU DESPENSA</p>
      <div className="home-carousel-with-nav">
        <div ref={despensaScrollRef} className="home-despensa-scroll">
        <div className="brew-pantry-row">
          {pantryItems.map((row) => (
            <div
              key={row.item.id}
              className="brew-pantry-card"
              role="button"
              tabIndex={0}
              onClick={() => onPantryCoffeeClick?.(row.coffee.id)}
              onKeyDown={(e) => {
                if (e.key === "Enter" || e.key === " ") {
                  e.preventDefault();
                  onPantryCoffeeClick?.(row.coffee.id);
                }
              }}
            >
              <div className="diary-pantry-top">
                {row.coffee.image_url ? (
                  <img src={row.coffee.image_url} alt={row.coffee.nombre} loading="lazy" decoding="async" />
                ) : (
                  <span className="brew-pantry-fallback" aria-hidden="true">{row.coffee.nombre.slice(0, 1).toUpperCase()}</span>
                )}
                <Button
                  variant="plain"
                  type="button"
                  className="diary-pantry-options"
                  aria-label="Opciones"
                  onClick={(e) => {
                    e.stopPropagation();
                    setPantryOptionsCoffeeId(row.item.id);
                  }}
                >
                  <UiIcon name="more" className="ui-icon" />
                </Button>
              </div>
              <div className="brew-pantry-body">
                <strong>{row.coffee.nombre}</strong>
                <small>{Math.round(row.remaining)}/{Math.round(row.total)}g</small>
                <div className="brew-pantry-progress" aria-hidden="true">
                  <span style={{ width: `${Math.max(0, Math.min(100, row.progress * 100))}%` }} />
                </div>
              </div>
            </div>
          ))}
          {onAddToPantry ? (
            <Button
              variant="plain"
              type="button"
              className="brew-pantry-add-card"
              aria-label="Añadir café a despensa"
              onClick={onAddToPantry}
            >
              <span className="brew-pantry-add-main" aria-hidden="true">
                <span className="brew-pantry-add-icon-wrap">
                  <UiIcon name="add" className="ui-icon" />
                </span>
              </span>
              <span className="brew-pantry-add-footer" aria-hidden="true" />
            </Button>
          ) : null}
        </div>
        </div>
        {carouselHasScroll.despensa ? (
          <div className="home-carousel-nav">
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-prev" aria-label="Anterior" onClick={() => scrollCarousel(despensaScrollRef, "prev")}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-next" aria-label="Siguiente" onClick={() => scrollCarousel(despensaScrollRef, "next")}>
              <UiIcon name="arrow-right" className="ui-icon" />
            </button>
          </div>
        ) : null}
      </div>
    </section>
  ) : null;

  const recommendationSection = recommendations.length ? (
    <section className="suggestion-strip home-recommendations-day" aria-label="Recomendaciones del día">
      <p className="section-title">Recomendaciones del día</p>
      <div className="home-carousel-with-nav">
        <div ref={recommendationsScrollRef} className="home-recommendations-list home-carousel-scroll">
          {recommendationChunks.map((chunk, chunkIndex) => (
            <div key={chunkIndex} className="home-recommendation-card">
              {chunk.map((coffee) => (
                <Button
                  key={coffee.id}
                  variant="plain"
                  className="home-recommendation-row"
                  onClick={() => onOpenCoffee(coffee.id)}
                >
                  <span className="home-recommendation-row-image">
                    {coffee.image_url ? (
                      <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
                    ) : (
                      <span className="home-recommendation-row-placeholder" aria-hidden="true" />
                    )}
                  </span>
                  <span className="home-recommendation-row-copy">
                    <span className="home-recommendation-row-name">{coffee.nombre}</span>
                    <span className="home-recommendation-row-brand">{(coffee.marca || "").trim() || "—"}</span>
                  </span>
                </Button>
              ))}
            </div>
          ))}
        </div>
        {carouselHasScroll.recommendations ? (
          <div className="home-carousel-nav">
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-prev" aria-label="Anterior" onClick={() => scrollCarousel(recommendationsScrollRef, "prev")}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-next" aria-label="Siguiente" onClick={() => scrollCarousel(recommendationsScrollRef, "next")}>
              <UiIcon name="arrow-right" className="ui-icon" />
            </button>
          </div>
        ) : null}
      </div>
    </section>
  ) : null;

  const userSuggestionsSection = suggestions.length ? (
    <section className="suggestion-strip">
      <p className="section-title">Personas que podrias seguir</p>
      <div className="home-carousel-with-nav">
        <div ref={suggestionsScrollRef} className="horizontal-cards home-carousel-scroll">
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
        {carouselHasScroll.suggestions ? (
          <div className="home-carousel-nav">
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-prev" aria-label="Anterior" onClick={() => scrollCarousel(suggestionsScrollRef, "prev")}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
            <button type="button" className="home-carousel-nav-btn home-carousel-nav-next" aria-label="Siguiente" onClick={() => scrollCarousel(suggestionsScrollRef, "next")}>
              <UiIcon name="arrow-right" className="ui-icon" />
            </button>
          </div>
        ) : null}
      </div>
    </section>
  ) : null;

  const desktopSideContent = sidePanel ? (
    <aside className="home-side-column">{sidePanel}</aside>
  ) : null;

  if (!cards.length) {
    const emptyTitle = EMPTY.HOME_TITLE;
    const emptySubtitle = EMPTY.HOME_SUBTITLE;
    if (isDesktopHome) {
      return (
        <div className="home-shell home-shell-desktop">
          <div className={`home-desktop-columns ${desktopSideContent ? "home-desktop-columns-has-side" : ""}`.trim()}>
            <div className="home-main-column">
              {elaborationMethodsBlock}
              {pantryBlock}
              {recommendationSection}
              <article className="home-empty">
                <h3>{emptyTitle}</h3>
                <p>{emptySubtitle}</p>
              </article>
              {userSuggestionsSection}
            </div>
            {desktopSideContent}
          </div>
        </div>
      );
    }
    return (
      <>
        {elaborationMethodsBlock}
        {pantryBlock}
        {recommendationSection}
        <article className="home-empty">
          <h3>{emptyTitle}</h3>
          <p>{emptySubtitle}</p>
        </article>
        {userSuggestionsSection}
      </>
    );
  }

  return (
    <div
      className={`home-shell ${isDesktopHome ? "home-shell-desktop" : ""}`.trim()}
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

      <div className={`home-desktop-columns ${isDesktopHome && desktopSideContent ? "home-desktop-columns-has-side" : ""}`.trim()}>
          <div className="home-main-column">
            {elaborationMethodsBlock}
            {pantryBlock}
            {recommendationSection}
            {!visibleCards.length ? (
              <EmptyState
                title={EMPTY.HOME_NO_POSTS}
                subtitle={EMPTY.HOME_NO_POSTS_SUB}
              />
            ) : null}
            {userSuggestionsSection}
          </div>
          {isDesktopHome ? desktopSideContent : null}
        </div>
      {pantryOptionsCoffeeId && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Opciones despensa" onDismiss={() => setPantryOptionsCoffeeId(null)} onClick={() => setPantryOptionsCoffeeId(null)}>
              <SheetCard className="diary-sheet diary-sheet-pantry-options list-options-general-wrap" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-sheet-list list-options-general-wrap">
                  <div className="list-options-page-section">
                    <h3 className="create-list-privacy-subtitle">Organiza</h3>
                    <div className="list-options-general-card">
                      {onUpdatePantryStock ? (
                        <Button
                          variant="plain"
                          type="button"
                          className="list-options-page-action diary-sheet-action-pantry"
                          onClick={() => {
                            const row = pantryItems.find((r) => r.item.id === pantryOptionsCoffeeId);
                            if (!row) return;
                            setStockEditTotal(String(Math.max(1, row.total)));
                            setStockEditRemaining(String(Math.max(0, row.remaining)));
                            setStockEditCoffeeId(pantryOptionsCoffeeId);
                            setPantryOptionsCoffeeId(null);
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">edit</span>
                          <span>Editar stock</span>
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">chevron_right</span>
                        </Button>
                      ) : null}
                      {onMarkPantryCoffeeFinished ? (
                        <Button
                          variant="plain"
                          type="button"
                          className="list-options-page-action diary-sheet-action-pantry"
                          onClick={() => {
                            setPantryFinishedConfirmCoffeeId(pantryOptionsCoffeeId);
                            setPantryOptionsCoffeeId(null);
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">check_circle</span>
                          <span>Café terminado</span>
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">chevron_right</span>
                        </Button>
                      ) : null}
                    </div>
                  </div>
                  <div className="list-options-page-section list-options-section-spaced">
                    <h3 className="create-list-privacy-subtitle">General</h3>
                    <div className="list-options-general-card">
                      {onRemovePantryItem ? (
                        <Button
                          variant="plain"
                          type="button"
                          className="list-options-page-action diary-sheet-action-pantry"
                          disabled={removingStock}
                          onClick={() => {
                            setPantryDeleteConfirmCoffeeId(pantryOptionsCoffeeId);
                            setPantryOptionsCoffeeId(null);
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">delete</span>
                          <span>Eliminar de la despensa</span>
                          <span className="ui-icon material-symbol-icon is-filled diary-sheet-action-fill-icon" aria-hidden="true">chevron_right</span>
                        </Button>
                      ) : null}
                    </div>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {pantryFinishedConfirmCoffeeId && onMarkPantryCoffeeFinished && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Café terminado" onDismiss={() => setPantryFinishedConfirmCoffeeId(null)} onClick={() => setPantryFinishedConfirmCoffeeId(null)}>
              <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-delete-confirm-body">
                  <h2 className="diary-delete-confirm-title">Café terminado</h2>
                  <p className="diary-delete-confirm-text">
                    ¿Marcar este café como terminado? Se quitará de tu despensa y se guardará en Historial.
                  </p>
                  <div className="diary-delete-confirm-actions">
                    <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => setPantryFinishedConfirmCoffeeId(null)} disabled={markingFinished}>
                      Cancelar
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-submit"
                      disabled={markingFinished}
                      onClick={async () => {
                        if (markingFinished) return;
                        setMarkingFinished(true);
                        try {
                          await onMarkPantryCoffeeFinished(pantryFinishedConfirmCoffeeId);
                          setPantryFinishedConfirmCoffeeId(null);
                        } finally {
                          setMarkingFinished(false);
                        }
                      }}
                    >
                      {markingFinished ? "Guardando..." : "Confirmar"}
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {pantryDeleteConfirmCoffeeId && onRemovePantryItem && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Eliminar de la despensa" onDismiss={() => setPantryDeleteConfirmCoffeeId(null)} onClick={() => setPantryDeleteConfirmCoffeeId(null)}>
              <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-delete-confirm-body">
                  <h2 className="diary-delete-confirm-title">Eliminar de la despensa</h2>
                  <p className="diary-delete-confirm-text">
                    ¿Estás seguro de eliminar este café? Se borrará tu stock actual.
                  </p>
                  <div className="diary-delete-confirm-actions">
                    <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => setPantryDeleteConfirmCoffeeId(null)} disabled={removingStock}>
                      Cancelar
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-submit"
                      disabled={removingStock}
                      onClick={async () => {
                        if (removingStock) return;
                        setRemovingStock(true);
                        try {
                          await onRemovePantryItem(pantryDeleteConfirmCoffeeId);
                          setPantryDeleteConfirmCoffeeId(null);
                        } finally {
                          setRemovingStock(false);
                        }
                      }}
                    >
                      {removingStock ? "Eliminando..." : "Eliminar"}
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {stockEditTarget && onUpdatePantryStock && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Editar stock" onDismiss={() => setStockEditCoffeeId(null)} onClick={() => setStockEditCoffeeId(null)}>
              <SheetCard className="diary-sheet diary-stock-edit-sheet" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header diary-stock-edit-header">
                  <strong className="sheet-title">Editar Stock</strong>
                </header>
                <div className="diary-sheet-form diary-stock-edit-form">
                  <label className="diary-stock-edit-field">
                    <span>Cantidad de café total (g)</span>
                    <Input
                      className="diary-stock-edit-value search-wide"
                      type="text"
                      inputMode="numeric"
                      value={String(Math.max(0, Number(stockEditTotal || 0)))}
                      onChange={(event) => {
                        const digitsOnly = event.target.value.replace(/[^0-9]/g, "");
                        const nextTotal = String(Number(digitsOnly || 0));
                        setStockEditTotal(nextTotal);
                        const parsedNextTotal = Number(nextTotal || 0);
                        const parsedCurrentRemaining = Number(stockEditRemaining || 0);
                        if (Number.isFinite(parsedNextTotal) && parsedNextTotal >= 0 && Number.isFinite(parsedCurrentRemaining) && parsedCurrentRemaining > parsedNextTotal) {
                          setStockEditRemaining(String(parsedNextTotal));
                        }
                      }}
                    />
                    <Input
                      className="diary-stock-edit-slider app-range search-wide"
                      type="range"
                      min={0}
                      max={1000}
                      step={1}
                      value={Math.max(0, Math.min(1000, Number(stockEditTotal || 0)))}
                      style={{ "--range-progress": `${Math.max(0, Math.min(100, (Math.max(0, Math.min(1000, Number(stockEditTotal || 0))) / 1000) * 100))}%` } as CSSProperties}
                      onChange={(event) => {
                        const nextTotal = Number(event.target.value);
                        setStockEditTotal(String(nextTotal));
                        const currentRemaining = Number(stockEditRemaining || 0);
                        if (currentRemaining > nextTotal) setStockEditRemaining(String(nextTotal));
                      }}
                    />
                  </label>
                  <label className="diary-stock-edit-field">
                    <span>Cantidad de café restante (g)</span>
                    <Input
                      className="diary-stock-edit-value search-wide"
                      type="text"
                      inputMode="numeric"
                      value={String(Math.max(0, Number(stockEditRemaining || 0)))}
                      onChange={(event) => {
                        const digitsOnly = event.target.value.replace(/[^0-9]/g, "");
                        setStockEditRemaining(String(Number(digitsOnly || 0)));
                      }}
                    />
                    <Input
                      className="diary-stock-edit-slider app-range search-wide"
                      type="range"
                      min={0}
                      max={Math.max(1, Number(stockEditTotal || 0))}
                      step={1}
                      value={Math.max(0, Math.min(Number(stockEditRemaining || 0), Math.max(1, Number(stockEditTotal || 0))))}
                      style={{
                        "--range-progress": `${Math.max(
                          0,
                          Math.min(
                            100,
                            (Math.max(0, Math.min(Number(stockEditRemaining || 0), Math.max(1, Number(stockEditTotal || 0)))) / Math.max(1, Number(stockEditTotal || 0))) * 100
                          )
                        )}%`
                      } as CSSProperties}
                      onChange={(event) => setStockEditRemaining(event.target.value)}
                    />
                  </label>
                  {!canSaveStock && stockValidationMessage ? <p className="diary-inline-error">{stockValidationMessage}</p> : null}
                  <div className="diary-sheet-form-actions diary-stock-edit-actions">
                    <Button variant="plain" type="button" className="action-button diary-stock-edit-cancel" onClick={() => setStockEditCoffeeId(null)} disabled={savingStock}>
                      CANCELAR
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="action-button diary-stock-edit-save"
                      disabled={savingStock || !canSaveStock}
                      onClick={async () => {
                        if (savingStock || !canSaveStock) return;
                        setSavingStock(true);
                        try {
                          await onUpdatePantryStock(stockEditTarget.item.id, parsedStockTotal, parsedStockRemaining);
                          setStockEditCoffeeId(null);
                        } finally {
                          setSavingStock(false);
                        }
                      }}
                    >
                      {savingStock ? "GUARDANDO..." : "GUARDAR"}
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





