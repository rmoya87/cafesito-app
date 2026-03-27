-- Drop seguro de tablas legacy no usadas por frontend actual.
-- Ejecutar solo después de desplegar el código que elimina referencias.
-- Fecha: 2026-03-26

begin;

-- Funciones RPC i18n de cafés (dependen de coffee_translations).
drop function if exists public.get_coffee_localized_by_id(text, text, text);
drop function if exists public.get_coffees_localized(text, text);

-- Trigger/helper de actualización timestamp en traducciones.
drop trigger if exists trg_set_updated_at_coffee_translations on public.coffee_translations;
drop function if exists public.set_updated_at_coffee_translations();

-- Tablas legacy social.
drop table if exists public.post_coffee_tags;
drop table if exists public.comments_db;
drop table if exists public.likes_db;
drop table if exists public.posts_db;

-- Tabla de traducciones de café (si decides desactivar i18n por tabla).
drop table if exists public.coffee_translations;

commit;
