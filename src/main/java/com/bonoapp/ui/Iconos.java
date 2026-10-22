package com.bonoapp.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import javax.swing.Icon;
import java.awt.Color;

/**
 * Iconos de la aplicacion: SVG sencillos (estilo Tabler Icons, licencia MIT) guardados en
 * src/main/resources/icons. Se pintan en vectores, asi que se ven nitidos con cualquier tamano de letra.
 */
final class Iconos {

    private Iconos() {
    }

    /** El icono "nombre" (archivo icons/nombre.svg), del tamano y color dados. */
    static Icon get(String nombre, int tamano, Color color) {
        FlatSVGIcon icono = new FlatSVGIcon("icons/" + nombre + ".svg", tamano, tamano);
        icono.setColorFilter(new FlatSVGIcon.ColorFilter(original -> color));
        return icono;
    }
}
