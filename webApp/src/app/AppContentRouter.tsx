import type { ReactNode } from "react";
import type { TabId } from "../types";
import { IconButton } from "../ui/components";
import { UiIcon } from "../ui/iconography";

export function AppContentRouter({
  activeTab,
  mode,
  timelineContent,
  searchContent,
  coffeeContent,
  brewContent,
  diaryContent,
  profileContent,
  onOpenCreatePost
}: {
  activeTab: TabId;
  mode: string;
  timelineContent: ReactNode;
  searchContent: ReactNode;
  coffeeContent: ReactNode;
  brewContent: ReactNode;
  diaryContent: ReactNode;
  profileContent: ReactNode;
  onOpenCreatePost: () => void;
}) {
  return (
    <>
      <section aria-live="polite" className="content">
        {activeTab === "timeline" ? timelineContent : null}
        {activeTab === "search" ? searchContent : null}
        {activeTab === "coffee" ? coffeeContent : null}
        {activeTab === "brewlab" ? brewContent : null}
        {activeTab === "diary" ? diaryContent : null}
        {activeTab === "profile" ? profileContent : null}
      </section>

      {activeTab === "timeline" && mode !== "desktop" ? (
        <IconButton className="fab" aria-label="Nuevo Post" onClick={onOpenCreatePost}>
          <UiIcon name="add" className="ui-icon" />
        </IconButton>
      ) : null}
    </>
  );
}
