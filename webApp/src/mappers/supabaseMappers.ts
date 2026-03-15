import type {
  CoffeeRow,
  CoffeeReviewRow,
  CoffeeSensoryProfileRow,
  CommentRow,
  DiaryEntryRow,
  FavoriteRow,
  FollowRow,
  InitialDataBundle,
  LikeRow,
  NotificationRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  UserDataBundle,
  UserRow
} from "../types";
import { resolvedSupabaseUrl } from "../supabase";

function toStringOrEmpty(value: unknown): string {
  return typeof value === "string" ? value : value == null ? "" : String(value);
}

function toSafeImageUrl(value: unknown): string {
  let raw = toStringOrEmpty(value).trim();
  if (!raw) return "";

  raw = raw.replace(/^['"]+|['"]+$/g, "");
  raw = raw.replace(/\\\//g, "/").replace(/\\/g, "/");

  if (raw.startsWith("//")) {
    raw = `https:${raw}`;
  }

  if (raw.startsWith("/storage/") && resolvedSupabaseUrl) {
    raw = `${resolvedSupabaseUrl.replace(/\/+$/, "")}${raw}`;
  }

  // Solo forzamos https para URLs de Supabase.
  if (raw.startsWith("http://") && raw.includes(".supabase.co/")) {
    raw = `https://${raw.slice("http://".length)}`;
  }

  return raw;
}

function toNullableString(value: unknown): string | null {
  const text = toStringOrEmpty(value).trim();
  return text ? text : null;
}

function toNumberOr(value: unknown, fallback: number): number {
  const numeric = Number(value);
  return Number.isFinite(numeric) ? numeric : fallback;
}

function toTimestamp(value: unknown): number {
  const parsed = toNumberOr(value, 0);
  return parsed > 0 ? parsed : Date.now();
}

export function mapUserRow(input: unknown): UserRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: toNumberOr(row.id, 0),
    username: toStringOrEmpty(row.username),
    full_name: toStringOrEmpty(row.full_name),
    avatar_url: toSafeImageUrl(row.avatar_url),
    email: toStringOrEmpty(row.email),
    bio: toNullableString(row.bio)
  };
}

export function mapCoffeeRow(input: unknown): CoffeeRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: toStringOrEmpty(row.id),
    nombre: toStringOrEmpty(row.nombre),
    marca: toNullableString(row.marca),
    pais_origen: toNullableString(row.pais_origen),
    codigo_barras: toNullableString((row as Record<string, unknown>).codigo_barras),
    descripcion: toNullableString(row.descripcion),
    proceso: toNullableString(row.proceso),
    variedad_tipo: toNullableString(row.variedad_tipo),
    molienda_recomendada: toNullableString(row.molienda_recomendada),
    product_url: toNullableString(row.product_url),
    cafeina: toNullableString(row.cafeina),
    aroma: row.aroma == null ? null : toNumberOr(row.aroma, 0),
    sabor: row.sabor == null ? null : toNumberOr(row.sabor, 0),
    cuerpo: row.cuerpo == null ? null : toNumberOr(row.cuerpo, 0),
    acidez: row.acidez == null ? null : toNumberOr(row.acidez, 0),
    dulzura: row.dulzura == null ? null : toNumberOr(row.dulzura, 0),
    especialidad: toNullableString(row.especialidad),
    tueste: toNullableString(row.tueste),
    formato: toNullableString(row.formato),
    image_url: toStringOrEmpty(row.image_url)
  };
}

export function mapCustomCoffeeRow(input: unknown): CoffeeRow {
  const row = (input ?? {}) as Record<string, unknown>;
  const hasCaffeine = row.has_caffeine;
  return {
    id: toStringOrEmpty(row.id),
    nombre: toStringOrEmpty(row.name),
    marca: toNullableString(row.brand),
    pais_origen: toNullableString(row.country),
    descripcion: null,
    proceso: null,
    variedad_tipo: toNullableString(row.variety),
    molienda_recomendada: null,
    product_url: null,
    cafeina: hasCaffeine == null ? null : Boolean(hasCaffeine) ? "Con cafeina" : "Sin cafeina",
    aroma: null,
    sabor: null,
    cuerpo: null,
    acidez: null,
    dulzura: null,
    especialidad: toNullableString(row.specialty),
    tueste: toNullableString(row.roast),
    formato: toNullableString(row.format),
    image_url: toStringOrEmpty(row.image_url)
  };
}

export function mapReviewRow(input: unknown): CoffeeReviewRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: row.id == null ? undefined : toNumberOr(row.id, 0),
    coffee_id: toStringOrEmpty(row.coffee_id),
    user_id: row.user_id == null ? undefined : toNumberOr(row.user_id, 0),
    rating: toNumberOr(row.rating, 0),
    comment: toNullableString(row.comment),
    image_url: toNullableString(row.image_url),
    timestamp: toTimestamp(row.timestamp)
  };
}

export function mapSensoryProfileRow(input: unknown): CoffeeSensoryProfileRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    coffee_id: toStringOrEmpty(row.coffee_id),
    user_id: toNumberOr(row.user_id, 0),
    aroma: toNumberOr(row.aroma, 0),
    sabor: toNumberOr(row.sabor, 0),
    cuerpo: toNumberOr(row.cuerpo, 0),
    acidez: toNumberOr(row.acidez, 0),
    dulzura: toNumberOr(row.dulzura, 0),
    updated_at: toTimestamp(row.updated_at)
  };
}

export function mapPostRow(input: unknown): PostRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: toStringOrEmpty(row.id),
    user_id: toNumberOr(row.user_id, 0),
    image_url: toStringOrEmpty(row.image_url),
    comment: toStringOrEmpty(row.comment),
    timestamp: toTimestamp(row.timestamp)
  };
}

export function mapLikeRow(input: unknown): LikeRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    post_id: toStringOrEmpty(row.post_id),
    user_id: toNumberOr(row.user_id, 0)
  };
}

export function mapCommentRow(input: unknown): CommentRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: toNumberOr(row.id, 0),
    post_id: toStringOrEmpty(row.post_id),
    user_id: toNumberOr(row.user_id, 0),
    text: toStringOrEmpty(row.text),
    timestamp: toTimestamp(row.timestamp)
  };
}

export function mapPostCoffeeTagRow(input: unknown): PostCoffeeTagRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    post_id: toStringOrEmpty(row.post_id),
    coffee_id: toNullableString(row.coffee_id),
    coffee_name: toStringOrEmpty(row.coffee_name),
    coffee_brand: toStringOrEmpty(row.coffee_brand)
  };
}

export function mapFollowRow(input: unknown): FollowRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    follower_id: toNumberOr(row.follower_id, 0),
    followed_id: toNumberOr(row.followed_id, 0),
    created_at: toTimestamp(row.created_at)
  };
}

export function mapDiaryEntryRow(input: unknown): DiaryEntryRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: toNumberOr(row.id, 0),
    user_id: toNumberOr(row.user_id, 0),
    coffee_id: toNullableString(row.coffee_id),
    coffee_name: toStringOrEmpty(row.coffee_name),
    caffeine_mg: toNumberOr(row.caffeine_mg, 0),
    amount_ml: toNumberOr(row.amount_ml, 0),
    coffee_grams: toNumberOr(row.coffee_grams, 0),
    preparation_type: toStringOrEmpty(row.preparation_type),
    size_label: toNullableString(row.size_label),
    timestamp: toTimestamp(row.timestamp),
    type: toStringOrEmpty(row.type),
    pantry_item_id: toNullableString(row.pantry_item_id)
  };
}

export function mapPantryItemRow(input: unknown): PantryItemRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: toStringOrEmpty(row.id),
    coffee_id: toStringOrEmpty(row.coffee_id),
    user_id: toNumberOr(row.user_id, 0),
    grams_remaining: toNumberOr(row.grams_remaining, 0),
    total_grams: toNumberOr(row.total_grams, 0),
    last_updated: toTimestamp(row.last_updated)
  };
}

export function mapFavoriteRow(input: unknown): FavoriteRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    coffee_id: toStringOrEmpty(row.coffee_id),
    user_id: toNumberOr(row.user_id, 0),
    saved_at: toTimestamp(row.saved_at)
  };
}

export function mapNotificationRow(input: unknown): NotificationRow {
  const row = (input ?? {}) as Record<string, unknown>;
  return {
    id: toNumberOr(row.id, 0),
    user_id: toNumberOr(row.user_id, 0),
    type: toStringOrEmpty(row.type),
    from_username: toStringOrEmpty(row.from_username),
    message: toStringOrEmpty(row.message),
    timestamp: toTimestamp(row.timestamp),
    is_read: Boolean(row.is_read),
    related_id: toNullableString(row.related_id)
  };
}

export function mapInitialDataBundle(input: InitialDataBundle): InitialDataBundle {
  return {
    users: input.users.map(mapUserRow),
    coffees: input.coffees.map(mapCoffeeRow),
    reviews: input.reviews.map(mapReviewRow),
    sensoryProfiles: input.sensoryProfiles.map(mapSensoryProfileRow),
    posts: input.posts.map(mapPostRow),
    likes: input.likes.map(mapLikeRow),
    comments: input.comments.map(mapCommentRow),
    postCoffeeTags: input.postCoffeeTags.map(mapPostCoffeeTagRow),
    follows: input.follows.map(mapFollowRow)
  };
}

export function mapUserDataBundle(input: UserDataBundle): UserDataBundle {
  return {
    diaryEntries: input.diaryEntries.map(mapDiaryEntryRow),
    pantryItems: input.pantryItems.map(mapPantryItemRow),
    favorites: input.favorites.map(mapFavoriteRow),
    customCoffees: input.customCoffees.map(mapCoffeeRow),
    finishedCoffees: input.finishedCoffees.map((r) => ({ coffee_id: r.coffee_id, finished_at: r.finished_at }))
  };
}
