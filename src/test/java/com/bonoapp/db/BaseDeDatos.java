package com.bonoapp.db;

import com.bonoapp.model.Vendedora;
import com.bonoapp.util.Sesion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Base de las pruebas que usan la base de datos: antes de CADA prueba deja la carpeta de datos vacia y crea
 * una base nueva, asi ninguna prueba depende de otra.
 *
 * Seguridad: borra la carpeta de datos, por eso se niega a correr si no esta en la carpeta de pruebas
 * (target/home-pruebas, que pone el pom). Nunca debe tocar la base real del usuario.
 */
abstract class BaseDeDatos {

    private static int contadorDocumentos = 0;

    @BeforeAll
    static void verificarQueEsLaCarpetaDePruebas() {
        String casa = System.getProperty("user.home").replace('\\', '/');
        if (!casa.endsWith("/home-pruebas")) {
            throw new IllegalStateException("Las pruebas deben correr con user.home = target/home-pruebas (lo pone el pom), "
                    + "pero user.home es " + casa + ". Se cancela para no tocar los datos reales.");
        }
    }

    @BeforeEach
    void baseNueva() throws IOException {
        Path carpeta = Database.archivo().getParent();
        if (Files.exists(carpeta)) {
            try (Stream<Path> todo = Files.walk(carpeta)) {
                for (Path p : todo.sorted(Comparator.reverseOrder()).toList()) {
                    if (!p.equals(carpeta)) {
                        Files.deleteIfExists(p);
                    }
                }
            }
        }
        Database.inicializar();
        Sesion.iniciar("prueba");
    }

    /** Crea una vendedora activa con ese nombre (documento distinto cada vez) y devuelve su id. */
    static int crearVendedora(String nombre) {
        VendedoraDAO.insertar(new Vendedora(0, nombre, "doc" + (++contadorDocumentos), "", true));
        return VendedoraDAO.listar().stream()
                .filter(v -> v.getNombre().equals(nombre))
                .findFirst().orElseThrow().getId();
    }

    /** Los ids de todas las boletas guardadas, en el orden en que salen de recientes. */
    static List<Integer> idsDeBoletas() {
        return BoletaDAO.listarRecientes(10_000_000).stream().map(b -> b.getId()).toList();
    }
}
