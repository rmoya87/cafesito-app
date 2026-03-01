import React from "react";
import type { TabId } from "../../types";
import { NAV_ITEMS } from "../../config/navigation";
import { UiIcon } from "../../ui/iconography";
import { Button } from "../../ui/components";

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
  return (
    <nav className="nav nav-mobile" aria-label="Navegacion principal">
      {NAV_ITEMS.map((item) => {
        const isActive = activeTab === item.id;
        return (
          <Button
            key={item.id}
            variant="plain"
            className={`nav-item ${isActive ? "is-active" : ""}`}
            onClick={() => onNavClick(item.id)}
            aria-current={isActive ? "page" : undefined}
          >
            <NavGlyph item={item} isActive={isActive} avatarUrl={avatarUrl} />
            <span className="nav-label">{item.label}</span>
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
  return (
    <aside className="nav-rail" aria-label="Navegacion principal">
      <nav className="nav nav-desktop">
        {NAV_ITEMS.map((item) => {
          const isActive = activeTab === item.id;
          return (
            <Button
              key={item.id}
              variant="plain"
              className={`nav-item ${isActive ? "is-active" : ""}`}
              onClick={() => onNavClick(item.id)}
              aria-current={isActive ? "page" : undefined}
              aria-label={item.label}
              title={item.label}
            >
              <NavGlyph item={item} isActive={isActive} avatarUrl={avatarUrl} />
            </Button>
          );
        })}
      </nav>
    </aside>
  );
}
