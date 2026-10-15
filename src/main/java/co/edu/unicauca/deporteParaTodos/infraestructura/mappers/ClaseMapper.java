package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ClaseDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ClaseEntidad;

public class ClaseMapper {

    // ClaseEntidad usa idGrupoCategoria/idGrupoCurso/idGrupoAnio/idGrupoIterable;
    // Clase y ClaseDto usan categoria/curso/anio/iterable.
    public static Clase toDominio(ClaseEntidad entidad) {
        try {
            Clase modelo = new Clase();
            modelo.setCodigo(entidad.getCodigo());
            modelo.setCategoria(entidad.getIdGrupoCategoria());
            modelo.setCurso(entidad.getIdGrupoCurso());
            modelo.setAnio(entidad.getIdGrupoAnio());
            modelo.setIterable(entidad.getIdGrupoIterable());
            modelo.setIdInstructor(entidad.getIdInstructor());
            modelo.setFecha(entidad.getFecha());
            modelo.setHoras(entidad.getHoras());
            modelo.setMinutos(entidad.getMinutos());
            modelo.setObservacion(entidad.getObservacion());
            modelo.setEliminado(entidad.getEliminado());
            modelo.setJustificacion(entidad.getJustificacion());
            return modelo;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir ClaseEntidad a dominio: " + e.getMessage());
        }
    }

    public static ClaseEntidad toEntidad(Clase clase) {
        try {
            ClaseEntidad entidad = new ClaseEntidad();
            entidad.setCodigo(clase.getCodigo());
            entidad.setIdGrupoCategoria(clase.getCategoria());
            entidad.setIdGrupoCurso(clase.getCurso());
            entidad.setIdGrupoAnio(clase.getAnio());
            entidad.setIdGrupoIterable(clase.getIterable());
            entidad.setIdInstructor(clase.getIdInstructor());
            entidad.setFecha(clase.getFecha());
            entidad.setHoras(clase.getHoras());
            entidad.setMinutos(clase.getMinutos());
            entidad.setObservacion(clase.getObservacion());
            entidad.setEliminado(clase.getEliminado());
            entidad.setJustificacion(clase.getJustificacion());
            return entidad;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Clase a entidad: " + e.getMessage());
        }
    }

    public static ClaseDto toDto(Clase clase) {
        try {
            ClaseDto dto = new ClaseDto();
            dto.setCodigo(clase.getCodigo());
            dto.setCategoria(clase.getCategoria());
            dto.setCurso(clase.getCurso());
            dto.setAnio(clase.getAnio());
            dto.setIterable(clase.getIterable());
            dto.setIdInstructor(clase.getIdInstructor());
            dto.setFecha(clase.getFecha());
            dto.setHoras(clase.getHoras());
            dto.setMinutos(clase.getMinutos());
            dto.setObservacion(clase.getObservacion());
            dto.setEliminado(clase.getEliminado());
            dto.setJustificacion(clase.getJustificacion());
            return dto;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Clase a DTO: " + e.getMessage());
        }
    }

    public static Clase fromDto(ClaseDto dto) {
        try {
            Clase modelo = new Clase();
            modelo.setCodigo(dto.getCodigo());
            modelo.setCategoria(dto.getCategoria());
            modelo.setCurso(dto.getCurso());
            modelo.setAnio(dto.getAnio());
            modelo.setIterable(dto.getIterable());
            modelo.setIdInstructor(dto.getIdInstructor());
            modelo.setFecha(dto.getFecha());
            modelo.setHoras(dto.getHoras());
            modelo.setMinutos(dto.getMinutos());
            modelo.setObservacion(dto.getObservacion());
            modelo.setEliminado(dto.getEliminado());
            modelo.setJustificacion(dto.getJustificacion());
            return modelo;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir ClaseDto a dominio: " + e.getMessage());
        }
    }
}
