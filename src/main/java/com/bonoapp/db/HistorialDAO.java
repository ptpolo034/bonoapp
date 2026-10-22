package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.EntradaHistorial;
import com.bonoapp.util.Sesion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Historial de cambios. Los DAOs anotan aqui cada operacion que modifica datos, para poder saber despues
 * que paso, cuando y con que usuario. Los datos borrados ya no estan, pero queda constancia de su descripcion.
 */
public final class HistorialDAO {

    private static final int MAX_ELEMENTOS_EN_DETALLE = 12;

    private HistorialDAO() {
    }

    /** Anota un cambio dentro de una transaccion ya abierta: si la operacion se revierte, la nota tambien. */
    static void registrar(Connection c, String accion, String detalle) throws SQLException {
        String sql = "INSERT INTO historial (fecha_hora, usuario, accion, detalle) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, LocalDateTime.now().withNano(0).toString());
            ps.setString(2, Sesion.getUsuario());
            ps.setString(3, accion);
            // Las fechas se guardan como AAAA-MM-DD en la base; en el historial se muestran como DD/MM/AAAA.
            ps.setString(4, detalle == null ? null : detalle.replaceAll("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b", "$3/$2/$1"));
            ps.executeUpdate();
        }
    }

    /** Los ultimos movimientos, del mas reciente al mas antiguo. */
    public static List<EntradaHistorial> listar(int limite) {
        String sql = "SELECT fecha_hora, usuario, accion, detalle FROM historial ORDER BY id DESC LIMIT ?";
        List<EntradaHistorial> resultado = new ArrayList<>();
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(new EntradaHistorial(LocalDateTime.parse(rs.getString("fecha_hora")),
                            rs.getString("usuario"), rs.getString("accion"), rs.getString("detalle")));
                }
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener el historial", e);
        }
        return resultado;
    }

    /** "010, 011, 012 y 5 más": para que el detalle no sea enorme al tocar cientos de boletas a la vez. */
    static String resumir(List<String> elementos) {
        if (elementos.size() <= MAX_ELEMENTOS_EN_DETALLE) {
            return String.join(", ", elementos);
        }
        return String.join(", ", elementos.subList(0, MAX_ELEMENTOS_EN_DETALLE))
                + " y " + (elementos.size() - MAX_ELEMENTOS_EN_DETALLE) + " más";
    }

    static String pesos(int monto) {
        return "$ " + NumberFormat.getNumberInstance(new Locale("es", "CO")).format(monto);
    }

    /** Signos de interrogacion separados por coma para una clausula IN con "n" valores. */
    static String marcadores(int n) {
        return String.join(",", java.util.Collections.nCopies(n, "?"));
    }
}
