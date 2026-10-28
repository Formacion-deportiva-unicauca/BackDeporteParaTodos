package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        // ASI_ESTADO es NOT NULL sin DEFAULT a nivel de entidad JPA (el DEFAULT
        // 'PRESENTE' de la columna solo aplica si Hibernate omitiera el campo
        // del INSERT, lo cual no hace) -- debe fijarse explicitamente aqui o la
        // insercion viola la restriccion NOT NULL. El constructor de 3
        // argumentos de Asistencia ya lo inicializa en "PRESENTE".
        asisEntidad.setEstado(datosAsistencia.getEstado());
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

    @Override
    public Asistencia marcarPresente(String perfId, int clsId) {
        return upsertEstado(perfId, clsId, "PRESENTE");
    }

    @Override
    public Asistencia marcarAusente(String perfId, int clsId) {
        return upsertEstado(perfId, clsId, "AUSENTE");
    }

    // Faltas acumuladas (etapa 4 de 5): crea la fila si no existe, o la
    // reactiva (META_ELIMINADO=0) y fija su ASI_ESTADO -- unico punto que
    // escribe filas PRESENTE/AUSENTE durante la confirmacion.
    private Asistencia upsertEstado(String perfId, int clsId, String estado) {
        AsistenciaId idAsistencia = new AsistenciaId();
        idAsistencia.setPerfilId(perfId);
        idAsistencia.setClaseCodigo(clsId);
        AsistenciaEntidad entidad = repoAsistencia.findById(idAsistencia).orElseGet(() -> {
            AsistenciaEntidad nueva = new AsistenciaEntidad();
            nueva.setPerfilId(perfId);
            nueva.setClaseCodigo(clsId);
            return nueva;
        });
        entidad.setEliminado(0);
        entidad.setEstado(estado);
        AsistenciaEntidad guardada = repoAsistencia.save(entidad);
        return AsistenciaMapper.toDominio(guardada);
    }

    @Override
    public Map<String, Integer> contarFaltasPorGrupo(String categoria, String curso, Integer anio, Integer iterable,
            LocalDate hoy) {
        List<Object[]> filas = repoAsistencia.contarFaltasPorGrupo(categoria, curso, anio, iterable, hoy);
        Map<String, Integer> faltasPorAlumno = new HashMap<>();
        for (Object[] fila : filas) {
            faltasPorAlumno.put((String) fila[0], ((Number) fila[1]).intValue());
        }
        return faltasPorAlumno;
    }
}
