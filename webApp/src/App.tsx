import { Fragment, type CSSProperties, type PointerEvent as ReactPointerEvent, type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  addPostCoffeeTag,
  createDiaryEntry,
  createPost,
  createComment,
  deleteCoffeeReview,
  deleteComment,
  deleteDiaryEntry,
  deletePantryItem,
  deletePost,
  fetchInitialData,
  fetchUserData,
  toggleFavoriteCoffee,
  upsertCoffeeReview,
  upsertCoffeeSensoryProfile,
  upsertCustomCoffee,
  upsertPantryStock,
  toggleFollow,
  toggleLike,
  uploadImageFile,
  updateComment,
  updateDiaryEntry,
  updatePost,
  updateUserProfile
} from "./data/supabaseApi";
import { getSupabaseClient, supabaseConfigError } from "./supabase";
import type {
  BrewStep,
  CoffeeRow,
  CoffeeReviewRow,
  CoffeeSensoryProfileRow,
  CommentRow,
  DiaryEntryRow,
  FavoriteRow,
  FollowRow,
  LikeRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  TabId,
  TimelineCard,
  UserRow,
  ViewMode
} from "./types";

type IconName =
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

const NAV_ITEMS: Array<{ id: TabId; label: string; icon: IconName; activeIcon: IconName }> = [
  { id: "timeline", label: "Inicio", icon: "nav-home-outline", activeIcon: "nav-home-filled" },
  { id: "search", label: "Explorar", icon: "nav-explore-outline", activeIcon: "nav-explore-filled" },
  { id: "brewlab", label: "Elabora", icon: "nav-science-outline", activeIcon: "nav-science-filled" },
  { id: "diary", label: "Diario", icon: "nav-book-outline", activeIcon: "nav-book-filled" },
  { id: "profile", label: "Perfil", icon: "nav-person-outline", activeIcon: "nav-person-filled" }
];

const BREW_METHODS: Array<{ name: string; icon: string }> = [
  { name: "Aeropress", icon: "/brew-methods/maq_aeropress.png" },
  { name: "Chemex", icon: "/brew-methods/maq_chemex.png" },
  { name: "Espresso", icon: "/brew-methods/maq_espresso.png" },
  { name: "Goteo", icon: "/brew-methods/maq_goteo.png" },
  { name: "Hario V60", icon: "/brew-methods/maq_hario_v60.png" },
  { name: "Italiana", icon: "/brew-methods/maq_italiana.png" },
  { name: "Manual", icon: "/brew-methods/maq_manual.png" },
  { name: "Prensa francesa", icon: "/brew-methods/maq_prensa_francesa.png" },
  { name: "Sifón", icon: "/brew-methods/maq_sifon.png" },
  { name: "Turco", icon: "/brew-methods/maq_turco.png" }
];
const COMMENT_EMOJIS = ["ðŸ˜€", "ðŸ˜", "ðŸ¤Ž", "â˜•", "ðŸ”¥", "ðŸ™Œ", "ðŸ‘", "ðŸ˜‹", "ðŸ¥³", "ðŸ˜Ž"];

type RouteState = {
  tab: TabId;
  searchMode: "coffees" | "users";
  profileUsername: string | null;
  coffeeSlug: string | null;
};

type BarcodeDetectorResult = { rawValue?: string };
type BarcodeDetectorInstance = {
  detect: (source: CanvasImageSource) => Promise<BarcodeDetectorResult[]>;
};
type BarcodeDetectorConstructor = {
  new (options?: { formats?: string[] }): BarcodeDetectorInstance;
};

declare global {
  interface Window {
    BarcodeDetector?: BarcodeDetectorConstructor;
  }
}

function parseRoute(pathname: string): RouteState {
  const clean = pathname.replace(/\/+$/, "") || "/";
  const segments = clean.split("/").filter(Boolean);
  const first = segments[0] ?? "";
  const second = segments[1] ?? "";

  if (first === "search") {
    return {
      tab: "search",
      searchMode: second === "users" ? "users" : "coffees",
      profileUsername: null,
      coffeeSlug: null
    };
  }
  if (first === "brewlab") return { tab: "brewlab", searchMode: "coffees", profileUsername: null, coffeeSlug: null };
  if (first === "diary") return { tab: "diary", searchMode: "coffees", profileUsername: null, coffeeSlug: null };
  if (first === "profile") {
    return {
      tab: "profile",
      searchMode: "coffees",
      profileUsername: second ? decodeURIComponent(second) : null,
      coffeeSlug: null
    };
  }
  if (first === "coffee") {
    return {
      tab: "coffee",
      searchMode: "coffees",
      profileUsername: null,
      coffeeSlug: second ? decodeURIComponent(second) : null
    };
  }
  return { tab: "timeline", searchMode: "coffees", profileUsername: null, coffeeSlug: null };
}

function toCoffeeSlug(name: string): string {
  const base = normalizeLookupText(name)
    .replace(/[^a-z0-9\s-]/g, "")
    .replace(/\s+/g, "-")
    .replace(/-+/g, "-")
    .replace(/^-|-$/g, "");
  return base || "cafe";
}

function buildRoute(tab: TabId, searchMode: "coffees" | "users", profileUsername: string | null, coffeeSlug?: string | null): string {
  if (tab === "search") return searchMode === "users" ? "/search/users" : "/search";
  if (tab === "brewlab") return "/brewlab";
  if (tab === "diary") return "/diary";
  if (tab === "profile") return profileUsername ? `/profile/${encodeURIComponent(profileUsername)}` : "/profile";
  if (tab === "coffee") return coffeeSlug ? `/coffee/${encodeURIComponent(coffeeSlug)}` : "/timeline";
  return "/timeline";
}

function toRelativeMinutes(timestamp: number): string {
  const diffMs = Math.max(0, Date.now() - timestamp);
  const mins = Math.max(1, Math.floor(diffMs / 60000));
  if (mins < 60) return `${mins} min`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs} h`;
  const days = Math.floor(hrs / 24);
  return `${days} d`;
}

function withinDays(timestamp: number, days: number): boolean {
  const ms = days * 24 * 60 * 60 * 1000;
  return Date.now() - timestamp <= ms;
}

function normalizeLookupText(value: string | null | undefined): string {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

function toUiOptionValue(value: string | null | undefined): string {
  const compact = (value ?? "").replace(/\s+/g, " ").trim();
  if (!compact) return "";
  return compact
    .split(" ")
    .map((chunk) => {
      if (!chunk) return chunk;
      const lower = chunk.toLocaleLowerCase("es");
      return lower.charAt(0).toLocaleUpperCase("es") + lower.slice(1);
    })
    .join(" ");
}

function buildNormalizedOptions(values: Array<string | null | undefined>): string[] {
  const map = new Map<string, string>();
  values.forEach((raw) => {
    const normalizedKey = normalizeLookupText(raw);
    if (!normalizedKey) return;
    if (!map.has(normalizedKey)) {
      map.set(normalizedKey, toUiOptionValue(raw));
    }
  });
  return Array.from(map.values()).sort((a, b) => a.localeCompare(b, "es"));
}

function toEventTimestamp(value: unknown): number {
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string") {
    const asNumber = Number(value);
    if (Number.isFinite(asNumber) && asNumber > 0) return asNumber;
    const parsed = Date.parse(value);
    if (Number.isFinite(parsed) && parsed > 0) return parsed;
  }
  return 0;
}


function MaterialSymbolIcon({
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

function UiIcon({ name, className }: { name: IconName; className?: string }) {
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

function getBrewStepTitle(step: BrewStep): string {
  if (step === "method") return "ELABORACION";
  if (step === "coffee") return "ELIGE CAFE";
  if (step === "config") return "CONFIGURA";
  if (step === "brewing") return "PROCESO EN CURSO";
  return "RESULTADO";
}

type BrewPhaseInfo = {
  label: string;
  instruction: string;
  durationSeconds: number;
};

function getBrewTimelineForMethod(method: string, waterMl: number): BrewPhaseInfo[] {
  const key = normalizeLookupText(method);
  if (key.includes("espresso")) {
    return [
      {
        label: "Extracción",
        instruction: "Aplica presión constante. Vigila el flujo: debe ser como un hilo de miel. Busca obtener unos 36-40g de líquido final.",
        durationSeconds: 25
      }
    ];
  }
  if (key.includes("prensa")) {
    return [
      {
        label: "Inmersión",
        instruction: "Vierte todo el agua caliente uniformemente sobre el café. Coloca la tapa sin presionar para mantener el calor.",
        durationSeconds: 240
      }
    ];
  }
  if (key.includes("aeropress")) {
    return [
      {
        label: "Pre-infusión",
        instruction: "Vierte unos 50ml de agua para humedecer todo el café. Remueve suavemente 3 veces para asegurar una extracción uniforme.",
        durationSeconds: 30
      },
      {
        label: "Infusión",
        instruction: "Añade el resto del agua. Deja que el café repose e interactúe con el agua para extraer todos sus sabores.",
        durationSeconds: 90
      },
      {
        label: "Presión",
        instruction: "Presiona el émbolo hacia abajo con una fuerza firme y constante. Escucha el 'sssh' final y detente.",
        durationSeconds: 30
      }
    ];
  }
  if (key.includes("italiana")) {
    const boilTime = Math.round(120 + waterMl * 0.2);
    return [
      {
        label: "Calentamiento",
        instruction: "Mantén el fuego medio-bajo. El agua en la base empezará a crear presión para subir por la chimenea.",
        durationSeconds: boilTime
      },
      {
        label: "Extracción",
        instruction: "Cuando el café empiece a salir, baja el fuego o retíralo. Escucha el burbujeo suave y detente antes del chorro final.",
        durationSeconds: 40
      }
    ];
  }
  if (key.includes("turco")) {
    return [
      {
        label: "Infusión",
        instruction: "Calienta a fuego muy lento hasta que veas que se forma una espuma densa y oscura en la superficie (crema).",
        durationSeconds: 120
      },
      {
        label: "Levantamiento 1",
        instruction: "Retira el cezve del fuego justo antes de que hierva. Deja que la espuma baje un poco y vuelve al fuego.",
        durationSeconds: 20
      },
      {
        label: "Levantamiento 2",
        instruction: "Repite el proceso: deja que suba la espuma por segunda vez para intensificar el cuerpo y sabor.",
        durationSeconds: 20
      },
      {
        label: "Toque Final",
        instruction: "Último ciclo de espuma. El café turco se caracteriza por su densidad y su sedimento único.",
        durationSeconds: 20
      }
    ];
  }
  if (key.includes("sifon")) {
    return [
      {
        label: "Ascenso",
        instruction: "La presión enviará el agua a la cámara superior. Espera a que se estabilice antes de añadir el café.",
        durationSeconds: 90
      },
      {
        label: "Mezcla",
        instruction: "Añade el café molido y remueve en círculos suavemente. Asegúrate de que todo el café esté sumergido.",
        durationSeconds: 60
      },
      {
        label: "Efecto Vacío",
        instruction: "Retira la fuente de calor. El enfriamiento creará un vacío que filtrará el café hacia abajo a través del filtro.",
        durationSeconds: 45
      }
    ];
  }

  const totalPourTime = Math.max(90, Math.min(300, Math.round(120 + (waterMl - 250) * 0.18)));
  const bloomMl = Math.floor(waterMl / 10);
  const devMl = Math.floor(waterMl * 0.6);
  return [
    {
      label: "Pre-infusión",
      instruction: `Vierte unos ${bloomMl}ml. Verás burbujas: es el CO2 liberándose para que el agua penetre mejor.`,
      durationSeconds: 30
    },
    {
      label: "Desarrollo de Sabor",
      instruction: `Vierte en espiral desde el centro hacia afuera hasta los ${devMl}ml. Mantén un flujo constante.`,
      durationSeconds: Math.round(totalPourTime * 0.4)
    },
    {
      label: "Cuerpo y Dulzor",
      instruction: `Añade el agua restante hasta los ${Math.floor(waterMl)}ml. Hazlo con suavidad para finalizar la extracción limpiamente.`,
      durationSeconds: Math.round(totalPourTime * 0.6)
    }
  ];
}

function formatClock(totalSeconds: number): string {
  const safe = Math.max(0, Math.floor(totalSeconds));
  const mm = Math.floor(safe / 60)
    .toString()
    .padStart(2, "0");
  const ss = (safe % 60).toString().padStart(2, "0");
  return `${mm}:${ss}`;
}

function getBrewDialRecommendation(taste: string): string {
  const key = normalizeLookupText(taste);
  if (key === "amargo") return "Sobre-extracción: Muele más grueso o baja la temperatura 2°C para reducir el amargor.";
  if (key === "acido") return "Sub-extracción: Muele más fino o vierte más lento para aumentar el contacto con el agua.";
  if (key === "equilibrado") return "¡Perfil perfecto! Has logrado un equilibrio ideal entre dulzor, acidez y cuerpo.";
  if (key === "salado") return "Muy sub-extraído: Muele mucho más fino. Indica que el agua no ha extraído los azúcares.";
  if (key === "acuoso") return "Poca intensidad: Usa un ratio de café más alto o intenta una molienda un punto más fina.";
  if (key === "aspero") return "Astringencia: Evita remover en exceso y asegúrate de que el filtro esté bien lavado.";
  if (key === "dulce") return "¡Extraordinario! Has resaltado los azúcares naturales del grano. Mantén estos ajustes.";
  return "";
}

function MentionText({
  text,
  onMentionClick
}: {
  text: string;
  onMentionClick?: (username: string) => void;
}) {
  const mentionRegex = /@([A-Za-z0-9._-]{2,30})/g;
  const parts: Array<{ value: string; mention: boolean; key: string }> = [];
  let lastIndex = 0;
  let matchIndex = 0;
  for (const match of text.matchAll(mentionRegex)) {
    const index = match.index ?? 0;
    if (index > lastIndex) {
      const chunk = text.slice(lastIndex, index);
      parts.push({ value: chunk, mention: false, key: `t-${matchIndex}-${index}` });
    }
    const username = match[1] ?? "";
    parts.push({ value: `@${username}`, mention: true, key: `m-${matchIndex}-${username}` });
    lastIndex = index + match[0].length;
    matchIndex += 1;
  }
  if (lastIndex < text.length) {
    parts.push({ value: text.slice(lastIndex), mention: false, key: `t-end-${lastIndex}` });
  }

  if (!parts.length) return <>{text}</>;

  return (
    <>
      {parts.map((part) =>
        part.mention ? (
          <button
            key={part.key}
            type="button"
            className="mention-button"
            onClick={() => onMentionClick?.(part.value.slice(1))}
          >
            {part.value}
          </button>
        ) : (
          <span key={part.key}>{part.value}</span>
        )
      )}
    </>
  );
}

function vibrateTap(duration = 10): void {
  if (typeof navigator === "undefined" || typeof navigator.vibrate !== "function") return;
  navigator.vibrate(duration);
}

export function App() {
  const initialRoute = parseRoute(window.location.pathname);
  const [mode, setMode] = useState<ViewMode>(window.innerWidth < 900 ? "mobile" : "desktop");
  const [viewportWidth, setViewportWidth] = useState<number>(window.innerWidth);
  const [activeTab, setActiveTab] = useState<TabId>(initialRoute.tab);

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
  const [favorites, setFavorites] = useState<FavoriteRow[]>([]);
  const [follows, setFollows] = useState<FollowRow[]>([]);

  const [searchQuery, setSearchQuery] = useState("");
  const [searchMode, setSearchMode] = useState<"coffees" | "users">(initialRoute.searchMode);
  const [searchFilter, setSearchFilter] = useState<"todo" | "origen" | "nombre">("todo");
  const [searchSelectedOrigins, setSearchSelectedOrigins] = useState<Set<string>>(new Set());
  const [searchSelectedRoasts, setSearchSelectedRoasts] = useState<Set<string>>(new Set());
  const [searchSelectedSpecialties, setSearchSelectedSpecialties] = useState<Set<string>>(new Set());
  const [searchSelectedFormats, setSearchSelectedFormats] = useState<Set<string>>(new Set());
  const [searchMinRating, setSearchMinRating] = useState(0);
  const [searchActiveFilterType, setSearchActiveFilterType] = useState<"origen" | "tueste" | "especialidad" | "formato" | "nota" | null>(null);
  const [searchSelectedCoffeeId, setSearchSelectedCoffeeId] = useState<string | null>(null);
  const [searchFocusCoffeeProfile, setSearchFocusCoffeeProfile] = useState(false);
  const [detailCoffeeId, setDetailCoffeeId] = useState<string | null>(null);
  const [detailHostTab, setDetailHostTab] = useState<"timeline" | "search" | "profile" | "diary" | null>(null);
  const [detailReviewText, setDetailReviewText] = useState("");
  const [detailReviewRating, setDetailReviewRating] = useState(0);
  const [detailReviewImageFile, setDetailReviewImageFile] = useState<File | null>(null);
  const [detailReviewImagePreviewUrl, setDetailReviewImagePreviewUrl] = useState("");
  const [detailSensoryDraft, setDetailSensoryDraft] = useState({ aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 });
  const [detailStockDraft, setDetailStockDraft] = useState({ total: 0, remaining: 0 });
  const [detailOpenStockSignal, setDetailOpenStockSignal] = useState(0);
  const [showBarcodeScannerSheet, setShowBarcodeScannerSheet] = useState(false);

  const [brewStep, setBrewStep] = useState<BrewStep>("method");
  const [brewMethod, setBrewMethod] = useState("");
  const [brewCoffeeId, setBrewCoffeeId] = useState<string>("");
  const [waterMl, setWaterMl] = useState(300);
  const [ratio, setRatio] = useState(16);
  const [timerSeconds, setTimerSeconds] = useState(150);
  const [brewRunning, setBrewRunning] = useState(false);
  const [showCreateCoffeeComposer, setShowCreateCoffeeComposer] = useState(false);
  const [createCoffeeSaving, setCreateCoffeeSaving] = useState(false);
  const [createCoffeeError, setCreateCoffeeError] = useState<string | null>(null);
  const [createCoffeeDraft, setCreateCoffeeDraft] = useState({
    name: "",
    brand: "",
    specialty: "",
    country: "",
    format: "",
    roast: "",
    variety: "",
    hasCaffeine: true,
    totalGrams: 250
  });
  const [createCoffeeImageFile, setCreateCoffeeImageFile] = useState<File | null>(null);
  const [createCoffeeImagePreviewUrl, setCreateCoffeeImagePreviewUrl] = useState("");

  const [diaryTab, setDiaryTab] = useState<"actividad" | "despensa">("actividad");
  const [diaryPeriod, setDiaryPeriod] = useState<"hoy" | "7d" | "30d">("hoy");
  const [showDiaryQuickActions, setShowDiaryQuickActions] = useState(false);
  const [showDiaryPeriodSheet, setShowDiaryPeriodSheet] = useState(false);
  const [showDiaryWaterSheet, setShowDiaryWaterSheet] = useState(false);
  const [diaryWaterMlDraft, setDiaryWaterMlDraft] = useState("250");
  const [showDiaryCoffeeSheet, setShowDiaryCoffeeSheet] = useState(false);
  const [diaryCoffeeIdDraft, setDiaryCoffeeIdDraft] = useState("");
  const [diaryCoffeeMlDraft, setDiaryCoffeeMlDraft] = useState("250");
  const [diaryCoffeeCaffeineDraft, setDiaryCoffeeCaffeineDraft] = useState("95");
  const [diaryCoffeePreparationDraft, setDiaryCoffeePreparationDraft] = useState("Manual");
  const [showDiaryAddPantrySheet, setShowDiaryAddPantrySheet] = useState(false);
  const [diaryPantryCoffeeIdDraft, setDiaryPantryCoffeeIdDraft] = useState("");
  const [diaryPantryGramsDraft, setDiaryPantryGramsDraft] = useState("250");
  const [profileTab, setProfileTab] = useState<"posts" | "adn" | "favoritos">("posts");
  const [profileUsername, setProfileUsername] = useState<string | null>(initialRoute.profileUsername);
  const [profileEditSignal, setProfileEditSignal] = useState(0);
  useEffect(() => {
    if (activeTab === "profile") return;
    if (profileEditSignal === 0) return;
    setProfileEditSignal(0);
  }, [activeTab, profileEditSignal]);

  const [commentSheetPostId, setCommentSheetPostId] = useState<string | null>(null);
  const [commentDraft, setCommentDraft] = useState("");
  const [editingCommentId, setEditingCommentId] = useState<number | null>(null);
  const [commentMenuId, setCommentMenuId] = useState<number | null>(null);
  const [highlightedCommentId, setHighlightedCommentId] = useState<number | null>(null);
  const [handledTimelineDeepLink, setHandledTimelineDeepLink] = useState(false);
  const [topbarScrolled, setTopbarScrolled] = useState(false);
  const [timelineActionBanner, setTimelineActionBanner] = useState<string | null>(null);
  const [timelineRefreshing, setTimelineRefreshing] = useState(false);
  const [timelineBusyMessage, setTimelineBusyMessage] = useState<string | null>(null);
  const [showNotificationsPanel, setShowNotificationsPanel] = useState(false);
  const [notificationsLastSeenAt, setNotificationsLastSeenAt] = useState(0);
  const [dismissedNotificationIds, setDismissedNotificationIds] = useState<Set<string>>(new Set());
  const [dismissingNotificationIds, setDismissingNotificationIds] = useState<Set<string>>(new Set());
  const dismissNotificationTimersRef = useRef<number[]>([]);
  const [showCommentEmojiPanel, setShowCommentEmojiPanel] = useState(false);
  const [commentImageFile, setCommentImageFile] = useState<File | null>(null);
  const [commentImageName, setCommentImageName] = useState("");
  const [commentImagePreviewError, setCommentImagePreviewError] = useState(false);
  const [commentImagePreviewUrl, setCommentImagePreviewUrl] = useState("");
  const [showCreatePost, setShowCreatePost] = useState(false);
  const [newPostStep, setNewPostStep] = useState<0 | 1>(0);
  const [newPostText, setNewPostText] = useState("");
  const [newPostImageFile, setNewPostImageFile] = useState<File | null>(null);
  const [newPostImagePreviewUrl, setNewPostImagePreviewUrl] = useState("");
  const [newPostGalleryItems, setNewPostGalleryItems] = useState<Array<{ id: string; file: File; previewUrl: string }>>([]);
  const [newPostSelectedImageId, setNewPostSelectedImageId] = useState<string | null>(null);
  const [newPostCoffeeId, setNewPostCoffeeId] = useState<string>("");
  const [showCreatePostCoffeeSheet, setShowCreatePostCoffeeSheet] = useState(false);
  const [createPostCoffeeQuery, setCreatePostCoffeeQuery] = useState("");
  const [showCreatePostEmojiPanel, setShowCreatePostEmojiPanel] = useState(false);
  const newPostImageInputRef = useRef<HTMLInputElement | null>(null);
  const newPostCameraInputRef = useRef<HTMLInputElement | null>(null);
  const [authReady, setAuthReady] = useState(false);
  const [sessionEmail, setSessionEmail] = useState<string | null>(null);
  const [authBusy, setAuthBusy] = useState(false);
  const [authError, setAuthError] = useState<string | null>(null);
  const [showAuthPrompt, setShowAuthPrompt] = useState(false);
  const isMobileOsDevice = useMemo(() => /Android|iPhone|iPad|iPod/i.test(window.navigator.userAgent), []);

  const activeUser = useMemo(() => {
    if (!users.length) return null;
    if (sessionEmail) {
      const sessionEmailNormalized = normalizeLookupText(sessionEmail);
      const found = users.find((user) => normalizeLookupText(user.email) === sessionEmailNormalized);
      if (found) return found;
    }
    return users[0] ?? null;
  }, [sessionEmail, users]);

  const navigateToTab = useCallback(
    (
      tab: TabId,
      options?: {
        searchMode?: "coffees" | "users";
        profileUserId?: number | null;
        profileUsername?: string | null;
        coffeeSlug?: string | null;
        replace?: boolean;
      }
    ) => {
      const nextSearchMode = options?.searchMode ?? searchMode;
      const userById =
        options?.profileUserId != null
          ? users.find((item) => item.id === options.profileUserId) ?? null
          : null;
      const nextProfileUsername =
        options?.profileUsername ??
        userById?.username ??
        (tab === "profile" ? profileUsername : null);

      setActiveTab(tab);
      if (tab === "search") setSearchMode(nextSearchMode);
      if (tab === "profile") setProfileUsername(nextProfileUsername ?? null);

      const nextPath = buildRoute(tab, nextSearchMode, nextProfileUsername ?? null, options?.coffeeSlug ?? null);
      if (window.location.pathname === nextPath) return;
      const method = options?.replace ? "replaceState" : "pushState";
      window.history[method]({}, "", `${nextPath}${window.location.search}${window.location.hash}`);
    },
    [profileUsername, searchMode, users]
  );

  useEffect(() => {
    const onPopState = () => {
      const route = parseRoute(window.location.pathname);
      setActiveTab(route.tab);
      setSearchMode(route.searchMode);
      setProfileUsername(route.profileUsername);
      if (route.tab === "coffee") {
        setDetailHostTab(null);
        if (!route.coffeeSlug) {
          setDetailCoffeeId(null);
        } else {
          const counts = new Map<string, number>();
          const sorted = [...coffees].sort((a, b) => {
            const nameCmp = a.nombre.localeCompare(b.nombre);
            if (nameCmp !== 0) return nameCmp;
            return a.id.localeCompare(b.id);
          });
          let found: string | null = null;
          for (const item of sorted) {
            const base = toCoffeeSlug(item.nombre);
            const count = (counts.get(base) ?? 0) + 1;
            counts.set(base, count);
            const slug = count > 1 ? `${base}-${count}` : base;
            if (slug === route.coffeeSlug) {
              found = item.id;
              break;
            }
          }
          setDetailCoffeeId(found);
        }
      } else {
        setDetailCoffeeId(null);
        setDetailHostTab(null);
      }
    };
    window.addEventListener("popstate", onPopState);
    return () => window.removeEventListener("popstate", onPopState);
  }, [coffees]);

  useEffect(() => {
    const route = parseRoute(window.location.pathname);
    const normalized = buildRoute(route.tab, route.searchMode, route.profileUsername, route.coffeeSlug);
    if (window.location.pathname !== normalized) {
      window.history.replaceState({}, "", `${normalized}${window.location.search}${window.location.hash}`);
    }
  }, []);

  useEffect(() => {
    const onResize = () => {
      setViewportWidth(window.innerWidth);
      setMode(window.innerWidth < 900 ? "mobile" : "desktop");
    };
    const onShortcut = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        navigateToTab("search", { searchMode: "coffees" });
        document.getElementById("quick-search")?.focus();
      }
    };

    window.addEventListener("resize", onResize);
    window.addEventListener("keydown", onShortcut);
    const onScroll = () => setTopbarScrolled(window.scrollY > 18);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => {
      window.removeEventListener("resize", onResize);
      window.removeEventListener("keydown", onShortcut);
      window.removeEventListener("scroll", onScroll);
    };
  }, [navigateToTab]);

  useEffect(() => {
    if (!timelineActionBanner) return;
    const id = window.setTimeout(() => setTimelineActionBanner(null), 1800);
    return () => window.clearTimeout(id);
  }, [timelineActionBanner]);

  useEffect(() => {
    setEditingCommentId(null);
    setCommentMenuId(null);
    setCommentDraft("");
    setShowCommentEmojiPanel(false);
  }, [commentSheetPostId]);

  useEffect(() => {
    if (!showCreatePost) return;
    setNewPostCoffeeId((prev) => prev || "");
  }, [showCreatePost]);

  useEffect(() => {
    if (!showCreatePost || newPostStep !== 1) return;
    const id = window.requestAnimationFrame(() => {
      document.getElementById("new-post-text")?.focus();
    });
    return () => window.cancelAnimationFrame(id);
  }, [newPostStep, showCreatePost]);

  useEffect(() => {
    if (activeTab !== "search" || searchMode !== "coffees" || searchFocusCoffeeProfile) {
      setSearchActiveFilterType(null);
    }
  }, [activeTab, searchFocusCoffeeProfile, searchMode]);

  useEffect(() => {
    if (showCreatePost) return;
    newPostGalleryItems.forEach((item) => {
      if (item.previewUrl.startsWith("blob:")) URL.revokeObjectURL(item.previewUrl);
    });
  }, [newPostGalleryItems, newPostImagePreviewUrl, showCreatePost]);

  useEffect(() => {
    if (supabaseConfigError) {
      setAuthError(supabaseConfigError);
      setAuthReady(true);
      return;
    }

    const supabase = getSupabaseClient();
    let mounted = true;
    void supabase.auth.getSession().then(({ data, error }) => {
      if (!mounted) return;
      if (error) setAuthError(error.message);
      setSessionEmail(data.session?.user?.email ?? null);
      setAuthReady(true);
    });

    const { data } = supabase.auth.onAuthStateChange((_event, session) => {
      setSessionEmail(session?.user?.email ?? null);
      setAuthReady(true);
    });

    return () => {
      mounted = false;
      data.subscription.unsubscribe();
    };
  }, []);

  const handleGoogleLogin = useCallback(async () => {
    if (supabaseConfigError) return;
    setAuthBusy(true);
    setAuthError(null);
    try {
      const supabase = getSupabaseClient();
      const { error } = await supabase.auth.signInWithOAuth({
        provider: "google",
        options: {
          redirectTo: `${window.location.origin}${window.location.pathname}`
        }
      });
      if (error) throw error;
    } catch (error) {
      setAuthError((error as Error).message);
      setAuthBusy(false);
      return;
    }
  }, []);

  const requestLogin = useCallback(() => {
    setAuthError(null);
    setShowAuthPrompt(true);
  }, []);

  useEffect(() => {
    if (sessionEmail) setShowAuthPrompt(false);
  }, [sessionEmail]);

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
      setShowCreateCoffeeComposer(false);
      setCreateCoffeeSaving(false);
      setCreateCoffeeError(null);
      setCreateCoffeeDraft({
        name: "",
        brand: "",
        specialty: "",
        country: "",
        format: "",
        roast: "",
        variety: "",
        hasCaffeine: true,
        totalGrams: 250
      });
      setCreateCoffeeImageFile(null);
      setCreateCoffeeImagePreviewUrl("");
      setDetailCoffeeId(null);
      setDetailHostTab(null);
      setGlobalStatus("Listo");
    } catch (error) {
      setAuthError((error as Error).message);
    }
  }, []);

  const resetCreatePostComposer = useCallback(() => {
    newPostGalleryItems.forEach((item) => {
      if (item.previewUrl.startsWith("blob:")) URL.revokeObjectURL(item.previewUrl);
    });
    setShowCreatePost(false);
    setNewPostStep(0);
    setNewPostText("");
    setNewPostImageFile(null);
    setNewPostImagePreviewUrl("");
    setNewPostGalleryItems([]);
    setNewPostSelectedImageId(null);
    setNewPostCoffeeId("");
    setCreatePostCoffeeQuery("");
    setShowCreatePostCoffeeSheet(false);
    setShowCreatePostEmojiPanel(false);
  }, [newPostGalleryItems, newPostImagePreviewUrl]);

  const openCreatePostComposer = useCallback(() => {
    setShowCreatePost(true);
    setNewPostStep(0);
  }, []);

  const appendNewPostFiles = useCallback(
    (files: File[]) => {
      if (!files.length) return;
      const added = files.map((file, index) => ({
        id: `${Date.now()}-${index}-${Math.random().toString(36).slice(2, 8)}`,
        file,
        previewUrl: URL.createObjectURL(file)
      }));
      setNewPostGalleryItems((prev) => [...added, ...prev].slice(0, 24));
      const first = added[0];
      if (first) {
        setNewPostSelectedImageId(first.id);
        setNewPostImageFile(first.file);
        setNewPostImagePreviewUrl(first.previewUrl);
      }
    },
    []
  );

  useEffect(() => {
    return () => {
      if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
    };
  }, [createCoffeeImagePreviewUrl]);

  useEffect(() => {
    const onEscSheet = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      if (showNotificationsPanel) {
        setShowNotificationsPanel(false);
      }
      if (showCreatePost) {
        resetCreatePostComposer();
      }
      if (showCreatePostCoffeeSheet) {
        setShowCreatePostCoffeeSheet(false);
      }
      if (commentSheetPostId) {
        setCommentSheetPostId(null);
        setCommentDraft("");
        setHighlightedCommentId(null);
      }
      if (showCreateCoffeeComposer) {
        setShowCreateCoffeeComposer(false);
      }
    };
    window.addEventListener("keydown", onEscSheet);
    return () => window.removeEventListener("keydown", onEscSheet);
  }, [commentSheetPostId, resetCreatePostComposer, showCreateCoffeeComposer, showCreatePost, showCreatePostCoffeeSheet, showNotificationsPanel]);

  useEffect(() => {
    const hasModal = Boolean(commentSheetPostId || showCreatePost || showNotificationsPanel || showCreatePostCoffeeSheet);
    const prev = document.body.style.overflow;
    if (hasModal) document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [commentSheetPostId, showCreatePost, showCreatePostCoffeeSheet, showNotificationsPanel]);

  useEffect(() => {
    const onEsc = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setCommentMenuId(null);
      if (editingCommentId) {
        setEditingCommentId(null);
        setCommentDraft("");
      }
    };
    window.addEventListener("keydown", onEsc);
    return () => window.removeEventListener("keydown", onEsc);
  }, [editingCommentId]);

  useEffect(() => {
    if (activeTab !== "search" || searchMode !== "users") return;
    const id = window.setTimeout(() => {
      document.getElementById("quick-search")?.focus();
    }, 40);
    return () => window.clearTimeout(id);
  }, [activeTab, searchMode]);

  useEffect(() => {
    if (!commentSheetPostId) return;
    const list = commentListRef.current;
    if (!list) return;
    list.scrollTop = list.scrollHeight;
  }, [commentSheetPostId, comments, editingCommentId]);

  useEffect(() => {
    return () => {
      if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
    };
  }, [commentImagePreviewUrl]);

  useEffect(() => {
    if (!commentSheetPostId || !highlightedCommentId) return;
    const list = commentListRef.current;
    if (!list) return;
    const target = list.querySelector<HTMLElement>(`[data-comment-id="${highlightedCommentId}"]`);
    if (!target) return;
    target.scrollIntoView({ block: "center", behavior: "smooth" });
  }, [commentSheetPostId, comments, highlightedCommentId]);

  useEffect(() => {
    if (handledTimelineDeepLink) return;
    const params = new URLSearchParams(window.location.search);
    const postId = params.get("postId");
    const commentIdRaw = params.get("commentId");
    const commentId = commentIdRaw ? Number(commentIdRaw) : null;
    if (!postId) return;
    if (!posts.some((post) => post.id === postId)) return;
    navigateToTab("timeline", { replace: true });
    setCommentSheetPostId(postId);
    if (commentId != null && Number.isFinite(commentId)) setHighlightedCommentId(commentId);
    setHandledTimelineDeepLink(true);
    const url = new URL(window.location.href);
    url.searchParams.delete("postId");
    url.searchParams.delete("commentId");
    window.history.replaceState({}, "", url.toString());
  }, [handledTimelineDeepLink, navigateToTab, posts]);

  useEffect(
    () => () => {
      dismissNotificationTimersRef.current.forEach((id) => window.clearTimeout(id));
      dismissNotificationTimersRef.current = [];
    },
    []
  );

  const loadInitialData = useCallback(async () => {
    try {
      const data = await fetchInitialData();
      setUsers(data.users);
      setCoffees(data.coffees);
      setCoffeeReviews(data.reviews);
      setCoffeeSensoryProfiles(data.sensoryProfiles);
      setPosts(data.posts);
      setLikes(data.likes);
      setComments(data.comments);
      setPostCoffeeTags(data.postCoffeeTags);
      setFollows(data.follows);
      setBrewCoffeeId((prev) => prev || data.coffees[0]?.id || "");
      setGlobalStatus("Listo");
    } catch (error) {
      setGlobalStatus(`Error: ${(error as Error).message}`);
    }
  }, []);

  useEffect(() => {
    if (!authReady) return;
    void loadInitialData();
  }, [authReady, loadInitialData]);

  useEffect(() => {
    if (activeTab === "diary") return;
    setShowDiaryQuickActions(false);
    setShowDiaryPeriodSheet(false);
    setShowDiaryWaterSheet(false);
    setShowDiaryCoffeeSheet(false);
    setShowDiaryAddPantrySheet(false);
  }, [activeTab]);

  useEffect(() => {
    if (!activeUser) return;
    (async () => {
      try {
        const data = await fetchUserData(activeUser.id);
        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
        setCustomCoffees(data.customCoffees);
      } catch (error) {
        setGlobalStatus(`Error: ${(error as Error).message}`);
      }
    })();
  }, [activeUser]);

  const brewCoffeeCatalog = useMemo(() => {
    if (!customCoffees.length) return coffees;
    const byId = new Map<string, CoffeeRow>();
    coffees.forEach((coffee) => byId.set(String(coffee.id), coffee));
    customCoffees.forEach((coffee) => byId.set(String(coffee.id), coffee));
    return Array.from(byId.values());
  }, [coffees, customCoffees]);

  useEffect(() => {
    if (!brewRunning || brewStep !== "brewing") return;
    const totalDuration = getBrewTimelineForMethod(brewMethod, waterMl).reduce((acc, phase) => acc + phase.durationSeconds, 0);
    if (totalDuration > 0 && timerSeconds >= totalDuration) {
      setBrewRunning(false);
      setBrewStep("result");
      return;
    }
    const id = window.setTimeout(() => setTimerSeconds((prev) => prev + 1), 1000);
    return () => window.clearTimeout(id);
  }, [brewMethod, brewRunning, brewStep, timerSeconds, waterMl]);

  const usersById = useMemo(() => {
    const map = new Map<number, UserRow>();
    users.forEach((user) => map.set(user.id, user));
    return map;
  }, [users]);

  const tagsByPostId = useMemo(() => {
    const map = new Map<string, PostCoffeeTagRow>();
    postCoffeeTags.forEach((tag) => {
      if (!map.has(tag.post_id)) map.set(tag.post_id, tag);
    });
    return map;
  }, [postCoffeeTags]);

  const likesByPostId = useMemo(() => {
    const map = new Map<string, number>();
    likes.forEach((like) => {
      map.set(like.post_id, (map.get(like.post_id) ?? 0) + 1);
    });
    return map;
  }, [likes]);

  const commentsByPostId = useMemo(() => {
    const map = new Map<string, CommentRow[]>();
    comments.forEach((comment) => {
      const bucket = map.get(comment.post_id);
      if (bucket) {
        bucket.push(comment);
      } else {
        map.set(comment.post_id, [comment]);
      }
    });
    return map;
  }, [comments]);

  const coffeeIdByNameBrand = useMemo(() => {
    const map = new Map<string, string>();
    const nameOnlyMap = new Map<string, string>();
    coffees.forEach((coffee) => {
      const normalizedBrand = normalizeLookupText(coffee.marca);
      const normalizedName = normalizeLookupText(coffee.nombre);
      const key = `${normalizedBrand}|${normalizedName}`;
      map.set(key, coffee.id);
      if (normalizedName && !nameOnlyMap.has(normalizedName)) {
        nameOnlyMap.set(normalizedName, coffee.id);
      }
    });
    return { byNameBrand: map, byName: nameOnlyMap };
  }, [coffees]);
  const coffeesById = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    coffees.forEach((coffee) => map.set(coffee.id, coffee));
    return map;
  }, [coffees]);
  const coffeeSlugIndex = useMemo(() => {
    const bySlug = new Map<string, string>();
    const byId = new Map<string, string>();
    const counts = new Map<string, number>();
    const sorted = [...coffees].sort((a, b) => {
      const nameCmp = a.nombre.localeCompare(b.nombre);
      if (nameCmp !== 0) return nameCmp;
      return a.id.localeCompare(b.id);
    });
    sorted.forEach((coffee) => {
      const base = toCoffeeSlug(coffee.nombre);
      const count = (counts.get(base) ?? 0) + 1;
      counts.set(base, count);
      const slug = count > 1 ? `${base}-${count}` : base;
      bySlug.set(slug, coffee.id);
      byId.set(coffee.id, slug);
    });
    return { bySlug, byId };
  }, [coffees]);

  useEffect(() => {
    const route = parseRoute(window.location.pathname);
    if (route.tab !== "coffee") return;
    if (!route.coffeeSlug) return;
    const coffeeId = coffeeSlugIndex.bySlug.get(route.coffeeSlug) ?? null;
    if (coffeeId) {
      setDetailCoffeeId(coffeeId);
      setDetailHostTab(null);
    }
  }, [coffeeSlugIndex]);


  const timelineCards: TimelineCard[] = useMemo(
    () =>
      posts.map((post) => {
        const user = usersById.get(post.user_id);
        const postLikes = likesByPostId.get(post.id) ?? 0;
        const postComments = commentsByPostId.get(post.id)?.length ?? 0;
        const likedByActiveUser =
          activeUser != null &&
          likes.some((like) => like.post_id === post.id && like.user_id === activeUser.id);
        const tag = tagsByPostId.get(post.id);
        const normalizedTagBrand = normalizeLookupText(tag?.coffee_brand);
        const normalizedTagName = normalizeLookupText(tag?.coffee_name);
        const coffeeKey = tag ? `${normalizedTagBrand}|${normalizedTagName}` : "";
        const coffeeId =
          (tag?.coffee_id ? tag.coffee_id : null) ??
          (coffeeKey ? coffeeIdByNameBrand.byNameBrand.get(coffeeKey) : null) ??
          (normalizedTagName ? coffeeIdByNameBrand.byName.get(normalizedTagName) : null) ??
          null;
        const coffee = coffeeId ? coffeesById.get(coffeeId) ?? null : null;

        return {
          id: post.id,
          userId: post.user_id,
          userName: user?.full_name ?? `Usuario ${post.user_id}`,
          username: user?.username ?? `user${post.user_id}`,
          avatarUrl: user?.avatar_url ?? "",
          text: post.comment,
          imageUrl: post.image_url,
          minsAgoLabel: toRelativeMinutes(post.timestamp),
          likes: postLikes,
          comments: postComments,
          coffeeId,
          coffeeTagName: tag?.coffee_name ?? null,
          coffeeTagBrand: tag?.coffee_brand ?? null,
          coffeeImageUrl: coffee?.image_url ?? null,
          likedByActiveUser
        };
      }),
    [activeUser, coffeeIdByNameBrand, coffeesById, commentsByPostId, likes, likesByPostId, posts, tagsByPostId, usersById]
  );

  const filteredCoffees = useMemo(() => {
    const q = normalizeLookupText(searchQuery);
    const ratingByCoffee = new Map<string, { total: number; count: number }>();
    coffeeReviews.forEach((review) => {
      const bucket = ratingByCoffee.get(review.coffee_id) ?? { total: 0, count: 0 };
      bucket.total += review.rating;
      bucket.count += 1;
      ratingByCoffee.set(review.coffee_id, bucket);
    });
    return coffees.filter((coffee) => {
      const byName = normalizeLookupText(coffee.nombre).includes(q);
      const byBrand = normalizeLookupText(coffee.marca).includes(q);
      const byOrigin = normalizeLookupText(coffee.pais_origen).includes(q);
      const queryMatch = !q || byName || byBrand || byOrigin;
      if (!queryMatch) return false;

      const origin = toUiOptionValue(coffee.pais_origen ?? "");
      const originMatch =
        !searchSelectedOrigins.size || (origin ? searchSelectedOrigins.has(origin) : false);
      if (!originMatch) return false;

      const roast = toUiOptionValue(coffee.tueste ?? "");
      const roastMatch = !searchSelectedRoasts.size || (roast ? searchSelectedRoasts.has(roast) : false);
      if (!roastMatch) return false;

      const specialty = toUiOptionValue(coffee.especialidad ?? "");
      const specialtyMatch = !searchSelectedSpecialties.size || (specialty ? searchSelectedSpecialties.has(specialty) : false);
      if (!specialtyMatch) return false;

      const format = toUiOptionValue(coffee.formato ?? "");
      const formatMatch = !searchSelectedFormats.size || (format ? searchSelectedFormats.has(format) : false);
      if (!formatMatch) return false;

      if (searchMinRating > 0) {
        const stats = ratingByCoffee.get(coffee.id);
        const avg = stats && stats.count > 0 ? stats.total / stats.count : 0;
        if (avg < searchMinRating) return false;
      }

      return true;
    });
  }, [
    coffeeReviews,
    coffees,
    searchMinRating,
    searchQuery,
    searchSelectedFormats,
    searchSelectedOrigins,
    searchSelectedRoasts,
    searchSelectedSpecialties
  ]);

  const searchOriginOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.pais_origen)),
    [coffees]
  );

  const searchRoastOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.tueste)),
    [coffees]
  );

  const searchSpecialtyOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.especialidad)),
    [coffees]
  );

  const searchFormatOptions = useMemo(
    () => buildNormalizedOptions(coffees.map((coffee) => coffee.formato)),
    [coffees]
  );
  const selectedCoffee = brewCoffeeCatalog.find((coffee) => coffee.id === brewCoffeeId) ?? brewCoffeeCatalog[0];
  const coffeeGrams = Math.max(1, Math.round(waterMl / ratio));
  const saveBrewToDiary = useCallback(
    async (taste: string) => {
      if (!activeUser || !selectedCoffee) return;
      const decaf = normalizeLookupText(selectedCoffee.cafeina ?? "").includes("sin");
      const caffeineMg = Math.max(0, Math.round(coffeeGrams * (decaf ? 2 : 9)));
      const preparationType = `Lab: ${brewMethod || "Metodo"} (${taste})`;
      const created = await createDiaryEntry({
        userId: activeUser.id,
        coffeeId: selectedCoffee.id,
        coffeeName: selectedCoffee.nombre,
        amountMl: waterMl,
        caffeineMg,
        preparationType,
        type: "CUP"
      });
      setDiaryEntries((prev) => [created, ...prev]);
      setBrewRunning(false);
      setTimerSeconds(0);
      setBrewStep("method");
      navigateToTab("diary");
    },
    [activeUser, brewMethod, coffeeGrams, navigateToTab, selectedCoffee, setBrewStep, waterMl]
  );
  const brewPantryItems = useMemo(() => {
    const coffeeById = new Map<string, CoffeeRow>();
    brewCoffeeCatalog.forEach((coffee) => {
      coffeeById.set(String(coffee.id), coffee);
    });

    const latestByCoffee = new Map<string, PantryItemRow>();
    pantryItems.forEach((item) => {
      const coffeeId = String(item.coffee_id);
      if (!coffeeId) return;
      const prev = latestByCoffee.get(coffeeId);
      if (!prev || Number(item.last_updated ?? 0) > Number(prev.last_updated ?? 0)) {
        latestByCoffee.set(coffeeId, item);
      }
    });

    return Array.from(latestByCoffee.values())
      .map((item) => {
        const coffeeId = String(item.coffee_id);
        const coffee = coffeeById.get(coffeeId);
        if (!coffee) return null;
        const total = Math.max(0, Number(item.total_grams ?? 0));
        const remaining = Math.max(0, Math.min(total, Number(item.grams_remaining ?? 0)));
        const progress = total > 0 ? remaining / total : 0;
        return { item, coffee, total, remaining, progress };
      })
      .filter((row): row is { item: PantryItemRow; coffee: CoffeeRow; total: number; remaining: number; progress: number } => Boolean(row))
      .sort((a, b) => Number(b.item.last_updated ?? 0) - Number(a.item.last_updated ?? 0));
  }, [brewCoffeeCatalog, pantryItems]);

  const diaryEntriesActivity = diaryEntries.slice(0, 80);
  const pantryCoffeeRows = pantryItems
    .map((item) => ({ item, coffee: brewCoffeeCatalog.find((coffee) => coffee.id === item.coffee_id) }))
    .filter((row) => row.coffee);
  const handleDeleteDiaryEntry = useCallback(
    async (entryId: number) => {
      if (!activeUser) return;
      await deleteDiaryEntry(entryId, activeUser.id);
      setDiaryEntries((prev) => prev.filter((entry) => entry.id !== entryId));
    },
    [activeUser]
  );
  const handleUpdateDiaryEntry = useCallback(
    async (entryId: number, amountMl: number, caffeineMg: number, preparationType: string) => {
      if (!activeUser) return;
      const updated = await updateDiaryEntry({
        entryId,
        userId: activeUser.id,
        amountMl,
        caffeineMg,
        preparationType
      });
      setDiaryEntries((prev) => prev.map((entry) => (entry.id === updated.id ? { ...entry, ...updated } : entry)));
    },
    [activeUser]
  );
  const handleUpdatePantryStock = useCallback(
    async (coffeeId: string, totalGrams: number, gramsRemaining: number) => {
      if (!activeUser) return;
      const total = Math.max(1, Math.round(totalGrams));
      const remaining = Math.max(0, Math.min(total, Math.round(gramsRemaining)));
      const updated = await upsertPantryStock({
        coffeeId,
        userId: activeUser.id,
        totalGrams: total,
        gramsRemaining: remaining
      });
      setPantryItems((prev) =>
        [updated, ...prev.filter((row) => !(row.user_id === updated.user_id && row.coffee_id === updated.coffee_id))]
      );
    },
    [activeUser]
  );
  const handleRemovePantryItem = useCallback(
    async (coffeeId: string) => {
      if (!activeUser) return;
      await deletePantryItem(coffeeId, activeUser.id);
      setPantryItems((prev) => prev.filter((row) => !(row.user_id === activeUser.id && row.coffee_id === coffeeId)));
    },
    [activeUser]
  );
  const diaryCoffeeOptions = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    pantryCoffeeRows.forEach((row) => {
      if (row.coffee?.id) map.set(row.coffee.id, row.coffee);
    });
    brewCoffeeCatalog.forEach((coffee) => {
      if (!map.has(coffee.id)) map.set(coffee.id, coffee);
    });
    return Array.from(map.values()).slice(0, 500);
  }, [brewCoffeeCatalog, pantryCoffeeRows]);
  const selectedDiaryCoffee = useMemo(
    () => diaryCoffeeOptions.find((coffee) => coffee.id === diaryCoffeeIdDraft) ?? diaryCoffeeOptions[0] ?? null,
    [diaryCoffeeIdDraft, diaryCoffeeOptions]
  );
  const selectedDiaryPantryCoffee = useMemo(
    () => diaryCoffeeOptions.find((coffee) => coffee.id === diaryPantryCoffeeIdDraft) ?? diaryCoffeeOptions[0] ?? null,
    [diaryCoffeeOptions, diaryPantryCoffeeIdDraft]
  );
  const bumpPantryGrams = (delta: number) => {
    const next = Math.max(1, Math.round((Number(diaryPantryGramsDraft || 0) || 0) + delta));
    setDiaryPantryGramsDraft(String(next));
  };

  const profileUser = useMemo(() => {
    if (profileUsername) {
      const routeUsername = normalizeLookupText(profileUsername);
      const fromRoute = users.find((user) => normalizeLookupText(user.username) === routeUsername);
      if (fromRoute) return fromRoute;
    }
    return usersById.get(activeUser?.id ?? -1) ?? null;
  }, [activeUser?.id, profileUsername, users, usersById]);
  const profilePosts = timelineCards.filter((card) => card.userId === (profileUser?.id ?? -1));

  const favoriteCoffees = favorites.map((favorite) => coffees.find((coffee) => coffee.id === favorite.coffee_id)).filter((coffee): coffee is CoffeeRow => Boolean(coffee));
  const detailCoffee = detailCoffeeId ? coffeesById.get(detailCoffeeId) ?? null : null;
  const detailCoffeeReviews = useMemo(() => {
    if (!detailCoffeeId) return [] as Array<CoffeeReviewRow & { user: UserRow | null }>;
    return coffeeReviews
      .filter((item) => item.coffee_id === detailCoffeeId && typeof item.user_id === "number")
      .map((item) => ({
        ...item,
        user: typeof item.user_id === "number" ? usersById.get(item.user_id) ?? null : null
      }))
      .sort((a, b) => (b.timestamp ?? 0) - (a.timestamp ?? 0));
  }, [coffeeReviews, detailCoffeeId, usersById]);
  const detailCoffeeAverageRating = useMemo(() => {
    if (!detailCoffeeReviews.length) return 0;
    const sum = detailCoffeeReviews.reduce((acc, item) => acc + item.rating, 0);
    return Number((sum / detailCoffeeReviews.length).toFixed(1));
  }, [detailCoffeeReviews]);
  const detailCurrentUserReview = useMemo(() => {
    if (!activeUser || !detailCoffeeId) return null;
    return coffeeReviews.find((item) => item.coffee_id === detailCoffeeId && item.user_id === activeUser.id) ?? null;
  }, [activeUser, coffeeReviews, detailCoffeeId]);
  const detailCurrentUser = useMemo(() => {
    if (!activeUser) return null;
    return usersById.get(activeUser.id) ?? null;
  }, [activeUser, usersById]);
  const detailCurrentUserReviewWithUser = useMemo(() => {
    if (!detailCurrentUserReview) return null;
    return {
      ...detailCurrentUserReview,
      user: detailCurrentUser
    };
  }, [detailCurrentUser, detailCurrentUserReview]);
  const detailIsFavorite = useMemo(() => {
    if (!activeUser || !detailCoffeeId) return false;
    return favorites.some((item) => item.user_id === activeUser.id && item.coffee_id === detailCoffeeId);
  }, [activeUser, detailCoffeeId, favorites]);
  const detailPantryStock = useMemo(() => {
    if (!activeUser || !detailCoffeeId) return null;
    return pantryItems.find((item) => item.user_id === activeUser.id && item.coffee_id === detailCoffeeId) ?? null;
  }, [activeUser, detailCoffeeId, pantryItems]);
  const detailSensoryAverages = useMemo(() => {
    if (!detailCoffeeId || !detailCoffee) return { aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 };
    const rows = coffeeSensoryProfiles.filter((item) => item.coffee_id === detailCoffeeId);
    const base = {
      aroma: Number(detailCoffee.aroma ?? 0),
      sabor: Number(detailCoffee.sabor ?? 0),
      cuerpo: Number(detailCoffee.cuerpo ?? 0),
      acidez: Number(detailCoffee.acidez ?? 0),
      dulzura: Number(detailCoffee.dulzura ?? 0)
    };
    if (!rows.length) return base;
    const avg = (key: keyof CoffeeSensoryProfileRow) => {
      const total = rows.reduce((acc, row) => acc + Number(row[key]), 0);
      return Number((total / rows.length).toFixed(1));
    };
    return {
      aroma: avg("aroma"),
      sabor: avg("sabor"),
      cuerpo: avg("cuerpo"),
      acidez: avg("acidez"),
      dulzura: avg("dulzura")
    };
  }, [coffeeSensoryProfiles, detailCoffee, detailCoffeeId]);

  useEffect(() => {
    if (!detailCoffee) return;
    if (detailCurrentUserReview) {
      setDetailReviewText(detailCurrentUserReview.comment ?? "");
      setDetailReviewRating(detailCurrentUserReview.rating);
      setDetailReviewImagePreviewUrl(detailCurrentUserReview.image_url ?? "");
    } else {
      setDetailReviewText("");
      setDetailReviewRating(0);
      setDetailReviewImagePreviewUrl("");
    }
    setDetailReviewImageFile(null);
    setDetailStockDraft({
      total: detailPantryStock?.total_grams ?? 0,
      remaining: detailPantryStock?.grams_remaining ?? 0
    });
    setDetailSensoryDraft({
      aroma: detailSensoryAverages.aroma,
      sabor: detailSensoryAverages.sabor,
      cuerpo: detailSensoryAverages.cuerpo,
      acidez: detailSensoryAverages.acidez,
      dulzura: detailSensoryAverages.dulzura
    });
  }, [detailCoffee, detailCurrentUserReview, detailPantryStock, detailSensoryAverages]);

  useEffect(() => {
    if (!detailReviewImagePreviewUrl.startsWith("blob:")) return;
    const blobUrl = detailReviewImagePreviewUrl;
    return () => {
      URL.revokeObjectURL(blobUrl);
    };
  }, [detailReviewImagePreviewUrl]);

  const followersCount = profileUser ? follows.filter((follow) => follow.followed_id === profileUser.id).length : 0;
  const followingCount = profileUser ? follows.filter((follow) => follow.follower_id === profileUser.id).length : 0;
  const followingIds = useMemo(() => {
    if (!activeUser) return new Set<number>();
    return new Set(follows.filter((follow) => follow.follower_id === activeUser.id).map((follow) => follow.followed_id));
  }, [activeUser, follows]);
  const followerCounts = useMemo(() => {
    const map = new Map<number, number>();
    follows.forEach((follow) => {
      map.set(follow.followed_id, (map.get(follow.followed_id) ?? 0) + 1);
    });
    return map;
  }, [follows]);
  const filteredSearchUsers = useMemo(() => {
    const q = normalizeLookupText(searchQuery);
    const base = users.filter((user) => user.id !== activeUser?.id);
    if (!q) return base.filter((user) => !followingIds.has(user.id)).slice(0, 80);
    return base
      .filter((user) => normalizeLookupText(user.username).includes(q) || normalizeLookupText(user.full_name).includes(q))
      .slice(0, 120);
  }, [activeUser?.id, followingIds, searchQuery, users]);
  const selectedCreatePostCoffee = useMemo(
    () => coffees.find((coffee) => coffee.id === newPostCoffeeId) ?? null,
    [coffees, newPostCoffeeId]
  );
  const filteredCreatePostCoffees = useMemo(() => {
    const query = normalizeLookupText(createPostCoffeeQuery);
    if (!query) return coffees.slice(0, 30);
    return coffees
      .filter((coffee) => normalizeLookupText(coffee.nombre).includes(query) || normalizeLookupText(coffee.marca).includes(query))
      .slice(0, 40);
  }, [coffees, createPostCoffeeQuery]);
  const createPostMentionSuggestions = useMemo(() => {
    const draftParts = newPostText.split(/\s+/);
    const token = draftParts[draftParts.length - 1] ?? "";
    if (!token.startsWith("@")) return [] as UserRow[];
    const query = normalizeLookupText(token.slice(1));
    if (!query) return users.slice(0, 6);
    return users.filter((user) => normalizeLookupText(user.username).includes(query)).slice(0, 6);
  }, [newPostText, users]);

  const timelineRecommendations = useMemo(() => {
    if (!coffees.length) return [] as CoffeeRow[];
    if (!activeUser) return coffees.slice(0, 5);

    const scoreByCoffeeId = new Map<string, number>();
    const addScore = (coffeeId: string | null | undefined, points: number) => {
      if (!coffeeId) return;
      scoreByCoffeeId.set(coffeeId, (scoreByCoffeeId.get(coffeeId) ?? 0) + points);
    };

    favorites
      .filter((favorite) => favorite.user_id === activeUser.id)
      .forEach((favorite) => addScore(favorite.coffee_id, 120));

    pantryItems
      .filter((item) => item.user_id === activeUser.id)
      .forEach((item) => addScore(item.coffee_id, 70 + Math.min(20, Math.round((item.grams_remaining ?? 0) / 40))));

    diaryEntries
      .filter((entry) => entry.user_id === activeUser.id)
      .forEach((entry) => addScore(entry.coffee_id, 16));

    const followedIds = new Set(
      follows.filter((follow) => follow.follower_id === activeUser.id).map((follow) => follow.followed_id)
    );
    timelineCards
      .filter((card) => followedIds.has(card.userId))
      .forEach((card) => addScore(card.coffeeId, 18));

    const coffeeByPostId = new Map<string, string | null>();
    timelineCards.forEach((card) => coffeeByPostId.set(card.id, card.coffeeId));

    likes
      .filter((like) => like.user_id === activeUser.id)
      .forEach((like) => addScore(coffeeByPostId.get(like.post_id) ?? null, 14));

    comments
      .filter((comment) => comment.user_id === activeUser.id)
      .forEach((comment) => addScore(coffeeByPostId.get(comment.post_id) ?? null, 8));

    const scored = coffees
      .map((coffee) => ({ coffee, score: scoreByCoffeeId.get(coffee.id) ?? 0 }))
      .sort((a, b) => {
        if (b.score !== a.score) return b.score - a.score;
        return a.coffee.nombre.localeCompare(b.coffee.nombre);
      })
      .map((entry) => entry.coffee);

    return scored.slice(0, 5);
  }, [activeUser, coffees, comments, diaryEntries, favorites, follows, likes, pantryItems, timelineCards]);
  const timelineSuggestions = useMemo(
    () =>
      users
        .filter((user) => user.id !== activeUser?.id && !followingIds.has(user.id))
        .slice(0, 5),
    [activeUser?.id, followingIds, users]
  );
  const timelineSuggestionIndices = useMemo(() => {
    if (timelineCards.length < 3) return [] as number[];
    const seed = Math.max(timelineCards.length, 1) + (activeUser?.id ?? 0);
    const first = Math.max(1, seed % timelineCards.length);
    let second = Math.max(1, (seed * 7) % timelineCards.length);
    if (second === first) second = Math.min(timelineCards.length - 1, second + 1);
    return [first, second];
  }, [activeUser?.id, timelineCards.length]);

  const commentSheetRows = useMemo(
    () =>
      [...(commentsByPostId.get(commentSheetPostId ?? "") ?? [])]
        .sort((a, b) => a.timestamp - b.timestamp)
        .slice(-120),
    [commentSheetPostId, commentsByPostId]
  );
  const activeCommentMenuRow = useMemo(
    () => commentSheetRows.find((row) => row.id === commentMenuId) ?? null,
    [commentMenuId, commentSheetRows]
  );
  const commentMentionSuggestions = useMemo(() => {
    const draftParts = commentDraft.split(/\s+/);
    const token = draftParts[draftParts.length - 1] ?? "";
    if (!token.startsWith("@")) return [];
    const query = normalizeLookupText(token.slice(1));
    if (!query) return users.slice(0, 8);
    return users.filter((user) => normalizeLookupText(user.username).includes(query)).slice(0, 8);
  }, [commentDraft, users]);
  const commentListRef = useRef<HTMLUListElement | null>(null);
  const commentImageInputRef = useRef<HTMLInputElement | null>(null);
  const timelineNotifications = useMemo<TimelineNotificationItem[]>(() => {
    if (!activeUser) return [];

    const followEvents = follows
      .filter((follow) => follow.followed_id === activeUser.id && follow.follower_id !== activeUser.id)
      .map((follow) => ({
        id: `follow-${follow.follower_id}-${follow.followed_id}`,
        type: "follow" as const,
        userId: follow.follower_id,
        text: "ha comenzado a seguirte",
        timestamp: toEventTimestamp(follow.created_at)
      }));

    const commentEvents = comments
      .filter((comment) => comment.user_id !== activeUser.id)
      .slice(0, 40)
      .map((comment) => {
        const post = posts.find((entry) => entry.id === comment.post_id);
        if (!post || post.user_id !== activeUser.id) return null;
        return {
          id: `comment-${comment.id}`,
          type: "comment" as const,
          userId: comment.user_id,
          text: "ha comentado en tu publicacion",
          timestamp: toEventTimestamp(comment.timestamp),
          postId: comment.post_id,
          commentId: comment.id
        };
      })
      .filter(
        (event): event is { id: string; type: "comment"; userId: number; text: string; timestamp: number; postId: string; commentId: number } =>
          Boolean(event)
      );

    return [...followEvents, ...commentEvents]
      .sort((a, b) => b.timestamp - a.timestamp)
      .slice(0, 24);
  }, [activeUser, comments, follows, posts]);
  const visibleTimelineNotifications = useMemo(
    () => timelineNotifications.filter((item) => !dismissedNotificationIds.has(item.id)),
    [dismissedNotificationIds, timelineNotifications]
  );
  const showNotificationsBadge = useMemo(
    () => visibleTimelineNotifications.some((item) => item.timestamp > notificationsLastSeenAt),
    [notificationsLastSeenAt, visibleTimelineNotifications]
  );

  useEffect(() => {
    if (!showNotificationsPanel) return;
    const latestSeen = visibleTimelineNotifications.reduce((max, item) => Math.max(max, item.timestamp), 0);
    if (latestSeen > notificationsLastSeenAt) setNotificationsLastSeenAt(latestSeen);
  }, [notificationsLastSeenAt, showNotificationsPanel, visibleTimelineNotifications]);

  const handleToggleLike = async (postId: string) => {
    if (!activeUser) return;

    const alreadyLiked = likes.some((like) => like.post_id === postId && like.user_id === activeUser.id);
    setTimelineBusyMessage(alreadyLiked ? "Quitando like..." : "Enviando like...");
    vibrateTap(8);
    try {
      const payload = await toggleLike(postId, activeUser.id, alreadyLiked);
      if (!payload) {
        setLikes((prev) => prev.filter((like) => !(like.post_id === postId && like.user_id === activeUser.id)));
        setTimelineBusyMessage(null);
        return;
      }
      setLikes((prev) => [...prev, payload]);
    } catch (error) {
      setGlobalStatus(`Error like: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
    setTimelineActionBanner(alreadyLiked ? "Like eliminado" : "Like enviado");
  };

  const handleAddComment = async () => {
    if (!activeUser || !commentSheetPostId) return;
    const text = commentDraft.trim();
    if (!text) return;

    setTimelineBusyMessage("Publicando comentario...");
    vibrateTap(8);
    try {
      const newComment = await createComment(commentSheetPostId, activeUser.id, text);
      setComments((prev) => [newComment, ...prev]);
      setCommentDraft("");
      if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
      setCommentImageFile(null);
      setCommentImageName("");
      setCommentImagePreviewError(false);
      setCommentImagePreviewUrl("");
      setTimelineActionBanner("Comentario publicado");
    } catch (error) {
      setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleUpdateComment = async () => {
    if (!editingCommentId) return;
    const text = commentDraft.trim();
    if (!text) return;
    setTimelineBusyMessage("Actualizando comentario...");
    vibrateTap(8);
    try {
      await updateComment(editingCommentId, text);
      setComments((prev) =>
        prev.map((entry) => (entry.id === editingCommentId ? { ...entry, text } : entry))
      );
      setEditingCommentId(null);
      setCommentDraft("");
      if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
      setCommentImageFile(null);
      setCommentImageName("");
      setCommentImagePreviewError(false);
      setCommentImagePreviewUrl("");
      setTimelineActionBanner("Comentario actualizado");
    } catch (error) {
      setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleDeleteComment = async (commentId: number) => {
    setTimelineBusyMessage("Eliminando comentario...");
    vibrateTap(8);
    try {
      await deleteComment(commentId);
      setComments((prev) => prev.filter((entry) => entry.id !== commentId));
      setCommentMenuId(null);
      if (editingCommentId === commentId) {
        setEditingCommentId(null);
        setCommentDraft("");
      }
      setTimelineActionBanner("Comentario eliminado");
    } catch (error) {
      setGlobalStatus(`Error comentario: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleToggleFollow = async (targetUserId: number) => {
    if (!activeUser || targetUserId === activeUser.id) return;
    const alreadyFollowing = follows.some(
      (follow) => follow.follower_id === activeUser.id && follow.followed_id === targetUserId
    );
    setTimelineBusyMessage(alreadyFollowing ? "Actualizando seguimiento..." : "Siguiendo usuario...");
    vibrateTap(8);
    try {
      const payload = await toggleFollow(activeUser.id, targetUserId, alreadyFollowing);
      if (!payload) {
        setFollows((prev) =>
          prev.filter(
            (follow) => !(follow.follower_id === activeUser.id && follow.followed_id === targetUserId)
          )
        );
        setTimelineActionBanner("Dejaste de seguir");
        setTimelineBusyMessage(null);
        return;
      }
      setFollows((prev) => [...prev, payload]);
      setTimelineActionBanner("Ahora sigues a este usuario");
    } catch (error) {
      setGlobalStatus(`Error follow: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleEditPost = async (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => {
    setTimelineBusyMessage("Actualizando publicacion...");
    vibrateTap(8);
    try {
      const imageToPersist = imageFile ? await uploadImageFile("posts", imageFile) : newImageUrl;
      await updatePost(postId, newText, imageToPersist);
      setPosts((prev) =>
        prev.map((post) =>
          post.id === postId ? { ...post, comment: newText, image_url: imageToPersist } : post
        )
      );
      setTimelineActionBanner("Publicacion actualizada");
    } catch (error) {
      setGlobalStatus(`Error editar post: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };

  const handleDeletePost = async (postId: string) => {
    setTimelineBusyMessage("Eliminando publicacion...");
    vibrateTap(10);
    try {
      await deletePost(postId);
      setPosts((prev) => prev.filter((post) => post.id !== postId));
      setLikes((prev) => prev.filter((like) => like.post_id !== postId));
      setComments((prev) => prev.filter((comment) => comment.post_id !== postId));
      setPostCoffeeTags((prev) => prev.filter((tag) => tag.post_id !== postId));
      setTimelineActionBanner("Publicacion eliminada");
    } catch (error) {
      setGlobalStatus(`Error borrar post: ${(error as Error).message}`);
      setTimelineBusyMessage(null);
      return;
    }
    setTimelineBusyMessage(null);
  };
  const handleUpdateProfile = async (
    userId: number,
    fullName: string,
    bio: string,
    avatarFile?: File | null,
    removeAvatar?: boolean
  ) => {
    const trimmedName = fullName.trim();
    const normalizedBio = bio.trim();
    if (!trimmedName) return;
    setTimelineBusyMessage("Actualizando perfil...");
    try {
      const avatarUrl = removeAvatar ? null : avatarFile ? await uploadImageFile("avatars", avatarFile) : undefined;
      await updateUserProfile(userId, {
        full_name: trimmedName,
        bio: normalizedBio ? normalizedBio : null,
        avatar_url: avatarUrl
      });
      setUsers((prev) =>
        prev.map((row) =>
          row.id === userId
            ? {
                ...row,
                full_name: trimmedName,
                bio: normalizedBio ? normalizedBio : null,
                avatar_url: avatarUrl === undefined ? row.avatar_url : avatarUrl ?? ""
              }
            : row
        )
      );
      setTimelineActionBanner("Perfil actualizado");
    } catch (error) {
      setGlobalStatus(`Error actualizando perfil: ${(error as Error).message}`);
    } finally {
      setTimelineBusyMessage(null);
    }
  };

  const handleRefreshTimeline = async () => {
    setTimelineRefreshing(true);
    setTimelineBusyMessage("Actualizando timeline...");
    await loadInitialData();
    setTimelineRefreshing(false);
    setTimelineBusyMessage(null);
    setTimelineActionBanner("Timeline actualizado");
  };

  const handleCreatePost = async () => {
    if (!activeUser) return;
    const text = newPostText.trim();
    if (!text && !newPostImageFile) return;

    setTimelineBusyMessage("Publicando tu contenido...");
    vibrateTap(10);
    try {
      const imageUrl = newPostImageFile ? await uploadImageFile("posts", newPostImageFile) : "";
      const post = await createPost(activeUser.id, text, imageUrl);
      setPosts((prev) => [post, ...prev]);

      if (newPostCoffeeId) {
        const selectedCoffee = coffees.find((coffee) => coffee.id === newPostCoffeeId);
        if (selectedCoffee) {
          const tag: PostCoffeeTagRow = {
            post_id: post.id,
            coffee_id: selectedCoffee.id,
            coffee_name: selectedCoffee.nombre,
            coffee_brand: selectedCoffee.marca ?? ""
          };
          await addPostCoffeeTag(tag);
          setPostCoffeeTags((prev) => [tag, ...prev]);
        }
      }

      resetCreatePostComposer();
      setTimelineActionBanner("Publicacion creada");
    } catch (error) {
      setGlobalStatus(`Error publicar: ${(error as Error).message}`);
      return;
    } finally {
      setTimelineBusyMessage(null);
    }
  };

  const handleMentionNavigation = (username: string) => {
    if (!username) return;
    navigateToTab("search", { searchMode: "users" });
    setSearchQuery(username);
    window.requestAnimationFrame(() => {
      document.getElementById("quick-search")?.focus();
    });
  };

  useEffect(() => {
    const onVisible = () => {
      if (document.visibilityState !== "visible") return;
      if (activeTab !== "timeline") return;
      void handleRefreshTimeline();
    };
    window.addEventListener("focus", onVisible);
    document.addEventListener("visibilitychange", onVisible);
    return () => {
      window.removeEventListener("focus", onVisible);
      document.removeEventListener("visibilitychange", onVisible);
    };
  }, [activeTab]);

  const closeNotificationsPanel = () => {
    setNotificationsLastSeenAt(Date.now());
    setShowNotificationsPanel(false);
  };
  const dismissNotification = (id: string) => {
    setDismissingNotificationIds((prev) => {
      if (prev.has(id)) return prev;
      const next = new Set(prev);
      next.add(id);
      return next;
    });
    const timer = window.setTimeout(() => {
      setDismissedNotificationIds((prev) => {
        const next = new Set(prev);
        next.add(id);
        return next;
      });
      setDismissingNotificationIds((prev) => {
        const next = new Set(prev);
        next.delete(id);
        return next;
      });
      dismissNotificationTimersRef.current = dismissNotificationTimersRef.current.filter((value) => value !== timer);
    }, 220);
    dismissNotificationTimersRef.current.push(timer);
  };

  const openCoffeeDetail = useCallback(
    (coffeeId: string, sourceTab: "timeline" | "search" | "profile" | "diary") => {
      const coffee = coffeesById.get(coffeeId);
      if (!coffee) return;
      const slug = coffeeSlugIndex.byId.get(coffeeId) ?? toCoffeeSlug(coffee.nombre);
      setDetailCoffeeId(coffeeId);
      if (sourceTab === "diary") {
        setDetailHostTab(null);
        setActiveTab("coffee");
      } else if (mode === "desktop") {
        setDetailHostTab(sourceTab);
      } else {
        setDetailHostTab(null);
        setActiveTab("coffee");
      }
      const nextPath = buildRoute("coffee", searchMode, profileUsername, slug);
      if (window.location.pathname !== nextPath) {
        window.history.pushState({}, "", `${nextPath}${window.location.search}${window.location.hash}`);
      }
    },
    [coffeeSlugIndex.byId, coffeesById, mode, profileUsername, searchMode]
  );

  const closeCoffeePanel = useCallback(() => {
    setDetailCoffeeId(null);
    setDetailHostTab(null);
    if (mode === "desktop") {
      const backPath = buildRoute(activeTab, searchMode, profileUsername, null);
      if (window.location.pathname !== backPath) {
        window.history.replaceState({}, "", `${backPath}${window.location.search}${window.location.hash}`);
      }
    }
  }, [activeTab, mode, profileUsername, searchMode]);

  const saveDetailFavorite = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const next = !detailIsFavorite;
    const result = await toggleFavoriteCoffee(activeUser.id, detailCoffeeId, detailIsFavorite);
    setFavorites((prev) => {
      if (next && result) return [result, ...prev.filter((item) => !(item.user_id === result.user_id && item.coffee_id === result.coffee_id))];
      return prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === detailCoffeeId));
    });
  }, [activeUser, detailCoffeeId, detailIsFavorite]);

  const saveDetailReview = useCallback(async () => {
    if (!activeUser || !detailCoffeeId || detailReviewRating <= 0) return;
    let imageUrl = detailCurrentUserReview?.image_url ?? "";
    if (detailReviewImageFile) {
      imageUrl = await uploadImageFile("reviews", detailReviewImageFile);
    }
    const row = await upsertCoffeeReview({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      rating: detailReviewRating,
      comment: detailReviewText.trim(),
      imageUrl
    });
    setCoffeeReviews((prev) => [
      row,
      ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))
    ]);
  }, [activeUser, detailCoffeeId, detailCurrentUserReview?.image_url, detailReviewImageFile, detailReviewRating, detailReviewText]);

  const removeDetailReview = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    await deleteCoffeeReview(detailCoffeeId, activeUser.id);
    setCoffeeReviews((prev) => prev.filter((item) => !(item.coffee_id === detailCoffeeId && item.user_id === activeUser.id)));
  }, [activeUser, detailCoffeeId]);

  const saveDetailSensory = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const row = await upsertCoffeeSensoryProfile({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      aroma: detailSensoryDraft.aroma,
      sabor: detailSensoryDraft.sabor,
      cuerpo: detailSensoryDraft.cuerpo,
      acidez: detailSensoryDraft.acidez,
      dulzura: detailSensoryDraft.dulzura
    });
    setCoffeeSensoryProfiles((prev) => [
      row,
      ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))
    ]);
  }, [activeUser, detailCoffeeId, detailSensoryDraft]);

  const saveDetailStock = useCallback(async () => {
    if (!activeUser || !detailCoffeeId) return;
    const total = Math.max(0, Number.isFinite(detailStockDraft.total) ? detailStockDraft.total : 0);
    const remaining = Math.min(total, Math.max(0, Number.isFinite(detailStockDraft.remaining) ? detailStockDraft.remaining : 0));
    const row = await upsertPantryStock({
      coffeeId: detailCoffeeId,
      userId: activeUser.id,
      totalGrams: total,
      gramsRemaining: remaining
    });
    setPantryItems((prev) => [
      row,
      ...prev.filter((item) => !(item.coffee_id === row.coffee_id && item.user_id === row.user_id))
    ]);
    setDetailStockDraft({
      total,
      remaining
    });
  }, [activeUser, detailCoffeeId, detailStockDraft]);

  const closeCreateCoffeeComposer = useCallback(() => {
    if (createCoffeeSaving) return;
    setShowCreateCoffeeComposer(false);
    setCreateCoffeeError(null);
  }, [createCoffeeSaving]);

  const saveCreateCoffee = useCallback(async () => {
    if (!activeUser) return;
    if (!createCoffeeDraft.name.trim() || !createCoffeeDraft.brand.trim()) {
      setCreateCoffeeError("Nombre y marca son obligatorios.");
      return;
    }
    if (!createCoffeeDraft.specialty.trim() || !createCoffeeDraft.country.trim() || !createCoffeeDraft.format.trim()) {
      setCreateCoffeeError("Completa especialidad, país y formato.");
      return;
    }
    setCreateCoffeeSaving(true);
    setCreateCoffeeError(null);
    try {
      let imageUrl = "";
      if (createCoffeeImageFile) {
        imageUrl = await uploadImageFile("posts", createCoffeeImageFile);
      }
      const created = await upsertCustomCoffee({
        userId: activeUser.id,
        name: createCoffeeDraft.name,
        brand: createCoffeeDraft.brand,
        specialty: createCoffeeDraft.specialty,
        country: createCoffeeDraft.country,
        format: createCoffeeDraft.format,
        roast: createCoffeeDraft.roast || null,
        variety: createCoffeeDraft.variety || null,
        hasCaffeine: createCoffeeDraft.hasCaffeine,
        imageUrl,
        totalGrams: createCoffeeDraft.totalGrams
      });

      const pantryRow = await upsertPantryStock({
        coffeeId: created.id,
        userId: activeUser.id,
        totalGrams: Math.max(1, createCoffeeDraft.totalGrams || 250),
        gramsRemaining: Math.max(1, createCoffeeDraft.totalGrams || 250)
      });

      setCustomCoffees((prev) => [created, ...prev.filter((item) => item.id !== created.id)]);
      setPantryItems((prev) => [pantryRow, ...prev.filter((item) => !(item.coffee_id === pantryRow.coffee_id && item.user_id === pantryRow.user_id))]);
      setBrewCoffeeId(created.id);
      setBrewStep("config");
      setShowCreateCoffeeComposer(false);
      setCreateCoffeeDraft({
        name: "",
        brand: "",
        specialty: "",
        country: "",
        format: "",
        roast: "",
        variety: "",
        hasCaffeine: true,
        totalGrams: 250
      });
      if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
      setCreateCoffeeImageFile(null);
      setCreateCoffeeImagePreviewUrl("");
    } catch (error) {
      setCreateCoffeeError((error as Error).message);
    } finally {
      setCreateCoffeeSaving(false);
    }
  }, [activeUser, createCoffeeDraft, createCoffeeImageFile, createCoffeeImagePreviewUrl]);

  const detailPanel = detailCoffee ? (
    <CoffeeDetailView
      coffee={detailCoffee}
      reviews={detailCoffeeReviews}
      currentUser={detailCurrentUser}
      currentUserReview={detailCurrentUserReviewWithUser}
      avgRating={detailCoffeeAverageRating}
      isFavorite={detailIsFavorite}
      pantry={detailPantryStock}
      sensory={detailSensoryAverages}
      sensoryDraft={detailSensoryDraft}
      stockDraft={detailStockDraft}
      reviewDraftText={detailReviewText}
      reviewDraftRating={detailReviewRating}
      reviewDraftImagePreviewUrl={detailReviewImagePreviewUrl}
      onClose={closeCoffeePanel}
      onToggleFavorite={() => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        void saveDetailFavorite();
      }}
      onReviewTextChange={setDetailReviewText}
      onReviewRatingChange={setDetailReviewRating}
      onReviewImagePick={(file, previewUrl) => {
        setDetailReviewImageFile(file);
        setDetailReviewImagePreviewUrl(previewUrl);
      }}
      onSaveReview={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await saveDetailReview();
      }}
      onDeleteReview={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await removeDetailReview();
      }}
      canDeleteReview={Boolean(detailCurrentUserReview)}
      onSensoryDraftChange={setDetailSensoryDraft}
      onSaveSensory={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await saveDetailSensory();
      }}
      onStockDraftChange={setDetailStockDraft}
      onSaveStock={async () => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        await saveDetailStock();
      }}
      onOpenUserProfile={(userId) => {
        if (!sessionEmail) {
          requestLogin();
          return;
        }
        navigateToTab("profile", { profileUserId: userId });
      }}
      isGuest={!sessionEmail}
      onRequireAuth={requestLogin}
      fullPage={false}
      externalOpenStockSignal={0}
    />
  ) : null;

  const createCoffeePanel = (
    <CreateCoffeeView
      draft={createCoffeeDraft}
      imagePreviewUrl={createCoffeeImagePreviewUrl}
      saving={createCoffeeSaving}
      error={createCoffeeError}
      countryOptions={searchOriginOptions}
      specialtyOptions={searchSpecialtyOptions}
      onChange={setCreateCoffeeDraft}
      onPickImage={(file, previewUrl) => {
        if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
        setCreateCoffeeImageFile(file);
        setCreateCoffeeImagePreviewUrl(previewUrl);
      }}
      onRemoveImage={() => {
        if (createCoffeeImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(createCoffeeImagePreviewUrl);
        setCreateCoffeeImageFile(null);
        setCreateCoffeeImagePreviewUrl("");
      }}
      onClose={closeCreateCoffeeComposer}
      onSave={() => void saveCreateCoffee()}
      fullPage={mode !== "desktop"}
    />
  );

  useEffect(() => {
    const isCoffeeRoute = window.location.pathname.startsWith("/coffee/");
    const siteUrl = (import.meta.env.VITE_SITE_URL as string | undefined) ?? window.location.origin;
    const canonicalHref = `${siteUrl}${window.location.pathname}`;

    let canonical = document.querySelector("link[rel='canonical']") as HTMLLinkElement | null;
    if (!canonical) {
      canonical = document.createElement("link");
      canonical.rel = "canonical";
      document.head.appendChild(canonical);
    }
    canonical.href = canonicalHref;

    let descriptionMeta = document.querySelector("meta[name='description']") as HTMLMetaElement | null;
    if (!descriptionMeta) {
      descriptionMeta = document.createElement("meta");
      descriptionMeta.name = "description";
      document.head.appendChild(descriptionMeta);
    }

    if (!isCoffeeRoute) {
      document.title = "Cafesito Web";
      descriptionMeta.content = "Comunidad de cafe para compartir timeline, explorar cafes y seguir perfiles.";
      return;
    }
    const title = detailCoffee ? `${detailCoffee.nombre} | Cafesito` : "Café | Cafesito";
    const description = detailCoffee
      ? (detailCoffee.descripcion?.trim() || `${detailCoffee.nombre} ${detailCoffee.marca ?? ""}`.trim() || "Detalle de café en Cafesito")
      : "Detalle de café en Cafesito";
    document.title = title;
    descriptionMeta.content = description.slice(0, 160);

    let ogTitle = document.querySelector("meta[property='og:title']") as HTMLMetaElement | null;
    if (!ogTitle) {
      ogTitle = document.createElement("meta");
      ogTitle.setAttribute("property", "og:title");
      document.head.appendChild(ogTitle);
    }
    ogTitle.content = title;

    let ogType = document.querySelector("meta[property='og:type']") as HTMLMetaElement | null;
    if (!ogType) {
      ogType = document.createElement("meta");
      ogType.setAttribute("property", "og:type");
      document.head.appendChild(ogType);
    }
    ogType.content = "product";
  }, [detailCoffee]);

  const handleNavClick = (tabId: TabId) => {
    if (!sessionEmail) {
      requestLogin();
      return;
    }
    setDetailCoffeeId(null);
    setDetailHostTab(null);
    setShowCreateCoffeeComposer(false);
    if (tabId === "search") {
      setSearchSelectedCoffeeId(null);
      setSearchFocusCoffeeProfile(false);
      navigateToTab("search", { searchMode: "coffees" });
      return;
    }
    if (tabId === "profile") {
      navigateToTab("profile", { profileUsername: activeUser?.username ?? null });
      return;
    }
    navigateToTab(tabId);
  };

  const nav = (
    <nav aria-label="Navegacion principal" className="nav nav-mobile">
      {NAV_ITEMS.map((item) => (
        (() => {
          const isActive = activeTab === item.id;
          return (
        <button
          key={item.id}
          type="button"
          className={`nav-item ${isActive ? "is-active" : ""}`}
          onClick={() => handleNavClick(item.id)}
          aria-current={isActive ? "page" : undefined}
        >
          <span className="nav-glyph" aria-hidden="true">
            <UiIcon name={isActive ? item.activeIcon : item.icon} className="ui-icon" />
          </span>
          <span className="nav-label">{item.label}</span>
        </button>
          );
        })()
      ))}
    </nav>
  );

  const navRail = (
    <aside className="nav-rail" aria-label="Navegacion principal">
      <nav className="nav nav-desktop">
        {NAV_ITEMS.map((item) => (
          (() => {
            const isActive = activeTab === item.id;
            return (
          <button
            key={item.id}
            type="button"
            className={`nav-item ${isActive ? "is-active" : ""}`}
            onClick={() => handleNavClick(item.id)}
            aria-current={isActive ? "page" : undefined}
            aria-label={item.label}
            title={item.label}
          >
            <span className="nav-glyph" aria-hidden="true">
              <UiIcon name={isActive ? item.activeIcon : item.icon} className="ui-icon" />
            </span>
          </button>
            );
          })()
        ))}
      </nav>
    </aside>
  );

  if (!authReady) {
    return <LoginGate loading message="Verificando sesion..." />;
  }

  if (!sessionEmail && activeTab !== "coffee") {
    return (
      <LoginGate
        loading={authBusy}
        message={authBusy ? "Redirigiendo a Google..." : undefined}
        errorMessage={authError}
        onGoogleLogin={handleGoogleLogin}
      />
    );
  }

  const isWideDesktop = mode === "desktop" && viewportWidth >= 1520;
  const useRightRailDetail = Boolean(
    isWideDesktop &&
      detailPanel &&
      ((activeTab === "timeline" && detailHostTab === "timeline") ||
        (activeTab === "search" && detailHostTab === "search") ||
        (activeTab === "profile" && detailHostTab === "profile"))
  );
  const isDesktopComposer = mode === "desktop";

  return (
    <div className={`layout ${mode}`.trim()}>
      {mode === "desktop" ? navRail : null}
      <main className="main-shell">
        <TopBar
          activeTab={activeTab}
          searchQuery={searchQuery}
          searchMode={searchMode}
          onSearchQueryChange={(value) => {
            setSearchQuery(value);
            if (searchFocusCoffeeProfile) setSearchFocusCoffeeProfile(false);
          }}
          onSearchCancel={() => {
            setSearchQuery("");
            setSearchFocusCoffeeProfile(false);
          }}
          onSearchBarcodeClick={() => {
            if (!isMobileOsDevice) return;
            setShowBarcodeScannerSheet(true);
          }}
          showSearchBarcodeButton={isMobileOsDevice && searchMode === "coffees"}
          showSearchCoffeeFilterChips={searchMode === "coffees" && !searchFocusCoffeeProfile}
          searchOriginCount={searchSelectedOrigins.size}
          searchSpecialtyCount={searchSelectedSpecialties.size}
          searchRoastCount={searchSelectedRoasts.size}
          searchFormatCount={searchSelectedFormats.size}
          searchHasRatingFilter={searchMinRating > 0}
          onOpenSearchFilter={(filter) => setSearchActiveFilterType(filter)}
          onSearchBack={() => {
            if (searchMode === "users") {
              setSearchQuery("");
              setSearchFocusCoffeeProfile(false);
              navigateToTab("timeline");
              return;
            }
            setSearchQuery("");
          }}
          showNotificationsBadge={showNotificationsBadge}
          onTimelineSearchUsers={() => {
            setSearchQuery("");
            setSearchFocusCoffeeProfile(false);
            navigateToTab("search", { searchMode: "users" });
          }}
          onTimelineNotifications={() => {
            setShowNotificationsPanel(true);
            const latestSeen = visibleTimelineNotifications.reduce((max, item) => Math.max(max, item.timestamp), 0);
            setNotificationsLastSeenAt((prev) => Math.max(prev, latestSeen));
          }}
          diaryPeriod={diaryPeriod}
          onDiaryOpenQuickActions={() => setShowDiaryQuickActions(true)}
          onDiaryOpenPeriodSelector={() => setShowDiaryPeriodSheet(true)}
          scrolled={topbarScrolled}
          brewStep={brewStep}
          onBrewBack={() => {
            if (showCreateCoffeeComposer) {
              closeCreateCoffeeComposer();
              return;
            }
            if (brewStep === "coffee") setBrewStep("method");
            else if (brewStep === "config") setBrewStep("coffee");
            else if (brewStep === "brewing") setBrewStep("config");
            else if (brewStep === "result") setBrewStep("method");
          }}
          onBrewForward={() => {
            if (brewStep === "config") {
              setTimerSeconds(0);
              setBrewRunning(true);
              setBrewStep("brewing");
            }
          }}
          brewCreateCoffeeOpen={showCreateCoffeeComposer}
          onBrewCreateCoffeeBack={closeCreateCoffeeComposer}
          onProfileSignOut={handleSignOut}
          profileMenuEnabled={Boolean(profileUser && activeUser && profileUser.id === activeUser.id)}
          onProfileOpenEdit={() => setProfileEditSignal((prev) => prev + 1)}
          onCoffeeBack={() => {
            if (!sessionEmail) {
              requestLogin();
              return;
            }
            if (window.history.length > 1) {
              window.history.back();
              return;
            }
            navigateToTab("timeline", { replace: true });
          }}
          coffeeTopbarFavoriteActive={activeTab === "coffee" ? detailIsFavorite : false}
          coffeeTopbarStockActive={activeTab === "coffee" ? Boolean(detailPantryStock) : false}
          onCoffeeTopbarToggleFavorite={() => {
            if (!sessionEmail) {
              requestLogin();
              return;
            }
            void saveDetailFavorite();
          }}
          onCoffeeTopbarOpenStock={() => {
            if (!sessionEmail) {
              requestLogin();
              return;
            }
            setDetailOpenStockSignal((prev) => prev + 1);
          }}
        />

        <section aria-live="polite" className="content">
          {activeTab === "timeline" ? (
            <TimelineView
              mode={mode}
              cards={timelineCards}
              recommendations={timelineRecommendations}
              suggestions={timelineSuggestions}
              suggestionIndices={timelineSuggestionIndices}
              followingIds={followingIds}
              followerCounts={followerCounts}
              loading={globalStatus === "Cargando datos..."}
              errorMessage={globalStatus.startsWith("Error") ? globalStatus : null}
              refreshing={timelineRefreshing}
              activeUserId={activeUser?.id ?? null}
              onOpenComments={(postId) => {
                setHighlightedCommentId(null);
                setCommentSheetPostId(postId);
              }}
              onToggleLike={handleToggleLike}
              onToggleFollow={handleToggleFollow}
              onEditPost={handleEditPost}
              onDeletePost={handleDeletePost}
              onRefresh={handleRefreshTimeline}
              onMentionClick={handleMentionNavigation}
              onOpenUserProfile={(userId) => {
                navigateToTab("profile", { profileUserId: userId });
              }}
              onOpenCoffee={(coffeeId) => {
                openCoffeeDetail(coffeeId, "timeline");
              }}
              sidePanel={mode === "desktop" && !useRightRailDetail && detailHostTab === "timeline" ? detailPanel : null}
            />
          ) : null}
          {activeTab === "search" ? (
            <SearchView
              mode={searchMode}
              searchQuery={searchQuery}
              onSearchQueryChange={(value) => {
                setSearchQuery(value);
                if (searchFocusCoffeeProfile) setSearchFocusCoffeeProfile(false);
              }}
              searchFilter={searchFilter}
              onSearchFilterChange={(value) => {
                setSearchFilter(value);
                if (searchFocusCoffeeProfile) setSearchFocusCoffeeProfile(false);
              }}
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
              onToggleOrigin={(value) =>
                setSearchSelectedOrigins((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onToggleRoast={(value) =>
                setSearchSelectedRoasts((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onToggleSpecialty={(value) =>
                setSearchSelectedSpecialties((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onToggleFormat={(value) =>
                setSearchSelectedFormats((prev) => {
                  const next = new Set(prev);
                  if (next.has(value)) next.delete(value);
                  else next.add(value);
                  return next;
                })
              }
              onSetMinRating={setSearchMinRating}
              onClearCoffeeFilters={() => {
                setSearchSelectedOrigins(new Set());
                setSearchSelectedRoasts(new Set());
                setSearchSelectedSpecialties(new Set());
                setSearchSelectedFormats(new Set());
                setSearchMinRating(0);
              }}
              coffees={filteredCoffees}
              users={filteredSearchUsers}
              followingIds={followingIds}
              selectedCoffee={coffees.find((item) => item.id === searchSelectedCoffeeId) ?? null}
              onSelectCoffee={(coffeeId) => {
                setSearchSelectedCoffeeId(coffeeId);
                setSearchFocusCoffeeProfile(false);
                openCoffeeDetail(coffeeId, "search");
              }}
              onSelectUser={(userId) => {
                navigateToTab("profile", { profileUserId: userId });
              }}
              onToggleFollow={handleToggleFollow}
              focusCoffeeProfile={searchFocusCoffeeProfile}
              onExitCoffeeFocus={() => setSearchFocusCoffeeProfile(false)}
              sidePanel={mode === "desktop" && !useRightRailDetail && detailHostTab === "search" ? detailPanel : null}
            />
          ) : null}
          {activeTab === "coffee" ? (
            detailCoffee ? (
              <CoffeeDetailView
                coffee={detailCoffee}
                reviews={detailCoffeeReviews}
                currentUser={detailCurrentUser}
                currentUserReview={detailCurrentUserReviewWithUser}
                avgRating={detailCoffeeAverageRating}
                isFavorite={detailIsFavorite}
                pantry={detailPantryStock}
                sensory={detailSensoryAverages}
                sensoryDraft={detailSensoryDraft}
                stockDraft={detailStockDraft}
                reviewDraftText={detailReviewText}
                reviewDraftRating={detailReviewRating}
                reviewDraftImagePreviewUrl={detailReviewImagePreviewUrl}
                onClose={() => {
                  if (window.history.length > 1) window.history.back();
                  else navigateToTab("timeline", { replace: true });
                }}
                onToggleFavorite={() => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  void saveDetailFavorite();
                }}
                onReviewTextChange={setDetailReviewText}
                onReviewRatingChange={setDetailReviewRating}
                onReviewImagePick={(file, previewUrl) => {
                  setDetailReviewImageFile(file);
                  setDetailReviewImagePreviewUrl(previewUrl);
                }}
                onSaveReview={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await saveDetailReview();
                }}
                onDeleteReview={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await removeDetailReview();
                }}
                canDeleteReview={Boolean(detailCurrentUserReview)}
                onSensoryDraftChange={setDetailSensoryDraft}
                onSaveSensory={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await saveDetailSensory();
                }}
                onStockDraftChange={setDetailStockDraft}
                onSaveStock={async () => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  await saveDetailStock();
                }}
                onOpenUserProfile={(userId) => {
                  if (!sessionEmail) {
                    requestLogin();
                    return;
                  }
                  navigateToTab("profile", { profileUserId: userId });
                }}
                isGuest={!sessionEmail}
                onRequireAuth={requestLogin}
                fullPage
                externalOpenStockSignal={detailOpenStockSignal}
              />
            ) : (
              <article className="coffee-detail-empty coffee-detail-empty-full">
                <h2 className="title">Café no encontrado</h2>
                <p className="coffee-sub">La URL no corresponde a ningún café disponible.</p>
                <button
                  type="button"
                  className="action-button"
                  onClick={() => {
                    navigateToTab("search", { searchMode: "coffees", replace: true });
                  }}
                >
                  Volver a Explorar
                </button>
              </article>
            )
          ) : null}
          {activeTab === "brewlab" ? (
            showCreateCoffeeComposer && mode !== "desktop" ? (
              <section className="create-coffee-mobile-screen">{createCoffeePanel}</section>
            ) : (
              <BrewLabView
                brewStep={brewStep}
                setBrewStep={setBrewStep}
                brewMethod={brewMethod}
                setBrewMethod={setBrewMethod}
                brewCoffeeId={brewCoffeeId}
                setBrewCoffeeId={setBrewCoffeeId}
                coffees={brewCoffeeCatalog}
                pantryItems={brewPantryItems}
                onAddNotFoundCoffee={() => {
                  setCreateCoffeeError(null);
                  setShowCreateCoffeeComposer(true);
                }}
                waterMl={waterMl}
                setWaterMl={setWaterMl}
                ratio={ratio}
                setRatio={setRatio}
                timerSeconds={timerSeconds}
                setTimerSeconds={setTimerSeconds}
                brewRunning={brewRunning}
                setBrewRunning={setBrewRunning}
                selectedCoffee={selectedCoffee}
                coffeeGrams={coffeeGrams}
                onSaveResultToDiary={saveBrewToDiary}
              />
            )
          ) : null}
          {activeTab === "diary" ? (
            <DiaryView
              mode={mode}
              tab={diaryTab}
              setTab={setDiaryTab}
              period={diaryPeriod}
              entries={diaryEntriesActivity}
              coffeeCatalog={brewCoffeeCatalog}
              pantryRows={pantryCoffeeRows}
              onDeleteEntry={handleDeleteDiaryEntry}
              onEditEntry={handleUpdateDiaryEntry}
              onUpdatePantryStock={handleUpdatePantryStock}
              onRemovePantryItem={handleRemovePantryItem}
              onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "diary")}
            />
          ) : null}
          {activeTab === "profile" && profileUser ? (
            <ProfileView
              user={profileUser}
              mode={mode}
              tab={profileTab}
              setTab={setProfileTab}
              posts={profilePosts}
              favoriteCoffees={profileUser.id === activeUser?.id ? favoriteCoffees : []}
              allCoffees={coffees}
              coffeeReviews={coffeeReviews}
              coffeeSensoryProfiles={coffeeSensoryProfiles}
              followers={followersCount}
              following={followingCount}
              onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "profile")}
              onOpenUserProfile={(userId) => navigateToTab("profile", { profileUserId: userId })}
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
                if (!exists) return;
                await toggleFavoriteCoffee(activeUser.id, coffeeId, true);
                setFavorites((prev) => prev.filter((item) => !(item.user_id === activeUser.id && item.coffee_id === coffeeId)));
              }}
              onEditPost={handleEditPost}
              onDeletePost={handleDeletePost}
              onToggleLike={handleToggleLike}
              onOpenComments={(postId) => {
                setHighlightedCommentId(null);
                setCommentSheetPostId(postId);
              }}
              externalEditProfileSignal={profileEditSignal}
              sidePanel={mode === "desktop" && !useRightRailDetail && detailHostTab === "profile" ? detailPanel : null}
            />
          ) : null}
        </section>

        {activeTab === "timeline" && mode !== "desktop" ? (
          <button className="fab" type="button" aria-label="Nuevo Post" onClick={openCreatePostComposer}>
            <UiIcon name="add" className="ui-icon" />
          </button>
        ) : null}
      </main>

      {mode === "desktop" ? (
        <aside
          className={useRightRailDetail || (activeTab === "brewlab" && showCreateCoffeeComposer) ? "detail-rail-fixed" : "fab-rail"}
          aria-label={useRightRailDetail ? "Detalle cafe" : activeTab === "brewlab" && showCreateCoffeeComposer ? "Crear cafe" : "Acciones"}
        >
          {useRightRailDetail ? (
            <div className="desktop-detail-wrap">{detailPanel}</div>
          ) : activeTab === "brewlab" && showCreateCoffeeComposer ? (
            <div className="desktop-detail-wrap">{createCoffeePanel}</div>
          ) : activeTab === "timeline" ? (
            <button className="fab fab-desktop" type="button" aria-label="Nuevo Post" onClick={openCreatePostComposer}>
              <UiIcon name="add" className="ui-icon" />
            </button>
          ) : null}
        </aside>
      ) : null}

      {mode === "mobile" && !(activeTab === "brewlab" && showCreateCoffeeComposer) ? <footer className="bottom-tabs">{nav}</footer> : null}

      {showAuthPrompt ? (
        <div className="auth-prompt-overlay" role="dialog" aria-modal="true" aria-label="Acceso" onClick={() => setShowAuthPrompt(false)}>
          <div className="auth-prompt-card" onClick={(event) => event.stopPropagation()}>
            <button type="button" className="auth-prompt-close" aria-label="Cerrar" onClick={() => setShowAuthPrompt(false)}>
              <UiIcon name="close" className="ui-icon" />
            </button>
            <div className="auth-prompt-avatar" aria-hidden="true">
              <img src="/logo.png" alt="" loading="lazy" />
            </div>
            <p className="auth-prompt-copy">
              Únete a la comunidad del cafe para descrubir, elaborar y compartir tu pasión.
            </p>
            <button
              type="button"
              className="action-button auth-prompt-primary"
              disabled={authBusy}
              onClick={() => {
                void handleGoogleLogin();
              }}
            >
              <span className="auth-prompt-google-g" aria-hidden="true">G</span>
              <span>{authBusy ? "Conectando..." : "Continuar con Google"}</span>
            </button>
            {authError ? <p className="auth-prompt-error">{authError}</p> : null}
          </div>
        </div>
      ) : null}

      <MobileBarcodeScannerSheet
        open={showBarcodeScannerSheet}
        onClose={() => setShowBarcodeScannerSheet(false)}
        onDetected={(value) => {
          setSearchQuery(value);
          setSearchFocusCoffeeProfile(false);
          setShowBarcodeScannerSheet(false);
        }}
      />

      {commentSheetPostId ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Comentarios"
          onClick={() => {
            setCommentSheetPostId(null);
            setCommentDraft("");
            setCommentMenuId(null);
            setHighlightedCommentId(null);
            if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
            setCommentImageFile(null);
            setCommentImageName("");
            setCommentImagePreviewError(false);
            setCommentImagePreviewUrl("");
          }}
        >
          <div className="sheet-card" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">COMENTARIOS</strong>
            </header>
            <ul className="sheet-list comments-list" ref={commentListRef}>
              {commentSheetRows.length
                ? commentSheetRows.map((row) => {
                    const user = usersById.get(row.user_id);
                    const isOwnComment = activeUser?.id === row.user_id;
                    return (
                      <li
                        key={row.id}
                        data-comment-id={row.id}
                        className={`sheet-item ${highlightedCommentId === row.id ? "is-highlighted" : ""}`}
                      >
                        <div className="sheet-item-head">
                          {user?.avatar_url ? (
                            <img className="comment-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                          ) : (
                            <div className="comment-avatar comment-avatar-fallback" aria-hidden="true">
                              {(user?.username ?? "us").slice(0, 2).toUpperCase()}
                            </div>
                          )}
                          <div className="comment-copy">
                            <p className="comment-author">@{user?.username ?? `user${row.user_id}`}</p>
                          </div>
                          {isOwnComment ? (
                            <button
                              type="button"
                              className="icon-button post-menu-trigger"
                              onClick={() => setCommentMenuId(row.id)}
                            >
                              <UiIcon name="more" className="ui-icon" />
                            </button>
                          ) : null}
                        </div>
                        <p className="sheet-item-text">
                          <MentionText text={row.text} onMentionClick={handleMentionNavigation} />
                        </p>
                      </li>
                    );
                  })
                : <li className="sheet-item comments-empty">No hay comentarios todavia</li>}
            </ul>
            {activeCommentMenuRow ? (
              <div className="sheet-overlay comment-action-overlay" onClick={() => setCommentMenuId(null)}>
                <div className="sheet-card comment-action-sheet" onClick={(event) => event.stopPropagation()}>
                  <div className="sheet-handle" aria-hidden="true" />
                  <div className="comment-action-list">
                    <p className="comment-action-title">OPCIONES</p>
                    <button
                      type="button"
                      className="comment-action-button"
                      onClick={() => {
                        setEditingCommentId(activeCommentMenuRow.id);
                        setCommentDraft(activeCommentMenuRow.text);
                        setCommentMenuId(null);
                      }}
                    >
                      <UiIcon name="edit" className="ui-icon" />
                      <span>Editar</span>
                      <UiIcon name="chevron-right" className="ui-icon trailing" />
                    </button>
                    <button
                      type="button"
                      className="comment-action-button is-danger"
                      onClick={() => {
                        const confirmed = window.confirm("Borrar comentario?");
                        if (!confirmed) return;
                        setCommentMenuId(null);
                        void handleDeleteComment(activeCommentMenuRow.id);
                      }}
                    >
                      <UiIcon name="trash" className="ui-icon" />
                      <span>Borrar</span>
                      <UiIcon name="chevron-right" className="ui-icon trailing" />
                    </button>
                  </div>
                </div>
              </div>
            ) : null}
            {editingCommentId ? (
              <div className="edit-comment-banner">
                <span>Editando comentario</span>
                <button
                  type="button"
                  className="text-button"
                  onClick={() => {
                    setEditingCommentId(null);
                    setCommentDraft("");
                  }}
                >
                  Cancelar
                </button>
              </div>
            ) : null}
            <div className="sheet-composer">
              <div className="sheet-input-wrap">
                <input
                  ref={commentImageInputRef}
                  type="file"
                  accept="image/*"
                  className="file-input-hidden"
                  onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    if (!file) return;
                    if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
                    const preview = URL.createObjectURL(file);
                    setCommentImageFile(file);
                    setCommentImageName(file.name);
                    setCommentImagePreviewError(false);
                    setCommentImagePreviewUrl(preview);
                    event.currentTarget.value = "";
                  }}
                />
                <div className="sheet-input-shell">
                  {showCommentEmojiPanel ? (
                    <div className="comment-inline-panel emoji-panel">
                      {COMMENT_EMOJIS.map((emoji) => (
                        <button
                          key={emoji}
                          type="button"
                          className="emoji-chip"
                          onClick={() => setCommentDraft((prev) => `${prev}${emoji}`)}
                        >
                          {emoji}
                        </button>
                      ))}
                    </div>
                  ) : commentMentionSuggestions.length ? (
                    <div className="comment-inline-panel mention-suggestions">
                      {commentMentionSuggestions.map((user) => (
                        <button
                          key={user.id}
                          type="button"
                          className="mention-chip"
                          onClick={() => {
                            const parts = commentDraft.split(/\s+/);
                            parts[parts.length - 1] = `@${user.username}`;
                            setCommentDraft(`${parts.join(" ")} `);
                            setShowCommentEmojiPanel(false);
                          }}
                        >
                          {user.avatar_url ? (
                            <img className="mention-chip-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                          ) : (
                            <span className="mention-chip-fallback">{user.username.slice(0, 1).toUpperCase()}</span>
                          )}
                          <span>@{user.username}</span>
                        </button>
                      ))}
                    </div>
                  ) : null}
                  <textarea
                    className="search-wide sheet-input"
                    placeholder="Anade un comentario..."
                    value={commentDraft}
                    rows={2}
                    onChange={(event) => {
                      setCommentDraft(event.target.value);
                      if (event.target.value.endsWith("@")) setShowCommentEmojiPanel(false);
                    }}
                  />
                  <div className="sheet-composer-bottom">
                    <div className="sheet-composer-tools-inline">
                      <button
                        type="button"
                        className="icon-button"
                        onClick={() => commentImageInputRef.current?.click()}
                        aria-label="Agregar foto"
                      >
                        <UiIcon name="camera" className="ui-icon" />
                      </button>
                      <button type="button" className="icon-button" onClick={() => setCommentDraft((prev) => `${prev}@`)}>
                        <UiIcon name="at" className="ui-icon" />
                      </button>
                      <button
                        type="button"
                        className={`icon-button ${showCommentEmojiPanel ? "is-active" : ""}`}
                        onClick={() => setShowCommentEmojiPanel((prev) => !prev)}
                      >
                        <UiIcon name="smile" className="ui-icon" />
                      </button>
                    </div>
                    <button
                      type="button"
                      className="send-button"
                      onClick={editingCommentId ? handleUpdateComment : handleAddComment}
                    >
                      <UiIcon name="send" className="ui-icon" />
                      {editingCommentId ? "Guardar" : "Enviar"}
                    </button>
                  </div>
                  {commentImagePreviewUrl ? (
                    <div className="comment-image-thumb-wrap">
                      {commentImagePreviewError ? (
                        <div className="comment-image-thumb-fallback">{commentImageName || "Imagen seleccionada"}</div>
                      ) : (
                        <img
                          className="comment-image-thumb"
                          src={commentImagePreviewUrl}
                          alt="Miniatura comentario"
                          loading="lazy"
                          onError={() => setCommentImagePreviewError(true)}
                        />
                      )}
                      <button
                        type="button"
                        className="comment-image-remove"
                        onClick={() => {
                          if (commentImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(commentImagePreviewUrl);
                          setCommentImageFile(null);
                          setCommentImageName("");
                          setCommentImagePreviewError(false);
                          setCommentImagePreviewUrl("");
                        }}
                        aria-label="Quitar imagen"
                      >
                        x
                      </button>
                    </div>
                  ) : null}
                </div>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {showCreatePost ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Crear publicacion"
          onClick={resetCreatePostComposer}
        >
          <div className="sheet-card create-post-sheet" onClick={(event) => event.stopPropagation()}>
            <header className="topbar topbar-centered topbar-timeline create-post-header">
              <div className="topbar-slot">
                <button
                  type="button"
                  className="icon-button topbar-icon-button"
                  aria-label={newPostStep === 0 ? "Cerrar" : "Atras"}
                  onClick={() => {
                    if (newPostStep === 0) {
                      resetCreatePostComposer();
                      return;
                    }
                    setNewPostStep(0);
                  }}
                >
                  <UiIcon name={newPostStep === 0 ? "close" : "arrow-left"} className="ui-icon" />
                </button>
              </div>
              <h2 className="title title-upper topbar-title-center topbar-brand-title create-post-title">
                {newPostStep === 0 ? "NUEVO POST" : "DETALLES"}
              </h2>
              <div className="topbar-slot topbar-slot-end">
                {newPostStep === 0 ? (
                  <button
                    type="button"
                    className="icon-button topbar-icon-button"
                    aria-label="Siguiente"
                    onClick={() => setNewPostStep(1)}
                    disabled={!newPostImageFile}
                  >
                    <UiIcon name="arrow-right" className="ui-icon" />
                  </button>
                ) : (
                  <button
                    type="button"
                    className="text-button create-post-publish"
                    onClick={handleCreatePost}
                    disabled={!newPostText.trim() && !newPostImageFile}
                  >
                    PUBLICAR
                  </button>
                )}
              </div>
            </header>
            <div className={`create-post-body create-post-flow ${newPostStep === 0 ? "create-post-step-0" : "create-post-step-1"}`}>
              <input
                ref={newPostImageInputRef}
                type="file"
                accept="image/*"
                multiple
                className="file-input-hidden"
                onChange={(event) => {
                  const files = Array.from(event.target.files ?? []);
                  appendNewPostFiles(files);
                  event.currentTarget.value = "";
                }}
              />
              <input
                ref={newPostCameraInputRef}
                type="file"
                accept="image/*"
                capture="environment"
                className="file-input-hidden"
                onChange={(event) => {
                  const files = Array.from(event.target.files ?? []);
                  appendNewPostFiles(files);
                  event.currentTarget.value = "";
                }}
              />
              {newPostStep === 0 ? (
                <>
                  <button
                    type="button"
                    className="create-post-image-stage"
                    onClick={() => newPostImageInputRef.current?.click()}
                    aria-label="Seleccionar imagen"
                  >
                    {newPostImagePreviewUrl ? (
                      <img className="create-post-preview" src={newPostImagePreviewUrl} alt="Previsualizacion" loading="lazy" />
                    ) : (
                      <span className="create-post-image-placeholder">Selecciona una foto</span>
                    )}
                  </button>
                  {!isDesktopComposer ? (
                    <>
                      <div className="create-post-source-row">
                        <strong>Galeria</strong>
                        <div className="create-post-source-actions">
                          {newPostImagePreviewUrl ? (
                            <button
                              type="button"
                              className="text-button create-post-secondary"
                              onClick={() => {
                                if (!newPostSelectedImageId) return;
                                setNewPostGalleryItems((prev) => {
                                  const target = prev.find((item) => item.id === newPostSelectedImageId);
                                  if (target?.previewUrl.startsWith("blob:")) URL.revokeObjectURL(target.previewUrl);
                                  const next = prev.filter((item) => item.id !== newPostSelectedImageId);
                                  const replacement = next[0] ?? null;
                                  setNewPostSelectedImageId(replacement?.id ?? null);
                                  setNewPostImageFile(replacement?.file ?? null);
                                  setNewPostImagePreviewUrl(replacement?.previewUrl ?? "");
                                  return next;
                                });
                              }}
                            >
                              Quitar
                            </button>
                          ) : null}
                          <button
                            type="button"
                            className="icon-button topbar-icon-button create-post-camera"
                            onClick={() => newPostCameraInputRef.current?.click()}
                            aria-label="Abrir camara"
                          >
                            <UiIcon name="camera" className="ui-icon" />
                          </button>
                        </div>
                      </div>
                      <div className="create-post-gallery-grid" role="list" aria-label="Galeria">
                        {newPostGalleryItems.length ? (
                          newPostGalleryItems.map((item) => {
                            const isSelected = item.id === newPostSelectedImageId;
                            return (
                              <button
                                key={item.id}
                                type="button"
                                role="listitem"
                                className={`create-post-gallery-item ${isSelected ? "is-selected" : ""}`}
                                onClick={() => {
                                  setNewPostSelectedImageId(item.id);
                                  setNewPostImageFile(item.file);
                                  setNewPostImagePreviewUrl(item.previewUrl);
                                }}
                              >
                                <img src={item.previewUrl} alt="Miniatura galeria" loading="lazy" />
                                {isSelected ? <span className="create-post-gallery-check material-symbol-icon is-filled" aria-hidden="true">check_circle</span> : null}
                              </button>
                            );
                          })
                        ) : (
                          <div className="create-post-gallery-empty" role="listitem">No hay imagenes para mostrar</div>
                        )}
                      </div>
                    </>
                  ) : null}
                </>
              ) : (
                <>
                  {activeUser ? (
                    <div className="create-post-user-row">
                      {activeUser.avatar_url ? (
                        <img src={activeUser.avatar_url} alt={activeUser.username} loading="lazy" />
                      ) : (
                        <div className="create-post-user-fallback">{activeUser.username.slice(0, 1).toUpperCase()}</div>
                      )}
                      <div>
                        <p>{activeUser.full_name}</p>
                        <span>@{activeUser.username}</span>
                      </div>
                    </div>
                  ) : null}
                  <div className="create-post-composer-card sheet-input-shell">
                    <textarea
                      id="new-post-text"
                      className="search-wide sheet-input create-post-textarea"
                      placeholder="¿Qué estás pensando?"
                      rows={4}
                      value={newPostText}
                      onChange={(event) => setNewPostText(event.target.value)}
                    />
                    {showCreatePostEmojiPanel ? (
                      <div className="create-post-inline-panel">
                        {COMMENT_EMOJIS.map((emoji) => (
                          <button
                            key={emoji}
                            type="button"
                            className="emoji-chip"
                            onClick={() => setNewPostText((prev) => `${prev}${emoji}`)}
                          >
                            {emoji}
                          </button>
                        ))}
                      </div>
                    ) : createPostMentionSuggestions.length && !newPostText.trim().endsWith("@") ? (
                      <div className="create-post-inline-panel create-post-mention-suggestions">
                        {createPostMentionSuggestions.map((user) => (
                          <button
                            key={user.id}
                            type="button"
                            className="mention-chip"
                            onClick={() => {
                              const parts = newPostText.split(/\s+/);
                              parts[parts.length - 1] = `@${user.username}`;
                              setNewPostText(`${parts.join(" ")} `);
                            }}
                          >
                            {user.avatar_url ? (
                              <img className="mention-chip-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                            ) : (
                              <span className="mention-chip-fallback">{user.username.slice(0, 1).toUpperCase()}</span>
                            )}
                            <span>@{user.username}</span>
                          </button>
                        ))}
                      </div>
                    ) : null}
                    <div className="create-post-composer-tools">
                      <button
                        type="button"
                        className="icon-button"
                        onClick={() => {
                          setNewPostText((prev) => `${prev}@`);
                          setShowCreatePostEmojiPanel(false);
                        }}
                        aria-label="Mencionar"
                      >
                        <UiIcon name="at" className="ui-icon" />
                      </button>
                      <button
                        type="button"
                        className={`icon-button ${showCreatePostEmojiPanel ? "is-active" : ""}`}
                        onClick={() => setShowCreatePostEmojiPanel((prev) => !prev)}
                        aria-label="Emojis"
                      >
                        <UiIcon name="smile" className="ui-icon" />
                      </button>
                    </div>
                  </div>
                  <button
                    type="button"
                    className="create-post-coffee-row"
                    onClick={() => setShowCreatePostCoffeeSheet(true)}
                  >
                    <UiIcon name="coffee" className="ui-icon" />
                    <span>Anadir cafe</span>
                    <strong>{selectedCreatePostCoffee ? selectedCreatePostCoffee.nombre : "Seleccionar cafe"}</strong>
                    <UiIcon name="chevron-right" className="ui-icon" />
                  </button>
                  {newPostImagePreviewUrl ? (
                    <div className="create-post-image-detail-wrap">
                      <img className="create-post-preview create-post-preview-detail" src={newPostImagePreviewUrl} alt="Previsualizacion" loading="lazy" />
                      <button
                        type="button"
                        className="create-post-image-remove"
                        onClick={() => {
                          if (!newPostSelectedImageId) return;
                          setNewPostGalleryItems((prev) => {
                            const target = prev.find((item) => item.id === newPostSelectedImageId);
                            if (target?.previewUrl.startsWith("blob:")) URL.revokeObjectURL(target.previewUrl);
                            const next = prev.filter((item) => item.id !== newPostSelectedImageId);
                            const replacement = next[0] ?? null;
                            setNewPostSelectedImageId(replacement?.id ?? null);
                            setNewPostImageFile(replacement?.file ?? null);
                            setNewPostImagePreviewUrl(replacement?.previewUrl ?? "");
                            return next;
                          });
                        }}
                        aria-label="Quitar foto"
                      >
                        <UiIcon name="close" className="ui-icon" />
                      </button>
                    </div>
                  ) : (
                    <button
                      type="button"
                      className="create-post-add-photo-row"
                      onClick={() => newPostImageInputRef.current?.click()}
                    >
                      <UiIcon name="camera" className="ui-icon" />
                      <span>Anadir foto</span>
                      <UiIcon name="chevron-right" className="ui-icon" />
                    </button>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      ) : null}

      {showCreatePostCoffeeSheet ? (
        <div className="sheet-overlay create-post-coffee-overlay" role="dialog" aria-modal="true" aria-label="Seleccionar cafe" onClick={() => setShowCreatePostCoffeeSheet(false)}>
          <div className="sheet-card create-post-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">SELECCIONAR CAFE</strong>
            </header>
            <div className="create-post-coffee-body">
              <input
                className="search-wide"
                placeholder="Buscar cafe"
                value={createPostCoffeeQuery}
                onChange={(event) => setCreatePostCoffeeQuery(event.target.value)}
              />
              <ul className="create-post-coffee-list">
                {filteredCreatePostCoffees.map((coffee) => (
                  <li key={coffee.id}>
                    <button
                      type="button"
                      className={`create-post-coffee-item ${coffee.id === newPostCoffeeId ? "is-selected" : ""}`}
                      onClick={() => {
                        setNewPostCoffeeId(coffee.id);
                        setShowCreatePostCoffeeSheet(false);
                      }}
                    >
                      {coffee.image_url ? <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" /> : <span className="create-post-coffee-fallback">{coffee.nombre.slice(0, 1).toUpperCase()}</span>}
                      <div>
                        <p>{coffee.nombre}</p>
                        <span>{coffee.marca}</span>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      ) : null}

      {showNotificationsPanel ? (
        <div
          className="sheet-overlay notifications-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Notificaciones"
          onClick={closeNotificationsPanel}
        >
          <div className="sheet-card notifications-panel" onClick={(event) => event.stopPropagation()}>
            <header className="topbar topbar-centered topbar-timeline notifications-header">
              <div className="topbar-slot">
                <button
                  type="button"
                  className="icon-button topbar-icon-button notifications-back"
                  onClick={closeNotificationsPanel}
                  aria-label="Atras"
                >
                  <UiIcon name="arrow-left" className="ui-icon" />
                </button>
              </div>
              <h2 className="title title-upper topbar-title-center topbar-brand-title notifications-title">NOTIFICACIONES</h2>
              <div className="topbar-slot topbar-slot-end">
                <button type="button" className="icon-button topbar-icon-button notifications-header-spacer" aria-hidden="true" tabIndex={-1}>
                  <UiIcon name="notifications" className="ui-icon" />
                </button>
              </div>
            </header>
            <ul className="sheet-list notifications-list">
              {visibleTimelineNotifications.length ? (
                visibleTimelineNotifications.map((item) => {
                  const user = usersById.get(item.userId);
                  const isUnread = item.timestamp > notificationsLastSeenAt;
                  return (
                    <NotificationRow
                      key={item.id}
                      item={item}
                      user={user}
                      isUnread={isUnread}
                      isFollowing={followingIds.has(item.userId)}
                      isDismissing={dismissingNotificationIds.has(item.id)}
                      onDismiss={dismissNotification}
                      onOpen={() => {
                        if (item.type === "comment" && item.postId) {
                          setCommentSheetPostId(item.postId);
                          setHighlightedCommentId(item.commentId ?? null);
                          closeNotificationsPanel();
                          return;
                        }
                        closeNotificationsPanel();
                      }}
                      onToggleFollow={handleToggleFollow}
                      onOpenUserProfile={() => {
                        navigateToTab("profile", { profileUserId: item.userId });
                        closeNotificationsPanel();
                      }}
                      onReply={() => {
                        if (item.type !== "comment" || !item.postId) return;
                        setCommentSheetPostId(item.postId);
                        setHighlightedCommentId(item.commentId ?? null);
                        closeNotificationsPanel();
                      }}
                    />
                  );
                })
              ) : (
                <li className="sheet-item notifications-empty">No tienes notificaciones</li>
              )}
            </ul>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryQuickActions ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Nuevo registro" onClick={() => setShowDiaryQuickActions(false)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">NUEVO REGISTRO</strong>
            </header>
            <div className="diary-sheet-list">
              <button
                type="button"
                className="diary-sheet-action is-water"
                onClick={() => {
                  setDiaryWaterMlDraft("250");
                  setShowDiaryQuickActions(false);
                  setShowDiaryWaterSheet(true);
                }}
              >
                <UiIcon name="stock" className="ui-icon" />
                <span>Agua</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
              <button
                type="button"
                className="diary-sheet-action is-coffee"
                onClick={() => {
                  const defaultCoffee = diaryCoffeeOptions[0];
                  if (defaultCoffee) {
                    setDiaryCoffeeIdDraft(defaultCoffee.id);
                    const isDecaf = normalizeLookupText(defaultCoffee.cafeina ?? "").includes("sin");
                    setDiaryCoffeeCaffeineDraft(isDecaf ? "5" : "95");
                  } else {
                    setDiaryCoffeeIdDraft("");
                    setDiaryCoffeeCaffeineDraft("95");
                  }
                  setDiaryCoffeeMlDraft("250");
                  setDiaryCoffeePreparationDraft("Manual");
                  setShowDiaryQuickActions(false);
                  setShowDiaryCoffeeSheet(true);
                }}
              >
                <UiIcon name="coffee" className="ui-icon" />
                <span>Café</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
              <button
                type="button"
                className="diary-sheet-action is-pantry"
                onClick={() => {
                  const defaultCoffee = diaryCoffeeOptions[0];
                  setDiaryPantryCoffeeIdDraft(defaultCoffee?.id ?? "");
                  setDiaryPantryGramsDraft("250");
                  setShowDiaryQuickActions(false);
                  setShowDiaryAddPantrySheet(true);
                }}
              >
                <UiIcon name="add" className="ui-icon" />
                <span>Añadir a Despensa</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryPeriodSheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Seleccionar periodo" onClick={() => setShowDiaryPeriodSheet(false)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">SELECCIONAR PERIODO</strong>
            </header>
            <div className="diary-sheet-list">
              {([
                { value: "hoy", label: "HOY" },
                { value: "7d", label: "SEMANA" },
                { value: "30d", label: "MES" }
              ] as const).map((option) => (
                <button
                  key={option.value}
                  type="button"
                  className={`diary-sheet-action ${diaryPeriod === option.value ? "is-active" : ""}`.trim()}
                  onClick={() => {
                    setDiaryPeriod(option.value);
                    setShowDiaryPeriodSheet(false);
                  }}
                >
                  <span>{option.label}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryWaterSheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Registrar agua" onClick={() => setShowDiaryWaterSheet(false)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">REGISTRAR AGUA</strong>
            </header>
            <div className="diary-sheet-form">
              <div className="diary-water-presets">
                {[250, 500, 750].map((value) => (
                  <button
                    key={value}
                    type="button"
                    className={`chip-button period-chip ${Number(diaryWaterMlDraft) === value ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryWaterMlDraft(String(value))}
                  >
                    {value} ml
                  </button>
                ))}
              </div>
              <label>
                <span>Cantidad personalizada (ml)</span>
                <input
                  className="search-wide"
                  type="number"
                  min={1}
                  value={diaryWaterMlDraft}
                  onChange={(event) => setDiaryWaterMlDraft(event.target.value)}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={() => setShowDiaryWaterSheet(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  onClick={async () => {
                    if (!activeUser) return;
                    const amount = Math.max(1, Math.round(Number(diaryWaterMlDraft || 0)));
                    const created = await createDiaryEntry({
                      userId: activeUser.id,
                      coffeeId: null,
                      coffeeName: "Agua",
                      amountMl: amount,
                      caffeineMg: 0,
                      preparationType: "None",
                      type: "WATER"
                    });
                    setDiaryEntries((prev) => [created, ...prev]);
                    setShowDiaryWaterSheet(false);
                  }}
                >
                  Guardar
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryCoffeeSheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Registrar cafe" onClick={() => setShowDiaryCoffeeSheet(false)}>
          <div className="sheet-card diary-sheet diary-coffee-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">REGISTRAR CAFÉ</strong>
            </header>
            <div className="diary-sheet-form">
              <div className="diary-edit-entry-presets diary-coffee-method-presets">
                {[
                  { label: "Espresso", drawable: "maq_espresso.png" },
                  { label: "V60", drawable: "maq_hario_v60.png" },
                  { label: "Aeropress", drawable: "maq_aeropress.png" },
                  { label: "Moka", drawable: "maq_italiana.png" }
                ].map((method) => (
                  <button
                    key={method.label}
                    type="button"
                    className={`chip-button period-chip diary-edit-entry-method-chip ${normalizeLookupText(diaryCoffeePreparationDraft) === normalizeLookupText(method.label) ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryCoffeePreparationDraft(method.label)}
                  >
                    <img src={`/android-drawable/${method.drawable}`} alt="" aria-hidden="true" />
                    <span>{method.label}</span>
                  </button>
                ))}
              </div>
              <label>
                <span>Café</span>
                <div className="diary-coffee-picker" role="listbox" aria-label="Seleccionar café">
                  {diaryCoffeeOptions.map((coffee) => {
                    const selected = coffee.id === diaryCoffeeIdDraft;
                    return (
                      <button
                        key={coffee.id}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={`diary-coffee-picker-item ${selected ? "is-active" : ""}`.trim()}
                        onClick={() => {
                          setDiaryCoffeeIdDraft(coffee.id);
                          const isDecaf = normalizeLookupText(coffee.cafeina ?? "").includes("sin");
                          setDiaryCoffeeCaffeineDraft(isDecaf ? "5" : "95");
                        }}
                      >
                        <span className="diary-coffee-picker-media">
                          {coffee.image_url ? (
                            <img src={coffee.image_url} alt="" loading="lazy" />
                          ) : (
                            <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" />
                          )}
                        </span>
                        <span className="diary-coffee-picker-copy">
                          <strong>{coffee.nombre}</strong>
                          <em>{(coffee.marca || "CAFÉ").toUpperCase()}</em>
                        </span>
                      </button>
                    );
                  })}
                </div>
              </label>
              <label>
                <span>Preparación</span>
                <input
                  className="search-wide"
                  value={diaryCoffeePreparationDraft}
                  onChange={(event) => setDiaryCoffeePreparationDraft(event.target.value)}
                />
              </label>
              <label>
                <span>Cantidad (ml)</span>
                <input
                  className="search-wide diary-edit-entry-input"
                  type="number"
                  inputMode="numeric"
                  min={1}
                  value={diaryCoffeeMlDraft}
                  onChange={(event) => setDiaryCoffeeMlDraft(event.target.value)}
                />
                <input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={10}
                  max={700}
                  step={10}
                  value={Math.max(10, Number(diaryCoffeeMlDraft || 10))}
                  onChange={(event) => setDiaryCoffeeMlDraft(String(Math.max(10, Number(event.target.value || 10))))}
                />
              </label>
              <div className="diary-coffee-size-presets">
                {[
                  { label: "Espresso", ml: 30, drawable: "taza_espresso.png" },
                  { label: "Peq.", ml: 180, drawable: "taza_pequeno.png" },
                  { label: "Med.", ml: 275, drawable: "taza_mediano.png" },
                  { label: "Gra.", ml: 375, drawable: "taza_grande.png" },
                  { label: "XL", ml: 475, drawable: "taza_xl.png" }
                ].map((size) => (
                  <button
                    key={size.label}
                    type="button"
                    className={`chip-button period-chip diary-coffee-size-chip ${Math.abs(Number(diaryCoffeeMlDraft || 0) - size.ml) <= 10 ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryCoffeeMlDraft(String(size.ml))}
                  >
                    <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" />
                    <span>{size.label}</span>
                  </button>
                ))}
              </div>
              <label>
                <span>Cafeína (mg)</span>
                <input
                  className="search-wide diary-edit-entry-input"
                  type="number"
                  inputMode="numeric"
                  min={0}
                  value={diaryCoffeeCaffeineDraft}
                  onChange={(event) => setDiaryCoffeeCaffeineDraft(event.target.value)}
                />
                <input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={0}
                  max={350}
                  step={5}
                  value={Math.max(0, Number(diaryCoffeeCaffeineDraft || 0))}
                  onChange={(event) => setDiaryCoffeeCaffeineDraft(String(Math.max(0, Number(event.target.value || 0))))}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={() => setShowDiaryCoffeeSheet(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  onClick={async () => {
                    if (!activeUser) return;
                    if (!selectedDiaryCoffee) return;
                    const created = await createDiaryEntry({
                      userId: activeUser.id,
                      coffeeId: selectedDiaryCoffee.id,
                      coffeeName: selectedDiaryCoffee.nombre,
                      amountMl: Math.max(1, Number(diaryCoffeeMlDraft || 0)),
                      caffeineMg: Math.max(0, Number(diaryCoffeeCaffeineDraft || 0)),
                      preparationType: diaryCoffeePreparationDraft.trim() || "Manual",
                      type: "CUP"
                    });
                    setDiaryEntries((prev) => [created, ...prev]);
                    setShowDiaryCoffeeSheet(false);
                  }}
                >
                  Guardar
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {activeTab === "diary" && showDiaryAddPantrySheet ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Añadir a despensa" onClick={() => setShowDiaryAddPantrySheet(false)}>
          <div className="sheet-card diary-sheet diary-pantry-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">AÑADIR A DESPENSA</strong>
            </header>
            <div className="diary-sheet-form">
              {selectedDiaryPantryCoffee ? (
                <div className="diary-edit-entry-preview" aria-hidden="true">
                  <div className="diary-entry-media">
                    {selectedDiaryPantryCoffee.image_url ? (
                      <img src={selectedDiaryPantryCoffee.image_url} alt={selectedDiaryPantryCoffee.nombre} loading="lazy" />
                    ) : (
                      <img className="diary-entry-fallback-drawable" src="/android-drawable/taza_mediano.png" alt="" aria-hidden="true" loading="lazy" />
                    )}
                  </div>
                  <div className="diary-entry-copy">
                    <p className="feed-user">{selectedDiaryPantryCoffee.nombre}</p>
                    <p className="feed-meta diary-entry-brand">{(selectedDiaryPantryCoffee.marca || "CAFÉ").toUpperCase()}</p>
                  </div>
                </div>
              ) : null}
              <label>
                <span>Café</span>
                <div className="diary-coffee-picker" role="listbox" aria-label="Seleccionar café para despensa">
                  {diaryCoffeeOptions.map((coffee) => {
                    const selected = coffee.id === diaryPantryCoffeeIdDraft;
                    return (
                      <button
                        key={coffee.id}
                        type="button"
                        role="option"
                        aria-selected={selected}
                        className={`diary-coffee-picker-item ${selected ? "is-active" : ""}`.trim()}
                        onClick={() => setDiaryPantryCoffeeIdDraft(coffee.id)}
                      >
                        <span className="diary-coffee-picker-media">
                          {coffee.image_url ? (
                            <img src={coffee.image_url} alt="" loading="lazy" />
                          ) : (
                            <img src="/android-drawable/taza_mediano.png" alt="" loading="lazy" />
                          )}
                        </span>
                        <span className="diary-coffee-picker-copy">
                          <strong>{coffee.nombre}</strong>
                          <em>{(coffee.marca || "CAFÉ").toUpperCase()}</em>
                        </span>
                      </button>
                    );
                  })}
                </div>
              </label>
              <div className="diary-water-presets diary-edit-entry-presets is-water">
                {[100, 250, 500].map((value) => (
                  <button
                    key={value}
                    type="button"
                    className={`chip-button period-chip ${Number(diaryPantryGramsDraft) === value ? "is-active" : ""}`.trim()}
                    onClick={() => setDiaryPantryGramsDraft(String(value))}
                  >
                    {value} g
                  </button>
                ))}
              </div>
              <label>
                <span>Gramos a añadir</span>
                <div className="diary-edit-entry-measure">
                  <button
                    type="button"
                    className="diary-edit-entry-step"
                    aria-label="Reducir gramos"
                    onClick={() => bumpPantryGrams(-25)}
                  >
                    -
                  </button>
                  <input
                    className="search-wide diary-edit-entry-input"
                    type="number"
                    inputMode="numeric"
                    min={1}
                    value={diaryPantryGramsDraft}
                    onChange={(event) => setDiaryPantryGramsDraft(event.target.value)}
                  />
                  <span className="diary-edit-entry-unit" aria-hidden="true">g</span>
                  <button
                    type="button"
                    className="diary-edit-entry-step"
                    aria-label="Aumentar gramos"
                    onClick={() => bumpPantryGrams(25)}
                  >
                    +
                  </button>
                </div>
                <input
                  className="diary-edit-entry-slider"
                  type="range"
                  min={1}
                  max={2000}
                  step={25}
                  value={Math.max(1, Number(diaryPantryGramsDraft || 1))}
                  onChange={(event) => setDiaryPantryGramsDraft(String(Math.max(1, Number(event.target.value || 1))))}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost diary-edit-entry-cancel" onClick={() => setShowDiaryAddPantrySheet(false)}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button diary-edit-entry-save"
                  onClick={async () => {
                    if (!activeUser) return;
                    if (!selectedDiaryPantryCoffee) return;
                    const gramsToAdd = Math.max(1, Math.round(Number(diaryPantryGramsDraft || 0)));
                    const existing = pantryItems.find(
                      (item) => item.user_id === activeUser.id && item.coffee_id === selectedDiaryPantryCoffee.id
                    );
                    const total = Math.max(gramsToAdd, (existing?.total_grams ?? 0) + gramsToAdd);
                    const remaining = Math.max(gramsToAdd, (existing?.grams_remaining ?? 0) + gramsToAdd);
                    const updated = await upsertPantryStock({
                      coffeeId: selectedDiaryPantryCoffee.id,
                      userId: activeUser.id,
                      totalGrams: total,
                      gramsRemaining: remaining
                    });
                    setPantryItems((prev) =>
                      [updated, ...prev.filter((row) => !(row.user_id === updated.user_id && row.coffee_id === updated.coffee_id))]
                    );
                    setDiaryTab("despensa");
                    setShowDiaryAddPantrySheet(false);
                  }}
                >
                  Guardar
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function NotificationRow({
  item,
  user,
  isUnread,
  isFollowing,
  isDismissing,
  onDismiss,
  onOpen,
  onToggleFollow,
  onOpenUserProfile,
  onReply
}: {
  item: TimelineNotificationItem;
  user?: UserRow;
  isUnread: boolean;
  isFollowing: boolean;
  isDismissing: boolean;
  onDismiss: (id: string) => void;
  onOpen: () => void;
  onToggleFollow: (userId: number) => void;
  onOpenUserProfile: () => void;
  onReply: () => void;
}) {
  const [offsetX, setOffsetX] = useState(0);
  const pointerIdRef = useRef<number | null>(null);
  const startXRef = useRef(0);
  const movedRef = useRef(false);
  const threshold = -76;
  const maxDrag = -124;
  const isActionTarget = (target: EventTarget | null) =>
    target instanceof Element && Boolean(target.closest(".notifications-action"));
  const isUserLinkTarget = (target: EventTarget | null) =>
    target instanceof Element && Boolean(target.closest(".notifications-user-link"));

  const handlePointerDown = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target) || isUserLinkTarget(event.target)) return;
    if (pointerIdRef.current != null) return;
    pointerIdRef.current = event.pointerId;
    startXRef.current = event.clientX;
    movedRef.current = false;
    event.currentTarget.setPointerCapture(event.pointerId);
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target) || isUserLinkTarget(event.target)) return;
    if (pointerIdRef.current !== event.pointerId) return;
    const dx = event.clientX - startXRef.current;
    if (Math.abs(dx) > 6) movedRef.current = true;
    if (dx >= 0) {
      setOffsetX(0);
      return;
    }
    setOffsetX(Math.max(maxDrag, dx));
  };

  const endDrag = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (isActionTarget(event.target) || isUserLinkTarget(event.target)) return;
    if (pointerIdRef.current !== event.pointerId) return;
    pointerIdRef.current = null;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    if (offsetX <= threshold) {
      onDismiss(item.id);
      setOffsetX(0);
      return;
    }
    setOffsetX(0);
    window.setTimeout(() => {
      movedRef.current = false;
    }, 60);
  };

  return (
    <li
      className={`sheet-item notifications-item ${isUnread ? "is-unread" : ""} ${isDismissing ? "is-dismissing" : ""} ${offsetX < -1 ? "is-swiping" : ""}`}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={endDrag}
      onPointerCancel={endDrag}
      onClick={(event) => {
        if (isUserLinkTarget(event.target) || isActionTarget(event.target)) return;
        if (movedRef.current || isDismissing) return;
        onOpen();
      }}
    >
      <div className="notifications-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div className="notifications-swipe-content" style={{ transform: `translateX(${offsetX}px)` }}>
        <div className="sheet-item-head notifications-item-head">
          <button
            type="button"
            className="notifications-user-link"
            onClick={(event) => {
              event.stopPropagation();
              onOpenUserProfile();
            }}
          >
            <div className="notifications-avatar-wrap">
              {user?.avatar_url ? (
                <img className="avatar avatar-photo notifications-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
              ) : (
                <div className="avatar notifications-avatar" aria-hidden="true">
                  {(user?.username ?? "us").slice(0, 2).toUpperCase()}
                </div>
              )}
              {isUnread ? <span className="sheet-unread-dot notifications-unread-dot" aria-hidden="true" /> : null}
            </div>
            <div className="notifications-copy">
              <p className="feed-user notifications-user">@{user?.username ?? `user${item.userId}`}</p>
              <p className="feed-meta notifications-subtitle">{item.text}</p>
            </div>
          </button>
          {item.type === "follow" ? (
            <button
              type="button"
              className={`action-button notification-follow-button notifications-action ${isFollowing ? "is-following" : ""}`}
              onClick={(event) => {
                event.stopPropagation();
                onToggleFollow(item.userId);
              }}
            >
              {isFollowing ? "SIGUIENDO" : "SEGUIR"}
            </button>
          ) : null}
          {item.type === "comment" ? (
            <button
              type="button"
              className="action-button notifications-action notifications-reply"
              onClick={(event) => {
                event.stopPropagation();
                onReply();
              }}
            >
              RESPONDER
            </button>
          ) : null}
        </div>
      </div>
    </li>
  );
}

function MobileBarcodeScannerSheet({
  open,
  onClose,
  onDetected
}: {
  open: boolean;
  onClose: () => void;
  onDetected: (value: string) => void;
}) {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const rafRef = useRef<number | null>(null);
  const closedRef = useRef(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    closedRef.current = false;
    setErrorMessage(null);

    const stop = () => {
      if (rafRef.current != null) {
        window.cancelAnimationFrame(rafRef.current);
        rafRef.current = null;
      }
      if (streamRef.current) {
        streamRef.current.getTracks().forEach((track) => track.stop());
        streamRef.current = null;
      }
    };

    const boot = async () => {
      try {
        if (!window.BarcodeDetector) {
          setErrorMessage("Tu navegador no soporta escaneo de codigo en web.");
          return;
        }
        const stream = await window.navigator.mediaDevices.getUserMedia({
          video: {
            facingMode: { ideal: "environment" },
            width: { ideal: 1280 },
            height: { ideal: 720 }
          },
          audio: false
        });
        if (closedRef.current) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        streamRef.current = stream;
        const video = videoRef.current;
        if (!video) return;
        video.srcObject = stream;
        await video.play();
        const detector = new window.BarcodeDetector({
          formats: ["ean_13", "ean_8", "upc_a", "upc_e", "code_128", "code_39", "qr_code"]
        });

        const scanLoop = async () => {
          if (closedRef.current) return;
          const currentVideo = videoRef.current;
          if (!currentVideo || currentVideo.readyState < 2) {
            rafRef.current = window.requestAnimationFrame(scanLoop);
            return;
          }
          try {
            const hits = await detector.detect(currentVideo);
            const value = hits.find((hit) => Boolean(hit.rawValue?.trim()))?.rawValue?.trim();
            if (value) {
              onDetected(value);
              onClose();
              return;
            }
          } catch {
            // continue scanning
          }
          rafRef.current = window.requestAnimationFrame(scanLoop);
        };

        rafRef.current = window.requestAnimationFrame(scanLoop);
      } catch {
        setErrorMessage("No pudimos acceder a la camara del dispositivo.");
      }
    };

    void boot();
    return () => {
      closedRef.current = true;
      stop();
    };
  }, [onClose, onDetected, open]);

  if (!open) return null;

  return (
    <div className="sheet-overlay barcode-scanner-overlay" role="dialog" aria-modal="true" aria-label="Escanear codigo" onClick={onClose}>
      <div className="sheet-card barcode-scanner-sheet" onClick={(event) => event.stopPropagation()}>
        <div className="sheet-handle" aria-hidden="true" />
        <header className="sheet-header">
          <strong className="sheet-title">ESCANEAR CODIGO</strong>
        </header>
        <div className="barcode-scanner-body">
          {errorMessage ? (
            <p className="barcode-scanner-error">{errorMessage}</p>
          ) : (
            <div className="barcode-scanner-video-wrap">
              <video ref={videoRef} className="barcode-scanner-video" playsInline muted autoPlay />
              <div className="barcode-scanner-frame" aria-hidden="true" />
            </div>
          )}
          <button type="button" className="action-button action-button-ghost barcode-scanner-close" onClick={onClose}>
            Cerrar
          </button>
        </div>
      </div>
    </div>
  );
}

function TopBar({
  activeTab,
  searchQuery,
  searchMode,
  onSearchQueryChange,
  onSearchCancel,
  onSearchBarcodeClick,
  showSearchBarcodeButton,
  showSearchCoffeeFilterChips,
  searchOriginCount,
  searchSpecialtyCount,
  searchRoastCount,
  searchFormatCount,
  searchHasRatingFilter,
  onOpenSearchFilter,
  onSearchBack,
  showNotificationsBadge,
  onTimelineSearchUsers,
  onTimelineNotifications,
  diaryPeriod,
  onDiaryOpenQuickActions,
  onDiaryOpenPeriodSelector,
  scrolled,
  brewStep,
  onBrewBack,
  onBrewForward,
  brewCreateCoffeeOpen,
  onBrewCreateCoffeeBack,
  onProfileSignOut,
  profileMenuEnabled,
  onProfileOpenEdit,
  onCoffeeBack,
  coffeeTopbarFavoriteActive,
  coffeeTopbarStockActive,
  onCoffeeTopbarToggleFavorite,
  onCoffeeTopbarOpenStock
}: {
  activeTab: TabId;
  searchQuery: string;
  searchMode: "coffees" | "users";
  onSearchQueryChange: (value: string) => void;
  onSearchCancel: () => void;
  onSearchBarcodeClick: () => void;
  showSearchBarcodeButton: boolean;
  showSearchCoffeeFilterChips: boolean;
  searchOriginCount: number;
  searchSpecialtyCount: number;
  searchRoastCount: number;
  searchFormatCount: number;
  searchHasRatingFilter: boolean;
  onOpenSearchFilter: (filter: "origen" | "tueste" | "especialidad" | "formato" | "nota") => void;
  onSearchBack: () => void;
  showNotificationsBadge: boolean;
  onTimelineSearchUsers: () => void;
  onTimelineNotifications: () => void;
  diaryPeriod: "hoy" | "7d" | "30d";
  onDiaryOpenQuickActions: () => void;
  onDiaryOpenPeriodSelector: () => void;
  scrolled: boolean;
  brewStep: BrewStep;
  onBrewBack: () => void;
  onBrewForward: () => void;
  brewCreateCoffeeOpen: boolean;
  onBrewCreateCoffeeBack: () => void;
  onProfileSignOut: () => void;
  profileMenuEnabled: boolean;
  onProfileOpenEdit: () => void;
  onCoffeeBack: () => void;
  coffeeTopbarFavoriteActive: boolean;
  coffeeTopbarStockActive: boolean;
  onCoffeeTopbarToggleFavorite: () => void;
  onCoffeeTopbarOpenStock: () => void;
}) {
  const [searchFocus, setSearchFocus] = useState(false);
  const [searchHintWord, setSearchHintWord] = useState<"marca" | "cafe">("marca");
  const [notificationPop, setNotificationPop] = useState(false);
  const [showProfileOptions, setShowProfileOptions] = useState(false);
  const showSearchCancel = Boolean(searchQuery || searchFocus);

  useEffect(() => {
    if (activeTab !== "search") return;
    const interval = window.setInterval(() => {
      setSearchHintWord((prev) => (prev === "marca" ? "cafe" : "marca"));
    }, 2200);
    return () => window.clearInterval(interval);
  }, [activeTab]);

  useEffect(() => {
    if (!showNotificationsBadge || activeTab !== "timeline") return;
    setNotificationPop(true);
    const id = window.setTimeout(() => setNotificationPop(false), 340);
    return () => window.clearTimeout(id);
  }, [activeTab, showNotificationsBadge]);

  useEffect(() => {
    setShowProfileOptions(false);
  }, [activeTab]);

  if (activeTab === "search") {
    if (searchMode === "users") {
      return (
        <header className={`topbar topbar-search-users ${showSearchCancel ? "has-cancel" : ""} ${scrolled ? "topbar-scrolled" : ""}`.trim()}>
          <div className="topbar-slot">
            <button className="icon-button topbar-icon-button search-users-back" type="button" onClick={onSearchBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
          </div>
          <div className="search-users-field">
            <UiIcon name="search" className="ui-icon search-users-leading-icon" />
            <input
              id="quick-search"
              className="search-wide search-input-standard search-users-input"
              placeholder="Buscar usuarios..."
              value={searchQuery}
              onFocus={() => setSearchFocus(true)}
              onBlur={() => setSearchFocus(false)}
              onChange={(event) => onSearchQueryChange(event.target.value)}
              aria-label="Buscar usuarios"
            />
          </div>
          <div className="topbar-slot topbar-slot-end search-users-cancel-slot">
            <button
              className={`text-button search-cancel-button ${showSearchCancel ? "is-visible" : ""}`}
              type="button"
              onClick={() => {
                onSearchCancel();
                setSearchFocus(false);
                const activeElement = document.activeElement;
                if (activeElement instanceof HTMLElement) activeElement.blur();
              }}
              aria-hidden={!showSearchCancel}
              tabIndex={showSearchCancel ? 0 : -1}
            >
              Cancelar
            </button>
          </div>
        </header>
      );
    }

    return (
      <header className={`topbar topbar-search ${showSearchCancel ? "has-cancel" : ""} ${scrolled ? "topbar-scrolled" : ""}`.trim()}>
        <div className="search-coffee-field">
          <UiIcon name="search" className="ui-icon search-coffee-leading-icon" />
          {!searchQuery && !searchFocus ? (
            <div className="search-coffee-placeholder" aria-hidden="true">
              <span>Busca </span>
              <span key={searchHintWord} className="search-coffee-placeholder-word">
                {searchHintWord}
              </span>
            </div>
          ) : null}
          <input
            id="quick-search"
            className="search-wide search-input-standard search-coffee-input"
            placeholder=""
            value={searchQuery}
            onFocus={() => setSearchFocus(true)}
            onBlur={() => setSearchFocus(false)}
            onChange={(event) => onSearchQueryChange(event.target.value)}
            aria-label="Busqueda"
          />
          {showSearchBarcodeButton ? (
            <button type="button" className="icon-button search-coffee-trailing-button" aria-label="Escanear codigo" onClick={onSearchBarcodeClick}>
              <UiIcon name="barcode" className="ui-icon" />
            </button>
          ) : null}
        </div>
        <button
          className={`text-button search-cancel-button ${showSearchCancel ? "is-visible" : ""}`}
          type="button"
          onClick={() => {
            onSearchCancel();
            setSearchFocus(false);
            const activeElement = document.activeElement;
            if (activeElement instanceof HTMLElement) activeElement.blur();
          }}
          aria-hidden={!showSearchCancel}
          tabIndex={showSearchCancel ? 0 : -1}
        >
          Cancelar
        </button>
        {showSearchCoffeeFilterChips ? (
          <div className="topbar-search-chips" role="tablist" aria-label="Filtros de busqueda">
            <button type="button" className={`filter-chip ${searchOriginCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("origen")}>
              PAIS
              {searchOriginCount ? <span className="filter-chip-count">{searchOriginCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchSpecialtyCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("especialidad")}>
              ESPECIALIDAD
              {searchSpecialtyCount ? <span className="filter-chip-count">{searchSpecialtyCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchRoastCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("tueste")}>
              TUESTE
              {searchRoastCount ? <span className="filter-chip-count">{searchRoastCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchFormatCount ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("formato")}>
              FORMATO
              {searchFormatCount ? <span className="filter-chip-count">{searchFormatCount}</span> : null}
            </button>
            <button type="button" className={`filter-chip ${searchHasRatingFilter ? "is-active" : ""}`} onClick={() => onOpenSearchFilter("nota")}>
              NOTA
              {searchHasRatingFilter ? <span className="filter-chip-count">1</span> : null}
            </button>
          </div>
        ) : null}
      </header>
    );
  }

  if (activeTab === "brewlab") {
    if (brewCreateCoffeeOpen) {
      return (
        <header className={`topbar topbar-timeline topbar-centered ${scrolled ? "topbar-scrolled" : ""}`}>
          <div className="topbar-slot">
            <button className="icon-button topbar-icon-button" type="button" onClick={onBrewCreateCoffeeBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
          </div>
          <h1 className="title title-upper topbar-title-center">CREAR CAFE</h1>
          <div className="topbar-slot topbar-slot-end" />
        </header>
      );
    }
    return (
      <header className={`topbar topbar-timeline topbar-centered ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-slot">
          {brewStep !== "method" ? (
            <button className="icon-button topbar-icon-button" type="button" onClick={onBrewBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
          ) : null}
        </div>
        <h1 className="title title-upper topbar-title-center">{getBrewStepTitle(brewStep)}</h1>
        <div className="topbar-slot topbar-slot-end">
          {brewStep === "config" ? (
            <button className="icon-button topbar-icon-button" type="button" onClick={onBrewForward} aria-label="Empezar">
              <UiIcon name="arrow-right" className="ui-icon" />
            </button>
          ) : null}
        </div>
      </header>
    );
  }

  if (activeTab === "diary") {
    const periodLabel = diaryPeriod === "hoy" ? "HOY" : diaryPeriod === "7d" ? "SEMANA" : "MES";
    return (
      <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-slot" />
        <h1 className="title title-upper topbar-title-center">MI DIARIO</h1>
        <div className="topbar-slot topbar-slot-end">
          <button className="icon-button topbar-icon-button diary-topbar-add" type="button" aria-label="Agregar" onClick={onDiaryOpenQuickActions}>
            <UiIcon name="add" className="ui-icon" />
          </button>
          <button className="chip-button diary-period-chip" type="button" onClick={onDiaryOpenPeriodSelector}>{periodLabel}</button>
        </div>
      </header>
    );
  }

  if (activeTab === "profile") {
    return (
      <>
        <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""}`}>
          <div className="topbar-slot" />
          <h1 className="title title-upper topbar-title-center">PERFIL</h1>
          <div className="topbar-slot topbar-slot-end">
            {profileMenuEnabled ? (
              <button
                className="icon-button profile-topbar-menu-trigger"
                type="button"
                aria-label="Opciones de perfil"
                onClick={() => setShowProfileOptions(true)}
              >
                <UiIcon name="more" className="ui-icon" />
              </button>
            ) : null}
          </div>
        </header>
        {showProfileOptions && profileMenuEnabled ? (
          <div className="sheet-overlay profile-topbar-options-overlay" role="dialog" aria-modal="true" aria-label="Opciones de perfil" onClick={() => setShowProfileOptions(false)}>
            <div className="sheet-card profile-topbar-options-sheet" onClick={(event) => event.stopPropagation()}>
              <div className="sheet-handle" aria-hidden="true" />
              <div className="comment-action-list">
                <p className="comment-action-title">OPCIONES</p>
                <button
                  type="button"
                  className="comment-action-button"
                  onClick={() => {
                    setShowProfileOptions(false);
                    onProfileOpenEdit();
                  }}
                >
                  <UiIcon name="edit" className="ui-icon" />
                  <span>Editar perfil</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </button>
                <button
                  type="button"
                  className="comment-action-button is-danger"
                  onClick={() => {
                    setShowProfileOptions(false);
                    onProfileSignOut();
                  }}
                >
                  <UiIcon name="close" className="ui-icon" />
                  <span>Cerrar sesión</span>
                  <UiIcon name="chevron-right" className="ui-icon trailing" />
                </button>
              </div>
            </div>
          </div>
        ) : null}
      </>
    );
  }

  if (activeTab === "coffee") {
    return (
      <header className={`topbar topbar-timeline topbar-centered topbar-coffee ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-slot">
          <button className="icon-button topbar-icon-button" type="button" onClick={onCoffeeBack} aria-label="Volver">
            <UiIcon name="arrow-left" className="ui-icon" />
          </button>
        </div>
        <h1 className="title title-upper topbar-title-center">CAFE</h1>
        <div className="topbar-slot topbar-slot-end">
          <button
            className={`icon-button topbar-icon-button ${coffeeTopbarFavoriteActive ? "is-active" : ""}`.trim()}
            type="button"
            aria-label={coffeeTopbarFavoriteActive ? "Quitar de favoritos" : "Guardar en favoritos"}
            onClick={onCoffeeTopbarToggleFavorite}
          >
            <UiIcon name={coffeeTopbarFavoriteActive ? "favorite-filled" : "favorite"} className="ui-icon" />
          </button>
          <button
            className={`icon-button topbar-icon-button ${coffeeTopbarStockActive ? "is-active" : ""}`.trim()}
            type="button"
            aria-label="Añadir a stock"
            onClick={onCoffeeTopbarOpenStock}
          >
            <UiIcon name="stock" className="ui-icon" />
          </button>
        </div>
      </header>
    );
  }

  return (
    <header className={`topbar topbar-centered topbar-timeline ${scrolled ? "topbar-scrolled" : ""}`}>
      <div className="topbar-slot">
        <button className="icon-button topbar-icon-button" type="button" aria-label="Buscar Usuarios" onClick={onTimelineSearchUsers}>
          <UiIcon name="search" className="ui-icon" />
        </button>
      </div>
      <h1 className="title title-upper topbar-title-center topbar-brand-title">CAFESITO</h1>
      <div className="topbar-slot topbar-slot-end">
        <button
          className={`icon-button topbar-icon-button ${notificationPop ? "notification-pop" : ""}`}
          type="button"
          aria-label="Notificaciones"
          onClick={onTimelineNotifications}
        >
          <UiIcon name="notifications" className="ui-icon" />
          {showNotificationsBadge ? <span className="badge-dot" aria-hidden="true" /> : null}
        </button>
      </div>
    </header>
  );
}

function LoginGate({
  loading,
  message,
  errorMessage,
  onGoogleLogin
}: {
  loading: boolean;
  message?: string;
  errorMessage?: string | null;
  onGoogleLogin?: () => void;
}) {
  return (
    <main className="login-gate" aria-label="Inicio de sesion">
      <section className="login-card">
        <p className="login-brand">CAFESITO</p>
        <h1 className="login-title">Inicia sesion</h1>
        <p className="login-copy">Accede con Google para continuar.</p>
        <button
          type="button"
          className="google-login-button"
          onClick={onGoogleLogin}
          disabled={loading || !onGoogleLogin}
        >
          <span className="google-dot" aria-hidden="true" />
          {loading ? "Conectando..." : "Continuar con Google"}
        </button>
        {message ? <p className="login-hint">{message}</p> : null}
        {errorMessage ? <p className="login-error">{errorMessage}</p> : null}
      </section>
    </main>
  );
}

function TimelineView({
  mode,
  cards,
  recommendations,
  suggestions,
  suggestionIndices,
  followingIds,
  followerCounts,
  loading,
  errorMessage,
  refreshing,
  activeUserId,
  onOpenComments,
  onToggleLike,
  onToggleFollow,
  onEditPost,
  onDeletePost,
  onRefresh,
  onMentionClick,
  onOpenUserProfile,
  onOpenCoffee,
  sidePanel
}: {
  mode: "mobile" | "desktop";
  cards: TimelineCard[];
  recommendations: CoffeeRow[];
  suggestions: UserRow[];
  suggestionIndices: number[];
  followingIds: Set<number>;
  followerCounts: Map<number, number>;
  loading: boolean;
  errorMessage: string | null;
  refreshing: boolean;
  activeUserId: number | null;
  onOpenComments: (postId: string) => void;
  onToggleLike: (postId: string) => void;
  onToggleFollow: (userId: number) => void;
  onEditPost: (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => Promise<void>;
  onDeletePost: (postId: string) => Promise<void>;
  onRefresh: () => Promise<void>;
  onMentionClick: (username: string) => void;
  onOpenUserProfile: (userId: number) => void;
  onOpenCoffee: (coffeeId: string) => void;
  sidePanel?: ReactNode;
}) {
  const [menuPostId, setMenuPostId] = useState<string | null>(null);
  const [editingPostId, setEditingPostId] = useState<string | null>(null);
  const [editingText, setEditingText] = useState("");
  const [editingImageUrl, setEditingImageUrl] = useState("");
  const [editingImageFile, setEditingImageFile] = useState<File | null>(null);
  const [editingImagePreviewUrl, setEditingImagePreviewUrl] = useState("");
  const [likeBurstPostId, setLikeBurstPostId] = useState<string | null>(null);
  const [dismissingSuggestionIds, setDismissingSuggestionIds] = useState<Set<number>>(new Set());
  const [pendingSuggestionFollowIds, setPendingSuggestionFollowIds] = useState<Set<number>>(new Set());
  const [pullDistance, setPullDistance] = useState(0);
  const editImageInputRef = useRef<HTMLInputElement | null>(null);
  const touchStartY = useRef<number | null>(null);
  const pullActive = useRef(false);
  const likeBurstTimerRef = useRef<number | null>(null);
  const visibleCards = cards;
  const isDesktopTimeline = mode === "desktop";
  const activeMenuPost = useMemo(
    () => visibleCards.find((card) => card.id === menuPostId) ?? null,
    [menuPostId, visibleCards]
  );

  const closeEditModal = useCallback(() => {
    if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
    setEditingPostId(null);
    setEditingText("");
    setEditingImageUrl("");
    setEditingImageFile(null);
    setEditingImagePreviewUrl("");
  }, [editingImagePreviewUrl]);

  const isMobileLike = typeof window !== "undefined" ? window.innerWidth < 900 : false;
  const pullProgress = Math.min(1, pullDistance / 84);

  const handleTouchStart = (event: React.TouchEvent<HTMLElement>) => {
    if (!isMobileLike) return;
    if (window.scrollY > 0) return;
    touchStartY.current = event.touches[0]?.clientY ?? null;
    pullActive.current = true;
  };

  const handleTouchMove = (event: React.TouchEvent<HTMLElement>) => {
    if (!pullActive.current) return;
    const start = touchStartY.current;
    const current = event.touches[0]?.clientY;
    if (start == null || current == null) return;
    const delta = Math.max(0, current - start);
    if (delta <= 0) return;
    setPullDistance(Math.min(120, delta * 0.55));
  };

  const handleTouchEnd = () => {
    if (!pullActive.current) return;
    pullActive.current = false;
    touchStartY.current = null;
    const shouldRefresh = pullDistance >= 76 && !refreshing;
    setPullDistance(0);
    if (shouldRefresh) void onRefresh();
  };

  useEffect(() => {
    const onEsc = (event: KeyboardEvent) => {
      if (event.key !== "Escape") return;
      setMenuPostId(null);
      if (editingPostId) closeEditModal();
    };
    window.addEventListener("keydown", onEsc);
    return () => window.removeEventListener("keydown", onEsc);
  }, [closeEditModal, editingPostId]);

  useEffect(() => {
    if (!editingPostId) return;
    const prev = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = prev;
    };
  }, [editingPostId]);

  useEffect(() => {
    return () => {
      if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
    };
  }, [editingImagePreviewUrl]);

  useEffect(() => {
    return () => {
      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
    };
  }, []);

  if (loading) {
    return (
      <ul className="feed-list">
        {[1, 2, 3, 4, 5].map((item) => (
          <li key={item} className="feed-card shimmer-card" />
        ))}
      </ul>
    );
  }

  if (errorMessage) {
    return (
      <article className="timeline-empty timeline-error">
        <h3>No pudimos cargar el timeline</h3>
        <p>{errorMessage}</p>
      </article>
    );
  }

  const recommendationSection = recommendations.length ? (
    <section className="suggestion-strip">
      <p className="section-title">Recomendados para tu paladar</p>
      <div className="horizontal-cards">
        {recommendations.map((coffee) => (
          <button
            key={coffee.id}
            type="button"
            className="mini-card mini-coffee-card mini-coffee-link"
            onClick={() => onOpenCoffee(coffee.id)}
          >
            {coffee.image_url ? <img className="mini-cover" src={coffee.image_url} alt={coffee.nombre} loading="lazy" /> : null}
            <p className="coffee-origin">{coffee.pais_origen ?? "Origen"}</p>
            <p className="feed-user">{coffee.nombre}</p>
            <p className="coffee-sub">{coffee.marca}</p>
          </button>
        ))}
      </div>
    </section>
  ) : null;

  const userSuggestionsSection = suggestions.length ? (
    <section className="suggestion-strip">
      <p className="section-title">Personas que podrias seguir</p>
      <div className="horizontal-cards">
        {suggestions.map((user) => {
          const isDismissing = dismissingSuggestionIds.has(user.id);
          const isPendingFollow = pendingSuggestionFollowIds.has(user.id);
          return (
            <article key={user.id} className={`mini-card mini-user-card ${isDismissing ? "is-removing" : ""}`.trim()}>
              <button type="button" className="mini-user-link" onClick={() => onOpenUserProfile(user.id)}>
                {user.avatar_url ? <img className="mini-avatar" src={user.avatar_url} alt={user.username} loading="lazy" /> : <div className="avatar mini-avatar-fallback">{user.username.slice(0, 2).toUpperCase()}</div>}
                <div className="mini-user-copy">
                  <p className="feed-user">{user.full_name}</p>
                  <p className="feed-meta">@{user.username}</p>
                  <p className="coffee-sub">{followerCounts.get(user.id) ?? 0} seguidores</p>
                </div>
              </button>
              <button
                className={`action-button ${followingIds.has(user.id) ? "action-button-following" : "action-button-ghost"}`}
                type="button"
                disabled={isPendingFollow}
                onClick={async () => {
                  if (isPendingFollow) return;
                  setPendingSuggestionFollowIds((prev) => new Set(prev).add(user.id));
                  setDismissingSuggestionIds((prev) => new Set(prev).add(user.id));
                  await new Promise((resolve) => window.setTimeout(resolve, 190));
                  await onToggleFollow(user.id);
                  setPendingSuggestionFollowIds((prev) => {
                    const next = new Set(prev);
                    next.delete(user.id);
                    return next;
                  });
                  setDismissingSuggestionIds((prev) => {
                    const next = new Set(prev);
                    next.delete(user.id);
                    return next;
                  });
                }}
              >
                {followingIds.has(user.id) ? "Siguiendo" : "Seguir"}
              </button>
            </article>
          );
        })}
      </div>
    </section>
  ) : null;

  const desktopSideContent = sidePanel ? (
    <aside className="timeline-side-column">{sidePanel}</aside>
  ) : (userSuggestionsSection || recommendationSection) ? (
    <aside className="timeline-side-column">
      {userSuggestionsSection}
      {recommendationSection}
    </aside>
  ) : null;

  if (!cards.length) {
    if (isDesktopTimeline) {
      return (
        <div className="timeline-shell timeline-shell-desktop">
          <div className="timeline-desktop-columns">
            <div className="timeline-main-column">
              <article className="timeline-empty">
                <h3>Tu timeline esta vacio</h3>
                <p>Empieza siguiendo personas o publicando tu primer cafe.</p>
                <button className="action-button" type="button">
                  Publica tu primer cafe
                </button>
              </article>
            </div>
            {desktopSideContent}
          </div>
        </div>
      );
    }
    return (
      <>
        <article className="timeline-empty">
          <h3>Tu timeline esta vacio</h3>
          <p>Empieza siguiendo personas o publicando tu primer cafe.</p>
          <button className="action-button" type="button">
            Publica tu primer cafe
          </button>
        </article>
        {userSuggestionsSection}
        {recommendationSection}
      </>
    );
  }

  return (
    <div
      className={`timeline-shell ${isDesktopTimeline ? "timeline-shell-desktop" : ""}`.trim()}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
    >
      <div
        className={`pull-indicator ${pullDistance > 2 ? "is-visible" : ""}`}
        style={{ ["--pull-distance" as string]: `${pullDistance}px`, ["--pull-progress" as string]: `${pullProgress}` }}
        aria-hidden="true"
      >
        <span className={`pull-dot ${pullProgress >= 1 ? "is-ready" : ""}`} />
      </div>

      {visibleCards.length ? (
        <div className="timeline-desktop-columns">
          <div className="timeline-main-column">
            <ul className="feed-list">
              {visibleCards.map((card, index) => (
                <Fragment key={card.id}>
                  {!isDesktopTimeline && recommendationSection && suggestionIndices[0] != null && index === suggestionIndices[0] ? (
                    <li className="feed-inline-item">{recommendationSection}</li>
                  ) : null}
                  {!isDesktopTimeline && userSuggestionsSection && suggestionIndices[1] != null && index === suggestionIndices[1] ? (
                    <li className="feed-inline-item">{userSuggestionsSection}</li>
                  ) : null}
                  <li className="feed-card feed-card-premium feed-entry" style={{ ["--feed-index" as string]: index }}>
                    <article>
              <header className="feed-head">
                <button
                  type="button"
                  className="feed-user-link"
                  onClick={() => onOpenUserProfile(card.userId)}
                >
                  {card.avatarUrl ? (
                    <img className="avatar avatar-photo" src={card.avatarUrl} alt={card.username} loading="lazy" />
                  ) : (
                    <div className="avatar" aria-hidden="true">
                      {card.userName
                        .split(" ")
                        .map((part) => part[0])
                        .join("")
                        .slice(0, 2)
                        .toUpperCase()}
                    </div>
                  )}
                  <div>
                    <p className="feed-user">{card.userName}</p>
                    <p className="feed-meta">{card.minsAgoLabel.toUpperCase()}</p>
                  </div>
                </button>
                {activeUserId === card.userId ? (
                  <button
                    type="button"
                    className="icon-button post-menu-trigger"
                    onClick={() => setMenuPostId(card.id)}
                    aria-label="Opciones"
                  >
                    <UiIcon name="more" className="ui-icon" />
                  </button>
                ) : null}
              </header>

              {card.text ? (
                <p className="feed-text">
                  <MentionText text={card.text} onMentionClick={onMentionClick} />
                </p>
              ) : null}

              {card.imageUrl ? <img className={`feed-image ${card.text ? "" : "feed-image-no-text"}`.trim()} src={card.imageUrl} alt="Publicacion" loading="lazy" /> : null}

              {card.coffeeTagName ? (
                <button type="button" className="coffee-tag-card" onClick={() => card.coffeeId && onOpenCoffee(card.coffeeId)} disabled={!card.coffeeId}>
                  <div className="coffee-tag-card-media">
                    {card.coffeeImageUrl ? (
                      <img className="coffee-tag-image" src={card.coffeeImageUrl} alt={card.coffeeTagName} loading="lazy" />
                    ) : (
                      <div className="coffee-tag-image coffee-tag-image-fallback" aria-hidden="true">
                        <UiIcon name="coffee" className="ui-icon" />
                      </div>
                    )}
                  </div>
                  <div className="coffee-tag-copy">
                    <p className="coffee-origin">CAFE ETIQUETADO</p>
                    <p className="coffee-tag-name">{card.coffeeTagName}</p>
                    {card.coffeeTagBrand ? <p className="coffee-tag-brand">{card.coffeeTagBrand.toUpperCase()}</p> : null}
                  </div>
                  <UiIcon name="chevron-right" className="ui-icon" />
                </button>
              ) : null}

              <footer className="feed-stats">
                <button
                  type="button"
                  className={`inline-action action-like ${card.likedByActiveUser ? "is-liked" : ""} ${likeBurstPostId === card.id ? "is-bursting" : ""}`}
                  onClick={() => {
                    if (!card.likedByActiveUser) {
                      setLikeBurstPostId(card.id);
                      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
                      likeBurstTimerRef.current = window.setTimeout(() => {
                        setLikeBurstPostId(null);
                        likeBurstTimerRef.current = null;
                      }, 520);
                    }
                    onToggleLike(card.id);
                  }}
                >
                  <span className="like-icon-wrap">
                    <UiIcon name={card.likedByActiveUser ? "coffee-filled" : "coffee"} className="ui-icon" />
                    <span className="like-burst" aria-hidden="true">
                      <span />
                      <span />
                      <span />
                      <span />
                      <span />
                      <span />
                    </span>
                  </span>
                  {card.likes > 0 ? <span>{card.likes}</span> : null}
                </button>
                <button type="button" className="inline-action" onClick={() => onOpenComments(card.id)}>
                  <UiIcon name="chat" className="ui-icon" />
                  {card.comments > 0 ? <span>{card.comments}</span> : null}
                </button>
              </footer>
                    </article>
                  </li>
                </Fragment>
              ))}
            </ul>
          </div>
          {isDesktopTimeline ? desktopSideContent : null}
        </div>
      ) : (
        <article className="timeline-empty">
          <h3>No hay publicaciones disponibles</h3>
          <p>Publica tu primer cafe o sigue a mas personas.</p>
        </article>
      )}
      {activeMenuPost ? (
        <div className="sheet-overlay comment-action-overlay" onClick={() => setMenuPostId(null)}>
          <div className="sheet-card comment-action-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <div className="comment-action-list">
              <p className="comment-action-title">OPCIONES</p>
              <button
                type="button"
                className="comment-action-button"
                onClick={() => {
                  setEditingPostId(activeMenuPost.id);
                  setEditingText(activeMenuPost.text);
                  const initialImage = activeMenuPost.imageUrl ?? "";
                  setEditingImageUrl(initialImage);
                  setEditingImageFile(null);
                  setEditingImagePreviewUrl(initialImage);
                  setMenuPostId(null);
                }}
              >
                <UiIcon name="edit" className="ui-icon" />
                <span>Editar</span>
                <UiIcon name="chevron-right" className="ui-icon trailing" />
              </button>
              <button
                type="button"
                className="comment-action-button is-danger"
                onClick={async () => {
                  const confirmed = window.confirm("Borrar post definitivamente?");
                  if (!confirmed) return;
                  await onDeletePost(activeMenuPost.id);
                  setMenuPostId(null);
                }}
              >
                <UiIcon name="trash" className="ui-icon" />
                <span>Borrar</span>
                <UiIcon name="chevron-right" className="ui-icon trailing" />
              </button>
            </div>
          </div>
        </div>
      ) : null}
      {editingPostId ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Editar publicacion"
          onClick={closeEditModal}
        >
          <div className="sheet-card" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <div className="create-post-body edit-post-sheet">
              <h3 className="edit-post-title">Editar</h3>
              <input
                ref={editImageInputRef}
                type="file"
                accept="image/*"
                className="file-input-hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  if (!file) return;
                  if (editingImagePreviewUrl.startsWith("blob:")) URL.revokeObjectURL(editingImagePreviewUrl);
                  const preview = URL.createObjectURL(file);
                  setEditingImageFile(file);
                  setEditingImagePreviewUrl(preview);
                }}
              />
              <button
                type="button"
                className="edit-image-picker"
                onClick={() => editImageInputRef.current?.click()}
              >
                {editingImagePreviewUrl.trim() ? (
                  <img className="create-post-preview" src={editingImagePreviewUrl.trim()} alt="Previsualizacion" loading="lazy" />
                ) : (
                  <span className="edit-image-placeholder">Seleccionar imagen</span>
                )}
              </button>
              <textarea
                className="search-wide sheet-input"
                placeholder="Descripción"
                value={editingText}
                rows={4}
                onChange={(event) => setEditingText(event.target.value)}
              />
              <div className="create-post-actions edit-post-actions-native">
                <button
                  type="button"
                  className="action-button action-button-ghost edit-post-cancel"
                  onClick={closeEditModal}
                >
                  CANCELAR
                </button>
                <button
                  type="button"
                  className="action-button edit-post-save"
                  disabled={!editingText.trim() && !editingImageUrl.trim() && !editingImageFile}
                  onClick={async () => {
                    await onEditPost(editingPostId, editingText.trim(), editingImageUrl.trim(), editingImageFile);
                    closeEditModal();
                  }}
                >
                  GUARDAR
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function SearchView({
  mode,
  searchQuery,
  onSearchQueryChange,
  searchFilter,
  onSearchFilterChange,
  selectedOrigins,
  selectedRoasts,
  selectedSpecialties,
  selectedFormats,
  minRating,
  activeFilterType,
  onSetActiveFilterType,
  originOptions,
  roastOptions,
  specialtyOptions,
  formatOptions,
  onToggleOrigin,
  onToggleRoast,
  onToggleSpecialty,
  onToggleFormat,
  onSetMinRating,
  onClearCoffeeFilters,
  coffees,
  users,
  followingIds,
  selectedCoffee,
  onSelectCoffee,
  onSelectUser,
  onToggleFollow,
  focusCoffeeProfile,
  onExitCoffeeFocus,
  sidePanel
}: {
  mode: "coffees" | "users";
  searchQuery: string;
  onSearchQueryChange: (value: string) => void;
  searchFilter: "todo" | "origen" | "nombre";
  onSearchFilterChange: (value: "todo" | "origen" | "nombre") => void;
  selectedOrigins: Set<string>;
  selectedRoasts: Set<string>;
  selectedSpecialties: Set<string>;
  selectedFormats: Set<string>;
  minRating: number;
  activeFilterType: "origen" | "tueste" | "especialidad" | "formato" | "nota" | null;
  onSetActiveFilterType: (filter: "origen" | "tueste" | "especialidad" | "formato" | "nota" | null) => void;
  originOptions: string[];
  roastOptions: string[];
  specialtyOptions: string[];
  formatOptions: string[];
  onToggleOrigin: (value: string) => void;
  onToggleRoast: (value: string) => void;
  onToggleSpecialty: (value: string) => void;
  onToggleFormat: (value: string) => void;
  onSetMinRating: (value: number) => void;
  onClearCoffeeFilters: () => void;
  coffees: CoffeeRow[];
  users: UserRow[];
  followingIds: Set<number>;
  selectedCoffee: CoffeeRow | null;
  onSelectCoffee: (coffeeId: string) => void;
  onSelectUser: (userId: number) => void;
  onToggleFollow: (targetUserId: number) => void;
  focusCoffeeProfile: boolean;
  onExitCoffeeFocus: () => void;
  sidePanel?: ReactNode;
}) {
  const selectedCoffeeRef = useRef<HTMLElement | null>(null);
  const [recentSearches, setRecentSearches] = useState<string[]>(() => {
    if (typeof window === "undefined") return [];
    try {
      const raw = window.localStorage.getItem("cafesito:search:recent");
      if (!raw) return [];
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed.filter((item): item is string => typeof item === "string").slice(0, 10) : [];
    } catch {
      return [];
    }
  });

  const saveRecentSearch = useCallback((term: string) => {
    const cleaned = term.trim();
    if (!cleaned) return;
    setRecentSearches((prev) => [cleaned, ...prev.filter((item) => item.toLowerCase() !== cleaned.toLowerCase())].slice(0, 10));
  }, []);

  const clearRecentSearches = useCallback(() => setRecentSearches([]), []);

  useEffect(() => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem("cafesito:search:recent", JSON.stringify(recentSearches));
    } catch {
      // noop
    }
  }, [recentSearches]);

  useEffect(() => {
    if (!focusCoffeeProfile || !selectedCoffeeRef.current) return;
    selectedCoffeeRef.current.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [focusCoffeeProfile, selectedCoffee?.id]);

  const content = (
    <>
      {mode === "users" ? (
        <ul className="search-users-list">
          {users.length ? (
            users.map((user) => (
              <li key={user.id} className="search-users-row">
                <button type="button" className="search-users-link" onClick={() => onSelectUser(user.id)}>
                  {user.avatar_url ? (
                    <img className="avatar avatar-photo search-users-avatar" src={user.avatar_url} alt={user.username} loading="lazy" />
                  ) : (
                    <div className="avatar search-users-avatar-fallback" aria-hidden="true">
                      {user.username.slice(0, 2).toUpperCase()}
                    </div>
                  )}
                  <div className="search-users-copy">
                    <p className="search-users-username">{user.username}</p>
                    <p className="search-users-fullname">{user.full_name}</p>
                  </div>
                </button>
                <button
                  type="button"
                  className={`action-button search-users-follow ${followingIds.has(user.id) ? "action-button-following" : "action-button-ghost"}`}
                  onClick={() => onToggleFollow(user.id)}
                >
                  {followingIds.has(user.id) ? "Siguiendo" : "Seguir"}
                </button>
              </li>
            ))
          ) : (
            <li className="search-users-empty">
              {searchQuery.trim() ? "No se encontraron usuarios" : "Busca amigos para seguir"}
            </li>
          )}
        </ul>
      ) : null}

      {mode === "coffees" ? (
        <div className="search-coffee-view">
          {!focusCoffeeProfile && recentSearches.length ? (
            <section className="search-recent">
              <div className="search-recent-head">
                <p className="search-recent-title">Busquedas recientes</p>
                <button type="button" className="text-button search-recent-clear" onClick={clearRecentSearches}>
                  Limpiar
                </button>
              </div>
              <div className="search-recent-list">
                {recentSearches.map((term) => (
                  <button
                    key={term}
                    type="button"
                    className="search-recent-chip"
                    onClick={() => {
                      onSearchQueryChange(term);
                      saveRecentSearch(term);
                    }}
                  >
                    {term}
                  </button>
                ))}
              </div>
            </section>
          ) : null}

          {selectedCoffee && focusCoffeeProfile ? (
            <article className={`coffee-profile-card ${focusCoffeeProfile ? "is-focused" : ""}`} ref={selectedCoffeeRef}>
              {focusCoffeeProfile ? (
                <div className="coffee-profile-head">
                  <p className="coffee-profile-badge">PERFIL DE CAFE</p>
                  <button type="button" className="text-button" onClick={onExitCoffeeFocus}>
                    Ver todos
                  </button>
                </div>
              ) : null}
              {selectedCoffee.image_url ? <img className="coffee-profile-image" src={selectedCoffee.image_url} alt={selectedCoffee.nombre} loading="lazy" /> : null}
              <div className="coffee-profile-copy">
                <p className="coffee-origin">{selectedCoffee.pais_origen ?? "Origen desconocido"}</p>
                <h3 className="coffee-profile-title">{selectedCoffee.nombre}</h3>
                <p className="coffee-profile-brand">{selectedCoffee.marca || "Marca"}</p>
              </div>
            </article>
          ) : null}

          <ul className="coffee-list">
            {coffees.map((coffee) => (
              <li key={coffee.id}>
                <button
                  type="button"
                  className={`coffee-card coffee-card-row coffee-card-interactive ${selectedCoffee?.id === coffee.id ? "is-selected" : ""}`}
                  onClick={() => {
                    if (searchQuery.trim()) saveRecentSearch(searchQuery);
                    onSelectCoffee(coffee.id);
                  }}
                >
                  {coffee.image_url ? (
                    <img className="search-coffee-thumb" src={coffee.image_url} alt={coffee.nombre} loading="lazy" />
                  ) : (
                    <div className="search-coffee-thumb search-coffee-thumb-fallback" aria-hidden="true">
                      <UiIcon name="coffee" className="ui-icon" />
                    </div>
                  )}
                  <div className="search-coffee-copy">
                    <strong>{coffee.nombre}</strong>
                    <p className="coffee-sub">{coffee.marca || "Marca"}</p>
                    <p className="coffee-origin">{coffee.pais_origen ?? "Origen desconocido"}</p>
                  </div>
                  <UiIcon name="chevron-right" className="ui-icon search-coffee-chevron" />
                </button>
              </li>
            ))}
          </ul>
          {!coffees.length ? <p className="search-coffee-empty">No encontramos cafes con esos filtros.</p> : null}

          {activeFilterType ? (
            <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Filtros" onClick={() => onSetActiveFilterType(null)}>
              <div className="sheet-card search-filter-sheet" onClick={(event) => event.stopPropagation()}>
                <div className="sheet-handle" aria-hidden="true" />
                <header className="sheet-header">
                  <strong className="sheet-title">
                    {activeFilterType === "origen"
                      ? "FILTRAR POR PAIS"
                      : activeFilterType === "tueste"
                        ? "FILTRAR POR TUESTE"
                        : activeFilterType === "especialidad"
                          ? "FILTRAR POR ESPECIALIDAD"
                          : activeFilterType === "formato"
                            ? "FILTRAR POR FORMATO"
                            : "FILTRAR POR NOTA"}
                  </strong>
                </header>
                <div className="search-filter-actions">
                  <button type="button" className="text-button" onClick={onClearCoffeeFilters}>
                    Limpiar filtros
                  </button>
                </div>
                {activeFilterType === "nota" ? (
                  <div className="search-rating-filter">
                    <p className="search-rating-label">{minRating > 0 ? `Nota minima: ${minRating}+` : "Cualquier nota"}</p>
                    <input
                      type="range"
                      min={0}
                      max={5}
                      step={1}
                      value={minRating}
                      onChange={(event) => onSetMinRating(Number(event.target.value))}
                    />
                    <div className="search-rating-scale">
                      <span>0</span>
                      <span>5</span>
                    </div>
                  </div>
                ) : (
                  <ul className="search-filter-list">
                    {(activeFilterType === "origen"
                      ? originOptions
                      : activeFilterType === "tueste"
                        ? roastOptions
                        : activeFilterType === "especialidad"
                          ? specialtyOptions
                          : formatOptions).map((option) => {
                      const checked =
                        activeFilterType === "origen"
                          ? selectedOrigins.has(option)
                          : activeFilterType === "tueste"
                            ? selectedRoasts.has(option)
                            : activeFilterType === "especialidad"
                              ? selectedSpecialties.has(option)
                              : selectedFormats.has(option);
                      return (
                        <li key={option}>
                          <button
                            type="button"
                            className={`search-filter-item ${checked ? "is-selected" : ""}`.trim()}
                            onClick={() => {
                              if (activeFilterType === "origen") onToggleOrigin(option);
                              else if (activeFilterType === "tueste") onToggleRoast(option);
                              else if (activeFilterType === "especialidad") onToggleSpecialty(option);
                              else onToggleFormat(option);
                            }}
                          >
                            <input type="checkbox" readOnly checked={checked} aria-hidden="true" tabIndex={-1} />
                            <span>{option}</span>
                          </button>
                        </li>
                      );
                    })}
                  </ul>
                )}
              </div>
            </div>
          ) : null}
        </div>
      ) : null}
    </>
  );

  if (sidePanel) {
    return (
      <div className="split-with-side profile-split-with-side">
        <div className="profile-content-wrap">{content}</div>
        <aside className="timeline-side-column">{sidePanel}</aside>
      </div>
    );
  }

  return content;
}

function CoffeeDetailView({
  coffee,
  reviews,
  currentUser,
  currentUserReview,
  avgRating,
  isFavorite,
  pantry,
  sensory,
  sensoryDraft,
  stockDraft,
  reviewDraftText,
  reviewDraftRating,
  reviewDraftImagePreviewUrl,
  onClose,
  onToggleFavorite,
  onReviewTextChange,
  onReviewRatingChange,
  onReviewImagePick,
  onSaveReview,
  onDeleteReview,
  canDeleteReview,
  onSensoryDraftChange,
  onSaveSensory,
  onStockDraftChange,
  onSaveStock,
  onOpenUserProfile,
  isGuest,
  onRequireAuth,
  fullPage,
  externalOpenStockSignal
}: {
  coffee: CoffeeRow;
  reviews: Array<CoffeeReviewRow & { user: UserRow | null }>;
  currentUser: UserRow | null;
  currentUserReview: (CoffeeReviewRow & { user: UserRow | null }) | null;
  avgRating: number;
  isFavorite: boolean;
  pantry: PantryItemRow | null;
  sensory: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
  sensoryDraft: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
  stockDraft: { total: number; remaining: number };
  reviewDraftText: string;
  reviewDraftRating: number;
  reviewDraftImagePreviewUrl: string;
  onClose: () => void;
  onToggleFavorite: () => void;
  onReviewTextChange: (value: string) => void;
  onReviewRatingChange: (value: number) => void;
  onReviewImagePick: (file: File | null, previewUrl: string) => void;
  onSaveReview: () => Promise<void>;
  onDeleteReview: () => Promise<void>;
  canDeleteReview: boolean;
  onSensoryDraftChange: (value: { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number }) => void;
  onSaveSensory: () => Promise<void>;
  onStockDraftChange: (value: { total: number; remaining: number }) => void;
  onSaveStock: () => Promise<void>;
  onOpenUserProfile: (userId: number) => void;
  isGuest: boolean;
  onRequireAuth: () => void;
  fullPage: boolean;
  externalOpenStockSignal: number;
}) {
  const [showSensorySheet, setShowSensorySheet] = useState(false);
  const [showStockSheet, setShowStockSheet] = useState(false);
  const [showReviewSheet, setShowReviewSheet] = useState(false);
  const [reviewSheetError, setReviewSheetError] = useState<string | null>(null);
  const [stockSheetError, setStockSheetError] = useState<string | null>(null);
  const [sensorySheetError, setSensorySheetError] = useState<string | null>(null);
  const [savingSensory, setSavingSensory] = useState(false);
  const [savingStock, setSavingStock] = useState(false);
  const [savingReview, setSavingReview] = useState(false);
  const [deletingReview, setDeletingReview] = useState(false);

  const sensoryKeys: Array<keyof typeof sensoryDraft> = ["aroma", "sabor", "cuerpo", "acidez", "dulzura"];
  const sensoryLabels: Record<keyof typeof sensoryDraft, string> = {
    aroma: "Aroma",
    sabor: "Sabor",
    cuerpo: "Cuerpo",
    acidez: "Acidez",
    dulzura: "Dulzura"
  };
  const techRowsBase: Array<{ icon: IconName; label: string; value: string }> = [
    { icon: "origin", label: "Pais", value: coffee.pais_origen ?? "" },
    { icon: "specialty", label: "Especialidad", value: coffee.especialidad ?? "" },
    { icon: "variety", label: "Variedad", value: coffee.variedad_tipo ?? "" },
    { icon: "roast", label: "Tueste", value: coffee.tueste ?? "" },
    { icon: "process", label: "Proceso", value: coffee.proceso ?? "" },
    { icon: "grind", label: "Molienda", value: coffee.molienda_recomendada ?? "" }
  ];
  const techRows = techRowsBase.filter((row) => row.value.trim().length > 0);
  const otherReviews = reviews.filter((review) => !currentUser || review.user_id !== currentUser.id);
  const stockTotal = Number.isFinite(stockDraft.total) ? Math.max(0, stockDraft.total) : 0;
  const stockRemaining = Number.isFinite(stockDraft.remaining) ? Math.max(0, stockDraft.remaining) : 0;
  const isStockDraftInvalid = stockRemaining > stockTotal;
  const isSensoryDirty = sensoryKeys.some((key) => Math.abs(sensoryDraft[key] - sensory[key]) > 0.001);
  const pantryTotal = Math.max(0, Number(pantry?.total_grams ?? 0));
  const pantryRemaining = Math.min(pantryTotal, Math.max(0, Number(pantry?.grams_remaining ?? 0)));
  const isStockDirty = stockTotal !== pantryTotal || stockRemaining !== pantryRemaining;
  const canSaveStock = !isStockDraftInvalid && isStockDirty;
  const baseReviewText = (currentUserReview?.comment ?? "").trim();
  const draftReviewText = reviewDraftText.trim();
  const baseReviewRating = Number(currentUserReview?.rating ?? 0);
  const baseReviewImage = currentUserReview?.image_url ?? "";
  const draftReviewImage = reviewDraftImagePreviewUrl ?? "";
  const isReviewDirty =
    draftReviewText !== baseReviewText ||
    Math.abs(reviewDraftRating - baseReviewRating) > 0.001 ||
    draftReviewImage !== baseReviewImage;
  const canSaveReview = reviewDraftRating > 0 && isReviewDirty;
  const sensoryEditorsCount = new Set(reviews.map((review) => review.user_id).filter((value): value is number => typeof value === "number")).size;
  const hasAnyOpinions = Boolean(currentUserReview) || otherReviews.length > 0;
  const acquireLabel = useMemo(() => {
    if (!coffee.product_url) return "";
    try {
      const host = new URL(coffee.product_url).hostname.replace(/^www\./i, "");
      return host.toUpperCase();
    } catch {
      return "ADQUIRIR";
    }
  }, [coffee.product_url]);

  useEffect(() => {
    if (!fullPage || externalOpenStockSignal <= 0) return;
    setStockSheetError(null);
    setShowStockSheet(true);
  }, [externalOpenStockSignal, fullPage]);

  return (
    <article className={`coffee-detail ${fullPage ? "is-full-page" : "is-side-panel"}`.trim()}>
      <header className="coffee-detail-hero">
        {coffee.image_url ? <img className="coffee-detail-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" /> : null}
        <div className="coffee-detail-overlay" />
        {!fullPage ? (
          <div className="coffee-detail-hero-top-actions">
            <button type="button" className="icon-button topbar-icon-button coffee-detail-topbar-icon" aria-label="Cerrar detalle" onClick={onClose}>
              <UiIcon name="close" className="ui-icon" />
            </button>
            <div className="coffee-detail-topbar-actions">
              <button
                type="button"
                className={`icon-button topbar-icon-button coffee-detail-topbar-icon ${isFavorite ? "is-active" : ""}`.trim()}
                aria-label={isFavorite ? "Quitar de favoritos" : "Guardar en favoritos"}
                onClick={onToggleFavorite}
              >
                <UiIcon name={isFavorite ? "favorite-filled" : "favorite"} className="ui-icon" />
              </button>
              <button
                type="button"
                className={`icon-button topbar-icon-button coffee-detail-topbar-icon ${pantry ? "is-active" : ""}`.trim()}
                aria-label="Añadir a stock"
                onClick={() => {
                  if (isGuest) {
                    onRequireAuth();
                    return;
                  }
                  setStockSheetError(null);
                  setShowStockSheet(true);
                }}
              >
                <UiIcon name="stock" className="ui-icon" />
              </button>
            </div>
          </div>
        ) : null}

        <div className="coffee-detail-headline">
          <p className="coffee-origin">{coffee.marca ?? "Marca"}</p>
          <h2 className="coffee-detail-title">{coffee.nombre}</h2>
        </div>
        {avgRating > 0 ? (
          <span className="coffee-detail-rating-badge">
            <UiIcon name="star" className="ui-icon" />
            <strong>{avgRating.toFixed(1)}</strong>
          </span>
        ) : null}
      </header>

      <section className="coffee-detail-section coffee-detail-section-first">
        {coffee.descripcion ? <p className="feed-text coffee-detail-description">{coffee.descripcion}</p> : <p className="coffee-sub coffee-detail-opinion-empty">Sin descripción.</p>}
      </section>

      <section className="coffee-detail-section">
        <h3 className="section-title">Detalles técnicos</h3>
        <ul className="coffee-detail-tech-list">
          {techRows.map((row) => (
            <li key={row.label} className="coffee-detail-tech-item">
              <span className="coffee-detail-tech-icon-wrap" aria-hidden="true">
                <UiIcon name={row.icon} className="ui-icon coffee-detail-tech-icon" />
              </span>
              <span className="coffee-detail-tech-copy">
                <strong>{row.label}</strong>
                <em>{row.value}</em>
              </span>
            </li>
          ))}
        </ul>
      </section>

      <section className="coffee-detail-section">
        <div className="coffee-detail-section-head">
          <h3 className="section-title">Perfil sensorial</h3>
          <button
            type="button"
            className="text-button coffee-detail-inline-action"
            onClick={() => {
              if (isGuest) {
                onRequireAuth();
                return;
              }
              setSensorySheetError(null);
              setShowSensorySheet(true);
            }}
          >
            Editar
          </button>
        </div>
        {sensoryEditorsCount > 0 ? (
          <p className="coffee-detail-sensory-note">
            Basado en los comentarios de {sensoryEditorsCount} usuarios. {sensoryEditorsCount} son las personas que lo han editado.
          </p>
        ) : null}
        <div className="coffee-detail-sensory-summary">
          {sensoryKeys.map((key) => (
            <div key={key} className="coffee-detail-sensory-row">
              <div className="coffee-detail-sensory-meta">
                <span>{sensoryLabels[key].toUpperCase()}</span>
                <strong>{`${sensory[key].toFixed(1).replace(".", ",")}/10`}</strong>
              </div>
              <div className="coffee-detail-sensory-track" aria-hidden="true">
                <div className="coffee-detail-sensory-fill" style={{ width: `${Math.max(0, Math.min(100, (sensory[key] / 10) * 100))}%` }} />
              </div>
            </div>
          ))}
        </div>
      </section>

      {coffee.product_url ? (
        <section className="coffee-detail-section">
          <div className="coffee-detail-acquire">
            <h3 className="section-title">Adquirir</h3>
            <a className="coffee-detail-acquire-row" href={coffee.product_url} target="_blank" rel="noreferrer">
              <span className="coffee-detail-acquire-main">
                <UiIcon name="shop" className="ui-icon coffee-detail-acquire-icon" />
                <strong>{acquireLabel}</strong>
              </span>
              <UiIcon name="chevron-right" className="ui-icon coffee-detail-acquire-chevron" />
            </a>
          </div>
        </section>
      ) : null}

      <section className="coffee-detail-section coffee-detail-opinions-section">
        <div className="coffee-detail-section-head">
          <h3 className="section-title">Opiniones</h3>
          <button
            type="button"
            className="coffee-detail-opinions-cta"
            onClick={() => {
              if (isGuest) {
                onRequireAuth();
                return;
              }
              setReviewSheetError(null);
              setShowReviewSheet(true);
            }}
          >
            {currentUserReview ? "EDITAR" : "+ AÑADIR"}
          </button>
        </div>
        {!hasAnyOpinions ? (
          <p className="coffee-detail-opinions-empty">No hay opiniones aún. ¡Sé el primero!</p>
        ) : null}
        {currentUserReview ? (
          <article className="coffee-detail-opinion-preview">
            <p className="coffee-detail-opinion-label">Tu opinión</p>
            <div className="coffee-detail-opinion-head">
              {currentUser?.id ? (
                <button
                  type="button"
                  className="coffee-detail-opinion-user-link"
                  onClick={() => {
                    if (isGuest) {
                      onRequireAuth();
                      return;
                    }
                    onOpenUserProfile(currentUser.id);
                  }}
                >
                  <span className="coffee-detail-opinion-user">
                    {currentUser.avatar_url ? (
                      <img className="coffee-detail-opinion-avatar" src={currentUser.avatar_url} alt={currentUser.username} loading="lazy" />
                    ) : (
                      <span className="coffee-detail-opinion-avatar" aria-hidden="true">
                        {(currentUser.username ?? "tu").slice(0, 2).toUpperCase()}
                      </span>
                    )}
                    <span className="coffee-detail-opinion-copy">
                      <span className="feed-user">@{currentUser.username}</span>
                      <span className="feed-meta">{toRelativeMinutes(currentUserReview.timestamp ?? 0)}</span>
                    </span>
                  </span>
                </button>
              ) : (
                <div className="coffee-detail-opinion-user">
                  <div className="coffee-detail-opinion-avatar" aria-hidden="true">TU</div>
                  <div className="coffee-detail-opinion-copy">
                    <p className="feed-user">@tu_usuario</p>
                    <p className="feed-meta">{toRelativeMinutes(currentUserReview.timestamp ?? 0)}</p>
                  </div>
                </div>
              )}
              <p className="feed-meta coffee-detail-opinion-rating"><UiIcon name="star" className="ui-icon" />{currentUserReview.rating.toFixed(1)} / 5</p>
            </div>
            {currentUserReview.comment ? <p className="feed-text">{currentUserReview.comment}</p> : null}
            {currentUserReview.image_url ? <img className="coffee-detail-review-image" src={currentUserReview.image_url} alt="Tu reseña" loading="lazy" /> : null}
          </article>
        ) : null}
        <ul className="coffee-list">
          {otherReviews.map((review) => (
            <li key={`${review.user_id}-${review.id ?? review.timestamp ?? 0}`} className="coffee-card coffee-detail-opinion-item">
              <div className="coffee-detail-opinion-head">
                {review.user?.id ? (
                  <button
                    type="button"
                    className="coffee-detail-opinion-user-link"
                    onClick={() => {
                      if (isGuest) {
                        onRequireAuth();
                        return;
                      }
                      onOpenUserProfile(review.user!.id);
                    }}
                  >
                    <span className="coffee-detail-opinion-user">
                      {review.user.avatar_url ? (
                        <img className="coffee-detail-opinion-avatar" src={review.user.avatar_url} alt={review.user.username} loading="lazy" />
                      ) : (
                        <span className="coffee-detail-opinion-avatar" aria-hidden="true">
                          {(review.user.username ?? "us").slice(0, 2).toUpperCase()}
                        </span>
                      )}
                      <span className="coffee-detail-opinion-copy">
                        <span className="feed-user">@{review.user.username}</span>
                        <span className="feed-meta">{toRelativeMinutes(review.timestamp ?? 0)}</span>
                      </span>
                    </span>
                  </button>
                ) : (
                  <div className="coffee-detail-opinion-user">
                    <div className="coffee-detail-opinion-avatar" aria-hidden="true">US</div>
                    <div className="coffee-detail-opinion-copy">
                      <p className="feed-user">@usuario</p>
                      <p className="feed-meta">{toRelativeMinutes(review.timestamp ?? 0)}</p>
                    </div>
                  </div>
                )}
                <p className="feed-meta coffee-detail-opinion-rating"><UiIcon name="star" className="ui-icon" />{review.rating.toFixed(1)} / 5</p>
              </div>
              {review.comment ? <p className="feed-text">{review.comment}</p> : null}
              {review.image_url ? <img className="coffee-detail-review-image" src={review.image_url} alt="Imagen reseña" loading="lazy" /> : null}
            </li>
          ))}
        </ul>
      </section>

      {showSensorySheet ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Editar perfil sensorial"
          onClick={() => {
            if (savingSensory) return;
            setSensorySheetError(null);
            setShowSensorySheet(false);
          }}
        >
          <div className="sheet-card coffee-detail-sheet coffee-detail-sensory-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="coffee-detail-sensory-sheet-head">
              <h3 className="coffee-detail-sensory-sheet-title">Perfil sensorial</h3>
              <p className="coffee-detail-sensory-sheet-copy">Tu opinión se unirá a la media de todas las valoraciones</p>
            </header>
            <div className="coffee-detail-sheet-body coffee-detail-sliders coffee-detail-sensory-sliders">
              {sensoryKeys.map((key) => (
                <label key={key} className="coffee-detail-sensory-control">
                  <span className="coffee-detail-slider-label">
                    {sensoryLabels[key]}
                    <strong>{sensoryDraft[key].toFixed(1).replace(".", ",")}</strong>
                  </span>
                  <span className="coffee-detail-sensory-slider-row">
                    <small>0</small>
                    <input
                      style={{ "--sensory-progress": `${Math.max(0, Math.min(100, (sensoryDraft[key] / 10) * 100))}%` } as CSSProperties}
                      type="range"
                      min={0}
                      max={10}
                      step={0.5}
                      value={sensoryDraft[key]}
                      onChange={(event) => onSensoryDraftChange({ ...sensoryDraft, [key]: Number(event.target.value) })}
                    />
                    <small>10</small>
                  </span>
                </label>
              ))}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions">
              <button
                type="button"
                className="action-button coffee-detail-sensory-submit"
                disabled={savingSensory}
                onClick={async () => {
                  setSavingSensory(true);
                  try {
                    await onSaveSensory();
                    setSensorySheetError(null);
                    setShowSensorySheet(false);
                  } catch {
                    setSensorySheetError("No se pudo guardar el perfil sensorial.");
                  } finally {
                    setSavingSensory(false);
                  }
                }}
              >
                {savingSensory ? "Guardando..." : "Listo"}
              </button>
            </div>
            {sensorySheetError ? <p className="coffee-detail-sheet-error">{sensorySheetError}</p> : null}
          </div>
        </div>
      ) : null}

      {showStockSheet ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Editar stock"
          onClick={() => {
            if (savingStock) return;
            setStockSheetError(null);
            setShowStockSheet(false);
          }}
        >
          <div className="sheet-card coffee-detail-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">STOCK EN DESPENSA</strong>
            </header>
            <div className="coffee-detail-sheet-body coffee-detail-stock">
              <input
                className="search-wide search-input-standard"
                type="number"
                min={0}
                value={stockDraft.total}
                onChange={(event) => {
                  onStockDraftChange({ ...stockDraft, total: Number(event.target.value) });
                  if (stockSheetError) setStockSheetError(null);
                }}
                placeholder="Total gramos"
              />
              <input
                className="search-wide"
                type="number"
                min={0}
                value={stockDraft.remaining}
                onChange={(event) => {
                  onStockDraftChange({ ...stockDraft, remaining: Number(event.target.value) });
                  if (stockSheetError) setStockSheetError(null);
                }}
                placeholder="Restante gramos"
              />
              {stockSheetError ? <p className="coffee-detail-sheet-error">{stockSheetError}</p> : null}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions">
              <button
                type="button"
                className="action-button action-button-ghost"
                disabled={savingStock}
                onClick={() => {
                  if (savingStock) return;
                  setStockSheetError(null);
                  setShowStockSheet(false);
                }}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="action-button"
                disabled={!canSaveStock || savingStock}
                onClick={async () => {
                  if (isStockDraftInvalid) {
                    setStockSheetError("El restante no puede superar el total.");
                    return;
                  }
                  if (!isStockDirty) return;
                  setStockSheetError(null);
                  setSavingStock(true);
                  try {
                    await onSaveStock();
                    setStockSheetError(null);
                    setShowStockSheet(false);
                  } catch {
                    setStockSheetError("No se pudo guardar el stock.");
                  } finally {
                    setSavingStock(false);
                  }
                }}
              >
                {savingStock ? "Guardando..." : "Guardar stock"}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {showReviewSheet ? (
        <div
          className="sheet-overlay"
          role="dialog"
          aria-modal="true"
          aria-label="Escribir reseña"
          onClick={() => {
            setReviewSheetError(null);
            setShowReviewSheet(false);
          }}
        >
          <div className="sheet-card coffee-detail-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">TU OPINIÓN</strong>
            </header>
            <div className="coffee-detail-sheet-body coffee-detail-review-editor">
              <label className="coffee-detail-rating-field">
                <span className="coffee-detail-slider-label">
                  Nota
                  <strong>{reviewDraftRating > 0 ? `${reviewDraftRating.toFixed(1)} / 5` : "Sin nota"}</strong>
                </span>
                <div className="coffee-detail-rating-stars" role="radiogroup" aria-label="Seleccionar nota">
                  {[1, 2, 3, 4, 5].map((value) => (
                    <button
                      key={value}
                      type="button"
                      className={`coffee-detail-rating-star ${reviewDraftRating >= value ? "is-active" : ""}`.trim()}
                      onClick={() => {
                        onReviewRatingChange(value);
                        setReviewSheetError(null);
                      }}
                      onKeyDown={(event) => {
                        if (event.key === "ArrowRight" || event.key === "ArrowUp") {
                          event.preventDefault();
                          onReviewRatingChange(Math.min(5, Math.max(0, Math.round(reviewDraftRating)) + 1));
                          setReviewSheetError(null);
                          return;
                        }
                        if (event.key === "ArrowLeft" || event.key === "ArrowDown") {
                          event.preventDefault();
                          onReviewRatingChange(Math.max(0, Math.max(0, Math.round(reviewDraftRating)) - 1));
                          setReviewSheetError(null);
                          return;
                        }
                        if (event.key === "Home") {
                          event.preventDefault();
                          onReviewRatingChange(1);
                          setReviewSheetError(null);
                          return;
                        }
                        if (event.key === "End") {
                          event.preventDefault();
                          onReviewRatingChange(5);
                          setReviewSheetError(null);
                        }
                      }}
                      role="radio"
                      aria-checked={reviewDraftRating === value}
                      aria-label={`${value} estrellas`}
                    >
                      <UiIcon name={reviewDraftRating >= value ? "star-filled" : "star"} className="ui-icon" />
                    </button>
                  ))}
                  {reviewDraftRating > 0 ? (
                    <button
                      type="button"
                      className="text-button coffee-detail-rating-clear"
                      onClick={() => {
                        onReviewRatingChange(0);
                        setReviewSheetError("Selecciona una nota para guardar.");
                      }}
                    >
                      Quitar
                    </button>
                  ) : null}
                </div>
              </label>
              <textarea
                className="search-wide sheet-input"
                rows={3}
                value={reviewDraftText}
                onChange={(event) => {
                  onReviewTextChange(event.target.value);
                  if (reviewSheetError) setReviewSheetError(null);
                }}
                placeholder="Escribe tu reseña"
              />
              <label className="action-button action-button-ghost coffee-detail-file coffee-detail-cta">
                Adjuntar imagen
                <input
                  type="file"
                  accept="image/*"
                  onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    if (!file) {
                      onReviewImagePick(null, "");
                      return;
                    }
                    onReviewImagePick(file, URL.createObjectURL(file));
                  }}
                />
              </label>
              {reviewDraftImagePreviewUrl ? (
                <img className="coffee-detail-review-image" src={reviewDraftImagePreviewUrl} alt="Previsualización reseña" loading="lazy" />
              ) : null}
              {reviewSheetError ? <p className="coffee-detail-sheet-error">{reviewSheetError}</p> : null}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions">
              {canDeleteReview ? (
                <button
                  type="button"
                  className="action-button action-button-ghost"
                  disabled={savingReview || deletingReview}
                  onClick={async () => {
                    setDeletingReview(true);
                    try {
                      await onDeleteReview();
                      setReviewSheetError(null);
                    } finally {
                      setDeletingReview(false);
                    }
                  }}
                >
                  {deletingReview ? "Borrando..." : "Borrar reseña"}
                </button>
              ) : null}
              <button
                type="button"
                className="action-button"
                disabled={!canSaveReview || savingReview || deletingReview}
                onClick={async () => {
                  if (reviewDraftRating <= 0) {
                    setReviewSheetError("Selecciona una nota para guardar.");
                    return;
                  }
                  if (!isReviewDirty) return;
                  setReviewSheetError(null);
                  setSavingReview(true);
                  try {
                    await onSaveReview();
                    setShowReviewSheet(false);
                  } finally {
                    setSavingReview(false);
                  }
                }}
              >
                {savingReview ? "Guardando..." : "Guardar reseña"}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </article>
  );
}
function BrewLabView({
  brewStep,
  setBrewStep,
  brewMethod,
  setBrewMethod,
  brewCoffeeId,
  setBrewCoffeeId,
  coffees,
  pantryItems,
  onAddNotFoundCoffee,
  waterMl,
  setWaterMl,
  ratio,
  setRatio,
  timerSeconds,
  setTimerSeconds,
  brewRunning,
  setBrewRunning,
  selectedCoffee,
  coffeeGrams,
  onSaveResultToDiary
}: {
  brewStep: BrewStep;
  setBrewStep: (step: BrewStep) => void;
  brewMethod: string;
  setBrewMethod: (value: string) => void;
  brewCoffeeId: string;
  setBrewCoffeeId: (value: string) => void;
  coffees: CoffeeRow[];
  pantryItems: Array<{ item: PantryItemRow; coffee: CoffeeRow; total: number; remaining: number; progress: number }>;
  onAddNotFoundCoffee: () => void;
  waterMl: number;
  setWaterMl: (value: number) => void;
  ratio: number;
  setRatio: (value: number) => void;
  timerSeconds: number;
  setTimerSeconds: (value: number) => void;
  brewRunning: boolean;
  setBrewRunning: (value: boolean) => void;
  selectedCoffee?: CoffeeRow;
  coffeeGrams: number;
  onSaveResultToDiary: (taste: string) => Promise<void>;
}) {
  const [brewCoffeeQuery, setBrewCoffeeQuery] = useState("");
  const [resultTaste, setResultTaste] = useState("");
  const [savingResult, setSavingResult] = useState(false);
  const q = normalizeLookupText(brewCoffeeQuery);
  const filteredPantry = useMemo(
    () =>
      pantryItems.filter((row) => {
        if (!q) return true;
        return normalizeLookupText(row.coffee.nombre).includes(q) || normalizeLookupText(row.coffee.marca).includes(q);
      }),
    [pantryItems, q]
  );
  const filteredSuggestions = useMemo(
    () =>
      coffees
        .filter((coffee) => {
          if (!q) return true;
          return normalizeLookupText(coffee.nombre).includes(q) || normalizeLookupText(coffee.marca).includes(q);
        })
        .slice(0, 24),
    [coffees, q]
  );
  const coffeeGramsPrecise = useMemo(() => Number((waterMl / ratio).toFixed(1)), [ratio, waterMl]);
  const coffeeGramsLabel = useMemo(() => coffeeGramsPrecise.toFixed(1).replace(".", ","), [coffeeGramsPrecise]);
  const waterProgress = useMemo(() => {
    const min = 150;
    const max = 600;
    return Math.max(0, Math.min(100, ((waterMl - min) / (max - min)) * 100));
  }, [waterMl]);
  const ratioProgress = useMemo(() => {
    const min = 12;
    const max = 20;
    return Math.max(0, Math.min(100, ((ratio - min) / (max - min)) * 100));
  }, [ratio]);
  const brewAdvice = useMemo(() => {
    const methodKey = normalizeLookupText(brewMethod);
    if (!methodKey) return "";

    let intensity = "";
    if (methodKey.includes("espresso")) {
      if (ratio < 16) intensity = "Perfil sedoso y cuerpo ligero";
      else if (ratio < 20) intensity = "Equilibrio perfecto de dulzor";
      else intensity = "Cuerpo intenso y textura densa";
    } else if (methodKey.includes("italiana")) {
      if (ratio < 18) intensity = "Sabor suave y retrogusto limpio";
      else if (ratio < 21) intensity = "Intensidad clásica con mucho cuerpo";
      else intensity = "Textura muy robusta y concentrada";
    } else {
      if (ratio < 13) intensity = "Cuerpo muy pesado y sabores potentes";
      else if (ratio < 15) intensity = "Notas dulces muy marcadas y untuosas";
      else if (ratio < 17.5) intensity = "Extracción equilibrada y balanceada";
      else if (ratio < 19) intensity = "Mayor claridad aromática y cuerpo sutil";
      else intensity = "Perfil muy ligero con notas acuosas";
    }

    let volumeDesc = "";
    if (waterMl < 150) volumeDesc = "en formato de taza corta e intensa.";
    else if (waterMl < 300) volumeDesc = "en una taza estándar ideal para el día a día.";
    else volumeDesc = "en formato diseñado para compartir.";

    return `${intensity} ${volumeDesc}`;
  }, [brewMethod, ratio, waterMl]);
  const baristaTips = useMemo(() => {
    const methodKey = normalizeLookupText(brewMethod);
    const isEspresso = methodKey.includes("espresso");
    if (isEspresso) {
      return { grind: "Fina", temperature: "90-93°C" };
    }
    return { grind: "Fina/Media", temperature: "80-85°C" };
  }, [brewMethod]);
  const brewTimeline = useMemo(() => getBrewTimelineForMethod(brewMethod, waterMl), [brewMethod, waterMl]);
  const brewTotalSeconds = useMemo(() => brewTimeline.reduce((acc, phase) => acc + phase.durationSeconds, 0), [brewTimeline]);
  const elapsedSeconds = useMemo(() => Math.max(0, Math.min(timerSeconds, brewTotalSeconds || timerSeconds)), [brewTotalSeconds, timerSeconds]);
  const currentPhaseIndex = useMemo(() => {
    if (!brewTimeline.length) return 0;
    let elapsed = 0;
    for (let i = 0; i < brewTimeline.length; i += 1) {
      elapsed += brewTimeline[i].durationSeconds;
      if (elapsedSeconds < elapsed) return i;
    }
    return Math.max(0, brewTimeline.length - 1);
  }, [brewTimeline, elapsedSeconds]);
  const currentPhase = brewTimeline[currentPhaseIndex] ?? { label: "Listo", instruction: "Proceso completado.", durationSeconds: 0 };
  const nextPhase = brewTimeline[currentPhaseIndex + 1];
  const elapsedBeforeCurrentPhase = useMemo(() => {
    if (!brewTimeline.length) return 0;
    return brewTimeline.slice(0, currentPhaseIndex).reduce((acc, phase) => acc + phase.durationSeconds, 0);
  }, [brewTimeline, currentPhaseIndex]);
  const remainingInPhase = useMemo(
    () => Math.max(0, currentPhase.durationSeconds - Math.max(0, elapsedSeconds - elapsedBeforeCurrentPhase)),
    [currentPhase.durationSeconds, elapsedBeforeCurrentPhase, elapsedSeconds]
  );
  const resultRecommendation = useMemo(() => getBrewDialRecommendation(resultTaste), [resultTaste]);
  const tasteOptions = useMemo(
    () => [
      { label: "Amargo", icon: "taste-bitter" as IconName },
      { label: "Acido", icon: "taste-acid" as IconName },
      { label: "Equilibrado", icon: "taste-balance" as IconName },
      { label: "Salado", icon: "taste-salty" as IconName },
      { label: "Acuoso", icon: "taste-watery" as IconName },
      { label: "Aspero", icon: "grind" as IconName },
      { label: "Dulce", icon: "taste-sweet" as IconName }
    ],
    []
  );

  useEffect(() => {
    if (brewStep !== "result") {
      setResultTaste("");
      setSavingResult(false);
    }
  }, [brewStep]);

  return (
    <>
      {brewStep === "method" ? (
        <div className="brew-method-grid-native">
          {BREW_METHODS.map((method) => (
            <button key={method.name} type="button" className={`brew-method-card-native ${brewMethod === method.name ? "is-active" : ""}`} onClick={() => {
              setBrewMethod(method.name);
              setBrewCoffeeId("");
              setBrewStep("coffee");
            }}>
              <img src={method.icon} alt={method.name} loading="lazy" />
              <strong>{method.name.toUpperCase()}</strong>
            </button>
          ))}
        </div>
      ) : null}

      {brewStep === "coffee" ? (
        <section className="brew-choose-coffee">
          <div className="brew-coffee-block">
            <p className="section-title">Tu despensa</p>
            {filteredPantry.length ? (
              <div className="brew-pantry-row">
                {filteredPantry.map((row) => (
                  <button
                    key={`${row.coffee.id}-pantry`}
                    type="button"
                    className="brew-pantry-card"
                    onClick={() => {
                      setBrewCoffeeId(row.coffee.id);
                      setBrewStep("config");
                    }}
                  >
                    {row.coffee.image_url ? (
                      <img src={row.coffee.image_url} alt={row.coffee.nombre} loading="lazy" />
                    ) : (
                      <span className="brew-pantry-fallback" aria-hidden="true">{row.coffee.nombre.slice(0, 1).toUpperCase()}</span>
                    )}
                    <div className="brew-pantry-body">
                      <strong>{row.coffee.nombre}</strong>
                      <small>{row.remaining}G REST.</small>
                      <div className="brew-pantry-progress" aria-hidden="true">
                        <span style={{ width: `${Math.max(0, Math.min(100, row.progress * 100))}%` }} />
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <p className="coffee-sub">{q ? "No hay coincidencias en tu despensa." : "Tu despensa está vacía."}</p>
            )}
          </div>

          <input
            className="search-wide search-input-standard brew-coffee-search"
            value={brewCoffeeQuery}
            onChange={(event) => setBrewCoffeeQuery(event.target.value)}
            placeholder="Buscar café..."
          />

          <div className="brew-coffee-block-head">
            <p className="section-title">Sugerencias</p>
            <button type="button" className="text-button brew-add-coffee-link" onClick={onAddNotFoundCoffee}>
              <UiIcon name="add" className="ui-icon" />
              Crear mi café
            </button>
          </div>

          {filteredSuggestions.length ? (
            <ul className="brew-suggestions-list">
              {filteredSuggestions.map((coffee) => (
                <li key={coffee.id}>
                  <button
                    type="button"
                    className={`brew-suggestion-card ${brewCoffeeId === coffee.id ? "is-active" : ""}`}
                    onClick={() => {
                      setBrewCoffeeId(coffee.id);
                      setBrewStep("config");
                    }}
                  >
                    {coffee.image_url ? (
                      <img src={coffee.image_url} alt={coffee.nombre} loading="lazy" />
                    ) : (
                      <span className="brew-suggestion-fallback" aria-hidden="true">{coffee.nombre.slice(0, 1).toUpperCase()}</span>
                    )}
                    <span>
                      <strong>{coffee.nombre}</strong>
                      <small>{coffee.marca}</small>
                    </span>
                    <UiIcon name="add" className="ui-icon" />
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <p className="coffee-sub">No hay sugerencias disponibles.</p>
          )}
        </section>
      ) : null}

      {brewStep === "config" ? (
        <section className="brew-config-native">
          <div className="brew-config-layout">
            <div className="brew-config-main">
              <p className="section-title brew-config-heading">AJUSTES TÉCNICOS</p>
              <article className="brew-tech-card">
                <div className="brew-tech-top">
                  <div>
                    <span>AGUA</span>
                    <strong className="is-water">{waterMl} ml</strong>
                  </div>
                  <div>
                    <span>CAFÉ</span>
                    <strong>{coffeeGramsLabel} g</strong>
                  </div>
                </div>

                <label className="brew-tech-slider">
                  <span>CANTIDAD DE AGUA</span>
                  <input
                    className="brew-tech-range is-water"
                    style={{ "--range-progress": `${waterProgress}%` } as CSSProperties}
                    type="range"
                    min={150}
                    max={600}
                    step={10}
                    value={waterMl}
                    onChange={(event) => setWaterMl(Number(event.target.value))}
                  />
                </label>

                <label className="brew-tech-slider">
                  <span>RATIO (INTENSIDAD)</span>
                  <input
                    className="brew-tech-range"
                    style={{ "--range-progress": `${ratioProgress}%` } as CSSProperties}
                    type="range"
                    min={12}
                    max={20}
                    step={1}
                    value={ratio}
                    onChange={(event) => setRatio(Number(event.target.value))}
                  />
                </label>

                <div className="brew-tech-advice">
                  <UiIcon name="star-filled" className="ui-icon" />
                  <p>{brewAdvice}</p>
                </div>
              </article>
            </div>

            <aside className="brew-barista-block">
              <p className="section-title brew-config-heading">CONSEJOS DEL BARISTA</p>
              <div className="brew-barista-grid">
                <article className="brew-barista-tip">
                  <span className="brew-barista-icon"><UiIcon name="grind" className="ui-icon" /></span>
                  <div>
                    <small>MOLIENDA</small>
                    <strong>{baristaTips.grind}</strong>
                  </div>
                </article>
                <article className="brew-barista-tip">
                  <span className="brew-barista-icon"><UiIcon name="thermostat" className="ui-icon" /></span>
                  <div>
                    <small>TEMPERATURA</small>
                    <strong>{baristaTips.temperature}</strong>
                  </div>
                </article>
              </div>
            </aside>
          </div>
        </section>
      ) : null}

      {brewStep === "brewing" ? (
        <section className="brew-prep-screen">
          <article className="brew-prep-card">
            <div className="brew-prep-head">
              <p className="brew-prep-phase">{currentPhase.label}</p>
              <strong className={`brew-prep-clock ${remainingInPhase <= 5 && brewRunning ? "is-warning" : ""}`.trim()}>
                {formatClock(remainingInPhase)}
              </strong>
              <p className="brew-prep-next">Siguiente: {nextPhase?.label ?? "Finalizar"}</p>
            </div>

            <div className="brew-prep-timeline">
              <div className="brew-prep-time-labels" aria-hidden="true">
                {brewTimeline.map((phase) => {
                  const widthWeight = Math.max(phase.durationSeconds / Math.max(1, brewTotalSeconds), 0.08);
                  return (
                    <small key={`label-${phase.label}`} style={{ flexGrow: widthWeight }}>
                      {phase.durationSeconds}s
                    </small>
                  );
                })}
              </div>
              <div className="brew-prep-bars" aria-hidden="true">
                {brewTimeline.map((phase, index) => {
                  const widthWeight = Math.max(phase.durationSeconds / Math.max(1, brewTotalSeconds), 0.08);
                  const elapsedBefore = brewTimeline.slice(0, index).reduce((acc, item) => acc + item.durationSeconds, 0);
                  const progress = elapsedSeconds <= elapsedBefore
                    ? 0
                    : elapsedSeconds >= elapsedBefore + phase.durationSeconds
                      ? 1
                      : (elapsedSeconds - elapsedBefore) / phase.durationSeconds;
                  return (
                    <span key={`bar-${phase.label}-${index}`} className="brew-prep-bar" style={{ flexGrow: widthWeight }}>
                      <i style={{ width: `${Math.max(0, Math.min(100, progress * 100))}%` }} />
                    </span>
                  );
                })}
              </div>
              <div className="brew-prep-total">
                <span>TOTAL {formatClock(brewTotalSeconds)}</span>
                <strong>{formatClock(elapsedSeconds)}</strong>
              </div>
            </div>

            <div className="brew-prep-instruction">
              <p>{currentPhase.instruction}</p>
            </div>
          </article>

          <div className={`brew-prep-actions ${elapsedSeconds > 0 ? "" : "is-single"}`.trim()}>
            {elapsedSeconds > 0 ? (
              <button
                className="action-button action-button-ghost brew-prep-action-secondary"
                type="button"
                onClick={() => {
                  setBrewRunning(false);
                  setTimerSeconds(0);
                }}
              >
                REINICIAR
              </button>
            ) : null}
            <button
              className={`action-button brew-prep-action-primary ${brewRunning ? "is-running" : ""}`.trim()}
              type="button"
              onClick={() => setBrewRunning(!brewRunning)}
            >
              {brewRunning ? "PAUSAR" : "INICIAR"}
            </button>
          </div>
        </section>
      ) : null}

      {brewStep === "result" ? (
        <section className="brew-result-screen">
          <article className="brew-result-card">
            <p className="brew-result-title">QUE SABOR HAS OBTENIDO?</p>
            <div className="brew-result-grid">
              {tasteOptions.map((taste) => (
                <button
                  key={taste.label}
                  type="button"
                  className={`brew-taste-chip ${resultTaste === taste.label ? "is-active" : ""}`.trim()}
                  onClick={() => setResultTaste(taste.label)}
                >
                  <UiIcon name={taste.icon} className="ui-icon" />
                  <span>{taste.label.toUpperCase()}</span>
                </button>
              ))}
            </div>
            {resultRecommendation ? (
              <div className="brew-result-reco">
                <div className="brew-result-reco-head">
                  <UiIcon name="star-filled" className="ui-icon" />
                  <strong>Recomendacion</strong>
                </div>
                <p>{resultRecommendation}</p>
              </div>
            ) : null}
          </article>

          <div className="brew-result-actions">
            <button
              className="action-button action-button-ghost brew-result-action-secondary"
              type="button"
              onClick={() => {
                setBrewRunning(false);
                setTimerSeconds(0);
                setBrewStep("method");
              }}
            >
              REINICIAR
            </button>
            <button
              className="action-button brew-result-action-primary"
              type="button"
              disabled={!selectedCoffee || !resultTaste || savingResult}
              onClick={async () => {
                if (!selectedCoffee || !resultTaste || savingResult) return;
                setSavingResult(true);
                try {
                  await onSaveResultToDiary(resultTaste);
                } finally {
                  setSavingResult(false);
                }
              }}
            >
              {savingResult ? "GUARDANDO..." : "GUARDAR EN DIARIO"}
            </button>
          </div>
        </section>
      ) : null}
    </>
  );
}

function CreateCoffeeView({
  draft,
  imagePreviewUrl,
  saving,
  error,
  countryOptions,
  specialtyOptions,
  onChange,
  onPickImage,
  onRemoveImage,
  onClose,
  onSave,
  fullPage
}: {
  draft: {
    name: string;
    brand: string;
    specialty: string;
    country: string;
    format: string;
    roast: string;
    variety: string;
    hasCaffeine: boolean;
    totalGrams: number;
  };
  imagePreviewUrl: string;
  saving: boolean;
  error: string | null;
  countryOptions: string[];
  specialtyOptions: string[];
  onChange: (next: {
    name: string;
    brand: string;
    specialty: string;
    country: string;
    format: string;
    roast: string;
    variety: string;
    hasCaffeine: boolean;
    totalGrams: number;
  }) => void;
  onPickImage: (file: File | null, previewUrl: string) => void;
  onRemoveImage: () => void;
  onClose: () => void;
  onSave: () => void;
  fullPage: boolean;
}) {
  const rootRef = useRef<HTMLElement | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const [attemptedSave, setAttemptedSave] = useState(false);
  const requiredValues = [
    draft.name.trim(),
    draft.brand.trim(),
    draft.specialty.trim(),
    draft.country.trim(),
    draft.format.trim()
  ];
  const missingRequiredCount = requiredValues.filter((value) => !value).length;
  const canSave = missingRequiredCount === 0;
  const nameMissing = attemptedSave && !draft.name.trim();
  const brandMissing = attemptedSave && !draft.brand.trim();
  const specialtyMissing = attemptedSave && !draft.specialty.trim();
  const countryMissing = attemptedSave && !draft.country.trim();
  const formatMissing = attemptedSave && !draft.format.trim();

  const handleSaveAttempt = () => {
    setAttemptedSave(true);
    if (!canSave) {
      const firstInvalid = rootRef.current?.querySelector<HTMLInputElement | HTMLSelectElement>(".is-invalid .search-wide");
      firstInvalid?.focus();
      return;
    }
    onSave();
  };

  const getSpecialtyIcon = (value: string): IconName => {
    const key = normalizeLookupText(value);
    if (key.includes("arabica")) return "leaf";
    if (key.includes("mezcla") || key.includes("blend")) return "blend";
    return "specialty";
  };

  const getRoastIcon = (value: string): IconName => {
    const key = normalizeLookupText(value);
    if (key.includes("ligero") || key.includes("claro") || key.includes("light")) return "roast-light";
    if (key.includes("medio-oscuro") || key.includes("oscuro") || key.includes("dark")) return "roast-dark";
    return "roast-medium";
  };

  const roastChoices = useMemo(
    () => [
      { label: "Ligero", value: "Ligero" },
      { label: "Medio", value: "Medio" },
      { label: "Medio-oscuro", value: "Medio-oscuro" }
    ],
    []
  );

  const formatChoices = useMemo(
    () => [
      { label: "Grano", value: "Grano" },
      { label: "Molido", value: "Molido" },
      { label: "Capsula", value: "Capsula" }
    ],
    []
  );

  return (
    <section ref={rootRef} className={`create-coffee-view ${fullPage ? "is-full-page" : "is-side-panel"}`.trim()}>
      <div className="create-coffee-head">
        <p className="section-title">Datos del café</p>
      </div>
      <p className="create-coffee-hint">{attemptedSave ? (canSave ? "Listo para guardar" : `Faltan ${missingRequiredCount} campos obligatorios`) : ""}</p>
      <div className="create-coffee-image create-coffee-image-top create-coffee-image-card">
        <input
          ref={imageInputRef}
          type="file"
          accept="image/*"
          className="file-input-hidden"
          onChange={(event) => {
            const file = event.target.files?.[0] ?? null;
            if (!file) return;
            const previewUrl = URL.createObjectURL(file);
            onPickImage(file, previewUrl);
            event.currentTarget.value = "";
          }}
        />
        {imagePreviewUrl ? (
          <div className="create-coffee-image-preview-wrap">
            <img src={imagePreviewUrl} alt="Previsualización café" loading="lazy" />
            <button type="button" className="icon-button" onClick={onRemoveImage} aria-label="Quitar imagen">
              <UiIcon name="close" className="ui-icon" />
            </button>
          </div>
        ) : (
          <button type="button" className="create-coffee-image-empty" onClick={() => imageInputRef.current?.click()}>
            <span className="create-coffee-image-empty-icon" aria-hidden="true">
              <UiIcon name="camera" className="ui-icon" />
              <span className="create-coffee-image-empty-plus">+</span>
            </span>
            <span>Añadir foto</span>
          </button>
        )}
        <div className="create-coffee-image-fields">
          <div className={`create-coffee-inline-field ${nameMissing ? "is-invalid" : ""}`.trim()}>
            <input
              className="search-wide"
              placeholder="Nombre del café"
              value={draft.name}
              onChange={(event) => onChange({ ...draft, name: event.target.value })}
              aria-invalid={nameMissing}
            />
          </div>
          <div className={`create-coffee-inline-field ${brandMissing ? "is-invalid" : ""}`.trim()}>
            <input
              className="search-wide"
              placeholder="Marca"
              value={draft.brand}
              onChange={(event) => onChange({ ...draft, brand: event.target.value })}
              aria-invalid={brandMissing}
            />
          </div>
        </div>
      </div>
      <div className="create-coffee-profile-origin">
        <p className="create-coffee-block-title">Perfil y Origen</p>
        <p className="create-coffee-block-subtitle">Especialidad</p>
        <div className={`create-coffee-choice-grid create-coffee-choice-grid-specialty ${specialtyMissing ? "is-invalid" : ""}`.trim()}>
          {specialtyOptions.length ? specialtyOptions.map((value) => (
            <button
              key={value}
              type="button"
              className={`create-coffee-choice ${draft.specialty === value ? "is-selected" : ""}`.trim()}
              onClick={() => onChange({ ...draft, specialty: value })}
              aria-pressed={draft.specialty === value}
            >
              <UiIcon name={getSpecialtyIcon(value)} className="ui-icon" />
              <span>{value}</span>
            </button>
          )) : (
            <p className="coffee-sub">Sin opciones</p>
          )}
        </div>
        <p className="create-coffee-block-subtitle">Tueste</p>
        <div className="create-coffee-choice-grid create-coffee-choice-grid-roast">
          {roastChoices.map((choice) => (
            <button
              key={choice.value}
              type="button"
              className={`create-coffee-choice ${draft.roast === choice.value ? "is-selected" : ""}`.trim()}
              onClick={() => onChange({ ...draft, roast: draft.roast === choice.value ? "" : choice.value })}
              aria-pressed={draft.roast === choice.value}
            >
              <UiIcon name={getRoastIcon(choice.value)} className="ui-icon" />
              <span>{choice.label}</span>
            </button>
          ))}
        </div>
        <label className={`create-coffee-country ${countryMissing ? "is-invalid" : ""}`.trim()}>
          <span>País</span>
          <select
            className="search-wide"
            value={draft.country}
            onChange={(event) => onChange({ ...draft, country: event.target.value })}
            aria-invalid={countryMissing}
          >
            <option value="">Selecciona</option>
            {countryOptions.map((value) => (
              <option key={value} value={value}>{value}</option>
            ))}
          </select>
        </label>
      </div>
      <div className={`create-coffee-format-block ${formatMissing ? "is-invalid" : ""}`.trim()}>
        <p className="create-coffee-block-title">Formato</p>
        <label className="create-coffee-caffeine-row">
          <span>¿Tiene cafeína?</span>
          <button
            type="button"
            role="switch"
            aria-checked={draft.hasCaffeine}
            className={`create-coffee-caffeine-switch ${draft.hasCaffeine ? "is-on" : ""}`.trim()}
            onClick={() => onChange({ ...draft, hasCaffeine: !draft.hasCaffeine })}
          >
            <span />
          </button>
        </label>
        <p className="create-coffee-block-subtitle">Presentación</p>
        <div className="create-coffee-choice-grid create-coffee-choice-grid-format">
          {formatChoices.map((choice) => (
            <button
              key={choice.value}
              type="button"
              className={`create-coffee-choice ${draft.format === choice.value ? "is-selected" : ""}`.trim()}
              onClick={() => onChange({ ...draft, format: choice.value })}
              aria-pressed={draft.format === choice.value}
            >
              <UiIcon
                name={
                  choice.value === "Grano"
                    ? "rugby"
                    : choice.value === "Molido"
                      ? "blend"
                      : "recycle"
                }
                className="ui-icon"
              />
              <span>{choice.label}</span>
            </button>
          ))}
        </div>
      </div>
      {error ? <p className="create-coffee-error">{error}</p> : null}
      {!fullPage ? (
        <div className="create-coffee-actions">
          <button type="button" className="action-button action-button-ghost" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button type="button" className="action-button" onClick={handleSaveAttempt} disabled={saving}>
            {saving ? "Guardando..." : "Guardar"}
          </button>
        </div>
      ) : null}
      {fullPage ? (
        <button type="button" className="action-button create-coffee-mobile-save" onClick={handleSaveAttempt} disabled={saving}>
          {saving ? "Guardando..." : "Guardar café"}
        </button>
      ) : null}
    </section>
  );
}

function DiaryView({
  mode,
  tab,
  setTab,
  period,
  entries,
  coffeeCatalog,
  pantryRows,
  onDeleteEntry,
  onEditEntry,
  onUpdatePantryStock,
  onRemovePantryItem,
  onOpenCoffee
}: {
  mode: "mobile" | "desktop";
  tab: "actividad" | "despensa";
  setTab: (value: "actividad" | "despensa") => void;
  period: "hoy" | "7d" | "30d";
  entries: DiaryEntryRow[];
  coffeeCatalog: CoffeeRow[];
  pantryRows: Array<{ item: PantryItemRow; coffee?: CoffeeRow }>;
  onDeleteEntry: (entryId: number) => Promise<void>;
  onEditEntry: (entryId: number, amountMl: number, caffeineMg: number, preparationType: string) => Promise<void>;
  onUpdatePantryStock: (coffeeId: string, totalGrams: number, gramsRemaining: number) => Promise<void>;
  onRemovePantryItem: (coffeeId: string) => Promise<void>;
  onOpenCoffee: (coffeeId: string) => void;
}) {
  const [deletingEntryId, setDeletingEntryId] = useState<number | null>(null);
  const [editEntryId, setEditEntryId] = useState<number | null>(null);
  const [editAmountMl, setEditAmountMl] = useState("");
  const [editCaffeineMg, setEditCaffeineMg] = useState("");
  const [editPreparationType, setEditPreparationType] = useState("");
  const [savingEditEntry, setSavingEditEntry] = useState(false);
  const [pantryOptionsCoffeeId, setPantryOptionsCoffeeId] = useState<string | null>(null);
  const [pantryDeleteConfirmCoffeeId, setPantryDeleteConfirmCoffeeId] = useState<string | null>(null);
  const [stockEditCoffeeId, setStockEditCoffeeId] = useState<string | null>(null);
  const [stockEditTotal, setStockEditTotal] = useState("");
  const [stockEditRemaining, setStockEditRemaining] = useState("");
  const [savingStock, setSavingStock] = useState(false);
  const [removingStock, setRemovingStock] = useState(false);
  const [showCaffeineInfo, setShowCaffeineInfo] = useState(false);
  const editPrepScrollRef = useRef<HTMLDivElement | null>(null);
  const editSizeScrollRef = useRef<HTMLDivElement | null>(null);
  const editScrollPointerIdRef = useRef<number | null>(null);
  const editScrollStartXRef = useRef(0);
  const editScrollStartLeftRef = useRef(0);
  const editScrollTargetRef = useRef<HTMLDivElement | null>(null);
  const editScrollActiveRef = useRef(false);
  const editScrollRafRef = useRef<number | null>(null);
  const editScrollPendingLeftRef = useRef(0);
  const [editChipsDragging, setEditChipsDragging] = useState(false);
  const visibleEntries = useMemo(() => {
    const now = new Date();
    const start = new Date(now);
    if (period === "hoy") {
      start.setHours(0, 0, 0, 0);
    } else if (period === "7d") {
      const day = start.getDay(); // Sunday=0
      const diffToMonday = day === 0 ? 6 : day - 1;
      start.setDate(start.getDate() - diffToMonday);
      start.setHours(0, 0, 0, 0);
    } else {
      start.setDate(1);
      start.setHours(0, 0, 0, 0);
    }
    const startMs = start.getTime();
    return entries
      .filter((entry) => Number(entry.timestamp) >= startMs)
      .sort((a, b) => Number(b.timestamp) - Number(a.timestamp));
  }, [entries, period]);

  const analytics = useMemo(() => {
    const caffeine = visibleEntries.reduce((acc, entry) => acc + Math.max(0, entry.caffeine_mg || 0), 0);
    const hydrationMl = visibleEntries
      .filter((entry) => (entry.type || "").toUpperCase() === "WATER")
      .reduce((acc, entry) => acc + Math.max(0, entry.amount_ml || 0), 0);
    const coffeeCups = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() !== "WATER").length;
    const waterEntries = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() === "WATER").length;
    const avgCaffeine = coffeeCups > 0 ? Math.round(caffeine / coffeeCups) : 0;
    return { caffeine, hydrationMl, coffeeCups, waterEntries, avgCaffeine };
  }, [visibleEntries]);
  const hydrationTargetMl = period === "hoy" ? 2000 : period === "7d" ? 14000 : 60000;
  const caffeineTargetMg = period === "hoy" ? 160 : period === "7d" ? 1120 : 4800;
  const hydrationProgressPct = Math.max(0, Math.min(100, Math.round((analytics.hydrationMl / hydrationTargetMl) * 100)));
  const caffeineTrendPct = Math.round(((analytics.caffeine - caffeineTargetMg) / Math.max(1, caffeineTargetMg)) * 100);
  const hydrationTrendPct = Math.round(((analytics.hydrationMl - hydrationTargetMl) / Math.max(1, hydrationTargetMl)) * 100);
  const chartData = useMemo(() => {
    if (period === "hoy") {
      const byHour = new Map<number, { caffeine: number; water: number }>();
      visibleEntries.forEach((entry) => {
        const hour = new Date(entry.timestamp).getHours();
        const prev = byHour.get(hour) ?? { caffeine: 0, water: 0 };
        if ((entry.type || "").toUpperCase() === "WATER") {
          prev.water += Math.max(0, entry.amount_ml || 0);
        } else {
          prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
        }
        byHour.set(hour, prev);
      });
      return Array.from({ length: 24 }).map((_, idx) => {
        const values = byHour.get(idx) ?? { caffeine: 0, water: 0 };
        return { label: `${String(idx).padStart(2, "0")}`, caffeine: values.caffeine, water: values.water };
      });
    }

    if (period === "7d") {
      const dayLabels = ["L", "M", "X", "J", "V", "S", "D"];
      const byDay = new Map<number, { caffeine: number; water: number }>();
      visibleEntries.forEach((entry) => {
        const jsDay = new Date(entry.timestamp).getDay(); // Sun=0..Sat=6
        const mondayStartIndex = jsDay === 0 ? 6 : jsDay - 1;
        const prev = byDay.get(mondayStartIndex) ?? { caffeine: 0, water: 0 };
        if ((entry.type || "").toUpperCase() === "WATER") {
          prev.water += Math.max(0, entry.amount_ml || 0);
        } else {
          prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
        }
        byDay.set(mondayStartIndex, prev);
      });
      return dayLabels.map((label, idx) => {
        const values = byDay.get(idx) ?? { caffeine: 0, water: 0 };
        return { label, caffeine: values.caffeine, water: values.water };
      });
    }

    const now = new Date();
    const year = now.getFullYear();
    const month = now.getMonth();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const byDay = new Map<number, { caffeine: number; water: number }>();
    visibleEntries.forEach((entry) => {
      const d = new Date(entry.timestamp);
      const day = d.getDate();
      const prev = byDay.get(day) ?? { caffeine: 0, water: 0 };
      if ((entry.type || "").toUpperCase() === "WATER") {
        prev.water += Math.max(0, entry.amount_ml || 0);
      } else {
        prev.caffeine += Math.max(0, entry.caffeine_mg || 0);
      }
      byDay.set(day, prev);
    });
    return Array.from({ length: daysInMonth }).map((_, idx) => {
      const day = idx + 1;
      const values = byDay.get(day) ?? { caffeine: 0, water: 0 };
      return { label: String(day), caffeine: values.caffeine, water: values.water };
    });
  }, [period, visibleEntries]);
  const chartScrollRef = useRef<HTMLDivElement | null>(null);
  const chartPointerIdRef = useRef<number | null>(null);
  const chartDragStartXRef = useRef(0);
  const chartDragStartScrollRef = useRef(0);
  const [chartDragging, setChartDragging] = useState(false);

  useEffect(() => {
    const node = chartScrollRef.current;
    if (!node) return;
    node.scrollLeft = 0;
  }, [period, chartData.length]);
  const sortedPantryRows = useMemo(
    () => [...pantryRows].sort((a, b) => Number(b.item.last_updated || 0) - Number(a.item.last_updated || 0)),
    [pantryRows]
  );
  const entryImageByCoffeeId = useMemo(() => {
    const map = new Map<string, string>();
    coffeeCatalog.forEach((coffee) => {
      const id = String(coffee.id || "");
      const image = String(coffee.image_url || "");
      if (id && image && !map.has(id)) map.set(id, image);
    });
    sortedPantryRows.forEach((row) => {
      const id = String(row.item.coffee_id || "");
      const image = row.coffee?.image_url || "";
      if (id && image && !map.has(id)) map.set(id, image);
    });
    return map;
  }, [coffeeCatalog, sortedPantryRows]);
  const entryBrandByCoffeeId = useMemo(() => {
    const map = new Map<string, string>();
    coffeeCatalog.forEach((coffee) => {
      const id = String(coffee.id || "");
      const brand = String((coffee.marca || "").trim());
      if (id && brand && !map.has(id)) map.set(id, brand);
    });
    sortedPantryRows.forEach((row) => {
      const id = String(row.item.coffee_id || "");
      const brand = (row.coffee?.marca || "").trim();
      if (id && brand && !map.has(id)) map.set(id, brand);
    });
    return map;
  }, [coffeeCatalog, sortedPantryRows]);
  const diaryRows = useMemo(() => {
    const rows: Array<{ type: "header"; key: string; label: string } | { type: "entry"; key: string; entry: DiaryEntryRow }> = [];
    let lastDayKey = "";
    visibleEntries.forEach((entry) => {
      const date = new Date(entry.timestamp);
      const dayKey = `${date.getFullYear()}-${date.getMonth()}-${date.getDate()}`;
      if (period !== "hoy" && dayKey !== lastDayKey) {
        rows.push({
          type: "header",
          key: `header-${dayKey}`,
          label: date.toLocaleDateString("es-ES", { day: "numeric", month: "long" })
        });
        lastDayKey = dayKey;
      }
      rows.push({ type: "entry", key: `entry-${entry.id}`, entry });
    });
    return rows;
  }, [period, visibleEntries]);
  const stockEditTarget = useMemo(
    () => sortedPantryRows.find((row) => row.item.coffee_id === stockEditCoffeeId) ?? null,
    [sortedPantryRows, stockEditCoffeeId]
  );
  const editEntryTarget = useMemo(
    () => visibleEntries.find((entry) => entry.id === editEntryId) ?? null,
    [editEntryId, visibleEntries]
  );
  const editEntryIsWater = (editEntryTarget?.type || "").toUpperCase() === "WATER";
  const editEntryTime = editEntryTarget
    ? new Date(editEntryTarget.timestamp).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" })
    : "";
  const editMethodOptions = [
    { label: "Espresso", drawable: "maq_espresso.png" },
    { label: "Americano", drawable: "maq_manual.png" },
    { label: "Cappuccino", drawable: "maq_espresso.png" },
    { label: "V60", drawable: "maq_hario_v60.png" },
    { label: "Aeropress", drawable: "maq_aeropress.png" },
    { label: "Chemex", drawable: "maq_chemex.png" },
    { label: "Prensa francesa", drawable: "maq_prensa_francesa.png" },
    { label: "Moka", drawable: "maq_italiana.png" },
    { label: "Goteo", drawable: "maq_goteo.png" },
    { label: "Sifón", drawable: "maq_sifon.png" },
    { label: "Turco", drawable: "maq_turco.png" },
    { label: "Manual", drawable: "maq_manual.png" }
  ];
  const editSizeOptions = [
    { label: "Espresso", range: "25-30 ml", ml: 30, drawable: "taza_espresso.png" },
    { label: "Pequeño", range: "150-200 ml", ml: 175, drawable: "taza_pequeno.png" },
    { label: "Mediano", range: "250-300 ml", ml: 275, drawable: "taza_mediano.png" },
    { label: "Grande", range: "320-400 ml", ml: 360, drawable: "taza_grande.png" }
  ];
  const parsedStockTotal = Number(stockEditTotal || 0);
  const parsedStockRemaining = Number(stockEditRemaining || 0);
  const canSaveStock =
    Number.isFinite(parsedStockTotal) &&
    Number.isFinite(parsedStockRemaining) &&
    parsedStockTotal >= 1 &&
    parsedStockRemaining >= 0 &&
    parsedStockRemaining <= parsedStockTotal;
  const stockValidationMessage =
    (!Number.isFinite(parsedStockTotal) || !Number.isFinite(parsedStockRemaining))
      ? "Introduce valores válidos."
      : parsedStockTotal < 1
      ? "El total debe ser mayor que 0."
      : parsedStockRemaining < 0
        ? "El restante no puede ser negativo."
        : parsedStockRemaining > parsedStockTotal
          ? "El restante no puede superar el total."
          : "";
  const parsedEditAmount = Number(editAmountMl || 0);
  const parsedEditCaffeine = Number(editCaffeineMg || 0);
  const parsedEditDose = ((editPreparationType || "").match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1] || "15").replace(",", ".");
  const canSaveEditEntry =
    Number.isFinite(parsedEditAmount) &&
    parsedEditAmount > 0 &&
    (editEntryIsWater || (Number.isFinite(parsedEditCaffeine) && parsedEditCaffeine >= 0));
  const editValidationMessage =
    !Number.isFinite(parsedEditAmount)
      ? "Introduce una cantidad válida."
      : parsedEditAmount <= 0
      ? "La cantidad debe ser mayor que 0 ml."
      : (!editEntryIsWater && !Number.isFinite(parsedEditCaffeine))
        ? "Introduce una cafeína válida."
      : (!editEntryIsWater && parsedEditCaffeine < 0)
        ? "La cafeína no puede ser negativa."
        : "";
  const handleEditScrollPointerDown = (event: ReactPointerEvent<HTMLDivElement>, section: "prep" | "size") => {
    const node = section === "prep" ? editPrepScrollRef.current : editSizeScrollRef.current;
    if (!node) return;
    if (event.pointerType !== "mouse") return;
    if (node.scrollWidth <= node.clientWidth) return;
    editScrollPointerIdRef.current = event.pointerId;
    editScrollTargetRef.current = node;
    editScrollStartXRef.current = event.clientX;
    editScrollStartLeftRef.current = node.scrollLeft;
    editScrollActiveRef.current = false;
  };
  const handleEditScrollPointerMove = (event: ReactPointerEvent<HTMLDivElement>) => {
    const node = editScrollTargetRef.current;
    if (!node) return;
    if (editScrollPointerIdRef.current !== event.pointerId) return;
    const delta = event.clientX - editScrollStartXRef.current;
    if (!editScrollActiveRef.current) {
      if (Math.abs(delta) < 6) return;
      editScrollActiveRef.current = true;
      setEditChipsDragging(true);
      if (!node.hasPointerCapture(event.pointerId)) {
        node.setPointerCapture(event.pointerId);
      }
    }
    event.preventDefault();
    editScrollPendingLeftRef.current = editScrollStartLeftRef.current - delta;
    if (editScrollRafRef.current != null) return;
    editScrollRafRef.current = window.requestAnimationFrame(() => {
      const target = editScrollTargetRef.current;
      if (target) target.scrollLeft = editScrollPendingLeftRef.current;
      editScrollRafRef.current = null;
    });
  };
  const handleEditScrollPointerEnd = (event: ReactPointerEvent<HTMLDivElement>) => {
    const node = editScrollTargetRef.current;
    if (!node) return;
    if (editScrollPointerIdRef.current !== event.pointerId) return;
    editScrollPointerIdRef.current = null;
    editScrollTargetRef.current = null;
    editScrollActiveRef.current = false;
    setEditChipsDragging(false);
    if (editScrollRafRef.current != null) {
      window.cancelAnimationFrame(editScrollRafRef.current);
      editScrollRafRef.current = null;
    }
    if (node.hasPointerCapture(event.pointerId)) {
      node.releasePointerCapture(event.pointerId);
    }
  };
  useEffect(() => {
    return () => {
      if (editScrollRafRef.current != null) {
        window.cancelAnimationFrame(editScrollRafRef.current);
        editScrollRafRef.current = null;
      }
    };
  }, []);
  const activityList = (
    <ul className="diary-list">
      {diaryRows.length ? diaryRows.map((row) => {
        if (row.type === "header") {
          return <li key={row.key} className="diary-day-header">{row.label}</li>;
        }
        const entry = row.entry;
        return (
          <DiaryActivityRow
            key={row.key}
            entry={entry}
            imageUrl={entry.coffee_id ? entryImageByCoffeeId.get(String(entry.coffee_id)) ?? "" : ""}
            brand={entry.coffee_id ? entryBrandByCoffeeId.get(String(entry.coffee_id)) ?? "" : ""}
            deleting={deletingEntryId === entry.id}
            onOpenEdit={() => {
              const isWater = (entry.type || "").toUpperCase() === "WATER";
              setEditEntryId(entry.id);
              setEditAmountMl(String(Math.max(1, entry.amount_ml || 1)));
              setEditCaffeineMg(String(Math.max(0, entry.caffeine_mg || 0)));
              setEditPreparationType((entry.preparation_type || "").trim() || (isWater ? "Agua" : "Sin método"));
            }}
            onDelete={async () => {
              if (deletingEntryId === entry.id) return;
              setDeletingEntryId(entry.id);
              try {
                await new Promise((resolve) => window.setTimeout(resolve, 170));
                await onDeleteEntry(entry.id);
              } finally {
                setDeletingEntryId(null);
              }
            }}
          />
        );
      }) : <li className="diary-empty-card">Sin café o agua registrada</li>}
    </ul>
  );
  const pantryList = (
    <ul className="diary-pantry-grid">
      {sortedPantryRows.length ? sortedPantryRows.map((row) => (
        <li
          key={`${row.item.user_id}-${row.item.coffee_id}`}
          className="diary-pantry-card"
          onClick={() => {
            if (!row.coffee?.id) return;
            onOpenCoffee(row.coffee.id);
          }}
          role="button"
          tabIndex={0}
          onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
              event.preventDefault();
              if (!row.coffee?.id) return;
              onOpenCoffee(row.coffee.id);
            }
          }}
        >
          <div className="diary-pantry-media">
            {row.coffee?.image_url ? (
              <img src={row.coffee.image_url} alt={row.coffee.nombre} loading="lazy" />
            ) : (
              <span className="diary-pantry-fallback" aria-hidden="true">{(row.coffee?.nombre || "C").slice(0, 1).toUpperCase()}</span>
            )}
            <div className="diary-pantry-overlay" />
            <div className="diary-pantry-copy">
              <small>{(row.coffee?.marca || "").toUpperCase()}</small>
              <strong>{row.coffee?.nombre ?? row.item.coffee_id}</strong>
            </div>
            <button
              type="button"
              className="diary-pantry-options"
              aria-label="Opciones"
              onClick={(event) => {
                event.stopPropagation();
                setPantryOptionsCoffeeId(row.item.coffee_id);
              }}
            >
              <UiIcon name="more" className="ui-icon" />
            </button>
          </div>
          <div className="diary-pantry-foot">
            <div className="diary-pantry-values">
              <span>{row.item.grams_remaining}g</span>
              <span>{row.item.total_grams > 0 ? Math.round((row.item.grams_remaining / row.item.total_grams) * 100) : 0}%</span>
            </div>
            <div className="diary-pantry-progress" aria-hidden="true">
              <i style={{ width: `${Math.max(0, Math.min(100, row.item.total_grams > 0 ? (row.item.grams_remaining / row.item.total_grams) * 100 : 0))}%` }} />
            </div>
          </div>
        </li>
      )) : <li className="diary-empty-card">No hay café en tu despensa</li>}
    </ul>
  );

  return (
    <>
      <article className="diary-analytics-card">
        <div className="diary-analytics-top">
          <div className="diary-analytics-head-block">
            <p className="metric-label diary-analytics-label">
              CAFEÍNA ESTIMADA
              <span className="diary-analytics-info-wrap">
                <button
                  type="button"
                  className="diary-analytics-info"
                  aria-label="Información de cafeína estimada"
                  aria-expanded={showCaffeineInfo}
                  onClick={() => setShowCaffeineInfo((value) => !value)}
                  onMouseEnter={() => setShowCaffeineInfo(true)}
                  onMouseLeave={() => setShowCaffeineInfo(false)}
                  onBlur={() => setShowCaffeineInfo(false)}
                >
                  i
                </button>
                {showCaffeineInfo ? (
                  <span className="diary-analytics-tooltip" role="tooltip">
                    Estimación basada en tus registros de consumo en el periodo seleccionado.
                  </span>
                ) : null}
              </span>
            </p>
            <p className="analytics-value diary-analytics-main-value">{analytics.caffeine} mg</p>
            <span className={`diary-analytics-trend ${caffeineTrendPct >= 0 ? "is-up" : "is-down"}`.trim()}>
              {caffeineTrendPct >= 0 ? "↑" : "↓"} {Math.abs(caffeineTrendPct)}%
            </span>
          </div>
          <div className="diary-analytics-head-block diary-analytics-hydration">
            <p className="metric-label diary-analytics-label">HIDRATACIÓN</p>
            <p className="analytics-value diary-analytics-main-value">{analytics.hydrationMl} ml</p>
            <span className={`diary-analytics-trend is-water ${hydrationTrendPct >= 0 ? "is-up" : "is-down"}`.trim()}>
              {hydrationTrendPct >= 0 ? "↑" : "↓"} {Math.abs(hydrationTrendPct)}%
            </span>
          </div>
        </div>
        <div
          ref={chartScrollRef}
          className={`diary-chart-scroll ${chartDragging ? "is-dragging" : ""}`.trim()}
          aria-label="Gráfico de consumo"
          onPointerDown={(event) => {
            const node = chartScrollRef.current;
            if (!node) return;
            if (node.scrollWidth <= node.clientWidth) return;
            event.preventDefault();
            chartPointerIdRef.current = event.pointerId;
            chartDragStartXRef.current = event.clientX;
            chartDragStartScrollRef.current = node.scrollLeft;
            setChartDragging(true);
            node.setPointerCapture(event.pointerId);
          }}
          onPointerMove={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            event.preventDefault();
            const delta = event.clientX - chartDragStartXRef.current;
            node.scrollLeft = chartDragStartScrollRef.current - delta;
          }}
          onPointerUp={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            chartPointerIdRef.current = null;
            setChartDragging(false);
            if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
          }}
          onPointerCancel={(event) => {
            const node = chartScrollRef.current;
            if (!node || chartPointerIdRef.current !== event.pointerId) return;
            chartPointerIdRef.current = null;
            setChartDragging(false);
            if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
          }}
          onPointerLeave={() => {
            chartPointerIdRef.current = null;
            setChartDragging(false);
          }}
        >
          <div className={`diary-chart ${period === "7d" ? "is-week" : ""}`.trim()}>
            {chartData.map((item) => {
              const caffeineRatio = Math.min(1, item.caffeine / 400);
              const waterRatio = Math.min(1, item.water / 2000);
              const hasCaffeine = item.caffeine > 0;
              const hasWater = item.water > 0;
              const maxBarHeight = 136;
              const minActiveBarHeight = 14;
              const emptyBarHeight = 4;
              const caffeineHeight = hasCaffeine
                ? Math.max(minActiveBarHeight, Math.round(caffeineRatio * maxBarHeight))
                : emptyBarHeight;
              const waterHeight = hasWater
                ? Math.max(minActiveBarHeight, Math.round(waterRatio * maxBarHeight))
                : emptyBarHeight;
              return (
                <div key={item.label} className="diary-chart-col">
                  <div className="diary-chart-bars">
                    <div className="diary-chart-bar-wrap">
                      {hasCaffeine ? <small className="diary-chart-bar-value">{Math.round(item.caffeine)}</small> : null}
                      <span
                        className={`diary-chart-bar caffeine ${hasCaffeine ? "is-active" : ""}`.trim()}
                        style={{ height: `${caffeineHeight}px` }}
                        title={`Cafeína: ${Math.round(item.caffeine)} mg`}
                        aria-label={`Cafeína ${Math.round(item.caffeine)} miligramos`}
                      />
                    </div>
                    <div className="diary-chart-bar-wrap">
                      {hasWater ? <small className="diary-chart-bar-value">{Math.round(item.water)}</small> : null}
                      <span
                        className={`diary-chart-bar water ${hasWater ? "is-active" : ""}`.trim()}
                        style={{ height: `${waterHeight}px` }}
                        title={`Agua: ${Math.round(item.water)} ml`}
                        aria-label={`Agua ${Math.round(item.water)} mililitros`}
                      />
                    </div>
                  </div>
                  <small>{item.label}</small>
                </div>
              );
            })}
          </div>
        </div>
        <div className="analytics-grid">
          <div className="diary-metric-box">
            <UiIcon name="star" className="ui-icon" />
            <p className="analytics-value">{analytics.avgCaffeine} mg</p>
            <p className="metric-label">MEDIA</p>
          </div>
          <div className="diary-metric-box">
            <UiIcon name="coffee" className="ui-icon" />
            <p className="analytics-value">{analytics.coffeeCups}</p>
            <p className="metric-label">TAZAS</p>
          </div>
          <div className="diary-metric-box">
            <UiIcon name="taste-watery" className="ui-icon" />
            <p className="analytics-value">{hydrationProgressPct}%</p>
            <p className="metric-label">PROGRESO</p>
          </div>
        </div>
      </article>

      {mode !== "desktop" ? (
        <>
          <div className="premium-tabs diary-tabs" role="tablist" aria-label="Tabs diario">
            <button type="button" className={`premium-tab ${tab === "actividad" ? "is-active" : ""}`} role="tab" aria-selected={tab === "actividad"} onClick={() => setTab("actividad")}>ACTIVIDAD</button>
            <button type="button" className={`premium-tab ${tab === "despensa" ? "is-active" : ""}`} role="tab" aria-selected={tab === "despensa"} onClick={() => setTab("despensa")}>DESPENSA</button>
          </div>
          {tab === "actividad" ? activityList : null}
          {tab === "despensa" ? pantryList : null}
        </>
      ) : (
        <section className="diary-desktop-columns" aria-label="Actividad y despensa">
          <div className="diary-section-block">
            <h3 className="diary-section-title">Actividad</h3>
            {activityList}
          </div>
          <div className="diary-section-block">
            <h3 className="diary-section-title">Despensa</h3>
            {pantryList}
          </div>
        </section>
      )}

      {pantryOptionsCoffeeId ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Opciones despensa" onClick={() => setPantryOptionsCoffeeId(null)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">OPCIONES DESPENSA</strong>
            </header>
            <div className="diary-sheet-list">
              <button
                type="button"
                className="diary-sheet-action"
                onClick={() => {
                  const row = sortedPantryRows.find((item) => item.item.coffee_id === pantryOptionsCoffeeId);
                  if (!row) return;
                  setStockEditCoffeeId(row.item.coffee_id);
                  setStockEditTotal(String(Math.max(1, row.item.total_grams)));
                  setStockEditRemaining(String(Math.max(0, row.item.grams_remaining)));
                  setPantryOptionsCoffeeId(null);
                }}
              >
                <UiIcon name="edit" className="ui-icon" />
                <span>Editar stock</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
              <button
                type="button"
                className="diary-sheet-action"
                disabled={removingStock}
                onClick={() => {
                  setPantryDeleteConfirmCoffeeId(pantryOptionsCoffeeId);
                  setPantryOptionsCoffeeId(null);
                }}
              >
                <UiIcon name="trash" className="ui-icon" />
                <span>Eliminar de despensa</span>
                <UiIcon name="chevron-right" className="ui-icon" />
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {pantryDeleteConfirmCoffeeId ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Confirmar eliminación" onClick={() => setPantryDeleteConfirmCoffeeId(null)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">ELIMINAR DE DESPENSA</strong>
            </header>
            <div className="diary-sheet-form">
              <p className="feed-meta">¿Seguro que quieres eliminar este café de tu despensa?</p>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={() => setPantryDeleteConfirmCoffeeId(null)} disabled={removingStock}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  disabled={removingStock}
                  onClick={async () => {
                    if (removingStock) return;
                    setRemovingStock(true);
                    try {
                      await onRemovePantryItem(pantryDeleteConfirmCoffeeId);
                      setPantryDeleteConfirmCoffeeId(null);
                    } finally {
                      setRemovingStock(false);
                    }
                  }}
                >
                  {removingStock ? "Eliminando..." : "Eliminar"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {stockEditTarget ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Editar stock" onClick={() => setStockEditCoffeeId(null)}>
          <div className="sheet-card diary-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">EDITAR STOCK</strong>
            </header>
            <div className="diary-sheet-form">
              <label>
                <span>Total (g)</span>
                <input
                  className="search-wide"
                  type="number"
                  min={1}
                  step={1}
                  value={stockEditTotal}
                  onChange={(event) => {
                    const nextTotal = event.target.value;
                    setStockEditTotal(nextTotal);
                    const parsedNextTotal = Number(nextTotal || 0);
                    const parsedCurrentRemaining = Number(stockEditRemaining || 0);
                    if (
                      Number.isFinite(parsedNextTotal) &&
                      parsedNextTotal >= 0 &&
                      Number.isFinite(parsedCurrentRemaining) &&
                      parsedCurrentRemaining > parsedNextTotal
                    ) {
                      setStockEditRemaining(String(parsedNextTotal));
                    }
                  }}
                />
              </label>
              <label>
                <span>Restante (g)</span>
                <input
                  className="search-wide"
                  type="number"
                  min={0}
                  max={Math.max(0, parsedStockTotal || 0)}
                  step={1}
                  value={stockEditRemaining}
                  onChange={(event) => setStockEditRemaining(event.target.value)}
                />
              </label>
              {!canSaveStock && stockValidationMessage ? <p className="diary-inline-error">{stockValidationMessage}</p> : null}
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={() => setStockEditCoffeeId(null)} disabled={savingStock}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  disabled={savingStock || !canSaveStock}
                  onClick={async () => {
                    if (savingStock || !canSaveStock) return;
                    setSavingStock(true);
                    try {
                      await onUpdatePantryStock(
                        stockEditTarget.item.coffee_id,
                        parsedStockTotal,
                        parsedStockRemaining
                      );
                      setStockEditCoffeeId(null);
                    } finally {
                      setSavingStock(false);
                    }
                  }}
                >
                  {savingStock ? "Guardando..." : "Guardar"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {editEntryTarget ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Editar entrada" onClick={() => setEditEntryId(null)}>
          <div className="sheet-card diary-sheet diary-edit-entry-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">Editar registro de café</strong>
            </header>
            <div className="diary-sheet-form">
              {editEntryIsWater ? (
                <div className="diary-water-presets diary-edit-entry-presets is-water">
                  {[250, 500, 750].map((value) => (
                    <button
                      key={value}
                      type="button"
                      className={`chip-button period-chip ${Number(editAmountMl) === value ? "is-active" : ""}`.trim()}
                      onClick={() => setEditAmountMl(String(value))}
                    >
                      {value} ml
                    </button>
                  ))}
                </div>
              ) : (
                <div className="diary-edit-entry-coffee-layout">
                  <section className="diary-edit-entry-block">
                    <h4 className="diary-edit-entry-block-title">Preparación</h4>
                    <div
                      ref={editPrepScrollRef}
                      className={`diary-edit-entry-presets is-coffee ${editChipsDragging ? "is-dragging" : ""}`.trim()}
                      onPointerDown={(event) => handleEditScrollPointerDown(event, "prep")}
                      onPointerMove={handleEditScrollPointerMove}
                      onPointerUp={handleEditScrollPointerEnd}
                      onPointerCancel={handleEditScrollPointerEnd}
                    >
                      {editMethodOptions.map((method) => (
                        <button
                          key={method.label}
                          type="button"
                          className={`chip-button period-chip diary-edit-entry-method-chip ${normalizeLookupText(editPreparationType) === normalizeLookupText(method.label) ? "is-active" : ""}`.trim()}
                          onClick={() => setEditPreparationType(method.label)}
                        >
                          <img src={`/android-drawable/${method.drawable}`} alt="" aria-hidden="true" />
                          <span>{method.label}</span>
                        </button>
                      ))}
                    </div>
                  </section>

                  <section className="diary-edit-entry-metrics-grid">
                    <label className="diary-edit-entry-metric-field">
                      <span>Cafeína (mg)</span>
                      <div className="diary-edit-entry-metric-value">
                        <UiIcon name="caffeine" className="ui-icon" />
                        <input
                          className="diary-edit-entry-metric-input"
                          type="number"
                          inputMode="numeric"
                          min={0}
                          step={1}
                          value={editCaffeineMg}
                          placeholder="0"
                          onChange={(event) => setEditCaffeineMg(event.target.value)}
                        />
                      </div>
                    </label>
                    <div className="diary-edit-entry-metric-field is-readonly">
                      <span>Dosis (g)</span>
                      <div className="diary-edit-entry-metric-value">
                        <UiIcon name="grind" className="ui-icon" />
                        <strong>{Number(parsedEditDose).toFixed(1).replace(".", ",")}</strong>
                      </div>
                    </div>
                  </section>

                  <section className="diary-edit-entry-block">
                    <h4 className="diary-edit-entry-block-title">Tamaño</h4>
                    <div
                      ref={editSizeScrollRef}
                      className={`diary-coffee-size-presets diary-edit-entry-size-presets ${editChipsDragging ? "is-dragging" : ""}`.trim()}
                      onPointerDown={(event) => handleEditScrollPointerDown(event, "size")}
                      onPointerMove={handleEditScrollPointerMove}
                      onPointerUp={handleEditScrollPointerEnd}
                      onPointerCancel={handleEditScrollPointerEnd}
                    >
                      {editSizeOptions.map((size) => (
                        <button
                          key={size.label}
                          type="button"
                          className={`chip-button period-chip diary-coffee-size-chip ${Math.abs(Number(editAmountMl || 0) - size.ml) <= 20 ? "is-active" : ""}`.trim()}
                          onClick={() => setEditAmountMl(String(size.ml))}
                        >
                          <img src={`/android-drawable/${size.drawable}`} alt="" aria-hidden="true" />
                          <span>{size.label}</span>
                          <small>{size.range}</small>
                        </button>
                      ))}
                    </div>
                  </section>

                  <section className="diary-edit-entry-metric-field is-readonly is-time">
                    <span>Tiempo (HH:mm)</span>
                    <div className="diary-edit-entry-metric-value">
                      <UiIcon name="clock" className="ui-icon" />
                      <strong>{editEntryTime}</strong>
                    </div>
                  </section>
                </div>
              )}
              {!canSaveEditEntry && editValidationMessage ? <p className="diary-inline-error">{editValidationMessage}</p> : null}
              <div className="diary-sheet-form-actions">
                <button
                  type="button"
                  className="action-button diary-edit-entry-save"
                  disabled={savingEditEntry || !canSaveEditEntry}
                  onClick={async () => {
                    if (savingEditEntry || !canSaveEditEntry) return;
                    setSavingEditEntry(true);
                    try {
                      await onEditEntry(
                        editEntryTarget.id,
                        parsedEditAmount,
                        editEntryIsWater ? 0 : parsedEditCaffeine,
                        editPreparationType
                      );
                      setEditEntryId(null);
                    } finally {
                      setSavingEditEntry(false);
                    }
                  }}
                >
                  {savingEditEntry ? "Guardando..." : "Guardar cambios"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );
}

function DiaryActivityRow({
  entry,
  imageUrl,
  brand,
  deleting,
  onOpenEdit,
  onDelete
}: {
  entry: DiaryEntryRow;
  imageUrl: string;
  brand?: string;
  deleting: boolean;
  onOpenEdit: () => void;
  onDelete: () => Promise<void>;
}) {
  const isWaterEntry = (entry.type || "").toUpperCase() === "WATER";
  const entryTitle = entry.coffee_name || (isWaterEntry ? "Agua" : "Entrada");
  const entrySubtitle = (brand || (isWaterEntry ? "Agua" : "Café")).toUpperCase();
  const prepValue = (entry.preparation_type || "").trim() || (isWaterEntry ? "Agua" : "-");
  const doseFromPrep = ((entry.preparation_type || "").match(/(\d+(?:[.,]\d+)?)\s*g/i)?.[1] || "").replace(",", ".");
  const doseValue = doseFromPrep ? `${Math.round(Number(doseFromPrep))} g` : (isWaterEntry ? "-" : "15 g");
  const sizeValue = isWaterEntry
    ? `${Math.max(0, entry.amount_ml || 0)} ml`
    : (() => {
      const ml = Math.max(0, entry.amount_ml || 0);
      if (ml <= 45) return "Espresso";
      if (ml <= 120) return "Corto";
      if (ml <= 220) return "Mediano";
      return "Largo";
    })();
  const timeValue = new Date(entry.timestamp).toLocaleTimeString("es-ES", { hour: "2-digit", minute: "2-digit" });
  const prepDrawable = (() => {
    const normalized = normalizeLookupText(prepValue);
    if (normalized.includes("espresso")) return "maq_espresso.png";
    if (normalized.includes("v60") || normalized.includes("hario")) return "maq_hario_v60.png";
    if (normalized.includes("aero")) return "maq_aeropress.png";
    if (normalized.includes("moka") || normalized.includes("italiana")) return "maq_italiana.png";
    if (normalized.includes("chemex")) return "maq_chemex.png";
    if (normalized.includes("prensa")) return "maq_prensa_francesa.png";
    if (normalized.includes("goteo")) return "maq_goteo.png";
    if (normalized.includes("sifon")) return "maq_sifon.png";
    if (normalized.includes("turco")) return "maq_turco.png";
    return "maq_manual.png";
  })();
  const sizeDrawable = (() => {
    if (isWaterEntry) return null;
    if (sizeValue === "Espresso") return "taza_espresso.png";
    if (sizeValue === "Corto") return "taza_pequeno.png";
    if (sizeValue === "Mediano") return "taza_mediano.png";
    return "taza_grande.png";
  })();
  const metaItems: Array<{ key: string; icon?: IconName; drawable?: string; label: string; value: string }> = [
    { key: "caffeine", icon: "caffeine", label: "Cafeína", value: `${Math.max(0, entry.caffeine_mg || 0)} mg` },
    { key: "prep", drawable: prepDrawable, label: "Preparación", value: prepValue },
    { key: "dose", icon: "grind", label: "Dosis", value: doseValue },
    { key: "size", drawable: sizeDrawable ?? undefined, icon: sizeDrawable ? undefined : "stock", label: "Tamaño", value: sizeValue }
  ];
  const metaScrollRef = useRef<HTMLDivElement | null>(null);
  const metaPointerIdRef = useRef<number | null>(null);
  const metaDragStartXRef = useRef(0);
  const metaDragStartScrollRef = useRef(0);
  const metaRafRef = useRef<number | null>(null);
  const metaPendingScrollRef = useRef(0);
  const metaInteractingRef = useRef(false);
  const [metaDragging, setMetaDragging] = useState(false);
  const [offsetX, setOffsetX] = useState(0);
  const pointerIdRef = useRef<number | null>(null);
  const startXRef = useRef(0);
  const startYRef = useRef(0);
  const swipeActiveRef = useRef(false);
  const movedRef = useRef(false);
  const threshold = -76;
  const maxDrag = -124;

  const handlePointerDown = (event: ReactPointerEvent<HTMLLIElement>) => {
    const target = event.target as HTMLElement | null;
    if (metaInteractingRef.current || target?.closest(".diary-entry-meta-scroll")) return;
    if (deleting) return;
    if (pointerIdRef.current != null) return;
    pointerIdRef.current = event.pointerId;
    startXRef.current = event.clientX;
    startYRef.current = event.clientY;
    swipeActiveRef.current = false;
    movedRef.current = false;
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLLIElement>) => {
    if (metaInteractingRef.current) return;
    if (pointerIdRef.current !== event.pointerId) return;
    const dx = event.clientX - startXRef.current;
    const dy = event.clientY - startYRef.current;
    const absDx = Math.abs(dx);
    const absDy = Math.abs(dy);
    if (!swipeActiveRef.current) {
      if (absDx < 7 && absDy < 7) return;
      if (absDy > absDx + 4) {
        pointerIdRef.current = null;
        setOffsetX(0);
        return;
      }
      if (dx < 0 && absDx > absDy) {
        swipeActiveRef.current = true;
        movedRef.current = true;
        event.currentTarget.setPointerCapture(event.pointerId);
      } else {
        pointerIdRef.current = null;
        setOffsetX(0);
        return;
      }
    }
    if (dx >= 0) {
      setOffsetX(0);
      return;
    }
    setOffsetX(Math.max(maxDrag, dx));
  };

  const handlePointerEnd = async (event: ReactPointerEvent<HTMLLIElement>) => {
    if (metaInteractingRef.current) return;
    if (pointerIdRef.current !== event.pointerId) return;
    pointerIdRef.current = null;
    swipeActiveRef.current = false;
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }
    if (offsetX <= threshold) {
      setOffsetX(0);
      await onDelete();
      return;
    }
    setOffsetX(0);
    window.setTimeout(() => {
      movedRef.current = false;
    }, 80);
  };

  return (
    <li
      className={`diary-card-swipe-wrap ${offsetX < -1 ? "is-swiping" : ""} ${deleting ? "is-dismissing" : ""}`.trim()}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={(event) => {
        void handlePointerEnd(event);
      }}
      onPointerCancel={(event) => {
        void handlePointerEnd(event);
      }}
      onClick={(event) => {
        const target = event.target as HTMLElement | null;
        if (metaInteractingRef.current || target?.closest(".diary-entry-meta-scroll")) return;
        if (movedRef.current || deleting) return;
        onOpenEdit();
      }}
    >
      <div className="diary-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div className="diary-swipe-content" style={{ transform: `translateX(${offsetX}px)` }}>
        <div className="diary-card">
          <div className="diary-entry-head">
            <div className="diary-entry-media">
              {imageUrl ? (
                <img src={imageUrl} alt={entryTitle} loading="lazy" />
              ) : (
                <span className="diary-entry-fallback" aria-hidden="true">
                  <UiIcon name={isWaterEntry ? "stock" : "coffee"} className="ui-icon" />
                </span>
              )}
            </div>
            <div className="diary-entry-copy">
              <p className="feed-user">{entryTitle}</p>
              <p className="feed-meta diary-entry-brand">{entrySubtitle.toUpperCase()}</p>
            </div>
            <div className="diary-entry-time-pill">
              <UiIcon name="clock" className="ui-icon" />
              <span>{timeValue}</span>
            </div>
          </div>
          <div
            ref={metaScrollRef}
            className={`diary-entry-meta-scroll ${metaDragging ? "is-dragging" : ""}`.trim()}
            onPointerDown={(event) => {
              event.stopPropagation();
              metaInteractingRef.current = true;
              const node = metaScrollRef.current;
              if (!node) return;
              if (event.pointerType !== "mouse") return;
              if (node.scrollWidth <= node.clientWidth) return;
              event.preventDefault();
              metaPointerIdRef.current = event.pointerId;
              metaDragStartXRef.current = event.clientX;
              metaDragStartScrollRef.current = node.scrollLeft;
              setMetaDragging(true);
              node.setPointerCapture(event.pointerId);
            }}
            onPointerMove={(event) => {
              event.stopPropagation();
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              event.preventDefault();
              const delta = event.clientX - metaDragStartXRef.current;
              metaPendingScrollRef.current = metaDragStartScrollRef.current - delta;
              if (metaRafRef.current != null) return;
              metaRafRef.current = window.requestAnimationFrame(() => {
                const target = metaScrollRef.current;
                if (target) target.scrollLeft = metaPendingScrollRef.current;
                metaRafRef.current = null;
              });
            }}
            onPointerUp={(event) => {
              event.stopPropagation();
              metaInteractingRef.current = false;
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
              if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
            }}
            onPointerCancel={(event) => {
              event.stopPropagation();
              metaInteractingRef.current = false;
              const node = metaScrollRef.current;
              if (!node) return;
              if (metaPointerIdRef.current !== event.pointerId) return;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
              if (node.hasPointerCapture(event.pointerId)) node.releasePointerCapture(event.pointerId);
            }}
            onPointerLeave={() => {
              metaInteractingRef.current = false;
              metaPointerIdRef.current = null;
              setMetaDragging(false);
              if (metaRafRef.current != null) {
                window.cancelAnimationFrame(metaRafRef.current);
                metaRafRef.current = null;
              }
            }}
          >
            <div className="diary-entry-meta-grid">
              {metaItems.map((item) => (
                <div key={item.key} className="diary-entry-meta-item">
                  {item.drawable ? (
                    <img className="diary-entry-meta-drawable" src={`/android-drawable/${item.drawable}`} alt="" aria-hidden="true" />
                  ) : item.icon ? (
                    <UiIcon name={item.icon} className="ui-icon" />
                  ) : null}
                  <span className="diary-entry-meta-label">{item.label}</span>
                  <strong className="diary-entry-meta-value">{item.value}</strong>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </li>
  );
}

function ProfileFavoriteItem({
  coffee,
  onOpenCoffee,
  onRemoveFavorite
}: {
  coffee: CoffeeRow;
  onOpenCoffee: (coffeeId: string) => void;
  onRemoveFavorite: (coffeeId: string) => Promise<void>;
}) {
  const [isRemoving, setIsRemoving] = useState(false);
  const [offsetX, setOffsetX] = useState(0);
  const [isSwiping, setIsSwiping] = useState(false);
  const startXRef = useRef<number | null>(null);
  const pointerIdRef = useRef<number | null>(null);
  const offsetRef = useRef(0);
  const swipeThreshold = -72;
  const maxLeft = -110;

  const resetSwipe = useCallback(() => {
    setOffsetX(0);
    setIsSwiping(false);
    startXRef.current = null;
    pointerIdRef.current = null;
    offsetRef.current = 0;
  }, []);
  useEffect(() => {
    offsetRef.current = offsetX;
  }, [offsetX]);

  return (
    <li className="profile-favorite-item">
      <div className="profile-favorite-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div
        className={`coffee-card coffee-card-interactive profile-favorite-row ${offsetX < -1 ? "is-swiping" : ""}`.trim()}
        style={{ transform: `translateX(${offsetX}px)` }}
        role="button"
        tabIndex={0}
        onClick={(event) => {
          if (Math.abs(offsetX) > 4 || isSwiping) {
            event.preventDefault();
            return;
          }
          onOpenCoffee(coffee.id);
        }}
        onPointerDown={(event) => {
          if (isRemoving) return;
          pointerIdRef.current = event.pointerId;
          startXRef.current = event.clientX;
          setIsSwiping(true);
          event.currentTarget.setPointerCapture(event.pointerId);
        }}
        onPointerMove={(event) => {
          if (!isSwiping || startXRef.current == null || pointerIdRef.current !== event.pointerId) return;
          const delta = event.clientX - startXRef.current;
          const next = Math.max(maxLeft, Math.min(0, offsetRef.current + delta));
          setOffsetX(next);
        }}
        onPointerUp={async (event) => {
          if (pointerIdRef.current !== event.pointerId) return;
          const shouldDelete = offsetX <= swipeThreshold;
          if (shouldDelete && !isRemoving) {
            setIsRemoving(true);
            try {
              await onRemoveFavorite(coffee.id);
            } finally {
              setIsRemoving(false);
              resetSwipe();
            }
            return;
          }
          resetSwipe();
        }}
        onPointerCancel={() => resetSwipe()}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            onOpenCoffee(coffee.id);
          }
        }}
      >
        <span className="profile-favorite-media">
          {coffee.image_url ? (
            <img className="profile-favorite-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" />
          ) : (
            <span className="profile-favorite-image profile-favorite-fallback" aria-hidden="true">{coffee.nombre.slice(0, 1).toUpperCase()}</span>
          )}
        </span>
        <span className="profile-favorite-copy">
          <strong>{coffee.nombre}</strong>
          <em>{coffee.marca || "Marca"}</em>
        </span>
        <button
          type="button"
          className="profile-favorite-remove"
          aria-label="Quitar de favoritos"
          disabled={isRemoving}
          onClick={async (event) => {
            event.preventDefault();
            event.stopPropagation();
            if (isRemoving) return;
            setIsRemoving(true);
            try {
              await onRemoveFavorite(coffee.id);
            } finally {
              setIsRemoving(false);
            }
          }}
        >
          <UiIcon name="favorite-filled" className="ui-icon" />
        </button>
      </div>
    </li>
  );
}

function ProfileView({
  user,
  mode,
  tab,
  setTab,
  posts,
  favoriteCoffees,
  allCoffees,
  coffeeReviews,
  coffeeSensoryProfiles,
  followers,
  following,
  onOpenCoffee,
  onOpenUserProfile,
  canEditProfile,
  canFollowProfile,
  isFollowingProfile,
  onToggleFollowProfile,
  onSaveProfile,
  onRemoveFavorite,
  onEditPost,
  onDeletePost,
  onToggleLike,
  onOpenComments,
  externalEditProfileSignal,
  sidePanel
}: {
  user: UserRow;
  mode: ViewMode;
  tab: "posts" | "adn" | "favoritos";
  setTab: (value: "posts" | "adn" | "favoritos") => void;
  posts: TimelineCard[];
  favoriteCoffees: CoffeeRow[];
  allCoffees: CoffeeRow[];
  coffeeReviews: CoffeeReviewRow[];
  coffeeSensoryProfiles: CoffeeSensoryProfileRow[];
  followers: number;
  following: number;
  onOpenCoffee: (coffeeId: string) => void;
  onOpenUserProfile: (userId: number) => void;
  canEditProfile: boolean;
  canFollowProfile: boolean;
  isFollowingProfile: boolean;
  onToggleFollowProfile: () => Promise<void>;
  onSaveProfile: (
    userId: number,
    fullName: string,
    bio: string,
    avatarFile?: File | null,
    removeAvatar?: boolean
  ) => Promise<void>;
  onRemoveFavorite: (coffeeId: string) => Promise<void>;
  onEditPost: (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => Promise<void>;
  onDeletePost: (postId: string) => Promise<void>;
  onToggleLike: (postId: string) => void;
  onOpenComments: (postId: string) => void;
  externalEditProfileSignal: number;
  sidePanel?: ReactNode;
}) {
  const initials = user.full_name
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
  const coffeesById = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    allCoffees.forEach((coffee) => map.set(coffee.id, coffee));
    return map;
  }, [allCoffees]);
  const sensoryAverages = useMemo(() => {
    type SensorySample = { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
    const samples: SensorySample[] = [];
    const addSample = (source: {
      aroma?: number | null;
      sabor?: number | null;
      cuerpo?: number | null;
      acidez?: number | null;
      dulzura?: number | null;
    } | null | undefined) => {
      if (!source) return;
      const sample = {
        aroma: Math.max(0, Number(source.aroma ?? 0)),
        sabor: Math.max(0, Number(source.sabor ?? 0)),
        cuerpo: Math.max(0, Number(source.cuerpo ?? 0)),
        acidez: Math.max(0, Number(source.acidez ?? 0)),
        dulzura: Math.max(0, Number(source.dulzura ?? 0))
      };
      if (sample.aroma || sample.sabor || sample.cuerpo || sample.acidez || sample.dulzura) {
        samples.push(sample);
      }
    };

    favoriteCoffees.forEach((coffee) => addSample(coffee));
    coffeeReviews
      .filter((review) => review.user_id === user.id)
      .forEach((review) => addSample(coffeesById.get(review.coffee_id)));
    coffeeSensoryProfiles
      .filter((profile) => profile.user_id === user.id)
      .forEach((profile) => addSample(profile));

    if (!samples.length) return { aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0, count: 0 };

    const totals = samples.reduce(
      (acc, coffee) => ({
        aroma: acc.aroma + Math.max(0, Number(coffee.aroma ?? 0)),
        sabor: acc.sabor + Math.max(0, Number(coffee.sabor ?? 0)),
        cuerpo: acc.cuerpo + Math.max(0, Number(coffee.cuerpo ?? 0)),
        acidez: acc.acidez + Math.max(0, Number(coffee.acidez ?? 0)),
        dulzura: acc.dulzura + Math.max(0, Number(coffee.dulzura ?? 0))
      }),
      { aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 }
    );
    const count = Math.max(1, samples.length);
    return {
      aroma: Number((totals.aroma / count).toFixed(1)),
      sabor: Number((totals.sabor / count).toFixed(1)),
      cuerpo: Number((totals.cuerpo / count).toFixed(1)),
      acidez: Number((totals.acidez / count).toFixed(1)),
      dulzura: Number((totals.dulzura / count).toFixed(1)),
      count: samples.length
    };
  }, [coffeesById, coffeeReviews, coffeeSensoryProfiles, favoriteCoffees, user.id]);
  const adnRadar = useMemo(() => {
    const labels = ["Aroma", "Sabor", "Cuerpo", "Acidez", "Dulzura"];
    const values = [sensoryAverages.aroma, sensoryAverages.sabor, sensoryAverages.cuerpo, sensoryAverages.acidez, sensoryAverages.dulzura]
      .map((value) => Math.max(0, Math.min(10, Number(value) || 0)));
    const cx = 120;
    const cy = 90;
    const radius = 60;
    const angleStep = (Math.PI * 2) / labels.length;
    const startAngle = -Math.PI / 2;
    const rings = [1, 2, 3, 4, 5].map((level) => {
      const factor = level / 5;
      return labels.map((_, index) => {
        const angle = startAngle + index * angleStep;
        return {
          x: cx + Math.cos(angle) * radius * factor,
          y: cy + Math.sin(angle) * radius * factor
        };
      });
    });
    const axes = labels.map((_, index) => {
      const angle = startAngle + index * angleStep;
      return {
        x1: cx,
        y1: cy,
        x2: cx + Math.cos(angle) * radius,
        y2: cy + Math.sin(angle) * radius
      };
    });
    const dataPoints = labels.map((_, index) => {
      const angle = startAngle + index * angleStep;
      const factor = values[index] / 10;
      return {
        x: cx + Math.cos(angle) * radius * factor,
        y: cy + Math.sin(angle) * radius * factor
      };
    });
    const labelPoints = labels.map((label, index) => {
      const angle = startAngle + index * angleStep;
      const dist = radius + 16;
      return {
        label,
        x: cx + Math.cos(angle) * dist,
        y: cy + Math.sin(angle) * dist
      };
    });
    return { rings, axes, dataPoints, labelPoints };
  }, [sensoryAverages.acidez, sensoryAverages.aroma, sensoryAverages.cuerpo, sensoryAverages.dulzura, sensoryAverages.sabor]);
  const desktopPostColumns = useMemo(() => {
    const cols: [TimelineCard[], TimelineCard[], TimelineCard[]] = [[], [], []];
    posts.forEach((post, index) => {
      cols[index % 3].push(post);
    });
    return cols;
  }, [posts]);
  const [showEditProfile, setShowEditProfile] = useState(false);
  const [editNameDraft, setEditNameDraft] = useState(user.full_name);
  const [editBioDraft, setEditBioDraft] = useState(user.bio ?? "");
  const [editAvatarFile, setEditAvatarFile] = useState<File | null>(null);
  const [editAvatarPreview, setEditAvatarPreview] = useState("");
  const [removeAvatarDraft, setRemoveAvatarDraft] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [postMenuId, setPostMenuId] = useState<string | null>(null);
  const [editPostId, setEditPostId] = useState<string | null>(null);
  const [editPostText, setEditPostText] = useState("");
  const [editPostImageUrl, setEditPostImageUrl] = useState("");
  const [editPostImageFile, setEditPostImageFile] = useState<File | null>(null);
  const [savingPostEdit, setSavingPostEdit] = useState(false);
  const [togglingFollow, setTogglingFollow] = useState(false);
  const [avatarLoadFailed, setAvatarLoadFailed] = useState(false);
  const [likeBurstPostId, setLikeBurstPostId] = useState<string | null>(null);
  const [showAdnAnalysisSheet, setShowAdnAnalysisSheet] = useState(false);
  const likeBurstTimerRef = useRef<number | null>(null);
  const editAvatarInputRef = useRef<HTMLInputElement | null>(null);
  const editPostImageInputRef = useRef<HTMLInputElement | null>(null);
  useEffect(() => {
    setEditNameDraft(user.full_name);
    setEditBioDraft(user.bio ?? "");
    setEditAvatarFile(null);
    setEditAvatarPreview("");
    setRemoveAvatarDraft(false);
  }, [user.bio, user.full_name]);
  useEffect(() => {
    setAvatarLoadFailed(false);
  }, [user.avatar_url]);
  useEffect(() => {
    if (!canEditProfile) return;
    if (externalEditProfileSignal <= 0) return;
    setShowEditProfile(true);
  }, [canEditProfile, externalEditProfileSignal]);
  const closeEditProfileModal = useCallback(() => {
    if (editAvatarPreview.startsWith("blob:")) URL.revokeObjectURL(editAvatarPreview);
    setEditAvatarFile(null);
    setEditAvatarPreview("");
    setRemoveAvatarDraft(false);
    setShowEditProfile(false);
  }, [editAvatarPreview]);
  useEffect(() => {
    return () => {
      if (editAvatarPreview.startsWith("blob:")) URL.revokeObjectURL(editAvatarPreview);
    };
  }, [editAvatarPreview]);
  const closeEditPostModal = useCallback(() => {
    if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
    setEditPostId(null);
    setEditPostImageFile(null);
    setEditPostImageUrl("");
    setEditPostText("");
  }, [editPostImageUrl]);
  const adnAnalysis = useMemo(() => {
    const traits = [
      { label: "Aroma", value: sensoryAverages.aroma },
      { label: "Sabor", value: sensoryAverages.sabor },
      { label: "Cuerpo", value: sensoryAverages.cuerpo },
      { label: "Acidez", value: sensoryAverages.acidez },
      { label: "Dulzura", value: sensoryAverages.dulzura }
    ].sort((a, b) => b.value - a.value);
    const highest = traits[0] ?? null;
    const second = traits[1] ?? null;
    const lead = highest
      ? second && second.value > 3
        ? `Tu paladar es una combinación experta de ${highest.label.toLowerCase()} y ${second.label.toLowerCase()}.`
        : `Tu paladar destaca principalmente por preferir notas de ${highest.label.toLowerCase()}.`
      : "Aún no hay datos suficientes para calcular tu paladar.";
    const description = highest
      ? highest.label === "Acidez"
        ? "Buscas brillo en cada taza. Te apasionan los perfiles cítricos y vibrantes de cafés de altura (1500m+)."
        : highest.label === "Dulzura"
          ? "Eres amante de lo meloso. Prefieres cafés con procesos naturales que resaltan notas de chocolate, caramelo y frutas maduras."
          : highest.label === "Cuerpo"
            ? "Para ti, la textura lo es todo. Disfrutas de esa sensación aterciopelada y densa, típica de tuestes medios y perfiles clásicos."
            : highest.label === "Aroma"
              ? "Tu ritual empieza con el olfato. Te atraen las fragancias complejas, florales y especiadas que inundan la habitación."
              : highest.label === "Sabor"
                ? "Buscas la máxima intensidad y complejidad gustativa. Valoras la persistencia de las notas tras cada sorbo."
                : "Disfrutas de una complejidad excepcional y un balance perfectamente equilibrado en tu ADN cafetero."
      : "Marca cafés como favoritos y añade opiniones o reseñas para construir tu ADN sensorial.";
    const recommendation = highest
      ? highest.label === "Acidez"
        ? { type: "Lavados de alta montaña", origin: "Etiopía o Colombia (Nariño)" }
        : highest.label === "Dulzura"
          ? { type: "Procesos Natural o Honey", origin: "Brasil o El Salvador" }
          : highest.label === "Cuerpo"
            ? { type: "Tuestes medios / Naturales", origin: "Sumatra o Guatemala" }
            : highest.label === "Aroma"
              ? { type: "Variedades florales / Geishas", origin: "Panamá o Ruanda" }
              : highest.label === "Sabor"
                ? { type: "Micro-lotes de especialidad", origin: "Costa Rica o Kenia" }
                : { type: "Blends equilibrados", origin: "Cualquier origen de especialidad" }
      : { type: "Blends equilibrados", origin: "Cualquier origen de especialidad" };
    return { lead, description, recommendation };
  }, [sensoryAverages.acidez, sensoryAverages.aroma, sensoryAverages.cuerpo, sensoryAverages.dulzura, sensoryAverages.sabor]);
  useEffect(() => {
    return () => {
      if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
    };
  }, [editPostImageUrl]);
  useEffect(() => {
    return () => {
      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
    };
  }, []);
  const content = (
    <>
      <article className="profile-hero profile-hero-card">
        <div className="profile-hero-main">
          <div className="profile-avatar-wrap" aria-hidden="true">
            {user.avatar_url && !avatarLoadFailed ? (
              <img
                className="profile-avatar-image"
                src={user.avatar_url}
                alt={user.username}
                loading="lazy"
                onError={() => setAvatarLoadFailed(true)}
              />
            ) : (
              <div className="avatar avatar-lg profile-avatar-fallback">{initials}</div>
            )}
          </div>
          <div className="profile-head-copy">
            <p className="feed-user profile-name">{user.full_name}</p>
            <p className="feed-meta profile-username">@{user.username}</p>
            {user.bio ? <p className="profile-bio">{user.bio}</p> : null}
          </div>
        </div>
        {canFollowProfile ? (
          <div className="profile-head-actions profile-head-actions-inline">
            <button
              className={`action-button profile-edit-button profile-follow-button ${isFollowingProfile ? "action-button-ghost is-following" : ""}`.trim()}
              type="button"
              disabled={togglingFollow}
              onClick={async () => {
                if (togglingFollow) return;
                setTogglingFollow(true);
                try {
                  await onToggleFollowProfile();
                } finally {
                  setTogglingFollow(false);
                }
              }}
            >
              {togglingFollow ? "..." : isFollowingProfile ? "Siguiendo" : "Seguir"}
            </button>
          </div>
        ) : null}
      </article>

      <section className="profile-stats-row" aria-label="Estadisticas de perfil">
        <article className="profile-stat-item"><strong className="profile-stat-value">{posts.length}</strong><span className="profile-stat-label">POSTS</span></article>
        <article className="profile-stat-item"><strong className="profile-stat-value">{followers}</strong><span className="profile-stat-label">SEGUIDORES</span></article>
        <article className="profile-stat-item"><strong className="profile-stat-value">{following}</strong><span className="profile-stat-label">SIGUIENDO</span></article>
      </section>

      <div className="premium-tabs profile-tabs" role="tablist" aria-label="Tabs perfil">
        <button type="button" className={`premium-tab ${tab === "posts" ? "is-active" : ""}`} role="tab" aria-selected={tab === "posts"} onClick={() => setTab("posts")}>Posts</button>
        <button type="button" className={`premium-tab ${tab === "adn" ? "is-active" : ""}`} role="tab" aria-selected={tab === "adn"} onClick={() => setTab("adn")}>ADN</button>
        <button type="button" className={`premium-tab ${tab === "favoritos" ? "is-active" : ""}`} role="tab" aria-selected={tab === "favoritos"} onClick={() => setTab("favoritos")}>Favoritos</button>
      </div>

      {tab === "posts" ? (
        <>
          <ul className="feed-list profile-post-list profile-post-list-mobile">
            {posts.length ? posts.map((post, index) => (
            <li key={post.id} className="feed-card feed-card-premium feed-entry" style={{ ["--feed-index" as string]: index }}>
              <article>
                <header className="feed-head">
                  <button type="button" className="feed-user-link" onClick={() => onOpenUserProfile(user.id)}>
                    {user.avatar_url ? (
                      <img className="avatar avatar-photo" src={user.avatar_url} alt={user.username} loading="lazy" />
                    ) : (
                      <div className="avatar" aria-hidden="true">{initials}</div>
                    )}
                    <div>
                      <p className="feed-user">{user.full_name}</p>
                      <p className="feed-meta">{post.minsAgoLabel.toUpperCase()}</p>
                    </div>
                  </button>
                  {canEditProfile ? (
                    <button type="button" className="icon-button post-menu-trigger" aria-label="Opciones" onClick={() => setPostMenuId(post.id)}>
                      <UiIcon name="more" className="ui-icon" />
                    </button>
                  ) : null}
                </header>
                {post.text ? <p className="feed-text"><MentionText text={post.text} /></p> : null}
                {post.imageUrl ? <img className={`feed-image ${post.text ? "" : "feed-image-no-text"}`.trim()} src={post.imageUrl} alt="Publicación" loading="lazy" /> : null}
                {post.coffeeTagName ? (
                  <button type="button" className="coffee-tag-card" onClick={() => post.coffeeId && onOpenCoffee(post.coffeeId)} disabled={!post.coffeeId}>
                    <div className="coffee-tag-card-media">
                      {post.coffeeImageUrl ? (
                        <img className="coffee-tag-image" src={post.coffeeImageUrl} alt={post.coffeeTagName} loading="lazy" />
                      ) : (
                        <div className="coffee-tag-image coffee-tag-image-fallback" aria-hidden="true">
                          <UiIcon name="coffee" className="ui-icon" />
                        </div>
                      )}
                    </div>
                    <div className="coffee-tag-copy">
                      <p className="coffee-origin">CAFE ETIQUETADO</p>
                      <p className="coffee-tag-name">{post.coffeeTagName}</p>
                      {post.coffeeTagBrand ? <p className="coffee-tag-brand">{post.coffeeTagBrand.toUpperCase()}</p> : null}
                    </div>
                    <UiIcon name="chevron-right" className="ui-icon" />
                  </button>
                ) : null}
                <footer className="feed-stats">
                  <button
                    type="button"
                    className={`inline-action action-like ${post.likedByActiveUser ? "is-liked" : ""} ${likeBurstPostId === post.id ? "is-bursting" : ""}`.trim()}
                    onClick={() => {
                      if (!post.likedByActiveUser) {
                        setLikeBurstPostId(post.id);
                        if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
                        likeBurstTimerRef.current = window.setTimeout(() => {
                          setLikeBurstPostId(null);
                          likeBurstTimerRef.current = null;
                        }, 520);
                      }
                      onToggleLike(post.id);
                    }}
                  >
                    <span className="like-icon-wrap">
                      <UiIcon name={post.likedByActiveUser ? "coffee-filled" : "coffee"} className="ui-icon" />
                      <span className="like-burst" aria-hidden="true">
                        <span />
                        <span />
                        <span />
                        <span />
                        <span />
                        <span />
                      </span>
                    </span>
                    {post.likes > 0 ? <span>{post.likes}</span> : null}
                  </button>
                  <button type="button" className="inline-action" onClick={() => onOpenComments(post.id)}>
                    <UiIcon name="chat" className="ui-icon" />
                    {post.comments > 0 ? <span>{post.comments}</span> : null}
                  </button>
                </footer>
              </article>
            </li>
            )) : <li className="feed-card profile-empty-card">Aún no hay publicaciones</li>}
          </ul>
          {posts.length ? (
            <div className="profile-post-masonry-desktop">
              {desktopPostColumns.map((column, columnIndex) => (
                <ul key={`profile-col-${columnIndex}`} className="feed-list profile-post-column">
                  {column.map((post, index) => (
                    <li key={post.id} className="feed-card feed-card-premium feed-entry" style={{ ["--feed-index" as string]: index }}>
                      <article>
                        <header className="feed-head">
                          <button type="button" className="feed-user-link" onClick={() => onOpenUserProfile(user.id)}>
                            {user.avatar_url ? (
                              <img className="avatar avatar-photo" src={user.avatar_url} alt={user.username} loading="lazy" />
                            ) : (
                              <div className="avatar" aria-hidden="true">{initials}</div>
                            )}
                            <div>
                              <p className="feed-user">{user.full_name}</p>
                              <p className="feed-meta">{post.minsAgoLabel.toUpperCase()}</p>
                            </div>
                          </button>
                          {canEditProfile ? (
                            <button type="button" className="icon-button post-menu-trigger" aria-label="Opciones" onClick={() => setPostMenuId(post.id)}>
                              <UiIcon name="more" className="ui-icon" />
                            </button>
                          ) : null}
                        </header>
                        {post.text ? <p className="feed-text"><MentionText text={post.text} /></p> : null}
                        {post.imageUrl ? <img className={`feed-image ${post.text ? "" : "feed-image-no-text"}`.trim()} src={post.imageUrl} alt="Publicación" loading="lazy" /> : null}
                        {post.coffeeTagName ? (
                          <button type="button" className="coffee-tag-card" onClick={() => post.coffeeId && onOpenCoffee(post.coffeeId)} disabled={!post.coffeeId}>
                            <div className="coffee-tag-card-media">
                              {post.coffeeImageUrl ? (
                                <img className="coffee-tag-image" src={post.coffeeImageUrl} alt={post.coffeeTagName} loading="lazy" />
                              ) : (
                                <div className="coffee-tag-image coffee-tag-image-fallback" aria-hidden="true">
                                  <UiIcon name="coffee" className="ui-icon" />
                                </div>
                              )}
                            </div>
                            <div className="coffee-tag-copy">
                              <p className="coffee-origin">CAFE ETIQUETADO</p>
                              <p className="coffee-tag-name">{post.coffeeTagName}</p>
                              {post.coffeeTagBrand ? <p className="coffee-tag-brand">{post.coffeeTagBrand.toUpperCase()}</p> : null}
                            </div>
                            <UiIcon name="chevron-right" className="ui-icon" />
                          </button>
                        ) : null}
                        <footer className="feed-stats">
                          <button
                            type="button"
                            className={`inline-action action-like ${post.likedByActiveUser ? "is-liked" : ""} ${likeBurstPostId === post.id ? "is-bursting" : ""}`.trim()}
                            onClick={() => {
                              if (!post.likedByActiveUser) {
                                setLikeBurstPostId(post.id);
                                if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
                                likeBurstTimerRef.current = window.setTimeout(() => {
                                  setLikeBurstPostId(null);
                                  likeBurstTimerRef.current = null;
                                }, 520);
                              }
                              onToggleLike(post.id);
                            }}
                          >
                            <span className="like-icon-wrap">
                              <UiIcon name={post.likedByActiveUser ? "coffee-filled" : "coffee"} className="ui-icon" />
                              <span className="like-burst" aria-hidden="true">
                                <span />
                                <span />
                                <span />
                                <span />
                                <span />
                                <span />
                              </span>
                            </span>
                            {post.likes > 0 ? <span>{post.likes}</span> : null}
                          </button>
                          <button type="button" className="inline-action" onClick={() => onOpenComments(post.id)}>
                            <UiIcon name="chat" className="ui-icon" />
                            {post.comments > 0 ? <span>{post.comments}</span> : null}
                          </button>
                        </footer>
                      </article>
                    </li>
                  ))}
                </ul>
              ))}
            </div>
          ) : null}
        </>
      ) : null}

      {tab === "adn" ? (
        <div className={`profile-adn-layout ${mode === "desktop" ? "is-desktop" : ""}`.trim()}>
          {mode === "desktop" ? (
            <article className="config-card profile-adn-card profile-adn-radar-card is-static">
              <div className="profile-adn-radar-wrap">
                <svg className="profile-adn-radar" viewBox="0 0 240 190" aria-label="Perfil sensorial radar">
                  {adnRadar.rings.map((ring, index) => (
                    <polygon
                      key={`ring-${index}`}
                      points={ring.map((point) => `${point.x},${point.y}`).join(" ")}
                      className="profile-adn-radar-ring"
                    />
                  ))}
                  {adnRadar.axes.map((axis, index) => (
                    <line key={`axis-${index}`} x1={axis.x1} y1={axis.y1} x2={axis.x2} y2={axis.y2} className="profile-adn-radar-axis" />
                  ))}
                  <polygon
                    points={adnRadar.dataPoints.map((point) => `${point.x},${point.y}`).join(" ")}
                    className="profile-adn-radar-shape"
                  />
                  {adnRadar.dataPoints.map((point, index) => (
                    <circle key={`point-${index}`} cx={point.x} cy={point.y} r="3" className="profile-adn-radar-point" />
                  ))}
                  {adnRadar.labelPoints.map((point, index) => (
                    <text key={`label-${index}`} x={point.x} y={point.y} className="profile-adn-radar-label" textAnchor="middle" dominantBaseline="middle">
                      {point.label}
                    </text>
                  ))}
                </svg>
              </div>
              <p className="profile-adn-caption">Tus gustos basados en favoritos, opiniones y reseñas.</p>
            </article>
          ) : (
            <button
              type="button"
              className="config-card profile-adn-card profile-adn-radar-card profile-adn-open"
              onClick={() => setShowAdnAnalysisSheet(true)}
              aria-label="Abrir análisis de preferencia"
            >
              <div className="profile-adn-radar-wrap">
                <svg className="profile-adn-radar" viewBox="0 0 240 190" aria-label="Perfil sensorial radar">
                  {adnRadar.rings.map((ring, index) => (
                    <polygon
                      key={`ring-${index}`}
                      points={ring.map((point) => `${point.x},${point.y}`).join(" ")}
                      className="profile-adn-radar-ring"
                    />
                  ))}
                  {adnRadar.axes.map((axis, index) => (
                    <line key={`axis-${index}`} x1={axis.x1} y1={axis.y1} x2={axis.x2} y2={axis.y2} className="profile-adn-radar-axis" />
                  ))}
                  <polygon
                    points={adnRadar.dataPoints.map((point) => `${point.x},${point.y}`).join(" ")}
                    className="profile-adn-radar-shape"
                  />
                  {adnRadar.dataPoints.map((point, index) => (
                    <circle key={`point-${index}`} cx={point.x} cy={point.y} r="3" className="profile-adn-radar-point" />
                  ))}
                  {adnRadar.labelPoints.map((point, index) => (
                    <text key={`label-${index}`} x={point.x} y={point.y} className="profile-adn-radar-label" textAnchor="middle" dominantBaseline="middle">
                      {point.label}
                    </text>
                  ))}
                </svg>
              </div>
              <p className="profile-adn-caption">Tus gustos basados en favoritos, opiniones y reseñas.</p>
            </button>
          )}
          {mode === "desktop" ? (
            <article className="config-card profile-adn-analysis-panel">
              <h3 className="profile-adn-analysis-title">ANÁLISIS DE PREFERENCIAS</h3>
              <div className="profile-adn-analysis-body">
                <p className="profile-adn-analysis-lead">{adnAnalysis.lead}</p>
                <p className="profile-adn-analysis-text">{adnAnalysis.description}</p>
                <article className="profile-adn-recommend-card">
                  <div className="profile-adn-recommend-head">
                    <MaterialSymbolIcon symbol="auto_awesome" filled={false} className="ui-icon" />
                    <strong>RECOMENDACIÓN IDEAL</strong>
                  </div>
                  <p className="profile-adn-recommend-title">Deberías probar: {adnAnalysis.recommendation.type}</p>
                  <p className="profile-adn-recommend-origin">Orígenes sugeridos: {adnAnalysis.recommendation.origin}</p>
                </article>
              </div>
            </article>
          ) : null}
        </div>
      ) : null}

      {tab === "favoritos" ? (
        <>
          <ul className="coffee-list profile-favorite-list">
            {favoriteCoffees.length ? favoriteCoffees.map((coffee) => (
              <ProfileFavoriteItem key={coffee.id} coffee={coffee} onOpenCoffee={onOpenCoffee} onRemoveFavorite={onRemoveFavorite} />
            )) : <li className="coffee-card profile-empty-card">No hay cafés favoritos</li>}
          </ul>
        </>
      ) : null}
      {showEditProfile ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Editar perfil" onClick={closeEditProfileModal}>
          <div className="sheet-card profile-edit-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">Editar perfil</strong>
            </header>
            <div className="diary-sheet-form">
              <input
                ref={editAvatarInputRef}
                type="file"
                accept="image/*"
                className="file-input-hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  if (!file) return;
                  if (editAvatarPreview.startsWith("blob:")) URL.revokeObjectURL(editAvatarPreview);
                  const preview = URL.createObjectURL(file);
                  setEditAvatarFile(file);
                  setEditAvatarPreview(preview);
                  setRemoveAvatarDraft(false);
                  event.currentTarget.value = "";
                }}
              />
              <div className="profile-edit-avatar-row">
                <div className="profile-edit-avatar-preview" aria-hidden="true">
                  {editAvatarPreview || (!removeAvatarDraft && user.avatar_url) ? (
                    <img src={editAvatarPreview || user.avatar_url} alt={user.username} loading="lazy" />
                  ) : (
                    <span>{initials}</span>
                  )}
                </div>
                <div className="profile-edit-avatar-actions">
                  <button type="button" className="action-button action-button-ghost" onClick={() => editAvatarInputRef.current?.click()}>
                    Cambiar foto
                  </button>
                  {(editAvatarPreview || user.avatar_url) ? (
                    <button
                      type="button"
                      className="action-button action-button-ghost"
                      onClick={() => {
                        if (editAvatarPreview.startsWith("blob:")) URL.revokeObjectURL(editAvatarPreview);
                        setEditAvatarPreview("");
                        setEditAvatarFile(null);
                        setRemoveAvatarDraft(true);
                      }}
                    >
                      Quitar foto
                    </button>
                  ) : null}
                </div>
              </div>
              <label>
                <span>Nombre</span>
                <input
                  className="search-wide"
                  value={editNameDraft}
                  maxLength={60}
                  onChange={(event) => setEditNameDraft(event.target.value)}
                />
              </label>
              <label>
                <span>Bio</span>
                <textarea
                  className="search-wide profile-edit-bio"
                  rows={4}
                  value={editBioDraft}
                  maxLength={240}
                  onChange={(event) => setEditBioDraft(event.target.value)}
                />
              </label>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={closeEditProfileModal} disabled={savingProfile}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  disabled={savingProfile || !editNameDraft.trim()}
                  onClick={async () => {
                    if (!editNameDraft.trim() || savingProfile) return;
                    setSavingProfile(true);
                    try {
                      await onSaveProfile(user.id, editNameDraft, editBioDraft, editAvatarFile, removeAvatarDraft);
                      closeEditProfileModal();
                    } finally {
                      setSavingProfile(false);
                    }
                  }}
                >
                  {savingProfile ? "Guardando..." : "Guardar"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
      {showAdnAnalysisSheet && mode !== "desktop" ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Análisis de preferencia" onClick={() => setShowAdnAnalysisSheet(false)}>
          <div className="sheet-card profile-adn-analysis-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">ANÁLISIS DE PREFERENCIAS</strong>
            </header>
            <div className="profile-adn-analysis-body">
              <p className="profile-adn-analysis-lead">{adnAnalysis.lead}</p>
              <p className="profile-adn-analysis-text">{adnAnalysis.description}</p>
              <article className="profile-adn-recommend-card">
                <div className="profile-adn-recommend-head">
                  <MaterialSymbolIcon symbol="auto_awesome" filled={false} className="ui-icon" />
                  <strong>RECOMENDACIÓN IDEAL</strong>
                </div>
                <p className="profile-adn-recommend-title">Deberías probar: {adnAnalysis.recommendation.type}</p>
                <p className="profile-adn-recommend-origin">Orígenes sugeridos: {adnAnalysis.recommendation.origin}</p>
              </article>
              <button
                type="button"
                className="action-button profile-adn-continue-button"
                onClick={() => setShowAdnAnalysisSheet(false)}
              >
                CONTINUAR EXPLORANDO
              </button>
            </div>
          </div>
        </div>
      ) : null}
      {postMenuId && canEditProfile ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Opciones publicación" onClick={() => setPostMenuId(null)}>
          <div className="sheet-card profile-post-menu-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <div className="comment-action-list">
              <p className="comment-action-title">OPCIONES</p>
              <button
                type="button"
                className="comment-action-button is-danger"
                onClick={async () => {
                  const confirmed = window.confirm("Borrar publicación?");
                  if (!confirmed) return;
                  const postId = postMenuId;
                  setPostMenuId(null);
                  if (postId) await onDeletePost(postId);
                }}
              >
                <UiIcon name="trash" className="ui-icon" />
                <span>Borrar</span>
                <UiIcon name="chevron-right" className="ui-icon trailing" />
              </button>
              <button
                type="button"
                className="comment-action-button"
                onClick={() => {
                  const post = posts.find((item) => item.id === postMenuId);
                  if (!post) return;
                  setEditPostId(post.id);
                  setEditPostText(post.text || "");
                  setEditPostImageUrl(post.imageUrl || "");
                  setEditPostImageFile(null);
                  setPostMenuId(null);
                }}
              >
                <UiIcon name="edit" className="ui-icon" />
                <span>Editar</span>
                <UiIcon name="chevron-right" className="ui-icon trailing" />
              </button>
            </div>
          </div>
        </div>
      ) : null}
      {editPostId ? (
        <div className="sheet-overlay" role="dialog" aria-modal="true" aria-label="Editar publicación" onClick={closeEditPostModal}>
          <div className="sheet-card profile-edit-sheet" onClick={(event) => event.stopPropagation()}>
            <div className="sheet-handle" aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">Editar publicación</strong>
            </header>
            <div className="diary-sheet-form">
              <input
                ref={editPostImageInputRef}
                type="file"
                accept="image/*"
                className="file-input-hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  if (!file) return;
                  if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
                  const preview = URL.createObjectURL(file);
                  setEditPostImageFile(file);
                  setEditPostImageUrl(preview);
                  event.currentTarget.value = "";
                }}
              />
              <label>
                <span>Texto</span>
                <textarea
                  className="search-wide profile-edit-bio"
                  rows={4}
                  maxLength={500}
                  value={editPostText}
                  onChange={(event) => setEditPostText(event.target.value)}
                />
              </label>
              <div className="profile-edit-post-image-wrap">
                {editPostImageUrl ? <img src={editPostImageUrl} alt="Previsualización" loading="lazy" /> : <p className="feed-meta">Sin imagen</p>}
              </div>
              <div className="profile-edit-post-actions">
                <button type="button" className="action-button action-button-ghost" onClick={() => editPostImageInputRef.current?.click()}>
                  Cambiar imagen
                </button>
                {editPostImageUrl ? (
                  <button
                    type="button"
                    className="action-button action-button-ghost"
                    onClick={() => {
                      if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
                      setEditPostImageFile(null);
                      setEditPostImageUrl("");
                    }}
                  >
                    Quitar imagen
                  </button>
                ) : null}
              </div>
              <div className="diary-sheet-form-actions">
                <button type="button" className="action-button action-button-ghost" onClick={closeEditPostModal} disabled={savingPostEdit}>
                  Cancelar
                </button>
                <button
                  type="button"
                  className="action-button"
                  disabled={savingPostEdit}
                  onClick={async () => {
                    if (!editPostId || savingPostEdit) return;
                    setSavingPostEdit(true);
                    try {
                      await onEditPost(editPostId, editPostText, editPostImageUrl, editPostImageFile);
                      closeEditPostModal();
                    } finally {
                      setSavingPostEdit(false);
                    }
                  }}
                >
                  {savingPostEdit ? "Guardando..." : "Guardar"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}
    </>
  );

  if (sidePanel) {
    return (
      <div className="split-with-side">
        <div className="profile-content-wrap">{content}</div>
        <aside className="timeline-side-column">{sidePanel}</aside>
      </div>
    );
  }

  return content;
}






