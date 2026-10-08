package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConteoInscripcionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InstructorGrupoResumen;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechasGrupoInvalidasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrupoServicioTest {

    @Mock
    private IGrupoGateway grupoGateway;

    @Mock
    private IInscripcionGateway inscripcionGateway;

    @Mock
    private ICursoGateway cursoGateway;

    @Mock
    private IHorarioGateway horarioGateway;

    @InjectMocks
    private GrupoServicio grupoServicio;

    private static final String  CATEGORIA = "Recreativo";
    private static final String  CURSO     = "Natacion";
    private static final Integer ANIO      = 2025;
    private static final Integer ITERABLE  = 1;

    private Grupo grupoModelo() {
        Grupo g = new Grupo();
        g.setCategoria(CATEGORIA);
        g.setCurso(CURSO);
        g.setAnio(ANIO);
        g.setIterable(ITERABLE);
        g.setCupos(20);
        g.setImagenGrupo(1);
        g.setIdInstructor("INS001");
        g.setFechaCreacion(LocalDate.of(2025, 1, 15));
        g.setPeriodo(1);
        return g;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerTodosGrupos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerTodosGrupos_delegaAlGateway() {
        when(grupoGateway.obtenerTodosGrupos()).thenReturn(List.of(grupoModelo(), grupoModelo()));

        List<Grupo> resultado = grupoServicio.obtenerTodosGrupos();

        assertEquals(2, resultado.size());
        verify(grupoGateway).obtenerTodosGrupos();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerGruposDisponibles
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerGruposDisponibles_delegaAlGateway() {
        when(grupoGateway.obtenerGruposDisponibles()).thenReturn(List.of(grupoModelo()));

        List<Grupo> resultado = grupoServicio.obtenerGruposDisponibles();

        assertEquals(1, resultado.size());
        verify(grupoGateway).obtenerGruposDisponibles();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerGruposDeCurso
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerGruposDeCurso_delegaAlGatewayConParametros() {
        when(grupoGateway.obtenerGruposDeCurso(CATEGORIA, CURSO)).thenReturn(List.of(grupoModelo()));

        List<Grupo> resultado = grupoServicio.obtenerGruposDeCurso(CATEGORIA, CURSO);

        assertEquals(1, resultado.size());
        verify(grupoGateway).obtenerGruposDeCurso(CATEGORIA, CURSO);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerGruposInscripcionDisponible
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerGruposInscripcionDisponible_delegaAlGateway() {
        when(grupoGateway.obtenerGruposInscripcionDisponible()).thenReturn(List.of(grupoModelo()));

        List<Grupo> resultado = grupoServicio.obtenerGruposInscripcionDisponible();

        assertEquals(1, resultado.size());
        verify(grupoGateway).obtenerGruposInscripcionDisponible();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerGruposInstructor
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerGruposInstructor_delegaAlGatewayConIdInstructor() {
        when(grupoGateway.obtenerGruposInstructor("INS001")).thenReturn(List.of(grupoModelo()));

        List<Grupo> resultado = grupoServicio.obtenerGruposInstructor("INS001");

        assertEquals(1, resultado.size());
        verify(grupoGateway).obtenerGruposInstructor("INS001");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // insertarGrupo
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void insertarGrupo_exitoso_delegaAlGateway() {
        when(grupoGateway.insertarGrupo(any())).thenReturn(grupoModelo());

        Grupo resultado = grupoServicio.insertarGrupo(grupoModelo());

        assertNotNull(resultado);
        assertEquals(CATEGORIA, resultado.getCategoria());
        verify(grupoGateway).insertarGrupo(any());
    }

    @Test
    void insertarGrupo_conNulo_delegaNuloAlGateway() {
        when(grupoGateway.insertarGrupo(nullable(Grupo.class))).thenReturn(grupoModelo());

        assertDoesNotThrow(() -> grupoServicio.insertarGrupo(null));

        verify(grupoGateway).insertarGrupo(null);
    }

    // R1: fechaFinalizacion no puede ser anterior a fechaCreacion.
    // R2: fechaIncripcionCierre no puede ser anterior a fechaInscripcionApertura.
    // Si cualquiera de las dos fechas de una regla es null, esa regla no aplica.

    @Test
    void insertarGrupo_R1_fechaFinalizacionAnteriorAFechaCreacion_lanzaFechasGrupoInvalidasExcepcion() {
        Grupo grupo = grupoModelo();
        grupo.setFechaCreacion(LocalDate.of(2026, 2, 1));
        grupo.setFechaFinalizacion(LocalDate.of(2026, 1, 1));

        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.insertarGrupo(grupo));
        verify(grupoGateway, never()).insertarGrupo(any());
    }

    @Test
    void insertarGrupo_R2_fechaCierreAnteriorAFechaApertura_lanzaFechasGrupoInvalidasExcepcion() {
        Grupo grupo = grupoModelo();
        grupo.setFechaInscripcionApertura(LocalDate.of(2026, 2, 1));
        grupo.setFechaIncripcionCierre(LocalDate.of(2026, 1, 1));

        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.insertarGrupo(grupo));
        verify(grupoGateway, never()).insertarGrupo(any());
    }

    @Test
    void insertarGrupo_fechasValidas_delegaAlGateway() {
        Grupo grupo = grupoModelo();
        grupo.setFechaCreacion(LocalDate.of(2026, 1, 1));
        grupo.setFechaFinalizacion(LocalDate.of(2026, 6, 1));
        grupo.setFechaInscripcionApertura(LocalDate.of(2026, 1, 1));
        grupo.setFechaIncripcionCierre(LocalDate.of(2026, 1, 15));
        when(grupoGateway.insertarGrupo(grupo)).thenReturn(grupo);

        assertDoesNotThrow(() -> grupoServicio.insertarGrupo(grupo));
        verify(grupoGateway).insertarGrupo(grupo);
    }

    @Test
    void insertarGrupo_conFechaCreacionNula_noValidaR1() {
        Grupo grupo = grupoModelo();
        grupo.setFechaCreacion(null);
        // Anterior a cualquier fecha de creacion razonable, pero R1 no aplica sin fechaCreacion.
        grupo.setFechaFinalizacion(LocalDate.of(2020, 1, 1));
        when(grupoGateway.insertarGrupo(grupo)).thenReturn(grupo);

        assertDoesNotThrow(() -> grupoServicio.insertarGrupo(grupo));
        verify(grupoGateway).insertarGrupo(grupo);
    }

    @Test
    void insertarGrupo_conFechaCierreNula_noValidaR2() {
        Grupo grupo = grupoModelo();
        grupo.setFechaInscripcionApertura(LocalDate.of(2026, 1, 1));
        grupo.setFechaIncripcionCierre(null); // R2 no aplica sin fechaIncripcionCierre.
        when(grupoGateway.insertarGrupo(grupo)).thenReturn(grupo);

        assertDoesNotThrow(() -> grupoServicio.insertarGrupo(grupo));
        verify(grupoGateway).insertarGrupo(grupo);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerGrupoPorId
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerGrupoPorId_exitoso_retornaGrupo() {
        when(grupoGateway.obtenerGrupoPorId(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(grupoModelo());

        Grupo resultado = grupoServicio.obtenerGrupoPorId(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNotNull(resultado);
        assertEquals(CATEGORIA, resultado.getCategoria());
        assertEquals(CURSO, resultado.getCurso());
    }

    @Test
    void obtenerGrupoPorId_grupoNulo_lanzaNoExisteExcepcion() {
        when(grupoGateway.obtenerGrupoPorId(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(null);

        assertThrows(NoExisteExcepcion.class,
                () -> grupoServicio.obtenerGrupoPorId(CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // actualizarGrupo
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void actualizarGrupo_exitoso_actualizaCorrectamente() {
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupoModelo());
        when(grupoGateway.actualizarGrupo(eq(CATEGORIA), eq(CURSO), eq(ANIO), eq(ITERABLE), any()))
                .thenReturn(grupoModelo());

        Grupo resultado = grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, grupoModelo());

        assertNotNull(resultado);
        assertEquals(CATEGORIA, resultado.getCategoria());
        verify(grupoGateway).actualizarGrupo(eq(CATEGORIA), eq(CURSO), eq(ANIO), eq(ITERABLE), any());
    }

    @Test
    void actualizarGrupo_noExiste_lanzaNoExisteExcepcion() {
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, grupoModelo()));

        verify(grupoGateway, never()).actualizarGrupo(any(), any(), any(), any(), any());
    }

    // La validacion debe evaluarse sobre los valores FINALES (body combinado con lo
    // que ya tenia la fila) -- no solo sobre lo que trae el body.

    @Test
    void actualizarGrupo_R1_fechaFinalizacionDelBodyAnteriorAFechaCreacionDeLaFila_lanzaExcepcion() {
        Grupo filaActual = grupoModelo();
        filaActual.setFechaCreacion(LocalDate.of(2026, 3, 1));
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(filaActual);

        Grupo datos = new Grupo();
        datos.setFechaFinalizacion(LocalDate.of(2026, 1, 1)); // body solo trae esta; fechaCreacion sale de la fila

        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, datos));
        verify(grupoGateway, never()).actualizarGrupo(any(), any(), any(), any(), any());
    }

    @Test
    void actualizarGrupo_R2_fechaCierreDelBodyAnteriorAFechaAperturaDeLaFila_lanzaExcepcion() {
        Grupo filaActual = grupoModelo();
        filaActual.setFechaInscripcionApertura(LocalDate.of(2026, 3, 1));
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(filaActual);

        Grupo datos = new Grupo();
        datos.setFechaIncripcionCierre(LocalDate.of(2026, 1, 1)); // body solo trae esta; apertura sale de la fila

        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, datos));
        verify(grupoGateway, never()).actualizarGrupo(any(), any(), any(), any(), any());
    }

    @Test
    void actualizarGrupo_fechasCombinadasValidas_actualizaCorrectamente() {
        Grupo filaActual = grupoModelo();
        filaActual.setFechaCreacion(LocalDate.of(2026, 1, 1));
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(filaActual);
        when(grupoGateway.actualizarGrupo(eq(CATEGORIA), eq(CURSO), eq(ANIO), eq(ITERABLE), any()))
                .thenReturn(grupoModelo());

        Grupo datos = new Grupo();
        datos.setFechaFinalizacion(LocalDate.of(2026, 6, 1)); // posterior a la fechaCreacion de la fila

        assertDoesNotThrow(() -> grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, datos));
        verify(grupoGateway).actualizarGrupo(eq(CATEGORIA), eq(CURSO), eq(ANIO), eq(ITERABLE), any());
    }

    @Test
    void actualizarGrupo_fechaAperturaEnviadaEnElBody_laUsaParaValidarEnVezDeLaDeLaFila() {
        Grupo filaActual = grupoModelo();
        filaActual.setFechaInscripcionApertura(LocalDate.of(2020, 1, 1)); // iria antes que cualquier cierre razonable
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(filaActual);
        when(grupoGateway.actualizarGrupo(eq(CATEGORIA), eq(CURSO), eq(ANIO), eq(ITERABLE), any()))
                .thenReturn(grupoModelo());

        Grupo datos = new Grupo();
        // El body trae su propia fechaInscripcionApertura, mas reciente que la de la fila;
        // la validacion debe usar esta, no la de la fila (que hubiera hecho fallar R2).
        datos.setFechaInscripcionApertura(LocalDate.of(2026, 1, 1));
        datos.setFechaIncripcionCierre(LocalDate.of(2026, 1, 15));

        assertDoesNotThrow(() -> grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, datos));
        verify(grupoGateway).actualizarGrupo(eq(CATEGORIA), eq(CURSO), eq(ANIO), eq(ITERABLE), any());
    }

    @Test
    void actualizarGrupo_sinFechasNiEnBodyNiEnFila_noValidaNada() {
        Grupo filaActual = grupoModelo(); // sin fechas seteadas
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(filaActual);
        when(grupoGateway.actualizarGrupo(eq(CATEGORIA), eq(CURSO), eq(ANIO), eq(ITERABLE), any()))
                .thenReturn(grupoModelo());

        Grupo datos = new Grupo();

        assertDoesNotThrow(() -> grupoServicio.actualizarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, datos));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarGrupo
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void eliminarGrupo_exitoso_delegaAlGateway() {
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.existeGrupoEliminado(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(false);
        when(grupoGateway.eliminarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(grupoModelo());

        Grupo resultado = grupoServicio.eliminarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNotNull(resultado);
        verify(grupoGateway).eliminarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);
    }

    @Test
    void eliminarGrupo_noExiste_lanzaNoExisteExcepcion() {
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> grupoServicio.eliminarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE));

        verify(grupoGateway, never()).eliminarGrupo(any(), any(), any(), any());
    }

    // Llamar eliminarGrupo() dos veces seguidas: la primera exitosa, la segunda debe
    // dar 409 en vez de volver a marcar eliminado=1 sin avisar (ver GrupoIT para la
    // prueba real HTTP, y GrupoRestSecurityTest para los roles).
    @Test
    void eliminarGrupo_yaEstabaEliminado_lanzaYaExisteElementoExcepcion() {
        when(grupoGateway.existeGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);
        when(grupoGateway.existeGrupoEliminado(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(true);

        YaExisteElementoExcepcion ex = assertThrows(YaExisteElementoExcepcion.class,
                () -> grupoServicio.eliminarGrupo(CATEGORIA, CURSO, ANIO, ITERABLE));

        assertEquals("El grupo ya se encuentra eliminado", ex.getMessage());
        verify(grupoGateway, never()).eliminarGrupo(any(), any(), any(), any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerGrupo
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerGrupo_exitoso_retornaGrupo() {
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(grupoModelo());

        Grupo resultado = grupoServicio.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNotNull(resultado);
        assertEquals(CATEGORIA, resultado.getCategoria());
    }

    @Test
    void obtenerGrupo_retornaNullSiGatewayDevuelveNull() {
        when(grupoGateway.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(null);

        Grupo resultado = grupoServicio.obtenerGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNull(resultado);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerMisGrupos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerMisGrupos_conDatos_componeCursoHorariosYConteos() {
        Grupo grupoConConteo = grupoModelo();
        Grupo grupoSinConteo = grupoModelo();
        grupoSinConteo.setCurso("Futbol");
        Curso curso = new Curso();
        curso.setEstadoCurso(EstadoCurso.ACTIVO);
        curso.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        Horario horarioLunes = new Horario(1, CATEGORIA, CURSO, ANIO, ITERABLE, "LUNES", "08:00", "10:00", "Cancha 1", 0);
        Horario horarioMiercoles = new Horario(2, CATEGORIA, CURSO, ANIO, ITERABLE, "MIERCOLES", "08:00", "10:00", "Cancha 1", 0);

        when(grupoGateway.obtenerGruposInstructor("INS001")).thenReturn(List.of(grupoConConteo, grupoSinConteo));
        when(inscripcionGateway.contarInscripcionesPorInstructor("INS001"))
                .thenReturn(List.of(new ConteoInscripcionGrupo(CATEGORIA, CURSO, ANIO, ITERABLE, 3, 1)));
        when(cursoGateway.obtenerCurso(eq(CATEGORIA), anyString())).thenReturn(curso);
        when(horarioGateway.listarHorariosPorGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of(horarioLunes, horarioMiercoles));
        when(horarioGateway.listarHorariosPorGrupo(CATEGORIA, "Futbol", ANIO, ITERABLE))
                .thenReturn(List.of());

        List<InstructorGrupoResumen> resultado = grupoServicio.obtenerMisGrupos("INS001");

        assertEquals(2, resultado.size());
        InstructorGrupoResumen filaConConteo = resultado.get(0);
        assertEquals(CATEGORIA, filaConConteo.getCategoria());
        assertEquals(CURSO, filaConConteo.getCurso());
        assertEquals(ANIO, filaConConteo.getAnio());
        assertEquals(ITERABLE, filaConConteo.getIterable());
        assertEquals(1, filaConConteo.getPeriodo());
        assertEquals(20, filaConConteo.getCupos());
        assertEquals(3, filaConConteo.getInscritos());
        assertEquals(1, filaConConteo.getEnEspera());
        assertEquals(2, filaConConteo.getHorarios().size(), "una sola fila por grupo, con los 2 horarios anidados");
        assertEquals(EstadoCurso.ACTIVO, filaConConteo.getEstadoCurso());
        assertEquals(EstadoInscripciones.ABIERTO, filaConConteo.getEstadoInscripciones());

        // Grupo sin inscripciones no aparece en el query agrupado -- debe tratarse
        // como 0/0, no lanzar excepcion ni omitirse de la respuesta.
        InstructorGrupoResumen filaSinConteo = resultado.get(1);
        assertEquals("Futbol", filaSinConteo.getCurso());
        assertEquals(0, filaSinConteo.getInscritos());
        assertEquals(0, filaSinConteo.getEnEspera());
        assertTrue(filaSinConteo.getHorarios().isEmpty());

        // Un solo query de conteos para TODOS los grupos, no uno por grupo.
        verify(inscripcionGateway, times(1)).contarInscripcionesPorInstructor("INS001");
    }

    @Test
    void obtenerMisGrupos_instructorSinGrupos_retornaListaVaciaSinConsultarNada() {
        when(grupoGateway.obtenerGruposInstructor("INS001")).thenReturn(List.of());

        List<InstructorGrupoResumen> resultado = grupoServicio.obtenerMisGrupos("INS001");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verify(inscripcionGateway, never()).contarInscripcionesPorInstructor(any());
        verify(cursoGateway, never()).obtenerCurso(any(), any());
        verify(horarioGateway, never()).listarHorariosPorGrupo(any(), any(), anyInt(), anyInt());
    }

    // Hallazgo del diseno: un curso INACTIVO no debe ocultar el grupo -- debe
    // devolverse igual, con estadoCurso=INACTIVO reflejado tal cual.
    @Test
    void obtenerMisGrupos_cursoInactivo_devuelveGrupoConEstadoInactivo() {
        Grupo grupo = grupoModelo();
        Curso cursoInactivo = new Curso();
        cursoInactivo.setEstadoCurso(EstadoCurso.INACTIVO);
        cursoInactivo.setEstadoInscripciones(EstadoInscripciones.CERRADO);
        when(grupoGateway.obtenerGruposInstructor("INS001")).thenReturn(List.of(grupo));
        when(inscripcionGateway.contarInscripcionesPorInstructor("INS001")).thenReturn(List.of());
        when(cursoGateway.obtenerCurso(CATEGORIA, CURSO)).thenReturn(cursoInactivo);
        when(horarioGateway.listarHorariosPorGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(List.of());

        List<InstructorGrupoResumen> resultado = grupoServicio.obtenerMisGrupos("INS001");

        assertEquals(1, resultado.size());
        assertEquals(EstadoCurso.INACTIVO, resultado.get(0).getEstadoCurso());
    }
}
