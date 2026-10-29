package com.bonoapp.db;

import com.bonoapp.model.TipoBoleta;
import com.bonoapp.util.ErrorRegistrado;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tratos: los numeros de boleta que una vendedora siempre vende. Son independientes por sorteo: los de la
 * Diaria y los del Premio mayor se guardan y se editan por separado.
 */
public final class TratoDAO {

    private TratoDAO() {
    }

    /** Los tratos de una vendedora para un tipo de boleta. */
    public static List<String> listarNumeros(int vendedoraId, TipoBoleta tipo) {
        String sql = "SELECT numero FROM tratos WHERE vendedora_id = ? AND tipo = ? ORDER BY numero, id";
        List<String> numeros = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, vendedoraId);
            ps.setString(2, tipo.name());
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

    /** Reemplaza los tratos de UN tipo (un numero puede aparecer mas de una vez); los del otro tipo no se tocan. */
    public static void reemplazar(int vendedoraId, TipoBoleta tipo, List<String> numeros) {
        reemplazar(vendedoraId, Map.of(tipo, numeros));
    }

    /**
     * Reemplaza los tratos de los tipos indicados, todos en una sola transaccion (si algo falla no queda a medias).
     * Los tipos que no aparecen en el mapa no se tocan.
     */
    public static void reemplazar(int vendedoraId, Map<TipoBoleta, List<String>> numerosPorTipo) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                for (Map.Entry<TipoBoleta, List<String>> entrada : numerosPorTipo.entrySet()) {
                    TipoBoleta tipo = entrada.getKey();
                    List<String> numeros = entrada.getValue();
                    try (PreparedStatement borrar = c.prepareStatement(
                            "DELETE FROM tratos WHERE vendedora_id = ? AND tipo = ?")) {
                        borrar.setInt(1, vendedoraId);
                        borrar.setString(2, tipo.name());
                        borrar.executeUpdate();
                    }
                    try (PreparedStatement insertar = c.prepareStatement(
                            "INSERT INTO tratos (vendedora_id, numero, tipo) VALUES (?, ?, ?)")) {
                        for (String numero : numeros) {
                            insertar.setInt(1, vendedoraId);
                            insertar.setString(2, numero);
                            insertar.setString(3, tipo.name());
                            insertar.addBatch();
                        }
                        insertar.executeBatch();
                    }
                    HistorialDAO.registrar(c, "Tratos actualizados", VendedoraDAO.nombrePorId(c, vendedoraId)
                            + " · " + tipo.getEtiqueta() + ": "
                            + (numeros.isEmpty() ? "sin tratos" : HistorialDAO.resumir(numeros)));
                }
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron guardar los tratos", e);
        }
    }
}
