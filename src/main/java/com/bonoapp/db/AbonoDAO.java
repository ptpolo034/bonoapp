package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.Abono;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de los abonos (pagos a los prestamos). */
public final class AbonoDAO {

    private AbonoDAO() {
    }

    /**
     * Registra un abono a un prestamo. Lanza IllegalArgumentException si el monto no es valido
     * o si es mayor que lo que todavia se debe.
     */
    public static void registrar(int prestamoId, int monto, LocalDate fecha, String nota) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El abono debe ser mayor a cero");
        }
        String saldoSql = """
                SELECT p.monto - COALESCE((SELECT SUM(a.monto) FROM abonos a WHERE a.prestamo_id = p.id), 0)
                FROM prestamos p WHERE p.id = ?
                """;
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                int saldo;
                try (PreparedStatement ps = c.prepareStatement(saldoSql)) {
                    ps.setInt(1, prestamoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("El préstamo ya no existe");
                        }
                        saldo = rs.getInt(1);
                    }
                }
                if (monto > saldo) {
                    throw new IllegalArgumentException("El abono (" + monto + ") es mayor que el saldo del préstamo ("
                            + saldo + ")");
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO abonos (prestamo_id, monto, fecha, nota) VALUES (?, ?, ?, ?)")) {
                    ps.setInt(1, prestamoId);
                    ps.setInt(2, monto);
                    ps.setString(3, fecha.toString());
                    ps.setString(4, nota == null || nota.isBlank() ? null : nota.trim());
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, "Abono registrado", HistorialDAO.pesos(monto) + " al préstamo "
                        + descripcionPrestamo(c, prestamoId) + " (" + fecha + "). Saldo: "
                        + HistorialDAO.pesos(saldo - monto));
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo registrar el abono", e);
        }
    }

    public static List<Abono> listar(int prestamoId) {
        String sql = "SELECT id, monto, fecha, nota FROM abonos WHERE prestamo_id = ? ORDER BY fecha, id";
        List<Abono> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new Abono(rs.getInt("id"), rs.getInt("monto"),
                            LocalDate.parse(rs.getString("fecha")), rs.getString("nota")));
                }
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron obtener los abonos", e);
        }
        return resultado;
    }

    public static void eliminar(int abonoId) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                String descripcion = null;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT monto, fecha, prestamo_id FROM abonos WHERE id = ?")) {
                    ps.setInt(1, abonoId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            descripcion = HistorialDAO.pesos(rs.getInt(1)) + " del " + rs.getString(2)
                                    + " al préstamo " + descripcionPrestamo(c, rs.getInt(3));
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement("DELETE FROM abonos WHERE id = ?")) {
                    ps.setInt(1, abonoId);
                    ps.executeUpdate();
                }
                if (descripcion != null) {
                    HistorialDAO.registrar(c, "Abono eliminado", descripcion);
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo eliminar el abono", e);
        }
    }

    /** "Ana · Emergencia ($ 200.000)": para describir el prestamo de un abono en el historial. */
    private static String descripcionPrestamo(Connection c, int prestamoId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT v.nombre, p.concepto, p.monto FROM prestamos p"
                        + " JOIN vendedoras v ON v.id = p.vendedora_id WHERE p.id = ?")) {
            ps.setInt(1, prestamoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? rs.getString(1) + " · " + rs.getString(2) + " (" + HistorialDAO.pesos(rs.getInt(3)) + ")"
                        : "?";
            }
        }
    }
}
