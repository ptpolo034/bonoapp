package com.bonoapp.model;

/**
 * Resumen de un dia para cuadrar la caja: lo recaudado en las liquidaciones de ese dia (sin contar las
 * revertidas), cuantas boletas se vendieron y devolvieron, y cuantas de las entregadas ese dia siguen
 * sin liquidar.
 */
public record CierreDia(int liquidaciones, int vendidas, int devueltas, int recaudado, int porLiquidar) {
}
