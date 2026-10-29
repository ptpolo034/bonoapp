package com.bonoapp.util;

import com.bonoapp.db.Database;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Copias de seguridad diarias")
class RespaldoTest {

    @BeforeAll
    static void soloEnLaCarpetaDePruebas() {
        assertTrue(System.getProperty("user.home").replace('\\', '/').endsWith("/home-pruebas"),
                "las pruebas deben correr con user.home = target/home-pruebas");
        Database.inicializar();
    }

    @Test
    @DisplayName("La copia del dia queda con la fecha en el nombre y trae la base de datos")
    void copiaDelDia(@TempDir Path carpeta) throws IOException {
        Path copia = Respaldo.copiaDelDia(carpeta);
        assertEquals("bonoapp_respaldo_" + LocalDate.now() + ".db", copia.getFileName().toString());
        assertTrue(Files.size(copia) > 0);
        assertFalse(Files.exists(carpeta.resolve(copia.getFileName() + ".tmp")), "no debe quedar el archivo temporal");
    }

    @Test
    @DisplayName("Se conservan las ultimas 14 copias y no se toca ningun otro archivo")
    void rotacion(@TempDir Path carpeta) throws IOException {
        LocalDate dia = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < 20; i++) {
            Files.writeString(carpeta.resolve(Respaldo.nombreCopia(dia.plusDays(i))), "copia " + i);
        }
        Files.writeString(carpeta.resolve("mis_fotos.zip"), "no tocar");
        Files.writeString(carpeta.resolve("bonoapp_backup_2026-01-01.db"), "copia exportada a mano");

        Respaldo.rotar(carpeta);

        long copiasDiarias = 0;
        try (var archivos = Files.list(carpeta)) {
            copiasDiarias = archivos.filter(p -> p.getFileName().toString().startsWith("bonoapp_respaldo_")).count();
        }
        assertEquals(14, copiasDiarias);
        assertFalse(Files.exists(carpeta.resolve(Respaldo.nombreCopia(dia))), "la mas vieja se borra");
        assertTrue(Files.exists(carpeta.resolve(Respaldo.nombreCopia(dia.plusDays(19)))), "la mas nueva se conserva");
        assertTrue(Files.exists(carpeta.resolve("mis_fotos.zip")));
        assertTrue(Files.exists(carpeta.resolve("bonoapp_backup_2026-01-01.db")));
    }
}
