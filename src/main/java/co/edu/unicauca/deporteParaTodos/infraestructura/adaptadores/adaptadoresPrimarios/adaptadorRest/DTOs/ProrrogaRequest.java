package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// SCRUM-80 (rediseno, bloque 1 de 3): cuerpo del PUT /instructores/prorroga-registro.
// Lista vacia -> 400 (via @NotEmpty). ids duplicados: NO es error, se deduplican
// en el servicio (decision documentada en el reporte).
@Getter
@Setter
public class ProrrogaRequest {

    @NotEmpty(message = "{prorroga.idsInstructores.notEmpty}")
    private List<String> idsInstructores;

    @NotNull(message = "{prorroga.anio.notNull}")
    private Integer anio;

    @NotNull(message = "{prorroga.mes.notNull}")
    @Min(value = 1, message = "{prorroga.mes.rango}")
    @Max(value = 12, message = "{prorroga.mes.rango}")
    private Integer mes;

    @NotNull(message = "{prorroga.dias.notNull}")
    @Min(value = 0, message = "{prorroga.dias.rango}")
    @Max(value = 60, message = "{prorroga.dias.rango}")
    private Integer dias;
}
