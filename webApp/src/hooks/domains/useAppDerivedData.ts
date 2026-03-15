import { useMemo } from "react";
import { getOrderedBrewMethods } from "../../config/brew";
import { buildNormalizedOptions, normalizeLookupText, splitAtomizedList, toEventTimestamp, toUiOptionValue } from "../../core/text";
import { toCoffeeSlug } from "../../core/routing";
import { toRelativeMinutes } from "../../core/time";
import type {
  CoffeeReviewRow,
  CoffeeRow,
  CoffeeSensoryProfileRow,
  CommentRow,
  DiaryEntryRow,
  FavoriteRow,
  FollowRow,
  LikeRow,
  NotificationRow,
  PantryItemRow,
  PostCoffeeTagRow,
  PostRow,
  ProfileActivityItem,
  TimelineCard,
  UserRow
} from "../../types";
import type { UserListItemActivityRow } from "../../data/supabaseApi";
import type { TimelineNotificationItem } from "../../features/timeline/NotificationRow";

export function useAppDerivedData({
  users,
  coffees,
  customCoffees,
  coffeeReviews,
  coffeeSensoryProfiles,
  posts,
  likes,
  comments,
  postCoffeeTags,
  diaryEntries,
  pantryItems,
  favorites,
  follows,
  activeUser,
  profileUsername,
  profileUserDiaryEntries = [],
  profileUserFavorites = [],
  allListItemsForActivity = [],
  profileUserListItems = [],
  followedUsersActivityData = [],
  searchQuery,
  searchSelectedOrigins,
  searchSelectedRoasts,
  searchSelectedSpecialties,
  searchSelectedFormats,
  searchMinRating,
  newPostCoffeeId,
  createPostCoffeeQuery,
  brewCoffeeId,
  newPostText,
  dismissedNotificationIds,
  notificationsLastSeenAt,
  detailCoffeeId,
  notifications,
  recommendationDateKey = new Date().toISOString().slice(0, 10)
}: {
  users: UserRow[];
  coffees: CoffeeRow[];
  customCoffees: CoffeeRow[];
  coffeeReviews: CoffeeReviewRow[];
  coffeeSensoryProfiles: CoffeeSensoryProfileRow[];
  posts: PostRow[];
  likes: LikeRow[];
  comments: CommentRow[];
  postCoffeeTags: PostCoffeeTagRow[];
  diaryEntries: DiaryEntryRow[];
  pantryItems: PantryItemRow[];
  favorites: FavoriteRow[];
  follows: FollowRow[];
  activeUser: UserRow | null;
  profileUsername: string | null;
  /** Diario del usuario del perfil cuando se visita un perfil ajeno (para pestaña Actividad). */
  profileUserDiaryEntries?: DiaryEntryRow[];
  /** Favoritos/listas del usuario del perfil cuando se visita un perfil ajeno (para pestaña Actividad). */
  profileUserFavorites?: FavoriteRow[];
  allListItemsForActivity?: UserListItemActivityRow[];
  profileUserListItems?: UserListItemActivityRow[];
  /** Actividad (diario primera vez, favoritos, listas) de usuarios que sigues, para fusionar en mi perfil. */
  followedUsersActivityData?: Array<{
    userId: number;
    diaryEntries: DiaryEntryRow[];
    favorites: FavoriteRow[];
    listItems: UserListItemActivityRow[];
  }>;
  searchQuery: string;
  searchSelectedOrigins: Set<string>;
  searchSelectedRoasts: Set<string>;
  searchSelectedSpecialties: Set<string>;
  searchSelectedFormats: Set<string>;
  searchMinRating: number;
  newPostCoffeeId: string | null;
  createPostCoffeeQuery: string;
  brewCoffeeId: string;
  newPostText: string;
  dismissedNotificationIds: Set<string>;
  notificationsLastSeenAt: number;
  detailCoffeeId: string | null;
  notifications: NotificationRow[];
  /** Clave de fecha (YYYY-MM-DD) para que las recomendaciones del día varíen cada día */
  recommendationDateKey?: string;
}) {
  const brewCoffeeCatalog = useMemo(() => {
    if (!customCoffees.length) return coffees;
    const byId = new Map<string, CoffeeRow>();
    coffees.forEach((coffee) => byId.set(String(coffee.id), coffee));
    customCoffees.forEach((coffee) => byId.set(String(coffee.id), coffee));
    return Array.from(byId.values());
  }, [coffees, customCoffees]);

  const usersById = useMemo(() => {
    const map = new Map<number, UserRow>();
    users.forEach((user) => map.set(user.id, user));
    return map;
  }, [users]);

  const usersByUsername = useMemo(() => {
    const map = new Map<string, UserRow>();
    users.forEach((user) => map.set(normalizeLookupText(user.username), user));
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
      if (bucket) bucket.push(comment);
      else map.set(comment.post_id, [comment]);
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
    const nameCounts = new Map<string, number>();
    const sorted = [...coffees].sort((a, b) => {
      const nameCmp = a.nombre.localeCompare(b.nombre);
      if (nameCmp !== 0) return nameCmp;
      return a.id.localeCompare(b.id);
    });
    sorted.forEach((coffee) => {
      const key = normalizeLookupText(coffee.nombre);
      nameCounts.set(key, (nameCounts.get(key) ?? 0) + 1);
    });
    sorted.forEach((coffee) => {
      const hasDuplicatedName = (nameCounts.get(normalizeLookupText(coffee.nombre)) ?? 0) > 1;
      const base = toCoffeeSlug(coffee.nombre, coffee.marca, hasDuplicatedName);
      const count = (counts.get(base) ?? 0) + 1;
      counts.set(base, count);
      const slug = count > 1 ? `${base}-${count}` : base;
      bySlug.set(slug, coffee.id);
      byId.set(coffee.id, slug);
    });
    return { bySlug, byId };
  }, [coffees]);

  const timelineCards: TimelineCard[] = useMemo(() => {
    const postCards: TimelineCard[] = posts.map((post) => {
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
        minsAgoLabel: toRelativeMinutes(post.timestamp ?? 0),
        timestamp: post.timestamp ?? 0,
        likes: postLikes,
        comments: postComments,
        coffeeId,
        coffeeTagName: tag?.coffee_name ?? null,
        coffeeTagBrand: tag?.coffee_brand ?? null,
        coffeeImageUrl: coffee?.image_url ?? null,
        likedByActiveUser
      };
    });

    const combined = [...postCards];

    // Map reviews as cards too
    coffeeReviews.forEach((review) => {
      const user = usersById.get(review.user_id ?? -1);
      const coffee = coffeesById.get(review.coffee_id);
      if (!coffee) return;

      const reviewId = `review-${review.id || review.timestamp}`;
      // Note: Reviews don't have likes/comments in the same way in the current DB schema
      // but we can show the rating.
      combined.push({
        id: reviewId,
        userId: review.user_id ?? -1,
        userName: user?.full_name ?? `Usuario ${review.user_id}`,
        username: user?.username ?? `user${review.user_id}`,
        avatarUrl: user?.avatar_url ?? "",
        text: review.comment || "",
        imageUrl: review.image_url || "",
        minsAgoLabel: toRelativeMinutes(review.timestamp ?? 0),
        timestamp: review.timestamp ?? 0,
        likes: 0,
        comments: 0,
        coffeeId: review.coffee_id,
        coffeeTagName: coffee.nombre,
        coffeeTagBrand: coffee.marca,
        coffeeImageUrl: coffee.image_url,
        likedByActiveUser: false,
        rating: review.rating // We might need to add this to TimelineCard type
      });
    });

    return combined.sort((a, b) => (b.timestamp ?? 0) - (a.timestamp ?? 0));
  }, [
    activeUser,
    coffeeIdByNameBrand,
    coffeesById,
    commentsByPostId,
    likes,
    likesByPostId,
    posts,
    tagsByPostId,
    usersById,
    coffeeReviews
  ]);

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
      const byBarcode =
        coffee.codigo_barras != null &&
        coffee.codigo_barras !== "" &&
        normalizeLookupText(coffee.codigo_barras) === normalizeLookupText(q);
      const queryMatch = !q || byName || byBrand || byOrigin || byBarcode;
      if (!queryMatch) return false;

      const origins = splitAtomizedList(coffee.pais_origen).map((item) => toUiOptionValue(item));
      const originMatch = !searchSelectedOrigins.size || origins.some((item) => searchSelectedOrigins.has(item));
      if (!originMatch) return false;

      const roasts = splitAtomizedList(coffee.tueste).map((item) => toUiOptionValue(item));
      const roastMatch = !searchSelectedRoasts.size || roasts.some((item) => searchSelectedRoasts.has(item));
      if (!roastMatch) return false;

      const specialties = splitAtomizedList(coffee.especialidad).map((item) => toUiOptionValue(item));
      const specialtyMatch =
        !searchSelectedSpecialties.size || specialties.some((item) => searchSelectedSpecialties.has(item));
      if (!specialtyMatch) return false;

      const formats = splitAtomizedList(coffee.formato).map((item) => toUiOptionValue(item));
      const formatMatch = !searchSelectedFormats.size || formats.some((item) => searchSelectedFormats.has(item));
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

  const createCoffeeCountryOptions = useMemo(() => {
    const fromCatalog = searchOriginOptions;
    try {
      const IntlWithRegion = Intl as unknown as { supportedValuesOf?: (key: string) => string[] };
      if (typeof Intl !== "undefined" && typeof IntlWithRegion.supportedValuesOf === "function") {
        const codes = IntlWithRegion.supportedValuesOf("region").filter((c) => c.length === 2);
        const displayNames = new Intl.DisplayNames(["es"], { type: "region" });
        const names = new Set<string>(fromCatalog);
        codes.forEach((code) => {
          try {
            const name = displayNames.of(code);
            if (name) names.add(name);
          } catch {
            // ignore invalid code
          }
        });
        return Array.from(names).sort((a, b) => a.localeCompare(b, "es"));
      }
    } catch {
      // fallback
    }
    return fromCatalog;
  }, [searchOriginOptions]);

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

  const selectedCoffeeForBrew = useMemo(() => {
    if (!brewCoffeeId || brewCoffeeId.trim() === "") return null;
    return brewCoffeeCatalog.find((coffee) => coffee.id === brewCoffeeId) ?? null;
  }, [brewCoffeeCatalog, brewCoffeeId]);

  /** Ítems de despensa para home (TU DESPENSA): ordenados por último uso (last_updated desc) para mostrar a la izquierda el más recientemente utilizado. */
  const brewPantryItems = useMemo(() => {
    const coffeeById = new Map<string, CoffeeRow>();
    brewCoffeeCatalog.forEach((coffee) => {
      coffeeById.set(String(coffee.id), coffee);
    });

    return pantryItems
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
  const orderedBrewMethods = useMemo(() => getOrderedBrewMethods(diaryEntries), [diaryEntries]);
  const pantryCoffeeRows = pantryItems
    .map((item) => ({ item, coffee: brewCoffeeCatalog.find((coffee) => coffee.id === item.coffee_id) }))
    .filter((row): row is { item: PantryItemRow; coffee: CoffeeRow } => row.coffee != null);

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

  const profileUser = useMemo(() => {
    if (profileUsername) {
      const routeUsername = normalizeLookupText(profileUsername);
      const fromRoute = users.find((user) => normalizeLookupText(user.username) === routeUsername);
      if (fromRoute) return fromRoute;
    }
    return usersById.get(activeUser?.id ?? -1) ?? null;
  }, [activeUser?.id, profileUsername, users, usersById]);

  /** Usuarios que siguen al perfil visitado (seguidores). */
  const profileFollowersUsers = useMemo((): UserRow[] => {
    if (!profileUser) return [];
    const ids = follows.filter((f) => f.followed_id === profileUser.id).map((f) => f.follower_id);
    return ids.map((id) => usersById.get(id)).filter((u): u is UserRow => u != null);
  }, [profileUser, follows, usersById]);

  /** Usuarios a los que sigue el perfil visitado (siguiendo). */
  const profileFollowingUsers = useMemo((): UserRow[] => {
    if (!profileUser) return [];
    const ids = follows.filter((f) => f.follower_id === profileUser.id).map((f) => f.followed_id);
    return ids.map((id) => usersById.get(id)).filter((u): u is UserRow => u != null);
  }, [profileUser, follows, usersById]);

  const profilePosts = useMemo(
    () => timelineCards.filter((card) => card.userId === (profileUser?.id ?? -1)),
    [profileUser?.id, timelineCards]
  );

  const favoriteCoffees = useMemo(
    () => favorites.map((favorite) => coffees.find((coffee) => coffee.id === favorite.coffee_id)).filter((coffee): coffee is CoffeeRow => Boolean(coffee)),
    [coffees, favorites]
  );

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

  const followersCount = profileUser ? follows.filter((follow) => follow.followed_id === profileUser.id).length : 0;
  const followingCount = profileUser ? follows.filter((follow) => follow.follower_id === profileUser.id).length : 0;

  const followingIds = useMemo(() => {
    if (!activeUser) return new Set<number>();
    return new Set(follows.filter((follow) => follow.follower_id === activeUser.id).map((follow) => follow.followed_id));
  }, [activeUser, follows]);

  /** Actividad de usuarios que sigues: opiniones (con valoración y comentario), primera vez café, listas públicas. */
  const profileFollowedActivity = useMemo((): ProfileActivityItem[] => {
    if (!followingIds.size) return [];
    const list: ProfileActivityItem[] = [];
    coffeeReviews.forEach((review) => {
      const uid = review.user_id ?? -1;
      if (!followingIds.has(uid)) return;
      const u = usersById.get(uid);
      const coffee = coffeesById.get(review.coffee_id);
      list.push({
        id: `review-${review.id ?? review.timestamp}-${uid}`,
        type: "review",
        userId: uid,
        userName: u?.full_name ?? `Usuario ${uid}`,
        username: u?.username ?? `user${uid}`,
        avatarUrl: u?.avatar_url ?? "",
        timestamp: review.timestamp ?? 0,
        label: "opinó sobre un café",
        coffeeId: review.coffee_id,
        coffeeName: coffee?.nombre ?? null,
        rating: review.rating,
        comment: review.comment ?? null
      });
    });
    return list.sort((a, b) => b.timestamp - a.timestamp);
  }, [coffeeReviews, followingIds, usersById, coffeesById]);

  /** Primera vez por café solo si ese café tiene una sola entrada; si lo ha tomado más veces no se muestra en actividad. Excluye agua. */
  const firstTimeCoffeeFromDiary = useMemo(() => {
    return (entries: DiaryEntryRow[]): DiaryEntryRow[] => {
      const countByCoffee = new Map<string, number>();
      const byCoffee = new Map<string, DiaryEntryRow>();
      for (const entry of entries) {
        if ((entry.type ?? "").toUpperCase() === "WATER") continue;
        const cid = entry.coffee_id ?? "";
        if (!cid) continue;
        countByCoffee.set(cid, (countByCoffee.get(cid) ?? 0) + 1);
        const existing = byCoffee.get(cid);
        if (!existing || (entry.timestamp ?? 0) < (existing.timestamp ?? 0)) {
          byCoffee.set(cid, entry);
        }
      }
      return Array.from(byCoffee.values()).filter((entry) => (countByCoffee.get(entry.coffee_id ?? "") ?? 0) === 1);
    };
  }, []);

  /** Actividad del usuario actual: reviews, favoritos, listas, y solo "probó por primera vez" por café (no cada consumo). */
  const profileOwnActivity = useMemo((): ProfileActivityItem[] => {
    if (!activeUser) return [];
    const u = usersById.get(activeUser.id);
    const list: ProfileActivityItem[] = [];
    coffeeReviews.forEach((review) => {
      if ((review.user_id ?? -1) !== activeUser.id) return;
      const coffee = coffeesById.get(review.coffee_id);
      list.push({
        id: `review-${review.id ?? review.timestamp}-${activeUser.id}`,
        type: "review",
        userId: activeUser.id,
        userName: u?.full_name ?? activeUser.full_name,
        username: u?.username ?? activeUser.username,
        avatarUrl: u?.avatar_url ?? activeUser.avatar_url ?? "",
        timestamp: review.timestamp ?? 0,
        label: "opinaste sobre un café",
        coffeeId: review.coffee_id,
        coffeeName: coffee?.nombre ?? null,
        rating: review.rating,
        comment: review.comment ?? null
      });
    });
    favorites.forEach((fav) => {
      if (fav.user_id !== activeUser.id) return;
      const coffee = coffeesById.get(fav.coffee_id);
      list.push({
        id: `favorite-${fav.coffee_id}-${fav.saved_at}`,
        type: "favorite",
        userId: activeUser.id,
        userName: u?.full_name ?? activeUser.full_name,
        username: u?.username ?? activeUser.username,
        avatarUrl: u?.avatar_url ?? activeUser.avatar_url ?? "",
        timestamp: fav.saved_at,
        label: "añadió a su lista",
        coffeeId: fav.coffee_id,
        coffeeName: coffee?.nombre ?? null,
        listId: "favorites",
        listName: "Favoritos"
      });
    });
    firstTimeCoffeeFromDiary(diaryEntries).forEach((entry) => {
      const coffee = coffeesById.get(entry.coffee_id ?? "");
      list.push({
        id: `diary-first-${entry.id}`,
        type: "diary",
        userId: activeUser.id,
        userName: u?.full_name ?? activeUser.full_name,
        username: u?.username ?? activeUser.username,
        avatarUrl: u?.avatar_url ?? activeUser.avatar_url ?? "",
        timestamp: entry.timestamp ?? 0,
        label: "probó por primera vez",
        coffeeId: entry.coffee_id ?? undefined,
        coffeeName: entry.coffee_name ?? coffee?.nombre ?? null
      });
    });
    allListItemsForActivity.forEach((item) => {
      const coffee = coffeesById.get(item.coffee_id);
      list.push({
        id: `list-${item.list_id}-${item.coffee_id}-${item.created_at}`,
        type: "favorite",
        userId: activeUser.id,
        userName: u?.full_name ?? activeUser.full_name,
        username: u?.username ?? activeUser.username,
        avatarUrl: u?.avatar_url ?? activeUser.avatar_url ?? "",
        timestamp: item.created_at,
        label: "añadió a su lista",
        coffeeId: item.coffee_id,
        coffeeName: coffee?.nombre ?? null,
        listId: item.list_id,
        listName: item.list_name ?? null
      });
    });
    return list.sort((a, b) => b.timestamp - a.timestamp);
  }, [activeUser, coffeeReviews, favorites, diaryEntries, firstTimeCoffeeFromDiary, allListItemsForActivity, usersById, coffeesById]);

  /** Perfil de terceros: solo la actividad de ese usuario (opiniones, primera vez café, listas públicas). No se incluye la de personas que ese usuario sigue. */
  const profileUserActivity = useMemo((): ProfileActivityItem[] => {
    if (!profileUser || profileUser.id === activeUser?.id) return [];
    const u = usersById.get(profileUser.id);
    const list: ProfileActivityItem[] = [];
    coffeeReviews.forEach((review) => {
      if ((review.user_id ?? -1) !== profileUser.id) return;
      const coffee = coffeesById.get(review.coffee_id);
      list.push({
        id: `review-${review.id ?? review.timestamp}-${profileUser.id}`,
        type: "review",
        userId: profileUser.id,
        userName: u?.full_name ?? profileUser.full_name,
        username: u?.username ?? profileUser.username,
        avatarUrl: u?.avatar_url ?? profileUser.avatar_url ?? "",
        timestamp: review.timestamp ?? 0,
        label: "opinó sobre un café",
        coffeeId: review.coffee_id,
        coffeeName: coffee?.nombre ?? null,
        rating: review.rating,
        comment: review.comment ?? null
      });
    });
    firstTimeCoffeeFromDiary(profileUserDiaryEntries).forEach((entry) => {
      const coffee = coffeesById.get(entry.coffee_id ?? "");
      list.push({
        id: `diary-first-${entry.id}`,
        type: "diary",
        userId: profileUser.id,
        userName: u?.full_name ?? profileUser.full_name,
        username: u?.username ?? profileUser.username,
        avatarUrl: u?.avatar_url ?? profileUser.avatar_url ?? "",
        timestamp: entry.timestamp ?? 0,
        label: "probó por primera vez",
        coffeeId: entry.coffee_id ?? undefined,
        coffeeName: entry.coffee_name ?? coffee?.nombre ?? null
      });
    });
    profileUserFavorites.forEach((fav) => {
      const coffee = coffeesById.get(fav.coffee_id);
      list.push({
        id: `favorite-${fav.coffee_id}-${fav.saved_at}`,
        type: "favorite",
        userId: profileUser.id,
        userName: u?.full_name ?? profileUser.full_name,
        username: u?.username ?? profileUser.username,
        avatarUrl: u?.avatar_url ?? profileUser.avatar_url ?? "",
        timestamp: fav.saved_at,
        label: "añadió a su lista",
        coffeeId: fav.coffee_id,
        coffeeName: coffee?.nombre ?? null,
        listId: "favorites",
        listName: "Favoritos"
      });
    });
    profileUserListItems.forEach((item) => {
      if (!(item as { is_public?: boolean }).is_public) return;
      const coffee = coffeesById.get(item.coffee_id);
      list.push({
        id: `list-${item.list_id}-${item.coffee_id}-${item.created_at}`,
        type: "favorite",
        userId: profileUser.id,
        userName: u?.full_name ?? profileUser.full_name,
        username: u?.username ?? profileUser.username,
        avatarUrl: u?.avatar_url ?? profileUser.avatar_url ?? "",
        timestamp: item.created_at,
        label: "añadió a su lista",
        coffeeId: item.coffee_id,
        coffeeName: coffee?.nombre ?? null,
        listId: item.list_id,
        listName: item.list_name ?? null
      });
    });
    return list.sort((a, b) => b.timestamp - a.timestamp);
  }, [profileUser, activeUser?.id, coffeeReviews, profileUserDiaryEntries, profileUserFavorites, profileUserListItems, firstTimeCoffeeFromDiary, usersById, coffeesById]);

  /** Actividad construida desde followedUsersActivityData (primera vez café, favoritos, listas) por cada usuario que sigues. */
  const followedUsersExtraActivity = useMemo((): ProfileActivityItem[] => {
    const list: ProfileActivityItem[] = [];
    for (const { userId, diaryEntries: de, favorites: fav, listItems: li } of followedUsersActivityData) {
      const u = usersById.get(userId);
      if (!u) continue;
      firstTimeCoffeeFromDiary(de).forEach((entry) => {
        const coffee = coffeesById.get(entry.coffee_id ?? "");
        list.push({
          id: `followed-diary-first-${userId}-${entry.id}`,
          type: "diary",
          userId,
          userName: u.full_name,
          username: u.username ?? "",
          avatarUrl: u.avatar_url ?? "",
          timestamp: entry.timestamp ?? 0,
          label: "probó por primera vez",
          coffeeId: entry.coffee_id ?? undefined,
          coffeeName: entry.coffee_name ?? coffee?.nombre ?? null
        });
      });
      fav.forEach((f) => {
        const coffee = coffeesById.get(f.coffee_id);
        list.push({
          id: `followed-fav-${userId}-${f.coffee_id}-${f.saved_at}`,
          type: "favorite",
          userId,
          userName: u.full_name,
          username: u.username ?? "",
          avatarUrl: u.avatar_url ?? "",
          timestamp: f.saved_at,
          label: "añadió a su lista",
          coffeeId: f.coffee_id,
          coffeeName: coffee?.nombre ?? null,
          listId: "favorites",
          listName: "Favoritos"
        });
      });
      li.forEach((item) => {
        if (!item.is_public) return;
        const coffee = coffeesById.get(item.coffee_id);
        list.push({
          id: `followed-list-${userId}-${item.list_id}-${item.coffee_id}-${item.created_at}`,
          type: "favorite",
          userId,
          userName: u.full_name,
          username: u.username ?? "",
          avatarUrl: u.avatar_url ?? "",
          timestamp: item.created_at,
          label: "añadió a su lista",
          coffeeId: item.coffee_id,
          coffeeName: coffee?.nombre ?? null,
          listId: item.list_id,
          listName: item.list_name ?? null
        });
      });
    }
    return list.sort((a, b) => b.timestamp - a.timestamp);
  }, [followedUsersActivityData, firstTimeCoffeeFromDiary, usersById, coffeesById]);

  /** Mi perfil: mis actividades (opiniones, primera vez, listas) + actividades de cada usuario que sigo (solo listas públicas). */
  const profileMineAndFollowedActivity = useMemo((): ProfileActivityItem[] => {
    const merged = [...profileOwnActivity, ...profileFollowedActivity, ...followedUsersExtraActivity];
    return merged.sort((a, b) => b.timestamp - a.timestamp);
  }, [profileOwnActivity, profileFollowedActivity, followedUsersExtraActivity]);

  const followerCounts = useMemo(() => {
    const map = new Map<number, number>();
    follows.forEach((follow) => {
      map.set(follow.followed_id, (map.get(follow.followed_id) ?? 0) + 1);
    });
    return map;
  }, [follows]);

  /** Por usuario: cuántos sigue (following count). */
  const followingCounts = useMemo(() => {
    const map = new Map<number, number>();
    follows.forEach((follow) => {
      map.set(follow.follower_id, (map.get(follow.follower_id) ?? 0) + 1);
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

  const timelineRecommendations = useMemo(() => {
    if (!coffees.length) return [] as CoffeeRow[];
    if (!activeUser) return coffees.slice(0, 9);

    // Referencia: favoritos, despensa y visitados (ej. café actual en detalle)
    const referenceIds = new Set<string>();
    favorites
      .filter((f) => f.user_id === activeUser.id)
      .forEach((f) => referenceIds.add(f.coffee_id));
    pantryItems
      .filter((p) => p.user_id === activeUser.id)
      .forEach((p) => referenceIds.add(p.coffee_id));
    if (detailCoffeeId) referenceIds.add(detailCoffeeId);

    const referenceCoffees = coffees.filter((c) => referenceIds.has(c.id));
    const preferenceTags = new Set<string>();
    referenceCoffees.forEach((coffee) => {
      splitAtomizedList(coffee.pais_origen).forEach((t) => preferenceTags.add(t.trim().toLowerCase()));
      splitAtomizedList(coffee.tueste).forEach((t) => preferenceTags.add(t.trim().toLowerCase()));
      splitAtomizedList(coffee.especialidad).forEach((t) => preferenceTags.add(t.trim().toLowerCase()));
      splitAtomizedList(coffee.formato).forEach((t) => preferenceTags.add(t.trim().toLowerCase()));
      if (coffee.proceso?.trim()) preferenceTags.add(coffee.proceso.trim().toLowerCase());
    });

    const candidates = coffees.filter((c) => !referenceIds.has(c.id));
    const similar = preferenceTags.size === 0
      ? candidates
      : candidates.filter((coffee) => {
          const tags = [
            ...splitAtomizedList(coffee.pais_origen),
            ...splitAtomizedList(coffee.tueste),
            ...splitAtomizedList(coffee.especialidad),
            ...splitAtomizedList(coffee.formato),
            ...(coffee.proceso?.trim() ? [coffee.proceso.trim()] : [])
          ].map((t) => t.trim().toLowerCase());
          return tags.some((t) => t && preferenceTags.has(t));
        });

    const dateSeed = recommendationDateKey.split("").reduce((acc, c) => acc + c.charCodeAt(0), 0);
    const seed = (activeUser.id * 31 + coffees.length + dateSeed * 7) % 1_000_000;
    const shuffled = [...similar].sort((a, b) => {
      const ha = (a.id.split("").reduce((acc, c) => acc + c.charCodeAt(0), 0) + seed) % 1000;
      const hb = (b.id.split("").reduce((acc, c) => acc + c.charCodeAt(0), 0) + seed) % 1000;
      return ha - hb;
    });
    const normalizeMarca = (m: string | null) => (m?.trim().toLowerCase() ?? "") || "\u0000";
    const seenMarcas = new Set<string>();
    const result: CoffeeRow[] = [];
    for (const coffee of shuffled) {
      if (result.length >= 9) break;
      const marcaKey = normalizeMarca(coffee.marca);
      if (seenMarcas.has(marcaKey)) continue;
      seenMarcas.add(marcaKey);
      result.push(coffee);
    }
    return result;
  }, [activeUser, coffees, detailCoffeeId, favorites, pantryItems, recommendationDateKey]);

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

  const timelineNotifications = useMemo<TimelineNotificationItem[]>(() => {
    if (!activeUser) return [];

    const normalizeNotificationText = (text: string) => {
      const trimmed = text.trim().toLowerCase();
      if (trimmed === "ha respondido en una publicación donde participas") {
        return "respondió a una publicación";
      }
      return text;
    };

    const parseNotificationTarget = (relatedId: string | null): { postId?: string; commentId?: number } => {
      if (!relatedId) return {};
      const delimiter = relatedId.includes(":")
        ? ":"
        : relatedId.includes("|")
          ? "|"
          : relatedId.includes(";")
            ? ";"
            : null;
      if (!delimiter) return { postId: relatedId };
      const parts = relatedId.split(delimiter);
      if (parts.length !== 2) return { postId: relatedId };
      const commentId = Number(parts[1]);
      return {
        postId: parts[0],
        commentId: Number.isFinite(commentId) ? commentId : undefined
      };
    };

    return notifications.map((n) => {
      const type = n.type.toLowerCase();
      const isComment = type === "comment" || type === "mention";
      const isFollow = type === "follow";
      const isListInvite = type === "list_invite";
      const sender = usersByUsername.get(normalizeLookupText(n.from_username)) ?? usersById.get(n.user_id);
      const target = isComment ? parseNotificationTarget(n.related_id) : {};

      if (isListInvite) {
        return {
          id: String(n.id),
          type: "list_invite" as const,
          userId: sender?.id ?? n.user_id,
          text: normalizeNotificationText(n.message),
          timestamp: n.timestamp,
          invitationId: n.related_id?.trim() ?? undefined,
          is_read: n.is_read
        } as TimelineNotificationItem;
      }

      return {
        id: String(n.id),
        type: isFollow ? "follow" : "comment",
        userId: sender?.id ?? n.user_id,
        text: normalizeNotificationText(n.message),
        timestamp: n.timestamp,
        postId: target.postId,
        commentId: target.commentId,
        is_read: n.is_read
      } as TimelineNotificationItem;
    });
  }, [activeUser, notifications, usersById, usersByUsername]);

  const visibleTimelineNotifications = useMemo(
    () => timelineNotifications.filter((item) => !dismissedNotificationIds.has(String(item.id))),
    [dismissedNotificationIds, timelineNotifications]
  );

  const showNotificationsBadge = useMemo(
    () => visibleTimelineNotifications.some((item) => item.is_read === false),
    [visibleTimelineNotifications]
  );

  return {
    brewCoffeeCatalog,
    usersById,
    coffeesById,
    coffeeSlugIndex,
    timelineCards,
    filteredCoffees,
    searchOriginOptions,
    createCoffeeCountryOptions,
    searchRoastOptions,
    searchSpecialtyOptions,
    searchFormatOptions,
    selectedCreatePostCoffee,
    filteredCreatePostCoffees,
    createPostMentionSuggestions,
    selectedCoffeeForBrew,
    brewPantryItems,
    diaryEntriesActivity,
    orderedBrewMethods,
    pantryCoffeeRows,
    diaryCoffeeOptions,
    profileUser,
    profilePosts,
    favoriteCoffees,
    detailCoffee,
    detailCoffeeReviews,
    detailCoffeeAverageRating,
    detailCurrentUserReview,
    detailCurrentUser,
    detailCurrentUserReviewWithUser,
    detailIsFavorite,
    detailPantryStock,
    detailSensoryAverages,
    followersCount,
    followingCount,
    followingIds,
    profileFollowersUsers,
    profileFollowingUsers,
    profileFollowedActivity,
    profileOwnActivity,
    profileUserActivity,
    profileMineAndFollowedActivity,
    followerCounts,
    followingCounts,
    filteredSearchUsers,
    timelineRecommendations,
    timelineSuggestions,
    timelineSuggestionIndices,
    visibleTimelineNotifications,
    showNotificationsBadge
  };
}
