import { useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { type DiaryPeriod } from "../../core/diaryAnalytics";
import { resolveAvatarUrl } from "../../core/avatarUrl";
import { sendEvent } from "../../core/ga4";
import type { BrewStep, TabId } from "../../types";
import { UiIcon } from "../../ui/iconography";
import { Button, Chip, IconButton, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
import { useI18n } from "../../i18n";

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
  brewTimerEnded = false,
  onBrewGoToConfig,
  onBrewGoToConsumptionWhenTimerEnded,
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
  onProfileOpenLanguage,
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
  /** True cuando el temporizador de elaboración acaba de terminar (mostrar Siguiente → consumo). */
  brewTimerEnded?: boolean;
  onBrewGoToConfig?: () => void;
  /** Al pulsar Siguiente cuando el temporizador ha terminado: ir a página de consumo (diario + sheet). */
  onBrewGoToConsumptionWhenTimerEnded?: () => void;
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
  onProfileOpenLanguage: () => void;
  onHistorialClick?: () => void;
  profileSubPanel?: "historial" | "followers" | "following" | "favorites" | "list" | "language" | null;
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
  const { t } = useI18n();
  const [searchFocus, setSearchFocus] = useState(false);
  const [notificationPop, setNotificationPop] = useState(false);
  const [showProfileOptions, setShowProfileOptions] = useState(false);
  const [showDeleteAccountConfirm, setShowDeleteAccountConfirm] = useState(false);
  const [deletingAccount, setDeletingAccount] = useState(false);
  /** Índices de avatares en listMemberPreviews que fallaron al cargar (se muestra placeholder). */
  const [failedTopbarAvatarIndices, setFailedTopbarAvatarIndices] = useState<Set<number>>(new Set());
  const showSearchCancel = Boolean(searchQuery || searchFocus);

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
            <IconButton tone="topbar" className="search-users-back" onClick={onSearchBack} aria-label={t("top.search.back")}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <div className="search-users-field">
            <UiIcon name="search" className="ui-icon search-users-leading-icon" />
            <Input
              id="quick-search"
              variant="search"
              className="search-users-input"
              placeholder={t("top.search.users.placeholder")}
              value={searchQuery}
              onFocus={() => setSearchFocus(true)}
              onBlur={() => setSearchFocus(false)}
              onChange={(event) => onSearchQueryChange(event.target.value)}
              aria-label={t("top.search.users.aria")}
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
              {t("common.cancel")}
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
              <span>{t("top.search.placeholder")}</span>
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
            aria-label={t("top.search.aria")}
          />
          {showSearchBarcodeButton ? (
            <IconButton className="search-coffee-trailing-button" aria-label={t("top.search.scan")} onClick={onSearchBarcodeClick}>
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
          {t("common.cancel")}
        </Button>
        {showSearchCoffeeFilterChips ? (
          <div className="topbar-search-chips" role="tablist" aria-label={t("top.search.filtersAria")}>
            <Chip active={Boolean(searchOriginCount)} onClick={() => onOpenSearchFilter("origen")}>{t("top.filter.country")}{searchOriginCount ? <span className="filter-chip-count">{searchOriginCount}</span> : null}</Chip>
            <Chip active={Boolean(searchSpecialtyCount)} onClick={() => onOpenSearchFilter("especialidad")}>{t("top.filter.specialty")}{searchSpecialtyCount ? <span className="filter-chip-count">{searchSpecialtyCount}</span> : null}</Chip>
            <Chip active={Boolean(searchRoastCount)} onClick={() => onOpenSearchFilter("tueste")}>{t("top.filter.roast")}{searchRoastCount ? <span className="filter-chip-count">{searchRoastCount}</span> : null}</Chip>
            <Chip active={Boolean(searchFormatCount)} onClick={() => onOpenSearchFilter("formato")}>{t("top.filter.format")}{searchFormatCount ? <span className="filter-chip-count">{searchFormatCount}</span> : null}</Chip>
            <Chip active={searchHasRatingFilter} onClick={() => onOpenSearchFilter("nota")}>{t("top.filter.rating")}{searchHasRatingFilter ? <span className="filter-chip-count">1</span> : null}</Chip>
          </div>
        ) : null}
      </header>
    );
  }

  if (activeTab === "crear-cafe" && onBrewCreateCoffeeBack) {
    return (
      <header className={`topbar topbar-home topbar-brew topbar-crear-cafe topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
        <div className="topbar-slot">
          <IconButton tone="topbar" onClick={onBrewCreateCoffeeBack} aria-label={t("common.back")}>
            <UiIcon name="arrow-left" className="ui-icon" />
          </IconButton>
        </div>
        <h1 className="title title-upper topbar-title-center">{t("top.createCoffee")}</h1>
        <div className="topbar-slot topbar-slot-end" />
      </header>
    );
  }

  if (activeTab === "brewlab" || activeTab === "selecciona-cafe") {
    if ((brewSelectCoffeePageOpen || activeTab === "selecciona-cafe") && onBrewSelectCoffeeBack) {
      return (
        <header className={`topbar topbar-home topbar-brew topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
          <div className="topbar-slot">
            <IconButton tone="topbar" onClick={onBrewSelectCoffeeBack} aria-label={t("common.back")}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <h1 className="title title-upper topbar-title-center">{t("top.selectCoffee")}</h1>
          <div className="topbar-slot topbar-slot-end" />
        </header>
      );
    }
    if (brewCreateCoffeeOpen) {
      return (
        <header className={`topbar topbar-home topbar-brew topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
          <div className="topbar-slot">
            <IconButton tone="topbar" onClick={onBrewCreateCoffeeBack} aria-label={t("top.search.back")}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <h1 className="title title-upper topbar-title-center">{t("top.createCoffee").toUpperCase()}</h1>
          <div className="topbar-slot topbar-slot-end">
            <Button
              variant="plain"
              type="button"
              className={`topbar-create-coffee-save ${!brewCreateCoffeeFormValid || brewCreateCoffeeSaving ? "is-disabled" : ""}`.trim()}
              onClick={() => (brewCreateCoffeeFormValid && !brewCreateCoffeeSaving ? onBrewCreateCoffeeSave() : undefined)}
              disabled={!brewCreateCoffeeFormValid || brewCreateCoffeeSaving}
              aria-label={t("top.saveLabel")}
            >
              {brewCreateCoffeeSaving ? t("common.saving") : t("common.save")}
            </Button>
          </div>
        </header>
      );
    }
    return (
      <header className={`topbar topbar-home topbar-brew topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
        <div className="topbar-slot">
          {brewStep !== "method" ? (
            <IconButton tone="topbar" onClick={onBrewBack} aria-label={t("top.search.back")}>
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
              aria-label={brewTimerEnabled ? t("top.goBrewing") : t("top.goResult")}
              disabled={!brewCanGoToConfig}
              className={!brewCanGoToConfig ? "topbar-brew-next-inactive" : undefined}
            >
              <UiIcon name="arrow-right" className="ui-icon" />
            </IconButton>
          ) : brewStep === "brewing" && brewTimerEnded && onBrewGoToConsumptionWhenTimerEnded ? (
            <IconButton
              tone="topbar"
              onClick={onBrewGoToConsumptionWhenTimerEnded}
              aria-label={t("top.nextConsumption")}
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
              {brewResultSaving ? t("common.saving") : t("common.save")}
            </button>
          ) : null}
        </div>
      </header>
    );
  }

  if (activeTab === "diary") {
    if (diarySubView === "cafes-probados") return null;
    const canNext = canDiaryGoNext === true;
    const arrowLabelPrev = diaryPeriod === "30d" ? t("top.prevMonth") : t("top.prev");
    const arrowLabelNext = diaryPeriod === "30d" ? t("top.nextMonth") : t("top.next");
    return (
      <header className={`topbar topbar-centered topbar-home topbar-diary ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
        <div className="topbar-slot diary-topbar-date-slot">
          <div className="diary-period-chip diary-period-chip-with-arrows" role="group" aria-label={t("top.selectPeriod")}>
            <button type="button" className="diary-chip-arrow" onClick={(e) => { e.stopPropagation(); onDiaryPrev(); }} aria-label={arrowLabelPrev}>
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
            <button type="button" className="diary-chip-date" onClick={(e) => { e.stopPropagation(); onDiaryOpenPeriodSelector(); }} aria-label={t("top.selectPeriod")}>
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
        <h1 className="title title-upper topbar-title-center">{t("top.myDiary")}</h1>
        <div className="topbar-slot topbar-slot-end diary-topbar-end-spacer" aria-hidden="true" />
      </header>
    );
  }

  if (activeTab === "profile") {
    if (profileSubPanel === "historial" || profileSubPanel === "followers" || profileSubPanel === "following" || profileSubPanel === "favorites" || profileSubPanel === "list" || profileSubPanel === "language") {
      const sectionTitle = profileSubPanel === "historial"
        ? t("top.history")
        : profileSubPanel === "followers"
          ? t("top.followers")
          : profileSubPanel === "following"
            ? t("top.following")
            : profileSubPanel === "favorites"
              ? t("top.favorites")
              : profileSubPanel === "language"
                ? t("language.title").toUpperCase()
                : (profileListName ?? t("top.listDefault")).toUpperCase();
      return (
        <header className={`topbar topbar-centered topbar-home topbar-historial ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`} dir="ltr">
          <div className="topbar-slot topbar-slot-back">
            <IconButton tone="topbar" aria-label={t("common.back")} onClick={onHistorialBack}>
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
                aria-label={t("top.joinThisList")}
              >
                {t("top.join")}
              </Button>
            ) : profileSubPanel === "list" ? (
              <>
                {onOpenListOptionsSheet && typeof listMemberCount === "number" && Array.isArray(listMemberPreviews) && listMemberPreviews.length > 0 ? (
                  <button
                    type="button"
                    className="topbar-list-members-btn"
                    onClick={onOpenListOptionsSheet}
                    aria-label={t("top.listOptions")}
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
                    aria-label={t("top.listOptions")}
                  >
                    <UiIcon name="person_add" className="topbar-share-list-icon" />
                  </button>
                ) : null}
                {onOpenListOptionsSheet && !(typeof listMemberCount === "number" && Array.isArray(listMemberPreviews) && listMemberPreviews.length > 0) ? (
                  <IconButton tone="topbar" className="topbar-list-options-btn" aria-label={t("top.listOptions")} onClick={onOpenListOptionsSheet}>
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
              <IconButton tone="topbar" aria-label={t("top.searchUsers")} onClick={onHomeSearchUsers}>
                <UiIcon name="search" className="ui-icon" />
              </IconButton>
            ) : null}
          </div>
          <h1 className="title title-upper topbar-title-center">{t("top.profile")}</h1>
          <div className="topbar-slot topbar-slot-end">
            {profileMenuEnabled ? (
              <IconButton tone="menu" aria-label={t("top.profileOptions")} onClick={() => { sendEvent("modal_open", { modal_id: "profile_options" }); setShowProfileOptions(true); }}>
                <UiIcon name="more" className="ui-icon" />
              </IconButton>
            ) : null}
          </div>
        </header>
        {showProfileOptions && profileMenuEnabled && typeof document !== "undefined"
          ? createPortal(
              <SheetOverlay className="profile-topbar-options-overlay" role="dialog" aria-modal="true" aria-label={t("top.profileOptions")} onDismiss={() => { sendEvent("modal_close", { modal_id: "profile_options" }); setShowProfileOptions(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "profile_options" }); setShowProfileOptions(false); }}>
                <SheetCard className="diary-sheet diary-sheet-pantry-options profile-topbar-options-sheet" onClick={(event) => event.stopPropagation()}>
                  <SheetHandle aria-hidden="true" />
                  <div className="diary-sheet-list list-options-general-wrap">
                    <h3 className="create-list-privacy-subtitle">{t("top.general")}</h3>
                    <div className="list-options-general-card">
                      {onHistorialClick ? (
                        <Button
                          variant="plain"
                          className="list-options-page-action"
                          onClick={() => {
                            sendEvent("modal_close", { modal_id: "profile_options" });
                            setShowProfileOptions(false);
                            onHistorialClick();
                          }}
                        >
                          <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">history</span>
                          <span>{t("top.coffeesConsumed")}</span>
                          <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                        </Button>
                      ) : null}
                    </div>
                    <div className="list-options-page-section list-options-section-spaced">
                      <h3 className="create-list-privacy-subtitle">{t("top.account")}</h3>
                      <div className="list-options-general-card">
                      <Button
                        variant="plain"
                        className="list-options-page-action"
                        onClick={() => {
                          sendEvent("modal_close", { modal_id: "profile_options" });
                          setShowProfileOptions(false);
                          onProfileOpenEdit();
                        }}
                      >
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">edit</span>
                        <span>{t("top.editProfile")}</span>
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                      </Button>
                      <Button
                        variant="plain"
                        className="list-options-page-action"
                        onClick={() => {
                          sendEvent("modal_close", { modal_id: "profile_options" });
                          setShowProfileOptions(false);
                          onProfileOpenLanguage();
                        }}
                      >
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">language</span>
                        <span>{t("top.language")}</span>
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                      </Button>
                      <Button
                        variant="plain"
                        className="list-options-page-action"
                        onClick={() => {
                          sendEvent("modal_close", { modal_id: "profile_options" });
                          setShowProfileOptions(false);
                          sendEvent("modal_open", { modal_id: "delete_confirm_account" });
                          setShowDeleteAccountConfirm(true);
                        }}
                      >
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">person_remove</span>
                        <span>{t("top.deleteAccountData")}</span>
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
                      </Button>
                      <Button
                        variant="plain"
                        className="list-options-page-action"
                        onClick={() => {
                          sendEvent("modal_close", { modal_id: "profile_options" });
                          setShowProfileOptions(false);
                          onProfileSignOut();
                        }}
                      >
                        <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">logout</span>
                        <span>{t("top.signOut")}</span>
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
              <SheetOverlay role="dialog" aria-modal="true" aria-label={t("top.deleteAccountAria")} onDismiss={() => { sendEvent("modal_close", { modal_id: "delete_confirm_account" }); setShowDeleteAccountConfirm(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "delete_confirm_account" }); setShowDeleteAccountConfirm(false); }}>
                <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
                  <SheetHandle aria-hidden="true" />
                  <div className="diary-delete-confirm-body">
                    <h2 className="diary-delete-confirm-title">{t("top.deleteAccountData")}</h2>
                    <p className="diary-delete-confirm-text">
                      {t("top.deleteAccountText")}
                    </p>
                    <div className="diary-delete-confirm-actions">
                      <Button
                        variant="plain"
                        type="button"
                        className="diary-delete-confirm-cancel"
                        disabled={deletingAccount}
                        onClick={() => { sendEvent("modal_close", { modal_id: "delete_confirm_account" }); setShowDeleteAccountConfirm(false); }}
                      >
                        {t("common.cancel")}
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
                            sendEvent("modal_close", { modal_id: "delete_confirm_account" });
                            setShowDeleteAccountConfirm(false);
                          } finally {
                            setDeletingAccount(false);
                          }
                        }}
                      >
                        {deletingAccount ? t("top.processing") : t("top.delete")}
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
          <IconButton tone="topbar" onClick={onCoffeeBack} aria-label={t("common.back")}>
            <UiIcon name="arrow-left" className="ui-icon" />
          </IconButton>
        </div>
        <h1 className="title title-upper topbar-title-center">{t("top.coffee")}</h1>
        <div className="topbar-slot topbar-slot-end">
          <IconButton tone="topbar" className={`coffee-topbar-favorite ${coffeeTopbarFavoriteActive ? "is-active" : ""}`.trim()} aria-label={coffeeTopbarFavoriteActive ? t("top.removeFromLists") : t("top.addToLists")} onClick={onCoffeeTopbarToggleFavorite}>
            <UiIcon name={coffeeTopbarFavoriteActive ? "list-alt-check" : "list-alt-add"} className="ui-icon" />
          </IconButton>
          <IconButton tone="topbar" className={coffeeTopbarStockActive ? "is-active" : ""} aria-label={t("top.addStock")} onClick={onCoffeeTopbarOpenStock}>
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
        <IconButton tone="topbar" className={notificationPop ? "notification-pop" : ""} aria-label={t("top.notifications")} onClick={onHomeNotifications}>
          <UiIcon name="notifications" className="ui-icon" />
          {showNotificationsBadge ? <span className="badge-dot" aria-hidden="true" /> : null}
        </IconButton>
      </div>
    </header>
  );
}
