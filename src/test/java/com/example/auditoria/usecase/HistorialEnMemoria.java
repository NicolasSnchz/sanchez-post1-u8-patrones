package com.example.auditoria.usecase;

import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.usecase.port.CambioEstadoView;
import com.example.auditoria.usecase.port.HistorialAuditoriaPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

class HistorialEnMemoria implements HistorialAuditoriaPort {

    final Map<HallazgoId, List<CambioEstadoView>> registros = new HashMap<>();

    @Override
    public void registrar(HallazgoId hallazgoId, EstadoHallazgo anterior, EstadoHallazgo nuevo,
                          String motivo, String usuario) {
        registros.computeIfAbsent(hallazgoId, k -> new ArrayList<>())
                .add(new CambioEstadoView(anterior.name(), nuevo.name(), motivo, usuario, LocalDateTime.now()));
    }

    @Override
    public List<CambioEstadoView> listarPorHallazgo(HallazgoId hallazgoId) {
        return List.copyOf(registros.getOrDefault(hallazgoId, List.of()));
    }

    int total() {
        return registros.values().stream().mapToInt(List::size).sum();
    }
}
