package com.bonoapp.ui;

import javax.swing.JPanel;
import javax.swing.Scrollable;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * Envuelve un panel para que dentro de un JScrollPane su ancho siempre llene el viewport
 * (asi el contenido interno se puede distribuir en columnas) pero el alto siga siendo el
 * necesario para que aparezca la barra de desplazamiento vertical cuando no quepa todo.
 */
class PanelDesplazable extends JPanel implements Scrollable {

    private final boolean ajustarAlto;

    PanelDesplazable(Component contenido) {
        this(contenido, false);
    }

    /**
     * Con "ajustarAlto", mientras la ventana tenga al menos el alto minimo del contenido, este se estira o se
     * encoge para llenar la ventana (sin barra para bajar la pagina entera, y asi las barras quedan solo dentro
     * de las tablas). Si la ventana es mas baja que ese minimo, vuelve a desplazarse como antes.
     */
    PanelDesplazable(Component contenido, boolean ajustarAlto) {
        super(new BorderLayout());
        this.ajustarAlto = ajustarAlto;
        setOpaque(false);
        // Espacio a la derecha: asi las tarjetas no quedan pegadas a la barra de desplazamiento (parecia un corte).
        setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 14));
        add(contenido, BorderLayout.CENTER);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 16;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (!ajustarAlto) {
            return false;
        }
        return getParent() instanceof javax.swing.JViewport viewport
                && viewport.getHeight() >= getMinimumSize().height;
    }
}
