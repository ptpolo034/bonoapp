package com.bonoapp.ui;

import com.bonoapp.db.AhorroDAO;
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

/** Formulario para registrar un ahorro de una vendedora, o un retiro de lo que ya tiene ahorrado. */
public class RegistrarAhorroDialog extends JDialog {

    private final boolean retiro;
    private final List<Vendedora> todasActivas;
    private final JTextField campoBuscarVendedora = new JTextField(20);
    private final JComboBox<Vendedora> comboVendedora = new JComboBox<>();
    private final JTextField campoMonto = new JTextField(12);
    private final JDateChooser campoFecha = new JDateChooser(new Date());
    private final JTextField campoConcepto;

    private boolean confirmado = false;

    public RegistrarAhorroDialog(Frame propietario, boolean retiro) {
        super(propietario, retiro ? "Registrar retiro de ahorro" : "Registrar ahorro", true);
        this.retiro = retiro;
        campoConcepto = new JTextField(retiro ? "Retiro de ahorro" : "Ahorro", 24);
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
        Formularios.agregarFila(form, gbc, 2, retiro ? "Monto a retirar" : "Monto a ahorrar", construirFilaMonto());
        Formularios.agregarFila(form, gbc, 3, "Fecha", campoFecha);
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

    /**
     * El campo del monto. En un retiro lleva al lado el boton "Saldo completo" (igual que el abono), que
     * escribe todo lo que la vendedora elegida tiene ahorrado.
     */
    private JPanel construirFilaMonto() {
        JPanel fila = new JPanel(new BorderLayout(8, 0));
        fila.setOpaque(false);
        fila.add(campoMonto, BorderLayout.CENTER);
        if (retiro) {
            JButton botonSaldo = new JButton("Saldo completo");
            botonSaldo.setToolTipText("Retirar todo el ahorro de la vendedora elegida");
            botonSaldo.addActionListener(e -> usarSaldoCompleto());
            fila.add(botonSaldo, BorderLayout.EAST);
        }
        return fila;
    }

    private void usarSaldoCompleto() {
        Vendedora vendedora = (Vendedora) comboVendedora.getSelectedItem();
        if (vendedora == null) {
            JOptionPane.showMessageDialog(this, "Primero selecciona la vendedora",
                    "Sin vendedora", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int saldo = AhorroDAO.saldoDe(vendedora.getId());
        if (saldo <= 0) {
            JOptionPane.showMessageDialog(this, vendedora.getNombre() + " no tiene ahorro para retirar.",
                    "Sin ahorro", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        campoMonto.setText(String.valueOf(saldo));
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
        LocalDate fecha = Formularios.aLocalDate(campoFecha.getDate());
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Selecciona la fecha",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String concepto = campoConcepto.getText().trim();
        if (concepto.isEmpty()) {
            concepto = retiro ? "Retiro de ahorro" : "Ahorro";
        }

        try {
            if (retiro) {
                AhorroDAO.retirar(vendedora.getId(), monto, fecha, concepto);
            } else {
                AhorroDAO.registrarManual(vendedora.getId(), monto, fecha, concepto);
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(),
                    retiro ? "No se pudo retirar" : "No se pudo guardar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}
