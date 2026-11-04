package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Identificador natural de un grupo (categoria, curso, anio, iterable). */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ClaveGrupo {

    private String categoria;
    private String curso;
    private int anio;
    private int iterable;
}
