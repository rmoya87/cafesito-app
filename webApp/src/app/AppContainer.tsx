import React, { Suspense, useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import {
  addUserListItem,
  createListInvitation,
  createUserList,
  deleteUserList,
  fetchListInvitationsByListId,
  fetchListMembersByListId,
  fetchProfileUserActivityData,
  fetchUserById,
  fetchUsersByIds,
  fetchUserListById,
  fetchUserListItems,
  fetchSharedWithMeLists,
  fetchUserLists,
  insertFinishedCoffee,
  leaveList,
  removeUserListItem,
  requestAccountDeletion,
  syncAccountLifecycleAfterLogin,
  toggleFavoriteCoffee,
  updateUserList,
  updateUserListWithPrivacy,
  type ListInvitationRow,
  type ListMemberRow,
  type UserListItemActivityRow
} from "../data/supabaseApi";
import { BREW_METHODS } from "../config/brew";
import { buildRoute, getAppRootPath, isKnownRoute, parseRoute } from "../core/routing";
import { sendEvent, sendPageView, setGa4UserId } from "../core/ga4";
import { shouldUseRightRailDetail, sidePanelForTab } from "../core/layouts";
import { canAccessTabAsGuest, resolveGuardedTab } from "../core/guards";
import { getBrewMethodProfile, getBrewStepTitle, getBrewTimeProfile } from "../core/brew";
import { formatMonthYear, formatWeekRange, getMondayOfWeek, type DiaryPeriod } from "../core/diaryAnalytics";
import { normalizeLookupText } from "../core/text";

import { useGlobalUiEvents } from "../hooks/useGlobalUiEvents";
import { useResponsiveMode } from "../hooks/useResponsiveMode";
import { useAuthSession } from "../hooks/domains/useAuthSession";
import { useInitialDataLoader } from "../hooks/domains/useInitialDataLoader";
import { useUserDataLoader } from "../hooks/domains/useUserDataLoader";
import { useBrewTimer } from "../hooks/domains/useBrewTimer";
import { useAuthActionGuard } from "../hooks/domains/useAuthActionGuard";
import { useSearchDomain } from "../hooks/domains/useSearchDomain";
import { useProfileDomain } from "../hooks/domains/useProfileDomain";
import { useCoffeeDetailDomain, useCoffeeDetailDraftSync } from "../hooks/domains/useCoffeeDetailDomain";
import { useCoffeeDetailActions } from "../hooks/domains/useCoffeeDetailActions";
import { useTimelineComposerDomain } from "../hooks/domains/useTimelineComposerDomain";
import { useAppNavigationDomain } from "../hooks/domains/useAppNavigationDomain";
import { useTimelineActions } from "../hooks/domains/useTimelineActions";
import { useCreateCoffeeDomain } from "../hooks/domains/useCreateCoffeeDomain";
import { useProfileActions } from "../hooks/domains/useProfileActions";
import { useDiaryActions } from "../hooks/domains/useDiaryActions";
import { useDiarySheetActions } from "../hooks/domains/useDiarySheetActions";
import { useTimelineSheetActions } from "../hooks/domains/useTimelineSheetActions";
import { useNotificationsDomain } from "../hooks/domains/useNotificationsDomain";
import { useCoffeeDetailNavigation } from "../hooks/domains/useCoffeeDetailNavigation";
import { useCoffeeDetailInteractions } from "../hooks/domains/useCoffeeDetailInteractions";
import { useTopBarActions } from "../hooks/domains/useTopBarActions";
import { useAppDerivedData } from "../hooks/domains/useAppDerivedData";
import { useAppUiEffects } from "../hooks/domains/useAppUiEffects";
import { useCoffeeSeoMeta } from "../hooks/domains/useCoffeeSeoMeta";
import {
  clearReturnAfterLoginPath,
  getReturnAfterLoginPath,
  useCoffeeRouteSync,
  useRouteCanonicalSync,
  useRouteGuardSync
} from "../hooks/domains/useRouteSync";
import { getSupabaseClient, supabaseConfigError } from "../supabase";
import { applyThemeToDocument, getThemeMode } from "../core/theme";
import { TopBar } from "../features/topbar/TopBar";
import { FavoritosListView } from "../features/profile/FavoritosListView";
import { HistorialView } from "../features/profile/HistorialView";
import { ProfileUsersListView } from "../features/profile/ProfileUsersListView";
import {
  LazyHomeView,
  LazySearchView,
  LazyCoffeeDetailView,
  LazyBrewLabView,
  LazyCreateCoffeeView,
  LazyDiaryView,
  LazyProfileView,
  LazyNotFoundView
} from "./lazyViews";
import { NotificationsSheet } from "../features/timeline/NotificationsSheet";
import { MobileBarcodeScannerSheet } from "../features/search/MobileBarcodeScannerSheet";
import { LoginGate } from "../features/auth/LoginGate";
import { AuthPromptOverlay } from "../features/auth/AuthPromptOverlay";
import { CafesProbadosView } from "../features/diary/CafesProbadosView";
import { DiarySheets } from "../features/diary/DiarySheets";
import { BottomNav, DesktopNavRail } from "../features/navigation/NavControls";
import { AppContentRouter } from "./AppContentRouter";
import { AppOverlayLayers } from "./AppOverlayLayers";
import { CookieConsentBanner } from "../features/consent/CookieConsentBanner";
import { OfflineBanner } from "./OfflineBanner";
import { EditListSheet } from "../features/lists/EditListSheet";
import { ListOptionsPage } from "../features/lists/ListOptionsPage";
import { BrewSelectCoffeePage } from "../features/brew/BrewSelectCoffeePage";

import { UiIcon } from "../ui/iconography";
import { Button, cn, IconButton, SheetCard, SheetHandle, SheetOverlay } from "../ui/components";
import type {
  BrewStep,
  CoffeeRow,
  CoffeeReviewRow,
  CoffeeSensoryProfileRow,
  CommentRow,
  DiaryEntryRow,
  FavoriteRow,
  FinishedCoffeeRow,
  FollowRow,
  LikeRow,
  ListPrivacy,
  NotificationRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  TabId,
  UserListRow,
  UserRow
} from "../types";

export function AppContainer() {
  const initialRoute = parseRoute(window.location.pathname);
  const isNotFoundRoute = !isKnownRoute(window.location.pathname);
  const { mode, viewportWidth } = useResponsiveMode();
  const [activeTab, setActiveTab] = useState<TabId>(initialRoute.tab);
  const [diarySubView, setDiarySubView] = useState<"cafes-probados" | null>((initialRoute as { diarySubView?: "cafes-probados" }).diarySubView ?? null);

  /* Sincronizar clase theme-light/theme-dark en html con la preferencia guardada (por si se perdió) */
  useEffect(() => {
    applyThemeToDocument(getThemeMode());
  }, []);

  const [globalStatus, setGlobalStatus] = useState("Cargando datos...");

  const [users, setUsers] = useState<UserRow[]>([]);
  const [coffees, setCoffees] = useState<CoffeeRow[]>([]);
  const [customCoffees, setCustomCoffees] = useState<CoffeeRow[]>([]);
  const [coffeeReviews, setCoffeeReviews] = useState<CoffeeReviewRow[]>([]);
  const [coffeeSensoryProfiles, setCoffeeSensoryProfiles] = useState<CoffeeSensoryProfileRow[]>([]);
  const [posts, setPosts] = useState<PostRow[]>([]);
  const [likes, setLikes] = useState<LikeRow[]>([]);
  const [comments, setComments] = useState<CommentRow[]>([]);
  const [postCoffeeTags, setPostCoffeeTags] = useState<PostCoffeeTagRow[]>([]);

  const [diaryEntries, setDiaryEntries] = useState<DiaryEntryRow[]>([]);
  const [pantryItems, setPantryItems] = useState<PantryItemRow[]>([]);
  const [finishedCoffees, setFinishedCoffees] = useState<FinishedCoffeeRow[]>([]);
  const [favorites, setFavorites] = useState<FavoriteRow[]>([]);
  const [userLists, setUserLists] = useState<UserListRow[]>([]);
  const [coffeeIdsInUserLists, setCoffeeIdsInUserLists] = useState<string[]>([]);
  const [profileSubPanel, setProfileSubPanel] = useState<"historial" | "followers" | "following" | "favorites" | "list" | null>(
    () =>
      initialRoute.tab === "profile" && "profileSection" in initialRoute && initialRoute.profileSection
        ? initialRoute.profileSection
        : null
  );
  const [profileListId, setProfileListId] = useState<string | null>(
    () => (initialRoute.tab === "profile" && "profileListId" in initialRoute && initialRoute.profileListId) || null
  );
  const [listOptionsView, setListOptionsView] = useState(
    () => (initialRoute as { listOptionsView?: boolean }).listOptionsView === true
  );
  const [showEditListSheet, setShowEditListSheet] = useState(false);
  const [showDeleteListConfirmSheet, setShowDeleteListConfirmSheet] = useState(false);
  const [showLeaveListConfirmSheet, setShowLeaveListConfirmSheet] = useState(false);
  const [listViewMembers, setListViewMembers] = useState<ListMemberRow[]>([]);
  const [listViewMemberUsers, setListViewMemberUsers] = useState<UserRow[]>([]);
  const [listOptionsMembers, setListOptionsMembers] = useState<ListMemberRow[]>([]);
  const [listOptionsInvitations, setListOptionsInvitations] = useState<ListInvitationRow[]>([]);
  const [listOptionsMemberUsers, setListOptionsMemberUsers] = useState<UserRow[]>([]);
  const [listOptionsInvitingId, setListOptionsInvitingId] = useState<number | null>(null);
  const [showCopyChip, setShowCopyChip] = useState(false);
  const [copyChipExiting, setCopyChipExiting] = useState(false);
  const [follows, setFollows] = useState<FollowRow[]>([]);
  const [notifications, setNotifications] = useState<NotificationRow[]>([]);
  const [profileUserDiaryEntries, setProfileUserDiaryEntries] = useState<DiaryEntryRow[]>([]);
  const [profileUserFavorites, setProfileUserFavorites] = useState<FavoriteRow[]>([]);
  const [profileUserListItems, setProfileUserListItems] = useState<UserListItemActivityRow[]>([]);
  const [allListItemsForActivity, setAllListItemsForActivity] = useState<UserListItemActivityRow[]>([]);
  const [followedUsersActivityData, setFollowedUsersActivityData] = useState<Array<{
    userId: number;
    diaryEntries: DiaryEntryRow[];
    favorites: FavoriteRow[];
    listItems: { list_id: string; coffee_id: string; created_at: number }[];
  }>>([]);

  const {
    searchQuery,
    setSearchQuery,
    searchMode,
    setSearchMode,
    searchFilter,
    searchSelectedOrigins,
    searchSelectedRoasts,
    searchSelectedSpecialties,
    searchSelectedFormats,
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
  } = useSearchDomain(initialRoute.searchMode);
  const {
    detailCoffeeId,
    setDetailCoffeeId,
    detailHostTab,
    setDetailHostTab,
    detailReviewText,
    setDetailReviewText,
    detailReviewRating,
    setDetailReviewRating,
    detailReviewImageFile,
    setDetailReviewImageFile,
    detailReviewImagePreviewUrl,
    setDetailReviewImagePreviewUrl,
    detailSensoryDraft,
    setDetailSensoryDraft,
    detailStockDraft,
    setDetailStockDraft,
    detailOpenStockSignal,
    setDetailOpenStockSignal
  } = useCoffeeDetailDomain();
  const [showBarcodeScannerSheet, setShowBarcodeScannerSheet] = useState(false);
  const [barcodeOrigin, setBarcodeOrigin] = useState<"search" | "diary" | "brew" | "createPostCoffee" | null>(null);
  const [barcodeDetectedValueForDiary, setBarcodeDetectedValueForDiary] = useState<string | null>(null);
  const [barcodeDetectedValueForBrew, setBarcodeDetectedValueForBrew] = useState<string | null>(null);
  const barcodeSearchPendingRef = useRef(false);

  const initialBrewDraft = useMemo(() => {
    try {
      const raw = typeof localStorage !== "undefined" ? localStorage.getItem("cafesito_brew_draft") : null;
      if (!raw) return null;
      return JSON.parse(raw) as {
        brewMethod?: string;
        brewCoffeeId?: string;
        brewDrinkType?: string;
        waterMl?: number;
        ratio?: number;
        coffeeGrams?: number;
        brewStep?: string;
        brewTimerEnabled?: boolean;
      };
    } catch {
      return null;
    }
  }, []);
  const [brewStep, setBrewStep] = useState<BrewStep>("method");
  const [brewTimerEnabled, setBrewTimerEnabled] = useState(Boolean(initialBrewDraft?.brewTimerEnabled));
  const [brewTimerEnded, setBrewTimerEnded] = useState(false);
  const brewResultSaveRef = useRef<() => Promise<void>>(async () => {});
  const [brewResultSaveMeta, setBrewResultSaveMeta] = useState({ canSave: false, saving: false, showGuardar: false });
  const onBrewResultSaveState = useCallback(
    (s: { save: () => Promise<void>; canSave: boolean; saving: boolean; showGuardar: boolean }) => {
      brewResultSaveRef.current = s.save;
      setBrewResultSaveMeta({ canSave: s.canSave, saving: s.saving, showGuardar: s.showGuardar ?? false });
    },
    []
  );
  const [brewMethod, setBrewMethod] = useState(initialBrewDraft?.brewMethod ?? "");
  const [brewCoffeeId, setBrewCoffeeId] = useState<string>("");
  /** Ítem de despensa elegido (cuando el usuario selecciona desde Tu despensa). Si hay varios del mismo café, así se identifica cuál. */
  const [brewPantryItemId, setBrewPantryItemId] = useState<string>("");
  const [brewDrinkType, setBrewDrinkType] = useState<string>(initialBrewDraft?.brewDrinkType ?? "Espresso");
  const [waterMl, setWaterMl] = useState(initialBrewDraft?.waterMl ?? 300);
  const [brewCoffeeGrams, setBrewCoffeeGrams] = useState<number>(() => {
    const w = initialBrewDraft?.waterMl ?? 300;
    const r = initialBrewDraft?.ratio ?? 16;
    return initialBrewDraft?.coffeeGrams ?? Math.max(1, Math.round(w / Math.max(0.1, r)));
  });
  const ratio = useMemo(() => (brewCoffeeGrams > 0 ? waterMl / brewCoffeeGrams : 16), [waterMl, brewCoffeeGrams]);
  const [espressoTimeSeconds, setEspressoTimeSeconds] = useState(27);
  const [timerSeconds, setTimerSeconds] = useState(150);
  const [brewRunning, setBrewRunning] = useState(false);
  useEffect(() => {
    try {
      localStorage.setItem(
        "cafesito_brew_draft",
        JSON.stringify({
          brewMethod,
          brewDrinkType,
          waterMl,
          ratio,
          coffeeGrams: brewCoffeeGrams,
          brewStep,
          brewTimerEnabled
        })
      );
    } catch {
      /* ignore */
    }
  }, [brewMethod, brewDrinkType, waterMl, brewCoffeeGrams, brewStep, brewTimerEnabled]);
  useEffect(() => {
    if (activeTab !== "brewlab" || brewStep !== "brewing") setBrewTimerEnded(false);
  }, [activeTab, brewStep]);
  const [diaryTab, setDiaryTab] = useState<"actividad" | "despensa">("actividad");
  const [diaryPeriod, setDiaryPeriod] = useState<DiaryPeriod>("week");
  const todayStr = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const [recommendationDateKey] = useState<string>(() => new Date().toISOString().slice(0, 10));
  const [selectedDiaryDate, setSelectedDiaryDate] = useState<string>(() => getMondayOfWeek(new Date().toISOString().slice(0, 10)));
  const currentMonthKey = useMemo(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  }, []);
  const [selectedDiaryMonth, setSelectedDiaryMonth] = useState<string>(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`;
  });
  useEffect(() => {
    if (diaryPeriod === "30d") setSelectedDiaryMonth((prev) => prev || currentMonthKey);
  }, [diaryPeriod, currentMonthKey]);
  const [showDiaryQuickActions, setShowDiaryQuickActions] = useState(false);
  const [showDiaryPeriodSheet, setShowDiaryPeriodSheet] = useState(false);
  const [showDiaryCalendarSheet, setShowDiaryCalendarSheet] = useState(false);
  const [showDiaryWaterSheet, setShowDiaryWaterSheet] = useState(false);
  const [diaryWaterMlDraft, setDiaryWaterMlDraft] = useState("250");
  const [showDiaryCoffeeSheet, setShowDiaryCoffeeSheet] = useState(false);
  const [coffeeSheetOpenedDirectlyToDose, setCoffeeSheetOpenedDirectlyToDose] = useState(false);
  const [coffeeSheetOpenedFromBrew, setCoffeeSheetOpenedFromBrew] = useState(false);
  const [coffeeSheetStep, setCoffeeSheetStep] = useState<"select" | "dose" | "tipo" | "tamaño" | "createCoffee">("select");
  const [diaryCoffeeIdDraft, setDiaryCoffeeIdDraft] = useState("");
  const [diarySelectedPantryItemIdDraft, setDiarySelectedPantryItemIdDraft] = useState("");
  const [diaryCoffeeGramsDraft, setDiaryCoffeeGramsDraft] = useState("15");
  const [diaryCoffeeMlDraft, setDiaryCoffeeMlDraft] = useState("250");
  const [diaryCoffeeCaffeineDraft, setDiaryCoffeeCaffeineDraft] = useState("0");
  const [diaryCoffeePreparationDraft, setDiaryCoffeePreparationDraft] = useState("Espresso");
  const [showDiaryAddPantrySheet, setShowDiaryAddPantrySheet] = useState(false);
  const [addPantrySheetHideBolt, setAddPantrySheetHideBolt] = useState(false);
  const [brewSelectCoffeePageOpen, setBrewSelectCoffeePageOpen] = useState(false);
  const [pantrySheetStep, setPantrySheetStep] = useState<"select" | "form" | "createCoffee">("select");
  const [diaryPantryCoffeeIdDraft, setDiaryPantryCoffeeIdDraft] = useState("");
  const [diaryPantryGramsDraft, setDiaryPantryGramsDraft] = useState("250");
  const [lastCreatedCoffeeNameForSheet, setLastCreatedCoffeeNameForSheet] = useState<string | null>(null);
  const addPantryOpenedFromBrewRef = useRef(false);
  const addPantryOpenedFromHomeRef = useRef(false);
  const lastSelectedPantryCoffeeRef = useRef<CoffeeRow | null>(null);
  const {
    profileTab,
    setProfileTab,
    profileUsername,
    setProfileUsername,
    profileEditSignal,
    triggerProfileEdit
  } = useProfileDomain(initialRoute.profileUsername, activeTab);

  const {
    showCreatePost,
    newPostText,
    setNewPostText,
    newPostImageFile,
    setNewPostImageFile,
    newPostImagePreviewUrl,
    setNewPostImagePreviewUrl,
    newPostGalleryItems,
    setNewPostGalleryItems,
    newPostSelectedImageId,
    setNewPostSelectedImageId,
    newPostCoffeeId,
    setNewPostCoffeeId,
    showCreatePostCoffeeSheet,
    setShowCreatePostCoffeeSheet,
    createPostCoffeeQuery,
    setCreatePostCoffeeQuery,
    showCreatePostEmojiPanel,
    setShowCreatePostEmojiPanel,
    newPostImageInputRef,
    resetCreatePostComposer,
    openCreatePostComposer,
    appendNewPostFiles
  } = useTimelineComposerDomain();
  const [handledHomeDeepLink, setHandledHomeDeepLink] = useState(false);
  const [topbarScrolled, setTopbarScrolled] = useState(false);
  const mainScrollRef = useRef<HTMLDivElement>(null);
  const [homeActionBanner, setHomeActionBanner] = useState<string | null>(null);
  const [homeRefreshing, setHomeRefreshing] = useState(false);
  const [homeBusyMessage, setHomeBusyMessage] = useState<string | null>(null);
  const {
    showNotificationsPanel,
    setShowNotificationsPanel,
    notificationsLastSeenAt,
    setNotificationsLastSeenAt,
    dismissedNotificationIds,
    dismissingNotificationIds,
    dismissNotificationTimersRef,
    closeNotificationsPanel,
    dismissNotification
  } = useNotificationsDomain();
  const {
    authReady,
    sessionEmail,
    authBusy,
    authError,
    showAuthPrompt,
    setAuthError,
    setShowAuthPrompt,
    handleGoogleLogin,
    handleGoogleCredential,
    requestLogin
  } = useAuthSession();
  const isMobileOsDevice = useMemo(() => /Android|iPhone|iPad|iPod/i.test(window.navigator.userAgent), []);
  const isAndroidMobile = useMemo(() => {
    if (!/Android/i.test(window.navigator.userAgent)) return false;
    if (!/Mobile/i.test(window.navigator.userAgent)) return false;
    try {
      if (typeof window.matchMedia !== "undefined" && window.matchMedia("(display-mode: standalone)").matches) return false;
    } catch {
      /* ignore */
    }
    return true;
  }, []);

  const ANDROID_BANNER_STORAGE_KEY = "cafesito-android-install-banner-dismissed";
  const [androidBannerDismissed, setAndroidBannerDismissed] = useState(() => {
    try {
      return window.localStorage.getItem(ANDROID_BANNER_STORAGE_KEY) === "1";
    } catch {
      return false;
    }
  });
  const dismissAndroidBanner = useCallback(() => {
    setAndroidBannerDismissed(true);
    try {
      window.localStorage.setItem(ANDROID_BANNER_STORAGE_KEY, "1");
    } catch {
      /* ignore */
    }
  }, []);

  const activeUser = useMemo(() => {
    if (!users.length) return null;
    if (sessionEmail) {
      const sessionEmailNormalized = normalizeLookupText(sessionEmail);
      const found = users.find((user) => normalizeLookupText(user.email) === sessionEmailNormalized);
      return found ?? null;
    }
    return users[0] ?? null;
  }, [sessionEmail, users]);

  const { runWithAuth } = useAuthActionGuard({ sessionEmail, onRequireAuth: requestLogin });

  const reloadInitialData = useInitialDataLoader({
    authReady,
    setUsers,
    setCoffees,
    setCoffeeReviews,
    setCoffeeSensoryProfiles,
    setFollows,
    setBrewCoffeeId,
    setGlobalStatus
  });

  useUserDataLoader({
    activeUser,
    setDiaryEntries,
    setPantryItems,
    setFavorites,
    setUserLists,
    setCoffeeIdsInUserLists,
    setAllListItemsForActivity,
    setCustomCoffees,
    setFinishedCoffees,
    setNotifications,
    setGlobalStatus
  });

  useEffect(() => {
    if (!activeUser) setFinishedCoffees([]);
  }, [activeUser]);

  const [listDetailItemIds, setListDetailItemIds] = useState<string[]>([]);
  const [profileListMeta, setProfileListMeta] = useState<UserListRow | null>(null);

  useEffect(() => {
    if (profileSubPanel !== "list" || !profileListId) {
      setListDetailItemIds([]);
      setProfileListMeta(null);
      return;
    }
    let cancelled = false;
    fetchUserListItems(profileListId)
      .then((items) => {
        if (!cancelled) setListDetailItemIds(items.map((i) => i.coffee_id));
      })
      .catch(() => {
        if (!cancelled) setListDetailItemIds([]);
      });
    return () => {
      cancelled = true;
    };
  }, [profileSubPanel, profileListId]);

  const listDetailCoffees = useMemo(() => {
    const byId = new Map(coffees.map((c) => [c.id, c]));
    return listDetailItemIds.map((id) => byId.get(id)).filter((c): c is CoffeeRow => c != null);
  }, [listDetailItemIds, coffees]);

  const {
    showCreateCoffeeComposer,
    setShowCreateCoffeeComposer,
    createCoffeeSaving,
    createCoffeeError,
    createCoffeeDraft,
    setCreateCoffeeDraft,
    createCoffeeImagePreviewUrl,
    openCreateCoffeeComposer,
    closeCreateCoffeeComposer,
    saveCreateCoffee,
    onPickImage: onPickCreateCoffeeImage,
    onRemoveImage: onRemoveCreateCoffeeImage,
    resetCreateCoffeeDomain
  } = useCreateCoffeeDomain({
    activeUser,
    setCustomCoffees,
    setPantryItems,
    setBrewCoffeeId,
    setBrewStep
  });

  useBrewTimer({
    brewRunning,
    brewStep,
    brewMethod,
    waterMl,
    espressoTimeSeconds,
    timerSeconds,
    setTimerSeconds,
    setBrewRunning,
    setBrewStep
  });

  const { navigateToTab, handleNavClick: handleNavClickBase } = useAppNavigationDomain({
    searchMode,
    setSearchMode,
    profileUsername,
    setProfileUsername,
    setProfileSubPanel,
    setProfileListId,
    setListOptionsView,
    profileListId,
    users,
    setActiveTab,
    setDiarySubView,
    activeUserUsername: activeUser?.username ?? null,
    coffees,
    setDetailCoffeeId,
    setDetailHostTab,
    setShowCreateCoffeeComposer,
    setSearchSelectedCoffeeId,
    setSearchFocusCoffeeProfile,
    isAuthenticated: Boolean(sessionEmail),
    onRequireAuth: requestLogin
  });

  const handleOpenCreateCoffee = useCallback(() => {
    runWithAuth(() => navigateToTab("crear-cafe"));
  }, [runWithAuth, navigateToTab]);

  const handleCloseCreateCoffee = useCallback(() => {
    setShowCreateCoffeeComposer(false);
    if (activeTab === "crear-cafe") navigateToTab("selecciona-cafe");
  }, [activeTab, navigateToTab, setShowCreateCoffeeComposer]);

  const handleOpenSeleccionaCafe = useCallback(() => {
    runWithAuth(() => navigateToTab("selecciona-cafe"));
  }, [runWithAuth, navigateToTab]);

  const handleCloseSeleccionaCafe = useCallback(() => {
    setBrewSelectCoffeePageOpen(false);
    if (activeTab === "selecciona-cafe") navigateToTab("brewlab");
  }, [activeTab, navigateToTab, setBrewSelectCoffeePageOpen]);

  const handleOpenBrewToMethod = useCallback(
    (methodName: string) => {
      const profile = getBrewMethodProfile(methodName);
      const methodTime = getBrewTimeProfile(methodName);
      setBrewMethod(methodName);
      setWaterMl(profile.defaultWaterMl);
      setBrewCoffeeGrams(Math.max(1, Math.round(profile.defaultWaterMl / Math.max(0.1, profile.defaultRatio))));
      setEspressoTimeSeconds(methodTime.defaultSeconds);
      setBrewStep("method");
      navigateToTab("brewlab");
    },
    [navigateToTab]
  );

  const handleCreateList = useCallback(
    async (name: string, privacy: ListPrivacy, membersCanEdit?: boolean) => {
      if (!activeUser) return;
      try {
        const list = await createUserList(activeUser.id, name, privacy, membersCanEdit);
        setUserLists((prev) => [...prev, list]);
      } catch (err) {
        console.error("Error creando lista:", err);
      }
    },
    [activeUser]
  );

  const handleOpenListOptionsPage = useCallback(() => {
    if (profileListId) navigateToTab("profile", { profileSection: "list", profileListId, listOptionsView: true });
  }, [navigateToTab, profileListId]);
  const handleEditListSave = useCallback(
    async (listId: string, name: string, privacy: ListPrivacy, membersCanEdit?: boolean) => {
      const updated = await updateUserListWithPrivacy(listId, name, privacy, membersCanEdit);
      setUserLists((prev) =>
        prev.map((l) =>
          l.id === listId
            ? { ...l, name: updated.name, is_public: updated.is_public, privacy: updated.privacy, members_can_edit: updated.members_can_edit }
            : l
        )
      );
      if (profileListMeta?.id === listId) {
        setProfileListMeta((m) =>
          m?.id === listId
            ? { ...m, name: updated.name, is_public: updated.is_public, privacy: updated.privacy, members_can_edit: updated.members_can_edit }
            : m
        );
      }
      sendEvent("modal_close", { modal_id: "list_edit" });
      setShowEditListSheet(false);
    },
    [profileListMeta?.id]
  );
  const handleDeleteListConfirm = useCallback(async () => {
    if (!profileListId) return;
    await deleteUserList(profileListId);
    setUserLists((prev) => prev.filter((l) => l.id !== profileListId));
    setListDetailItemIds([]);
    navigateToTab("profile", { profileSection: null, profileListId: null, replace: true });
    setShowDeleteListConfirmSheet(false);
  }, [profileListId, navigateToTab]);

  const handleLeaveListConfirm = useCallback(async () => {
    if (!profileListId || !activeUser) return;
    try {
      await leaveList(profileListId, activeUser.id);
      setUserLists((prev) => prev.filter((l) => l.id !== profileListId));
      setProfileListMeta(null);
      setShowLeaveListConfirmSheet(false);
      navigateToTab("profile", { profileSection: null, profileListId: null, replace: true });
    } catch (err) {
      console.error("Error al abandonar la lista:", err);
    }
  }, [profileListId, activeUser, navigateToTab]);

  useRouteCanonicalSync(Boolean(sessionEmail), authReady);
  const loginRootPath = useMemo(() => getAppRootPath(window.location.pathname) || "/", []);
  useRouteGuardSync({
    authReady,
    isAuthenticated: Boolean(sessionEmail),
    onBlocked: requestLogin,
    fallbackPath: loginRootPath
  });

  // Tras login: restaurar ruta de lista compartida si se guardó
  useEffect(() => {
    if (!sessionEmail) return;
    const returnPath = getReturnAfterLoginPath();
    if (!returnPath) return;
    const route = parseRoute(returnPath);
    if (route.tab !== "profile" || route.profileSection !== "list" || !(route as { profileListId?: string }).profileListId) {
      clearReturnAfterLoginPath();
      return;
    }
    const listId = (route as { profileListId?: string }).profileListId;
    clearReturnAfterLoginPath();
    setActiveTab("profile");
    setProfileSubPanel("list");
    setProfileListId(listId ?? null);
    const base = (getAppRootPath(window.location.pathname) || "/").replace(/\/+$/, "") || "/";
    const newPath = base === "/" ? buildRoute("profile", "coffees", null, null, "list", listId) : `${base}${buildRoute("profile", "coffees", null, null, "list", listId)}`;
    window.history.replaceState({}, "", `${newPath}${window.location.search}${window.location.hash}`);
  }, [sessionEmail]);

  // Al cargar o cambiar de página: guardar como último acceso (tab, path y fecha)
  useEffect(() => {
    try {
      if (typeof window === "undefined" || !localStorage) return;
      const path = window.location.pathname;
      localStorage.setItem(
        "cafesito_last_page",
        JSON.stringify({
          tab: activeTab,
          path,
          searchMode,
          lastAccessAt: new Date().toISOString()
        })
      );
    } catch {
      // ignore
    }
  }, [activeTab, searchMode]);

  // Si hay error de auth (login fallido, 500, callback con error), llevar a /home para no dejar en URL de error
  useEffect(() => {
    if (!authError) return;
    const pathname = window.location.pathname;
    const base = (getAppRootPath(pathname) || "/").replace(/\/+$/, "") || "";
    const homePath = (base ? `${base}/home` : "/home").replace(/\/+/g, "/");
    if (pathname.replace(/\/+$/, "") !== homePath.replace(/\/+$/, "")) {
      window.history.replaceState(null, "", `${homePath}${window.location.search}${window.location.hash}`);
      setActiveTab("home");
    }
  }, [authError]);

  useGlobalUiEvents({
    onOpenSearch: () => {
      navigateToTab("search", { searchMode: "coffees" });
      document.getElementById("quick-search")?.focus();
    },
    onTopbarScroll: setTopbarScrolled
  });

  const lastScrollTopRef = useRef(0);

  const applyScrollState = useCallback((scrollTop: number) => {
    setTopbarScrolled(scrollTop > 18);
    const mainShell = mainScrollRef.current?.parentElement;
    if (mainShell) {
      mainShell.style.setProperty("--topbar-translate-y", "0px");
    }
  }, []);

  useEffect(() => {
    const el = mainScrollRef.current;
    if (!el) return;
    lastScrollTopRef.current = el.scrollTop;
    applyScrollState(el.scrollTop);

    let scrollEndTimer: ReturnType<typeof setTimeout> | null = null;
    const SCROLL_END_MS = 100;

    const syncFromScroll = () => {
      const st = el.scrollTop;
      lastScrollTopRef.current = st;
      applyScrollState(st);
    };

    const onScroll = () => {
      syncFromScroll();
      if (scrollEndTimer != null) clearTimeout(scrollEndTimer);
      scrollEndTimer = setTimeout(() => {
        scrollEndTimer = null;
        syncFromScroll();
      }, SCROLL_END_MS);
    };

    const onScrollEnd = () => {
      if (scrollEndTimer != null) {
        clearTimeout(scrollEndTimer);
        scrollEndTimer = null;
      }
      syncFromScroll();
    };

    el.addEventListener("scroll", onScroll, { passive: true });
    el.addEventListener("scrollend", onScrollEnd);
    return () => {
      el.removeEventListener("scroll", onScroll);
      el.removeEventListener("scrollend", onScrollEnd);
      if (scrollEndTimer != null) clearTimeout(scrollEndTimer);
    };
  }, [applyScrollState, sessionEmail]);

  const handleSignOut = useCallback(async () => {
    if (supabaseConfigError) return;
    try {
      const supabase = getSupabaseClient();
      await supabase.auth.signOut();
      setUsers([]);
      setCoffees([]);
      setCustomCoffees([]);
      setCoffeeReviews([]);
      setCoffeeSensoryProfiles([]);
      setPosts([]);
      setLikes([]);
      setComments([]);
      setPostCoffeeTags([]);
      setFollows([]);
      setDiaryEntries([]);
      setPantryItems([]);
      setFavorites([]);
      setUserLists([]);
      setCoffeeIdsInUserLists([]);
      resetCreateCoffeeDomain();
      setDetailCoffeeId(null);
      setDetailHostTab(null);
      setGlobalStatus("Listo");
    } catch (error) {
      setAuthError((error as Error).message);
    }
  }, [resetCreateCoffeeDomain, setAuthError]);

  const handleDeleteAccount = useCallback(async () => {
    if (!activeUser) return;
    try {
      await requestAccountDeletion(activeUser.id);
      await handleSignOut();
    } catch (error) {
      setAuthError((error as Error).message);
    }
  }, [activeUser, handleSignOut, setAuthError]);

  useEffect(() => {
    if (!sessionEmail || !activeUser) return;
    let cancelled = false;
    void (async () => {
      try {
        const outcome = await syncAccountLifecycleAfterLogin(activeUser.id);
        if (cancelled) return;
        if (outcome === "reactivated") {
          setHomeActionBanner("Se canceló la eliminación de tu cuenta porque volviste a iniciar sesión.");
        } else if (outcome === "deleted") {
          await handleSignOut();
          if (!cancelled) setGlobalStatus("Tu cuenta se eliminó por haber superado el plazo de 30 días.");
        }
      } catch {
        // Best effort: no bloquea la sesión si el backend aún no tiene estas columnas.
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [activeUser, handleSignOut, sessionEmail, setHomeActionBanner]);

  const {
    brewCoffeeCatalog,
    usersById,
    coffeesById,
    coffeeSlugIndex,
    homeCards,
    filteredCoffees,
    searchOriginOptions,
    createCoffeeCountryOptions,
    searchRoastOptions,
    searchSpecialtyOptions,
    searchFormatOptions,
    selectedCreatePostCoffee,
    filteredCreatePostCoffees,
    createPostMentionSuggestions,
    selectedCoffeeForBrew,
    brewPantryItems,
    diaryEntriesActivity,
    orderedBrewMethods,
    pantryCoffeeRows,
    diaryCoffeeOptions,
    profileUser,
    profileFollowedActivity,
    profileMineAndFollowedActivity,
    profileUserActivity,
    favoriteCoffees,
    detailCoffee,
    detailCoffeeReviews,
    detailCoffeeAverageRating,
    detailCurrentUserReview,
    detailCurrentUser,
    detailCurrentUserReviewWithUser,
    detailIsFavorite,
    detailPantryStock,
    detailSensoryAverages,
    followersCount,
    followingCount,
    followingIds,
    profileFollowersUsers,
    profileFollowingUsers,
    followerCounts,
    followingCounts,
    filteredSearchUsers,
    homeRecommendations,
    homeSuggestions,
    homeSuggestionIndices,
    visibleHomeNotifications,
    showNotificationsBadge
  } = useAppDerivedData({
    users,
    coffees,
    customCoffees,
    coffeeReviews,
    coffeeSensoryProfiles,
    posts,
    likes,
    comments,
    postCoffeeTags,
    diaryEntries,
    pantryItems,
    favorites,
    follows,
    activeUser,
    profileUsername,
    searchQuery,
    searchSelectedOrigins,
    searchSelectedRoasts,
    searchSelectedSpecialties,
    searchSelectedFormats,
    searchMinRating,
    newPostCoffeeId,
    createPostCoffeeQuery,
    brewCoffeeId,
    newPostText,
    dismissedNotificationIds,
    notificationsLastSeenAt,
    detailCoffeeId,
    notifications,
    recommendationDateKey,
    profileUserDiaryEntries,
    profileUserFavorites,
    allListItemsForActivity,
    profileUserListItems,
    followedUsersActivityData
  });

  const createCoffeeBrandSuggestions = useMemo(() => {
    const byKey = new Map<string, string>();
    coffees.forEach((c) => {
      const raw = c.marca?.trim();
      if (!raw) return;
      const key = raw.toLowerCase();
      if (byKey.has(key)) return;
      const unified = raw.charAt(0).toUpperCase() + raw.slice(1).toLowerCase();
      byKey.set(key, unified);
    });
    return Array.from(byKey.values()).sort((a, b) => a.localeCompare(b, "es"));
  }, [coffees]);

  useEffect(() => {
    if (profileSubPanel !== "list" || !profileListId) {
      setProfileListMeta(null);
      return;
    }
    const inUserLists = userLists.some((l) => l.id === profileListId);
    if (profileUser && inUserLists) {
      setProfileListMeta(userLists.find((l) => l.id === profileListId) ?? null);
      return;
    }
    let cancelled = false;
    fetchUserListById(profileListId)
      .then((list) => {
        if (cancelled) return;
        if (list) {
          setProfileListMeta(list);
          if (!profileUsername && list.user_id) {
            fetchUserById(list.user_id).then((owner) => {
              if (!cancelled && owner) {
                setUsers((prev) => (prev.some((u) => u.id === owner.id) ? prev : [...prev, owner]));
                setProfileUsername(owner.username);
              }
            });
          }
        } else {
          setProfileListMeta(null);
        }
      })
      .catch(() => {
        if (!cancelled) setProfileListMeta(null);
      });
    return () => {
      cancelled = true;
    };
  }, [profileSubPanel, profileListId, profileUser, profileUsername, userLists, setProfileUsername, setUsers]);

  useEffect(() => {
    if (profileSubPanel !== "list" || !profileListId || !activeUser) {
      setListViewMembers([]);
      return;
    }
    const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
    const isOwner = list?.user_id === activeUser.id;
    const isMember = !isOwner && userLists.some((l) => l.id === profileListId);
    const isPublicOrInvitation = list?.privacy === "public" || list?.privacy === "invitation";
    if (!isPublicOrInvitation || (!isOwner && !isMember)) {
      setListViewMembers([]);
      return;
    }
    let cancelled = false;
    fetchListMembersByListId(profileListId).then((members) => {
      if (!cancelled) setListViewMembers(members);
    });
    return () => {
      cancelled = true;
    };
  }, [profileSubPanel, profileListId, activeUser, userLists, profileListMeta]);

  // Resolver avatares para TopBar: dueño + miembros (orden: dueño, luego miembros; hasta 3 para avatares apilados). Para dueño y para miembro.
  useEffect(() => {
    if (profileSubPanel !== "list" || !profileListId) {
      setListViewMemberUsers([]);
      return;
    }
    const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
    const ownerId = list?.user_id;
    const isPublicOrInvitation = list?.privacy === "public" || list?.privacy === "invitation";
    if (!isPublicOrInvitation || listViewMembers.length === 0) {
      setListViewMemberUsers([]);
      return;
    }
    const ids = ownerId != null
      ? [ownerId, ...listViewMembers.map((m) => m.user_id)]
      : listViewMembers.map((m) => m.user_id);
    const uniq = [...new Set(ids)];
    if (uniq.length === 0) {
      setListViewMemberUsers([]);
      return;
    }
    let cancelled = false;
    fetchUsersByIds(uniq).then((rows) => {
      if (!cancelled) setListViewMemberUsers(rows);
    }).catch(() => {
      if (!cancelled) setListViewMemberUsers([]);
    });
    return () => { cancelled = true; };
  }, [profileSubPanel, profileListId, activeUser?.id, userLists, profileListMeta, listViewMembers]);

  // Al abrir Opciones de lista, refrescar la lista desde el servidor para tener members_can_invite y members_can_edit actualizados (p. ej. admin acaba de activar "permitir que los miembros inviten").
  useEffect(() => {
    if (!listOptionsView || !profileListId) return;
    let cancelled = false;
    fetchUserListById(profileListId)
      .then((list) => {
        if (cancelled || !list) return;
        setProfileListMeta(list);
        setUserLists((prev) =>
          prev.some((l) => l.id === profileListId)
            ? prev.map((l) =>
                l.id === profileListId
                  ? { ...l, members_can_edit: list.members_can_edit, members_can_invite: list.members_can_invite }
                  : l
              )
            : prev
        );
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [listOptionsView, profileListId]);

  // Cargar miembros e invitaciones cuando se abre la página Opciones de lista (sección Miembros).
  useEffect(() => {
    if (!listOptionsView || !profileListId) {
      setListOptionsMembers([]);
      setListOptionsInvitations([]);
      setListOptionsMemberUsers([]);
      return;
    }
    let cancelled = false;
    Promise.all([
      fetchListMembersByListId(profileListId),
      fetchListInvitationsByListId(profileListId)
    ]).then(([members, invitations]) => {
      if (!cancelled) {
        setListOptionsMembers(members);
        setListOptionsInvitations(invitations);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [listOptionsView, profileListId]);

  useEffect(() => {
    if (!listOptionsView || !profileListId) return;
    const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
    const ownerId = list?.user_id;
    if (ownerId == null) return;
    const ids = [ownerId, ...listOptionsMembers.map((m) => m.user_id)];
    const uniq = [...new Set(ids)];
    let cancelled = false;
    fetchUsersByIds(uniq).then((rows) => {
      if (!cancelled) setListOptionsMemberUsers(rows);
    });
    return () => {
      cancelled = true;
    };
  }, [listOptionsView, profileListId, listOptionsMembers, userLists, profileListMeta]);

  useEffect(() => {
    if (!showCopyChip) {
      setCopyChipExiting(false);
      return;
    }
    const t1 = setTimeout(() => setCopyChipExiting(true), 3000);
    const t2 = setTimeout(() => {
      setShowCopyChip(false);
      setCopyChipExiting(false);
    }, 3250);
    return () => {
      clearTimeout(t1);
      clearTimeout(t2);
    };
  }, [showCopyChip]);

  /** Catálogo + custom para listado y total de cafés probados (solo se muestran los que están en esta lista). */
  const coffeeCatalogIncludingCustom = useMemo(
    () => [...brewCoffeeCatalog, ...customCoffees],
    [brewCoffeeCatalog, customCoffees]
  );

  const diaryCoffeesWithFirstTried = useMemo(() => {
    const coffeeEntries = diaryEntries.filter((e) => (e.type ?? "").toUpperCase() !== "WATER");
    const coffeeById = new Map<string, CoffeeRow>();
    coffeeCatalogIncludingCustom.forEach((c) => coffeeById.set(String(c.id), c));
    const byCoffeeId = new Map<string, number>();
    coffeeEntries.forEach((entry) => {
      if (entry.coffee_id) {
        const ts = Number(entry.timestamp);
        const prev = byCoffeeId.get(entry.coffee_id);
        if (prev == null || ts < prev) byCoffeeId.set(entry.coffee_id, ts);
      }
    });
    const list: Array<{ coffee: CoffeeRow; firstTriedTs: number }> = [];
    byCoffeeId.forEach((firstTriedTs, coffeeId) => {
      const coffee = coffeeById.get(coffeeId);
      if (coffee) list.push({ coffee, firstTriedTs });
    });
    list.sort((a, b) => a.firstTriedTs - b.firstTriedTs);
    return list;
  }, [diaryEntries, coffeeCatalogIncludingCustom]);

  const isListActive = useMemo(
    () =>
      Boolean(
        detailIsFavorite ||
          (detailCoffeeId != null && detailCoffeeId !== "" && coffeeIdsInUserLists.includes(detailCoffeeId))
      ),
    [detailIsFavorite, detailCoffeeId, coffeeIdsInUserLists]
  );

  useEffect(() => {
    if (!profileUser || profileUser.id !== activeUser?.id) {
      setProfileUserDiaryEntries([]);
      setProfileUserFavorites([]);
      setProfileUserListItems([]);
      return;
    }
    let cancelled = false;
    void fetchProfileUserActivityData(profileUser.id).then(({ diaryEntries: de, favorites: fav, listItems: li }) => {
      if (!cancelled) {
        setProfileUserDiaryEntries(de);
        setProfileUserFavorites(fav);
        setProfileUserListItems(li);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [profileUser?.id, activeUser?.id]);

  useEffect(() => {
    if (!activeUser || profileUser?.id !== activeUser.id) {
      setFollowedUsersActivityData([]);
      return;
    }
    const ids = follows.filter((f) => f.follower_id === activeUser.id).map((f) => f.followed_id);
    if (ids.length === 0) {
      setFollowedUsersActivityData([]);
      return;
    }
    let cancelled = false;
    Promise.all(ids.map((userId) => fetchProfileUserActivityData(userId)))
      .then((results) => {
        if (cancelled) return;
        setFollowedUsersActivityData(
          ids.map((userId, i) => ({
            userId,
            diaryEntries: results[i].diaryEntries,
            favorites: results[i].favorites,
            listItems: results[i].listItems
          }))
        );
      })
      .catch(() => {
        if (!cancelled) setFollowedUsersActivityData([]);
      });
    return () => {
      cancelled = true;
    };
  }, [activeUser?.id, profileUser?.id, follows]);

  useCoffeeRouteSync({
    coffeeSlugToId: coffeeSlugIndex.bySlug,
    setDetailCoffeeId,
    setDetailHostTab
  });

  const selectedCoffee = selectedCoffeeForBrew ?? undefined;
  const isRapido = normalizeLookupText(brewMethod) === "rapido" || normalizeLookupText(brewMethod) === "otros";
  // Cantidad de café = agua/ratio; no se recalcula al cambiar tamaño, el usuario la edita
  const coffeeGrams = brewCoffeeGrams;
  const displayCoffeeGrams = coffeeGrams;
  const {
    saveBrewToDiary,
    handleDeleteDiaryEntry,
    handleUpdateDiaryEntry,
    handleUpdatePantryStock,
    handleRemovePantryItem
  } = useDiaryActions({
    activeUser,
    selectedCoffee,
    coffeeGrams,
    brewMethod,
    waterMl,
    pantryItems,
    brewPantryItemId,
    setDiaryEntries,
    setPantryItems,
    setBrewRunning,
    setTimerSeconds,
    setBrewStep,
    navigateToDiary: () => {
    setDiaryTab("actividad");
    navigateToTab("diary");
  }
  });
  const handleMarkPantryCoffeeFinished = useCallback(
    async (pantryItemId: string) => {
      if (!activeUser?.id) return;
      const item = pantryItems.find((p) => p.id === pantryItemId);
      if (!item) return;
      const finishedAt = Date.now();
      await insertFinishedCoffee(activeUser.id, item.coffee_id, finishedAt);
      await handleRemovePantryItem(pantryItemId);
      setFinishedCoffees((prev) => [...prev, { coffee_id: item.coffee_id, finished_at: finishedAt }]);
    },
    [activeUser?.id, handleRemovePantryItem, pantryItems]
  );

  const selectedDiaryCoffee = useMemo(
    () => diaryCoffeeOptions.find((coffee) => coffee.id === diaryCoffeeIdDraft) ?? diaryCoffeeOptions[0] ?? null,
    [diaryCoffeeIdDraft, diaryCoffeeOptions]
  );
  const selectedDiaryPantryCoffee = useMemo(
    () =>
      diaryPantryCoffeeIdDraft
        ? diaryCoffeeOptions.find((coffee) => coffee.id === diaryPantryCoffeeIdDraft) ?? null
        : null,
    [diaryCoffeeOptions, diaryPantryCoffeeIdDraft]
  );
  const {
    openWaterSheet,
    openCoffeeSheet,
    openAddPantrySheet,
    setDiaryCoffeeDraftWithCaffeine,
    saveWater,
    saveWaterWithAmount,
    saveCoffee,
    savePantry
  } = useDiarySheetActions({
    activeUser,
    diaryCoffeeOptions,
    customCoffees,
    getLastSelectedPantryCoffee: () => lastSelectedPantryCoffeeRef.current ?? null,
    selectedDiaryCoffee,
    selectedDiaryPantryCoffee,
    diaryPantryCoffeeIdDraft,
    diaryWaterMlDraft,
    diaryCoffeeMlDraft,
    diaryCoffeeGramsDraft,
    diaryCoffeeCaffeineDraft,
    diaryCoffeePreparationDraft,
    diaryPantryGramsDraft,
    pantryItems,
    setDiaryEntries,
    setPantryItems,
    setDiaryTab,
    setShowDiaryQuickActions,
    setShowDiaryWaterSheet,
    setShowDiaryCoffeeSheet,
    setShowDiaryAddPantrySheet,
    setPantrySheetStep,
    setDiaryWaterMlDraft,
    setDiaryCoffeeIdDraft,
    setDiaryCoffeeCaffeineDraft,
    setDiaryCoffeeMlDraft,
    setDiaryCoffeePreparationDraft,
    setDiaryPantryCoffeeIdDraft,
    setDiaryPantryGramsDraft,
    setLastCreatedCoffeeNameForSheet
  });

  const openAddPantrySheetWithClear = useCallback(() => {
    lastSelectedPantryCoffeeRef.current = null;
    openAddPantrySheet();
  }, [openAddPantrySheet]);

  const openAddPantrySheetForBrew = useCallback(() => {
    addPantryOpenedFromBrewRef.current = true;
    setAddPantrySheetHideBolt(true);
    openAddPantrySheetWithClear();
  }, [openAddPantrySheetWithClear]);

  /** Abre la página «Selecciona café» (URL /selecciona-cafe) para elegir café para la elaboración. */
  const openCoffeeSheetForBrew = useCallback(() => {
    handleOpenSeleccionaCafe();
  }, [handleOpenSeleccionaCafe]);

  /** Al terminar el temporizador de elaboración: ir a Diario y abrir consumo con datos del brew para completar tipo/tamaño. */
  const onBrewTimerEndedGoToConsumption = useCallback(() => {
    setDiaryTab("actividad");
    navigateToTab("diary");
    setDiaryCoffeeIdDraft(brewCoffeeId || "");
    setDiarySelectedPantryItemIdDraft(brewPantryItemId || "");
    setDiaryCoffeePreparationDraft(brewMethod || "Espresso");
    setDiaryCoffeeGramsDraft(String(brewCoffeeGrams ?? 15));
    setCoffeeSheetStep("tipo");
    setCoffeeSheetOpenedFromBrew(true);
    setShowDiaryCoffeeSheet(true);
    setBrewStep("method");
  }, [navigateToTab, brewCoffeeId, brewPantryItemId, brewMethod, brewCoffeeGrams, setBrewStep]);

  /** Abre añadir a despensa desde la página Selecciona café de elaboración; al guardar se asigna ese café a la elaboración y se cierra la página. */
  const openAddPantrySheetFromBrewSelect = useCallback(() => {
    addPantryOpenedFromBrewRef.current = true;
    openAddPantrySheetWithClear();
  }, [openAddPantrySheetWithClear]);

  const openAddPantrySheetFromHome = useCallback(() => {
    addPantryOpenedFromHomeRef.current = true;
    openAddPantrySheetWithClear();
  }, [openAddPantrySheetWithClear]);

  const handleSavePantry = useCallback(async () => {
    const newRow = await savePantry();
    lastSelectedPantryCoffeeRef.current = null;
    if (addPantryOpenedFromHomeRef.current) {
      addPantryOpenedFromHomeRef.current = false;
      navigateToTab("home");
    } else if (addPantryOpenedFromBrewRef.current && newRow) {
      setBrewCoffeeId(newRow.coffee_id);
      setBrewPantryItemId(newRow.id);
      setBrewStep("method");
      addPantryOpenedFromBrewRef.current = false;
      handleCloseSeleccionaCafe();
    }
  }, [handleCloseSeleccionaCafe, navigateToTab, savePantry, setBrewCoffeeId, setBrewPantryItemId, setBrewStep]);

  useCoffeeDetailDraftSync({
    hasDetailCoffee: Boolean(detailCoffee),
    currentUserReview: detailCurrentUserReview,
    pantryStock: detailPantryStock,
    sensoryAverages: detailSensoryAverages,
    setDetailReviewText,
    setDetailReviewRating,
    setDetailReviewImagePreviewUrl,
    setDetailReviewImageFile,
    setDetailStockDraft,
    setDetailSensoryDraft
  });


  const {
    handleToggleLike,
    handleToggleFollow,
    handleEditPost,
    handleDeletePost,
    handleRefreshHome: handleRefreshHomeBase,
    handleCreatePost,
    handleMentionNavigation
  } = useTimelineActions({
    activeUser,
    likes,
    follows,
    comments,
    coffees,
    newPostText,
    newPostImageFile,
    newPostCoffeeId,
    setComments,
    setLikes,
    setFollows,
    setPosts,
    setPostCoffeeTags,
    setTimelineBusyMessage: setHomeBusyMessage,
    setTimelineActionBanner: setHomeActionBanner,
    setGlobalStatus,
    resetCreatePostComposer,
    reloadInitialData,
    navigateToTab,
    setSearchQuery
  });

  const handleRefreshHome = useCallback(async () => {
    setHomeRefreshing(true);
    await handleRefreshHomeBase();
    setHomeRefreshing(false);
  }, [handleRefreshHomeBase]);
  const { handleUpdateProfile } = useProfileActions({
    setUsers,
    setTimelineBusyMessage: setHomeBusyMessage,
    setTimelineActionBanner: setHomeActionBanner,
    setGlobalStatus
  });

  const closeDiarySheets = useCallback(() => {
    setShowDiaryQuickActions(false);
    setShowDiaryPeriodSheet(false);
    setShowDiaryWaterSheet(false);
    setShowDiaryCoffeeSheet(false);
    setShowDiaryAddPantrySheet(false);
  }, []);
  const { removeSelectedCreatePostImage } = useTimelineSheetActions({
    newPostSelectedImageId,
    newPostGalleryItems,
    setNewPostSelectedImageId,
    setNewPostImageFile,
    setNewPostImagePreviewUrl,
    setNewPostGalleryItems
  });

  useAppUiEffects({
    timelineActionBanner: homeActionBanner,
    setTimelineActionBanner: setHomeActionBanner,
    activeTab,
    searchMode,
    searchFocusCoffeeProfile,
    setSearchActiveFilterType,
    showNotificationsPanel,
    showCreatePost,
    showCreatePostCoffeeSheet,
    showCreateCoffeeComposer,
    resetCreatePostComposer,
    setShowNotificationsPanel,
    setShowCreatePostCoffeeSheet,
    setShowCreateCoffeeComposer,
    showAuthPrompt,
    setShowAuthPrompt,
    handledTimelineDeepLink: handledHomeDeepLink,
    setHandledTimelineDeepLink: setHandledHomeDeepLink,
    posts,
    navigateToHomeReplace: () => navigateToTab("home", { replace: true }),
    setNotificationsLastSeenAt,
    notificationsLastSeenAt,
    visibleTimelineNotifications: visibleHomeNotifications,
    dismissNotificationTimersRef,
    closeDiarySheets,
    handleRefreshTimeline: handleRefreshHome,
    onCloseCreateCoffee: handleCloseCreateCoffee
  });

  const { openCoffeeDetail, closeCoffeePanel } = useCoffeeDetailNavigation({
    coffeesById,
    coffeeSlugById: coffeeSlugIndex.byId,
    mode,
    searchMode,
    profileUsername,
    activeTab,
    setDetailCoffeeId,
    setDetailHostTab,
    setActiveTab,
    setDiarySubView
  });

  // Tras escanear código de barras en buscador: 1 resultado → detalle; varios → lista en búsqueda
  useEffect(() => {
    if (activeTab !== "search" || searchMode !== "coffees" || !barcodeSearchPendingRef.current) return;
    barcodeSearchPendingRef.current = false;
    if (filteredCoffees.length === 1) {
      openCoffeeDetail(filteredCoffees[0].id, "search");
    }
  }, [activeTab, searchMode, filteredCoffees, openCoffeeDetail]);

  const { saveDetailFavorite, saveDetailReview, removeDetailReview, saveDetailSensory, saveDetailStock } = useCoffeeDetailActions({
    activeUser,
    detailCoffeeId,
    detailPantryStock,
    detailIsFavorite,
    detailCurrentUserReviewImageUrl: detailCurrentUserReview?.image_url ?? "",
    detailReviewImageFile,
    detailReviewRating,
    detailReviewText,
    detailSensoryDraft,
    detailStockDraft,
    setFavorites,
    setCoffeeReviews,
    setCoffeeSensoryProfiles,
    setPantryItems,
    setDetailStockDraft
  });
  const { sidePanel: sidePanelDetailActions, fullPage: fullPageDetailActions } = useCoffeeDetailInteractions({
    isAuthenticated: Boolean(sessionEmail),
    requestLogin,
    runWithAuth,
    saveDetailFavorite,
    saveDetailReview,
    removeDetailReview,
    saveDetailSensory,
    saveDetailStock,
    navigateToProfile: (userId) => navigateToTab("profile", { profileUserId: userId })
  });

  const detailPanel = detailCoffee ? (
    <LazyCoffeeDetailView
      coffee={detailCoffee}
      reviews={detailCoffeeReviews}
      currentUser={detailCurrentUser}
      currentUserReview={detailCurrentUserReviewWithUser}
      avgRating={detailCoffeeAverageRating}
      isFavorite={detailIsFavorite}
      isListActive={isListActive}
      pantry={detailPantryStock}
      sensory={detailSensoryAverages}
      sensoryDraft={detailSensoryDraft}
      stockDraft={detailStockDraft}
      reviewDraftText={detailReviewText}
      reviewDraftRating={detailReviewRating}
      reviewDraftImagePreviewUrl={detailReviewImagePreviewUrl}
      onClose={closeCoffeePanel}
      onToggleFavorite={sidePanelDetailActions.onToggleFavorite}
      onReviewTextChange={setDetailReviewText}
      onReviewRatingChange={setDetailReviewRating}
      onReviewImagePick={(file: File | null, previewUrl: string) => {
        setDetailReviewImageFile(file);
        setDetailReviewImagePreviewUrl(previewUrl);
      }}
      onSaveReview={sidePanelDetailActions.onSaveReview}
      onDeleteReview={sidePanelDetailActions.onDeleteReview}
      canDeleteReview={Boolean(detailCurrentUserReview)}
      onSensoryDraftChange={setDetailSensoryDraft}
      onSaveSensory={sidePanelDetailActions.onSaveSensory}
      onStockDraftChange={setDetailStockDraft}
      onSaveStock={sidePanelDetailActions.onSaveStock}
      onOpenUserProfile={sidePanelDetailActions.onOpenUserProfile}
      isGuest={!sessionEmail}
      onRequireAuth={requestLogin}
      fullPage={false}
      externalOpenStockSignal={0}
      userLists={userLists}
      onCreateList={handleCreateList}
      onAddCoffeeToList={
        detailCoffee?.id
          ? async (listId) => {
              await addUserListItem(listId, detailCoffee.id);
              setCoffeeIdsInUserLists((prev) =>
                prev.includes(detailCoffee.id) ? prev : [...prev, detailCoffee.id]
              );
            }
          : undefined
      }
    />
  ) : null;

  const createCoffeePanel = (
    <LazyCreateCoffeeView
      draft={createCoffeeDraft}
      imagePreviewUrl={createCoffeeImagePreviewUrl}
      saving={createCoffeeSaving}
      error={createCoffeeError}
      countryOptions={searchOriginOptions}
      specialtyOptions={searchSpecialtyOptions}
      brandSuggestions={createCoffeeBrandSuggestions}
      onChange={setCreateCoffeeDraft}
      onPickImage={onPickCreateCoffeeImage}
      onRemoveImage={onRemoveCreateCoffeeImage}
      onClose={closeCreateCoffeeComposer}
      onSave={() => void saveCreateCoffee({ fromBrewChooser: true })}
      fullPage={mode !== "desktop"}
      hideActions={true}
    />
  );

  const crearCafeContent = (
    <section className="create-coffee-mobile-screen">
      <Suspense fallback={<div className="create-coffee-sheet-loading" aria-live="polite">Cargando...</div>}>
        <LazyCreateCoffeeView
          draft={createCoffeeDraft}
          imagePreviewUrl={createCoffeeImagePreviewUrl}
          saving={createCoffeeSaving}
          error={createCoffeeError}
          countryOptions={searchOriginOptions}
          specialtyOptions={searchSpecialtyOptions}
          brandSuggestions={createCoffeeBrandSuggestions}
          onChange={setCreateCoffeeDraft}
          onPickImage={onPickCreateCoffeeImage}
          onRemoveImage={onRemoveCreateCoffeeImage}
          onClose={handleCloseCreateCoffee}
          onSave={() => void saveCreateCoffee({ fromBrewChooser: true })}
          fullPage={true}
          hideActions={true}
        />
      </Suspense>
    </section>
  );

  const seleccionarCafeContent = (
    <BrewSelectCoffeePage
      pantryItems={brewPantryItems}
      coffeeOptions={diaryCoffeeOptions}
      brewCoffeeGrams={brewCoffeeGrams}
      onBack={handleCloseSeleccionaCafe}
      onSelectCoffee={(coffeeId, pantryItemId) => {
        setBrewCoffeeId(coffeeId);
        setBrewPantryItemId(pantryItemId ?? "");
        handleCloseSeleccionaCafe();
      }}
      onAddToPantry={openAddPantrySheetFromBrewSelect}
      onCreateCoffee={() => {
        handleCloseSeleccionaCafe();
        handleOpenCreateCoffee();
      }}
      onUpdatePantryStock={handleUpdatePantryStock}
      onRemovePantryItem={handleRemovePantryItem}
      onMarkPantryCoffeeFinished={handleMarkPantryCoffeeFinished}
      showBarcodeButton={isMobileOsDevice}
      onBarcodeClick={() => {
        setBarcodeOrigin("brew");
        setShowBarcodeScannerSheet(true);
      }}
    />
  );

  const createCoffeeFormForSheet = (
    <Suspense fallback={<div className="create-coffee-sheet-loading" aria-live="polite">Cargando...</div>}>
      <LazyCreateCoffeeView
        draft={createCoffeeDraft}
        imagePreviewUrl={createCoffeeImagePreviewUrl}
        saving={createCoffeeSaving}
        error={createCoffeeError}
        countryOptions={searchOriginOptions}
        specialtyOptions={searchSpecialtyOptions}
        brandSuggestions={createCoffeeBrandSuggestions}
        onChange={setCreateCoffeeDraft}
        onPickImage={onPickCreateCoffeeImage}
        onRemoveImage={onRemoveCreateCoffeeImage}
        onClose={() => setCoffeeSheetStep("select")}
        onSave={async () => {
          const result = await saveCreateCoffee({ fromDiarySheet: true });
          if (result) {
            setDiaryCoffeeDraftWithCaffeine(result.id);
            setLastCreatedCoffeeNameForSheet(result.name);
            setCoffeeSheetStep("dose");
          }
        }}
        fullPage={false}
        hideActions={true}
        hideHead={true}
      />
    </Suspense>
  );

  const isCreateCoffeeFormValid =
    createCoffeeDraft.name.trim() !== "" &&
    createCoffeeDraft.brand.trim() !== "" &&
    createCoffeeDraft.specialty.trim() !== "" &&
    createCoffeeDraft.country.trim() !== "" &&
    createCoffeeDraft.format.trim() !== "" &&
    createCoffeeDraft.totalGrams > 0;

  const createCoffeeFormForPantrySheet = (
    <Suspense fallback={<div className="create-coffee-sheet-loading" aria-live="polite">Cargando...</div>}>
      <LazyCreateCoffeeView
        draft={createCoffeeDraft}
        imagePreviewUrl={createCoffeeImagePreviewUrl}
        saving={createCoffeeSaving}
        error={createCoffeeError}
        countryOptions={createCoffeeCountryOptions}
        specialtyOptions={searchSpecialtyOptions}
        brandSuggestions={createCoffeeBrandSuggestions}
        onChange={setCreateCoffeeDraft}
        onPickImage={onPickCreateCoffeeImage}
        onRemoveImage={onRemoveCreateCoffeeImage}
        onClose={() => setPantrySheetStep("select")}
        onSave={async () => {
          await handleCreateCoffeeNextForPantry();
        }}
        fullPage={false}
        hideActions={true}
        hideHead={true}
        showQuantityField={true}
      />
    </Suspense>
  );

  const handleCreateCoffeeNext = useCallback(async () => {
    const result = await saveCreateCoffee({ fromDiarySheet: true });
    if (result) {
      setDiaryCoffeeDraftWithCaffeine(result.id);
      setLastCreatedCoffeeNameForSheet(result.name);
      setCoffeeSheetStep("dose");
    }
  }, [saveCreateCoffee, setDiaryCoffeeDraftWithCaffeine, setCoffeeSheetStep]);

  const handleCreateCoffeeNextForPantry = useCallback(async () => {
    const result = await saveCreateCoffee({ fromPantrySheet: true });
    if (result) {
      setDiaryPantryCoffeeIdDraft(result.id);
      const suggestedGrams = Math.max(1, Math.round(Number(createCoffeeDraft.totalGrams || 250)));
      setDiaryPantryGramsDraft(String(suggestedGrams));
      setPantrySheetStep("form");
    }
  }, [createCoffeeDraft.totalGrams, saveCreateCoffee, setDiaryPantryCoffeeIdDraft, setDiaryPantryGramsDraft, setPantrySheetStep]);

  useCoffeeSeoMeta(
    detailCoffee,
    {
      avgRating: detailCoffeeAverageRating,
      reviewCount: detailCoffeeReviews.length
    },
    typeof window !== "undefined" ? window.location.pathname : undefined
  );

  const handleNavClick = (tabId: TabId) => {
    if (tabId === "brewlab") setBrewCoffeeId("");
    handleNavClickBase(tabId);
  };

  const guardedActiveTab = resolveGuardedTab(
    activeTab,
    Boolean(sessionEmail),
    activeTab === "search" ? searchMode : undefined
  );

  // GA4: enviar page_view en cada cambio de ruta (SPA)
  const coffeeSlugForGa =
    guardedActiveTab === "coffee" && detailCoffeeId ? coffeeSlugIndex.byId.get(detailCoffeeId) ?? null : null;
  const gaPagePath = buildRoute(guardedActiveTab, searchMode, profileUsername, coffeeSlugForGa, profileSubPanel, profileListId);
  useEffect(() => {
    const titles: Partial<Record<TabId, string>> = {
      home: "Cafesito",
      search: "Explorar",
      brewlab: "Elabora",
      diary: "Diario",
      profile: "Perfil",
      coffee: detailCoffee?.nombre ? `Café · ${detailCoffee.nombre}` : "Café",
      "crear-cafe": "Crea tu café",
      "selecciona-cafe": "Seleccionar café"
    };
    const pageTitle = titles[guardedActiveTab] ?? "Cafesito";
    sendPageView(gaPagePath, `Cafesito - ${pageTitle}`);
  }, [gaPagePath, guardedActiveTab]);

  // GA4: mismo usuario en todas las sesiones cuando está logueado (unifica analíticas por user_id)
  useEffect(() => {
    if (sessionEmail && activeUser?.id != null) {
      setGa4UserId(String(activeUser.id));
    } else {
      setGa4UserId(null);
    }
  }, [sessionEmail, activeUser?.id]);

  // Al cambiar de vista: reiniciar scroll y mostrar topbar para que en timeline, perfil y resto se comporte igual
  useEffect(() => {
    const el = mainScrollRef.current;
    if (!el) return;
    el.scrollTop = 0;
    lastScrollTopRef.current = 0;
    setTopbarScrolled(false);
    const mainShell = el.parentElement;
    if (mainShell) mainShell.style.setProperty("--topbar-translate-y", "0px");
  }, [guardedActiveTab]);

  // Vista detalle café a pantalla completa: clase en html y body para quitar franja superior (notch/status bar)
  useEffect(() => {
    const onCoffee = guardedActiveTab === "coffee";
    document.documentElement.classList.toggle("is-coffee-view", onCoffee);
    document.body.classList.toggle("is-coffee-view", onCoffee);
    return () => {
      document.documentElement.classList.remove("is-coffee-view");
      document.body.classList.remove("is-coffee-view");
    };
  }, [guardedActiveTab]);

  const guestCanAccessCurrentTab = canAccessTabAsGuest(
    guardedActiveTab,
    guardedActiveTab === "search" ? searchMode : undefined
  );
  const showingLogin = (!authReady && !guestCanAccessCurrentTab) || (!sessionEmail && !guestCanAccessCurrentTab);
  useLayoutEffect(() => {
    if (isNotFoundRoute) return;
    if (!authReady) return;
    if (!showingLogin) return;
    const pathname = window.location.pathname;
    const root = (getAppRootPath(pathname) || "/").replace(/\/+$/, "") || "/";
    const current = pathname.replace(/\/+$/, "") || "/";
    if (current !== root) {
      window.history.replaceState({}, "", `${root}${window.location.search}${window.location.hash}`);
    }
  }, [isNotFoundRoute, showingLogin, authReady]);
  const mentionUsersByUsername = useMemo(() => {
    const map = new Map<string, UserRow>();
    users.forEach((user) => {
      const key = String(user.username || "").trim().toLowerCase();
      if (!key) return;
      if (!map.has(key)) map.set(key, user);
    });
    return map;
  }, [users]);
  const resolveMentionUser = useCallback((username: string) => {
    const key = String(username || "").trim().toLowerCase();
    const user = mentionUsersByUsername.get(key);
    if (!user) return null;
    return { username: user.username, avatarUrl: user.avatar_url };
  }, [mentionUsersByUsername]);
  const isSearchUsersPage = guardedActiveTab === "search" && searchMode === "users";
  const navActiveTab = isSearchUsersPage ? "home" : guardedActiveTab === "crear-cafe" || guardedActiveTab === "selecciona-cafe" ? "brewlab" : guardedActiveTab;
  const nav = <BottomNav activeTab={navActiveTab} onNavClick={handleNavClick} avatarUrl={activeUser?.avatar_url ?? null} />;
  const navRail = <DesktopNavRail activeTab={navActiveTab} onNavClick={handleNavClick} avatarUrl={activeUser?.avatar_url ?? null} />;
  const topbarActions = useTopBarActions({
    searchMode,
    resetSearchUi,
    navigateToTab,
    setShowBarcodeScannerSheet,
    isMobileOsDevice,
    setShowNotificationsPanel,
    visibleTimelineNotifications: visibleHomeNotifications,
    setNotificationsLastSeenAt,
    setShowDiaryQuickActions,
    setShowDiaryPeriodSheet,
    showCreateCoffeeComposer,
    closeCreateCoffeeComposer,
    brewStep,
    setBrewStep,
    setTimerSeconds,
    setBrewRunning,
    runWithAuth,
    setDetailOpenStockSignal,
    saveDetailFavorite,
    activeUserId: activeUser?.id ?? null
  });

  if (isNotFoundRoute) {
    const openRandomCoffee = () => {
      if (!coffees.length) {
        window.location.replace(getAppRootPath(window.location.pathname) || "/");
        return;
      }
      const randomCoffee = coffees[Math.floor(Math.random() * coffees.length)];
      const slug = coffeeSlugIndex.byId.get(randomCoffee.id) ?? null;
      const nextPath = buildRoute("coffee", "coffees", null, slug);
      window.location.assign(nextPath);
    };
    return (
      <div className={`layout ${mode}`.trim()}>
        {mode === "desktop" ? navRail : null}
        <main className="main-shell">
          <Suspense fallback={<div className="app-content-loading" aria-hidden="true" />}>
            <LazyNotFoundView
              onGoHome={() => window.location.replace(getAppRootPath(window.location.pathname) || "/")}
              onOpenRandomCoffee={openRandomCoffee}
              randomCoffeeEnabled={coffees.length > 0}
            />
          </Suspense>
        </main>
        {mode === "mobile" ? <footer className="bottom-tabs">{nav}</footer> : null}
        <AuthPromptOverlay
          open={showAuthPrompt}
          authBusy={authBusy}
          authError={authError}
          onClose={() => setShowAuthPrompt(false)}
          onGoogleLogin={() => void handleGoogleLogin()}
          onGoogleCredential={handleGoogleCredential}
        />
      </div>
    );
  }

  if (!authReady && !guestCanAccessCurrentTab) {
    return (
      <div className="app-auth-check" role="status" aria-live="polite" aria-busy="true">
        <div className="app-auth-check-spinner" aria-hidden="true" />
        <p className="app-auth-check-message">Verificando sesión...</p>
      </div>
    );
  }

  if (!sessionEmail && !guestCanAccessCurrentTab) {
    return (
      <LoginGate
        loading={authBusy}
        errorMessage={authError}
        onGoogleLogin={handleGoogleLogin}
        onGoogleCredential={handleGoogleCredential}
      />
    );
  }

  const useRightRailDetail = shouldUseRightRailDetail({
    mode,
    viewportWidth,
    hasDetailPanel: Boolean(detailPanel),
    activeTab: guardedActiveTab,
    detailHostTab
  });
  const activeSidePanelTarget = sidePanelForTab({
    mode,
    activeTab: guardedActiveTab,
    detailHostTab,
    useRightRailDetail
  });
  const isDesktopComposer = mode === "desktop";
  const homeContent =
    guardedActiveTab === "home" ? (
      <LazyHomeView
        mode={mode}
        cards={homeCards}
        recommendations={homeRecommendations}
        suggestions={homeSuggestions}
        suggestionIndices={homeSuggestionIndices}
        followingIds={followingIds}
        followerCounts={followerCounts}
        loading={globalStatus === "Cargando datos..."}
        errorMessage={globalStatus.startsWith("Error") ? globalStatus : null}
        refreshing={homeRefreshing}
        activeUserId={activeUser?.id ?? null}
        onToggleFollow={handleToggleFollow}
        onRefresh={handleRefreshHome}
        onMentionClick={handleMentionNavigation}
        resolveMentionUser={resolveMentionUser}
        onOpenUserProfile={(userId) => {
          navigateToTab("profile", { profileUserId: userId });
        }}
        onOpenCoffee={(coffeeId) => {
          openCoffeeDetail(coffeeId, "home");
        }}
        onOpenSearch={() => navigateToTab("search", { searchMode: "coffees" })}
        activeUserDisplay={activeUser ? { fullName: activeUser.full_name ?? "", username: activeUser.username ?? "", avatarUrl: activeUser.avatar_url ?? null } : null}
        sidePanel={activeSidePanelTarget === "home" ? detailPanel : null}
        onOpenBrewToMethod={handleOpenBrewToMethod}
        orderedBrewMethods={orderedBrewMethods}
        pantryItems={brewPantryItems}
        onPantryCoffeeClick={(coffeeId) => openCoffeeDetail(coffeeId, "home")}
        onAddToPantry={openAddPantrySheetFromHome}
        onUpdatePantryStock={handleUpdatePantryStock}
        onRemovePantryItem={handleRemovePantryItem}
        onMarkPantryCoffeeFinished={handleMarkPantryCoffeeFinished}
      />
    ) : null;
  const searchContent =
    guardedActiveTab === "search" ? (
      <LazySearchView
        mode={searchMode}
        searchQuery={searchQuery}
        onSearchQueryChange={onSearchQueryChange}
        searchFilter={searchFilter}
        onSearchFilterChange={onSearchFilterChange}
        selectedOrigins={searchSelectedOrigins}
        selectedRoasts={searchSelectedRoasts}
        selectedSpecialties={searchSelectedSpecialties}
        selectedFormats={searchSelectedFormats}
        minRating={searchMinRating}
        activeFilterType={searchActiveFilterType}
        onSetActiveFilterType={setSearchActiveFilterType}
        originOptions={searchOriginOptions}
        roastOptions={searchRoastOptions}
        specialtyOptions={searchSpecialtyOptions}
        formatOptions={searchFormatOptions}
        onToggleOrigin={onToggleOrigin}
        onToggleRoast={onToggleRoast}
        onToggleSpecialty={onToggleSpecialty}
        onToggleFormat={onToggleFormat}
        onSetMinRating={setSearchMinRating}
        onClearCoffeeFilters={onClearCoffeeFilters}
        coffees={filteredCoffees}
        users={filteredSearchUsers}
        followerCounts={followerCounts}
        followingCounts={followingCounts}
        followingIds={followingIds}
        selectedCoffee={coffees.find((item) => item.id === searchSelectedCoffeeId) ?? null}
        onSelectCoffee={(coffeeId) => {
          onSelectCoffee(coffeeId);
          openCoffeeDetail(coffeeId, "search");
        }}
        onSelectUser={(userId) => {
          navigateToTab("profile", { profileUserId: userId });
        }}
        onToggleFollow={handleToggleFollow}
        focusCoffeeProfile={searchFocusCoffeeProfile}
        onExitCoffeeFocus={() => setSearchFocusCoffeeProfile(false)}
        sidePanel={activeSidePanelTarget === "search" ? detailPanel : null}
      />
    ) : null;
  const coffeeContent =
    guardedActiveTab === "coffee" ? (
      detailCoffee ? (
        <LazyCoffeeDetailView
          coffee={detailCoffee}
          reviews={detailCoffeeReviews}
          currentUser={detailCurrentUser}
          currentUserReview={detailCurrentUserReviewWithUser}
          avgRating={detailCoffeeAverageRating}
          isFavorite={detailIsFavorite}
          isListActive={isListActive}
          pantry={detailPantryStock}
          sensory={detailSensoryAverages}
          sensoryDraft={detailSensoryDraft}
          stockDraft={detailStockDraft}
          reviewDraftText={detailReviewText}
          reviewDraftRating={detailReviewRating}
          reviewDraftImagePreviewUrl={detailReviewImagePreviewUrl}
          onClose={() => {
            if (window.history.length > 1) window.history.back();
            else navigateToTab("home", { replace: true });
          }}
          onToggleFavorite={fullPageDetailActions.onToggleFavorite}
          onReviewTextChange={setDetailReviewText}
          onReviewRatingChange={setDetailReviewRating}
          onReviewImagePick={(file, previewUrl) => {
            setDetailReviewImageFile(file);
            setDetailReviewImagePreviewUrl(previewUrl);
          }}
          onSaveReview={fullPageDetailActions.onSaveReview}
          onDeleteReview={fullPageDetailActions.onDeleteReview}
          canDeleteReview={Boolean(detailCurrentUserReview)}
          onSensoryDraftChange={setDetailSensoryDraft}
          onSaveSensory={fullPageDetailActions.onSaveSensory}
          onStockDraftChange={setDetailStockDraft}
          onSaveStock={fullPageDetailActions.onSaveStock}
          onOpenUserProfile={fullPageDetailActions.onOpenUserProfile}
          isGuest={!sessionEmail}
          onRequireAuth={requestLogin}
          fullPage
          externalOpenStockSignal={detailOpenStockSignal}
          userLists={userLists}
          onCreateList={handleCreateList}
          onAddCoffeeToList={
            detailCoffee?.id
              ? async (listId) => {
                  await addUserListItem(listId, detailCoffee.id);
                  setCoffeeIdsInUserLists((prev) =>
                    prev.includes(detailCoffee.id) ? prev : [...prev, detailCoffee.id]
                  );
                }
              : undefined
          }
        />
      ) : (
        <article className="coffee-detail-empty coffee-detail-empty-full">
          <h2 className="title">Café no encontrado</h2>
          <p className="coffee-sub">La URL no corresponde a ningún café disponible.</p>
          <Button
            variant="primary"
            onClick={() => {
              navigateToTab("search", { searchMode: "coffees", replace: true });
            }}
          >
            Volver a Explorar
          </Button>
        </article>
      )
    ) : null;
  const brewContent =
    guardedActiveTab === "brewlab" ? (
      showCreateCoffeeComposer && mode !== "desktop" ? (
        <section className="create-coffee-mobile-screen">
          <Suspense fallback={<div className="create-coffee-sheet-loading" aria-live="polite">Cargando...</div>}>
            {createCoffeePanel}
          </Suspense>
        </section>
      ) : (
        <LazyBrewLabView
          brewStep={brewStep}
          setBrewStep={setBrewStep}
          brewMethod={brewMethod}
          setBrewMethod={setBrewMethod}
          brewCoffeeId={brewCoffeeId}
          setBrewCoffeeId={setBrewCoffeeId}
          brewPantryItemId={brewPantryItemId}
          setBrewPantryItemId={setBrewPantryItemId}
          brewDrinkType={brewDrinkType}
          setBrewDrinkType={setBrewDrinkType}
          coffees={brewCoffeeCatalog}
          orderedBrewMethods={orderedBrewMethods}
          pantryItems={brewPantryItems}
          onAddNotFoundCoffee={handleOpenCreateCoffee}
          onAddToPantry={openCoffeeSheetForBrew}
          waterMl={waterMl}
          setWaterMl={setWaterMl}
          ratio={ratio}
          coffeeGrams={brewCoffeeGrams}
          setCoffeeGrams={setBrewCoffeeGrams}
          espressoTimeSeconds={espressoTimeSeconds}
          setEspressoTimeSeconds={setEspressoTimeSeconds}
          timerSeconds={timerSeconds}
          setTimerSeconds={setTimerSeconds}
          brewRunning={brewRunning}
          setBrewRunning={setBrewRunning}
          selectedCoffee={selectedCoffee}
          onSaveResultToDiary={saveBrewToDiary}
          onBrewResultSaveState={onBrewResultSaveState}
          onSaveWaterFromBrew={async (amountMl) => {
            await saveWaterWithAmount(amountMl);
            setBrewStep("method");
            setBrewMethod("");
            setBrewCoffeeId("");
            setBrewPantryItemId("");
          }}
          showBarcodeButton={isMobileOsDevice}
          onBarcodeClick={() => {
            setBarcodeOrigin("brew");
            setShowBarcodeScannerSheet(true);
          }}
          barcodeDetectedValue={barcodeDetectedValueForBrew}
          onClearBarcodeDetectedValue={() => setBarcodeDetectedValueForBrew(null)}
          brewTimerEnabled={brewTimerEnabled}
          setBrewTimerEnabled={setBrewTimerEnabled}
          onTimerEndedGoToConsumption={onBrewTimerEndedGoToConsumption}
          onTimerEndedChange={setBrewTimerEnded}
        />
      )
    ) : null;
  const diaryContent =
    guardedActiveTab === "diary" ? (
      diarySubView === "cafes-probados" ? (
        <CafesProbadosView
          coffeesWithFirstTried={diaryCoffeesWithFirstTried}
          onBack={() => navigateToTab("diary")}
          onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "diary", { diarySubView: "cafes-probados" })}
        />
      ) : (
        <LazyDiaryView
          mode={mode}
          period={diaryPeriod}
          selectedDiaryDate={selectedDiaryDate}
          selectedDiaryMonth={selectedDiaryMonth}
          entries={diaryEntriesActivity}
          coffeeCatalog={coffeeCatalogIncludingCustom}
          pantryRows={pantryCoffeeRows}
          orderedBrewMethods={orderedBrewMethods}
          onDeleteEntry={handleDeleteDiaryEntry}
          onEditEntry={handleUpdateDiaryEntry}
          onUpdatePantryStock={handleUpdatePantryStock}
          onRemovePantryItem={handleRemovePantryItem}
          onMarkPantryCoffeeFinished={handleMarkPantryCoffeeFinished}
          onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "diary")}
          onOpenCafesProbados={() => navigateToTab("diary", { diarySubView: "cafes-probados" })}
        />
      )
    ) : null;

  const profileContent =
    guardedActiveTab === "profile" && profileUser ? (
      profileSubPanel === "historial" ? (
        <HistorialView
          finishedCoffees={finishedCoffees}
          coffeeCatalog={coffeeCatalogIncludingCustom}
          onBack={() => {
            setProfileSubPanel(null);
            navigateToTab("profile", { replace: true });
          }}
          onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "profile")}
        />
      ) : profileSubPanel === "followers" ? (
        <ProfileUsersListView
          title="Seguidores"
          users={profileFollowersUsers}
          followerCounts={followerCounts}
          followingCounts={followingCounts}
          followingIds={followingIds}
          onSelectUser={(userId) => navigateToTab("profile", { profileUserId: userId })}
          onToggleFollow={handleToggleFollow}
        />
      ) : profileSubPanel === "following" ? (
        <ProfileUsersListView
          title="Siguiendo"
          users={profileFollowingUsers}
          followerCounts={followerCounts}
          followingCounts={followingCounts}
          followingIds={followingIds}
          onSelectUser={(userId) => navigateToTab("profile", { profileUserId: userId })}
          onToggleFollow={handleToggleFollow}
        />
      ) : profileSubPanel === "favorites" ? (
        <FavoritosListView
          favoriteCoffees={favoriteCoffees}
          onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "profile")}
          onRemoveFavorite={async (coffeeId) => {
            if (!activeUser) return;
            const exists = favorites.some((item) => item.user_id === activeUser.id && item.coffee_id === coffeeId);
            const result = await toggleFavoriteCoffee(activeUser.id, coffeeId, exists);
            setFavorites((prev) => {
              if (exists) {
                return prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === coffeeId));
              }
              if (!result) return prev;
              return [
                result,
                ...prev.filter((item) => !(item.user_id === result.user_id && item.coffee_id === result.coffee_id))
              ];
            });
          }}
        />
      ) : profileSubPanel === "list" && profileListId && listOptionsView ? (
        (() => {
          const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
          const isOwner = list != null && activeUser != null && list.user_id === activeUser.id;
          const listPrivacy: ListPrivacy = list?.privacy ?? (list?.is_public ? "public" : "private");
          const listMembersCanEdit = list?.members_can_edit ?? false;
          const listMembersCanInvite = list?.members_can_invite ?? false;
          const handlePrivacyChange = async (privacy: ListPrivacy, membersCanEdit: boolean, membersCanInvite?: boolean) => {
            if (!profileListId || !list?.name) return;
            try {
              await updateUserListWithPrivacy(profileListId, list.name, privacy, membersCanEdit, membersCanInvite);
              setUserLists((prev) =>
                prev.map((l) =>
                  l.id === profileListId
                    ? { ...l, is_public: privacy === "public", privacy, members_can_edit: membersCanEdit, members_can_invite: membersCanInvite ?? l.members_can_invite }
                    : l
                )
              );
              if (profileListMeta?.id === profileListId) {
                setProfileListMeta((m) =>
                  m?.id === profileListId ? { ...m, is_public: privacy === "public", privacy, members_can_edit: membersCanEdit, members_can_invite: membersCanInvite ?? m.members_can_invite } : m
                );
              }
            } catch (err) {
              console.error("Error al actualizar privacidad:", err);
            }
          };
          const shareUrl = `${typeof window !== "undefined" ? window.location.origin : ""}${buildRoute("profile", "coffees", null, null, "list", profileListId)}`;
          const followerIds = activeUser
            ? new Set(follows.filter((f) => f.followed_id === activeUser.id).map((f) => f.follower_id))
            : new Set<number>();
          const mutualFollowIds = new Set([...followingIds].filter((id) => followerIds.has(id)));
          return (
            <ListOptionsPage
              list={list}
              isOwner={isOwner}
              listPrivacy={listPrivacy}
              listMembersCanEdit={listMembersCanEdit}
              listMembersCanInvite={listMembersCanInvite}
              onPrivacyChange={handlePrivacyChange}
              listOptionsMembers={listOptionsMembers}
              listOptionsMemberUsers={listOptionsMemberUsers}
              users={users.filter((u) => u.id !== activeUser?.id)}
              currentUserId={activeUser!.id}
              shareUrl={shareUrl}
              invitingId={listOptionsInvitingId}
              mutualFollowIds={mutualFollowIds}
              invitations={listOptionsInvitations}
              onInvite={async (inviteeId) => {
                setListOptionsInvitingId(inviteeId);
                try {
                  await createListInvitation(profileListId, inviteeId);
                  const [invitations] = await Promise.all([
                    fetchListInvitationsByListId(profileListId),
                    fetchListMembersByListId(profileListId)
                  ]);
                  setListOptionsInvitations(invitations);
                  setListOptionsMembers(await fetchListMembersByListId(profileListId));
                } catch (err) {
                  console.error("Error invitando a la lista:", err);
                } finally {
                  setListOptionsInvitingId(null);
                }
              }}
              onCopyLink={() => {
                navigator.clipboard.writeText(shareUrl).catch(() => {
                  const input = document.createElement("input");
                  input.value = shareUrl;
                  document.body.appendChild(input);
                  input.select();
                  document.execCommand("copy");
                  document.body.removeChild(input);
                });
                setShowCopyChip(true);
              }}
              onRemoveMember={async (userId) => {
                try {
                  await leaveList(profileListId, userId);
                  const members = await fetchListMembersByListId(profileListId);
                  setListOptionsMembers(members);
                  const listForMeta = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
                  const ownerId = listForMeta?.user_id;
                  if (ownerId != null) {
                    const ids = [ownerId, ...members.map((m) => m.user_id)];
                    const rows = await fetchUsersByIds([...new Set(ids)]);
                    setListOptionsMemberUsers(rows);
                  }
                } catch (err) {
                  console.error("Error al eliminar miembro:", err);
                }
              }}
              showCopyChip={showCopyChip}
              copyChipExiting={copyChipExiting}
              onEditList={() => { sendEvent("modal_open", { modal_id: "list_edit" }); setShowEditListSheet(true); }}
              onDeleteList={() => { sendEvent("modal_open", { modal_id: "delete_confirm_list" }); setShowDeleteListConfirmSheet(true); }}
              onLeaveList={() => { sendEvent("modal_open", { modal_id: "leave_list_confirm" }); setShowLeaveListConfirmSheet(true); }}
            />
          );
        })()
      ) : profileSubPanel === "list" && profileListId ? (
        (() => {
          const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
          const isOwner = list != null && activeUser != null && list.user_id === activeUser.id;
          const canEditList = isOwner || (list?.members_can_edit === true && !isOwner);
          return (
            <FavoritosListView
              favoriteCoffees={listDetailCoffees}
              onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "profile")}
              onRemoveFavorite={
                canEditList
                  ? async (coffeeId) => {
                      await removeUserListItem(profileListId, coffeeId);
                      setListDetailItemIds((prev) => prev.filter((id) => id !== coffeeId));
                    }
                  : undefined
              }
            />
          );
        })()
      ) : (
      <LazyProfileView
        user={profileUser}
        mode={mode}
        tab={profileTab}
        setTab={setProfileTab}
        followedActivity={
          profileUser.id === activeUser?.id
            ? profileMineAndFollowedActivity
            : profileUserActivity
        }
        activityIsProfileUser={profileUser.id !== activeUser?.id}
        favoriteCoffees={profileUser.id === activeUser?.id ? favoriteCoffees : []}
        allCoffees={coffees}
        coffeeReviews={coffeeReviews}
        coffeeSensoryProfiles={coffeeSensoryProfiles}
        followers={followersCount}
        following={followingCount}
        onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "profile")}
        onOpenUserList={(userId, listId) => {
          if (listId === "favorites") {
            navigateToTab("profile", { profileUserId: userId, profileSection: "favorites" });
          } else {
            navigateToTab("profile", { profileUserId: userId, profileSection: "list", profileListId: listId });
          }
        }}
        onOpenUserProfile={(userId) => navigateToTab("profile", { profileUserId: userId })}
        onOpenFollowers={() => navigateToTab("profile", { profileSection: "followers" })}
        onOpenFollowing={() => navigateToTab("profile", { profileSection: "following" })}
        onOpenFavoritesList={() => navigateToTab("profile", { profileSection: "favorites" })}
        canEditProfile={profileUser.id === activeUser?.id}
        canFollowProfile={Boolean(activeUser && profileUser.id !== activeUser.id)}
        isFollowingProfile={Boolean(profileUser && followingIds.has(profileUser.id))}
        onToggleFollowProfile={async () => {
          if (!profileUser) return;
          await handleToggleFollow(profileUser.id);
        }}
        onSaveProfile={handleUpdateProfile}
        onRemoveFavorite={async (coffeeId) => {
          if (!activeUser) return;
          const exists = favorites.some((item) => item.user_id === activeUser.id && item.coffee_id === coffeeId);
          const result = await toggleFavoriteCoffee(activeUser.id, coffeeId, exists);
          setFavorites((prev) => {
            if (exists) {
              return prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === coffeeId));
            }
            if (!result) return prev;
            return [
              result,
              ...prev.filter((item) => !(item.user_id === result.user_id && item.coffee_id === result.coffee_id))
            ];
          });
        }}
        userLists={profileUser.id === activeUser?.id ? userLists : []}
        onCreateList={handleCreateList}
        onOpenList={(listId) => navigateToTab("profile", { profileSection: "list", profileListId: listId })}
        externalEditProfileSignal={profileEditSignal}
        sidePanel={activeSidePanelTarget === "profile" ? detailPanel : null}
        profileDiaryCoffeeIds={
          (profileUser.id === activeUser?.id
            ? diaryEntries.filter((e) => (e.type ?? "").toUpperCase() !== "WATER").map((e) => e.coffee_id)
            : profileUserDiaryEntries.filter((e) => (e.type ?? "").toUpperCase() !== "WATER").map((e) => e.coffee_id)
          ).filter((id): id is string => id != null)
        }
        profileListCoffeeIds={
          profileUser.id === activeUser?.id ? coffeeIdsInUserLists : profileUserListItems.map((i) => i.coffee_id)
        }
        onExploreCafes={() => navigateToTab("search", { searchMode: "coffees" })}
        activeUserId={activeUser?.id ?? null}
      />
      )
    ) : null;
  const authPromptOverlay = (
    <AuthPromptOverlay
      open={showAuthPrompt}
      authBusy={authBusy}
      authError={authError}
      onClose={() => setShowAuthPrompt(false)}
      onGoogleLogin={() => void handleGoogleLogin()}
      onGoogleCredential={handleGoogleCredential}
    />
  );
  const barcodeScannerOverlay = (
    <MobileBarcodeScannerSheet
      open={showBarcodeScannerSheet}
      onClose={() => {
        setShowBarcodeScannerSheet(false);
        setBarcodeOrigin(null);
      }}
      onDetected={(value) => {
        setShowBarcodeScannerSheet(false);
        if (barcodeOrigin === "diary") {
          setBarcodeDetectedValueForDiary(value);
        } else if (barcodeOrigin === "brew") {
          setBarcodeDetectedValueForBrew(value);
        } else if (barcodeOrigin === "createPostCoffee") {
          setCreatePostCoffeeQuery(value);
        } else {
          // Buscador: ir a búsqueda en modo cafés; si hay 1 resultado se abre detalle en un efecto
          barcodeSearchPendingRef.current = true;
          navigateToTab("search", { searchMode: "coffees" });
          onSearchQueryChange(value);
        }
        setBarcodeOrigin(null);
      }}
    />
  );
  const notificationsOverlay = (
    <NotificationsSheet
      open={showNotificationsPanel}
      notifications={visibleHomeNotifications}
      usersById={usersById}
      notificationsLastSeenAt={notificationsLastSeenAt}
      followingIds={followingIds}
      dismissingNotificationIds={dismissingNotificationIds}
      onClose={closeNotificationsPanel}
      onDismiss={(id) => {
        dismissNotification(id, async (dismissedId) => {
          try {
            const { deleteNotification: apiDelete } = await import("../data/supabaseApi");
            await apiDelete(Number(dismissedId));
            setNotifications((prev) => prev.filter((n) => String(n.id) !== dismissedId));
          } catch (err) {
            console.error("Failed to delete notification:", err);
          }
        });
      }}
      onToggleFollow={handleToggleFollow}
      onOpenCommentThread={() => closeNotificationsPanel()}
      onOpenUserProfile={(userId) => {
        navigateToTab("profile", { profileUserId: userId });
        closeNotificationsPanel();
      }}
      onAcceptListInvite={async (invitationId) => {
        try {
          const { acceptListInvitation } = await import("../data/supabaseApi");
          await acceptListInvitation(invitationId);
          setNotifications((prev) => prev.filter((n) => n.related_id !== invitationId));
          closeNotificationsPanel();
          reloadInitialData();
        } catch (err) {
          console.error("Accept list invite failed:", err);
        }
      }}
      onDeclineListInvite={async (invitationId) => {
        try {
          const { declineListInvitation } = await import("../data/supabaseApi");
          await declineListInvitation(invitationId);
          setNotifications((prev) => prev.filter((n) => n.related_id !== invitationId));
        } catch (err) {
          console.error("Decline list invite failed:", err);
        }
      }}
    />
  );
  const diarySheetsOverlay = (
    <DiarySheets
      isActive={guardedActiveTab === "diary" || guardedActiveTab === "brewlab" || showDiaryAddPantrySheet || showDiaryCoffeeSheet}
      showPeriodSheet={showDiaryPeriodSheet}
      showWaterSheet={showDiaryWaterSheet}
      showCoffeeSheet={showDiaryCoffeeSheet}
      coffeeSheetOpenedDirectlyToDose={coffeeSheetOpenedDirectlyToDose}
      showAddPantrySheet={showDiaryAddPantrySheet}
      hideAddPantrySheetBolt={addPantrySheetHideBolt}
      onClosePeriodSheet={() => setShowDiaryPeriodSheet(false)}
      showCalendarSheet={showDiaryCalendarSheet}
      onCloseCalendarSheet={() => setShowDiaryCalendarSheet(false)}
      selectedDiaryDate={selectedDiaryDate}
      setSelectedDiaryDate={setSelectedDiaryDate}
      selectedDiaryMonth={selectedDiaryMonth}
      setSelectedDiaryMonth={setSelectedDiaryMonth}
      currentMonthKey={currentMonthKey}
      diaryTodayStr={todayStr}
      diaryEntries={diaryEntries}
      onCloseWaterSheet={() => setShowDiaryWaterSheet(false)}
      onCloseCoffeeSheet={() => {
        setLastCreatedCoffeeNameForSheet(null);
        setCoffeeSheetOpenedDirectlyToDose(false);
        setCoffeeSheetOpenedFromBrew(false);
        setCoffeeSheetStep("select");
        setShowDiaryCoffeeSheet(false);
      }}
      coffeeSheetOpenedFromBrew={coffeeSheetOpenedFromBrew}
      onCoffeeSelectedForBrew={(coffeeId: string) => {
        setBrewCoffeeId(coffeeId);
        setCoffeeSheetOpenedFromBrew(false);
        setCoffeeSheetStep("select");
        setShowDiaryCoffeeSheet(false);
      }}
      onCloseAddPantrySheet={() => {
        addPantryOpenedFromBrewRef.current = false;
        addPantryOpenedFromHomeRef.current = false;
        lastSelectedPantryCoffeeRef.current = null;
        setAddPantrySheetHideBolt(false);
        setPantrySheetStep("select");
        setShowDiaryAddPantrySheet(false);
      }}
      onOpenWaterSheet={openWaterSheet}
      onOpenCoffeeSheet={() => {
        setCoffeeSheetStep("select");
        openCoffeeSheet();
      }}
      onOpenAddPantrySheet={openAddPantrySheetWithClear}
      diaryPeriod={diaryPeriod}
      setDiaryPeriod={setDiaryPeriod}
      diaryWaterMlDraft={diaryWaterMlDraft}
      setDiaryWaterMlDraft={setDiaryWaterMlDraft}
      onSaveWater={saveWater}
      pantryCoffeeRows={pantryCoffeeRows}
      activeUserId={activeUser?.id ?? null}
      diaryCoffeeOptions={diaryCoffeeOptions}
      coffeeSheetStep={coffeeSheetStep}
      setCoffeeSheetStep={setCoffeeSheetStep}
      diaryCoffeeIdDraft={diaryCoffeeIdDraft}
      setDiaryCoffeeIdDraft={setDiaryCoffeeDraftWithCaffeine}
      diarySelectedPantryItemIdDraft={diarySelectedPantryItemIdDraft}
      setDiarySelectedPantryItemIdDraft={setDiarySelectedPantryItemIdDraft}
      diaryCoffeeGramsDraft={diaryCoffeeGramsDraft}
      setDiaryCoffeeGramsDraft={setDiaryCoffeeGramsDraft}
      createCoffeeFormContent={createCoffeeFormForSheet}
      onCreateCoffeeNext={handleCreateCoffeeNext}
      createCoffeeFormForPantrySheet={createCoffeeFormForPantrySheet}
      onCreateCoffeeNextForPantry={handleCreateCoffeeNextForPantry}
      isCreateCoffeeFormValid={isCreateCoffeeFormValid}
      diaryCoffeePreparationDraft={diaryCoffeePreparationDraft}
      setDiaryCoffeePreparationDraft={setDiaryCoffeePreparationDraft}
      diaryCoffeeMlDraft={diaryCoffeeMlDraft}
      setDiaryCoffeeMlDraft={setDiaryCoffeeMlDraft}
      diaryCoffeeCaffeineDraft={diaryCoffeeCaffeineDraft}
      setDiaryCoffeeCaffeineDraft={setDiaryCoffeeCaffeineDraft}
      onSaveCoffee={saveCoffee}
      lastCreatedCoffeeNameForSheet={lastCreatedCoffeeNameForSheet}
      pantrySheetStep={pantrySheetStep}
      setPantrySheetStep={setPantrySheetStep}
      diaryPantryCoffeeIdDraft={diaryPantryCoffeeIdDraft}
      setDiaryPantryCoffeeIdDraft={setDiaryPantryCoffeeIdDraft}
      diaryPantryGramsDraft={diaryPantryGramsDraft}
      setDiaryPantryGramsDraft={setDiaryPantryGramsDraft}
      onSavePantry={handleSavePantry}
      selectedDiaryPantryCoffee={selectedDiaryPantryCoffee}
      customCoffees={customCoffees}
      onSelectPantryCoffee={(coffee) => {
        lastSelectedPantryCoffeeRef.current = coffee;
      }}
      showBarcodeButton={isMobileOsDevice}
      onBarcodeClick={() => {
        setBarcodeOrigin("diary");
        setShowBarcodeScannerSheet(true);
      }}
      barcodeDetectedValue={barcodeDetectedValueForDiary}
      onClearBarcodeDetectedValue={() => setBarcodeDetectedValueForDiary(null)}
    />
  );
  const showAndroidBanner = mode === "mobile" && isAndroidMobile && !androidBannerDismissed;

  return (
    <div
      className={`layout ${mode} ${mode === "desktop" && isSearchUsersPage ? "is-search-users-page" : ""} ${guardedActiveTab === "home" ? "is-home" : ""} ${guardedActiveTab === "coffee" ? "is-coffee" : ""}`.trim()}
    >
      {showAndroidBanner ? (
        <div className="android-install-banner-wrap">
          <aside className="android-install-banner android-install-banner-fixed" role="banner" aria-label="Instalar app">
            <a
              href="https://play.google.com/store/apps/details?id=com.cafesito.app"
              target="_blank"
              rel="noopener noreferrer"
              className="android-install-banner-link"
            >
              <span className="android-install-banner-icon" aria-hidden="true">
                <UiIcon name="shop" className="ui-icon" />
              </span>
              <span className="android-install-banner-text">Instala la app Cafesito en Google Play</span>
            </a>
            <Button
              variant="plain"
              type="button"
              className="android-install-banner-dismiss"
              aria-label="Cerrar"
              onClick={dismissAndroidBanner}
            >
              <UiIcon name="close" className="ui-icon" />
            </Button>
          </aside>
          <div className="android-install-banner-spacer" aria-hidden="true" />
        </div>
      ) : null}
      <OfflineBanner />
      <CookieConsentBanner isAuthenticated={Boolean(sessionEmail)} />
      {mode === "desktop" && !isSearchUsersPage ? navRail : null}
      <main className={`main-shell${guardedActiveTab === "home" ? " is-home" : ""}`}>
        {guardedActiveTab !== "coffee" ? (
        <TopBar
          activeTab={guardedActiveTab}
          diarySubView={diarySubView}
          searchQuery={searchQuery}
          searchMode={searchMode}
          onSearchQueryChange={onSearchQueryChange}
          onSearchCancel={resetSearchUi}
          onSearchBarcodeClick={() => {
            setBarcodeOrigin("search");
            topbarActions.onSearchBarcodeClick();
          }}
          showSearchBarcodeButton={isMobileOsDevice && searchMode === "coffees"}
          showSearchCoffeeFilterChips={searchMode === "coffees" && !searchFocusCoffeeProfile}
          searchOriginCount={searchSelectedOrigins.size}
          searchSpecialtyCount={searchSelectedSpecialties.size}
          searchRoastCount={searchSelectedRoasts.size}
          searchFormatCount={searchSelectedFormats.size}
          searchHasRatingFilter={searchMinRating > 0}
          onOpenSearchFilter={(filter) => { sendEvent("modal_open", { modal_id: "search_filter" }); setSearchActiveFilterType(filter); }}
          onSearchBack={topbarActions.onSearchBack}
          showNotificationsBadge={showNotificationsBadge}
          onHomeSearchUsers={topbarActions.onHomeSearchUsers}
          onHomeNotifications={topbarActions.onHomeNotifications}
          diaryPeriod={diaryPeriod}
          diaryDateLabel={
            diaryPeriod === "hoy" ? "Hoy" : diaryPeriod === "7d" || diaryPeriod === "week" ? formatWeekRange(selectedDiaryDate) : formatMonthYear(selectedDiaryMonth)
          }
          diarySelectedDate={selectedDiaryDate}
          diarySelectedMonth={selectedDiaryMonth}
          diaryTodayStr={todayStr}
          currentMonthKey={currentMonthKey}
          canDiaryGoNext={
            diaryPeriod === "30d"
              ? selectedDiaryMonth < currentMonthKey
              : diaryPeriod === "7d" || diaryPeriod === "week"
                ? selectedDiaryDate !== getMondayOfWeek(todayStr)
                : false
          }
          onDiaryPrev={() => {
            if (diaryPeriod === "30d") {
              const [y, m] = selectedDiaryMonth.split("-").map(Number);
              const d = new Date(y, (m ?? 1) - 1, 1);
              d.setMonth(d.getMonth() - 1);
              setSelectedDiaryMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
            } else if (diaryPeriod === "7d" || diaryPeriod === "week") {
              const d = new Date(selectedDiaryDate + "T12:00:00");
              d.setDate(d.getDate() - 7);
              setSelectedDiaryDate(d.toISOString().slice(0, 10));
            }
          }}
          onDiaryNext={() => {
            if (diaryPeriod === "30d" && selectedDiaryMonth < currentMonthKey) {
              const [y, m] = selectedDiaryMonth.split("-").map(Number);
              const d = new Date(y, (m ?? 1) - 1, 1);
              d.setMonth(d.getMonth() + 1);
              setSelectedDiaryMonth(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}`);
            } else if (diaryPeriod === "7d" || diaryPeriod === "week") {
              const d = new Date(selectedDiaryDate + "T12:00:00");
              d.setDate(d.getDate() + 7);
              const todayMon = new Date(getMondayOfWeek(todayStr) + "T12:00:00");
              const nextMon = new Date(d.getFullYear(), d.getMonth(), d.getDate());
              if (nextMon.getTime() > todayMon.getTime()) return;
              setSelectedDiaryDate(d.toISOString().slice(0, 10));
            }
          }}
          onDiaryOpenPeriodSelector={topbarActions.onDiaryOpenPeriodSelector}
          scrolled={topbarScrolled}
          hidden={false}
          brewStep={brewStep}
          brewStepTitle={getBrewStepTitle(brewStep)}
          onBrewBack={topbarActions.onBrewBack}
          onBrewForward={topbarActions.onBrewForward}
          brewCanGoToConfig={guardedActiveTab === "brewlab" && brewStep === "method" && (brewMethod === "Agua" ? waterMl > 0 : Boolean(brewMethod))}
          brewTimerEnabled={brewTimerEnabled}
          brewTimerEnded={brewTimerEnded}
          onBrewGoToConfig={() => {
            if (brewMethod === "Agua") {
              void saveWaterWithAmount(waterMl).then(() => {
                setBrewStep("method");
                setBrewMethod("");
                setBrewCoffeeId("");
                navigateToTab("diary");
              });
              return;
            }
            sendEvent("button_click", { button_id: "brew_next_step" });
            if (brewTimerEnabled) {
              setTimerSeconds(0);
              setBrewRunning(false);
              setBrewStep("brewing");
            } else {
              setBrewStep("result");
            }
          }}
          onBrewGoToConsumptionWhenTimerEnded={() => {
            setBrewTimerEnded(false);
            onBrewTimerEndedGoToConsumption();
          }}
          onBrewResultSave={() => void brewResultSaveRef.current()}
          brewResultCanSave={brewResultSaveMeta.canSave}
          brewResultSaving={brewResultSaveMeta.saving}
          brewResultShowGuardar={brewResultSaveMeta.showGuardar}
          brewSelectCoffeePageOpen={guardedActiveTab === "selecciona-cafe" || brewSelectCoffeePageOpen}
          onBrewSelectCoffeeBack={handleCloseSeleccionaCafe}
          brewCreateCoffeeOpen={guardedActiveTab === "crear-cafe" || showCreateCoffeeComposer}
          onBrewCreateCoffeeBack={handleCloseCreateCoffee}
          onBrewCreateCoffeeSave={() => void saveCreateCoffee({ fromBrewChooser: true })}
          brewCreateCoffeeFormValid={isCreateCoffeeFormValid}
          brewCreateCoffeeSaving={createCoffeeSaving}
          onProfileSignOut={handleSignOut}
          onProfileDeleteAccount={handleDeleteAccount}
          profileMenuEnabled={Boolean(profileUser && activeUser && profileUser.id === activeUser.id)}
          onProfileOpenEdit={triggerProfileEdit}
          onHistorialClick={() => {
            navigateToTab("profile", { profileSection: "historial" });
          }}
          profileSubPanel={profileSubPanel}
          profileListName={
            profileListId
              ? listOptionsView
                ? "Opciones"
                : (profileListMeta?.name ?? userLists.find((l) => l.id === profileListId)?.name ?? "Lista")
              : undefined
          }
          onOpenListOptionsSheet={
            profileSubPanel === "list" && profileListId && userLists.some((l) => l.id === profileListId) && !listOptionsView
              ? handleOpenListOptionsPage
              : undefined
          }
          showShareListButton={
            (() => {
              if (profileSubPanel !== "list" || !profileListId || !activeUser) return false;
              const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
              return (list?.user_id ?? null) === activeUser.id;
            })()
          }
          listMemberCount={
            (() => {
              if (profileSubPanel !== "list" || !profileListId || !activeUser) return undefined;
              const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
              if (list?.privacy !== "public" && list?.privacy !== "invitation") return undefined;
              const isOwner = (list?.user_id ?? null) === activeUser.id;
              const isMember = !isOwner && userLists.some((l) => l.id === profileListId);
              if (!isOwner && !isMember) return undefined;
              const total = 1 + listViewMembers.length;
              return total;
            })()
          }
          listMemberPreviews={
            (() => {
              if (profileSubPanel !== "list" || !profileListId || !activeUser) return undefined;
              const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
              if (list?.privacy !== "public" && list?.privacy !== "invitation") return undefined;
              const isOwner = (list?.user_id ?? null) === activeUser.id;
              const isMember = !isOwner && userLists.some((l) => l.id === profileListId);
              if (!isOwner && !isMember) return undefined;
              const ownerId = list?.user_id;
              const orderedIds = ownerId != null
                ? [ownerId, ...listViewMembers.map((m) => m.user_id)].filter((id, i, a) => a.indexOf(id) === i).slice(0, 3)
                : listViewMembers.map((m) => m.user_id).slice(0, 3);
              if (orderedIds.length === 0) return undefined;
              return orderedIds.map((id) => {
                const u = listViewMemberUsers.find((x) => x.id === id) ?? users.find((x) => x.id === id);
                return { avatar_url: u?.avatar_url ?? null };
              });
            })()
          }
          showJoinPublicListButton={
            (() => {
              if (profileSubPanel !== "list" || !profileListId || !profileUser || !activeUser || profileUser.id === activeUser.id) return false;
              const list = userLists.find((l) => l.id === profileListId) ?? profileListMeta;
              const isPublicOrInvitation = list?.privacy === "public" || list?.privacy === "invitation";
              return Boolean(isPublicOrInvitation && !userLists.some((l) => l.id === profileListId));
            })()
          }
          onJoinPublicList={
            profileListId && activeUser
              ? async () => {
                  try {
                    const { joinPublicList } = await import("../data/supabaseApi");
                    await joinPublicList(profileListId);
                    const [owned, shared] = await Promise.all([
                      fetchUserLists(activeUser.id),
                      fetchSharedWithMeLists(activeUser.id)
                    ]);
                    const byId = new Map<string, UserListRow>();
                    [...owned, ...shared].forEach((l) => byId.set(l.id, l));
                    setUserLists(Array.from(byId.values()));
                  } catch (err) {
                    console.error("Join public list failed:", err);
                  }
                }
              : undefined
          }
          onHistorialBack={
            listOptionsView && profileListId
              ? () => navigateToTab("profile", { profileSection: "list", profileListId, listOptionsView: false })
              : () => navigateToTab("profile", { profileSection: null, profileListId: null, replace: true })
          }
          onCoffeeBack={() => {
            void runWithAuth(async () => {
              if (window.history.length > 1) {
                window.history.back();
                return;
              }
              navigateToTab("home", { replace: true });
            });
          }}
          coffeeTopbarFavoriteActive={false}
          coffeeTopbarStockActive={false}
          onCoffeeTopbarToggleFavorite={topbarActions.onCoffeeTopbarToggleFavorite}
          onCoffeeTopbarOpenStock={topbarActions.onCoffeeTopbarOpenStock}
        />
        ) : null}
        <div
          ref={mainScrollRef}
          className={`main-shell-scroll ${activeTab === "coffee" ? "is-coffee" : ""} ${guardedActiveTab === "search" && searchMode === "coffees" ? "is-search-coffees" : ""} ${guardedActiveTab === "search" && searchMode === "users" ? "is-search-users" : ""} ${guardedActiveTab === "home" ? "is-home" : ""} ${guardedActiveTab === "profile" ? "is-profile" : ""} ${guardedActiveTab === "diary" && diarySubView === "cafes-probados" ? "is-cafes-probados" : ""}`.trim()}
        >
          <Suspense fallback={<div className="app-content-loading" aria-hidden="true" />}>
            <AppContentRouter
              activeTab={guardedActiveTab}
              mode={mode}
              homeContent={homeContent}
              searchContent={searchContent}
              coffeeContent={coffeeContent}
              brewContent={brewContent}
              diaryContent={diaryContent}
              profileContent={profileContent}
              crearCafeContent={crearCafeContent}
              seleccionarCafeContent={seleccionarCafeContent}
            />
          </Suspense>
        </div>
      </main>

      {mode === "desktop" && !(guardedActiveTab === "search" && !detailCoffee) ? (
        <aside
          className={useRightRailDetail || (guardedActiveTab === "brewlab" && showCreateCoffeeComposer) ? "detail-rail-fixed" : "fab-rail"}
          aria-label={useRightRailDetail ? "Detalle cafe" : guardedActiveTab === "brewlab" && showCreateCoffeeComposer ? "Crea tu café" : "Acciones"}
        >
          {useRightRailDetail ? (
            <div className="desktop-detail-wrap">{detailPanel}</div>
          ) : guardedActiveTab === "brewlab" && showCreateCoffeeComposer ? (
            <div className="desktop-detail-wrap">{createCoffeePanel}</div>
          ) : null}
        </aside>
      ) : null}

      {mode === "mobile" && guardedActiveTab !== "crear-cafe" && guardedActiveTab !== "selecciona-cafe" && !(guardedActiveTab === "brewlab" && showCreateCoffeeComposer) && !isSearchUsersPage && !detailCoffeeId ? <footer className="bottom-tabs">{nav}</footer> : null}

      {showEditListSheet && profileListId && (() => {
        const list = userLists.find((l) => l.id === profileListId);
        if (!list) return null;
        const initialPrivacy: ListPrivacy = list.privacy ?? (list.is_public ? "public" : "private");
        return typeof document !== "undefined"
          ? createPortal(
              <SheetOverlay role="dialog" aria-modal="true" aria-label="Editar lista" onDismiss={() => { sendEvent("modal_close", { modal_id: "list_edit" }); setShowEditListSheet(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "list_edit" }); setShowEditListSheet(false); }}>
                <EditListSheet
                  listId={list.id}
                  initialName={list.name}
                  initialPrivacy={initialPrivacy}
                  initialMembersCanEdit={list.members_can_edit ?? false}
                  onDismiss={() => { sendEvent("modal_close", { modal_id: "list_edit" }); setShowEditListSheet(false); }}
                  onSave={handleEditListSave}
                />
              </SheetOverlay>,
              document.body
            )
          : null;
      })()}
      {showDeleteListConfirmSheet && profileListId && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Eliminar lista" onDismiss={() => { sendEvent("modal_close", { modal_id: "delete_confirm_list" }); setShowDeleteListConfirmSheet(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "delete_confirm_list" }); setShowDeleteListConfirmSheet(false); }}>
              <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(e) => e.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-delete-confirm-body">
                  <h2 className="diary-delete-confirm-title">Eliminar lista</h2>
                  <p className="diary-delete-confirm-text">
                    ¿Estás seguro de que quieres eliminar esta lista? Se quitarán todos los cafés que contiene.
                  </p>
                  <div className="diary-delete-confirm-actions">
                    <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => { sendEvent("modal_close", { modal_id: "delete_confirm_list" }); setShowDeleteListConfirmSheet(false); }}>
                      Cancelar
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-submit"
                      onClick={() => { sendEvent("modal_close", { modal_id: "delete_confirm_list" }); void handleDeleteListConfirm(); }}
                    >
                      Eliminar
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      {showLeaveListConfirmSheet && profileListId && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label="Abandonar lista" onDismiss={() => { sendEvent("modal_close", { modal_id: "leave_list_confirm" }); setShowLeaveListConfirmSheet(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "leave_list_confirm" }); setShowLeaveListConfirmSheet(false); }}>
              <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(e) => e.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <div className="diary-delete-confirm-body">
                  <h2 className="diary-delete-confirm-title">Abandonar lista</h2>
                  <p className="diary-delete-confirm-text">
                    ¿Estás seguro de que quieres abandonar esta lista? Dejarás de tener acceso a ella y ya no aparecerá en tu sección de listas.
                  </p>
                  <div className="diary-delete-confirm-actions">
                    <Button variant="plain" type="button" className="diary-delete-confirm-cancel" onClick={() => { sendEvent("modal_close", { modal_id: "leave_list_confirm" }); setShowLeaveListConfirmSheet(false); }}>
                      Cancelar
                    </Button>
                    <Button
                      variant="plain"
                      type="button"
                      className="diary-delete-confirm-submit"
                      onClick={() => { sendEvent("modal_close", { modal_id: "leave_list_confirm" }); void handleLeaveListConfirm(); }}
                    >
                      Abandonar
                    </Button>
                  </div>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
      <AppOverlayLayers
        authPrompt={authPromptOverlay}
        barcodeScanner={barcodeScannerOverlay}
        commentSheet={null}
        createPostSheet={null}
        notificationsSheet={notificationsOverlay}
        diarySheets={diarySheetsOverlay}
      />
    </div>
  );
}












