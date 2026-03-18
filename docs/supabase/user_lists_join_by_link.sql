-- Listas compartidas por enlace: obtener info para mostrar "Unirse a la lista" y unirse sin invitación previa.
-- Ejecutar después de user_list_members_and_invitations.sql y user_lists_privacy_column.sql.
-- Permite que cualquier usuario autenticado con el enlace pueda ver nombre/dueño y unirse si la lista es pública o por invitación.

-- Devuelve nombre y user_id del dueño solo para listas que permiten unirse por enlace (pública o invitation).
CREATE OR REPLACE FUNCTION public.get_list_info_for_join(p_list_id uuid)
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_me bigint;
  v_row record;
BEGIN
  v_me := get_my_internal_id();
  IF v_me IS NULL THEN
    RETURN NULL;
  END IF;

  SELECT ul.name, ul.user_id
  INTO v_row
  FROM public.user_lists ul
  WHERE ul.id = p_list_id
    AND (ul.is_public = true OR ul.privacy = 'invitation');

  IF v_row IS NULL THEN
    RETURN NULL;
  END IF;

  RETURN jsonb_build_object('name', v_row.name, 'user_id', v_row.user_id);
END;
$$;

GRANT EXECUTE ON FUNCTION public.get_list_info_for_join(uuid) TO authenticated;

-- Une al usuario actual a la lista por enlace (lista pública o por invitación). Idempotente (ON CONFLICT DO NOTHING).
CREATE OR REPLACE FUNCTION public.join_list_by_link(p_list_id uuid)
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

  IF NOT EXISTS (
    SELECT 1 FROM public.user_lists
    WHERE id = p_list_id AND (is_public = true OR privacy = 'invitation')
  ) THEN
    RAISE EXCEPTION 'La lista no existe o no permite unirse por enlace';
  END IF;

  INSERT INTO public.user_list_members (list_id, user_id, role, invited_by)
  VALUES (p_list_id, v_me, 'viewer', NULL)
  ON CONFLICT (list_id, user_id) DO NOTHING;
END;
$$;

GRANT EXECUTE ON FUNCTION public.join_list_by_link(uuid) TO authenticated;
