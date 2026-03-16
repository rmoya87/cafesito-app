export type TabId = "home" | "search" | "brewlab" | "diary" | "profile" | "coffee" | "crear-cafe" | "selecciona-cafe";
export type ViewMode = "mobile" | "desktop";
export type BrewStep = "method" | "coffee" | "config" | "brewing" | "result";

export type CoffeeRow = {
  id: string;
  nombre: string;
  marca: string | null;
  pais_origen: string | null;
  codigo_barras?: string | null;
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
  coffee_grams?: number;
  preparation_type: string;
  size_label?: string | null;
  timestamp: number;
  type: string;
  /** ID del ítem de despensa del que se restó stock; para restaurar al eliminar. */
  pantry_item_id?: string | null;
};

export type PantryItemRow = {
  id: string;
  coffee_id: string;
  user_id: number;
  grams_remaining: number;
  total_grams: number;
  last_updated: number;
};

/** Café marcado como terminado desde la despensa; fuente de verdad: Supabase tabla pantry_historical. */
export type FinishedCoffeeRow = {
  coffee_id: string;
  finished_at: number;
};

export type FavoriteRow = {
  coffee_id: string;
  user_id: number;
  saved_at: number;
};

/** Modo de privacidad de una lista: pública (cualquiera puede suscribirse), por invitación, o privada (solo dueño). */
export type ListPrivacy = "public" | "invitation" | "private";

/** Lista personalizada del usuario (tabla user_lists en Supabase). */
export type UserListRow = {
  id: string;
  user_id: number;
  name: string;
  is_public: boolean;
  /** Si existe en BD (columna privacy); si no, se deriva: is_public true → "public", false → "private". */
  privacy?: ListPrivacy;
  /** Si los miembros pueden añadir/quitar cafés (solo aplica cuando privacy es public o invitation). Por defecto false. */
  members_can_edit?: boolean;
  /** Si los miembros pueden invitar a otras personas (solo aplica cuando privacy es invitation). Por defecto false. */
  members_can_invite?: boolean;
  created_at?: string;
};

export type FollowRow = {
  follower_id: number;
  followed_id: number;
  created_at: number;
};

export type NotificationRow = {
  id: number;
  user_id: number;
  type: string;
  from_username: string;
  message: string;
  timestamp: number;
  is_read: boolean;
  related_id: string | null;
};

/** Item para la pestaña Actividad del perfil: opiniones, café añadido a lista pública, café probado por primera vez. */
export type ProfileActivityItem = {
  id: string;
  type: "review" | "favorite" | "diary";
  userId: number;
  userName: string;
  username: string;
  avatarUrl: string;
  timestamp: number;
  label: string;
  coffeeId?: string | null;
  coffeeName?: string | null;
  /** Para opiniones: valoración y comentario (clic lleva al detalle del café). */
  rating?: number;
  comment?: string | null;
  /** Para "añadió a lista": id y nombre de la lista pública (clic lleva a esa lista). */
  listId?: string | null;
  listName?: string | null;
};

export type HomeCard = {
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
  rating?: number;
  timestamp: number;
};

export type InitialDataBundle = {
  users: UserRow[];
  coffees: CoffeeRow[];
  reviews: CoffeeReviewRow[];
  sensoryProfiles: CoffeeSensoryProfileRow[];
  follows: FollowRow[];
};

export type UserDataBundle = {
  diaryEntries: DiaryEntryRow[];
  pantryItems: PantryItemRow[];
  favorites: FavoriteRow[];
  customCoffees: CoffeeRow[];
  /** Historial (cafés terminados): fuente de verdad Supabase tabla pantry_historical */
  finishedCoffees: FinishedCoffeeRow[];
};
