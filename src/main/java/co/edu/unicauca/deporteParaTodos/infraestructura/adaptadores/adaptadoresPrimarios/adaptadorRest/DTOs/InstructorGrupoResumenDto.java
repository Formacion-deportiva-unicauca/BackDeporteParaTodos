package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InstructorGrupoResumen;
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
public class InstructorGrupoResumenDto {

    private String categoria;
    private String curso;
    private int anio;
    private int iterable;
    private int periodo;
    private Integer cupos;
    private EstadoCurso estadoCurso;
    private EstadoInscripciones estadoInscripciones;
    private int inscritos;
    private int enEspera;
    private List<HorarioDto> horarios;

    public static InstructorGrupoResumenDto fabricarDeModelo(InstructorGrupoResumen modelo) {
        InstructorGrupoResumenDto dto = new InstructorGrupoResumenDto();
        dto.setCategoria(modelo.getCategoria());
        dto.setCurso(modelo.getCurso());
        dto.setAnio(modelo.getAnio());
        dto.setIterable(modelo.getIterable());
        dto.setPeriodo(modelo.getPeriodo());
        dto.setCupos(modelo.getCupos());
        dto.setEstadoCurso(modelo.getEstadoCurso());
        dto.setEstadoInscripciones(modelo.getEstadoInscripciones());
        dto.setInscritos(modelo.getInscritos());
        dto.setEnEspera(modelo.getEnEspera());
        dto.setHorarios(modelo.getHorarios().stream()
                .map(HorarioMapper::toDto)
                .collect(Collectors.toList()));
        return dto;
    }
}
