package com.bonoapp.model;

/**
 * Forma elegida por el usuario para recuperar el acceso si olvida la contrasena: un codigo que se le
 * entrega una sola vez, o una pregunta de seguridad con su respuesta.
 */
public enum TipoRecuperacion {
    CODIGO("Código de recuperación"),
    PREGUNTA("Pregunta de seguridad");

    private final String etiqueta;

    TipoRecuperacion(String etiqueta) {
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
