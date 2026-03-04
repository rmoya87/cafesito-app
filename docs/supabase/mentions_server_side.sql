-- ==========================================================
-- MENTIONS SERVER-SIDE (posts_db + comments_db)
-- ==========================================================
-- Objetivo:
-- - Garantizar que las menciones generen notifications_db en backend
--   aunque el cliente (web/android) sea una version antigua.
-- - Evitar duplicados cuando tambien exista logica de cliente.
--
-- Ejecutar en Supabase SQL Editor.
-- ==========================================================

-- 1) Deduplicar menciones existentes para poder crear indice unico parcial.
with ranked as (
  select
    id,
    row_number() over (
      partition by user_id, type, from_username, related_id
      order by id desc
    ) as rn
  from public.notifications_db
  where type = 'MENTION'
    and related_id is not null
)
delete from public.notifications_db n
using ranked r
where n.id = r.id
  and r.rn > 1;

-- 2) Indice parcial para blindar duplicados de menciones.
create unique index if not exists idx_notifications_mention_dedupe
  on public.notifications_db (user_id, type, from_username, related_id)
  where type = 'MENTION'
    and related_id is not null;

-- 3) Trigger para menciones en posts.
create or replace function public.notify_mentions_on_post_insert()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  author_username text;
  mention_lc text;
  mentioned_id bigint;
  related text;
begin
  select u.username
  into author_username
  from public.users_db u
  where u.id = NEW.user_id
  limit 1;

  if author_username is null or btrim(author_username) = '' then
    return NEW;
  end if;

  related := NEW.id || ':-1';

  for mention_lc in
    select distinct lower((m)[1]) as username_lc
    from regexp_matches(coalesce(NEW.comment, ''), '@([A-Za-z0-9._-]{2,30})', 'g') as m
  loop
    if mention_lc = lower(author_username) then
      continue;
    end if;

    select u.id
    into mentioned_id
    from public.users_db u
    where lower(u.username) = mention_lc
    limit 1;

    if mentioned_id is null or mentioned_id = NEW.user_id then
      continue;
    end if;

    begin
      perform public.create_notification(
        mentioned_id,
        'MENTION',
        author_username,
        'te ha mencionado',
        NEW.timestamp,
        related
      );
    exception
      when unique_violation then
        null; -- ya creada por otro flujo cliente/backend
    end;
  end loop;

  return NEW;
end;
$$;

drop trigger if exists notify_mentions_post_insert on public.posts_db;
create trigger notify_mentions_post_insert
after insert on public.posts_db
for each row
execute function public.notify_mentions_on_post_insert();

-- 4) Trigger para menciones en comentarios.
create or replace function public.notify_mentions_on_comment_insert()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  author_username text;
  mention_lc text;
  mentioned_id bigint;
  related text;
begin
  select u.username
  into author_username
  from public.users_db u
  where u.id = NEW.user_id
  limit 1;

  if author_username is null or btrim(author_username) = '' then
    return NEW;
  end if;

  related := NEW.post_id || ':' || NEW.id::text;

  for mention_lc in
    select distinct lower((m)[1]) as username_lc
    from regexp_matches(coalesce(NEW.text, ''), '@([A-Za-z0-9._-]{2,30})', 'g') as m
  loop
    if mention_lc = lower(author_username) then
      continue;
    end if;

    select u.id
    into mentioned_id
    from public.users_db u
    where lower(u.username) = mention_lc
    limit 1;

    if mentioned_id is null or mentioned_id = NEW.user_id then
      continue;
    end if;

    begin
      perform public.create_notification(
        mentioned_id,
        'MENTION',
        author_username,
        NEW.text,
        NEW.timestamp,
        related
      );
    exception
      when unique_violation then
        null; -- ya creada por otro flujo cliente/backend
    end;
  end loop;

  return NEW;
end;
$$;

drop trigger if exists notify_mentions_comment_insert on public.comments_db;
create trigger notify_mentions_comment_insert
after insert on public.comments_db
for each row
execute function public.notify_mentions_on_comment_insert();

