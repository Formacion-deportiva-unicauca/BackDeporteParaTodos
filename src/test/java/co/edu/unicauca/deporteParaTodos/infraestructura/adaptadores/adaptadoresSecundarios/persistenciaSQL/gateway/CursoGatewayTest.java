package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.CursoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.CursoId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICategoriaCursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.ICursoRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IDeporteRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IImagenRepositorio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DependenciaFallida;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CursoGatewayTest {

    @Mock private ICursoRepositorio repoCurso;
    @Mock private IImagenRepositorio repoImagen;
    @Mock private IDeporteRepositorio repoDeporte;
    @Mock private ICategoriaCursoRepositorio repoCategoria;
    @Mock private ModelMapper mapper;

    @InjectMocks
    private CursoGateway cursoGateway;

    private static final String CATEGORIA = "Acuatico";
    private static final String NOMBRE    = "Natacion";

    private CursoEntidad entidadActiva() {
        CursoEntidad e = new CursoEntidad();
        e.setCategoriaCurso(CATEGORIA);
        e.setNombre(NOMBRE);
        e.setDeporte("Natacion");
        e.setDescripcion("Descripcion de prueba");
        e.setObjImagen(1);
        e.setEliminado(0);
        e.setEstadoInscripciones("ABIERTO");
        return e;
    }

    private CursoEntidad entidadEliminada() {
        CursoEntidad e = entidadActiva();
        e.setEliminado(1);
        return e;
    }

    private Curso cursoModelo() {
        Curso c = new Curso();
        c.setCategoriaCurso(CATEGORIA);
        c.setNombre(NOMBRE);
        c.setDeporte("Natacion");
        c.setDescripcion("Descripcion de prueba");
        c.setImagenId(1);
        c.setEstadoInscripciones(EstadoInscripciones.ABIERTO);
        return c;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // existeCurso
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void existeCurso_delegaAlRepositorioConClavesCompuestas() {
        CursoId id = new CursoId(CATEGORIA, NOMBRE);
        when(repoCurso.existsById(id)).thenReturn(true);

        boolean resultado = cursoGateway.existeCurso(CATEGORIA, NOMBRE);

        assertTrue(resultado);
        verify(repoCurso).existsById(id);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerCursos
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerCursos_retornaListaCompletaSinFiltrarEliminados() {
        // findAll() no filtra por eliminado: tanto activos como inactivos aparecen.
        doReturn(List.of(entidadActiva(), entidadEliminada())).when(repoCurso).findAll();

        List<Curso> resultado = cursoGateway.obtenerCursos();

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(c -> EstadoCurso.ACTIVO.equals(c.getEstadoCurso())));
        assertTrue(resultado.stream().anyMatch(c -> EstadoCurso.INACTIVO.equals(c.getEstadoCurso())));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerCurso
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerCurso_existente_retornaCursoMapeado() {
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE)))
                .thenReturn(Optional.of(entidadActiva()));

        Curso resultado = cursoGateway.obtenerCurso(CATEGORIA, NOMBRE);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getNombre());
        assertEquals(CATEGORIA, resultado.getCategoriaCurso());
        assertEquals(EstadoCurso.ACTIVO, resultado.getEstadoCurso());
    }

    @Test
    void obtenerCurso_noExiste_retornaNullEnVezDeExcepcion() {
        // COMPORTAMIENTO CUESTIONABLE: todos los demás métodos del gateway lanzan
        // NoExisteExcepcion cuando el curso no existe; este devuelve null,
        // exponiendo a cualquier caller a NullPointerException silenciosa.
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE)))
                .thenReturn(Optional.empty());

        Curso resultado = cursoGateway.obtenerCurso(CATEGORIA, NOMBRE);

        assertNull(resultado,
                "obtenerCurso retorna null cuando el curso no existe (inconsistente con el resto del gateway)");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // insertarCurso
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void insertarCurso_exitoso_setaEliminadoCeroAntesDePersistir() {
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoDeporte.existsById(any())).thenReturn(true);
        when(repoCurso.save(any())).thenReturn(entidadActiva());

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        cursoGateway.insertarCurso(cursoModelo());

        verify(repoCurso).save(captor.capture());
        assertEquals(0, captor.getValue().getEliminado(),
                "insertarCurso debe persistir con eliminado=0");
    }

    @Test
    void insertarCurso_categoriaNoExiste_lanzaDependenciaFallida() {
        when(repoCategoria.existsById(any())).thenReturn(false);

        assertThrows(DependenciaFallida.class, () -> cursoGateway.insertarCurso(cursoModelo()));

        verify(repoCurso, never()).save(any());
    }

    @Test
    void insertarCurso_imagenNoExiste_lanzaDependenciaFallida() {
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(false);

        assertThrows(DependenciaFallida.class, () -> cursoGateway.insertarCurso(cursoModelo()));

        verify(repoCurso, never()).save(any());
    }

    @Test
    void insertarCurso_deporteNoExiste_lanzaDependenciaFallida() {
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoDeporte.existsById(any())).thenReturn(false);

        assertThrows(DependenciaFallida.class, () -> cursoGateway.insertarCurso(cursoModelo()));

        verify(repoCurso, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // actualizarCurso
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void actualizarCurso_exitoso_actualizaCamposCorrectos() {
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoDeporte.existsById(any())).thenReturn(true);
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.of(entidadActiva()));
        when(repoCurso.save(any())).thenReturn(entidadActiva());

        Curso resultado = cursoGateway.actualizarCurso(CATEGORIA, NOMBRE, cursoModelo());

        assertNotNull(resultado);
        verify(repoCurso).save(any());
    }

    @Test
    void actualizarCurso_reactivaCursoEliminado_comportamientoActual() {
        // COMPORTAMIENTO CUESTIONABLE: si el curso tenía eliminado=1 (INACTIVO),
        // actualizarCurso lo reactiva silenciosamente con eliminado=0.
        // Este test documenta el comportamiento actual; corregirlo requiere decisión aparte.
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoDeporte.existsById(any())).thenReturn(true);
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE)))
                .thenReturn(Optional.of(entidadEliminada())); // parte de eliminado=1
        when(repoCurso.save(any())).thenReturn(entidadActiva());

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        cursoGateway.actualizarCurso(CATEGORIA, NOMBRE, cursoModelo());

        verify(repoCurso).save(captor.capture());
        assertEquals(0, captor.getValue().getEliminado(),
                "actualizarCurso siempre seta eliminado=0, reactivando cursos que estaban INACTIVOS");
    }

    @Test
    void actualizarCurso_noExiste_lanzaNoExisteExcepcion() {
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoDeporte.existsById(any())).thenReturn(true);
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> cursoGateway.actualizarCurso(CATEGORIA, NOMBRE, cursoModelo()));

        verify(repoCurso, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarCurso
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void eliminarCurso_exitoso_setaEliminadoUno() {
        CursoId id = new CursoId(CATEGORIA, NOMBRE);
        when(repoCurso.existsById(id)).thenReturn(true);
        when(repoCurso.findById(id)).thenReturn(Optional.of(entidadActiva()));
        when(repoCurso.save(any())).thenReturn(entidadEliminada());

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        cursoGateway.eliminarCurso(CATEGORIA, NOMBRE);

        verify(repoCurso).save(captor.capture());
        assertEquals(1, captor.getValue().getEliminado(),
                "eliminarCurso debe persistir con eliminado=1");
    }

    @Test
    void eliminarCurso_noExiste_lanzaNoExisteExcepcion() {
        when(repoCurso.existsById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> cursoGateway.eliminarCurso(CATEGORIA, NOMBRE));

        verify(repoCurso, never()).save(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerCursosDeCategoria
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerCursosDeCategoria_llamaRepoConEliminadoCero() {
        when(repoCurso.findByCategoriaCursoAndEliminado(CATEGORIA, 0))
                .thenReturn(List.of(entidadActiva()));

        List<Curso> resultado = cursoGateway.obtenerCursosDeCategoria(CATEGORIA);

        assertEquals(1, resultado.size());
        verify(repoCurso).findByCategoriaCursoAndEliminado(CATEGORIA, 0);
        verify(repoCurso, never()).findByCategoriaCurso(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerTodosCursosDeCategoria
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerTodosCursosDeCategoria_llamaFindByCategoriaSinFiltro() {
        when(repoCurso.findByCategoriaCurso(CATEGORIA))
                .thenReturn(List.of(entidadActiva(), entidadEliminada()));

        List<Curso> resultado = cursoGateway.obtenerTodosCursosDeCategoria(CATEGORIA);

        assertEquals(2, resultado.size());
        verify(repoCurso).findByCategoriaCurso(CATEGORIA);
        verify(repoCurso, never()).findByCategoriaCursoAndEliminado(any(), any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // cambiarEstadoCurso
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void cambiarEstadoCurso_estadoInactivo_setaEliminadoUno() {
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.of(entidadActiva()));
        when(repoCurso.save(any())).thenReturn(entidadEliminada());

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        cursoGateway.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO);

        verify(repoCurso).save(captor.capture());
        assertEquals(1, captor.getValue().getEliminado());
    }

    @Test
    void cambiarEstadoCurso_estadoActivo_setaEliminadoCero() {
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.of(entidadEliminada()));
        when(repoCurso.save(any())).thenReturn(entidadActiva());

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        cursoGateway.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.ACTIVO);

        verify(repoCurso).save(captor.capture());
        assertEquals(0, captor.getValue().getEliminado());
    }

    @Test
    void cambiarEstadoCurso_estadoCerrado_setaEliminadoCeroComoSiFueraActivo_comportamientoActual() {
        // COMPORTAMIENTO CUESTIONABLE (documentado en EstadoCurso.java con TODO):
        // EstadoCurso.CERRADO no es INACTIVO, por lo que el gateway lo trata como ACTIVO
        // y seta eliminado=0. Si el curso estaba eliminado, queda reactivado silenciosamente.
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.of(entidadEliminada()));
        when(repoCurso.save(any())).thenReturn(entidadActiva());

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        cursoGateway.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.CERRADO);

        verify(repoCurso).save(captor.capture());
        assertEquals(0, captor.getValue().getEliminado(),
                "EstadoCurso.CERRADO es código muerto: se trata como ACTIVO (eliminado=0)");
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarCursoPermanente
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void eliminarCursoPermanente_exitoso_setaEliminadoUno() {
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.of(entidadActiva()));
        when(repoCurso.save(any())).thenReturn(entidadEliminada());

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        cursoGateway.eliminarCursoPermanente(CATEGORIA, NOMBRE);

        verify(repoCurso).save(captor.capture());
        assertEquals(1, captor.getValue().getEliminado());
    }

    @Test
    void eliminarCursoPermanente_noExiste_lanzaNoExisteExcepcion() {
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class,
                () -> cursoGateway.eliminarCursoPermanente(CATEGORIA, NOMBRE));

        verify(repoCurso, never()).save(any());
    }

    // ── actualizarCurso: el publico se actualiza ──────────────────────────────

    @Test
    void actualizarCurso_cambiaElPublicoDelCurso() {
        CursoEntidad existente = entidadActiva();
        existente.setPublico("MIXTO");
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoDeporte.existsById(any())).thenReturn(true);
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.of(existente));
        when(repoCurso.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Curso cambio = cursoModelo();
        cambio.setPublico(PublicoCurso.FUNCIONARIO);

        Curso resultado = cursoGateway.actualizarCurso(CATEGORIA, NOMBRE, cambio);

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        verify(repoCurso).save(captor.capture());
        assertEquals("FUNCIONARIO", captor.getValue().getPublico());
        assertEquals(PublicoCurso.FUNCIONARIO, resultado.getPublico());
    }

    @Test
    void actualizarCurso_sinPublicoEnLaPeticion_loDejaComoMixto() {
        CursoEntidad existente = entidadActiva();
        existente.setPublico("ESTUDIANTE");
        when(repoCategoria.existsById(any())).thenReturn(true);
        when(repoImagen.existsById(any())).thenReturn(true);
        when(repoDeporte.existsById(any())).thenReturn(true);
        when(repoCurso.findById(new CursoId(CATEGORIA, NOMBRE))).thenReturn(Optional.of(existente));
        when(repoCurso.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Curso cambio = cursoModelo();
        cambio.setPublico(null);

        cursoGateway.actualizarCurso(CATEGORIA, NOMBRE, cambio);

        ArgumentCaptor<CursoEntidad> captor = ArgumentCaptor.forClass(CursoEntidad.class);
        verify(repoCurso).save(captor.capture());
        assertEquals("MIXTO", captor.getValue().getPublico());
    }
}
