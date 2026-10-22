package com.bonoapp.ui;

import com.bonoapp.db.PrestamoDAO;
import com.bonoapp.db.VendedoraDAO;
import com.bonoapp.model.Vendedora;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
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

/** Formulario para registrar un prestamo: a quien, cuanto, para que (concepto) y en que fecha. */
public class PrestamoDialog extends JDialog {

    private final List<Vendedora> todasActivas;
    private final JTextField campoBuscarVendedora = new JTextField(20);
    private final JComboBox<Vendedora> comboVendedora = new JComboBox<>();
    private final JTextField campoMonto = new JTextField(12);
    private final JDateChooser campoFecha = new JDateChooser(new Date());
    private final JTextField campoConcepto = new JTextField(24);

    private boolean confirmado = false;

    public PrestamoDialog(Frame propietario) {
        super(propietario, "Nuevo préstamo", true);
        setLayout(new BorderLayout());

        todasActivas = VendedoraDAO.listarActivas();
        filtrarVendedoras();
        campoBuscarVendedora.getDocument().addDocumentListener(new BuscadorListener(this::filtrarVendedoras));
        campoFecha.setDateFormatString("dd/MM/yyyy");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Buscar vendedora (nombre o documento)", campoBuscarVendedora);
        Formularios.agregarFila(form, gbc, 1, "Vendedora", comboVendedora);
        Formularios.agregarFila(form, gbc, 2, "Monto prestado", campoMonto);
        Formularios.agregarFila(form, gbc, 3, "Fecha del préstamo", campoFecha);
        Formularios.agregarFila(form, gbc, 4, "Concepto", campoConcepto);

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

    private void filtrarVendedoras() {
        Formularios.filtrarVendedoras(comboVendedora, todasActivas, campoBuscarVendedora.getText());
    }

    private void guardar() {
        Vendedora vendedora = (Vendedora) comboVendedora.getSelectedItem();
        if (vendedora == null) {
            JOptionPane.showMessageDialog(this,
                    todasActivas.isEmpty() ? "No hay vendedoras activas" : "Selecciona una vendedora de la lista",
                    "Sin vendedora", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int monto;
        try {
            monto = Formularios.leerPesos(campoMonto.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El monto debe ser un número (ej: 50000 o 50.000)",
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (monto <= 0) {
            JOptionPane.showMessageDialog(this, "El monto debe ser mayor a cero",
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (campoConcepto.getText().isBlank()) {
            JOptionPane.showMessageDialog(this, "Escribe el concepto del préstamo",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate fecha = Formularios.aLocalDate(campoFecha.getDate());
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Selecciona la fecha del préstamo",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            PrestamoDAO.insertar(vendedora.getId(), monto, campoConcepto.getText(), fecha);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el préstamo: " + e.getMessage(),
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
