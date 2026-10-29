package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.db.VendedoraDAO;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.util.Preferencias;
import com.bonoapp.util.Respaldo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Guia de "primeros pasos" de Inicio: mientras no se haya registrado ninguna boleta, explica por donde empezar.
 * Cada paso muestra si ya esta hecho y tiene un boton que lleva a la pantalla donde se hace. Desaparece sola
 * al registrar la primera entrega.
 */
final class GuiaPrimerosPasos extends JPanel {

    private static final NumberFormat PESOS = NumberFormat.getIntegerInstance(new Locale("es", "CO"));

    private final Consumer<String> irASeccion;
    private final JPanel pasos = new JPanel();

    GuiaPrimerosPasos(Consumer<String> irASeccion) {
        super(new BorderLayout());
        this.irASeccion = irASeccion;
        setOpaque(false);

        JPanel tarjeta = Theme.crearTarjeta();
        tarjeta.setLayout(new BorderLayout(0, 16));
        tarjeta.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel titulo = new JLabel("Bienvenida: para empezar");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 20f));
        JLabel ayuda = new JLabel("<html><body style='width:640px'>Sigue estos pasos en orden. Cada uno tiene un botón que te "
                + "lleva a la pantalla donde se hace. Esta guía desaparece sola cuando registres la primera entrega.</body></html>");
        ayuda.setForeground(Theme.TEXTO_SUAVE);
        JPanel encabezado = new JPanel(new BorderLayout(0, 6));
        encabezado.setOpaque(false);
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(ayuda, BorderLayout.CENTER);

        pasos.setOpaque(false);
        pasos.setLayout(new BoxLayout(pasos, BoxLayout.Y_AXIS));

        tarjeta.add(encabezado, BorderLayout.NORTH);
        tarjeta.add(pasos, BorderLayout.CENTER);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setOpaque(false);
        contenedor.add(tarjeta, BorderLayout.NORTH);
        add(contenedor, BorderLayout.CENTER);
        setPreferredSize(new Dimension(760, 10));
        refrescar();
    }

    /** Es la primera vez: todavia no se ha registrado ninguna boleta, en ningun dia. */
    static boolean hayQueMostrar() {
        return !BoletaDAO.hayBoletas();
    }

    /** Vuelve a calcular que pasos ya estan hechos. */
    void refrescar() {
        boolean conCopias = Respaldo.carpetaConfigurada() != null;
        boolean conVendedoras = !VendedoraDAO.listar().isEmpty();

        pasos.removeAll();
        pasos.add(paso(1, conCopias, "Elige dónde se guardan las copias de seguridad",
                "Mejor en el otro disco del computador: así, si el principal falla, los datos siguen a salvo.",
                "Ir a Configuración", "Configuración"));
        pasos.add(paso(2, false, "Revisa el precio de las boletas",
                "Ahora: Diaria $" + PESOS.format(Preferencias.getPrecio(TipoBoleta.DIARIA))
                        + " · Premio mayor $" + PESOS.format(Preferencias.getPrecio(TipoBoleta.PREMIO_MAYOR))
                        + ". Si no son esos, cámbialos en Configuración.",
                "Ir a Configuración", "Configuración"));
        pasos.add(paso(3, conVendedoras, "Crea las vendedoras",
                "Con su nombre y documento. Si alguna siempre vende los mismos números, anótalos en «Tratos».",
                "Ir a Vendedoras", "Vendedoras"));
        pasos.add(paso(4, false, "Registra la primera entrega de boletas",
                "Elige la vendedora, el tipo (Diaria o Premio mayor) y la cantidad. Desde ahí se lleva todo el día.",
                "Ir a Boletas", "Boletas"));
        pasos.revalidate();
        pasos.repaint();
    }

    private JPanel paso(int numero, boolean hecho, String texto, String detalle, String boton, String seccion) {
        JLabel titulo = new JLabel(texto);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 14f));
        titulo.setForeground(hecho ? Theme.TEXTO_SUAVE : Theme.TEXTO);
        // Un poco de margen a la derecha: con la letra en negrita el ultimo caracter se recortaba.
        titulo.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        JLabel descripcion = new JLabel("<html><body style='width:430px'>" + detalle + "</body></html>");
        descripcion.setForeground(Theme.TEXTO_SUAVE);
        descripcion.setFont(descripcion.getFont().deriveFont(12f));

        JPanel textos = new JPanel();
        textos.setOpaque(false);
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        titulo.setAlignmentX(Component.LEFT_ALIGNMENT);
        descripcion.setAlignmentX(Component.LEFT_ALIGNMENT);
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(2));
        textos.add(descripcion);

        JButton ir = new JButton(boton);
        if (!hecho) {
            Theme.estiloPrincipal(ir);
        }
        ir.addActionListener(e -> irASeccion.accept(seccion));
        JPanel derecha = new JPanel(new BorderLayout());
        derecha.setOpaque(false);
        derecha.add(ir, BorderLayout.NORTH);

        JPanel fila = new JPanel(new BorderLayout(16, 0));
        fila.setOpaque(false);
        fila.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        JPanel izquierda = new JPanel(new BorderLayout());
        izquierda.setOpaque(false);
        izquierda.add(new Circulo(numero, hecho), BorderLayout.NORTH);
        fila.add(izquierda, BorderLayout.WEST);
        fila.add(textos, BorderLayout.CENTER);
        fila.add(derecha, BorderLayout.EAST);
        fila.setAlignmentX(Component.LEFT_ALIGNMENT);
        fila.setMaximumSize(new Dimension(Integer.MAX_VALUE, fila.getPreferredSize().height + 20));
        return fila;
    }

    /** El numero del paso dentro de un circulo; si ya esta hecho, se pinta verde con una marca. */
    private static final class Circulo extends JComponent {

        private final int numero;
        private final boolean hecho;

        Circulo(int numero, boolean hecho) {
            this.numero = numero;
            this.hecho = hecho;
            setPreferredSize(new Dimension(32, 32));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int d = Math.min(getWidth(), getHeight());
            g2.setColor(hecho ? Theme.EXITO : Theme.PRINCIPAL_SUAVE);
            g2.fillOval(0, 0, d, d);
            if (hecho) {
                g2.setColor(Color.WHITE);
                g2.setStroke(new java.awt.BasicStroke(2.4f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                Path2D marca = new Path2D.Double();
                marca.moveTo(d * 0.30, d * 0.52);
                marca.lineTo(d * 0.44, d * 0.66);
                marca.lineTo(d * 0.72, d * 0.36);
                g2.draw(marca);
            } else {
                g2.setColor(Theme.PRINCIPAL);
                g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
                String texto = String.valueOf(numero);
                int ancho = g2.getFontMetrics().stringWidth(texto);
                int y = (d - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
                g2.drawString(texto, (d - ancho) / 2, y);
            }
            g2.dispose();
        }
    }
}
