import { Fragment, type PointerEvent as ReactPointerEvent, type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import {
  addPostCoffeeTag,
  createPost,
  createComment,
  deleteCoffeeReview,
  deleteComment,
  deletePost,
  fetchInitialData,
  fetchUserData,
  toggleFavoriteCoffee,
  upsertCoffeeReview,
  upsertCoffeeSensoryProfile,
  upsertPantryStock,
  toggleFollow,
  toggleLike,
  uploadImageFile,
  updateComment,
  updatePost
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
  | "camera"
  | "chat"
  | "at"
  | "smile"
  | "chevron-right"
  | "send"
  | "edit"
  | "trash";

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

const BREW_METHODS = ["V60", "AeroPress", "French Press", "Chemex", "Espresso"];
const COMMENT_EMOJIS = ["😀", "😍", "🤎", "☕", "🔥", "🙌", "👏", "😋", "🥳", "😎"];

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
  symbol: "book" | "science" | "explore";
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
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 21s6-5.4 6-11a6 6 0 1 0-12 0c0 5.6 6 11 6 11z" />
        <circle cx="12" cy="10" r="2.2" />
      </svg>
    );
  }
  if (name === "specialty") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="m12 3 2.5 5.1 5.6.8-4 3.9.9 5.5L12 15.8 7 18.3l1-5.5-4-3.9 5.5-.8z" />
      </svg>
    );
  }
  if (name === "roast") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M9 20c-2.4-1.6-3.6-3.5-3.6-5.8C5.4 10 9.5 8.8 9.5 5 12.5 7 14 9.5 14 12c0 4.6-3 8-5 8zM15 21c1.9-1.2 3.6-3.3 3.6-6.3 0-1.5-.4-2.8-1.2-4.2" />
      </svg>
    );
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
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M7 4h10v3H7zM9 7v6.5L5 20h14l-4-6.5V7" />
      </svg>
    );
  }
  if (name === "variety") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M12 20c-3.9-2.7-6.2-6.3-6.2-10.1 0-2.3 1.9-4.2 4.2-4.2 1 0 2 .4 2.8 1.1.8-.7 1.8-1.1 2.8-1.1 2.3 0 4.2 1.9 4.2 4.2 0 3.8-2.3 7.4-6.2 10.1z" />
      </svg>
    );
  }
  if (name === "grind") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M8 4h8v3H8zM7 7h10l1.4 5.4A4 4 0 0 1 14.5 17h-5A4 4 0 0 1 5.6 12.4zM9 20h6" />
      </svg>
    );
  }
  if (name === "caffeine") {
    return (
      <svg viewBox="0 0 24 24" className={className} aria-hidden="true">
        <path d="M5 10h11a3 3 0 0 1 0 6H5zM7 7c0-1.4.8-2.2 2-3M11 7c0-1.4.8-2.2 2-3" />
      </svg>
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
  if (step === "method") return "ELIGE METODO";
  if (step === "coffee") return "ELIGE CAFE";
  if (step === "config") return "CONFIGURA";
  if (step === "brewing") return "PREPARANDO";
  return "RESULTADO";
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
  const [detailHostTab, setDetailHostTab] = useState<"timeline" | "search" | "profile" | null>(null);
  const [detailReviewText, setDetailReviewText] = useState("");
  const [detailReviewRating, setDetailReviewRating] = useState(0);
  const [detailReviewImageFile, setDetailReviewImageFile] = useState<File | null>(null);
  const [detailReviewImagePreviewUrl, setDetailReviewImagePreviewUrl] = useState("");
  const [detailSensoryDraft, setDetailSensoryDraft] = useState({ aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 });
  const [detailStockDraft, setDetailStockDraft] = useState({ total: 0, remaining: 0 });
  const [showBarcodeScannerSheet, setShowBarcodeScannerSheet] = useState(false);

  const [brewStep, setBrewStep] = useState<BrewStep>("method");
  const [brewMethod, setBrewMethod] = useState("V60");
  const [brewCoffeeId, setBrewCoffeeId] = useState<string>("");
  const [waterMl, setWaterMl] = useState(300);
  const [ratio, setRatio] = useState(16);
  const [timerSeconds, setTimerSeconds] = useState(150);
  const [brewRunning, setBrewRunning] = useState(false);

  const [diaryTab, setDiaryTab] = useState<"actividad" | "despensa">("actividad");
  const [profileTab, setProfileTab] = useState<"posts" | "adn" | "favoritos">("posts");
  const [profileUsername, setProfileUsername] = useState<string | null>(initialRoute.profileUsername);

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

  const handleSignOut = useCallback(async () => {
    if (supabaseConfigError) return;
    try {
      const supabase = getSupabaseClient();
      await supabase.auth.signOut();
      setUsers([]);
      setCoffees([]);
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
    };
    window.addEventListener("keydown", onEscSheet);
    return () => window.removeEventListener("keydown", onEscSheet);
  }, [commentSheetPostId, resetCreatePostComposer, showCreatePost, showCreatePostCoffeeSheet, showNotificationsPanel]);

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
    if (!sessionEmail) return;
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
  }, [sessionEmail]);

  useEffect(() => {
    if (!authReady || !sessionEmail) return;
    void loadInitialData();
  }, [authReady, loadInitialData, sessionEmail]);

  useEffect(() => {
    if (!activeUser) return;
    (async () => {
      try {
        const data = await fetchUserData(activeUser.id);
        setDiaryEntries(data.diaryEntries);
        setPantryItems(data.pantryItems);
        setFavorites(data.favorites);
      } catch (error) {
        setGlobalStatus(`Error: ${(error as Error).message}`);
      }
    })();
  }, [activeUser]);

  useEffect(() => {
    if (!brewRunning || brewStep !== "brewing") return;
    if (timerSeconds <= 0) {
      setBrewRunning(false);
      setBrewStep("result");
      return;
    }
    const id = window.setTimeout(() => setTimerSeconds((prev) => prev - 1), 1000);
    return () => window.clearTimeout(id);
  }, [brewRunning, brewStep, timerSeconds]);

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

      const originMatch =
        !searchSelectedOrigins.size || (coffee.pais_origen ? searchSelectedOrigins.has(coffee.pais_origen) : false);
      if (!originMatch) return false;

      const roast = coffee.tueste ?? "";
      const roastMatch = !searchSelectedRoasts.size || (roast ? searchSelectedRoasts.has(roast) : false);
      if (!roastMatch) return false;

      const specialty = coffee.especialidad ?? "";
      const specialtyMatch = !searchSelectedSpecialties.size || (specialty ? searchSelectedSpecialties.has(specialty) : false);
      if (!specialtyMatch) return false;

      const format = coffee.formato ?? "";
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
    () =>
      Array.from(new Set(coffees.map((coffee) => coffee.pais_origen).filter((value): value is string => Boolean(value && value.trim())))).sort(
        (a, b) => a.localeCompare(b)
      ),
    [coffees]
  );

  const searchRoastOptions = useMemo(
    () => Array.from(new Set(coffees.map((coffee) => coffee.tueste ?? "").filter((value) => value.trim().length > 0))).sort((a, b) => a.localeCompare(b)),
    [coffees]
  );

  const searchSpecialtyOptions = useMemo(
    () => Array.from(new Set(coffees.map((coffee) => coffee.especialidad ?? "").filter((value) => value.trim().length > 0))).sort((a, b) => a.localeCompare(b)),
    [coffees]
  );

  const searchFormatOptions = useMemo(
    () => Array.from(new Set(coffees.map((coffee) => coffee.formato ?? "").filter((value) => value.trim().length > 0))).sort((a, b) => a.localeCompare(b)),
    [coffees]
  );
  const selectedCoffee = coffees.find((coffee) => coffee.id === brewCoffeeId) ?? coffees[0];
  const coffeeGrams = Math.max(1, Math.round(waterMl / ratio));

  const diaryEntriesActivity = diaryEntries.slice(0, 80);
  const pantryCoffeeRows = pantryItems
    .map((item) => ({ item, coffee: coffees.find((coffee) => coffee.id === item.coffee_id) }))
    .filter((row) => row.coffee);

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
    (coffeeId: string, sourceTab: "timeline" | "search" | "profile") => {
      const coffee = coffeesById.get(coffeeId);
      if (!coffee) return;
      const slug = coffeeSlugIndex.byId.get(coffeeId) ?? toCoffeeSlug(coffee.nombre);
      setDetailCoffeeId(coffeeId);
      if (mode === "desktop") {
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
      onToggleFavorite={() => void saveDetailFavorite()}
      onReviewTextChange={setDetailReviewText}
      onReviewRatingChange={setDetailReviewRating}
      onReviewImagePick={(file, previewUrl) => {
        setDetailReviewImageFile(file);
        setDetailReviewImagePreviewUrl(previewUrl);
      }}
      onSaveReview={saveDetailReview}
      onDeleteReview={removeDetailReview}
      canDeleteReview={Boolean(detailCurrentUserReview)}
      onSensoryDraftChange={setDetailSensoryDraft}
      onSaveSensory={saveDetailSensory}
      onStockDraftChange={setDetailStockDraft}
      onSaveStock={saveDetailStock}
      onOpenUserProfile={(userId) => navigateToTab("profile", { profileUserId: userId })}
      fullPage={false}
    />
  ) : null;

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
    setDetailCoffeeId(null);
    setDetailHostTab(null);
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

  if (!sessionEmail) {
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
          scrolled={topbarScrolled}
          brewStep={brewStep}
          onBrewBack={() => {
            if (brewStep === "coffee") setBrewStep("method");
            else if (brewStep === "config") setBrewStep("coffee");
            else if (brewStep === "brewing") setBrewStep("config");
            else if (brewStep === "result") setBrewStep("method");
          }}
          onBrewForward={() => {
            if (brewStep === "config") {
              setTimerSeconds(150);
              setBrewRunning(true);
              setBrewStep("brewing");
            }
          }}
          onProfileSignOut={handleSignOut}
          onCoffeeBack={() => {
            if (window.history.length > 1) {
              window.history.back();
              return;
            }
            navigateToTab("timeline", { replace: true });
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
                onToggleFavorite={() => void saveDetailFavorite()}
                onReviewTextChange={setDetailReviewText}
                onReviewRatingChange={setDetailReviewRating}
                onReviewImagePick={(file, previewUrl) => {
                  setDetailReviewImageFile(file);
                  setDetailReviewImagePreviewUrl(previewUrl);
                }}
                onSaveReview={saveDetailReview}
                onDeleteReview={removeDetailReview}
                canDeleteReview={Boolean(detailCurrentUserReview)}
                onSensoryDraftChange={setDetailSensoryDraft}
                onSaveSensory={saveDetailSensory}
                onStockDraftChange={setDetailStockDraft}
                onSaveStock={saveDetailStock}
                onOpenUserProfile={(userId) => navigateToTab("profile", { profileUserId: userId })}
                fullPage
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
          {activeTab === "brewlab" ? <BrewLabView brewStep={brewStep} setBrewStep={setBrewStep} brewMethod={brewMethod} setBrewMethod={setBrewMethod} brewCoffeeId={brewCoffeeId} setBrewCoffeeId={setBrewCoffeeId} coffees={coffees} waterMl={waterMl} setWaterMl={setWaterMl} ratio={ratio} setRatio={setRatio} timerSeconds={timerSeconds} brewRunning={brewRunning} setBrewRunning={setBrewRunning} selectedCoffee={selectedCoffee} coffeeGrams={coffeeGrams} /> : null}
          {activeTab === "diary" ? <DiaryView tab={diaryTab} setTab={setDiaryTab} entries={diaryEntriesActivity} pantryRows={pantryCoffeeRows} /> : null}
          {activeTab === "profile" && profileUser ? (
            <ProfileView
              user={profileUser}
              tab={profileTab}
              setTab={setProfileTab}
              posts={profilePosts}
              favoriteCoffees={profileUser.id === activeUser?.id ? favoriteCoffees : []}
              followers={followersCount}
              following={followingCount}
              onOpenCoffee={(coffeeId) => openCoffeeDetail(coffeeId, "profile")}
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
        <aside className={useRightRailDetail ? "detail-rail-fixed" : "fab-rail"} aria-label={useRightRailDetail ? "Detalle cafe" : "Acciones"}>
          {useRightRailDetail ? (
            <div className="desktop-detail-wrap">{detailPanel}</div>
          ) : activeTab === "timeline" ? (
            <button className="fab fab-desktop" type="button" aria-label="Nuevo Post" onClick={openCreatePostComposer}>
              <UiIcon name="add" className="ui-icon" />
            </button>
          ) : null}
        </aside>
      ) : null}

      {mode === "mobile" ? <footer className="bottom-tabs">{nav}</footer> : null}

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
  scrolled,
  brewStep,
  onBrewBack,
  onBrewForward,
  onProfileSignOut,
  onCoffeeBack
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
  scrolled: boolean;
  brewStep: BrewStep;
  onBrewBack: () => void;
  onBrewForward: () => void;
  onProfileSignOut: () => void;
  onCoffeeBack: () => void;
}) {
  const [searchFocus, setSearchFocus] = useState(false);
  const [searchHintWord, setSearchHintWord] = useState<"marca" | "cafe">("marca");
  const [notificationPop, setNotificationPop] = useState(false);
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

  if (activeTab === "search") {
    if (searchMode === "users") {
      return (
        <header className={`topbar topbar-search-users ${scrolled ? "topbar-scrolled" : ""}`}>
          <div className="topbar-slot">
            <button className="icon-button topbar-icon-button search-users-back" type="button" onClick={onSearchBack} aria-label="Atras">
              <UiIcon name="arrow-left" className="ui-icon" />
            </button>
          </div>
          <div className="search-users-field">
            <UiIcon name="search" className="ui-icon search-users-leading-icon" />
            <input
              id="quick-search"
              className="search-wide search-users-input"
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
            className="search-wide search-coffee-input"
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
          onClick={onSearchCancel}
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
    return (
      <header className={`topbar ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-inline">
          {brewStep !== "method" ? <button className="icon-button" type="button" onClick={onBrewBack} aria-label="Atras"><UiIcon name="arrow-left" className="ui-icon" /></button> : null}
          <h1 className="title title-upper">{getBrewStepTitle(brewStep)}</h1>
        </div>
        {brewStep === "config" ? <button className="icon-button" type="button" onClick={onBrewForward} aria-label="Empezar"><UiIcon name="arrow-right" className="ui-icon" /></button> : null}
      </header>
    );
  }

  if (activeTab === "diary") {
    return (
      <header className={`topbar ${scrolled ? "topbar-scrolled" : ""}`}>
        <h1 className="title title-upper">MI DIARIO</h1>
        <div className="topbar-actions">
          <button className="icon-button" type="button" aria-label="Agregar"><UiIcon name="add" className="ui-icon" /></button>
          <button className="chip-button" type="button">HOY</button>
        </div>
      </header>
    );
  }

  if (activeTab === "profile") {
    return (
      <header className={`topbar ${scrolled ? "topbar-scrolled" : ""}`}>
        <h1 className="title title-upper">PERFIL</h1>
        <button className="icon-button" type="button" aria-label="Cerrar sesion" onClick={onProfileSignOut}><UiIcon name="settings" className="ui-icon" /></button>
      </header>
    );
  }

  if (activeTab === "coffee") {
    return (
      <header className={`topbar topbar-timeline topbar-centered ${scrolled ? "topbar-scrolled" : ""}`}>
        <div className="topbar-slot">
          <button className="icon-button topbar-icon-button" type="button" onClick={onCoffeeBack} aria-label="Volver">
            <UiIcon name="arrow-left" className="ui-icon" />
          </button>
        </div>
        <h1 className="title title-upper topbar-title-center">CAFE</h1>
        <div className="topbar-slot topbar-slot-end" />
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
                    <UiIcon name="coffee" className="ui-icon" />
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
      <div className="split-with-side">
        <div>{content}</div>
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
  fullPage
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
  fullPage: boolean;
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
    { icon: "origin", label: "País", value: coffee.pais_origen ?? "" },
    { icon: "specialty", label: "Especialidad", value: coffee.especialidad ?? "" },
    { icon: "roast", label: "Tueste", value: coffee.tueste ?? "" },
    { icon: "format", label: "Formato", value: coffee.formato ?? "" },
    { icon: "process", label: "Proceso", value: coffee.proceso ?? "" },
    { icon: "variety", label: "Variedad", value: coffee.variedad_tipo ?? "" },
    { icon: "grind", label: "Molienda", value: coffee.molienda_recomendada ?? "" },
    { icon: "caffeine", label: "Cafeína", value: coffee.cafeina ?? "" }
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

  return (
    <article className={`coffee-detail ${fullPage ? "is-full-page" : "is-side-panel"}`.trim()}>
      <header className="coffee-detail-topbar">
        <div className="coffee-detail-topbar-left">
          {!fullPage ? (
            <button type="button" className="icon-button topbar-icon-button coffee-detail-topbar-icon" aria-label="Cerrar detalle" onClick={onClose}>
              <UiIcon name="close" className="ui-icon" />
            </button>
          ) : null}
        </div>
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
              setStockSheetError(null);
              setShowStockSheet(true);
            }}
          >
            <UiIcon name="stock" className="ui-icon" />
          </button>
        </div>
      </header>
      <header className="coffee-detail-hero">
        {coffee.image_url ? <img className="coffee-detail-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" /> : null}
        <div className="coffee-detail-overlay" />

        <div className="coffee-detail-headline">
          <p className="coffee-origin">{coffee.marca ?? "Marca"}</p>
          <h2 className="coffee-detail-title">{coffee.nombre}</h2>
          <p className="coffee-sub">{avgRating > 0 ? `Nota ${avgRating.toFixed(1)} / 5` : "Sin valoraciones"}</p>
        </div>
        {avgRating > 0 ? (
          <span className="coffee-detail-rating-badge">
            <UiIcon name="star" className="ui-icon" />
            <strong>{avgRating.toFixed(1)}</strong>
          </span>
        ) : null}
      </header>

      <section className="coffee-detail-section">
        {coffee.product_url ? (
          <div className="coffee-detail-intro-actions">
            <a className="text-button coffee-detail-inline-action coffee-detail-intro-link" href={coffee.product_url} target="_blank" rel="noreferrer">
              Adquirir
            </a>
          </div>
        ) : null}
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
              setSensorySheetError(null);
              setShowSensorySheet(true);
            }}
          >
            Editar perfil
          </button>
        </div>
        <div className="coffee-detail-sensory-summary">
          {sensoryKeys.map((key) => (
            <div key={key} className="coffee-detail-sensory-row">
              <span>{sensoryLabels[key]}</span>
              <div className="coffee-detail-sensory-track" aria-hidden="true">
                <div className="coffee-detail-sensory-fill" style={{ width: `${Math.max(0, Math.min(100, (sensory[key] / 10) * 100))}%` }} />
              </div>
              <strong>{sensory[key].toFixed(1)}</strong>
            </div>
          ))}
        </div>
      </section>

      <section className="coffee-detail-section coffee-detail-opinions-section">
        <div className="coffee-detail-section-head">
          <h3 className="section-title">Opiniones</h3>
          <button
            type="button"
            className="text-button coffee-detail-inline-action"
            onClick={() => {
              setReviewSheetError(null);
              setShowReviewSheet(true);
            }}
          >
            {currentUserReview ? "Editar" : "Escribir"}
          </button>
        </div>
        {currentUserReview ? (
          <article className="coffee-detail-opinion-preview">
            <p className="coffee-detail-opinion-label">Tu opinión</p>
            <div className="coffee-detail-opinion-head">
              {currentUser?.id ? (
                <button type="button" className="coffee-detail-opinion-user-link" onClick={() => onOpenUserProfile(currentUser.id)}>
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
        ) : (
          <p className="coffee-sub coffee-detail-opinion-empty">Aún no has escrito tu opinión.</p>
        )}
        <ul className="coffee-list">
          {otherReviews.length ? otherReviews.map((review) => (
            <li key={`${review.user_id}-${review.id ?? review.timestamp ?? 0}`} className="coffee-card coffee-detail-opinion-item">
              <div className="coffee-detail-opinion-head">
                {review.user?.id ? (
                  <button type="button" className="coffee-detail-opinion-user-link" onClick={() => onOpenUserProfile(review.user!.id)}>
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
          )) : <li className="coffee-card">Sin opiniones aún</li>}
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
            <header className="sheet-header">
              <strong className="sheet-title">PERFIL SENSORIAL</strong>
            </header>
            <div className="coffee-detail-sheet-body coffee-detail-sliders coffee-detail-sensory-sliders">
              {sensoryKeys.map((key) => (
                <label key={key} className="coffee-detail-sensory-control">
                  <span className="coffee-detail-slider-label">
                    {sensoryLabels[key]}
                    <strong>{sensoryDraft[key].toFixed(1)}</strong>
                  </span>
                  <input
                    type="range"
                    min={0}
                    max={10}
                    step={0.5}
                    value={sensoryDraft[key]}
                    onChange={(event) => onSensoryDraftChange({ ...sensoryDraft, [key]: Number(event.target.value) })}
                  />
                </label>
              ))}
            </div>
            <div className="coffee-detail-actions coffee-detail-sheet-actions">
              <button
                type="button"
                className="action-button action-button-ghost"
                disabled={savingSensory}
                onClick={() => {
                  setSensorySheetError(null);
                  setShowSensorySheet(false);
                }}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="action-button"
                disabled={!isSensoryDirty || savingSensory}
                onClick={async () => {
                  if (!isSensoryDirty) return;
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
                {savingSensory ? "Guardando..." : "Guardar perfil"}
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
                className="search-wide"
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
  waterMl,
  setWaterMl,
  ratio,
  setRatio,
  timerSeconds,
  brewRunning,
  setBrewRunning,
  selectedCoffee,
  coffeeGrams
}: {
  brewStep: BrewStep;
  setBrewStep: (step: BrewStep) => void;
  brewMethod: string;
  setBrewMethod: (value: string) => void;
  brewCoffeeId: string;
  setBrewCoffeeId: (value: string) => void;
  coffees: CoffeeRow[];
  waterMl: number;
  setWaterMl: (value: number) => void;
  ratio: number;
  setRatio: (value: number) => void;
  timerSeconds: number;
  brewRunning: boolean;
  setBrewRunning: (value: boolean) => void;
  selectedCoffee?: CoffeeRow;
  coffeeGrams: number;
}) {
  return (
    <>
      <ol className="stepper" aria-label="Pasos de elabora">
        {["method", "coffee", "config", "brewing", "result"].map((id) => (
          <li key={id} className={`step ${brewStep === id ? "is-active" : ""}`}>{id.toUpperCase()}</li>
        ))}
      </ol>

      {brewStep === "method" ? (
        <div className="method-grid">
          {BREW_METHODS.map((method) => (
            <button key={method} type="button" className={`method-card ${brewMethod === method ? "is-active" : ""}`} onClick={() => {
              setBrewMethod(method);
              setBrewStep("coffee");
            }}>
              {method}
            </button>
          ))}
        </div>
      ) : null}

      {brewStep === "coffee" ? (
        <ul className="coffee-list">
          {coffees.slice(0, 20).map((coffee) => (
            <li key={coffee.id}>
              <button type="button" className={`coffee-select ${brewCoffeeId === coffee.id ? "is-active" : ""}`} onClick={() => {
                setBrewCoffeeId(coffee.id);
                setBrewStep("config");
              }}>
                <span>{coffee.nombre}</span>
                <small>{coffee.marca}</small>
              </button>
            </li>
          ))}
        </ul>
      ) : null}

      {brewStep === "config" ? (
        <section className="config-card">
          <p className="section-title">Configuracion</p>
          <p className="feed-meta">Metodo: {brewMethod}</p>
          <p className="feed-meta">Cafe: {selectedCoffee?.nombre ?? "No seleccionado"}</p>

          <label className="slider-row">
            Agua {waterMl} ml
            <input type="range" min={150} max={600} step={10} value={waterMl} onChange={(event) => setWaterMl(Number(event.target.value))} />
          </label>

          <label className="slider-row">
            Ratio 1:{ratio}
            <input type="range" min={12} max={20} step={1} value={ratio} onChange={(event) => setRatio(Number(event.target.value))} />
          </label>

          <p className="metric-pill">Café recomendado: {coffeeGrams} g</p>
          <button className="action-button" type="button" onClick={() => setBrewStep("brewing")}>Empezar preparacion</button>
        </section>
      ) : null}

      {brewStep === "brewing" ? (
        <section className="brew-timer">
          <p className="section-title">Preparando {brewMethod}</p>
          <strong className="timer-value">{timerSeconds}s</strong>
          <div className="timer-actions">
            <button className="action-button" type="button" onClick={() => setBrewRunning(!brewRunning)}>{brewRunning ? "Pausar" : "Iniciar"}</button>
            <button className="action-button action-button-ghost" type="button" onClick={() => {
              setBrewRunning(false);
              setBrewStep("result");
            }}>
              Finalizar
            </button>
          </div>
        </section>
      ) : null}

      {brewStep === "result" ? (
        <section className="config-card">
          <p className="section-title">Resultado</p>
          <p className="feed-meta">Metodo: {brewMethod}</p>
          <p className="feed-meta">Cafe: {selectedCoffee?.nombre ?? "-"}</p>
          <p className="metric-pill">Recomendacion: sube 1 click molienda para mas dulzor</p>
          <button className="action-button" type="button" onClick={() => setBrewStep("method")}>Nueva preparacion</button>
        </section>
      ) : null}
    </>
  );
}

function DiaryView({
  tab,
  setTab,
  entries,
  pantryRows
}: {
  tab: "actividad" | "despensa";
  setTab: (value: "actividad" | "despensa") => void;
  entries: DiaryEntryRow[];
  pantryRows: Array<{ item: PantryItemRow; coffee?: CoffeeRow }>;
}) {
  const [period, setPeriod] = useState<"hoy" | "7d" | "30d">("hoy");
  const visibleEntries = useMemo(() => {
    if (period === "hoy") return entries.filter((entry) => withinDays(entry.timestamp, 1));
    if (period === "7d") return entries.filter((entry) => withinDays(entry.timestamp, 7));
    return entries.filter((entry) => withinDays(entry.timestamp, 30));
  }, [entries, period]);

  const analytics = useMemo(() => {
    const caffeine = visibleEntries.reduce((acc, entry) => acc + Math.max(0, entry.caffeine_mg || 0), 0);
    const coffeeCups = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() !== "WATER").length;
    const waterEntries = visibleEntries.filter((entry) => (entry.type || "").toUpperCase() === "WATER").length;
    return { caffeine, coffeeCups, waterEntries };
  }, [visibleEntries]);

  return (
    <>
      <article className="diary-analytics-card">
        <p className="section-title">Consumo de cafeina</p>
        <div className="analytics-grid">
          <div>
            <p className="metric-label">Total</p>
            <p className="analytics-value">{analytics.caffeine} mg</p>
          </div>
          <div>
            <p className="metric-label">Cafe</p>
            <p className="analytics-value">{analytics.coffeeCups}</p>
          </div>
          <div>
            <p className="metric-label">Agua</p>
            <p className="analytics-value">{analytics.waterEntries}</p>
          </div>
        </div>
      </article>

      <div className="period-row" role="tablist" aria-label="Periodo diario">
        {(["hoy", "7d", "30d"] as const).map((item) => (
          <button
            key={item}
            type="button"
            className={`chip-button period-chip ${period === item ? "is-active" : ""}`}
            role="tab"
            aria-selected={period === item}
            onClick={() => setPeriod(item)}
          >
            {item.toUpperCase()}
          </button>
        ))}
      </div>

      <div className="premium-tabs" role="tablist" aria-label="Tabs diario">
        <button type="button" className={`premium-tab ${tab === "actividad" ? "is-active" : ""}`} role="tab" aria-selected={tab === "actividad"} onClick={() => setTab("actividad")}>ACTIVIDAD</button>
        <button type="button" className={`premium-tab ${tab === "despensa" ? "is-active" : ""}`} role="tab" aria-selected={tab === "despensa"} onClick={() => setTab("despensa")}>DESPENSA</button>
      </div>

      {tab === "actividad" ? (
        <ul className="diary-list">
          {visibleEntries.length ? visibleEntries.map((entry) => (
            <li key={entry.id} className="diary-card">
              <div>
                <p className="feed-user">{entry.coffee_name || "Entrada"}</p>
                <p className="feed-meta">{entry.preparation_type || entry.type}</p>
              </div>
              <div className="score-pill">{entry.caffeine_mg} mg</div>
              <p className="feed-meta">{toRelativeMinutes(entry.timestamp)}</p>
            </li>
          )) : <li className="diary-card">Sin actividad en el periodo</li>}
        </ul>
      ) : null}

      {tab === "despensa" ? (
        <ul className="coffee-list">
          {pantryRows.length ? pantryRows.map((row) => (
            <li key={`${row.item.user_id}-${row.item.coffee_id}`} className="coffee-card">
              <p className="coffee-origin">{row.coffee?.pais_origen ?? "Origen"}</p>
              <strong>{row.coffee?.nombre ?? row.item.coffee_id}</strong>
              <p className="coffee-sub">Stock {row.item.total_grams}g | restante {row.item.grams_remaining}g</p>
            </li>
          )) : <li className="coffee-card">Sin items en despensa</li>}
        </ul>
      ) : null}
    </>
  );
}

function ProfileView({
  user,
  tab,
  setTab,
  posts,
  favoriteCoffees,
  followers,
  following,
  onOpenCoffee,
  sidePanel
}: {
  user: UserRow;
  tab: "posts" | "adn" | "favoritos";
  setTab: (value: "posts" | "adn" | "favoritos") => void;
  posts: TimelineCard[];
  favoriteCoffees: CoffeeRow[];
  followers: number;
  following: number;
  onOpenCoffee: (coffeeId: string) => void;
  sidePanel?: ReactNode;
}) {
  const content = (
    <>
      <article className="profile-hero">
        <div className="avatar avatar-lg" aria-hidden="true">{user.full_name.split(" ").map((part) => part[0]).join("").slice(0, 2).toUpperCase()}</div>
        <div>
          <p className="feed-user">{user.full_name}</p>
          <p className="feed-meta">@{user.username}</p>
        </div>
        <button className="action-button action-button-ghost" type="button">Editar</button>
      </article>

      <section className="metric-grid">
        <article className="metric-card"><p className="metric-label">Posts</p><strong className="metric-value">{posts.length}</strong></article>
        <article className="metric-card"><p className="metric-label">Seguidores</p><strong className="metric-value">{followers}</strong></article>
        <article className="metric-card"><p className="metric-label">Siguiendo</p><strong className="metric-value">{following}</strong></article>
      </section>

      <div className="premium-tabs" role="tablist" aria-label="Tabs perfil">
        <button type="button" className={`premium-tab ${tab === "posts" ? "is-active" : ""}`} role="tab" aria-selected={tab === "posts"} onClick={() => setTab("posts")}>POSTS</button>
        <button type="button" className={`premium-tab ${tab === "adn" ? "is-active" : ""}`} role="tab" aria-selected={tab === "adn"} onClick={() => setTab("adn")}>ADN</button>
        <button type="button" className={`premium-tab ${tab === "favoritos" ? "is-active" : ""}`} role="tab" aria-selected={tab === "favoritos"} onClick={() => setTab("favoritos")}>FAVORITOS</button>
      </div>

      {tab === "posts" ? (
        <ul className="feed-list">
          {posts.length ? posts.map((post) => (
            <li key={post.id} className="feed-card">
              <p className="feed-text">{post.text || "Sin texto"}</p>
              <p className="feed-meta">hace {post.minsAgoLabel}</p>
            </li>
          )) : <li className="feed-card">Aun no hay publicaciones</li>}
        </ul>
      ) : null}

      {tab === "adn" ? (
        <article className="config-card">
          <p className="section-title">Perfil sensorial</p>
          <p className="metric-pill">Dulzor 0 | Acidez 0 | Cuerpo 0 | Amargor 0</p>
          <p className="feed-meta">Calculado desde favoritos y reseñas.</p>
        </article>
      ) : null}

      {tab === "favoritos" ? (
        <ul className="coffee-list">
          {favoriteCoffees.length ? favoriteCoffees.map((coffee) => (
            <li key={coffee.id}>
              <button type="button" className="coffee-card coffee-card-interactive profile-coffee-link" onClick={() => onOpenCoffee(coffee.id)}>
              <p className="coffee-origin">{coffee.pais_origen ?? "Origen"}</p>
              <strong>{coffee.nombre}</strong>
              <p className="coffee-sub">{coffee.marca}</p>
              </button>
            </li>
          )) : <li className="coffee-card">No hay cafes favoritos</li>}
        </ul>
      ) : null}
    </>
  );

  if (sidePanel) {
    return (
      <div className="split-with-side">
        <div>{content}</div>
        <aside className="timeline-side-column">{sidePanel}</aside>
      </div>
    );
  }

  return content;
}



