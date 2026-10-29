package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.Boleta;
import com.bonoapp.model.EstadoBoleta;
import com.bonoapp.model.TipoBoleta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Acceso a datos de la tabla boletas (entregas de boletas a las vendedoras). */
public final class BoletaDAO {

    /** Con ~2.000 boletas y numeros de 3 cifras, cada numero puede usarse como maximo 2 veces dentro de un lote. */
    public static final int MAX_USOS_POR_NUMERO = 2;

    /**
     * Tamano de un lote: 2.000 boletas de un tipo (Diaria o Premio mayor) por cada fecha de entrega. Cada dia
     * empieza de cero: los numeros usados ayer no cuentan hoy.
     */
    public static final int MAX_BOLETAS_POR_TIPO = 2000;

    private static final int SIN_EXCLUIR = -1;

    private BoletaDAO() {
    }

    /**
     * Busca boletas por numero exacto y/o rango de fechas de entrega (cualquier filtro puede ser
     * null y se ignora). Devuelve como maximo 500, de la entrega mas reciente a la mas antigua.
     */
    public static List<Boleta> buscar(String numero, LocalDate desde, LocalDate hasta) {
        StringBuilder sql = new StringBuilder("""
                SELECT b.id, b.numero, b.tipo, v.nombre AS vendedora_nombre,
                       b.fecha_entrega, b.estado
                FROM boletas b
                JOIN vendedoras v ON v.id = b.vendedora_id
                WHERE 1 = 1""");
        List<String> parametros = new ArrayList<>();
        if (numero != null) {
            sql.append(" AND b.numero = ?");
            parametros.add(numero);
        }
        if (desde != null) {
            sql.append(" AND b.fecha_entrega >= ?");
            parametros.add(desde.toString());
        }
        if (hasta != null) {
            sql.append(" AND b.fecha_entrega <= ?");
            parametros.add(hasta.toString());
        }
        sql.append(" ORDER BY b.fecha_entrega DESC, b.tipo, v.nombre LIMIT 500");

        List<Boleta> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo buscar las boletas", e);
        }
        return resultado;
    }

    private static final String SELECT_BOLETAS = """
            SELECT b.id, b.numero, b.tipo, v.nombre AS vendedora_nombre,
                   b.fecha_entrega, b.estado
            FROM boletas b
            JOIN vendedoras v ON v.id = b.vendedora_id
            """;

    /** Las boletas entregadas un dia concreto. Es lo que muestra la pantalla de Boletas: solo un dia, no todo el historial. */
    public static List<Boleta> listarPorFecha(LocalDate fecha) {
        return consultar(SELECT_BOLETAS + " WHERE b.fecha_entrega = ? ORDER BY v.nombre, b.tipo, b.numero",
                List.of(fecha.toString()));
    }

    /** Las "limite" boletas mas recientes de todos los dias (para "Todos los dias" sin cargar todo el historial). */
    public static List<Boleta> listarRecientes(int limite) {
        return consultar(SELECT_BOLETAS + " ORDER BY b.fecha_entrega DESC, v.nombre, b.numero LIMIT " + limite,
                List.of());
    }

    /**
     * Busca en TODOS los dias por numero exacto, o por nombre o documento de la vendedora (coincidencia parcial).
     * Devuelve como maximo "limite", de la entrega mas reciente a la mas antigua. La comparacion de nombres no
     * distingue mayusculas de minusculas en letras sin tilde (limitacion de SQLite).
     */
    public static List<Boleta> buscarEnTodos(String texto, int limite) {
        String parcial = "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return consultar(SELECT_BOLETAS
                        + " WHERE b.numero = ? OR b.vendedora_id IN (SELECT id FROM vendedoras"
                        + " WHERE nombre LIKE ? ESCAPE '\\' OR documento LIKE ? ESCAPE '\\')"
                        + " ORDER BY b.fecha_entrega DESC, v.nombre, b.numero LIMIT " + limite,
                List.of(texto, parcial, parcial));
    }

    /** Cuantas boletas hay entregadas de un tipo en un dia y cuantas de ellas todavia no tienen numero. */
    public record AvanceLote(int total, int sinNumero) {
    }

    /** El avance del lote (hasta MAX_BOLETAS_POR_TIPO) de un tipo en un dia; sirve para ver si ya estan las 2.000. */
    public static AvanceLote avanceDelLote(TipoBoleta tipo, LocalDate fecha) {
        String sql = "SELECT COUNT(*), COALESCE(SUM(CASE WHEN numero IS NULL THEN 1 ELSE 0 END), 0)"
                + " FROM boletas WHERE tipo = ? AND fecha_entrega = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            ps.setString(2, fecha.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return new AvanceLote(rs.getInt(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo contar las boletas del dia", e);
        }
    }

    /** Cuantas boletas de dias anteriores a "hoy" siguen sin liquidar (para avisarlo en la pantalla de Boletas). */
    public static int contarPendientesAnteriores(LocalDate hoy) {
        String sql = "SELECT COUNT(*) FROM boletas WHERE estado = 'ENTREGADA' AND fecha_entrega < ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, hoy.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo contar las boletas pendientes de dias anteriores", e);
        }
    }

    private static List<Boleta> consultar(String sql, List<String> parametros) {
        List<Boleta> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setString(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo consultar las boletas", e);
        }
        return resultado;
    }

    /** Boletas de un tipo de una vendedora que todavia no se han liquidado (ni vendidas ni devueltas). */
    public static List<Boleta> listarPendientesPorVendedora(int vendedoraId, TipoBoleta tipo) {
        String sql = """
                SELECT b.id, b.numero, b.tipo, v.nombre AS vendedora_nombre,
                       b.fecha_entrega, b.estado
                FROM boletas b
                JOIN vendedoras v ON v.id = b.vendedora_id
                WHERE b.vendedora_id = ? AND b.tipo = ? AND b.estado = 'ENTREGADA'
                ORDER BY b.fecha_entrega, b.numero
                """;
        List<Boleta> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendedoraId);
            ps.setString(2, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener las boletas pendientes de liquidar", e);
        }
        return resultado;
    }

    /**
     * Cambia el estado de un grupo de boletas (por ejemplo al liquidarlas). Debe usarse dentro
     * de una transaccion. Por seguridad, solo actualiza boletas que de verdad sean de esa
     * vendedora, de ese tipo y que sigan "ENTREGADA" (asi un id equivocado por un error de la
     * interfaz nunca puede liquidar la boleta de otra persona). Devuelve cuantas se actualizaron
     * de verdad, para que quien llama pueda basar los calculos de dinero en la cifra real.
     */
    static int marcarEstado(Connection c, List<Integer> ids, EstadoBoleta estado, int vendedoraId, TipoBoleta tipo,
                            int liquidacionId) throws SQLException {
        if (ids.isEmpty()) {
            return 0;
        }
        String sql = "UPDATE boletas SET estado = ?, liquidacion_id = ? WHERE vendedora_id = ? AND tipo = ?"
                + " AND estado = 'ENTREGADA' AND id IN (" + HistorialDAO.marcadores(ids.size()) + ")";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, estado.name());
            ps.setInt(2, liquidacionId);
            ps.setInt(3, vendedoraId);
            ps.setString(4, tipo.name());
            for (int i = 0; i < ids.size(); i++) {
                ps.setInt(i + 5, ids.get(i));
            }
            return ps.executeUpdate();
        }
    }

    /** Elimina varias boletas de una sola vez (una unica sentencia, no un DELETE por cada una). */
    public static void eliminarVarios(List<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }
        String marcadores = HistorialDAO.marcadores(ids.size());
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                // Antes de borrar se anota que boletas eran, porque despues ya no se pueden consultar.
                List<String> descripciones = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT b.numero, b.tipo, v.nombre FROM boletas b JOIN vendedoras v ON v.id = b.vendedora_id"
                                + " WHERE b.id IN (" + marcadores + ") ORDER BY v.nombre, b.numero")) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String numero = rs.getString(1) == null ? "sin número" : rs.getString(1);
                            descripciones.add(numero + " " + TipoBoleta.valueOf(rs.getString(2)).getEtiqueta()
                                    + " (" + rs.getString(3) + ")");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM boletas WHERE id IN (" + marcadores + ")")) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, "Boletas eliminadas",
                        descripciones.size() + " boleta(s): " + HistorialDAO.resumir(descripciones));
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron eliminar las boletas", e);
        }
    }

    /**
     * Registra la entrega de un lote de boletas a una vendedora.
     * Se crean "cantidad" boletas. Las primeras llevan los numeros indicados (pueden ser menos que la
     * cantidad, nunca mas) y las que sobren quedan sin numero asignado todavia. Ningun numero puede
     * quedar usado mas de MAX_USOS_POR_NUMERO veces dentro del mismo tipo.
     */
    public static void registrarEntrega(int vendedoraId, TipoBoleta tipo, int cantidad,
                                         List<String> numeros, LocalDate fechaEntrega) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad de boletas debe ser mayor a cero");
        }
        if (numeros != null && numeros.size() > cantidad) {
            throw new IllegalArgumentException("Hay mas numeros (" + numeros.size()
                    + ") que boletas (" + cantidad + ")");
        }

        String sql = "INSERT INTO boletas (numero, tipo, vendedora_id, fecha_entrega) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            verificarCupoDeBoletas(c, tipo, cantidad, fechaEntrega);
            if (numeros != null && !numeros.isEmpty()) {
                verificarLimiteDeUsos(c, tipo, numeros, SIN_EXCLUIR, fechaEntrega);
            }
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (int i = 0; i < cantidad; i++) {
                    String numero = (numeros != null && i < numeros.size()) ? numeros.get(i) : null;
                    ps.setString(1, numero);
                    ps.setString(2, tipo.name());
                    ps.setInt(3, vendedoraId);
                    ps.setString(4, fechaEntrega.toString());
                    ps.addBatch();
                }
                ps.executeBatch();
                String detalleNumeros = (numeros == null || numeros.isEmpty())
                        ? "sin números todavía" : "números: " + HistorialDAO.resumir(numeros);
                HistorialDAO.registrar(c, "Entrega de boletas", cantidad + " boleta(s) " + tipo.getEtiqueta()
                        + " a " + VendedoraDAO.nombrePorId(c, vendedoraId) + " (" + detalleNumeros + ")");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo registrar la entrega de boletas", e);
        }
    }

    /** Comprueba que registrar "cantidad" boletas mas no pase del lote de MAX_BOLETAS_POR_TIPO del tipo. */
    private static void verificarCupoDeBoletas(Connection c, TipoBoleta tipo, int cantidad, LocalDate fecha)
            throws SQLException {
        String sql = "SELECT COUNT(*) FROM boletas WHERE tipo = ? AND fecha_entrega = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            ps.setString(2, fecha.toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                int existentes = rs.getInt(1);
                if (existentes + cantidad > MAX_BOLETAS_POR_TIPO) {
                    int quedan = Math.max(0, MAX_BOLETAS_POR_TIPO - existentes);
                    throw new IllegalArgumentException("Ya hay " + existentes + " boletas " + tipo.getEtiqueta()
                            + " del " + fecha
                            + " de un máximo de " + MAX_BOLETAS_POR_TIPO + ". "
                            + (quedan == 0 ? "No se pueden registrar más."
                            : "Solo se pueden registrar " + quedan + " más y estás intentando " + cantidad + "."));
                }
            }
        }
    }

    /**
     * Cambia el numero y la fecha de entrega de una boleta (el numero puede quedar en null = sin
     * numero). Respeta el limite de MAX_USOS_POR_NUMERO contando las demas boletas del mismo tipo.
     */
    public static void actualizar(int id, String numero, LocalDate fecha) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                TipoBoleta tipo;
                String antes;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT b.tipo, b.numero, b.fecha_entrega, v.nombre FROM boletas b"
                                + " JOIN vendedoras v ON v.id = b.vendedora_id WHERE b.id = ?")) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("La boleta ya no existe");
                        }
                        tipo = TipoBoleta.valueOf(rs.getString(1));
                        antes = "boleta " + tipo.getEtiqueta() + " de " + rs.getString(4) + ": número "
                                + (rs.getString(2) == null ? "sin número" : rs.getString(2)) + ", entrega "
                                + rs.getString(3);
                    }
                }
                if (numero != null) {
                    verificarLimiteDeUsos(c, tipo, List.of(numero), id, fecha);
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE boletas SET numero = ?, fecha_entrega = ? WHERE id = ?")) {
                    ps.setString(1, numero);
                    ps.setString(2, fecha.toString());
                    ps.setInt(3, id);
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, "Boleta editada", "Era " + antes + ". Ahora: número "
                        + (numero == null ? "sin número" : numero) + ", entrega " + fecha);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo actualizar la boleta", e);
        }
    }

    /**
     * Pasa boletas a otra vendedora: como la boleta es una sola fila, la vendedora anterior deja de
     * tenerla automaticamente. Solo mueve las que siguen "ENTREGADA" (las ya liquidadas no se tocan,
     * porque ya generaron ahorro). Devuelve cuantas se movieron de verdad.
     */
    public static int mover(List<Integer> ids, int vendedoraDestinoId) {
        if (ids.isEmpty()) {
            return 0;
        }
        String marcadores = HistorialDAO.marcadores(ids.size());
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                // Se lee quien las tenia antes de moverlas, para dejarlo anotado en el historial.
                List<String> descripciones = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT b.numero, b.tipo, v.nombre FROM boletas b JOIN vendedoras v ON v.id = b.vendedora_id"
                                + " WHERE b.estado = 'ENTREGADA' AND b.vendedora_id <> ? AND b.id IN (" + marcadores
                                + ") ORDER BY v.nombre, b.numero")) {
                    ps.setInt(1, vendedoraDestinoId);
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 2, ids.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String numero = rs.getString(1) == null ? "sin número" : rs.getString(1);
                            descripciones.add(numero + " " + TipoBoleta.valueOf(rs.getString(2)).getEtiqueta()
                                    + " (de " + rs.getString(3) + ")");
                        }
                    }
                }
                int movidas;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE boletas SET vendedora_id = ? WHERE estado = 'ENTREGADA' AND vendedora_id <> ?"
                                + " AND id IN (" + marcadores + ")")) {
                    ps.setInt(1, vendedoraDestinoId);
                    ps.setInt(2, vendedoraDestinoId);
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 3, ids.get(i));
                    }
                    movidas = ps.executeUpdate();
                }
                if (movidas > 0) {
                    HistorialDAO.registrar(c, "Boletas movidas", movidas + " boleta(s) pasaron a "
                            + VendedoraDAO.nombrePorId(c, vendedoraDestinoId) + ": "
                            + HistorialDAO.resumir(descripciones));
                }
                c.commit();
                return movidas;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron mover las boletas", e);
        }
    }

    /**
     * Comprueba que, sumando los numeros nuevos a los ya registrados del mismo tipo, ninguno pase
     * de MAX_USOS_POR_NUMERO. Si alguno se pasa, lanza IllegalArgumentException con un mensaje
     * que dice cuantas veces esta en uso y a que vendedoras esta asignado. "excluirId" deja por
     * fuera una boleta (la que se esta editando); SIN_EXCLUIR para no excluir ninguna.
     */
    private static void verificarLimiteDeUsos(Connection c, TipoBoleta tipo, List<String> numeros, int excluirId,
                                              LocalDate fecha) throws SQLException {
        Map<String, Integer> nuevos = new LinkedHashMap<>();
        for (String numero : numeros) {
            nuevos.merge(numero, 1, Integer::sum);
        }

        String parametros = String.join(",", nuevos.keySet().stream().map(n -> "?").toArray(String[]::new));
        String sql = "SELECT b.numero, v.nombre FROM boletas b JOIN vendedoras v ON v.id = b.vendedora_id"
                + " WHERE b.tipo = ? AND b.id <> ? AND b.fecha_entrega = ?"
                + " AND b.numero IN (" + parametros + ") ORDER BY b.numero, v.nombre";
        Map<String, List<String>> titulares = new HashMap<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            ps.setInt(2, excluirId);
            int i = 3;
            ps.setString(i++, fecha.toString());
            for (String numero : nuevos.keySet()) {
                ps.setString(i++, numero);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    titulares.computeIfAbsent(rs.getString(1), k -> new ArrayList<>()).add(rs.getString(2));
                }
            }
        }

        List<String> problemas = new ArrayList<>();
        for (Map.Entry<String, Integer> entrada : nuevos.entrySet()) {
            String numero = entrada.getKey();
            int agregando = entrada.getValue();
            List<String> actuales = titulares.getOrDefault(numero, List.of());
            if (actuales.size() + agregando <= MAX_USOS_POR_NUMERO) {
                continue;
            }
            if (actuales.isEmpty()) {
                problemas.add("El número " + numero + " (" + tipo.getEtiqueta() + ") aparece " + agregando
                        + " veces en esta entrega; solo se puede repetir hasta " + MAX_USOS_POR_NUMERO + " veces.");
            } else {
                int cupo = MAX_USOS_POR_NUMERO - actuales.size();
                problemas.add("El número " + numero + " (" + tipo.getEtiqueta()
                        + ", " + fecha + ") ya está en uso "
                        + actuales.size() + " de " + MAX_USOS_POR_NUMERO + " veces, asignado a: "
                        + describirTitulares(actuales) + ". "
                        + (cupo == 0 ? "Ya no admite más."
                        : "Solo admite " + cupo + " más y lo estás agregando " + agregando + " veces."));
            }
        }
        if (!problemas.isEmpty()) {
            throw new IllegalArgumentException(String.join("\n", problemas));
        }
    }

    /** "Ana, Beatriz (2)": agrupa los nombres repetidos indicando cuantas boletas tiene cada una. */
    private static String describirTitulares(List<String> nombres) {
        Map<String, Integer> conteo = new LinkedHashMap<>();
        for (String nombre : nombres) {
            conteo.merge(nombre, 1, Integer::sum);
        }
        List<String> partes = new ArrayList<>();
        conteo.forEach((nombre, veces) -> partes.add(veces > 1 ? nombre + " (" + veces + ")" : nombre));
        return String.join(", ", partes);
    }

    /** Si ya se registro alguna boleta, en cualquier dia (sirve para saber si es la primera vez que se usa el programa). */
    public static boolean hayBoletas() {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM boletas LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next();
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo revisar si hay boletas", e);
        }
    }

    public static int contarEntregadasHoy() {
        String sql = "SELECT COUNT(*) FROM boletas WHERE fecha_entrega = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().toString());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo contar las boletas entregadas hoy", e);
        }
    }

    private static Boleta mapear(ResultSet rs) throws SQLException {
        return new Boleta(
                rs.getInt("id"),
                rs.getString("numero"),
                TipoBoleta.valueOf(rs.getString("tipo")),
                rs.getString("vendedora_nombre"),
                LocalDate.parse(rs.getString("fecha_entrega")),
                EstadoBoleta.valueOf(rs.getString("estado")));
    }
}
