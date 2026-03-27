import React from "react";
import type { TabId } from "../../types";
import { NAV_ITEMS } from "../../config/navigation";
import { UiIcon } from "../../ui/iconography";
import { Button } from "../../ui/components";
import { useI18n } from "../../i18n";

function tabLabel(tab: TabId, t: ReturnType<typeof useI18n>["t"]): string {
  if (tab === "home") return t("nav.home");
  if (tab === "search") return t("nav.search");
  if (tab === "brewlab") return t("nav.brewlab");
  if (tab === "diary") return t("nav.diary");
  return t("nav.profile");
}

function NavGlyph({
  item,
  isActive,
  avatarUrl
}: {
  item: (typeof NAV_ITEMS)[number];
  isActive: boolean;
  avatarUrl?: string | null;
}) {
  const isProfile = item.id === "profile";
  const showAvatar = isProfile && avatarUrl;
  const [avatarError, setAvatarError] = React.useState(false);
  React.useEffect(() => {
    setAvatarError(false);
  }, [avatarUrl]);
  const showImg = showAvatar && !avatarError;
  return (
    <span className={`nav-glyph ${isProfile ? "is-profile" : ""} ${isProfile && isActive ? "is-profile-active" : ""}`.trim()} aria-hidden="true">
      {showImg ? (
        <img
          src={avatarUrl!}
          alt=""
          className="nav-avatar"
          loading="lazy"
          decoding="async"
          referrerPolicy="no-referrer"
          crossOrigin="anonymous"
          onError={() => setAvatarError(true)}
        />
      ) : (
        <UiIcon name={isActive ? item.activeIcon : item.icon} className="ui-icon" />
      )}
    </span>
  );
}

export function BottomNav({
  activeTab,
  onNavClick,
  avatarUrl
}: {
  activeTab: TabId;
  onNavClick: (tab: TabId) => void;
  avatarUrl?: string | null;
}) {
  const { t } = useI18n();
  return (
    <nav className="nav nav-mobile" aria-label={t("nav.main")}>
      {NAV_ITEMS.map((item) => {
        const isActive = activeTab === item.id;
        const label = tabLabel(item.id, t);
        return (
          <Button
            key={item.id}
            variant="plain"
            className={`nav-item ${isActive ? "is-active" : ""}`}
            onClick={() => onNavClick(item.id)}
            aria-current={isActive ? "page" : undefined}
          >
            <NavGlyph item={item} isActive={isActive} avatarUrl={avatarUrl} />
            <span className="nav-label">{label}</span>
          </Button>
        );
      })}
    </nav>
  );
}

export function DesktopNavRail({
  activeTab,
  onNavClick,
  avatarUrl
}: {
  activeTab: TabId;
  onNavClick: (tab: TabId) => void;
  avatarUrl?: string | null;
}) {
  const { t } = useI18n();
  return (
    <aside className="nav-rail" aria-label={t("nav.main")}>
      <nav className="nav nav-desktop">
        {NAV_ITEMS.map((item) => {
          const isActive = activeTab === item.id;
          const label = tabLabel(item.id, t);
          return (
            <Button
              key={item.id}
              variant="plain"
              className={`nav-item ${isActive ? "is-active" : ""}`}
              onClick={() => onNavClick(item.id)}
              aria-current={isActive ? "page" : undefined}
              aria-label={label}
              title={label}
            >
              <NavGlyph item={item} isActive={isActive} avatarUrl={avatarUrl} />
            </Button>
          );
        })}
      </nav>
    </aside>
  );
}
