package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ClaseEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IClaseRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaseGatewayTest {

    @Mock
    private IClaseRepositorio repoClase;

    @InjectMocks
    private ClaseGateway claseGateway;

    private static final String  CATEGORIA = "Recreativo";
    private static final String  CURSO     = "Natacion";
    private static final Integer ANIO      = 2025;
    private static final Integer ITERABLE  = 1;
    private static final LocalDate FECHA   = LocalDate.of(2025, 1, 15);

    private ClaseEntidad entidadBase() {
        ClaseEntidad e = new ClaseEntidad();
        e.setCodigo(1);
        e.setIdGrupoCategoria(CATEGORIA);
        e.setIdGrupoCurso(CURSO);
        e.setIdGrupoAnio(ANIO);
        e.setIdGrupoIterable(ITERABLE);
        e.setIdInstructor("INS001");
        e.setFecha(FECHA);
        e.setHoras(1);
        e.setMinutos(30);
        e.setEliminado(0);
        return e;
    }

    private Clase claseBase() {
        Clase c = new Clase();
        c.setCodigo(1);
        c.setCategoria(CATEGORIA);
        c.setCurso(CURSO);
        c.setAnio(ANIO);
        c.setIterable(ITERABLE);
        c.setIdInstructor("INS001");
        c.setFecha(FECHA);
        c.setHoras(1);
        c.setMinutos(30);
        c.setEliminado(0);
        return c;
    }

    @Test
    void insertarClase_guardaEntidadConCodigoNuloYEliminado0() {
        when(repoClase.save(any(ClaseEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        claseGateway.insertarClase(claseBase());

        ArgumentCaptor<ClaseEntidad> captor = ArgumentCaptor.forClass(ClaseEntidad.class);
        verify(repoClase).save(captor.capture());
        assertNull(captor.getValue().getCodigo());
        assertEquals(0,         captor.getValue().getEliminado());
        assertEquals(CATEGORIA, captor.getValue().getIdGrupoCategoria());
        assertEquals(CURSO,     captor.getValue().getIdGrupoCurso());
    }

    @Test
    void existeClaseEnFecha_delegaAlRepositorioConEliminado0() {
        when(repoClase.existsByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
                CATEGORIA, CURSO, ANIO, ITERABLE, FECHA, 0)).thenReturn(true);

        boolean resultado = claseGateway.existeClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, FECHA);

        assertTrue(resultado);
        verify(repoClase).existsByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
                CATEGORIA, CURSO, ANIO, ITERABLE, FECHA, 0);
    }

    @Test
    void obtenerClaseEnFecha_existente_retornaClaseMapeada() {
        when(repoClase.findByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
                CATEGORIA, CURSO, ANIO, ITERABLE, FECHA, 0)).thenReturn(Optional.of(entidadBase()));

        Clase resultado = claseGateway.obtenerClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, FECHA);

        assertEquals(1,         resultado.getCodigo());
        assertEquals(CATEGORIA, resultado.getCategoria());
        assertEquals(CURSO,     resultado.getCurso());
    }

    @Test
    void obtenerClaseEnFecha_noExiste_lanzaNoExisteExcepcion() {
        when(repoClase.findByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
                CATEGORIA, CURSO, ANIO, ITERABLE, FECHA, 0)).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> claseGateway.obtenerClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, FECHA));
    }

    @Test
    void obtenerClasesGrupo_retornaListaMapeadaDesdeEntidades() {
        when(repoClase.findByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndEliminado(
                anyString(), anyString(), anyInt(), anyInt(), anyInt()))
                .thenReturn(List.of(entidadBase()));

        List<Clase> resultado = claseGateway.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals(1,         resultado.size());
        assertEquals(CATEGORIA, resultado.get(0).getCategoria());
        assertEquals(CURSO,     resultado.get(0).getCurso());
    }

    @Test
    void eliminarClase_existente_llamamarcarComoEliminadoYRetornaEliminado1() {
        when(repoClase.findById(1)).thenReturn(Optional.of(entidadBase()));

        Clase resultado = claseGateway.eliminarClase(1);

        verify(repoClase).marcarComoEliminado(1);
        assertEquals(1, resultado.getEliminado());
    }

    @Test
    void eliminarClase_noExiste_lanzaNoExisteExcepcion() {
        when(repoClase.findById(99)).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class, () -> claseGateway.eliminarClase(99));
    }
}
