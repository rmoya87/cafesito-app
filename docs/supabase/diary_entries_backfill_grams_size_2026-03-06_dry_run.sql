-- DRY RUN (no writes) for diary_entries backfill
-- Date: 2026-03-06
-- Use this first in production to validate impact.

-- 1) Rows that would be affected
SELECT
  COUNT(*) FILTER (WHERE COALESCE(type, 'CUP') = 'CUP') AS cup_rows,
  COUNT(*) FILTER (WHERE COALESCE(type, 'CUP') = 'CUP' AND COALESCE(coffee_grams, 0) <= 0) AS would_fill_coffee_grams,
  COUNT(*) FILTER (WHERE COALESCE(type, 'CUP') = 'CUP' AND (size_label IS NULL OR btrim(size_label) = '')) AS would_fill_size_label
FROM public.diary_entries;

-- 2) Preview of coffee_grams computed value (top 100)
WITH preview AS (
  SELECT
    id,
    timestamp,
    coffee_name,
    amount_ml,
    coffee_grams AS current_coffee_grams,
    COALESCE(
      NULLIF(
        ROUND(
          NULLIF(
            REPLACE(
              substring(preparation_type from '(\\d+(?:[\\.,]\\d+)?)\\s*g'),
              ',',
              '.'
            ),
            ''
          )::numeric
        )::int,
        0
      ),
      NULLIF(ROUND((GREATEST(amount_ml, 1))::numeric / 16.0)::int, 0),
      15
    ) AS would_set_coffee_grams,
    preparation_type
  FROM public.diary_entries
  WHERE COALESCE(type, 'CUP') = 'CUP'
)
SELECT *
FROM preview
WHERE COALESCE(current_coffee_grams, 0) <= 0
ORDER BY timestamp DESC
LIMIT 100;

-- 3) Preview of size_label computed value (top 100)
WITH preview AS (
  SELECT
    id,
    timestamp,
    coffee_name,
    amount_ml,
    size_label AS current_size_label,
    CASE
      WHEN amount_ml <= 105 THEN 'Espresso'
      WHEN amount_ml <= 228 THEN 'Pequeño'
      WHEN amount_ml <= 325 THEN 'Mediano'
      WHEN amount_ml <= 425 THEN 'Grande'
      ELSE 'Tazón XL'
    END AS would_set_size_label
  FROM public.diary_entries
  WHERE COALESCE(type, 'CUP') = 'CUP'
)
SELECT *
FROM preview
WHERE current_size_label IS NULL OR btrim(current_size_label) = ''
ORDER BY timestamp DESC
LIMIT 100;
