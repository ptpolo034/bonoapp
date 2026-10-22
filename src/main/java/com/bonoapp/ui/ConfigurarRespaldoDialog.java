package com.bonoapp.ui;

import com.bonoapp.util.Preferencias;
import com.bonoapp.util.Respaldo;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ventana para elegir la carpeta de las copias de seguridad automaticas. Sale sola la primera vez que se abre
 * la app (y cada vez que se abre mientras no se haya elegido carpeta), y tambien desde Configuracion para
 * cambiarla. Sugiere una carpeta en otro disco y avisa si se elige el mismo disco de los datos.
 */
public final class ConfigurarRespaldoDialog extends JDialog {

    private final JTextField campoRuta = new JTextField(34);
    private boolean guardado = false;

    private ConfigurarRespaldoDialog(Frame propietario, boolean primeraVez) {
        super(propietario, "Copias de seguridad", true);
        setLayout(new BorderLayout());

        Path actual = Respaldo.carpetaConfigurada();
        Path sugerida = actual != null ? actual : Respaldo.sugerirCarpeta();
        campoRuta.setText(sugerida == null ? "" : sugerida.toString());

        JLabel titulo = new JLabel(primeraVez ? "¿Dónde guardamos las copias de seguridad?" : "Carpeta de las copias");
        titulo.setFont(titulo.getFont().deriveFont(java.awt.Font.BOLD, titulo.getFont().getSize2D() + 3f));
        JLabel explicacion = new JLabel("<html><body style='width: 430px'>BonoApp guardará una copia de todos tus datos "
                + "<b>automáticamente</b>: al abrir y al cerrar el programa, un archivo por día, y conserva los últimos "
                + Respaldo.DIAS_A_CONSERVAR + " días. Lo mejor es una carpeta en <b>otro disco</b>, para que si un "
                + "disco falla no pierdas los datos y la copia a la vez.</body></html>");
        explicacion.setForeground(Theme.TEXTO_SUAVE);

        JButton botonElegir = Botones.secundario("Elegir carpeta…", "plus");
        botonElegir.addActionListener(e -> elegirCarpeta());
        JPanel filaRuta = new JPanel(new BorderLayout(8, 0));
        filaRuta.setOpaque(false);
        filaRuta.add(campoRuta, BorderLayout.CENTER);
        filaRuta.add(botonElegir, BorderLayout.EAST);

        JLabel sugerencia = new JLabel(sugerida != null && actual == null
                ? "Sugerida: una carpeta en otro disco de este equipo."
                : " ");
        sugerencia.setForeground(Theme.TEXTO_SUAVE);

        JPanel centro = new JPanel(new BorderLayout(0, 12));
        centro.setBorder(BorderFactory.createEmptyBorder(22, 26, 8, 26));
        centro.add(titulo, BorderLayout.NORTH);
        JPanel medio = new JPanel(new BorderLayout(0, 12));
        medio.setOpaque(false);
        medio.add(explicacion, BorderLayout.NORTH);
        medio.add(filaRuta, BorderLayout.CENTER);
        medio.add(sugerencia, BorderLayout.SOUTH);
        centro.add(medio, BorderLayout.CENTER);

        JButton botonGuardar = Botones.principal("Guardar y hacer la primera copia", "check");
        botonGuardar.addActionListener(e -> guardar());
        JButton botonCancelar = new JButton(primeraVez ? "Ahora no" : "Cancelar");
        botonCancelar.addActionListener(e -> dispose());
        JPanel botones = new JPanel();
        botones.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        botones.add(botonCancelar);
        botones.add(botonGuardar);

        add(centro, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);
        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonGuardar);
    }

    /** Abre la ventana y devuelve true si se guardó una carpeta nueva. */
    public static boolean abrir(Frame propietario, boolean primeraVez) {
        ConfigurarRespaldoDialog dialogo = new ConfigurarRespaldoDialog(propietario, primeraVez);
        dialogo.setVisible(true);
        return dialogo.guardado;
    }

    /**
     * Se llama una vez al abrir la app: si todavia no hay carpeta de copias la pide, y si la hay, hace la copia
     * de hoy (si no existe) y avisa si algo fallo, para que nadie crea que tiene copias cuando no las tiene.
     */
    public static void alAbrirLaApp(Frame ventana) {
        if (Respaldo.carpetaConfigurada() == null) {
            abrir(ventana, true);
            return;
        }
        String problema = Respaldo.alAbrir();
        if (problema != null) {
            int opcion = JOptionPane.showOptionDialog(ventana, problema, "Copia de seguridad",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[]{"Cambiar carpeta", "Más tarde"}, "Más tarde");
            if (opcion == 0) {
                abrir(ventana, false);
            }
        }
    }

    private void elegirCarpeta() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Elige la carpeta para las copias de seguridad");
        selector.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        String texto = campoRuta.getText().trim();
        if (!texto.isEmpty()) {
            Path ruta = Path.of(texto);
            // Si la carpeta todavia no existe, se abre en la mas cercana que si exista.
            while (ruta != null && !Files.isDirectory(ruta)) {
                ruta = ruta.getParent();
            }
            if (ruta != null) {
                selector.setCurrentDirectory(new File(ruta.toString()));
            }
        }
        if (selector.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            campoRuta.setText(selector.getSelectedFile().getPath());
        }
    }

    private void guardar() {
        String texto = campoRuta.getText().trim();
        if (texto.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Elige la carpeta donde se guardarán las copias.",
                    "Falta la carpeta", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Path carpeta;
        try {
            carpeta = Path.of(texto);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "Esa ruta no es válida.", "Carpeta no válida",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (Respaldo.mismoDisco(carpeta)) {
            int opcion = JOptionPane.showOptionDialog(this,
                    "Esta carpeta está en el mismo disco que los datos de BonoApp.\n"
                            + "Si ese disco se daña, perderías los datos y la copia a la vez.\n\n"
                            + "¿Quieres usarla de todos modos?",
                    "Mismo disco", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[]{"Elegir otra carpeta", "Usarla de todos modos"}, "Elegir otra carpeta");
            if (opcion != 1) {
                return;
            }
        }

        // Se hace la primera copia ahora mismo: asi se comprueba de verdad que se puede escribir ahi.
        Path guardada;
        try {
            Files.createDirectories(carpeta);
            guardada = Respaldo.copiaDelDia(carpeta);
        } catch (IOException | RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo guardar una copia en esa carpeta:\n" + e.getMessage()
                    + "\n\nElige otra carpeta o revisa que el disco esté disponible.",
                    "No se pudo guardar", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Preferencias.guardarCarpetaRespaldo(carpeta.toString());
        guardado = true;
        Avisos.exito(this, "Copias automáticas activadas", "Se guardó la primera copia en:\n" + guardada
                + "\n\nDesde ahora se guarda sola, al abrir y al cerrar BonoApp.");
        dispose();
    }
}
