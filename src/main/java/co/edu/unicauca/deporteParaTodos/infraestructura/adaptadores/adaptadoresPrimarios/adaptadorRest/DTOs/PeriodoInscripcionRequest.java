package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Fase 4: cuerpo del PATCH /grupo/periodo. La apertura es opcional (null conserva la de cada grupo); el cierre es obligatorio.
@Getter
@Setter
public class PeriodoInscripcionRequest {

    private LocalDate fechaApertura;

    @NotNull(message = "La fecha de cierre de inscripciones es obligatoria")
    private LocalDate fechaCierre;

    @NotEmpty(message = "Debe seleccionar al menos un grupo")
    @Valid
    private List<GrupoClaveRequest> grupos;

    @Getter
    @Setter
    public static class GrupoClaveRequest {
        @NotNull private String categoria;
        @NotNull private String curso;
        @NotNull private Integer anio;
        @NotNull private Integer iterable;
    }
}
