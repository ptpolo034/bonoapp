package com.bonoapp.db;

import com.bonoapp.model.Liquidacion;
import com.bonoapp.model.NumeroGanador;
import com.bonoapp.model.ResumenVendedora;
import com.bonoapp.model.TipoBoleta;
import com.bonoapp.util.Preferencias;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Liquidacion, reportes y numero ganador")
class LiquidacionTest extends BaseDeDatos {

    private static final LocalDate HOY = LocalDate.now();

    private int ana;
    private List<Integer> ids;

    private void entregarCuatro() {
        ana = crearVendedora("Ana");
        BoletaDAO.registrarEntrega(ana, TipoBoleta.DIARIA, 4, List.of("010", "011", "012", "013"), HOY);
        ids = idsDeBoletas();
    }

    @Test
    @DisplayName("Liquidar: recaudado = vendidas x precio, y no se crea ahorro solo")
    void liquidar() {
        entregarCuatro();
        assertEquals(1, ReporteDAO.pendientesPorVendedora().size());
        assertEquals(4, ReporteDAO.pendientesPorVendedora().get(0).boletasSinLiquidar());

        LiquidacionDAO.Resultado r = LiquidacionDAO.liquidar(TipoBoleta.DIARIA, ana, ids.subList(0, 3), ids.subList(3, 4));
        assertEquals(3, r.cantidadVendida());
        assertEquals(1, r.cantidadDevuelta());
        assertEquals(30000, r.recaudado());
        assertEquals(0, AhorroDAO.totalAcumulado(), "liquidar no debe crear ahorro");
        assertTrue(ReporteDAO.pendientesPorVendedora().isEmpty());

        ResumenVendedora resumen = ReporteDAO.resumenPorVendedora(HOY, HOY).get(0);
        assertEquals(3, resumen.vendidas());
        assertEquals(1, resumen.devueltas());
        assertEquals(30000, resumen.recaudado());
    }

    @Test
    @DisplayName("Cambiar el precio despues no altera lo ya liquidado")
    void precioGuardadoEnLaLiquidacion() {
        entregarCuatro();
        LiquidacionDAO.liquidar(TipoBoleta.DIARIA, ana, ids.subList(0, 3), ids.subList(3, 4));
        Preferencias.guardarPrecio(TipoBoleta.DIARIA, 12000);
        Liquidacion liquidacion = LiquidacionDAO.listar().get(0);
        assertEquals(10000, liquidacion.precio());
        assertEquals(30000, liquidacion.recaudado());
    }

    @Test
    @DisplayName("Liquidar dos veces lo mismo se rechaza sin dejar basura")
    void noSePuedeLiquidarDosVeces() {
        entregarCuatro();
        LiquidacionDAO.liquidar(TipoBoleta.DIARIA, ana, ids, List.of());
        assertThrows(IllegalArgumentException.class, () -> LiquidacionDAO.liquidar(TipoBoleta.DIARIA, ana, ids, List.of()));
        assertEquals(1, LiquidacionDAO.listar().size());
    }

    @Test
    @DisplayName("Revertir devuelve las boletas a entregadas, no cuenta en reportes y se puede volver a liquidar")
    void revertir() {
        entregarCuatro();
        LiquidacionDAO.liquidar(TipoBoleta.DIARIA, ana, ids.subList(0, 3), ids.subList(3, 4));
        int liquidacionId = LiquidacionDAO.listar().get(0).id();

        assertEquals(4, LiquidacionDAO.revertir(liquidacionId));
        assertEquals(4, BoletaDAO.listarPendientesPorVendedora(ana, TipoBoleta.DIARIA).size());
        assertTrue(LiquidacionDAO.listar().get(0).anulada());
        assertEquals(0, ReporteDAO.resumenPorVendedora(HOY, HOY).get(0).recaudado());
        assertThrows(IllegalArgumentException.class, () -> LiquidacionDAO.revertir(liquidacionId));

        assertEquals(40000, LiquidacionDAO.liquidar(TipoBoleta.DIARIA, ana, ids, List.of()).recaudado());
    }

    @Test
    @DisplayName("Numero ganador: se guarda, se corrige el mismo dia y solo acepta 3 cifras")
    void numeroGanador() {
        entregarCuatro();
        GanadorDAO.guardar(TipoBoleta.DIARIA, HOY, "011");
        NumeroGanador ganador = GanadorDAO.ultimo(TipoBoleta.DIARIA);
        assertNotNull(ganador);
        assertEquals("011", ganador.numero());
        List<com.bonoapp.model.Boleta> titulares = GanadorDAO.titulares(TipoBoleta.DIARIA, "011", HOY);
        assertEquals(1, titulares.size());
        assertEquals("Ana", titulares.get(0).getVendedoraNombre());

        GanadorDAO.guardar(TipoBoleta.DIARIA, HOY, "012");
        assertEquals("012", GanadorDAO.ultimo(TipoBoleta.DIARIA).numero());
        assertNull(GanadorDAO.ultimo(TipoBoleta.PREMIO_MAYOR));
        assertThrows(IllegalArgumentException.class, () -> GanadorDAO.guardar(TipoBoleta.DIARIA, HOY, "12"));
    }

    @Test
    @DisplayName("Las operaciones que cambian datos quedan en el historial con el usuario")
    void historial() {
        entregarCuatro();
        LiquidacionDAO.liquidar(TipoBoleta.DIARIA, ana, ids.subList(0, 3), ids.subList(3, 4));
        LiquidacionDAO.revertir(LiquidacionDAO.listar().get(0).id());
        GanadorDAO.guardar(TipoBoleta.DIARIA, HOY, "011");
        BoletaDAO.actualizar(ids.get(0), "099", HOY);
        AhorroDAO.registrarManual(ana, 5000, HOY, "x");
        PrestamoDAO.insertar(ana, 20000, "Emergencia", HOY);
        BoletaDAO.eliminarVarios(List.of(ids.get(3)));

        for (String accion : List.of("Vendedora creada", "Entrega de boletas", "Liquidación", "Liquidación revertida",
                "Número ganador", "Boleta editada", "Ahorro registrado", "Préstamo registrado", "Boletas eliminadas")) {
            assertTrue(HistorialDAO.listar(500).stream().anyMatch(e -> e.accion().equals(accion)),
                    "el historial debe tener \"" + accion + "\"");
        }
        assertEquals("prueba", HistorialDAO.listar(1).get(0).usuario());
    }
}
