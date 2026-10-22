package com.bonoapp.util;

/** Usuario que inicio sesion; se anota en el historial junto a cada cambio. */
public final class Sesion {

    private static volatile String usuario;

    private Sesion() {
    }

    public static String getUsuario() {
        return usuario;
    }

    public static void iniciar(String nombreUsuario) {
        usuario = nombreUsuario;
    }
}
