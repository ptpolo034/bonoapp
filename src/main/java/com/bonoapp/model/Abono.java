package com.bonoapp.model;

import java.time.LocalDate;

/** Pago parcial o total que una vendedora hace a un prestamo. */
public record Abono(int id, int monto, LocalDate fecha, String nota) {
}
