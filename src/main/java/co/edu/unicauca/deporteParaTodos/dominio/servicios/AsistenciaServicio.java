package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.AsistenciaMapper;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ErrorInternoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class AsistenciaServicio implements IAsistenciaServicio {

    private final IAsistenciaGateway asistenciaGateway;
    private final IAlumnoGateway alumnoGateway;
    private final IClaseGateway claseGateway;

    public AsistenciaServicio(IAsistenciaGateway asistenciaGateway, IAlumnoGateway alumnoGateway, IClaseGateway claseGateway) {
        this.asistenciaGateway = asistenciaGateway;
        this.alumnoGateway = alumnoGateway;
        this.claseGateway = claseGateway;
    }

    @Override
    public List<Asistencia> obtenerAsistencias() {
        // SonarCloud javabugs:S2259: obtenerAsistencias() no esta implementado en
        // el gateway y retorna null (no una lista vacia) -- se guarda contra eso
        // en vez de confiar en que el contrato del metodo garantice un valor no nulo.
        List<Asistencia> asistencias = asistenciaGateway.obtenerAsistencias();
        if (asistencias == null || asistencias.isEmpty()) {
            throw new ListadoVacioExcepcion("No hay registro de asistencias");
        }
        return asistencias;
    }

    @Override
    public List<AtencionDto> obtenerAtencionesPorClase(Integer claseId) {
        List<Asistencia> asistencias = asistenciaGateway.obtenerAtencionesPorClase(claseId);
        if (asistencias.isEmpty()) {
            throw new ListadoVacioExcepcion("No hay atenciones registradas para la clase consultada");
        }
        List<AtencionDto> listaDtos = new ArrayList<>();
        asistencias.forEach(modelo -> listaDtos.add(AsistenciaMapper.toDto(modelo)));
        return listaDtos;
    }

    @Transactional
    @Override
    public void registrarAtencionesPorClase(List<AtencionDto> atenciones, Integer claseId) {
        if (claseId == null || !claseGateway.existeClase(claseId)) {
            throw new NoExisteExcepcion("No existe la clase con codigo " + claseId);
        }
        if (atenciones == null || atenciones.isEmpty()) {
            return;
        }
        for (AtencionDto dto : atenciones) {
            if (dto == null || dto.getIdPerfil() == null || dto.getEstaAtendido() == null) {
                continue;
            }
            String alumnoId = dto.getIdPerfil();
            if (alumnoId == null || alumnoId.isBlank()) {
                continue;
            }
            if (!alumnoGateway.existeAlumno(alumnoId)) {
                throw new NoExisteExcepcion("No existe el perfil de alumno con id " + alumnoId);
            }
            if (dto.getIdClase() != null && !dto.getIdClase().equals(claseId)) {
                throw new ErrorInternoException("La clase indicada en el DTO no coincide con la claseId proporcionada");
            }
            if (Boolean.TRUE.equals(dto.getEstaAtendido())) {
                if (!asistenciaGateway.existeAsistencia(alumnoId, claseId)) {
                    Asistencia asistencia = new Asistencia(alumnoId, claseId, 0);
                    asistenciaGateway.InsertarAsistencia(asistencia);
                }
            } else {
                if (asistenciaGateway.existeAsistencia(alumnoId, claseId)) {
                    asistenciaGateway.eliminarAsitencia(alumnoId, claseId);
                }
            }
        }
    }

    @Override
    public Asistencia obtenerAsistencia(String perfId, int clsId) {
        return asistenciaGateway.obtenerAsistencia(perfId, clsId).orElseThrow(() -> new NoExisteExcepcion());
    }

    @Override
    public Asistencia InsertarAsistencia(Asistencia datosAsistencia) {
        if (asistenciaGateway.existeAsistencia(datosAsistencia.getIdPerfil(), datosAsistencia.getClsCodigo())) {
            throw new YaExisteElementoExcepcion("Ya existe el elemento");
        }
        return asistenciaGateway.InsertarAsistencia(datosAsistencia);
    }

    @Override
    public Asistencia eliminarAsitencia(String perfId, int clsId) {
        if (asistenciaGateway.existeAsistencia(perfId, clsId)) {
            return asistenciaGateway.eliminarAsitencia(perfId, clsId);
        }
        throw new NoExisteExcepcion();
    }

    @Override
    public Asistencia eliminarAsistencia(String perfId, Long clsCodigo) {
        if (!asistenciaGateway.existeAsistencia(perfId, clsCodigo.intValue())) {
            throw new NoExisteExcepcion("La asistencia no existe");
        }
        Asistencia asistencia = asistenciaGateway.obtenerAsistencia(perfId, clsCodigo.intValue())
                .orElseThrow(() -> new NoExisteExcepcion("La asistencia no existe"));
        if (Integer.valueOf(1).equals(asistencia.getEliminado())) {
            throw new YaExisteElementoExcepcion("La asistencia ya se encuentra eliminada");
        }
        return asistenciaGateway.eliminarAsistencia(perfId, clsCodigo);
    }
}
