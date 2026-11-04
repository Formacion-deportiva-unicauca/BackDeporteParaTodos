package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.CursoDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.CursoEntidad;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;

public class CursoMapper {

    public static Curso toDominio(CursoEntidad entidad) {
        try {
            Curso fabricado = new Curso();
            fabricado.setNombre(entidad.getNombre());
            fabricado.setCategoriaCurso(entidad.getCategoriaCurso());
            fabricado.setDescripcion(entidad.getDescripcion());
            fabricado.setDeporte(entidad.getDeporte());
            fabricado.setImagenId(entidad.getObjImagen());
            fabricado.setEstadoCurso(
                Integer.valueOf(1).equals(entidad.getEliminado()) ? EstadoCurso.INACTIVO : EstadoCurso.ACTIVO
            );
            fabricado.setEstadoInscripciones(
                entidad.getEstadoInscripciones() != null
                    ? EstadoInscripciones.valueOf(entidad.getEstadoInscripciones())
                    : EstadoInscripciones.ABIERTO
            );
            fabricado.setPublico(PublicoCurso.desdeTexto(entidad.getPublico()));
            return fabricado;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir CursoEntidad a dominio: " + e.getMessage());
        }
    }

    public static CursoEntidad toEntidad(Curso curso) {
        try {
            CursoEntidad entidad = new CursoEntidad();
            entidad.setCategoriaCurso(curso.getCategoriaCurso());
            entidad.setNombre(curso.getNombre());
            entidad.setDescripcion(curso.getDescripcion());
            entidad.setDeporte(curso.getDeporte());
            entidad.setObjImagen(curso.getImagenId());
            entidad.setEstadoInscripciones(
                curso.getEstadoInscripciones() != null
                    ? curso.getEstadoInscripciones().name()
                    : EstadoInscripciones.ABIERTO.name()
            );
            entidad.setPublico(
                curso.getPublico() != null ? curso.getPublico().name() : PublicoCurso.MIXTO.name()
            );
            return entidad;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Curso a entidad: " + e.getMessage());
        }
    }

    public static CursoDto toDto(Curso curso) {
        try {
            CursoDto dto = new CursoDto();
            dto.setCategoriaCurso(curso.getCategoriaCurso());
            dto.setNombre(curso.getNombre());
            dto.setDescripcion(curso.getDescripcion());
            dto.setIdImagen(curso.getImagenId());
            dto.setDeporte(curso.getDeporte());
            dto.setEstadoCurso(curso.getEstadoCurso());
            dto.setEstadoInscripciones(curso.getEstadoInscripciones());
            dto.setPublico(curso.getPublico());
            return dto;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir Curso a DTO: " + e.getMessage());
        }
    }

    public static Curso fromDto(CursoDto dto) {
        try {
            Curso fabricado = new Curso();
            fabricado.setCategoriaCurso(dto.getCategoriaCurso());
            fabricado.setNombre(dto.getNombre());
            fabricado.setDescripcion(dto.getDescripcion());
            fabricado.setDeporte(dto.getDeporte());
            fabricado.setImagenId(dto.getIdImagen());
            fabricado.setEstadoInscripciones(dto.getEstadoInscripciones());
            fabricado.setPublico(dto.getPublico());
            return fabricado;
        } catch (Exception e) {
            throw new NoProcesableEntidadException("No fue posible convertir CursoDto a dominio: " + e.getMessage());
        }
    }
}
