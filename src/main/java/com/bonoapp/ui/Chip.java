package com.bonoapp.ui;

import com.bonoapp.ui.Theme.Tono;

import javax.swing.JLabel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Etiqueta pequena con fondo de color y esquinas redondas ("Vendida", "Pendiente"...). El color
 * depende del {@link Tono}, que significa un estado: verde = al dia, ambar = pendiente, rojo = deuda.
 */
final class Chip extends JLabel {

    private Tono tono = Tono.NEUTRO;

    Chip() {
        setOpaque(false);
        setHorizontalAlignment(CENTER);
    }

    /** El tono de color; con null el texto se ve plano, sin fondo (para valores como "—"). */
    void setTono(Tono tono) {
        this.tono = tono;
        setForeground(tono == null ? Theme.TEXTO_SUAVE : Theme.textoTono(tono));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(d.width + 16, d.height + 4);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (tono != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.fondoTono(tono));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
