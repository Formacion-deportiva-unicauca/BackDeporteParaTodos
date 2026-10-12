package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
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

import java.util.List;
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

    @InjectMocks
    private InstructorServicio instructorServicio;

    // SCRUM-79: captura el log real de ch.qos.logback (no se mockea el Logger
    // estatico de la clase) para confirmar el rastro de auditoria del correo.
    private ListAppender<ILoggingEvent> logAppender;
    private Logger logbackLogger;

    @BeforeEach
    void capturarLogs() {
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
}
