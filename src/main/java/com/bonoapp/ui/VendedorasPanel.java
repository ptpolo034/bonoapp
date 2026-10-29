package com.bonoapp.ui;

import com.bonoapp.db.VendedoraDAO;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.model.Vendedora;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.FlowLayout;
import java.util.List;

/** Pantalla de gestion de vendedoras: listar, crear, editar y eliminar. */
public class VendedorasPanel extends JPanel {

    private final ModeloVendedoras modelo = new ModeloVendedoras();
    private final JTable tabla = new JTable(modelo);
    private final TableRowSorter<ModeloVendedoras> ordenador = new TableRowSorter<>(modelo);
    private final JTextField campoBusqueda = new JTextField(20);

    public VendedorasPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Vendedoras");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));

        JLabel etiquetaBusqueda = new JLabel("Buscar (nombre o documento):");
        campoBusqueda.getDocument().addDocumentListener(new BuscadorListener(this::filtrar));

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        busqueda.setOpaque(false);
        busqueda.add(etiquetaBusqueda);
        busqueda.add(Box.createHorizontalStrut(8));
        busqueda.add(campoBusqueda);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(busqueda, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);

        tabla.setRowSorter(ordenador);
        Tablas.estilizar(tabla);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new ChipRenderer(
                texto -> "Sí".equals(texto) ? Theme.Tono.EXITO : Theme.Tono.NEUTRO));
        add(new TablaConVacio(tabla, "users", "Aún no hay vendedoras",
                "Pulsa \"Nueva vendedora\" para crear la primera y poder entregarle boletas.", null,
                () -> !campoBusqueda.getText().isBlank()), BorderLayout.CENTER);

        JButton botonNueva = Botones.principal("Nueva vendedora", "plus");
        JButton botonEditar = Botones.secundario("Editar", "edit");
        JButton botonTratos = Botones.secundario("Tratos", "ticket");
        JButton botonEliminar = Botones.peligro("Eliminar", "trash");

        botonNueva.addActionListener(e -> nueva());
        botonEditar.addActionListener(e -> editar());
        botonTratos.addActionListener(e -> tratos());
        botonEliminar.addActionListener(e -> eliminar());

        add(Botones.fila(botonEliminar, botonNueva, botonEditar, botonTratos), BorderLayout.SOUTH);

        cargar();
    }

    private void cargar() {
        modelo.setDatos(VendedoraDAO.listar());
    }

    private void filtrar() {
        String texto = campoBusqueda.getText().trim();
        if (texto.isEmpty()) {
            ordenador.setRowFilter(null);
            return;
        }
        String patron = "(?i)" + java.util.regex.Pattern.quote(texto);
        ordenador.setRowFilter(RowFilter.regexFilter(patron, 0, 1));
    }

    private void nueva() {
        VendedoraDialog dialogo = new VendedoraDialog(ventana(), null);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            Vendedora v = new Vendedora();
            dialogo.aplicarA(v);
            VendedoraDAO.insertar(v);
            cargar();
        }
    }

    private void editar() {
        Vendedora seleccionada = seleccionActual();
        if (seleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una vendedora de la lista",
                    "Ninguna seleccion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        VendedoraDialog dialogo = new VendedoraDialog(ventana(), seleccionada);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            dialogo.aplicarA(seleccionada);
            VendedoraDAO.actualizar(seleccionada);
            cargar();
        }
    }

    private void tratos() {
        Vendedora seleccionada = seleccionActual();
        if (seleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una vendedora de la lista",
                    "Ninguna seleccion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new TratosDialog(ventana(), seleccionada, TipoBoleta.DIARIA).setVisible(true);
    }

    private void eliminar() {
        Vendedora seleccionada = seleccionActual();
        if (seleccionada == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una vendedora de la lista",
                    "Ninguna seleccion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar a " + seleccionada.getNombre() + "?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion == JOptionPane.YES_OPTION) {
            try {
                VendedoraDAO.eliminar(seleccionada.getId());
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar: esta vendedora ya tiene boletas, liquidaciones, ahorros o préstamos registrados.\n"
                                + "Si ya no trabaja aqui, mejor inactívala en vez de eliminarla.",
                        "No se pudo eliminar", JOptionPane.ERROR_MESSAGE);
                return;
            }
            cargar();
        }
    }

    private Vendedora seleccionActual() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            return null;
        }
        return modelo.getFila(tabla.convertRowIndexToModel(fila));
    }

    private java.awt.Frame ventana() {
        return (java.awt.Frame) SwingUtilities.getWindowAncestor(this);
    }

    private static class ModeloVendedoras extends AbstractTableModel {
        private final String[] columnas = {"Nombre", "Documento", "Teléfono", "Activa"};
        private List<Vendedora> datos = List.of();

        void setDatos(List<Vendedora> datos) {
            this.datos = datos;
            fireTableDataChanged();
        }

        Vendedora getFila(int fila) {
            return datos.get(fila);
        }

        @Override
        public int getRowCount() {
            return datos.size();
        }

        @Override
        public int getColumnCount() {
            return columnas.length;
        }

        @Override
        public String getColumnName(int columna) {
            return columnas[columna];
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Vendedora v = datos.get(fila);
            return switch (columna) {
                case 0 -> v.getNombre();
                case 1 -> v.getDocumento();
                case 2 -> v.getTelefono();
                case 3 -> v.isActiva() ? "Sí" : "No";
                default -> "";
            };
        }
    }
}
