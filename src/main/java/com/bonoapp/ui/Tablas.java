package com.bonoapp.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.Arrays;

/** Ajustes visuales comunes a todas las tablas (el estilo general se define en Main.aplicarTema). */
final class Tablas {

    private Tablas() {
    }

    /**
     * Deja el encabezado alineado con los datos: a la izquierda, salvo las columnas indicadas (las de
     * montos y cantidades), que van a la derecha. Conserva las flechas de orden que dibuja FlatLaf.
     */
    static void estilizar(JTable tabla, int... columnasDerecha) {
        tabla.setFillsViewportHeight(true);
        // FlatLaf no pinta por si solo el sombreado alterno con estos renderizadores: se aplica aqui, sobre
        // el renderizador que ya tenga cada tipo de columna (incluidos los personalizados de cada pantalla).
        for (Class<?> tipo : new Class<?>[]{Object.class, Number.class, Boolean.class}) {
            tabla.setDefaultRenderer(tipo, conFilaAlterna(tabla.getDefaultRenderer(tipo)));
        }
        JTableHeader encabezado = tabla.getTableHeader();
        encabezado.setReorderingAllowed(false);
        TableCellRenderer base = encabezado.getDefaultRenderer();
        encabezado.setDefaultRenderer((t, valor, seleccionada, foco, fila, columna) -> {
            Component c = base.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            if (c instanceof JLabel etiqueta) {
                int modelo = t.convertColumnIndexToModel(columna);
                boolean derecha = Arrays.stream(columnasDerecha).anyMatch(d -> d == modelo);
                etiqueta.setHorizontalAlignment(derecha ? JLabel.RIGHT : JLabel.LEFT);
            }
            return c;
        });
    }

    /** Envuelve un renderizador para que las filas impares (sin seleccionar) lleven el color de fila alterna. */
    private static TableCellRenderer conFilaAlterna(TableCellRenderer base) {
        return (t, valor, seleccionada, foco, fila, columna) -> {
            Component c = base.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            if (!seleccionada && c instanceof javax.swing.JComponent jc) {
                jc.setOpaque(true);
                jc.setBackground(fila % 2 == 1 ? Theme.FILA_ALTERNA : t.getBackground());
            }
            return c;
        };
    }

    /** Tabla dentro de su area con desplazamiento, con el borde fino comun de la app. */
    static JScrollPane enDesplazamiento(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDE));
        scroll.getViewport().setBackground(Theme.TARJETA);
        return scroll;
    }
}
