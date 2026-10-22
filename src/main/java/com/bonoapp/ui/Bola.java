package com.bonoapp.ui;

import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Circulo de color con el numero ganador adentro, como una balota de sorteo. */
final class Bola extends JLabel {

    private static final int DIAMETRO = 64;

    private final Color color;

    Bola(Color color) {
        super("—", CENTER);
        this.color = color;
        setForeground(Color.WHITE);
        setFont(getFont().deriveFont(Font.BOLD, 22f));
        setPreferredSize(new Dimension(DIAMETRO, DIAMETRO));
        setMinimumSize(new Dimension(DIAMETRO, DIAMETRO));
        setMaximumSize(new Dimension(DIAMETRO, DIAMETRO));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor("—".equals(getText()) ? Theme.fondoTono(Theme.Tono.NEUTRO) : color);
        g2.fillOval(0, 0, getWidth(), getHeight());
        g2.dispose();
        // El guion de "sin resultado" va en gris, no en blanco, porque el fondo de esa bola es claro.
        setForeground("—".equals(getText()) ? Theme.TEXTO_SUAVE : Color.WHITE);
        super.paintComponent(g);
    }
}
