package com.bonoapp.model;

import java.time.LocalDate;

/** Una liquidacion hecha: cuantas boletas se vendieron y devolvieron, y cuanto se recaudo a ese precio. */
public record Liquidacion(int id, String vendedoraNombre, String vendedoraDocumento, TipoBoleta tipo, LocalDate fecha, int vendidas,
                          int devueltas, int precio, int recaudado, boolean anulada) {
}
