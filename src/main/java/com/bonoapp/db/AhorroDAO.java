package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.Ahorro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Acceso a datos de la tabla ahorros: los movimientos de ahorro de cada vendedora. Se suma dinero al
 * liquidar boletas o al registrar un ahorro voluntario, y se resta al registrar un retiro.
 */
public final class AhorroDAO {

    private AhorroDAO() {
    }

    /** Inserta un movimiento de ahorro dentro de una transaccion ya abierta (ver LiquidacionDAO). */
    static void insertar(Connection c, int vendedoraId, int monto, LocalDate fecha, String concepto)
            throws SQLException {
        String sql = "INSERT INTO ahorros (vendedora_id, monto, fecha, concepto) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendedoraId);
            ps.setInt(2, monto);
            ps.setString(3, fecha.toString());
            ps.setString(4, concepto);
            ps.executeUpdate();
        }
    }

    /** Todos los movimientos de ahorro, del mas reciente al mas antiguo. */
    public static List<Ahorro> listar() {
        String sql = """
                SELECT a.id, v.nombre, v.documento, a.monto, a.concepto, a.fecha
                FROM ahorros a
                JOIN vendedoras v ON v.id = a.vendedora_id
                ORDER BY a.fecha DESC, a.id DESC
                """;
        List<Ahorro> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new Ahorro(rs.getInt("id"), rs.getString("nombre"), rs.getString("documento"),
                        rs.getInt("monto"), rs.getString("concepto"), LocalDate.parse(rs.getString("fecha"))));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener la lista de ahorros", e);
        }
        return resultado;
    }

    /** Suma de los ahorros de todas las vendedoras (ya descontados los retiros). */
    public static int totalAcumulado() {
        String sql = "SELECT COALESCE(SUM(monto), 0) FROM ahorros";
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo calcular el ahorro acumulado", e);
        }
    }

    /** Lo que una vendedora tiene ahorrado ahora: ahorros menos retiros. */
    public static int saldoDe(int vendedoraId) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COALESCE(SUM(monto), 0) FROM ahorros WHERE vendedora_id = ?")) {
            ps.setInt(1, vendedoraId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo calcular el ahorro de la vendedora", e);
        }
    }

    /** Registra un ahorro voluntario ingresado manualmente para una vendedora. */
    public static void registrarManual(int vendedoraId, int monto, LocalDate fecha, String concepto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                insertar(c, vendedoraId, monto, fecha, concepto);
                HistorialDAO.registrar(c, "Ahorro registrado", VendedoraDAO.nombrePorId(c, vendedoraId) + ": "
                        + HistorialDAO.pesos(monto) + " · " + concepto + " (" + fecha + ")");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo registrar el ahorro", e);
        }
    }

    /**
     * Registra un retiro del ahorro de una vendedora. Lanza IllegalArgumentException si el monto
     * no es valido o si es mayor que lo que ella tiene ahorrado.
     */
    public static void retirar(int vendedoraId, int monto, LocalDate fecha, String concepto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                int ahorrado;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT COALESCE(SUM(monto), 0) FROM ahorros WHERE vendedora_id = ?")) {
                    ps.setInt(1, vendedoraId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        ahorrado = rs.getInt(1);
                    }
                }
                if (monto > ahorrado) {
                    NumberFormat pesos = NumberFormat.getNumberInstance(new Locale("es", "CO"));
                    throw new IllegalArgumentException("Esta vendedora solo tiene $ " + pesos.format(ahorrado)
                            + " ahorrado y quieres retirar $ " + pesos.format(monto));
                }
                insertar(c, vendedoraId, -monto, fecha, concepto);
                HistorialDAO.registrar(c, "Retiro de ahorro", VendedoraDAO.nombrePorId(c, vendedoraId) + ": "
                        + HistorialDAO.pesos(monto) + " · " + concepto + " (" + fecha + ")");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo registrar el retiro", e);
        }
    }

    public static void eliminarVarios(List<Integer> ids) {
        if (ids.isEmpty()) {
            return;
        }
        String marcadores = HistorialDAO.marcadores(ids.size());
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                List<String> descripciones = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT v.nombre, a.monto, a.fecha FROM ahorros a"
                                + " JOIN vendedoras v ON v.id = a.vendedora_id WHERE a.id IN (" + marcadores
                                + ") ORDER BY a.fecha")) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            int monto = rs.getInt(2);
                            descripciones.add(rs.getString(1) + " " + (monto < 0 ? "retiro " : "ahorro ")
                                    + HistorialDAO.pesos(Math.abs(monto)) + " (" + rs.getString(3) + ")");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM ahorros WHERE id IN (" + marcadores + ")")) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, "Movimientos de ahorro eliminados", HistorialDAO.resumir(descripciones));
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron eliminar los movimientos de ahorro", e);
        }
    }
}
