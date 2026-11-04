package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.List;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;

public interface ICursoGateway{
    /**
     * Verifica la existencia de un curso en el sistema
     * @param categoria identificador de la categoria
     * @param nombreCurso identifiacdor del curso
     * @return true si existe, false de lo contrario
     */
    public boolean existeCurso(String categoria, String nombreCurso);
    /**
     * Obtinen toda los cursos en el sistema sin restricciones
     * @return lista de cursos.
     */
    public List<Curso> obtenerCursos();
    /**
     * Obtiene un curso del sistema a partir de sus identificadores
     * @param categoria identificador de la categoria
     * @param nombreCurso identificador del curso
     * @return curso enconstrado
     */
    public Curso obtenerCurso(String categoria, String nombreCurso);
    /***
     * Obteine todos los cursos de una categoria que esten disponibles
     * @param categoria
     * @return lista de cursos en formato del dominio
     */
    public List<Curso> obtenerCursosDeCategoria(String nombreCategoria);
    /**
     * Obtiene todos los cursos de una categoria sin filtrar por estado,
     * incluyendo los marcados como eliminados (meta_eliminado=1)
     * @param nombreCategoria titulo de la categoria
     * @return lista completa de cursos de la categoria
     */
    public List<Curso> obtenerTodosCursosDeCategoria(String nombreCategoria);
    /***
     * Registra un curso en el sistema
     * @param curso entidad a ser insertada
     * @return curso insertado
     */
    public Curso insertarCurso(Curso curso);
    /***
     * Actualiza la informacion de un curso
     * @param categoria identificador de la categoria
     * @param nombre identificador del curso
     * @param curso datos a actualizar
     * @return datos actualizados
     */
    public Curso actualizarCurso(String categoria, String nombre, Curso curso);
    /**
     * marca un curso como eliminado
     * @param categoria, identificador de la categoria
     * @param curso, identificador del curso
     * @return curso eliminado
     */
    public Curso eliminarCurso(String categoria, String curso);
    /**
     * Cambia el estado de un curso actualizando meta_eliminado en BD
     * @param categoria identificador de la categoria
     * @param nombreCurso identificador del curso
     * @param estado nuevo estado (ACTIVO → 0, INACTIVO → 1)
     * @return curso con estado actualizado
     */
    public Curso cambiarEstadoCurso(String categoria, String nombreCurso, EstadoCurso estado);
    /**
     * Borrado lógico de un curso: setea meta_eliminado = 1 sin eliminar el registro
     * @param categoria identificador de la categoria
     * @param nombreCurso identificador del curso
     * @return curso con meta_eliminado en 1
     */
    public Curso eliminarCursoPermanente(String categoria, String nombreCurso);
}
