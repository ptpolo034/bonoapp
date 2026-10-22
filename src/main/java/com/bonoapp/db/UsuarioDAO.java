package com.bonoapp.db;

import com.bonoapp.util.ErrorRegistrado;
import com.bonoapp.model.TipoRecuperacion;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Locale;

/** Acceso a datos del unico usuario administrador que protege la aplicacion. */
public final class UsuarioDAO {

    private static final int ITERACIONES = 65536;
    private static final int LONGITUD_LLAVE = 256;

    private UsuarioDAO() {
    }

    public static boolean existeUsuario() {
        String sql = "SELECT COUNT(*) FROM usuarios";
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1) > 0;
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo consultar los usuarios", e);
        }
    }

    public static String obtenerUsuario() {
        String sql = "SELECT usuario FROM usuarios LIMIT 1";
        try (Connection c = Database.conectar();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo obtener el usuario", e);
        }
    }

    public static void crear(String usuario, char[] contrasena) {
        byte[] sal = generarSal();
        byte[] hash = calcularHash(contrasena, sal);
        String sql = "INSERT INTO usuarios (usuario, hash, sal) VALUES (?, ?, ?)";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            ps.setString(2, Base64.getEncoder().encodeToString(hash));
            ps.setString(3, Base64.getEncoder().encodeToString(sal));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo crear el usuario", e);
        }
    }

    public static boolean validar(String usuario, char[] contrasena) {
        String sql = "SELECT hash, sal FROM usuarios WHERE usuario = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return false;
                }
                byte[] hashGuardado = Base64.getDecoder().decode(rs.getString("hash"));
                byte[] sal = Base64.getDecoder().decode(rs.getString("sal"));
                byte[] hashIngresado = calcularHash(contrasena, sal);
                return MessageDigest.isEqual(hashGuardado, hashIngresado);
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo validar el usuario", e);
        }
    }

    /** Cambia la contrasena solo si "actual" coincide con la guardada. Devuelve false si no coincide. */
    public static boolean cambiarContrasena(String usuario, char[] actual, char[] nueva) {
        if (!validar(usuario, actual)) {
            return false;
        }
        byte[] sal = generarSal();
        byte[] hash = calcularHash(nueva, sal);
        String sql = "UPDATE usuarios SET hash = ?, sal = ? WHERE usuario = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Base64.getEncoder().encodeToString(hash));
            ps.setString(2, Base64.getEncoder().encodeToString(sal));
            ps.setString(3, usuario);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo cambiar la contrasena", e);
        }
    }

    /** Cambia el nombre de usuario solo si "contrasenaActual" coincide con la guardada. */
    public static boolean cambiarUsuario(String usuarioActual, char[] contrasenaActual, String nuevoUsuario) {
        if (!validar(usuarioActual, contrasenaActual)) {
            return false;
        }
        String sql = "UPDATE usuarios SET usuario = ? WHERE usuario = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nuevoUsuario);
            ps.setString(2, usuarioActual);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo cambiar el usuario", e);
        }
    }

    /** Genera un nuevo codigo de recuperacion, lo guarda (cifrado) y lo devuelve en texto plano una sola vez. */
    public static String configurarRecuperacionConCodigo(String usuario) {
        String codigo = generarCodigo();
        guardarRecuperacion(usuario, TipoRecuperacion.CODIGO, null, normalizarCodigo(codigo));
        return codigo;
    }

    /** Configura una pregunta de seguridad como metodo de recuperacion. */
    public static void configurarRecuperacionConPregunta(String usuario, String pregunta, String respuesta) {
        guardarRecuperacion(usuario, TipoRecuperacion.PREGUNTA, pregunta, normalizarRespuesta(respuesta));
    }

    public static TipoRecuperacion obtenerTipoRecuperacion(String usuario) {
        String sql = "SELECT recuperacion_tipo FROM usuarios WHERE usuario = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getString(1) == null) {
                    return null;
                }
                return TipoRecuperacion.valueOf(rs.getString(1));
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo consultar el metodo de recuperacion", e);
        }
    }

    public static String obtenerPreguntaRecuperacion(String usuario) {
        String sql = "SELECT recuperacion_pregunta FROM usuarios WHERE usuario = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo consultar la pregunta de recuperacion", e);
        }
    }

    /** Valida el codigo o la respuesta de recuperacion (segun el tipo configurado para ese usuario). */
    public static boolean validarRecuperacion(String usuario, String codigoORespuesta) {
        String sql = "SELECT recuperacion_tipo, recuperacion_hash, recuperacion_sal FROM usuarios WHERE usuario = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, usuario);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || rs.getString("recuperacion_tipo") == null) {
                    return false;
                }
                TipoRecuperacion tipo = TipoRecuperacion.valueOf(rs.getString("recuperacion_tipo"));
                byte[] hashGuardado = Base64.getDecoder().decode(rs.getString("recuperacion_hash"));
                byte[] sal = Base64.getDecoder().decode(rs.getString("recuperacion_sal"));
                String normalizado = tipo == TipoRecuperacion.CODIGO
                        ? normalizarCodigo(codigoORespuesta)
                        : normalizarRespuesta(codigoORespuesta);
                byte[] hashIngresado = calcularHash(normalizado.toCharArray(), sal);
                return MessageDigest.isEqual(hashGuardado, hashIngresado);
            }
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo validar la recuperacion", e);
        }
    }

    /** Pone una contrasena nueva sin pedir la actual (se usa despues de validar la recuperacion). */
    public static void restablecerContrasena(String usuario, char[] nueva) {
        byte[] sal = generarSal();
        byte[] hash = calcularHash(nueva, sal);
        String sql = "UPDATE usuarios SET hash = ?, sal = ? WHERE usuario = ?";
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, Base64.getEncoder().encodeToString(hash));
            ps.setString(2, Base64.getEncoder().encodeToString(sal));
            ps.setString(3, usuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo restablecer la contrasena", e);
        }
    }

    private static void guardarRecuperacion(String usuario, TipoRecuperacion tipo, String pregunta,
                                             String valorNormalizado) {
        byte[] sal = generarSal();
        byte[] hash = calcularHash(valorNormalizado.toCharArray(), sal);
        String sql = """
                UPDATE usuarios
                SET recuperacion_tipo = ?, recuperacion_pregunta = ?, recuperacion_hash = ?, recuperacion_sal = ?
                WHERE usuario = ?
                """;
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tipo.name());
            ps.setString(2, pregunta);
            ps.setString(3, Base64.getEncoder().encodeToString(hash));
            ps.setString(4, Base64.getEncoder().encodeToString(sal));
            ps.setString(5, usuario);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ErrorRegistrado("No se pudo guardar el metodo de recuperacion", e);
        }
    }

    private static String generarCodigo() {
        String alfabeto = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
        SecureRandom aleatorio = new SecureRandom();
        StringBuilder codigo = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                codigo.append('-');
            }
            codigo.append(alfabeto.charAt(aleatorio.nextInt(alfabeto.length())));
        }
        return codigo.toString();
    }

    private static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT).replace(" ", "");
    }

    private static String normalizarRespuesta(String respuesta) {
        return respuesta.trim().toLowerCase(Locale.ROOT);
    }

    private static byte[] generarSal() {
        byte[] sal = new byte[16];
        new SecureRandom().nextBytes(sal);
        return sal;
    }

    private static byte[] calcularHash(char[] contrasena, byte[] sal) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, sal, ITERACIONES, LONGITUD_LLAVE);
        try {
            SecretKeyFactory fabrica = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return fabrica.generateSecret(especificacion).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ErrorRegistrado("No se pudo calcular el hash de la contrasena", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
