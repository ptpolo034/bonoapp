package com.bonoapp.util;

import com.bonoapp.db.Database;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Copias de seguridad automaticas en la carpeta que el usuario elige (idealmente en otro disco).
 *
 * Cada copia es el archivo completo de la base de datos, con todo lo que hay hasta ese momento. Se guarda
 * una copia por dia con la fecha en el nombre (bonoapp_respaldo_AAAA-MM-DD.db): al abrir la app (si todavia
 * no hay una de hoy) y al cerrarla (se actualiza la de hoy). Se conservan las ultimas
 * {@link #DIAS_A_CONSERVAR}; las mas viejas se borran solas, y solo se borran archivos con ese nombre exacto,
 * nunca otros que haya en la carpeta.
 */
public final class Respaldo {

    public static final int DIAS_A_CONSERVAR = 14;

    private static final Pattern COPIA_DIARIA = Pattern.compile("bonoapp_respaldo_(\\d{4}-\\d{2}-\\d{2})\\.db");
    private static final String CARPETA_SUGERIDA = "BonoApp_Respaldos";

    private Respaldo() {
    }

    /** La carpeta elegida por el usuario (exista o no en este momento), o null si todavia no eligio ninguna. */
    public static Path carpetaConfigurada() {
        String carpeta = Preferencias.getCarpetaRespaldo();
        return carpeta == null || carpeta.isBlank() ? null : Path.of(carpeta);
    }

    /** La carpeta elegida, solo si existe ahora (un disco o una USB desconectados no cuentan). */
    public static Path carpetaDisponible() {
        Path ruta = carpetaConfigurada();
        return ruta != null && Files.isDirectory(ruta) ? ruta : null;
    }

    /**
     * Copia la base de datos a "destino". Escribe primero en un archivo temporal y luego lo renombra, para
     * que nunca quede una copia a medias si algo falla (disco lleno, apagon...).
     */
    public static Path copiarA(Path destino) throws IOException {
        Path temporal = destino.resolveSibling(destino.getFileName() + ".tmp");
        try {
            Files.copy(Database.archivo(), temporal, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporal);
        }
        return destino;
    }

    /** Exporta con nombre por fecha a la carpeta (la que el usuario saca a mano con "Exportar copia"). */
    public static Path exportarACarpeta(Path carpeta) throws IOException {
        return copiarA(carpeta.resolve("bonoapp_backup_" + LocalDate.now() + ".db"));
    }

    /** Guarda la copia de hoy en la carpeta (la crea o la actualiza) y borra las mas viejas de la cuenta. */
    public static Path copiaDelDia(Path carpeta) throws IOException {
        Path destino = copiarA(carpeta.resolve(nombreCopia(LocalDate.now())));
        rotar(carpeta);
        return destino;
    }

    /**
     * Al abrir la app: si hay carpeta configurada y todavia no hay copia de hoy, la hace. Devuelve null si todo
     * esta bien (o si todavia no se eligio carpeta), o un mensaje para mostrar al usuario si algo fallo.
     */
    public static String alAbrir() {
        Path configurada = carpetaConfigurada();
        if (configurada == null) {
            return null;
        }
        Path carpeta = carpetaDisponible();
        if (carpeta == null) {
            return "La carpeta de las copias de seguridad no está disponible:\n" + configurada
                    + "\n\nSi es un disco o una USB, revisa que esté conectado.";
        }
        if (Files.exists(carpeta.resolve(nombreCopia(LocalDate.now())))) {
            return null;
        }
        return intentarCopia(carpeta);
    }

    /** Al cerrar la app: actualiza la copia de hoy. Misma convencion de retorno que {@link #alAbrir()}. */
    public static String alCerrar() {
        Path configurada = carpetaConfigurada();
        if (configurada == null) {
            return null;
        }
        Path carpeta = carpetaDisponible();
        if (carpeta == null) {
            return "No se pudo guardar la copia de seguridad: la carpeta no está disponible:\n" + configurada;
        }
        return intentarCopia(carpeta);
    }

    private static String intentarCopia(Path carpeta) {
        try {
            copiaDelDia(carpeta);
            return null;
        } catch (IOException | RuntimeException e) {
            return "No se pudo guardar la copia de seguridad en:\n" + carpeta + "\n\n" + e.getMessage();
        }
    }

    /** Fecha y hora de la copia automatica mas reciente de la carpeta, o null si no hay ninguna. */
    public static LocalDateTime ultimaCopia() {
        Path carpeta = carpetaDisponible();
        if (carpeta == null) {
            return null;
        }
        try (Stream<Path> archivos = Files.list(carpeta)) {
            return archivos.filter(p -> COPIA_DIARIA.matcher(p.getFileName().toString()).matches())
                    .map(p -> {
                        try {
                            return Files.getLastModifiedTime(p).toInstant().atZone(ZoneId.systemDefault())
                                    .toLocalDateTime();
                        } catch (IOException e) {
                            return null;
                        }
                    })
                    .filter(f -> f != null)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    static String nombreCopia(LocalDate fecha) {
        return "bonoapp_respaldo_" + fecha + ".db";
    }

    /** Borra las copias diarias mas viejas, dejando las ultimas DIAS_A_CONSERVAR. No toca otros archivos. */
    static void rotar(Path carpeta) throws IOException {
        List<Path> copias = new ArrayList<>();
        try (Stream<Path> archivos = Files.list(carpeta)) {
            archivos.filter(p -> COPIA_DIARIA.matcher(p.getFileName().toString()).matches()).forEach(copias::add);
        }
        // El nombre lleva la fecha en formato AAAA-MM-DD, asi que ordenar por nombre es ordenar por fecha.
        copias.sort(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed());
        for (int i = DIAS_A_CONSERVAR; i < copias.size(); i++) {
            Files.deleteIfExists(copias.get(i));
        }
    }

    /** True si la carpeta esta en el mismo disco que la base de datos: si ese disco se daña, se pierde todo junto. */
    public static boolean mismoDisco(Path carpeta) {
        try {
            Path existente = carpeta.toAbsolutePath();
            while (existente != null && !Files.exists(existente)) {
                existente = existente.getParent();
            }
            if (existente == null) {
                return false;
            }
            return Files.getFileStore(existente).equals(Files.getFileStore(Database.archivo().toAbsolutePath()));
        } catch (IOException e) {
            Path a = carpeta.toAbsolutePath().getRoot();
            Path b = Database.archivo().toAbsolutePath().getRoot();
            return a != null && a.equals(b);
        }
    }

    /**
     * Una carpeta sugerida en OTRO disco distinto al de los datos (por ejemplo D:\BonoApp_Respaldos), o null si
     * este equipo solo tiene un disco con espacio libre.
     */
    public static Path sugerirCarpeta() {
        try {
            FileStore deLosDatos = Files.getFileStore(Database.archivo().toAbsolutePath());
            for (Path raiz : FileSystems.getDefault().getRootDirectories()) {
                try {
                    FileStore disco = Files.getFileStore(raiz);
                    if (!disco.equals(deLosDatos) && !disco.isReadOnly() && disco.getUsableSpace() > 1L << 30) {
                        return raiz.resolve(CARPETA_SUGERIDA);
                    }
                } catch (IOException e) {
                    // Unidad sin disco (lector vacio, por ejemplo): se salta.
                }
            }
        } catch (IOException e) {
            // Sin sugerencia: el usuario elige.
        }
        return null;
    }
}
