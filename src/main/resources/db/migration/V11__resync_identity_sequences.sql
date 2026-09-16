-- Forzar resincronización de secuencias IDENTITY / SERIAL
-- (corrige duplicate key en movimientos_inventario y otras tablas)

DO $$
DECLARE
  t TEXT;
  seq_name TEXT;
  max_id BIGINT;
  tables TEXT[] := ARRAY[
    'movimientos_inventario',
    'ventas',
    'detalle_venta',
    'domicilios',
    'lotes',
    'inventario',
    'compras',
    'detalle_compra',
    'devoluciones',
    'detalle_devolucion',
    'clientes',
    'empleados',
    'usuarios',
    'medicamentos',
    'productos',
    'proveedores',
    'categorias',
    'formulas_medicas',
    'detalle_formula',
    'venta_pagos',
    'precio_historial',
    'puntos_movimiento',
    'alerta_recompra'
  ];
BEGIN
  FOREACH t IN ARRAY tables
  LOOP
    IF EXISTS (
      SELECT 1 FROM information_schema.tables
      WHERE table_schema = 'public' AND table_name = t
    ) THEN
      BEGIN
        seq_name := pg_get_serial_sequence(t, 'id');
        IF seq_name IS NOT NULL THEN
          EXECUTE format('SELECT COALESCE(MAX(id), 0) FROM %I', t) INTO max_id;
          -- is_called = true cuando max_id > 0 → el próximo nextval será max_id+1
          PERFORM setval(seq_name, GREATEST(max_id, 1), max_id > 0);
          RAISE NOTICE 'Secuencia % sincronizada a max_id=%', seq_name, max_id;
        END IF;
      EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE 'No se pudo sincronizar %: %', t, SQLERRM;
      END;
    END IF;
  END LOOP;
END $$;
