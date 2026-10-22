package com.bonoapp.model;

import java.time.LocalDate;

/**
 * Movimiento de ahorro de una vendedora. Un monto positivo es dinero que se suma a su ahorro
 * (por una liquidacion o un ahorro voluntario) y uno negativo es un retiro.
 */
public record Ahorro(int id, String vendedoraNombre, String vendedoraDocumento, int monto,
                     String concepto, LocalDate fecha) {
}
