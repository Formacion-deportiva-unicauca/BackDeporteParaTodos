package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Faltas acumuladas (etapa 4 de 5): item de PUT /clase/{codigo}/asistencia.
// Sin idClase (a diferencia de AtencionDto) -- la clase sale del path, no del
// cuerpo. estaAtendido es opcional: ausente si no viene (ver regla 2 del ticket).
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ItemConfirmacionAsistenciaDto {

    @NotBlank(message = "El id del perfil no puede estar en blanco")
    private String idPerfil;

    private Boolean estaAtendido;
}
