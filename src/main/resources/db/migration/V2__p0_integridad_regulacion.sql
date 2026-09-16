-- Módulo 1 P0: integridad, Habeas Data, cuarentena, fraccionamiento, optimistic lock

-- Cliente: autorización Habeas Data (Ley 1581 de 2012)
ALTER TABLE clientes
    ADD COLUMN IF NOT EXISTS autoriza_tratamiento_datos BOOLEAN NOT NULL DEFAULT FALSE;

-- Medicamento: apto para fraccionamiento (restricción INVIMA empaque primario)
ALTER TABLE medicamentos
    ADD COLUMN IF NOT EXISTS apto_fraccionamiento BOOLEAN NOT NULL DEFAULT FALSE;

-- Optimistic locking
ALTER TABLE lotes
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE inventario
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- Comentario de estados de lote: CUARENTENA se usa desde la app (enum en Java)
-- EstadoLote: ACTIVO | CUARENTENA | VENCIDO | RETIRADO
-- TipoMovimientoInventario: ENTRADA | SALIDA | AJUSTE | SALIDA_BAJA
