package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.db.TratoDAO;
import com.bonoapp.model.Vendedora;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Edita los tratos de una vendedora: los numeros de boleta que ella siempre vende. */
public class TratosDialog extends JDialog {

    private final Vendedora vendedora;
    private final JTextArea campoNumeros = new JTextArea(6, 28);

    public TratosDialog(Frame propietario, Vendedora vendedora) {
        super(propietario, "Tratos de " + vendedora.getNombre(), true);
        this.vendedora = vendedora;
        setLayout(new BorderLayout());

        campoNumeros.setLineWrap(true);
        campoNumeros.setWrapStyleWord(true);
        campoNumeros.setText(String.join(", ", TratoDAO.listarNumeros(vendedora.getId())));

        JLabel ayuda = new JLabel("<html>Escribe los números que esta vendedora siempre vende, "
                + "separados por coma, espacio o uno por línea (ej: 15, 234, 087).<br>"
                + "Aparecen solos al elegir a la vendedora en Registrar entrega.</html>");
        ayuda.setForeground(Theme.TEXTO_SUAVE);

        JScrollPane scroll = new JScrollPane(campoNumeros);
        scroll.setPreferredSize(new Dimension(360, 130));

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 12, 20));
        contenido.add(ayuda, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);

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
    }

    private void guardar() {
        List<String> numeros;
        try {
            numeros = NumerosBoleta.parsear(campoNumeros.getText());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Map<String, Integer> veces = new HashMap<>();
        for (String numero : numeros) {
            if (veces.merge(numero, 1, Integer::sum) > BoletaDAO.MAX_USOS_POR_NUMERO) {
                JOptionPane.showMessageDialog(this,
                        "El número " + numero + " está más de " + BoletaDAO.MAX_USOS_POR_NUMERO
                                + " veces; cada número solo se puede repetir hasta "
                                + BoletaDAO.MAX_USOS_POR_NUMERO + " veces.",
                        "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        try {
            TratoDAO.reemplazar(vendedora.getId(), numeros);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudieron guardar los tratos: " + e.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        dispose();
    }
}
