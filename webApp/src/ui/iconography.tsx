export type IconName =
  | "home"
  | "search"
  | "barcode"
  | "science"
  | "book"
  | "person"
  | "nav-home-outline"
  | "nav-home-filled"
  | "nav-explore-outline"
  | "nav-explore-filled"
  | "nav-science-outline"
  | "nav-science-filled"
  | "nav-book-outline"
  | "nav-book-filled"
  | "nav-person-outline"
  | "nav-person-filled"
  | "notifications"
  | "settings"
  | "add"
  | "close"
  | "favorite"
  | "favorite-filled"
  | "star"
  | "star-filled"
  | "stock"
  | "origin"
  | "specialty"
  | "roast"
  | "format"
  | "process"
  | "variety"
  | "grind"
  | "caffeine"
  | "arrow-left"
  | "arrow-right"
  | "more"
  | "coffee"
  | "coffee-filled"
  | "camera"
  | "chat"
  | "at"
  | "smile"
  | "chevron-right"
  | "send"
  | "edit"
  | "trash"
  | "shop"
  | "leaf"
  | "blend"
  | "roast-light"
  | "roast-medium"
  | "roast-dark"
  | "recycle"
  | "rugby"
  | "thermostat"
  | "taste-bitter"
  | "taste-acid"
  | "taste-balance"
  | "taste-salty"
  | "taste-watery"
  | "taste-sweet"
  | "clock";

type TimelineNotificationItem = {
  id: string;
  type: "follow" | "comment";
  userId: number;
  text: string;
  timestamp: number;
  postId?: string;
  commentId?: number;
};

export function MaterialSymbolIcon({
  symbol,
  filled,
  className
}: {
  symbol:
    | "book"
    | "auto_awesome"
    | "science"
    | "explore"
    | "globe"
    | "verified"
    | "auto_awesome_mosaic"
    | "local_fire_department"
    | "whatshot"
    | "fireplace"
    | "grain"
    | "energy_savings_leaf"
    | "sports_rugby"
    | "settings"
    | "lens_blur"
    | "storefront"
    | "device_thermostat"
    | "waves"
    | "water_drop"
    | "favorite";
  filled: boolean;
  className?: string;
}) {
  return (
    <span
      className={`${className ?? ""} material-symbol-icon ${filled ? "is-filled" : "is-outlined"}`.trim()}
      aria-hidden="true"
    >
      {symbol}
    </span>
  );
}

export function UiIcon({ name, className }: { name: IconName; className?: string }) {
  if (name === "nav-home-outline") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 10.8 12 4l8 6.8V20h-5.2v-5.4H9.2V20H4z" />
      </svg>
    );
  }
  if (name === "nav-home-filled") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 3.4 3.5 10.6V20h5.8v-5.6h5.4V20h5.8v-9.4z" fill="currentColor" stroke="none" />
      </svg>
    );
  }
  if (name === "nav-explore-outline") {
    return <MaterialSymbolIcon symbol="explore" filled={false} className={className} />;
  }
  if (name === "nav-explore-filled") {
    return <MaterialSymbolIcon symbol="explore" filled={true} className={className} />;
  }
  if (name === "nav-science-outline") {
    return <MaterialSymbolIcon symbol="science" filled={false} className={className} />;
  }
  if (name === "nav-science-filled") {
    return <MaterialSymbolIcon symbol="science" filled={true} className={className} />;
  }
  if (name === "nav-book-outline") {
    return <MaterialSymbolIcon symbol="book" filled={false} className={className} />;
  }
  if (name === "nav-book-filled") {
    return <MaterialSymbolIcon symbol="book" filled={true} className={className} />;
  }
  if (name === "nav-person-outline") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 12c2.76 0 5-2.24 5-5S14.76 2 12 2 7 4.24 7 7s2.24 5 5 5zm0-8c1.65 0 3 1.35 3 3s-1.35 3-3 3-3-1.35-3-3 1.35-3 3-3zM12 14c-3.33 0-10 1.67-10 5v3h20v-3c0-3.33-6.67-5-10-5zm8 6H4v-1c0-1.34 4.34-3 8-3s8 1.66 8 3z" fill="currentColor" stroke="none" />
      </svg>
    );
  }
  if (name === "nav-person-filled") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 12c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm0 2c-3.33 0-10 1.67-10 5v3h20v-3c0-3.33-6.67-5-10-5z" fill="currentColor" stroke="none" />
      </svg>
    );
  }
  if (name === "home") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 11.5L12 5l8 6.5V20h-5.5v-5h-5v5H4z" />
      </svg>
    );
  }
  if (name === "search") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="11" cy="11" r="6.5" />
        <path d="M16 16l4 4" />
      </svg>
    );
  }
  if (name === "barcode") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M3 6v12M6 6v12M9 8v8M12 6v12M15 8v8M18 6v12M21 6v12" />
      </svg>
    );
  }
  if (name === "science") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M9 3h6M10 3v4l-5.2 8.8A3 3 0 007.4 20h9.2a3 3 0 002.6-4.2L14 7V3" />
        <path d="M9 14h6" />
      </svg>
    );
  }
  if (name === "book") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M5 4h6.5A2.5 2.5 0 0114 6.5V20H7a2 2 0 01-2-2z" />
        <path d="M19 4h-6.5A2.5 2.5 0 0010 6.5V20h7a2 2 0 002-2z" />
      </svg>
    );
  }
  if (name === "person") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="12" cy="8" r="3.5" />
        <path d="M5 20c0-3.8 3.1-6 7-6s7 2.2 7 6" />
      </svg>
    );
  }
  if (name === "notifications") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 4a5 5 0 00-5 5v3.5L5 15v1h14v-1l-2-2.5V9a5 5 0 00-5-5z" />
        <path d="M10 18.5a2 2 0 004 0" />
      </svg>
    );
  }
  if (name === "settings") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="12" cy="12" r="3.2" />
        <path d="M12 4.2v2.2M12 17.6v2.2M4.2 12h2.2M17.6 12h2.2M6.4 6.4l1.6 1.6M16 16l1.6 1.6M17.6 6.4L16 8M8 16l-1.6 1.6" />
      </svg>
    );
  }
  if (name === "add") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 5v14M5 12h14" />
      </svg>
    );
  }
  if (name === "close") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M6 6l12 12M18 6L6 18" />
      </svg>
    );
  }
  if (name === "favorite") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 20.6 4.2 13.5a4.8 4.8 0 0 1 0-7 4.9 4.9 0 0 1 6.9 0l.9.9.9-.9a4.9 4.9 0 0 1 6.9 0 4.8 4.8 0 0 1 0 7z" />
      </svg>
    );
  }
  if (name === "favorite-filled") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 20.6 4.2 13.5a4.8 4.8 0 0 1 0-7 4.9 4.9 0 0 1 6.9 0l.9.9.9-.9a4.9 4.9 0 0 1 6.9 0 4.8 4.8 0 0 1 0 7z" fill="currentColor" stroke="none" />
      </svg>
    );
  }
  if (name === "star") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="m12 3.9 2.4 4.9 5.4.8-3.9 3.8.9 5.4-4.8-2.6-4.8 2.6.9-5.4-3.9-3.8 5.4-.8z" />
      </svg>
    );
  }
  if (name === "star-filled") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="m12 3.9 2.4 4.9 5.4.8-3.9 3.8.9 5.4-4.8-2.6-4.8 2.6.9-5.4-3.9-3.8 5.4-.8z" fill="currentColor" stroke="none" />
      </svg>
    );
  }
  if (name === "stock") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 7.5 12 4l8 3.5-8 3.5zM4 7.5V16l8 4 8-4V7.5M12 11v9" />
      </svg>
    );
  }
  if (name === "origin") {
    return <MaterialSymbolIcon symbol="globe" filled={false} className={className} />;
  }
  if (name === "specialty") {
    return <MaterialSymbolIcon symbol="verified" filled={false} className={className} />;
  }
  if (name === "roast") {
    return <MaterialSymbolIcon symbol="local_fire_department" filled={false} className={className} />;
  }
  if (name === "format") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <rect x="5" y="4" width="14" height="16" rx="2" />
        <path d="M8 8h8M8 12h8M8 16h5" />
      </svg>
    );
  }
  if (name === "process") {
    return <MaterialSymbolIcon symbol="settings" filled={false} className={className} />;
  }
  if (name === "variety") {
    return <MaterialSymbolIcon symbol="auto_awesome_mosaic" filled={false} className={className} />;
  }
  if (name === "grind") {
    return <MaterialSymbolIcon symbol="lens_blur" filled={false} className={className} />;
  }
  if (name === "shop") {
    return <MaterialSymbolIcon symbol="storefront" filled={false} className={className} />;
  }
  if (name === "caffeine") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M13.5 2 6 13h5l-1 9 8-12h-5.5z" />
      </svg>
    );
  }
  if (name === "leaf") {
    return <MaterialSymbolIcon symbol="energy_savings_leaf" filled={false} className={className} />;
  }
  if (name === "blend") {
    return <MaterialSymbolIcon symbol="grain" filled={false} className={className} />;
  }
  if (name === "rugby") {
    return <MaterialSymbolIcon symbol="sports_rugby" filled={false} className={className} />;
  }
  if (name === "thermostat") {
    return <MaterialSymbolIcon symbol="device_thermostat" filled={false} className={className} />;
  }
  if (name === "taste-bitter") {
    return <MaterialSymbolIcon symbol="local_fire_department" filled={true} className={className} />;
  }
  if (name === "taste-acid") {
    return <MaterialSymbolIcon symbol="science" filled={true} className={className} />;
  }
  if (name === "taste-balance") {
    return <MaterialSymbolIcon symbol="verified" filled={true} className={className} />;
  }
  if (name === "taste-salty") {
    return <MaterialSymbolIcon symbol="waves" filled={true} className={className} />;
  }
  if (name === "taste-watery") {
    return <MaterialSymbolIcon symbol="water_drop" filled={true} className={className} />;
  }
  if (name === "taste-sweet") {
    return <MaterialSymbolIcon symbol="favorite" filled={true} className={className} />;
  }
  if (name === "clock") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="12" cy="12" r="8.5" />
        <path d="M12 7.8v4.6l3 1.8" />
      </svg>
    );
  }
  if (name === "roast-light") {
    return <MaterialSymbolIcon symbol="local_fire_department" filled={false} className={className} />;
  }
  if (name === "roast-medium") {
    return <MaterialSymbolIcon symbol="local_fire_department" filled={false} className={className} />;
  }
  if (name === "roast-dark") {
    return <MaterialSymbolIcon symbol="fireplace" filled={false} className={className} />;
  }
  if (name === "recycle") {
    return (
      <span className={`${className ?? ""} material-symbol-icon is-outlined`.trim()} aria-hidden="true">
        recycling
      </span>
    );
  }
  if (name === "arrow-left") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M14.5 5.5L8 12l6.5 6.5M8 12h11" />
      </svg>
    );
  }
  if (name === "arrow-right") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M9.5 5.5L16 12l-6.5 6.5M16 12H5" />
      </svg>
    );
  }
  if (name === "more") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="6" cy="12" r="1.8" />
        <circle cx="12" cy="12" r="1.8" />
        <circle cx="18" cy="12" r="1.8" />
      </svg>
    );
  }
  if (name === "coffee") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M3.5 7.5h12v5.2A4.3 4.3 0 0111.2 17H7.8a4.3 4.3 0 01-4.3-4.3z" />
        <path d="M15.4 8.8h2a2.8 2.8 0 010 5.6h-2" />
        <path d="M6.2 19.4h7.6" />
      </svg>
    );
  }
  if (name === "coffee-filled") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M3.5 7.5h12v5.2A4.3 4.3 0 0111.2 17H7.8a4.3 4.3 0 01-4.3-4.3z" fill="currentColor" stroke="none" />
        <path d="M15.4 8.8h2a2.8 2.8 0 010 5.6h-2" fill="currentColor" stroke="none" />
        <path d="M6.2 19.4h7.6" />
      </svg>
    );
  }
  if (name === "camera") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4.5 7.5h4l1.3-2h4.4l1.3 2h4a1.8 1.8 0 011.8 1.8v8.7a1.8 1.8 0 01-1.8 1.8h-15A1.8 1.8 0 012.7 18V9.3A1.8 1.8 0 014.5 7.5z" />
        <circle cx="12" cy="13" r="3.2" />
      </svg>
    );
  }
  if (name === "chat") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4.5 6.2h15v8.8H9.2l-4.7 3.2z" />
      </svg>
    );
  }
  if (name === "at") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M16.5 12a4.5 4.5 0 10-1.1 2.9c.5.6 1.6.8 2.5.3 1.1-.6 1.6-1.8 1.6-3.2A7.5 7.5 0 1012 19.5" />
      </svg>
    );
  }
  if (name === "smile") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <circle cx="12" cy="12" r="8.5" />
        <circle cx="9" cy="10" r="1" />
        <circle cx="15" cy="10" r="1" />
        <path d="M8.5 14c.8 1.4 2 2 3.5 2s2.7-.6 3.5-2" />
      </svg>
    );
  }
  if (name === "chevron-right") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M9.5 5.5L16 12l-6.5 6.5" />
      </svg>
    );
  }
  if (name === "send") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 11.5L20 4l-4.8 16-3.4-5.2L4 11.5z" />
        <path d="M20 4L11.8 14.8" />
      </svg>
    );
  }
  if (name === "edit") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M4 16.8V20h3.2L18 9.2l-3.2-3.2L4 16.8z" />
        <path d="M13.8 6.9l3.2 3.2" />
      </svg>
    );
  }
  if (name === "trash") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M5.5 7.5h13" />
        <path d="M9.5 7.5V5.8h5V7.5" />
        <path d="M8 7.5l.8 11h6.4l.8-11" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
      <path d="M9.5 5.5L16 12l-6.5 6.5M16 12H5" />
    </svg>
  );
}




