-- ============================================================
-- Permitir leer perfiles públicos (avatar, username) de users_db
-- ============================================================
-- Necesario para que la app muestre avatares en:
-- - Gestionar invitados (modal Compartir lista)
-- - TopBar de lista (contador + avatares de miembros)
--
-- Sin esta política, fetchUserById(id) devuelve null para otros
-- usuarios por RLS y los avatares no se cargan.
-- ============================================================

-- Habilitar RLS si no está (no quita políticas existentes).
ALTER TABLE IF EXISTS public.users_db ENABLE ROW LEVEL SECURITY;

-- Política: cualquier usuario autenticado puede leer perfil público de cualquier fila.
-- Con varias políticas SELECT, RLS hace OR: si alguna permite la fila, se devuelve.
-- Sin esta política (o una equivalente), fetchUserById/fetchUsersByIds devuelven
-- vacío para otros usuarios y no se ven avatares en Gestionar invitados ni en la TopBar.
DROP POLICY IF EXISTS "users_db_select_public_profiles" ON public.users_db;
CREATE POLICY "users_db_select_public_profiles"
  ON public.users_db
  FOR SELECT
  TO authenticated
  USING (true);
