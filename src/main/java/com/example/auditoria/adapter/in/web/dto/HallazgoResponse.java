package com.example.auditoria.adapter.in.web.dto;

import com.example.auditoria.domain.entity.HallazgoAuditoria;
import com.example.auditoria.domain.valueobject.PlanRemediacion;

import java.time.LocalDate;

public record HallazgoResponse(
        String id,
        String titulo,
        String descripcion,
        String areaResponsable,
        String severidad,
        String estado,
        LocalDate fechaDeteccion,
        LocalDate fechaCierre,
        PlanResponse planRemediacion) {

    public record PlanResponse(String responsable, LocalDate fechaLimite, String notas) {
    }

    public static HallazgoResponse desde(HallazgoAuditoria h) {
        PlanRemediacion plan = h.getPlanRemediacion();
        PlanResponse planResponse = plan == null ? null
                : new PlanResponse(plan.responsable(), plan.fechaLimite(), plan.notas());
        return new HallazgoResponse(
                h.getId().toString(),
                h.getTitulo(),
                h.getDescripcion(),
                h.getAreaResponsable(),
                h.getSeveridad().name(),
                h.getEstado().name(),
                h.getFechaDeteccion(),
                h.getFechaCierre(),
                planResponse);
    }
}
