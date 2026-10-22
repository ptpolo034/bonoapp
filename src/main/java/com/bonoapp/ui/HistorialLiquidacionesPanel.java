package com.bonoapp.ui;

import com.bonoapp.db.LiquidacionDAO;
import com.bonoapp.model.Liquidacion;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Liquidaciones ya hechas, con su recaudado de ese momento. Permite revertir una si hubo un error. */
public class HistorialLiquidacionesPanel extends JPanel implements Actualizable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));
    private static final int COLUMNA_ESTADO = 7;

    private final ModeloLiquidaciones modelo = new ModeloLiquidaciones();
    private final JTable tabla = new JTable(modelo);
    private final JTextField campoBusqueda = new JTextField(16);
    private final JLabel etiquetaResumen = new JLabel(" ");
    private List<Liquidacion> todas = new ArrayList<>();

    public HistorialLiquidacionesPanel() {
        setLayout(new BorderLayout(0, 16));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        campoBusqueda.getDocument().addDocumentListener(new BuscadorListener(this::filtrar));
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        busqueda.setOpaque(false);
        busqueda.add(new JLabel("Buscar (nombre o documento):"));
        busqueda.add(campoBusqueda);
        busqueda.add(Box.createHorizontalStrut(12));
        etiquetaResumen.setForeground(Theme.TEXTO_SUAVE);
        busqueda.add(etiquetaResumen);
        add(busqueda, BorderLayout.NORTH);

        tabla.setDefaultRenderer(Object.class, new RenderizadorLiquidaciones());
        Tablas.estilizar(tabla, 3, 4, 5, 6);
        tabla.getColumnModel().getColumn(COLUMNA_ESTADO).setCellRenderer(new ChipRenderer(
                texto -> "Vigente".equals(texto) ? Theme.Tono.EXITO : Theme.Tono.NEUTRO));
        add(new TablaConVacio(tabla, "checklist", "Aún no hay liquidaciones",
                "Cuando liquides las boletas de una vendedora, quedarán registradas aquí.", null,
                () -> !campoBusqueda.getText().isBlank()), BorderLayout.CENTER);

        JButton botonRevertir = Botones.secundario("Revertir liquidación", "arrows-exchange");
        botonRevertir.addActionListener(e -> revertir());
        JLabel ayuda = new JLabel("Revertir devuelve las boletas a \"Entregada\" para volver a liquidarlas bien.");
        ayuda.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        ayuda.setForeground(Theme.TEXTO_SUAVE);
        JPanel pie = Botones.fila(null, botonRevertir);
        pie.add(ayuda, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);

        actualizar();
    }

    @Override
    public void actualizar() {
        todas = LiquidacionDAO.listar();
        filtrar();
    }

    private void filtrar() {
        String buscado = campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);
        List<Liquidacion> filtradas = new ArrayList<>();
        int recaudado = 0;
        for (Liquidacion l : todas) {
            if (buscado.isEmpty() || l.vendedoraNombre().toLowerCase(Locale.ROOT).contains(buscado)
                    || (l.vendedoraDocumento() != null
                    && l.vendedoraDocumento().toLowerCase(Locale.ROOT).contains(buscado))) {
                filtradas.add(l);
                if (!l.anulada()) {
                    recaudado += l.recaudado();
                }
            }
        }
        modelo.setDatos(filtradas);
        etiquetaResumen.setText(filtradas.isEmpty()
                ? "Todavía no hay liquidaciones" + (buscado.isEmpty() ? "" : " con ese filtro")
                : filtradas.size() + " liquidación(es) · recaudado (sin las revertidas): $ "
                + FORMATO_PESOS.format(recaudado));
    }

    private void revertir() {
        int fila = tabla.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Selecciona la liquidación que quieres revertir",
                    "Ninguna selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Liquidacion l = modelo.getFila(tabla.convertRowIndexToModel(fila));
        if (l.anulada()) {
            JOptionPane.showMessageDialog(this, "Esa liquidación ya fue revertida",
                    "Ya revertida", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Revertir la liquidación de " + l.vendedoraNombre() + " del " + l.fecha().format(FORMATO_FECHA)
                        + "?\n" + l.vendidas() + " vendida(s) y " + l.devueltas() + " devuelta(s) ("
                        + l.tipo().getEtiqueta() + ", $ " + FORMATO_PESOS.format(l.recaudado()) + ")\n\n"
                        + "Las boletas volverán a quedar pendientes de liquidar.",
                "Confirmar reversión", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int restauradas = LiquidacionDAO.revertir(l.id());
            Avisos.exito(this, "Liquidación revertida",
                    restauradas + " boleta(s) volvieron a pendientes. Ya puedes liquidarlas de nuevo.");
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "No se pudo revertir", JOptionPane.WARNING_MESSAGE);
        }
        actualizar();
    }

    private static class ModeloLiquidaciones extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha", "Vendedora", "Tipo", "Vendidas", "Devueltas", "Precio",
                "Recaudado", "Estado"};
        private List<Liquidacion> datos = new ArrayList<>();

        void setDatos(List<Liquidacion> datos) {
            this.datos = datos;
            fireTableDataChanged();
        }

        Liquidacion getFila(int fila) {
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
            Liquidacion l = datos.get(fila);
            return switch (columna) {
                case 0 -> l.fecha().format(FORMATO_FECHA);
                case 1 -> l.vendedoraNombre();
                case 2 -> l.tipo().getEtiqueta();
                case 3 -> l.vendidas();
                case 4 -> l.devueltas();
                case 5 -> "$ " + FORMATO_PESOS.format(l.precio());
                case 6 -> "$ " + FORMATO_PESOS.format(l.recaudado());
                case COLUMNA_ESTADO -> l.anulada() ? "Revertida" : "Vigente";
                default -> "";
            };
        }
    }

    /** Cantidades y montos a la derecha; las liquidaciones revertidas se ven atenuadas. */
    private class RenderizadorLiquidaciones extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(t, valor, seleccionada, foco, fila, columna);
            boolean anulada = modelo.getFila(t.convertRowIndexToModel(fila)).anulada();
            setHorizontalAlignment(columna >= 3 && columna < COLUMNA_ESTADO ? RIGHT : LEFT);
            c.setFont(t.getFont().deriveFont(anulada ? Font.ITALIC : Font.PLAIN));
            if (!seleccionada) {
                c.setForeground(anulada ? Theme.TEXTO_SUAVE : t.getForeground());
            }
            return c;
        }
    }
}
