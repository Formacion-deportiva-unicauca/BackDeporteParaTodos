package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Faltas acumuladas (etapa 4 de 5): resultado de PUT /clase/{codigo}/asistencia.
@Getter
@AllArgsConstructor
public class ResultadoConfirmacionAsistencia {
    private final Integer codigoClase;
    private final LocalDateTime confirmadaEn;
    private final int presentes;
    private final int ausentes;
}
