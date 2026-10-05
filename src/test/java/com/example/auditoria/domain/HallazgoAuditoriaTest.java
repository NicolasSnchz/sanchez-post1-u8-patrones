package com.example.auditoria.domain;

import com.example.auditoria.domain.entity.HallazgoAuditoria;
import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.domain.valueobject.PlanRemediacion;
import com.example.auditoria.domain.valueobject.Severidad;
import com.example.auditoria.domain.valueobject.TransicionInvalidaException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HallazgoAuditoriaTest {

    private HallazgoAuditoria nuevoHallazgo() {
        return new HallazgoAuditoria(HallazgoId.nuevo(), "Credenciales por defecto",
                "Servidor QA con usuario de fabrica", "Infraestructura", Severidad.ALTA,
                LocalDate.of(2026, 8, 1));
    }

    private PlanRemediacion plan() {
        return new PlanRemediacion("Equipo de Infraestructura", LocalDate.of(2026, 8, 20), "Rotar credenciales");
    }

    @Test
    void naceAbierto() {
        assertEquals(EstadoHallazgo.ABIERTO, nuevoHallazgo().getEstado());
    }

    @Test
    void recorreElCicloCompletoYDevuelveElEstadoAnterior() {
        HallazgoAuditoria h = nuevoHallazgo();

        assertEquals(EstadoHallazgo.ABIERTO, h.iniciarRemediacion(plan()));
        assertEquals(EstadoHallazgo.EN_REMEDIACION, h.getEstado());

        assertEquals(EstadoHallazgo.EN_REMEDIACION, h.cerrar());
        assertEquals(EstadoHallazgo.CERRADO, h.getEstado());
        assertNotNull(h.getFechaCierre());

        assertEquals(EstadoHallazgo.CERRADO, h.reabrir());
        assertEquals(EstadoHallazgo.REABIERTO, h.getEstado());
        assertNull(h.getFechaCierre());

        assertEquals(EstadoHallazgo.REABIERTO, h.iniciarRemediacion(plan()));
        assertEquals(EstadoHallazgo.EN_REMEDIACION, h.getEstado());
    }

    @Test
    void noSePuedeCerrarSinPlanDeRemediacion() {
        HallazgoAuditoria h = nuevoHallazgo();
        assertThrows(IllegalStateException.class, h::cerrar);
        assertEquals(EstadoHallazgo.ABIERTO, h.getEstado());
    }

    @Test
    void noSePuedeReabrirUnHallazgoAbierto() {
        HallazgoAuditoria h = nuevoHallazgo();
        assertThrows(TransicionInvalidaException.class, h::reabrir);
    }

    @Test
    void sinPlanNoCambiaDeEstado() {
        HallazgoAuditoria h = nuevoHallazgo();
        assertThrows(NullPointerException.class, () -> h.iniciarRemediacion(null));
        assertEquals(EstadoHallazgo.ABIERTO, h.getEstado());
    }

    @Test
    void validaCamposObligatorios() {
        assertThrows(IllegalArgumentException.class, () -> new HallazgoAuditoria(HallazgoId.nuevo(),
                " ", "x", "Infraestructura", Severidad.BAJA, LocalDate.now()));
        assertThrows(IllegalArgumentException.class, () -> new HallazgoAuditoria(HallazgoId.nuevo(),
                "Titulo", "x", "", Severidad.BAJA, LocalDate.now()));
        assertThrows(IllegalArgumentException.class,
                () -> new PlanRemediacion(" ", LocalDate.now(), "notas"));
    }

    @Test
    void reconstituirRespetaElEstadoGuardado() {
        LocalDate cierre = LocalDate.of(2026, 8, 15);
        HallazgoAuditoria h = HallazgoAuditoria.reconstituir(HallazgoId.nuevo(), "T", "D", "Finanzas",
                Severidad.MEDIA, LocalDate.of(2026, 8, 1), EstadoHallazgo.CERRADO, plan(), cierre);
        assertEquals(EstadoHallazgo.CERRADO, h.getEstado());
        assertEquals(cierre, h.getFechaCierre());

        HallazgoAuditoria reabierto = HallazgoAuditoria.reconstituir(HallazgoId.nuevo(), "T", "D", "Finanzas",
                Severidad.MEDIA, LocalDate.of(2026, 8, 1), EstadoHallazgo.REABIERTO, plan(), null);
        assertEquals(EstadoHallazgo.REABIERTO, reabierto.iniciarRemediacion(plan()));
    }
}
