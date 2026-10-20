package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPlazoCargaAtencionesGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.MesFuturoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.PlazoCargaInvalidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ProrrogaMesFuturoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PlazoCargaAtenciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AvisoRegistroDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.EstadoRegistroDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PlazoCargaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ProrrogaDto;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstructorServicioTest {

    @Mock
    private IInstructorGateway instructorsGateway;

    @Mock
    private IPerfilGateway perfilGateway;

    @Mock
    private IPlazoCargaAtencionesGateway plazoCargaGateway;

    @Mock
    private IProrrogaRegistroAsistenciaGateway prorrogaGateway;

    @Mock
    private IGrupoGateway grupoGateway;

    @Mock
    private IClaseGateway claseGateway;

    @Mock
    private Clock clock;

    @InjectMocks
    private InstructorServicio instructorServicio;

    // SCRUM-80: periodo academico 2 (mes >= 7) para el 2026-10-12, igual patron
    // de GrupoServicioTest -- lenient() porque no todos los tests de esta clase
    // usan el Clock.
    private static final LocalDate HOY_FIJO = LocalDate.of(2026, 10, 12);

    // SCRUM-79: captura el log real de ch.qos.logback (no se mockea el Logger
    // estatico de la clase) para confirmar el rastro de auditoria del correo.
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logbackLogger;

    // SonarCloud java:S8745: una sola clase no puede declarar mas de un metodo
    // @BeforeEach -- se fusionan aqui la configuracion del Clock (SCRUM-80) y la
    // captura de logs (SCRUM-79), antes en metodos separados.
    @BeforeEach
    void configurarMocksYCapturarLogs() {
        lenient().when(clock.instant()).thenReturn(HOY_FIJO.atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));

        logbackLogger = (Logger) LoggerFactory.getLogger(InstructorServicio.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logbackLogger.addAppender(logAppender);
    }

    @AfterEach
    void quitarCapturaDeLogs() {
        logbackLogger.detachAppender(logAppender);
    }

    private static final String ID      = "98765432";
    private static final String NOMBRE  = "Prof. García";
    private static final String CORREO  = "garcia@unicauca.edu.co";
    private static final String TIPO_ID = "CC";
    private static final String SEXO    = "M";

    private Instructor instructorConPerfil() {
        Perfil perfil = new Perfil();
        perfil.setId(ID);
        perfil.setNombre(NOMBRE);
        perfil.setCorreo(CORREO);
        perfil.setSexo(SEXO);
        Instructor inst = new Instructor();
        inst.setInst_codigo(ID);
        inst.setPerfil(perfil);
        return inst;
    }

    private PerfilDto dtoBase() {
        PerfilDto dto = new PerfilDto();
        dto.setId(ID);
        dto.setNombre(NOMBRE);
        dto.setCorreo(CORREO);
        dto.setTipoId(TIPO_ID);
        dto.setSexo(SEXO);
        dto.setTipoAlumno("Instructor");
        return dto;
    }

    // ── obtenerInstructores ─────────────────────────────────────────────────

    @Test
    void obtenerInstructores_conDatos_retornaListaDtos() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));

        List<InstructorDto> resultado = instructorServicio.obtenerInstructores();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals(ID,     resultado.get(0).getId());
        assertEquals(CORREO, resultado.get(0).getCorreo());
    }

    @Test
    void obtenerInstructores_sinDatos_lanzaListadoVacioExcepcion() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of());

        assertThrows(ListadoVacioExcepcion.class,
                () -> instructorServicio.obtenerInstructores());
    }

    // ── obtenerInstructor ───────────────────────────────────────────────────

    @Test
    void obtenerInstructor_existente_retornaInstructorDto() {
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));

        InstructorDto resultado = instructorServicio.obtenerInstructor(ID);

        assertNotNull(resultado);
        assertEquals(ID,     resultado.getId());
        assertEquals(NOMBRE, resultado.getNombre());
    }

    @Test
    void obtenerInstructor_noExistente_lanzaNoExisteExcepcion() {
        when(instructorsGateway.obtenerInstructor(anyString())).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> instructorServicio.obtenerInstructor("desconocido"));
    }

    // ── registrarInstructor ─────────────────────────────────────────────────

    @Test
    void registrarInstructor_sinAlumnoCodigo_seRegistraCorrectamente() {
        PerfilDto dto = new PerfilDto();
        dto.setId(ID);
        dto.setNombre(NOMBRE);
        dto.setCorreo(CORREO);
        dto.setTipoId(TIPO_ID);
        dto.setSexo(SEXO);
        dto.setTipoAlumno("Instructor");
        // alumnoCodigo NOT set — remains null (regression: SCRUM-137)

        when(instructorsGateway.existeInstructor(dto.getId())).thenReturn(false);
        when(instructorsGateway.registrarInstructor(any(Perfil.class), eq("Instructor")))
                .thenReturn(new Instructor());

        InstructorDto resultado = instructorServicio.registrarInstructor(dto);

        assertNotNull(resultado);
        verify(instructorsGateway).registrarInstructor(any(Perfil.class), eq("Instructor"));
    }

    @Test
    void registrarInstructor_yaExistente_lanzaYaExisteElementoExcepcion() {
        when(instructorsGateway.existeInstructor(ID)).thenReturn(true);

        assertThrows(YaExisteElementoExcepcion.class,
                () -> instructorServicio.registrarInstructor(dtoBase()));

        verify(instructorsGateway, never()).registrarInstructor(any(), any());
    }

    // ── eliminarInstructor ──────────────────────────────────────────────────
    // SCRUM-138: eliminarInstructor() hacia DELETE fisico (riesgo real de romper
    // FK_GRUPO_INSTRUCTOR -- RESTRICT -- y de borrar silenciosamente la atribucion
    // historica en tbl_clase via FK_CLASE_INSTRUCTOR ON DELETE SET NULL). Mismo
    // patron exacto de HorarioServicio.eliminarHorario(): existeInstructor() (404),
    // luego obtenerInstructor() para leer el estado actual y guardar contra 409.

    @Test
    void eliminarInstructor_noExiste_lanzaNoExisteExcepcion() {
        when(instructorsGateway.existeInstructor(ID)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.eliminarInstructor(ID));

        verify(instructorsGateway, never()).eliminarInstructor(any());
    }

    @Test
    void eliminarInstructor_exitoso_delegaAlGateway() {
        Instructor activo = instructorConPerfil();
        activo.setEliminado(0);
        Instructor eliminado = instructorConPerfil();
        eliminado.setEliminado(1);
        when(instructorsGateway.existeInstructor(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(activo));
        when(instructorsGateway.eliminarInstructor(ID)).thenReturn(eliminado);

        Instructor resultado = instructorServicio.eliminarInstructor(ID);

        assertNotNull(resultado);
        verify(instructorsGateway).eliminarInstructor(ID);
    }

    // Llamar eliminarInstructor() dos veces seguidas: la primera exitosa, la segunda
    // debe dar 409 en vez de reintentar el borrado silenciosamente.
    @Test
    void eliminarInstructor_yaEliminado_lanzaYaExisteElementoExcepcion() {
        Instructor yaEliminado = instructorConPerfil();
        yaEliminado.setEliminado(1);
        when(instructorsGateway.existeInstructor(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(yaEliminado));

        YaExisteElementoExcepcion ex = assertThrows(YaExisteElementoExcepcion.class,
                () -> instructorServicio.eliminarInstructor(ID));

        assertEquals("El instructor ya se encuentra eliminado", ex.getMessage());
        verify(instructorsGateway, never()).eliminarInstructor(any());
    }

    // ── actualizarInstructor (SCRUM-79, HE-14 HU-01) ─────────────────────────
    // Reutiliza perfilGateway.actualizarPerfil (SCRUM-182: saveAndFlush +
    // YaExisteElementoExcepcion en 409 por correo duplicado) -- no se duplica esa
    // logica aqui. existeInstructorActivo ya distingue inexistente/eliminado/
    // no-instructor en una sola consulta (true solo si existe y no eliminado).

    private static final String ID_COORDINADOR = "coord-99";
    private static final String NOMBRE_NUEVO = "Prof. García Actualizado";
    private static final String CORREO_NUEVO = "garcia.nuevo@unicauca.edu.co";

    private Instructor datosActualizar(String nombre, String correo) {
        Perfil perfil = new Perfil();
        perfil.setNombre(nombre);
        perfil.setCorreo(correo);
        Instructor datos = new Instructor();
        datos.setPerfil(perfil);
        return datos;
    }

    @Test
    void actualizarInstructor_casoFeliz_actualizaNombreYCorreoYRetornaInstructor() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));
        Perfil perfilActualizado = new Perfil();
        perfilActualizado.setId(ID);
        perfilActualizado.setNombre(NOMBRE_NUEVO);
        perfilActualizado.setCorreo(CORREO_NUEVO);
        perfilActualizado.setSexo(SEXO);
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(perfilActualizado);

        Instructor resultado = instructorServicio.actualizarInstructor(
                ID, datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), ID_COORDINADOR);

        assertNotNull(resultado);
        assertEquals(NOMBRE_NUEVO, resultado.getPerfil().getNombre());
        assertEquals(CORREO_NUEVO, resultado.getPerfil().getCorreo());
        verify(perfilGateway).actualizarPerfil(eq(ID), any(Perfil.class));
    }

    @Test
    void actualizarInstructor_instructorInexistente_lanzaNoExisteExcepcionSinTocarPerfil() {
        when(instructorsGateway.existeInstructorActivo("desconocido")).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.actualizarInstructor(
                "desconocido", datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), ID_COORDINADOR));

        verify(perfilGateway, never()).actualizarPerfil(any(), any());
    }

    // existeInstructorActivo ya filtra eliminado=1 -- desde este servicio,
    // "eliminado" e "inexistente" son el mismo caso (false), pero se deja un test
    // explicito con ese nombre porque es un escenario de negocio distinto.
    @Test
    void actualizarInstructor_instructorEliminado_lanzaNoExisteExcepcionSinTocarPerfil() {
        when(instructorsGateway.existeInstructorActivo("elim-1")).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.actualizarInstructor(
                "elim-1", datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), ID_COORDINADOR));

        verify(perfilGateway, never()).actualizarPerfil(any(), any());
    }

    // Un perfil que existe pero NO es instructor (p.ej. solo alumno): existsById
    // en tbl_instructor es false -> existeInstructorActivo tambien false -> 404.
    // Impide usar este endpoint para editar el perfil de otro rol.
    @Test
    void actualizarInstructor_perfilQueNoEsInstructor_lanzaNoExisteExcepcionSinTocarPerfil() {
        when(instructorsGateway.existeInstructorActivo("solo-alumno-1")).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.actualizarInstructor(
                "solo-alumno-1", datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), ID_COORDINADOR));

        verify(perfilGateway, never()).actualizarPerfil(any(), any());
    }

    @Test
    void actualizarInstructor_correoDuplicado_propagaYaExisteElementoExcepcion() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class)))
                .thenThrow(new YaExisteElementoExcepcion("El correo " + CORREO_NUEVO + " ya esta en uso por otro perfil"));

        assertThrows(YaExisteElementoExcepcion.class, () -> instructorServicio.actualizarInstructor(
                ID, datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), ID_COORDINADOR));
    }

    // HALLAZGO (verificacion SCRUM-79/139): confirmado con una sonda real que
    // mapper.map(PerfilEntidad, Perfil.class) (ModelMapper generico, sin
    // configuracion custom) pierde nombre/correo/tipoId/imagen en el mapeo plano
    // -- solo "sexo" sobrevive por casualidad. Este test simula exactamente eso
    // (actualizarPerfil devuelve un Perfil casi vacio) y prueba que la respuesta
    // de actualizarInstructor() NO depende de ese valor de retorno: sexo debe
    // seguir viniendo de `actual` (mapeo manual confiable de InstructorGateway).
    @Test
    void actualizarInstructor_elResultadoPreservaSexoAunqueActualizarPerfilDevuelvaUnPerfilIncompleto() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));
        // Simula el bug real de ModelMapper: el Perfil "retornado" viene casi vacio.
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(new Perfil());

        Instructor resultado = instructorServicio.actualizarInstructor(
                ID, datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), ID_COORDINADOR);

        assertEquals(SEXO, resultado.getPerfil().getSexo(),
                "el sexo original debe preservarse en la respuesta aunque actualizarPerfil() "
                        + "devuelva un Perfil con el mapeo incompleto");
        assertEquals(NOMBRE_NUEVO, resultado.getPerfil().getNombre());
        assertEquals(CORREO_NUEVO, resultado.getPerfil().getCorreo());
    }

    @Test
    void actualizarInstructor_mismoCorreoPropio_actualizaSinLanzarExcepcion() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));
        Perfil perfilActualizado = new Perfil();
        perfilActualizado.setId(ID);
        perfilActualizado.setNombre(NOMBRE_NUEVO);
        perfilActualizado.setCorreo(CORREO); // mismo correo que ya tenia
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(perfilActualizado);

        Instructor resultado = assertDoesNotThrow(() -> instructorServicio.actualizarInstructor(
                ID, datosActualizar(NOMBRE_NUEVO, CORREO), ID_COORDINADOR));

        assertEquals(CORREO, resultado.getPerfil().getCorreo());
    }

    // ── Rastro de auditoria: log SOLO cuando el correo cambia, nunca con datos
    // personales (correo/nombre) en el mensaje -- solo identificadores. ──────

    @Test
    void actualizarInstructor_correoCambia_emiteUnLogInfoConSoloIdentificadores() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));
        Perfil perfilActualizado = new Perfil();
        perfilActualizado.setCorreo(CORREO_NUEVO);
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(perfilActualizado);

        instructorServicio.actualizarInstructor(ID, datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), ID_COORDINADOR);

        assertEquals(1, logAppender.list.size(), "debe emitirse exactamente una linea de log");
        String mensaje = logAppender.list.get(0).getFormattedMessage();
        assertTrue(mensaje.contains(ID), "debe mencionar el id del instructor");
        assertTrue(mensaje.contains(ID_COORDINADOR), "debe mencionar el perf_id del coordinador");
        assertFalse(mensaje.toLowerCase().contains(CORREO.toLowerCase()), "no debe contener el correo anterior");
        assertFalse(mensaje.toLowerCase().contains(CORREO_NUEVO.toLowerCase()), "no debe contener el correo nuevo");
        assertFalse(mensaje.contains(NOMBRE_NUEVO), "no debe contener el nombre");
    }

    // Seguridad (Sonar java:S5145, log forging): un id (o claim del coordinador)
    // con CR/LF no puede forjar lineas de log falsas ni partir la linea de
    // auditoria en varias -- debe sanitizarse a "_" antes de registrar.
    @Test
    void actualizarInstructor_idConSaltosDeLinea_noProduceLogMultilineaYSinCorreos() {
        String idConCrlf = "98765432\r\nFAKE LOG LINE: acceso concedido\n";
        String coordinadorConCrlf = "coord-99\nFAKE LOG LINE: otra inyeccion";
        Instructor actualConIdSucio = instructorConPerfil();
        actualConIdSucio.setInst_codigo(idConCrlf);
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(actualConIdSucio));
        Perfil perfilActualizado = new Perfil();
        perfilActualizado.setCorreo(CORREO_NUEVO);
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(perfilActualizado);

        instructorServicio.actualizarInstructor(ID, datosActualizar(NOMBRE_NUEVO, CORREO_NUEVO), coordinadorConCrlf);

        assertEquals(1, logAppender.list.size(),
                "un id/claim con saltos de linea no debe producir mas de UNA linea de log");
        String mensaje = logAppender.list.get(0).getFormattedMessage();
        assertFalse(mensaje.contains("\r"), "no debe quedar ningun retorno de carro crudo");
        assertFalse(mensaje.contains("\n"), "no debe quedar ningun salto de linea crudo");
        assertEquals(1, mensaje.lines().count(), "la linea de auditoria debe seguir siendo UNA sola linea");
        assertTrue(mensaje.contains("98765432__FAKE LOG LINE: acceso concedido_"),
                "el id sanitizado (CR/LF -> _, uno por cada caracter) debe seguir siendo identificable");
        assertTrue(mensaje.contains("coord-99_FAKE LOG LINE: otra inyeccion"),
                "el claim del coordinador sanitizado debe seguir siendo identificable");
        assertFalse(mensaje.toLowerCase().contains(CORREO.toLowerCase()), "no debe contener el correo anterior");
        assertFalse(mensaje.toLowerCase().contains(CORREO_NUEVO.toLowerCase()), "no debe contener el correo nuevo");
    }

    @Test
    void actualizarInstructor_correoNoCambia_noEmiteNingunLog() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));
        Perfil perfilActualizado = new Perfil();
        perfilActualizado.setCorreo(CORREO);
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(perfilActualizado);

        instructorServicio.actualizarInstructor(ID, datosActualizar(NOMBRE_NUEVO, CORREO), ID_COORDINADOR);

        assertTrue(logAppender.list.isEmpty(), "no debe emitirse ningun log si el correo no cambia");
    }

    // Comparacion sin distinguir mayusculas y con trim -- mismo correo con distinta
    // capitalizacion/espacios no cuenta como cambio para el log.
    @Test
    void actualizarInstructor_correoSoloCambiaEnMayusculasYEspacios_noEmiteLog() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.obtenerInstructor(ID)).thenReturn(Optional.of(instructorConPerfil()));
        String correoConEspaciosYMayusculas = "  " + CORREO.toUpperCase() + "  ";
        Perfil perfilActualizado = new Perfil();
        perfilActualizado.setCorreo(CORREO);
        when(perfilGateway.actualizarPerfil(eq(ID), any(Perfil.class))).thenReturn(perfilActualizado);

        instructorServicio.actualizarInstructor(
                ID, datosActualizar(NOMBRE_NUEVO, correoConEspaciosYMayusculas), ID_COORDINADOR);

        assertTrue(logAppender.list.isEmpty());
    }

    // ── asignarPlazoCarga / obtenerPlazosCarga (SCRUM-80, HE-14 HU-02) ──────

    private static final String ID_INSTRUCTOR_B = "ins-b";

    @Test
    void asignarPlazoCarga_instructorActivoFechaFutura_hacenUpsertConElPeriodoVigente() {
        LocalDate fechaLimite = HOY_FIJO.plusDays(10);
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(plazoCargaGateway.upsertPlazo(any(PlazoCargaAtenciones.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plazoCargaGateway.contarClasesConAsistenciaPorInstructor(2026, 2)).thenReturn(Map.of());

        PlazoCargaDto resultado = instructorServicio.asignarPlazoCarga(ID, fechaLimite, ID_COORDINADOR);

        assertEquals(ID, resultado.getIdInstructor());
        assertEquals(2026, resultado.getAnio());
        assertEquals(2, resultado.getPeriodo());
        assertEquals(fechaLimite.toString(), resultado.getFechaLimite());
        assertEquals(EstadoReporte.PENDIENTE.name(), resultado.getEstadoReporte());
        assertFalse(resultado.isPlazoVencido());

        verify(plazoCargaGateway).upsertPlazo(argThat(datos ->
                datos.getIdInstructor().equals(ID)
                        && datos.getAnio() == 2026
                        && datos.getPeriodo() == 2
                        && datos.getFechaLimite().equals(fechaLimite)
                        && datos.getIdCoordinador().equals(ID_COORDINADOR)
                        && datos.getFechaAsignacion() != null));
    }

    @Test
    void asignarPlazoCarga_fechaIgualAHoy_esValidaPorDiaInclusivo() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(plazoCargaGateway.upsertPlazo(any(PlazoCargaAtenciones.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plazoCargaGateway.contarClasesConAsistenciaPorInstructor(2026, 2)).thenReturn(Map.of());

        PlazoCargaDto resultado = assertDoesNotThrow(
                () -> instructorServicio.asignarPlazoCarga(ID, HOY_FIJO, ID_COORDINADOR));

        assertEquals(EstadoReporte.PENDIENTE.name(), resultado.getEstadoReporte());
    }

    @Test
    void asignarPlazoCarga_fechaAnteriorAHoy_lanzaPlazoCargaInvalidoExcepcionSinUpsert() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);

        assertThrows(PlazoCargaInvalidoExcepcion.class, () -> instructorServicio.asignarPlazoCarga(
                ID, HOY_FIJO.minusDays(1), ID_COORDINADOR));

        verify(plazoCargaGateway, never()).upsertPlazo(any());
    }

    @Test
    void asignarPlazoCarga_instructorInexistente_lanzaNoExisteExcepcionSinUpsert() {
        when(instructorsGateway.existeInstructorActivo("desconocido")).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.asignarPlazoCarga(
                "desconocido", HOY_FIJO.plusDays(1), ID_COORDINADOR));

        verify(plazoCargaGateway, never()).upsertPlazo(any());
    }

    @Test
    void asignarPlazoCarga_instructorEliminado_lanzaNoExisteExcepcionSinUpsert() {
        when(instructorsGateway.existeInstructorActivo("elim-1")).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.asignarPlazoCarga(
                "elim-1", HOY_FIJO.plusDays(1), ID_COORDINADOR));

        verify(plazoCargaGateway, never()).upsertPlazo(any());
    }

    @Test
    void asignarPlazoCarga_mismoValorDosVeces_esIdempotente() {
        LocalDate fechaLimite = HOY_FIJO.plusDays(5);
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(plazoCargaGateway.upsertPlazo(any(PlazoCargaAtenciones.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plazoCargaGateway.contarClasesConAsistenciaPorInstructor(2026, 2)).thenReturn(Map.of());

        PlazoCargaDto primero = instructorServicio.asignarPlazoCarga(ID, fechaLimite, ID_COORDINADOR);
        PlazoCargaDto segundo = instructorServicio.asignarPlazoCarga(ID, fechaLimite, ID_COORDINADOR);

        assertEquals(primero.getFechaLimite(), segundo.getFechaLimite());
        verify(plazoCargaGateway, times(2)).upsertPlazo(any(PlazoCargaAtenciones.class));
    }

    // Auditoria: log INFO con solo ids y la fecha -- nunca correos ni nombres.
    @Test
    void asignarPlazoCarga_casoFeliz_emiteUnLogInfoConSoloIdentificadoresYFecha() {
        LocalDate fechaLimite = HOY_FIJO.plusDays(10);
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(plazoCargaGateway.upsertPlazo(any(PlazoCargaAtenciones.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plazoCargaGateway.contarClasesConAsistenciaPorInstructor(2026, 2)).thenReturn(Map.of());

        instructorServicio.asignarPlazoCarga(ID, fechaLimite, ID_COORDINADOR);

        assertEquals(1, logAppender.list.size());
        String mensaje = logAppender.list.get(0).getFormattedMessage();
        assertTrue(mensaje.contains(ID));
        assertTrue(mensaje.contains(ID_COORDINADOR));
        assertTrue(mensaje.contains(fechaLimite.toString()));
    }

    // Sonar java:S5145 (log forging): instructorId/idCoordinador con CR/LF no
    // pueden forjar lineas de log falsas en la auditoria del plazo de carga.
    @Test
    void asignarPlazoCarga_idsConSaltosDeLinea_noProduceLogMultilinea() {
        String idConCrlf = ID + "\r\nFAKE LOG LINE";
        String coordinadorConCrlf = ID_COORDINADOR + "\nOTRA INYECCION";
        LocalDate fechaLimite = HOY_FIJO.plusDays(10);
        when(instructorsGateway.existeInstructorActivo(idConCrlf)).thenReturn(true);
        when(plazoCargaGateway.upsertPlazo(any(PlazoCargaAtenciones.class))).thenAnswer(inv -> inv.getArgument(0));
        when(plazoCargaGateway.contarClasesConAsistenciaPorInstructor(2026, 2)).thenReturn(Map.of());

        instructorServicio.asignarPlazoCarga(idConCrlf, fechaLimite, coordinadorConCrlf);

        assertEquals(1, logAppender.list.size());
        String mensaje = logAppender.list.get(0).getFormattedMessage();
        assertEquals(1, mensaje.lines().count());
        assertFalse(mensaje.contains("\r"));
        assertFalse(mensaje.contains("\n"));
    }

    @Test
    void obtenerPlazosCarga_unInstructorConPlazoYOtroSinPlazo_retornaUnElementoPorInstructorActivo() {
        Instructor instructorA = instructorConPerfil();
        Instructor instructorB = new Instructor();
        instructorB.setInst_codigo(ID_INSTRUCTOR_B);
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorA, instructorB));

        PlazoCargaAtenciones plazoA = new PlazoCargaAtenciones();
        plazoA.setIdInstructor(ID);
        plazoA.setAnio(2026);
        plazoA.setPeriodo(2);
        plazoA.setFechaLimite(HOY_FIJO.minusDays(1));
        when(plazoCargaGateway.obtenerPlazosPorPeriodo(2026, 2)).thenReturn(Map.of(ID, plazoA));
        when(plazoCargaGateway.contarClasesConAsistenciaPorInstructor(2026, 2)).thenReturn(Map.of(ID, 2));

        List<PlazoCargaDto> resultado = instructorServicio.obtenerPlazosCarga();

        assertEquals(2, resultado.size());
        PlazoCargaDto dtoA = resultado.stream().filter(d -> d.getIdInstructor().equals(ID)).findFirst().orElseThrow();
        assertEquals(EstadoReporte.GENERADO.name(), dtoA.getEstadoReporte(),
                "tiene clases con asistencia -> GENERADO aunque el plazo este vencido");
        assertTrue(dtoA.isPlazoVencido(), "plazoVencido es independiente del estado GENERADO");
        assertEquals(2, dtoA.getClasesConAsistencia());

        PlazoCargaDto dtoB = resultado.stream().filter(d -> d.getIdInstructor().equals(ID_INSTRUCTOR_B)).findFirst().orElseThrow();
        assertEquals(EstadoReporte.SIN_PLAZO.name(), dtoB.getEstadoReporte());
        assertNull(dtoB.getFechaLimite());
        assertEquals(0, dtoB.getClasesConAsistencia());
    }

    @Test
    void obtenerPlazosCarga_sinInstructoresActivos_retornaListaVacia() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of());

        List<PlazoCargaDto> resultado = instructorServicio.obtenerPlazosCarga();

        assertTrue(resultado.isEmpty());
        verify(plazoCargaGateway, never()).obtenerPlazosPorPeriodo(anyInt(), anyInt());
        verify(plazoCargaGateway, never()).contarClasesConAsistenciaPorInstructor(anyInt(), anyInt());
    }

    // ── asignarProrrogaRegistro / obtenerProrrogasRegistro (SCRUM-80, rediseno bloque 1) ──
    // HOY_FIJO = 2026-10-12 -> mes actual = (2026, 10).

    private static final String ID_B = "ins-b";

    @Test
    void asignarProrrogaRegistro_variosInstructoresActivos_asignaATodosYRetornaDtos() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.existeInstructorActivo(ID_B)).thenReturn(true);
        when(prorrogaGateway.upsertProrroga(any(ProrrogaRegistroAsistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ProrrogaDto> resultado = instructorServicio.asignarProrrogaRegistro(
                List.of(ID, ID_B), 2026, 10, 10, ID_COORDINADOR);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(d -> d.getDias() == 10));
        // finDeMes(2026,10) = 2026-10-31; fechaAsignacion = HOY_FIJO = 2026-10-12 ->
        // base = finDeMes (posterior) -> limite = 2026-10-31 + 10 = 2026-11-10.
        assertTrue(resultado.stream().allMatch(d -> "2026-11-10".equals(d.getFechaLimite())));
        verify(prorrogaGateway, times(2)).upsertProrroga(any(ProrrogaRegistroAsistencia.class));
    }

    @Test
    void asignarProrrogaRegistro_unIdInactivo_lanzaNoExisteExcepcionYNoGuardaNadaAtomico() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(instructorsGateway.existeInstructorActivo("desconocido")).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.asignarProrrogaRegistro(
                List.of(ID, "desconocido"), 2026, 10, 10, ID_COORDINADOR));

        verify(prorrogaGateway, never()).upsertProrroga(any());
        verify(prorrogaGateway, never()).eliminarProrroga(any(), anyInt(), anyInt());
    }

    @Test
    void asignarProrrogaRegistro_idsDuplicados_seDeduplicanYSeProcesaUnaSolaVez() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(prorrogaGateway.upsertProrroga(any(ProrrogaRegistroAsistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ProrrogaDto> resultado = instructorServicio.asignarProrrogaRegistro(
                List.of(ID, ID, ID), 2026, 10, 10, ID_COORDINADOR);

        assertEquals(1, resultado.size(), "los ids duplicados se deduplican");
        verify(prorrogaGateway, times(1)).upsertProrroga(any(ProrrogaRegistroAsistencia.class));
        verify(instructorsGateway, times(1)).existeInstructorActivo(ID);
    }

    @Test
    void asignarProrrogaRegistro_diasCero_eliminaLaProrrogaYRetornaFechaLimiteNula() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);

        List<ProrrogaDto> resultado = instructorServicio.asignarProrrogaRegistro(
                List.of(ID), 2026, 10, 0, ID_COORDINADOR);

        assertEquals(1, resultado.size());
        assertEquals(0, resultado.get(0).getDias());
        assertNull(resultado.get(0).getFechaLimite());
        verify(prorrogaGateway).eliminarProrroga(ID, 2026, 10);
        verify(prorrogaGateway, never()).upsertProrroga(any());
    }

    @Test
    void asignarProrrogaRegistro_mesFuturo_lanzaProrrogaMesFuturoExcepcionSinGuardarNada() {
        assertThrows(ProrrogaMesFuturoExcepcion.class, () -> instructorServicio.asignarProrrogaRegistro(
                List.of(ID), 2026, 11, 10, ID_COORDINADOR));

        verify(prorrogaGateway, never()).upsertProrroga(any());
        verify(instructorsGateway, never()).existeInstructorActivo(any());
    }

    @Test
    void asignarProrrogaRegistro_mesActual_esPermitidoNoEsFuturo() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(prorrogaGateway.upsertProrroga(any(ProrrogaRegistroAsistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> instructorServicio.asignarProrrogaRegistro(
                List.of(ID), 2026, 10, 10, ID_COORDINADOR));
    }

    // Reabrir un mes YA vencido: anio/mes = septiembre 2026 (anterior a HOY_FIJO,
    // octubre 2026) -- no es futuro, asi que se permite asignar la prorroga, y el
    // limite se calcula desde HOY_FIJO (fecha de asignacion), no desde finDeMes.
    @Test
    void asignarProrrogaRegistro_mesAnteriorYaVencido_reabreUsandoFechaDeAsignacionComoBase() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(prorrogaGateway.upsertProrroga(any(ProrrogaRegistroAsistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<ProrrogaDto> resultado = instructorServicio.asignarProrrogaRegistro(
                List.of(ID), 2026, 9, 5, ID_COORDINADOR);

        // finDeMes(2026,9) = 2026-09-30; fechaAsignacion = HOY_FIJO = 2026-10-12 ->
        // base = fechaAsignacion (posterior) -> limite = 2026-10-12 + 5 = 2026-10-17.
        assertEquals("2026-10-17", resultado.get(0).getFechaLimite());
    }

    @Test
    void asignarProrrogaRegistro_auditoria_emiteUnLogPorInstructorConSoloIdentificadores() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(prorrogaGateway.upsertProrroga(any(ProrrogaRegistroAsistencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        instructorServicio.asignarProrrogaRegistro(List.of(ID), 2026, 10, 10, ID_COORDINADOR);

        assertEquals(1, logAppender.list.size());
        String mensaje = logAppender.list.get(0).getFormattedMessage();
        assertTrue(mensaje.contains(ID));
        assertTrue(mensaje.contains(ID_COORDINADOR));
    }

    @Test
    void obtenerProrrogasRegistro_retornaListaDelMes() {
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setIdInstructor(ID);
        prorroga.setDias(10);
        prorroga.setFechaLimite(LocalDate.of(2026, 11, 10));
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 10)).thenReturn(Map.of(ID, prorroga));

        List<ProrrogaDto> resultado = instructorServicio.obtenerProrrogasRegistro(2026, 10);

        assertEquals(1, resultado.size());
        assertEquals(ID, resultado.get(0).getIdInstructor());
        assertEquals("2026-11-10", resultado.get(0).getFechaLimite());
    }

    @Test
    void obtenerProrrogasRegistro_sinProrrogasEnElMes_retornaListaVacia() {
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 10)).thenReturn(Map.of());

        assertTrue(instructorServicio.obtenerProrrogasRegistro(2026, 10).isEmpty());
    }

    @Test
    void obtenerProrrogasRegistro_mesFuturo_lanzaProrrogaMesFuturoExcepcion() {
        assertThrows(ProrrogaMesFuturoExcepcion.class,
                () -> instructorServicio.obtenerProrrogasRegistro(2026, 11));

        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void obtenerProrrogasRegistro_mesActual_esPermitido() {
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 10)).thenReturn(Map.of());

        assertDoesNotThrow(() -> instructorServicio.obtenerProrrogasRegistro(2026, 10));
    }

    // ── obtenerEstadoRegistro (SCRUM-80, rediseno bloque 3) ──────────────────
    // HOY_FIJO = 2026-10-12 -> mes actual = (2026, 10).

    private static final String CAT = "Deportes";
    private static final String CURSO = "Futbol";
    private static final int ANIO = 2026;
    private static final int ITER = 1;

    private Grupo grupoConPeriodo(LocalDate inicio, LocalDate fin) {
        Grupo g = new Grupo();
        g.setCategoria(CAT);
        g.setCurso(CURSO);
        g.setAnio(ANIO);
        g.setIterable(ITER);
        g.setFechaCreacion(inicio);
        g.setFechaFinalizacion(fin);
        return g;
    }

    private Clase claseEn(LocalDate fecha) {
        Clase c = new Clase();
        c.setFecha(fecha);
        return c;
    }

    private void sinProrrogasDelMes(int anio, int mes) {
        lenient().when(prorrogaGateway.obtenerProrrogasPorMes(anio, mes)).thenReturn(Map.of());
    }

    @Test
    void obtenerEstadoRegistro_conGrupoActivoYClaseEnElMes_retornaCompleto() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 10, 1), null)));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER))
                .thenReturn(List.of(claseEn(LocalDate.of(2026, 10, 5))));
        sinProrrogasDelMes(2026, 10);

        List<EstadoRegistroDto> resultado = instructorServicio.obtenerEstadoRegistro(null, null);

        assertEquals(1, resultado.size());
        EstadoRegistroDto dto = resultado.get(0);
        assertEquals(ID, dto.getIdInstructor());
        assertEquals(2026, dto.getAnio());
        assertEquals(10, dto.getMes());
        assertEquals("COMPLETO", dto.getEstado());
        assertEquals(1, dto.getGruposActivos());
        assertEquals(1, dto.getGruposRegistrados());
        assertEquals("2026-10-31", dto.getFechaLimite());
        assertEquals("2026-10-12", dto.getHoy());
    }

    @Test
    void obtenerEstadoRegistro_conGrupoActivoSinClase_retornaPendiente() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 10, 1), null)));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 10);

        List<EstadoRegistroDto> resultado = instructorServicio.obtenerEstadoRegistro(null, null);

        assertEquals("PENDIENTE", resultado.get(0).getEstado());
        assertEquals(1, resultado.get(0).getGruposActivos());
        assertEquals(0, resultado.get(0).getGruposRegistrados());
    }

    @Test
    void obtenerEstadoRegistro_claseEnOtroMes_noCuentaComoRegistrada() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 10, 1), null)));
        // Clase en septiembre: el grupo esta activo en octubre pero la clase no es de octubre.
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER))
                .thenReturn(List.of(claseEn(LocalDate.of(2026, 9, 30))));
        sinProrrogasDelMes(2026, 10);

        List<EstadoRegistroDto> resultado = instructorServicio.obtenerEstadoRegistro(null, null);

        assertEquals("PENDIENTE", resultado.get(0).getEstado());
        assertEquals(0, resultado.get(0).getGruposRegistrados());
    }

    @Test
    void obtenerEstadoRegistro_instructorSinGrupos_retornaSinGrupos() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));
        when(grupoGateway.obtenerGruposInstructor(ID)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 10);

        List<EstadoRegistroDto> resultado = instructorServicio.obtenerEstadoRegistro(null, null);

        assertEquals("SIN_GRUPOS", resultado.get(0).getEstado());
        assertEquals(0, resultado.get(0).getGruposActivos());
        verify(claseGateway, never()).obtenerClasesGrupo(any(), any(), anyInt(), anyInt());
    }

    @Test
    void obtenerEstadoRegistro_mesPasadoSinRegistro_retornaVencido() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 9);

        List<EstadoRegistroDto> resultado = instructorServicio.obtenerEstadoRegistro(2026, 9);

        assertEquals("VENCIDO", resultado.get(0).getEstado());
        assertEquals("2026-09-30", resultado.get(0).getFechaLimite());
    }

    @Test
    void obtenerEstadoRegistro_mesPasadoConProrrogaVigente_retornaPendiente() {
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER)).thenReturn(List.of());
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 20));
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(ID, prorroga));

        List<EstadoRegistroDto> resultado = instructorServicio.obtenerEstadoRegistro(2026, 9);

        assertEquals("PENDIENTE", resultado.get(0).getEstado());
        assertEquals("2026-10-20", resultado.get(0).getFechaLimite());
    }

    @Test
    void obtenerEstadoRegistro_dosGruposUnoRegistrado_retornaPendienteConConteos() {
        Grupo grupoA = grupoConPeriodo(LocalDate.of(2026, 10, 1), null);
        Grupo grupoB = new Grupo();
        grupoB.setCategoria(CAT);
        grupoB.setCurso("Baloncesto");
        grupoB.setAnio(ANIO);
        grupoB.setIterable(2);
        grupoB.setFechaCreacion(LocalDate.of(2026, 10, 1));
        when(instructorsGateway.obtenerInstructores()).thenReturn(List.of(instructorConPerfil()));
        when(grupoGateway.obtenerGruposInstructor(ID)).thenReturn(List.of(grupoA, grupoB));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER))
                .thenReturn(List.of(claseEn(LocalDate.of(2026, 10, 5))));
        lenient().when(claseGateway.obtenerClasesGrupo(CAT, "Baloncesto", ANIO, 2)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 10);

        List<EstadoRegistroDto> resultado = instructorServicio.obtenerEstadoRegistro(null, null);

        assertEquals(2, resultado.get(0).getGruposActivos());
        assertEquals(1, resultado.get(0).getGruposRegistrados());
        assertEquals("PENDIENTE", resultado.get(0).getEstado());
    }

    @Test
    void obtenerEstadoRegistro_mesFuturo_lanzaMesFuturoExcepcionSinConsultar() {
        assertThrows(MesFuturoExcepcion.class,
                () -> instructorServicio.obtenerEstadoRegistro(2026, 11));

        verify(grupoGateway, never()).obtenerGruposInstructor(any());
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void obtenerEstadoRegistro_anioFuturo_lanzaMesFuturoExcepcion() {
        assertThrows(MesFuturoExcepcion.class,
                () -> instructorServicio.obtenerEstadoRegistro(2027, 1));
    }

    // ── obtenerAvisosRegistro (SCRUM-80, rediseno bloque 3) ──────────────────

    @Test
    void obtenerAvisosRegistro_instructorInexistente_lanzaNoExisteExcepcion() {
        when(instructorsGateway.existeInstructorActivo("desconocido")).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> instructorServicio.obtenerAvisosRegistro("desconocido"));

        verify(grupoGateway, never()).obtenerGruposInstructor(any());
    }

    @Test
    void obtenerAvisosRegistro_idNulo_lanzaNoExisteExcepcion() {
        assertThrows(NoExisteExcepcion.class, () -> instructorServicio.obtenerAvisosRegistro(null));
    }

    @Test
    void obtenerAvisosRegistro_mesAnteriorIncompleto_retornaAvisoVencido() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        // Grupo activo SOLO en septiembre (mes anterior): en octubre no aplica.
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))));
        lenient().when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 9);
        sinProrrogasDelMes(2026, 10);

        List<AvisoRegistroDto> resultado = instructorServicio.obtenerAvisosRegistro(ID);

        assertEquals(1, resultado.size());
        assertEquals(2026, resultado.get(0).getAnio());
        assertEquals(9, resultado.get(0).getMes());
        assertEquals("VENCIDO", resultado.get(0).getEstado());
        assertEquals("2026-09-30", resultado.get(0).getFechaLimite());
    }

    @Test
    void obtenerAvisosRegistro_mesActualCompleto_noGeneraAvisos() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 10, 1), null)));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER))
                .thenReturn(List.of(claseEn(LocalDate.of(2026, 10, 5))));
        sinProrrogasDelMes(2026, 10);

        List<AvisoRegistroDto> resultado = instructorServicio.obtenerAvisosRegistro(ID);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void obtenerAvisosRegistro_sinGruposActivos_noGeneraAvisos() {
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(grupoGateway.obtenerGruposInstructor(ID)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 10);

        List<AvisoRegistroDto> resultado = instructorServicio.obtenerAvisosRegistro(ID);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void obtenerAvisosRegistro_mesActualConPocosDiasParaElLimite_retornaProximoAVencer() {
        // Hoy = 2026-10-29 (2 dias antes del fin de mes) -> PROXIMO_A_VENCER.
        lenient().when(clock.instant())
                .thenReturn(LocalDate.of(2026, 10, 29).atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 10, 1), null)));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 10);

        List<AvisoRegistroDto> resultado = instructorServicio.obtenerAvisosRegistro(ID);

        assertEquals(1, resultado.size());
        assertEquals("PROXIMO_A_VENCER", resultado.get(0).getEstado());
        assertEquals(2, resultado.get(0).getDiasRestantes());
    }

    @Test
    void obtenerAvisosRegistro_mesActualConMuchosDias_noGeneraAviso() {
        // HOY_FIJO = 2026-10-12, fin de mes = 2026-10-31 -> 19 dias > 3 -> sin aviso.
        when(instructorsGateway.existeInstructorActivo(ID)).thenReturn(true);
        when(grupoGateway.obtenerGruposInstructor(ID))
                .thenReturn(List.of(grupoConPeriodo(LocalDate.of(2026, 10, 1), null)));
        when(claseGateway.obtenerClasesGrupo(CAT, CURSO, ANIO, ITER)).thenReturn(List.of());
        sinProrrogasDelMes(2026, 10);

        List<AvisoRegistroDto> resultado = instructorServicio.obtenerAvisosRegistro(ID);

        assertTrue(resultado.isEmpty());
    }
}
