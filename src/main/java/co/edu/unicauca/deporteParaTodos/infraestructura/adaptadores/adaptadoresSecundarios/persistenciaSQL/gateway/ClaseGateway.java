package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ClaseEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IClaseRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.ClaseMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;

@Service
public class ClaseGateway implements IClaseGateway {

    @Autowired
    private IClaseRepositorio repoClase;

    @Override
    public boolean existeClase(int id) {
        return repoClase.existsById(id);
    }

    @Override
    public boolean existeClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        return repoClase.existsByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
                categoria, curso, anio, iterable, fecha, 0);
    }

    @Override
    public Clase obtenerClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        Optional<ClaseEntidad> entidad = repoClase
                .findByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndFechaAndEliminado(
                        categoria, curso, anio, iterable, fecha, 0);
        return entidad.map(ClaseMapper::toDominio)
                .orElseThrow(() -> new NoExisteExcepcion("No existe una clase para el grupo en esa fecha"));
    }

    @Override
    public List<Clase> obtenerClasesGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        List<ClaseEntidad> entidades = repoClase
                .findByIdGrupoCategoriaAndIdGrupoCursoAndIdGrupoAnioAndIdGrupoIterableAndEliminado(
                        categoria, curso, anio, iterable, 0);
        return entidades.stream()
                .map(ClaseMapper::toDominio)
                .collect(Collectors.toList());
    }

    @Override
    public Clase insertarClase(Clase datoClase) {
        ClaseEntidad entidad = ClaseMapper.toEntidad(datoClase);
        entidad.setCodigo(null);
        entidad.setEliminado(0);
        ClaseEntidad claseInsertada = repoClase.save(entidad);
        return ClaseMapper.toDominio(claseInsertada);
    }

    @Override
    public Clase eliminarClase(int id) {
        ClaseEntidad entidad = repoClase.findById(id).orElseThrow(NoExisteExcepcion::new);
        repoClase.marcarComoEliminado(id);
        entidad.setEliminado(1);
        return ClaseMapper.toDominio(entidad);
    }
}
