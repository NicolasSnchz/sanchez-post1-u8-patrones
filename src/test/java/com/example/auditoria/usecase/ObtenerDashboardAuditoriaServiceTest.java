package com.example.auditoria.usecase;

import com.example.auditoria.domain.entity.HallazgoAuditoria;
import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.domain.valueobject.PlanRemediacion;
import com.example.auditoria.domain.valueobject.Severidad;
import com.example.auditoria.usecase.impl.ObtenerDashboardAuditoriaService;
import com.example.auditoria.usecase.port.ConteoCategoria;
import com.example.auditoria.usecase.port.DashboardAuditoriaView;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObtenerDashboardAuditoriaServiceTest {

    private final RepositorioEnMemoria repo = new RepositorioEnMemoria();

    private void cerrado(String area, Severidad severidad, LocalDate deteccion, LocalDate cierre) {
        PlanRemediacion plan = new PlanRemediacion("Responsable", cierre, null);
        repo.guardar(HallazgoAuditoria.reconstituir(HallazgoId.nuevo(), "T", "D", area, severidad,
                deteccion, EstadoHallazgo.CERRADO, plan, cierre));
    }

    @Test
    void armaLosTresBloquesDelDashboard() {
        cerrado("Finanzas", Severidad.ALTA, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 11));
        cerrado("Finanzas", Severidad.MEDIA, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 21));
        cerrado("Tecnologia", Severidad.ALTA, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 6));
        repo.guardar(new HallazgoAuditoria(HallazgoId.nuevo(), "Abierto", "D", "Tecnologia",
                Severidad.CRITICA, LocalDate.of(2026, 9, 10)));

        DashboardAuditoriaView vista = new ObtenerDashboardAuditoriaService(repo).ejecutar();

        assertEquals(new ConteoCategoria("ALTA", 2), vista.porSeveridad().get(0));
        assertEquals(3, vista.porSeveridad().size());
        assertEquals(new ConteoCategoria("ABIERTO", 1), vista.porEstado().get(0));
        assertEquals(new ConteoCategoria("CERRADO", 3), vista.porEstado().get(1));
        assertEquals("Finanzas", vista.promedioDiasCierrePorArea().get(0).categoria());
        assertEquals(15.0, vista.promedioDiasCierrePorArea().get(0).promedioDias(), 0.001);
        assertEquals(5.0, vista.promedioDiasCierrePorArea().get(1).promedioDias(), 0.001);
    }
}
