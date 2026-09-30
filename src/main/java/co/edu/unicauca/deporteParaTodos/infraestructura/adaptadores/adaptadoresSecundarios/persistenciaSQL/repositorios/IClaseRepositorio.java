package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ClaseEntidad;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface IClaseRepositorio extends CrudRepository<ClaseEntidad,Integer>{

    List<ClaseEntidad> findByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndEliminado(
        String idGrupoCategoria,
        String idGrupoCurso,
        Integer idGrupoAnio,
        Integer idGrupoIterable,
        Integer eliminado
        );

    boolean existsByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
        String idGrupoCategoria,
        String idGrupoCurso,
        Integer idGrupoAnio,
        Integer idGrupoIterable,
        LocalDate fecha,
        Integer eliminado
        );

    Optional<ClaseEntidad> findByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
        String idGrupoCategoria,
        String idGrupoCurso,
        Integer idGrupoAnio,
        Integer idGrupoIterable,
        LocalDate fecha,
        Integer eliminado
        );

    @Modifying
    @Transactional
    @Query("UPDATE ClaseEntidad c SET c.eliminado = 1 WHERE c.codigo =:codigo")
    int marcarComoEliminado(@Param("codigo") Integer codigo);
}
