package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Tratos: los numeros de boleta que una vendedora siempre vende. */
public final class TratoDAO {

    private TratoDAO() {
    }

    public static List<String> listarNumeros(int vendedoraId) {
        String sql = "SELECT numero FROM tratos WHERE vendedora_id = ? ORDER BY numero, id";
        List<String> numeros = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendedoraId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    numeros.add(rs.getString(1));
                }
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron obtener los tratos", e);
        }
        return numeros;
    }

    /** Reemplaza todos los tratos de la vendedora por la lista dada (un numero puede aparecer mas de una vez). */
    public static void reemplazar(int vendedoraId, List<String> numeros) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement borrar = c.prepareStatement("DELETE FROM tratos WHERE vendedora_id = ?")) {
                    borrar.setInt(1, vendedoraId);
                    borrar.executeUpdate();
                }
                try (PreparedStatement insertar = c.prepareStatement(
                        "INSERT INTO tratos (vendedora_id, numero) VALUES (?, ?)")) {
                    for (String numero : numeros) {
                        insertar.setInt(1, vendedoraId);
                        insertar.setString(2, numero);
                        insertar.addBatch();
                    }
                    insertar.executeBatch();
                }
                HistorialDAO.registrar(c, "Tratos actualizados", VendedoraDAO.nombrePorId(c, vendedoraId) + ": "
                        + (numeros.isEmpty() ? "sin tratos" : HistorialDAO.resumir(numeros)));
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron guardar los tratos", e);
        }
    }
}
