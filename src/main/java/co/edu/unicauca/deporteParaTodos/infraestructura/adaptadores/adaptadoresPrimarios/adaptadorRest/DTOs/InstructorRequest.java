package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para PUT /api/v2/instructor/{id}.
 * Solo contiene los 2 campos editables del instructor:
 *   - nombre  -> tbl_perfil.perf_nombre (VARCHAR(100))
 *   - correo  -> tbl_perfil.perf_correo (VARCHAR(100))
 *
 * Cualquier otro campo del cuerpo (id, sexo, rol...) se IGNORA: este DTO
 * no los declara, asi que no puede modificar nada mas.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstructorRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String nombre;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El correo debe tener un formato válido")
    @Size(max = 100, message = "El correo no puede superar los 100 caracteres")
    private String correo;
}
