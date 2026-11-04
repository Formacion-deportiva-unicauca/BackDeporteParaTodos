package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Perfil {

    private String id;

    private String nombre;

    private String correo;

    private Integer imagen;

    private String tipoId;

    private String sexo;

    private String rol;

    private String facultad;

    private String tipoAlumno;

    private String alumnoCodigo;

    // Estudiante y Docente: programa academico (la facultad se deriva de el).
    private String programa;

    // Administrativo y Contratista: dependencia, oficina o unidad donde trabajan.
    private String dependencia;
}
