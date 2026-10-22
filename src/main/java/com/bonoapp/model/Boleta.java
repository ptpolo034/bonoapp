package com.bonoapp.model;

import java.time.LocalDate;

/**
 * Una boleta entregada a una vendedora: su numero de 3 cifras (puede estar vacio hasta que se asigne),
 * el tipo, quien la tiene, la fecha de entrega y su estado. Solo se lee: los cambios se hacen por BoletaDAO.
 */
public class Boleta {

    private final int id;
    private final String numero;
    private final TipoBoleta tipo;
    private final String vendedoraNombre;
    private final LocalDate fechaEntrega;
    private final EstadoBoleta estado;

    public Boleta(int id, String numero, TipoBoleta tipo, String vendedoraNombre,
                  LocalDate fechaEntrega, EstadoBoleta estado) {
        this.id = id;
        this.numero = numero;
        this.tipo = tipo;
        this.vendedoraNombre = vendedoraNombre;
        this.fechaEntrega = fechaEntrega;
        this.estado = estado;
    }

    public int getId() {
        return id;
    }

    public String getNumero() {
        return numero;
    }

    public TipoBoleta getTipo() {
        return tipo;
    }

    public String getVendedoraNombre() {
        return vendedoraNombre;
    }

    public LocalDate getFechaEntrega() {
        return fechaEntrega;
    }

    public EstadoBoleta getEstado() {
        return estado;
    }
}
