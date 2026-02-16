-- ==========================================================
-- COMMENTS - utilidades RPC para operaciones seguras con RLS
-- ==========================================================
-- Ejecuta este script en Supabase SQL Editor.
--
-- Contexto:
-- En la app se usa user_id numérico propio; en Supabase Auth auth.uid() es UUID.
-- Si las políticas RLS de comments_db comparan auth.uid() con user_id,
-- el borrado directo puede fallar silenciosamente para cliente autenticado.
--
-- Esta RPC evita desincronizaciones ("yo lo veo borrado, otros no")
-- al ejecutar DELETE como SECURITY DEFINER.
-- ==========================================================

create or replace function public.delete_comment(
    p_comment_id bigint
) returns void
language plpgsql
security definer
set search_path = public
as $$
begin
    delete from public.comments_db
    where id = p_comment_id;
end;
$$;

grant execute on function public.delete_comment(bigint) to authenticated;
