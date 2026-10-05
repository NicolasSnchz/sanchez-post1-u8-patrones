package com.example.auditoria.usecase;

import com.example.auditoria.domain.valueobject.HallazgoId;

public class HallazgoNotFoundException extends RuntimeException {

    public HallazgoNotFoundException(HallazgoId id) {
        super("No existe un hallazgo con id " + id);
    }
}
