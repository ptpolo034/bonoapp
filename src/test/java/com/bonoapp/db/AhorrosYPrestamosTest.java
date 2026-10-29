package com.bonoapp.db;

import com.bonoapp.model.Prestamo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Ahorros (manuales) y prestamos con abonos")
class AhorrosYPrestamosTest extends BaseDeDatos {

    private static final LocalDate HOY = LocalDate.now();

    private static Prestamo prestamo(int id) {
        return PrestamoDAO.listar().stream().filter(p -> p.id() == id).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("El ahorro se suma a mano y un retiro no puede pasar del saldo")
    void ahorros() {
        int ana = crearVendedora("Ana");
        AhorroDAO.registrarManual(ana, 50000, HOY, "Ahorro");
        AhorroDAO.retirar(ana, 20000, HOY, "Retiro");
        assertEquals(30000, AhorroDAO.totalAcumulado());
        assertEquals(30000, AhorroDAO.saldoDe(ana));
        assertEquals(2, AhorroDAO.listar().size());

        assertThrows(IllegalArgumentException.class, () -> AhorroDAO.retirar(ana, 40000, HOY, "Retiro grande"));
        assertEquals(30000, AhorroDAO.totalAcumulado());
    }

    @Test
    @DisplayName("Abonos: bajan el saldo, no pueden pasar de lo que se debe y se borran con el prestamo")
    void prestamosYAbonos() {
        int ana = crearVendedora("Ana");
        PrestamoDAO.insertar(ana, 20000, "Emergencia", HOY);
        PrestamoDAO.insertar(ana, 10000, "Transporte", HOY);
        int emergencia = PrestamoDAO.listar().stream().filter(p -> p.concepto().equals("Emergencia"))
                .findFirst().orElseThrow().id();

        AbonoDAO.registrar(emergencia, 5000, HOY, "primer pago");
        AbonoDAO.registrar(emergencia, 3000, HOY, null);
        assertEquals(8000, prestamo(emergencia).abonado());
        assertEquals(12000, prestamo(emergencia).saldo());
        assertEquals(22000, PrestamoDAO.totalPendiente());

        assertThrows(IllegalArgumentException.class, () -> AbonoDAO.registrar(emergencia, 13000, HOY, null));
        AbonoDAO.registrar(emergencia, 12000, HOY, "pago final");
        assertEquals(0, prestamo(emergencia).saldo());
        assertEquals(3, AbonoDAO.listar(emergencia).size());

        var resumen = ReporteDAO.resumenPorVendedora(HOY.withDayOfMonth(1), HOY).get(0);
        assertEquals(30000, resumen.prestado());
        assertEquals(20000, resumen.abonos());

        PrestamoDAO.eliminarVarios(List.of(emergencia));
        assertEquals(0, AbonoDAO.listar(emergencia).size(), "los abonos se borran con su prestamo");
    }
}
