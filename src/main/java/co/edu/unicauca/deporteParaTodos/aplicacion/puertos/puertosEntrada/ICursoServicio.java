package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;

public interface ICursoServicio {
    /***
     * Obtiene todos los cursos del sistema sin restricciones
     * @return lista de cursos en dominio
     */
    public List<Curso> recuperarCursos();
    /***
     * Obteine todos los cursos de una categoria que esten disponibles
     * @param categoria
     * @return lista de cursos en dominio
     */
    public List<Curso> cursosDeCategoria(String categoria);
    /***
     * Obtiene todos los cursos de una categoria sin filtrar por estado,
     * incluyendo cursos marcados como inactivos/eliminados
     * @param categoria titulo de la categoria
     * @return lista completa de cursos en dominio
     */
    public List<Curso> todosLosCursosDeCategoria(String categoria);
    /***
     * Cursos de una categoria que un alumno puede ver: activos, dirigidos a su tipo de vinculo (o a toda la comunidad)
     * y con al menos un grupo con inscripciones abiertas por fecha.
     * @param categoria titulo de la categoria
     * @param alumnoId identificador del perfil del alumno (null = tipo desconocido: solo cursos para toda la comunidad)
     * @return cursos visibles para ese alumno
     */
    public List<Curso> cursosDeCategoriaParaAlumno(String categoria, String alumnoId);
    /***
     * Igual que cursosDeCategoriaParaAlumno pero sobre todos los cursos del sistema.
     */
    public List<Curso> recuperarCursosParaAlumno(String alumnoId);
    /***
     * Obtien un curso del sistema a partir del titulo de la categoria y el nombre del curso que fungen como identificadores
     * @param titulo identificador de la categoria
     * @param nombre identificador del curso
     * @return curso encontrado
     */
    public Curso obtenerCurso(String titulo, String nombre);
    /***
     * Inserta un curso en el sistema
     * @param datosCurso informacion del curso en dominio
     * @return curso registrado
     */
    public Curso insertarCurso(Curso datosCurso);
    /***
     * Actualiza la informacion de un curso en el sistema, no altera los identificadores
     * @param categoria identificador de la categoria a la que pertenece el curso
     * @param curso identificador del curso
     * @param datCurso datos a actualizar en dominio
     * @return datos actualizados
     */
    public Curso actualizarCurso(String categoria, String curso, Curso datCurso);
    /***
     * marca como eliminado un curso
     * @param categoria, identificador de la categoria
     * @param curso, identificador del curso
     * @return curso eliminado
     */
    public Curso eliminarCurso(String categoria, String curso);
    /***
     * Cambia el estado de un curso (ACTIVO/INACTIVO)
     * @param categoria identificador de la categoria
     * @param nombreCurso identificador del curso
     * @param estado nuevo estado a aplicar
     * @return curso con estado actualizado
     */
    public Curso cambiarEstadoCurso(String categoria, String nombreCurso, EstadoCurso estado);
    /***
     * Borrado lógico de un curso: marca meta_eliminado=1
     * Lanza NoExisteExcepcion si el curso no existe
     * Lanza YaExisteElementoExcepcion si ya estaba eliminado (meta_eliminado=1)
     * @param categoria identificador de la categoria
     * @param nombreCurso identificador del curso
     * @return curso con estadoCurso INACTIVO
     */
    public Curso eliminarCursoPermanente(String categoria, String nombreCurso);
}
