import type { ReactNode } from "react";
import type { TabId } from "../types";

export function AppContentRouter({
  activeTab,
  mode,
  homeContent,
  searchContent,
  coffeeContent,
  brewContent,
  diaryContent,
  profileContent
}: {
  activeTab: TabId;
  mode: string;
  homeContent: ReactNode;
  searchContent: ReactNode;
  coffeeContent: ReactNode;
  brewContent: ReactNode;
  diaryContent: ReactNode;
  profileContent: ReactNode;
}) {
  return (
    <section aria-live="polite" className={`content content-${activeTab}`.trim()}>
      {activeTab === "home" ? homeContent : null}
      {activeTab === "search" ? searchContent : null}
      {activeTab === "coffee" ? coffeeContent : null}
      {activeTab === "brewlab" ? brewContent : null}
      {activeTab === "diary" ? diaryContent : null}
      {activeTab === "profile" ? profileContent : null}
    </section>
  );
}
