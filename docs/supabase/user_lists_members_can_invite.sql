-- Columna para permitir que los miembros inviten a otras personas (solo listas por invitación).
-- Si es false o no existe, solo el dueño puede invitar. Si es true, cualquier miembro puede invitar.
-- Ejecutar después de user_lists_members_can_edit.sql.

ALTER TABLE public.user_lists
  ADD COLUMN IF NOT EXISTS members_can_invite boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN public.user_lists.members_can_invite IS 'Si true (solo aplica cuando privacy = invitation), los miembros pueden crear invitaciones (buscar y copiar enlace). Si false, solo el dueño.';

-- Actualizar la RPC create_list_invitation para permitir a miembros invitar cuando members_can_invite = true.
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

  -- Dueño puede siempre; miembro solo si la lista tiene members_can_invite = true (y privacy = invitation).
  IF NOT EXISTS (
    SELECT 1 FROM public.user_lists ul
    WHERE ul.id = p_list_id
      AND (
        ul.user_id = v_me
        OR (
          ul.members_can_invite = true
          AND EXISTS (SELECT 1 FROM public.user_list_members m WHERE m.list_id = ul.id AND m.user_id = v_me)
        )
      )
  ) THEN
    RAISE EXCEPTION 'No puedes invitar a esta lista';
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
