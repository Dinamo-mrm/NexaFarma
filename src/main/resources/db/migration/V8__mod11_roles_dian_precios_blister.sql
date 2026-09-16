-- Precio máximo regulado (CNPMDM) y factor blíster
ALTER TABLE productos ADD COLUMN IF NOT EXISTS precio_maximo_regulado NUMERIC(12,2);
ALTER TABLE productos ADD COLUMN IF NOT EXISTS factor_blister INTEGER NOT NULL DEFAULT 1;

-- Cliente DIAN
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS tipo_documento VARCHAR(10) NOT NULL DEFAULT 'CC';
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS email_facturacion VARCHAR(150);
ALTER TABLE clientes ADD COLUMN IF NOT EXISTS responsabilidad_tributaria VARCHAR(80);

-- Auditoría de cambios de precio
CREATE TABLE IF NOT EXISTS precio_historial (
    id              BIGSERIAL PRIMARY KEY,
    producto_id     BIGINT NOT NULL,
    precio_anterior NUMERIC(12,2),
    precio_nuevo    NUMERIC(12,2) NOT NULL,
    precio_maximo_regulado NUMERIC(12,2),
    usuario         VARCHAR(80),
    creado_en       TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_precio_hist_producto ON precio_historial(producto_id);
