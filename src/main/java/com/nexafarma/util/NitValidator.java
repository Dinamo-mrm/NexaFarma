package com.nexafarma.util;

/**
 * Valida el dígito de verificación (DV) del NIT colombiano según el algoritmo
 * oficial de la DIAN (factores 3,7,13,17,19,23,29,37,41,43,47,53,59,67,71).
 *
 * Acepta NIT con o sin guion/DV explícito. Si el DV viene en el string
 * (ej. "900123456-1" o "9001234561"), se verifica; si solo llega la base
 * numérica sin DV, se rechaza para forzar identificación completa.
 */
public final class NitValidator {

    private static final int[] FACTORES = {
            3, 7, 13, 17, 19, 23, 29, 37, 41, 43, 47, 53, 59, 67, 71
    };

    private NitValidator() {
    }

    /**
     * @param nitRaw NIT tal como lo ingresa el usuario (puede incluir puntos, espacios o guion)
     * @return true si el DV es correcto
     */
    public static boolean esValido(String nitRaw) {
        if (nitRaw == null || nitRaw.isBlank()) {
            return false;
        }
        String limpio = nitRaw.replaceAll("[^0-9]", "");
        if (limpio.length() < 2 || limpio.length() > 16) {
            return false;
        }

        int dvIngresado = Character.getNumericValue(limpio.charAt(limpio.length() - 1));
        String base = limpio.substring(0, limpio.length() - 1);
        int dvCalculado = calcularDigitoVerificacion(base);
        return dvCalculado == dvIngresado;
    }

    /**
     * Calcula el DV a partir de la base numérica del NIT (sin el dígito de verificación).
     */
    public static int calcularDigitoVerificacion(String baseNumerica) {
        if (baseNumerica == null || baseNumerica.isBlank() || !baseNumerica.matches("\\d+")) {
            throw new IllegalArgumentException("Base de NIT inválida");
        }
        int suma = 0;
        int factorIdx = 0;
        for (int i = baseNumerica.length() - 1; i >= 0; i--) {
            int digito = Character.getNumericValue(baseNumerica.charAt(i));
            suma += digito * FACTORES[factorIdx % FACTORES.length];
            factorIdx++;
        }
        int residuo = suma % 11;
        if (residuo == 0 || residuo == 1) {
            return residuo;
        }
        return 11 - residuo;
    }

    /** Normaliza a solo dígitos (base + DV) para persistencia consistente. */
    public static String normalizar(String nitRaw) {
        if (nitRaw == null) {
            return null;
        }
        return nitRaw.replaceAll("[^0-9]", "");
    }
}
