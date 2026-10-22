package com.bonoapp.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Font;

/** Seccion "Liquidación": hacer una liquidación nueva y consultar (o revertir) las ya hechas. */
public class LiquidacionesPanel extends JPanel implements Actualizable {

    private final LiquidacionPanel nueva = new LiquidacionPanel();
    private final HistorialLiquidacionesPanel hechas = new HistorialLiquidacionesPanel();

    public LiquidacionesPanel() {
        setLayout(new BorderLayout(0, 8));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Liquidación de boletas");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        add(titulo, BorderLayout.NORTH);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Nueva liquidación", nueva);
        pestanas.addTab("Liquidaciones hechas", hechas);
        // Al cambiar de pestana se vuelve a leer: asi lo recien liquidado aparece en "hechas" y lo revertido en "nueva".
        pestanas.addChangeListener(e -> actualizar());
        add(pestanas, BorderLayout.CENTER);
    }

    @Override
    public void actualizar() {
        nueva.actualizar();
        hechas.actualizar();
    }
}
