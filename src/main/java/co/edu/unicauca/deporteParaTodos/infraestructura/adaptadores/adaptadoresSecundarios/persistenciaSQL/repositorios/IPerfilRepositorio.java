package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PerfilEntidad;
import java.util.Optional;


// JpaRepository (no CrudRepository) -- necesitamos saveAndFlush() para que la
// violacion de UQ_PERFIL_CORREO se dispare de forma sincrona y sea capturable
// dentro del mismo metodo (ver PerfilGateway.actualizarPerfil, SCRUM-182).
public interface IPerfilRepositorio extends JpaRepository<PerfilEntidad,String>{
    //List<GrupoEntidad> findByCategoriaAndCursoAndEliminado(String categoria, String curso, Integer eliminado);
    Optional<PerfilEntidad> findByPerfcorreo(String perfcorreo);
}
