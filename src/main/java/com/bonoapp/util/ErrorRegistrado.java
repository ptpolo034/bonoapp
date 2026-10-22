package com.bonoapp.util;

/**
 * Una excepcion que se anota sola en el registro de errores en cuanto se crea. Se usa para los fallos de la base de
 * datos y de los archivos (en lugar de RuntimeException), asi quedan registrados con todo su detalle tecnico aunque la
 * pantalla ya muestre un mensaje al usuario.
 */
public class ErrorRegistrado extends RuntimeException {

    public ErrorRegistrado(String mensaje, Throwable causa) {
        super(mensaje, causa);
        Registro.error("Fallo de datos o archivos (el usuario ya vio un mensaje)", this);
    }

    public ErrorRegistrado(String mensaje) {
        super(mensaje);
        Registro.error("Fallo de datos o archivos (el usuario ya vio un mensaje)", this);
    }
}
