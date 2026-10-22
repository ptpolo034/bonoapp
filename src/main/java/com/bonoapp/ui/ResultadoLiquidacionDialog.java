package com.bonoapp.ui;

import com.bonoapp.db.LiquidacionDAO;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.model.Vendedora;
import com.bonoapp.ui.Theme.Tono;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Ventana pequena, al centro, que muestra el resultado de una liquidacion: cuantas boletas se vendieron y
 * se devolvieron y, sobre todo, cuanto debe entregar la vendedora. Se queda hasta que se pulsa "Aceptar",
 * porque es un dato que hay que ver y no se debe pasar por alto (por eso no es un aviso que se va solo).
 */
final class ResultadoLiquidacionDialog extends JDialog {

    private static final NumberFormat FORMATO_PESOS = NumberFormat.getNumberInstance(new Locale("es", "CO"));

    ResultadoLiquidacionDialog(Frame propietario, Vendedora vendedora, TipoBoleta tipo,
                               LiquidacionDAO.Resultado resultado) {
        super(propietario, "Liquidación registrada", true);
        setLayout(new BorderLayout());

        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(BorderFactory.createEmptyBorder(22, 32, 12, 32));

        Insignia insignia = new Insignia("check", Tono.EXITO);
        insignia.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel quien = new JLabel(vendedora.getNombre(), JLabel.CENTER);
        quien.setFont(quien.getFont().deriveFont(Font.BOLD, quien.getFont().getSize2D() + 3f));
        quien.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel que = new JLabel("Boletas " + tipo.getEtiqueta(), JLabel.CENTER);
        que.setForeground(Theme.TEXTO_SUAVE);
        que.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel etiquetaEntregar = new JLabel("Debe entregar", JLabel.CENTER);
        etiquetaEntregar.setForeground(Theme.TEXTO_SUAVE);
        etiquetaEntregar.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel entregar = new JLabel("$ " + FORMATO_PESOS.format(resultado.recaudado()), JLabel.CENTER);
        entregar.setFont(entregar.getFont().deriveFont(Font.BOLD, 34f));
        entregar.setForeground(Theme.PRINCIPAL);
        entregar.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel cifras = new JPanel(new GridLayout(1, 2, 28, 0));
        cifras.setOpaque(false);
        cifras.add(cifra("Vendidas", resultado.cantidadVendida()));
        cifras.add(cifra("Devueltas", resultado.cantidadDevuelta()));
        cifras.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel nota = new JLabel("Si te equivocaste, puedes revertirla en \"Liquidaciones hechas\".", JLabel.CENTER);
        nota.setForeground(Theme.TEXTO_SUAVE);
        nota.setFont(nota.getFont().deriveFont(nota.getFont().getSize2D() - 1.5f));
        nota.setAlignmentX(Component.CENTER_ALIGNMENT);

        contenido.add(insignia);
        contenido.add(Box.createVerticalStrut(10));
        contenido.add(quien);
        contenido.add(que);
        contenido.add(Box.createVerticalStrut(16));
        contenido.add(etiquetaEntregar);
        contenido.add(entregar);
        contenido.add(Box.createVerticalStrut(14));
        contenido.add(cifras);
        contenido.add(Box.createVerticalStrut(16));
        contenido.add(nota);

        JButton aceptar = Botones.principal("Aceptar", "check");
        aceptar.addActionListener(e -> dispose());
        JPanel pie = new JPanel();
        pie.setBorder(BorderFactory.createEmptyBorder(0, 0, 14, 0));
        pie.add(aceptar);

        add(contenido, BorderLayout.CENTER);
        add(pie, BorderLayout.SOUTH);
        pack();
        setResizable(false);
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(aceptar);
    }

    private static JPanel cifra(String titulo, int valor) {
        JLabel etiqueta = new JLabel(titulo, JLabel.CENTER);
        etiqueta.setForeground(Theme.TEXTO_SUAVE);
        JLabel numero = new JLabel(String.valueOf(valor), JLabel.CENTER);
        numero.setFont(numero.getFont().deriveFont(Font.BOLD, numero.getFont().getSize2D() + 6f));
        JPanel p = new JPanel(new GridLayout(2, 1, 0, 2));
        p.setOpaque(false);
        p.add(numero);
        p.add(etiqueta);
        return p;
    }
}
