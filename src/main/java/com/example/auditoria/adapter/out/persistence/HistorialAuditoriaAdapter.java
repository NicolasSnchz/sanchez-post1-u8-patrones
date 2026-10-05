package com.example.auditoria.adapter.out.persistence;

import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.usecase.port.CambioEstadoView;
import com.example.auditoria.usecase.port.HistorialAuditoriaPort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class HistorialAuditoriaAdapter implements HistorialAuditoriaPort {

    private final HistorialCambioEstadoJpaRepository jpa;

    public HistorialAuditoriaAdapter(HistorialCambioEstadoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void registrar(HallazgoId hallazgoId, EstadoHallazgo anterior, EstadoHallazgo nuevo,
                          String motivo, String usuario) {
        jpa.save(new HistorialCambioEstadoJpaEntity(
                hallazgoId.toString(), anterior, nuevo, motivo, usuario, LocalDateTime.now()));
    }

    @Override
    public List<CambioEstadoView> listarPorHallazgo(HallazgoId hallazgoId) {
        return jpa.findByHallazgoIdOrderByFechaAscIdAsc(hallazgoId.toString()).stream()
                .map(e -> new CambioEstadoView(e.getEstadoAnterior().name(), e.getEstadoNuevo().name(),
                        e.getMotivo(), e.getUsuario(), e.getFecha()))
                .toList();
    }
}
