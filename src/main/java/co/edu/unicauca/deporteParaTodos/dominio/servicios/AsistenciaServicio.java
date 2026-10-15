package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.VentanaRegistroAsistencia;
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
    private final IProrrogaRegistroAsistenciaGateway prorrogaGateway;
    private final Clock clock;

    public AsistenciaServicio(IAsistenciaGateway asistenciaGateway, IAlumnoGateway alumnoGateway,
            IClaseGateway claseGateway, IProrrogaRegistroAsistenciaGateway prorrogaGateway, Clock clock) {
        this.asistenciaGateway = asistenciaGateway;
        this.alumnoGateway = alumnoGateway;
        this.claseGateway = claseGateway;
        this.prorrogaGateway = prorrogaGateway;
        this.clock = clock;
    }

    @Override
    public List<Asistencia> obtenerAsistencias() {
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

    // SCRUM-80 (rediseno, bloque 2): misma regla que ClaseServicio -- optimiza
    // para no consultar la prorroga cuando la fecha ya es futura o el rol
    // esta exento (quien decide/lanza sigue siendo VentanaRegistroAsistencia).
    private LocalDate limiteProrrogaSiAplica(boolean esExentoDePlazo, String perfIdLlamador, LocalDate fecha, LocalDate hoy) {
        if (esExentoDePlazo || fecha.isAfter(hoy)) {
            return null;
        }
        ProrrogaRegistroAsistencia prorroga = prorrogaGateway
                .obtenerProrrogasPorMes(fecha.getYear(), fecha.getMonthValue())
                .get(perfIdLlamador);
        return prorroga != null ? prorroga.getFechaLimite() : null;
    }

    private void validarVentanaRegistro(Integer claseId, String perfIdLlamador, String rolLlamador) {
        Clase clase = claseGateway.obtenerClase(claseId);
        boolean esExentoDePlazo = !"Instructor".equals(rolLlamador);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limiteProrroga = limiteProrrogaSiAplica(esExentoDePlazo, perfIdLlamador, clase.getFecha(), hoy);
        VentanaRegistroAsistencia.validarRegistro(clase.getFecha(), hoy, esExentoDePlazo, limiteProrroga);
    }

    @Transactional
    @Override
    public void registrarAtencionesPorClase(List<AtencionDto> atenciones, Integer claseId,
            String perfIdLlamador, String rolLlamador) {
        if (claseId == null || !claseGateway.existeClase(claseId)) {
            throw new NoExisteExcepcion("No existe la clase con codigo " + claseId);
        }
        validarVentanaRegistro(claseId, perfIdLlamador, rolLlamador);

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
            // SonarCloud/PASO 0 (caracterizacion): existeAsistencia() no distingue
            // META_ELIMINADO -- se usa obtenerAsistencia() para decidir entre
            // insertar (no existe ninguna fila), reactivar (existe con eliminado=1)
            // o no hacer nada (ya existe activa -- idempotente), y lo mismo en
            // sentido inverso para eliminar.
            Optional<Asistencia> existente = asistenciaGateway.obtenerAsistencia(alumnoId, claseId);
            if (Boolean.TRUE.equals(dto.getEstaAtendido())) {
                if (existente.isEmpty()) {
                    asistenciaGateway.InsertarAsistencia(new Asistencia(alumnoId, claseId, 0));
                } else if (Integer.valueOf(1).equals(existente.get().getEliminado())) {
                    asistenciaGateway.reactivarAsistencia(alumnoId, claseId);
                }
                // else: ya existe activa -- idempotente, no se hace nada.
            } else {
                if (existente.isPresent() && !Integer.valueOf(1).equals(existente.get().getEliminado())) {
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
    public Asistencia eliminarAsistencia(String perfId, Long clsCodigo, String perfIdLlamador, String rolLlamador) {
        if (!asistenciaGateway.existeAsistencia(perfId, clsCodigo.intValue())) {
            throw new NoExisteExcepcion("La asistencia no existe");
        }
        Asistencia asistencia = asistenciaGateway.obtenerAsistencia(perfId, clsCodigo.intValue())
                .orElseThrow(() -> new NoExisteExcepcion("La asistencia no existe"));
        if (Integer.valueOf(1).equals(asistencia.getEliminado())) {
            throw new YaExisteElementoExcepcion("La asistencia ya se encuentra eliminada");
        }
        validarVentanaRegistro(clsCodigo.intValue(), perfIdLlamador, rolLlamador);
        return asistenciaGateway.eliminarAsistencia(perfId, clsCodigo);
    }
}
