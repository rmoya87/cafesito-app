-- Comprobar por qué un café custom no vuelve en Android (logcat: "Coffee <id> not returned by Supabase")
-- Ejecutar en Supabase SQL Editor. Las consultas 1 y 2 pueden ejecutarse sin JWT; la 3 necesita sesión.

-- 1) Ver el café y su user_id (sustituye el id por el que sale en logcat)
SELECT id, nombre, is_custom, user_id
FROM public.coffees
WHERE id = '9f945a5f-6916-4549-8375-b6abe6d4909a';
-- Anota user_id (ej. 70838558). Ese valor debe coincidir con get_my_internal_id() cuando la app hace la petición.

-- 2) Ver cómo está definida get_my_internal_id() (debe mapear auth.uid() al id de users_db)
SELECT pg_get_functiondef(oid) AS definition
FROM pg_proc
WHERE proname = 'get_my_internal_id';

-- 3) Con sesión autenticada (mismo usuario que en la app): comprobar qué devuelve get_my_internal_id()
--    En SQL Editor con "Run as user" o usando el JWT de la app no se puede; hazlo desde una función
--    que use auth.uid() o desde una petición PostgREST con el mismo JWT que envía Android.
SELECT get_my_internal_id() AS my_internal_id;
-- Si devuelve NULL o un número distinto del user_id del café (paso 1), ese café no pasará la RLS
-- para peticiones autenticadas con "Coffees are readable by all or owner".

-- 4) Políticas SELECT en coffees (solo informativo)
SELECT policyname, cmd, qual, roles
FROM pg_policies
WHERE tablename = 'coffees' AND schemaname = 'public' AND cmd = 'SELECT';
-- Para rol "authenticated" aplica la que tenga (is_custom = false OR (is_custom = true AND user_id = get_my_internal_id())).
-- Si get_my_internal_id() es NULL, los custom no se ven. Revisa la definición de get_my_internal_id()
-- (paso 2) para que devuelva el id de users_db correspondiente al auth.uid() del JWT.
