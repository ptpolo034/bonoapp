package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.model.Boleta;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/** Formulario para corregir el número y la fecha de entrega de una boleta. */
public class EditarBoletaDialog extends JDialog {

    private final Boleta boleta;
    private final JTextField campoNumero = new JTextField(8);
    private final JDateChooser campoFecha = new JDateChooser();

    private boolean confirmado = false;

    public EditarBoletaDialog(Frame propietario, Boleta boleta) {
        super(propietario, "Editar boleta", true);
        this.boleta = boleta;
        setLayout(new BorderLayout());

        campoNumero.setText(boleta.getNumero() == null ? "" : boleta.getNumero());
        campoFecha.setDateFormatString("dd/MM/yyyy");
        campoFecha.setDate(java.sql.Date.valueOf(boleta.getFechaEntrega()));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Vendedora", new JLabel(boleta.getVendedoraNombre()));
        Formularios.agregarFila(form, gbc, 1, "Tipo", new JLabel(boleta.getTipo().getEtiqueta()));
        Formularios.agregarFila(form, gbc, 2, "Número (3 cifras, vacío = sin número)", campoNumero);
        Formularios.agregarFila(form, gbc, 3, "Fecha de entrega", campoFecha);

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

        pack();
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonGuardar);
    }

    private void guardar() {
        String numero = null;
        if (!campoNumero.getText().isBlank()) {
            List<String> numeros;
            try {
                numeros = NumerosBoleta.parsear(campoNumero.getText());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Número inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (numeros.size() != 1) {
                JOptionPane.showMessageDialog(this, "Escribe un solo número por boleta",
                        "Número inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
            numero = numeros.get(0);
        }
        LocalDate fecha = Formularios.aLocalDate((Date) campoFecha.getDate());
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Selecciona la fecha de entrega",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            BoletaDAO.actualizar(boleta.getId(), numero, fecha);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la boleta: " + e.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}
