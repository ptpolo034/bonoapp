package com.bonoapp.model;

/**
 * En que punto de su vida esta una boleta: recien entregada a la vendedora (pendiente de liquidar),
 * vendida o devuelta (estas dos se fijan al liquidar y se deshacen si la liquidacion se revierte).
 */
public enum EstadoBoleta {
    ENTREGADA("Entregada"),
    VENDIDA("Vendida"),
    DEVUELTA("Devuelta");

    private final String etiqueta;

    EstadoBoleta(String etiqueta) {
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
