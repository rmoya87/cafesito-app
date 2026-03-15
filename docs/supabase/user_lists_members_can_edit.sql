-- Columna para permitir que los miembros editen la lista (añadir/quitar cafés).
-- Si es false o no existe, solo el dueño puede modificar items. Si es true, cualquier miembro puede.
-- Ejecutar después de user_lists_privacy_column.sql.

ALTER TABLE public.user_lists
  ADD COLUMN IF NOT EXISTS members_can_edit boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN public.user_lists.members_can_edit IS 'Si true, los miembros (user_list_members) pueden insertar/borrar en user_list_items. Si false, solo el dueño.';

-- Opcional: ampliar RLS de user_list_items para permitir insert/delete a miembros cuando members_can_edit = true.
-- Si no ejecutas lo siguiente, solo el dueño podrá añadir/quitar cafés (el backend rechazará a miembros).
-- Requiere que existan las políticas user_list_items_insert y user_list_items_delete actuales (solo dueño).

-- Política de INSERT: dueño O (miembro Y list.members_can_edit)
DROP POLICY IF EXISTS "user_list_items_insert" ON public.user_list_items;
CREATE POLICY "user_list_items_insert" ON public.user_list_items
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.user_lists ul
      WHERE ul.id = user_list_items.list_id
        AND (
          ul.user_id = get_my_internal_id()
          OR (ul.members_can_edit = true AND EXISTS (
            SELECT 1 FROM public.user_list_members m
            WHERE m.list_id = ul.id AND m.user_id = get_my_internal_id()
          ))
        )
    )
  );

-- Política de DELETE: dueño O (miembro Y list.members_can_edit)
DROP POLICY IF EXISTS "user_list_items_delete" ON public.user_list_items;
CREATE POLICY "user_list_items_delete" ON public.user_list_items
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.user_lists ul
      WHERE ul.id = user_list_items.list_id
        AND (
          ul.user_id = get_my_internal_id()
          OR (ul.members_can_edit = true AND EXISTS (
            SELECT 1 FROM public.user_list_members m
            WHERE m.list_id = ul.id AND m.user_id = get_my_internal_id()
          ))
        )
    )
  );
