package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class InstructorGrupoResumen {

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
    private List<Horario> horarios;
}
