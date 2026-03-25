-- Onboarding: estado único por usuario (Android + WebApp).
-- Ejecutar en Supabase SQL editor tras revisar. Ajustar nombre de tabla si difiere.
--
-- Regla legacy (producto, alineado con FUTUROS §3.4.1):
-- - Usuarios que **ya tienen actividad** (diario, despensa, social o publicaciones) pasan a
--   `completed_value` para no tratarlos como “pendientes de primer valor”.
-- - El resto de filas con onboarding NULL pasan a `skipped` (cuenta existente sin datos = no molestar).

-- 1) Columnas
ALTER TABLE public.users_db
  ADD COLUMN IF NOT EXISTS onboarding_status text;

ALTER TABLE public.users_db
  ADD COLUMN IF NOT EXISTS onboarding_completed_at bigint;

ALTER TABLE public.users_db
  ADD COLUMN IF NOT EXISTS onboarding_skipped_at bigint;

-- 2) Migración legacy (solo filas con onboarding_status IS NULL)
-- 2a) Con actividad → completed_value
--     Si alguna tabla no existe en tu proyecto, elimina el OR correspondiente.
UPDATE public.users_db u
SET
  onboarding_status = 'completed_value',
  onboarding_completed_at = COALESCE(u.onboarding_completed_at, (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint),
  onboarding_skipped_at = NULL
WHERE u.onboarding_status IS NULL
  AND (
    EXISTS (SELECT 1 FROM public.diary_entries d WHERE d.user_id = u.id)
    OR EXISTS (SELECT 1 FROM public.pantry_items p WHERE p.user_id = u.id)
    OR EXISTS (SELECT 1 FROM public.follows f WHERE f.follower_id = u.id OR f.followed_id = u.id)
    OR EXISTS (SELECT 1 FROM public.posts_db po WHERE po.user_id = u.id)
    -- Opcional si usáis historial de despensa:
    -- OR EXISTS (SELECT 1 FROM public.pantry_historical ph WHERE ph.user_id = u.id)
  );

-- 2b) Sin actividad pero cuenta ya creada → skipped (no mostrar flujo de onboarding)
UPDATE public.users_db
SET
  onboarding_status = 'skipped',
  onboarding_skipped_at = COALESCE(onboarding_skipped_at, (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint)
WHERE onboarding_status IS NULL;

-- 3) Valores por defecto para filas nuevas
ALTER TABLE public.users_db
  ALTER COLUMN onboarding_status SET DEFAULT 'pending';

-- 4) Check opcional (comentar si hay datos legacy fuera del enum)
-- ALTER TABLE public.users_db DROP CONSTRAINT IF EXISTS users_db_onboarding_status_check;
-- ALTER TABLE public.users_db ADD CONSTRAINT users_db_onboarding_status_check
--   CHECK (onboarding_status IN ('pending', 'completed_value', 'skipped'));

-- 5) RLS: política UPDATE para la fila propia (onboarding + otros updates vía PostgREST).
--    Ejecutar en Supabase: docs/supabase/users_db_update_own_row.sql
--    (requiere get_my_internal_id(); ver fix_get_my_internal_id_no_500.sql).
