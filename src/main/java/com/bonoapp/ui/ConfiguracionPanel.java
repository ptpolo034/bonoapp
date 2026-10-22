package com.bonoapp.ui;

import com.bonoapp.Main;
import com.bonoapp.db.Database;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.util.Preferencias;
import com.bonoapp.util.Respaldo;
import com.bonoapp.util.Version;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

/** Pantalla de configuracion: tamano de letra, copia de seguridad y valores de las boletas. */
public class ConfiguracionPanel extends JPanel {

    private static final Map<String, Integer> OPCIONES_TAMANO_FUENTE = new LinkedHashMap<>();

    static {
        OPCIONES_TAMANO_FUENTE.put("Normal (13)", 13);
        OPCIONES_TAMANO_FUENTE.put("Grande (15)", 15);
        OPCIONES_TAMANO_FUENTE.put("Más grande (17)", 17);
        OPCIONES_TAMANO_FUENTE.put("Muy grande (20)", 20);
    }

    private final JComboBox<String> comboTamanoFuente =
            new JComboBox<>(OPCIONES_TAMANO_FUENTE.keySet().toArray(new String[0]));

    private final JComboBox<TipoBoleta> comboTipoBoleta = new JComboBox<>(TipoBoleta.values());
    private final JTextField campoPrecioBoleta = new JTextField(10);
    private final JLabel etiquetaUbicacion = new JLabel(" ");

    public ConfiguracionPanel() {
        setLayout(new BorderLayout(0, 24));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Configuración");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        add(titulo, BorderLayout.NORTH);

        JPanel columnaIzquierda = new JPanel();
        columnaIzquierda.setOpaque(false);
        columnaIzquierda.setLayout(new BoxLayout(columnaIzquierda, BoxLayout.Y_AXIS));
        columnaIzquierda.setAlignmentY(Component.TOP_ALIGNMENT);
        columnaIzquierda.add(tarjetaTamanoFuente());
        columnaIzquierda.add(Box.createVerticalStrut(20));
        columnaIzquierda.add(tarjetaRespaldo());

        JPanel columnaDerecha = new JPanel();
        columnaDerecha.setOpaque(false);
        columnaDerecha.setLayout(new BoxLayout(columnaDerecha, BoxLayout.Y_AXIS));
        columnaDerecha.setAlignmentY(Component.TOP_ALIGNMENT);
        columnaDerecha.add(tarjetaValoresBoleta());
        columnaDerecha.add(Box.createVerticalStrut(20));
        columnaDerecha.add(tarjetaSoporte());

        JPanel contenido = new JPanel();
        contenido.setOpaque(false);
        contenido.setLayout(new BoxLayout(contenido, BoxLayout.X_AXIS));
        contenido.add(columnaIzquierda);
        contenido.add(Box.createHorizontalStrut(20));
        contenido.add(columnaDerecha);

        JScrollPane scroll = new JScrollPane(new PanelDesplazable(contenido));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    /** Tamano de letra de toda la app. */
    private JPanel tarjetaTamanoFuente() {
        JPanel tarjeta = tarjetaBase();
        tarjeta.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Tamaño de letra");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        seleccionarTamanoFuenteActual();

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Formularios.agregarFila(form, gbc, 0, "Tamaño", comboTamanoFuente);

        JButton botonGuardar = new JButton("Guardar y aplicar");
        Theme.estiloPrincipal(botonGuardar);
        botonGuardar.addActionListener(e -> guardarTamanoFuente());
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBoton.setOpaque(false);
        filaBoton.add(botonGuardar);

        JPanel cuerpo = new JPanel(new BorderLayout(0, 8));
        cuerpo.setOpaque(false);
        cuerpo.add(form, BorderLayout.NORTH);
        cuerpo.add(filaBoton, BorderLayout.CENTER);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(cuerpo, BorderLayout.CENTER);
        return tarjeta;
    }

    private JPanel tarjetaRespaldo() {
        JPanel tarjeta = tarjetaBase();
        tarjeta.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Copia de seguridad");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        JLabel descripcion = new JLabel("<html><body style='width: 330px'>Las copias se guardan solas en la carpeta que "
                + "elegiste: al abrir y al cerrar BonoApp, un archivo por día, conservando los últimos "
                + Respaldo.DIAS_A_CONSERVAR + " días. Cada copia trae todos tus datos hasta ese momento.</body></html>");
        descripcion.setForeground(Theme.TEXTO_SUAVE);

        actualizarEtiquetaUbicacion();
        etiquetaUbicacion.setForeground(Theme.TEXTO_SUAVE);

        JPanel textos = new JPanel(new BorderLayout(0, 8));
        textos.setOpaque(false);
        textos.add(descripcion, BorderLayout.NORTH);
        textos.add(etiquetaUbicacion, BorderLayout.SOUTH);

        JButton botonExportar = new JButton("Exportar copia");
        botonExportar.addActionListener(e -> exportarCopia());
        JButton botonUbicacion = new JButton("Cambiar ubicación");
        botonUbicacion.addActionListener(e -> {
            if (ConfigurarRespaldoDialog.abrir(ventanaActual(), false)) {
                actualizarEtiquetaUbicacion();
            }
        });
        JButton botonRestaurar = new JButton("Restaurar copia");
        botonRestaurar.addActionListener(e -> restaurarCopia());

        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila.setOpaque(false);
        fila.add(botonExportar);
        fila.add(botonUbicacion);
        fila.add(botonRestaurar);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(textos, BorderLayout.CENTER);
        tarjeta.add(fila, BorderLayout.SOUTH);
        return tarjeta;
    }

    private Frame ventanaActual() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void actualizarEtiquetaUbicacion() {
        String carpeta = Preferencias.getCarpetaRespaldo();
        etiquetaUbicacion.setToolTipText(carpeta);
        if (carpeta == null) {
            etiquetaUbicacion.setText("Ubicación de las copias: aún no elegida");
            return;
        }
        // Una ruta larga no debe ensanchar la tarjeta: se acorta por el medio (completa en el tooltip).
        String visible = carpeta.length() > 48
                ? carpeta.substring(0, 20) + "…" + carpeta.substring(carpeta.length() - 25)
                : carpeta;
        java.time.LocalDateTime ultima = Respaldo.ultimaCopia();
        etiquetaUbicacion.setText("<html>Ubicación de las copias: " + visible + "<br>Última copia automática: "
                + (ultima == null ? "todavía ninguna" : ultima.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
                + "</html>");
    }

    private void exportarCopia() {
        Path carpeta = Respaldo.carpetaDisponible();
        if (carpeta == null) {
            if (Preferencias.getCarpetaRespaldo() != null) {
                JOptionPane.showMessageDialog(this,
                        "La carpeta de las copias no está disponible:\n" + Preferencias.getCarpetaRespaldo()
                                + "\n\nConecta la USB o el disco, o usa \"Cambiar ubicación\".",
                        "Ubicación no disponible", JOptionPane.WARNING_MESSAGE);
                return;
            }
            // Todavia no hay carpeta: se pide con la misma ventana de la primera vez.
            if (!ConfigurarRespaldoDialog.abrir(ventanaActual(), true)) {
                return;
            }
            actualizarEtiquetaUbicacion();
            carpeta = Respaldo.carpetaDisponible();
            if (carpeta == null) {
                return;
            }
        }
        try {
            Path destino = Respaldo.exportarACarpeta(carpeta);
            Avisos.exito(this, "Copia guardada", "La copia quedÃ³ en:\n" + destino);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo exportar la copia: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restaurarCopia() {
        JFileChooser selector = new JFileChooser();
        selector.setDialogTitle("Restaurar copia de seguridad");
        selector.setFileFilter(new FileNameExtensionFilter("Base de datos de BonoApp (*.db)", "db"));

        if (selector.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path origen = selector.getSelectedFile().toPath();

        int confirmacion = JOptionPane.showConfirmDialog(this,
                "Esto reemplaza TODOS los datos actuales por los de la copia seleccionada.\n"
                        + "Se guardará un respaldo de lo que hay ahora, por seguridad.\n\n¿Continuar?",
                "Confirmar restauración", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmacion != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Path respaldoAntesDeRestaurar = Database.archivo().resolveSibling("bonoapp_antes_de_restaurar.db");
            Files.copy(Database.archivo(), respaldoAntesDeRestaurar, StandardCopyOption.REPLACE_EXISTING);
            Files.copy(origen, Database.archivo(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No se pudo restaurar la copia: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Avisos.exito(this, "Copia restaurada",
                "BonoApp se va a cerrar; vuelve a abrirlo para ver los datos restaurados.");
        System.exit(0);
    }

    private JPanel tarjetaValoresBoleta() {
        JPanel tarjeta = tarjetaBase();
        tarjeta.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Valores de la boleta");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        comboTipoBoleta.addActionListener(e -> cargarValoresBoleta());
        cargarValoresBoleta();

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Tipo de boleta", comboTipoBoleta);
        Formularios.agregarFila(form, gbc, 1, "Precio de la boleta", campoPrecioBoleta);

        JButton botonGuardar = new JButton("Guardar");
        Theme.estiloPrincipal(botonGuardar);
        botonGuardar.addActionListener(e -> guardarValoresBoleta());
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBoton.setOpaque(false);
        filaBoton.add(botonGuardar);

        // El formulario y el boton van juntos arriba, para que no queden separados por un hueco
        // cuando la tarjeta se estira a la altura de la columna vecina.
        JPanel cuerpo = new JPanel(new BorderLayout(0, 8));
        cuerpo.setOpaque(false);
        cuerpo.add(form, BorderLayout.NORTH);
        cuerpo.add(filaBoton, BorderLayout.CENTER);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(cuerpo, BorderLayout.CENTER);
        return tarjeta;
    }

    private void cargarValoresBoleta() {
        TipoBoleta tipo = (TipoBoleta) comboTipoBoleta.getSelectedItem();
        campoPrecioBoleta.setText(String.valueOf(Preferencias.getPrecio(tipo)));
    }

    private void guardarValoresBoleta() {
        TipoBoleta tipo = (TipoBoleta) comboTipoBoleta.getSelectedItem();
        int precio;
        try {
            precio = Integer.parseInt(campoPrecioBoleta.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "El precio debe ser un numero",
                    "Datos invalidos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (precio <= 0) {
            JOptionPane.showMessageDialog(this, "El precio debe ser mayor a cero",
                    "Datos invalidos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Preferencias.guardarPrecio(tipo, precio);
        Avisos.exito(this, "Valores actualizados", "Los valores de " + tipo.getEtiqueta() + " quedaron guardados.");
    }

    /** Soporte: la version de la app y acceso al registro de errores, para saber que version se esta usando y enviar el archivo si algo falla. */
    private JPanel tarjetaSoporte() {
        JPanel tarjeta = tarjetaBase();
        tarjeta.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Soporte");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        JLabel version = new JLabel("<html><body style='width: 330px'>Versión de BonoApp: <b>" + Version.ACTUAL + "</b><br>"
                + "<span style='color: gray'>Si algo falla, el error queda anotado en un archivo (errores.log). "
                + "Envíalo para encontrar la causa.</span></body></html>");

        JButton botonRegistro = new JButton("Abrir carpeta del registro");
        botonRegistro.addActionListener(e -> AvisoDeError.abrirCarpetaDelRegistro());
        JPanel fila = new JPanel(new FlowLayout(FlowLayout.LEFT));
        fila.setOpaque(false);
        fila.add(botonRegistro);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(version, BorderLayout.CENTER);
        tarjeta.add(fila, BorderLayout.SOUTH);
        return tarjeta;
    }

    private JPanel tarjetaBase() {
        return Theme.crearTarjeta();
    }

    private void seleccionarTamanoFuenteActual() {
        int actual = Preferencias.getTamanoFuente();
        for (Map.Entry<String, Integer> entrada : OPCIONES_TAMANO_FUENTE.entrySet()) {
            if (entrada.getValue() == actual) {
                comboTamanoFuente.setSelectedItem(entrada.getKey());
                return;
            }
        }
    }

    /** Guarda el tamano de letra y reabre la ventana para que se vea (los componentes viejos no se actualizan solos). */
    private void guardarTamanoFuente() {
        String seleccion = (String) comboTamanoFuente.getSelectedItem();
        Preferencias.guardarTamanoFuente(OPCIONES_TAMANO_FUENTE.get(seleccion));

        Frame ventanaActual = (Frame) SwingUtilities.getWindowAncestor(this);
        SwingUtilities.invokeLater(() -> {
            ventanaActual.dispose();
            Main.aplicarTema();
            new MainFrame().setVisible(true);
        });
    }
}
