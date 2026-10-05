package com.example.auditoria.adapter.out.persistence;

import org.springframework.data.repository.Repository;

import java.util.List;

/**
 * Extiende Repository y no JpaRepository a proposito: solo expone insertar y leer,
 * asi no existe ningun metodo delete ni de actualizacion masiva sobre la bitacora.
 */
public interface HistorialCambioEstadoJpaRepository extends Repository<HistorialCambioEstadoJpaEntity, Long> {

    HistorialCambioEstadoJpaEntity save(HistorialCambioEstadoJpaEntity registro);

    List<HistorialCambioEstadoJpaEntity> findByHallazgoIdOrderByFechaAscIdAsc(String hallazgoId);
}
