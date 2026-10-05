package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Grupo {

    private String categoria;

    private String curso;

    private int anio;

    private int iterable;

    private Integer imagenGrupo;

    private Integer cupos;

    private String idInstructor;

    private LocalDate fechaCreacion;

    private LocalDate fechaFinalizacion;

    private LocalDate fechaInscripcionApertura;

    private LocalDate fechaIncripcionCierre;

    private int periodo;

    private Integer eliminado;
}
