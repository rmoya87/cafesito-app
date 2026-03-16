-- ==========================================================
-- LISTAS COMPARTIDAS E INVITACIONES
-- ==========================================================
-- Ejecutar después de user_lists.sql y notifications.sql.
-- Requiere: get_my_internal_id(), create_notification(), users_db.

-- Helpers para evitar recursión de RLS entre user_lists y user_list_members.
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

-- 1) Tabla: miembros de listas (quién puede ver una lista además del dueño)
CREATE TABLE IF NOT EXISTS public.user_list_members (
  list_id uuid NOT NULL REFERENCES public.user_lists(id) ON DELETE CASCADE,
  user_id bigint NOT NULL,
  role text NOT NULL DEFAULT 'viewer' CHECK (role IN ('viewer', 'editor')),
  invited_by bigint,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY (list_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_user_list_members_user_id ON public.user_list_members(user_id);

ALTER TABLE public.user_list_members ENABLE ROW LEVEL SECURITY;

-- Solo el dueño puede insertar/borrar miembros (o RPC join_public_list). Usuario puede verse a sí mismo.
DROP POLICY IF EXISTS "user_list_members_select_own" ON public.user_list_members;
CREATE POLICY "user_list_members_select_own" ON public.user_list_members
  FOR SELECT USING (
    get_my_internal_id() IS NOT NULL
    AND (user_id = get_my_internal_id()
         OR invited_by = get_my_internal_id()
         OR public.is_owner_of_list(list_id))
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

-- 2) Tabla: invitaciones pendientes
CREATE TABLE IF NOT EXISTS public.user_list_invitations (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  list_id uuid NOT NULL REFERENCES public.user_lists(id) ON DELETE CASCADE,
  inviter_id bigint NOT NULL,
  invitee_id bigint NOT NULL,
  status text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(list_id, invitee_id)
);

CREATE INDEX IF NOT EXISTS idx_user_list_invitations_invitee ON public.user_list_invitations(invitee_id);
CREATE INDEX IF NOT EXISTS idx_user_list_invitations_list ON public.user_list_invitations(list_id);

ALTER TABLE public.user_list_invitations ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "user_list_invitations_select_inviter_or_invitee" ON public.user_list_invitations;
CREATE POLICY "user_list_invitations_select_inviter_or_invitee" ON public.user_list_invitations
  FOR SELECT USING (
    get_my_internal_id() IS NOT NULL
    AND (inviter_id = get_my_internal_id() OR invitee_id = get_my_internal_id())
  );

DROP POLICY IF EXISTS "user_list_invitations_insert_by_owner" ON public.user_list_invitations;
CREATE POLICY "user_list_invitations_insert_by_owner" ON public.user_list_invitations
  FOR INSERT WITH CHECK (
    get_my_internal_id() IS NOT NULL
    AND inviter_id = get_my_internal_id()
    AND EXISTS (SELECT 1 FROM public.user_lists ul WHERE ul.id = list_id AND ul.user_id = get_my_internal_id())
  );

DROP POLICY IF EXISTS "user_list_invitations_update_by_invitee" ON public.user_list_invitations;
CREATE POLICY "user_list_invitations_update_by_invitee" ON public.user_list_invitations
  FOR UPDATE USING (get_my_internal_id() IS NOT NULL AND invitee_id = get_my_internal_id());

-- 3) Ampliar SELECT de user_lists: también si soy miembro
DROP POLICY IF EXISTS "user_lists_select_own" ON public.user_lists;
CREATE POLICY "user_lists_select_own" ON public.user_lists
  FOR SELECT USING (
    get_my_internal_id() IS NOT NULL
    AND (user_id = get_my_internal_id()
         OR is_public = true
         OR public.is_member_of_list(id))
  );

-- Quitar la política select_public duplicada (ya cubierta arriba con is_public)
DROP POLICY IF EXISTS "user_lists_select_public" ON public.user_lists;

-- 4) Ampliar SELECT de user_list_items: también si soy miembro de la lista
DROP POLICY IF EXISTS "user_list_items_select" ON public.user_list_items;
CREATE POLICY "user_list_items_select" ON public.user_list_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.user_lists ul
      LEFT JOIN public.user_list_members m ON m.list_id = ul.id AND m.user_id = get_my_internal_id()
      WHERE ul.id = user_list_items.list_id
        AND (ul.user_id = get_my_internal_id()
             OR ul.is_public = true
             OR m.user_id IS NOT NULL)
    )
  );

-- 5) RPC: crear invitación y notificación (solo dueño de la lista)
CREATE OR REPLACE FUNCTION public.create_list_invitation(p_list_id uuid, p_invitee_id bigint)
RETURNS uuid
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_me bigint;
  v_invitation_id uuid;
  v_list_name text;
  v_inviter_username text;
  v_timestamp bigint;
BEGIN
  v_me := get_my_internal_id();
  IF v_me IS NULL THEN
    RAISE EXCEPTION 'No autenticado';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.user_lists WHERE id = p_list_id AND user_id = v_me) THEN
    RAISE EXCEPTION 'No eres el dueño de la lista';
  END IF;

  IF p_invitee_id = v_me THEN
    RAISE EXCEPTION 'No puedes invitarte a ti mismo';
  END IF;

  SELECT name INTO v_list_name FROM public.user_lists WHERE id = p_list_id;
  SELECT username INTO v_inviter_username FROM public.users_db WHERE id = v_me LIMIT 1;

  INSERT INTO public.user_list_invitations (list_id, inviter_id, invitee_id, status)
  VALUES (p_list_id, v_me, p_invitee_id, 'pending')
  ON CONFLICT (list_id, invitee_id) DO UPDATE SET status = 'pending', created_at = now()
  RETURNING id INTO v_invitation_id;

  v_timestamp := (EXTRACT(EPOCH FROM now()) * 1000)::bigint;
  PERFORM create_notification(
    p_invitee_id,
    'LIST_INVITE',
    COALESCE(v_inviter_username, 'Usuario'),
    'Te ha invitado a la lista «' || COALESCE(v_list_name, '') || '»',
    v_timestamp,
    v_invitation_id::text
  );

  RETURN v_invitation_id;
END;
$$;

GRANT EXECUTE ON FUNCTION public.create_list_invitation(uuid, bigint) TO authenticated;

-- 6) RPC: aceptar invitación (solo el invitee)
CREATE OR REPLACE FUNCTION public.accept_list_invitation(p_invitation_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_me bigint;
  v_inv record;
BEGIN
  v_me := get_my_internal_id();
  IF v_me IS NULL THEN
    RAISE EXCEPTION 'No autenticado';
  END IF;

  SELECT list_id, inviter_id, invitee_id, status INTO v_inv
  FROM public.user_list_invitations WHERE id = p_invitation_id;

  IF v_inv.invitee_id IS NULL OR v_inv.invitee_id != v_me THEN
    RAISE EXCEPTION 'Invitación no encontrada o no eres el invitado';
  END IF;

  IF v_inv.status != 'pending' THEN
    RAISE EXCEPTION 'La invitación ya no está pendiente';
  END IF;

  UPDATE public.user_list_invitations SET status = 'accepted' WHERE id = p_invitation_id;

  INSERT INTO public.user_list_members (list_id, user_id, role, invited_by)
  VALUES (v_inv.list_id, v_me, 'viewer', v_inv.inviter_id)
  ON CONFLICT (list_id, user_id) DO NOTHING;
END;
$$;

GRANT EXECUTE ON FUNCTION public.accept_list_invitation(uuid) TO authenticated;

-- 7) RPC: rechazar invitación (solo el invitee)
CREATE OR REPLACE FUNCTION public.decline_list_invitation(p_invitation_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_me bigint;
BEGIN
  v_me := get_my_internal_id();
  IF v_me IS NULL THEN
    RAISE EXCEPTION 'No autenticado';
  END IF;

  UPDATE public.user_list_invitations
  SET status = 'declined'
  WHERE id = p_invitation_id AND invitee_id = v_me AND status = 'pending';
END;
$$;

GRANT EXECUTE ON FUNCTION public.decline_list_invitation(uuid) TO authenticated;

-- 8) RPC: unirse a lista pública (cualquier usuario autenticado)
CREATE OR REPLACE FUNCTION public.join_public_list(p_list_id uuid)
RETURNS void
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_me bigint;
BEGIN
  v_me := get_my_internal_id();
  IF v_me IS NULL THEN
    RAISE EXCEPTION 'No autenticado';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM public.user_lists WHERE id = p_list_id AND is_public = true) THEN
    RAISE EXCEPTION 'La lista no existe o no es pública';
  END IF;

  INSERT INTO public.user_list_members (list_id, user_id, role, invited_by)
  VALUES (p_list_id, v_me, 'viewer', NULL)
  ON CONFLICT (list_id, user_id) DO NOTHING;
END;
$$;

GRANT EXECUTE ON FUNCTION public.join_public_list(uuid) TO authenticated;

-- Nota borrado de cuenta: en hard_delete_user_data(p_user_id) añadir:
--   DELETE FROM public.user_list_invitations WHERE inviter_id = p_user_id OR invitee_id = p_user_id;
--   DELETE FROM public.user_list_members WHERE user_id = p_user_id OR invited_by = p_user_id;
