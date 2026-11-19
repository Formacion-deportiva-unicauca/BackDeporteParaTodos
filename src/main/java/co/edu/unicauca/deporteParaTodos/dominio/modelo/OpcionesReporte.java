package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.util.List;

/** Valores para los filtros del reporte y primer/ultimo mes con datos (formato yyyy-MM; null si no hay datos). */
public record OpcionesReporte(String primerMes, String ultimoMes, List<String> disciplinas, List<String> modalidades,
        List<String> publicos, List<String> vinculos, List<String> facultades) {
}
