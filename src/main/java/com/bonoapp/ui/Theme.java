package com.bonoapp.ui;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

/**
 * Colores de la aplicacion en un solo lugar para cambiarlos facilmente.
 *
 * Regla de color: el indigo es el unico color de marca; verde, ambar y rojo solo significan estado
 * (al dia, pendiente, deuda o error) y el gris es lo neutral.
 */
public final class Theme {

    /** Significado de un color de estado (etiquetas, avisos, tarjetas). */
    public enum Tono { EXITO, ALERTA, PELIGRO, NEUTRO, MARCA }

    public static final Color MENU_FONDO = new Color(0x1F2937);
    public static final Color MENU_TEXTO = new Color(0xE5E7EB);
    public static final Color MENU_HOVER = new Color(0x374151);
    public static final Color PRINCIPAL = new Color(0x4F46E5);
    public static final Color PRINCIPAL_SUAVE = new Color(0xEEF2FF);
    public static final Color FONDO = new Color(0xF3F4F6);
    public static final Color TARJETA = Color.WHITE;
    public static final Color TEXTO = new Color(0x111827);
    public static final Color TEXTO_SUAVE = new Color(0x6B7280);
    public static final Color BORDE = new Color(0xE5E7EB);
    public static final Color FILA_ALTERNA = new Color(0xF9FAFB);
    public static final Color SELECCION = new Color(0xE0E7FF);
    public static final Color EXITO = new Color(0x15803D);
    public static final Color ALERTA = new Color(0xB45309);
    public static final Color PELIGRO = new Color(0xB91C1C);

    private static final Color EXITO_FONDO = new Color(0xDCFCE7);
    private static final Color ALERTA_FONDO = new Color(0xFEF3C7);
    private static final Color PELIGRO_FONDO = new Color(0xFEE2E2);
    private static final Color NEUTRO_FONDO = new Color(0xE5E7EB);
    private static final Color NEUTRO_TEXTO = new Color(0x374151);

    private Theme() {
    }

    /** Color de fondo de una etiqueta o chip del tono dado. */
    public static Color fondoTono(Tono tono) {
        return switch (tono) {
            case EXITO -> EXITO_FONDO;
            case ALERTA -> ALERTA_FONDO;
            case PELIGRO -> PELIGRO_FONDO;
            case MARCA -> PRINCIPAL_SUAVE;
            case NEUTRO -> NEUTRO_FONDO;
        };
    }

    /** Color del texto (o del icono) sobre {@link #fondoTono(Tono)}. */
    public static Color textoTono(Tono tono) {
        return switch (tono) {
            case EXITO -> EXITO;
            case ALERTA -> ALERTA;
            case PELIGRO -> PELIGRO;
            case MARCA -> PRINCIPAL;
            case NEUTRO -> NEUTRO_TEXTO;
        };
    }

    /** Da a un boton el estilo de accion principal de la app (relleno con el color de acento). */
    public static void estiloPrincipal(AbstractButton boton) {
        boton.putClientProperty("FlatLaf.style",
                "background: " + hex(PRINCIPAL) + "; foreground: #FFFFFF; borderWidth: 0;"
                        + "hoverBackground: " + hex(PRINCIPAL.darker()) + "; pressedBackground: "
                        + hex(PRINCIPAL.darker().darker()));
    }

    /** Boton de una accion destructiva (eliminar): neutro, pero con el texto en rojo para no pulsarlo sin querer. */
    public static void estiloPeligro(AbstractButton boton) {
        boton.setForeground(PELIGRO);
    }

    /** Convierte un color a su forma "#RRGGBB" para usarlo en estilos de FlatLaf. */
    public static String hex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    /** Panel con el estilo de "tarjeta" comun a todas las pantallas (fondo, borde, esquinas). */
    public static JPanel crearTarjeta() {
        JPanel p = new JPanel();
        p.setBackground(TARJETA);
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        p.putClientProperty("FlatLaf.style", "arc: 20");
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        return p;
    }
}
