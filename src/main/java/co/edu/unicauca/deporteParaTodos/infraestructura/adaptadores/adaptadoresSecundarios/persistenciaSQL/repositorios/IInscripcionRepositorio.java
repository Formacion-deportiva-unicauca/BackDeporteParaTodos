package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.InscripcionEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.InscripcionId;

public interface IInscripcionRepositorio extends CrudRepository<InscripcionEntidad, InscripcionId> {

    @Query("""
SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
FROM InscripcionEntidad i
WHERE i.alumnoId = :alumnoId
  AND i.categoria = :categoria
  AND i.curso = :curso
  AND i.anio = :anio
  AND i.iterable = :iterable
  AND i.eliminado = 0
  AND i.estado = 'INSCRITO'
  AND i.fechaInscripcion <= CURRENT_TIMESTAMP
  AND i.fechaDesvinculacion IS NULL
""")
    boolean existeInscripcionActiva(
            @Param("alumnoId") String alumnoId,
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") int anio,
            @Param("iterable") int iterable);

    @Query("""
SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
FROM InscripcionEntidad i
WHERE i.alumnoId = :alumnoId
  AND i.categoria = :categoria
  AND i.curso = :curso
  AND i.anio = :anio
  AND i.iterable = :iterable
  AND i.estado = 'EN_ESPERA'
  AND i.eliminado = 0
  AND i.fechaDesvinculacion IS NULL
""")
    boolean existeEnEspera(
            @Param("alumnoId") String alumnoId,
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") int anio,
            @Param("iterable") int iterable);

    // Guard dedicado para desvincularInscripcion(): existencia de la fila SIN
    // filtrar por estado (aplica tanto a INSCRITO como a EN_ESPERA), pero exigiendo
    // que no haya sido desvinculada ya. No reutilizar existeInscripcion() para esto:
    // ese metodo se usa tambien en inscribir() para decidir si reactivar una fila
    // ya desvinculada, y agregarle este filtro rompería esa reactivacion.
    @Query("""
SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END
FROM InscripcionEntidad i
WHERE i.alumnoId = :alumnoId
  AND i.categoria = :categoria
  AND i.curso = :curso
  AND i.anio = :anio
  AND i.iterable = :iterable
  AND i.eliminado = 0
  AND i.fechaDesvinculacion IS NULL
""")
    boolean existeInscripcionSinDesvincular(
            @Param("alumnoId") String alumnoId,
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") int anio,
            @Param("iterable") int iterable);

    @Query("""
SELECT COUNT(i) FROM InscripcionEntidad i
WHERE i.categoria = :categoria
  AND i.curso = :curso
  AND i.anio = :anio
  AND i.iterable = :iterable
  AND i.eliminado = 0
  AND i.estado = 'INSCRITO'
  AND i.fechaDesvinculacion IS NULL
""")
    long contarInscripcionesActivasGrupo(
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") int anio,
            @Param("iterable") int iterable);

    @Query("""
SELECT COUNT(i) FROM InscripcionEntidad i
WHERE i.alumnoId = :alumnoId
  AND i.eliminado = 0
  AND i.estado = 'INSCRITO'
  AND i.fechaDesvinculacion IS NULL
""")
    long contarCursosActivosAlumno(@Param("alumnoId") String alumnoId);

    @Query("""
SELECT COUNT(i) FROM InscripcionEntidad i
WHERE i.categoria = :categoria
  AND i.curso = :curso
  AND i.anio = :anio
  AND i.iterable = :iterable
  AND i.estado = 'EN_ESPERA'
  AND i.eliminado = 0
  AND i.fechaDesvinculacion IS NULL
""")
    long contarEnEsperaGrupo(
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") int anio,
            @Param("iterable") int iterable);

    @Query("""
SELECT i FROM InscripcionEntidad i
WHERE i.categoria = :categoria
  AND i.curso = :curso
  AND i.anio = :anio
  AND i.iterable = :iterable
  AND i.estado = 'EN_ESPERA'
  AND i.eliminado = 0
  AND i.fechaDesvinculacion IS NULL
ORDER BY i.fechaInscripcion ASC
""")
    List<InscripcionEntidad> findEnEsperaOrdenados(
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") int anio,
            @Param("iterable") int iterable);

    @Query("""
SELECT i.alumnoId, p.perf_nombre, p.perfcorreo, i.fechaInscripcion
FROM InscripcionEntidad i, PerfilEntidad p
WHERE i.alumnoId = p.perf_id
  AND i.categoria = :categoria
  AND i.curso = :curso
  AND i.anio = :anio
  AND i.iterable = :iterable
  AND i.estado = 'EN_ESPERA'
  AND i.eliminado = 0
  AND i.fechaDesvinculacion IS NULL
ORDER BY i.fechaInscripcion ASC
""")
    List<Object[]> findEnEsperaConPerfil(
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") int anio,
            @Param("iterable") int iterable);

    // "Mis cursos": solo inscripciones activas del alumno (sin importar si el curso/grupo
    // destino fue eliminado logicamente -- eso se refleja aparte via el flag grupoActivo,
    // no ocultando la fila). Orden: INSCRITO antes que EN_ESPERA, luego mas reciente primero.
    @Query("""
SELECT i FROM InscripcionEntidad i
WHERE i.alumnoId = :alumnoId
  AND i.eliminado = 0
  AND i.fechaDesvinculacion IS NULL
ORDER BY CASE WHEN i.estado = 'INSCRITO' THEN 0 ELSE 1 END, i.anio DESC, i.iterable DESC
""")
    List<InscripcionEntidad> findMisCursos(@Param("alumnoId") String alumnoId);
}
