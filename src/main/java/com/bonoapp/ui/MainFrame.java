package com.bonoapp.ui;

import com.bonoapp.util.Registro;
import com.bonoapp.util.Respaldo;
import com.bonoapp.util.Version;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventana principal: menu lateral a la izquierda y contenido a la derecha. Las pantallas se crean la
 * primera vez que se abren (no todas al arrancar), para que la app arranque rapido y gaste menos
 * memoria en equipos modestos.
 */
public class MainFrame extends JFrame {

    private final CardLayout cartas = new CardLayout();
    private final JPanel contenido = new JPanel(cartas);
    private final JPanel menu = new JPanel();
    private final Map<String, JToggleButton> botones = new HashMap<>();
    private final ButtonGroup grupo = new ButtonGroup();
    private final Map<String, JPanel> creadas = new HashMap<>();
    private JToggleButton botonInicio;
    private boolean zonaInferior = false;

    public MainFrame() {
        super("BonoApp");
        setIconImages(Marca.iconos());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        // Si ya se eligio una carpeta de respaldo, se guarda la copia del dia al cerrar (y se avisa si falla). Solo se
        // dispara al cerrar con la X (no al reabrir la ventana por el cambio de tamano de letra).
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                String problema = Respaldo.alCerrar();
                if (problema != null) {
                    JOptionPane.showMessageDialog(MainFrame.this, problema, "Copia de seguridad",
                            JOptionPane.WARNING_MESSAGE);
                }
            }
        });
        // Tamano inicial: grande si la pantalla lo permite (asi Inicio cabe sin desplazar), sin pasarse del area util.
        java.awt.Rectangle util = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(1280, util.width), Math.min(820, util.height));
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        construirMenu();
        contenido.setBackground(Theme.FONDO);
        add(menu, BorderLayout.WEST);
        add(contenido, BorderLayout.CENTER);

        agregarSeccion("Inicio", "home", () -> new InicioPanel(this::irA));
        agregarSeccion("Vendedoras", "users", VendedorasPanel::new);
        agregarSeccion("Boletas", "ticket", BoletasPanel::new);
        agregarSeccion("Liquidación", "checklist", LiquidacionesPanel::new);
        agregarSeccion("Ahorros", "pig-money", AhorrosPanel::new);
        agregarSeccion("Préstamos", "cash", PrestamosPanel::new);
        agregarSeccion("Reportes", "report", ReportesPanel::new);
        agregarSeccion("Historial", "history", HistorialPanel::new);
        // Configuracion y Cuenta van separadas, abajo del todo.
        agregarSeparadorInferior();
        agregarSeccion("Configuración", "settings", ConfiguracionPanel::new);
        agregarSeccion("Cuenta", "user-circle", CuentaPanel::new);
        agregarFirma();

        // Selecciona "Inicio" al abrir
        botonInicio.doClick();
    }

    private void construirMenu() {
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setBackground(Theme.MENU_FONDO);
        menu.setBorder(BorderFactory.createEmptyBorder(22, 14, 14, 14));
        menu.setPreferredSize(new Dimension(220, 0));

        JLabel logo = new JLabel("BonoApp", Iconos.get("ticket", 26, new Color(0xA5B4FC)), JLabel.LEFT);
        logo.setIconTextGap(10);
        logo.setFont(logo.getFont().deriveFont(Font.BOLD, 20f));
        logo.setForeground(Color.WHITE);
        logo.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(logo);
        menu.add(Box.createVerticalStrut(24));
    }

    /** Version y firma del desarrollador, pequenas y en gris suave, abajo del menu: se ven en todas las pantallas. */
    private void agregarFirma() {
        menu.add(Box.createVerticalStrut(12));
        for (String texto : new String[] {"BonoApp " + Version.ACTUAL, Version.CREDITO}) {
            JLabel linea = new JLabel(texto);
            linea.setFont(linea.getFont().deriveFont(11f));
            linea.setForeground(new Color(0x9CA3AF));
            linea.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
            linea.setMaximumSize(new Dimension(Integer.MAX_VALUE, linea.getPreferredSize().height));
            linea.setAlignmentX(Component.LEFT_ALIGNMENT);
            menu.add(linea);
        }
    }

    /** Empuja lo que se agregue despues hacia abajo y deja una linea fina que lo separa del resto. */
    private void agregarSeparadorInferior() {
        menu.add(Box.createVerticalGlue());
        JPanel linea = new JPanel();
        linea.setBackground(Theme.MENU_HOVER);
        linea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        linea.setPreferredSize(new Dimension(1, 1));
        linea.setAlignmentX(Component.LEFT_ALIGNMENT);
        menu.add(linea);
        menu.add(Box.createVerticalStrut(8));
        zonaInferior = true;
    }

    /** Cambia a otra seccion del menu, como si se pulsara su boton (la usa la guia de primeros pasos). */
    private void irA(String nombre) {
        JToggleButton boton = botones.get(nombre);
        if (boton != null) {
            boton.doClick();
        }
    }

    private void agregarSeccion(String nombre, String icono, Supplier<JPanel> fabrica) {
        JToggleButton boton = new JToggleButton(nombre);
        boton.setIcon(Iconos.get(icono, 18, Theme.MENU_TEXTO));
        boton.setSelectedIcon(Iconos.get(icono, 18, Color.WHITE));
        boton.setIconTextGap(12);
        boton.setHorizontalAlignment(JToggleButton.LEFT);
        boton.setAlignmentX(Component.LEFT_ALIGNMENT);
        boton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        boton.setFocusPainted(false);
        boton.putClientProperty("FlatLaf.style",
                "arc: 12; borderWidth: 0; focusWidth: 0; margin: 8,12,8,12;"
                        + "background: " + Theme.hex(Theme.MENU_FONDO) + "; foreground: " + Theme.hex(Theme.MENU_TEXTO) + ";"
                        + "hoverBackground: " + Theme.hex(Theme.MENU_HOVER) + ";"
                        + "selectedBackground: " + Theme.hex(Theme.PRINCIPAL) + "; selectedForeground: #FFFFFF");
        boton.addActionListener(e -> {
            Registro.pantalla(nombre);
            JPanel panel = creadas.get(nombre);
            if (panel == null) {
                panel = fabrica.get();
                creadas.put(nombre, panel);
                contenido.add(panel, nombre);
            } else if (panel instanceof Actualizable actualizable) {
                // Al volver a una pantalla ya abierta se refresca; la primera vez ya se carga al crearla.
                actualizable.actualizar();
            }
            cartas.show(contenido, nombre);
        });

        botones.put(nombre, boton);
        grupo.add(boton);
        menu.add(boton);
        if (!zonaInferior || !"Cuenta".equals(nombre)) {
            menu.add(Box.createVerticalStrut(4));
        }

        if ("Inicio".equals(nombre)) {
            botonInicio = boton;
        }
    }
}
