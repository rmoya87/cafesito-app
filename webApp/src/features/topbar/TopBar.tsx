import { useEffect, useState } from "react";
import type { BrewStep, TabId } from "../../types";
import { UiIcon } from "../../ui/iconography";
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
        <header className={`topbar topbar-search-users ${showSearchCancel ? "has-cancel" : ""} ${scrolled ? "topbar-scrolled" : ""}`.trim()}>
          <div className="topbar-slot">
            <button className="icon-button topbar-icon-button search-users-back" type="button" onClick={onSearchBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
          </div>
          <div className="search-users-field">
            <UiIcon name="search" className="ui-icon search-users-leading-icon" />
            <input
              id="quick-search"
              className="search-wide search-input-standard search-users-input"
              placeholder="Buscar usuarios..."
              value={searchQuery}
              onFocus={() => setSearchFocus(true)}
              onBlur={() => setSearchFocus(false)}
              onChange={(event) => onSearchQueryChange(event.target.value)}
              aria-label="Buscar usuarios"
            />
          </div>
          <div className="topbar-slot topbar-slot-end search-users-cancel-slot">
            <button
              className={`text-button search-cancel-button ${showSearchCancel ? "is-visible" : ""}`}
              type="button"
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
            </button>
          </div>
        </header>
      );
    }

    return (
      <header className={`topbar topbar-search ${showSearchCancel ? "has-cancel" : ""} ${scrolled ? "topbar-scrolled" : ""}`.trim()}>
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
          <input
            id="quick-search"
            className="search-wide search-input-standard search-coffee-input"
            placeholder=""
            value={searchQuery}
            onFocus={() => setSearchFocus(true)}
            onBlur={() => setSearchFocus(false)}
            onChange={(event) => onSearchQueryChange(event.target.value)}
            aria-label="Busqueda"
          />
          {showSearchBarcodeButton ? (
            <button type="button" className="icon-button search-coffee-trailing-button" aria-label="Escanear codigo" onClick={onSearchBarcodeClick}>
              <UiIcon name="barcode" className="ui-icon" />
            </button>
          ) : null}
        </div>
        <button
          className={`text-button search-cancel-button ${showSearchCancel ? "is-visible" : ""}`}
          type="button"
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
        </button>
        {showSearchCoffeeFilterChips ? (
          <div className="topbar-search-chips" role="tablist" aria-label="Filtros de busqueda">
            <button type="button" className={`filter-chip ${searchOriginCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("origen")}>
              PAIS
              {searchOriginCount ? <span className="filter-chip-count">{searchOriginCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchSpecialtyCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("especialidad")}>
              ESPECIALIDAD
              {searchSpecialtyCount ? <span className="filter-chip-count">{searchSpecialtyCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchRoastCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("tueste")}>
              TUESTE
              {searchRoastCount ? <span className="filter-chip-count">{searchRoastCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchFormatCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("formato")}>
              FORMATO
              {searchFormatCount ? <span className="filter-chip-count">{searchFormatCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchHasRatingFilter ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("nota")}>
              NOTA
              {searchHasRatingFilter ? <span className="filter-chip-count">1</span> : null}
            </button>
          </div>
        ) : null}
      </header>
    );
  }

  if (activeTab === "brewlab") {
    if (brewCreateCoffeeOpen) {
      return (
        <header className={`topbar topbar-timeline topbar-centered ${scrolled ? "topbar-scrolled" : ""}`}>
          <div className="topbar-slot">
            <button className="icon-button topbar-icon-button" type="button" onClick={onBrewCreateCoffeeBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
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
            <button className="icon-button topbar-icon-button" type="button" onClick={onBrewBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
          ) : null}
        </div>
        <h1 className="title title-upper topbar-title-center">{brewStepTitle}</h1>
        <div className="topbar-slot topbar-slot-end">
          {brewStep === "config" ? (
            <button className="icon-button topbar-icon-button" type="button" onClick={onBrewForward} aria-label="Empezar">
              <UiIcon name="arrow-right" className="ui-icon" />
            </button>
          ) : null}
        </div>
      </header>
    );
  }

  if (activeTab === "diary") {
    const periodLabel = diaryPeriod === "hoy" ? "HOY" : diaryPeriod === "7d" ? "SEMANA" : "MES";
    return (
      <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-slot" />
        <h1 className="title title-upper topbar-title-center">MI DIARIO</h1>
        <div className="topbar-slot topbar-slot-end">
          <button className="icon-button topbar-icon-button diary-topbar-add" type="button" aria-label="Agregar" onClick={onDiaryOpenQuickActions}>
            <UiIcon name="add" className="ui-icon" />
          </button>
          <button className="chip-button diary-period-chip" type="button" onClick={onDiaryOpenPeriodSelector}>{periodLabel}</button>
        </div>
      </header>
    );
  }

  if (activeTab === "profile") {
    return (
      <>
        <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""}`}>
          <div className="topbar-slot" />
          <h1 className="title title-upper topbar-title-center">PERFIL</h1>
          <div className="topbar-slot topbar-slot-end">
            {profileMenuEnabled ? (
              <button
                className="icon-button profile-topbar-menu-trigger"
                type="button"
                aria-label="Opciones de perfil"
                onClick={() => setShowProfileOptions(true)}
              >
                <UiIcon name="more" className="ui-icon" />
              </button>
            ) : null}
          </div>
        </header>
        {showProfileOptions && profileMenuEnabled ? (
          <div className="sheet-overlay profile-topbar-options-overlay" role="dialog" aria-modal="true" aria-label="Opciones de perfil" onClick={() => setShowProfileOptions(false)}>
            <div className="sheet-card profile-topbar-options-sheet" onClick={(event) => event.stopPropagation()}>
              <div className="sheet-handle" aria-hidden="true" />
              <div className="comment-action-list">
                <p className="comment-action-title">OPCIONES</p>
                <button
                  type="button"
                  className="comment-action-button"
                  onClick={() => {
                    setShowProfileOptions(false);
                    onProfileOpenEdit();
                  }}
                >
                  <UiIcon name="edit" className="ui-icon" />
                  <span>Editar perfil</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </button>
                <button
                  type="button"
                  className="comment-action-button is-danger"
                  onClick={() => {
                    setShowProfileOptions(false);
                    onProfileSignOut();
                  }}
                >
                  <UiIcon name="close" className="ui-icon" />
                  <span>Cerrar sesión</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </>
    );
  }

  if (activeTab === "coffee") {
    return (
      <header className={`topbar topbar-timeline topbar-centered topbar-coffee ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-slot">
          <button className="icon-button topbar-icon-button" type="button" onClick={onCoffeeBack} aria-label="Volver">
            <UiIcon name="arrow-left" className="ui-icon" />
          </button>
        </div>
        <h1 className="title title-upper topbar-title-center">CAFE</h1>
        <div className="topbar-slot topbar-slot-end">
          <button
            className={`icon-button topbar-icon-button ${coffeeTopbarFavoriteActive ? "is-active" : ""}`.trim()}
            type="button"
            aria-label={coffeeTopbarFavoriteActive ? "Quitar de favoritos" : "Guardar en favoritos"}
            onClick={onCoffeeTopbarToggleFavorite}
          >
            <UiIcon name={coffeeTopbarFavoriteActive ? "favorite-filled" : "favorite"} className="ui-icon" />
          </button>
          <button
            className={`icon-button topbar-icon-button ${coffeeTopbarStockActive ? "is-active" : ""}`.trim()}
            type="button"
            aria-label="Añadir a stock"
            onClick={onCoffeeTopbarOpenStock}
          >
            <UiIcon name="stock" className="ui-icon" />
          </button>
        </div>
      </header>
    );
  }

  return (
    <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""}`}>
      <div className="topbar-slot">
        <button className="icon-button topbar-icon-button" type="button" aria-label="Buscar Usuarios" onClick={onTimelineSearchUsers}>
          <UiIcon name="search" className="ui-icon" />
        </button>
      </div>
      <h1 className="title title-upper topbar-title-center topbar-brand-title">CAFESITO</h1>
      <div className="topbar-slot topbar-slot-end">
        <button
          className={`icon-button topbar-icon-button ${notificationPop ? "notification-pop" : ""}`}
          type="button"
          aria-label="Notificaciones"
          onClick={onTimelineNotifications}
        >
          <UiIcon name="notifications" className="ui-icon" />
          {showNotificationsBadge ? <span className="badge-dot" aria-hidden="true" /> : null}
        </button>
      </div>
    </header>
  );
}


