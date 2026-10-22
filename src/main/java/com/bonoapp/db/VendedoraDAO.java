package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.Vendedora;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Acceso a datos de la tabla vendedoras. */
public final class VendedoraDAO {

    private VendedoraDAO() {
    }

    public static List<Vendedora> listar() {
        String sql = "SELECT id, nombre, documento, telefono, activa FROM vendedoras ORDER BY nombre";
        List<Vendedora> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener la lista de vendedoras", e);
        }
        return resultado;
    }

    public static List<Vendedora> listarActivas() {
        String sql = "SELECT id, nombre, documento, telefono, activa FROM vendedoras WHERE activa = 1 ORDER BY nombre";
        List<Vendedora> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(mapear(rs));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener la lista de vendedoras activas", e);
        }
        return resultado;
    }

    public static void insertar(Vendedora v) {
        String sql = "INSERT INTO vendedoras (nombre, documento, telefono, activa) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, v.getNombre());
                ps.setString(2, v.getDocumento());
                ps.setString(3, v.getTelefono());
                ps.setInt(4, v.isActiva() ? 1 : 0);
                ps.executeUpdate();
                HistorialDAO.registrar(c, "Vendedora creada", v.getNombre() + " (doc. " + v.getDocumento() + ")");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo guardar la vendedora", e);
        }
    }

    public static void actualizar(Vendedora v) {
        String sql = "UPDATE vendedoras SET nombre = ?, documento = ?, telefono = ?, activa = ? WHERE id = ?";
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String antes = nombrePorId(c, v.getId());
                ps.setString(1, v.getNombre());
                ps.setString(2, v.getDocumento());
                ps.setString(3, v.getTelefono());
                ps.setInt(4, v.isActiva() ? 1 : 0);
                ps.setInt(5, v.getId());
                ps.executeUpdate();
                HistorialDAO.registrar(c, "Vendedora editada", antes + (antes.equals(v.getNombre()) ? "" : " → "
                        + v.getNombre()) + " (doc. " + v.getDocumento() + ", " + (v.isActiva() ? "activa" : "inactiva") + ")");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo actualizar la vendedora", e);
        }
    }

    public static void eliminar(int id) {
        String sql = "DELETE FROM vendedoras WHERE id = ?";
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                String nombre = nombrePorId(c, id);
                ps.setInt(1, id);
                ps.executeUpdate();
                HistorialDAO.registrar(c, "Vendedora eliminada", nombre);
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo eliminar la vendedora", e);
        }
    }

    /** Nombre de una vendedora, para describirla en el historial y los mensajes; "?" si ya no existe. */
    static String nombrePorId(Connection c, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT nombre FROM vendedoras WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : "?";
            }
        }
    }

    private static Vendedora mapear(ResultSet rs) throws SQLException {
        return new Vendedora(
                rs.getInt("id"),
                rs.getString("nombre"),
                rs.getString("documento"),
                rs.getString("telefono"),
                rs.getInt("activa") == 1);
    }
}
