package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPlazoCargaAtencionesGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PlazoCargaAtenciones;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PlazoCargaAtencionesEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.PlazoCargaAtencionesId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPlazoCargaAtencionesRepositorio;

@Service
public class PlazoCargaAtencionesGateway implements IPlazoCargaAtencionesGateway {

    @Autowired
    private IPlazoCargaAtencionesRepositorio repoPlazoCarga;

    // ── Mapeo manual Entidad -> Dominio (regla del proyecto: nunca en el Servicio) ──
    private PlazoCargaAtenciones mapearEntidadADominio(PlazoCargaAtencionesEntidad entidad) {
        PlazoCargaAtenciones modelo = new PlazoCargaAtenciones();
        modelo.setIdInstructor(entidad.getIdInstructor());
        modelo.setAnio(entidad.getAnio());
        modelo.setPeriodo(entidad.getPeriodo());
        modelo.setFechaLimite(entidad.getFechaLimite());
        modelo.setIdCoordinador(entidad.getIdCoordinador());
        modelo.setFechaAsignacion(entidad.getFechaAsignacion());
        modelo.setEliminado(entidad.getEliminado());
        return modelo;
    }

    @Override
    public PlazoCargaAtenciones upsertPlazo(PlazoCargaAtenciones datos) {
        PlazoCargaAtencionesId id = new PlazoCargaAtencionesId(datos.getIdInstructor(), datos.getAnio(), datos.getPeriodo());
        PlazoCargaAtencionesEntidad entidad = repoPlazoCarga.findById(id).orElseGet(PlazoCargaAtencionesEntidad::new);
        entidad.setIdInstructor(datos.getIdInstructor());
        entidad.setAnio(datos.getAnio());
        entidad.setPeriodo(datos.getPeriodo());
        entidad.setFechaLimite(datos.getFechaLimite());
        entidad.setIdCoordinador(datos.getIdCoordinador());
        entidad.setFechaAsignacion(datos.getFechaAsignacion());
        entidad.setEliminado(0);
        PlazoCargaAtencionesEntidad guardado = repoPlazoCarga.save(entidad);
        return mapearEntidadADominio(guardado);
    }

    @Override
    public Map<String, PlazoCargaAtenciones> obtenerPlazosPorPeriodo(int anio, int periodo) {
        Map<String, PlazoCargaAtenciones> resultado = new HashMap<>();
        repoPlazoCarga.findByAnioAndPeriodoAndEliminado(anio, periodo, 0)
                .forEach(entidad -> resultado.put(entidad.getIdInstructor(), mapearEntidadADominio(entidad)));
        return resultado;
    }

    @Override
    public Map<String, Integer> contarClasesConAsistenciaPorInstructor(int anio, int periodo) {
        Map<String, Integer> resultado = new HashMap<>();
        for (Object[] fila : repoPlazoCarga.contarClasesConAsistenciaPorInstructor(anio, periodo)) {
            String idInstructor = (String) fila[0];
            Number clases = (Number) fila[1];
            resultado.put(idInstructor, clases.intValue());
        }
        return resultado;
    }
}
