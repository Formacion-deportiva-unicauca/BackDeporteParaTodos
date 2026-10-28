package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.ResultadoConfirmacionAsistencia;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Faltas acumuladas (etapa 4 de 5): respuesta 200 de PUT /clase/{codigo}/asistencia.
@Getter
@Setter
public class ConfirmacionAsistenciaDto {
    private Integer codigoClase;
    private LocalDateTime confirmadaEn;
    private int presentes;
    private int ausentes;

    public static ConfirmacionAsistenciaDto fabricarDeModelo(ResultadoConfirmacionAsistencia modelo) {
        ConfirmacionAsistenciaDto dto = new ConfirmacionAsistenciaDto();
        dto.setCodigoClase(modelo.getCodigoClase());
        dto.setConfirmadaEn(modelo.getConfirmadaEn());
        dto.setPresentes(modelo.getPresentes());
        dto.setAusentes(modelo.getAusentes());
        return dto;
    }
}
