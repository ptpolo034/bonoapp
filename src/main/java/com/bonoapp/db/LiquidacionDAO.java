package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.EstadoBoleta;
import com.bonoapp.model.Liquidacion;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.util.Preferencias;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orquesta la liquidacion de boletas de una vendedora: marca cuales se vendieron y cuales se
 * devolvieron, en una sola transaccion, y deja la liquidacion registrada (fecha, cantidades, precio y
 * recaudado de ese momento). Una liquidacion se puede revertir. El ahorro NO se acredita aqui: se
 * registra a mano en Ahorros.
 */
public final class LiquidacionDAO {

    private LiquidacionDAO() {
    }

    /** Lo recaudado es el precio de las boletas vendidas, segun el precio configurado para ese tipo. */
    public record Resultado(int cantidadVendida, int cantidadDevuelta, int recaudado) {
    }

    public static Resultado liquidar(TipoBoleta tipo, int vendedoraId, List<Integer> idsVendidos,
                                      List<Integer> idsDevueltos) {
        int precio = Preferencias.getPrecio(tipo);
        LocalDate fecha = LocalDate.now();

        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                int liquidacionId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO liquidaciones (vendedora_id, tipo, fecha, precio) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, vendedoraId);
                    ps.setString(2, tipo.name());
                    ps.setString(3, fecha.toString());
                    ps.setInt(4, precio);
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        liquidacionId = rs.getInt(1);
                    }
                }

                // marcarEstado solo toca boletas que de verdad son de esta vendedora, de este tipo y
                // que siguen "ENTREGADA": si alguno de los ids ya no coincide (por un error de la
                // interfaz o una liquidacion doble), simplemente no se cuenta, en vez de liquidar por
                // accidente la boleta de otra persona.
                int vendidas = BoletaDAO.marcarEstado(c, idsVendidos, EstadoBoleta.VENDIDA, vendedoraId, tipo,
                        liquidacionId);
                int devueltas = BoletaDAO.marcarEstado(c, idsDevueltos, EstadoBoleta.DEVUELTA, vendedoraId, tipo,
                        liquidacionId);
                if (vendidas + devueltas == 0) {
                    throw new IllegalArgumentException("Ninguna de esas boletas está pendiente de liquidar");
                }

                int recaudado = vendidas * precio;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE liquidaciones SET vendidas = ?, devueltas = ?, recaudado = ? WHERE id = ?")) {
                    ps.setInt(1, vendidas);
                    ps.setInt(2, devueltas);
                    ps.setInt(3, recaudado);
                    ps.setInt(4, liquidacionId);
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, "Liquidación",
                        VendedoraDAO.nombrePorId(c, vendedoraId) + " · " + tipo.getEtiqueta() + ": " + vendidas
                                + " vendida(s), " + devueltas + " devuelta(s), recaudado "
                                + HistorialDAO.pesos(recaudado) + " (a " + HistorialDAO.pesos(precio) + " c/u)");
                c.commit();
                return new Resultado(vendidas, devueltas, recaudado);
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo liquidar las boletas", e);
        }
    }

    /**
     * Deshace una liquidacion: sus boletas vuelven a quedar pendientes ("Entregada") y la liquidacion
     * queda marcada como anulada (no se borra, para que conste en el historial y no cuente en reportes).
     * Devuelve cuantas boletas volvieron a pendientes (pueden ser menos si alguna se elimino despues).
     */
    public static int revertir(int liquidacionId) {
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                String descripcion;
                try (PreparedStatement ps = c.prepareStatement("""
                        SELECT v.nombre, l.tipo, l.fecha, l.vendidas, l.devueltas, l.recaudado, l.anulada
                        FROM liquidaciones l JOIN vendedoras v ON v.id = l.vendedora_id WHERE l.id = ?
                        """)) {
                    ps.setInt(1, liquidacionId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException("La liquidación ya no existe");
                        }
                        if (rs.getInt("anulada") == 1) {
                            throw new IllegalArgumentException("Esta liquidación ya fue revertida");
                        }
                        descripcion = rs.getString("nombre") + " · "
                                + TipoBoleta.valueOf(rs.getString("tipo")).getEtiqueta() + " del "
                                + rs.getString("fecha") + ": " + rs.getInt("vendidas") + " vendida(s), "
                                + rs.getInt("devueltas") + " devuelta(s), recaudado "
                                + HistorialDAO.pesos(rs.getInt("recaudado"));
                    }
                }
                int restauradas;
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE boletas SET estado = 'ENTREGADA', liquidacion_id = NULL WHERE liquidacion_id = ?")) {
                    ps.setInt(1, liquidacionId);
                    restauradas = ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE liquidaciones SET anulada = 1, anulada_fecha = ? WHERE id = ?")) {
                    ps.setString(1, LocalDate.now().toString());
                    ps.setInt(2, liquidacionId);
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, "Liquidación revertida",
                        descripcion + ". " + restauradas + " boleta(s) volvieron a pendientes");
                c.commit();
                return restauradas;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo revertir la liquidación", e);
        }
    }

    /** Todas las liquidaciones (incluidas las revertidas), de la mas reciente a la mas antigua. */
    public static List<Liquidacion> listar() {
        String sql = """
                SELECT l.id, v.nombre, v.documento, l.tipo, l.fecha, l.vendidas, l.devueltas, l.precio, l.recaudado, l.anulada
                FROM liquidaciones l
                JOIN vendedoras v ON v.id = l.vendedora_id
                ORDER BY l.fecha DESC, l.id DESC
                """;
        List<Liquidacion> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                resultado.add(new Liquidacion(rs.getInt("id"), rs.getString("nombre"), rs.getString("documento"),
                        TipoBoleta.valueOf(rs.getString("tipo")), LocalDate.parse(rs.getString("fecha")),
                        rs.getInt("vendidas"), rs.getInt("devueltas"), rs.getInt("precio"),
                        rs.getInt("recaudado"), rs.getInt("anulada") == 1));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudieron obtener las liquidaciones", e);
        }
        return resultado;
    }
}
