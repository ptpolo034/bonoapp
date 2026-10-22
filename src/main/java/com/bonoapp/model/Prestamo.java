package com.bonoapp.model;

import java.time.LocalDate;

/** Dinero prestado a una vendedora, con su concepto, la fecha del prestamo y lo que ya se ha abonado. */
public record Prestamo(int id, String vendedoraNombre, String vendedoraDocumento, int monto, int abonado,
                       String concepto, LocalDate fecha) {

    /** Lo que todavia debe la vendedora. */
    public int saldo() {
        return monto - abonado;
    }
}
