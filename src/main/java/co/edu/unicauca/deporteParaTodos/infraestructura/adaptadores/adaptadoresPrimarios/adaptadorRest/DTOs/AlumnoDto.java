package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlumnoDto {

    private Integer eliminadoestado;

    private String id;

    private String codigo;

    private String tipo;

    private String nombre;

    private String correo;

    private String sexo;

    private String tipoid;

    private Integer imagen;

    public static AlumnoDto fabricarDeModelo(Alumno modelo) {
        AlumnoDto dto = new AlumnoDto();
        dto.setEliminadoestado(modelo.getEliminadoestado());
        dto.setCodigo(modelo.getAlm_codigo());
        dto.setTipo(modelo.getTipoAlumno());
        if (modelo.getPerfil() != null) {
            dto.setId(modelo.getPerfil().getId());
            dto.setNombre(modelo.getPerfil().getNombre());
            dto.setCorreo(modelo.getPerfil().getCorreo());
            dto.setSexo(modelo.getPerfil().getSexo());
            dto.setTipoid(modelo.getPerfil().getTipoId());
            dto.setImagen(modelo.getPerfil().getImagen());
        }
        return dto;
    }
}
