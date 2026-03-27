-- coffees_i18n_all_in_one.sql
-- Un solo documento para:
-- 1) Crear tabla i18n
-- 2) Backfill de es desde coffees
-- 3) Crear en/fr/pt/de
-- 4) Traducir todos los campos de coffee_translations
-- 5) Exponer RPCs localizadas con fallback

BEGIN;

-- =========================================================
-- A. ESTRUCTURA I18N
-- =========================================================

create table if not exists public.coffee_translations (
  coffee_id text not null references public.coffees(id) on delete cascade,
  locale text not null,
  nombre text not null,
  descripcion text null,
  notas_cata text null,
  marca text null,
  pais_origen text null,
  variedad_tipo text null,
  especialidad text null,
  formato text null,
  cafeina text null,
  tueste text null,
  proceso text null,
  molienda_recomendada text null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint coffee_translations_pk primary key (coffee_id, locale),
  constraint coffee_translations_locale_ck check (locale ~ '^[a-z]{2}(-[A-Z]{2})?$')
);

create index if not exists idx_coffee_translations_locale on public.coffee_translations(locale);
create index if not exists idx_coffee_translations_coffee_id on public.coffee_translations(coffee_id);

create or replace function public.set_updated_at_coffee_translations()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_set_updated_at_coffee_translations on public.coffee_translations;
create trigger trg_set_updated_at_coffee_translations
before update on public.coffee_translations
for each row execute function public.set_updated_at_coffee_translations();

alter table public.coffee_translations enable row level security;

do $$
begin
  if not exists (
    select 1
    from pg_policies
    where schemaname = 'public'
      and tablename = 'coffee_translations'
      and policyname = 'Coffee translations are readable by authenticated users'
  ) then
    create policy "Coffee translations are readable by authenticated users"
      on public.coffee_translations
      for select
      to authenticated
      using (true);
  end if;
end$$;

-- =========================================================
-- B. BACKFILL ES + CLONADO A EN/FR/PT/DE
-- =========================================================

insert into public.coffee_translations (
  coffee_id, locale, nombre, descripcion, notas_cata, marca, pais_origen, variedad_tipo,
  especialidad, formato, cafeina, tueste, proceso, molienda_recomendada
)
select
  c.id,
  'es',
  coalesce(c.nombre, ''),
  nullif(c.descripcion, ''),
  nullif(c.notas_cata, ''),
  nullif(c.marca, ''),
  nullif(c.pais_origen, ''),
  nullif(c.variedad_tipo, ''),
  nullif(c.especialidad, ''),
  nullif(c.formato, ''),
  nullif(c.cafeina, ''),
  nullif(c.tueste, ''),
  nullif(c.proceso, ''),
  nullif(c.molienda_recomendada, '')
from public.coffees c
on conflict (coffee_id, locale) do update set
  nombre = excluded.nombre,
  descripcion = excluded.descripcion,
  notas_cata = excluded.notas_cata,
  marca = excluded.marca,
  pais_origen = excluded.pais_origen,
  variedad_tipo = excluded.variedad_tipo,
  especialidad = excluded.especialidad,
  formato = excluded.formato,
  cafeina = excluded.cafeina,
  tueste = excluded.tueste,
  proceso = excluded.proceso,
  molienda_recomendada = excluded.molienda_recomendada,
  updated_at = now();

insert into public.coffee_translations (
  coffee_id, locale, nombre, descripcion, notas_cata, marca, pais_origen, variedad_tipo,
  especialidad, formato, cafeina, tueste, proceso, molienda_recomendada
)
select
  es.coffee_id, l.locale, es.nombre, es.descripcion, es.notas_cata, es.marca, es.pais_origen, es.variedad_tipo,
  es.especialidad, es.formato, es.cafeina, es.tueste, es.proceso, es.molienda_recomendada
from public.coffee_translations es
cross join (values ('en'), ('fr'), ('pt'), ('de')) as l(locale)
where es.locale = 'es'
on conflict (coffee_id, locale) do nothing;

-- =========================================================
-- C. UTILIDADES DE TRADUCCION
-- =========================================================

create or replace function public.i18n_norm(p text)
returns text
language sql
immutable
as $$
  select lower(
    translate(
      coalesce(trim(p), ''),
      'ÁÀÂÄÃáàâäãÉÈÊËéèêëÍÌÎÏíìîïÓÒÔÖÕóòôöõÚÙÛÜúùûüÑñÇç',
      'AAAAAaaaaaEEEEeeeeIIIIiiiiOOOOOoooooUUUUuuuuNnCc'
    )
  );
$$;

create temporary table tmp_i18n_dict (
  locale text not null,
  field_name text not null,
  source_norm text not null,
  target_text text not null
) on commit drop;

insert into tmp_i18n_dict(locale, field_name, source_norm, target_text)
values
  -- nombre
  ('en','nombre','americano','Americano'),
  ('en','nombre','capuchino','Cappuccino'),
  ('en','nombre','descafeinado','Decaf'),
  ('en','nombre','frappuccino','Frappuccino'),
  ('en','nombre','irlandes','Irish Coffee'),
  ('en','nombre','latte macchiato','Latte Macchiato'),
  ('en','nombre','leche con chocolate','Mocha'),
  ('en','nombre','marroqui','Moroccan Coffee'),
  ('en','nombre','moca','Mocha'),
  ('en','nombre','vienes','Viennese Coffee'),
  ('fr','nombre','americano','Cafe Americain'),
  ('fr','nombre','descafeinado','Decafeine'),
  ('fr','nombre','irlandes','Cafe Irlandais'),
  ('fr','nombre','leche con chocolate','Moka'),
  ('fr','nombre','marroqui','Cafe Marocain'),
  ('fr','nombre','moca','Moka'),
  ('fr','nombre','vienes','Cafe Viennois'),
  ('pt','nombre','americano','Cafe Americano'),
  ('pt','nombre','irlandes','Cafe Irlandes'),
  ('pt','nombre','marroqui','Cafe Marroquino'),
  ('pt','nombre','vienes','Cafe Vienense'),
  ('de','nombre','descafeinado','Entkoffeiniert'),
  ('de','nombre','leche con chocolate','Mokka'),
  ('de','nombre','marroqui','Marokkanischer Kaffee'),
  ('de','nombre','moca','Mokka'),
  ('de','nombre','vienes','Wiener Kaffee'),

  -- pais_origen
  ('en','pais_origen','brasil','Brazil'),
  ('en','pais_origen','colombia','Colombia'),
  ('en','pais_origen','etiopia','Ethiopia'),
  ('en','pais_origen','mexico','Mexico'),
  ('en','pais_origen','guatemala','Guatemala'),
  ('en','pais_origen','peru','Peru'),
  ('en','pais_origen','honduras','Honduras'),
  ('fr','pais_origen','brasil','Bresil'),
  ('fr','pais_origen','colombia','Colombie'),
  ('fr','pais_origen','etiopia','Ethiopie'),
  ('fr','pais_origen','mexico','Mexique'),
  ('fr','pais_origen','guatemala','Guatemala'),
  ('fr','pais_origen','peru','Perou'),
  ('fr','pais_origen','honduras','Honduras'),
  ('pt','pais_origen','brasil','Brasil'),
  ('pt','pais_origen','colombia','Colombia'),
  ('pt','pais_origen','etiopia','Etiopia'),
  ('pt','pais_origen','mexico','Mexico'),
  ('pt','pais_origen','guatemala','Guatemala'),
  ('pt','pais_origen','peru','Peru'),
  ('pt','pais_origen','honduras','Honduras'),
  ('de','pais_origen','brasil','Brasilien'),
  ('de','pais_origen','colombia','Kolumbien'),
  ('de','pais_origen','etiopia','Athiopien'),
  ('de','pais_origen','mexico','Mexiko'),
  ('de','pais_origen','guatemala','Guatemala'),
  ('de','pais_origen','peru','Peru'),
  ('de','pais_origen','honduras','Honduras'),

  -- variedad_tipo
  ('en','variedad_tipo','arabica','Arabica'),
  ('en','variedad_tipo','arabica blend','Arabica blend'),
  ('en','variedad_tipo','robusta','Robusta'),
  ('en','variedad_tipo','blend','Blend'),
  ('fr','variedad_tipo','arabica','Arabica'),
  ('fr','variedad_tipo','arabica blend','Assemblage arabica'),
  ('fr','variedad_tipo','robusta','Robusta'),
  ('fr','variedad_tipo','blend','Assemblage'),
  ('pt','variedad_tipo','arabica','Arabica'),
  ('pt','variedad_tipo','arabica blend','Blend arabica'),
  ('pt','variedad_tipo','robusta','Robusta'),
  ('pt','variedad_tipo','blend','Blend'),
  ('de','variedad_tipo','arabica','Arabica'),
  ('de','variedad_tipo','arabica blend','Arabica-Mischung'),
  ('de','variedad_tipo','robusta','Robusta'),
  ('de','variedad_tipo','blend','Mischung'),

  -- especialidad
  ('en','especialidad','especialidad','Specialty'),
  ('en','especialidad','comercial','Commercial'),
  ('fr','especialidad','especialidad','Specialite'),
  ('fr','especialidad','comercial','Commercial'),
  ('pt','especialidad','especialidad','Especialidade'),
  ('pt','especialidad','comercial','Comercial'),
  ('de','especialidad','especialidad','Spezialitaet'),
  ('de','especialidad','comercial','Handelsware'),

  -- formato
  ('en','formato','grano','Beans'),
  ('en','formato','molido','Ground'),
  ('en','formato','capsulas','Capsules'),
  ('fr','formato','grano','En grains'),
  ('fr','formato','molido','Moulu'),
  ('fr','formato','capsulas','Capsules'),
  ('pt','formato','grano','Graos'),
  ('pt','formato','molido','Moido'),
  ('pt','formato','capsulas','Capsulas'),
  ('de','formato','grano','Bohnen'),
  ('de','formato','molido','Gemahlen'),
  ('de','formato','capsulas','Kapseln'),

  -- cafeina
  ('en','cafeina','si','Yes'), ('en','cafeina','no','No'),
  ('fr','cafeina','si','Oui'), ('fr','cafeina','no','Non'),
  ('pt','cafeina','si','Sim'), ('pt','cafeina','no','Nao'),
  ('de','cafeina','si','Ja'), ('de','cafeina','no','Nein'),

  -- tueste
  ('en','tueste','claro','Light'), ('en','tueste','medio','Medium'), ('en','tueste','medio oscuro','Medium-dark'), ('en','tueste','oscuro','Dark'),
  ('fr','tueste','claro','Clair'), ('fr','tueste','medio','Moyen'), ('fr','tueste','medio oscuro','Moyen-fonce'), ('fr','tueste','oscuro','Fonce'),
  ('pt','tueste','claro','Claro'), ('pt','tueste','medio','Medio'), ('pt','tueste','medio oscuro','Medio-escuro'), ('pt','tueste','oscuro','Escuro'),
  ('de','tueste','claro','Hell'), ('de','tueste','medio','Mittel'), ('de','tueste','medio oscuro','Mittel-dunkel'), ('de','tueste','oscuro','Dunkel'),

  -- proceso
  ('en','proceso','lavado','Washed'), ('en','proceso','natural','Natural'), ('en','proceso','anaerobico','Anaerobic'),
  ('fr','proceso','lavado','Lave'), ('fr','proceso','natural','Naturel'), ('fr','proceso','anaerobico','Anaerobie'),
  ('pt','proceso','lavado','Lavado'), ('pt','proceso','natural','Natural'), ('pt','proceso','anaerobico','Anaerobico'),
  ('de','proceso','lavado','Gewaschen'), ('de','proceso','natural','Natural'), ('de','proceso','anaerobico','Anaerob'),

  -- molienda_recomendada
  ('en','molienda_recomendada','fina','Fine'),
  ('en','molienda_recomendada','media','Medium'),
  ('en','molienda_recomendada','gruesa','Coarse'),
  ('fr','molienda_recomendada','fina','Fine'),
  ('fr','molienda_recomendada','media','Moyenne'),
  ('fr','molienda_recomendada','gruesa','Grossiere'),
  ('pt','molienda_recomendada','fina','Fina'),
  ('pt','molienda_recomendada','media','Media'),
  ('pt','molienda_recomendada','gruesa','Grossa'),
  ('de','molienda_recomendada','fina','Fein'),
  ('de','molienda_recomendada','media','Mittel'),
  ('de','molienda_recomendada','gruesa','Grob');

-- =========================================================
-- D. TRADUCCION DE TODOS LOS CAMPOS
-- =========================================================

update public.coffee_translations t
set
  nombre = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'nombre' and d.source_norm = public.i18n_norm(t.nombre) limit 1), t.nombre),
  marca = t.marca,
  pais_origen = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'pais_origen' and d.source_norm = public.i18n_norm(t.pais_origen) limit 1), t.pais_origen),
  variedad_tipo = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'variedad_tipo' and d.source_norm = public.i18n_norm(t.variedad_tipo) limit 1), t.variedad_tipo),
  especialidad = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'especialidad' and d.source_norm = public.i18n_norm(t.especialidad) limit 1), t.especialidad),
  formato = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'formato' and d.source_norm = public.i18n_norm(t.formato) limit 1), t.formato),
  cafeina = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'cafeina' and d.source_norm = public.i18n_norm(t.cafeina) limit 1), t.cafeina),
  tueste = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'tueste' and d.source_norm = public.i18n_norm(t.tueste) limit 1), t.tueste),
  proceso = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'proceso' and d.source_norm = public.i18n_norm(t.proceso) limit 1), t.proceso),
  molienda_recomendada = coalesce((select d.target_text from tmp_i18n_dict d where d.locale = t.locale and d.field_name = 'molienda_recomendada' and d.source_norm = public.i18n_norm(t.molienda_recomendada) limit 1), t.molienda_recomendada)
where t.locale in ('en','fr','pt','de');

-- Traduccion de descripcion/notas_cata por reemplazo semantico
create temporary table tmp_i18n_text (
  locale text not null,
  source_pattern text not null,
  target_text text not null
) on commit drop;

insert into tmp_i18n_text(locale, source_pattern, target_text)
values
  ('en','café','coffee'), ('en','cafe','coffee'), ('en','notas de cata','tasting notes'), ('en','notas','notes'),
  ('en','frutal','fruity'), ('en','floral','floral'), ('en','chocolate','chocolate'), ('en','caramelo','caramel'),
  ('en','avellana','hazelnut'), ('en','vainilla','vanilla'), ('en','acidez','acidity'), ('en','cuerpo','body'),
  ('en','dulzura','sweetness'), ('en','sabor','flavor'), ('en','aroma','aroma'), ('en','proceso','process'),
  ('en','tueste','roast'), ('en','molienda','grind'), ('en','intenso','intense'), ('en','suave','smooth'),

  ('fr','café','cafe'), ('fr','cafe','cafe'), ('fr','notas de cata','notes de degustation'), ('fr','notas','notes'),
  ('fr','frutal','fruite'), ('fr','floral','floral'), ('fr','chocolate','chocolat'), ('fr','caramelo','caramel'),
  ('fr','avellana','noisette'), ('fr','vainilla','vanille'), ('fr','acidez','acidite'), ('fr','cuerpo','corps'),
  ('fr','dulzura','douceur'), ('fr','sabor','saveur'), ('fr','aroma','arome'), ('fr','proceso','processus'),
  ('fr','tueste','torrefaction'), ('fr','molienda','mouture'), ('fr','intenso','intense'), ('fr','suave','doux'),

  ('pt','café','cafe'), ('pt','cafe','cafe'), ('pt','notas de cata','notas de prova'), ('pt','notas','notas'),
  ('pt','frutal','frutado'), ('pt','floral','floral'), ('pt','chocolate','chocolate'), ('pt','caramelo','caramelo'),
  ('pt','avellana','avelã'), ('pt','vainilla','baunilha'), ('pt','acidez','acidez'), ('pt','cuerpo','corpo'),
  ('pt','dulzura','docura'), ('pt','sabor','sabor'), ('pt','aroma','aroma'), ('pt','proceso','processo'),
  ('pt','tueste','torra'), ('pt','molienda','moagem'), ('pt','intenso','intenso'), ('pt','suave','suave'),

  ('de','café','kaffee'), ('de','cafe','kaffee'), ('de','notas de cata','verkostungsnoten'), ('de','notas','noten'),
  ('de','frutal','fruchtig'), ('de','floral','blumig'), ('de','chocolate','schokolade'), ('de','caramelo','karamell'),
  ('de','avellana','haselnuss'), ('de','vainilla','vanille'), ('de','acidez','saeure'), ('de','cuerpo','koerper'),
  ('de','dulzura','suesse'), ('de','sabor','geschmack'), ('de','aroma','aroma'), ('de','proceso','prozess'),
  ('de','tueste','rostung'), ('de','molienda','mahlgrad'), ('de','intenso','intensiv'), ('de','suave','mild');

create or replace function public.i18n_translate_text(p_text text, p_locale text)
returns text
language plpgsql
as $$
declare
  v text;
  r record;
begin
  if p_text is null or btrim(p_text) = '' then
    return p_text;
  end if;
  v := p_text;
  for r in
    select source_pattern, target_text
    from tmp_i18n_text
    where locale = p_locale
    order by length(source_pattern) desc
  loop
    v := regexp_replace(v, r.source_pattern, r.target_text, 'gi');
  end loop;
  return v;
end;
$$;

update public.coffee_translations t
set
  descripcion = public.i18n_translate_text(t.descripcion, t.locale),
  notas_cata = public.i18n_translate_text(t.notas_cata, t.locale)
where t.locale in ('en','fr','pt','de');

drop function if exists public.i18n_translate_text(text, text);
drop function if exists public.i18n_norm(text);

-- =========================================================
-- E. RPCS LOCALIZADAS (FALLBACK)
-- =========================================================

create or replace function public.get_coffees_localized(
  p_locale text default 'es',
  p_fallback_locale text default 'es'
)
returns table (
  id text,
  especialidad text,
  marca text,
  pais_origen text,
  variedad_tipo text,
  nombre text,
  descripcion text,
  fuente_puntuacion text,
  puntuacion_oficial double precision,
  notas_cata text,
  formato text,
  cafeina text,
  tueste text,
  proceso text,
  ratio_recomendado text,
  molienda_recomendada text,
  aroma real,
  sabor real,
  retrogusto real,
  acidez real,
  cuerpo real,
  uniformidad real,
  dulzura real,
  puntuacion_total double precision,
  codigo_barras text,
  image_url text,
  product_url text,
  is_custom boolean,
  user_id integer
)
language sql
stable
as $$
  select
    c.id,
    coalesce(t_loc.especialidad, t_fb.especialidad, c.especialidad),
    coalesce(t_loc.marca, t_fb.marca, c.marca),
    coalesce(t_loc.pais_origen, t_fb.pais_origen, c.pais_origen),
    coalesce(t_loc.variedad_tipo, t_fb.variedad_tipo, c.variedad_tipo),
    coalesce(t_loc.nombre, t_fb.nombre, c.nombre),
    coalesce(t_loc.descripcion, t_fb.descripcion, c.descripcion),
    c.fuente_puntuacion,
    c.puntuacion_oficial,
    coalesce(t_loc.notas_cata, t_fb.notas_cata, c.notas_cata),
    coalesce(t_loc.formato, t_fb.formato, c.formato),
    coalesce(t_loc.cafeina, t_fb.cafeina, c.cafeina),
    coalesce(t_loc.tueste, t_fb.tueste, c.tueste),
    coalesce(t_loc.proceso, t_fb.proceso, c.proceso),
    c.ratio_recomendado,
    coalesce(t_loc.molienda_recomendada, t_fb.molienda_recomendada, c.molienda_recomendada),
    c.aroma,
    c.sabor,
    c.retrogusto,
    c.acidez,
    c.cuerpo,
    c.uniformidad,
    c.dulzura,
    c.puntuacion_total,
    c.codigo_barras,
    c.image_url,
    c.product_url,
    c.is_custom,
    c.user_id
  from public.coffees c
  left join public.coffee_translations t_loc
    on t_loc.coffee_id = c.id and t_loc.locale = lower(trim(p_locale))
  left join public.coffee_translations t_fb
    on t_fb.coffee_id = c.id and t_fb.locale = lower(trim(p_fallback_locale))
  order by coalesce(t_loc.nombre, t_fb.nombre, c.nombre) asc;
$$;

grant execute on function public.get_coffees_localized(text, text) to authenticated;

create or replace function public.get_coffee_localized_by_id(
  p_coffee_id text,
  p_locale text default 'es',
  p_fallback_locale text default 'es'
)
returns table (
  id text,
  especialidad text,
  marca text,
  pais_origen text,
  variedad_tipo text,
  nombre text,
  descripcion text,
  fuente_puntuacion text,
  puntuacion_oficial double precision,
  notas_cata text,
  formato text,
  cafeina text,
  tueste text,
  proceso text,
  ratio_recomendado text,
  molienda_recomendada text,
  aroma real,
  sabor real,
  retrogusto real,
  acidez real,
  cuerpo real,
  uniformidad real,
  dulzura real,
  puntuacion_total double precision,
  codigo_barras text,
  image_url text,
  product_url text,
  is_custom boolean,
  user_id integer
)
language sql
stable
as $$
  select *
  from public.get_coffees_localized(p_locale, p_fallback_locale)
  where id = p_coffee_id
  limit 1;
$$;

grant execute on function public.get_coffee_localized_by_id(text, text, text) to authenticated;

COMMIT;

-- Verificaciones:
-- select locale, count(*) from public.coffee_translations group by locale order by locale;
-- select coffee_id, locale, nombre, pais_origen, variedad_tipo, especialidad, formato, descripcion, notas_cata
-- from public.coffee_translations
-- order by coffee_id, locale;
