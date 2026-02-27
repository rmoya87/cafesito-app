import { useMemo } from "react";
import { buildNormalizedOptions, normalizeLookupText, toEventTimestamp, toUiOptionValue } from "../../core/text";
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
  TimelineCard,
  UserRow
} from "../../types";
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
  commentDraft,
  commentSheetPostId,
  commentMenuId,
  dismissedNotificationIds,
  notificationsLastSeenAt,
  detailCoffeeId,
  notifications
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
  commentDraft: string;
  commentSheetPostId: string | null;
  commentMenuId: number | null;
  dismissedNotificationIds: Set<string>;
  notificationsLastSeenAt: number;
  detailCoffeeId: string | null;
  notifications: NotificationRow[];
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

  const createCoffeeCountryOptions = useMemo(() => {
    const fromCatalog = searchOriginOptions;
    try {
      if (typeof Intl !== "undefined" && typeof (Intl as { supportedValuesOf?: (key: string) => string[] }).supportedValuesOf === "function") {
        const codes = (Intl as { supportedValuesOf: (key: string) => string[] }).supportedValuesOf("region").filter((c) => c.length === 2);
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

  const selectedCoffeeForBrew = useMemo(
    () => brewCoffeeCatalog.find((coffee) => coffee.id === brewCoffeeId) ?? brewCoffeeCatalog[0] ?? null,
    [brewCoffeeCatalog, brewCoffeeId]
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

  const timelineNotifications = useMemo<TimelineNotificationItem[]>(() => {
    if (!activeUser) return [];

    return notifications.map((n) => {
      const type = n.type.toLowerCase();
      const isComment = type === "comment" || type === "mention";
      const isFollow = type === "follow";

      return {
        id: String(n.id),
        type: isFollow ? "follow" : "comment",
        userId: n.user_id,
        text: n.message,
        timestamp: n.timestamp,
        postId: isComment ? n.related_id : undefined,
        commentId: isComment ? Number(n.related_id) : undefined // Simplification
      } as TimelineNotificationItem;
    });
  }, [activeUser, notifications]);

  const visibleTimelineNotifications = useMemo(
    () => timelineNotifications.filter((item) => !dismissedNotificationIds.has(String(item.id))),
    [dismissedNotificationIds, timelineNotifications]
  );

  const showNotificationsBadge = useMemo(
    () => visibleTimelineNotifications.some((item) => item.timestamp > notificationsLastSeenAt),
    [notificationsLastSeenAt, visibleTimelineNotifications]
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
    followerCounts,
    filteredSearchUsers,
    timelineRecommendations,
    timelineSuggestions,
    timelineSuggestionIndices,
    commentSheetRows,
    activeCommentMenuRow,
    commentMentionSuggestions,
    visibleTimelineNotifications,
    showNotificationsBadge
  };
}
