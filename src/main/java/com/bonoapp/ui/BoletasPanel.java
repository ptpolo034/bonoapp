package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.model.Boleta;
import com.bonoapp.model.EstadoBoleta;
import com.toedter.calendar.JDateChooser;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Pantalla de gestion de boletas: registrar entregas a vendedoras, listar y buscar.
 *
 * Por defecto muestra solo las boletas de HOY, para que cada dia empiece en blanco y nadie se enrede
 * con las de dias anteriores (que siguen guardadas): hay un selector de dia y la opcion "Todos los dias".
 * Al escribir en el buscador se busca en TODOS los dias, sin importar el dia elegido.
 */
public class BoletasPanel extends JPanel implements Actualizable {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ModeloBoletas modelo = new ModeloBoletas();
    private final JTable tabla = new JTable(modelo);
    private final JTextField campoBusqueda = new JTextField(20);
    private final JLabel titulo = new JLabel("Boletas");
    private final JDateChooser campoDia = new JDateChooser(new Date());
    private final JCheckBox todosLosDias = new JCheckBox("Todos los días");
    private final JLabel avisoAnteriores = new JLabel(" ");
    private TablaConVacio tablaConVacio;
    private static final int LIMITE_BUSQUEDA = 1000;
    private static final int LIMITE_TODOS_LOS_DIAS = 2000;

    public BoletasPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));

        JLabel etiquetaBusqueda = new JLabel("Buscar (número, documento o nombre):");
        campoBusqueda.putClientProperty("JTextField.placeholderText", "En todos los días");
        campoBusqueda.setToolTipText("La búsqueda incluye todos los días, no solo el que se está viendo");
        campoBusqueda.getDocument().addDocumentListener(new BuscadorListener(this::filtrar));

        JPanel busqueda = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        busqueda.setOpaque(false);
        busqueda.add(etiquetaBusqueda);
        busqueda.add(Box.createHorizontalStrut(8));
        busqueda.add(campoBusqueda);

        JPanel filaTitulo = new JPanel(new BorderLayout());
        filaTitulo.setOpaque(false);
        filaTitulo.add(titulo, BorderLayout.WEST);
        filaTitulo.add(busqueda, BorderLayout.EAST);

        // Selector de dia: por defecto hoy; "Todos los dias" lo desactiva.
        campoDia.setDateFormatString("dd/MM/yyyy");
        campoDia.setPreferredSize(new Dimension(150, campoBusqueda.getPreferredSize().height));
        campoDia.addPropertyChangeListener("date", e -> filtrar());
        todosLosDias.setOpaque(false);
        todosLosDias.addActionListener(e -> {
            campoDia.setEnabled(!todosLosDias.isSelected());
            filtrar();
        });
        JButton botonHoy = Botones.secundario("Hoy", "home");
        botonHoy.addActionListener(e -> verDia(new Date()));
        avisoAnteriores.setForeground(Theme.ALERTA);

        JPanel filaDia = new JPanel();
        filaDia.setOpaque(false);
        filaDia.setLayout(new BoxLayout(filaDia, BoxLayout.X_AXIS));
        filaDia.add(new JLabel("Día:"));
        filaDia.add(Box.createHorizontalStrut(8));
        filaDia.add(campoDia);
        filaDia.add(Box.createHorizontalStrut(8));
        filaDia.add(botonHoy);
        filaDia.add(Box.createHorizontalStrut(12));
        filaDia.add(todosLosDias);
        filaDia.add(Box.createHorizontalStrut(16));
        filaDia.add(avisoAnteriores);
        filaDia.add(Box.createHorizontalGlue());

        JPanel encabezado = new JPanel(new BorderLayout(0, 10));
        encabezado.setOpaque(false);
        encabezado.add(filaTitulo, BorderLayout.NORTH);
        encabezado.add(filaDia, BorderLayout.CENTER);
        add(encabezado, BorderLayout.NORTH);

        Tablas.estilizar(tabla);
        tabla.getColumnModel().getColumn(4).setCellRenderer(new ChipRenderer(Botones.TONO_ESTADO_BOLETA));
        tablaConVacio = new TablaConVacio(tabla, "ticket", "Hoy aún no hay entregas",
                "Pulsa \"Registrar entrega\" para anotar la primera del día.", null,
                () -> !campoBusqueda.getText().isBlank());
        add(tablaConVacio, BorderLayout.CENTER);

        JButton botonEntrega = Botones.principal("Registrar entrega", "plus");
        JButton botonEditar = Botones.secundario("Editar", "edit");
        JButton botonMover = Botones.secundario("Mover", "arrows-exchange");
        botonMover.setToolTipText("Pasar las boletas seleccionadas a otra vendedora");
        JButton botonGanador = Botones.secundario("Número ganador", "trophy");
        JButton botonEliminar = Botones.peligro("Eliminar selección", "trash");

        botonEntrega.addActionListener(e -> registrarEntrega());
        botonEditar.addActionListener(e -> editarSeleccion());
        botonMover.addActionListener(e -> moverSeleccion());
        botonGanador.addActionListener(e -> new GanadorDialog(ventana()).setVisible(true));
        botonEliminar.addActionListener(e -> eliminarSeleccion());

        add(Botones.fila(botonEliminar, botonEntrega, botonEditar, botonMover, botonGanador), BorderLayout.SOUTH);

        cargar();
    }

    /** Vuelve a leer las boletas al entrar a la seccion (por ejemplo, tras liquidar en otra pantalla). */
    @Override
    public void actualizar() {
        cargar();
    }

    /** Vuelve a leer lo que se esta viendo (el dia elegido, o el resultado de la busqueda). */
    private void cargar() {
        filtrar();
    }

    /** Pasa a ver un dia concreto (sale de "Todos los dias" y limpia la busqueda para que se vea ese dia). */
    private void verDia(Date dia) {
        todosLosDias.setSelected(false);
        campoDia.setEnabled(true);
        campoBusqueda.setText("");
        campoDia.setDate(dia);
        filtrar();
    }

    /**
     * Sin texto en el buscador: muestra el dia elegido (hoy por defecto) o todos los dias.
     * Con texto: busca en TODOS los dias. El numero de boleta debe escribirse completo (busqueda exacta)
     * para no confundir, por ejemplo, la boleta "02" con la "002" o con un documento que contenga esos
     * digitos; el documento y el nombre de la vendedora se buscan por coincidencia parcial.
     *
     * Siempre se consulta la base de datos con lo que se va a mostrar (un dia, o las mas recientes, o los
     * resultados): nunca se carga todo el historial en memoria, porque con los meses habria cientos de miles.
     */
    private void filtrar() {
        String texto = campoBusqueda.getText().trim();
        LocalDate dia = Formularios.aLocalDate(campoDia.getDate());
        boolean verTodos = todosLosDias.isSelected() || dia == null;

        List<Boleta> mostradas;
        if (!texto.isEmpty()) {
            mostradas = BoletaDAO.buscarEnTodos(texto, LIMITE_BUSQUEDA);
        } else if (verTodos) {
            mostradas = BoletaDAO.listarRecientes(LIMITE_TODOS_LOS_DIAS);
        } else {
            mostradas = BoletaDAO.listarPorFecha(dia);
        }

        actualizarTitulos(!texto.isEmpty(), verTodos, dia, mostradas.size());
        modelo.setDatos(mostradas);
    }

    /** Titulo segun lo que se esta viendo, el mensaje de "sin datos" de ese dia y el aviso de dias anteriores. */
    private void actualizarTitulos(boolean buscando, boolean verTodos, LocalDate dia, int cantidadMostrada) {
        LocalDate hoy = LocalDate.now();
        if (buscando) {
            titulo.setText(cantidadMostrada >= LIMITE_BUSQUEDA
                    ? "Boletas · primeros " + LIMITE_BUSQUEDA + " resultados"
                    : "Boletas · resultados de la búsqueda");
        } else if (verTodos) {
            titulo.setText(cantidadMostrada >= LIMITE_TODOS_LOS_DIAS
                    ? "Boletas · las " + LIMITE_TODOS_LOS_DIAS + " más recientes"
                    : "Boletas · todos los días");
        } else if (dia.equals(hoy)) {
            titulo.setText("Boletas de hoy");
        } else {
            titulo.setText("Boletas del " + dia.format(FORMATO_FECHA));
        }

        if (verTodos) {
            tablaConVacio.setMensajeVacio("Aún no hay boletas",
                    "Pulsa \"Registrar entrega\" para anotar la primera.");
        } else if (dia.equals(hoy)) {
            tablaConVacio.setMensajeVacio("Hoy aún no hay entregas",
                    "Pulsa \"Registrar entrega\" para anotar la primera del día.");
        } else {
            tablaConVacio.setMensajeVacio("No hubo entregas el " + dia.format(FORMATO_FECHA),
                    "Elige otro día o marca \"Todos los días\".");
        }

        // Lo que quedo sin liquidar de dias anteriores no se ve en la tabla de hoy: se avisa para no olvidarlo.
        int anteriores = BoletaDAO.contarPendientesAnteriores(hoy);
        avisoAnteriores.setText(anteriores == 0 ? " "
                : anteriores + " boleta(s) de días anteriores sin liquidar");
    }

    private void registrarEntrega() {
        EntregaBoletasDialog dialogo = new EntregaBoletasDialog(ventana());
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            cargar();
            // Se muestra el dia de la entrega recien registrada, para que se vea lo que se acaba de hacer.
            LocalDate fecha = dialogo.getFechaEntregada();
            if (fecha != null && !todosLosDias.isSelected()) {
                verDia(Date.from(fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()));
            }
        }
    }

    private void editarSeleccion() {
        int[] filasVista = tabla.getSelectedRows();
        if (filasVista.length != 1) {
            JOptionPane.showMessageDialog(this, "Selecciona una sola boleta de la lista",
                    "Selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Boleta boleta = modelo.getFila(tabla.convertRowIndexToModel(filasVista[0]));
        EditarBoletaDialog dialogo = new EditarBoletaDialog(ventana(), boleta);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            cargar();
        }
    }

    private void moverSeleccion() {
        int[] filasVista = tabla.getSelectedRows();
        if (filasVista.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una o más boletas de la lista",
                    "Ninguna selección", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Integer> ids = new ArrayList<>();
        for (int filaVista : filasVista) {
            Boleta b = modelo.getFila(tabla.convertRowIndexToModel(filaVista));
            if (b.getEstado() == EstadoBoleta.ENTREGADA) {
                ids.add(b.getId());
            }
        }
        if (ids.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Solo se pueden mover boletas pendientes de liquidar (estado Entregada).",
                    "No se puede mover", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        MoverBoletasDialog dialogo = new MoverBoletasDialog(ventana(), ids);
        dialogo.setVisible(true);
        if (dialogo.isConfirmado()) {
            cargar();
        }
    }

    private void eliminarSeleccion() {
        int[] filasVista = tabla.getSelectedRows();
        if (filasVista.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona una o mas boletas de la lista",
                    "Ninguna seleccion", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "¿Eliminar " + filasVista.length + " boleta(s) seleccionada(s)?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        List<Integer> ids = new ArrayList<>();
        for (int filaVista : filasVista) {
            ids.add(modelo.getFila(tabla.convertRowIndexToModel(filaVista)).getId());
        }
        BoletaDAO.eliminarVarios(ids);
        cargar();
    }

    private Frame ventana() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }
}
