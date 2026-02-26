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
  saveDetailFavorite
}: {
  searchMode: "coffees" | "users";
  resetSearchUi: () => void;
  navigateToTab: (tab: "timeline" | "search", options?: Record<string, unknown>) => void;
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
          navigateToTab("timeline");
          return;
        }
        resetSearchUi();
      },
      onTimelineSearchUsers: () => {
        resetSearchUi();
        navigateToTab("search", { searchMode: "users" });
      },
      onTimelineNotifications: () => {
        setShowNotificationsPanel(true);
        const latestSeen = visibleTimelineNotifications.reduce((max, item) => Math.max(max, item.timestamp), 0);
        setNotificationsLastSeenAt((prev) => Math.max(prev, latestSeen));
      },
      onDiaryOpenQuickActions: () => setShowDiaryQuickActions(true),
      onDiaryOpenPeriodSelector: () => setShowDiaryPeriodSheet(true),
      onBrewBack: () => {
        if (showCreateCoffeeComposer) {
          closeCreateCoffeeComposer();
          return;
        }
        if (brewStep === "coffee") setBrewStep("method");
        else if (brewStep === "config") setBrewStep("coffee");
        else if (brewStep === "brewing") setBrewStep("config");
        else if (brewStep === "result") setBrewStep("method");
      },
      onBrewForward: () => {
        if (brewStep !== "config") return;
        setTimerSeconds(0);
        setBrewRunning(true);
        setBrewStep("brewing");
      },
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
      setShowDiaryQuickActions,
      setShowNotificationsPanel,
      setTimerSeconds,
      showCreateCoffeeComposer,
      visibleTimelineNotifications
    ]
  );
}
