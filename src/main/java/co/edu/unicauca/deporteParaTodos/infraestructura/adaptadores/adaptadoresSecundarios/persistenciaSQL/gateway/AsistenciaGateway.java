package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AsistenciaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.AsistenciaId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAsistenciaRepositorio;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.AsistenciaMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;

@Service
public class AsistenciaGateway implements IAsistenciaGateway {

    private final IAsistenciaRepositorio repoAsistencia;

    public AsistenciaGateway(IAsistenciaRepositorio repoAsistencia) {
        this.repoAsistencia = repoAsistencia;
    }

    @Override
    public boolean existeAsistencia(String perfId, int clsId) {
        AsistenciaId idAsistencia = new AsistenciaId();
        idAsistencia.setPerfilId(perfId);
        idAsistencia.setClaseCodigo(clsId);
        return repoAsistencia.existsById(idAsistencia);
    }

    @Override
    public List<Asistencia> obtenerAsistencias() {
        return null;
    }

    @Override
    public List<Asistencia> obtenerAtencionesPorClase(Integer claseId) {
        List<AsistenciaEntidad> entidades = repoAsistencia.findByClaseCodigo(claseId);
        List<Asistencia> lista = new ArrayList<>();
        entidades.forEach(entidad -> lista.add(AsistenciaMapper.toDominio(entidad)));
        return lista;
    }

    @Override
    public Optional<Asistencia> obtenerAsistencia(String perfId, int clsId) {
        AsistenciaId idAsistencia = new AsistenciaId();
        idAsistencia.setPerfilId(perfId);
        idAsistencia.setClaseCodigo(clsId);
        Optional<AsistenciaEntidad> existente = repoAsistencia.findById(idAsistencia);
        return existente.map(AsistenciaMapper::toDominio);
    }

    @Override
    public Asistencia InsertarAsistencia(Asistencia datosAsistencia) {
        AsistenciaEntidad asisEntidad = new AsistenciaEntidad();
        asisEntidad.setPerfilId(datosAsistencia.getIdPerfil());
        asisEntidad.setClaseCodigo(datosAsistencia.getClsCodigo());
        asisEntidad.setEliminado(0);
        AsistenciaEntidad insertada = repoAsistencia.save(asisEntidad);
        return AsistenciaMapper.toDominio(insertada);
    }

    @Override
    public Asistencia eliminarAsitencia(String perfId, int clsId) {
        AsistenciaId idAsistencia = new AsistenciaId();
        idAsistencia.setPerfilId(perfId);
        idAsistencia.setClaseCodigo(clsId);
        Optional<AsistenciaEntidad> asisExistente = repoAsistencia.findById(idAsistencia);
        if (asisExistente.isPresent()) {
            AsistenciaEntidad entidad = asisExistente.get();
            repoAsistencia.delete(entidad);
            return AsistenciaMapper.toDominio(entidad);
        }
        throw new NoExisteExcepcion();
    }

    @Override
    public Asistencia eliminarAsistencia(String perfId, Long clsCodigo) {
        AsistenciaId idAsistencia = new AsistenciaId();
        idAsistencia.setPerfilId(perfId);
        idAsistencia.setClaseCodigo(clsCodigo.intValue());
        Optional<AsistenciaEntidad> op = repoAsistencia.findById(idAsistencia);
        if (op.isEmpty()) {
            throw new NoExisteExcepcion("La asistencia no existe");
        }
        AsistenciaEntidad entidad = op.get();
        entidad.setEliminado(1);
        AsistenciaEntidad guardada = repoAsistencia.save(entidad);
        return AsistenciaMapper.toDominio(guardada);
    }

    @Override
    public Asistencia reactivarAsistencia(String perfId, int clsId) {
        AsistenciaId idAsistencia = new AsistenciaId();
        idAsistencia.setPerfilId(perfId);
        idAsistencia.setClaseCodigo(clsId);
        AsistenciaEntidad entidad = repoAsistencia.findById(idAsistencia)
                .orElseThrow(() -> new NoExisteExcepcion("La asistencia no existe"));
        entidad.setEliminado(0);
        AsistenciaEntidad guardada = repoAsistencia.save(entidad);
        return AsistenciaMapper.toDominio(guardada);
    }
}
