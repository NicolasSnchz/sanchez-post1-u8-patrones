package com.example.auditoria.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record ReabrirRequest(@NotBlank(message = "es obligatorio") String motivo) {
}
