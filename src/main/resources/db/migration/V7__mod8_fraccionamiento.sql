ALTER TABLE productos ADD COLUMN IF NOT EXISTS factor_conversion INTEGER NOT NULL DEFAULT 1;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS unidad_minima VARCHAR(40) DEFAULT 'UNIDAD';
-- apto_fraccionamiento already on medicamentos from V2
