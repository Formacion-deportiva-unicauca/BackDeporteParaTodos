package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ProgramaEntidad;

public interface IProgramaRepositorio extends CrudRepository<ProgramaEntidad, String>{

    @Query("SELECT p FROM ProgramaEntidad p WHERE p.facultad.nombre = :facultad ORDER BY p.prg_nombre")
    List<ProgramaEntidad> findPorFacultad(@Param("facultad") String facultad);
}
