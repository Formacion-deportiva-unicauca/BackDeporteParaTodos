package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PerfilEntidad;

public class PerfilMapper {

    public static Perfil toDominio(PerfilEntidad entidad) {
        // rol, facultad y tipoAlumno se hardcodean null; obtenerUsuario() los asigna
        // después de esta conversión según consultas adicionales a BD
        try {
            Perfil perfil = new Perfil();
            perfil.setId(entidad.getPerf_id());
            perfil.setNombre(entidad.getPerf_nombre());
            perfil.setCorreo(entidad.getPerfcorreo());      // sin guión bajo (campo DB: perf_correo)
            perfil.setImagen(entidad.getPerf_imagen());
            perfil.setTipoId(entidad.getPerf_tipo());
            perfil.setSexo(entidad.getPerf_Sexo());         // getter con S mayúscula
            perfil.setRol(null);
            perfil.setFacultad(null);
            perfil.setTipoAlumno(null);
            return perfil;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir PerfilEntidad a dominio: " + e.getMessage());
        }
    }

    public static PerfilEntidad toEntidad(Perfil perfil) {
        try {
            PerfilEntidad entidad = new PerfilEntidad();
            entidad.setPerf_id(perfil.getId());
            entidad.setPerf_nombre(perfil.getNombre());
            entidad.setPerfcorreo(perfil.getCorreo());       // sin guión bajo
            entidad.setPerf_imagen(perfil.getImagen());
            entidad.setPerf_tipo(perfil.getTipoId());
            entidad.setPerf_Sexo(perfil.getSexo());          // setter con S mayúscula
            entidad.setEliminado(0);
            return entidad;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Perfil a entidad: " + e.getMessage());
        }
    }

    public static PerfilDto toDto(Perfil perfil) {
        try {
            PerfilDto dto = new PerfilDto();
            dto.setId(perfil.getId());
            dto.setNombre(perfil.getNombre());
            dto.setCorreo(perfil.getCorreo());
            dto.setRole(perfil.getRol());           // ASIMÉTRICO: rol (dominio) → role (DTO)
            dto.setSexo(perfil.getSexo());
            dto.setTipoId(perfil.getTipoId());
            dto.setFacultad(perfil.getFacultad());
            dto.setTipoAlumno(perfil.getTipoAlumno());
            dto.setPrograma(perfil.getPrograma());
            dto.setDependencia(perfil.getDependencia());
            // alumnoCodigo no se mapea — consistente con comportamiento de fabricarDeModelo anterior
            return dto;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Perfil a PerfilDto: " + e.getMessage());
        }
    }

    public static Perfil fromDto(PerfilDto dto) {
        try {
            Perfil perfil = new Perfil();
            perfil.setId(dto.getId());
            perfil.setNombre(dto.getNombre());
            perfil.setCorreo(dto.getCorreo());
            perfil.setTipoId(dto.getTipoId());
            perfil.setSexo(dto.getSexo());
            perfil.setRol(dto.getRole());           // ASIMÉTRICO: role (DTO) → rol (dominio)
            perfil.setFacultad(dto.getFacultad());
            perfil.setTipoAlumno(dto.getTipoAlumno());
            perfil.setAlumnoCodigo(dto.getAlumnoCodigo());
            perfil.setPrograma(dto.getPrograma());
            perfil.setDependencia(dto.getDependencia());
            perfil.setImagen(null);
            return perfil;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir PerfilDto a dominio: " + e.getMessage());
        }
    }
}
