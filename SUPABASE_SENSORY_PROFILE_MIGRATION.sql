-- Crear tabla para persistir opiniones de perfil sensorial por usuario y café
create table if not exists public.coffee_sensory_profiles (
  coffee_id text not null references public.coffees(id) on delete cascade,
  user_id bigint not null references public.users_db(id) on delete cascade,
  aroma real not null check (aroma >= 0 and aroma <= 5),
  sabor real not null check (sabor >= 0 and sabor <= 5),
  cuerpo real not null check (cuerpo >= 0 and cuerpo <= 5),
  acidez real not null check (acidez >= 0 and acidez <= 5),
  dulzura real not null check (dulzura >= 0 and dulzura <= 5),
  updated_at bigint not null default (extract(epoch from now()) * 1000)::bigint,
  primary key (coffee_id, user_id)
);

alter table public.coffee_sensory_profiles enable row level security;

-- Lectura pública para mostrar medias de perfil sensorial en detalle de café
create policy if not exists "coffee_sensory_profiles_select_public"
on public.coffee_sensory_profiles
for select
using (true);

-- Escritura sólo del propio usuario
create policy if not exists "coffee_sensory_profiles_upsert_own"
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
