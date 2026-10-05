package com.example.auditoria.adapter.in.web;

import com.example.auditoria.adapter.in.web.dto.HallazgoResponse;
import com.example.auditoria.adapter.in.web.dto.IniciarRemediacionRequest;
import com.example.auditoria.adapter.in.web.dto.ReabrirRequest;
import com.example.auditoria.adapter.in.web.dto.RegistrarHallazgoRequest;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.usecase.CerrarHallazgoUseCase;
import com.example.auditoria.usecase.ConsultarHallazgoUseCase;
import com.example.auditoria.usecase.ConsultarHistorialUseCase;
import com.example.auditoria.usecase.IniciarRemediacionUseCase;
import com.example.auditoria.usecase.ObtenerDashboardAuditoriaUseCase;
import com.example.auditoria.usecase.ReabrirHallazgoUseCase;
import com.example.auditoria.usecase.RegistrarHallazgoUseCase;
import com.example.auditoria.usecase.port.CambioEstadoView;
import com.example.auditoria.usecase.port.DashboardAuditoriaView;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/hallazgos")
public class HallazgoController {

    static final String USUARIO_HEADER = "X-Usuario";
    static final String USUARIO_POR_DEFECTO = "anonimo";

    private final RegistrarHallazgoUseCase registrarUseCase;
    private final IniciarRemediacionUseCase iniciarRemediacionUseCase;
    private final CerrarHallazgoUseCase cerrarUseCase;
    private final ReabrirHallazgoUseCase reabrirUseCase;
    private final ConsultarHallazgoUseCase consultarUseCase;
    private final ObtenerDashboardAuditoriaUseCase dashboardUseCase;
    private final ConsultarHistorialUseCase consultarHistorialUseCase;

    public HallazgoController(RegistrarHallazgoUseCase registrarUseCase,
                              IniciarRemediacionUseCase iniciarRemediacionUseCase,
                              CerrarHallazgoUseCase cerrarUseCase,
                              ReabrirHallazgoUseCase reabrirUseCase,
                              ConsultarHallazgoUseCase consultarUseCase,
                              ObtenerDashboardAuditoriaUseCase dashboardUseCase,
                              ConsultarHistorialUseCase consultarHistorialUseCase) {
        this.registrarUseCase = registrarUseCase;
        this.iniciarRemediacionUseCase = iniciarRemediacionUseCase;
        this.cerrarUseCase = cerrarUseCase;
        this.reabrirUseCase = reabrirUseCase;
        this.consultarUseCase = consultarUseCase;
        this.dashboardUseCase = dashboardUseCase;
        this.consultarHistorialUseCase = consultarHistorialUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> registrar(@Valid @RequestBody RegistrarHallazgoRequest req) {
        HallazgoId id = registrarUseCase.ejecutar(
                req.titulo(), req.descripcion(), req.areaResponsable(), req.severidad(), req.fechaDeteccion());
        return Map.of("hallazgoId", id.toString());
    }

    @PatchMapping("/{id}/iniciar-remediacion")
    public Map<String, String> iniciarRemediacion(@PathVariable("id") String id,
                                                  @Valid @RequestBody IniciarRemediacionRequest req,
                                                  @RequestHeader(value = USUARIO_HEADER,
                                                          defaultValue = USUARIO_POR_DEFECTO) String usuario) {
        iniciarRemediacionUseCase.ejecutar(aId(id), req.responsable(), req.fechaLimite(), req.notas(), usuario);
        return Map.of("estado", "EN_REMEDIACION");
    }

    @PatchMapping("/{id}/cerrar")
    public Map<String, String> cerrar(@PathVariable("id") String id,
                                      @RequestHeader(value = USUARIO_HEADER,
                                              defaultValue = USUARIO_POR_DEFECTO) String usuario) {
        cerrarUseCase.ejecutar(aId(id), usuario);
        return Map.of("estado", "CERRADO");
    }

    @PatchMapping("/{id}/reabrir")
    public Map<String, String> reabrir(@PathVariable("id") String id, @Valid @RequestBody ReabrirRequest req,
                                       @RequestHeader(value = USUARIO_HEADER,
                                               defaultValue = USUARIO_POR_DEFECTO) String usuario) {
        reabrirUseCase.ejecutar(aId(id), req.motivo(), usuario);
        return Map.of("estado", "REABIERTO");
    }

    @GetMapping("/dashboard")
    public DashboardAuditoriaView dashboard() {
        return dashboardUseCase.ejecutar();
    }

    @GetMapping("/{id}/historial")
    public List<CambioEstadoView> historial(@PathVariable("id") String id) {
        return consultarHistorialUseCase.ejecutar(aId(id));
    }

    @GetMapping("/{id}")
    public HallazgoResponse buscar(@PathVariable("id") String id) {
        return HallazgoResponse.desde(consultarUseCase.buscarPorId(aId(id)));
    }

    @GetMapping
    public List<HallazgoResponse> listar() {
        return consultarUseCase.listarTodos().stream().map(HallazgoResponse::desde).toList();
    }

    private HallazgoId aId(String id) {
        return new HallazgoId(UUID.fromString(id));
    }
}
