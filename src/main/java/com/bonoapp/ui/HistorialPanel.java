package com.bonoapp.ui;

import com.bonoapp.db.HistorialDAO;
import com.bonoapp.db.VendedoraDAO;
import com.bonoapp.model.EntradaHistorial;
import com.bonoapp.model.Vendedora;

import javax.swing.Box;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Registro de cambios: quien hizo que y cuando (entregas, ediciones, eliminaciones, liquidaciones, ahorros...). */
public class HistorialPanel extends JPanel implements Actualizable {

    private static final int LIMITE = 3000;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ModeloHistorial modelo = new ModeloHistorial();
    private final JTable tabla = new JTable(modelo);
    private final JTextField campoBusqueda = new JTextField(22);
    private final JLabel etiquetaResumen = new JLabel(" ");
    private List<EntradaHistorial> todas = new ArrayList<>();

    public HistorialPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Historial de cambios");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));

        campoBusqueda.getDocument().addDocumentListener(new BuscadorListener(this::filtrar));
        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        busqueda.setOpaque(false);
        busqueda.add(new JLabel("Buscar (nombre, documento, acción o detalle):"));
        busqueda.add(Box.createHorizontalStrut(8));
        busqueda.add(campoBusqueda);

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        encabezado.add(titulo, BorderLayout.WEST);
        encabezado.add(busqueda, BorderLayout.EAST);
        add(encabezado, BorderLayout.NORTH);

        Tablas.estilizar(tabla);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(150);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(190);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(560);
        add(new TablaConVacio(tabla, "history", "Aún no hay movimientos",
                "Cada cambio que hagas en la app quedará anotado aquí.", null,
                () -> !campoBusqueda.getText().isBlank()), BorderLayout.CENTER);

        etiquetaResumen.setForeground(Theme.TEXTO_SUAVE);
        add(etiquetaResumen, BorderLayout.SOUTH);

        actualizar();
    }

    @Override
    public void actualizar() {
        todas = HistorialDAO.listar(LIMITE);
        filtrar();
    }

    private void filtrar() {
        String buscado = campoBusqueda.getText().trim().toLowerCase(Locale.ROOT);
        // El historial describe a las vendedoras por nombre, no por documento. Para poder buscar por
        // documento se buscan las vendedoras cuyo documento coincide y se muestran los movimientos
        // que mencionan su nombre.
        List<String> nombresPorDocumento = new ArrayList<>();
        if (!buscado.isEmpty()) {
            for (Vendedora v : VendedoraDAO.listar()) {
                if (contiene(v.getDocumento(), buscado) && v.getNombre() != null) {
                    nombresPorDocumento.add(v.getNombre().toLowerCase(Locale.ROOT));
                }
            }
        }
        List<EntradaHistorial> filtradas = new ArrayList<>();
        for (EntradaHistorial e : todas) {
            if (buscado.isEmpty() || contiene(e.accion(), buscado) || contiene(e.usuario(), buscado)
                    || contiene(e.detalle(), buscado) || mencionaAlguno(e.detalle(), nombresPorDocumento)) {
                filtradas.add(e);
            }
        }
        modelo.setDatos(filtradas);
        etiquetaResumen.setText(filtradas.size() + " movimiento(s)"
                + (todas.size() >= LIMITE ? " · se muestran los últimos " + LIMITE : "")
                + " · los cambios quedan anotados automáticamente con el usuario que los hizo.");
    }

    /** True si el detalle nombra a alguna de las vendedoras indicadas (nombres ya en minusculas). */
    private static boolean mencionaAlguno(String detalle, List<String> nombres) {
        if (detalle == null) {
            return false;
        }
        String minusculas = detalle.toLowerCase(Locale.ROOT);
        for (String nombre : nombres) {
            if (minusculas.contains(nombre)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contiene(String texto, String buscado) {
        return texto != null && texto.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private static class ModeloHistorial extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Fecha y hora", "Usuario", "Acción", "Detalle"};
        private List<EntradaHistorial> datos = new ArrayList<>();

        void setDatos(List<EntradaHistorial> datos) {
            this.datos = datos;
            fireTableDataChanged();
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
            EntradaHistorial e = datos.get(fila);
            return switch (columna) {
                case 0 -> e.fechaHora().format(FORMATO);
                case 1 -> e.usuario() == null ? "—" : e.usuario();
                case 2 -> e.accion();
                case 3 -> e.detalle();
                default -> "";
            };
        }
    }
}
