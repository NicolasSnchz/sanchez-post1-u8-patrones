package com.example.auditoria.usecase;

import com.example.auditoria.domain.valueobject.EstadoHallazgo;
import com.example.auditoria.domain.valueobject.HallazgoId;
import com.example.auditoria.domain.valueobject.Severidad;
import com.example.auditoria.domain.valueobject.TransicionInvalidaException;
import com.example.auditoria.usecase.impl.CerrarHallazgoService;
import com.example.auditoria.usecase.impl.ConsultarHallazgoService;
import com.example.auditoria.usecase.impl.IniciarRemediacionService;
import com.example.auditoria.usecase.impl.ReabrirHallazgoService;
import com.example.auditoria.usecase.impl.RegistrarHallazgoService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CasosDeUsoHallazgoTest {

    private final RepositorioEnMemoria repo = new RepositorioEnMemoria();
    private final RegistrarHallazgoUseCase registrar = new RegistrarHallazgoService(repo);
    private final IniciarRemediacionUseCase iniciar = new IniciarRemediacionService(repo);
    private final CerrarHallazgoUseCase cerrar = new CerrarHallazgoService(repo);
    private final ReabrirHallazgoUseCase reabrir = new ReabrirHallazgoService(repo);
    private final ConsultarHallazgoUseCase consultar = new ConsultarHallazgoService(repo);

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
        iniciar.ejecutar(id, "Seguridad TI", LocalDate.of(2026, 8, 30), "Cifrar con AES");
        cerrar.ejecutar(id);
        assertEquals(EstadoHallazgo.CERRADO, consultar.buscarPorId(id).getEstado());
        reabrir.ejecutar(id, "Se encontraron copias nuevas sin cifrar");
        assertEquals(EstadoHallazgo.REABIERTO, consultar.buscarPorId(id).getEstado());
    }

    @Test
    void cerrarSinRemediacionFallaYNoGuarda() {
        HallazgoId id = registrarUno();
        int antes = repo.guardados;
        assertThrows(IllegalStateException.class, () -> cerrar.ejecutar(id));
        assertEquals(antes, repo.guardados);
    }

    @Test
    void reabrirUnHallazgoAbiertoFalla() {
        HallazgoId id = registrarUno();
        assertThrows(TransicionInvalidaException.class, () -> reabrir.ejecutar(id, "motivo"));
    }

    @Test
    void hallazgoInexistenteLanzaNotFound() {
        assertThrows(HallazgoNotFoundException.class, () -> consultar.buscarPorId(HallazgoId.nuevo()));
    }
}
