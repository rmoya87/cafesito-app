-- Tour contextual por pestañas (Android + WebApp), sincronizado vía users_db.
-- Ejecutar en Supabase SQL Editor tras revisar políticas UPDATE en la fila propia.

alter table public.users_db
  add column if not exists app_tour_skipped_at bigint null,
  add column if not exists app_tour_dismissed_steps text null;

comment on column public.users_db.app_tour_skipped_at is 'Si no es null, el usuario omitió todo el tour; no mostrar overlays.';
comment on column public.users_db.app_tour_dismissed_steps is 'JSON array de ids de paso ya vistos (p. ej. ["home","search"]).';
