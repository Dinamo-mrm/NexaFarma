package com.nexafarma.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Generador mínimo de PDF 1.4 sin dependencias externas.
 * Suficiente para comprobantes de texto (factura de farmacia).
 */
public final class SimplePdfWriter {

    private final List<String> lines = new ArrayList<>();
    private final int fontSize;

    public SimplePdfWriter() {
        this(11);
    }

    public SimplePdfWriter(int fontSize) {
        this.fontSize = fontSize;
    }

    public SimplePdfWriter line(String text) {
        lines.add(text == null ? "" : text);
        return this;
    }

    public SimplePdfWriter blank() {
        lines.add("");
        return this;
    }

    public byte[] build() {
        // Página A4: 595 x 842 pt
        float yStart = 800;
        float leading = fontSize + 4;
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 ").append(fontSize).append(" Tf\n");
        float y = yStart;
        for (String raw : lines) {
            String safe = escapePdf(raw);
            content.append("50 ").append(String.format("%.1f", y)).append(" Td\n(").append(safe).append(") Tj\n0 -")
                    .append(String.format("%.1f", leading)).append(" Td\n");
            y -= leading;
            if (y < 50) {
                // sin multipágina por simplicidad
                break;
            }
        }
        content.append("ET");
        byte[] stream = content.toString().getBytes(StandardCharsets.US_ASCII);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            List<Integer> offsets = new ArrayList<>();
            write(out, "%PDF-1.4\n");
            // obj 1 catalog
            offsets.add(out.size());
            write(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
            // obj 2 pages
            offsets.add(out.size());
            write(out, "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
            // obj 3 page
            offsets.add(out.size());
            write(out, "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                    + "/Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>\nendobj\n");
            // obj 4 content
            offsets.add(out.size());
            write(out, "4 0 obj\n<< /Length " + stream.length + " >>\nstream\n");
            out.write(stream);
            write(out, "\nendstream\nendobj\n");
            // obj 5 font
            offsets.add(out.size());
            write(out, "5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n");

            int xrefPos = out.size();
            write(out, "xref\n0 " + (offsets.size() + 1) + "\n");
            write(out, "0000000000 65535 f \n");
            for (int off : offsets) {
                write(out, String.format("%010d 00000 n \n", off));
            }
            write(out, "trailer\n<< /Size " + (offsets.size() + 1) + " /Root 1 0 R >>\n");
            write(out, "startxref\n" + xrefPos + "\n%%EOF\n");
        } catch (IOException e) {
            throw new IllegalStateException("Error generando PDF", e);
        }
        return out.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String s) throws IOException {
        out.write(s.getBytes(StandardCharsets.US_ASCII));
    }

    private static String escapePdf(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c == '\\' || c == '(' || c == ')') {
                sb.append('\\').append(c);
            } else if (c > 127) {
                // Helvetica no tiene acentos: transliteración básica
                sb.append(stripAccent(c));
            } else if (c < 32) {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static char stripAccent(char c) {
        return switch (c) {
            case 'á', 'à', 'ä', 'â' -> 'a';
            case 'é', 'è', 'ë', 'ê' -> 'e';
            case 'í', 'ì', 'ï', 'î' -> 'i';
            case 'ó', 'ò', 'ö', 'ô' -> 'o';
            case 'ú', 'ù', 'ü', 'û' -> 'u';
            case 'ñ' -> 'n';
            case 'Á', 'À', 'Ä', 'Â' -> 'A';
            case 'É', 'È', 'Ë', 'Ê' -> 'E';
            case 'Í', 'Ì', 'Ï', 'Î' -> 'I';
            case 'Ó', 'Ò', 'Ö', 'Ô' -> 'O';
            case 'Ú', 'Ù', 'Ü', 'Û' -> 'U';
            case 'Ñ' -> 'N';
            case 'º', '°' -> 'o';
            case '–', '—' -> '-';
            default -> '?';
        };
    }
}
