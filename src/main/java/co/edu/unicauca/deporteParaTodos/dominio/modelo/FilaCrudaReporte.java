package co.edu.unicauca.deporteParaTodos.dominio.modelo;

/** Resultado granular de la base: un conteo por curso, vinculo y genero. sinInscripcion solo aplica a atenciones. */
public record FilaCrudaReporte(String categoria, String curso, String disciplina, String publico, String vinculo,
        String sexo, long total, long sinInscripcion) {
}
