package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manejo de la base de datos SQLite.
 * El archivo se guarda en la carpeta del usuario: ~/.bonoapp/bonoapp.db
 * (para hacer copia de seguridad basta con copiar ese archivo).
 */
public final class Database {

    private static final Path CARPETA = Path.of(System.getProperty("user.home"), ".bonoapp");
    private static final String URL = "jdbc:sqlite:" + CARPETA.resolve("bonoapp.db");

    private static final String CREACION_BOLETAS = """
            CREATE TABLE IF NOT EXISTS boletas (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                numero        TEXT,
                tipo          TEXT NOT NULL CHECK (tipo IN ('DIARIA', 'PREMIO_MAYOR')),
                vendedora_id  INTEGER NOT NULL REFERENCES vendedoras(id),
                fecha_entrega TEXT NOT NULL,
                estado        TEXT NOT NULL DEFAULT 'ENTREGADA'
            )
            """;

    private static final String CREACION_TRATOS = """
            CREATE TABLE IF NOT EXISTS tratos (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                vendedora_id INTEGER NOT NULL REFERENCES vendedoras(id) ON DELETE CASCADE,
                numero       TEXT NOT NULL
            )
            """;

    private Database() {
    }

    /** Ruta del archivo de la base de datos (para hacer copias de seguridad). */
    public static Path archivo() {
        return CARPETA.resolve("bonoapp.db");
    }

    public static Connection conectar() throws SQLException {
        Connection c = DriverManager.getConnection(URL);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        } catch (SQLException e) {
            c.close();
            throw e;
        }
        return c;
    }

    /** Crea las tablas si no existen. Aqui se iran agregando las de cada version. */
    public static void inicializar() {
        try {
            Files.createDirectories(CARPETA);
            try (Connection c = conectar(); Statement st = c.createStatement()) {
                st.execute("""
                        CREATE TABLE IF NOT EXISTS vendedoras (
                            id        INTEGER PRIMARY KEY AUTOINCREMENT,
                            nombre    TEXT NOT NULL,
                            documento TEXT,
                            telefono  TEXT,
                            activa    INTEGER NOT NULL DEFAULT 1
                        )
                        """);
                st.execute(CREACION_BOLETAS);
                migrarNumeroBoletaOpcional(st);
                migrarEstadoBoleta(st);
                // Los numeros de boleta (3 cifras) se repiten por fuerza: se venden ~2.000 boletas.
                st.execute("DROP INDEX IF EXISTS idx_boletas_numero_tipo");
                st.execute(CREACION_TRATOS);
                migrarTratosSinUnico(st);
                st.execute("""
                        CREATE TABLE IF NOT EXISTS usuarios (
                            id                    INTEGER PRIMARY KEY AUTOINCREMENT,
                            usuario               TEXT NOT NULL UNIQUE,
                            hash                  TEXT NOT NULL,
                            sal                   TEXT NOT NULL,
                            recuperacion_tipo     TEXT,
                            recuperacion_pregunta TEXT,
                            recuperacion_hash     TEXT,
                            recuperacion_sal      TEXT
                        )
                        """);
                agregarColumnaSiFalta(st, "usuarios", "recuperacion_tipo", "TEXT");
                agregarColumnaSiFalta(st, "usuarios", "recuperacion_pregunta", "TEXT");
                agregarColumnaSiFalta(st, "usuarios", "recuperacion_hash", "TEXT");
                agregarColumnaSiFalta(st, "usuarios", "recuperacion_sal", "TEXT");
                st.execute("""
                        CREATE TABLE IF NOT EXISTS ahorros (
                            id           INTEGER PRIMARY KEY AUTOINCREMENT,
                            vendedora_id INTEGER NOT NULL REFERENCES vendedoras(id),
                            monto        INTEGER NOT NULL,
                            fecha        TEXT NOT NULL,
                            concepto     TEXT
                        )
                        """);
                st.execute("""
                        CREATE TABLE IF NOT EXISTS prestamos (
                            id           INTEGER PRIMARY KEY AUTOINCREMENT,
                            vendedora_id INTEGER NOT NULL REFERENCES vendedoras(id),
                            monto        INTEGER NOT NULL,
                            concepto     TEXT NOT NULL,
                            fecha        TEXT NOT NULL
                        )
                        """);
                st.execute("""
                        CREATE TABLE IF NOT EXISTS abonos (
                            id          INTEGER PRIMARY KEY AUTOINCREMENT,
                            prestamo_id INTEGER NOT NULL REFERENCES prestamos(id) ON DELETE CASCADE,
                            monto       INTEGER NOT NULL,
                            fecha       TEXT NOT NULL,
                            nota        TEXT
                        )
                        """);
                // Cada liquidacion queda registrada (fecha, cantidades, precio y recaudado de ese momento)
                // para poder consultarla, sumarla en reportes y revertirla si hubo un error.
                st.execute("""
                        CREATE TABLE IF NOT EXISTS liquidaciones (
                            id           INTEGER PRIMARY KEY AUTOINCREMENT,
                            vendedora_id INTEGER NOT NULL REFERENCES vendedoras(id),
                            tipo         TEXT NOT NULL,
                            fecha        TEXT NOT NULL,
                            vendidas     INTEGER NOT NULL DEFAULT 0,
                            devueltas    INTEGER NOT NULL DEFAULT 0,
                            precio       INTEGER NOT NULL,
                            recaudado    INTEGER NOT NULL DEFAULT 0,
                            anulada      INTEGER NOT NULL DEFAULT 0,
                            anulada_fecha TEXT
                        )
                        """);
                agregarColumnaSiFalta(st, "boletas", "liquidacion_id", "INTEGER");
                st.execute("CREATE INDEX IF NOT EXISTS idx_boletas_liquidacion ON boletas(liquidacion_id)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_liquidaciones_fecha ON liquidaciones(fecha)");
                // Registro de cambios: quien hizo que y cuando.
                st.execute("""
                        CREATE TABLE IF NOT EXISTS historial (
                            id         INTEGER PRIMARY KEY AUTOINCREMENT,
                            fecha_hora TEXT NOT NULL,
                            usuario    TEXT,
                            accion     TEXT NOT NULL,
                            detalle    TEXT
                        )
                        """);
                st.execute("CREATE INDEX IF NOT EXISTS idx_historial_fecha ON historial(fecha_hora)");
                // Numero ganador de cada sorteo: uno por tipo de boleta y fecha.
                st.execute("""
                        CREATE TABLE IF NOT EXISTS ganadores (
                            id     INTEGER PRIMARY KEY AUTOINCREMENT,
                            tipo   TEXT NOT NULL,
                            fecha  TEXT NOT NULL,
                            numero TEXT NOT NULL,
                            UNIQUE (tipo, fecha)
                        )
                        """);
                st.execute("CREATE INDEX IF NOT EXISTS idx_abonos_prestamo ON abonos(prestamo_id)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_boletas_fecha_entrega ON boletas(fecha_entrega)");
                // Para que buscar por numero o por vendedora, y contar pendientes, sea rapido aunque haya
                // cientos de miles de boletas guardadas.
                st.execute("CREATE INDEX IF NOT EXISTS idx_boletas_numero ON boletas(numero)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_boletas_vendedora ON boletas(vendedora_id)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_boletas_estado_fecha ON boletas(estado, fecha_entrega)");
                st.execute("CREATE INDEX IF NOT EXISTS idx_ahorros_vendedora ON ahorros(vendedora_id)");
            }
        } catch (IOException | SQLException e) {
            throw new ErrorRegistrado("No se pudo iniciar la base de datos", e);
        }
    }

    /**
     * Versiones antiguas creaban boletas.numero como NOT NULL. SQLite no permite quitar esa
     * restriccion con ALTER TABLE, asi que si se detecta el esquema viejo se reconstruye la tabla.
     */
    private static void migrarNumeroBoletaOpcional(Statement st) throws SQLException {
        boolean esquemaViejo = false;
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(boletas)")) {
            while (rs.next()) {
                if ("numero".equals(rs.getString("name")) && rs.getInt("notnull") == 1) {
                    esquemaViejo = true;
                    break;
                }
            }
        }
        if (!esquemaViejo) {
            return;
        }
        st.execute("ALTER TABLE boletas RENAME TO boletas_viejas");
        st.execute(CREACION_BOLETAS);
        st.execute("""
                INSERT INTO boletas (id, numero, tipo, vendedora_id, fecha_entrega)
                SELECT id, numero, tipo, vendedora_id, fecha_entrega FROM boletas_viejas
                """);
        st.execute("DROP TABLE boletas_viejas");
    }

    /**
     * La primera version de "tratos" tenia UNIQUE (vendedora_id, numero), pero un numero puede
     * repetirse hasta 2 veces para la misma vendedora. SQLite no permite quitar esa restriccion
     * con ALTER TABLE, asi que si se detecta se reconstruye la tabla conservando los datos.
     */
    private static void migrarTratosSinUnico(Statement st) throws SQLException {
        String definicion = null;
        try (ResultSet rs = st.executeQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = 'tratos'")) {
            if (rs.next()) {
                definicion = rs.getString(1);
            }
        }
        if (definicion == null || !definicion.toUpperCase().contains("UNIQUE")) {
            return;
        }
        st.execute("ALTER TABLE tratos RENAME TO tratos_viejos");
        st.execute(CREACION_TRATOS);
        st.execute("INSERT INTO tratos (id, vendedora_id, numero) SELECT id, vendedora_id, numero FROM tratos_viejos");
        st.execute("DROP TABLE tratos_viejos");
    }

    /** Agrega la columna "estado" a boletas si viene de una version anterior que no la tenia. */
    private static void migrarEstadoBoleta(Statement st) throws SQLException {
        agregarColumnaSiFalta(st, "boletas", "estado", "TEXT NOT NULL DEFAULT 'ENTREGADA'");
    }

    /** Agrega una columna a una tabla si todavia no existe (para versiones anteriores de la base). */
    private static void agregarColumnaSiFalta(Statement st, String tabla, String columna, String definicion)
            throws SQLException {
        boolean existe = false;
        try (ResultSet rs = st.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (rs.next()) {
                if (columna.equals(rs.getString("name"))) {
                    existe = true;
                    break;
                }
            }
        }
        if (!existe) {
            st.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
        }
    }

}
