package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
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
    /** Fecha de finalizacion del grupo (null si no tiene): la misma que usa el cruce de horarios para ignorar grupos terminados. */
    private LocalDate fechaFinalizacion;
}
