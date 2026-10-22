package com.bonoapp.ui;

import com.bonoapp.ui.Theme.Tono;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Cuadrado de esquinas redondas con un icono al centro; el color indica de que trata la tarjeta (marca, al dia, pendiente). */
final class Insignia extends JComponent {

    private static final int TAMANO = 44;

    private final Tono tono;
    private final Icon icono;

    Insignia(String nombreIcono, Tono tono) {
        this.tono = tono;
        this.icono = Iconos.get(nombreIcono, 22, Theme.textoTono(tono));
        setPreferredSize(new Dimension(TAMANO, TAMANO));
        setMinimumSize(new Dimension(TAMANO, TAMANO));
        setMaximumSize(new Dimension(TAMANO, TAMANO));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Theme.fondoTono(tono));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
        icono.paintIcon(this, g2, (getWidth() - icono.getIconWidth()) / 2, (getHeight() - icono.getIconHeight()) / 2);
        g2.dispose();
    }
}
