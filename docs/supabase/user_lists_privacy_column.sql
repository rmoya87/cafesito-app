-- Añade columna privacy a user_lists para tres modos: public, invitation, private.
-- Ejecutar en Supabase SQL Editor si quieres usar "Por invitación" además de Pública/Privada.
-- Si no ejecutas esto, la app usará solo is_public (Pública = true, Privada = false).

ALTER TABLE public.user_lists
  ADD COLUMN IF NOT EXISTS privacy text NOT NULL DEFAULT 'private'
  CHECK (privacy IN ('public', 'invitation', 'private'));

-- Sincronizar: listas ya públicas con is_public true
UPDATE public.user_lists SET privacy = 'public' WHERE is_public = true AND (privacy IS NULL OR privacy = 'private');

COMMENT ON COLUMN public.user_lists.privacy IS 'public = cualquiera puede suscribirse; invitation = solo por invitación; private = solo el dueño';
