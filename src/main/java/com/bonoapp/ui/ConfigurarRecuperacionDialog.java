package com.bonoapp.ui;

import com.bonoapp.db.UsuarioDAO;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/** Formulario para elegir y configurar el metodo de recuperacion de acceso (codigo o pregunta). */
public class ConfigurarRecuperacionDialog extends JDialog {

    private static final String[] PREGUNTAS_SUGERIDAS = {
            "¿Cuál es el nombre de tu primera mascota?",
            "¿En qué ciudad naciste?",
            "¿Cuál es tu comida favorita?",
            "Otra (escríbela abajo)"
    };

    private final String usuario;
    private final boolean permiteOmitir;

    private final JRadioButton opcionCodigo = new JRadioButton("Código de recuperación", true);
    private final JRadioButton opcionPregunta = new JRadioButton("Pregunta de seguridad");

    private final CardLayout tarjetas = new CardLayout();
    private final JPanel panelTarjetas = new JPanel(tarjetas);

    private final JButton botonGenerarCodigo = new JButton("Generar código");
    private final JTextField campoCodigoGenerado = new JTextField();

    private final JComboBox<String> comboPregunta = new JComboBox<>(PREGUNTAS_SUGERIDAS);
    private final JTextField campoPreguntaPersonalizada = new JTextField(20);
    private final JTextField campoRespuesta = new JTextField(20);

    private boolean confirmado = false;
    private String codigoGenerado;

    public ConfigurarRecuperacionDialog(Frame propietario, String usuario, boolean permiteOmitir) {
        super(propietario, "Configurar recuperación de acceso", true);
        this.usuario = usuario;
        this.permiteOmitir = permiteOmitir;
        setLayout(new BorderLayout());

        ButtonGroup grupo = new ButtonGroup();
        grupo.add(opcionCodigo);
        grupo.add(opcionPregunta);
        opcionCodigo.addActionListener(e -> tarjetas.show(panelTarjetas, "codigo"));
        opcionPregunta.addActionListener(e -> tarjetas.show(panelTarjetas, "pregunta"));

        JPanel form = new JPanel(new BorderLayout(0, 12));
        form.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel intro = new JLabel("<html>Si olvidas tu contraseña, podrás recuperarla con esto.<br>"
                + "Elige una opción:</html>");

        JPanel opciones = new JPanel();
        opciones.setLayout(new javax.swing.BoxLayout(opciones, javax.swing.BoxLayout.Y_AXIS));
        opciones.add(opcionCodigo);
        opciones.add(opcionPregunta);

        panelTarjetas.add(construirTarjetaCodigo(), "codigo");
        panelTarjetas.add(construirTarjetaPregunta(), "pregunta");

        JPanel norte = new JPanel(new BorderLayout(0, 10));
        norte.add(intro, BorderLayout.NORTH);
        norte.add(opciones, BorderLayout.CENTER);

        form.add(norte, BorderLayout.NORTH);
        form.add(panelTarjetas, BorderLayout.CENTER);

        JButton botonGuardar = new JButton("Guardar");
        Theme.estiloPrincipal(botonGuardar);
        botonGuardar.addActionListener(e -> guardar());
        JPanel botones = new JPanel();
        if (permiteOmitir) {
            JButton botonOmitir = new JButton("Omitir por ahora");
            botonOmitir.addActionListener(e -> dispose());
            botones.add(botonOmitir);
        }
        botones.add(botonGuardar);

        add(form, BorderLayout.CENTER);
        add(botones, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(propietario);
        getRootPane().setDefaultButton(botonGuardar);
    }

    private JPanel construirTarjetaCodigo() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        campoCodigoGenerado.setEditable(false);
        campoCodigoGenerado.setHorizontalAlignment(JTextField.CENTER);
        botonGenerarCodigo.addActionListener(e -> {
            codigoGenerado = UsuarioDAO.configurarRecuperacionConCodigo(usuario);
            campoCodigoGenerado.setText(codigoGenerado);
        });

        JLabel advertencia = new JLabel("<html>Guarda este código en un lugar seguro (papel, celular, etc).<br>"
                + "No podrás volver a verlo despues de cerrar esta ventana.</html>");
        advertencia.setForeground(Theme.TEXTO_SUAVE);

        JPanel fila = new JPanel();
        fila.add(botonGenerarCodigo);
        fila.add(campoCodigoGenerado);
        campoCodigoGenerado.setColumns(12);

        panel.add(fila, BorderLayout.NORTH);
        panel.add(advertencia, BorderLayout.CENTER);
        return panel;
    }

    private JPanel construirTarjetaPregunta() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        comboPregunta.addActionListener(e -> campoPreguntaPersonalizada.setEnabled(esOtraPregunta()));
        campoPreguntaPersonalizada.setEnabled(false);

        Formularios.agregarFila(panel, gbc, 0, "Pregunta", comboPregunta);
        Formularios.agregarFila(panel, gbc, 1, "Escribe tu pregunta", campoPreguntaPersonalizada);
        Formularios.agregarFila(panel, gbc, 2, "Respuesta", campoRespuesta);

        return panel;
    }

    private boolean esOtraPregunta() {
        return comboPregunta.getSelectedIndex() == PREGUNTAS_SUGERIDAS.length - 1;
    }

    private void guardar() {
        if (opcionCodigo.isSelected()) {
            if (codigoGenerado == null) {
                JOptionPane.showMessageDialog(this, "Genera el código antes de guardar",
                        "Falta el código", JOptionPane.WARNING_MESSAGE);
                return;
            }
            Avisos.exito(this, "Código guardado", "Recuerda tu código de recuperación:\n" + codigoGenerado);
        } else {
            String pregunta = esOtraPregunta()
                    ? campoPreguntaPersonalizada.getText().trim()
                    : (String) comboPregunta.getSelectedItem();
            String respuesta = campoRespuesta.getText().trim();
            if (pregunta.isEmpty() || respuesta.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Completa la pregunta y la respuesta",
                        "Datos incompletos", JOptionPane.WARNING_MESSAGE);
                return;
            }
            UsuarioDAO.configurarRecuperacionConPregunta(usuario, pregunta, respuesta);
            Avisos.exito(this, "Pregunta guardada", "La pregunta de seguridad quedÃ³ guardada.");
        }
        confirmado = true;
        dispose();
    }

    public boolean isConfirmado() {
        return confirmado;
    }
}
