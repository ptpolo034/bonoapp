package com.bonoapp.model;

/**
 * Los dos sorteos que maneja el negocio: la boleta Diaria y la del Premio mayor. Cada tipo tiene su
 * propio precio (Configuracion) y su propio lote de 2.000 boletas.
 */
public enum TipoBoleta {
    DIARIA("Diaria"),
    PREMIO_MAYOR("Premio mayor");

    private final String etiqueta;

    TipoBoleta(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
