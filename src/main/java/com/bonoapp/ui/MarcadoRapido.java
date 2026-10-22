package com.bonoapp.ui;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Empareja los numeros que el usuario escribe o pega ("123 456 789") con las filas de la lista de boletas
 * pendientes, para marcarlas como devueltas o moverlas a otra vendedora de una sola vez.
 *
 * Un numero puede estar hasta 2 veces el mismo dia, asi que si se escribe una sola vez se toma una sola boleta, y
 * si se escribe dos veces, las dos. Los numeros que no estan en la lista se devuelven aparte para avisarlos.
 */
public final class MarcadoRapido {

    /** Filas encontradas (indices en la lista, de menor a mayor) y los numeros escritos que no se encontraron. */
    public record Resultado(List<Integer> filas, List<String> noEncontrados) {
    }

    private MarcadoRapido() {
    }

    /**
     * @param numerosDeFilas el numero de cada fila (null si la boleta no tiene numero todavia)
     * @param disponible     para cada fila, si se puede elegir (por ejemplo, solo las que siguen como vendidas)
     * @param escritos       los numeros escritos, ya normalizados a 3 cifras y con sus repetidos
     */
    public static Resultado emparejar(List<String> numerosDeFilas, boolean[] disponible, List<String> escritos) {
        Map<String, ArrayDeque<Integer>> porNumero = new HashMap<>();
        for (int i = 0; i < numerosDeFilas.size(); i++) {
            String numero = numerosDeFilas.get(i);
            if (numero != null && disponible[i]) {
                porNumero.computeIfAbsent(numero, k -> new ArrayDeque<>()).add(i);
            }
        }
        List<Integer> filas = new ArrayList<>();
        List<String> noEncontrados = new ArrayList<>();
        for (String numero : escritos) {
            ArrayDeque<Integer> candidatas = porNumero.get(numero);
            if (candidatas == null || candidatas.isEmpty()) {
                noEncontrados.add(numero);
            } else {
                filas.add(candidatas.poll());
            }
        }
        Collections.sort(filas);
        return new Resultado(filas, noEncontrados);
    }
}
