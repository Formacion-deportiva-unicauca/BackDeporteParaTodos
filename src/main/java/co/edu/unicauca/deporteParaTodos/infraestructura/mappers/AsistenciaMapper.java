package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AsistenciaEntidad;

public final class AsistenciaMapper {

    private AsistenciaMapper() {}

    public static Asistencia toDominio(AsistenciaEntidad entidad) {
        if (entidad == null)
            throw new NoProcesableEntidadException("AsistenciaEntidad no puede ser nula");
        Asistencia modelo = new Asistencia();
        modelo.setIdPerfil(entidad.getPerfilId());
        modelo.setClsCodigo(entidad.getClaseCodigo());
        modelo.setEliminado(entidad.getEliminado());
        modelo.setEstado(entidad.getEstado());
        return modelo;
    }

    public static AsistenciaEntidad toEntidad(Asistencia modelo) {
        if (modelo == null)
            throw new NoProcesableEntidadException("Asistencia modelo no puede ser nula");
        AsistenciaEntidad entidad = new AsistenciaEntidad();
        entidad.setPerfilId(modelo.getIdPerfil());
        entidad.setClaseCodigo(modelo.getClsCodigo());
        entidad.setEliminado(modelo.getEliminado());
        entidad.setEstado(modelo.getEstado());
        return entidad;
    }

    public static AtencionDto toDto(Asistencia modelo) {
        if (modelo == null)
            throw new NoProcesableEntidadException("Asistencia modelo no puede ser nula");
        AtencionDto dto = new AtencionDto();
        dto.setIdPerfil(modelo.getIdPerfil());
        dto.setIdClase(modelo.getClsCodigo());
        // Faltas acumuladas (etapa 2 de 5): estaAtendido ahora exige AMBAS
        // condiciones -- ASI_ESTADO='PRESENTE' Y no eliminada. Integer.equals()
        // es null-safe: null→true (activo), 0→true (activo), 1→false (eliminado).
        dto.setEstaAtendido("PRESENTE".equals(modelo.getEstado()) && !Integer.valueOf(1).equals(modelo.getEliminado()));
        return dto;
    }

    public static Asistencia fromDto(AtencionDto dto) {
        if (dto == null)
            throw new NoProcesableEntidadException("AtencionDto no puede ser nulo");
        Asistencia modelo = new Asistencia();
        modelo.setIdPerfil(dto.getIdPerfil());
        modelo.setClsCodigo(dto.getIdClase());
        modelo.setEliminado(Boolean.TRUE.equals(dto.getEstaAtendido()) ? 0 : 1);
        return modelo;
    }
}
