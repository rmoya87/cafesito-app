import { type ReactNode, useCallback, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { EMPTY } from "../../core/emptyErrorStrings";
import { sendEvent } from "../../core/ga4";
import { UiIcon } from "../../ui/iconography";
import { Button, IconButton, Input, SheetCard, SheetHandle, SheetOverlay, TabButton, Tabs, Textarea } from "../../ui/components";
import { CreateListSheet } from "../lists/CreateListSheet";
import { toRelativeMinutes } from "../../core/time";
import type { CoffeeRow, ListPrivacy, CoffeeReviewRow, CoffeeSensoryProfileRow, ProfileActivityItem, UserListRow, UserRow, ViewMode } from "../../types";
import { useI18n } from "../../i18n";

const SWIPE_THRESHOLD_PX = 3;
const VERTICAL_LOCK_PX = 2;

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
  const [swipeActive, setSwipeActive] = useState(false);
  const startXRef = useRef<number | null>(null);
  const startYRef = useRef<number | null>(null);
  const pointerIdRef = useRef<number | null>(null);
  const offsetRef = useRef(0);
  const contentRef = useRef<HTMLDivElement | null>(null);
  const rafIdRef = useRef<number | null>(null);
  const movedRef = useRef(false);
  const swipeThreshold = -72;
  const maxLeft = -110;

  const resetSwipe = useCallback(() => {
    setOffsetX(0);
    setSwipeActive(false);
    startXRef.current = null;
    startYRef.current = null;
    pointerIdRef.current = null;
    offsetRef.current = 0;
    movedRef.current = false;
    const el = contentRef.current;
    if (el) el.style.transform = "";
    if (rafIdRef.current != null) {
      cancelAnimationFrame(rafIdRef.current);
      rafIdRef.current = null;
    }
  }, []);

  return (
    <li className="profile-favorite-item">
      <div className="profile-favorite-swipe-bg" aria-hidden="true">
        <UiIcon name="trash" className="ui-icon" />
      </div>
      <div
        ref={contentRef}
        className={`coffee-card coffee-card-interactive profile-favorite-row ${swipeActive || offsetX < -1 ? "is-swiping" : ""}`.trim()}
        style={{ transform: `translateX(${offsetX}px)` }}
        role="button"
        tabIndex={0}
        aria-label={`Ver detalle de ${coffee.nombre}`}
        onClick={(event) => {
          if (movedRef.current || Math.abs(offsetX) > 4) {
            event.preventDefault();
            return;
          }
          onOpenCoffee(coffee.id);
        }}
        onPointerDown={(event) => {
          if (isRemoving) return;
          pointerIdRef.current = event.pointerId;
          startXRef.current = event.clientX;
          startYRef.current = event.clientY;
          movedRef.current = false;
        }}
        onPointerMove={(event) => {
          if (startXRef.current == null || startYRef.current == null || pointerIdRef.current !== event.pointerId) return;
          const dx = event.clientX - startXRef.current;
          const dy = event.clientY - startYRef.current;
          const absDx = Math.abs(dx);
          const absDy = Math.abs(dy);
          if (!swipeActive) {
            if (absDx < SWIPE_THRESHOLD_PX && absDy < SWIPE_THRESHOLD_PX) return;
            if (absDy > absDx + VERTICAL_LOCK_PX) return;
            if (dx < 0 && absDx > absDy) {
              setSwipeActive(true);
              movedRef.current = true;
              event.currentTarget.setPointerCapture(event.pointerId);
            } else return;
          }
          if (dx >= 0) {
            offsetRef.current = 0;
            setOffsetX(0);
            const el = contentRef.current;
            if (el) el.style.transform = "translateX(0px)";
            return;
          }
          const raw = dx;
          const withResistance = raw <= maxLeft ? maxLeft + (raw - maxLeft) * 0.25 : raw;
          const offset = Math.max(maxLeft, Math.min(0, withResistance));
          offsetRef.current = offset;
          const el = contentRef.current;
          if (el) el.style.transform = `translateX(${offset}px)`;
          if (rafIdRef.current == null) {
            rafIdRef.current = requestAnimationFrame(() => {
              setOffsetX(offsetRef.current);
              rafIdRef.current = null;
            });
          }
        }}
        onPointerUp={async (event) => {
          if (pointerIdRef.current !== event.pointerId) return;
          if (rafIdRef.current != null) {
            cancelAnimationFrame(rafIdRef.current);
            rafIdRef.current = null;
          }
          pointerIdRef.current = null;
          startXRef.current = null;
          startYRef.current = null;
          setSwipeActive(false);
          if (event.currentTarget.hasPointerCapture(event.pointerId)) {
            event.currentTarget.releasePointerCapture(event.pointerId);
          }
          const finalOffset = offsetRef.current;
          const el = contentRef.current;
          if (el) el.style.transform = "";
          if (finalOffset <= swipeThreshold && !isRemoving) {
            setOffsetX(0);
            setIsRemoving(true);
            try {
              await onRemoveFavorite(coffee.id);
            } finally {
              setIsRemoving(false);
            }
            resetSwipe();
            return;
          }
          setOffsetX(finalOffset);
          requestAnimationFrame(() => setOffsetX(0));
          window.setTimeout(() => { movedRef.current = false; }, 120);
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
          <em>{(coffee.marca || "Marca").toUpperCase()}</em>
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
  followedActivity,
  activityIsProfileUser = false,
  favoriteCoffees,
  allCoffees,
  coffeeReviews,
  coffeeSensoryProfiles,
  followers,
  following,
  onOpenCoffee,
  onOpenUserList,
  onOpenUserProfile,
  onOpenFollowers,
  onOpenFollowing,
  onOpenFavoritesList,
  canEditProfile,
  canFollowProfile,
  isFollowingProfile,
  onToggleFollowProfile,
  onSaveProfile,
  onRemoveFavorite,
  userLists = [],
  onCreateList,
  onOpenList,
  externalEditProfileSignal,
  sidePanel,
  profileDiaryCoffeeIds = [],
  profileListCoffeeIds = [],
  onExploreCafes,
  activeUserId = null
}: {
  user: UserRow;
  mode: ViewMode;
  tab: "actividad" | "adn" | "favoritos";
  setTab: (value: "actividad" | "adn" | "favoritos") => void;
  /** Actividad: de personas que sigues (propio perfil) o del usuario del perfil (perfil tercero) */
  followedActivity: ProfileActivityItem[];
  /** true cuando la actividad mostrada es la del usuario del perfil visitado (perfil tercero) */
  activityIsProfileUser?: boolean;
  /** Opcional: al pulsar CTA "Explorar cafés" en estado vacío de Actividad (ej. ir a pestaña Búsqueda) */
  onExploreCafes?: () => void;
  /** ID del usuario logueado: si la actividad es suya, se muestra en segunda persona */
  activeUserId?: number | null;
  favoriteCoffees: CoffeeRow[];
  allCoffees: CoffeeRow[];
  coffeeReviews: CoffeeReviewRow[];
  coffeeSensoryProfiles: CoffeeSensoryProfileRow[];
  followers: number;
  following: number;
  onOpenCoffee: (coffeeId: string) => void;
  /** Abrir la lista pública de otro usuario (perfil > listas > lista). */
  onOpenUserList?: (userId: number, listId: string) => void;
  onOpenUserProfile: (userId: number) => void;
  onOpenFollowers: () => void;
  onOpenFollowing: () => void;
  onOpenFavoritesList?: () => void;
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
  userLists?: UserListRow[];
  onCreateList?: (name: string, privacy: ListPrivacy) => Promise<void>;
  onOpenList?: (listId: string) => void;
  externalEditProfileSignal: number;
  sidePanel?: ReactNode;
  /** IDs de cafés consumidos (diario, excl. agua) del usuario del perfil, para ADN */
  profileDiaryCoffeeIds?: string[];
  /** IDs de cafés en listas del usuario del perfil, para ADN */
  profileListCoffeeIds?: string[];
}) {
  const { t, locale } = useI18n();
  const profileCopy = useMemo(() => {
    if (locale === "en") {
      return {
        brandFallback: "Brand",
        me: "You",
        ownProfile: "Open your profile",
        userProfile: (name: string) => `Open profile of ${name}`,
        dnaNoData: "There is not enough data yet to calculate your palate.",
        dnaNoDataCta: "Consume coffees, add them to lists or favorites and leave reviews to build your sensory DNA.",
        dnaAcidity: "You seek brightness in every cup. You love citrus and vibrant profiles from high-altitude coffees (1500m+).",
        dnaSweetness: "You love sweetness. You prefer natural-process coffees with chocolate, caramel and ripe fruit notes.",
        dnaBody: "For you, texture is everything. You enjoy that velvety and dense mouthfeel, typical of medium roasts and classic profiles.",
        dnaAroma: "Your ritual starts with smell. You are drawn to complex, floral and spicy fragrances that fill the room.",
        dnaFlavor: "You seek maximum intensity and gustatory complexity. You value lingering notes after each sip.",
        dnaDefault: "You enjoy exceptional complexity and a perfectly balanced coffee DNA.",
        adnCaption: "Your tastes are based on coffees you consume, keep in lists/favorites, and review.",
        preferenceAnalysisTitle: "PREFERENCE ANALYSIS",
        shouldTry: "You should try:",
        suggestedOrigins: "Suggested origins:",
        openPreferenceAnalysis: "Open preference analysis",
        radarAria: "Sensory radar profile",
        continueExploring: "CONTINUE EXPLORING",
        viewList: (name: string) => `Open list ${name}`
      };
    }
    if (locale === "fr") {
      return {
        brandFallback: "Marque",
        me: "Toi",
        ownProfile: "Voir ton profil",
        userProfile: (name: string) => `Voir le profil de ${name}`,
        dnaNoData: "Il n'y a pas encore assez de donnees pour calculer ton palais.",
        dnaNoDataCta: "Consomme des cafes, ajoute-les a des listes ou favoris et laisse des avis pour construire ton ADN sensoriel.",
        dnaAcidity: "Tu recherches l'eclat dans chaque tasse. Tu aimes les profils vifs et agrumes des cafes d'altitude.",
        dnaSweetness: "Tu adores la douceur. Tu preferes les cafes naturels aux notes de chocolat, caramel et fruits murs.",
        dnaBody: "Pour toi, la texture est essentielle. Tu apprecies une sensation dense et veloutee.",
        dnaAroma: "Ton rituel commence par l'odorat. Tu es attire par les aromes floraux et epices.",
        dnaFlavor: "Tu recherches une intensite et une complexite gustative elevees.",
        dnaDefault: "Tu apprecies un ADN cafe equilibre et complexe.",
        adnCaption: "Tes gouts selon les cafes que tu consommes, en listes/favoris et que tu notes.",
        preferenceAnalysisTitle: "ANALYSE DES PRÉFÉRENCES",
        shouldTry: "Tu devrais essayer :",
        suggestedOrigins: "Origines suggérées :",
        openPreferenceAnalysis: "Ouvrir l'analyse des préférences",
        radarAria: "Profil sensoriel radar",
        continueExploring: "CONTINUER À EXPLORER",
        viewList: (name: string) => `Voir la liste ${name}`
      };
    }
    if (locale === "pt") {
      return {
        brandFallback: "Marca",
        me: "Voce",
        ownProfile: "Ver seu perfil",
        userProfile: (name: string) => `Ver perfil de ${name}`,
        dnaNoData: "Ainda nao ha dados suficientes para calcular seu paladar.",
        dnaNoDataCta: "Consuma cafes, adicione a listas ou favoritos e deixe avaliacoes para construir seu DNA sensorial.",
        dnaAcidity: "Voce busca brilho em cada xicara. Gosta de perfis citricos e vibrantes.",
        dnaSweetness: "Voce gosta de doçura. Prefere cafes com notas de chocolate e caramelo.",
        dnaBody: "Para voce, textura e essencial. Gosta de uma sensacao aveludada e densa.",
        dnaAroma: "Seu ritual comeca pelo aroma. Voce busca fragrancias complexas.",
        dnaFlavor: "Voce busca intensidade e complexidade de sabor.",
        dnaDefault: "Voce aprecia um DNA de cafe equilibrado e complexo.",
        adnCaption: "Seus gostos com base nos cafes que voce consome, salva em listas/favoritos e avalia.",
        preferenceAnalysisTitle: "ANÁLISE DE PREFERÊNCIAS",
        shouldTry: "Você deveria provar:",
        suggestedOrigins: "Origens sugeridas:",
        openPreferenceAnalysis: "Abrir análise de preferências",
        radarAria: "Perfil sensorial radar",
        continueExploring: "CONTINUAR EXPLORANDO",
        viewList: (name: string) => `Ver lista ${name}`
      };
    }
    if (locale === "de") {
      return {
        brandFallback: "Marke",
        me: "Du",
        ownProfile: "Dein Profil ansehen",
        userProfile: (name: string) => `Profil von ${name} ansehen`,
        dnaNoData: "Es gibt noch nicht genug Daten, um deinen Geschmack zu berechnen.",
        dnaNoDataCta: "Trinke Kaffee, fuege ihn zu Listen/Favoriten hinzu und schreibe Bewertungen fuer deine sensorische DNA.",
        dnaAcidity: "Du suchst Helligkeit in jeder Tasse und magst lebendige Profile.",
        dnaSweetness: "Du liebst Suesse und bevorzugst Schokoladen- und Karamellnoten.",
        dnaBody: "Fuer dich ist die Textur entscheidend. Du magst ein samtiges Mundgefuehl.",
        dnaAroma: "Dein Ritual beginnt mit dem Geruchssinn. Du magst komplexe Aromen.",
        dnaFlavor: "Du suchst hohe Intensitaet und geschmackliche Komplexitaet.",
        dnaDefault: "Du geniesst eine ausgewogene und komplexe Kaffee-DNA.",
        adnCaption: "Dein Geschmack basiert auf konsumierten Kaffees, Listen/Favoriten und Bewertungen.",
        preferenceAnalysisTitle: "PRÄFERENZANALYSE",
        shouldTry: "Du solltest probieren:",
        suggestedOrigins: "Empfohlene Herkünfte:",
        openPreferenceAnalysis: "Präferenzanalyse öffnen",
        radarAria: "Sensorisches Radarprofil",
        continueExploring: "WEITER ENTDECKEN",
        viewList: (name: string) => `Liste anzeigen ${name}`
      };
    }
    return {
      brandFallback: "Marca",
      me: "Tú",
      ownProfile: "Ver tu perfil",
      userProfile: (name: string) => `Ver perfil de ${name}`,
      dnaNoData: "Aún no hay datos suficientes para calcular tu paladar.",
      dnaNoDataCta: "Consume cafés, añádelos a listas o favoritos y deja reseñas para construir tu ADN sensorial.",
      dnaAcidity: "Buscas brillo en cada taza. Te apasionan los perfiles cítricos y vibrantes de cafés de altura (1500m+).",
      dnaSweetness: "Eres amante de lo meloso. Prefieres cafés con procesos naturales que resaltan notas de chocolate, caramelo y frutas maduras.",
      dnaBody: "Para ti, la textura lo es todo. Disfrutas de esa sensación aterciopelada y densa, típica de tuestes medios y perfiles clásicos.",
      dnaAroma: "Tu ritual empieza con el olfato. Te atraen las fragancias complejas, florales y especiadas que inundan la habitación.",
      dnaFlavor: "Buscas la máxima intensidad y complejidad gustativa. Valoras la persistencia de las notas tras cada sorbo.",
      dnaDefault: "Disfrutas de una complejidad excepcional y un balance perfectamente equilibrado en tu ADN cafetero.",
      adnCaption: "Tus gustos basados en los cafés que consumes, tienes en listas o favoritos y has reseñado.",
      preferenceAnalysisTitle: "ANÁLISIS DE PREFERENCIAS",
      shouldTry: "Deberías probar:",
      suggestedOrigins: "Orígenes sugeridos:",
      openPreferenceAnalysis: "Abrir análisis de preferencia",
      radarAria: "Perfil sensorial radar",
      continueExploring: "CONTINUAR EXPLORANDO",
      viewList: (name: string) => `Ver lista ${name}`
    };
  }, [locale]);
  const [showCreateListModal, setShowCreateListModal] = useState(false);
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
    profileDiaryCoffeeIds.forEach((id) => addSample(coffeesById.get(id)));
    profileListCoffeeIds.forEach((id) => addSample(coffeesById.get(id)));

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
  }, [coffeesById, coffeeReviews, coffeeSensoryProfiles, favoriteCoffees, user.id, profileDiaryCoffeeIds, profileListCoffeeIds]);
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
  const [showEditProfile, setShowEditProfile] = useState(false);
  const [editNameDraft, setEditNameDraft] = useState(user.full_name);
  const [editBioDraft, setEditBioDraft] = useState(user.bio ?? "");
  const [editAvatarFile, setEditAvatarFile] = useState<File | null>(null);
  const [editAvatarPreview, setEditAvatarPreview] = useState("");
  const [removeAvatarDraft, setRemoveAvatarDraft] = useState(false);
  const [savingProfile, setSavingProfile] = useState(false);
  const [togglingFollow, setTogglingFollow] = useState(false);
  const [avatarLoadFailed, setAvatarLoadFailed] = useState(false);
  const [showAdnAnalysisSheet, setShowAdnAnalysisSheet] = useState(false);
  const editAvatarInputRef = useRef<HTMLInputElement | null>(null);
  const profileTabBarRef = useRef<HTMLDivElement>(null);
  const profilePanelsWrapRef = useRef<HTMLDivElement>(null);
  const [profileDragOffsetPx, setProfileDragOffsetPx] = useState<number | null>(null);
  const profileTabDragRefs = useRef({
    startX: 0,
    startY: 0,
    startOffsetPx: 0,
    wrapWidth: 0,
    isDragging: false,
    committed: false,
    lastOffsetPx: 0
  });
  const PROFILE_COMMIT_THRESHOLD_PX = 4;

  const onProfilePanelsTouchStart = useCallback(
    (e: React.TouchEvent) => {
      if (e.touches.length !== 1) return;
      const wrap = profilePanelsWrapRef.current;
      if (!wrap) return;
      const w = wrap.clientWidth;
      const startOffsetPx = tab === "actividad" ? 0 : tab === "adn" ? -w : -2 * w;
      profileTabDragRefs.current.wrapWidth = w;
      profileTabDragRefs.current.startX = e.touches[0].clientX;
      profileTabDragRefs.current.startY = e.touches[0].clientY;
      profileTabDragRefs.current.startOffsetPx = startOffsetPx;
      profileTabDragRefs.current.isDragging = false;
      profileTabDragRefs.current.committed = false;
      profileTabDragRefs.current.lastOffsetPx = startOffsetPx;
    },
    [tab]
  );

  const onProfilePanelsTouchMove = useCallback((e: React.TouchEvent) => {
    if (e.touches.length !== 1) return;
    const r = profileTabDragRefs.current;
    const deltaX = e.touches[0].clientX - r.startX;
    const deltaY = e.touches[0].clientY - r.startY;
    const maxOffset = -2 * r.wrapWidth;
    if (!r.committed) {
      if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > PROFILE_COMMIT_THRESHOLD_PX) {
        r.committed = true;
        r.isDragging = true;
        const next = Math.max(maxOffset, Math.min(0, r.startOffsetPx + deltaX));
        r.lastOffsetPx = next;
        setProfileDragOffsetPx(next);
      } else if (Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > PROFILE_COMMIT_THRESHOLD_PX) {
        r.committed = true;
      }
      return;
    }
    if (!r.isDragging) return;
    const next = Math.max(maxOffset, Math.min(0, r.startOffsetPx + deltaX));
    r.lastOffsetPx = next;
    setProfileDragOffsetPx(next);
  }, []);

  const onProfilePanelsTouchEnd = useCallback((e: React.TouchEvent) => {
    const r = profileTabDragRefs.current;
    if (!r.isDragging) return;
    e.preventDefault();
    e.stopPropagation();
    r.isDragging = false;
    r.committed = false;
    const current = r.lastOffsetPx;
    const w = r.wrapWidth;
    const targetTab =
      current > -w / 2 ? "actividad" : current > -1.5 * w ? "adn" : "favoritos";
    setTab(targetTab);
    setProfileDragOffsetPx(null);
  }, [setTab]);

  useEffect(() => {
    const el = profileTabBarRef.current;
    if (!el) return;
    const onMove = (e: TouchEvent) => {
      if (profileTabDragRefs.current.isDragging && e.cancelable) e.preventDefault();
    };
    el.addEventListener("touchmove", onMove, { passive: false });
    return () => el.removeEventListener("touchmove", onMove);
  }, []);

  const profileTabOffset = tab === "actividad" ? 0 : tab === "adn" ? 33.333 : 66.666;

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
      : profileCopy.dnaNoData;
    const description = highest
      ? highest.label === "Acidez"
        ? profileCopy.dnaAcidity
        : highest.label === "Dulzura"
          ? profileCopy.dnaSweetness
          : highest.label === "Cuerpo"
            ? profileCopy.dnaBody
            : highest.label === "Aroma"
              ? profileCopy.dnaAroma
              : highest.label === "Sabor"
                ? profileCopy.dnaFlavor
                : profileCopy.dnaDefault
      : profileCopy.dnaNoDataCta;
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
              {t("profile.changePhoto")}
            </Button>
          ) : null}
          <div className="profile-head-copy">
            {canEditProfile && showEditProfile ? (
              <div className="profile-inline-edit">
                <section className="diary-edit-entry-metrics-grid profile-inline-metrics">
                  <label className="diary-edit-entry-metric-field is-caffeine profile-inline-name-field">
                    <span>{t("profile.name")}</span>
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
                    <span>{t("profile.bio")}</span>
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
              {togglingFollow ? "..." : isFollowingProfile ? t("profile.following") : t("profile.follow")}
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
              {savingProfile ? t("profile.saving") : t("profile.save")}
            </Button>
          </div>
        ) : null}
      </article>

      <section className="profile-stats-row" aria-label={t("profile.stats")}>
        <button type="button" className="profile-stat-item profile-stat-clickable" onClick={onOpenFollowers} aria-label={t("top.followers")}>
          <strong className="profile-stat-value">{followers}</strong>
          <span className="profile-stat-label">{t("profile.seguidores")}</span>
        </button>
        <button type="button" className="profile-stat-item profile-stat-clickable" onClick={onOpenFollowing} aria-label={t("profile.following")}>
          <strong className="profile-stat-value">{following}</strong>
          <span className="profile-stat-label">{t("profile.following").toUpperCase()}</span>
        </button>
      </section>

      <div className="profile-tab-swipe">
        <div
          ref={profileTabBarRef}
          onTouchStart={onProfilePanelsTouchStart}
          onTouchMove={onProfilePanelsTouchMove}
          onTouchEnd={onProfilePanelsTouchEnd}
          onTouchCancel={onProfilePanelsTouchEnd}
        >
        <Tabs className="profile-tabs" aria-label={t("profile.tabProfile")}>
          <span
            className="tab-sliding-indicator"
            style={{
              ["--indicator-pos" as string]:
                profileDragOffsetPx !== null && profileTabDragRefs.current.wrapWidth > 0
                  ? Math.min(2, Math.max(0, -profileDragOffsetPx / profileTabDragRefs.current.wrapWidth))
                  : tab === "actividad"
                    ? 0
                    : tab === "adn"
                      ? 1
                      : 2,
              transform: "translateX(calc(var(--indicator-pos, 0) * (100% + 6px)))",
              transition: profileDragOffsetPx !== null ? "none" : undefined
            }}
            aria-hidden="true"
          />
          <TabButton active={tab === "actividad"} role="tab" aria-selected={tab === "actividad"} onClick={() => setTab("actividad")}>{t("profile.tabActivity")}</TabButton>
          <TabButton active={tab === "adn"} role="tab" aria-selected={tab === "adn"} onClick={() => setTab("adn")}>{t("profile.tabAdn")}</TabButton>
          <TabButton active={tab === "favoritos"} role="tab" aria-selected={tab === "favoritos"} onClick={() => setTab("favoritos")}>{t("profile.tabLists")}</TabButton>
        </Tabs>
        </div>
        <div ref={profilePanelsWrapRef} className="profile-tab-panels-wrap">
          <div
            className="profile-tab-panels"
            style={{
              transform: profileDragOffsetPx !== null ? `translateX(${profileDragOffsetPx}px)` : `translateX(-${profileTabOffset}%)`,
              transition: profileDragOffsetPx !== null ? "none" : undefined
            }}
          >
            <div className="profile-tab-panel" aria-hidden={tab !== "actividad"}>
              <ul className="profile-activity-list" aria-label={activityIsProfileUser ? t("profile.activityOfThisUser") : t("profile.activityMineAndFollowing")}>
                {followedActivity.length
                  ? followedActivity.map((item) => {
                      const coffee = item.coffeeId != null ? coffeesById.get(item.coffeeId) : null;
                      const isOwnActivity = activeUserId != null && item.userId === activeUserId;
                      const displayName = isOwnActivity ? profileCopy.me : item.userName;
                      const displayLabel = item.type === "review"
                        ? (isOwnActivity ? t("profile.activity.review.own") : t("profile.activity.review.other"))
                        : item.type === "diary"
                          ? (isOwnActivity ? t("profile.activity.diary.own") : t("profile.activity.diary.other"))
                          : item.type === "favorite"
                            ? (isOwnActivity ? t("profile.activity.favorite.own") : t("profile.activity.favorite.other"))
                            : item.label;
                      return (
                      <li key={item.id} className="profile-activity-item">
                        <article className="card profile-activity-card" data-activity-type={item.type}>
                          <Button
                            variant="plain"
                            type="button"
                            className="profile-activity-avatar-link"
                            onClick={() => onOpenUserProfile(item.userId)}
                            aria-label={isOwnActivity ? profileCopy.ownProfile : profileCopy.userProfile(item.userName)}
                          >
                            {item.avatarUrl ? (
                              <img className="avatar avatar-photo profile-activity-avatar" src={item.avatarUrl} alt={`${displayName}`} loading="lazy" decoding="async" referrerPolicy="no-referrer" crossOrigin="anonymous" />
                            ) : (
                              <div className="avatar profile-activity-avatar" aria-hidden="true">{displayName.slice(0, 2).toUpperCase()}</div>
                            )}
                          </Button>
                          <div className="profile-activity-copy">
                            <p className="profile-activity-text">
                              <Button variant="plain" type="button" className="profile-activity-coffee-link profile-activity-user-name-link" onClick={() => onOpenUserProfile(item.userId)} aria-label={isOwnActivity ? profileCopy.ownProfile : profileCopy.userProfile(item.userName)}>
                                <strong>{displayName}</strong>
                              </Button>
                              {" "}
                              {displayLabel}
                            </p>
                            <p className="profile-activity-meta">{toRelativeMinutes(item.timestamp).toUpperCase()}</p>
                            {item.coffeeId != null && coffee ? (
                              <div className="profile-activity-coffee-card">
                                {item.type === "favorite" && item.listId != null && onOpenUserList ? (
                                  <>
                                    <Button
                                      variant="plain"
                                      type="button"
                                      className={`profile-activity-coffee-card-list ${item.listId === "favorites" ? "is-favorites" : ""}`.trim()}
                                      onClick={(e) => { e.stopPropagation(); onOpenUserList(item.userId, item.listId!); }}
                                      aria-label={t("profile.activity.viewListAria", { listName: item.listName ?? t("profile.activity.listFallback") })}
                                    >
                                      {item.listId === "favorites" ? (
                                        <UiIcon name="favorite-filled" className="ui-icon profile-activity-coffee-card-list-icon" aria-hidden="true" />
                                      ) : (
                                        <UiIcon name="list-alt" className="ui-icon profile-activity-coffee-card-list-icon" aria-hidden="true" />
                                      )}
                                      <span className="profile-activity-coffee-card-list-name">{item.listName ?? t("profile.activity.listFallback")}</span>
                                    </Button>
                                    <div className="profile-activity-coffee-card-sep" aria-hidden="true" />
                                  </>
                                ) : null}
                                {item.type === "review" && (item.rating != null || item.comment) ? (
                                  <>
                                    <div className="profile-activity-coffee-card-review-block" aria-label={t("profile.activity.ratingAndReviewAria")}>
                                      {item.rating != null ? <span className="coffee-detail-opinion-rating-chip" aria-label="Nota"><UiIcon name="star-filled" className="ui-icon coffee-detail-opinion-chip-star" />{Math.round(item.rating)}/5</span> : null}
                                      {item.rating != null && item.comment ? " " : null}
                                      {item.comment ? <span className="profile-activity-coffee-card-review-comment">{item.comment}</span> : null}
                                    </div>
                                    <div className="profile-activity-coffee-card-sep" aria-hidden="true" />
                                  </>
                                ) : null}
                                <Button
                                  variant="plain"
                                  type="button"
                                  className="profile-activity-coffee-card-main"
                                  onClick={() => onOpenCoffee(item.coffeeId!)}
                                  aria-label={`Ver detalle de ${coffee.nombre}`}
                                >
                                  <span className="profile-activity-coffee-card-media">
                                    {coffee.image_url ? (
                                      <img className="profile-activity-coffee-card-image" src={coffee.image_url} alt={t("profile.activity.coffeeImageAlt", { coffeeName: coffee.nombre })} loading="lazy" decoding="async" />
                                    ) : (
                                      <span className="profile-activity-coffee-card-image profile-activity-coffee-card-fallback" aria-hidden="true">{(coffee.nombre || "C").slice(0, 1).toUpperCase()}</span>
                                    )}
                                  </span>
                                  <span className="profile-activity-coffee-card-copy">
                                    <strong className="profile-activity-coffee-card-name">{coffee.nombre}</strong>
                                    <em className="profile-activity-coffee-card-brand">{coffee.marca || profileCopy.brandFallback}</em>
                                  </span>
                                  <UiIcon name="chevron-right" className="ui-icon profile-activity-coffee-card-arrow" aria-hidden="true" />
                                </Button>
                              </div>
                            ) : null}
                          </div>
                        </article>
                      </li>
                    ); })
                  : (
                    <li className="profile-activity-empty-wrap">
                      <article className="profile-activity-empty" aria-live="polite">
                        <span className="profile-activity-empty-icon" aria-hidden="true">
                          <UiIcon name="coffee" className="ui-icon" />
                        </span>
                        <h3 className="profile-activity-empty-title">
                          {activityIsProfileUser ? EMPTY.ACTIVITY_PROFILE_EMPTY_TITLE : EMPTY.ACTIVITY_MINE_EMPTY_TITLE}
                        </h3>
                        <p className="profile-activity-empty-sub">
                          {activityIsProfileUser ? EMPTY.ACTIVITY_PROFILE_EMPTY_SUB : EMPTY.ACTIVITY_MINE_EMPTY_SUB}
                        </p>
                        {!activityIsProfileUser && onExploreCafes ? (
                          <Button variant="primary" type="button" className="profile-activity-empty-cta" onClick={onExploreCafes}>
                            {EMPTY.ACTIVITY_CTA_EXPLORE}
                          </Button>
                        ) : null}
                      </article>
                    </li>
                  )}
              </ul>
            </div>
            <div className="profile-tab-panel" aria-hidden={tab !== "adn"}>
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
              <p className="profile-adn-caption">{profileCopy.adnCaption}</p>
            </article>
          ) : (
            <Button variant="plain"
              type="button"
              className="config-card profile-adn-card profile-adn-radar-card profile-adn-open"
              onClick={() => { sendEvent("modal_open", { modal_id: "sensory_detail" }); setShowAdnAnalysisSheet(true); }}
              aria-label={profileCopy.openPreferenceAnalysis}
            >
              <div className="profile-adn-radar-wrap">
                <svg className="profile-adn-radar" viewBox="0 0 240 190" aria-label={profileCopy.radarAria}>
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
              <p className="profile-adn-caption">{profileCopy.adnCaption}</p>
            </Button>
          )}
          {mode === "desktop" ? (
            <article className="config-card profile-adn-analysis-panel">
              <h3 className="profile-adn-analysis-title">{profileCopy.preferenceAnalysisTitle}</h3>
              <div className="profile-adn-analysis-body">
                <p className="profile-adn-analysis-lead">{adnAnalysis.lead}</p>
                <p className="profile-adn-analysis-text">{adnAnalysis.description}</p>
                <article className="profile-adn-recommend-card">
                  <div className="profile-adn-recommend-head">
                    <UiIcon name="sparkles" className="ui-icon" />
                    <strong>{t("brew.recommendation").toUpperCase()}</strong>
                  </div>
                  <p className="profile-adn-recommend-title">{profileCopy.shouldTry} {adnAnalysis.recommendation.type}</p>
                  <p className="profile-adn-recommend-origin">{profileCopy.suggestedOrigins} {adnAnalysis.recommendation.origin}</p>
                </article>
              </div>
            </article>
          ) : null}
        </div>
            </div>
            <div className="profile-tab-panel" aria-hidden={tab !== "favoritos"}>
              <div className="profile-lists-panel">
                {canEditProfile && (
                  <>
                    <button
                      type="button"
                      className="profile-list-row profile-list-row-create"
                      onClick={() => {
                      sendEvent("modal_open", { modal_id: "profile_create_list" });
                      setShowCreateListModal(true);
                    }}
                      aria-label={t("lists.newList")}
                    >
                      <UiIcon name="add" className="ui-icon profile-list-icon" />
                      <span>{t("lists.newList")}</span>
                    </button>
                    <button
                      type="button"
                      className="profile-list-row profile-list-row-favorites"
                      onClick={() => onOpenFavoritesList?.()}
                      aria-label={t("top.favorites")}
                    >
                      <UiIcon name="favorite-filled" className="ui-icon profile-list-icon profile-list-icon-favorite" />
                      <span>{t("top.favorites")}</span>
                      <UiIcon name="chevron-right" className="ui-icon" />
                    </button>
                    {userLists.map((list) => (
                      <button
                        key={list.id}
                        type="button"
                        className="profile-list-row"
                        onClick={() => onOpenList?.(list.id)}
                        aria-label={profileCopy.viewList(list.name)}
                      >
                        <UiIcon name="list-alt" className="ui-icon profile-list-icon" />
                        <span>{list.name}</span>
                        <UiIcon name="chevron-right" className="ui-icon" />
                      </button>
                    ))}
                  </>
                )}
                {!canEditProfile && (
                  <button
                    type="button"
                    className="profile-list-row profile-list-row-favorites"
                    onClick={() => onOpenFavoritesList?.()}
                      aria-label={t("top.favorites")}
                  >
                    <UiIcon name="favorite-filled" className="ui-icon profile-list-icon profile-list-icon-favorite" />
                    <span>{t("top.favorites")}</span>
                    <UiIcon name="chevron-right" className="ui-icon" />
                  </button>
                )}
              </div>
              {showCreateListModal && typeof document !== "undefined" && createPortal(
                <SheetOverlay role="dialog" aria-modal="true" aria-label={t("lists.newList")} onDismiss={() => { sendEvent("modal_close", { modal_id: "profile_create_list" }); setShowCreateListModal(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "profile_create_list" }); setShowCreateListModal(false); }}>
                  <CreateListSheet
                    onDismiss={() => { sendEvent("modal_close", { modal_id: "profile_create_list" }); setShowCreateListModal(false); }}
                    onCreate={(name, privacy) => (onCreateList?.(name, privacy) ?? Promise.resolve()).then(() => { sendEvent("modal_close", { modal_id: "profile_create_list" }); setShowCreateListModal(false); })}
                  />
                </SheetOverlay>,
                document.body
              )}
            </div>
          </div>
        </div>
      </div>

      {showAdnAnalysisSheet && mode !== "desktop" && typeof document !== "undefined"
        ? createPortal(
            <SheetOverlay role="dialog" aria-modal="true" aria-label={t("profile.preferenceAnalysis")} onDismiss={() => { sendEvent("modal_close", { modal_id: "sensory_detail" }); setShowAdnAnalysisSheet(false); }} onClick={() => { sendEvent("modal_close", { modal_id: "sensory_detail" }); setShowAdnAnalysisSheet(false); }}>
              <SheetCard className="profile-adn-analysis-sheet" onClick={(event) => event.stopPropagation()}>
                <SheetHandle aria-hidden="true" />
                <header className="sheet-header">
                  <strong className="sheet-title">{t("profile.preferenceAnalysis").toUpperCase()}</strong>
                </header>
                <div className="profile-adn-analysis-body">
                  <p className="profile-adn-analysis-lead">{adnAnalysis.lead}</p>
                  <p className="profile-adn-analysis-text">{adnAnalysis.description}</p>
                  <article className="profile-adn-recommend-card">
                    <div className="profile-adn-recommend-head">
                      <UiIcon name="sparkles" className="ui-icon" />
                      <strong>{t("brew.recommendation").toUpperCase()}</strong>
                    </div>
                    <p className="profile-adn-recommend-title">{profileCopy.shouldTry} {adnAnalysis.recommendation.type}</p>
                    <p className="profile-adn-recommend-origin">{profileCopy.suggestedOrigins} {adnAnalysis.recommendation.origin}</p>
                  </article>
                  <Button variant="primary"
                    className="action-button profile-adn-continue-button"
                    onClick={() => { sendEvent("modal_close", { modal_id: "sensory_detail" }); setShowAdnAnalysisSheet(false); }}
                  >
                    {profileCopy.continueExploring}
                  </Button>
                </div>
              </SheetCard>
            </SheetOverlay>,
            document.body
          )
        : null}
    </>
  );

  if (sidePanel) {
    return (
      <div className="split-with-side">
        <div className="profile-content-wrap">{content}</div>
        <aside className="home-side-column">{sidePanel}</aside>
      </div>
    );
  }

  return content;
}








