package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionResumen;
import co.edu.unicauca.deporteParaTodos.infraestructura.mappers.HorarioMapper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InscripcionResumenDto {

    private String categoria;
    private String curso;
    private int anio;
    private int iterable;
    private int periodo;
    private String estado;
    private String nombreInstructor;
    private boolean grupoActivo;
    private List<HorarioDto> horarios;

    public static InscripcionResumenDto fabricarDeModelo(InscripcionResumen modelo) {
        InscripcionResumenDto dto = new InscripcionResumenDto();
        dto.setCategoria(modelo.getCategoria());
        dto.setCurso(modelo.getCurso());
        dto.setAnio(modelo.getAnio());
        dto.setIterable(modelo.getIterable());
        dto.setPeriodo(modelo.getPeriodo());
        dto.setEstado(modelo.getEstado());
        dto.setNombreInstructor(modelo.getNombreInstructor());
        dto.setGrupoActivo(modelo.isGrupoActivo());
        dto.setHorarios(modelo.getHorarios().stream()
                .map(HorarioMapper::toDto)
                .collect(Collectors.toList()));
        return dto;
    }
}
