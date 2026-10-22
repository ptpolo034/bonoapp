package com.bonoapp.model;

import java.time.LocalDate;

/** El numero que salio en un sorteo de un tipo de boleta en una fecha. */
public record NumeroGanador(int id, TipoBoleta tipo, LocalDate fecha, String numero) {
}
