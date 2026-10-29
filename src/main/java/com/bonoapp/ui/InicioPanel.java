package com.bonoapp.ui;

import com.bonoapp.db.AhorroDAO;
import com.bonoapp.db.BoletaDAO;
import com.bonoapp.db.GanadorDAO;
import com.bonoapp.db.PrestamoDAO;
import com.bonoapp.db.ReporteDAO;
import com.bonoapp.model.Boleta;
import com.bonoapp.model.CierreDia;
import com.bonoapp.model.NumeroGanador;
import com.bonoapp.model.Pendiente;
import com.bonoapp.model.TipoBoleta;
import com.toedter.calendar.JDateChooser;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Pantalla de inicio: resumenes y consulta rapida de boletas por numero y fecha. */
public class InicioPanel extends JPanel implements Actualizable {

    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));
    private static final int ANCHO_FECHA = 150;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final JLabel valorBoletasHoy = new JLabel("0");
    private final JLabel valorAhorroAcumulado = new JLabel("$ 0");
    private final JLabel valorPrestamos = new JLabel("$ 0");
    private final JLabel cierreRecaudado = new JLabel("$ 0");
    private final JLabel cierreVendidas = new JLabel("0");
    private final JLabel cierreDevueltas = new JLabel("0");
    private final JLabel cierrePorLiquidar = new JLabel("0");

    private final JTextField campoNumero = new JTextField(6);
    private final JDateChooser campoDesde = new JDateChooser();
    private final JDateChooser campoHasta = new JDateChooser();
    private final ModeloBoletas modeloResultados = new ModeloBoletas();
    private final JLabel etiquetaResultado = new JLabel(" ");
    private final Map<TipoBoleta, Bola> numerosGanadores = new EnumMap<>(TipoBoleta.class);
    private final Map<TipoBoleta, JLabel> detallesGanadores = new EnumMap<>(TipoBoleta.class);
    private final Map<TipoBoleta, JPanel> titularesGanadores = new EnumMap<>(TipoBoleta.class);
    private final ModeloPendientes modeloPendientes = new ModeloPendientes();
    private final JLabel resumenPendientes = new JLabel(" ");

    private final CardLayout cartas = new CardLayout();
    private final JPanel cuerpo = new JPanel(cartas);
    private final GuiaPrimerosPasos guia;

    public InicioPanel() {
        this(seccion -> { });
    }

    /** "irASeccion" lleva a otra pantalla del menu (lo usan los botones de la guia de primeros pasos). */
    public InicioPanel(Consumer<String> irASeccion) {
        setLayout(new BorderLayout(0, 24));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Inicio");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        add(titulo, BorderLayout.NORTH);

        JPanel tarjetas = new JPanel(new GridLayout(1, 3, 20, 0));
        tarjetas.setOpaque(false);
        tarjetas.add(tarjeta("Entregadas hoy", valorBoletasHoy, "ticket", Theme.Tono.MARCA));
        tarjetas.add(tarjeta("Ahorro acumulado", valorAhorroAcumulado, "pig-money", Theme.Tono.EXITO));
        tarjetas.add(tarjeta("Préstamos por cobrar", valorPrestamos, "cash", Theme.Tono.ALERTA));

        // "Pendiente" lleva una tabla con 4 columnas y necesita mas ancho que los numeros ganadores.
        JPanel medio = new JPanel(new GridBagLayout());
        medio.setOpaque(false);
        GridBagConstraints celda = new GridBagConstraints();
        celda.fill = GridBagConstraints.BOTH;
        celda.weighty = 1;
        // Los ganadores ocupan un ancho fijo; "Pendiente" (con su tabla de 4 columnas) toma el resto.
        JPanel ganadores = construirGanadores();
        ganadores.setPreferredSize(new Dimension(380, 0));
        ganadores.setMinimumSize(new Dimension(380, 0));
        celda.weightx = 0;
        medio.add(ganadores, celda);
        // Tamaños base pequeños: asi el ancho que sobra va a "Pendiente" y no se reparte segun el contenido.
        JPanel pendientes = construirPendientes();
        pendientes.setPreferredSize(new Dimension(340, 0));
        pendientes.setMinimumSize(new Dimension(340, 0));
        celda.weightx = 1;
        celda.insets = new java.awt.Insets(0, 20, 0, 0);
        medio.add(pendientes, celda);
        medio.setPreferredSize(new Dimension(0, 290));
        medio.setMinimumSize(new Dimension(0, 250));

        JPanel busqueda = construirBusqueda();
        busqueda.setPreferredSize(new Dimension(0, 300));
        busqueda.setMinimumSize(new Dimension(0, 230));

        // Las tarjetas de arriba tienen alto fijo; "Pendiente" y "Consultar boleta" se reparten el alto que sobra
        // de la ventana, y sus tablas llevan su propia barra de desplazamiento (no la pagina entera).
        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setOpaque(false);
        GridBagConstraints fila = new GridBagConstraints();
        fila.gridx = 0;
        fila.weightx = 1;
        fila.fill = GridBagConstraints.BOTH;
        fila.gridy = 0;
        contenido.add(tarjetas, fila);
        fila.gridy = 1;
        fila.insets = new java.awt.Insets(14, 0, 0, 0);
        contenido.add(construirCierre(), fila);
        fila.gridy = 2;
        fila.weighty = 0.45;
        contenido.add(medio, fila);
        fila.gridy = 3;
        fila.weighty = 0.55;
        contenido.add(busqueda, fila);

        // Si la ventana es muy baja y no cabe todo, se vuelve a desplazar la pagina como respaldo.
        JScrollPane scroll = new JScrollPane(new PanelDesplazable(contenido, true));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        guia = new GuiaPrimerosPasos(irASeccion);
        cuerpo.setOpaque(false);
        cuerpo.add(scroll, "resumen");
        cuerpo.add(guia, "guia");
        add(cuerpo, BorderLayout.CENTER);

        actualizar();
    }

    /** Vuelve a calcular los resumenes. Se llama cada vez que se entra a esta seccion. */
    public void actualizar() {
        // La primera vez (sin ninguna boleta registrada) se muestra la guia en lugar de los resumenes vacios.
        if (GuiaPrimerosPasos.hayQueMostrar()) {
            guia.refrescar();
            cartas.show(cuerpo, "guia");
        } else {
            cartas.show(cuerpo, "resumen");
        }
        valorBoletasHoy.setText(String.valueOf(BoletaDAO.contarEntregadasHoy()));
        valorAhorroAcumulado.setText("$ " + FORMATO_PESOS.format(AhorroDAO.totalAcumulado()));
        valorPrestamos.setText("$ " + FORMATO_PESOS.format(PrestamoDAO.totalPendiente()));
        actualizarCierre();
        actualizarGanadores();
        actualizarPendientes();
    }

    /**
     * Tarjeta "Cierre de hoy": lo recaudado hoy, para cuadrar la caja. Como las vendedoras entregan todo el
     * dinero el mismo dia, el recaudado de hoy debe coincidir con el efectivo recibido.
     */
    private JPanel construirCierre() {
        JPanel p = Theme.crearTarjeta();
        p.setLayout(new BorderLayout(24, 0));
        p.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        JLabel titulo = new JLabel("Cierre de hoy");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        JLabel ayuda = new JLabel("Para cuadrar la caja");
        ayuda.setToolTipText("El recaudado de hoy debe coincidir con el efectivo recibido de las vendedoras.");
        ayuda.setForeground(Theme.TEXTO_SUAVE);
        JPanel izquierda = new JPanel(new GridLayout(2, 1, 0, 2));
        izquierda.setOpaque(false);
        izquierda.add(titulo);
        izquierda.add(ayuda);

        cierreRecaudado.setForeground(Theme.PRINCIPAL);
        JPanel cifras = new JPanel(new GridLayout(1, 4, 20, 0));
        cifras.setOpaque(false);
        cifras.add(cifraCierre("Recaudado", cierreRecaudado));
        cifras.add(cifraCierre("Vendidas", cierreVendidas));
        cifras.add(cifraCierre("Devueltas", cierreDevueltas));
        cifras.add(cifraCierre("Por liquidar", cierrePorLiquidar));

        p.add(izquierda, BorderLayout.CENTER);
        p.add(cifras, BorderLayout.EAST);
        return p;
    }

    private JPanel cifraCierre(String etiqueta, JLabel valor) {
        JLabel titulo = new JLabel(etiqueta);
        titulo.setForeground(Theme.TEXTO_SUAVE);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 20f));
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.add(titulo);
        p.add(valor);
        return p;
    }

    private void actualizarCierre() {
        CierreDia cierre = ReporteDAO.cierreDelDia(LocalDate.now());
        cierreRecaudado.setText("$ " + FORMATO_PESOS.format(cierre.recaudado()));
        cierreVendidas.setText(String.valueOf(cierre.vendidas()));
        cierreDevueltas.setText(String.valueOf(cierre.devueltas()));
        cierrePorLiquidar.setText(String.valueOf(cierre.porLiquidar()));
        // Ambar si todavia falta liquidar algo de lo entregado hoy; verde si ya esta todo al dia.
        cierrePorLiquidar.setForeground(cierre.porLiquidar() > 0 ? Theme.ALERTA : Theme.EXITO);
    }

    /** Tarjeta con el ultimo numero ganador de cada sorteo y quien lo tiene. */
    private JPanel construirGanadores() {
        JPanel p = Theme.crearTarjeta();
        p.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Números ganadores");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        JPanel columnas = new JPanel(new GridLayout(1, 2, 24, 0));
        columnas.setOpaque(false);
        for (TipoBoleta tipo : TipoBoleta.values()) {
            // Cada sorteo con su color de bola: indigo para la Diaria y violeta para el Premio mayor.
            Bola numero = new Bola(tipo == TipoBoleta.DIARIA ? Theme.PRINCIPAL : new Color(0x7C3AED));
            JLabel detalle = new JLabel(" ", JLabel.CENTER);
            detalle.setForeground(Theme.TEXTO_SUAVE);
            numerosGanadores.put(tipo, numero);
            detallesGanadores.put(tipo, detalle);

            JLabel etiqueta = new JLabel(tipo.getEtiqueta(), JLabel.CENTER);
            etiqueta.setFont(etiqueta.getFont().deriveFont(Font.BOLD));
            etiqueta.setAlignmentX(Component.CENTER_ALIGNMENT);
            numero.setAlignmentX(Component.CENTER_ALIGNMENT);
            detalle.setAlignmentX(Component.CENTER_ALIGNMENT);
            JPanel col = new JPanel();
            col.setOpaque(false);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.add(numero);
            col.add(Box.createVerticalStrut(8));
            col.add(etiqueta);
            col.add(Box.createVerticalStrut(2));
            col.add(detalle);
            col.add(Box.createVerticalStrut(6));
            JPanel lista = new JPanel();
            lista.setOpaque(false);
            lista.setLayout(new BoxLayout(lista, BoxLayout.Y_AXIS));
            lista.setAlignmentX(Component.CENTER_ALIGNMENT);
            titularesGanadores.put(tipo, lista);
            col.add(lista);
            columnas.add(col);
        }

        p.add(titulo, BorderLayout.NORTH);
        p.add(columnas, BorderLayout.CENTER);
        return p;
    }

    private void actualizarGanadores() {
        for (TipoBoleta tipo : TipoBoleta.values()) {
            NumeroGanador ganador = GanadorDAO.ultimo(tipo);
            Bola numero = numerosGanadores.get(tipo);
            JLabel detalle = detallesGanadores.get(tipo);
            if (ganador == null) {
                numero.setText("—");
                detalle.setText("Sin resultado");
                JPanel lista = titularesGanadores.get(tipo);
                lista.removeAll();
                JLabel ayuda = new JLabel("Anótalo desde Boletas", JLabel.CENTER);
                ayuda.setForeground(Theme.TEXTO_SUAVE);
                ayuda.setAlignmentX(Component.CENTER_ALIGNMENT);
                lista.add(ayuda);
                lista.revalidate();
                lista.repaint();
                continue;
            }
            numero.setText(ganador.numero());
            detalle.setText("Sorteo del " + ganador.fecha().format(FORMATO_FECHA));

            // Una fila por vendedora que tiene una boleta con ese numero, con su estado en una etiqueta de color.
            // Pueden ser dos (el numero se repite hasta 2 veces al dia); si es la misma vendedora se agrupa.
            JPanel lista = titularesGanadores.get(tipo);
            lista.removeAll();
            List<Titulares.Fila> filas = Titulares.agrupar(GanadorDAO.titulares(tipo, ganador.numero(), ganador.fecha()));
            if (filas.isEmpty()) {
                JLabel nadie = new JLabel("Nadie tiene esta boleta", JLabel.CENTER);
                nadie.setForeground(Theme.TEXTO_SUAVE);
                nadie.setAlignmentX(Component.CENTER_ALIGNMENT);
                lista.add(nadie);
            }
            for (Titulares.Fila fila : filas) {
                JPanel renglon = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 1));
                renglon.setOpaque(false);
                renglon.setAlignmentX(Component.CENTER_ALIGNMENT);
                Chip estado = new Chip();
                estado.setText(fila.estado().getEtiqueta());
                estado.setFont(estado.getFont().deriveFont(estado.getFont().getSize2D() - 1.5f));
                estado.setTono(Botones.TONO_ESTADO_BOLETA.apply(fila.estado().getEtiqueta()));
                renglon.add(new JLabel(fila.texto()));
                renglon.add(estado);
                lista.add(renglon);
            }
            lista.revalidate();
            lista.repaint();
        }
    }

    /** Tarjeta con lo que queda pendiente por vendedora: boletas sin liquidar y prestamos por cobrar. */
    private JPanel construirPendientes() {
        JPanel p = Theme.crearTarjeta();
        p.setLayout(new BorderLayout(0, 8));

        JLabel titulo = new JLabel("Pendiente");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));
        resumenPendientes.setForeground(Theme.TEXTO_SUAVE);
        JPanel cabecera = new JPanel(new BorderLayout(0, 2));
        cabecera.setOpaque(false);
        cabecera.add(titulo, BorderLayout.NORTH);
        cabecera.add(resumenPendientes, BorderLayout.CENTER);

        JTable tabla = new JTable(modeloPendientes);
        tabla.setEnabled(false);
        Tablas.estilizar(tabla, 1, 3);
        DefaultTableCellRenderer derecha = new DefaultTableCellRenderer();
        derecha.setHorizontalAlignment(DefaultTableCellRenderer.RIGHT);
        tabla.getColumnModel().getColumn(1).setCellRenderer(derecha);
        tabla.getColumnModel().getColumn(3).setCellRenderer(derecha);
        // "Desde": una etiqueta que se pone ambar cuando lleva 3 dias o mas sin liquidar.
        tabla.getColumnModel().getColumn(2).setCellRenderer(new ChipRenderer(InicioPanel::tonoAntiguedad));
        tabla.getColumnModel().getColumn(0).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(105);

        p.add(cabecera, BorderLayout.NORTH);
        p.add(Tablas.enDesplazamiento(tabla), BorderLayout.CENTER);
        return p;
    }

    /** Tono de la etiqueta "Desde": ambar si hace 3 dias o mas, neutro si es reciente, sin color para "—". */
    private static Theme.Tono tonoAntiguedad(String texto) {
        if (!texto.startsWith("hace")) {
            return texto.equals("hoy") ? Theme.Tono.NEUTRO : null;
        }
        String digitos = texto.replaceAll("\\D", "");
        int dias = digitos.isEmpty() ? 0 : Integer.parseInt(digitos);
        return dias >= 3 ? Theme.Tono.ALERTA : Theme.Tono.NEUTRO;
    }

    /** "hoy", "hace 1 día", "hace 3 días": mas facil de leer que una fecha. */
    private static String textoAntiguedad(LocalDate desde) {
        if (desde == null) {
            return "—";
        }
        long dias = java.time.temporal.ChronoUnit.DAYS.between(desde, LocalDate.now());
        return dias <= 0 ? "hoy" : dias == 1 ? "hace 1 día" : "hace " + dias + " días";
    }

    private void actualizarPendientes() {
        List<Pendiente> pendientes = ReporteDAO.pendientesPorVendedora();
        modeloPendientes.setDatos(pendientes);
        int boletas = 0;
        int debe = 0;
        for (Pendiente x : pendientes) {
            boletas += x.boletasSinLiquidar();
            debe += x.debe();
        }
        resumenPendientes.setText(pendientes.isEmpty()
                ? "Todo al día: no hay boletas sin liquidar ni préstamos por cobrar."
                : boletas + " boleta(s) sin liquidar · $ " + FORMATO_PESOS.format(debe) + " por cobrar");
    }

    private static class ModeloPendientes extends AbstractTableModel {
        private static final String[] COLUMNAS = {"Vendedora", "Boletas", "Desde", "Debe"};
        private List<Pendiente> datos = new ArrayList<>();

        void setDatos(List<Pendiente> datos) {
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
            Pendiente x = datos.get(fila);
            return switch (columna) {
                case 0 -> x.vendedoraNombre();
                case 1 -> x.boletasSinLiquidar() == 0 ? "—" : x.boletasSinLiquidar();
                case 2 -> textoAntiguedad(x.entregadasDesde());
                case 3 -> x.debe() == 0 ? "—" : "$ " + FORMATO_PESOS.format(x.debe());
                default -> "";
            };
        }
    }

    private JPanel construirBusqueda() {
        JPanel p = Theme.crearTarjeta();
        p.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Consultar boleta");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        campoDesde.setDateFormatString("dd/MM/yyyy");
        campoHasta.setDateFormatString("dd/MM/yyyy");
        Dimension tamanoFecha = new Dimension(ANCHO_FECHA, campoNumero.getPreferredSize().height);
        campoDesde.setPreferredSize(tamanoFecha);
        campoHasta.setPreferredSize(tamanoFecha);
        campoNumero.addActionListener(e -> buscar());

        JButton botonBuscar = Botones.principal("Buscar", "search");
        botonBuscar.addActionListener(e -> buscar());
        JButton botonLimpiar = Botones.secundario("Limpiar", "x");
        botonLimpiar.addActionListener(e -> limpiar());

        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.setOpaque(false);
        filtros.add(new JLabel("Número"));
        filtros.add(campoNumero);
        filtros.add(new JLabel("Entregada desde"));
        filtros.add(campoDesde);
        filtros.add(new JLabel("hasta"));
        filtros.add(campoHasta);
        filtros.add(botonBuscar);
        filtros.add(botonLimpiar);

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.setOpaque(false);
        norte.add(titulo, BorderLayout.NORTH);
        norte.add(filtros, BorderLayout.CENTER);

        JTable tabla = new JTable(modeloResultados);
        Tablas.estilizar(tabla);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new ChipRenderer(Botones.TONO_ESTADO_BOLETA));

        etiquetaResultado.setForeground(Theme.TEXTO_SUAVE);

        p.add(norte, BorderLayout.NORTH);
        p.add(Tablas.enDesplazamiento(tabla), BorderLayout.CENTER);
        p.add(etiquetaResultado, BorderLayout.SOUTH);
        return p;
    }

    private void buscar() {
        String numero = null;
        String texto = campoNumero.getText().trim();
        if (!texto.isEmpty()) {
            try {
                List<String> leidos = NumerosBoleta.parsear(texto);
                if (leidos.size() != 1) {
                    throw new IllegalArgumentException("Escribe un solo número de boleta (de 1 a 3 cifras)");
                }
                numero = leidos.get(0);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "Datos inválidos", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        LocalDate desde = Formularios.aLocalDate(campoDesde.getDate());
        LocalDate hasta = Formularios.aLocalDate(campoHasta.getDate());

        if (numero == null && desde == null && hasta == null) {
            JOptionPane.showMessageDialog(this, "Escribe un número o elige una fecha para buscar",
                    "Nada que buscar", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            JOptionPane.showMessageDialog(this, "La fecha \"desde\" no puede ser posterior a \"hasta\"",
                    "Datos inválidos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<Boleta> resultado;
        try {
            resultado = BoletaDAO.buscar(numero, desde, hasta);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo buscar: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        modeloResultados.setDatos(resultado);
        etiquetaResultado.setText(resultado.isEmpty()
                ? "No hay boletas con ese filtro."
                : resultado.size() + " resultado(s)" + (resultado.size() == 500 ? " (se muestran las 500 más recientes)" : ""));
    }

    private void limpiar() {
        campoNumero.setText("");
        campoDesde.setDate(null);
        campoHasta.setDate(null);
        modeloResultados.setDatos(List.of());
        etiquetaResultado.setText(" ");
    }

    /** Tarjeta de resumen: insignia con icono a la izquierda, y el titulo y la cifra grande a su lado. */
    private JPanel tarjeta(String texto, JLabel valor, String icono, Theme.Tono tono) {
        JPanel p = Theme.crearTarjeta();
        p.setLayout(new BorderLayout(14, 0));
        p.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel etiqueta = new JLabel(texto);
        etiqueta.setForeground(Theme.TEXTO_SUAVE);
        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 26f));

        JPanel textos = new JPanel(new BorderLayout(0, 2));
        textos.setOpaque(false);
        textos.add(etiqueta, BorderLayout.NORTH);
        textos.add(valor, BorderLayout.CENTER);

        JPanel izquierda = new JPanel(new GridBagLayout());
        izquierda.setOpaque(false);
        izquierda.add(new Insignia(icono, tono));

        p.add(izquierda, BorderLayout.WEST);
        p.add(textos, BorderLayout.CENTER);
        return p;
    }
}
