ALTER TABLE clientes ADD COLUMN IF NOT EXISTS puntos_fidelizacion INTEGER NOT NULL DEFAULT 0;
ALTER TABLE productos ADD COLUMN IF NOT EXISTS puntos_por_unidad INTEGER NOT NULL DEFAULT 0;
CREATE TABLE IF NOT EXISTS alertas_recompra (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL REFERENCES clientes(id),
    medicamento_id BIGINT NOT NULL,
    venta_origen_id BIGINT,
    fecha_sugerida DATE NOT NULL,
    contactado BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_alertas_recompra_fecha ON alertas_recompra(fecha_sugerida) WHERE contactado = false;
