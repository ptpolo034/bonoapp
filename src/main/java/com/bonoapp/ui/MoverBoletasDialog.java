package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.db.VendedoraDAO;
import com.bonoapp.model.Vendedora;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

/**
 * Pasa boletas a otra vendedora. Al moverse, la vendedora anterior deja de tenerlas (cada boleta es
 * una sola fila en la base de datos). Solo se mueven las boletas pendientes de liquidar.
 */
public class MoverBoletasDialog extends JDialog {

    private final List<Integer> ids;
    private final List<Vendedora> todasActivas;
    private final JTextField campoBuscarVendedora = new JTextField(20);
    private final JComboBox<Vendedora> comboVendedora = new JComboBox<>();

    private boolean confirmado = false;

    public MoverBoletasDialog(Frame propietario, List<Integer> ids) {
        super(propietario, "Mover boletas", true);
        this.ids = ids;
        setLayout(new BorderLayout());

        todasActivas = VendedoraDAO.listarActivas();
        filtrarVendedoras();
        campoBuscarVendedora.getDocument().addDocumentListener(new BuscadorListener(this::filtrarVendedoras));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Boletas a mover", new JLabel(String.valueOf(ids.size())));
        Formularios.agregarFila(form, gbc, 1, "Buscar vendedora (nombre o documento)", campoBuscarVendedora);
        Formularios.agregarFila(form, gbc, 2, "Pasar a", comboVendedora);

        JButton botonMover = new JButton("Mover");
        Theme.estiloPrincipal(botonMover);
        JButton botonCancelar = new JButton("Cancelar");
        botonMover.addActionListener(e -> mover());
        botonCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.add(botonCancelar);
        botones.add(botonMover);

        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonMover);
    }

    private void filtrarVendedoras() {
        Formularios.filtrarVendedoras(comboVendedora, todasActivas, campoBuscarVendedora.getText());
    }

    private void mover() {
        Vendedora destino = (Vendedora) comboVendedora.getSelectedItem();
        if (destino == null) {
            JOptionPane.showMessageDialog(this,
                    todasActivas.isEmpty() ? "No hay vendedoras activas" : "Selecciona la vendedora de destino",
                    "Sin vendedora", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int movidas;
        try {
            movidas = BoletaDAO.mover(ids, destino.getId());
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo mover: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (movidas < ids.size()) {
            JOptionPane.showMessageDialog(this,
                    "Se movieron " + movidas + " de " + ids.size() + " boleta(s).\n"
                            + "Las demás ya estaban liquidadas o ya eran de " + destino.getNombre() + ".",
                    "Movimiento parcial", JOptionPane.INFORMATION_MESSAGE);
        }
        confirmado = movidas > 0;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}
