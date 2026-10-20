package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstructorDto {

    private String id;

    private String nombre;

    private String correo;

    private String sexo;

    public static InstructorDto fabricarDeModelo(Instructor modelo) {
        InstructorDto dto = new InstructorDto();
        if (modelo.getPerfil() != null) {
            dto.setId(modelo.getPerfil().getId());
            dto.setNombre(modelo.getPerfil().getNombre());
            dto.setCorreo(modelo.getPerfil().getCorreo());
            dto.setSexo(modelo.getPerfil().getSexo());
        }
        return dto;
    }
}
