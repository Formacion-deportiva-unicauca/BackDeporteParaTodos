package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.util.List;

/**
 * Parametros ya validados y normalizados de una consulta del reporte. Los filtros nulos significan "sin filtro"; vinculos nunca
 * esta vacio (sin filtro de vinculo trae los cuatro). modalidad va en minuscula y sin espacios sobrantes.
 */
public record ConsultaReporte(LocalDate inicio, LocalDate finExclusivo, LocalDate hoy, String disciplina, String modalidad,
        String publico, List<String> vinculos, String genero, String facultad) {
}
