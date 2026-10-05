package com.example.auditoria.usecase;

import com.example.auditoria.domain.entity.HallazgoAuditoria;
import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.usecase.port.ConteoCategoria;
import com.example.auditoria.usecase.port.HallazgoRepositoryPort;
import com.example.auditoria.usecase.port.PromedioCategoria;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

class RepositorioEnMemoria implements HallazgoRepositoryPort {

    final Map<HallazgoId, HallazgoAuditoria> datos = new LinkedHashMap<>();
    int guardados;

    @Override
    public void guardar(HallazgoAuditoria hallazgo) {
        datos.put(hallazgo.getId(), hallazgo);
        guardados++;
    }

    @Override
    public Optional<HallazgoAuditoria> buscarPorId(HallazgoId id) {
        return Optional.ofNullable(datos.get(id));
    }

    @Override
    public List<HallazgoAuditoria> buscarTodos() {
        return new ArrayList<>(datos.values());
    }

    @Override
    public List<ConteoCategoria> contarPorSeveridad() {
        return contar(h -> h.getSeveridad().name());
    }

    @Override
    public List<ConteoCategoria> contarPorEstado() {
        return contar(h -> h.getEstado().name());
    }

    @Override
    public List<PromedioCategoria> promedioDiasCierrePorArea() {
        return datos.values().stream()
                .filter(h -> h.getEstado() == EstadoHallazgo.CERRADO)
                .collect(Collectors.groupingBy(HallazgoAuditoria::getAreaResponsable, TreeMap::new,
                        Collectors.averagingLong(h -> ChronoUnit.DAYS.between(h.getFechaDeteccion(), h.getFechaCierre()))))
                .entrySet().stream()
                .map(e -> new PromedioCategoria(e.getKey(), e.getValue()))
                .toList();
    }

    private List<ConteoCategoria> contar(Function<HallazgoAuditoria, String> clave) {
        return datos.values().stream()
                .collect(Collectors.groupingBy(clave, TreeMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(e -> new ConteoCategoria(e.getKey(), e.getValue()))
                .toList();
    }
}
