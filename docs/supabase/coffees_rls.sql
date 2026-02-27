-- Políticas RLS para la tabla coffees (crear/editar cafés custom)
-- Ejecutar en Supabase SQL Editor si obtienes: "new row violates row-level security policy for table coffees"

-- Asegurar que RLS está activo
ALTER TABLE public.coffees ENABLE ROW LEVEL SECURITY;

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
