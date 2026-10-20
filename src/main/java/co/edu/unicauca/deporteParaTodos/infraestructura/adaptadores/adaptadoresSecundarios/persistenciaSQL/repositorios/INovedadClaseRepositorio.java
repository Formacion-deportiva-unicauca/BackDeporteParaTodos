package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.NovedadClaseEntidad;

public interface INovedadClaseRepositorio extends CrudRepository<NovedadClaseEntidad, Long> {

    List<NovedadClaseEntidad> findByCategoriaAndCursoAndAnioAndIterableAndFechaAndEliminado(
            String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha, Integer eliminado);

    boolean existsByCategoriaAndCursoAndAnioAndIterableAndFechaAndEliminado(
            String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha, Integer eliminado);

    List<NovedadClaseEntidad> findByCategoriaAndCursoAndAnioAndIterableAndEliminado(
            String categoria, String curso, Integer anio, Integer iterable, Integer eliminated);
}
