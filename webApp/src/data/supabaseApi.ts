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
  ListPrivacy,
  NotificationRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  UserDataBundle,
  UserListRow,
  UserRow
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
  mapUserDataBundle,
  mapUserRow
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

// --- Límites acotados para fetchInitialData (evitar cargas excesivas; ver doc ANDROID_Y_WEBAPP_SERVICIOS_CONECTADOS). posts/likes/comments/postCoffeeTags ya no se cargan. ---
const INITIAL_DATA_LIMITS = {
  users: 1200,
  coffees: 1500,
  reviews: 2000,
  sensoryProfiles: 2000,
  follows: 1500
} as const;

/** TTL de la caché de datos iniciales (ms). Configurable; 90s por defecto. En error al cargar se reintenta contra Supabase. */
export const INITIAL_DATA_CACHE_TTL_MS = 90 * 1000;

const RETRY_DELAY_MS = 2000;
let cachedInitialData: InitialDataBundle | null = null;
let cachedInitialDataAt = 0;

async function fetchInitialDataFromSupabase(): Promise<InitialDataBundle> {
  const supabase = getSupabaseClient();
  const L = INITIAL_DATA_LIMITS;
  const usersReq = supabase.from("users_db").select("id,username,full_name,avatar_url,email,bio").limit(L.users);
  const coffeesReq = supabase
    .from("coffees")
    .select(
      "id,nombre,marca,pais_origen,codigo_barras,descripcion,proceso,variedad_tipo,molienda_recomendada,product_url,cafeina,aroma,sabor,cuerpo,acidez,dulzura,especialidad,tueste,formato,image_url"
    )
    .order("nombre", { ascending: true })
    .limit(L.coffees);
  const reviewsReq = supabase
    .from("reviews_db")
    .select("id,coffee_id,user_id,rating,comment,image_url,timestamp")
    .limit(L.reviews);
  const sensoryReq = supabase
    .from("coffee_sensory_profiles")
    .select("coffee_id,user_id,aroma,sabor,cuerpo,acidez,dulzura,updated_at")
    .limit(L.sensoryProfiles);
  const followsReq = supabase.from("follows").select("follower_id,followed_id,created_at").limit(L.follows);

  const [usersRes, coffeesRes, reviewsRes, sensoryRes, followsRes] = await Promise.all([
    usersReq,
    coffeesReq,
    reviewsReq,
    sensoryReq,
    followsReq
  ]);

  throwIfError(
    usersRes.error ??
    coffeesRes.error ??
    reviewsRes.error ??
    sensoryRes.error ??
    followsRes.error
  );

  return mapInitialDataBundle({
    users: (usersRes.data ?? []) as InitialDataBundle["users"],
    coffees: (coffeesRes.data ?? []) as InitialDataBundle["coffees"],
    reviews: (reviewsRes.data ?? []) as InitialDataBundle["reviews"],
    sensoryProfiles: (sensoryRes.data ?? []) as InitialDataBundle["sensoryProfiles"],
    follows: (followsRes.data ?? []) as InitialDataBundle["follows"]
  });
}

export async function fetchInitialData(forceRefresh = false): Promise<InitialDataBundle> {
  const now = Date.now();
  if (
    !forceRefresh &&
    cachedInitialData !== null &&
    now - cachedInitialDataAt < INITIAL_DATA_CACHE_TTL_MS
  ) {
    return cachedInitialData;
  }

  try {
    const bundle = await fetchInitialDataFromSupabase();
    cachedInitialData = bundle;
    cachedInitialDataAt = Date.now();
    return bundle;
  } catch (e) {
    cachedInitialData = null;
    cachedInitialDataAt = 0;
    await new Promise((r) => setTimeout(r, RETRY_DELAY_MS));
    const bundle = await fetchInitialDataFromSupabase();
    cachedInitialData = bundle;
    cachedInitialDataAt = Date.now();
    return bundle;
  }
}

export async function fetchUserData(userId: number): Promise<UserDataBundle> {
  const supabase = getSupabaseClient();
  const diaryReq = supabase
    .from("diary_entries")
    .select("id,user_id,coffee_id,coffee_name,caffeine_mg,amount_ml,coffee_grams,preparation_type,size_label,timestamp,type,pantry_item_id")
    .eq("user_id", userId)
    .order("timestamp", { ascending: false })
    .limit(500);

  const pantryReq = supabase
    .from("pantry_items")
    .select("id,coffee_id,user_id,grams_remaining,total_grams,last_updated")
    .eq("user_id", userId)
    .order("last_updated", { ascending: false })
    .limit(500);

  const favoritesReq = supabase
    .from("local_favorites")
    .select("coffee_id,user_id,saved_at")
    .eq("user_id", userId)
    .limit(500);

  const finishedCoffeesReq = supabase
    .from("pantry_historical")
    .select("coffee_id,finished_at")
    .eq("user_id", userId)
    .order("finished_at", { ascending: false })
    .limit(500);

  const [diaryRes, pantryRes, favoritesRes, finishedRes] = await Promise.all([
    diaryReq,
    pantryReq,
    favoritesReq,
    finishedCoffeesReq
  ]);
  throwIfError(diaryRes.error ?? pantryRes.error ?? favoritesRes.error ?? finishedRes.error);

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
    customCoffees,
    finishedCoffees: (finishedRes.data ?? []).map(
      (r: { coffee_id: string; finished_at: number }) => ({ coffee_id: r.coffee_id, finished_at: r.finished_at })
    ) as UserDataBundle["finishedCoffees"]
  });
}

/** Ítem de lista para feed de actividad (user_list_items con created_at). */
export type UserListItemActivityRow = {
  list_id: string;
  coffee_id: string;
  created_at: number;
  list_name?: string;
  is_public?: boolean;
};

/** Devuelve ítems de listas del usuario con nombre y visibilidad (solo listas públicas en actividad de terceros). En 500/error devuelve []. */
export async function fetchAllListItemsForActivity(userId: number): Promise<UserListItemActivityRow[]> {
  try {
    const supabase = getSupabaseClient();
    const { data: listsData, error: listsError } = await supabase
      .from("user_lists")
      .select("id,name,is_public")
      .eq("user_id", userId);
    if (listsError || !listsData?.length) return [];
    const lists = listsData as { id: string; name: string; is_public: boolean }[];
    const listIds = lists.map((r) => r.id);
    const listMap = new Map(lists.map((l) => [l.id, { name: l.name, is_public: l.is_public }]));
    const { data, error } = await supabase
      .from("user_list_items")
      .select("list_id,coffee_id,created_at")
      .in("list_id", listIds)
      .order("created_at", { ascending: false });
    if (error) return [];
    const rows = (data ?? []) as { list_id: string; coffee_id: string; created_at: string }[];
    return rows.map((r) => {
      const meta = listMap.get(r.list_id);
      return {
        list_id: r.list_id,
        coffee_id: r.coffee_id,
        created_at: typeof r.created_at === "number" ? r.created_at : new Date(r.created_at).getTime(),
        list_name: meta?.name,
        is_public: meta?.is_public
      };
    });
  } catch {
    return [];
  }
}

/** Diario, favoritos e ítems de listas de un usuario (perfil visitado) para la pestaña Actividad. */
export async function fetchProfileUserActivityData(userId: number): Promise<{
  diaryEntries: DiaryEntryRow[];
  favorites: FavoriteRow[];
  listItems: UserListItemActivityRow[];
}> {
  const supabase = getSupabaseClient();
  const diaryReq = supabase
    .from("diary_entries")
    .select("id,user_id,coffee_id,coffee_name,caffeine_mg,amount_ml,coffee_grams,preparation_type,size_label,timestamp,type,pantry_item_id")
    .eq("user_id", userId)
    .order("timestamp", { ascending: false })
    .limit(200);

  const favoritesReq = supabase
    .from("local_favorites")
    .select("coffee_id,user_id,saved_at")
    .eq("user_id", userId)
    .limit(500);

  const [diaryRes, favoritesRes] = await Promise.all([diaryReq, favoritesReq]);
  if (diaryRes.error || favoritesRes.error) {
    return { diaryEntries: [], favorites: [], listItems: [] };
  }
  const diaryEntries = (diaryRes.data ?? []) as DiaryEntryRow[];
  const favorites = (favoritesRes.data ?? []) as FavoriteRow[];
  const listItems = await fetchAllListItemsForActivity(userId);
  return { diaryEntries, favorites, listItems };
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

/** Obtiene un usuario por ID (para resolver owner de lista, avatares en Gestionar invitados, etc.). */
export async function fetchUserById(userId: number): Promise<UserRow | null> {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase
    .from("users_db")
    .select("id,username,full_name,avatar_url,email,bio")
    .eq("id", userId)
    .maybeSingle();
  if (error || !data) return null;
  return mapUserRow(data);
}

/** Obtiene varios usuarios por ID en una sola petición (avatares en Gestionar invitados y TopBar). Requiere RLS que permita SELECT en users_db para usuarios autenticados (ver users_db_public_profiles_select.sql). */
export async function fetchUsersByIds(userIds: number[]): Promise<UserRow[]> {
  if (userIds.length === 0) return [];
  const supabase = getSupabaseClient();
  const uniq = [...new Set(userIds)];
  const { data, error } = await supabase
    .from("users_db")
    .select("id,username,full_name,avatar_url,email,bio")
    .in("id", uniq);
  if (error) return [];
  return ((data ?? []) as unknown[]).map(mapUserRow);
}

const USER_LISTS_SELECT_FULL = "id,user_id,name,is_public,privacy,members_can_edit,members_can_invite,created_at";
const USER_LISTS_SELECT_LEGACY = "id,user_id,name,is_public,privacy,members_can_edit,created_at";

function mapUserListRow(row: {
  id: string;
  user_id: number;
  name: string;
  is_public: boolean;
  privacy?: string;
  members_can_edit?: boolean;
  members_can_invite?: boolean;
  created_at?: string;
}): UserListRow {
  const pr = row.privacy === "public" || row.privacy === "invitation" || row.privacy === "private" ? row.privacy : (row.is_public ? "public" : "private");
  return {
    id: String(row.id),
    user_id: Number(row.user_id),
    name: String(row.name ?? ""),
    is_public: Boolean(row.is_public),
    privacy: pr,
    members_can_edit: row.members_can_edit,
    members_can_invite: row.members_can_invite,
    created_at: row.created_at
  };
}

// --- Caché listas (TTL 5 min); en error o TTL expirado se llama a Supabase (fuente de verdad) ---
const USER_LISTS_CACHE_TTL_MS = 5 * 60 * 1000;
const userListsCache = new Map<
  number,
  { owned: UserListRow[]; shared: UserListRow[]; ts: number }
>();

export function invalidateUserListsCache(userId?: number): void {
  if (userId !== undefined) userListsCache.delete(userId);
  else userListsCache.clear();
}

async function fetchUserListsRaw(userId: number): Promise<UserListRow[]> {
  const supabase = getSupabaseClient();
  let data: unknown;
  let error: Error | null = null;
  const { data: d1, error: e1 } = await supabase
    .from("user_lists")
    .select(USER_LISTS_SELECT_FULL)
    .eq("user_id", userId)
    .order("created_at", { ascending: true });
  if (e1) {
    data = null;
    error = e1;
  } else {
    data = d1;
  }
  if (error != null) {
    const { data: d2, error: e2 } = await supabase
      .from("user_lists")
      .select(USER_LISTS_SELECT_LEGACY)
      .eq("user_id", userId)
      .order("created_at", { ascending: true });
    if (e2) return [];
    data = d2;
  }
  return ((data ?? []) as Array<Parameters<typeof mapUserListRow>[0]>).map(mapUserListRow);
}

async function fetchSharedWithMeListsRaw(userId: number): Promise<UserListRow[]> {
  const supabase = getSupabaseClient();
  const { data: memberRows, error: membersError } = await supabase
    .from("user_list_members")
    .select("list_id")
    .eq("user_id", userId);
  if (membersError) throw membersError;
  const listIds = [...new Set(((memberRows ?? []) as { list_id: string }[]).map((r) => r.list_id))];
  if (listIds.length === 0) return [];
  let lists: unknown;
  const { data: data1, error: listsError1 } = await supabase
    .from("user_lists")
    .select(USER_LISTS_SELECT_FULL)
    .in("id", listIds);
  if (listsError1) {
    const { data: data2, error: listsError2 } = await supabase
      .from("user_lists")
      .select(USER_LISTS_SELECT_LEGACY)
      .in("id", listIds);
    if (listsError2) return [];
    lists = data2;
  } else {
    lists = data1;
  }
  return ((lists ?? []) as Array<Parameters<typeof mapUserListRow>[0]>).map(mapUserListRow);
}

async function fetchUserListsAndSharedCached(userId: number): Promise<{ owned: UserListRow[]; shared: UserListRow[] }> {
  const now = Date.now();
  const entry = userListsCache.get(userId);
  if (entry && now - entry.ts < USER_LISTS_CACHE_TTL_MS) {
    return { owned: entry.owned, shared: entry.shared };
  }
  try {
    const [owned, shared] = await Promise.all([fetchUserListsRaw(userId), fetchSharedWithMeListsRaw(userId)]);
    userListsCache.set(userId, { owned, shared, ts: Date.now() });
    return { owned, shared };
  } catch (e) {
    userListsCache.delete(userId);
    await new Promise((r) => setTimeout(r, 1500));
    const [owned, shared] = await Promise.all([fetchUserListsRaw(userId), fetchSharedWithMeListsRaw(userId)]);
    userListsCache.set(userId, { owned, shared, ts: Date.now() });
    return { owned, shared };
  }
}

/** Obtiene las listas propias del usuario (con caché TTL 5 min). En error se reintenta contra Supabase. */
export async function fetchUserLists(userId: number): Promise<UserListRow[]> {
  try {
    const { owned } = await fetchUserListsAndSharedCached(userId);
    return owned;
  } catch {
    return [];
  }
}

/** Obtiene las listas compartidas con el usuario (con caché TTL 5 min). En error se reintenta contra Supabase. */
export async function fetchSharedWithMeLists(userId: number): Promise<UserListRow[]> {
  try {
    const { shared } = await fetchUserListsAndSharedCached(userId);
    return shared;
  } catch {
    return [];
  }
}

/** Obtiene una lista por ID si el usuario actual tiene acceso (RLS: propia, pública o miembro). */
export async function fetchUserListById(listId: string): Promise<UserListRow | null> {
  const supabase = getSupabaseClient();
  const { data: data1, error: err1 } = await supabase
    .from("user_lists")
    .select(USER_LISTS_SELECT_FULL)
    .eq("id", listId)
    .maybeSingle();
  if (!err1 && data1) return mapUserListRow(data1 as Parameters<typeof mapUserListRow>[0]);
  const { data: data2, error: err2 } = await supabase
    .from("user_lists")
    .select(USER_LISTS_SELECT_LEGACY)
    .eq("id", listId)
    .maybeSingle();
  if (err2 || !data2) return null;
  return mapUserListRow(data2 as Parameters<typeof mapUserListRow>[0]);
}

/** Crea invitación a lista y dispara notificación (RPC). Devuelve el invitation_id. */
export async function createListInvitation(listId: string, inviteeId: number): Promise<string> {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase.rpc("create_list_invitation", {
    p_list_id: listId,
    p_invitee_id: inviteeId
  });
  throwIfError(error);
  if (data != null && typeof data === "object" && "create_list_invitation" in data) {
    return String((data as { create_list_invitation: unknown }).create_list_invitation ?? "").replace(/^"|"$/g, "");
  }
  return String(data ?? "").replace(/^"|"$/g, "");
}

/** Acepta una invitación a lista (RPC). */
export async function acceptListInvitation(invitationId: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.rpc("accept_list_invitation", { p_invitation_id: invitationId });
  throwIfError(error);
}

/** Rechaza una invitación a lista (RPC). */
export async function declineListInvitation(invitationId: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.rpc("decline_list_invitation", { p_invitation_id: invitationId });
  throwIfError(error);
}

/** Se une a una lista pública (RPC). */
export async function joinPublicList(listId: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.rpc("join_public_list", { p_list_id: listId });
  throwIfError(error);
}

/** Abandona una lista (el usuario actual deja de ser miembro). Requiere política RLS que permita DELETE donde user_id = get_my_internal_id(). */
export async function leaveList(listId: string, userId: number): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase
    .from("user_list_members")
    .delete()
    .eq("list_id", listId)
    .eq("user_id", userId);
  throwIfError(error);
}

/** Crea una lista personalizada (privacy: public | invitation | private) y devuelve la fila insertada. */
export async function createUserList(
  userId: number,
  name: string,
  privacy: ListPrivacy,
  membersCanEdit?: boolean,
  membersCanInvite?: boolean
): Promise<UserListRow> {
  try {
    const supabase = getSupabaseClient();
    const isPublic = privacy === "public";
    const insertPayload: Record<string, unknown> = { user_id: userId, name: name.trim(), is_public: isPublic, privacy };
    if (membersCanEdit !== undefined) insertPayload.members_can_edit = membersCanEdit;
    if (membersCanInvite !== undefined) insertPayload.members_can_invite = membersCanInvite;
    const { data, error } = await supabase
      .from("user_lists")
      .insert(insertPayload)
      .select("id,user_id,name,is_public,privacy,members_can_edit,members_can_invite,created_at")
      .single();
    if (error) {
      const fallbackPayload: Record<string, unknown> = { user_id: userId, name: name.trim(), is_public: isPublic };
      if (membersCanEdit !== undefined) fallbackPayload.members_can_edit = membersCanEdit;
      const { data: fallbackData, error: fallbackError } = await supabase
        .from("user_lists")
        .insert(fallbackPayload)
        .select("id,user_id,name,is_public,created_at")
        .single();
      throwIfError(fallbackError);
      const row = fallbackData as { id: string; user_id: number; name: string; is_public: boolean; created_at?: string };
      invalidateUserListsCache(userId);
      return {
        id: String(row.id),
        user_id: Number(row.user_id),
        name: String(row.name ?? ""),
        is_public: Boolean(row.is_public),
        privacy: row.is_public ? "public" : "private",
        created_at: row.created_at
      };
    }
    const row = data as { id: string; user_id: number; name: string; is_public: boolean; privacy?: string; members_can_edit?: boolean; members_can_invite?: boolean; created_at?: string };
    const pr: ListPrivacy = row.privacy === "public" || row.privacy === "invitation" || row.privacy === "private" ? row.privacy : (row.is_public ? "public" : "private");
    const result: UserListRow = {
      id: String(row.id),
      user_id: Number(row.user_id),
      name: String(row.name ?? ""),
      is_public: Boolean(row.is_public),
      privacy: pr,
      members_can_edit: row.members_can_edit,
      members_can_invite: row.members_can_invite,
      created_at: row.created_at
    };
    invalidateUserListsCache(userId);
    return result;
  } catch (err) {
    const msg = (err as { message?: string })?.message ?? "";
    if (msg.includes("500") || msg.includes("Internal Server Error") || msg.includes("permission") || msg.includes("policy")) {
      throw new Error("No se pudo crear la lista. Comprueba que la sesión es válida o inténtalo más tarde. Si persiste, puede ser un fallo de permisos en el servidor.");
    }
    throw err;
  }
}

/** Ítems de una lista (cafés en user_list_items). */
export type UserListItemRow = { coffee_id: string };

/** Devuelve los coffee_id que están en al menos una lista del usuario (propias + compartidas; para icono activo). */
export async function fetchCoffeeIdsInUserLists(userId: number): Promise<string[]> {
  const [owned, shared] = await Promise.all([fetchUserLists(userId), fetchSharedWithMeLists(userId)]);
  const listIds = [...new Set([...owned, ...shared].map((l) => l.id))];
  return fetchCoffeeIdsInLists(listIds);
}

/** Devuelve los coffee_id que están en las listas indicadas. */
export async function fetchCoffeeIdsInLists(listIds: string[]): Promise<string[]> {
  if (listIds.length === 0) return [];
  const supabase = getSupabaseClient();
  const { data: items, error } = await supabase
    .from("user_list_items")
    .select("coffee_id")
    .in("list_id", listIds);
  throwIfError(error);
  const ids = new Set<string>();
  ((items ?? []) as UserListItemRow[]).forEach((r) => ids.add(String(r.coffee_id)));
  return Array.from(ids);
}

/** Obtiene los coffee_id de una lista personalizada. */
export async function fetchUserListItems(listId: string): Promise<UserListItemRow[]> {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase
    .from("user_list_items")
    .select("coffee_id")
    .eq("list_id", listId)
    .order("created_at", { ascending: true });
  throwIfError(error);
  return ((data ?? []) as UserListItemRow[]).map((row) => ({ coffee_id: String(row.coffee_id) }));
}

/** Añade un café a una lista personalizada (idempotente: si ya está, no falla). */
export async function addUserListItem(listId: string, coffeeId: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase
    .from("user_list_items")
    .upsert(
      { list_id: listId, coffee_id: coffeeId },
      { onConflict: "list_id,coffee_id", ignoreDuplicates: true }
    );
  throwIfError(error);
  invalidateUserListsCache();
}

/** Quita un café de una lista personalizada. */
export async function removeUserListItem(listId: string, coffeeId: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase
    .from("user_list_items")
    .delete()
    .eq("list_id", listId)
    .eq("coffee_id", coffeeId);
  throwIfError(error);
  invalidateUserListsCache();
}

/** Actualiza nombre y/o visibilidad de una lista personalizada. */
export async function updateUserList(listId: string, name: string, isPublic: boolean): Promise<UserListRow> {
  return updateUserListWithPrivacy(listId, name, isPublic ? "public" : "private");
}

/** Actualiza nombre, privacidad y/o si los miembros pueden editar/invitar (privacy: public | invitation | private). */
export async function updateUserListWithPrivacy(
  listId: string,
  name: string,
  privacy: ListPrivacy,
  membersCanEdit?: boolean,
  membersCanInvite?: boolean
): Promise<UserListRow> {
  const supabase = getSupabaseClient();
  const isPublic = privacy === "public";
  const payload: Record<string, unknown> = { name: name.trim(), is_public: isPublic, privacy };
  if (membersCanEdit !== undefined) payload.members_can_edit = membersCanEdit;
  if (membersCanInvite !== undefined) payload.members_can_invite = membersCanInvite;
  const selectCols = "id,user_id,name,is_public,privacy,created_at";
  const selectWithEdit = "id,user_id,name,is_public,privacy,members_can_edit,members_can_invite,created_at";
  const { data, error } = await supabase
    .from("user_lists")
    .update(payload)
    .eq("id", listId)
    .select(selectWithEdit)
    .single();
  if (error) {
    const fallbackPayload: Record<string, unknown> = { name: name.trim(), is_public: isPublic, privacy };
    const { data: fallbackData, error: fallbackError } = await supabase
      .from("user_lists")
      .update(fallbackPayload)
      .eq("id", listId)
      .select(selectCols)
      .single();
    throwIfError(fallbackError);
    const row = fallbackData as { id: string; user_id: number; name: string; is_public: boolean; privacy?: string; created_at?: string };
    const pr = row.privacy === "public" || row.privacy === "invitation" || row.privacy === "private" ? row.privacy : (row.is_public ? "public" : "private");
    invalidateUserListsCache();
    return {
      id: String(row.id),
      user_id: Number(row.user_id),
      name: String(row.name ?? ""),
      is_public: Boolean(row.is_public),
      privacy: pr,
      created_at: row.created_at
    };
  }
  const row = data as { id: string; user_id: number; name: string; is_public: boolean; privacy?: string; members_can_edit?: boolean; members_can_invite?: boolean; created_at?: string };
  const pr = row.privacy === "public" || row.privacy === "invitation" || row.privacy === "private" ? row.privacy : (row.is_public ? "public" : "private");
  invalidateUserListsCache();
  return {
    id: String(row.id),
    user_id: Number(row.user_id),
    name: String(row.name ?? ""),
    is_public: Boolean(row.is_public),
    privacy: pr,
    members_can_edit: row.members_can_edit,
    members_can_invite: row.members_can_invite,
    created_at: row.created_at
  };
}

/** Invitación a una lista (para el dueño: listado de invitaciones enviadas). */
export type ListInvitationRow = {
  id: string;
  list_id: string;
  inviter_id: number;
  invitee_id: number;
  status: "pending" | "accepted" | "declined";
  created_at?: string;
};

/** Miembro de una lista (para el dueño: listado de quienes tienen acceso). */
export type ListMemberRow = {
  list_id: string;
  user_id: number;
  role: string;
  invited_by: number | null;
  created_at?: string;
};

/** Invitaciones enviadas por el dueño para esta lista (RLS: inviter_id = yo). */
export async function fetchListInvitationsByListId(listId: string): Promise<ListInvitationRow[]> {
  try {
    const supabase = getSupabaseClient();
    const { data, error } = await supabase
      .from("user_list_invitations")
      .select("id,list_id,inviter_id,invitee_id,status,created_at")
      .eq("list_id", listId);
    if (error) return [];
    return ((data ?? []) as ListInvitationRow[]).map((r) => ({
      id: String(r.id),
      list_id: String(r.list_id),
      inviter_id: Number(r.inviter_id),
      invitee_id: Number(r.invitee_id),
      status: r.status as "pending" | "accepted" | "declined",
      created_at: r.created_at
    }));
  } catch {
    return [];
  }
}

/** Miembros de la lista (para el dueño; RLS permite si es dueño). */
export async function fetchListMembersByListId(listId: string): Promise<ListMemberRow[]> {
  try {
    const supabase = getSupabaseClient();
    const { data, error } = await supabase
      .from("user_list_members")
      .select("list_id,user_id,role,invited_by,created_at")
      .eq("list_id", listId);
    if (error) return [];
    return ((data ?? []) as ListMemberRow[]).map((r) => ({
      list_id: String(r.list_id),
      user_id: Number(r.user_id),
      role: String(r.role),
      invited_by: r.invited_by != null ? Number(r.invited_by) : null,
      created_at: r.created_at
    }));
  } catch {
    return [];
  }
}

/** Elimina una lista personalizada (user_list_items en cascada). No modifica pantry_items: los cafés en despensa no se borran. */
export async function deleteUserList(listId: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("user_lists").delete().eq("id", listId);
  throwIfError(error);
  invalidateUserListsCache();
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

/** Inserta un nuevo registro en la despensa (el mismo café puede añadirse varias veces). */
export async function insertPantryItem(payload: {
  coffeeId: string;
  userId: number;
  totalGrams: number;
  gramsRemaining?: number;
}): Promise<PantryItemRow> {
  const supabase = getSupabaseClient();
  const now = Date.now();
  const row = {
    coffee_id: payload.coffeeId,
    user_id: payload.userId,
    total_grams: payload.totalGrams,
    grams_remaining: payload.gramsRemaining ?? payload.totalGrams,
    last_updated: now
  };
  const { data, error } = await supabase
    .from("pantry_items")
    .insert(row)
    .select("id,coffee_id,user_id,grams_remaining,total_grams,last_updated")
    .single();
  throwIfError(error);
  return mapPantryItemRow(data);
}

/** Actualiza un registro de despensa por id. */
export async function updatePantryItem(
  id: string,
  payload: { totalGrams: number; gramsRemaining: number }
): Promise<PantryItemRow> {
  const supabase = getSupabaseClient();
  const { data, error } = await supabase
    .from("pantry_items")
    .update({
      total_grams: payload.totalGrams,
      grams_remaining: payload.gramsRemaining,
      last_updated: Date.now()
    })
    .eq("id", id)
    .select("id,coffee_id,user_id,grams_remaining,total_grams,last_updated")
    .single();
  throwIfError(error);
  return mapPantryItemRow(data);
}

/** Elimina un registro de despensa por id. */
export async function deletePantryItemById(id: string): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("pantry_items").delete().eq("id", id);
  throwIfError(error);
}

/** Añade un café al historial (tabla pantry_historical). Fuente de verdad Supabase; coordinado con Android. */
export async function insertFinishedCoffee(
  userId: number,
  coffeeId: string,
  finishedAt: number
): Promise<void> {
  const supabase = getSupabaseClient();
  const { error } = await supabase.from("pantry_historical").insert({
    user_id: userId,
    coffee_id: coffeeId,
    finished_at: finishedAt
  });
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
  descripcion?: string | null;
  proceso?: string | null;
  codigo_barras?: string | null;
  molienda_recomendada?: string | null;
  product_url?: string | null;
  aroma?: number | null;
  sabor?: number | null;
  cuerpo?: number | null;
  acidez?: number | null;
  dulzura?: number | null;
}): Promise<CoffeeRow> {
  const supabase = getSupabaseClient();
  const id = crypto.randomUUID();
  const row: Record<string, unknown> = {
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
  if (payload.descripcion != null && payload.descripcion.trim() !== "") row.descripcion = payload.descripcion.trim();
  if (payload.proceso != null && payload.proceso.trim() !== "") row.proceso = payload.proceso.trim();
  if (payload.codigo_barras != null && payload.codigo_barras.trim() !== "") row.codigo_barras = payload.codigo_barras.trim();
  if (payload.molienda_recomendada != null && payload.molienda_recomendada.trim() !== "") row.molienda_recomendada = payload.molienda_recomendada.trim();
  if (payload.product_url != null && payload.product_url.trim() !== "") row.product_url = payload.product_url.trim();
  if (payload.aroma != null && Number.isFinite(payload.aroma)) row.aroma = payload.aroma;
  if (payload.sabor != null && Number.isFinite(payload.sabor)) row.sabor = payload.sabor;
  if (payload.cuerpo != null && Number.isFinite(payload.cuerpo)) row.cuerpo = payload.cuerpo;
  if (payload.acidez != null && Number.isFinite(payload.acidez)) row.acidez = payload.acidez;
  if (payload.dulzura != null && Number.isFinite(payload.dulzura)) row.dulzura = payload.dulzura;

  const { data, error } = await supabase
    .from("coffees")
    .upsert(row, { onConflict: "id" })
    .select("id,nombre,marca,pais_origen,codigo_barras,descripcion,proceso,variedad_tipo,molienda_recomendada,product_url,cafeina,aroma,sabor,cuerpo,acidez,dulzura,especialidad,tueste,formato,image_url")
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
  /** Si se resta de despensa, id del ítem concreto del que restar (varios ítems pueden ser del mismo café). */
  pantryItemId?: string | null;
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
  if (payload.pantryItemId !== undefined && payload.pantryItemId != null) row.pantry_item_id = payload.pantryItemId;
  const { data, error } = await supabase
    .from("diary_entries")
    .insert(row)
    .select("id,user_id,coffee_id,coffee_name,caffeine_mg,amount_ml,coffee_grams,preparation_type,size_label,timestamp,type,pantry_item_id")
    .single();
  throwIfError(error);
  return mapDiaryEntryRow(data);
}

/** Elimina la entrada del diario. Si tenía pantry_item_id, restaura el stock en ese ítem. Devuelve el ítem de despensa actualizado si se restauró. */
export async function deleteDiaryEntry(
  entryId: number,
  userId: number
): Promise<PantryItemRow | null> {
  const supabase = getSupabaseClient();
  let restored: PantryItemRow | null = null;
  const { data: entryRow, error: fetchErr } = await supabase
    .from("diary_entries")
    .select("id,pantry_item_id,coffee_grams,type")
    .eq("id", entryId)
    .eq("user_id", userId)
    .single();
  if (!fetchErr && entryRow?.pantry_item_id && (entryRow.coffee_grams ?? 0) > 0 && entryRow.type === "CUP") {
    const { data: pantryRow } = await supabase
      .from("pantry_items")
      .select("id,coffee_id,user_id,grams_remaining,total_grams,last_updated")
      .eq("id", entryRow.pantry_item_id)
      .single();
    if (pantryRow) {
      const newRemaining = Math.min(
        (pantryRow.grams_remaining ?? 0) + (entryRow.coffee_grams ?? 0),
        pantryRow.total_grams ?? 0
      );
      restored = await updatePantryItem(String(entryRow.pantry_item_id), {
        totalGrams: pantryRow.total_grams ?? 0,
        gramsRemaining: newRemaining
      });
    }
  }
  const { error } = await supabase.from("diary_entries").delete().eq("id", entryId).eq("user_id", userId);
  throwIfError(error);
  return restored;
}

export async function updateDiaryEntry(payload: {
  entryId: number;
  userId: number;
  caffeineMg: number;
  amountMl: number;
  coffeeGrams?: number;
  preparationType: string;
  sizeLabel?: string | null;
  timestampMs?: number;
}): Promise<DiaryEntryRow> {
  const supabase = getSupabaseClient();
  const row: Record<string, unknown> = {
    caffeine_mg: Math.max(0, Math.round(payload.caffeineMg)),
    amount_ml: Math.max(1, Math.round(payload.amountMl)),
    preparation_type: payload.preparationType.trim() || "None",
    ...(Number.isFinite(payload.timestampMs) ? { timestamp: Number(payload.timestampMs) } : {})
  };
  if (payload.coffeeGrams !== undefined) row.coffee_grams = Math.max(0, Math.round(payload.coffeeGrams));
  if (payload.sizeLabel !== undefined) row.size_label = payload.sizeLabel ?? null;
  const { data, error } = await supabase
    .from("diary_entries")
    .update(row)
    .eq("id", payload.entryId)
    .eq("user_id", payload.userId)
    .select("id,user_id,coffee_id,coffee_name,caffeine_mg,amount_ml,coffee_grams,preparation_type,size_label,timestamp,type,pantry_item_id")
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
    supabase.from("user_list_invitations").delete().or(`inviter_id.eq.${userId},invitee_id.eq.${userId}`),
    supabase.from("user_list_members").delete().or(`user_id.eq.${userId},invited_by.eq.${userId}`),
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



