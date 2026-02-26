import { type Dispatch, type SetStateAction, useCallback, useState } from "react";

type SearchMode = "coffees" | "users";
type SearchFilter = "todo" | "origen" | "nombre";
type SearchFilterType = "origen" | "tueste" | "especialidad" | "formato" | "nota" | null;

function toggleInSet(setter: Dispatch<SetStateAction<Set<string>>>, value: string) {
  setter((prev) => {
    const next = new Set(prev);
    if (next.has(value)) next.delete(value);
    else next.add(value);
    return next;
  });
}

export function useSearchDomain(initialMode: SearchMode) {
  const [searchQuery, setSearchQuery] = useState("");
  const [searchMode, setSearchMode] = useState<SearchMode>(initialMode);
  const [searchFilter, setSearchFilter] = useState<SearchFilter>("todo");
  const [searchSelectedOrigins, setSearchSelectedOrigins] = useState<Set<string>>(new Set());
  const [searchSelectedRoasts, setSearchSelectedRoasts] = useState<Set<string>>(new Set());
  const [searchSelectedSpecialties, setSearchSelectedSpecialties] = useState<Set<string>>(new Set());
  const [searchSelectedFormats, setSearchSelectedFormats] = useState<Set<string>>(new Set());
  const [searchMinRating, setSearchMinRating] = useState(0);
  const [searchActiveFilterType, setSearchActiveFilterType] = useState<SearchFilterType>(null);
  const [searchSelectedCoffeeId, setSearchSelectedCoffeeId] = useState<string | null>(null);
  const [searchFocusCoffeeProfile, setSearchFocusCoffeeProfile] = useState(false);

  const onSearchQueryChange = useCallback((value: string) => {
    setSearchQuery(value);
    setSearchFocusCoffeeProfile(false);
  }, []);

  const onSearchFilterChange = useCallback((value: SearchFilter) => {
    setSearchFilter(value);
    setSearchFocusCoffeeProfile(false);
  }, []);

  const onToggleOrigin = useCallback((value: string) => {
    toggleInSet(setSearchSelectedOrigins, value);
  }, []);

  const onToggleRoast = useCallback((value: string) => {
    toggleInSet(setSearchSelectedRoasts, value);
  }, []);

  const onToggleSpecialty = useCallback((value: string) => {
    toggleInSet(setSearchSelectedSpecialties, value);
  }, []);

  const onToggleFormat = useCallback((value: string) => {
    toggleInSet(setSearchSelectedFormats, value);
  }, []);

  const onClearCoffeeFilters = useCallback(() => {
    setSearchSelectedOrigins(new Set());
    setSearchSelectedRoasts(new Set());
    setSearchSelectedSpecialties(new Set());
    setSearchSelectedFormats(new Set());
    setSearchMinRating(0);
  }, []);

  const onSelectCoffee = useCallback((coffeeId: string) => {
    setSearchSelectedCoffeeId(coffeeId);
    setSearchFocusCoffeeProfile(false);
  }, []);

  const resetSearchUi = useCallback(() => {
    setSearchQuery("");
    setSearchFocusCoffeeProfile(false);
  }, []);

  return {
    searchQuery,
    setSearchQuery,
    searchMode,
    setSearchMode,
    searchFilter,
    setSearchFilter,
    searchSelectedOrigins,
    setSearchSelectedOrigins,
    searchSelectedRoasts,
    setSearchSelectedRoasts,
    searchSelectedSpecialties,
    setSearchSelectedSpecialties,
    searchSelectedFormats,
    setSearchSelectedFormats,
    searchMinRating,
    setSearchMinRating,
    searchActiveFilterType,
    setSearchActiveFilterType,
    searchSelectedCoffeeId,
    setSearchSelectedCoffeeId,
    searchFocusCoffeeProfile,
    setSearchFocusCoffeeProfile,
    onSearchQueryChange,
    onSearchFilterChange,
    onToggleOrigin,
    onToggleRoast,
    onToggleSpecialty,
    onToggleFormat,
    onClearCoffeeFilters,
    onSelectCoffee,
    resetSearchUi
  };
}
