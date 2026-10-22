package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.CierreDia;
import com.bonoapp.model.Pendiente;
import com.bonoapp.model.ResumenVendedora;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Consultas para los reportes. */
public final class ReporteDAO {

    private ReporteDAO() {
    }

    /**
     * Resumen por vendedora entre dos fechas (incluidas). Las boletas entregadas (y las que siguen sin
     * liquidar) se cuentan por fecha de entrega; lo vendido, devuelto y recaudado, por la fecha de cada
     * liquidacion (sin contar las revertidas); el ahorro, los prestamos y los abonos, por la fecha en
     * que se registraron. Solo devuelve las vendedoras que tuvieron alguna actividad en el periodo.
     */
    public static List<ResumenVendedora> resumenPorVendedora(LocalDate desde, LocalDate hasta) {
        String sql = """
                SELECT v.nombre, v.documento,
                       COALESCE(b.entregadas, 0) AS entregadas,
                       COALESCE(l.vendidas, 0)   AS vendidas,
                       COALESCE(l.devueltas, 0)  AS devueltas,
                       COALESCE(b.pendientes, 0) AS pendientes,
                       COALESCE(l.recaudado, 0)  AS recaudado,
                       COALESCE(a.total, 0)      AS ahorro,
                       COALESCE(p.total, 0)      AS prestado,
                       COALESCE(ab.total, 0)     AS abonos
                FROM vendedoras v
                LEFT JOIN (SELECT vendedora_id,
                                  COUNT(*) AS entregadas,
                                  SUM(estado = 'ENTREGADA') AS pendientes
                           FROM boletas
                           WHERE fecha_entrega BETWEEN ? AND ?
                           GROUP BY vendedora_id) b ON b.vendedora_id = v.id
                LEFT JOIN (SELECT vendedora_id,
                                  SUM(vendidas)  AS vendidas,
                                  SUM(devueltas) AS devueltas,
                                  SUM(recaudado) AS recaudado
                           FROM liquidaciones
                           WHERE anulada = 0 AND fecha BETWEEN ? AND ?
                           GROUP BY vendedora_id) l ON l.vendedora_id = v.id
                LEFT JOIN (SELECT vendedora_id, SUM(monto) AS total
                           FROM ahorros
                           WHERE fecha BETWEEN ? AND ?
                           GROUP BY vendedora_id) a ON a.vendedora_id = v.id
                LEFT JOIN (SELECT vendedora_id, SUM(monto) AS total
                           FROM prestamos
                           WHERE fecha BETWEEN ? AND ?
                           GROUP BY vendedora_id) p ON p.vendedora_id = v.id
                LEFT JOIN (SELECT pr.vendedora_id, SUM(a.monto) AS total
                           FROM abonos a
                           JOIN prestamos pr ON pr.id = a.prestamo_id
                           WHERE a.fecha BETWEEN ? AND ?
                           GROUP BY pr.vendedora_id) ab ON ab.vendedora_id = v.id
                ORDER BY v.nombre
                """;
        List<ResumenVendedora> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < 5; i++) {
                ps.setString(i * 2 + 1, desde.toString());
                ps.setString(i * 2 + 2, hasta.toString());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ResumenVendedora fila = new ResumenVendedora(rs.getString("nombre"), rs.getString("documento"),
                            rs.getInt("entregadas"), rs.getInt("vendidas"), rs.getInt("devueltas"),
                            rs.getInt("pendientes"), rs.getInt("recaudado"), rs.getInt("ahorro"),
                            rs.getInt("prestado"), rs.getInt("abonos"));
                    if (!fila.sinActividad()) {
                        resultado.add(fila);
                    }
                }
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo generar el reporte", e);
        }
        return resultado;
    }

    /**
     * El cierre de un dia, para cuadrar la caja: lo liquidado ese dia (por la fecha de la liquidacion, sin las
     * revertidas) y cuantas boletas entregadas ese mismo dia todavia no se han liquidado.
     */
    public static CierreDia cierreDelDia(LocalDate dia) {
        String liquidado = """
                SELECT COUNT(*), COALESCE(SUM(vendidas), 0), COALESCE(SUM(devueltas), 0), COALESCE(SUM(recaudado), 0)
                FROM liquidaciones WHERE anulada = 0 AND fecha = ?
                """;
        String pendientes = "SELECT COUNT(*) FROM boletas WHERE estado = 'ENTREGADA' AND fecha_entrega = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps1 = c.prepareStatement(liquidado);
             PreparedStatement ps2 = c.prepareStatement(pendientes)) {
            ps1.setString(1, dia.toString());
            ps2.setString(1, dia.toString());
            try (ResultSet r1 = ps1.executeQuery(); ResultSet r2 = ps2.executeQuery()) {
                r1.next();
                r2.next();
                return new CierreDia(r1.getInt(1), r1.getInt(2), r1.getInt(3), r1.getInt(4), r2.getInt(1));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo calcular el cierre del dia", e);
        }
    }

    /**
     * Lo que queda pendiente hoy, por vendedora: boletas entregadas sin liquidar (con la fecha de la mas
     * antigua) y lo que deben de prestamos. Primero las que llevan mas tiempo con boletas sin liquidar.
     */
    public static List<Pendiente> pendientesPorVendedora() {
        String sql = """
                SELECT v.nombre, COALESCE(b.cantidad, 0) AS cantidad, b.desde, COALESCE(p.debe, 0) AS debe
                FROM vendedoras v
                LEFT JOIN (SELECT vendedora_id, COUNT(*) AS cantidad, MIN(fecha_entrega) AS desde
                           FROM boletas WHERE estado = 'ENTREGADA' GROUP BY vendedora_id) b ON b.vendedora_id = v.id
                LEFT JOIN (SELECT pr.vendedora_id, SUM(pr.monto) - COALESCE(SUM(ab.total), 0) AS debe
                           FROM prestamos pr
                           LEFT JOIN (SELECT prestamo_id, SUM(monto) AS total FROM abonos GROUP BY prestamo_id) ab
                                  ON ab.prestamo_id = pr.id
                           GROUP BY pr.vendedora_id) p ON p.vendedora_id = v.id
                WHERE COALESCE(b.cantidad, 0) > 0 OR COALESCE(p.debe, 0) > 0
                ORDER BY b.desde IS NULL, b.desde, v.nombre
                """;
        List<Pendiente> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                String desde = rs.getString("desde");
                resultado.add(new Pendiente(rs.getString("nombre"), rs.getInt("cantidad"),
                        desde == null ? null : LocalDate.parse(desde), rs.getInt("debe")));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo calcular lo pendiente", e);
        }
        return resultado;
    }
}
