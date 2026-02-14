-- Add size label to diary entries (coffee size selected in Mi Diario workflow)
ALTER TABLE diary_entries
ADD COLUMN IF NOT EXISTS size_label text;

COMMENT ON COLUMN diary_entries.size_label IS 'Selected size label (Espresso, Pequeño, Mediano, Grande, Tazón XL)';
