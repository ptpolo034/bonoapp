package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.db.TratoDAO;
import com.bonoapp.db.VendedoraDAO;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.model.Vendedora;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Formulario para registrar la entrega de un lote de boletas a una vendedora. */
public class EntregaBoletasDialog extends JDialog {

    private static final int CANTIDAD_MAXIMA = 5000;

    private final List<Vendedora> todasActivas;
    private final JTextField campoBuscarVendedora = new JTextField(20);
    private final JComboBox<Vendedora> comboVendedora = new JComboBox<>();
    private final JComboBox<TipoBoleta> comboTipo = new JComboBox<>(TipoBoleta.values());
    private final JTextField campoCantidad = new JTextField(10);
    private final JTextArea campoNumeros = new JTextArea(4, 20);
    private final JDateChooser campoFecha = new JDateChooser(new Date());

    private final JLabel etiquetaTratos = new JLabel(" ");
    private final JCheckBox incluirTratos = new JCheckBox("Incluir sus tratos en esta entrega", true);
    private List<String> tratosActuales = List.of();

    private boolean confirmado = false;
    private LocalDate fechaEntregada;

    public EntregaBoletasDialog(Frame propietario) {
        super(propietario, "Registrar entrega de boletas", true);
        setLayout(new BorderLayout());

        todasActivas = VendedoraDAO.listarActivas();
        filtrarVendedoras();
        comboVendedora.addActionListener(e -> cargarTratos());
        // Los tratos son independientes por tipo: al cambiar de Diaria a Premio mayor se muestran los del otro sorteo.
        comboTipo.addActionListener(e -> cargarTratos());

        campoBuscarVendedora.getDocument().addDocumentListener(new BuscadorListener(this::filtrarVendedoras));

        campoNumeros.setLineWrap(true);
        campoNumeros.setWrapStyleWord(true);

        campoFecha.setDateFormatString("dd/MM/yyyy");

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Buscar vendedora (nombre o documento)", campoBuscarVendedora);
        Formularios.agregarFila(form, gbc, 1, "Vendedora", comboVendedora);
        Formularios.agregarFila(form, gbc, 2, "Tipo de boleta", comboTipo);
        Formularios.agregarFila(form, gbc, 3, "Cantidad de boletas", campoCantidad);
        Formularios.agregarFila(form, gbc, 4, "Fecha de entrega", campoFecha);
        Formularios.agregarFila(form, gbc, 5, "Tratos", construirPanelTratos());

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(new JLabel("<html>Otros números<br>(opcional)</html>"), gbc);

        JScrollPane scrollNumeros = new JScrollPane(campoNumeros);
        scrollNumeros.setPreferredSize(new Dimension(220, 80));
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;
        form.add(scrollNumeros, gbc);

        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel ayuda = new JLabel("<html><body style='width:300px'>La cantidad es el total de boletas de la entrega: "
                + "los tratos cuentan dentro de ella. Escribe aquí los demás números que quieras asignar "
                + "(separados por coma, espacio o uno por línea); las boletas que queden sin número "
                + "se registran sin él.</body></html>");
        ayuda.setForeground(Theme.TEXTO_SUAVE);
        ayuda.setFont(ayuda.getFont().deriveFont(11f));
        form.add(ayuda, gbc);

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

    private JPanel construirPanelTratos() {
        JButton botonEditar = new JButton("Editar tratos");
        botonEditar.addActionListener(e -> editarTratos());

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fila.add(incluirTratos);
        fila.add(javax.swing.Box.createHorizontalStrut(12));
        fila.add(botonEditar);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        etiquetaTratos.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(etiquetaTratos);
        panel.add(javax.swing.Box.createVerticalStrut(4));
        panel.add(fila);
        return panel;
    }

    private void filtrarVendedoras() {
        Formularios.filtrarVendedoras(comboVendedora, todasActivas, campoBuscarVendedora.getText());
        // setModel() no avisa al combo de que cambio la seleccion: se actualiza a mano.
        cargarTratos();
    }

    /** Abre la ventana de tratos de la vendedora elegida y, al cerrarla, vuelve a leer los guardados. */
    private void editarTratos() {
        Vendedora vendedora = (Vendedora) comboVendedora.getSelectedItem();
        if (vendedora == null) {
            JOptionPane.showMessageDialog(this, "Primero selecciona una vendedora",
                    "Sin vendedora", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new TratosDialog((Frame) getOwner(), vendedora, (TipoBoleta) comboTipo.getSelectedItem()).setVisible(true);
        cargarTratos();
    }

    /** Lee los tratos guardados de la vendedora elegida, para el tipo elegido, y los muestra (sin tocar los demas campos). */
    private void cargarTratos() {
        Vendedora vendedora = (Vendedora) comboVendedora.getSelectedItem();
        TipoBoleta tipo = (TipoBoleta) comboTipo.getSelectedItem();
        tratosActuales = List.of();
        if (vendedora != null && tipo != null) {
            try {
                tratosActuales = TratoDAO.listarNumeros(vendedora.getId(), tipo);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, "No se pudieron leer los tratos: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        boolean hay = !tratosActuales.isEmpty();
        etiquetaTratos.setText(hay
                ? "<html><body style='width:300px'>" + String.join(", ", tratosActuales)
                + " (" + tratosActuales.size() + ")</body></html>"
                : "Esta vendedora no tiene tratos de " + (tipo == null ? "este tipo" : "la " + tipo.getEtiqueta()));
        incluirTratos.setEnabled(hay);
        if (hay) {
            incluirTratos.setSelected(true);
        }
    }

    private void guardar() {
        Vendedora vendedora = (Vendedora) comboVendedora.getSelectedItem();
        if (vendedora == null) {
            String motivo = todasActivas.isEmpty()
                    ? "No hay vendedoras activas para asignar boletas"
                    : "Selecciona una vendedora de la lista";
            JOptionPane.showMessageDialog(this, motivo, "Sin vendedora", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int cantidad;
        try {
            cantidad = Integer.parseInt(campoCantidad.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser un numero",
                    "Datos invalidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cantidad <= 0) {
            JOptionPane.showMessageDialog(this, "La cantidad debe ser mayor a cero",
                    "Datos invalidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cantidad > CANTIDAD_MAXIMA) {
            JOptionPane.showMessageDialog(this, "La cantidad no puede pasar de " + CANTIDAD_MAXIMA,
                    "Cantidad demasiado grande", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<String> tratos = incluirTratos.isSelected() ? tratosActuales : List.of();
        List<String> otros = List.of();
        if (!campoNumeros.getText().isBlank()) {
            try {
                otros = NumerosBoleta.parsear(campoNumeros.getText());
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        List<String> numeros = new ArrayList<>(tratos);
        numeros.addAll(otros);
        if (numeros.size() > cantidad) {
            JOptionPane.showMessageDialog(this,
                    "Entre los tratos (" + tratos.size() + ") y los otros números (" + otros.size() + ") son "
                            + numeros.size() + ", pero la cantidad de boletas es " + cantidad + ".",
                    "No coincide la cantidad", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Date fechaSeleccionada = campoFecha.getDate();
        if (fechaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona la fecha de entrega",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        LocalDate fecha = fechaSeleccionada.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        try {
            BoletaDAO.registrarEntrega(vendedora.getId(), (TipoBoleta) comboTipo.getSelectedItem(), cantidad,
                    numeros, fecha);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo registrar la entrega", JOptionPane.WARNING_MESSAGE);
            return;
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar la entrega: " + e.getMessage(),
                    "Error al guardar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        fechaEntregada = fecha;
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }

    /** La fecha de entrega con la que se registro (para que la pantalla de Boletas muestre ese dia). */
    public LocalDate getFechaEntregada() {
        return fechaEntregada;
    }
}
