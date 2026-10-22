package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.db.LiquidacionDAO;
import com.bonoapp.db.VendedoraDAO;
import com.bonoapp.model.Boleta;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.model.Vendedora;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.Locale;

/** Liquidacion de boletas: marca cuales se vendieron y cuales se devolvieron, por vendedora y tipo de boleta. */
public class LiquidacionPanel extends JPanel implements Actualizable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    private final JTextField campoBuscarVendedora = new JTextField(12);
    private final JComboBox<Vendedora> comboVendedora = new JComboBox<>();
    private final JComboBox<TipoBoleta> comboTipo = new JComboBox<>(TipoBoleta.values());
    private List<Vendedora> todasActivas = new ArrayList<>();
    private final ModeloLiquidacion modelo = new ModeloLiquidacion();
    private final JTable tabla = new JTable(modelo);
    private final JLabel etiquetaResumen = new JLabel(" ");
    private final JTextField campoNumeros = new JTextField();
    private final JLabel etiquetaMarcado = new JLabel(" ");
    private final JCheckBox todasLasFechas = new JCheckBox("Todas las fechas", true);
    private final JDateChooser campoFecha = new JDateChooser(new Date());
    private List<Boleta> pendientesTodas = List.of();

    public LiquidacionPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(16, 0, 0, 0));

        cargarVendedoras();
        comboVendedora.addActionListener(e -> cargarPendientes());
        comboTipo.addActionListener(e -> cargarPendientes());
        campoBuscarVendedora.getDocument().addDocumentListener(new BuscadorListener(this::filtrarVendedoras));

        // Etiqueta y campo cortos: con el texto largo anterior, el selector de tipo quedaba fuera de la ventana.
        campoBuscarVendedora.setToolTipText("Escribe el nombre o el documento de la vendedora");
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Buscar vendedora:"));
        filtros.add(campoBuscarVendedora);
        filtros.add(Box.createHorizontalStrut(12));
        filtros.add(new JLabel("Vendedora:"));
        filtros.add(comboVendedora);
        filtros.add(Box.createHorizontalStrut(12));
        filtros.add(new JLabel("Tipo:"));
        filtros.add(comboTipo);

        // Segunda fila: filtro por fecha de entrega. Por defecto "Todas las fechas", para que al elegir una
        // vendedora aparezcan sus pendientes de cualquier dia; desmarcandolo se liquida un dia a la vez.
        campoFecha.setDateFormatString("dd/MM/yyyy");
        campoFecha.setPreferredSize(new Dimension(150, campoBuscarVendedora.getPreferredSize().height));
        campoFecha.setEnabled(false);
        campoFecha.addPropertyChangeListener("date", e -> aplicarFiltroFecha());
        todasLasFechas.setOpaque(false);
        todasLasFechas.addActionListener(e -> {
            campoFecha.setEnabled(!todasLasFechas.isSelected());
            aplicarFiltroFecha();
        });
        JPanel filtroFecha = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtroFecha.setOpaque(false);
        filtroFecha.add(new JLabel("Fecha de entrega:"));
        filtroFecha.add(todasLasFechas);
        filtroFecha.add(campoFecha);

        JPanel norte = new JPanel(new GridLayout(3, 1, 0, 8));
        norte.setOpaque(false);
        norte.add(filtros);
        norte.add(filtroFecha);
        norte.add(construirMarcadoRapido());
        add(norte, BorderLayout.NORTH);

        Tablas.estilizar(tabla);
        add(Tablas.enDesplazamiento(tabla), BorderLayout.CENTER);

        JButton botonLiquidar = Botones.principal("Liquidar", "checklist");
        botonLiquidar.addActionListener(e -> liquidar());

        JPanel pie = new JPanel(new BorderLayout(0, 8));
        pie.setOpaque(false);
        etiquetaResumen.setForeground(Theme.TEXTO_SUAVE);
        etiquetaMarcado.setForeground(Theme.TEXTO_SUAVE);
        JPanel textos = new JPanel(new GridLayout(2, 1, 0, 2));
        textos.setOpaque(false);
        textos.add(etiquetaMarcado);
        textos.add(etiquetaResumen);
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        filaBoton.setOpaque(false);
        filaBoton.add(botonLiquidar);
        pie.add(textos, BorderLayout.NORTH);
        pie.add(filaBoton, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);

        cargarPendientes();
    }

    /**
     * Fila de marcado rapido: se escriben o pegan varios numeros (o se seleccionan filas en la tabla) y con un
     * boton se marcan como devueltas, o se pasan a otra vendedora que si las vende. Evita desmarcar de una en
     * una entre cientos de boletas.
     */
    private JPanel construirMarcadoRapido() {
        campoNumeros.putClientProperty("JTextField.placeholderText",
                "Escribe o pega números: 123 456 789  (o selecciona filas)");
        campoNumeros.addActionListener(e -> marcarDevueltas());

        JButton botonDevueltas = Botones.secundario("Marcar como devueltas", "x");
        botonDevueltas.addActionListener(e -> marcarDevueltas());
        JButton botonMover = Botones.secundario("Mover a otra vendedora", "arrows-exchange");
        botonMover.setToolTipText("Pasar estas boletas a otra vendedora que sí las vende");
        botonMover.addActionListener(e -> moverAOtraVendedora());
        JButton botonTodas = Botones.secundario("Todas vendidas", "check");
        botonTodas.setToolTipText("Quitar todas las marcas de devuelta");
        botonTodas.addActionListener(e -> {
            for (int i = 0; i < modelo.getRowCount(); i++) {
                modelo.setVendida(i, true);
            }
            etiquetaMarcado.setForeground(Theme.TEXTO_SUAVE);
            etiquetaMarcado.setText("Todas las boletas quedaron como vendidas.");
        });

        JPanel botones = new JPanel();
        botones.setOpaque(false);
        botones.setLayout(new javax.swing.BoxLayout(botones, javax.swing.BoxLayout.X_AXIS));
        botones.add(Box.createHorizontalStrut(8));
        botones.add(botonDevueltas);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(botonMover);
        botones.add(Box.createHorizontalStrut(8));
        botones.add(botonTodas);

        JPanel fila = new JPanel(new BorderLayout());
        fila.setOpaque(false);
        fila.add(campoNumeros, BorderLayout.CENTER);
        fila.add(botones, BorderLayout.EAST);
        return fila;
    }

    /**
     * Las filas sobre las que actuar: las que corresponden a los numeros escritos o, si no se escribio nada, las
     * seleccionadas en la tabla. Devuelve null (avisando al usuario) si no hay nada que hacer. Los numeros que no se
     * encuentran se avisan en la linea de estado sin bloquear lo que si se encontro.
     */
    private List<Integer> filasObjetivo(boolean soloVendidas) {
        String texto = campoNumeros.getText().trim();
        if (texto.isEmpty()) {
            int[] seleccion = tabla.getSelectedRows();
            if (seleccion.length == 0) {
                JOptionPane.showMessageDialog(this,
                        "Escribe o pega los números de las boletas, o selecciona filas en la tabla.",
                        "Nada que marcar", JOptionPane.INFORMATION_MESSAGE);
                return null;
            }
            List<Integer> filas = new ArrayList<>();
            for (int fila : seleccion) {
                filas.add(fila);
            }
            return filas;
        }

        etiquetaMarcado.setText(" ");
        List<String> escritos;
        try {
            escritos = NumerosBoleta.parsear(texto);
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Número inválido", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        boolean[] disponible = new boolean[modelo.getRowCount()];
        boolean[] vendidas = modelo.vendidas();
        for (int i = 0; i < disponible.length; i++) {
            disponible[i] = !soloVendidas || vendidas[i];
        }
        MarcadoRapido.Resultado resultado = MarcadoRapido.emparejar(modelo.numeros(), disponible, escritos);

        if (!resultado.noEncontrados().isEmpty()) {
            avisarNoEncontrados(resultado.noEncontrados(), resultado.filas().size());
        }
        if (resultado.filas().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ninguno de esos números está entre las boletas pendientes que se ven en la lista.",
                    "No se encontró", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return resultado.filas();
    }

    private void avisarNoEncontrados(List<String> noEncontrados, int encontrados) {
        // Si estan entre las pendientes de la vendedora pero en otra fecha, se le dice como verlos.
        Set<String> enOtraFecha = new LinkedHashSet<>();
        for (Boleta b : pendientesTodas) {
            if (b.getNumero() != null && noEncontrados.contains(b.getNumero())) {
                enOtraFecha.add(b.getNumero());
            }
        }
        String texto = "No encontré: " + String.join(", ", noEncontrados)
                + (enOtraFecha.isEmpty() ? "" : " (" + String.join(", ", enOtraFecha)
                + " está en otra fecha: marca \"Todas las fechas\")") + ".";
        etiquetaMarcado.setForeground(Theme.ALERTA);
        etiquetaMarcado.setText(texto + (encontrados > 0 ? "  Lo demás sí se aplicó." : ""));
    }

    private void marcarDevueltas() {
        List<Integer> filas = filasObjetivo(true);
        if (filas == null) {
            return;
        }
        for (int fila : filas) {
            modelo.setVendida(fila, false);
        }
        seleccionar(filas);
        campoNumeros.setText("");
        if (!etiquetaMarcado.getText().startsWith("No encontré")) {
            etiquetaMarcado.setForeground(Theme.TEXTO_SUAVE);
            etiquetaMarcado.setText(filas.size() + " boleta(s) marcadas como devueltas. Pulsa \"Liquidar\" para guardar.");
        }
    }

    private void moverAOtraVendedora() {
        List<Integer> filas = filasObjetivo(false);
        if (filas == null) {
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (int fila : filas) {
            ids.add(modelo.idDe(fila));
        }
        MoverBoletasDialog dialogo = new MoverBoletasDialog(
                (java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this), ids);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            campoNumeros.setText("");
            // Las movidas dejan esta lista; lo ya marcado como devuelto en las demas se conserva.
            recargarPendientes();
            etiquetaMarcado.setForeground(Theme.TEXTO_SUAVE);
            etiquetaMarcado.setText(ids.size() + " boleta(s) pasaron a otra vendedora.");
        }
    }

    private void seleccionar(List<Integer> filas) {
        tabla.clearSelection();
        for (int fila : filas) {
            tabla.addRowSelectionInterval(fila, fila);
        }
        if (!filas.isEmpty()) {
            tabla.scrollRectToVisible(tabla.getCellRect(filas.get(0), 0, true));
        }
    }

    /** Vuelve a cargar vendedoras y pendientes. Se llama cada vez que se entra a esta seccion. */
    @Override
    public void actualizar() {
        cargarVendedoras();
    }

    private void cargarVendedoras() {
        todasActivas = VendedoraDAO.listarActivas();
        filtrarVendedoras();
    }

    private void filtrarVendedoras() {
        Formularios.filtrarVendedoras(comboVendedora, todasActivas, campoBuscarVendedora.getText());
        // setModel() no dispara el ActionListener del combo: si la busqueda quito a la vendedora
        // seleccionada, hay que refrescar la tabla a mano para que no quede mostrando las boletas
        // de la vendedora anterior mientras el combo ya cambio.
        cargarPendientes();
    }

    /** Carga las pendientes de la vendedora y tipo elegidos, empezando de cero (todas marcadas como vendidas). */
    private void cargarPendientes() {
        modelo.limpiarMarcas();
        campoNumeros.setText("");
        etiquetaMarcado.setText(" ");
        recargarPendientes();
    }

    /** Vuelve a leer las pendientes SIN perder lo que ya se marco como devuelto (por ejemplo, despues de mover boletas). */
    private void recargarPendientes() {
        Vendedora vendedora = (Vendedora) comboVendedora.getSelectedItem();
        TipoBoleta tipo = (TipoBoleta) comboTipo.getSelectedItem();
        // Todas las pendientes de la vendedora, de cualquier fecha; el filtro de fecha se aplica despues.
        pendientesTodas = vendedora == null
                ? List.of()
                : BoletaDAO.listarPendientesPorVendedora(vendedora.getId(), tipo);
        aplicarFiltroFecha();
    }

    /** Muestra todas las pendientes o solo las de la fecha elegida, y explica que se esta viendo. */
    private void aplicarFiltroFecha() {
        TipoBoleta tipo = (TipoBoleta) comboTipo.getSelectedItem();
        LocalDate fecha = Formularios.aLocalDate(campoFecha.getDate());
        boolean todas = todasLasFechas.isSelected() || fecha == null;

        List<Boleta> mostradas = new ArrayList<>();
        for (Boleta b : pendientesTodas) {
            if (todas || b.getFechaEntrega().equals(fecha)) {
                mostradas.add(b);
            }
        }
        modelo.setDatos(mostradas);

        long deHoy = mostradas.stream().filter(b -> b.getFechaEntrega().equals(LocalDate.now())).count();
        String texto;
        if (pendientesTodas.isEmpty()) {
            texto = "No hay boletas " + tipo.getEtiqueta() + " pendientes de liquidar para esta vendedora.";
        } else if (mostradas.isEmpty()) {
            texto = "No hay pendientes entregadas el " + fecha.format(FORMATO_FECHA) + ", pero esta vendedora tiene "
                    + pendientesTodas.size() + " de otras fechas. Marca \"Todas las fechas\" para verlas.";
        } else {
            texto = mostradas.size() + " boleta(s) pendiente(s)"
                    + (todas ? " (" + deHoy + " de hoy, " + (mostradas.size() - deHoy) + " de días anteriores)" : "")
                    + ". Desmarca las que se devolvieron.";
        }
        etiquetaResumen.setText(texto);
    }

    private void liquidar() {
        Vendedora vendedora = (Vendedora) comboVendedora.getSelectedItem();
        TipoBoleta tipo = (TipoBoleta) comboTipo.getSelectedItem();
        if (vendedora == null || modelo.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No hay boletas pendientes para liquidar",
                    "Nada que liquidar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        List<Integer> idsVendidos = new ArrayList<>();
        List<Integer> idsDevueltos = new ArrayList<>();
        modelo.separarPorEstado(idsVendidos, idsDevueltos);

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Liquidar " + idsVendidos.size() + " vendida(s) y " + idsDevueltos.size()
                        + " devuelta(s) de " + tipo.getEtiqueta()
                        + (todasLasFechas.isSelected() ? "" : " entregadas el "
                        + Formularios.aLocalDate(campoFecha.getDate()).format(FORMATO_FECHA))
                        + " para " + vendedora.getNombre() + "?",
                "Confirmar liquidación", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        LiquidacionDAO.Resultado resultado;
        try {
            resultado = LiquidacionDAO.liquidar(tipo, vendedora.getId(), idsVendidos, idsDevueltos);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo liquidar: " + e.getMessage(),
                    "Error al liquidar", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // El resultado (sobre todo lo que debe entregar la vendedora) va en una ventana al centro que se queda
        // hasta que se pulsa "Aceptar": es un dato que no debe pasarse por alto.
        new ResultadoLiquidacionDialog((java.awt.Frame) javax.swing.SwingUtilities.getWindowAncestor(this),
                vendedora, tipo, resultado).setVisible(true);

        cargarPendientes();
    }

    /** "hoy", "hace 1 día", "hace 3 días": para ver de un vistazo cuanto lleva pendiente cada boleta. */
    private static String antiguedad(LocalDate entrega) {
        long dias = ChronoUnit.DAYS.between(entrega, LocalDate.now());
        return dias <= 0 ? "hoy" : dias == 1 ? "hace 1 día" : "hace " + dias + " días";
    }

    private static class ModeloLiquidacion extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Vendida", "Número", "Fecha de entrega"};
        private List<Boleta> datos = List.of();
        private boolean[] vendida = new boolean[0];

        /**
         * Las marcas (vendida o devuelta) se recuerdan por boleta, para que sobrevivan cuando la lista se vuelve a
         * cargar (por ejemplo, al mover unas boletas a otra vendedora o al cambiar el filtro de fecha).
         */
        private final Map<Integer, Boolean> marcas = new HashMap<>();

        void setDatos(List<Boleta> datos) {
            this.datos = datos;
            this.vendida = new boolean[datos.size()];
            for (int i = 0; i < datos.size(); i++) {
                vendida[i] = marcas.getOrDefault(datos.get(i).getId(), true);
            }
            fireTableDataChanged();
        }

        void limpiarMarcas() {
            marcas.clear();
        }

        boolean isVendida(int fila) {
            return vendida[fila];
        }

        void setVendida(int fila, boolean esVendida) {
            vendida[fila] = esVendida;
            marcas.put(datos.get(fila).getId(), esVendida);
            fireTableRowsUpdated(fila, fila);
        }

        int idDe(int fila) {
            return datos.get(fila).getId();
        }

        /** Los numeros de cada fila, en el orden de la tabla (null si la boleta todavia no tiene numero). */
        List<String> numeros() {
            List<String> numeros = new ArrayList<>();
            for (Boleta b : datos) {
                numeros.add(b.getNumero());
            }
            return numeros;
        }

        boolean[] vendidas() {
            return vendida.clone();
        }

        void separarPorEstado(List<Integer> idsVendidos, List<Integer> idsDevueltos) {
            for (int i = 0; i < datos.size(); i++) {
                if (vendida[i]) {
                    idsVendidos.add(datos.get(i).getId());
                } else {
                    idsDevueltos.add(datos.get(i).getId());
                }
            }
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
        public Class<?> getColumnClass(int columna) {
            return columna == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int fila, int columna) {
            return columna == 0;
        }

        @Override
        public void setValueAt(Object valor, int fila, int columna) {
            if (columna == 0) {
                vendida[fila] = (Boolean) valor;
                marcas.put(datos.get(fila).getId(), vendida[fila]);
            }
        }

        @Override
        public Object getValueAt(int fila, int columna) {
            Boleta b = datos.get(fila);
            return switch (columna) {
                case 0 -> vendida[fila];
                case 1 -> b.getNumero() == null ? "—" : b.getNumero();
                case 2 -> b.getFechaEntrega().format(FORMATO_FECHA) + "  ·  " + antiguedad(b.getFechaEntrega());
                default -> "";
            };
        }
    }
}
