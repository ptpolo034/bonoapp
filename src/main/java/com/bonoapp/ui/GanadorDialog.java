package com.bonoapp.ui;

import com.bonoapp.db.GanadorDAO;
import com.bonoapp.model.Boleta;
import com.bonoapp.model.TipoBoleta;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Formulario para registrar el numero ganador de un sorteo (por tipo de boleta y fecha). */
public class GanadorDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JComboBox<TipoBoleta> comboTipo = new JComboBox<>(TipoBoleta.values());
    private final JDateChooser campoFecha = new JDateChooser(new Date());
    private final JTextField campoNumero = new JTextField(6);

    private boolean confirmado = false;

    public GanadorDialog(Frame propietario) {
        super(propietario, "Número ganador", true);
        setLayout(new BorderLayout());
        campoFecha.setDateFormatString("dd/MM/yyyy");
        campoNumero.setFont(campoNumero.getFont().deriveFont(Font.BOLD, 20f));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 8, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Sorteo", comboTipo);
        Formularios.agregarFila(form, gbc, 1, "Fecha del sorteo", campoFecha);
        Formularios.agregarFila(form, gbc, 2, "Número ganador (3 cifras)", campoNumero);

        JLabel nota = new JLabel("Si ya habías registrado ese sorteo y esa fecha, se corrige el número.");
        nota.setForeground(Theme.TEXTO_SUAVE);
        nota.setBorder(BorderFactory.createEmptyBorder(0, 26, 8, 20));

        JButton botonGuardar = new JButton("Guardar");
        Theme.estiloPrincipal(botonGuardar);
        JButton botonCancelar = new JButton("Cancelar");
        botonGuardar.addActionListener(e -> guardar());
        botonCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.add(botonCancelar);
        botones.add(botonGuardar);

        add(form, BorderLayout.NORTH);
        add(nota, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonGuardar);
    }

    private void guardar() {
        TipoBoleta tipo = (TipoBoleta) comboTipo.getSelectedItem();
        LocalDate fecha = Formularios.aLocalDate(campoFecha.getDate());
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Selecciona la fecha del sorteo",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        List<String> numeros;
        try {
            numeros = NumerosBoleta.parsear(campoNumero.getText());
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Número inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (numeros.size() != 1) {
            JOptionPane.showMessageDialog(this, "Escribe un solo número ganador",
                    "Número inválido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String numero = numeros.get(0);

        List<Boleta> titulares;
        try {
            GanadorDAO.guardar(tipo, fecha, numero);
            titulares = GanadorDAO.titulares(tipo, numero, fecha);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<String> lineas = new ArrayList<>();
        for (Titulares.Fila fila : Titulares.agrupar(titulares)) {
            lineas.add("• " + fila.texto() + " (" + fila.estado().getEtiqueta().toLowerCase() + ")");
        }
        String quien = titulares.isEmpty()
                ? "Nadie tiene esa boleta registrada."
                : "Lo tiene:\n" + String.join("\n", lineas);
        Avisos.exito(this, "Número ganador guardado",
                "Ganador " + tipo.getEtiqueta() + " del " + fecha.format(FORMATO_FECHA) + ": " + numero + "\n\n" + quien);
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}
