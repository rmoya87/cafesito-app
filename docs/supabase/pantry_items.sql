-- Tabla pantry_items: despensa del usuario. Permite varios registros por (coffee_id, user_id).
-- Cada vez que el usuario "añade" un café a la despensa se crea un nuevo registro (no se actualiza uno existente).

-- Creación desde cero (si la tabla no existe):
-- create table if not exists public.pantry_items (
--   id uuid primary key default gen_random_uuid(),
--   user_id integer not null references public.users_db(id) on delete cascade,
--   coffee_id uuid not null references public.coffees(id) on delete cascade,
--   grams_remaining integer not null default 0,
--   total_grams integer not null default 0,
--   last_updated bigint not null default (extract(epoch from now()) * 1000)::bigint
-- );
-- create index if not exists idx_pantry_items_user_id on public.pantry_items(user_id);

-- Migración: si ya tienes pantry_items con PK (coffee_id, user_id), ejecuta en este orden:

-- 1) Añadir columna id (nullable primero para poder rellenar)
alter table public.pantry_items add column if not exists id uuid;

-- 2) Rellenar id en filas que no lo tengan
update public.pantry_items set id = gen_random_uuid() where id is null;

-- 3) Hacer id NOT NULL
alter table public.pantry_items alter column id set not null;

-- 4) Si la PK actual es (coffee_id, user_id), quitarla (sustituir por el nombre real de tu constraint)
--    Ejemplo si la constraint se llama pantry_items_pkey:
alter table public.pantry_items drop constraint if exists pantry_items_pkey;

-- 5) Establecer id como nueva PK
alter table public.pantry_items add primary key (id);

-- 5b) Para que INSERT sin id genere uuid en nuevos registros
alter table public.pantry_items alter column id set default gen_random_uuid();

-- 6) Índice para listar por usuario
create index if not exists idx_pantry_items_user_id on public.pantry_items(user_id);

-- RLS (ajustar si ya lo tienes)
alter table public.pantry_items enable row level security;

drop policy if exists "Users can read own pantry" on public.pantry_items;
create policy "Users can read own pantry"
  on public.pantry_items for select
  using (user_id = get_my_internal_id());

drop policy if exists "Users can insert own pantry" on public.pantry_items;
create policy "Users can insert own pantry"
  on public.pantry_items for insert
  with check (user_id = get_my_internal_id());

drop policy if exists "Users can update own pantry" on public.pantry_items;
create policy "Users can update own pantry"
  on public.pantry_items for update
  using (user_id = get_my_internal_id());

drop policy if exists "Users can delete own pantry" on public.pantry_items;
create policy "Users can delete own pantry"
  on public.pantry_items for delete
  using (user_id = get_my_internal_id());
