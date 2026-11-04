package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProgramaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ProgramaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IProgramaRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.ProgramaMapper;

@Service
public class ProgramaGateway implements IProgramaGateway {

    private final IProgramaRepositorio repoPrograma;

    public ProgramaGateway(IProgramaRepositorio repoPrograma) {
        this.repoPrograma = repoPrograma;
    }

    @Override
    public boolean existePrograma(String nombrePrograma) {
        return repoPrograma.existsById(nombrePrograma);
    }

    @Override
    public List<Programa> obtenerProgramas() {
        List<Programa> programas = new ArrayList<>();
        repoPrograma.findAll().forEach(e -> programas.add(ProgramaMapper.toDominio(e)));
        return programas;
    }

    @Override
    public List<Programa> obtenerProgramasPorFacultad(String nombreFacultad) {
        return repoPrograma.findPorFacultad(nombreFacultad).stream()
                .map(ProgramaMapper::toDominio)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Programa> obtenerPrograma(String nombrePrograma) {
        return repoPrograma.findById(nombrePrograma).map(ProgramaMapper::toDominio);
    }

    @Override
    public Programa insertarPrograma(Programa datosPrograma) {
        ProgramaEntidad entidad = ProgramaMapper.toEntidad(datosPrograma);
        ProgramaEntidad insertado = repoPrograma.save(entidad);
        return ProgramaMapper.toDominio(insertado);
    }

    @Override
    public Programa actualizarPrograma(Programa datosPrograma) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'actualizarPrograma'");
    }

    @Override
    public Programa eliminarPrograma(String nombrePrograma) {
        ProgramaEntidad entidad = repoPrograma.findById(nombrePrograma)
                .orElseThrow(NoExisteExcepcion::new);
        repoPrograma.delete(entidad);
        return ProgramaMapper.toDominio(entidad);
    }

}
