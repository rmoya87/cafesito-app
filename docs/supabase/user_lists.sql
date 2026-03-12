-- Listas de usuario (listas personalizadas además de la lista por defecto "Favoritos" que sigue en local_favorites)
-- Ejecutar en el SQL Editor de Supabase.
--
-- IMPORTANTE: Eliminar una lista (o una entrada de diary_entries) NO debe borrar ítems de pantry_items.
-- La despensa es independiente. Ver docs/supabase/pantry_independent_of_diary_and_lists.md.

-- Tabla de listas: cada usuario puede crear listas con nombre y visibilidad
-- user_id debe coincidir con el id de tu tabla de usuarios (p. ej. users_db o profiles).
CREATE TABLE IF NOT EXISTS public.user_lists (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id bigint NOT NULL,  -- REFERENCES public.users_db(id) ON DELETE CASCADE si aplica
  name text NOT NULL,
  is_public boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(user_id, name)
);

-- Índices para consultas habituales
CREATE INDEX IF NOT EXISTS idx_user_lists_user_id ON public.user_lists(user_id);
CREATE INDEX IF NOT EXISTS idx_user_lists_user_public ON public.user_lists(user_id, is_public) WHERE is_public = true;

-- Tabla de ítems: cafés en cada lista
CREATE TABLE IF NOT EXISTS public.user_list_items (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  list_id uuid NOT NULL REFERENCES public.user_lists(id) ON DELETE CASCADE,
  coffee_id text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(list_id, coffee_id)
);

CREATE INDEX IF NOT EXISTS idx_user_list_items_list_id ON public.user_list_items(list_id);
CREATE INDEX IF NOT EXISTS idx_user_list_items_coffee_id ON public.user_list_items(coffee_id);

-- RLS: usa get_my_internal_id() si tu proyecto lo tiene (devuelve el user_id numérico del usuario autenticado).
-- Si no existe, crea una función que mapee auth.uid() al id de tu tabla users_db/profiles.
ALTER TABLE public.user_lists ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_list_items ENABLE ROW LEVEL SECURITY;

-- Políticas user_lists (con get_my_internal_id)
DROP POLICY IF EXISTS "user_lists_select_own" ON public.user_lists;
CREATE POLICY "user_lists_select_own" ON public.user_lists
  FOR SELECT USING (get_my_internal_id() IS NOT NULL AND user_id = get_my_internal_id());

DROP POLICY IF EXISTS "user_lists_select_public" ON public.user_lists;
CREATE POLICY "user_lists_select_public" ON public.user_lists
  FOR SELECT USING (is_public = true);

DROP POLICY IF EXISTS "user_lists_insert_own" ON public.user_lists;
CREATE POLICY "user_lists_insert_own" ON public.user_lists
  FOR INSERT WITH CHECK (get_my_internal_id() IS NOT NULL AND user_id = get_my_internal_id());

DROP POLICY IF EXISTS "user_lists_update_own" ON public.user_lists;
CREATE POLICY "user_lists_update_own" ON public.user_lists
  FOR UPDATE USING (get_my_internal_id() IS NOT NULL AND user_id = get_my_internal_id());

DROP POLICY IF EXISTS "user_lists_delete_own" ON public.user_lists;
CREATE POLICY "user_lists_delete_own" ON public.user_lists
  FOR DELETE USING (get_my_internal_id() IS NOT NULL AND user_id = get_my_internal_id());

-- Políticas user_list_items
DROP POLICY IF EXISTS "user_list_items_select" ON public.user_list_items;
CREATE POLICY "user_list_items_select" ON public.user_list_items
  FOR SELECT USING (
    EXISTS (
      SELECT 1 FROM public.user_lists ul
      WHERE ul.id = user_list_items.list_id
        AND (ul.user_id = get_my_internal_id() OR ul.is_public = true)
    )
  );

DROP POLICY IF EXISTS "user_list_items_insert" ON public.user_list_items;
CREATE POLICY "user_list_items_insert" ON public.user_list_items
  FOR INSERT WITH CHECK (
    EXISTS (
      SELECT 1 FROM public.user_lists ul
      WHERE ul.id = user_list_items.list_id AND ul.user_id = get_my_internal_id()
    )
  );

DROP POLICY IF EXISTS "user_list_items_delete" ON public.user_list_items;
CREATE POLICY "user_list_items_delete" ON public.user_list_items
  FOR DELETE USING (
    EXISTS (
      SELECT 1 FROM public.user_lists ul
      WHERE ul.id = user_list_items.list_id AND ul.user_id = get_my_internal_id()
    )
  );

-- Nota: La lista por defecto "Favoritos" sigue siendo local_favorites (user_id + coffee_id).
-- Las listas personalizadas se gestionan con user_lists + user_list_items.
-- En la app: al mostrar "Listas", la primera fila es "Favoritos" (datos de local_favorites);
-- el resto son filas de user_lists del usuario.
--
-- Borrado de cuenta: si tienes una función hard_delete_user_data(p_user_id bigint), añade dentro de ella:
--
--   DELETE FROM public.user_list_items WHERE list_id IN (SELECT id FROM public.user_lists WHERE user_id = p_user_id);
--   DELETE FROM public.user_lists WHERE user_id = p_user_id;
--
-- (p_user_id es el parámetro de esa función; no ejecutes estos DELETE sueltos o dará error "column p_user_id does not exist")
