package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
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
}
