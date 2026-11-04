package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class PerfilDto {
    @NotBlank(message = "{perfil.id.blank}")
    private String id;

    @NotBlank(message = "{perfil.nombre.blank}")
    private String nombre;

    @NotBlank(message = "{perfil.correo.blank}")
    @Email(message = "{perfil.correo.email}")
    private String correo;

    @NotBlank(message = "{perfil.tipoid.blank}")
    private String tipoId;

    @NotBlank(message = "{perfil.sexo.blank}")
    private String sexo;

    private String role;

    //Este dato no existe para el modelo, solo para intercambiar con el front
    private String facultad;
    //Este dato no exite para el modelo, solo para intercambiar con el front
    @NotBlank(message = "{perfil.tipoalumno.blank}")
    private String tipoAlumno;

    private String alumnoCodigo;

    private String programa;

    private String dependencia;

}
