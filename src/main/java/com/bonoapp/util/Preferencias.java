package com.bonoapp.util;

import com.bonoapp.model.TipoBoleta;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Preferencias del usuario guardadas en ~/.bonoapp/config.properties. */
public final class Preferencias {

    private static final Path ARCHIVO = Path.of(System.getProperty("user.home"), ".bonoapp", "config.properties");
    private static final int TAMANO_FUENTE_POR_DEFECTO = 15;

    private Preferencias() {
    }

    public static int getTamanoFuente() {
        return getEntero("tamanoFuente", TAMANO_FUENTE_POR_DEFECTO);
    }

    public static void guardarTamanoFuente(int tamano) {
        guardarEntero("tamanoFuente", tamano);
    }

    public static int getPrecio(TipoBoleta tipo) {
        return getEntero("precio." + tipo.name(), tipo == TipoBoleta.DIARIA ? 10000 : 100000);
    }

    public static void guardarPrecio(TipoBoleta tipo, int precio) {
        guardarEntero("precio." + tipo.name(), precio);
    }

    /** Carpeta donde se guardan las copias de seguridad, o null si todavia no se ha elegido. */
    public static String getCarpetaRespaldo() {
        return cargar().getProperty("respaldo.carpeta");
    }

    public static void guardarCarpetaRespaldo(String carpeta) {
        Properties p = cargar();
        if (carpeta == null) {
            p.remove("respaldo.carpeta");
        } else {
            p.setProperty("respaldo.carpeta", carpeta);
        }
        guardar(p);
    }

    private static int getEntero(String clave, int porDefecto) {
        try {
            return Integer.parseInt(cargar().getProperty(clave, String.valueOf(porDefecto)));
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    private static void guardarEntero(String clave, int valor) {
        Properties p = cargar();
        p.setProperty(clave, String.valueOf(valor));
        guardar(p);
    }

    private static Properties cargar() {
        Properties p = new Properties();
        if (Files.exists(ARCHIVO)) {
            try (InputStream in = Files.newInputStream(ARCHIVO)) {
                p.load(in);
            } catch (IOException e) {
                throw new ErrorRegistrado("No se pudo leer la configuracion", e);
            }
        }
        return p;
    }

    private static void guardar(Properties p) {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            try (OutputStream out = Files.newOutputStream(ARCHIVO)) {
                p.store(out, "Configuracion de BonoApp");
            }
        } catch (IOException e) {
            throw new ErrorRegistrado("No se pudo guardar la configuracion", e);
        }
    }
}
