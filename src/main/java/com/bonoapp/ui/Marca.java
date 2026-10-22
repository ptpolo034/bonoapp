package com.bonoapp.ui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * La imagen de marca de BonoApp: un cuadrado redondeado de color indigo con una boleta blanca. Se dibuja por codigo (en
 * cualquier tamano) y se usa como icono de las ventanas y de la barra de tareas, y tambien para generar el icono
 * del instalador.
 */
public final class Marca {

    private static final int[] TAMANOS = {16, 24, 32, 48, 64, 128, 256};

    private Marca() {
    }

    /** El icono de la app en todos los tamanos que usa Windows (ventana, barra de tareas, accesos directos). */
    public static List<Image> iconos() {
        List<Image> imagenes = new ArrayList<>();
        for (int tamano : TAMANOS) {
            imagenes.add(icono(tamano));
        }
        return imagenes;
    }

    /** El icono de la app dibujado en un cuadrado de "tamano" pixeles. */
    public static BufferedImage icono(int tamano) {
        BufferedImage imagen = new BufferedImage(tamano, tamano, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = imagen.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x4F46E5));
        int arco = Math.max(4, Math.round(tamano * 0.24f));
        g.fillRoundRect(0, 0, tamano, tamano, arco, arco);
        int lado = Math.round(tamano * 0.62f);
        Icon boleta = Iconos.get("ticket", lado, Color.WHITE);
        boleta.paintIcon(null, g, (tamano - lado) / 2, (tamano - lado) / 2);
        g.dispose();
        return imagen;
    }
}
