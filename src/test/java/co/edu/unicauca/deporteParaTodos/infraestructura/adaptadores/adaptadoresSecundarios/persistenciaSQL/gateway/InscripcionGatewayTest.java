package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.InscripcionEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IInscripcionRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InscripcionGatewayTest {

    @Mock
    private IInscripcionRepositorio repoInscrp;

    @InjectMocks
    private InscripcionGateway gateway;

    private static final String    ALUMNO_ID = "alum1";
    private static final String    CATEGORIA = "cat1";
    private static final String    CURSO     = "cur1";
    private static final int       ANIO      = 2026;
    private static final int       ITERABLE  = 1;
    private static final Timestamp AHORA     = Timestamp.from(Instant.now());

    private InscripcionEntidad entidadBase() {
        InscripcionEntidad e = new InscripcionEntidad();
        e.setAlumnoId(ALUMNO_ID);
        e.setCategoria(CATEGORIA);
        e.setCurso(CURSO);
        e.setAnio(ANIO);
        e.setIterable(ITERABLE);
        e.setFechaInscripcion(AHORA);
        e.setFechaDesvinculacion(null);
        e.setEliminado(0);
        e.setEstado("INSCRITO");
        return e;
    }

    private InscripcionEntidad entidadEnEspera() {
        InscripcionEntidad e = entidadBase();
        e.setEstado("EN_ESPERA");
        return e;
    }

    // ── existeInscripcion ─────────────────────────────────────────────────────

    @Test
    void existeInscripcion_returnTrue() {
        when(repoInscrp.existsById(any())).thenReturn(true);
        assertTrue(gateway.existeInscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ── existeInscripcionActiva ───────────────────────────────────────────────

    @Test
    void existeInscripcionActiva_returnTrue() {
        when(repoInscrp.existeInscripcionActiva(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(true);
        assertTrue(gateway.existeInscripcionActiva(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ── existeEnEspera ────────────────────────────────────────────────────────

    @Test
    void existeEnEspera_returnTrue() {
        when(repoInscrp.existeEnEspera(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(true);
        assertTrue(gateway.existeEnEspera(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    @Test
    void existeEnEspera_returnFalse() {
        when(repoInscrp.existeEnEspera(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(false);
        assertFalse(gateway.existeEnEspera(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ── existeInscripcionSinDesvincular ─────────────────────────────────────────

    @Test
    void existeInscripcionSinDesvincular_returnTrue() {
        when(repoInscrp.existeInscripcionSinDesvincular(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(true);
        assertTrue(gateway.existeInscripcionSinDesvincular(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    @Test
    void existeInscripcionSinDesvincular_returnFalse() {
        when(repoInscrp.existeInscripcionSinDesvincular(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(false);
        assertFalse(gateway.existeInscripcionSinDesvincular(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ── obtenerInscripcion ────────────────────────────────────────────────────

    @Test
    void obtenerInscripcion_existente_retornaInscripcion() {
        when(repoInscrp.findById(any())).thenReturn(Optional.of(entidadBase()));

        Inscripcion resultado = gateway.obtenerInscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNotNull(resultado);
        assertEquals(ALUMNO_ID, resultado.getAlumnoId());
        assertEquals(CATEGORIA, resultado.getCategoria());
        assertEquals("INSCRITO", resultado.getEstado());
    }

    @Test
    void obtenerInscripcion_noExiste_lanzaNoExisteExcepcion() {
        when(repoInscrp.findById(any())).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> gateway.obtenerInscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ── guardarInscripcion ────────────────────────────────────────────────────

    @Test
    void guardarInscripcion_retornaInscripcionGuardada() {
        when(repoInscrp.save(any(InscripcionEntidad.class))).thenReturn(entidadBase());

        Inscripcion input = new Inscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, AHORA, null, "INSCRITO");
        Inscripcion resultado = gateway.guardarInscripcion(input);

        assertNotNull(resultado);
        assertEquals(ALUMNO_ID, resultado.getAlumnoId());
        assertEquals("INSCRITO", resultado.getEstado());
        verify(repoInscrp).save(any(InscripcionEntidad.class));
    }

    // ── desvincularInscripcion ────────────────────────────────────────────────

    @Test
    void desvincularInscripcion_existente_setFechaDesvinculacionYGuarda() {
        when(repoInscrp.findById(any())).thenReturn(Optional.of(entidadBase()));
        when(repoInscrp.save(any(InscripcionEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscripcion resultado = gateway.desvincularInscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNotNull(resultado);
        assertNotNull(resultado.getFechaDesvinculacion());
        verify(repoInscrp).save(any(InscripcionEntidad.class));
    }

    @Test
    void desvincularInscripcion_noExiste_lanzaNoExisteExcepcion() {
        when(repoInscrp.findById(any())).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> gateway.desvincularInscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
        verify(repoInscrp, never()).save(any());
    }

    // ── contarInscripcionesActivasGrupo ───────────────────────────────────────

    @Test
    void contarInscripcionesActivasGrupo_delegaAlRepositorio() {
        when(repoInscrp.contarInscripcionesActivasGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(5L);

        long resultado = gateway.contarInscripcionesActivasGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals(5L, resultado);
        verify(repoInscrp).contarInscripcionesActivasGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);
    }

    // ── contarCursosActivosAlumno ─────────────────────────────────────────────

    @Test
    void contarCursosActivosAlumno_delegaAlRepositorio() {
        when(repoInscrp.contarCursosActivosAlumno(ALUMNO_ID)).thenReturn(2L);

        long resultado = gateway.contarCursosActivosAlumno(ALUMNO_ID);

        assertEquals(2L, resultado);
        verify(repoInscrp).contarCursosActivosAlumno(ALUMNO_ID);
    }

    // ── contarEnEsperaGrupo ───────────────────────────────────────────────────

    @Test
    void contarEnEsperaGrupo_delegaAlRepositorio() {
        when(repoInscrp.contarEnEsperaGrupo(CATEGORIA, CURSO, ANIO, ITERABLE)).thenReturn(3L);

        long resultado = gateway.contarEnEsperaGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals(3L, resultado);
        verify(repoInscrp).contarEnEsperaGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);
    }

    // ── promoverPrimeroEnEspera ───────────────────────────────────────────────

    @Test
    void promoverPrimeroEnEspera_hayEnEspera_promueveYGuarda() {
        InscripcionEntidad enEspera = entidadEnEspera();
        when(repoInscrp.findEnEsperaOrdenados(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of(enEspera));
        when(repoInscrp.save(any(InscripcionEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        gateway.promoverPrimeroEnEspera(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals("INSCRITO", enEspera.getEstado());
        assertNotNull(enEspera.getFechaInscripcion());
        verify(repoInscrp).save(enEspera);
    }

    @Test
    void promoverPrimeroEnEspera_listaVacia_noGuarda() {
        when(repoInscrp.findEnEsperaOrdenados(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of());

        gateway.promoverPrimeroEnEspera(CATEGORIA, CURSO, ANIO, ITERABLE);

        verify(repoInscrp, never()).save(any());
    }

    // ── promoverInscripcion ───────────────────────────────────────────────────

    @Test
    void promoverInscripcion_existente_setEstadoInscritoYGuarda() {
        InscripcionEntidad enEspera = entidadEnEspera();
        when(repoInscrp.findById(any())).thenReturn(Optional.of(enEspera));
        when(repoInscrp.save(any(InscripcionEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Inscripcion resultado = gateway.promoverInscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNotNull(resultado);
        assertEquals("INSCRITO", resultado.getEstado());
        verify(repoInscrp).save(any(InscripcionEntidad.class));
    }

    @Test
    void promoverInscripcion_noExiste_lanzaNoExisteExcepcion() {
        when(repoInscrp.findById(any())).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> gateway.promoverInscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    // ── listarEnEspera ────────────────────────────────────────────────────────

    @Test
    void listarEnEspera_hayEntradas_retornaListaMapeada() {
        Object[] fila = new Object[]{"alum1", "Juan Perez", "juan@unicauca.edu.co", AHORA};
        List<Object[]> filas = new java.util.ArrayList<>();
        filas.add(fila);
        when(repoInscrp.findEnEsperaConPerfil(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(filas);

        List<InscripcionEnEspera> resultado = gateway.listarEnEspera(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals(1, resultado.size());
        assertEquals("alum1", resultado.get(0).getAlumnoId());
        assertEquals("Juan Perez", resultado.get(0).getNombre());
        assertEquals("juan@unicauca.edu.co", resultado.get(0).getCorreo());
        assertEquals(AHORA, resultado.get(0).getFechaInscripcion());
    }

    @Test
    void listarEnEspera_sinEntradas_retornaListaVacia() {
        when(repoInscrp.findEnEsperaConPerfil(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of());

        List<InscripcionEnEspera> resultado = gateway.listarEnEspera(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ── obtenerInscripcionesAlumno ────────────────────────────────────────────

    @Test
    void obtenerInscripcionesAlumno_hayFilas_retornaListaMapeada() {
        when(repoInscrp.findMisCursos(ALUMNO_ID)).thenReturn(List.of(entidadBase(), entidadEnEspera()));

        List<Inscripcion> resultado = gateway.obtenerInscripcionesAlumno(ALUMNO_ID);

        assertEquals(2, resultado.size());
        assertEquals(CATEGORIA, resultado.get(0).getCategoria());
        assertEquals("INSCRITO", resultado.get(0).getEstado());
        assertEquals("EN_ESPERA", resultado.get(1).getEstado());
    }

    @Test
    void obtenerInscripcionesAlumno_sinFilas_retornaListaVacia() {
        when(repoInscrp.findMisCursos(ALUMNO_ID)).thenReturn(List.of());

        List<Inscripcion> resultado = gateway.obtenerInscripcionesAlumno(ALUMNO_ID);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // ── contarInscripcionesPorInstructor ──────────────────────────────────────

    @Test
    void contarInscripcionesPorInstructor_hayFilas_mapeaCamposYConvierteNumerosAInt() {
        Object[] fila = new Object[]{CATEGORIA, CURSO, ANIO, ITERABLE, 3L, 1L};
        when(repoInscrp.contarInscripcionesPorInstructor("INS001")).thenReturn(List.<Object[]>of(fila));

        List<co.edu.unicauca.deporteParaTodos.dominio.modelo.ConteoInscripcionGrupo> resultado =
                gateway.contarInscripcionesPorInstructor("INS001");

        assertEquals(1, resultado.size());
        assertEquals(CATEGORIA, resultado.get(0).getCategoria());
        assertEquals(CURSO, resultado.get(0).getCurso());
        assertEquals(ANIO, resultado.get(0).getAnio());
        assertEquals(ITERABLE, resultado.get(0).getIterable());
        assertEquals(3, resultado.get(0).getInscritos());
        assertEquals(1, resultado.get(0).getEnEspera());
    }

    @Test
    void contarInscripcionesPorInstructor_sinFilas_retornaListaVacia() {
        when(repoInscrp.contarInscripcionesPorInstructor("INS001")).thenReturn(List.of());

        List<co.edu.unicauca.deporteParaTodos.dominio.modelo.ConteoInscripcionGrupo> resultado =
                gateway.contarInscripcionesPorInstructor("INS001");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }
}
