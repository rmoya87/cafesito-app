-- Persistencia remota para etiquetas de café en publicaciones
-- Ejecutar en Supabase SQL Editor

create table if not exists public.post_coffee_tags (
  post_id text primary key references public.posts_db(id) on delete cascade,
  coffee_id text not null,
  coffee_name text not null,
  coffee_brand text not null,
  coffee_image_url text not null,
  coffee_rating real null,
  created_at timestamptz not null default now()
);

create index if not exists idx_post_coffee_tags_coffee_id
  on public.post_coffee_tags (coffee_id);

alter table public.post_coffee_tags enable row level security;

-- Lectura pública para timeline/feed
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'post_coffee_tags'
      AND policyname = 'Post coffee tags are readable by authenticated users'
  ) THEN
    CREATE POLICY "Post coffee tags are readable by authenticated users"
      ON public.post_coffee_tags
      FOR SELECT
      TO authenticated
      USING (true);
  END IF;
END$$;

-- Escritura restringida al autor del post
DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'post_coffee_tags'
      AND policyname = 'Post coffee tags are insertable by post owner'
  ) THEN
    CREATE POLICY "Post coffee tags are insertable by post owner"
      ON public.post_coffee_tags
      FOR INSERT
      TO authenticated
      WITH CHECK (
        EXISTS (
          SELECT 1
          FROM public.posts_db p
          WHERE p.id = post_id
            AND p.user_id = auth.uid()::int
        )
      );
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'post_coffee_tags'
      AND policyname = 'Post coffee tags are updatable by post owner'
  ) THEN
    CREATE POLICY "Post coffee tags are updatable by post owner"
      ON public.post_coffee_tags
      FOR UPDATE
      TO authenticated
      USING (
        EXISTS (
          SELECT 1
          FROM public.posts_db p
          WHERE p.id = post_id
            AND p.user_id = auth.uid()::int
        )
      )
      WITH CHECK (
        EXISTS (
          SELECT 1
          FROM public.posts_db p
          WHERE p.id = post_id
            AND p.user_id = auth.uid()::int
        )
      );
  END IF;
END$$;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_policies
    WHERE schemaname = 'public'
      AND tablename = 'post_coffee_tags'
      AND policyname = 'Post coffee tags are deletable by post owner'
  ) THEN
    CREATE POLICY "Post coffee tags are deletable by post owner"
      ON public.post_coffee_tags
      FOR DELETE
      TO authenticated
      USING (
        EXISTS (
          SELECT 1
          FROM public.posts_db p
          WHERE p.id = post_id
            AND p.user_id = auth.uid()::int
        )
      );
  END IF;
END$$;
