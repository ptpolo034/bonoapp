package com.bonoapp.ui;

import com.bonoapp.model.Boleta;
import com.bonoapp.model.EstadoBoleta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrupa las boletas que tienen el numero ganador por vendedora y estado. Como un numero puede estar
 * hasta 2 veces el mismo dia, dos boletas ganadoras pueden ser de vendedoras distintas (cada una con su
 * propio estado) o de la misma vendedora ("Ana (2 boletas)").
 */
final class Titulares {

    /** Una vendedora con cuantas boletas ganadoras tiene en un mismo estado. */
    record Fila(String nombre, EstadoBoleta estado, int cantidad) {

        /** "Ana" o, si tiene mas de una, "Ana (2 boletas)". */
        String texto() {
            return cantidad > 1 ? nombre + " (" + cantidad + " boletas)" : nombre;
        }
    }

    private Titulares() {
    }

    static List<Fila> agrupar(List<Boleta> boletas) {
        Map<String, Fila> filas = new LinkedHashMap<>();
        for (Boleta b : boletas) {
            String clave = b.getVendedoraNombre() + "|" + b.getEstado();
            Fila existente = filas.get(clave);
            filas.put(clave, new Fila(b.getVendedoraNombre(), b.getEstado(),
                    existente == null ? 1 : existente.cantidad() + 1));
        }
        return new ArrayList<>(filas.values());
    }
}
