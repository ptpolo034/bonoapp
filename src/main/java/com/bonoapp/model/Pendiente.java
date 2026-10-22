package com.bonoapp.model;

import java.time.LocalDate;

/** Lo que una vendedora tiene pendiente: boletas por liquidar (y desde cuando) y lo que debe de prestamos. */
public record Pendiente(String vendedoraNombre, int boletasSinLiquidar, LocalDate entregadasDesde, int debe) {
}
