-- ============================================================
-- RLS: UPDATE en users_db solo sobre la fila del usuario autenticado
-- ============================================================
-- Prerrequisito: función public.get_my_internal_id() (ver fix_get_my_internal_id_no_500.sql).
-- Uso: onboarding (onboarding_status, …), updated_at, ciclo de vida de cuenta, etc.
--
-- Ejecutar en Supabase SQL Editor **después** de onboarding_users_db.sql si aún no
-- tenéis una política UPDATE equivalente. Si ya existe una política que permita
-- UPDATE donde id = get_my_internal_id(), no dupliquéis: comprobad con la query del final.
-- ============================================================

ALTER TABLE IF EXISTS public.users_db ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "users_db_update_own_row" ON public.users_db;

CREATE POLICY "users_db_update_own_row"
  ON public.users_db
  FOR UPDATE
  TO authenticated
  USING (id = public.get_my_internal_id())
  WITH CHECK (id = public.get_my_internal_id());

-- Comprobación (opcional): debe aparecer la política y get_my_internal_id() no NULL en sesión app
-- SELECT policyname, cmd, roles
-- FROM pg_policies
-- WHERE schemaname = 'public' AND tablename = 'users_db' AND cmd = 'UPDATE';
