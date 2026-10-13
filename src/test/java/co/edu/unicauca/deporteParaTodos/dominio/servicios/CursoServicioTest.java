package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CursoServicioTest {

    @Mock
    private ICursoGateway cursoGateway;

    @InjectMocks
    private CursoServicio cursoServicio;

    private static final String CATEGORIA = "Acuatico";
    private static final String NOMBRE    = "Natacion";

    @Test
    void cambiarEstadoCurso_exitoso_retornaDtoConEstadoActualizado() {
        Curso cursoActualizado = new Curso();
        cursoActualizado.setNombre(NOMBRE);
        cursoActualizado.setCategoriaCurso(CATEGORIA);
        cursoActualizado.setDescripcion("Descripcion");
        cursoActualizado.setDeporte("Natacion");
        cursoActualizado.setImagenId(1);
        cursoActualizado.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO))
            .thenReturn(cursoActualizado);

        Curso resultado = cursoServicio.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO);

        assertNotNull(resultado);
        assertEquals(EstadoCurso.INACTIVO, resultado.getEstadoCurso());
        verify(cursoGateway).cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO);
    }

    @Test
    void cambiarEstadoCurso_cursoNoExiste_lanzaNoExisteExcepcion() {
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
            () -> cursoServicio.cambiarEstadoCurso(CATEGORIA, NOMBRE, EstadoCurso.INACTIVO));

        verify(cursoGateway, never()).cambiarEstadoCurso(any(), any(), any());
    }

    // --- eliminarCursoPermanente ---

    @Test
    void eliminarCursoPermanente_exitoso_retornaDtoConEstadoInactivo() {
        Curso cursoActivo = new Curso();
        cursoActivo.setNombre(NOMBRE);
        cursoActivo.setCategoriaCurso(CATEGORIA);
        cursoActivo.setDescripcion("Descripcion");
        cursoActivo.setDeporte("Natacion");
        cursoActivo.setImagenId(1);
        cursoActivo.setEstadoCurso(EstadoCurso.ACTIVO);

        Curso cursoEliminado = new Curso();
        cursoEliminado.setNombre(NOMBRE);
        cursoEliminado.setCategoriaCurso(CATEGORIA);
        cursoEliminado.setDescripcion("Descripcion");
        cursoEliminado.setDeporte("Natacion");
        cursoEliminado.setImagenId(1);
        cursoEliminado.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(cursoActivo);
        when(cursoGateway.eliminarCursoPermanente(CATEGORIA, NOMBRE)).thenReturn(cursoEliminado);

        Curso resultado = cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE);

        assertNotNull(resultado);
        assertEquals(EstadoCurso.INACTIVO, resultado.getEstadoCurso());
        verify(cursoGateway).eliminarCursoPermanente(CATEGORIA, NOMBRE);
    }

    @Test
    void eliminarCursoPermanente_cursoNoExiste_lanzaNoExisteExcepcion() {
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
            () -> cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE));

        verify(cursoGateway, never()).eliminarCursoPermanente(any(), any());
    }

    @Test
    void eliminarCursoPermanente_yaEstaEliminado_lanzaYaExisteElementoExcepcion() {
        Curso cursoYaInactivo = new Curso();
        cursoYaInactivo.setNombre(NOMBRE);
        cursoYaInactivo.setCategoriaCurso(CATEGORIA);
        cursoYaInactivo.setDescripcion("Descripcion");
        cursoYaInactivo.setDeporte("Natacion");
        cursoYaInactivo.setImagenId(1);
        cursoYaInactivo.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(cursoYaInactivo);

        assertThrows(YaExisteElementoExcepcion.class,
            () -> cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE));

        verify(cursoGateway, never()).eliminarCursoPermanente(any(), any());
    }

    // SonarCloud javabugs:S2259: existeCurso() y obtenerCurso() son 2 consultas
    // separadas (TOCTOU) -- si el curso desaparece justo entre ambas,
    // obtenerCurso() retorna null y actual.getEstadoCurso() reventaba con NPE
    // real en vez de la NoExisteExcepcion de negocio esperada.
    @Test
    void eliminarCursoPermanente_obtenerCursoRetornaNull_lanzaNoExisteExcepcionSinNpe() {
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(null);

        assertThrows(NoExisteExcepcion.class,
            () -> cursoServicio.eliminarCursoPermanente(CATEGORIA, NOMBRE));

        verify(cursoGateway, never()).eliminarCursoPermanente(any(), any());
    }

    // --- cursosDeCategoria (regresión) ---

    @Test
    void cursosDeCategoria_sigueExcluyendoEliminados() {
        Curso cursoActivo = new Curso();
        cursoActivo.setNombre("Natacion");
        cursoActivo.setCategoriaCurso(CATEGORIA);
        cursoActivo.setDescripcion("Desc");
        cursoActivo.setDeporte("Natacion");
        cursoActivo.setImagenId(1);
        cursoActivo.setEstadoCurso(EstadoCurso.ACTIVO);

        when(cursoGateway.obtenerCursosDeCategoria(CATEGORIA))
            .thenReturn(java.util.List.of(cursoActivo));

        java.util.List<Curso> resultado = cursoServicio.cursosDeCategoria(CATEGORIA);

        assertEquals(1, resultado.size());
        assertTrue(resultado.stream().allMatch(d -> EstadoCurso.ACTIVO.equals(d.getEstadoCurso())),
            "cursosDeCategoria no debe retornar cursos INACTIVOS");
        verify(cursoGateway).obtenerCursosDeCategoria(CATEGORIA);
        verify(cursoGateway, never()).obtenerTodosCursosDeCategoria(any());
    }

    // --- todosLosCursosDeCategoria ---

    @Test
    void todosLosCursosDeCategoria_incluyeCursosEliminados() {
        Curso cursoActivo = new Curso();
        cursoActivo.setNombre("Natacion");
        cursoActivo.setCategoriaCurso(CATEGORIA);
        cursoActivo.setDescripcion("Desc");
        cursoActivo.setDeporte("Natacion");
        cursoActivo.setImagenId(1);
        cursoActivo.setEstadoCurso(EstadoCurso.ACTIVO);

        Curso cursoInactivo = new Curso();
        cursoInactivo.setNombre("Futbol");
        cursoInactivo.setCategoriaCurso(CATEGORIA);
        cursoInactivo.setDescripcion("Desc");
        cursoInactivo.setDeporte("Futbol");
        cursoInactivo.setImagenId(1);
        cursoInactivo.setEstadoCurso(EstadoCurso.INACTIVO);

        when(cursoGateway.obtenerTodosCursosDeCategoria(CATEGORIA))
            .thenReturn(java.util.List.of(cursoActivo, cursoInactivo));

        java.util.List<Curso> resultado = cursoServicio.todosLosCursosDeCategoria(CATEGORIA);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().anyMatch(d -> EstadoCurso.INACTIVO.equals(d.getEstadoCurso())),
            "Debe incluir al menos un curso con estadoCurso INACTIVO");
        verify(cursoGateway).obtenerTodosCursosDeCategoria(CATEGORIA);
        verify(cursoGateway, never()).obtenerCursosDeCategoria(any());
    }

    // --- cambiarEstadoInscripciones ---

    @Test
    void cambiarEstadoInscripciones_exitoso_retornaDtoConNuevoEstado() {
        Curso cursoCerrado = new Curso();
        cursoCerrado.setNombre(NOMBRE);
        cursoCerrado.setCategoriaCurso(CATEGORIA);
        cursoCerrado.setDescripcion("Descripcion");
        cursoCerrado.setDeporte("Natacion");
        cursoCerrado.setImagenId(1);
        cursoCerrado.setEstadoCurso(EstadoCurso.ACTIVO);
        cursoCerrado.setEstadoInscripciones(EstadoInscripciones.CERRADO);

        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.cambiarEstadoInscripciones(CATEGORIA, NOMBRE, EstadoInscripciones.CERRADO))
            .thenReturn(cursoCerrado);

        Curso resultado = cursoServicio.cambiarEstadoInscripciones(CATEGORIA, NOMBRE, EstadoInscripciones.CERRADO);

        assertNotNull(resultado);
        assertEquals(EstadoInscripciones.CERRADO, resultado.getEstadoInscripciones());
        verify(cursoGateway).cambiarEstadoInscripciones(CATEGORIA, NOMBRE, EstadoInscripciones.CERRADO);
    }

    @Test
    void cambiarEstadoInscripciones_cursoNoExiste_lanzaNoExisteExcepcion() {
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
            () -> cursoServicio.cambiarEstadoInscripciones(CATEGORIA, NOMBRE, EstadoInscripciones.CERRADO));

        verify(cursoGateway, never()).cambiarEstadoInscripciones(any(), any(), any());
    }

    // --- recuperarCursos ---

    @Test
    void recuperarCursos_delegaAlGateway() {
        Curso curso = new Curso();
        curso.setNombre(NOMBRE);
        curso.setCategoriaCurso(CATEGORIA);
        when(cursoGateway.obtenerCursos()).thenReturn(java.util.List.of(curso));

        java.util.List<Curso> resultado = cursoServicio.recuperarCursos();

        assertEquals(1, resultado.size());
        verify(cursoGateway).obtenerCursos();
    }

    // --- obtenerCurso ---

    @Test
    void obtenerCurso_existente_retornaCurso() {
        Curso curso = new Curso();
        curso.setNombre(NOMBRE);
        curso.setCategoriaCurso(CATEGORIA);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.obtenerCurso(CATEGORIA, NOMBRE)).thenReturn(curso);

        Curso resultado = cursoServicio.obtenerCurso(CATEGORIA, NOMBRE);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getNombre());
        verify(cursoGateway).obtenerCurso(CATEGORIA, NOMBRE);
    }

    // --- insertarCurso ---

    @Test
    void insertarCurso_nuevo_retornaCursoInsertado() {
        Curso nuevo = new Curso();
        nuevo.setNombre(NOMBRE);
        nuevo.setCategoriaCurso(CATEGORIA);
        nuevo.setDescripcion("Descripcion");
        nuevo.setDeporte("Natacion");
        nuevo.setImagenId(1);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(false);
        when(cursoGateway.insertarCurso(nuevo)).thenReturn(nuevo);

        Curso resultado = cursoServicio.insertarCurso(nuevo);

        assertNotNull(resultado);
        assertEquals(NOMBRE, resultado.getNombre());
        verify(cursoGateway).insertarCurso(nuevo);
    }

    // --- actualizarCurso ---

    @Test
    void actualizarCurso_existente_retornaCursoActualizado() {
        Curso datos = new Curso();
        datos.setNombre(NOMBRE);
        datos.setCategoriaCurso(CATEGORIA);
        datos.setDescripcion("Nueva descripcion");
        datos.setDeporte("Natacion");
        datos.setImagenId(1);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.actualizarCurso(CATEGORIA, NOMBRE, datos)).thenReturn(datos);

        Curso resultado = cursoServicio.actualizarCurso(CATEGORIA, NOMBRE, datos);

        assertNotNull(resultado);
        assertEquals("Nueva descripcion", resultado.getDescripcion());
        verify(cursoGateway).actualizarCurso(CATEGORIA, NOMBRE, datos);
    }

    // --- eliminarCurso ---

    @Test
    void eliminarCurso_existente_retornaCursoEliminado() {
        Curso cursoEliminado = new Curso();
        cursoEliminado.setNombre(NOMBRE);
        cursoEliminado.setCategoriaCurso(CATEGORIA);
        cursoEliminado.setEstadoCurso(EstadoCurso.INACTIVO);
        when(cursoGateway.existeCurso(CATEGORIA, NOMBRE)).thenReturn(true);
        when(cursoGateway.eliminarCurso(CATEGORIA, NOMBRE)).thenReturn(cursoEliminado);

        Curso resultado = cursoServicio.eliminarCurso(CATEGORIA, NOMBRE);

        assertNotNull(resultado);
        assertEquals(EstadoCurso.INACTIVO, resultado.getEstadoCurso());
        verify(cursoGateway).eliminarCurso(CATEGORIA, NOMBRE);
    }
}
