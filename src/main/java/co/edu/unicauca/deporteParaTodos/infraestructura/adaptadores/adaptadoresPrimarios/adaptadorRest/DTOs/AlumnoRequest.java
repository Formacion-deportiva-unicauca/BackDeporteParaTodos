package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO de entrada para PUT /api/v2/alumnos/{id}.
 * Solo contiene los 3 campos editables del alumno:
 *   - nombre  → tbl_perfil.perf_nombre
 *   - correo  → tbl_perfil.perf_correo
 *   - tipoAlumno → tbl_alumno.alm_tipo
 *
 * Campos NO editables por este endpoint:
 *   perf_id, alm_codigo, perf_sexo, perf_tipoid,
 *   alm_estado, perf_imagen.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlumnoRequest {

    @NotBlank(message = "El nombre no puede estar vacío")
    private String nombre;

    @NotBlank(message = "El correo no puede estar vacío")
    @Email(message = "El correo debe tener un formato válido")
    private String correo;

    @NotBlank(message = "El tipo de alumno no puede estar vacío")
    @Pattern(
        regexp = "Estudiante|Docente|Administrativo|Contratista",
        message = "El tipo de alumno debe ser: Estudiante, Docente, Administrativo o Contratista"
    )
    private String tipoAlumno;
}
