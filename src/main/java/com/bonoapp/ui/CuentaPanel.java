package com.bonoapp.ui;

import com.bonoapp.db.UsuarioDAO;
import com.bonoapp.model.TipoRecuperacion;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

/** Pantalla de cuenta: cambiar el usuario y la contrasena de acceso a la aplicacion. */
public class CuentaPanel extends JPanel {

    private final JLabel etiquetaUsuarioActual = new JLabel();
    private final JTextField campoNuevoUsuario = new JTextField(18);
    private final JPasswordField campoContrasenaParaUsuario = new JPasswordField(18);

    private final JPasswordField campoActual = new JPasswordField(18);
    private final JPasswordField campoNueva = new JPasswordField(18);
    private final JPasswordField campoConfirmar = new JPasswordField(18);

    private final JLabel etiquetaMetodoRecuperacion = new JLabel();

    public CuentaPanel() {
        setLayout(new BorderLayout(0, 24));
        setBackground(Theme.FONDO);
        setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));

        JLabel titulo = new JLabel("Cuenta");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 26f));
        add(titulo, BorderLayout.NORTH);

        JPanel columnaIzquierda = new JPanel();
        columnaIzquierda.setOpaque(false);
        columnaIzquierda.setLayout(new BoxLayout(columnaIzquierda, BoxLayout.Y_AXIS));
        columnaIzquierda.setAlignmentY(Component.TOP_ALIGNMENT);
        columnaIzquierda.add(tarjetaUsuario());

        JPanel columnaDerecha = new JPanel();
        columnaDerecha.setOpaque(false);
        columnaDerecha.setLayout(new BoxLayout(columnaDerecha, BoxLayout.Y_AXIS));
        columnaDerecha.setAlignmentY(Component.TOP_ALIGNMENT);
        columnaDerecha.add(tarjetaContrasena());
        columnaDerecha.add(Box.createVerticalStrut(20));
        columnaDerecha.add(tarjetaRecuperacion());

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

    private JPanel tarjetaUsuario() {
        JPanel tarjeta = tarjetaBase();
        tarjeta.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Cambiar usuario");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        actualizarUsuarioActual();
        etiquetaUsuarioActual.setForeground(Theme.TEXTO_SUAVE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(etiquetaUsuarioActual, gbc);
        gbc.gridwidth = 1;

        Formularios.agregarFila(form, gbc, 1, "Nuevo usuario", campoNuevoUsuario);
        Formularios.agregarFila(form, gbc, 2, "Contraseña actual", campoContrasenaParaUsuario);

        JButton botonCambiar = new JButton("Cambiar usuario");
        Theme.estiloPrincipal(botonCambiar);
        botonCambiar.addActionListener(e -> cambiarUsuario());
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBoton.setOpaque(false);
        filaBoton.add(botonCambiar);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(form, BorderLayout.CENTER);
        tarjeta.add(filaBoton, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void actualizarUsuarioActual() {
        String usuario = UsuarioDAO.obtenerUsuario();
        etiquetaUsuarioActual.setText("Usuario actual: " + (usuario == null ? "-" : usuario));
    }

    private void cambiarUsuario() {
        String nuevoUsuario = campoNuevoUsuario.getText().trim();
        char[] contrasena = campoContrasenaParaUsuario.getPassword();

        if (nuevoUsuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escribe el nuevo nombre de usuario",
                    "Datos incompletos", JOptionPane.WARNING_MESSAGE);
            Arrays.fill(contrasena, ' ');
            return;
        }

        boolean exito;
        try {
            String usuarioActual = UsuarioDAO.obtenerUsuario();
            exito = usuarioActual != null && UsuarioDAO.cambiarUsuario(usuarioActual, contrasena, nuevoUsuario);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo cambiar el usuario: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            Arrays.fill(contrasena, ' ');
            campoContrasenaParaUsuario.setText("");
        }

        if (exito) {
            campoNuevoUsuario.setText("");
            actualizarUsuarioActual();
            Avisos.exito(this, "Usuario actualizado", "Tu nombre de usuario se cambiÃ³ correctamente.");
        } else {
            JOptionPane.showMessageDialog(this, "La contraseña actual no es correcta",
                    "No se pudo cambiar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel tarjetaContrasena() {
        JPanel tarjeta = tarjetaBase();
        tarjeta.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Cambiar contraseña");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Formularios.agregarFila(form, gbc, 0, "Contraseña actual", campoActual);
        Formularios.agregarFila(form, gbc, 1, "Contraseña nueva", campoNueva);
        Formularios.agregarFila(form, gbc, 2, "Confirmar contraseña", campoConfirmar);

        JButton botonCambiar = new JButton("Cambiar contraseña");
        Theme.estiloPrincipal(botonCambiar);
        botonCambiar.addActionListener(e -> cambiarContrasena());
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBoton.setOpaque(false);
        filaBoton.add(botonCambiar);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(form, BorderLayout.CENTER);
        tarjeta.add(filaBoton, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void cambiarContrasena() {
        char[] actual = campoActual.getPassword();
        char[] nueva = campoNueva.getPassword();
        char[] confirmar = campoConfirmar.getPassword();

        if (nueva.length < 4) {
            JOptionPane.showMessageDialog(this, "La contraseña nueva debe tener al menos 4 caracteres",
                    "Datos invalidos", JOptionPane.WARNING_MESSAGE);
            limpiarCampos(actual, nueva, confirmar);
            return;
        }
        if (!Arrays.equals(nueva, confirmar)) {
            JOptionPane.showMessageDialog(this, "La confirmacion no coincide con la contraseña nueva",
                    "Datos invalidos", JOptionPane.WARNING_MESSAGE);
            limpiarCampos(actual, nueva, confirmar);
            return;
        }

        boolean exito;
        try {
            String usuario = UsuarioDAO.obtenerUsuario();
            exito = usuario != null && UsuarioDAO.cambiarContrasena(usuario, actual, nueva);
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo cambiar la contraseña: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            limpiarCampos(actual, nueva, confirmar);
        }

        if (exito) {
            Avisos.exito(this, "Contraseña actualizada", "Tu contraseña se cambió correctamente.");
        } else {
            JOptionPane.showMessageDialog(this, "La contraseña actual no es correcta",
                    "No se pudo cambiar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel tarjetaRecuperacion() {
        JPanel tarjeta = tarjetaBase();
        tarjeta.setLayout(new BorderLayout(0, 12));

        JLabel titulo = new JLabel("Recuperación de acceso");
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 16f));

        actualizarMetodoRecuperacion();
        etiquetaMetodoRecuperacion.setForeground(Theme.TEXTO_SUAVE);

        JButton botonConfigurar = new JButton("Configurar / cambiar método");
        botonConfigurar.addActionListener(e -> configurarRecuperacion());
        JPanel filaBoton = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filaBoton.setOpaque(false);
        filaBoton.add(botonConfigurar);

        tarjeta.add(titulo, BorderLayout.NORTH);
        tarjeta.add(etiquetaMetodoRecuperacion, BorderLayout.CENTER);
        tarjeta.add(filaBoton, BorderLayout.SOUTH);
        return tarjeta;
    }

    private void actualizarMetodoRecuperacion() {
        actualizarMetodoRecuperacion(UsuarioDAO.obtenerUsuario());
    }

    private void actualizarMetodoRecuperacion(String usuario) {
        TipoRecuperacion tipo = usuario == null ? null : UsuarioDAO.obtenerTipoRecuperacion(usuario);
        etiquetaMetodoRecuperacion.setText("<html>Método actual: "
                + (tipo == null ? "no configurado" : tipo.getEtiqueta()) + "</html>");
    }

    private void configurarRecuperacion() {
        String usuario = UsuarioDAO.obtenerUsuario();
        if (usuario == null) {
            return;
        }
        Frame ventana = (Frame) SwingUtilities.getWindowAncestor(this);
        new ConfigurarRecuperacionDialog(ventana, usuario, false).setVisible(true);
        actualizarMetodoRecuperacion(usuario);
    }

    private void limpiarCampos(char[] actual, char[] nueva, char[] confirmar) {
        Arrays.fill(actual, ' ');
        Arrays.fill(nueva, ' ');
        Arrays.fill(confirmar, ' ');
        campoActual.setText("");
        campoNueva.setText("");
        campoConfirmar.setText("");
    }

    private JPanel tarjetaBase() {
        return Theme.crearTarjeta();
    }
}
