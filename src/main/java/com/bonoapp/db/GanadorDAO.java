package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.Boleta;
import com.bonoapp.model.NumeroGanador;
import com.bonoapp.model.TipoBoleta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Numeros ganadores de los sorteos (uno por tipo de boleta y fecha). */
public final class GanadorDAO {

    private GanadorDAO() {
    }

    /**
     * Guarda el numero ganador de un sorteo. Si ya habia uno para ese tipo y esa fecha, lo reemplaza
     * (por si se escribio mal). El numero debe tener exactamente 3 cifras.
     */
    public static void guardar(TipoBoleta tipo, LocalDate fecha, String numero) {
        if (numero == null || !numero.matches("\\d{3}")) {
            throw new IllegalArgumentException("El número ganador debe tener 3 cifras (ej: 007)");
        }
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try {
                String anterior = null;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT numero FROM ganadores WHERE tipo = ? AND fecha = ?")) {
                    ps.setString(1, tipo.name());
                    ps.setString(2, fecha.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            anterior = rs.getString(1);
                        }
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO ganadores (tipo, fecha, numero) VALUES (?, ?, ?)"
                                + " ON CONFLICT (tipo, fecha) DO UPDATE SET numero = excluded.numero")) {
                    ps.setString(1, tipo.name());
                    ps.setString(2, fecha.toString());
                    ps.setString(3, numero);
                    ps.executeUpdate();
                }
                HistorialDAO.registrar(c, anterior == null ? "Número ganador" : "Número ganador corregido",
                        tipo.getEtiqueta() + " del " + fecha + ": " + numero
                                + (anterior == null ? "" : " (antes " + anterior + ")"));
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo guardar el número ganador", e);
        }
    }

    /** El ultimo numero ganador registrado de un tipo, o null si todavia no hay ninguno. */
    public static NumeroGanador ultimo(TipoBoleta tipo) {
        String sql = "SELECT id, fecha, numero FROM ganadores WHERE tipo = ? ORDER BY fecha DESC, id DESC LIMIT 1";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next()
                        ? new NumeroGanador(rs.getInt("id"), tipo, LocalDate.parse(rs.getString("fecha")),
                                rs.getString("numero"))
                        : null;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener el número ganador", e);
        }
    }

    /**
     * Quienes tienen una boleta con ese numero en ese tipo para el sorteo de esa fecha (como maximo 2: es el
     * limite de usos por numero en un dia). Las boletas se entregan por dia, y el numero puede repetirse en
     * otros dias, por eso hay que mirar solo la fecha del sorteo.
     */
    public static List<Boleta> titulares(TipoBoleta tipo, String numero, LocalDate fechaSorteo) {
        List<Boleta> titulares = new ArrayList<>();
        for (Boleta b : BoletaDAO.buscar(numero, fechaSorteo, fechaSorteo)) {
            if (b.getTipo() == tipo) {
                titulares.add(b);
            }
        }
        return titulares;
    }
}
