package com.bonoapp.ui;

import com.bonoapp.db.UsuarioDAO;
import com.bonoapp.model.TipoRecuperacion;

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

/** Recuperacion de acceso desde la pantalla de login: usuario -> desafio -> contrasena nueva. */
public class RecuperarAccesoDialog extends JDialog {

    private final JPanel contenedor = new JPanel(new BorderLayout());

    private String usuarioEnProceso;
    private TipoRecuperacion tipoEnProceso;

    public RecuperarAccesoDialog(Frame propietario) {
        super(propietario, "Recuperar acceso", true);
        setLayout(new BorderLayout());
        add(contenedor, BorderLayout.CENTER);
        mostrarPasoUsuario();
        setLocationRelativeTo(propietario);
    }

    private void mostrarPasoUsuario() {
        JTextField campoUsuario = new JTextField(18);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Formularios.agregarFila(form, gbc, 0, "Usuario", campoUsuario);

        JButton botonContinuar = new JButton("Continuar");
        botonContinuar.addActionListener(e -> {
            String usuario = campoUsuario.getText().trim();
            if (usuario.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Escribe tu usuario",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            TipoRecuperacion tipo = UsuarioDAO.obtenerTipoRecuperacion(usuario);
            if (tipo == null) {
                JOptionPane.showMessageDialog(this,
                        "Esta cuenta no tiene un metodo de recuperacion configurado",
                        "No disponible", JOptionPane.ERROR_MESSAGE);
                return;
            }
            usuarioEnProceso = usuario;
            tipoEnProceso = tipo;
            mostrarPasoDesafio();
        });

        mostrarPaso(form, botonContinuar);
        getRootPane().setDefaultButton(botonContinuar);
    }

    private void mostrarPasoDesafio() {
        boolean esCodigo = tipoEnProceso == TipoRecuperacion.CODIGO;
        JTextField campoRespuesta = new JTextField(18);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        if (esCodigo) {
            Formularios.agregarFila(form, gbc, 0, "Código de recuperación", campoRespuesta);
        } else {
            String pregunta = UsuarioDAO.obtenerPreguntaRecuperacion(usuarioEnProceso);
            if (pregunta == null) {
                pregunta = "(no se encontro la pregunta guardada)";
            }
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            form.add(new JLabel("<html>" + pregunta + "</html>"), gbc);
            gbc.gridwidth = 1;
            Formularios.agregarFila(form, gbc, 1, "Respuesta", campoRespuesta);
        }

        JButton botonVerificar = new JButton("Verificar");
        botonVerificar.addActionListener(e -> {
            if (!UsuarioDAO.validarRecuperacion(usuarioEnProceso, campoRespuesta.getText())) {
                JOptionPane.showMessageDialog(this,
                        esCodigo ? "El código no es correcto" : "La respuesta no es correcta",
                        "No coincide", JOptionPane.ERROR_MESSAGE);
                return;
            }
            mostrarPasoNuevaContrasena();
        });

        mostrarPaso(form, botonVerificar);
        getRootPane().setDefaultButton(botonVerificar);
    }

    private void mostrarPasoNuevaContrasena() {
        JPasswordField campoNueva = new JPasswordField(18);
        JPasswordField campoConfirmar = new JPasswordField(18);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Formularios.agregarFila(form, gbc, 0, "Contraseña nueva", campoNueva);
        Formularios.agregarFila(form, gbc, 1, "Confirmar contraseña", campoConfirmar);

        JButton botonGuardar = new JButton("Guardar");
        botonGuardar.addActionListener(e -> {
            char[] nueva = campoNueva.getPassword();
            char[] confirmar = campoConfirmar.getPassword();
            try {
                if (nueva.length < 4) {
                    JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 4 caracteres",
                            "Datos invalidos", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!Arrays.equals(nueva, confirmar)) {
                    JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden",
                            "Datos invalidos", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                UsuarioDAO.restablecerContrasena(usuarioEnProceso, nueva);
                Avisos.exito(this, "Contraseña actualizada",
                        "Ahora inicia sesión con tu nueva contraseña.");
                dispose();
            } catch (RuntimeException ex) {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar la contraseña: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                Arrays.fill(nueva, ' ');
                Arrays.fill(confirmar, ' ');
            }
        });

        mostrarPaso(form, botonGuardar);
        getRootPane().setDefaultButton(botonGuardar);
    }

    private void mostrarPaso(JPanel form, JButton botonPrincipal) {
        Theme.estiloPrincipal(botonPrincipal);
        JButton botonCancelar = new JButton("Cancelar");
        botonCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel();
        botones.add(botonCancelar);
        botones.add(botonPrincipal);

        contenedor.removeAll();
        contenedor.add(form, BorderLayout.CENTER);
        contenedor.add(botones, BorderLayout.SOUTH);
        pack();
        revalidate();
        repaint();
    }
}
