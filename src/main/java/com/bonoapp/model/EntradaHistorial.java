package com.bonoapp.model;

import java.time.LocalDateTime;

/** Una linea del historial de cambios: quien hizo que y cuando. */
public record EntradaHistorial(LocalDateTime fechaHora, String usuario, String accion, String detalle) {
}
