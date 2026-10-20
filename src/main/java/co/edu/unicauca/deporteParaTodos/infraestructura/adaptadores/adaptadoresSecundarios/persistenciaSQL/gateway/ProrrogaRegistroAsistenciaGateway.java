package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ProrrogaRegistroAsistenciaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.ProrrogaRegistroAsistenciaId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IProrrogaRegistroAsistenciaRepositorio;

@Service
public class ProrrogaRegistroAsistenciaGateway implements IProrrogaRegistroAsistenciaGateway {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProrrogaRegistroAsistenciaGateway.class);

    @Autowired
    private IProrrogaRegistroAsistenciaRepositorio repoProrroga;

    // ── Mapeo manual Entidad -> Dominio (regla del proyecto: nunca en el Servicio) ──
    private ProrrogaRegistroAsistencia mapearEntidadADominio(ProrrogaRegistroAsistenciaEntidad entidad) {
        ProrrogaRegistroAsistencia modelo = new ProrrogaRegistroAsistencia();
        modelo.setIdInstructor(entidad.getIdInstructor());
        modelo.setAnio(entidad.getAnio());
        modelo.setMes(entidad.getMes());
        modelo.setDias(entidad.getDias());
        modelo.setFechaLimite(entidad.getFechaLimite());
        modelo.setIdCoordinador(entidad.getIdCoordinador());
        modelo.setFechaAsignacion(entidad.getFechaAsignacion());
        modelo.setEliminado(entidad.getEliminado());
        return modelo;
    }

    @Override
    public Map<String, ProrrogaRegistroAsistencia> obtenerProrrogasPorMes(int anio, int mes) {
        Map<String, ProrrogaRegistroAsistencia> resultado = new HashMap<>();
        repoProrroga.findByAnioAndMesAndEliminado(anio, mes, 0)
                .forEach(entidad -> resultado.put(entidad.getIdInstructor(), mapearEntidadADominio(entidad)));
        return resultado;
    }

    @Override
    public ProrrogaRegistroAsistencia upsertProrroga(ProrrogaRegistroAsistencia datos) {
        ProrrogaRegistroAsistenciaId id = new ProrrogaRegistroAsistenciaId(
                datos.getIdInstructor(), datos.getAnio(), datos.getMes());
        ProrrogaRegistroAsistenciaEntidad entidad = repoProrroga.findById(id)
                .orElseGet(ProrrogaRegistroAsistenciaEntidad::new);
        entidad.setIdInstructor(datos.getIdInstructor());
        entidad.setAnio(datos.getAnio());
        entidad.setMes(datos.getMes());
        entidad.setDias(datos.getDias());
        entidad.setFechaLimite(datos.getFechaLimite());
        entidad.setIdCoordinador(datos.getIdCoordinador());
        entidad.setFechaAsignacion(datos.getFechaAsignacion());
        entidad.setEliminado(0);
        // La violacion de la PK compuesta solo se dispara en el flush -- se usa
        // saveAndFlush() (no save()) dentro del try/catch, mismo patron que
        // PerfilGateway.actualizarPerfil() (SCRUM-182).
        try {
            ProrrogaRegistroAsistenciaEntidad guardado = repoProrroga.saveAndFlush(entidad);
            return mapearEntidadADominio(guardado);
        } catch (DataIntegrityViolationException e) {
            throw new YaExisteElementoExcepcion(
                    "Ya existe una prorroga para el instructor " + datos.getIdInstructor()
                            + " en " + datos.getAnio() + "-" + datos.getMes());
        }
    }

    @Override
    public void eliminarProrroga(String idInstructor, int anio, int mes) {
        ProrrogaRegistroAsistenciaId id = new ProrrogaRegistroAsistenciaId(idInstructor, anio, mes);
        try {
            repoProrroga.deleteById(id);
        } catch (EmptyResultDataAccessException e) {
            // dias=0 sobre un instructor SIN prorroga: no hay nada que borrar.
            LOGGER.debug("No existe prorroga para eliminar: instructor={}, anio={}, mes={}", idInstructor, anio, mes);
        }
    }
}
