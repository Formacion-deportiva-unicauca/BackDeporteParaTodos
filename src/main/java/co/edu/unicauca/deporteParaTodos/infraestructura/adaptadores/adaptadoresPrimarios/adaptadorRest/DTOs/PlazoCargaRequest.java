package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// SCRUM-80 (HE-14 HU-02): cuerpo del PUT /instructor/{id}/plazo-carga. Unico
// campo del contrato -- cualquier otro campo del body lo ignora Jackson.
@Getter
@Setter
public class PlazoCargaRequest {

    @NotNull(message = "{plazoCarga.fechaLimite.null}")
    private LocalDate fechaLimite;
}
