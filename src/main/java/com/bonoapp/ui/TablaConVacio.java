package com.bonoapp.ui;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.function.BooleanSupplier;

/**
 * Una tabla que, cuando no tiene filas, muestra un mensaje amable en vez de un recuadro en blanco:
 * un icono, un titulo, una explicacion corta y (opcional) el boton de la accion que llena la pantalla.
 * Si hay una busqueda activa y no encuentra nada, el mensaje cambia a "Sin resultados".
 */
final class TablaConVacio extends JPanel {

    private static final String TABLA = "tabla";
    private static final String VACIO = "vacio";

    private final CardLayout cartas = new CardLayout();
    private final JTable tabla;
    private final BooleanSupplier hayFiltro;
    private final JLabel titulo = new JLabel();
    private final JLabel detalle = new JLabel();
    private final JLabel icono = new JLabel();
    private String tituloVacio;
    private String detalleVacio;
    private final JButton accion;

    /**
     * @param nombreIcono nombre del icono (icons/nombre.svg)
     * @param tituloVacio titulo cuando de verdad no hay datos, por ejemplo "Aún no hay préstamos"
     * @param detalleVacio explicacion corta, por ejemplo "Cuando le prestes a una vendedora, aparecerá aquí."
     * @param accion boton para crear el primer elemento, o null si no aplica
     * @param hayFiltro dice si el usuario tiene una busqueda escrita
     */
    TablaConVacio(JTable tabla, String nombreIcono, String tituloVacio, String detalleVacio, JButton accion,
                  BooleanSupplier hayFiltro) {
        super(new CardLayout());
        setOpaque(false);
        setLayout(cartas);
        this.tabla = tabla;
        this.hayFiltro = hayFiltro;
        this.tituloVacio = tituloVacio;
        this.detalleVacio = detalleVacio;
        this.accion = accion;

        add(Tablas.enDesplazamiento(tabla), TABLA);
        add(construirMensaje(nombreIcono), VACIO);

        tabla.getModel().addTableModelListener(e -> actualizar());
        // Con un filtro de filas (ordenador) el modelo puede tener datos y la vista ninguno.
        if (tabla.getRowSorter() != null) {
            tabla.getRowSorter().addRowSorterListener(e -> actualizar());
        }
        actualizar();
    }

    private JComponent construirMensaje(String nombreIcono) {
        icono.setIcon(Iconos.get(nombreIcono, 40, Theme.PRINCIPAL));
        icono.setAlignmentX(Component.CENTER_ALIGNMENT);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, titulo.getFont().getSize2D() + 2f));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        detalle.setForeground(Theme.TEXTO_SUAVE);
        detalle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel centro = new JPanel();
        centro.setOpaque(false);
        centro.setLayout(new BoxLayout(centro, BoxLayout.Y_AXIS));
        centro.add(icono);
        centro.add(Box.createVerticalStrut(10));
        centro.add(titulo);
        centro.add(Box.createVerticalStrut(4));
        centro.add(detalle);
        if (accion != null) {
            accion.setAlignmentX(Component.CENTER_ALIGNMENT);
            centro.add(Box.createVerticalStrut(14));
            centro.add(accion);
        }

        JPanel fondo = new JPanel(new java.awt.GridBagLayout());
        fondo.setBackground(Theme.TARJETA);
        fondo.setBorder(BorderFactory.createLineBorder(Theme.BORDE));
        fondo.add(centro);
        return fondo;
    }

    /** Cambia el mensaje de "sin datos" (por ejemplo segun el dia que se esta viendo). Se aplica antes de cargar las filas. */
    void setMensajeVacio(String titulo, String detalle) {
        this.tituloVacio = titulo;
        this.detalleVacio = detalle;
        actualizar();
    }

    private void actualizar() {
        if (tabla.getRowCount() > 0) {
            cartas.show(this, TABLA);
            return;
        }
        boolean filtrando = hayFiltro.getAsBoolean();
        titulo.setText(filtrando ? "Sin resultados" : tituloVacio);
        detalle.setText(filtrando ? "Prueba con otro nombre, documento o número." : detalleVacio);
        if (accion != null) {
            accion.setVisible(!filtrando);
        }
        cartas.show(this, VACIO);
    }
}
