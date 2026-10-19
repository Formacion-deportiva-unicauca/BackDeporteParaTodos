package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.NovedadClaseDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.NovedadClaseEntidad;

public class NovedadClaseMapper {

    public static NovedadClase toDominio(NovedadClaseEntidad entidad) {
        try {
            NovedadClase modelo = new NovedadClase();
            modelo.setCodigo(entidad.getCodigo());
            modelo.setCategoria(entidad.getCategoria());
            modelo.setCurso(entidad.getCurso());
            modelo.setAnio(entidad.getAnio());
            modelo.setIterable(entidad.getIterable());
            modelo.setFecha(entidad.getFecha());
            modelo.setMotivo(entidad.getMotivo());
            modelo.setDescripcion(entidad.getDescripcion());
            modelo.setIdUsuarioCreador(entidad.getIdUsuarioCreador());
            modelo.setFechaCreacion(entidad.getFechaCreacion());
            modelo.setIdUsuarioModificador(entidad.getIdUsuarioModificador());
            modelo.setFechaModificacion(entidad.getFechaModificacion());
            modelo.setVersion(entidad.getVersion());
            modelo.setEliminado(entidad.getEliminado());
            return modelo;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir NovedadClaseEntidad a dominio: " + e.getMessage());
        }
    }

    public static NovedadClaseEntidad toEntidad(NovedadClase modelo) {
        try {
            NovedadClaseEntidad entidad = new NovedadClaseEntidad();
            entidad.setCodigo(modelo.getCodigo());
            entidad.setCategoria(modelo.getCategoria());
            entidad.setCurso(modelo.getCurso());
            entidad.setAnio(modelo.getAnio());
            entidad.setIterable(modelo.getIterable());
            entidad.setFecha(modelo.getFecha());
            entidad.setMotivo(modelo.getMotivo());
            entidad.setDescripcion(modelo.getDescripcion());
            entidad.setIdUsuarioCreador(modelo.getIdUsuarioCreador());
            entidad.setFechaCreacion(modelo.getFechaCreacion());
            entidad.setIdUsuarioModificador(modelo.getIdUsuarioModificador());
            entidad.setFechaModificacion(modelo.getFechaModificacion());
            entidad.setVersion(modelo.getVersion());
            entidad.setEliminado(modelo.getEliminado());
            return entidad;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir NovedadClase a entidad: " + e.getMessage());
        }
    }

    public static NovedadClaseDto toDto(NovedadClase modelo) {
        try {
            NovedadClaseDto dto = new NovedadClaseDto();
            dto.setCodigo(modelo.getCodigo());
            dto.setCategoria(modelo.getCategoria());
            dto.setCurso(modelo.getCurso());
            dto.setAnio(modelo.getAnio());
            dto.setIterable(modelo.getIterable());
            dto.setFecha(modelo.getFecha());
            dto.setMotivo(modelo.getMotivo());
            dto.setDescripcion(modelo.getDescripcion());
            dto.setEstado("NO_REALIZADA");
            dto.setIdUsuarioCreador(modelo.getIdUsuarioCreador());
            dto.setFechaCreacion(modelo.getFechaCreacion());
            dto.setIdUsuarioModificador(modelo.getIdUsuarioModificador());
            dto.setFechaModificacion(modelo.getFechaModificacion());
            dto.setVersion(modelo.getVersion());
            return dto;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir NovedadClase a DTO: " + e.getMessage());
        }
    }

    public static NovedadClase fromDto(NovedadClaseDto dto) {
        try {
            NovedadClase modelo = new NovedadClase();
            modelo.setCodigo(dto.getCodigo());
            modelo.setCategoria(dto.getCategoria());
            modelo.setCurso(dto.getCurso());
            modelo.setAnio(dto.getAnio());
            modelo.setIterable(dto.getIterable());
            modelo.setFecha(dto.getFecha());
            modelo.setMotivo(dto.getMotivo());
            modelo.setDescripcion(dto.getDescripcion());
            return modelo;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir NovedadClaseDto a dominio: " + e.getMessage());
        }
    }
}
