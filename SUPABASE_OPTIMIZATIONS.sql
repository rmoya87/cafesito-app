-- ============================================
-- OPTIMIZACIONES DE SUPABASE PARA CAFESITO APP
-- ============================================
-- Ejecuta este script en el SQL Editor de Supabase
-- ============================================

-- 1. Activar extensión de trigramas para búsquedas rápidas
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- 2. Índices para búsquedas ILIKE con comodines
CREATE INDEX IF NOT EXISTS idx_coffees_nombre_trgm 
ON coffees USING gin (nombre gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_coffees_marca_trgm 
ON coffees USING gin (marca gin_trgm_ops);

-- 3. Índices para filtros exactos
CREATE INDEX IF NOT EXISTS idx_coffees_pais_origen ON coffees (pais_origen);
CREATE INDEX IF NOT EXISTS idx_coffees_tueste ON coffees (tueste);
CREATE INDEX IF NOT EXISTS idx_coffees_especialidad ON coffees (especialidad);
CREATE INDEX IF NOT EXISTS idx_coffees_formato ON coffees (formato);
CREATE INDEX IF NOT EXISTS idx_coffees_puntuacion_total ON coffees (puntuacion_total);

-- 4. Vista optimizada con JOINS para evitar múltiples peticiones
CREATE OR REPLACE VIEW coffees_with_stats AS
SELECT 
    c.*,
    COALESCE(AVG(r.rating), 0.0) as avg_rating,
    COUNT(DISTINCT r.id) as review_count
FROM 
    coffees c
LEFT JOIN 
    reviews_db r ON c.id = r.coffee_id
GROUP BY 
    c.id;

-- 5. Función RPC para recomendaciones (cálculo en servidor)
CREATE OR REPLACE FUNCTION get_coffee_recommendations(target_user_id INT)
RETURNS TABLE (
    id TEXT,
    especialidad TEXT,
    marca TEXT,
    pais_origen TEXT,
    variedad_tipo TEXT,
    nombre TEXT,
    descripcion TEXT,
    fuente_puntuacion TEXT,
    puntuacion_oficial DOUBLE PRECISION,
    notas_cata TEXT,
    formato TEXT,
    cafeina TEXT,
    tueste TEXT,
    proceso TEXT,
    ratio_recomendado TEXT,
    molienda_recomendada TEXT,
    aroma REAL,
    sabor REAL,
    retrogusto REAL,
    acidez REAL,
    cuerpo REAL,
    uniformidad REAL,
    dulzura REAL,
    puntuacion_total DOUBLE PRECISION,
    codigo_barras TEXT,
    image_url TEXT,
    product_url TEXT,
    is_custom BOOLEAN,
    user_id INT
)
LANGUAGE plpgsql
AS $$
DECLARE
    avg_aroma REAL;
    avg_sabor REAL;
BEGIN
    -- Calcular promedios de los favoritos del usuario
    SELECT 
        AVG(c.aroma),
        AVG(c.sabor)
    INTO avg_aroma, avg_sabor
    FROM coffees c
    INNER JOIN local_favorites lf ON c.id = lf.coffee_id
    WHERE lf.user_id = target_user_id;

    -- Si no hay favoritos, retornar vacío
    IF avg_aroma IS NULL THEN
        RETURN;
    END IF;

    -- Retornar cafés similares usando distancia euclidiana
    RETURN QUERY
    SELECT 
        c.id,
        c.especialidad,
        c.marca,
        c.pais_origen,
        c.variedad_tipo,
        c.nombre,
        c.descripcion,
        c.fuente_puntuacion,
        c.puntuacion_oficial,
        c.notas_cata,
        c.formato,
        c.cafeina,
        c.tueste,
        c.proceso,
        c.ratio_recomendado,
        c.molienda_recomendada,
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
    FROM coffees c
    WHERE c.id NOT IN (
        SELECT coffee_id FROM local_favorites WHERE user_id = target_user_id
    )
    ORDER BY 
        SQRT(POWER(c.aroma - avg_aroma, 2) + POWER(c.sabor - avg_sabor, 2))
    LIMIT 5;
END;
$$;

-- 6. Índices en tablas relacionales para mejorar JOINs
CREATE INDEX IF NOT EXISTS idx_reviews_coffee_id ON reviews_db (coffee_id);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews_db (user_id);
CREATE INDEX IF NOT EXISTS idx_local_favorites_user_id ON local_favorites (user_id);
CREATE INDEX IF NOT EXISTS idx_local_favorites_coffee_id ON local_favorites (coffee_id);
CREATE INDEX IF NOT EXISTS idx_local_favorites_custom_user_id ON local_favorites_custom (user_id);
CREATE INDEX IF NOT EXISTS idx_local_favorites_custom_coffee_id ON local_favorites_custom (coffee_id);

-- ============================================
-- FIN DE OPTIMIZACIONES
-- ============================================
