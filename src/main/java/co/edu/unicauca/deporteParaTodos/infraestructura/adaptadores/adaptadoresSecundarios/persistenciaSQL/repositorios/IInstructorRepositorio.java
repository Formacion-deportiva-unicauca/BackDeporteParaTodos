package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.InstructorEntidad;

public interface IInstructorRepositorio extends CrudRepository<InstructorEntidad, String>{

    List<InstructorEntidad> findByEliminado(Integer eliminado);

    boolean existsByIdPerfilAndEliminado(String idPerfil, Integer eliminado);

    @Query(value = """
        SELECT
            inst.perf_id AS id,
            perf.perf_nombre AS nombre,
            perf.perf_correo AS correo,
            perf.perf_sexo AS sexo
        FROM tbl_instructor inst
        INNER JOIN tbl_perfil perf ON perf.perf_id = inst.perf_id
        WHERE inst.meta_eliminado = :eliminado
          AND perf.meta_eliminado = :eliminado
        """, nativeQuery = true)
    List<Object[]> buscarInstructoresRaw(@Param("eliminado") Integer eliminado);
}
