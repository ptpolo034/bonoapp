package com.bonoapp.ui;

import com.bonoapp.ui.Theme.Tono;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.function.Function;

/**
 * Fabrica de botones con icono y los estilos de la app: el principal va relleno de indigo (uno por
 * pantalla), los demas son neutros y el de eliminar lleva el texto en rojo.
 */
final class Botones {

    private static final int TAMANO_ICONO = 16;

    private Botones() {
    }

    /** Accion principal de la pantalla: relleno de color e icono blanco. */
    static JButton principal(String texto, String icono) {
        JButton b = new JButton(texto, Iconos.get(icono, TAMANO_ICONO, Color.WHITE));
        b.setIconTextGap(8);
        Theme.estiloPrincipal(b);
        return b;
    }

    /** Accion secundaria: boton neutro con icono. */
    static JButton secundario(String texto, String icono) {
        JButton b = new JButton(texto, Iconos.get(icono, TAMANO_ICONO, Theme.TEXTO));
        b.setIconTextGap(8);
        return b;
    }

    /** Accion destructiva (eliminar): neutro, con texto e icono en rojo. */
    static JButton peligro(String texto, String icono) {
        JButton b = new JButton(texto, Iconos.get(icono, TAMANO_ICONO, Theme.PELIGRO));
        b.setIconTextGap(8);
        Theme.estiloPeligro(b);
        return b;
    }

    /**
     * Fila de botones de una pantalla: los de "izquierda" juntos al borde izquierdo (alineados con la tabla)
     * y, opcionalmente, un boton aparte a la derecha (el de eliminar, para no pulsarlo sin querer).
     */
    static JPanel fila(JButton derecha, JButton... izquierda) {
        JPanel grupo = new JPanel();
        grupo.setOpaque(false);
        grupo.setLayout(new BoxLayout(grupo, BoxLayout.X_AXIS));
        for (int i = 0; i < izquierda.length; i++) {
            if (i > 0) {
                grupo.add(Box.createHorizontalStrut(8));
            }
            grupo.add(izquierda[i]);
        }
        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        fila.add(grupo, BorderLayout.WEST);
        if (derecha != null) {
            fila.add(derecha, BorderLayout.EAST);
        }
        return fila;
    }

    /** Funcion de tono para estados de boleta: vendida = verde, devuelta = ambar, entregada = neutro. */
    static final Function<String, Tono> TONO_ESTADO_BOLETA = texto -> switch (texto) {
        case "Vendida" -> Tono.EXITO;
        case "Devuelta" -> Tono.ALERTA;
        default -> Tono.NEUTRO;
    };
}
