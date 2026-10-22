package com.bonoapp.ui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** DocumentListener para campos de busqueda: llama la misma accion sin importar el tipo de cambio. */
class BuscadorListener implements DocumentListener {

    private final Runnable filtrar;

    BuscadorListener(Runnable filtrar) {
        this.filtrar = filtrar;
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        filtrar.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        filtrar.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        filtrar.run();
    }
}
