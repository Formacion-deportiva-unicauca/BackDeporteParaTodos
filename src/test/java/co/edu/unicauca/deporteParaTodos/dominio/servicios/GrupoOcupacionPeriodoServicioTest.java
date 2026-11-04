package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechasGrupoInvalidasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ClaveGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConteoInscripcionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoVentanaInscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NivelOcupacion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OcupacionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Fase 4 (modulo del Coordinador): ocupacion de los grupos y aplicacion de un periodo de inscripciones en bloque.
 */
@ExtendWith(MockitoExtension.class)
class GrupoOcupacionPeriodoServicioTest {

    @Mock private IGrupoGateway grupoGateway;
    @Mock private IInscripcionGateway inscripcionGateway;
    @Mock private ICursoGateway cursoGateway;
    @Mock private Clock clock;

    @InjectMocks private GrupoServicio grupoServicio;

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    @BeforeEach
    void relojFijo() {
        lenient().when(clock.instant()).thenReturn(HOY.atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));
    }

    // ── helpers ──

    private Grupo grupo(String categoria, String curso, int iterable, Integer cupos) {
        Grupo g = new Grupo();
        g.setCategoria(categoria);
        g.setCurso(curso);
        g.setAnio(2026);
        g.setIterable(iterable);
        g.setPeriodo(1);
        g.setCupos(cupos);
        g.setFechaInscripcionApertura(HOY.minusDays(10));
        g.setFechaIncripcionCierre(HOY.plusDays(10));
        return g;
    }

    private Curso curso(String categoria, String nombre, PublicoCurso publico) {
        Curso c = new Curso();
        c.setCategoriaCurso(categoria);
        c.setNombre(nombre);
        c.setPublico(publico);
        return c;
    }

    private void cursosYGrupos(List<Curso> cursos, List<Grupo> grupos, List<ConteoInscripcionGrupo> conteos) {
        when(grupoGateway.obtenerGruposDisponibles()).thenReturn(grupos);
        when(cursoGateway.obtenerCursos()).thenReturn(cursos);
        when(inscripcionGateway.contarInscripcionesTodosLosGrupos()).thenReturn(conteos);
    }

    private ConteoInscripcionGrupo conteo(String categoria, String curso, int iterable, int inscritos, int enEspera) {
        return new ConteoInscripcionGrupo(categoria, curso, 2026, iterable, inscritos, enEspera);
    }

    private OcupacionGrupo unica(List<OcupacionGrupo> lista) {
        assertEquals(1, lista.size());
        return lista.get(0);
    }

    // ── obtenerOcupacion: nivel ──

    @Test
    void ocupacion_inscritosIgualAlCupo_esLleno() {
        cursosYGrupos(List.of(curso("Formativo", "Fulbol", PublicoCurso.MIXTO)),
                List.of(grupo("Formativo", "Fulbol", 1, 20)),
                List.of(conteo("Formativo", "Fulbol", 1, 20, 3)));

        OcupacionGrupo o = unica(grupoServicio.obtenerOcupacion(null, null, null));

        assertEquals(NivelOcupacion.LLENO, o.getNivel());
        assertEquals(20, o.getInscritos());
        assertEquals(3, o.getEnEspera());
        assertEquals(20, o.getCupos());
    }

    @Test
    void ocupacion_exactamenteElOchentaPorCiento_esPorLlenar() {
        cursosYGrupos(List.of(curso("Formativo", "Fulbol", PublicoCurso.MIXTO)),
                List.of(grupo("Formativo", "Fulbol", 1, 20)),
                List.of(conteo("Formativo", "Fulbol", 1, 16, 0)));

        assertEquals(NivelOcupacion.POR_LLENAR, unica(grupoServicio.obtenerOcupacion(null, null, null)).getNivel());
    }

    @Test
    void ocupacion_pordebajoDelOchentaPorCiento_esConCupo() {
        cursosYGrupos(List.of(curso("Formativo", "Fulbol", PublicoCurso.MIXTO)),
                List.of(grupo("Formativo", "Fulbol", 1, 20)),
                List.of(conteo("Formativo", "Fulbol", 1, 15, 0)));

        assertEquals(NivelOcupacion.CON_CUPO, unica(grupoServicio.obtenerOcupacion(null, null, null)).getNivel());
    }

    @Test
    void ocupacion_grupoSinInscripciones_cuentaCeroYTieneCupo() {
        cursosYGrupos(List.of(curso("Formativo", "Fulbol", PublicoCurso.MIXTO)),
                List.of(grupo("Formativo", "Fulbol", 1, 20)),
                List.of());

        OcupacionGrupo o = unica(grupoServicio.obtenerOcupacion(null, null, null));

        assertEquals(0, o.getInscritos());
        assertEquals(0, o.getEnEspera());
        assertEquals(NivelOcupacion.CON_CUPO, o.getNivel());
    }

    @Test
    void ocupacion_cuposNulos_seTrataComoSinLimite() {
        cursosYGrupos(List.of(curso("Formativo", "Fulbol", PublicoCurso.MIXTO)),
                List.of(grupo("Formativo", "Fulbol", 1, null)),
                List.of(conteo("Formativo", "Fulbol", 1, 50, 0)));

        assertEquals(NivelOcupacion.CON_CUPO, unica(grupoServicio.obtenerOcupacion(null, null, null)).getNivel());
    }

    // ── obtenerOcupacion: datos del grupo y filtros ──

    @Test
    void ocupacion_traePublicoDelCursoYEstadoDeLaVentana() {
        Grupo cerradoAMano = grupo("Formativo", "Fulbol", 1, 20);
        cerradoAMano.setInscripcionesCerradasManual(true);
        cursosYGrupos(List.of(curso("Formativo", "Fulbol", PublicoCurso.FUNCIONARIO)),
                List.of(cerradoAMano), List.of());

        OcupacionGrupo o = unica(grupoServicio.obtenerOcupacion(null, null, null));

        assertEquals(PublicoCurso.FUNCIONARIO, o.getPublico());
        assertEquals(EstadoVentanaInscripcion.CERRADAS, o.getEstadoInscripcion());
        assertTrue(o.isCerradaManual());
        assertEquals(HOY.plusDays(10), o.getFechaCierre());
    }

    @Test
    void ocupacion_filtraPorCategoriaCursoYPublico() {
        cursosYGrupos(
                List.of(curso("Formativo", "Fulbol", PublicoCurso.ESTUDIANTE),
                        curso("Formativo", "Natacion", PublicoCurso.MIXTO),
                        curso("Recreativo", "Yoga", PublicoCurso.ESTUDIANTE)),
                List.of(grupo("Formativo", "Fulbol", 1, 20),
                        grupo("Formativo", "Natacion", 1, 20),
                        grupo("Recreativo", "Yoga", 1, 20)),
                List.of());

        assertEquals(3, grupoServicio.obtenerOcupacion(null, null, null).size());
        assertEquals(2, grupoServicio.obtenerOcupacion("Formativo", null, null).size());
        assertEquals("Natacion", unica(grupoServicio.obtenerOcupacion("Formativo", "Natacion", null)).getCurso());
        List<OcupacionGrupo> soloEstudiantes = grupoServicio.obtenerOcupacion(null, null, PublicoCurso.ESTUDIANTE);
        assertEquals(2, soloEstudiantes.size());
        assertTrue(soloEstudiantes.stream().allMatch(g -> g.getPublico() == PublicoCurso.ESTUDIANTE));
    }

    // ── aplicarPeriodo: validaciones ──

    private List<ClaveGrupo> claves(int... iterables) {
        return java.util.Arrays.stream(iterables).mapToObj(i -> new ClaveGrupo("Formativo", "Fulbol", 2026, i)).toList();
    }

    @Test
    void aplicarPeriodo_cierreNulo_lanzaFechasInvalidas() {
        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.aplicarPeriodo(HOY, null, claves(1)));
        verify(grupoGateway, never()).actualizarVentanaInscripcion(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aplicarPeriodo_cierreAnteriorALaApertura_lanzaFechasInvalidas() {
        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.aplicarPeriodo(HOY.plusDays(5), HOY.plusDays(2), claves(1)));
        verify(grupoGateway, never()).actualizarVentanaInscripcion(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aplicarPeriodo_cierreYaPasado_lanzaFechasInvalidas() {
        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.aplicarPeriodo(null, HOY.minusDays(1), claves(1)));
        verify(grupoGateway, never()).actualizarVentanaInscripcion(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aplicarPeriodo_sinGrupos_lanzaFechasInvalidas() {
        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.aplicarPeriodo(HOY, HOY.plusDays(30), List.of()));
    }

    @Test
    void aplicarPeriodo_unGrupoInexistente_noModificaNinguno() {
        when(grupoGateway.existeGrupo("Formativo", "Fulbol", 2026, 1)).thenReturn(true);
        when(grupoGateway.existeGrupo("Formativo", "Fulbol", 2026, 2)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> grupoServicio.aplicarPeriodo(HOY, HOY.plusDays(30), claves(1, 2)));

        verify(grupoGateway, never()).actualizarVentanaInscripcion(any(), any(), any(), any(), any(), any());
    }

    @Test
    void aplicarPeriodo_sinApertura_noPuedeQuedarElCierreAntesDeLaAperturaActualDelGrupo() {
        Grupo conAperturaFutura = grupo("Formativo", "Fulbol", 1, 20);
        conAperturaFutura.setFechaInscripcionApertura(HOY.plusDays(40));
        when(grupoGateway.existeGrupo("Formativo", "Fulbol", 2026, 1)).thenReturn(true);
        when(grupoGateway.obtenerGrupo("Formativo", "Fulbol", 2026, 1)).thenReturn(conAperturaFutura);

        assertThrows(FechasGrupoInvalidasExcepcion.class,
                () -> grupoServicio.aplicarPeriodo(null, HOY.plusDays(30), claves(1)));

        verify(grupoGateway, never()).actualizarVentanaInscripcion(any(), any(), any(), any(), any(), any());
    }

    // ── aplicarPeriodo: caso feliz ──

    @Test
    void aplicarPeriodo_actualizaLaVentanaDeCadaGrupoSeleccionado() {
        for (int i : new int[] {1, 2}) {
            when(grupoGateway.existeGrupo("Formativo", "Fulbol", 2026, i)).thenReturn(true);
            when(grupoGateway.actualizarVentanaInscripcion("Formativo", "Fulbol", 2026, i, HOY, HOY.plusDays(30)))
                    .thenReturn(grupo("Formativo", "Fulbol", i, 20));
        }

        List<Grupo> resultado = grupoServicio.aplicarPeriodo(HOY, HOY.plusDays(30), claves(1, 2));

        assertEquals(2, resultado.size());
        verify(grupoGateway).actualizarVentanaInscripcion("Formativo", "Fulbol", 2026, 1, HOY, HOY.plusDays(30));
        verify(grupoGateway).actualizarVentanaInscripcion("Formativo", "Fulbol", 2026, 2, HOY, HOY.plusDays(30));
    }

    @Test
    void aplicarPeriodo_noReabreLosGruposCerradosAMano() {
        when(grupoGateway.existeGrupo("Formativo", "Fulbol", 2026, 1)).thenReturn(true);
        when(grupoGateway.actualizarVentanaInscripcion(any(), any(), any(), any(), any(), any()))
                .thenReturn(grupo("Formativo", "Fulbol", 1, 20));

        grupoServicio.aplicarPeriodo(HOY, HOY.plusDays(30), claves(1));

        verify(grupoGateway, never()).cambiarCierreManual(any(), any(), any(), any(), anyBoolean());
    }
}
