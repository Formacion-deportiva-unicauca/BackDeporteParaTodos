package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.INovedadClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.NovedadClaseEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.INovedadClaseRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.NovedadClaseMapper;

@Service
public class NovedadClaseGateway implements INovedadClaseGateway {

    @Autowired
    private INovedadClaseRepositorio repoNovedad;

    @Override
    public NovedadClase insertarNovedad(NovedadClase datos) {
        NovedadClaseEntidad entidad = NovedadClaseMapper.toEntidad(datos);
        NovedadClaseEntidad guardada = repoNovedad.save(entidad);
        return NovedadClaseMapper.toDominio(guardada);
    }

    @Override
    public Optional<NovedadClase> obtenerNovedadPorId(Long codigo) {
        Optional<NovedadClaseEntidad> entidad = repoNovedad.findById(codigo);
        return entidad.map(NovedadClaseMapper::toDominio);
    }

    @Override
    public List<NovedadClase> obtenerNovedadesPorGrupoFecha(String categoria, String curso, Integer anio,
            Integer iterable, LocalDate fecha) {
        List<NovedadClaseEntidad> entidades;
        if (fecha != null) {
            entidades = repoNovedad.findByCategoriaAndCursoAndAnioAndIterableAndFechaAndEliminado(
                    categoria, curso, anio, iterable, fecha, 0);
        } else {
            entidades = repoNovedad.findByCategoriaAndCursoAndAnioAndIterableAndEliminado(
                    categoria, curso, anio, iterable, 0);
        }
        return entidades.stream()
                .map(NovedadClaseMapper::toDominio)
                .toList();
    }

    @Override
    public NovedadClase actualizarNovedad(NovedadClase datos) {
        NovedadClaseEntidad entidad = NovedadClaseMapper.toEntidad(datos);
        NovedadClaseEntidad guardada = repoNovedad.save(entidad);
        return NovedadClaseMapper.toDominio(guardada);
    }

    @Override
    public boolean existeNovedadActiva(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        return repoNovedad.existsByCategoriaAndCursoAndAnioAndIterableAndFechaAndEliminado(
                categoria, curso, anio, iterable, fecha, 0);
    }
}
