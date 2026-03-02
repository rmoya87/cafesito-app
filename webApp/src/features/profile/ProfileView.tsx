import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { MentionText } from "../../ui/MentionText";
import { UiIcon } from "../../ui/iconography";
import { Button, IconButton, Input, SheetCard, SheetHandle, SheetOverlay, TabButton, Tabs, Textarea } from "../../ui/components";
import type { CoffeeRow, CoffeeReviewRow, CoffeeSensoryProfileRow, TimelineCard, UserRow, ViewMode } from "../../types";

function ProfileFavoriteItem({
  coffee,
  onOpenCoffee,
  onRemoveFavorite
}: {
  coffee: CoffeeRow;
  onOpenCoffee: (coffeeId: string) => void;
  onRemoveFavorite: (coffeeId: string) => Promise<void>;
}) {
  const [isRemoving, setIsRemoving] = useState(false);
  const [offsetX, setOffsetX] = useState(0);
  const [isSwiping, setIsSwiping] = useState(false);
  const startXRef = useRef<number | null>(null);
  const pointerIdRef = useRef<number | null>(null);
  const offsetRef = useRef(0);
  const swipeThreshold = -72;
  const maxLeft = -110;

  const resetSwipe = useCallback(() => {
    setOffsetX(0);
    setIsSwiping(false);
    startXRef.current = null;
    pointerIdRef.current = null;
    offsetRef.current = 0;
  }, []);
  useEffect(() => {
    offsetRef.current = offsetX;
  }, [offsetX]);

  return (
    <li className="profile-favorite-item">
      <div className="profile-favorite-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div
        className={`coffee-card coffee-card-interactive profile-favorite-row ${offsetX < -1 ? "is-swiping" : ""}`.trim()}
        style={{ transform: `translateX(${offsetX}px)` }}
        role="button"
        tabIndex={0}
        onClick={(event) => {
          if (Math.abs(offsetX) > 4 || isSwiping) {
            event.preventDefault();
            return;
          }
          onOpenCoffee(coffee.id);
        }}
        onPointerDown={(event) => {
          if (isRemoving) return;
          pointerIdRef.current = event.pointerId;
          startXRef.current = event.clientX;
          setIsSwiping(true);
          event.currentTarget.setPointerCapture(event.pointerId);
        }}
        onPointerMove={(event) => {
          if (!isSwiping || startXRef.current == null || pointerIdRef.current !== event.pointerId) return;
          const delta = event.clientX - startXRef.current;
          const next = Math.max(maxLeft, Math.min(0, offsetRef.current + delta));
          setOffsetX(next);
        }}
        onPointerUp={async (event) => {
          if (pointerIdRef.current !== event.pointerId) return;
          const shouldDelete = offsetX <= swipeThreshold;
          if (shouldDelete && !isRemoving) {
            setIsRemoving(true);
            try {
              await onRemoveFavorite(coffee.id);
            } finally {
              setIsRemoving(false);
              resetSwipe();
            }
            return;
          }
          resetSwipe();
        }}
        onPointerCancel={() => resetSwipe()}
        onKeyDown={(event) => {
          if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            onOpenCoffee(coffee.id);
          }
        }}
      >
        <span className="profile-favorite-media">
          {coffee.image_url ? (
            <img className="profile-favorite-image" src={coffee.image_url} alt={coffee.nombre} loading="lazy" decoding="async" />
          ) : (
            <span className="profile-favorite-image profile-favorite-fallback" aria-hidden="true">{coffee.nombre.slice(0, 1).toUpperCase()}</span>
          )}
        </span>
        <span className="profile-favorite-copy">
          <strong>{coffee.nombre}</strong>
          <em>{coffee.marca || "Marca"}</em>
        </span>
        <Button variant="plain"
          type="button"
          className="profile-favorite-remove"
          aria-label="Quitar de favoritos"
          disabled={isRemoving}
          onClick={async (event) => {
            event.preventDefault();
            event.stopPropagation();
            if (isRemoving) return;
            setIsRemoving(true);
            try {
              await onRemoveFavorite(coffee.id);
            } finally {
              setIsRemoving(false);
            }
          }}
        >
          <UiIcon name="favorite-filled" className="ui-icon" />
        </Button>
      </div>
    </li>
  );
}

export function ProfileView({
  user,
  mode,
  tab,
  setTab,
  posts,
  favoriteCoffees,
  allCoffees,
  coffeeReviews,
  coffeeSensoryProfiles,
  followers,
  following,
  onOpenCoffee,
  onOpenUserProfile,
  canEditProfile,
  canFollowProfile,
  isFollowingProfile,
  onToggleFollowProfile,
  onSaveProfile,
  onRemoveFavorite,
  onEditPost,
  onDeletePost,
  onToggleLike,
  onOpenComments,
  resolveMentionUser,
  externalEditProfileSignal,
  sidePanel
}: {
  user: UserRow;
  mode: ViewMode;
  tab: "posts" | "adn" | "favoritos";
  setTab: (value: "posts" | "adn" | "favoritos") => void;
  posts: TimelineCard[];
  favoriteCoffees: CoffeeRow[];
  allCoffees: CoffeeRow[];
  coffeeReviews: CoffeeReviewRow[];
  coffeeSensoryProfiles: CoffeeSensoryProfileRow[];
  followers: number;
  following: number;
  onOpenCoffee: (coffeeId: string) => void;
  onOpenUserProfile: (userId: number) => void;
  canEditProfile: boolean;
  canFollowProfile: boolean;
  isFollowingProfile: boolean;
  onToggleFollowProfile: () => Promise<void>;
  onSaveProfile: (
    userId: number,
    fullName: string,
    bio: string,
    avatarFile?: File | null,
    removeAvatar?: boolean
  ) => Promise<void>;
  onRemoveFavorite: (coffeeId: string) => Promise<void>;
  onEditPost: (postId: string, newText: string, newImageUrl: string, imageFile?: File | null) => Promise<void>;
  onDeletePost: (postId: string) => Promise<void>;
  onToggleLike: (postId: string) => void;
  onOpenComments: (postId: string) => void;
  resolveMentionUser?: (username: string) => { username: string; avatarUrl?: string | null } | null | undefined;
  externalEditProfileSignal: number;
  sidePanel?: ReactNode;
}) {
  const initials = user.full_name
    .split(" ")
    .map((part) => part[0])
    .join("")
    .slice(0, 2)
    .toUpperCase();
  const coffeesById = useMemo(() => {
    const map = new Map<string, CoffeeRow>();
    allCoffees.forEach((coffee) => map.set(coffee.id, coffee));
    return map;
  }, [allCoffees]);
  const sensoryAverages = useMemo(() => {
    type SensorySample = { aroma: number; sabor: number; cuerpo: number; acidez: number; dulzura: number };
    const samples: SensorySample[] = [];
    const addSample = (source: {
      aroma?: number | null;
      sabor?: number | null;
      cuerpo?: number | null;
      acidez?: number | null;
      dulzura?: number | null;
    } | null | undefined) => {
      if (!source) return;
      const sample = {
        aroma: Math.max(0, Number(source.aroma ?? 0)),
        sabor: Math.max(0, Number(source.sabor ?? 0)),
        cuerpo: Math.max(0, Number(source.cuerpo ?? 0)),
        acidez: Math.max(0, Number(source.acidez ?? 0)),
        dulzura: Math.max(0, Number(source.dulzura ?? 0))
      };
      if (sample.aroma || sample.sabor || sample.cuerpo || sample.acidez || sample.dulzura) {
        samples.push(sample);
      }
    };

    favoriteCoffees.forEach((coffee) => addSample(coffee));
    coffeeReviews
      .filter((review) => review.user_id === user.id)
      .forEach((review) => addSample(coffeesById.get(review.coffee_id)));
    coffeeSensoryProfiles
      .filter((profile) => profile.user_id === user.id)
      .forEach((profile) => addSample(profile));

    if (!samples.length) return { aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0, count: 0 };

    const totals = samples.reduce(
      (acc, coffee) => ({
        aroma: acc.aroma + Math.max(0, Number(coffee.aroma ?? 0)),
        sabor: acc.sabor + Math.max(0, Number(coffee.sabor ?? 0)),
        cuerpo: acc.cuerpo + Math.max(0, Number(coffee.cuerpo ?? 0)),
        acidez: acc.acidez + Math.max(0, Number(coffee.acidez ?? 0)),
        dulzura: acc.dulzura + Math.max(0, Number(coffee.dulzura ?? 0))
      }),
      { aroma: 0, sabor: 0, cuerpo: 0, acidez: 0, dulzura: 0 }
    );
    const count = Math.max(1, samples.length);
    return {
      aroma: Number((totals.aroma / count).toFixed(1)),
      sabor: Number((totals.sabor / count).toFixed(1)),
      cuerpo: Number((totals.cuerpo / count).toFixed(1)),
      acidez: Number((totals.acidez / count).toFixed(1)),
      dulzura: Number((totals.dulzura / count).toFixed(1)),
      count: samples.length
    };
  }, [coffeesById, coffeeReviews, coffeeSensoryProfiles, favoriteCoffees, user.id]);
  const adnRadar = useMemo(() => {
    const labels = ["Aroma", "Sabor", "Cuerpo", "Acidez", "Dulzura"];
    const values = [sensoryAverages.aroma, sensoryAverages.sabor, sensoryAverages.cuerpo, sensoryAverages.acidez, sensoryAverages.dulzura]
      .map((value) => Math.max(0, Math.min(10, Number(value) || 0)));
    const cx = 120;
    const cy = 90;
    const radius = 60;
    const angleStep = (Math.PI * 2) / labels.length;
    const startAngle = -Math.PI / 2;
    const rings = [1, 2, 3, 4, 5].map((level) => {
      const factor = level / 5;
      return labels.map((_, index) => {
        const angle = startAngle + index * angleStep;
        return {
          x: cx + Math.cos(angle) * radius * factor,
          y: cy + Math.sin(angle) * radius * factor
        };
      });
    });
    const axes = labels.map((_, index) => {
      const angle = startAngle + index * angleStep;
      return {
        x1: cx,
        y1: cy,
        x2: cx + Math.cos(angle) * radius,
        y2: cy + Math.sin(angle) * radius
      };
    });
    const dataPoints = labels.map((_, index) => {
      const angle = startAngle + index * angleStep;
      const factor = values[index] / 10;
      return {
        x: cx + Math.cos(angle) * radius * factor,
        y: cy + Math.sin(angle) * radius * factor
      };
    });
    const labelPoints = labels.map((label, index) => {
      const angle = startAngle + index * angleStep;
      const dist = radius + 16;
      return {
        label,
        x: cx + Math.cos(angle) * dist,
        y: cy + Math.sin(angle) * dist
      };
    });
    return { rings, axes, dataPoints, labelPoints };
  }, [sensoryAverages.acidez, sensoryAverages.aroma, sensoryAverages.cuerpo, sensoryAverages.dulzura, sensoryAverages.sabor]);
  const desktopPostColumns = useMemo(() => {
    const cols: [TimelineCard[], TimelineCard[], TimelineCard[]] = [[], [], []];
    posts.forEach((post, index) => {
      cols[index % 3].push(post);
    });
    return cols;
  }, [posts]);
  const [showEditProfile, setShowEditProfile] = useState(false);
  const [editNameDraft, setEditNameDraft] = useState(user.full_name);
  const [editBioDraft, setEditBioDraft] = useState(user.bio ?? "");
  const [editAvatarFile, setEditAvatarFile] = useState<File | null>(null);
  const [editAvatarPreview, setEditAvatarPreview] = useState("");
  const [removeAvatarDraft, setRemoveAvatarDraft] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [postMenuId, setPostMenuId] = useState<string | null>(null);
  const [deletePostConfirmId, setDeletePostConfirmId] = useState<string | null>(null);
  const [deletingPost, setDeletingPost] = useState(false);
  const [editPostId, setEditPostId] = useState<string | null>(null);
  const [editPostText, setEditPostText] = useState("");
  const [editPostImageUrl, setEditPostImageUrl] = useState("");
  const [editPostImageFile, setEditPostImageFile] = useState<File | null>(null);
  const [savingPostEdit, setSavingPostEdit] = useState(false);
  const [togglingFollow, setTogglingFollow] = useState(false);
  const [avatarLoadFailed, setAvatarLoadFailed] = useState(false);
  const [likeBurstPostId, setLikeBurstPostId] = useState<string | null>(null);
  const [showAdnAnalysisSheet, setShowAdnAnalysisSheet] = useState(false);
  const likeBurstTimerRef = useRef<number | null>(null);
  const editAvatarInputRef = useRef<HTMLInputElement | null>(null);
  const editPostImageInputRef = useRef<HTMLInputElement | null>(null);
  useEffect(() => {
    setEditNameDraft(user.full_name);
    setEditBioDraft(user.bio ?? "");
    setEditAvatarFile(null);
    setEditAvatarPreview("");
    setRemoveAvatarDraft(false);
  }, [user.bio, user.full_name]);
  useEffect(() => {
    setAvatarLoadFailed(false);
  }, [user.avatar_url]);
  useEffect(() => {
    if (!canEditProfile) return;
    if (externalEditProfileSignal <= 0) return;
    setShowEditProfile(true);
  }, [canEditProfile, externalEditProfileSignal]);
  const closeEditProfileModal = useCallback(() => {
    if (editAvatarPreview.startsWith("blob:")) URL.revokeObjectURL(editAvatarPreview);
    setEditAvatarFile(null);
    setEditAvatarPreview("");
    setRemoveAvatarDraft(false);
    setShowEditProfile(false);
  }, [editAvatarPreview]);
  useEffect(() => {
    return () => {
      if (editAvatarPreview.startsWith("blob:")) URL.revokeObjectURL(editAvatarPreview);
    };
  }, [editAvatarPreview]);
  const closeEditPostModal = useCallback(() => {
    if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
    setEditPostId(null);
    setEditPostImageFile(null);
    setEditPostImageUrl("");
    setEditPostText("");
  }, [editPostImageUrl]);
  const adnAnalysis = useMemo(() => {
    const traits = [
      { label: "Aroma", value: sensoryAverages.aroma },
      { label: "Sabor", value: sensoryAverages.sabor },
      { label: "Cuerpo", value: sensoryAverages.cuerpo },
      { label: "Acidez", value: sensoryAverages.acidez },
      { label: "Dulzura", value: sensoryAverages.dulzura }
    ].sort((a, b) => b.value - a.value);
    const highest = traits[0] ?? null;
    const second = traits[1] ?? null;
    const lead = highest
      ? second && second.value > 3
        ? `Tu paladar es una combinación experta de ${highest.label.toLowerCase()} y ${second.label.toLowerCase()}.`
        : `Tu paladar destaca principalmente por preferir notas de ${highest.label.toLowerCase()}.`
      : "Aún no hay datos suficientes para calcular tu paladar.";
    const description = highest
      ? highest.label === "Acidez"
        ? "Buscas brillo en cada taza. Te apasionan los perfiles cítricos y vibrantes de cafés de altura (1500m+)."
        : highest.label === "Dulzura"
          ? "Eres amante de lo meloso. Prefieres cafés con procesos naturales que resaltan notas de chocolate, caramelo y frutas maduras."
          : highest.label === "Cuerpo"
            ? "Para ti, la textura lo es todo. Disfrutas de esa sensación aterciopelada y densa, típica de tuestes medios y perfiles clásicos."
            : highest.label === "Aroma"
              ? "Tu ritual empieza con el olfato. Te atraen las fragancias complejas, florales y especiadas que inundan la habitación."
              : highest.label === "Sabor"
                ? "Buscas la máxima intensidad y complejidad gustativa. Valoras la persistencia de las notas tras cada sorbo."
                : "Disfrutas de una complejidad excepcional y un balance perfectamente equilibrado en tu ADN cafetero."
      : "Marca cafés como favoritos y añade opiniones o reseñas para construir tu ADN sensorial.";
    const recommendation = highest
      ? highest.label === "Acidez"
        ? { type: "Lavados de alta montaña", origin: "Etiopía o Colombia (Nariño)" }
        : highest.label === "Dulzura"
          ? { type: "Procesos Natural o Honey", origin: "Brasil o El Salvador" }
          : highest.label === "Cuerpo"
            ? { type: "Tuestes medios / Naturales", origin: "Sumatra o Guatemala" }
            : highest.label === "Aroma"
              ? { type: "Variedades florales / Geishas", origin: "Panamá o Ruanda" }
              : highest.label === "Sabor"
                ? { type: "Micro-lotes de especialidad", origin: "Costa Rica o Kenia" }
                : { type: "Blends equilibrados", origin: "Cualquier origen de especialidad" }
      : { type: "Blends equilibrados", origin: "Cualquier origen de especialidad" };
    return { lead, description, recommendation };
  }, [sensoryAverages.acidez, sensoryAverages.aroma, sensoryAverages.cuerpo, sensoryAverages.dulzura, sensoryAverages.sabor]);
  useEffect(() => {
    return () => {
      if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
    };
  }, [editPostImageUrl]);
  useEffect(() => {
    return () => {
      if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
    };
  }, []);
  const profileAvatarSrc = editAvatarPreview || (removeAvatarDraft ? "" : user.avatar_url);
  const content = (
    <>
      <article className="profile-hero profile-hero-card">
        <div className="profile-hero-main">
          {canEditProfile && showEditProfile ? (
            <Input
              ref={editAvatarInputRef}
              type="file"
              accept="image/*"
              className="file-input-hidden"
              onChange={(event) => {
                const file = event.target.files?.[0] ?? null;
                if (!file) return;
                if (editAvatarPreview.startsWith("blob:")) URL.revokeObjectURL(editAvatarPreview);
                const preview = URL.createObjectURL(file);
                setEditAvatarFile(file);
                setEditAvatarPreview(preview);
                setRemoveAvatarDraft(false);
                event.currentTarget.value = "";
              }}
            />
          ) : null}
          <div className="profile-avatar-wrap" aria-hidden="true">
            {profileAvatarSrc && (!avatarLoadFailed || editAvatarPreview) ? (
              <img
                className="profile-avatar-image"
                src={profileAvatarSrc}
                alt={user.username}
                loading="lazy" decoding="async"
                referrerPolicy="no-referrer"
                crossOrigin="anonymous"
                onError={() => setAvatarLoadFailed(true)}
              />
            ) : (
              <div className="avatar avatar-lg profile-avatar-fallback">{initials}</div>
            )}
          </div>
          {canEditProfile && showEditProfile ? (
            <Button
              variant="ghost"
              type="button"
              className="action-button action-button-ghost profile-inline-change-photo"
              onClick={() => editAvatarInputRef.current?.click()}
            >
              Cambiar foto
            </Button>
          ) : null}
          <div className="profile-head-copy">
            {canEditProfile && showEditProfile ? (
              <div className="profile-inline-edit">
                <section className="diary-edit-entry-metrics-grid profile-inline-metrics">
                  <label className="diary-edit-entry-metric-field is-caffeine profile-inline-name-field">
                    <span>Nombre</span>
                    <div className="diary-edit-entry-metric-value">
                      <Input
                        className="search-wide diary-edit-entry-metric-input profile-inline-name-input"
                        type="text"
                        inputMode="text"
                        value={editNameDraft}
                        maxLength={120}
                        onChange={(event) => setEditNameDraft(event.target.value)}
                      />
                    </div>
                  </label>
                  <label className="diary-edit-entry-metric-field is-caffeine profile-inline-bio-field">
                    <span>Bio</span>
                    <div className="diary-edit-entry-metric-value">
                      <Textarea
                        className="search-wide diary-edit-entry-metric-input profile-inline-bio-input"
                        rows={3}
                        value={editBioDraft}
                        maxLength={500}
                        onChange={(event) => setEditBioDraft(event.target.value)}
                      />
                    </div>
                  </label>
                </section>
              </div>
            ) : (
              <>
                <p className="feed-user profile-name">{user.full_name}</p>
                <p className="feed-meta profile-username">@{user.username}</p>
                {user.bio ? <p className="profile-bio">{user.bio}</p> : null}
              </>
            )}
          </div>
        </div>
        {canFollowProfile ? (
          <div className="profile-head-actions profile-head-actions-inline">
            <Button variant="primary"
              className={`action-button profile-edit-button profile-follow-button ${isFollowingProfile ? "action-button-ghost is-following" : ""}`.trim()}
              disabled={togglingFollow}
              onClick={async () => {
                if (togglingFollow) return;
                setTogglingFollow(true);
                try {
                  await onToggleFollowProfile();
                } finally {
                  setTogglingFollow(false);
                }
              }}
            >
              {togglingFollow ? "..." : isFollowingProfile ? "Siguiendo" : "Seguir"}
            </Button>
          </div>
        ) : canEditProfile && showEditProfile ? (
          <div className="profile-head-actions profile-inline-edit-actions">
            <Button variant="primary"
              className="action-button profile-inline-save-button"
              disabled={savingProfile || !editNameDraft.trim()}
              onClick={async () => {
                if (!editNameDraft.trim() || savingProfile) return;
                setSavingProfile(true);
                try {
                  await onSaveProfile(user.id, editNameDraft, editBioDraft, editAvatarFile, removeAvatarDraft);
                  closeEditProfileModal();
                } finally {
                  setSavingProfile(false);
                }
              }}
            >
              {savingProfile ? "Guardando..." : "Guardar"}
            </Button>
          </div>
        ) : null}
      </article>

      <section className="profile-stats-row" aria-label="Estadisticas de perfil">
        <article className="profile-stat-item"><strong className="profile-stat-value">{posts.length}</strong><span className="profile-stat-label">POSTS</span></article>
        <article className="profile-stat-item"><strong className="profile-stat-value">{followers}</strong><span className="profile-stat-label">SEGUIDORES</span></article>
        <article className="profile-stat-item"><strong className="profile-stat-value">{following}</strong><span className="profile-stat-label">SIGUIENDO</span></article>
      </section>

      <Tabs className="profile-tabs" aria-label="Tabs perfil">
        <TabButton active={tab === "posts"} role="tab" aria-selected={tab === "posts"} onClick={() => setTab("posts")}>Posts</TabButton>
        <TabButton active={tab === "adn"} role="tab" aria-selected={tab === "adn"} onClick={() => setTab("adn")}>ADN</TabButton>
        <TabButton active={tab === "favoritos"} role="tab" aria-selected={tab === "favoritos"} onClick={() => setTab("favoritos")}>Favoritos</TabButton>
      </Tabs>

      {tab === "posts" ? (
        <>
          <ul className="feed-list profile-post-list profile-post-list-mobile">
            {posts.length ? posts.map((post, index) => (
            <li key={post.id} className="feed-card feed-card-premium feed-entry" style={{ ["--feed-index" as string]: index }}>
              <article>
                <header className="feed-head">
                  <Button variant="plain" type="button" className="feed-user-link" onClick={() => onOpenUserProfile(user.id)}>
                    {user.avatar_url ? (
                      <img className="avatar avatar-photo" src={user.avatar_url} alt={user.username} loading="lazy" decoding="async" referrerPolicy="no-referrer" crossOrigin="anonymous" />
                    ) : (
                      <div className="avatar" aria-hidden="true">{initials}</div>
                    )}
                    <div>
                      <p className="feed-user">{user.full_name}</p>
                      <p className="feed-meta">{post.minsAgoLabel.toUpperCase()}</p>
                    </div>
                  </Button>
                  {canEditProfile ? (
                    <IconButton tone="default" className="post-menu-trigger" aria-label="Opciones" onClick={() => setPostMenuId(post.id)}>
                      <UiIcon name="more" className="ui-icon" />
                    </IconButton>
                  ) : null}
                </header>
                {post.text ? <p className="feed-text"><MentionText text={post.text} resolveMentionUser={resolveMentionUser} /></p> : null}
                {post.imageUrl ? <img className={`feed-image ${post.text ? "" : "feed-image-no-text"}`.trim()} src={post.imageUrl} alt="Publicación" loading="lazy" decoding="async" /> : null}
                {post.coffeeTagName ? (
                  <Button variant="plain" type="button" className="coffee-tag-card" onClick={() => post.coffeeId && onOpenCoffee(post.coffeeId)} disabled={!post.coffeeId}>
                    <div className="coffee-tag-card-media">
                      {post.coffeeImageUrl ? (
                        <img className="coffee-tag-image" src={post.coffeeImageUrl} alt={post.coffeeTagName} loading="lazy" decoding="async" />
                      ) : (
                        <div className="coffee-tag-image coffee-tag-image-fallback" aria-hidden="true">
                          <UiIcon name="coffee" className="ui-icon" />
                        </div>
                      )}
                    </div>
                    <div className="coffee-tag-copy">
                      <p className="coffee-origin">CAFE ETIQUETADO</p>
                      <p className="coffee-tag-name">{post.coffeeTagName}</p>
                      {post.coffeeTagBrand ? <p className="coffee-tag-brand">{post.coffeeTagBrand.toUpperCase()}</p> : null}
                    </div>
                    <UiIcon name="chevron-right" className="ui-icon" />
                  </Button>
                ) : null}
                <footer className="feed-stats">
                  <Button variant="plain"
                    type="button"
                    className={`inline-action action-like ${post.likedByActiveUser ? "is-liked" : ""} ${likeBurstPostId === post.id ? "is-bursting" : ""}`.trim()}
                    onClick={() => {
                      if (!post.likedByActiveUser) {
                        setLikeBurstPostId(post.id);
                        if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
                        likeBurstTimerRef.current = window.setTimeout(() => {
                          setLikeBurstPostId(null);
                          likeBurstTimerRef.current = null;
                        }, 520);
                      }
                      onToggleLike(post.id);
                    }}
                  >
                    <span className="like-icon-wrap">
                      <UiIcon name={post.likedByActiveUser ? "coffee-filled" : "coffee"} className="ui-icon" />
                      <span className="like-burst" aria-hidden="true">
                        <span />
                        <span />
                        <span />
                        <span />
                        <span />
                        <span />
                      </span>
                    </span>
                    {post.likes > 0 ? <span>{post.likes}</span> : null}
                  </Button>
                  <Button variant="plain" type="button" className="inline-action" onClick={() => onOpenComments(post.id)}>
                    <UiIcon name="chat" className="ui-icon" />
                    {post.comments > 0 ? <span>{post.comments}</span> : null}
                  </Button>
                </footer>
              </article>
            </li>
            )) : <li className="feed-card profile-empty-card">Aún no hay publicaciones</li>}
          </ul>
          {posts.length ? (
            <div className="profile-post-masonry-desktop">
              {desktopPostColumns.map((column, columnIndex) => (
                <ul key={`profile-col-${columnIndex}`} className="feed-list profile-post-column">
                  {column.map((post, index) => (
                    <li key={post.id} className="feed-card feed-card-premium feed-entry" style={{ ["--feed-index" as string]: index }}>
                      <article>
                        <header className="feed-head">
                          <Button variant="plain" type="button" className="feed-user-link" onClick={() => onOpenUserProfile(user.id)}>
                            {user.avatar_url ? (
                              <img className="avatar avatar-photo" src={user.avatar_url} alt={user.username} loading="lazy" decoding="async" referrerPolicy="no-referrer" crossOrigin="anonymous" />
                            ) : (
                              <div className="avatar" aria-hidden="true">{initials}</div>
                            )}
                            <div>
                              <p className="feed-user">{user.full_name}</p>
                              <p className="feed-meta">{post.minsAgoLabel.toUpperCase()}</p>
                            </div>
                          </Button>
                          {canEditProfile ? (
                            <IconButton tone="default" className="post-menu-trigger" aria-label="Opciones" onClick={() => setPostMenuId(post.id)}>
                              <UiIcon name="more" className="ui-icon" />
                            </IconButton>
                          ) : null}
                        </header>
                        {post.text ? <p className="feed-text"><MentionText text={post.text} resolveMentionUser={resolveMentionUser} /></p> : null}
                        {post.imageUrl ? <img className={`feed-image ${post.text ? "" : "feed-image-no-text"}`.trim()} src={post.imageUrl} alt="Publicación" loading="lazy" decoding="async" /> : null}
                        {post.coffeeTagName ? (
                          <Button variant="plain" type="button" className="coffee-tag-card" onClick={() => post.coffeeId && onOpenCoffee(post.coffeeId)} disabled={!post.coffeeId}>
                            <div className="coffee-tag-card-media">
                              {post.coffeeImageUrl ? (
                                <img className="coffee-tag-image" src={post.coffeeImageUrl} alt={post.coffeeTagName} loading="lazy" decoding="async" />
                              ) : (
                                <div className="coffee-tag-image coffee-tag-image-fallback" aria-hidden="true">
                                  <UiIcon name="coffee" className="ui-icon" />
                                </div>
                              )}
                            </div>
                            <div className="coffee-tag-copy">
                              <p className="coffee-origin">CAFE ETIQUETADO</p>
                              <p className="coffee-tag-name">{post.coffeeTagName}</p>
                              {post.coffeeTagBrand ? <p className="coffee-tag-brand">{post.coffeeTagBrand.toUpperCase()}</p> : null}
                            </div>
                            <UiIcon name="chevron-right" className="ui-icon" />
                          </Button>
                        ) : null}
                        <footer className="feed-stats">
                          <Button variant="plain"
                            type="button"
                            className={`inline-action action-like ${post.likedByActiveUser ? "is-liked" : ""} ${likeBurstPostId === post.id ? "is-bursting" : ""}`.trim()}
                            onClick={() => {
                              if (!post.likedByActiveUser) {
                                setLikeBurstPostId(post.id);
                                if (likeBurstTimerRef.current != null) window.clearTimeout(likeBurstTimerRef.current);
                                likeBurstTimerRef.current = window.setTimeout(() => {
                                  setLikeBurstPostId(null);
                                  likeBurstTimerRef.current = null;
                                }, 520);
                              }
                              onToggleLike(post.id);
                            }}
                          >
                            <span className="like-icon-wrap">
                              <UiIcon name={post.likedByActiveUser ? "coffee-filled" : "coffee"} className="ui-icon" />
                              <span className="like-burst" aria-hidden="true">
                                <span />
                                <span />
                                <span />
                                <span />
                                <span />
                                <span />
                              </span>
                            </span>
                            {post.likes > 0 ? <span>{post.likes}</span> : null}
                          </Button>
                          <Button variant="plain" type="button" className="inline-action" onClick={() => onOpenComments(post.id)}>
                            <UiIcon name="chat" className="ui-icon" />
                            {post.comments > 0 ? <span>{post.comments}</span> : null}
                          </Button>
                        </footer>
                      </article>
                    </li>
                  ))}
                </ul>
              ))}
            </div>
          ) : null}
        </>
      ) : null}

      {tab === "adn" ? (
        <div className={`profile-adn-layout ${mode === "desktop" ? "is-desktop" : ""}`.trim()}>
          {mode === "desktop" ? (
            <article className="config-card profile-adn-card profile-adn-radar-card is-static">
              <div className="profile-adn-radar-wrap">
                <svg className="profile-adn-radar" viewBox="0 0 240 190" aria-label="Perfil sensorial radar">
                  {adnRadar.rings.map((ring, index) => (
                    <polygon
                      key={`ring-${index}`}
                      points={ring.map((point) => `${point.x},${point.y}`).join(" ")}
                      className="profile-adn-radar-ring"
                    />
                  ))}
                  {adnRadar.axes.map((axis, index) => (
                    <line key={`axis-${index}`} x1={axis.x1} y1={axis.y1} x2={axis.x2} y2={axis.y2} className="profile-adn-radar-axis" />
                  ))}
                  <polygon
                    points={adnRadar.dataPoints.map((point) => `${point.x},${point.y}`).join(" ")}
                    className="profile-adn-radar-shape"
                  />
                  {adnRadar.dataPoints.map((point, index) => (
                    <circle key={`point-${index}`} cx={point.x} cy={point.y} r="3" className="profile-adn-radar-point" />
                  ))}
                  {adnRadar.labelPoints.map((point, index) => (
                    <text key={`label-${index}`} x={point.x} y={point.y} className="profile-adn-radar-label" textAnchor="middle" dominantBaseline="middle">
                      {point.label}
                    </text>
                  ))}
                </svg>
              </div>
              <p className="profile-adn-caption">Tus gustos basados en favoritos, opiniones y reseñas.</p>
            </article>
          ) : (
            <Button variant="plain"
              type="button"
              className="config-card profile-adn-card profile-adn-radar-card profile-adn-open"
              onClick={() => setShowAdnAnalysisSheet(true)}
              aria-label="Abrir análisis de preferencia"
            >
              <div className="profile-adn-radar-wrap">
                <svg className="profile-adn-radar" viewBox="0 0 240 190" aria-label="Perfil sensorial radar">
                  {adnRadar.rings.map((ring, index) => (
                    <polygon
                      key={`ring-${index}`}
                      points={ring.map((point) => `${point.x},${point.y}`).join(" ")}
                      className="profile-adn-radar-ring"
                    />
                  ))}
                  {adnRadar.axes.map((axis, index) => (
                    <line key={`axis-${index}`} x1={axis.x1} y1={axis.y1} x2={axis.x2} y2={axis.y2} className="profile-adn-radar-axis" />
                  ))}
                  <polygon
                    points={adnRadar.dataPoints.map((point) => `${point.x},${point.y}`).join(" ")}
                    className="profile-adn-radar-shape"
                  />
                  {adnRadar.dataPoints.map((point, index) => (
                    <circle key={`point-${index}`} cx={point.x} cy={point.y} r="3" className="profile-adn-radar-point" />
                  ))}
                  {adnRadar.labelPoints.map((point, index) => (
                    <text key={`label-${index}`} x={point.x} y={point.y} className="profile-adn-radar-label" textAnchor="middle" dominantBaseline="middle">
                      {point.label}
                    </text>
                  ))}
                </svg>
              </div>
              <p className="profile-adn-caption">Tus gustos basados en favoritos, opiniones y reseñas.</p>
            </Button>
          )}
          {mode === "desktop" ? (
            <article className="config-card profile-adn-analysis-panel">
              <h3 className="profile-adn-analysis-title">ANÁLISIS DE PREFERENCIAS</h3>
              <div className="profile-adn-analysis-body">
                <p className="profile-adn-analysis-lead">{adnAnalysis.lead}</p>
                <p className="profile-adn-analysis-text">{adnAnalysis.description}</p>
                <article className="profile-adn-recommend-card">
                  <div className="profile-adn-recommend-head">
                    <UiIcon name="sparkles" className="ui-icon" />
                    <strong>RECOMENDACIÓN IDEAL</strong>
                  </div>
                  <p className="profile-adn-recommend-title">Deberías probar: {adnAnalysis.recommendation.type}</p>
                  <p className="profile-adn-recommend-origin">Orígenes sugeridos: {adnAnalysis.recommendation.origin}</p>
                </article>
              </div>
            </article>
          ) : null}
        </div>
      ) : null}

      {tab === "favoritos" ? (
        <>
          <ul className="coffee-list profile-favorite-list">
            {favoriteCoffees.length ? favoriteCoffees.map((coffee) => (
              <ProfileFavoriteItem key={coffee.id} coffee={coffee} onOpenCoffee={onOpenCoffee} onRemoveFavorite={onRemoveFavorite} />
            )) : <li className="coffee-card profile-empty-card">No hay cafés favoritos</li>}
          </ul>
        </>
      ) : null}
      {showAdnAnalysisSheet && mode !== "desktop" ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Análisis de preferencia" onDismiss={() => setShowAdnAnalysisSheet(false)} onClick={() => setShowAdnAnalysisSheet(false)}>
          <SheetCard className="profile-adn-analysis-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">ANÁLISIS DE PREFERENCIAS</strong>
            </header>
            <div className="profile-adn-analysis-body">
              <p className="profile-adn-analysis-lead">{adnAnalysis.lead}</p>
              <p className="profile-adn-analysis-text">{adnAnalysis.description}</p>
              <article className="profile-adn-recommend-card">
                <div className="profile-adn-recommend-head">
                  <UiIcon name="sparkles" className="ui-icon" />
                  <strong>RECOMENDACIÓN IDEAL</strong>
                </div>
                <p className="profile-adn-recommend-title">Deberías probar: {adnAnalysis.recommendation.type}</p>
                <p className="profile-adn-recommend-origin">Orígenes sugeridos: {adnAnalysis.recommendation.origin}</p>
              </article>
              <Button variant="primary"
                className="action-button profile-adn-continue-button"
                onClick={() => setShowAdnAnalysisSheet(false)}
              >
                CONTINUAR EXPLORANDO
              </Button>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
      {postMenuId && canEditProfile ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Opciones publicación" onDismiss={() => setPostMenuId(null)} onClick={() => setPostMenuId(null)}>
          <SheetCard className="diary-sheet diary-sheet-pantry-options profile-post-menu-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="diary-sheet-list">
              <Button variant="plain"
                className="diary-sheet-action diary-sheet-action-pantry is-delete"
                onClick={() => {
                  if (!postMenuId) return;
                  setDeletePostConfirmId(postMenuId);
                  setPostMenuId(null);
                }}
              >
                <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">delete</span>
                <span>Borrar</span>
                <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
              </Button>
              <Button variant="plain"
                className="diary-sheet-action diary-sheet-action-pantry"
                onClick={() => {
                  const post = posts.find((item) => item.id === postMenuId);
                  if (!post) return;
                  setEditPostId(post.id);
                  setEditPostText(post.text || "");
                  setEditPostImageUrl(post.imageUrl || "");
                  setEditPostImageFile(null);
                  setPostMenuId(null);
                }}
              >
                <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">edit</span>
                <span>Editar</span>
                <span className="ui-icon material-symbol-icon is-filled" aria-hidden="true">chevron_right</span>
              </Button>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
      {deletePostConfirmId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Eliminar publicación" onDismiss={() => setDeletePostConfirmId(null)} onClick={() => setDeletePostConfirmId(null)}>
          <SheetCard className="diary-sheet diary-sheet-delete-confirm" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <div className="diary-delete-confirm-body">
              <h2 className="diary-delete-confirm-title">Eliminar publicación</h2>
              <p className="diary-delete-confirm-text">
                ¿Estás seguro de eliminar esta publicación? Esta acción no se puede deshacer.
              </p>
              <div className="diary-delete-confirm-actions">
                <Button
                  variant="plain"
                  type="button"
                  className="diary-delete-confirm-cancel"
                  onClick={() => setDeletePostConfirmId(null)}
                  disabled={deletingPost}
                >
                  Cancelar
                </Button>
                <Button
                  variant="plain"
                  type="button"
                  className="diary-delete-confirm-submit"
                  disabled={deletingPost}
                  onClick={async () => {
                    if (deletingPost) return;
                    setDeletingPost(true);
                    try {
                      await onDeletePost(deletePostConfirmId);
                      setDeletePostConfirmId(null);
                    } finally {
                      setDeletingPost(false);
                    }
                  }}
                >
                  {deletingPost ? "Eliminando..." : "Eliminar"}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
      {editPostId ? (
        <SheetOverlay role="dialog" aria-modal="true" aria-label="Editar publicación" onDismiss={closeEditPostModal} onClick={closeEditPostModal}>
          <SheetCard className="profile-edit-sheet" onClick={(event) => event.stopPropagation()}>
            <SheetHandle aria-hidden="true" />
            <header className="sheet-header">
              <strong className="sheet-title">Editar publicación</strong>
            </header>
            <div className="diary-sheet-form">
              <Input
                ref={editPostImageInputRef}
                type="file"
                accept="image/*"
                className="file-input-hidden"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  if (!file) return;
                  if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
                  const preview = URL.createObjectURL(file);
                  setEditPostImageFile(file);
                  setEditPostImageUrl(preview);
                  event.currentTarget.value = "";
                }}
              />
              <label>
                <span>Texto</span>
                <Textarea
                  className="search-wide profile-edit-bio"
                  rows={4}
                  maxLength={500}
                  value={editPostText}
                  onChange={(event) => setEditPostText(event.target.value)}
                />
              </label>
              <div className="profile-edit-post-image-wrap">
                {editPostImageUrl ? <img src={editPostImageUrl} alt="Previsualización" loading="lazy" decoding="async" /> : <p className="feed-meta">Sin imagen</p>}
              </div>
              <div className="profile-edit-post-actions">
                <Button variant="ghost" className="action-button action-button-ghost" onClick={() => editPostImageInputRef.current?.click()}>
                  Cambiar imagen
                </Button>
                {editPostImageUrl ? (
                  <Button variant="ghost"
                    className="action-button action-button-ghost"
                    onClick={() => {
                      if (editPostImageUrl.startsWith("blob:")) URL.revokeObjectURL(editPostImageUrl);
                      setEditPostImageFile(null);
                      setEditPostImageUrl("");
                    }}
                  >
                    Quitar imagen
                  </Button>
                ) : null}
              </div>
              <div className="diary-sheet-form-actions">
                <Button variant="ghost" className="action-button action-button-ghost" onClick={closeEditPostModal} disabled={savingPostEdit}>
                  Cancelar
                </Button>
                <Button variant="primary"
                  className="action-button"
                  disabled={savingPostEdit}
                  onClick={async () => {
                    if (!editPostId || savingPostEdit) return;
                    setSavingPostEdit(true);
                    try {
                      await onEditPost(editPostId, editPostText, editPostImageUrl, editPostImageFile);
                      closeEditPostModal();
                    } finally {
                      setSavingPostEdit(false);
                    }
                  }}
                >
                  {savingPostEdit ? "Guardando..." : "Guardar"}
                </Button>
              </div>
            </div>
          </SheetCard>
        </SheetOverlay>
      ) : null}
    </>
  );

  if (sidePanel) {
    return (
      <div className="split-with-side">
        <div className="profile-content-wrap">{content}</div>
        <aside className="timeline-side-column">{sidePanel}</aside>
      </div>
    );
  }

  return content;
}








