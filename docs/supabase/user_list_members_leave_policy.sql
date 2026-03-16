-- Permite que un usuario abandone una lista (elimine su propia fila en user_list_members).
-- Sin esta política, solo el dueño puede borrar filas; con ella, el miembro puede borrarse a sí mismo.
-- Ejecutar después de user_list_members_and_invitations.sql.

DROP POLICY IF EXISTS "user_list_members_delete_by_member_self" ON public.user_list_members;
CREATE POLICY "user_list_members_delete_by_member_self" ON public.user_list_members
  FOR DELETE USING (
    get_my_internal_id() IS NOT NULL
    AND user_id = get_my_internal_id()
  );
