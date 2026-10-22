package com.bonoapp.ui;

import com.bonoapp.ui.Theme.Tono;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.function.Function;

/**
 * Pinta una celda de tabla como una etiqueta de color (chip). Una funcion decide el tono segun el
 * texto de la celda, por ejemplo "Vendida" -> verde. Respeta la fila alterna y la seleccion de la tabla.
 */
final class ChipRenderer implements TableCellRenderer {

    private final Function<String, Tono> tonoPorTexto;
    private final JPanel panel = new JPanel(new GridBagLayout());
    private final Chip chip = new Chip();

    ChipRenderer(Function<String, Tono> tonoPorTexto) {
        this.tonoPorTexto = tonoPorTexto;
        panel.setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        // GridBag para que la etiqueta quede centrada en vertical y pegada a la izquierda de la celda.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1;
        panel.add(chip, gbc);
    }

    @Override
    public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                   boolean foco, int fila, int columna) {
        String texto = valor == null ? "" : valor.toString();
        Color alterna = UIManager.getColor("Table.alternateRowColor");
        panel.setBackground(seleccionada ? tabla.getSelectionBackground()
                : (fila % 2 == 1 && alterna != null ? alterna : tabla.getBackground()));
        chip.setText(texto);
        chip.setFont(tabla.getFont().deriveFont(tabla.getFont().getSize2D() - 1.5f));
        chip.setTono(tonoPorTexto.apply(texto));
        return panel;
    }
}
