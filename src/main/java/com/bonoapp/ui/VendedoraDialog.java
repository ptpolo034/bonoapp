package com.bonoapp.ui;

import com.bonoapp.model.Vendedora;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Formulario para crear o editar una vendedora. */
public class VendedoraDialog extends JDialog {

    private final JTextField campoNombre = new JTextField(20);
    private final JTextField campoDocumento = new JTextField(20);
    private final JTextField campoTelefono = new JTextField(20);
    private final JCheckBox campoActiva = new JCheckBox("Activa", true);

    private boolean confirmado = false;

    public VendedoraDialog(Frame propietario, Vendedora existente) {
        super(propietario, existente == null ? "Nueva vendedora" : "Editar vendedora", true);
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Nombre", campoNombre);
        Formularios.agregarFila(form, gbc, 1, "Documento", campoDocumento);
        Formularios.agregarFila(form, gbc, 2, "Teléfono", campoTelefono);

        gbc.gridx = 1;
        gbc.gridy = 3;
        form.add(campoActiva, gbc);

        JButton botonGuardar = new JButton("Guardar");
        Theme.estiloPrincipal(botonGuardar);
        JButton botonCancelar = new JButton("Cancelar");
        botonGuardar.addActionListener(e -> guardar());
        botonCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel();
        botones.add(botonCancelar);
        botones.add(botonGuardar);

        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        if (existente != null) {
            campoNombre.setText(existente.getNombre());
            campoDocumento.setText(existente.getDocumento());
            campoTelefono.setText(existente.getTelefono());
            campoActiva.setSelected(existente.isActiva());
        }

        pack();
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonGuardar);
    }

    private void guardar() {
        if (campoNombre.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio", "Datos incompletos",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    public void aplicarA(Vendedora v) {
        v.setNombre(campoNombre.getText().trim());
        v.setDocumento(campoDocumento.getText().trim());
        v.setTelefono(campoTelefono.getText().trim());
        v.setActiva(campoActiva.isSelected());
    }
}
