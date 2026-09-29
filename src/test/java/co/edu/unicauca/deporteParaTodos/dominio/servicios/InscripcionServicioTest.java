package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CuposAgotadosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InscripcionesCerradasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.LimiteCursosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Disponibilidad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class InscripcionServicioTest {

    @Mock
    private IInscripcionGateway gateway;

    @Mock
    private ICursoGateway cursoGateway;

    @Mock
    private IGrupoGateway grupoGateway;

    @InjectMocks
    private InscripcionServicio servicio;

    private Inscripcion datos;
    private Curso cursoAbierto;
    private Grupo grupoCon5Cupos;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(servicio, "limiteCursosAlumno", 3);
        datos = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, null, null, null);
        cursoAbierto = new Curso();
        cursoAbierto.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        cursoAbierto.setEstadoCurso(EstadoCurso.ACTIVO);
        grupoCon5Cupos = new Grupo();
        grupoCon5Cupos.setCupos(5);
    }

    // ── inscribir ─────────────────────────────────────────────────────────────

    @Test
    void inscribir_conInscripcionesCerradas_lanzaExcepcion() {
        Curso cursoCerrado = new Curso();
        cursoCerrado.setEstadoInscripciones(EstadoInscripciones.CERRADO);
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoCerrado);

        assertThrows(InscripcionesCerradasExcepcion.class, () -> servicio.inscribir(datos));
        verify(grupoGateway, never()).obtenerGrupoConLock(any(), any(), anyInt(), anyInt());
    }

    @Test
    void inscribir_cursoNoExiste_lanzaInscripcionesCerradasExcepcion() {
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(null);

        assertThrows(InscripcionesCerradasExcepcion.class, () -> servicio.inscribir(datos));
        verify(grupoGateway, never()).obtenerGrupoConLock(any(), any(), anyInt(), anyInt());
    }

    // SCRUM-178: un curso INACTIVO no debe aceptar nuevas inscripciones aunque las
    // inscripciones esten formalmente ABIERTO y el grupo tenga cupos.
    @Test
    void inscribir_cursoInactivo_lanzaInscripcionesCerradasExcepcion() {
        Curso cursoInactivo = new Curso();
        cursoInactivo.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        cursoInactivo.setEstadoCurso(EstadoCurso.INACTIVO);
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoInactivo);

        assertThrows(InscripcionesCerradasExcepcion.class, () -> servicio.inscribir(datos));
        verify(grupoGateway, never()).obtenerGrupoConLock(any(), any(), anyInt(), anyInt());
    }

    @Test
    void inscribir_conCuposNulos_lanzaCuposAgotadosExcepcion() {
        Grupo grupoSinCupos = new Grupo();
        grupoSinCupos.setCupos(null);
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoSinCupos);

        assertThrows(CuposAgotadosExcepcion.class, () -> servicio.inscribir(datos));
        verify(gateway, never()).guardarInscripcion(any());
    }

    @Test
    void inscribir_conCuposAgotados_creaInscripcionEnEspera() {
        Inscripcion guardada = new Inscripcion("alum1", "cat1", "cur1", 2026, 1,
                Timestamp.from(Instant.now()), null, "EN_ESPERA");
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(5L);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        when(gateway.existeInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        when(gateway.guardarInscripcion(any())).thenReturn(guardada);

        Inscripcion resultado = servicio.inscribir(datos);

        assertNotNull(resultado);
        assertEquals("EN_ESPERA", resultado.getEstado());
        verify(gateway).guardarInscripcion(any());
    }

    @Test
    void inscribir_alumnoYaEnEspera_lanzaYaExisteElementoExcepcion() {
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(5L);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);

        assertThrows(YaExisteElementoExcepcion.class, () -> servicio.inscribir(datos));
        verify(gateway, never()).guardarInscripcion(any());
    }

    @Test
    void inscribir_conLimiteAlcanzado_lanzaLimiteCursosExcepcion() {
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(2L);
        when(gateway.contarCursosActivosAlumno("alum1")).thenReturn(3L);

        assertThrows(LimiteCursosExcepcion.class, () -> servicio.inscribir(datos));
        verify(gateway, never()).guardarInscripcion(any());
    }

    @Test
    void inscribir_nueva_exitosa_guarda_con_fechaInscripcion() {
        Inscripcion guardada = new Inscripcion("alum1", "cat1", "cur1", 2026, 1,
                Timestamp.from(Instant.now()), null, "INSCRITO");
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(2L);
        when(gateway.contarCursosActivosAlumno("alum1")).thenReturn(1L);
        when(gateway.existeInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        when(gateway.guardarInscripcion(any())).thenReturn(guardada);

        Inscripcion resultado = servicio.inscribir(datos);

        assertNotNull(resultado);
        assertNotNull(resultado.getFechaInscripcion());
        assertEquals("INSCRITO", resultado.getEstado());
        verify(gateway).guardarInscripcion(any());
    }

    @Test
    void inscribir_existente_reactiva_fecha_desvinculacion_nula() {
        Timestamp ahora = Timestamp.from(Instant.now());
        Inscripcion existente = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, ahora, ahora, "INSCRITO");
        Inscripcion reactivada = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, ahora, null, "INSCRITO");
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(2L);
        when(gateway.contarCursosActivosAlumno("alum1")).thenReturn(1L);
        when(gateway.existeInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(gateway.obtenerInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(existente);
        when(gateway.guardarInscripcion(any())).thenReturn(reactivada);

        Inscripcion resultado = servicio.inscribir(datos);

        assertNull(resultado.getFechaDesvinculacion());
    }

    @Test
    void inscribir_sinCuposConInscripcionPreviaDesvinculada_reactivaComoEnEspera() {
        // Mismo patron que inscribir_existente_reactiva_fecha_desvinculacion_nula,
        // pero por el camino SIN CUPOS: el alumno ya tuvo una fila (desvinculada en
        // el pasado) para este grupo y vuelve a intentar inscribirse cuando ya no
        // hay cupo -- debe reactivarse como EN_ESPERA, no crear una fila nueva.
        Timestamp ahora = Timestamp.from(Instant.now());
        Inscripcion existente = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, ahora, ahora, "INSCRITO");
        Inscripcion reactivada = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, ahora, null, "EN_ESPERA");
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(5L);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        when(gateway.existeInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(gateway.obtenerInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(existente);
        when(gateway.guardarInscripcion(any())).thenReturn(reactivada);

        Inscripcion resultado = servicio.inscribir(datos);

        assertEquals("EN_ESPERA", resultado.getEstado());
        assertNull(resultado.getFechaDesvinculacion());
        verify(gateway, never()).guardarInscripcion(datos);
    }

    // fechaDesvinculacion llega del body de POST /inscripcion sin validar: al crear una fila
    // NUEVA el servicio debe ignorarla (rama INSCRITO y rama EN_ESPERA).
    @Test
    void inscribir_filaNuevaConCupo_ignoraFechaDesvinculacionDelCliente() {
        datos.setFechaDesvinculacion(Timestamp.from(Instant.now().plusSeconds(86400)));
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(2L);
        when(gateway.contarCursosActivosAlumno("alum1")).thenReturn(1L);
        when(gateway.existeInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        when(gateway.guardarInscripcion(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.inscribir(datos);

        ArgumentCaptor<Inscripcion> guardada = ArgumentCaptor.forClass(Inscripcion.class);
        verify(gateway).guardarInscripcion(guardada.capture());
        assertEquals("INSCRITO", guardada.getValue().getEstado());
        assertNull(guardada.getValue().getFechaDesvinculacion());
    }

    @Test
    void inscribir_filaNuevaSinCupo_ignoraFechaDesvinculacionDelCliente() {
        datos.setFechaDesvinculacion(Timestamp.from(Instant.now().plusSeconds(86400)));
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(5L);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        when(gateway.existeInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        when(gateway.guardarInscripcion(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.inscribir(datos);

        ArgumentCaptor<Inscripcion> guardada = ArgumentCaptor.forClass(Inscripcion.class);
        verify(gateway).guardarInscripcion(guardada.capture());
        assertEquals("EN_ESPERA", guardada.getValue().getEstado());
        assertNull(guardada.getValue().getFechaDesvinculacion());
    }

    // ── validarInscripcion ────────────────────────────────────────────────────

    @Test
    void validarInscripcion_devuelve_resultado_del_gateway() {
        when(gateway.existeInscripcionActiva("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        assertTrue(servicio.validarInscripcion("alum1", "cat1", "cur1", 2026, 1));

        when(gateway.existeInscripcionActiva("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);
        assertFalse(servicio.validarInscripcion("alum1", "cat1", "cur1", 2026, 1));
    }

    // ── desvincularInscripcion ────────────────────────────────────────────────

    @Test
    void desvincularInscripcion_noExiste_lanzaNoExisteExcepcion() {
        when(gateway.existeInscripcionSinDesvincular("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1));
        verify(gateway, never()).desvincularInscripcion(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void desvincularInscripcion_alumnoInscrito_invocaAutoPromocion() {
        Timestamp ahora = Timestamp.from(Instant.now());
        Inscripcion desvinculada = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, ahora, ahora, "INSCRITO");
        when(gateway.existeInscripcionSinDesvincular("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(gateway.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(desvinculada);

        Inscripcion resultado = servicio.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1);

        assertNotNull(resultado);
        verify(gateway).promoverPrimeroEnEspera("cat1", "cur1", 2026, 1);
    }

    // HALLAZGO 3-B: si el desvinculado estaba EN_ESPERA (no ocupaba cupo), no se debe
    // promover a nadie -- de lo contrario se sobre-inscribe el grupo.
    @Test
    void desvincularInscripcion_alumnoEnEspera_noInvocaAutoPromocion() {
        Timestamp ahora = Timestamp.from(Instant.now());
        Inscripcion desvinculada = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, ahora, ahora, "EN_ESPERA");
        when(gateway.existeInscripcionSinDesvincular("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(gateway.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(desvinculada);

        Inscripcion resultado = servicio.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1);

        assertNotNull(resultado);
        verify(gateway, never()).promoverPrimeroEnEspera(any(), any(), anyInt(), anyInt());
    }

    // Llamar desvincularInscripcion dos veces sobre la misma fila (doble clic, reintento):
    // la segunda llamada debe fallar con NoExisteExcepcion y NO debe volver a promover.
    @Test
    void desvincularInscripcion_llamadaRepetida_segundaVezNoRepromueve() {
        Timestamp ahora = Timestamp.from(Instant.now());
        Inscripcion desvinculada = new Inscripcion("alum1", "cat1", "cur1", 2026, 1, ahora, ahora, "INSCRITO");
        when(gateway.existeInscripcionSinDesvincular("alum1", "cat1", "cur1", 2026, 1))
                .thenReturn(true)
                .thenReturn(false);
        when(gateway.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(desvinculada);

        Inscripcion primeraLlamada = servicio.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1);
        assertNotNull(primeraLlamada);
        verify(gateway, times(1)).promoverPrimeroEnEspera("cat1", "cur1", 2026, 1);

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.desvincularInscripcion("alum1", "cat1", "cur1", 2026, 1));
        verify(gateway, times(1)).promoverPrimeroEnEspera("cat1", "cur1", 2026, 1);
        verify(gateway, times(1)).desvincularInscripcion(any(), any(), any(), anyInt(), anyInt());
    }

    // ── promoverManualmente ───────────────────────────────────────────────────

    @Test
    void promoverManualmente_noEstaEnEspera_lanzaNoExisteExcepcion() {
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.promoverManualmente("alum1", "cat1", "cur1", 2026, 1));
        verify(gateway, never()).promoverInscripcion(any(), any(), any(), anyInt(), anyInt());
        verify(grupoGateway, never()).obtenerGrupoConLock(any(), any(), anyInt(), anyInt());
    }

    // SCRUM-178: mismo hueco que inscribir() -- promover no debe permitirse si el curso
    // esta INACTIVO, aunque el alumno este legitimamente en espera.
    @Test
    void promoverManualmente_cursoInactivo_lanzaInscripcionesCerradasExcepcion() {
        Curso cursoInactivo = new Curso();
        cursoInactivo.setEstadoCurso(EstadoCurso.INACTIVO);
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoInactivo);

        assertThrows(InscripcionesCerradasExcepcion.class,
                () -> servicio.promoverManualmente("alum1", "cat1", "cur1", 2026, 1));
        verify(gateway, never()).existeEnEspera(any(), any(), any(), anyInt(), anyInt());
        verify(grupoGateway, never()).obtenerGrupoConLock(any(), any(), anyInt(), anyInt());
    }

    @Test
    void promoverManualmente_conCupoDisponible_promueveExitosamente() {
        Inscripcion promovida = new Inscripcion("alum1", "cat1", "cur1", 2026, 1,
                Timestamp.from(Instant.now()), null, "INSCRITO");
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(2L);
        when(gateway.promoverInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(promovida);

        Inscripcion resultado = servicio.promoverManualmente("alum1", "cat1", "cur1", 2026, 1);

        assertNotNull(resultado);
        assertEquals("INSCRITO", resultado.getEstado());
    }

    @Test
    void promoverManualmente_grupoSinCuposConfigurados_lanzaCuposAgotadosExcepcion() {
        Grupo grupoSinCupos = new Grupo();
        grupoSinCupos.setCupos(null);
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoSinCupos);

        assertThrows(CuposAgotadosExcepcion.class,
                () -> servicio.promoverManualmente("alum1", "cat1", "cur1", 2026, 1));
        verify(gateway, never()).contarInscripcionesActivasGrupo(any(), any(), anyInt(), anyInt());
        verify(gateway, never()).promoverInscripcion(any(), any(), any(), anyInt(), anyInt());
    }

    // HALLAZGO: promoverManualmente() no validaba cupos. Debe rechazar igual que
    // inscribir() cuando el grupo ya esta lleno (mismo tipo de excepcion -> HTTP 409).
    @Test
    void promoverManualmente_sinCupoDisponible_lanzaCuposAgotadosExcepcion() {
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(5L);

        assertThrows(CuposAgotadosExcepcion.class,
                () -> servicio.promoverManualmente("alum1", "cat1", "cur1", 2026, 1));
        verify(gateway, never()).promoverInscripcion(any(), any(), any(), anyInt(), anyInt());
    }

    // Confirma que se usa el mismo mecanismo de bloqueo pesimista que inscribir(),
    // tomado antes de contar cupos, para proteger la promocion manual de condiciones
    // de carrera (otro alumno inscribiendose, u otro Coordinador promoviendo a la vez).
    @Test
    void promoverManualmente_tomaLockDelGrupoAntesDeContarCupos() {
        Inscripcion promovida = new Inscripcion("alum1", "cat1", "cur1", 2026, 1,
                Timestamp.from(Instant.now()), null, "INSCRITO");
        when(cursoGateway.obtenerCurso("cat1", "cur1")).thenReturn(cursoAbierto);
        when(gateway.existeEnEspera("alum1", "cat1", "cur1", 2026, 1)).thenReturn(true);
        when(grupoGateway.obtenerGrupoConLock("cat1", "cur1", 2026, 1)).thenReturn(grupoCon5Cupos);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(2L);
        when(gateway.promoverInscripcion("alum1", "cat1", "cur1", 2026, 1)).thenReturn(promovida);

        servicio.promoverManualmente("alum1", "cat1", "cur1", 2026, 1);

        verify(grupoGateway).obtenerGrupoConLock("cat1", "cur1", 2026, 1);
    }

    // ── obtenerDisponibilidad ─────────────────────────────────────────────────

    @Test
    void obtenerDisponibilidad_calculaCuposCorrectamente() {
        Grupo grupo = new Grupo();
        grupo.setCupos(10);
        when(grupoGateway.obtenerGrupo("cat1", "cur1", 2026, 1)).thenReturn(grupo);
        when(gateway.contarInscripcionesActivasGrupo("cat1", "cur1", 2026, 1)).thenReturn(7L);
        when(gateway.contarEnEsperaGrupo("cat1", "cur1", 2026, 1)).thenReturn(2L);

        Disponibilidad resultado = servicio.obtenerDisponibilidad("cat1", "cur1", 2026, 1);

        assertEquals(10, resultado.getCuposTotales());
        assertEquals(3, resultado.getCuposDisponibles());
        assertEquals(2, resultado.getTamanoListaEspera());
    }

    // ── listarEnEspera ────────────────────────────────────────────────────────

    @Test
    void listarEnEspera_delegaAlGateway() {
        List<InscripcionEnEspera> esperados = List.of(
                new InscripcionEnEspera("alum1", "Juan Perez", "juan@unicauca.edu.co",
                        Timestamp.from(Instant.now())));
        when(gateway.listarEnEspera("cat1", "cur1", 2026, 1)).thenReturn(esperados);

        List<InscripcionEnEspera> resultado = servicio.listarEnEspera("cat1", "cur1", 2026, 1);

        assertEquals(1, resultado.size());
        assertEquals("alum1", resultado.get(0).getAlumnoId());
        verify(gateway).listarEnEspera("cat1", "cur1", 2026, 1);
    }
}
