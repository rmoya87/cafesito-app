-- Tabla pantry_historical: fuente de verdad para "café terminado" (historial desde despensa).
-- Coordinada entre webapp y Android; mismo esquema que pantry_items (user_id, coffee_id) + finished_at.
-- Ejecutar en el proyecto Supabase (SQL Editor o migración).
-- Al eliminar cuenta: añadir delete de pantry_historical en hard_delete_user_data (ver account_deletion_lifecycle.sql).

-- Tabla
create table if not exists public.pantry_historical (
  id bigint generated always as identity primary key,
  user_id integer not null references public.users_db(id) on delete cascade,
  coffee_id text not null,
  finished_at bigint not null default (extract(epoch from now()) * 1000)::bigint
);

create index if not exists idx_pantry_historical_user_id on public.pantry_historical(user_id);
create index if not exists idx_pantry_historical_finished_at on public.pantry_historical(finished_at desc);

comment on table public.pantry_historical is 'Cafés marcados como terminados desde la despensa (historial). Sincronizado con web y Android.';

-- RLS: cada usuario solo ve e inserta sus propias filas (usa get_my_internal_id() si existe en el proyecto)
alter table public.pantry_historical enable row level security;

drop policy if exists "Users can read own pantry historical" on public.pantry_historical;
create policy "Users can read own pantry historical"
  on public.pantry_historical for select
  using (get_my_internal_id() is not null and user_id = get_my_internal_id());

drop policy if exists "Users can insert own pantry historical" on public.pantry_historical;
create policy "Users can insert own pantry historical"
  on public.pantry_historical for insert
  with check (get_my_internal_id() is not null and user_id = get_my_internal_id());
