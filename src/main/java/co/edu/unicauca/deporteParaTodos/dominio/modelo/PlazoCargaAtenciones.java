package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SCRUM-80 (HE-14 HU-02): plazo de carga de atenciones que el Coordinador
// asigna a un instructor para el periodo (anio, periodo) vigente.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlazoCargaAtenciones {
    private String idInstructor;
    private Integer anio;
    private Integer periodo;
    private LocalDate fechaLimite;
    private String idCoordinador;
    private LocalDateTime fechaAsignacion;
    private Integer eliminado;
}
