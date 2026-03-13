/**
 * Textos unificados para estados vacío y error (Web + Android).
 * Ver docs/UX_EMPTY_AND_ERROR_STATES.md.
 */

export const EMPTY = {
  DIARY_NO_ENTRIES: "Sin café o agua registrada",
  DIARY_NO_PANTRY: "No hay café en tu despensa",
  TIMELINE_TITLE: "Tu timeline está vacío",
  TIMELINE_SUBTITLE: "Empieza siguiendo personas o publicando tu primer café.",
  TIMELINE_NO_POSTS: "No hay publicaciones disponibles",
  TIMELINE_NO_POSTS_SUB: "Publica tu primer café o sigue a más personas.",
  COMMENTS: "No hay comentarios todavía",
  FAVORITES: "No hay cafés favoritos",
  PROFILE_NO_POSTS: "Aún no hay publicaciones",
  BREW_NO_MATCHES: "No hay coincidencias en tu despensa.",
  BREW_EMPTY_PANTRY: "Tu despensa está vacía.",
  BREW_NO_SUGGESTIONS: "No hay sugerencias disponibles.",
  OPINIONS: "No hay opiniones aún. ¡Sé el primero!",
  NOTIFICATIONS: "No tienes notificaciones",
  /** Tab Actividad perfil: cuando es el perfil de otro usuario y no tiene actividad */
  ACTIVITY_PROFILE_EMPTY_TITLE: "Sin actividad reciente",
  ACTIVITY_PROFILE_EMPTY_SUB: "Este usuario aún no ha publicado reseñas ni añadido cafés.",
  /** Tab Actividad perfil: cuando es tu perfil y no sigues a nadie / no hay actividad */
  ACTIVITY_MINE_EMPTY_TITLE: "Tu actividad está vacía",
  ACTIVITY_MINE_EMPTY_SUB: "Sigue a otras personas para ver aquí sus reseñas, favoritos y cafés probados.",
  ACTIVITY_CTA_EXPLORE: "Explorar cafés",
} as const;

export const ERROR = {
  LOAD_DATA: "No se han podido cargar los datos.",
  LOAD_NOTIFICATIONS: "No se han podido cargar las notificaciones.",
  RETRY: "Reintentar",
} as const;

export const CTA = {
  PUBLISH_FIRST: "Publica tu primer café",
  ADD_ENTRY: "Añadir entrada",
} as const;
