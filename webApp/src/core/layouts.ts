import type { TabId } from "../types";

type DetailHostTab = "home" | "search" | "profile" | "diary" | null;

export function shouldUseRightRailDetail(params: {
  mode: "mobile" | "desktop";
  viewportWidth: number;
  hasDetailPanel: boolean;
  activeTab: TabId;
  detailHostTab: DetailHostTab;
}): boolean {
  const { mode, viewportWidth, hasDetailPanel, activeTab, detailHostTab } = params;
  if (mode !== "desktop" || viewportWidth < 1520 || !hasDetailPanel) return false;
  return (
    (activeTab === "home" && detailHostTab === "home") ||
    (activeTab === "search" && detailHostTab === "search") ||
    (activeTab === "profile" && detailHostTab === "profile")
  );
}

export function sidePanelForTab(params: {
  mode: "mobile" | "desktop";
  activeTab: TabId;
  detailHostTab: DetailHostTab;
  useRightRailDetail: boolean;
}): "home" | "search" | "profile" | null {
  const { mode, activeTab, detailHostTab, useRightRailDetail } = params;
  if (mode !== "desktop" || useRightRailDetail) return null;
  if (activeTab === "home" && detailHostTab === "home") return "home";
  if (activeTab === "search" && detailHostTab === "search") return "search";
  if (activeTab === "profile" && detailHostTab === "profile") return "profile";
  return null;
}
