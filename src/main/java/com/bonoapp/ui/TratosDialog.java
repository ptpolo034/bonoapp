package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.db.TratoDAO;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.model.Vendedora;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Edita los tratos de una vendedora: los numeros de boleta que ella siempre vende. Los de la Diaria y los del
 * Premio mayor son independientes: cada tipo tiene su propia lista y las dos se guardan juntas.
 */
public class TratosDialog extends JDialog {

    private final Vendedora vendedora;
    private final Map<TipoBoleta, JTextArea> campos = new EnumMap<>(TipoBoleta.class);
    private final Map<TipoBoleta, List<String>> originales = new EnumMap<>(TipoBoleta.class);

    /** @param tipoInicial el tipo cuya lista queda lista para escribir al abrir la ventana */
    public TratosDialog(Frame propietario, Vendedora vendedora, TipoBoleta tipoInicial) {
        super(propietario, "Tratos de " + vendedora.getNombre(), true);
        this.vendedora = vendedora;
        setLayout(new BorderLayout());

        JLabel ayuda = new JLabel("<html><body style='width:480px'>Escribe los números que esta vendedora siempre vende, "
                + "separados por coma, espacio o uno por línea (ej: 15, 234, 087). "
                + "Cada tipo de boleta tiene sus propios tratos: lo que pongas en la Diaria no cambia el Premio mayor. "
                + "Aparecen solos al elegir a la vendedora en Registrar entrega.</body></html>");
        ayuda.setForeground(Theme.TEXTO_SUAVE);

        JPanel columnas = new JPanel(new GridLayout(1, 0, 16, 0));
        for (TipoBoleta tipo : TipoBoleta.values()) {
            columnas.add(construirColumna(tipo));
        }

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 12, 20));
        contenido.add(ayuda, BorderLayout.NORTH);
        contenido.add(columnas, BorderLayout.CENTER);

        JButton botonGuardar = new JButton("Guardar");
        Theme.estiloPrincipal(botonGuardar);
        JButton botonCancelar = new JButton("Cancelar");
        botonGuardar.addActionListener(e -> guardar());
        botonCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.add(botonCancelar);
        botones.add(botonGuardar);

        add(contenido, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonGuardar);
        // El cursor queda en la lista del tipo con el que se abrio la ventana.
        SwingUtilities.invokeLater(() -> campos.get(tipoInicial == null ? TipoBoleta.DIARIA : tipoInicial).requestFocusInWindow());
    }

    private JPanel construirColumna(TipoBoleta tipo) {
        JTextArea campo = new JTextArea(6, 20);
        campo.setLineWrap(true);
        campo.setWrapStyleWord(true);
        List<String> guardados = TratoDAO.listarNumeros(vendedora.getId(), tipo);
        originales.put(tipo, guardados);
        campo.setText(String.join(", ", guardados));
        campos.put(tipo, campo);

        JLabel titulo = new JLabel(tipo.getEtiqueta());
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, 13f));

        JScrollPane scroll = new JScrollPane(campo);
        scroll.setPreferredSize(new Dimension(230, 130));

        JPanel columna = new JPanel(new BorderLayout(0, 6));
        columna.add(titulo, BorderLayout.NORTH);
        columna.add(scroll, BorderLayout.CENTER);
        return columna;
    }

    private static List<String> ordenados(List<String> numeros) {
        return numeros.stream().sorted().toList();
    }

    private void guardar() {
        Map<TipoBoleta, List<String>> numerosPorTipo = new EnumMap<>(TipoBoleta.class);
        for (TipoBoleta tipo : TipoBoleta.values()) {
            List<String> numeros;
            try {
                numeros = NumerosBoleta.parsear(campos.get(tipo).getText());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, tipo.getEtiqueta() + ": " + e.getMessage(),
                        "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                campos.get(tipo).requestFocusInWindow();
                return;
            }
            Map<String, Integer> veces = new HashMap<>();
            for (String numero : numeros) {
                if (veces.merge(numero, 1, Integer::sum) > BoletaDAO.MAX_USOS_POR_NUMERO) {
                    JOptionPane.showMessageDialog(this,
                            tipo.getEtiqueta() + ": el número " + numero + " está más de " + BoletaDAO.MAX_USOS_POR_NUMERO
                                    + " veces; cada número solo se puede repetir hasta "
                                    + BoletaDAO.MAX_USOS_POR_NUMERO + " veces.",
                            "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                    campos.get(tipo).requestFocusInWindow();
                    return;
                }
            }
            // Solo se guardan (y se anotan en el historial) los tipos que realmente cambiaron.
            if (!ordenados(numeros).equals(ordenados(originales.get(tipo)))) {
                numerosPorTipo.put(tipo, numeros);
            }
        }
        if (numerosPorTipo.isEmpty()) {
            dispose();
            return;
        }
        try {
            TratoDAO.reemplazar(vendedora.getId(), numerosPorTipo);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron guardar los tratos: " + e.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        dispose();
    }
}
