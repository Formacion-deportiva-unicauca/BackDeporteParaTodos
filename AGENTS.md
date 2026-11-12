# Instrucciones para Agentes de IA - DeporteParaTodos Backend

## Arquitectura del Proyecto

Este es un backend Spring Boot 3.2.3 con arquitectura hexagonal aplicada
a nivel monolítico. La refactorización en curso busca reducir acoplamiento,
mejorar mantenibilidad y facilitar futuras extensiones.

### Estructura de Capas

    dominio/
      modelo/          -> Entidades de dominio (Alumno, Curso, Grupo, etc.)
      servicios/       -> Lógica de negocio pura
    aplicacion/puertos/
      puertosEntrada/  -> Interfaces de servicios
      puertosSalida/   -> Interfaces gateway para persistencia
    infraestructura/
      adaptadores/
        adaptadoresPrimarios/    -> REST controllers (entradas)
        adaptadoresSecundarios/  -> SQL persistence, gateways (salidas)
      mappers/                   -> Configuración de ModelMapper
      controladorExcepciones/    -> Manejo centralizado de errores
      logs/                      -> Logging utilities

## Reglas que NUNCA debes romper

1. NO crear relaciones @OneToMany/@ManyToMany en entidades Java.
   Las relaciones se manejan con SQL explícito en los gateways.

2. El mapeo entidad -> modelo de dominio va SIEMPRE en el Gateway, nunca en el Service.

3. Los servicios lanzan SOLO estas excepciones personalizadas:
   - ListadoVacioExcepcion
   - NoExisteExcepcion
   - YaExisteElementoExcepcion
     Ubicadas en: infraestructura/controladorExcepciones/excepciones/

4. Los DTOs tienen método estático fabricarDeModelo(Modelo m).

5. NO modificar application.properties ni pom.xml salvo que se pida explícitamente.

6. Usar @Qualifier("modelMapperGenerico") para el ModelMapper genérico.

7. Siempre inyectar dependencias con @Autowired.

## Antes de escribir cualquier código

1. Lee la cadena completa de la entidad a modificar en este orden:
   modelo -> IServicio -> Servicio -> IGateway -> Gateway -> Entidad -> Rest -> DTO

2. Reporta exactamente qué encontraste (campos, métodos, endpoints existentes).

3. Indica qué vas a cambiar y en qué archivos, ANTES de escribir código.

4. Espera confirmación antes de proceder.

5. Por cada método nuevo en el Servicio, escribe su test JUnit 5 + Mockito:
   - Caso exitoso
   - Caso de entidad no encontrada (NoExisteExcepcion)

## Flujo de datos (seguir siempre este orden al implementar)

REST Controller -> IServicio (puerto entrada) -> Servicio (dominio)
-> IGateway (puerto salida) -> Gateway (infraestructura) -> Entidad JPA

## Convenciones de código

### DTOs

- Ubicación: infraestructura/adaptadores/adaptadoresPrimarios/DTOs/
- Siempre incluir método estático:
  public static MiDto fabricarDeModelo(MiModelo modelo) { ... }

### Servicios

- Ubicación: dominio/servicios/
- Implementan la interfaz de puertosEntrada/
- Contienen lógica de negocio y validaciones
- Lanzan excepciones personalizadas, nunca excepciones genéricas

### Gateways

- Ubicación: infraestructura/adaptadores/adaptadoresSecundarios/persistenciaSQL/gateway/
- Implementan la interfaz de puertosSalida/
- Manejan conversión ModelMapper <-> objeto de dominio
- Si se necesitan datos de múltiples entidades: usar JOIN en SQL, NO relaciones Java

### Logging

- private static final Logger LOGGER = LoggerFactory.getLogger(MiClase.class)
- Usar PeticionLogger para logs de peticiones HTTP

## Base de datos

- Oracle XE: jdbc:oracle:thin:@localhost:1521:xe
- Usuario: TEST / Password: test
- ddl-auto=none — el esquema lo gestiona Flyway (ver sección "Base de datos y migraciones")
- Puerto aplicación: 8082
- Swagger: http://localhost:8082/swagger-ui.html

## Base de datos y migraciones

- El esquema lo gestiona Flyway: migraciones versionadas en `db/flyway/V<n>__descripcion.sql`,
  numeración consecutiva; solo se agregan archivos nuevos.
- NUNCA editar una migración ya fusionada a `main` (ni sus comentarios): Flyway valida el
  checksum de cada archivo y fallaría en producción.
- CI aplica `db/flyway` + `db/ci` (datos ficticios). El `docker-compose.yml` local aplica
  `db/flyway` + `db/local` (datos de prueba).
- En producción las migraciones se aplican automáticamente antes del despliegue (imagen
  `deporte-migraciones` del mismo tag `vX.Y.Z`), con respaldo previo; si la migración falla,
  el backend no se despliega.
- Toda migración debe ser compatible hacia atrás con la versión anterior del backend
  (expandir/contraer): primero añadir, borrar o renombrar solo en una versión posterior.
- MySQL no revierte DDL: una migración fallida puede dejar cambios parciales; por eso las
  migraciones deben ser pequeñas e idempotentes cuando sea posible.
- `db/migraciones/` es histórico (ya incorporado en V1): no se usa para cambios nuevos.
- Los tags de versión se crean anotados: `git tag -a vX.Y.Z -m "..."`.

## Cómo agregar una entidad nueva (checklist)

1. Crear Entidad.java en dominio/modelo/
2. Crear EntidadEntidad.java (JPA) en adaptadoresSecundarios/persistenciaSQL/entidades/
3. Crear IEntidadGateway.java en aplicacion/puertos/puertosSalida/
4. Crear EntidadGateway.java en adaptadoresSecundarios/persistenciaSQL/gateway/
5. Crear IEntidadServicio.java en aplicacion/puertos/puertosEntrada/
6. Crear EntidadServicio.java en dominio/servicios/
7. Crear EntidadRest.java en adaptadoresPrimarios/adaptadorRest/api/
8. Crear DTOs en adaptadoresPrimarios/adaptadorRest/DTOs/

## Backlog priorizado — implementar en este orden

### Bloque 1 — Fundamento (sin esto el frontend no puede avanzar)

~~HE-03 HU-01 — Exponer estadoCurso en CursoDto~~ ✅ COMPLETADA
- EstadoCurso.java (enum ACTIVO/INACTIVO/CERRADO) existe en dominio/modelo/
- Curso.java y CursoDto.java tienen el campo estadoCurso
- Curso.fabricarDeEntidad mapea eliminado=1→INACTIVO, else→ACTIVO
- GET /cursos retorna estadoCurso en el JSON

~~HE-05 HU-01 — Deshabilitar curso deportivo~~ ✅ COMPLETADA
- PATCH /api/v2/curso/estado con @RequestParam prmCategoria, prmCurso, prmEstado
- Servicio: cambiarEstadoCurso — lanza NoExisteExcepcion si no existe
- Gateway: setEliminado(1) si INACTIVO, setEliminado(0) si ACTIVO

~~HE-05 HU-02 — Eliminación lógica de curso~~ ✅ COMPLETADA
- DELETE /api/v2/curso con @RequestParam prmCategoria, prmCurso
- Lanza NoExisteExcepcion (404) si no existe
- Lanza YaExisteElementoExcepcion (409) si estadoCurso ya es INACTIVO
- Gateway: setEliminado(1), NO delete físico

### Bloque 2 — Funcionalidades faltantes

~~HE-04 HU-02 — Campo horario en Curso~~ ✅ COMPLETADA
- CursoEntidad tiene @Column(name = "CUR_HORARIO") private String horario
- Curso.java y CursoDto.java tienen el campo horario
- POST y PUT persisten y exponen el campo horario

HE-04 HU-04 — Estado de inscripciones al crear curso

- Agregar campo estadoInscripciones al formulario de creación
- Opciones: ABIERTO / CERRADO
- Reflejar en CursoDto

HE-06 HU-04 — Cambiar estado de inscripciones de un curso

- Endpoint: PATCH /cursos/{id}/inscripciones
- Body: { "estadoInscripciones": "ABIERTO" }

~~HE-AT HU-03 — Eliminar atención de un deportista~~ ✅ COMPLETADA
- DELETE /api/v2/asistencia con @RequestParam prmPerfId, prmClsCodigo
- Borrado lógico: setEliminado(1) en TBL_ASISTENCIA
- Lanza NoExisteExcepcion (404) si no existe
- Lanza YaExisteElementoExcepcion (409) si ya estaba eliminada

HE-18 — Cambiar estado general de inscripciones (masivo)

- Endpoint: PATCH /cursos/inscripciones/abrir -> abre todos los cursos
- Endpoint: PATCH /cursos/inscripciones/cerrar -> cierra todos los cursos

HE-19 — Cambiar estado por tipo de curso

- Endpoint: PATCH /cursos/tipo/{tipoId}/inscripciones
- Body: { "estadoInscripciones": "ABIERTO" }

HE-20 — Cambiar estado de un deporte específico

- Ya cubierto por HE-06 HU-04

### Bloque 3 — Módulos nuevos

HE-13 HU-02 — Cursos asignados a un instructor

- Endpoint: GET /instructores/{id}/cursos
- Retorna lista de CursoDto asignados al instructor

HE-14 HU-02/03 — Habilitar/inhabilitar instructor

- Endpoint: PATCH /instructores/{id}/estado
- Body: { "estado": "ACTIVO" }

HE-15/16 — Estadísticas y exportación PDF

- Endpoints de consulta estadística por facultad, estamento, programa, sexo
- Exportación en PDF

HE-23/31/39 — Autenticación JWT + Spring Security

- Login por rol: ADMINISTRADOR, INSTRUCTOR, ESTUDIANTE
- Endpoints protegidos según rol
- Implementar al final para no bloquear desarrollo de funcionalidades

## Dependencias críticas del proyecto

- Spring Boot 3.2.3
- Spring Data JPA / Hibernate
- ModelMapper 3.0.0
- Lombok
- Validation (Jakarta)
- SpringDoc OpenAPI 2.3.0
- Oracle JDBC 19.8.0.0
