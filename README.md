# Post-contenido Unidad 8: Patrones Arquitectónicos II

## Descripción

Repositorio del post-contenido de la Unidad 8 de Patrones de Diseño de Software. Es un sistema de seguimiento de hallazgos de auditoría interna hecho con Clean Architecture sobre Spring Boot. En esta primera parte se implementan los cuatro círculos concéntricos y el ciclo de vida del hallazgo (registrar, iniciar remediación, cerrar, reabrir y consultar). La Parte 2 extiende este mismo proyecto.

- **Estudiante:** Nicolás Andrés Sánchez Villamizar
- **Repositorio:** `sanchez-post1-u8-patrones`

## Parte 1: Clean Architecture (Hallazgos de Auditoría)

El código está organizado en los cuatro círculos y todas las dependencias apuntan hacia adentro, hacia `domain/`:

| Círculo | Paquete | Qué contiene |
|---|---|---|
| Entities | `domain/` | El Aggregate Root [`HallazgoAuditoria`](src/main/java/com/example/auditoria/domain/entity/HallazgoAuditoria.java), la máquina de estados [`EstadoHallazgo`](src/main/java/com/example/auditoria/domain/valueobject/EstadoHallazgo.java) y los Value Objects `HallazgoId`, `Severidad` y `PlanRemediacion`. Java puro, sin ningún framework. |
| Use Cases | `usecase/` | Una interfaz por caso de uso, el puerto de salida [`HallazgoRepositoryPort`](src/main/java/com/example/auditoria/usecase/port/HallazgoRepositoryPort.java) y las implementaciones en `impl/`. Tampoco importa Spring. |
| Interface Adapters | `adapter/` | Entrada web ([`HallazgoController`](src/main/java/com/example/auditoria/adapter/in/web/HallazgoController.java), DTOs y manejo de errores) y salida de persistencia ([`HallazgoRepositoryAdapter`](src/main/java/com/example/auditoria/adapter/out/persistence/HallazgoRepositoryAdapter.java) con la entidad JPA). |
| Frameworks & Drivers | `config/` + Spring Boot, JPA y H2 | [`AuditoriaConfiguration`](src/main/java/com/example/auditoria/config/AuditoriaConfiguration.java) hace el wiring explícito: crea cada servicio con `new` y le pasa el puerto, por eso los casos de uso no necesitan `@Service`. |

Se puede comprobar que `domain/` y `usecase/` no dependen de ningún framework con:

```
grep -rnE "import (org\.springframework|jakarta\.)" src/main/java/com/example/auditoria/domain src/main/java/com/example/auditoria/usecase
```

El comando no devuelve nada.

### Estructura de paquetes

```
sanchez-post1-u8-patrones/
├── pom.xml
└── src/main/java/com/example/auditoria/
    ├── domain/                          (Entities)
    │   ├── entity/HallazgoAuditoria.java
    │   └── valueobject/
    │       ├── HallazgoId.java
    │       ├── Severidad.java
    │       ├── EstadoHallazgo.java
    │       ├── PlanRemediacion.java
    │       └── TransicionInvalidaException.java
    ├── usecase/                         (Use Cases)
    │   ├── RegistrarHallazgoUseCase.java, IniciarRemediacionUseCase.java,
    │   │   CerrarHallazgoUseCase.java, ReabrirHallazgoUseCase.java,
    │   │   ConsultarHallazgoUseCase.java, HallazgoNotFoundException.java
    │   ├── port/HallazgoRepositoryPort.java
    │   └── impl/ (un Service por caso de uso)
    ├── adapter/                         (Interface Adapters)
    │   ├── in/web/
    │   │   ├── HallazgoController.java
    │   │   ├── ManejadorErroresWeb.java
    │   │   └── dto/
    │   └── out/persistence/
    │       ├── HallazgoJpaEntity.java
    │       ├── HallazgoJpaRepository.java
    │       └── HallazgoRepositoryAdapter.java
    ├── config/AuditoriaConfiguration.java   (Frameworks & Drivers)
    └── AuditoriaHallazgosApplication.java
```

```
  adapter (web, persistencia)  ──►  usecase (puertos)  ──►  domain
             ▲
           config  (arma todo con @Bean)
```

## Decisiones de diseño

### 1. Severidad como enum simple y EstadoHallazgo como enum con máquina de estados

[`EstadoHallazgo`](src/main/java/com/example/auditoria/domain/valueobject/EstadoHallazgo.java#L6-L13) tiene el método `puedeTransicionarA(...)` porque el estado sí tiene una regla de negocio propia: solo existen cuatro transiciones válidas (ABIERTO → EN_REMEDIACION → CERRADO → REABIERTO → EN_REMEDIACION) y cualquier otra debe rechazarse. Si esa regla quedara afuera, por ejemplo en un `if` dentro de cada servicio, se repetiría en tres lugares y bastaría con olvidar uno para permitir cerrar un hallazgo que nunca estuvo en remediación. Al ponerla en el enum, el aggregate la usa en un solo punto ([`transicionar`](src/main/java/com/example/auditoria/domain/entity/HallazgoAuditoria.java#L83-L90)) y la prueba `EstadoHallazgoTest` verifica que en las 16 combinaciones posibles solo 4 son válidas.

[`Severidad`](src/main/java/com/example/auditoria/domain/valueobject/Severidad.java#L3-L5) en cambio es solo una clasificación. Ninguna severidad "pasa" a otra ni restringe lo que se puede hacer con el hallazgo, así que agregarle métodos sería comportamiento inventado sin regla que lo respalde. Si en el futuro la severidad definiera, por ejemplo, un plazo máximo de remediación (CRÍTICA = 15 días), ahí sí tendría sentido darle comportamiento, porque ya habría una regla real que encapsular.

### 2. PlanRemediacion como Value Object embebido y no como agregado separado

La invariante es que un hallazgo no puede estar EN_REMEDIACION sin un plan válido ni cerrarse sin uno ya definido. Según el criterio de límite de consistencia transaccional de la guía (Sección 3.3), todo lo que debe cumplir una invariante al mismo tiempo tiene que vivir dentro del mismo agregado y guardarse en la misma transacción. Si `PlanRemediacion` fuera un agregado aparte con su propio repositorio, guardar el plan y cambiar el estado serían dos operaciones distintas y podría quedar un hallazgo EN_REMEDIACION sin plan si la segunda falla.

Por eso el plan es un `record` inmutable dentro de [`HallazgoAuditoria`](src/main/java/com/example/auditoria/domain/entity/HallazgoAuditoria.java#L61-L75): `iniciarRemediacion(plan)` valida el plan y cambia el estado en la misma llamada, y `cerrar()` revisa que el plan exista antes de transicionar. En la base de datos se guarda en columnas de la misma tabla `hallazgos` (`planResponsable`, `planFechaLimite`, `planNotas`), así que un solo `save` persiste todo el agregado.

### Correcciones al código de la guía

Al revisar el enunciado contra sus propios checkpoints encontré varias cosas que no funcionaban tal cual y las corregí:

1. **El adaptador reconstruía el hallazgo repitiendo las transiciones.** El `toDomain` de la guía llamaba a `iniciarRemediacion`, `cerrar()` y `reabrir()` para volver a armar el estado. Eso falla con un hallazgo REABIERTO (intenta ir de EN_REMEDIACION a REABIERTO y lanza `TransicionInvalidaException`, o sea que el checkpoint de reabrir no se podía cumplir dos veces) y además `cerrar()` pone `fechaCierre = LocalDate.now()` cada vez que se lee, lo que daña la fecha real de cierre. Lo cambié por una fábrica [`HallazgoAuditoria.reconstituir(...)`](src/main/java/com/example/auditoria/domain/entity/HallazgoAuditoria.java#L44-L59) que restaura el estado guardado sin volver a ejecutar reglas, y el [adaptador](src/main/java/com/example/auditoria/adapter/out/persistence/HallazgoRepositoryAdapter.java#L37-L44) la usa.
2. **`ConsultarHallazgoUseCase` devolvía `HallazgoResponse`.** Ese DTO está en `adapter/in/web/dto`, entonces el círculo de Use Cases dependía de un círculo externo y se rompía la regla de dependencia. Ahora el caso de uso [devuelve `HallazgoAuditoria`](src/main/java/com/example/auditoria/usecase/ConsultarHallazgoUseCase.java#L8-L13) y el controller hace la conversión con [`HallazgoResponse.desde(...)`](src/main/java/com/example/auditoria/adapter/in/web/dto/HallazgoResponse.java#L22-L36).
3. **El 400 del checkpoint no tenía quién lo produjera.** La guía pide que cerrar un hallazgo ABIERTO responda 400, pero sin manejador Spring devuelve 500 para una excepción no controlada. Agregué [`ManejadorErroresWeb`](src/main/java/com/example/auditoria/adapter/in/web/ManejadorErroresWeb.java#L17-L35) que traduce `TransicionInvalidaException`, `IllegalStateException`, `IllegalArgumentException` y los errores de validación a 400, y `HallazgoNotFoundException` a 404.
4. **`iniciarRemediacion` cambiaba el estado antes de validar el plan.** Con un plan nulo el hallazgo quedaba EN_REMEDIACION y después saltaba la excepción. [Validé el plan primero](src/main/java/com/example/auditoria/domain/entity/HallazgoAuditoria.java#L61-L66) para que el agregado nunca quede en un estado inválido.
5. **Partes que la guía deja incompletas:** constructores de los servicios, `HallazgoNotFoundException` (la usa pero no aparece en la estructura, la dejé en `usecase/`), `buscarTodos()` en el adaptador y los cinco beans en la configuración.
6. **Validación y nombres explícitos.** La dependencia Validation estaba en el enunciado pero no se usaba, así que los DTOs tienen `@NotBlank`/`@NotNull` con mensajes en español. También dejé `@PathVariable("id")` con el nombre explícito y `<parameters>true</parameters>` en el `pom.xml`, para que el controller no dependa de cómo se compile.

## Cómo ejecutar

Requisitos: JDK 17 o superior y Maven 3.8+.

```
mvn clean package
mvn spring-boot:run
```

La API queda en `http://localhost:8080/api/hallazgos`. La base es H2 en memoria, así que cada vez que se reinicia la aplicación arranca vacía.

| Método | Ruta | Respuesta esperada |
|---|---|---|
| POST | `/api/hallazgos` | 201 con `hallazgoId` (UUID) |
| PATCH | `/api/hallazgos/{id}/iniciar-remediacion` | 200 y estado EN_REMEDIACION |
| PATCH | `/api/hallazgos/{id}/cerrar` | 200 si tiene plan, 400 si está ABIERTO |
| PATCH | `/api/hallazgos/{id}/reabrir` | 200 si está CERRADO, 400 en otro caso |
| GET | `/api/hallazgos/{id}` | 200 con el hallazgo, 404 si no existe |
| GET | `/api/hallazgos` | 200 con la lista |

## Capturas (Parte 1)

**Registro de hallazgos:** dos registros con 201 y un tercero sin título que responde 400 por validación.

![Registro](docs/capturas/p1-01-registrar.png)

**Transiciones de estado:** iniciar remediación (200), cerrar un hallazgo ABIERTO sin plan (400), reabrir uno ABIERTO (400), cerrar el que está en remediación (200) y reabrirlo (200).

![Transiciones](docs/capturas/p1-02-transiciones.png)

**Listado en el navegador:** el primer hallazgo quedó REABIERTO con `fechaCierre` en null y su plan conservado; el segundo sigue ABIERTO porque los intentos inválidos no lo modificaron.

![Listado](docs/capturas/p1-03-listar-navegador.png)

**Consulta por id en el navegador:**

![Consulta por id](docs/capturas/p1-04-buscar-navegador.png)

## Pruebas

En `src/test/java` hay pruebas JUnit 5 que no levantan Spring: `HallazgoAuditoriaTest` instancia el agregado y recorre todo el ciclo, `EstadoHallazgoTest` revisa la máquina de estados y `CasosDeUsoHallazgoTest` prueba los servicios con un repositorio en memoria. Se ejecutan con `mvn test`.

## Herramientas utilizadas

- Java 17, Spring Boot 3.5, Spring Web, Spring Data JPA, Validation, H2
- Apache Maven, curl, Git, GitHub
