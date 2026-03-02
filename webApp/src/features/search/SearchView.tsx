import { type ReactNode, useCallback, useEffect, useRef, useState } from "react";
import type { CoffeeRow, UserRow } from "../../types";
import { Button, Chip, Input, SheetCard, SheetHandle, SheetOverlay } from "../../ui/components";
import { UiIcon } from "../../ui/iconography";

const SEARCH_PAGE_SIZE = 15;
const SEARCH_LOAD_MORE_AT_INDEX = 12; // al llegar al 12.º elemento, cargar siguientes 15 (nunca todo el resultado de golpe)
export function SearchView({
  mode,
  searchQuery,
  onSearchQueryChange,
  searchFilter,
  onSearchFilterChange,
  selectedOrigins,
  selectedRoasts,
  selectedSpecialties,
  selectedFormats,
  minRating,
  activeFilterType,
  onSetActiveFilterType,
  originOptions,
  roastOptions,
  specialtyOptions,
  formatOptions,
  onToggleOrigin,
  onToggleRoast,
  onToggleSpecialty,
  onToggleFormat,
  onSetMinRating,
  onClearCoffeeFilters,
  coffees,
  users,
  followingIds,
  selectedCoffee,
  onSelectCoffee,
  onSelectUser,
  onToggleFollow,
  focusCoffeeProfile,
  onExitCoffeeFocus,
  sidePanel
}: {
  mode: "coffees" | "users";
  searchQuery: string;
  onSearchQueryChange: (value: string) => void;
  searchFilter: "todo" | "origen" | "nombre";
  onSearchFilterChange: (value: "todo" | "origen" | "nombre") => void;
  selectedOrigins: Set<string>;
  selectedRoasts: Set<string>;
  selectedSpecialties: Set<string>;
  selectedFormats: Set<string>;
  minRating: number;
  activeFilterType: "origen" | "tueste" | "especialidad" | "formato" | "nota" | null;
  onSetActiveFilterType: (filter: "origen" | "tueste" | "especialidad" | "formato" | "nota" | null) => void;
  originOptions: string[];
  roastOptions: string[];
  specialtyOptions: string[];
  formatOptions: string[];
  onToggleOrigin: (value: string) => void;
  onToggleRoast: (value: string) => void;
  onToggleSpecialty: (value: string) => void;
  onToggleFormat: (value: string) => void;
  onSetMinRating: (value: number) => void;
  onClearCoffeeFilters: () => void;
  coffees: CoffeeRow[];
  users: UserRow[];
  followingIds: Set<number>;
  selectedCoffee: CoffeeRow | null;
  onSelectCoffee: (coffeeId: string) => void;
  onSelectUser: (userId: number) => void;
  onToggleFollow: (targetUserId: number) => void;
  focusCoffeeProfile: boolean;
  onExitCoffeeFocus: () => void;
  sidePanel?: ReactNode;
}) {
  const selectedCoffeeRef = useRef<HTMLElement | null>(null);
  const loadMoreSentinelRef = useRef<HTMLDivElement | null>(null);
  const [visibleCount, setVisibleCount] = useState(SEARCH_PAGE_SIZE);

  useEffect(() => {
    setVisibleCount(SEARCH_PAGE_SIZE);
  }, [mode, searchQuery]);

  // Carga progresiva: solo sumar 15 cada vez que el centinela se ve (nunca mostrar todo el resultado de golpe)
  useEffect(() => {
    const sentinel = loadMoreSentinelRef.current;
    const listLength = mode === "coffees" ? coffees.length : users.length;
    if (!sentinel || visibleCount >= listLength) return;
    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries[0]?.isIntersecting) return;
        setVisibleCount((prev) => prev + SEARCH_PAGE_SIZE); // siempre +15; el slice(0, visibleCount) no pasará de listLength
      },
      { root: null, rootMargin: "100px", threshold: 0 }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [mode, coffees.length, users.length, visibleCount]);

  const [recentSearches, setRecentSearches] = useState<string[]>(() => {
    if (typeof window === "undefined") return [];
    try {
      const raw = window.localStorage.getItem("cafesito:search:recent");
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string").slice(0, 10) : [];
    } catch {
      return [];
    }
  });

  const saveRecentSearch = useCallback((term: string) => {
    const cleaned = term.trim();
    if (!cleaned) return;
    setRecentSearches((prev) => [cleaned, ...prev.filter((item) => item.toLowerCase() !== cleaned.toLowerCase())].slice(0, 10));
  }, []);

  const clearRecentSearches = useCallback(() => setRecentSearches([]), []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem("cafesito:search:recent", JSON.stringify(recentSearches));
    } catch {
      // noop
    }
  }, [recentSearches]);

  useEffect(() => {
    if (!focusCoffeeProfile || !selectedCoffeeRef.current) return;
    selectedCoffeeRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [focusCoffeeProfile, selectedCoffee?.id]);

  const visibleUsers = users.slice(0, visibleCount);
  const showUsersSentinel = mode === "users" && users.length > SEARCH_LOAD_MORE_AT_INDEX && visibleCount < users.length;
  const usersBeforeSentinel = showUsersSentinel ? visibleUsers.slice(0, SEARCH_LOAD_MORE_AT_INDEX) : visibleUsers;
  const usersAfterSentinel = showUsersSentinel ? visibleUsers.slice(SEARCH_LOAD_MORE_AT_INDEX) : [];

  const renderUserRow = (user: UserRow) => (
    <li key={user.id} className="search-users-row">
      <Button variant="plain" className="search-users-link" onClick={() => onSelectUser(user.id)}>
        {user.avatar_url ? (
          <img className="avatar avatar-photo search-users-avatar" src={user.avatar_url} alt={user.username} loading="lazy" decoding="async" referrerPolicy="no-referrer" crossOrigin="anonymous" />
        ) : (
          <div className="avatar search-users-avatar-fallback" aria-hidden="true">
            {user.username.slice(0, 2).toUpperCase()}
          </div>
        )}
        <div className="search-users-copy">
          <p className="search-users-username">{user.username}</p>
          <p className="search-users-fullname">{user.full_name}</p>
        </div>
      </Button>
      <Button
        variant="plain"
        className={`action-button search-users-follow ${followingIds.has(user.id) ? "action-button-following" : "action-button-ghost"}`}
        onClick={() => onToggleFollow(user.id)}
      >
        {followingIds.has(user.id) ? "Siguiendo" : "Seguir"}
      </Button>
    </li>
  );

  const content = (
    <>
      {mode === "users" ? (
        <ul className="search-users-list">
          {users.length ? (
            <>
              {usersBeforeSentinel.map(renderUserRow)}
              {showUsersSentinel ? (
                <>
                  <li key="sentinel-users" aria-hidden="true" style={{ height: 1, listStyle: "none" }}>
                    <div ref={loadMoreSentinelRef} style={{ height: 1 }} />
                  </li>
                  {usersAfterSentinel.map(renderUserRow)}
                </>
              ) : null}
            </>
          ) : (
            <li className="search-users-empty">
              {searchQuery.trim() ? "No se encontraron usuarios" : "Busca amigos para seguir"}
            </li>
          )}
        </ul>
      ) : null}

      {mode === "coffees" ? (
        <div className="search-coffee-view">
          {!focusCoffeeProfile && recentSearches.length ? (
            <section className="search-recent">
              <div className="search-recent-head">
                <p className="search-recent-title">Busquedas recientes</p>
                <Button variant="text" className="search-recent-clear" onClick={clearRecentSearches}>
                  Limpiar
                </Button>
              </div>
              <div className="search-recent-list">
                {recentSearches.map((term) => (
                  <Button
                    key={term}
                    variant="plain"
                    className="search-recent-chip"
                    onClick={() => {
                      onSearchQueryChange(term);
                      saveRecentSearch(term);
                    }}
                  >
                    {term}
                  </Button>
                ))}
              </div>
            </section>
          ) : null}

          {selectedCoffee && focusCoffeeProfile ? (
            <article className={`coffee-profile-card ${focusCoffeeProfile ? "is-focused" : ""}`} ref={selectedCoffeeRef}>
              {focusCoffeeProfile ? (
                <div className="coffee-profile-head">
                  <p className="coffee-profile-badge">PERFIL DE CAFE</p>
                  <Button variant="text" onClick={onExitCoffeeFocus}>
                    Ver todos
                  </Button>
                </div>
              ) : null}
              {selectedCoffee.image_url ? <img className="coffee-profile-image" src={selectedCoffee.image_url} alt={selectedCoffee.nombre} loading="lazy" decoding="async" /> : null}
              <div className="coffee-profile-copy">
                <p className="coffee-origin">{selectedCoffee.pais_origen ?? "Origen desconocido"}</p>
                <h3 className="coffee-profile-title">{selectedCoffee.nombre}</h3>
                <p className="coffee-profile-brand">{selectedCoffee.marca || "Marca"}</p>
              </div>
            </article>
          ) : null}

          {(() => {
            const visibleCoffees = coffees.slice(0, visibleCount);
            const showCoffeesSentinel = mode === "coffees" && coffees.length > SEARCH_LOAD_MORE_AT_INDEX && visibleCount < coffees.length;
            const coffeesBeforeSentinel = showCoffeesSentinel ? visibleCoffees.slice(0, SEARCH_LOAD_MORE_AT_INDEX) : visibleCoffees;
            const coffeesAfterSentinel = showCoffeesSentinel ? visibleCoffees.slice(SEARCH_LOAD_MORE_AT_INDEX) : [];
            return (
              <ul className="coffee-list">
                {coffeesBeforeSentinel.map((coffee) => (
                  <li key={coffee.id}>
                    <Button
                      variant="plain"
                      className={`coffee-card coffee-card-row coffee-card-interactive ${selectedCoffee?.id === coffee.id ? "is-selected" : ""}`}
                      onClick={() => {
                        if (searchQuery.trim()) saveRecentSearch(searchQuery);
                        onSelectCoffee(coffee.id);
                      }}
                    >
                      {coffee.image_url ? (
                        <img className="search-coffee-thumb" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
                      ) : (
                        <div className="search-coffee-thumb search-coffee-thumb-fallback" aria-hidden="true">
                          <UiIcon name="coffee" className="ui-icon" />
                        </div>
                      )}
                      <div className="search-coffee-copy">
                        <strong>{coffee.nombre}</strong>
                        <p className="coffee-sub">{coffee.marca || "Marca"}</p>
                        <p className="coffee-origin">{coffee.pais_origen ?? "Origen desconocido"}</p>
                      </div>
                      <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
                    </Button>
                  </li>
                ))}
                {showCoffeesSentinel ? (
                  <>
                    <li key="sentinel-coffees" aria-hidden="true" style={{ height: 1, listStyle: "none" }}>
                      <div ref={loadMoreSentinelRef} style={{ height: 1 }} />
                    </li>
                    {coffeesAfterSentinel.map((coffee) => (
                      <li key={coffee.id}>
                        <Button
                          variant="plain"
                          className={`coffee-card coffee-card-row coffee-card-interactive ${selectedCoffee?.id === coffee.id ? "is-selected" : ""}`}
                          onClick={() => {
                            if (searchQuery.trim()) saveRecentSearch(searchQuery);
                            onSelectCoffee(coffee.id);
                          }}
                        >
                          {coffee.image_url ? (
                            <img className="search-coffee-thumb" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
                          ) : (
                            <div className="search-coffee-thumb search-coffee-thumb-fallback" aria-hidden="true">
                              <UiIcon name="coffee" className="ui-icon" />
                            </div>
                          )}
                          <div className="search-coffee-copy">
                            <strong>{coffee.nombre}</strong>
                            <p className="coffee-sub">{coffee.marca || "Marca"}</p>
                            <p className="coffee-origin">{coffee.pais_origen ?? "Origen desconocido"}</p>
                          </div>
                          <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
                        </Button>
                      </li>
                    ))}
                  </>
                ) : null}
              </ul>
            );
          })()}
          {!coffees.length ? <p className="search-coffee-empty">No encontramos cafes con esos filtros.</p> : null}

          {activeFilterType ? (
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Filtros" onClick={() => onSetActiveFilterType(null)}>
              <SheetCard className="search-filter-sheet" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="search-filter-actions">
                  <Button variant="text" onClick={onClearCoffeeFilters}>
                    Limpiar filtros
                  </Button>
                </div>
                {activeFilterType === "nota" ? (
                  <div className="search-rating-filter">
                    <p className="search-rating-label">{minRating > 0 ? `Nota minima: ${minRating}+` : "Cualquier nota"}</p>
                    <Input
                      className="app-range"
                      style={{ "--range-progress": `${(minRating / 5) * 100}%` } as React.CSSProperties}
                      type="range"
                      min={0}
                      max={5}
                      step={1}
                      value={minRating}
                      onChange={(event) => onSetMinRating(Number(event.target.value))}
                    />
                    <div className="search-rating-scale">
                      <span>0</span>
                      <span>5</span>
                    </div>
                  </div>
                ) : (
                  <ul className="search-filter-list">
                    {(activeFilterType === "origen"
                      ? originOptions
                      : activeFilterType === "tueste"
                        ? roastOptions
                        : activeFilterType === "especialidad"
                          ? specialtyOptions
                          : formatOptions).map((option) => {
                      const checked =
                        activeFilterType === "origen"
                          ? selectedOrigins.has(option)
                          : activeFilterType === "tueste"
                            ? selectedRoasts.has(option)
                            : activeFilterType === "especialidad"
                              ? selectedSpecialties.has(option)
                              : selectedFormats.has(option);
                      return (
                        <li key={option}>
                          <Chip
                            className={`search-filter-item ${checked ? "is-selected" : ""}`.trim()}
                            onClick={() => {
                              if (activeFilterType === "origen") onToggleOrigin(option);
                              else if (activeFilterType === "tueste") onToggleRoast(option);
                              else if (activeFilterType === "especialidad") onToggleSpecialty(option);
                              else onToggleFormat(option);
                            }}
                          >
                            <input type="checkbox" readOnly checked={checked} aria-hidden="true" tabIndex={-1} />
                            <span>{option}</span>
                          </Chip>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </SheetCard>
            </SheetOverlay>
          ) : null}
        </div>
      ) : null}
    </>
  );

  if (sidePanel) {
    return (
      <div className="split-with-side profile-split-with-side">
        <div className="profile-content-wrap">{content}</div>
        <aside className="timeline-side-column">{sidePanel}</aside>
      </div>
    );
  }

  return content;
}





