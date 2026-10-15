package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ProrrogaRegistroAsistenciaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.ProrrogaRegistroAsistenciaId;

// JpaRepository (no CrudRepository): se necesita saveAndFlush para que la
// violacion de la PK compuesta (PERF_ID, PRR_ANIO, PRR_MES) en una carrera
// concurrente se vea de forma sincrona en el mismo metodo del gateway.
public interface IProrrogaRegistroAsistenciaRepositorio
        extends JpaRepository<ProrrogaRegistroAsistenciaEntidad, ProrrogaRegistroAsistenciaId> {

    List<ProrrogaRegistroAsistenciaEntidad> findByAnioAndMesAndEliminado(Integer anio, Integer mes, Integer eliminado);
}
