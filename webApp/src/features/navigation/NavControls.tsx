import type { TabId } from "../../types";
import { NAV_ITEMS } from "../../config/navigation";
import { UiIcon } from "../../ui/iconography";
import { Button } from "../../ui/components";

export function BottomNav({
  activeTab,
  onNavClick
}: {
  activeTab: TabId;
  onNavClick: (tab: TabId) => void;
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
            <span className="nav-glyph" aria-hidden="true">
              <UiIcon name={isActive ? item.activeIcon : item.icon} className="ui-icon" />
            </span>
            <span className="nav-label">{item.label}</span>
          </Button>
        );
      })}
    </nav>
  );
}

export function DesktopNavRail({
  activeTab,
  onNavClick
}: {
  activeTab: TabId;
  onNavClick: (tab: TabId) => void;
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
              <span className="nav-glyph" aria-hidden="true">
                <UiIcon name={isActive ? item.activeIcon : item.icon} className="ui-icon" />
              </span>
            </Button>
          );
        })}
      </nav>
    </aside>
  );
}
