package com.bonoapp.model;

/**
 * Una vendedora: la persona a la que se entregan boletas, se liquida y se le pueden hacer prestamos.
 * Una vendedora inactiva deja de aparecer al registrar entregas, pero conserva todo su historial.
 */
public class Vendedora {

    private int id;
    private String nombre;
    private String documento;
    private String telefono;
    private boolean activa;

    public Vendedora() {
    }

    public Vendedora(int id, String nombre, String documento, String telefono, boolean activa) {
        this.id = id;
        this.nombre = nombre;
        this.documento = documento;
        this.telefono = telefono;
        this.activa = activa;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
