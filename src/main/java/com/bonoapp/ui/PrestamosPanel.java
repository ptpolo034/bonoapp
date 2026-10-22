package com.bonoapp.ui;

import com.bonoapp.db.PrestamoDAO;
import com.bonoapp.model.Prestamo;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Prestamos: dinero prestado a las vendedoras (con concepto y fecha) y los abonos que ellas van haciendo. */
public class PrestamosPanel extends JPanel {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));
    private static final int COLUMNA_SALDO = 5;

    private final ModeloPrestamos modelo = new ModeloPrestamos();
    private final JTable tabla = new JTable(modelo);
    private final JTextField campoBusqueda = new JTextField(20);
    private final JLabel tituloResumen = new JLabel(" ");
    private final JLabel detalleResumen = new JLabel(" ");
    private final JLabel valorPrestado = new JLabel("$ 0");
    private final JLabel valorAbonado = new JLabel("$ 0");
    private final JLabel valorDebe = new JLabel("$ 0");
    private List<Prestamo> todos = new ArrayList<>();

    public PrestamosPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Préstamos");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));

        campoBusqueda.getDocument().addDocumentListener(new BuscadorListener(this::filtrar));
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        busqueda.setOpaque(false);
        busqueda.add(new JLabel("Buscar (vendedora, documento o concepto):"));
        busqueda.add(Box.createHorizontalStrut(8));
        busqueda.add(campoBusqueda);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(busqueda, BorderLayout.EAST);

        JPanel norte = new JPanel(new BorderLayout(0, 16));
        norte.setOpaque(false);
        norte.add(encabezado, BorderLayout.NORTH);
        norte.add(construirResumen(), BorderLayout.CENTER);
        add(norte, BorderLayout.NORTH);

        tabla.setDefaultRenderer(Object.class, new RenderizadorPrestamos());
        Tablas.estilizar(tabla, 3, 4, 5);
        add(new TablaConVacio(tabla, "cash", "Aún no hay préstamos",
                "Pulsa \"Nuevo préstamo\" cuando le prestes a una vendedora.", null,
                () -> !campoBusqueda.getText().isBlank()), BorderLayout.CENTER);

        JButton botonNuevo = Botones.principal("Nuevo préstamo", "plus");
        botonNuevo.addActionListener(e -> nuevo());
        JButton botonAbono = Botones.secundario("Registrar abono", "cash");
        botonAbono.addActionListener(e -> registrarAbono());
        JButton botonVerAbonos = Botones.secundario("Ver abonos", "history");
        botonVerAbonos.addActionListener(e -> verAbonos());
        JButton botonEliminar = Botones.peligro("Eliminar selección", "trash");
        botonEliminar.addActionListener(e -> eliminarSeleccion());

        add(Botones.fila(botonEliminar, botonNuevo, botonAbono, botonVerAbonos), BorderLayout.SOUTH);

        cargar();
    }

    /** Tarjeta con el resumen de lo que se esta viendo: cuanto se presto, cuanto se ha abonado y cuanto se debe. */
    private JPanel construirResumen() {
        JPanel tarjeta = Theme.crearTarjeta();
        tarjeta.setLayout(new BorderLayout(24, 0));

        tituloResumen.setFont(tituloResumen.getFont().deriveFont(Font.BOLD, 16f));
        detalleResumen.setForeground(Theme.TEXTO_SUAVE);
        JPanel izquierda = new JPanel(new GridLayout(2, 1, 0, 2));
        izquierda.setOpaque(false);
        izquierda.add(tituloResumen);
        izquierda.add(detalleResumen);

        JPanel cifras = new JPanel(new GridLayout(1, 3, 32, 0));
        cifras.setOpaque(false);
        cifras.add(cifra("Prestado", valorPrestado));
        cifras.add(cifra("Abonado", valorAbonado));
        cifras.add(cifra("Debe", valorDebe));

        tarjeta.add(izquierda, BorderLayout.CENTER);
        tarjeta.add(cifras, BorderLayout.EAST);
        return tarjeta;
    }

    private JPanel cifra(String etiqueta, JLabel valor) {
        JLabel titulo = new JLabel(etiqueta);
        titulo.setForeground(Theme.TEXTO_SUAVE);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 20f));
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.add(titulo);
        p.add(valor);
        return p;
    }

    private void actualizarResumen(List<Prestamo> mostrados, boolean hayFiltro) {
        int prestado = 0;
        int abonado = 0;
        Set<String> vendedoras = new LinkedHashSet<>();
        for (Prestamo p : mostrados) {
            prestado += p.monto();
            abonado += p.abonado();
            vendedoras.add(p.vendedoraNombre());
        }
        int debe = prestado - abonado;

        if (!hayFiltro) {
            tituloResumen.setText("Resumen general");
        } else if (vendedoras.size() == 1) {
            tituloResumen.setText("Resumen de " + vendedoras.iterator().next());
        } else {
            tituloResumen.setText("Resumen de la búsqueda");
        }
        detalleResumen.setText(mostrados.isEmpty()
                ? "No hay préstamos con ese filtro"
                : mostrados.size() + " préstamo(s)" + (vendedoras.size() > 1 ? " de " + vendedoras.size() + " vendedoras" : ""));

        valorPrestado.setText("$ " + FORMATO_PESOS.format(prestado));
        valorAbonado.setText("$ " + FORMATO_PESOS.format(abonado));
        valorDebe.setText("$ " + FORMATO_PESOS.format(debe));
        valorDebe.setForeground(debe > 0 ? Theme.PRINCIPAL
                : mostrados.isEmpty() ? UIManager.getColor("Label.foreground") : Theme.EXITO);
    }

    private void cargar() {
        todos = PrestamoDAO.listar();
        filtrar();
    }

    private void filtrar() {
        String buscado = campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);
        List<Prestamo> filtrados = new ArrayList<>();
        for (Prestamo p : todos) {
            if (buscado.isEmpty()
                    || contiene(p.vendedoraNombre(), buscado)
                    || contiene(p.vendedoraDocumento(), buscado)
                    || contiene(p.concepto(), buscado)) {
                filtrados.add(p);
            }
        }
        modelo.setDatos(filtrados);
        actualizarResumen(filtrados, !buscado.isEmpty());
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private Frame ventana() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void nuevo() {
        PrestamoDialog dialogo = new PrestamoDialog(ventana());
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            cargar();
        }
    }

    /** El prestamo seleccionado, o null (avisando al usuario) si no hay exactamente uno. */
    private Prestamo prestamoSeleccionado() {
        int[] filas = tabla.getSelectedRows();
        if (filas.length != 1) {
            JOptionPane.showMessageDialog(this, "Selecciona un solo préstamo de la lista",
                    "Selección", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return modelo.getFila(filas[0]);
    }

    private void registrarAbono() {
        Prestamo prestamo = prestamoSeleccionado();
        if (prestamo == null) {
            return;
        }
        if (prestamo.saldo() <= 0) {
            JOptionPane.showMessageDialog(this, "Este préstamo ya está pagado por completo.",
                    "Nada que abonar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        AbonoDialog dialogo = new AbonoDialog(ventana(), prestamo);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            cargar();
        }
    }

    private void verAbonos() {
        Prestamo prestamo = prestamoSeleccionado();
        if (prestamo == null) {
            return;
        }
        new AbonosDialog(ventana(), prestamo).setVisible(true);
        cargar();
    }

    private void eliminarSeleccion() {
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más préstamos de la lista",
                    "Ninguna selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar " + filas.length + " préstamo(s) seleccionado(s)?\nTambién se borrarán sus abonos.",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (int fila : filas) {
            ids.add(modelo.getFila(fila).id());
        }
        try {
            PrestamoDAO.eliminarVarios(ids);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        cargar();
    }

    private static class ModeloPrestamos extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Vendedora", "Concepto", "Monto", "Abonado", "Saldo"};
        private List<Prestamo> datos = new ArrayList<>();

        void setDatos(List<Prestamo> datos) {
            this.datos = datos;
            fireTableDataChanged();
        }

        Prestamo getFila(int fila) {
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
            Prestamo p = datos.get(fila);
            return switch (columna) {
                case 0 -> p.fecha().format(FORMATO_FECHA);
                case 1 -> p.vendedoraNombre();
                case 2 -> p.concepto();
                case 3 -> "$ " + FORMATO_PESOS.format(p.monto());
                case 4 -> "$ " + FORMATO_PESOS.format(p.abonado());
                case COLUMNA_SALDO -> p.saldo() <= 0 ? "Pagado" : "$ " + FORMATO_PESOS.format(p.saldo());
                default -> "";
            };
        }
    }

    /** Montos a la derecha y los prestamos ya pagados marcados en verde. */
    private class RenderizadorPrestamos extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            setHorizontalAlignment(columna >= 3 ? RIGHT : LEFT);
            boolean pagado = columna == COLUMNA_SALDO && modelo.getFila(fila).saldo() <= 0;
            c.setFont(t.getFont().deriveFont(pagado ? Font.BOLD : Font.PLAIN));
            if (!seleccionada) {
                c.setForeground(pagado ? Theme.EXITO : t.getForeground());
            }
            return c;
        }
    }
}
