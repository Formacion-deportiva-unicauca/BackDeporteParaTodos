package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.util.List;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Faltas acumuladas (etapa 4 de 5): cuerpo de PUT /clase/{codigo}/asistencia.
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmacionAsistenciaRequest {

    @Valid
    private List<ItemConfirmacionAsistenciaDto> asistencias;

    private Boolean confirmaTodosAusentes;
}
