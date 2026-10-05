package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.GrupoDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.GrupoEntidad;

public class GrupoMapper {

    public static Grupo toDominio(GrupoEntidad entidad) {
        try {
            Grupo grupo = new Grupo();
            grupo.setCategoria(entidad.getCategoria());
            grupo.setCurso(entidad.getCurso());
            grupo.setAnio(entidad.getAnio());
            grupo.setIterable(entidad.getIterable());
            grupo.setImagenGrupo(entidad.getImagenGrupo());
            grupo.setCupos(entidad.getCupos());
            grupo.setIdInstructor(entidad.getIdInstructor());
            grupo.setFechaCreacion(entidad.getFechaCreacion());
            grupo.setFechaFinalizacion(entidad.getFechaFinalizacion());
            grupo.setFechaInscripcionApertura(entidad.getFechaInscripcionApertura());
            grupo.setFechaIncripcionCierre(entidad.getFechaIncripcionCierre());
            grupo.setPeriodo(entidad.getPeriodo());
            grupo.setEliminado(entidad.getEliminado());
            return grupo;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir GrupoEntidad a dominio: " + e.getMessage());
        }
    }

    public static GrupoEntidad toEntidad(Grupo grupo) {
        try {
            GrupoEntidad entidad = new GrupoEntidad(
                grupo.getCategoria(),
                grupo.getCurso(),
                grupo.getAnio(),
                grupo.getIterable(),
                grupo.getImagenGrupo(),
                grupo.getCupos(),
                grupo.getIdInstructor(),
                grupo.getFechaCreacion(),
                grupo.getFechaFinalizacion(),
                grupo.getFechaInscripcionApertura(),
                grupo.getFechaIncripcionCierre(),
                grupo.getPeriodo(),
                grupo.getEliminado() != null ? grupo.getEliminado() : 0);
            return entidad;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Grupo a entidad: " + e.getMessage());
        }
    }

    public static GrupoDto toDto(Grupo grupo) {
        try {
            GrupoDto dto = new GrupoDto();
            dto.setCategoria(grupo.getCategoria());
            dto.setCurso(grupo.getCurso());
            dto.setAnio(grupo.getAnio());
            dto.setIterable(grupo.getIterable());
            dto.setImagenGrupo(grupo.getImagenGrupo());
            dto.setCupos(grupo.getCupos());
            dto.setIdInstructor(grupo.getIdInstructor());
            dto.setFechaCreacion(grupo.getFechaCreacion());
            dto.setFechaFinalizacion(grupo.getFechaFinalizacion());
            dto.setFechaInscripcionApertura(grupo.getFechaInscripcionApertura());
            dto.setFechaIncripcionCierre(grupo.getFechaIncripcionCierre());
            dto.setPeriodo(grupo.getPeriodo());
            return dto;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Grupo a DTO: " + e.getMessage());
        }
    }

    public static Grupo fromDto(GrupoDto dto) {
        try {
            Grupo grupo = new Grupo(
                dto.getCategoria(),
                dto.getCurso(),
                dto.getAnio(),
                dto.getIterable(),
                dto.getImagenGrupo(),
                dto.getCupos(),
                dto.getIdInstructor(),
                dto.getFechaCreacion(),
                dto.getFechaFinalizacion(),
                dto.getFechaInscripcionApertura(),
                dto.getFechaIncripcionCierre(),
                dto.getPeriodo() != null ? dto.getPeriodo() : 0,
                null);
            return grupo;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir GrupoDto a dominio: " + e.getMessage());
        }
    }
}
