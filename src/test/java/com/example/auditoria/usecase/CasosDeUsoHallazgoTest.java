package com.example.auditoria.usecase;

import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.domain.valueobject.Severidad;
import com.example.auditoria.domain.valueobject.TransicionInvalidaException;
import com.example.auditoria.usecase.impl.CerrarHallazgoService;
import com.example.auditoria.usecase.impl.ConsultarHallazgoService;
import com.example.auditoria.usecase.impl.ConsultarHistorialService;
import com.example.auditoria.usecase.impl.IniciarRemediacionService;
import com.example.auditoria.usecase.impl.ReabrirHallazgoService;
import com.example.auditoria.usecase.impl.RegistrarHallazgoService;
import com.example.auditoria.usecase.port.CambioEstadoView;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CasosDeUsoHallazgoTest {

    private final RepositorioEnMemoria repo = new RepositorioEnMemoria();
    private final HistorialEnMemoria historial = new HistorialEnMemoria();
    private final RegistrarHallazgoUseCase registrar = new RegistrarHallazgoService(repo);
    private final IniciarRemediacionUseCase iniciar = new IniciarRemediacionService(repo, historial);
    private final CerrarHallazgoUseCase cerrar = new CerrarHallazgoService(repo, historial);
    private final ReabrirHallazgoUseCase reabrir = new ReabrirHallazgoService(repo, historial);
    private final ConsultarHallazgoUseCase consultar = new ConsultarHallazgoService(repo);
    private final ConsultarHistorialUseCase consultarHistorial = new ConsultarHistorialService(repo, historial);

    private HallazgoId registrarUno() {
        return registrar.ejecutar("Backups sin cifrar", "Copias en disco externo", "Tecnologia",
                Severidad.CRITICA, LocalDate.of(2026, 7, 10));
    }

    @Test
    void registraYConsulta() {
        HallazgoId id = registrarUno();
        assertEquals(EstadoHallazgo.ABIERTO, consultar.buscarPorId(id).getEstado());
        assertEquals(1, consultar.listarTodos().size());
    }

    @Test
    void cicloCompletoPorCasosDeUso() {
        HallazgoId id = registrarUno();
        iniciar.ejecutar(id, "Seguridad TI", LocalDate.of(2026, 8, 30), "Cifrar con AES", "auditor1");
        cerrar.ejecutar(id, "auditor1");
        assertEquals(EstadoHallazgo.CERRADO, consultar.buscarPorId(id).getEstado());
        reabrir.ejecutar(id, "Se encontraron copias nuevas sin cifrar", "auditor2");
        assertEquals(EstadoHallazgo.REABIERTO, consultar.buscarPorId(id).getEstado());
    }

    @Test
    void cadaTransicionAgregaExactamenteUnRegistroEnOrden() {
        HallazgoId id = registrarUno();
        assertEquals(0, historial.total());

        iniciar.ejecutar(id, "Seguridad TI", LocalDate.of(2026, 8, 30), "Cifrar", "auditor1");
        assertEquals(1, historial.total());
        cerrar.ejecutar(id, "auditor1");
        assertEquals(2, historial.total());
        reabrir.ejecutar(id, "Reaparecio", "auditor2");
        assertEquals(3, historial.total());

        List<CambioEstadoView> cambios = consultarHistorial.ejecutar(id);
        assertEquals("ABIERTO", cambios.get(0).estadoAnterior());
        assertEquals("EN_REMEDIACION", cambios.get(0).estadoNuevo());
        assertEquals("EN_REMEDIACION", cambios.get(1).estadoAnterior());
        assertEquals("CERRADO", cambios.get(1).estadoNuevo());
        assertEquals("CERRADO", cambios.get(2).estadoAnterior());
        assertEquals("REABIERTO", cambios.get(2).estadoNuevo());
        assertEquals("auditor2", cambios.get(2).usuario());
        assertEquals("Reaparecio", cambios.get(2).motivo());
    }

    @Test
    void cerrarSinRemediacionFallaYNoGuardaNiRegistra() {
        HallazgoId id = registrarUno();
        int antes = repo.guardados;
        assertThrows(IllegalStateException.class, () -> cerrar.ejecutar(id, "auditor1"));
        assertEquals(antes, repo.guardados);
        assertEquals(0, historial.total());
    }

    @Test
    void reabrirUnHallazgoAbiertoFallaSinRegistro() {
        HallazgoId id = registrarUno();
        assertThrows(TransicionInvalidaException.class, () -> reabrir.ejecutar(id, "motivo", "auditor1"));
        assertEquals(0, historial.total());
    }

    @Test
    void hallazgoInexistenteLanzaNotFound() {
        assertThrows(HallazgoNotFoundException.class, () -> consultar.buscarPorId(HallazgoId.nuevo()));
        assertThrows(HallazgoNotFoundException.class, () -> consultarHistorial.ejecutar(HallazgoId.nuevo()));
    }
}
