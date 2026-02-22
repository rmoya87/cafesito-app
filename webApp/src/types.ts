export type TabId = "timeline" | "search" | "brewlab" | "diary" | "profile";
export type ViewMode = "mobile" | "desktop";
export type BrewStep = "method" | "coffee" | "config" | "brewing" | "result";

export type CoffeeRow = {
  id: string;
  nombre: string;
  marca: string;
  pais_origen: string | null;
  image_url: string;
};

export type UserRow = {
  id: number;
  username: string;
  full_name: string;
  avatar_url: string;
  email: string;
  bio: string | null;
};

export type PostRow = {
  id: string;
  user_id: number;
  image_url: string;
  comment: string;
  timestamp: number;
};

export type LikeRow = {
  post_id: string;
  user_id: number;
};

export type CommentRow = {
  id: number;
  post_id: string;
  user_id: number;
  text: string;
  timestamp: number;
};

export type PostCoffeeTagRow = {
  post_id: string;
  coffee_name: string;
  coffee_brand: string;
};

export type DiaryEntryRow = {
  id: number;
  user_id: number;
  coffee_id: string | null;
  coffee_name: string;
  caffeine_mg: number;
  amount_ml: number;
  preparation_type: string;
  timestamp: number;
  type: string;
};

export type PantryItemRow = {
  coffee_id: string;
  user_id: number;
  grams_remaining: number;
  total_grams: number;
  last_updated: number;
};

export type FavoriteRow = {
  coffee_id: string;
  user_id: number;
  saved_at: number;
};

export type FollowRow = {
  follower_id: number;
  followed_id: number;
  created_at: number;
};

export type TimelineCard = {
  id: string;
  userId: number;
  userName: string;
  username: string;
  avatarUrl: string;
  text: string;
  imageUrl: string;
  minsAgoLabel: string;
  likes: number;
  comments: number;
  coffeeId: string | null;
  coffeeTag: string | null;
  likedByActiveUser: boolean;
};

export type InitialDataBundle = {
  users: UserRow[];
  coffees: CoffeeRow[];
  posts: PostRow[];
  likes: LikeRow[];
  comments: CommentRow[];
  postCoffeeTags: PostCoffeeTagRow[];
  follows: FollowRow[];
};

export type UserDataBundle = {
  diaryEntries: DiaryEntryRow[];
  pantryItems: PantryItemRow[];
  favorites: FavoriteRow[];
};
