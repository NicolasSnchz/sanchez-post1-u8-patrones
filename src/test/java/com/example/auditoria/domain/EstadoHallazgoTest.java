package com.example.auditoria.domain;

import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import org.junit.jupiter.api.Test;

import static com.example.auditoria.domain.valueobject.EstadoHallazgo.ABIERTO;
import static com.example.auditoria.domain.valueobject.EstadoHallazgo.CERRADO;
import static com.example.auditoria.domain.valueobject.EstadoHallazgo.EN_REMEDIACION;
import static com.example.auditoria.domain.valueobject.EstadoHallazgo.REABIERTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstadoHallazgoTest {

    @Test
    void soloPermiteLasCuatroTransicionesDelCiclo() {
        assertTrue(ABIERTO.puedeTransicionarA(EN_REMEDIACION));
        assertTrue(EN_REMEDIACION.puedeTransicionarA(CERRADO));
        assertTrue(CERRADO.puedeTransicionarA(REABIERTO));
        assertTrue(REABIERTO.puedeTransicionarA(EN_REMEDIACION));

        int validas = 0;
        for (EstadoHallazgo origen : EstadoHallazgo.values()) {
            for (EstadoHallazgo destino : EstadoHallazgo.values()) {
                if (origen.puedeTransicionarA(destino)) {
                    validas++;
                }
            }
        }
        assertEquals(4, validas);
    }

    @Test
    void rechazaCerrarDesdeAbiertoYReabrirDesdeAbierto() {
        assertFalse(ABIERTO.puedeTransicionarA(CERRADO));
        assertFalse(ABIERTO.puedeTransicionarA(REABIERTO));
        assertFalse(EN_REMEDIACION.puedeTransicionarA(REABIERTO));
    }
}
