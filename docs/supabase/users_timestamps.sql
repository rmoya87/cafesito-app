-- users_db timestamps: created_at y updated_at
-- Ejecutar en Supabase SQL Editor.

alter table if exists public.users_db
    alter column created_at set default now();

alter table if exists public.users_db
    add column if not exists updated_at timestamptz;

update public.users_db
set updated_at = coalesce(updated_at, created_at, now())
where updated_at is null;

create or replace function public.set_users_db_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_users_db_updated_at on public.users_db;
create trigger trg_users_db_updated_at
before update on public.users_db
for each row
execute function public.set_users_db_updated_at();
