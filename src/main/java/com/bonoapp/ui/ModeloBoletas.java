package com.bonoapp.ui;

import com.bonoapp.model.Boleta;

import javax.swing.table.AbstractTableModel;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Modelo de tabla compartido para mostrar boletas: numero, tipo, vendedora, fecha de entrega y estado. */
class ModeloBoletas extends AbstractTableModel {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] COLUMNAS = {"Número", "Tipo", "Vendedora", "Fecha de entrega", "Estado"};

    private List<Boleta> datos = new ArrayList<>();

    void setDatos(List<Boleta> datos) {
        this.datos = datos;
        fireTableDataChanged();
    }

    Boleta getFila(int fila) {
        return datos.get(fila);
    }

    @Override
    public int getRowCount() {
        return datos.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNAS.length;
    }

    @Override
    public String getColumnName(int columna) {
        return COLUMNAS[columna];
    }

    @Override
    public Object getValueAt(int fila, int columna) {
        Boleta b = datos.get(fila);
        return switch (columna) {
            case 0 -> b.getNumero() == null ? "—" : b.getNumero();
            case 1 -> b.getTipo().getEtiqueta();
            case 2 -> b.getVendedoraNombre();
            case 3 -> b.getFechaEntrega().format(FORMATO_FECHA);
            case 4 -> b.getEstado().getEtiqueta();
            default -> "";
        };
    }
}
