package com.bonoapp.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Registro de errores: cada error inesperado (y cada fallo de la base de datos) queda anotado en
 * ~/.bonoapp/errores.log con la fecha, la version de la app, el sistema, la pantalla en la que estaba el usuario y
 * el detalle tecnico. Asi, si algo falla, hay un archivo que se puede enviar para encontrar la causa.
 *
 * Reglas: registrar NUNCA debe provocar otro error (cualquier fallo al escribir se ignora); no se guardan
 * contrasenas ni datos del negocio, solo informacion tecnica; el archivo no crece sin limite (al pasar de 1 MB el
 * viejo se rota a errores.log.1); y nada se envia por internet.
 */
public final class Registro {

    private static final long MAX_BYTES = 1_000_000;
    private static final long PAUSA_ENTRE_AVISOS_MS = 20_000;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Object CANDADO = new Object();

    private static volatile String pantalla = "(inicio de la app)";
    private static volatile Consumer<String> avisador;
    private static long ultimoAviso = 0;

    private Registro() {
    }

    /** El archivo del registro de errores. */
    public static Path archivo() {
        return Path.of(System.getProperty("user.home"), ".bonoapp", "errores.log");
    }

    /** Anota en que pantalla esta el usuario, para incluirlo en el registro si algo falla. */
    public static void pantalla(String nombre) {
        pantalla = nombre;
    }

    /** Quien mostrara el aviso "algo salio mal" al usuario cuando ocurre un error inesperado (lo pone la interfaz). */
    public static void alErrorInesperado(Consumer<String> aviso) {
        avisador = aviso;
    }

    /** Hace que cualquier error no atrapado, en cualquier hilo (incluido el de la interfaz), quede registrado. */
    public static void instalarManejadorGlobal() {
        Thread.setDefaultUncaughtExceptionHandler((hilo, error) -> inesperado(error));
    }

    /** Un error inesperado: se anota y, como mucho una vez cada 20 segundos, se avisa al usuario. */
    static void inesperado(Throwable error) {
        error("Error inesperado", error);
        Consumer<String> aviso = avisador;
        if (aviso == null) {
            return;
        }
        long ahora = System.currentTimeMillis();
        boolean avisar;
        synchronized (CANDADO) {
            avisar = ahora - ultimoAviso > PAUSA_ENTRE_AVISOS_MS;
            if (avisar) {
                ultimoAviso = ahora;
            }
        }
        if (avisar) {
            try {
                aviso.accept(error.getClass().getSimpleName() + (error.getMessage() == null ? "" : ": " + error.getMessage()));
            } catch (RuntimeException e) {
                // El aviso nunca debe provocar otro error.
            }
        }
    }

    /** Anota un error con su contexto ("Error inesperado", "Fallo de la base de datos"...). Nunca lanza excepciones. */
    public static void error(String contexto, Throwable error) {
        try {
            StringWriter trazaTexto = new StringWriter();
            error.printStackTrace(new PrintWriter(trazaTexto));
            String entrada = "==== " + LocalDateTime.now().format(FORMATO) + " ====\n"
                    + "Versión: " + Version.ACTUAL + " | " + System.getProperty("os.name") + " "
                    + System.getProperty("os.version") + " " + System.getProperty("os.arch")
                    + " | Java " + System.getProperty("java.version") + "\n"
                    + "Pantalla: " + pantalla + "\n"
                    + "Tipo: " + contexto + "\n"
                    + trazaTexto + "\n";
            escribir(entrada);
        } catch (RuntimeException e) {
            // Registrar nunca debe romper nada.
        }
    }

    private static void escribir(String entrada) {
        synchronized (CANDADO) {
            try {
                Path archivo = archivo();
                Files.createDirectories(archivo.getParent());
                if (Files.exists(archivo) && Files.size(archivo) > MAX_BYTES) {
                    Files.move(archivo, archivo.resolveSibling("errores.log.1"), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.writeString(archivo, entrada, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } catch (IOException | RuntimeException e) {
                // Si no se puede escribir (disco lleno, sin permisos), se ignora: no hay donde avisar.
            }
        }
    }
}
