package com.bonoapp.ui;

import com.bonoapp.model.Vendedora;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Ayudas compartidas para armar formularios con GridBagLayout y para filtrar vendedoras en un combo. */
final class Formularios {

    private Formularios() {
    }

    /** Lee un monto en pesos escrito a mano; acepta "50000", "50.000", "$ 50,000". Lanza NumberFormatException si no es un numero. */
    static int leerPesos(String texto) {
        return Integer.parseInt(texto.replaceAll("[\\s$.,]", ""));
    }

    /** Convierte la fecha de un selector (puede estar vacia) a LocalDate; null si no hay fecha. */
    static LocalDate aLocalDate(Date fecha) {
        return fecha == null ? null : fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    /** Agrega una fila "etiqueta: campo" a un formulario en GridBagLayout. */
    static void agregarFila(JPanel form, GridBagConstraints gbc, int fila, String etiqueta, Component campo) {
        gbc.gridx = 0;
        gbc.gridy = fila;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        form.add(new JLabel(etiqueta), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        form.add(campo, gbc);
    }

    /**
     * Filtra "todas" por nombre o documento (contiene, sin distinguir mayusculas/minusculas) y
     * reconstruye el modelo del combo, conservando la seleccion actual si sigue entre los
     * resultados filtrados.
     */
    static void filtrarVendedoras(JComboBox<Vendedora> combo, List<Vendedora> todas, String texto) {
        Vendedora seleccionActual = (Vendedora) combo.getSelectedItem();
        String buscado = texto.trim().toLowerCase(Locale.ROOT);

        List<Vendedora> filtradas = new ArrayList<>();
        for (Vendedora v : todas) {
            String nombre = v.getNombre() == null ? "" : v.getNombre().toLowerCase(Locale.ROOT);
            String documento = v.getDocumento() == null ? "" : v.getDocumento().toLowerCase(Locale.ROOT);
            if (buscado.isEmpty() || nombre.contains(buscado) || documento.contains(buscado)) {
                filtradas.add(v);
            }
        }

        combo.setModel(new DefaultComboBoxModel<>(filtradas.toArray(new Vendedora[0])));
        if (filtradas.contains(seleccionActual)) {
            combo.setSelectedItem(seleccionActual);
        }
    }
}
