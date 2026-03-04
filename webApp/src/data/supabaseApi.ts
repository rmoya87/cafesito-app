import { getSupabaseClient } from "../supabase";
import type {
  CommentRow,
  CoffeeRow,
  CoffeeReviewRow,
  CoffeeSensoryProfileRow,
  DiaryEntryRow,
  FavoriteRow,
  FollowRow,
  InitialDataBundle,
  LikeRow,
  NotificationRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  UserDataBundle
} from "../types";
import {
  mapCommentRow,
  mapCoffeeRow,
  mapDiaryEntryRow,
  mapInitialDataBundle,
  mapPantryItemRow,
  mapPostRow,
  mapReviewRow,
  mapSensoryProfileRow,
  mapUserDataBundle
} from "../mappers/supabaseMappers";

type SupabaseErrorLike = { message: string } | null;

function throwIfError(error: SupabaseErrorLike): void {
  if (error) throw new Error(error.message);
}

function extractMentionUsernames(text: string): string[] {
  const mentionRegex = /@([A-Za-z0-9._-]{2,30})/g;
  const seen = new Set<string>();
  const usernames: string[] = [];
  for (const match of text.matchAll(mentionRegex)) {
    const username = String(match[1] ?? "").trim();
    if (!username) continue;
    const key = username.toLowerCase();
    if (seen.has(key)) continue;
    seen.add(key);
    usernames.push(username);
  }
  return usernames;
}

async function createTimelineNotification(payload: {
  userId: number;
  type: "MENTION" | "COMMENT" | "FOLLOW";
  fromUsername: string;
  message: string;
  relatedId?: string | null;
}): Promise<void> {
  const supabase = getSupabaseClient();
  const rpc = await supabase.rpc("create_notification", {
    p_user_id: payload.userId,
    p_type: payload.type,
    p_from_username: payload.fromUsername,
    p_message: payload.message,
    p_timestamp: Date.now(),
    p_related_id: payload.relatedId ?? null
  });
  if (!rpc.error) return;

  const fallback = await supabase.from("notifications_db").insert({
    user_id: payload.userId,
    type: payload.type,
    from_username: payload.fromUsername,
    message: payload.message,
    timestamp: Date.now(),
    is_read: false,
    related_id: payload.relatedId ?? null
  });
  throwIfError(fallback.error);
}

async function notifyMentionsForText(params: {
  text: string;
  authorId: number;
  authorUsername: string;
  relatedId: string;
  mentionMessage: string;
}): Promise<void> {
  const supabase = getSupabaseClient();
  const usernames = extractMentionUsernames(params.text).filter(
    (username) => username.toLowerCase() !== params.authorUsername.toLowerCase()
  );
  if (!usernames.length) return;

  for (const username of usernames) {
    const { data, error } = await supabase
      .from("users_db")
      .select("id,username")
      .ilike("username", username)
      .limit(1)
      .maybeSingle<{ id: number; username: string }>();

    if (error || !data) continue;
    if (Number(data.id) === params.authorId) continue;
    try {
      await createTimelineNotification({
        userId: Number(data.id),
        type: "MENTION",
        fromUsername: params.authorUsername,
        message: params.mentionMessage,
        relatedId: params.relatedId
      });
    } catch (notifyError) {
      console.warn("No se pudo crear notificación de mención", notifyError);
    }
  }
}

// Cache en memoria para reducir Cached Egress (TTL 90s)
const INITIAL_DATA_CACHE_TTL_MS = 90 * 1000;
let cachedInitialData: InitialDataBundle | null = null;
let cachedInitialDataAt = 0;

export async function fetchInitialData(forceRefresh = false): Promise<InitialDataBundle> {
  const now = Date.now();
  if (
    !forceRefresh &&
    cachedInitialData !== null &&
    now - cachedInitialDataAt < INITIAL_DATA_CACHE_TTL_MS
  ) {
    return cachedInitialData;
  }

  const supabase = getSupabaseClient();
  const usersReq = supabase.from("users_db").select("id,username,full_name,avatar_url,email,bio").limit(1500);
  const coffeesReq = supabase
    .from("coffees")
    .select(
      "id,nombre,marca,pais_origen,codigo_barras,descripcion,proceso,variedad_tipo,molienda_recomendada,product_url,cafeina,aroma,sabor,cuerpo,acidez,dulzura,especialidad,tueste,formato,image_url"
    )
    .order("nombre", { ascending: true })
    .limit(2000);
  const reviewsReq = supabase
    .from("reviews_db")
    .select("id,coffee_id,user_id,rating,comment,image_url,timestamp")
    .limit(3000);
  const sensoryReq = supabase
    .from("coffee_sensory_profiles")
    .select("coffee_id,user_id,aroma,sabor,cuerpo,acidez,dulzura,updated_at")
    .limit(3000);
  const postsReq = supabase
    .from("posts_db")
    .select("id,user_id,image_url,comment,timestamp")
    .order("timestamp", { ascending: false })
    .limit(120);
  const likesReq = supabase.from("likes_db").select("post_id,user_id").limit(2000);
  const commentsReq = supabase
    .from("comments_db")
    .select("id,post_id,user_id,text,timestamp")
    .order("timestamp", { ascending: false })
    .limit(2000);
  const tagsReq = supabase.from("post_coffee_tags").select("post_id,coffee_id,coffee_name,coffee_brand").limit(500);
  const followsReq = supabase.from("follows").select("follower_id,followed_id,created_at").limit(2000);

  const [usersRes, coffeesRes, reviewsRes, sensoryRes, postsRes, likesRes, commentsRes, tagsRes, followsRes] = await Promise.all([
    usersReq,
    coffeesReq,
    reviewsReq,
    sensoryReq,
    postsReq,
    likesReq,
    commentsReq,
    tagsReq,
    followsReq
  ]);

  throwIfError(
    usersRes.error ??
    coffeesRes.error ??
    reviewsRes.error ??
    sensoryRes.error ??
    postsRes.error ??
    likesRes.error ??
    commentsRes.error ??
    tagsRes.error ??
    followsRes.error
  );

  const bundle = mapInitialDataBundle({
    users: (usersRes.data ?? []) as InitialDataBundle["users"],
    coffees: (coffeesRes.data ?? []) as InitialDataBundle["coffees"],
    reviews: (reviewsRes.data ?? []) as InitialDataBundle["reviews"],
    sensoryProfiles: (sensoryRes.data ?? []) as InitialDataBundle["sensoryProfiles"],
    posts: (postsRes.data ?? []) as InitialDataBundle["posts"],
    likes: (likesRes.data ?? []) as InitialDataBundle["likes"],
    comments: (commentsRes.data ?? []) as InitialDataBundle["comments"],
    postCoffeeTags: (tagsRes.data ?? []) as InitialDataBundle["postCoffeeTags"],
    follows: (followsRes.data ?? []) as InitialDataBundle["follows"]
  });
  cachedInitialData = bundle;
  cachedInitialDataAt = Date.now();
  return bundle;
}

export async function fetchUserData(userId: number): Promise<UserDataBundle> {
  const supabase = getSupabaseClient();
  const diaryReq = supabase
    .from("diary_entries")
    .select("id,user_id,coffee_id,coffee_name,caffeine_mg,amount_ml,preparation_type,timestamp,type")
    .eq("user_id", userId)
    .order("timestamp", { ascending: false })
    .limit(500);

  const pantryReq = supabase
    .from("pantry_items")
    .select("coffee_id,user_id,grams_remaining,total_grams,last_updated")
    .eq("user_id", userId)
    .limit(500);

  const favoritesReq = supabase
    .from("local_favorites")
    .select("coffee_id,user_id,saved_at")
    .eq("user_id", userId)
    .limit(500);
  const [diaryRes, pantryRes, favoritesRes] = await Promise.all([diaryReq, pantryReq, favoritesReq]);
  throwIfError(diaryRes.error ?? pantryRes.error ?? favoritesRes.error);

  // Custom coffees live in coffees with is_custom = true and user_id set.
  let customCoffees: CoffeeRow[] = [];
  try {
    const { data: customRows } = await supabase
      .from("coffees")
      .select("id,nombre,marca,pais_origen,descripcion,proceso,variedad_tipo,molienda_recomendada,product_url,cafeina,aroma,sabor,cuerpo,acidez,dulzura,especialidad,tueste,formato,image_url")
      .eq("is_custom", true)
      .eq("user_id", userId)
      .limit(500);

    customCoffees = (customRows ?? []).map(mapCoffeeRow);
  } catch {
    customCoffees = [];
  }

  return mapUserDataBundle({
    diaryEntries: (diaryRes.data ?? []) as UserDataBundle["diaryEntries"],
    pantryItems: (pantryRes.data ?? []) as UserDataBundle["pantryItems"],
    favorites: (favoritesRes.data ?? []) as UserDataBundle["favorites"],
    customCoffees
  });
}

export async function toggleLike(postId: string, userId: number, alreadyLiked: boolean): Promise<LikeRow | null> {
  const supabase = getSupabaseClient();
  if (alreadyLiked) {
    const { error } = await supabase.from("likes_db").delete().eq("post_id", postId).eq("user_id", userId);
    throwIfError(error);
    return null;
  }

  const payload: LikeRow = { post_id: postId, user_id: userId };
  const { error } = await supabase.from("likes_db").insert(payload);
  throwIfError(error);
  return payload;
}

export async function createComment(postId: string, userId: number, text: string): Promise<CommentRow> {
  const supabase = getSupabaseClient();
  const payload = {
    post_id: postId,
    user_id: userId,
    text,
    timestamp: Date.now()
  };

  const { data, error } = await supabase
    .from("comments_db")
    .insert(payload)
    .select("id,post_id,user_id,text,timestamp")
    .single();

  throwIfError(error);
  const row = mapCommentRow(data);

  // Web no tenía pipeline de menciones en comentarios; lo resolvemos aquí.
  const { data: author } = await supabase
    .from("users_db")
    .select("username")
    .eq("id", userId)
    .limit(1)
    .maybeSingle<{ username: string }>();
  const authorUsername = String(author?.username ?? "").trim();
  if (authorUsername) {
    try {
      await notifyMentionsForText({
        text,
        authorId: userId,
        authorUsername,
        relatedId: `${postId}:${row.id}`,
        mentionMessage: text
      });
    } catch (notifyError) {
      console.warn("No se pudieron procesar menciones del comentario", notifyError);
    }
  }

  return row;
}

export async function toggleFollow(
  followerId: number,
  followedId: number,
  alreadyFollowing: boolean
): Promise<FollowRow | null> {
  const supabase = getSupabaseClient();
  if (alreadyFollowing) {
    const { error } = await supabase
      .from("follows")
      .delete()
      .eq("follower_id", followerId)
      .eq("followed_id", followedId);
    throwIfError(error);
    return null;
  }

  const payload: FollowRow = {
    follower_id: followerId,
    followed_id: followedId,
    created_at: Date.now()
  };
  const { error } = await supabase.from("follows").insert(payload);
  throwIfError(error);
  if (followerId !== followedId) {
    try {
      const { data: follower } = await supabase
        .from("users_db")
        .select("username")
        .eq("id", followerId)
        .limit(1)
        .maybeSingle<{ username: string }>();
      const fromUsername = String(follower?.username ?? "").trim();
      if (fromUsername) {
        await createTimelineNotification({
          userId: followedId,
          type: "FOLLOW",
          fromUsername,
          message: "ha empezado a seguirte",
          relatedId: String(followerId)
        });
      }
    } catch (notifyError) {
      console.warn("No se pudo crear notificación de seguimiento", notifyError);
    }
  }
  return payload;
}

export async function updatePost(postId: string, text: string, imageUrl: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase
    .from("posts_db")
    .update({ comment: text, image_url: imageUrl })
    .eq("id", postId);
  throwIfError(error);
}

export async function deletePost(postId: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("posts_db").delete().eq("id", postId);
  throwIfError(error);
}

export async function updateComment(commentId: number, text: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("comments_db").update({ text }).eq("id", commentId);
  throwIfError(error);
}

export async function deleteComment(commentId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("comments_db").delete().eq("id", commentId);
  throwIfError(error);
}

export async function updateUserProfile(
  userId: number,
  payload: { full_name: string; bio: string | null; avatar_url?: string | null }
): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase
    .from("users_db")
    .update({
      full_name: payload.full_name,
      bio: payload.bio,
      ...(payload.avatar_url !== undefined ? { avatar_url: payload.avatar_url } : {})
    })
    .eq("id", userId);
  throwIfError(error);
}

export async function createPost(
  userId: number,
  text: string,
  imageUrl: string
): Promise<PostRow> {
  const supabase = getSupabaseClient();
  const id = crypto.randomUUID();
  const payload = {
    id,
    user_id: userId,
    comment: text,
    image_url: imageUrl,
    timestamp: Date.now()
  };
  const { data, error } = await supabase
    .from("posts_db")
    .insert(payload)
    .select("id,user_id,image_url,comment,timestamp")
    .single();
  throwIfError(error);
  const post = mapPostRow(data);

  // Web no enviaba notificaciones por mención en posts.
  const { data: author } = await supabase
    .from("users_db")
    .select("username")
    .eq("id", userId)
    .limit(1)
    .maybeSingle<{ username: string }>();
  const authorUsername = String(author?.username ?? "").trim();
  if (authorUsername) {
    try {
      await notifyMentionsForText({
        text,
        authorId: userId,
        authorUsername,
        relatedId: `${post.id}:-1`,
        mentionMessage: "te ha mencionado"
      });
    } catch (notifyError) {
      console.warn("No se pudieron procesar menciones del post", notifyError);
    }
  }

  return post;
}

export async function addPostCoffeeTag(tag: PostCoffeeTagRow): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("post_coffee_tags").insert(tag);
  throwIfError(error);
}

export async function uploadImageFile(bucket: string, file: File): Promise<string> {
  const supabase = getSupabaseClient();
  const ext = (file.name.split(".").pop() ?? "jpg").toLowerCase();
  const path = `${Date.now()}_${crypto.randomUUID()}.${ext}`;
  const { error } = await supabase.storage.from(bucket).upload(path, file, {
    upsert: true,
    contentType: file.type || "image/jpeg"
  });
  throwIfError(error);
  const { data } = supabase.storage.from(bucket).getPublicUrl(path);
  if (!data.publicUrl) throw new Error("No se pudo obtener URL publica de la imagen.");
  return data.publicUrl;
}

export async function toggleFavoriteCoffee(
  userId: number,
  coffeeId: string,
  alreadyFavorite: boolean
): Promise<FavoriteRow | null> {
  const supabase = getSupabaseClient();
  if (alreadyFavorite) {
    const { error } = await supabase.from("local_favorites").delete().eq("user_id", userId).eq("coffee_id", coffeeId);
    throwIfError(error);
    return null;
  }

  const payload: FavoriteRow = {
    user_id: userId,
    coffee_id: coffeeId,
    saved_at: Date.now()
  };
  // No dependemos de una constraint UNIQUE en (user_id, coffee_id):
  // limpiamos posibles duplicados y luego insertamos una fila fresca.
  const { error: cleanupError } = await supabase.from("local_favorites").delete().eq("user_id", userId).eq("coffee_id", coffeeId);
  throwIfError(cleanupError);
  const { error } = await supabase.from("local_favorites").insert(payload);
  throwIfError(error);
  return payload;
}

export async function upsertCoffeeReview(payload: {
  coffeeId: string;
  userId: number;
  rating: number;
  comment: string;
  imageUrl?: string;
}): Promise<CoffeeReviewRow> {
  const supabase = getSupabaseClient();
  const row = {
    coffee_id: payload.coffeeId,
    user_id: payload.userId,
    rating: payload.rating,
    comment: payload.comment,
    image_url: payload.imageUrl ?? "",
    timestamp: Date.now()
  };
  const { data, error } = await supabase
    .from("reviews_db")
    .upsert(row, { onConflict: "coffee_id,user_id" })
    .select("id,coffee_id,user_id,rating,comment,image_url,timestamp")
    .single();
  throwIfError(error);
  return mapReviewRow(data);
}

export async function deleteCoffeeReview(coffeeId: string, userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("reviews_db").delete().eq("coffee_id", coffeeId).eq("user_id", userId);
  throwIfError(error);
}

export async function upsertCoffeeSensoryProfile(payload: {
  coffeeId: string;
  userId: number;
  aroma: number;
  sabor: number;
  cuerpo: number;
  acidez: number;
  dulzura: number;
}): Promise<CoffeeSensoryProfileRow> {
  const supabase = getSupabaseClient();
  const row = {
    coffee_id: payload.coffeeId,
    user_id: payload.userId,
    aroma: payload.aroma,
    sabor: payload.sabor,
    cuerpo: payload.cuerpo,
    acidez: payload.acidez,
    dulzura: payload.dulzura,
    updated_at: Date.now()
  };
  const { data, error } = await supabase
    .from("coffee_sensory_profiles")
    .upsert(row, { onConflict: "coffee_id,user_id" })
    .select("coffee_id,user_id,aroma,sabor,cuerpo,acidez,dulzura,updated_at")
    .single();
  throwIfError(error);
  return mapSensoryProfileRow(data);
}

export async function upsertPantryStock(payload: {
  coffeeId: string;
  userId: number;
  gramsRemaining: number;
  totalGrams: number;
}): Promise<PantryItemRow> {
  const supabase = getSupabaseClient();
  const row = {
    coffee_id: payload.coffeeId,
    user_id: payload.userId,
    grams_remaining: payload.gramsRemaining,
    total_grams: payload.totalGrams,
    last_updated: Date.now()
  };
  const { data, error } = await supabase
    .from("pantry_items")
    .upsert(row, { onConflict: "coffee_id,user_id" })
    .select("coffee_id,user_id,grams_remaining,total_grams,last_updated")
    .single();
  throwIfError(error);
  return mapPantryItemRow(data);
}

export async function deletePantryItem(coffeeId: string, userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("pantry_items").delete().eq("coffee_id", coffeeId).eq("user_id", userId);
  throwIfError(error);
}

export async function upsertCustomCoffee(payload: {
  userId: number;
  name: string;
  brand: string;
  specialty: string;
  country: string;
  format: string;
  roast?: string | null;
  variety?: string | null;
  hasCaffeine: boolean;
  imageUrl?: string;
  totalGrams?: number;
}): Promise<CoffeeRow> {
  const supabase = getSupabaseClient();
  const id = crypto.randomUUID();
  const row = {
    id,
    user_id: payload.userId,
    nombre: payload.name.trim(),
    marca: payload.brand.trim(),
    especialidad: payload.specialty.trim(),
    tueste: payload.roast?.trim() || null,
    variedad_tipo: payload.variety?.trim() || null,
    pais_origen: payload.country.trim(),
    cafeina: payload.hasCaffeine ? "Sí" : "No",
    formato: payload.format.trim(),
    image_url: payload.imageUrl ?? "",
    is_custom: true
  };

  const { data, error } = await supabase
    .from("coffees")
    .upsert(row, { onConflict: "id" })
    .select("id,nombre,marca,pais_origen,especialidad,tueste,variedad_tipo,cafeina,formato,image_url")
    .single();
  throwIfError(error);

  return mapCoffeeRow(data);
}

export async function createDiaryEntry(payload: {
  userId: number;
  coffeeId: string | null;
  coffeeName: string;
  coffeeBrand?: string;
  amountMl: number;
  caffeineMg: number;
  coffeeGrams?: number;
  preparationType: string;
  sizeLabel?: string | null;
  type?: string;
}): Promise<DiaryEntryRow> {
  const supabase = getSupabaseClient();
  const row: Record<string, unknown> = {
    user_id: payload.userId,
    coffee_id: payload.coffeeId,
    coffee_name: payload.coffeeName,
    caffeine_mg: Math.max(0, Math.round(payload.caffeineMg)),
    amount_ml: Math.max(1, Math.round(payload.amountMl)),
    preparation_type: payload.preparationType,
    timestamp: Date.now(),
    type: payload.type ?? "CUP"
  };
  if (payload.coffeeBrand !== undefined) row.coffee_brand = payload.coffeeBrand;
  if (payload.coffeeGrams !== undefined) row.coffee_grams = Math.max(0, Math.round(payload.coffeeGrams));
  if (payload.sizeLabel !== undefined) row.size_label = payload.sizeLabel ?? null;
  const { data, error } = await supabase
    .from("diary_entries")
    .insert(row)
    .select("id,user_id,coffee_id,coffee_name,caffeine_mg,amount_ml,preparation_type,timestamp,type")
    .single();
  throwIfError(error);
  return mapDiaryEntryRow(data);
}

export async function deleteDiaryEntry(entryId: number, userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("diary_entries").delete().eq("id", entryId).eq("user_id", userId);
  throwIfError(error);
}

export async function updateDiaryEntry(payload: {
  entryId: number;
  userId: number;
  caffeineMg: number;
  amountMl: number;
  preparationType: string;
  timestampMs?: number;
}): Promise<DiaryEntryRow> {
  const supabase = getSupabaseClient();
  const row = {
    caffeine_mg: Math.max(0, Math.round(payload.caffeineMg)),
    amount_ml: Math.max(1, Math.round(payload.amountMl)),
    preparation_type: payload.preparationType.trim() || "None",
    ...(Number.isFinite(payload.timestampMs) ? { timestamp: Number(payload.timestampMs) } : {})
  };
  const { data, error } = await supabase
    .from("diary_entries")
    .update(row)
    .eq("id", payload.entryId)
    .eq("user_id", payload.userId)
    .select("id,user_id,coffee_id,coffee_name,caffeine_mg,amount_ml,preparation_type,timestamp,type")
    .single();
  throwIfError(error);
  return mapDiaryEntryRow(data);
}

export async function fetchNotifications(userId: number): Promise<NotificationRow[]> {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase
    .from("notifications_db")
    .select("id,user_id,type,from_username,message,timestamp,is_read,related_id")
    .eq("user_id", userId)
    .order("timestamp", { ascending: false })
    .limit(50);
  throwIfError(error);
  return (data ?? []).map((row) => ({
    id: Number(row.id),
    user_id: Number(row.user_id),
    type: String(row.type),
    from_username: String(row.from_username),
    message: String(row.message),
    timestamp: Number(row.timestamp),
    is_read: Boolean(row.is_read),
    related_id: row.related_id ? String(row.related_id) : null
  }));
}

export async function markNotificationsAsRead(userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase
    .from("notifications_db")
    .update({ is_read: true })
    .eq("user_id", userId)
    .eq("is_read", false);
  throwIfError(error);
}

export async function deleteNotification(notificationId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("notifications_db").delete().eq("id", notificationId);
  throwIfError(error);
}

const ACCOUNT_DELETION_GRACE_MS = 30 * 24 * 60 * 60 * 1000;
const ACCOUNT_STATUS_PENDING = "inactive_pending_deletion";
const ACCOUNT_STATUS_ACTIVE = "active";

type AccountLifecycleRow = {
  account_status?: string | null;
  scheduled_deletion_at?: number | null;
};

export async function requestAccountDeletion(userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const now = Date.now();
  const scheduled = now + ACCOUNT_DELETION_GRACE_MS;
  const { error } = await supabase
    .from("users_db")
    .update({
      account_status: ACCOUNT_STATUS_PENDING,
      deactivation_requested_at: now,
      scheduled_deletion_at: scheduled
    })
    .eq("id", userId);
  throwIfError(error);
}

async function reactivateAccount(userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase
    .from("users_db")
    .update({
      account_status: ACCOUNT_STATUS_ACTIVE,
      deactivation_requested_at: null,
      scheduled_deletion_at: null
    })
    .eq("id", userId);
  throwIfError(error);
}

async function hardDeleteAccountData(userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { data: postsRows } = await supabase.from("posts_db").select("id").eq("user_id", userId);
  const postIds = ((postsRows ?? []) as Array<{ id?: string | null }>).map((row) => row.id).filter(Boolean) as string[];

  if (postIds.length) {
    const { error: tagsError } = await supabase.from("post_coffee_tags").delete().in("post_id", postIds);
    throwIfError(tagsError);
  }

  const results = await Promise.all([
    supabase.from("comments_db").delete().eq("user_id", userId),
    supabase.from("likes_db").delete().eq("user_id", userId),
    supabase.from("local_favorites").delete().eq("user_id", userId),
    supabase.from("reviews_db").delete().eq("user_id", userId),
    supabase.from("coffee_sensory_profiles").delete().eq("user_id", userId),
    supabase.from("diary_entries").delete().eq("user_id", userId),
    supabase.from("pantry_items").delete().eq("user_id", userId),
    supabase.from("notifications_db").delete().eq("user_id", userId),
    supabase.from("follows").delete().eq("follower_id", userId),
    supabase.from("follows").delete().eq("followed_id", userId),
    supabase.from("posts_db").delete().eq("user_id", userId),
    supabase.from("users_db").delete().eq("id", userId)
  ]);

  results.forEach((result) => throwIfError(result.error as SupabaseErrorLike));
}

export async function syncAccountLifecycleAfterLogin(
  userId: number
): Promise<"none" | "reactivated" | "deleted"> {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase
    .from("users_db")
    .select("account_status,scheduled_deletion_at")
    .eq("id", userId)
    .single<AccountLifecycleRow>();
  throwIfError(error);

  const status = String(data?.account_status ?? "");
  const scheduled = Number(data?.scheduled_deletion_at ?? 0);
  if (status !== ACCOUNT_STATUS_PENDING) return "none";

  if (Number.isFinite(scheduled) && scheduled > 0 && scheduled <= Date.now()) {
    await hardDeleteAccountData(userId);
    return "deleted";
  }

  await reactivateAccount(userId);
  return "reactivated";
}



