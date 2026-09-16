package com.nexafarma.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Al arrancar: alinea secuencias IDENTITY de PostgreSQL con MAX(id).
 * Crítico tras seeds en Supabase (evita duplicate key ..._pkey).
 */
@Component
@Order(1)
public class SequenceResyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SequenceResyncRunner.class);

    private static final String[] TABLES = {
            "movimientos_inventario",
            "ventas",
            "detalle_venta",
            "domicilios",
            "lotes",
            "inventario",
            "compras",
            "detalle_compra",
            "devoluciones",
            "clientes",
            "empleados",
            "usuarios",
            "productos",
            "medicamentos",
            "proveedores",
            "formulas_medicas",
            "venta_pagos",
            "precio_historial"
    };

    private final JdbcTemplate jdbc;

    public SequenceResyncRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String table : TABLES) {
            resync(table);
        }
        // Extra explícito para la tabla que está fallando en POS
        resync("movimientos_inventario");
        log.info("SequenceResyncRunner finalizado");
    }

    private void resync(String table) {
        try {
            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = ?",
                    Integer.class, table);
            if (exists == null || exists == 0) {
                return;
            }
            Long maxId = jdbc.queryForObject(
                    "SELECT COALESCE(MAX(id), 0) FROM " + table, Long.class);
            if (maxId == null) {
                maxId = 0L;
            }
            String seq = null;
            try {
                seq = jdbc.queryForObject(
                        "SELECT pg_get_serial_sequence(?, 'id')", String.class, table);
            } catch (Exception ignored) {
                // ignore
            }
            if (seq == null || seq.isBlank()) {
                seq = table + "_id_seq";
            }
            Long newVal;
            if (maxId > 0) {
                newVal = jdbc.queryForObject("SELECT setval(?, ?, true)", Long.class, seq, maxId);
            } else {
                newVal = jdbc.queryForObject("SELECT setval(?, 1, false)", Long.class, seq);
            }
            log.info("Secuencia OK: {} max_id={} setval={}", seq, maxId, newVal);
        } catch (Exception e) {
            log.warn("Secuencia {} no sincronizada: {}", table, e.getMessage());
        }
    }
}
