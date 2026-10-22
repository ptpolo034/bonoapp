package com.bonoapp.ui;

import com.bonoapp.db.AbonoDAO;
import com.bonoapp.model.Prestamo;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;

/**
 * Formulario para REGISTRAR un abono (pago) NUEVO a un prestamo (singular: un solo abono).
 * Se abre con el boton "Registrar abono" de Prestamos. No confundir con {@link AbonosDialog} (plural),
 * que solo LISTA los abonos que el prestamo ya tiene.
 */
public class AbonoDialog extends JDialog {

    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private final Prestamo prestamo;
    private final JTextField campoMonto = new JTextField(12);
    private final JDateChooser campoFecha = new JDateChooser(new Date());
    private final JTextField campoNota = new JTextField(24);

    private boolean confirmado = false;

    public AbonoDialog(Frame propietario, Prestamo prestamo) {
        super(propietario, "Registrar abono", true);
        this.prestamo = prestamo;
        setLayout(new BorderLayout());
        campoFecha.setDateFormatString("dd/MM/yyyy");

        JPanel contenido = new JPanel(new BorderLayout(0, 16));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 8, 20));
        contenido.add(construirResumen(), BorderLayout.NORTH);
        contenido.add(construirFormulario(), BorderLayout.CENTER);

        JButton botonGuardar = new JButton("Guardar abono");
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

    /** Tarjeta con a quien se le presto, para que y como va la deuda. */
    private JPanel construirResumen() {
        JPanel tarjeta = Theme.crearTarjeta();
        tarjeta.setBackground(Theme.FONDO);
        tarjeta.setLayout(new BorderLayout(0, 10));

        JLabel nombre = new JLabel(prestamo.vendedoraNombre());
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 16f));
        JLabel concepto = new JLabel(prestamo.concepto());
        concepto.setForeground(Theme.TEXTO_SUAVE);
        JPanel encabezado = new JPanel(new GridLayout(2, 1));
        encabezado.setOpaque(false);
        encabezado.add(nombre);
        encabezado.add(concepto);

        JPanel cifras = new JPanel(new GridLayout(1, 3, 24, 0));
        cifras.setOpaque(false);
        cifras.add(cifra("Prestado", prestamo.monto(), false));
        cifras.add(cifra("Abonado", prestamo.abonado(), false));
        cifras.add(cifra("Saldo", prestamo.saldo(), true));

        tarjeta.add(encabezado, BorderLayout.NORTH);
        tarjeta.add(cifras, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel cifra(String etiqueta, int valor, boolean destacada) {
        JLabel titulo = new JLabel(etiqueta);
        titulo.setForeground(Theme.TEXTO_SUAVE);
        JLabel numero = new JLabel("$ " + FORMATO_PESOS.format(valor));
        numero.setFont(numero.getFont().deriveFont(Font.BOLD, destacada ? 18f : 14f));
        if (destacada) {
            numero.setForeground(Theme.PRINCIPAL);
        }
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.setOpaque(false);
        p.add(titulo);
        p.add(numero);
        return p;
    }

    private JPanel construirFormulario() {
        JButton botonSaldo = new JButton("Saldo completo");
        botonSaldo.addActionListener(e -> campoMonto.setText(String.valueOf(prestamo.saldo())));
        JPanel filaMonto = new JPanel(new BorderLayout(8, 0));
        filaMonto.add(campoMonto, BorderLayout.CENTER);
        filaMonto.add(botonSaldo, BorderLayout.EAST);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Formularios.agregarFila(form, gbc, 0, "Monto del abono", filaMonto);
        Formularios.agregarFila(form, gbc, 1, "Fecha del abono", campoFecha);
        Formularios.agregarFila(form, gbc, 2, "Nota (opcional)", campoNota);
        return form;
    }

    private void guardar() {
        int monto;
        try {
            monto = Formularios.leerPesos(campoMonto.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El monto debe ser un número (ej: 20000 o 20.000)",
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate fecha = Formularios.aLocalDate(campoFecha.getDate());
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Selecciona la fecha del abono",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            AbonoDAO.registrar(prestamo.id(), monto, fecha, campoNota.getText());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se puede abonar", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar el abono: " + e.getMessage(),
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
