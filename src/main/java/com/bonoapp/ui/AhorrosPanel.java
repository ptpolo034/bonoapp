package com.bonoapp.ui;

import com.bonoapp.db.AhorroDAO;
import com.bonoapp.model.Ahorro;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
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

/**
 * Ahorros: movimientos de ahorro de las vendedoras (con fecha y concepto). Al buscar una vendedora se ve
 * cuanto ha ahorrado, cuanto ha retirado y cuanto le queda.
 */
public class AhorrosPanel extends JPanel implements Actualizable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private final ModeloAhorros modelo = new ModeloAhorros();
    private final JTable tabla = new JTable(modelo);
    private final JTextField campoBusqueda = new JTextField(20);
    private final JLabel tituloResumen = new JLabel(" ");
    private final JLabel detalleResumen = new JLabel(" ");
    private final JLabel valorAhorrado = new JLabel("$ 0");
    private final JLabel valorRetirado = new JLabel("$ 0");
    private final JLabel valorDisponible = new JLabel("$ 0");
    private List<Ahorro> todos = new ArrayList<>();

    public AhorrosPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Ahorros");
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

        tabla.setDefaultRenderer(Object.class, new RenderizadorAhorros());
        Tablas.estilizar(tabla, 4);
        tabla.getColumnModel().getColumn(3).setCellRenderer(new ChipRenderer(
                texto -> "Retiro".equals(texto) ? Theme.Tono.ALERTA : Theme.Tono.EXITO));
        add(new TablaConVacio(tabla, "pig-money", "Aún no hay ahorros",
                "Pulsa \"Registrar ahorro\" para anotar el primero; los retiros también aparecerán aquí.", null,
                () -> !campoBusqueda.getText().isBlank()), BorderLayout.CENTER);

        JButton botonAhorro = Botones.principal("Registrar ahorro", "plus");
        botonAhorro.addActionListener(e -> registrar(false));
        JButton botonRetiro = Botones.secundario("Registrar retiro", "cash");
        botonRetiro.addActionListener(e -> registrar(true));
        JButton botonEliminar = Botones.peligro("Eliminar selección", "trash");
        botonEliminar.addActionListener(e -> eliminarSeleccion());

        add(Botones.fila(botonEliminar, botonAhorro, botonRetiro), BorderLayout.SOUTH);

        actualizar();
    }

    /** Tarjeta con lo que se esta viendo: cuanto se ha ahorrado, cuanto se ha retirado y cuanto queda. */
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
        cifras.add(cifra("Ahorrado", valorAhorrado));
        cifras.add(cifra("Retirado", valorRetirado));
        cifras.add(cifra("Disponible", valorDisponible));

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

    /** Vuelve a cargar los movimientos. Se llama cada vez que se entra a esta seccion. */
    @Override
    public void actualizar() {
        todos = AhorroDAO.listar();
        filtrar();
    }

    private void filtrar() {
        String buscado = campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);
        List<Ahorro> filtrados = new ArrayList<>();
        for (Ahorro a : todos) {
            if (buscado.isEmpty()
                    || contiene(a.vendedoraNombre(), buscado)
                    || contiene(a.vendedoraDocumento(), buscado)
                    || contiene(a.concepto(), buscado)) {
                filtrados.add(a);
            }
        }
        modelo.setDatos(filtrados);
        actualizarResumen(filtrados, !buscado.isEmpty());
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private void actualizarResumen(List<Ahorro> mostrados, boolean hayFiltro) {
        int ahorrado = 0;
        int retirado = 0;
        Set<String> vendedoras = new LinkedHashSet<>();
        for (Ahorro a : mostrados) {
            if (a.monto() >= 0) {
                ahorrado += a.monto();
            } else {
                retirado += -a.monto();
            }
            vendedoras.add(a.vendedoraNombre());
        }
        int disponible = ahorrado - retirado;

        if (!hayFiltro) {
            tituloResumen.setText("Resumen general");
        } else if (vendedoras.size() == 1) {
            tituloResumen.setText("Resumen de " + vendedoras.iterator().next());
        } else {
            tituloResumen.setText("Resumen de la búsqueda");
        }
        detalleResumen.setText(mostrados.isEmpty()
                ? "No hay movimientos con ese filtro"
                : mostrados.size() + " movimiento(s)" + (vendedoras.size() > 1 ? " de " + vendedoras.size() + " vendedoras" : ""));

        valorAhorrado.setText("$ " + FORMATO_PESOS.format(ahorrado));
        valorRetirado.setText("$ " + FORMATO_PESOS.format(retirado));
        valorDisponible.setText("$ " + FORMATO_PESOS.format(disponible));
        valorDisponible.setForeground(Theme.EXITO);
    }

    private Frame ventana() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void registrar(boolean retiro) {
        RegistrarAhorroDialog dialogo = new RegistrarAhorroDialog(ventana(), retiro);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            actualizar();
        }
    }

    private void eliminarSeleccion() {
        int[] filas = tabla.getSelectedRows();
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona uno o más movimientos de la lista",
                    "Ninguna selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar " + filas.length + " movimiento(s) de ahorro?\nEl disponible de la vendedora se recalcula.",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (int fila : filas) {
            ids.add(modelo.getFila(fila).id());
        }
        try {
            AhorroDAO.eliminarVarios(ids);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo eliminar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        actualizar();
    }

    private static class ModeloAhorros extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Vendedora", "Concepto", "Tipo", "Monto"};
        private List<Ahorro> datos = new ArrayList<>();

        void setDatos(List<Ahorro> datos) {
            this.datos = datos;
            fireTableDataChanged();
        }

        Ahorro getFila(int fila) {
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
            Ahorro a = datos.get(fila);
            return switch (columna) {
                case 0 -> a.fecha().format(FORMATO_FECHA);
                case 1 -> a.vendedoraNombre();
                case 2 -> a.concepto();
                case 3 -> a.monto() < 0 ? "Retiro" : "Ahorro";
                case 4 -> "$ " + FORMATO_PESOS.format(Math.abs(a.monto()));
                default -> "";
            };
        }
    }

    /** Monto a la derecha, y los retiros en el color de acento para distinguirlos de los ahorros. */
    private class RenderizadorAhorros extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            setHorizontalAlignment(columna == 4 ? RIGHT : LEFT);
            boolean retiro = modelo.getFila(fila).monto() < 0;
            if (!seleccionada) {
                c.setForeground(retiro && columna >= 3 ? Theme.PRINCIPAL : t.getForeground());
            }
            return c;
        }
    }
}
