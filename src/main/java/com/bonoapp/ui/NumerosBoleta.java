package com.bonoapp.ui;

import java.util.ArrayList;
import java.util.List;

/** Lee numeros de boleta escritos a mano (separados por coma, espacio o salto de linea). */
final class NumerosBoleta {

    private NumerosBoleta() {
    }

    /**
     * Devuelve los numeros normalizados a 3 cifras ("5" pasa a "005"), en el mismo orden y SIN
     * quitar repetidos (con ~2.000 boletas los numeros se repiten). Lanza IllegalArgumentException
     * con un mensaje listo para mostrar si algun valor no es un numero de 1 a 3 cifras.
     */
    static List<String> parsear(String texto) {
        List<String> numeros = new ArrayList<>();
        for (String parte : texto.trim().split("[,\\s]+")) {
            if (parte.isBlank()) {
                continue;
            }
            if (!parte.matches("\\d{1,3}")) {
                throw new IllegalArgumentException(
                        "\"" + parte + "\" no es un numero de boleta valido (maximo 3 cifras)");
            }
            numeros.add(String.format("%03d", Integer.parseInt(parte)));
        }
        return numeros;
    }
}
