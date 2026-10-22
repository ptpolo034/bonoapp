package com.bonoapp.ui;

import com.bonoapp.ui.Theme.Tono;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;

/**
 * Ventana pequena, al centro, para confirmar algo que salio bien ("Copia guardada", "Contraseña actualizada"...).
 * Reemplaza a los cuadros "Listo" genericos: lleva un icono verde, un titulo y el mensaje, y se cierra con
 * "Aceptar" o con Enter. Los errores y las advertencias siguen usando los cuadros normales.
 */
final class Avisos {

    private Avisos() {
    }

    /** Muestra la confirmacion. "origen" es cualquier componente de la ventana que la lanza (puede ser un dialogo). */
    static void exito(Component origen, String titulo, String mensaje) {
        Window propietario = origen == null ? null : SwingUtilities.getWindowAncestor(origen);
        JDialog dialogo = new JDialog(propietario, titulo, JDialog.DEFAULT_MODALITY_TYPE);

        JPanel contenido = new JPanel();
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.Y_AXIS));
        contenido.setBorder(BorderFactory.createEmptyBorder(24, 34, 10, 34));

        Insignia insignia = new Insignia("check", Tono.EXITO);
        insignia.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel etiquetaTitulo = new JLabel(titulo, JLabel.CENTER);
        etiquetaTitulo.setFont(etiquetaTitulo.getFont().deriveFont(Font.BOLD, etiquetaTitulo.getFont().getSize2D() + 3f));
        etiquetaTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // El mensaje admite saltos de linea; se escapan los caracteres especiales para que una ruta o un texto
        // con "<" no se interprete como codigo.
        String html = mensaje.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<br>");
        JLabel etiquetaMensaje = new JLabel("<html><body style='width: 340px; text-align: center'>" + html
                + "</body></html>", JLabel.CENTER);
        etiquetaMensaje.setForeground(Theme.TEXTO_SUAVE);
        etiquetaMensaje.setAlignmentX(Component.CENTER_ALIGNMENT);

        contenido.add(insignia);
        contenido.add(Box.createVerticalStrut(12));
        contenido.add(etiquetaTitulo);
        contenido.add(Box.createVerticalStrut(8));
        contenido.add(etiquetaMensaje);

        JButton aceptar = Botones.principal("Aceptar", "check");
        aceptar.addActionListener(e -> dialogo.dispose());
        JPanel pie = new JPanel();
        pie.setBorder(BorderFactory.createEmptyBorder(8, 0, 16, 0));
        pie.add(aceptar);

        dialogo.add(contenido, BorderLayout.CENTER);
        dialogo.add(pie, BorderLayout.SOUTH);
        dialogo.pack();
        dialogo.setResizable(false);
        dialogo.setLocationRelativeTo(propietario);
        dialogo.getRootPane().setDefaultButton(aceptar);
        dialogo.setVisible(true);
    }
}
