package com.bonoapp.ui;

import com.bonoapp.db.UsuarioDAO;
import com.bonoapp.util.Sesion;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;

/** Ventana de acceso: crea el primer usuario si no existe ninguno, o pide iniciar sesion. */
public final class LoginDialog extends JDialog {

    private final JTextField campoUsuario = new JTextField(18);
    private final JPasswordField campoContrasena = new JPasswordField(18);
    private final JPasswordField campoConfirmar;
    private final boolean esCreacion;
    private final Frame propietario;

    private boolean autenticado = false;

    private LoginDialog(Frame propietario, boolean esCreacion) {
        super(propietario, esCreacion ? "Crear cuenta de acceso" : "Iniciar sesion", true);
        this.propietario = propietario;
        this.esCreacion = esCreacion;
        this.campoConfirmar = esCreacion ? new JPasswordField(18) : null;
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setIconImages(Marca.iconos());
        setLayout(new BorderLayout());

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = new JLabel(esCreacion
                ? "Crea el usuario y contrasena de BonoApp"
                : "Ingresa tu usuario y contrasena");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        form.add(titulo, gbc);
        gbc.gridwidth = 1;

        Formularios.agregarFila(form, gbc, 1, "Usuario", campoUsuario);
        Formularios.agregarFila(form, gbc, 2, "Contrasena", campoContrasena);
        if (esCreacion) {
            Formularios.agregarFila(form, gbc, 3, "Confirmar contrasena", campoConfirmar);
        } else {
            JButton botonOlvide = new JButton("¿Olvidaste tu contraseña?");
            botonOlvide.setBorderPainted(false);
            botonOlvide.setContentAreaFilled(false);
            botonOlvide.setForeground(Theme.PRINCIPAL);
            botonOlvide.addActionListener(e -> new RecuperarAccesoDialog(propietario).setVisible(true));
            gbc.gridx = 1;
            gbc.gridy = 3;
            form.add(botonOlvide, gbc);
        }

        JButton botonAceptar = new JButton(esCreacion ? "Crear cuenta" : "Ingresar");
        Theme.estiloPrincipal(botonAceptar);
        JButton botonCancelar = new JButton("Cancelar");
        botonAceptar.addActionListener(e -> aceptar());
        botonCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel();
        botones.add(botonCancelar);
        botones.add(botonAceptar);

        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(botonAceptar);
        pack();
        setLocationRelativeTo(propietario);
    }

    private void aceptar() {
        String usuario = campoUsuario.getText().trim();
        char[] contrasena = campoContrasena.getPassword();
        try {
            if (usuario.isEmpty() || contrasena.length == 0) {
                JOptionPane.showMessageDialog(this, "Usuario y contrasena son obligatorios",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (esCreacion) {
                char[] confirmar = campoConfirmar.getPassword();
                try {
                    if (!Arrays.equals(contrasena, confirmar)) {
                        JOptionPane.showMessageDialog(this, "Las contrasenas no coinciden",
                                "Datos invalidos", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (contrasena.length < 4) {
                        JOptionPane.showMessageDialog(this, "La contrasena debe tener al menos 4 caracteres",
                                "Datos invalidos", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    UsuarioDAO.crear(usuario, contrasena);
                    Sesion.iniciar(usuario);
                    new ConfigurarRecuperacionDialog(propietario, usuario, true).setVisible(true);
                    autenticado = true;
                    dispose();
                } finally {
                    Arrays.fill(confirmar, ' ');
                }
            } else if (UsuarioDAO.validar(usuario, contrasena)) {
                Sesion.iniciar(usuario);
                autenticado = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Usuario o contrasena incorrectos",
                        "Acceso denegado", JOptionPane.ERROR_MESSAGE);
                campoContrasena.setText("");
            }
        } catch (RuntimeException e) {
            JOptionPane.showMessageDialog(this, "No se pudo completar la operacion: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(contrasena, ' ');
        }
    }

    /** Muestra la pantalla de acceso. Devuelve true si el usuario quedo autenticado. */
    public static boolean autenticar(Frame propietario) {
        boolean esCreacion = !UsuarioDAO.existeUsuario();
        LoginDialog dialogo = new LoginDialog(propietario, esCreacion);
        dialogo.setVisible(true);
        return dialogo.autenticado;
    }
}
