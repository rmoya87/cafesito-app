-- Crear tabla para persistir opiniones de perfil sensorial por usuario y café
create table if not exists public.coffee_sensory_profiles (
  coffee_id text not null references public.coffees(id) on delete cascade,
  user_id bigint not null references public.users_db(id) on delete cascade,
  aroma real not null check (aroma >= 0 and aroma <= 10),
  sabor real not null check (sabor >= 0 and sabor <= 10),
  cuerpo real not null check (cuerpo >= 0 and cuerpo <= 10),
  acidez real not null check (acidez >= 0 and acidez <= 10),
  dulzura real not null check (dulzura >= 0 and dulzura <= 10),
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  primary key (coffee_id, user_id)
);

alter table public.coffee_sensory_profiles enable row level security;

-- Nota: PostgreSQL/Supabase no soporta CREATE POLICY IF NOT EXISTS.
-- Para que sea idempotente, eliminamos y recreamos.
drop policy if exists coffee_sensory_profiles_select_public on public.coffee_sensory_profiles;
create policy coffee_sensory_profiles_select_public
on public.coffee_sensory_profiles
for select
using (true);

drop policy if exists coffee_sensory_profiles_upsert_own on public.coffee_sensory_profiles;
create policy coffee_sensory_profiles_upsert_own
on public.coffee_sensory_profiles
for all
using (auth.role() = 'authenticated')
with check (auth.role() = 'authenticated');

-- (Opcional) extender reviews_db para futuras integraciones cruzadas (retrocompatible)
alter table if exists public.reviews_db
  add column if not exists aroma real,
  add column if not exists sabor real,
  add column if not exists cuerpo real,
  add column if not exists acidez real,
  add column if not exists dulzura real;


-- Ajuste para instalaciones existentes creadas con checks 0..5
alter table if exists public.coffee_sensory_profiles
  drop constraint if exists coffee_sensory_profiles_aroma_check,
  drop constraint if exists coffee_sensory_profiles_sabor_check,
  drop constraint if exists coffee_sensory_profiles_cuerpo_check,
  drop constraint if exists coffee_sensory_profiles_acidez_check,
  drop constraint if exists coffee_sensory_profiles_dulzura_check;

alter table if exists public.coffee_sensory_profiles
  add constraint coffee_sensory_profiles_aroma_check check (aroma >= 0 and aroma <= 10),
  add constraint coffee_sensory_profiles_sabor_check check (sabor >= 0 and sabor <= 10),
  add constraint coffee_sensory_profiles_cuerpo_check check (cuerpo >= 0 and cuerpo <= 10),
  add constraint coffee_sensory_profiles_acidez_check check (acidez >= 0 and acidez <= 10),
  add constraint coffee_sensory_profiles_dulzura_check check (dulzura >= 0 and dulzura <= 10);

-- Compatibilidad con esquemas de diario antiguos sin external_id
alter table if exists public.diary_entries
  add column if not exists external_id text;
