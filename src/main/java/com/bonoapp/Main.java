package com.bonoapp;

import com.bonoapp.db.Database;
import com.bonoapp.ui.AvisoDeError;
import com.bonoapp.ui.ConfigurarRespaldoDialog;
import com.bonoapp.ui.LoginDialog;
import com.bonoapp.ui.MainFrame;
import com.bonoapp.ui.Theme;
import com.bonoapp.util.Preferencias;
import com.bonoapp.util.Registro;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.fonts.inter.FlatInterFont;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;
import java.awt.Font;
import java.util.Map;

/**
 * Punto de entrada de BonoApp: prepara la base de datos, aplica el tema, pide iniciar sesion y abre
 * la ventana principal.
 */
public class Main {

    public static void main(String[] args) {
        // Lo primero: que cualquier error inesperado quede anotado en el registro y se avise al usuario.
        Registro.instalarManejadorGlobal();
        Registro.alErrorInesperado(AvisoDeError::mostrar);

        Database.inicializar();

        SwingUtilities.invokeLater(() -> {
            aplicarTema();
            if (!LoginDialog.autenticar(null)) {
                System.exit(0);
            }
            MainFrame ventana = new MainFrame();
            ventana.setVisible(true);
            // Con la ventana ya visible: pide la carpeta de copias la primera vez, o hace la copia de hoy y avisa si falla.
            SwingUtilities.invokeLater(() -> ConfigurarRespaldoDialog.alAbrirLaApp(ventana));
        });
    }

    /**
     * Instala el tema con el tamano de letra guardado en las preferencias.
     * Se puede volver a llamar para aplicar un cambio sin cerrar el programa (hay que crear una ventana
     * nueva despues, los componentes ya existentes no se actualizan solos).
     */
    public static void aplicarTema() {
        // Fuente Inter incluida en la app. Solo se cargan los 3 estilos que se usan (normal, negrita y
        // cursiva) para no gastar memoria ni tiempo de arranque en el resto.
        FlatInterFont.installStyle(FlatInterFont.STYLE_REGULAR);
        FlatInterFont.installStyle(FlatInterFont.STYLE_BOLD);
        FlatInterFont.installStyle(FlatInterFont.STYLE_ITALIC);
        UIManager.put("defaultFont", new Font(FlatInterFont.FAMILY, Font.PLAIN, Preferencias.getTamanoFuente()));

        // El color de acento hace que el foco, los campos y las pestanas usen el mismo indigo de la marca.
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", Theme.hex(Theme.PRINCIPAL)));
        FlatLightLaf.setup();

        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.borderColor", Theme.BORDE);
        UIManager.put("Panel.background", Theme.FONDO);

        // Tablas: filas mas altas, sombreado alterno suave, sin lineas verticales y seleccion en tono indigo.
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.background", Theme.TARJETA);
        UIManager.put("Table.alternateRowColor", Theme.FILA_ALTERNA);
        UIManager.put("Table.showHorizontalLines", false);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 0));
        UIManager.put("Table.cellMargins", new InsetsUIResource(2, 10, 2, 10));
        UIManager.put("Table.selectionBackground", Theme.SELECCION);
        UIManager.put("Table.selectionForeground", Theme.TEXTO);
        UIManager.put("Table.selectionInactiveBackground", Theme.SELECCION);
        UIManager.put("Table.selectionInactiveForeground", Theme.TEXTO);
        UIManager.put("TableHeader.background", Theme.TARJETA);
        UIManager.put("TableHeader.foreground", Theme.TEXTO_SUAVE);
        UIManager.put("TableHeader.separatorColor", Theme.TARJETA);
        UIManager.put("TableHeader.bottomSeparatorColor", Theme.BORDE);
        UIManager.put("TableHeader.cellMargins", new InsetsUIResource(6, 10, 6, 10));
        UIManager.put("TableHeader.height", 36);

        // Barras de desplazamiento finas y redondeadas, sin botones de flecha.
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new InsetsUIResource(2, 2, 2, 2));
        UIManager.put("ScrollBar.showButtons", false);
    }
}
