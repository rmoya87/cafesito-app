-- ============================================================
-- Columna pantry_item_id en diary_entries
-- ============================================================
-- Permite asociar una entrada de actividad al ítem de despensa
-- del que se restó stock. Al eliminar la actividad, la app puede
-- restaurar el stock en ese ítem concreto (varios ítems pueden
-- ser del mismo café; se distinguen por id de despensa).
-- Ejecutar en SQL Editor del proyecto Supabase.
-- ============================================================

ALTER TABLE public.diary_entries
  ADD COLUMN IF NOT EXISTS pantry_item_id uuid REFERENCES public.pantry_items(id) ON DELETE SET NULL;

COMMENT ON COLUMN public.diary_entries.pantry_item_id IS 'ID del ítem de despensa del que se restó stock al crear esta actividad; para restaurar al eliminar.';
