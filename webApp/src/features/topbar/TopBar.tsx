import { useEffect, useState } from "react";
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
  onTimelineSearchUsers,
  onTimelineNotifications,
  diaryPeriod,
  onDiaryOpenQuickActions,
  onDiaryOpenPeriodSelector,
  scrolled,
  hidden = false,
  brewStep,
  brewStepTitle,
  onBrewBack,
  onBrewForward,
  brewCreateCoffeeOpen,
  onBrewCreateCoffeeBack,
  onProfileSignOut,
  profileMenuEnabled,
  onProfileOpenEdit,
  onCoffeeBack,
  coffeeTopbarFavoriteActive,
  coffeeTopbarStockActive,
  onCoffeeTopbarToggleFavorite,
  onCoffeeTopbarOpenStock
}: {
  activeTab: TabId;
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
  onTimelineSearchUsers: () => void;
  onTimelineNotifications: () => void;
  diaryPeriod: "hoy" | "7d" | "30d";
  onDiaryOpenQuickActions: () => void;
  onDiaryOpenPeriodSelector: () => void;
  scrolled: boolean;
  hidden?: boolean;
  brewStep: BrewStep;
  brewStepTitle: string;
  onBrewBack: () => void;
  onBrewForward: () => void;
  brewCreateCoffeeOpen: boolean;
  onBrewCreateCoffeeBack: () => void;
  onProfileSignOut: () => void;
  profileMenuEnabled: boolean;
  onProfileOpenEdit: () => void;
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
  const showSearchCancel = Boolean(searchQuery || searchFocus);

  useEffect(() => {
    if (activeTab !== "search") return;
    const interval = window.setInterval(() => {
      setSearchHintWord((prev) => (prev === "marca" ? "cafe" : "marca"));
    }, 2200);
    return () => window.clearInterval(interval);
  }, [activeTab]);

  useEffect(() => {
    if (!showNotificationsBadge || activeTab !== "timeline") return;
    setNotificationPop(true);
    const id = window.setTimeout(() => setNotificationPop(false), 340);
    return () => window.clearTimeout(id);
  }, [activeTab, showNotificationsBadge]);

  useEffect(() => {
    setShowProfileOptions(false);
  }, [activeTab]);

  if (activeTab === "search") {
    if (searchMode === "users") {
      return (
        <header className={`topbar topbar-search-users ${showSearchCancel ? "has-cancel" : ""} ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`.trim()}>
          <div className="topbar-slot">
            <IconButton tone="topbar" className="search-users-back" onClick={onSearchBack} aria-label="Atras">
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
            <IconButton className="search-coffee-trailing-button" aria-label="Escanear codigo" onClick={onSearchBarcodeClick}>
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
    if (brewCreateCoffeeOpen) {
      return (
        <header className={`topbar topbar-timeline topbar-centered ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
          <div className="topbar-slot">
            <IconButton tone="topbar" onClick={onBrewCreateCoffeeBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          </div>
          <h1 className="title title-upper topbar-title-center">CREAR CAFE</h1>
          <div className="topbar-slot topbar-slot-end" />
        </header>
      );
    }
    return (
      <header className={`topbar topbar-timeline topbar-centered ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-slot">
          {brewStep !== "method" ? (
            <IconButton tone="topbar" onClick={onBrewBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </IconButton>
          ) : null}
        </div>
        <h1 className="title title-upper topbar-title-center">{brewStepTitle}</h1>
        <div className="topbar-slot topbar-slot-end">
          {brewStep === "config" ? (
            <IconButton tone="topbar" onClick={onBrewForward} aria-label="Empezar">
              <UiIcon name="arrow-right" className="ui-icon" />
            </IconButton>
          ) : null}
        </div>
      </header>
    );
  }

  if (activeTab === "diary") {
    const periodLabel = diaryPeriod === "hoy" ? "HOY" : diaryPeriod === "7d" ? "SEMANA" : "MES";
    return (
      <header className={`topbar topbar-centered topbar-timeline topbar-diary ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
        <div className="topbar-slot">
          <Button variant="chip" className="diary-period-chip" onClick={onDiaryOpenPeriodSelector}>{periodLabel}</Button>
        </div>
        <h1 className="title title-upper topbar-title-center">MI DIARIO</h1>
        <div className="topbar-slot topbar-slot-end">
          <IconButton tone="topbar" className="diary-topbar-add" aria-label="Agregar" onClick={onDiaryOpenQuickActions}>
            <UiIcon name="add" className="ui-icon" />
          </IconButton>
        </div>
      </header>
    );
  }

  if (activeTab === "profile") {
    return (
      <>
        <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
          <div className="topbar-slot" />
          <h1 className="title title-upper topbar-title-center">PERFIL</h1>
          <div className="topbar-slot topbar-slot-end">
            {profileMenuEnabled ? (
              <IconButton tone="menu" aria-label="Opciones de perfil" onClick={() => setShowProfileOptions(true)}>
                <UiIcon name="more" className="ui-icon" />
              </IconButton>
            ) : null}
          </div>
        </header>
        {showProfileOptions && profileMenuEnabled ? (
          <SheetOverlay className="profile-topbar-options-overlay" role="dialog" aria-modal="true" aria-label="Opciones de perfil" onClick={() => setShowProfileOptions(false)}>
            <SheetCard className="profile-topbar-options-sheet" onClick={(event) => event.stopPropagation()}>
              <SheetHandle aria-hidden="true" />
              <div className="comment-action-list">
                <p className="comment-action-title">OPCIONES</p>
                <Button
                  variant="plain"
                  className="comment-action-button"
                  onClick={() => {
                    setShowProfileOptions(false);
                    onProfileOpenEdit();
                  }}
                >
                  <UiIcon name="edit" className="ui-icon" />
                  <span>Editar perfil</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </Button>
                <Button
                  variant="plain"
                  className="comment-action-button is-danger"
                  onClick={() => {
                    setShowProfileOptions(false);
                    onProfileSignOut();
                  }}
                >
                  <UiIcon name="close" className="ui-icon" />
                  <span>Cerrar sesión</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </Button>
              </div>
            </SheetCard>
          </SheetOverlay>
        ) : null}
      </>
    );
  }

  if (activeTab === "coffee") {
    return (
      <header className={`topbar topbar-timeline topbar-centered topbar-coffee ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
        <div className="topbar-slot">
          <IconButton tone="topbar" onClick={onCoffeeBack} aria-label="Volver">
            <UiIcon name="arrow-left" className="ui-icon" />
          </IconButton>
        </div>
        <h1 className="title title-upper topbar-title-center">CAFE</h1>
        <div className="topbar-slot topbar-slot-end">
          <IconButton tone="topbar" className={coffeeTopbarFavoriteActive ? "is-active" : ""} aria-label={coffeeTopbarFavoriteActive ? "Quitar de favoritos" : "Guardar en favoritos"} onClick={onCoffeeTopbarToggleFavorite}>
            <UiIcon name={coffeeTopbarFavoriteActive ? "favorite-filled" : "favorite"} className="ui-icon" />
          </IconButton>
          <IconButton tone="topbar" className={coffeeTopbarStockActive ? "is-active" : ""} aria-label="Añadir a stock" onClick={onCoffeeTopbarOpenStock}>
            <UiIcon name="stock" className="ui-icon" />
          </IconButton>
        </div>
      </header>
    );
  }

  return (
    <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""} ${hidden ? "topbar-is-hidden" : ""}`}>
      <div className="topbar-slot">
        <IconButton tone="topbar" aria-label="Buscar Usuarios" onClick={onTimelineSearchUsers}>
          <UiIcon name="search" className="ui-icon" />
        </IconButton>
      </div>
      <h1 className="title title-upper topbar-title-center topbar-brand-title">CAFESITO</h1>
      <div className="topbar-slot topbar-slot-end">
        <IconButton tone="topbar" className={notificationPop ? "notification-pop" : ""} aria-label="Notificaciones" onClick={onTimelineNotifications}>
          <UiIcon name="notifications" className="ui-icon" />
          {showNotificationsBadge ? <span className="badge-dot" aria-hidden="true" /> : null}
        </IconButton>
      </div>
    </header>
  );
}
