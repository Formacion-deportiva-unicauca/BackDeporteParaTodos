package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ConteoInscripcionGrupo {

    private String categoria;
    private String curso;
    private int anio;
    private int iterable;
    private int inscritos;
    private int enEspera;
}
