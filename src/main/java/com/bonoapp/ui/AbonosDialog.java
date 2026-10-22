package com.bonoapp.ui;

import com.bonoapp.db.AbonoDAO;
import com.bonoapp.model.Abono;
import com.bonoapp.model.Prestamo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * LISTA los abonos (plural) que un prestamo ya tiene, con opcion de eliminar uno registrado por error.
 * Se abre con el boton "Ver abonos" de Prestamos. No confundir con {@link AbonoDialog} (singular),
 * que es el formulario para registrar un abono nuevo.
 */
public class AbonosDialog extends JDialog {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private final Prestamo prestamo;
    private final ModeloAbonos modelo = new ModeloAbonos();
    private final JTable tabla = new JTable(modelo);
    private final JLabel resumen = new JLabel(" ");

    public AbonosDialog(Frame propietario, Prestamo prestamo) {
        super(propietario, "Abonos del préstamo", true);
        this.prestamo = prestamo;
        setLayout(new BorderLayout());

        JLabel nombre = new JLabel(prestamo.vendedoraNombre() + " — " + prestamo.concepto());
        nombre.setFont(nombre.getFont().deriveFont(Font.BOLD, 15f));
        resumen.setForeground(Theme.TEXTO_SUAVE);

        JPanel encabezado = new JPanel(new BorderLayout(0, 4));
        encabezado.add(nombre, BorderLayout.NORTH);
        encabezado.add(resumen, BorderLayout.CENTER);

        tabla.setRowHeight(28);
        tabla.setFillsViewportHeight(true);
        tabla.setDefaultRenderer(Object.class, new RenderizadorAbonos());
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setPreferredSize(new Dimension(520, 220));

        JPanel contenido = new JPanel(new BorderLayout(0, 12));
        contenido.setBorder(BorderFactory.createEmptyBorder(20, 20, 8, 20));
        contenido.add(encabezado, BorderLayout.NORTH);
        contenido.add(scroll, BorderLayout.CENTER);

        JButton botonEliminar = new JButton("Eliminar abono");
        botonEliminar.addActionListener(e -> eliminar());
        JButton botonCerrar = new JButton("Cerrar");
        Theme.estiloPrincipal(botonCerrar);
        botonCerrar.addActionListener(e -> dispose());
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        botones.add(botonEliminar);
        botones.add(botonCerrar);

        add(contenido, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        recargar();
        pack();
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonCerrar);
    }

    private void recargar() {
        List<Abono> abonos = AbonoDAO.listar(prestamo.id());
        modelo.setDatos(abonos);
        int abonado = abonos.stream().mapToInt(Abono::monto).sum();
        resumen.setText("Prestado $ " + FORMATO_PESOS.format(prestamo.monto())
                + "   ·   Abonado $ " + FORMATO_PESOS.format(abonado)
                + "   ·   Saldo $ " + FORMATO_PESOS.format(prestamo.monto() - abonado));
    }

    private void eliminar() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un abono de la lista",
                    "Ninguna selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este abono? El saldo del préstamo volverá a subir.", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            AbonoDAO.eliminar(modelo.getFila(fila).id());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        recargar();
    }

    private static class ModeloAbonos extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Monto", "Nota"};
        private List<Abono> datos = new ArrayList<>();

        void setDatos(List<Abono> datos) {
            this.datos = datos;
            fireTableDataChanged();
        }

        Abono getFila(int fila) {
            return datos.get(fila);
        }

        @Override
        public int getRowCount() {
            return datos.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNAS.length;
        }

        @Override
        public String getColumnName(int columna) {
            return COLUMNAS[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Abono a = datos.get(fila);
            return switch (columna) {
                case 0 -> a.fecha().format(FORMATO_FECHA);
                case 1 -> "$ " + FORMATO_PESOS.format(a.monto());
                case 2 -> a.nota() == null ? "" : a.nota();
                default -> "";
            };
        }
    }

    /** Montos alineados a la derecha, como en el resto de tablas con dinero. */
    private static class RenderizadorAbonos extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            setHorizontalAlignment(columna == 1 ? RIGHT : LEFT);
            return c;
        }
    }
}
