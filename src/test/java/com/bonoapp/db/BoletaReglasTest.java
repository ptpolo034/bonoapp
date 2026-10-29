package com.bonoapp.db;

import com.bonoapp.model.Boleta;
import com.bonoapp.model.TipoBoleta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Boletas: reglas de entrega, numeros repetidos, lote de 2.000 y movimientos")
class BoletaReglasTest extends BaseDeDatos {

    private static final LocalDate HOY = LocalDate.now();

    private static void entregar(int vendedora, TipoBoleta tipo, LocalDate fecha, String... numeros) {
        BoletaDAO.registrarEntrega(vendedora, tipo, numeros.length, List.of(numeros), fecha);
    }

    /** 2.000 numeros: del 000 al 999, cada uno dos veces (el maximo permitido). */
    private static List<String> numerosParaUnLoteCompleto() {
        List<String> numeros = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            numeros.add(String.format("%03d", i));
            numeros.add(String.format("%03d", i));
        }
        return numeros;
    }

    @Test
    @DisplayName("Un numero se puede usar hasta 2 veces por tipo y por dia")
    void numeroHastaDosVeces() {
        int ana = crearVendedora("Ana");
        int bea = crearVendedora("Beatriz");
        for (TipoBoleta tipo : TipoBoleta.values()) {
            entregar(ana, tipo, HOY, "001", "001");
            assertThrows(IllegalArgumentException.class, () -> entregar(bea, tipo, HOY, "001"),
                    tipo + ": el tercer 001 del dia debe bloquearse");
            // otro dia el mismo numero vuelve a estar libre
            LocalDate ayer = HOY.minusDays(1);
            entregar(ana, tipo, ayer, "001", "001");
            assertThrows(IllegalArgumentException.class, () -> entregar(bea, tipo, ayer, "001"));
        }
    }

    @Test
    @DisplayName("No se pueden poner 3 veces el mismo numero en una sola entrega")
    void tresVecesEnLaMismaEntrega() {
        int ana = crearVendedora("Ana");
        assertThrows(IllegalArgumentException.class, () -> entregar(ana, TipoBoleta.DIARIA, HOY, "003", "003", "003"));
        assertTrue(BoletaDAO.listarRecientes(100).isEmpty(), "no debe quedar nada guardado a medias");
    }

    @Test
    @DisplayName("Las boletas sin numero se registran sin el; mas numeros que boletas se rechaza")
    void entregaParcial() {
        int ana = crearVendedora("Ana");
        BoletaDAO.registrarEntrega(ana, TipoBoleta.DIARIA, 10, List.of("015", "234", "087"), HOY);
        List<Boleta> todas = BoletaDAO.listarRecientes(100);
        assertEquals(10, todas.size());
        assertEquals(3, todas.stream().filter(b -> b.getNumero() != null).count());
        assertEquals(7, todas.stream().filter(b -> b.getNumero() == null).count());

        assertThrows(IllegalArgumentException.class,
                () -> BoletaDAO.registrarEntrega(ana, TipoBoleta.DIARIA, 2, List.of("001", "002", "003"), HOY));
        assertEquals(10, BoletaDAO.listarRecientes(100).size());
    }

    @Test
    @DisplayName("El lote es de 2.000 boletas por tipo y por dia; cada tipo y cada dia tienen su cupo")
    void loteDe2000() {
        int ana = crearVendedora("Ana");
        BoletaDAO.registrarEntrega(ana, TipoBoleta.PREMIO_MAYOR, 2000, numerosParaUnLoteCompleto(), HOY);
        assertEquals(2000, BoletaDAO.avanceDelLote(TipoBoleta.PREMIO_MAYOR, HOY).total());

        assertThrows(IllegalArgumentException.class,
                () -> BoletaDAO.registrarEntrega(ana, TipoBoleta.PREMIO_MAYOR, 1, List.of(), HOY),
                "la boleta 2.001 del mismo tipo y dia debe bloquearse");
        // la Diaria tiene su propio cupo, y mañana empieza un lote nuevo
        BoletaDAO.registrarEntrega(ana, TipoBoleta.DIARIA, 1, List.of("500"), HOY);
        BoletaDAO.registrarEntrega(ana, TipoBoleta.PREMIO_MAYOR, 1, List.of(), HOY.plusDays(1));
    }

    @Test
    @DisplayName("El avance del lote cuenta las boletas del tipo y dia, y las que no tienen numero")
    void avanceDelLote() {
        int ana = crearVendedora("Ana");
        int bea = crearVendedora("Beatriz");
        assertEquals(new BoletaDAO.AvanceLote(0, 0), BoletaDAO.avanceDelLote(TipoBoleta.DIARIA, HOY));

        BoletaDAO.registrarEntrega(ana, TipoBoleta.DIARIA, 5, List.of("010", "011"), HOY);
        BoletaDAO.registrarEntrega(bea, TipoBoleta.PREMIO_MAYOR, 2, List.of("500"), HOY);
        assertEquals(new BoletaDAO.AvanceLote(5, 3), BoletaDAO.avanceDelLote(TipoBoleta.DIARIA, HOY));
        assertEquals(new BoletaDAO.AvanceLote(2, 1), BoletaDAO.avanceDelLote(TipoBoleta.PREMIO_MAYOR, HOY));
        assertEquals(0, BoletaDAO.avanceDelLote(TipoBoleta.DIARIA, HOY.minusDays(1)).total());
    }

    @Test
    @DisplayName("hayBoletas dice si ya se registro alguna (para la guia de primeros pasos)")
    void hayBoletas() {
        int ana = crearVendedora("Ana");
        assertFalse(BoletaDAO.hayBoletas());
        entregar(ana, TipoBoleta.DIARIA, HOY, "001");
        assertTrue(BoletaDAO.hayBoletas());
    }

    @Test
    @DisplayName("Editar una boleta respeta el maximo de 2 usos del numero en ese dia")
    void editarBoleta() {
        int ana = crearVendedora("Ana");
        entregar(ana, TipoBoleta.DIARIA, HOY, "010", "011", "012");
        List<Integer> ids = idsDeBoletas();

        BoletaDAO.actualizar(ids.get(0), "011", HOY);                      // 011 queda con 2 usos
        assertThrows(IllegalArgumentException.class, () -> BoletaDAO.actualizar(ids.get(2), "011", HOY));
        BoletaDAO.actualizar(ids.get(0), "011", HOY.minusDays(30));        // editarse a si misma no cuenta doble

        // pasarla a un dia donde su numero ya esta lleno se bloquea
        LocalDate manana = HOY.plusDays(1);
        entregar(ana, TipoBoleta.DIARIA, manana, "005", "005");
        entregar(ana, TipoBoleta.DIARIA, manana.plusDays(1), "005");
        int sola = BoletaDAO.buscar("005", manana.plusDays(1), manana.plusDays(1)).get(0).getId();
        assertThrows(IllegalArgumentException.class, () -> BoletaDAO.actualizar(sola, "005", manana));
    }

    @Test
    @DisplayName("Mover boletas a otra vendedora: solo las entregadas, y no a la misma dueña")
    void moverBoletas() {
        int ana = crearVendedora("Ana");
        int bea = crearVendedora("Beatriz");
        entregar(ana, TipoBoleta.DIARIA, HOY, "010", "011", "012");
        List<Integer> ids = idsDeBoletas();

        assertEquals(2, BoletaDAO.mover(ids.subList(0, 2), bea));
        assertEquals(1, BoletaDAO.listarPendientesPorVendedora(ana, TipoBoleta.DIARIA).size());
        assertEquals(2, BoletaDAO.listarPendientesPorVendedora(bea, TipoBoleta.DIARIA).size());
        assertEquals(0, BoletaDAO.mover(ids.subList(0, 2), bea), "a la misma dueña no se mueve");

        LiquidacionDAO.liquidar(TipoBoleta.DIARIA, bea, List.of(ids.get(0)), List.of(ids.get(1)));
        assertEquals(0, BoletaDAO.mover(List.of(ids.get(0)), ana), "una boleta ya liquidada no se mueve");
    }

    @Test
    @DisplayName("El numero ganador solo mira las boletas del dia del sorteo")
    void ganadorPorDia() {
        int ana = crearVendedora("Ana");
        int bea = crearVendedora("Beatriz");
        LocalDate ayer = HOY.minusDays(1);
        entregar(ana, TipoBoleta.DIARIA, ayer, "001");
        entregar(bea, TipoBoleta.DIARIA, ayer, "001");
        entregar(ana, TipoBoleta.DIARIA, HOY, "001");

        List<Boleta> deAyer = GanadorDAO.titulares(TipoBoleta.DIARIA, "001", ayer);
        assertEquals(2, deAyer.size());
        assertTrue(deAyer.stream().allMatch(b -> b.getFechaEntrega().equals(ayer)));
        assertEquals(1, GanadorDAO.titulares(TipoBoleta.DIARIA, "001", HOY).size());
    }
}
