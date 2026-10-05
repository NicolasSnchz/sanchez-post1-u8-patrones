package com.example.auditoria.config;

import com.example.auditoria.usecase.CerrarHallazgoUseCase;
import com.example.auditoria.usecase.ConsultarHallazgoUseCase;
import com.example.auditoria.usecase.ConsultarHistorialUseCase;
import com.example.auditoria.usecase.IniciarRemediacionUseCase;
import com.example.auditoria.usecase.ObtenerDashboardAuditoriaUseCase;
import com.example.auditoria.usecase.ReabrirHallazgoUseCase;
import com.example.auditoria.usecase.RegistrarHallazgoUseCase;
import com.example.auditoria.usecase.impl.CerrarHallazgoService;
import com.example.auditoria.usecase.impl.ConsultarHallazgoService;
import com.example.auditoria.usecase.impl.ConsultarHistorialService;
import com.example.auditoria.usecase.impl.IniciarRemediacionService;
import com.example.auditoria.usecase.impl.ObtenerDashboardAuditoriaService;
import com.example.auditoria.usecase.impl.ReabrirHallazgoService;
import com.example.auditoria.usecase.impl.RegistrarHallazgoService;
import com.example.auditoria.usecase.port.HallazgoRepositoryPort;
import com.example.auditoria.usecase.port.HistorialAuditoriaPort;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

@Configuration
public class AuditoriaConfiguration {

    private final PlatformTransactionManager transactionManager;

    public AuditoriaConfiguration(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Bean
    public RegistrarHallazgoUseCase registrarHallazgoUseCase(HallazgoRepositoryPort repo) {
        return new RegistrarHallazgoService(repo);
    }

    @Bean
    public IniciarRemediacionUseCase iniciarRemediacionUseCase(HallazgoRepositoryPort repo,
                                                               HistorialAuditoriaPort historial) {
        return transaccional(new IniciarRemediacionService(repo, historial), IniciarRemediacionUseCase.class);
    }

    @Bean
    public CerrarHallazgoUseCase cerrarHallazgoUseCase(HallazgoRepositoryPort repo,
                                                       HistorialAuditoriaPort historial) {
        return transaccional(new CerrarHallazgoService(repo, historial), CerrarHallazgoUseCase.class);
    }

    @Bean
    public ReabrirHallazgoUseCase reabrirHallazgoUseCase(HallazgoRepositoryPort repo,
                                                         HistorialAuditoriaPort historial) {
        return transaccional(new ReabrirHallazgoService(repo, historial), ReabrirHallazgoUseCase.class);
    }

    @Bean
    public ConsultarHallazgoUseCase consultarHallazgoUseCase(HallazgoRepositoryPort repo) {
        return new ConsultarHallazgoService(repo);
    }

    @Bean
    public ConsultarHistorialUseCase consultarHistorialUseCase(HallazgoRepositoryPort repo,
                                                               HistorialAuditoriaPort historial) {
        return new ConsultarHistorialService(repo, historial);
    }

    @Bean
    public ObtenerDashboardAuditoriaUseCase obtenerDashboardAuditoriaUseCase(HallazgoRepositoryPort repo) {
        return new ObtenerDashboardAuditoriaService(repo);
    }

    /**
     * Envuelve el caso de uso en una transaccion sin poner @Transactional dentro de usecase/.
     * Guardar el hallazgo y escribir la bitacora quedan en la misma transaccion: si una falla,
     * se revierten las dos.
     */
    private <T> T transaccional(T casoDeUso, Class<T> tipo) {
        ProxyFactory proxy = new ProxyFactory(casoDeUso);
        proxy.addInterface(tipo);
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new MatchAlwaysTransactionAttributeSource()));
        return tipo.cast(proxy.getProxy());
    }
}
