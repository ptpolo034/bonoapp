package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.Prestamo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de los prestamos (dinero prestado a las vendedoras). */
public final class PrestamoDAO {

    private PrestamoDAO() {
    }

    public static List<Prestamo> listar() {
        String sql = """
                SELECT p.id, v.nombre, v.documento, p.monto, p.concepto, p.fecha,
                       COALESCE((SELECT SUM(a.monto) FROM abonos a WHERE a.prestamo_id = p.id), 0) AS abonado
                FROM prestamos p
                JOIN vendedoras v ON v.id = p.vendedora_id
                ORDER BY p.fecha DESC, p.id DESC
                """;
        List<Prestamo> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new Prestamo(rs.getInt("id"), rs.getString("nombre"), rs.getString("documento"),
                        rs.getInt("monto"), rs.getInt("abonado"), rs.getString("concepto"),
                        LocalDate.parse(rs.getString("fecha"))));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener la lista de prestamos", e);
        }
        return resultado;
    }

    public static void insertar(int vendedoraId, int monto, String concepto, LocalDate fecha) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a cero");
        }
        if (concepto == null || concepto.isBlank()) {
            throw new IllegalArgumentException("El concepto es obligatorio");
        }
        String sql = "INSERT INTO prestamos (vendedora_id, monto, concepto, fecha) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, vendedoraId);
                ps.setInt(2, monto);
                ps.setString(3, concepto.trim());
                ps.setString(4, fecha.toString());
                ps.executeUpdate();
                HistorialDAO.registrar(c, "Préstamo registrado", VendedoraDAO.nombrePorId(c, vendedoraId) + ": "
                        + HistorialDAO.pesos(monto) + " · " + concepto.trim() + " (" + fecha + ")");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo registrar el prestamo", e);
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
                        "SELECT v.nombre, p.monto, p.concepto, p.fecha,"
                                + " (SELECT COALESCE(SUM(a.monto), 0) FROM abonos a WHERE a.prestamo_id = p.id)"
                                + " FROM prestamos p JOIN vendedoras v ON v.id = p.vendedora_id"
                                + " WHERE p.id IN (" + marcadores + ") ORDER BY p.fecha")) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            descripciones.add(rs.getString(1) + " " + HistorialDAO.pesos(rs.getInt(2)) + " · "
                                    + rs.getString(3) + " (" + rs.getString(4) + ", abonado "
                                    + HistorialDAO.pesos(rs.getInt(5)) + ")");
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM prestamos WHERE id IN (" + marcadores + ")")) {
                    for (int i = 0; i < ids.size(); i++) {
                        ps.setInt(i + 1, ids.get(i));
                    }
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, "Préstamos eliminados", HistorialDAO.resumir(descripciones));
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron eliminar los prestamos", e);
        }
    }

    /** Lo que todavia se debe en total: todo lo prestado menos todo lo abonado. */
    public static int totalPendiente() {
        String sql = """
                SELECT COALESCE((SELECT SUM(monto) FROM prestamos), 0)
                     - COALESCE((SELECT SUM(monto) FROM abonos), 0)
                """;
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo calcular el saldo de los prestamos", e);
        }
    }
}
