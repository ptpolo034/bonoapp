package com.bonoapp.db;

import com.bonoapp.model.TipoBoleta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Tratos: independientes por tipo de boleta")
class TratoDAOTest extends BaseDeDatos {

    @Test
    @DisplayName("Los tratos de la Diaria no cambian los del Premio mayor")
    void tiposIndependientes() {
        int ana = crearVendedora("Ana");

        TratoDAO.reemplazar(ana, TipoBoleta.DIARIA, List.of("015", "234", "234"));
        assertEquals(List.of("015", "234", "234"), TratoDAO.listarNumeros(ana, TipoBoleta.DIARIA));
        assertTrue(TratoDAO.listarNumeros(ana, TipoBoleta.PREMIO_MAYOR).isEmpty());

        TratoDAO.reemplazar(ana, TipoBoleta.PREMIO_MAYOR, List.of("777", "100"));
        assertEquals(List.of("100", "777"), TratoDAO.listarNumeros(ana, TipoBoleta.PREMIO_MAYOR));
        assertEquals(List.of("015", "234", "234"), TratoDAO.listarNumeros(ana, TipoBoleta.DIARIA));

        TratoDAO.reemplazar(ana, TipoBoleta.DIARIA, List.of());
        assertTrue(TratoDAO.listarNumeros(ana, TipoBoleta.DIARIA).isEmpty());
        assertEquals(2, TratoDAO.listarNumeros(ana, TipoBoleta.PREMIO_MAYOR).size());
    }

    @Test
    @DisplayName("Los tratos de una vendedora no se mezclan con los de otra")
    void vendedorasIndependientes() {
        int ana = crearVendedora("Ana");
        int bea = crearVendedora("Beatriz");
        TratoDAO.reemplazar(ana, TipoBoleta.DIARIA, List.of("001"));
        assertTrue(TratoDAO.listarNumeros(bea, TipoBoleta.DIARIA).isEmpty());
    }

    @Test
    @DisplayName("Guardar los dos tipos juntos y anotarlo en el historial")
    void dosTiposJuntosYHistorial() {
        int ana = crearVendedora("Ana");
        TratoDAO.reemplazar(ana, Map.of(TipoBoleta.DIARIA, List.of("001"), TipoBoleta.PREMIO_MAYOR, List.of("002")));
        assertEquals(List.of("001"), TratoDAO.listarNumeros(ana, TipoBoleta.DIARIA));
        assertEquals(List.of("002"), TratoDAO.listarNumeros(ana, TipoBoleta.PREMIO_MAYOR));
        assertTrue(HistorialDAO.listar(50).stream().anyMatch(e -> e.accion().equals("Tratos actualizados")
                && e.detalle().contains("Ana") && e.detalle().contains("Premio mayor")));
    }

    @Test
    @DisplayName("Una base antigua (sin columna tipo) se migra una sola vez: los tratos valen para los dos tipos")
    void migracionDeBaseAntigua() throws Exception {
        int ana = crearVendedora("Ana");
        int bea = crearVendedora("Beatriz");

        // Se deja la tabla de tratos como la tenia la version anterior: sin "tipo".
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + Database.archivo());
             Statement st = c.createStatement()) {
            st.execute("DROP TABLE tratos");
            st.execute("CREATE TABLE tratos (id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " vendedora_id INTEGER NOT NULL REFERENCES vendedoras(id) ON DELETE CASCADE,"
                    + " numero TEXT NOT NULL)");
            st.execute("INSERT INTO tratos (vendedora_id, numero) VALUES (" + ana + ", '010'), (" + ana + ", '011'), ("
                    + bea + ", '555')");
        }

        Database.inicializar();   // es lo que pasa al abrir la version nueva
        assertEquals(List.of("010", "011"), TratoDAO.listarNumeros(ana, TipoBoleta.DIARIA));
        assertEquals(List.of("010", "011"), TratoDAO.listarNumeros(ana, TipoBoleta.PREMIO_MAYOR));
        assertEquals(List.of("555"), TratoDAO.listarNumeros(bea, TipoBoleta.DIARIA));
        assertEquals(List.of("555"), TratoDAO.listarNumeros(bea, TipoBoleta.PREMIO_MAYOR));

        // Despues cada tipo se edita aparte, y abrir el programa de nuevo no vuelve a copiar nada.
        TratoDAO.reemplazar(ana, TipoBoleta.PREMIO_MAYOR, List.of("999"));
        Database.inicializar();
        Database.inicializar();
        assertEquals(List.of("999"), TratoDAO.listarNumeros(ana, TipoBoleta.PREMIO_MAYOR));
        assertEquals(List.of("010", "011"), TratoDAO.listarNumeros(ana, TipoBoleta.DIARIA));
        assertEquals(1, TratoDAO.listarNumeros(bea, TipoBoleta.DIARIA).size());
    }
}
