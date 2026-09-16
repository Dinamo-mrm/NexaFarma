-- Venta anónima / consumidor final: cliente opcional
ALTER TABLE ventas ALTER COLUMN cliente_id DROP NOT NULL;
