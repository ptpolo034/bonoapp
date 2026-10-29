package com.bonoapp.ui;

import com.bonoapp.db.BoletaDAO;
import com.bonoapp.model.TipoBoleta;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Cuantas boletas de cada tipo se han entregado en un dia, frente al lote de 2.000: sirve para comprobar,
 * mientras se reparten, que ya salieron todas. Se vuelve a calcular cada vez que se pide "mostrar".
 */
final class AvanceDeLotes extends JPanel {

    private static final NumberFormat MILES = NumberFormat.getIntegerInstance(new Locale("es", "CO"));

    private final Map<TipoBoleta, Bloque> bloques = new EnumMap<>(TipoBoleta.class);

    AvanceDeLotes() {
        super(new GridLayout(1, 0, 16, 0));
        setOpaque(false);
        for (TipoBoleta tipo : TipoBoleta.values()) {
            Bloque bloque = new Bloque(tipo);
            bloques.put(tipo, bloque);
            add(bloque);
        }
    }

    /** Lee de la base de datos el avance de cada tipo para ese dia. */
    void mostrar(LocalDate dia) {
        for (TipoBoleta tipo : TipoBoleta.values()) {
            bloques.get(tipo).mostrar(BoletaDAO.avanceDelLote(tipo, dia));
        }
    }

    /** Una tarjeta pequena: titulo, cuenta "1.250 de 2.000", barra y lo que falta. */
    private static final class Bloque extends JPanel {

        private final JLabel cuenta = new JLabel(" ");
        private final JLabel detalle = new JLabel(" ");
        private final Barra barra = new Barra();

        Bloque(TipoBoleta tipo) {
            super(new BorderLayout(0, 6));
            setBackground(Theme.TARJETA);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDE),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));

            JLabel nombre = new JLabel(tipo.getEtiqueta());
            nombre.setForeground(Theme.TEXTO_SUAVE);
            cuenta.setFont(cuenta.getFont().deriveFont(Font.BOLD, 16f));

            JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            fila.setOpaque(false);
            fila.add(nombre);
            fila.add(javax.swing.Box.createHorizontalStrut(10));
            fila.add(cuenta);

            detalle.setFont(detalle.getFont().deriveFont(12f));

            add(fila, BorderLayout.NORTH);
            add(barra, BorderLayout.CENTER);
            add(detalle, BorderLayout.SOUTH);
        }

        void mostrar(BoletaDAO.AvanceLote avance) {
            int meta = BoletaDAO.MAX_BOLETAS_POR_TIPO;
            boolean completo = avance.total() >= meta;
            cuenta.setText(MILES.format(avance.total()) + " de " + MILES.format(meta));
            cuenta.setForeground(completo ? Theme.EXITO : Theme.TEXTO);
            barra.fijar(avance.total() / (double) meta, completo);

            String texto;
            if (completo) {
                texto = "Completo: ya están las " + MILES.format(meta) + " boletas";
            } else if (avance.total() == 0) {
                texto = "Aún no se ha entregado ninguna";
            } else {
                texto = "Faltan " + MILES.format(meta - avance.total());
            }
            if (avance.sinNumero() > 0) {
                texto += " · " + MILES.format(avance.sinNumero()) + " sin número";
            }
            detalle.setText(texto);
            detalle.setForeground(completo ? Theme.EXITO : Theme.TEXTO_SUAVE);
        }
    }

    /** Barra de progreso delgada, dibujada a mano para que combine con el resto del diseno. */
    private static final class Barra extends JComponent {

        private double proporcion;
        private boolean completa;

        Barra() {
            setPreferredSize(new Dimension(10, 8));
        }

        void fijar(double proporcion, boolean completa) {
            this.proporcion = Math.max(0, Math.min(1, proporcion));
            this.completa = completa;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int alto = getHeight();
            g2.setColor(Theme.BORDE);
            g2.fillRoundRect(0, 0, getWidth(), alto, alto, alto);
            Color relleno = completa ? Theme.EXITO : Theme.PRINCIPAL;
            g2.setColor(relleno);
            int ancho = (int) Math.round(getWidth() * proporcion);
            if (ancho > 0) {
                g2.fillRoundRect(0, 0, Math.max(ancho, alto), alto, alto, alto);
            }
            g2.dispose();
        }
    }
}
