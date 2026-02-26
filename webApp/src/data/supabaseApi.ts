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
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  UserDataBundle
} from "../types";
import {
  mapCommentRow,
  mapCustomCoffeeRow,
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

export async function fetchInitialData(): Promise<InitialDataBundle> {
  const supabase = getSupabaseClient();
  const usersReq = supabase.from("users_db").select("id,username,full_name,avatar_url,email,bio").limit(80);
  const coffeesReq = supabase
    .from("coffees")
    .select(
      "id,nombre,marca,pais_origen,descripcion,proceso,variedad_tipo,molienda_recomendada,product_url,cafeina,aroma,sabor,cuerpo,acidez,dulzura,especialidad,tueste,formato,image_url"
    )
    .order("nombre", { ascending: true })
    .limit(3000);
  const reviewsReq = supabase
    .from("reviews_db")
    .select("id,coffee_id,user_id,rating,comment,image_url,timestamp")
    .limit(6000);
  const sensoryReq = supabase
    .from("coffee_sensory_profiles")
    .select("coffee_id,user_id,aroma,sabor,cuerpo,acidez,dulzura,updated_at")
    .limit(6000);
  const postsReq = supabase
    .from("posts_db")
    .select("id,user_id,image_url,comment,timestamp")
    .order("timestamp", { ascending: false })
    .limit(120);
  const likesReq = supabase.from("likes_db").select("post_id,user_id").limit(3000);
  const commentsReq = supabase
    .from("comments_db")
    .select("id,post_id,user_id,text,timestamp")
    .order("timestamp", { ascending: false })
    .limit(3000);
  const tagsReq = supabase.from("post_coffee_tags").select("post_id,coffee_id,coffee_name,coffee_brand").limit(1000);
  const followsReq = supabase.from("follows").select("follower_id,followed_id,created_at").limit(3000);

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

  return mapInitialDataBundle({
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

  // custom_coffees is optional for web pantry merge. If this query fails
  // (RLS/schema/env differences), user pantry still must load.
  let customCoffees: CoffeeRow[] = [];
  try {
    const { data: customRows } = await supabase
      .from("custom_coffees")
      .select("id,user_id,name,brand,specialty,roast,variety,country,has_caffeine,format,image_url,total_grams")
      .eq("user_id", userId)
      .limit(500);

    customCoffees = (customRows ?? []).map(mapCustomCoffeeRow);
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
  return mapCommentRow(data);
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
  const payload = {
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
  return mapPostRow(data);
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
  const { error } = await supabase.from("local_favorites").upsert(payload, { onConflict: "user_id,coffee_id" });
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
    name: payload.name.trim(),
    brand: payload.brand.trim(),
    specialty: payload.specialty.trim(),
    roast: payload.roast?.trim() || null,
    variety: payload.variety?.trim() || null,
    country: payload.country.trim(),
    has_caffeine: payload.hasCaffeine,
    format: payload.format.trim(),
    image_url: payload.imageUrl ?? "",
    total_grams: Math.max(1, Number(payload.totalGrams ?? 250))
  };

  const { data, error } = await supabase
    .from("custom_coffees")
    .upsert(row, { onConflict: "id" })
    .select("id,name,brand,specialty,roast,variety,country,has_caffeine,format,image_url")
    .single();
  throwIfError(error);

  return mapCustomCoffeeRow(data);
}

export async function createDiaryEntry(payload: {
  userId: number;
  coffeeId: string | null;
  coffeeName: string;
  amountMl: number;
  caffeineMg: number;
  preparationType: string;
  type?: string;
}): Promise<DiaryEntryRow> {
  const supabase = getSupabaseClient();
  const row = {
    user_id: payload.userId,
    coffee_id: payload.coffeeId,
    coffee_name: payload.coffeeName,
    caffeine_mg: Math.max(0, Math.round(payload.caffeineMg)),
    amount_ml: Math.max(1, Math.round(payload.amountMl)),
    preparation_type: payload.preparationType,
    timestamp: Date.now(),
    type: payload.type ?? "CUP"
  };
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
}): Promise<DiaryEntryRow> {
  const supabase = getSupabaseClient();
  const row = {
    caffeine_mg: Math.max(0, Math.round(payload.caffeineMg)),
    amount_ml: Math.max(1, Math.round(payload.amountMl)),
    preparation_type: payload.preparationType.trim() || "None"
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


