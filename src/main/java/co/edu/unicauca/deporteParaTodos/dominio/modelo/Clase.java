package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Clase {

    private Integer codigo;

    private String categoria;

    private String curso;

    private Integer anio;

    private Integer iterable;

    private String idInstructor;

    private LocalDate fecha;

    private Integer horas;

    private Integer minutos;

    private String observacion;

    private Integer eliminado;
}
