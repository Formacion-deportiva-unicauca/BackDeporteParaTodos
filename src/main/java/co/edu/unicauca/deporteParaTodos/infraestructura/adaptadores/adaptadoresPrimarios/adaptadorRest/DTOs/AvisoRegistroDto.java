package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoAvisoRegistro;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// SCRUM-80 (rediseno, bloque 3 de 4): un aviso de GET /instructor/avisos-registro.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AvisoRegistroDto {

    private int anio;
    private int mes;
    private String estado;
    private long diasRestantes;
    private String fechaLimite;

    // diasRestantes negativo = ya vencido (cuantos dias pasaron del limite).
    public static AvisoRegistroDto fabricarDeModelo(int anio, int mes, EstadoAvisoRegistro estado,
            LocalDate fechaLimite, LocalDate hoy) {
        AvisoRegistroDto dto = new AvisoRegistroDto();
        dto.setAnio(anio);
        dto.setMes(mes);
        dto.setEstado(estado != null ? estado.name() : null);
        dto.setFechaLimite(fechaLimite != null ? fechaLimite.toString() : null);
        dto.setDiasRestantes(ChronoUnit.DAYS.between(hoy, fechaLimite));
        return dto;
    }
}
