package com.bonoapp.ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Lectura de numeros de boleta escritos a mano")
class NumerosBoletaTest {

    @Test
    @DisplayName("Acepta coma, espacio y saltos de linea, completa a 3 cifras y conserva los repetidos")
    void parsea() {
        assertEquals(List.of("005", "012", "007", "007", "999"), NumerosBoleta.parsear("5, 12\n007 7  999"));
    }

    @Test
    @DisplayName("Un texto vacio da una lista vacia")
    void vacio() {
        assertTrue(NumerosBoleta.parsear("   ").isEmpty());
    }

    @Test
    @DisplayName("Rechaza lo que no es un numero de 1 a 3 cifras")
    void invalidos() {
        assertThrows(IllegalArgumentException.class, () -> NumerosBoleta.parsear("12, abc"));
        assertThrows(IllegalArgumentException.class, () -> NumerosBoleta.parsear("1234"));
        assertThrows(IllegalArgumentException.class, () -> NumerosBoleta.parsear("-5"));
    }
}
