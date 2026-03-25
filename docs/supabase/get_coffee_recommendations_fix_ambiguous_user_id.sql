-- =============================================================================
-- Fix: RPC get_coffee_recommendations → error PostgreSQL
--   "column reference user_id is ambiguous"
-- =============================================================================
-- Causa típica: JOIN entre tablas que tienen columna user_id (p. ej. coffees,
-- local_favorites, pantry_items, diary_entries) sin prefijo de tabla/alias.
--
-- Opción A (recomendada si quieres conservar la lógica actual):
--   En Supabase → SQL Editor, ejecuta:
--     SELECT pg_get_functiondef('public.get_coffee_recommendations(integer)'::regprocedure);
--   En el resultado, cualifica cada user_id: p. ej. lf.user_id, c.user_id, pi.user_id.
--
-- Opción B: sustituir la función por esta implementación (equivalente funcional
-- simple: cafés públicos no probados aún en favoritos/despensa/diario del usuario).
-- Revisa que los nombres de tablas/columnas coincidan con tu proyecto.
--
-- Tras aplicar, ejecutar en la consola de la WebApp:
--   fetchCoffeeRecommendationsRpc(...)  o abrir Home en la app.
-- =============================================================================

-- Paso 0 (opcional): inspeccionar firma/retorno actuales
-- SELECT
--   p.proname,
--   pg_catalog.pg_get_function_result(p.oid) AS return_type,
--   pg_catalog.pg_get_function_arguments(p.oid) AS args
-- FROM pg_proc p
-- JOIN pg_namespace n ON n.oid = p.pronamespace
-- WHERE n.nspname = 'public' AND p.proname = 'get_coffee_recommendations';

-- Paso 1: eliminar la función actual para permitir cambio de tipo de retorno
DROP FUNCTION IF EXISTS public.get_coffee_recommendations(integer);

CREATE OR REPLACE FUNCTION public.get_coffee_recommendations(target_user_id integer)
RETURNS SETOF public.coffees
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  -- Solo el usuario autenticado puede pedir sus propias recomendaciones.
  IF target_user_id IS NULL OR target_user_id IS DISTINCT FROM public.get_my_internal_id() THEN
    RAISE EXCEPTION 'forbidden';
  END IF;

  RETURN QUERY
  SELECT c.*
  FROM public.coffees c
  WHERE COALESCE(c.is_custom, false) = false
    AND NOT EXISTS (
      SELECT 1
      FROM public.local_favorites lf
      WHERE lf.coffee_id = c.id
        AND lf.user_id = target_user_id
    )
    AND NOT EXISTS (
      SELECT 1
      FROM public.pantry_items pi
      WHERE pi.coffee_id = c.id
        AND pi.user_id = target_user_id
    )
    AND NOT EXISTS (
      SELECT 1
      FROM public.diary_entries de
      WHERE de.user_id = target_user_id
        AND de.coffee_id IS NOT NULL
        AND de.coffee_id = c.id::text
    )
  ORDER BY c.puntuacion_total DESC NULLS LAST, c.nombre ASC
  LIMIT 50;
END;
$$;

-- Permisos (ajusta si tu proyecto ya los tiene)
ALTER FUNCTION public.get_coffee_recommendations(integer) OWNER TO postgres;
GRANT EXECUTE ON FUNCTION public.get_coffee_recommendations(integer) TO authenticated;
GRANT EXECUTE ON FUNCTION public.get_coffee_recommendations(integer) TO service_role;
