package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SCRUM-80 (rediseno, bloque 1 de 3): prorroga que el Coordinador asigna a un
// instructor para un mes (anio, mes) especifico. fechaLimite ya viene
// PRECALCULADA (VentanaRegistroAsistencia.calcularLimiteProrroga) al momento
// de asignarla.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProrrogaRegistroAsistencia {
    private String idInstructor;
    private Integer anio;
    private Integer mes;
    private Integer dias;
    private LocalDate fechaLimite;
    private String idCoordinador;
    private LocalDateTime fechaAsignacion;
    private Integer eliminado;
}
