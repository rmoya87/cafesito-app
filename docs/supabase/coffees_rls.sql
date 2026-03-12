-- Políticas RLS para la tabla coffees (crear/editar cafés custom)
-- Ejecutar en Supabase SQL Editor si obtienes: "new row violates row-level security policy for table coffees"
-- Requiere get_my_internal_id() que devuelve el id numérico (users_db) del usuario autenticado.

-- Asegurar que RLS está activo
ALTER TABLE public.coffees ENABLE ROW LEVEL SECURITY;

-- Permitir SELECT: cafés públicos (is_custom = false) o cafés custom del usuario (user_id = get_my_internal_id())
-- Sin esta política, Android/app no verá los custom en buscador ni despensa.
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'coffees'
      AND policyname = 'Coffees are readable by all or owner'
  ) THEN
    CREATE POLICY "Coffees are readable by all or owner"
      ON public.coffees
      FOR SELECT
      TO authenticated
      USING (
        (is_custom = false)
        OR (is_custom = true AND user_id = get_my_internal_id())
      );
  END IF;
END$$;

-- Permitir INSERT a usuarios autenticados (crear café custom)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'coffees'
      AND policyname = 'Coffees are insertable by authenticated users'
  ) THEN
    CREATE POLICY "Coffees are insertable by authenticated users"
      ON public.coffees
      FOR INSERT
      TO authenticated
      WITH CHECK (true);
  END IF;
END$$;

-- Permitir UPDATE a usuarios autenticados (editar café custom; opcional si ya puedes editar)
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'coffees'
      AND policyname = 'Coffees are updatable by authenticated users'
  ) THEN
    CREATE POLICY "Coffees are updatable by authenticated users"
      ON public.coffees
      FOR UPDATE
      TO authenticated
      USING (true)
      WITH CHECK (true);
  END IF;
END$$;

-- Si un café custom se ve en webapp pero no en Android: la sesión de Android debe ser la misma
-- (mismo usuario). Comprueba que get_my_internal_id() devuelve el id de users_db para el JWT de la app.
-- En Android, logcat "Coffee <id> not returned by Supabase" indica que RLS no devolvió esa fila.

-- OPCIONAL: Permitir que usuarios autenticados vean TODOS los cafés de la tabla (públicos + custom de cualquier usuario).
-- Ejecutar solo si quieres que la app Android muestre toda la tabla coffees sin depender de get_my_internal_id().
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'coffees'
      AND policyname = 'Coffees readable by authenticated (all rows)'
  ) THEN
    CREATE POLICY "Coffees readable by authenticated (all rows)"
      ON public.coffees
      FOR SELECT
      TO authenticated
      USING (true);
  END IF;
END$$;
