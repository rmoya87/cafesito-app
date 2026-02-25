export type TabId = "timeline" | "search" | "brewlab" | "diary" | "profile" | "coffee";
export type ViewMode = "mobile" | "desktop";
export type BrewStep = "method" | "coffee" | "config" | "brewing" | "result";

export type CoffeeRow = {
  id: string;
  nombre: string;
  marca: string | null;
  pais_origen: string | null;
  descripcion?: string | null;
  proceso?: string | null;
  variedad_tipo?: string | null;
  molienda_recomendada?: string | null;
  product_url?: string | null;
  cafeina?: string | null;
  aroma?: number | null;
  sabor?: number | null;
  cuerpo?: number | null;
  acidez?: number | null;
  dulzura?: number | null;
  especialidad?: string | null;
  tueste?: string | null;
  formato?: string | null;
  image_url: string;
};

export type CoffeeReviewRow = {
  id?: number;
  coffee_id: string;
  user_id?: number;
  rating: number;
  comment?: string | null;
  image_url?: string | null;
  timestamp?: number;
};

export type CoffeeSensoryProfileRow = {
  coffee_id: string;
  user_id: number;
  aroma: number;
  sabor: number;
  cuerpo: number;
  acidez: number;
  dulzura: number;
  updated_at: number;
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
  coffee_id?: string | null;
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
  coffeeTagName: string | null;
  coffeeTagBrand: string | null;
  coffeeImageUrl: string | null;
  likedByActiveUser: boolean;
};

export type InitialDataBundle = {
  users: UserRow[];
  coffees: CoffeeRow[];
  reviews: CoffeeReviewRow[];
  sensoryProfiles: CoffeeSensoryProfileRow[];
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
  customCoffees: CoffeeRow[];
};
