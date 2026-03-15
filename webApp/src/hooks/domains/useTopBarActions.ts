import { useMemo } from "react";
import type { BrewStep } from "../../types";

export function useTopBarActions({
  searchMode,
  resetSearchUi,
  navigateToTab,
  setShowBarcodeScannerSheet,
  isMobileOsDevice,
  setShowNotificationsPanel,
  visibleTimelineNotifications,
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
  activeUserId
}: {
  searchMode: "coffees" | "users";
  resetSearchUi: () => void;
  navigateToTab: (tab: "home" | "search", options?: Record<string, unknown>) => void;
  setShowBarcodeScannerSheet: (value: boolean) => void;
  isMobileOsDevice: boolean;
  setShowNotificationsPanel: (value: boolean) => void;
  visibleTimelineNotifications: Array<{ timestamp: number }>;
  setNotificationsLastSeenAt: (updater: (prev: number) => number) => void;
  setShowDiaryQuickActions: (value: boolean) => void;
  setShowDiaryPeriodSheet: (value: boolean) => void;
  showCreateCoffeeComposer: boolean;
  closeCreateCoffeeComposer: () => void;
  brewStep: BrewStep;
  setBrewStep: (value: BrewStep) => void;
  setTimerSeconds: (value: number) => void;
  setBrewRunning: (value: boolean) => void;
  runWithAuth: <T>(fn: () => Promise<T>) => Promise<T | null>;
  setDetailOpenStockSignal: (updater: (prev: number) => number) => void;
  saveDetailFavorite: () => Promise<void>;
  activeUserId: number | null;
}) {
  return useMemo(
    () => ({
      onSearchBarcodeClick: () => {
        if (!isMobileOsDevice) return;
        setShowBarcodeScannerSheet(true);
      },
      onSearchBack: () => {
        if (searchMode === "users") {
          resetSearchUi();
          navigateToTab("home");
          return;
        }
        resetSearchUi();
      },
      onHomeSearchUsers: () => {
        resetSearchUi();
        navigateToTab("search", { searchMode: "users" });
      },
      onHomeNotifications: () => {
        setShowNotificationsPanel(true);
        const now = Date.now();
        setNotificationsLastSeenAt(() => {
          localStorage.setItem("notifications_last_seen_at", String(now));
          return now;
        });

        if (activeUserId) {
          void import("../../data/supabaseApi").then((m) => {
            void m.markNotificationsAsRead(activeUserId);
          });
        }
      },
      onDiaryOpenPeriodSelector: () => setShowDiaryPeriodSheet(true),
      onBrewBack: () => {
        if (showCreateCoffeeComposer) {
          closeCreateCoffeeComposer();
          return;
        }
        if (brewStep === "coffee") setBrewStep("method");
        else if (brewStep === "result") setBrewStep("method");
        else if (brewStep === "brewing") {
          setTimerSeconds(0);
          setBrewRunning(false);
          setBrewStep("method");
        }
      },
      onBrewForward: () => {},
      onCoffeeTopbarToggleFavorite: () => {
        void runWithAuth(async () => {
          await saveDetailFavorite();
        });
      },
      onCoffeeTopbarOpenStock: () => {
        void runWithAuth(async () => {
          setDetailOpenStockSignal((prev) => prev + 1);
        });
      }
    }),
    [
      brewStep,
      closeCreateCoffeeComposer,
      isMobileOsDevice,
      navigateToTab,
      resetSearchUi,
      runWithAuth,
      saveDetailFavorite,
      searchMode,
      setBrewRunning,
      setBrewStep,
      setDetailOpenStockSignal,
      setNotificationsLastSeenAt,
      setShowBarcodeScannerSheet,
      setShowDiaryPeriodSheet,
      setShowNotificationsPanel,
      setTimerSeconds,
      showCreateCoffeeComposer,
      visibleTimelineNotifications,
      activeUserId
    ]
  );
}
