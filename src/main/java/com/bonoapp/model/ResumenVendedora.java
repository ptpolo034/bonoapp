package com.bonoapp.model;

/** Fila del reporte: lo que paso con una vendedora dentro de un rango de fechas. */
public record ResumenVendedora(String nombre, String documento, int entregadas, int vendidas, int devueltas,
                               int sinLiquidar, int recaudado, int ahorro, int prestado, int abonos) {

    /** True si no hubo ninguna actividad en el periodo. */
    public boolean sinActividad() {
        return entregadas == 0 && vendidas == 0 && devueltas == 0 && ahorro == 0 && prestado == 0 && abonos == 0;
    }
}
