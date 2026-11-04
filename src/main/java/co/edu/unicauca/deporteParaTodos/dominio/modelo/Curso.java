package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Curso {

    private String nombre;

    private String deporte;

    private String categoriaCurso;

    private String descripcion;

    private Integer imagenId;

    private EstadoCurso estadoCurso;

    private EstadoInscripciones estadoInscripciones;

    // A quien va dirigido; ausente = MIXTO (toda la comunidad).
    private PublicoCurso publico;
}
