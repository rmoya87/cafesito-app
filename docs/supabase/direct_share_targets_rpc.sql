-- Direct Share targets (Android + WebApp)
-- Estado: propuesta inicial Sprint 2 (ranking real)
-- Aplica una RPC para devolver destinos sugeridos de compartir.

create or replace function public.get_direct_share_targets(
  p_user_id integer,
  p_limit integer default 5
)
returns table (
  id text,
  type text,
  label text,
  deep_link text,
  rank_score double precision
)
language sql
security definer
set search_path = public
as $$
  with my_lists as (
    select
      ul.id::text as id,
      'list'::text as type,
      ul.name::text as label,
      ('https://cafesitoapp.com/profile/list/' || ul.id::text) as deep_link,
      (
        case when ul.user_id = p_user_id then 100 else 80 end
        + coalesce(extract(epoch from ul.created_at) / 1000000000.0, 0)
      )::double precision as rank_score
    from public.user_lists ul
    where ul.user_id = p_user_id
       or exists (
         select 1
         from public.user_list_members m
         where m.list_id = ul.id
           and m.user_id = p_user_id
       )
  ),
  frequent_contacts as (
    select
      u.id::text as id,
      'contact'::text as type,
      coalesce(nullif(trim(u.full_name), ''), '@' || u.username)::text as label,
      ('https://cafesitoapp.com/profile/' || u.username)::text as deep_link,
      (
        60
        + count(f.followed_id)::double precision
      )::double precision as rank_score
    from public.users_db u
    join public.follows f
      on f.followed_id = u.id
    where f.follower_id = p_user_id
    group by u.id, u.username, u.full_name
  ),
  unioned as (
    select * from my_lists
    union all
    select * from frequent_contacts
  )
  select *
  from unioned
  order by rank_score desc, label asc
  limit greatest(1, least(coalesce(p_limit, 5), 20));
$$;

grant execute on function public.get_direct_share_targets(integer, integer) to anon, authenticated, service_role;
