CREATE TABLE IF NOT EXISTS venta_pagos (
    id              BIGSERIAL PRIMARY KEY,
    venta_id        BIGINT NOT NULL REFERENCES ventas(id) ON DELETE CASCADE,
    metodo_pago     VARCHAR(25) NOT NULL,
    monto           NUMERIC(12,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_venta_pagos_venta ON venta_pagos(venta_id);
