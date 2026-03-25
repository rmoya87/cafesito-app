-- ============================================================
-- Reparación one-off: skipped → completed_value si hay actividad
-- ============================================================
-- Úsalo solo si ya ejecutaste una versión antigua de onboarding_users_db.sql
-- que ponía **todos** los legacy en `skipped` y quieres alinear con la regla
-- “quien ya tenía datos = completed_value”.
--
-- Idempotente en la práctica: solo afecta filas con onboarding_status = 'skipped'
-- que cumplen criterios de actividad (misma taxonomía que onboarding_users_db.sql §2a).
-- ============================================================

UPDATE public.users_db u
SET
  onboarding_status = 'completed_value',
  onboarding_completed_at = COALESCE(u.onboarding_completed_at, (EXTRACT(EPOCH FROM NOW()) * 1000)::bigint),
  onboarding_skipped_at = NULL
WHERE u.onboarding_status = 'skipped'
  AND (
    EXISTS (SELECT 1 FROM public.diary_entries d WHERE d.user_id = u.id)
    OR EXISTS (SELECT 1 FROM public.pantry_items p WHERE p.user_id = u.id)
    OR EXISTS (SELECT 1 FROM public.follows f WHERE f.follower_id = u.id OR f.followed_id = u.id)
    OR EXISTS (SELECT 1 FROM public.posts_db po WHERE po.user_id = u.id)
    -- OR EXISTS (SELECT 1 FROM public.pantry_historical ph WHERE ph.user_id = u.id)
  );
