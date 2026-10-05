package com.example.auditoria.usecase;

import com.example.auditoria.domain.entity.HallazgoAuditoria;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.usecase.port.HallazgoRepositoryPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
}
