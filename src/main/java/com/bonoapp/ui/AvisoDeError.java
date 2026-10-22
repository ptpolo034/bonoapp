package com.bonoapp.ui;

import com.bonoapp.util.Registro;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Path;

/**
 * El aviso que ve el usuario cuando ocurre un error inesperado: le dice que algo salio mal, que el error quedo anotado
 * en el registro y como enviarlo, y ofrece abrir la carpeta donde esta el archivo.
 */
public final class AvisoDeError {

    private AvisoDeError() {
    }

    /** Muestra el aviso (en el hilo de la interfaz). "resumen" es el nombre corto del error, solo como referencia. */
    public static void mostrar(String resumen) {
        SwingUtilities.invokeLater(() -> {
            Path archivo = Registro.archivo();
            int opcion = JOptionPane.showOptionDialog(null,
                    "Algo salió mal y el error quedó anotado.\n\n"
                            + "La aplicación puede seguir funcionando. Si el problema se repite, envía el archivo "
                            + "de errores:\n" + archivo + "\n\n(Referencia: " + resumen + ")",
                    "Ocurrió un error", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null,
                    new String[]{"Abrir carpeta del registro", "Cerrar"}, "Cerrar");
            if (opcion == 0) {
                abrirCarpetaDelRegistro();
            }
        });
    }

    /** Abre en el explorador la carpeta donde esta el archivo de errores. */
    public static void abrirCarpetaDelRegistro() {
        Path carpeta = Registro.archivo().getParent();
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                java.nio.file.Files.createDirectories(carpeta);
                Desktop.getDesktop().open(carpeta.toFile());
                return;
            }
        } catch (IOException | RuntimeException e) {
            // Si no se puede abrir, se le dice donde esta.
        }
        JOptionPane.showMessageDialog(null, "El registro de errores está en:\n" + Registro.archivo(),
                "Registro de errores", JOptionPane.INFORMATION_MESSAGE);
    }
}
