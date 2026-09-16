-- Domicilio sin cliente registrado (despacho a consumidor final / datos de entrega libres)
ALTER TABLE domicilios ALTER COLUMN cliente_id DROP NOT NULL;

-- Resincronizar secuencias IDENTITY (evita duplicate key tras seeds / inserts manuales)
DO $$
DECLARE
  r RECORD;
  seq TEXT;
  max_id BIGINT;
BEGIN
  FOR r IN
    SELECT c.table_name, c.column_name
    FROM information_schema.columns c
    WHERE c.table_schema = 'public'
      AND c.column_default LIKE 'nextval%'
  LOOP
    seq := pg_get_serial_sequence(format('%I', r.table_name), r.column_name);
    IF seq IS NOT NULL THEN
      EXECUTE format('SELECT COALESCE(MAX(%I), 0) FROM %I', r.column_name, r.table_name) INTO max_id;
      PERFORM setval(seq, GREATEST(max_id, 1), max_id > 0);
    END IF;
  END LOOP;

  -- Tablas clave por si alguna no aparece en information_schema.column_default
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'movimientos_inventario') THEN
    PERFORM setval(
      pg_get_serial_sequence('movimientos_inventario', 'id'),
      GREATEST((SELECT COALESCE(MAX(id), 0) FROM movimientos_inventario), 1),
      (SELECT COALESCE(MAX(id), 0) FROM movimientos_inventario) > 0
    );
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ventas') THEN
    PERFORM setval(
      pg_get_serial_sequence('ventas', 'id'),
      GREATEST((SELECT COALESCE(MAX(id), 0) FROM ventas), 1),
      (SELECT COALESCE(MAX(id), 0) FROM ventas) > 0
    );
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'domicilios') THEN
    PERFORM setval(
      pg_get_serial_sequence('domicilios', 'id'),
      GREATEST((SELECT COALESCE(MAX(id), 0) FROM domicilios), 1),
      (SELECT COALESCE(MAX(id), 0) FROM domicilios) > 0
    );
  END IF;
  IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'detalle_venta') THEN
    PERFORM setval(
      pg_get_serial_sequence('detalle_venta', 'id'),
      GREATEST((SELECT COALESCE(MAX(id), 0) FROM detalle_venta), 1),
      (SELECT COALESCE(MAX(id), 0) FROM detalle_venta) > 0
    );
  END IF;
END $$;
