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
