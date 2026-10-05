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
public class InscripcionResumen {

    private String categoria;
    private String curso;
    private int anio;
    private int iterable;
    private int periodo;
    private String estado;
    private String nombreInstructor;
    private boolean grupoActivo;
    private List<Horario> horarios;
}
