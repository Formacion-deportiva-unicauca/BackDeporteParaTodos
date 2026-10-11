package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.GrupoEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.GrupoId;

public interface IGrupoRepositorio extends CrudRepository<GrupoEntidad, GrupoId>{
    List<GrupoEntidad> findByEliminado(Integer eliminado);
    List<GrupoEntidad> findByCategoriaAndCursoAndEliminado(String categoria, String curso, Integer eliminado);

    @Query(value = """
    SELECT DISTINCT g.*
    FROM tbl_grupo g
    WHERE g.PERF_ID = :instructorId and g.META_ELIMINADO= 0
    """, nativeQuery = true)
    List<GrupoEntidad> obtenerGruposPorInstructor(@Param("instructorId") String instructorId);

    List<GrupoEntidad> findByIdInstructor(String idInstructor);

    /**
     * util para obtener el iterable identificador,
     * @param Categoria
     * @param Curso
     * @param anio
     * @return cantidad de coincidencias
     */
    int countByCategoriaAndCursoAndAnio(String Categoria, String Curso, Integer anio);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GrupoEntidad g WHERE g.categoria = :cat AND g.curso = :cur AND g.anio = :anio AND g.iterable = :iter")
    Optional<GrupoEntidad> findByIdWithLock(
        @Param("cat") String categoria,
        @Param("cur") String curso,
        @Param("anio") int anio,
        @Param("iter") int iterable
    );
}
