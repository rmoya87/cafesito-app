-- ============================================================
-- FIX DEFINITIVO: 500 en user_lists / user_list_members
-- ============================================================
-- Este script corrige dos causas de 500:
-- 1) get_my_internal_id() no mapea bien auth.uid() -> users_db.id
-- 2) recursión infinita de RLS entre user_lists y user_list_members
-- ============================================================

-- 1) get_my_internal_id() robusta.
-- Ajustada a esquemas donde users_db guarda el UUID de Auth en texto (p. ej. google_id).
CREATE OR REPLACE FUNCTION public.get_my_internal_id()
RETURNS integer
LANGUAGE plpgsql
STABLE
SECURITY INVOKER
SET search_path = public
AS $$
DECLARE
  v_uid uuid;
  v_id  integer;
BEGIN
  v_uid := auth.uid();
  IF v_uid IS NULL THEN
    RETURN NULL;
  END IF;

  -- Si en tu tabla no es google_id, cambia aquí por auth_id/auth_uid/auth_uuid.
  SELECT (u.id)::integer INTO v_id
  FROM public.users_db u
  WHERE u.google_id = v_uid::text
  LIMIT 1;

  RETURN v_id;
EXCEPTION
  WHEN OTHERS THEN
    RETURN NULL;
END;
$$;

-- 2) Helpers SECURITY DEFINER para evitar recursión de políticas.
CREATE OR REPLACE FUNCTION public.is_owner_of_list(p_list_id uuid)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.user_lists ul
    WHERE ul.id = p_list_id
      AND ul.user_id = public.get_my_internal_id()
  );
$$;

CREATE OR REPLACE FUNCTION public.is_member_of_list(p_list_id uuid, p_user_id bigint DEFAULT NULL)
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.user_list_members m
    WHERE m.list_id = p_list_id
      AND m.user_id = COALESCE(p_user_id, public.get_my_internal_id()::bigint)
  );
$$;

REVOKE ALL ON FUNCTION public.is_owner_of_list(uuid) FROM PUBLIC;
REVOKE ALL ON FUNCTION public.is_member_of_list(uuid, bigint) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_owner_of_list(uuid) TO authenticated;
GRANT EXECUTE ON FUNCTION public.is_member_of_list(uuid, bigint) TO authenticated;

-- 3) Políticas sin recursión.
DROP POLICY IF EXISTS "user_lists_select_own" ON public.user_lists;
CREATE POLICY "user_lists_select_own" ON public.user_lists
  FOR SELECT USING (
    get_my_internal_id() IS NOT NULL
    AND (
      user_id = get_my_internal_id()
      OR is_public = true
      OR public.is_member_of_list(id)
    )
  );

DROP POLICY IF EXISTS "user_list_members_select_own" ON public.user_list_members;
CREATE POLICY "user_list_members_select_own" ON public.user_list_members
  FOR SELECT USING (
    get_my_internal_id() IS NOT NULL
    AND (
      user_id = get_my_internal_id()
      OR invited_by = get_my_internal_id()
      OR public.is_owner_of_list(list_id)
    )
  );

DROP POLICY IF EXISTS "user_list_members_insert_by_owner_or_rpc" ON public.user_list_members;
CREATE POLICY "user_list_members_insert_by_owner_or_rpc" ON public.user_list_members
  FOR INSERT WITH CHECK (
    get_my_internal_id() IS NOT NULL
    AND public.is_owner_of_list(list_id)
  );

DROP POLICY IF EXISTS "user_list_members_delete_by_owner" ON public.user_list_members;
CREATE POLICY "user_list_members_delete_by_owner" ON public.user_list_members
  FOR DELETE USING (
    get_my_internal_id() IS NOT NULL
    AND public.is_owner_of_list(list_id)
  );

-- 4) Comprobación rápida (desde app autenticada):
--    SELECT get_my_internal_id() AS my_internal_id;
