import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { type DiaryPeriod } from "../../core/diaryAnalytics";
import { resolveAvatarUrl } from "../../core/avatarUrl";
import type { BrewStep, TabId } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { Button, Chip, IconButton, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";

export function TopBar({
  activeTab,
  searchQuery,
  searchMode,
  onSearchQueryChange,
  onSearchCancel,
  onSearchBarcodeClick,
  showSearchBarcodeButton,
  showSearchCoffeeFilterChips,
  searchOriginCount,
  searchSpecialtyCount,
  searchRoastCount,
  searchFormatCount,
  searchHasRatingFilter,
  onOpenSearchFilter,
  onSearchBack,
  showNotificationsBadge,
  onHomeSearchUsers,
  onHomeNotifications,
  diaryPeriod,
  diaryDateLabel,
  diarySelectedDate,
  diarySelectedMonth,
  diaryTodayStr,
  currentMonthKey,
  onDiaryPrev,
  onDiaryNext,
  onDiaryOpenPeriodSelector,
  canDiaryGoNext = false,
  scrolled,
  hidden = false,
  brewStep,
  brewStepTitle,
  onBrewBack,
  onBrewForward,
  brewCanGoToConfig,
  brewTimerEnabled = false,
  onBrewGoToConfig,
  onBrewResultSave,
  brewResultCanSave,
  brewResultSaving,
  brewResultShowGuardar,
  brewSelectCoffeePageOpen = false,
  onBrewSelectCoffeeBack,
  brewCreateCoffeeOpen,
  onBrewCreateCoffeeBack,
  onBrewCreateCoffeeSave,
  brewCreateCoffeeFormValid,
  brewCreateCoffeeSaving,
  onProfileSignOut,
  onProfileDeleteAccount,
  profileMenuEnabled,
  onProfileOpenEdit,
  onHistorialClick,
  profileSubPanel,
  profileListName,
  onOpenListOptionsSheet,
  showShareListButton = false,
  listMemberCount,
  listMemberPreviews,
  showJoinPublicListButton = false,
  onJoinPublicList,
  onHistorialBack,
  onCoffeeBack,
  coffeeTopbarFavoriteActive,
  coffeeTopbarStockActive,
  onCoffeeTopbarToggleFavorite,
  onCoffeeTopbarOpenStock,
  diarySubView = null,
}: {
  activeTab: TabId;
  /** Cuando es "cafes-probados" no se muestra la barra de Mi diario (mapa a pantalla completa). */
  diarySubView?: "cafes-probados" | null;
  searchQuery: string;
  searchMode: "coffees" | "users";
  onSearchQueryChange: (value: string) => void;
  onSearchCancel: () => void;
  onSearchBarcodeClick: () => void;
  showSearchBarcodeButton: boolean;
  showSearchCoffeeFilterChips: boolean;
  searchOriginCount: number;
  searchSpecialtyCount: number;
  searchRoastCount: number;
  searchFormatCount: number;
  searchHasRatingFilter: boolean;
  onOpenSearchFilter: (filter: "origen" | "tueste" | "especialidad" | "formato" | "nota") => void;
  onSearchBack: () => void;
  showNotificationsBadge: boolean;
  onHomeSearchUsers: () => void;
  onHomeNotifications: () => void;
  diaryPeriod: DiaryPeriod;
  diaryDateLabel: string;
  diarySelectedDate: string;
  diarySelectedMonth: string;
  diaryTodayStr: string;
  currentMonthKey: string;
  onDiaryPrev: () => void;
  onDiaryNext: () => void;
  onDiaryOpenPeriodSelector: () => void;
  /** Si false, no se muestra flecha siguiente (semana/mes). */
  canDiaryGoNext?: boolean;
  scrolled: boolean;
  hidden?: boolean;
  brewStep: BrewStep;
  brewStepTitle: string;
  onBrewBack: () => void;
  onBrewForward: () => void;
  brewCanGoToConfig?: boolean;
  brewTimerEnabled?: boolean;
  onBrewGoToConfig?: () => void;
  onBrewResultSave: () => void;
  brewResultCanSave: boolean;
  brewResultSaving: boolean;
  brewResultShowGuardar: boolean;
  brewSelectCoffeePageOpen?: boolean;
  onBrewSelectCoffeeBack?: () => void;
  brewCreateCoffeeOpen: boolean;
  onBrewCreateCoffeeBack: () => void;
  onBrewCreateCoffeeSave: () => void;
  brewCreateCoffeeFormValid: boolean;
  brewCreateCoffeeSaving: boolean;
  onProfileSignOut: () => void;
  onProfileDeleteAccount: () => Promise<void> | void;
  profileMenuEnabled: boolean;
  onProfileOpenEdit: () => void;
  onHistorialClick?: () => void;
  profileSubPanel?: "historial" | "followers" | "following" | "favorites" | "list" | null;
  profileListName?: string;
  /** Al pulsar el menú de 3 puntos en la vista de detalle de una lista (solo cuando profileSubPanel === "list"). */
  onOpenListOptionsSheet?: () => void;
  /** Mostrar botón invitar (person_add) o avatares + número cuando la lista es propia; al pulsar abre opciones de lista. */
  showShareListButton?: boolean;
  /** Si lista es pública o por invitación: número de miembros (sustituye el botón por avatares + número). */
  listMemberCount?: number;
  /** Hasta 3 avatares para mostrar apilados (mismo orden que listMemberCount). */
  listMemberPreviews?: Array<{ avatar_url: string | null }>;
  /** Mostrar botón "Unirse" cuando la lista es pública o por invitación y no eres miembro. */
  showJoinPublicListButton?: boolean;
  /** Al pulsar "Unirse" en una lista pública/invitación ajena. */
  onJoinPublicList?: () => void | Promise<void>;
  onHistorialBack?: () => void;
  onCoffeeBack: () => void;
  coffeeTopbarFavoriteActive: boolean;
  coffeeTopbarStockActive: boolean;
  onCoffeeTopbarToggleFavorite: () => void;
  onCoffeeTopbarOpenStock: () => void;
}) {
  const [searchFocus, setSearchFocus] = useState(false);
  const [searchHintWord, setSearchHintWord] = useState<"marca" | "cafe">("marca");
  const [notificationPop, setNotificationPop] = useState(false);
  const [showProfileOptions, setShowProfileOptions] = useState(false);
  const [showDeleteAccountConfirm, setShowDeleteAccountConfirm] = useState(false);
  const [deletingAccount, setDeletingAccount] = useState(false);
  /** Índices de avatares en listMemberPreviews que fallaron al cargar (se muestra placeholder). */
  const [failedTopbarAvatarIndices, setFailedTopbarAvatarIndices] = useState<Set<number>>(new Set());
  const showSearchCancel = Boolean(searchQuery || searchFocus);

  useEffect(() => {
    if (activeTab !== "search") return;
    const interval = window.setInterval(() => {
      setSearchHintWord((prev) => (prev === "marca" ? "cafe" : "marca"));
    }, 2200);
    return () => window.clearInterval(interval);
  }, [activeTab]);

  useEffect(() => {
    if (!showNotificationsBadge || activeTab !== "home") return;
    setNotificationPop(true);
    const id = window.setTimeout(() => setNotificationPop(false), 340);
    return () => window.clearTimeout(id);
  }, [activeTab, showNotificationsBadge]);

  useEffect(() => {
    setShowProfileOptions(false);
    setShowDeleteAccountConfirm(false);
  }, [activeTab]);

  if (activeTab === "search") {
    if (searchMode === "users") {
      return (
        <header className={`topbar topbar-search-users ${showSearchCancel ? "has-cancel" : ""} ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
          <div className="topbar-slot">
            <IconButton tone="topbar" className="search-users-back" onClick={onSearchBack} aria-label="Atrás">
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <div className="search-users-field">
            <UiIcon name="search" className="ui-icon search-users-leading-icon" />
            <Input
              id="quick-search"
              variant="search"
              className="search-users-input"
              placeholder="Buscar usuarios..."
              value={searchQuery}
              onFocus={() => setSearchFocus(true)}
              onBlur={() => setSearchFocus(false)}
              onChange={(event) => onSearchQueryChange(event.target.value)}
              aria-label="Buscar usuarios"
            />
          </div>
          <div className="topbar-slot topbar-slot-end search-users-cancel-slot">
            <Button
              variant="text"
              className={`search-cancel-button ${showSearchCancel ? "is-visible" : ""}`}
              onClick={() => {
                onSearchCancel();
                setSearchFocus(false);
                const activeElement = document.activeElement;
                if (activeElement instanceof HTMLElement) activeElement.blur();
              }}
              aria-hidden={!showSearchCancel}
              tabIndex={showSearchCancel ? 0 : -1}
            >
              Cancelar
            </Button>
          </div>
        </header>
      );
    }

    return (
      <header className={`topbar topbar-search ${showSearchCancel ? "has-cancel" : ""} ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
        <div className="search-coffee-field">
          <UiIcon name="search" className="ui-icon search-coffee-leading-icon" />
          {!searchQuery && !searchFocus ? (
            <div className="search-coffee-placeholder" aria-hidden="true">
              <span>Busca </span>
              <span key={searchHintWord} className="search-coffee-placeholder-word">
                {searchHintWord}
              </span>
            </div>
          ) : null}
          <Input
            id="quick-search"
            variant="search"
            className="search-coffee-input"
            placeholder=""
            value={searchQuery}
            onFocus={() => setSearchFocus(true)}
            onBlur={() => setSearchFocus(false)}
            onChange={(event) => onSearchQueryChange(event.target.value)}
            aria-label="Busqueda"
          />
          {showSearchBarcodeButton ? (
            <IconButton className="search-coffee-trailing-button" aria-label="Escanear código" onClick={onSearchBarcodeClick}>
              <UiIcon name="barcode" className="ui-icon" />
            </IconButton>
          ) : null}
        </div>
        <Button
          variant="text"
          className={`search-cancel-button ${showSearchCancel ? "is-visible" : ""}`}
          onClick={() => {
            onSearchCancel();
            setSearchFocus(false);
            const activeElement = document.activeElement;
            if (activeElement instanceof HTMLElement) activeElement.blur();
          }}
          aria-hidden={!showSearchCancel}
          tabIndex={showSearchCancel ? 0 : -1}
        >
          Cancelar
        </Button>
        {showSearchCoffeeFilterChips ? (
          <div className="topbar-search-chips" role="tablist" aria-label="Filtros de busqueda">
            <Chip active={Boolean(searchOriginCount)} onClick={() => onOpenSearchFilter("origen")}>PAIS{searchOriginCount ? <span className="filter-chip-count">{searchOriginCount}</span> : null}</Chip>
            <Chip active={Boolean(searchSpecialtyCount)} onClick={() => onOpenSearchFilter("especialidad")}>ESPECIALIDAD{searchSpecialtyCount ? <span className="filter-chip-count">{searchSpecialtyCount}</span> : null}</Chip>
            <Chip active={Boolean(searchRoastCount)} onClick={() => onOpenSearchFilter("tueste")}>TUESTE{searchRoastCount ? <span className="filter-chip-count">{searchRoastCount}</span> : null}</Chip>
            <Chip active={Boolean(searchFormatCount)} onClick={() => onOpenSearchFilter("formato")}>FORMATO{searchFormatCount ? <span className="filter-chip-count">{searchFormatCount}</span> : null}</Chip>
            <Chip active={searchHasRatingFilter} onClick={() => onOpenSearchFilter("nota")}>NOTA{searchHasRatingFilter ? <span className="filter-chip-count">1</span> : null}</Chip>
          </div>
        ) : null}
      </header>
    );
  }

  if (activeTab === "brewlab") {
    if (brewSelectCoffeePageOpen && onBrewSelectCoffeeBack) {
      return (
        <header className={`topbar topbar-home topbar-brew topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
          <div className="topbar-slot">
            <IconButton tone="topbar" onClick={onBrewSelectCoffeeBack} aria-label="Volver">
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <h1 className="title title-upper topbar-title-center">Selecciona café</h1>
          <div className="topbar-slot topbar-slot-end" />
        </header>
      );
    }
    if (brewCreateCoffeeOpen) {
      return (
        <header className={`topbar topbar-home topbar-brew topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
          <div className="topbar-slot">
            <IconButton tone="topbar" onClick={onBrewCreateCoffeeBack} aria-label="Atrás">
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <h1 className="title title-upper topbar-title-center">CREAR CAFE</h1>
          <div className="topbar-slot topbar-slot-end">
            <Button
              variant="plain"
              type="button"
              className={`topbar-create-coffee-save ${!brewCreateCoffeeFormValid || brewCreateCoffeeSaving ? "is-disabled" : ""}`.trim()}
              onClick={() => (brewCreateCoffeeFormValid && !brewCreateCoffeeSaving ? onBrewCreateCoffeeSave() : undefined)}
              disabled={!brewCreateCoffeeFormValid || brewCreateCoffeeSaving}
              aria-label="Guardar"
            >
              {brewCreateCoffeeSaving ? "Guardando..." : "Guardar"}
            </Button>
          </div>
        </header>
      );
    }
    return (
      <header className={`topbar topbar-home topbar-brew topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
        <div className="topbar-slot">
          {brewStep !== "method" ? (
            <IconButton tone="topbar" onClick={onBrewBack} aria-label="Atrás">
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          ) : null}
        </div>
        <h1 className="title title-upper topbar-title-center">{brewStepTitle}</h1>
        <div className="topbar-slot topbar-slot-end">
          {brewStep === "method" ? (
            <IconButton
              tone="topbar"
              onClick={() => {
                if (brewCanGoToConfig && onBrewGoToConfig) onBrewGoToConfig();
              }}
              aria-label={brewTimerEnabled ? "Ir a proceso en curso" : "Ir a resultado"}
              disabled={!brewCanGoToConfig}
              className={!brewCanGoToConfig ? "topbar-brew-next-inactive" : undefined}
            >
              <UiIcon name="arrow-right" className="ui-icon" />
            </IconButton>
          ) : brewResultShowGuardar ? (
            <button
              type="button"
              className="topbar-result-save"
              onClick={onBrewResultSave}
              disabled={!brewResultCanSave || brewResultSaving}
            >
              {brewResultSaving ? "Guardando…" : "Guardar"}
            </button>
          ) : null}
        </div>
      </header>
    );
  }

  if (activeTab === "diary") {
    if (diarySubView === "cafes-probados") return null;
    const canNext = canDiaryGoNext === true;
    const arrowLabelPrev = diaryPeriod === "30d" ? "Mes anterior" : "Anterior";
    const arrowLabelNext = diaryPeriod === "30d" ? "Mes siguiente" : "Siguiente";
    return (
      <header className={`topbar topbar-centered topbar-home topbar-diary ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
        <div className="topbar-slot diary-topbar-date-slot">
          <div className="diary-period-chip diary-period-chip-with-arrows" role="group" aria-label="Seleccionar periodo">
            <button type="button" className="diary-chip-arrow" onClick={(e) => { e.stopPropagation(); onDiaryPrev(); }} aria-label={arrowLabelPrev}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
            <button type="button" className="diary-chip-date" onClick={(e) => { e.stopPropagation(); onDiaryOpenPeriodSelector(); }} aria-label="Seleccionar periodo">
              {diaryDateLabel}
            </button>
            {canNext ? (
              <button type="button" className="diary-chip-arrow" onClick={(e) => { e.stopPropagation(); onDiaryNext(); }} aria-label={arrowLabelNext}>
                <UiIcon name="arrow-right" className="ui-icon" />
              </button>
            ) : (
              <span className="diary-chip-arrow diary-chip-arrow-placeholder" aria-hidden="true" />
            )}
          </div>
        </div>
        <h1 className="title title-upper topbar-title-center">MI DIARIO</h1>
        <div className="topbar-slot topbar-slot-end diary-topbar-end-spacer" aria-hidden="true" />
      </header>
    );
  }

  if (activeTab === "profile") {
    if (profileSubPanel === "historial" || profileSubPanel === "followers" || profileSubPanel === "following" || profileSubPanel === "favorites" || profileSubPanel === "list") {
      const sectionTitle = profileSubPanel === "historial" ? "HISTORIAL" : profileSubPanel === "followers" ? "SEGUIDORES" : profileSubPanel === "following" ? "SIGUIENDO" : profileSubPanel === "favorites" ? "FAVORITOS" : (profileListName ?? "Lista").toUpperCase();
      return (
        <header className={`topbar topbar-centered topbar-home topbar-historial ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`} dir="ltr">
          <div className="topbar-slot topbar-slot-back">
            <IconButton tone="topbar" aria-label="Volver" onClick={onHistorialBack}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <h1 className="title title-upper topbar-title-center">{sectionTitle}</h1>
          <div className="topbar-slot topbar-slot-end">
            {profileSubPanel === "list" && showJoinPublicListButton && onJoinPublicList ? (
              <Button
                variant="primary"
                className="action-button topbar-join-list-btn"
                onClick={() => void onJoinPublicList()}
                aria-label="Unirse a esta lista"
              >
                Unirse
              </Button>
            ) : profileSubPanel === "list" ? (
              <>
                {onOpenListOptionsSheet && typeof listMemberCount === "number" && Array.isArray(listMemberPreviews) && listMemberPreviews.length > 0 ? (
                  <button
                    type="button"
                    className="topbar-list-members-btn"
                    onClick={onOpenListOptionsSheet}
                    aria-label="Opciones de lista"
                  >
                    <span className="topbar-list-members-count" aria-hidden="true">
                      {listMemberCount}
                    </span>
                    <span className="topbar-list-members-avatars">
                      {listMemberPreviews.slice(0, 3).map((p, i) => {
                        const avatarUrl = resolveAvatarUrl(p.avatar_url);
                        const showImg = avatarUrl && !failedTopbarAvatarIndices.has(i);
                        return (
                          <span key={i} className="topbar-list-members-avatar-wrap">
                            {showImg ? (
                              <img
                                src={avatarUrl}
                                alt=""
                                className="topbar-list-members-avatar"
                                width={28}
                                height={28}
                                loading="lazy"
                                decoding="async"
                                referrerPolicy="no-referrer"
                                crossOrigin="anonymous"
                                onError={() => setFailedTopbarAvatarIndices((prev) => new Set(prev).add(i))}
                              />
                            ) : (
                              <span className="topbar-list-members-avatar topbar-list-members-avatar-placeholder" aria-hidden="true">
                                ?
                              </span>
                            )}
                          </span>
                        );
                      })}
                    </span>
                  </button>
                ) : showShareListButton && onOpenListOptionsSheet ? (
                  <button
                    type="button"
                    className="topbar-share-list-btn"
                    onClick={onOpenListOptionsSheet}
                    aria-label="Opciones de lista"
                  >
                    <UiIcon name="person_add" className="topbar-share-list-icon" />
                  </button>
                ) : null}
                {onOpenListOptionsSheet && !(typeof listMemberCount === "number" && Array.isArray(listMemberPreviews) && listMemberPreviews.length > 0) ? (
                  <IconButton tone="topbar" className="topbar-list-options-btn" aria-label="Opciones de lista" onClick={onOpenListOptionsSheet}>
                    <UiIcon name="more" className="ui-icon" />
                  </IconButton>
                ) : null}
              </>
            ) : null}
          </div>
        </header>
      );
    }
    return (
      <>
        <header className={`topbar topbar-centered topbar-home topbar-profile ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
          <div className="topbar-slot">
            {profileMenuEnabled ? (
              <IconButton tone="topbar" aria-label="Buscar usuarios" onClick={onHomeSearchUsers}>
                <UiIcon name="search" className="ui-icon" />
              </IconButton>
            ) : null}
          </div>
          <h1 className="title title-upper topbar-title-center">PERFIL</h1>
          <div className="topbar-slot topbar-slot-end">
            {profileMenuEnabled ? (
              <IconButton tone="menu" aria-label="Opciones de perfil" onClick={() => setShowProfileOptions(true)}>
                <UiIcon name="more" className="ui-icon" />
              </IconButton>
            ) : null}
          </div>
        </header>
        {showProfileOptions && profileMenuEnabled && typeof document !== "undefined"
          ? createPortal(
              <SheetOverlay className="profile-topbar-options-overlay" role="dialog" aria-modal="true" aria-label="Opciones de perfil" onDismiss={() => setShowProfileOptions(false)} onClick={() => setShowProfileOptions(false)}>
                <SheetCard className="diary-sheet diary-sheet-pantry-options profile-topbar-options-sheet" onClick={(event) => event.stopPropagation()}>
                  <SheetHandle aria-hidden="true" />
                  <div className="diary-sheet-list list-options-general-wrap">
                    <h3 className="create-list-privacy-subtitle">General</h3>
                    <div className="list-options-general-card">
                      {onHistorialClick ? (
                        <Button
                          variant="plain"
                          className="list-options-page-action"
                          onClick={() => {
                            setShowProfileOptions(false);
                            onHistorialClick();
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">history</span>
                          <span>Cafés consumidos</span>
                          <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                        </Button>
                      ) : null}
                    </div>
                    <div className="list-options-page-section list-options-section-spaced">
                      <h3 className="create-list-privacy-subtitle">Cuenta</h3>
                      <div className="list-options-general-card">
                      <Button
                        variant="plain"
                        className="list-options-page-action"
                        onClick={() => {
                          setShowProfileOptions(false);
                          onProfileOpenEdit();
                        }}
                      >
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">edit</span>
                        <span>Editar perfil</span>
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                      </Button>
                      <Button
                        variant="plain"
                        className="list-options-page-action"
                        onClick={() => {
                          setShowProfileOptions(false);
                          setShowDeleteAccountConfirm(true);
                        }}
                      >
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">person_remove</span>
                        <span>Eliminar mi cuenta y mis datos</span>
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                      </Button>
                      <Button
                        variant="plain"
                        className="list-options-page-action"
                        onClick={() => {
                          setShowProfileOptions(false);
                          onProfileSignOut();
                        }}
                      >
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">logout</span>
                        <span>Cerrar sesión</span>
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                      </Button>
                      </div>
                    </div>
                  </div>
                </SheetCard>
              </SheetOverlay>,
              document.body
            )
          : null}
        {showDeleteAccountConfirm && typeof document !== "undefined"
          ? createPortal(
              <SheetOverlay role="dialog" aria-modal="true" aria-label="Eliminar cuenta" onDismiss={() => setShowDeleteAccountConfirm(false)} onClick={() => setShowDeleteAccountConfirm(false)}>
                <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
                  <SheetHandle aria-hidden="true" />
                  <div className="diary-delete-confirm-body">
                    <h2 className="diary-delete-confirm-title">Eliminar mi cuenta y mis datos</h2>
                    <p className="diary-delete-confirm-text">
                      Tu cuenta quedará inactiva durante 30 días y luego se eliminará con todos tus datos. Si vuelves a acceder antes, se cancelará el proceso.
                    </p>
                    <div className="diary-delete-confirm-actions">
                      <Button
                        variant="plain"
                        type="button"
                        className="diary-delete-confirm-cancel"
                        disabled={deletingAccount}
                        onClick={() => setShowDeleteAccountConfirm(false)}
                      >
                        Cancelar
                      </Button>
                      <Button
                        variant="plain"
                        type="button"
                        className="diary-delete-confirm-submit"
                        disabled={deletingAccount}
                        onClick={async () => {
                          if (deletingAccount) return;
                          setDeletingAccount(true);
                          try {
                            await onProfileDeleteAccount();
                            setShowDeleteAccountConfirm(false);
                          } finally {
                            setDeletingAccount(false);
                          }
                        }}
                      >
                        {deletingAccount ? "Procesando..." : "Eliminar"}
                      </Button>
                    </div>
                  </div>
                </SheetCard>
              </SheetOverlay>,
              document.body
            )
          : null}
      </>
    );
  }

  if (activeTab === "coffee") {
    return (
      <header className={`topbar topbar-home topbar-centered topbar-coffee ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
        <div className="topbar-slot">
          <IconButton tone="topbar" onClick={onCoffeeBack} aria-label="Volver">
            <UiIcon name="arrow-left" className="ui-icon" />
          </IconButton>
        </div>
        <h1 className="title title-upper topbar-title-center">CAFE</h1>
        <div className="topbar-slot topbar-slot-end">
          <IconButton tone="topbar" className={`coffee-topbar-favorite ${coffeeTopbarFavoriteActive ? "is-active" : ""}`.trim()} aria-label={coffeeTopbarFavoriteActive ? "Quitar de listas" : "Añadir a listas"} onClick={onCoffeeTopbarToggleFavorite}>
            <UiIcon name={coffeeTopbarFavoriteActive ? "list-alt-check" : "list-alt-add"} className="ui-icon" />
          </IconButton>
          <IconButton tone="topbar" className={coffeeTopbarStockActive ? "is-active" : ""} aria-label="Añadir a stock" onClick={onCoffeeTopbarOpenStock}>
            <UiIcon name="stock" className="ui-icon" />
          </IconButton>
        </div>
      </header>
    );
  }

  return (
    <header className={`topbar topbar-centered topbar-home topbar-home-inicio ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
      <div className="topbar-slot" />
      <h1 className="title title-upper topbar-title-center topbar-brand-title">CAFESITO</h1>
      <div className="topbar-slot topbar-slot-end">
        <IconButton tone="topbar" className={notificationPop ? "notification-pop" : ""} aria-label="Notificaciones" onClick={onHomeNotifications}>
          <UiIcon name="notifications" className="ui-icon" />
          {showNotificationsBadge ? <span className="badge-dot" aria-hidden="true" /> : null}
        </IconButton>
      </div>
    </header>
  );
}
