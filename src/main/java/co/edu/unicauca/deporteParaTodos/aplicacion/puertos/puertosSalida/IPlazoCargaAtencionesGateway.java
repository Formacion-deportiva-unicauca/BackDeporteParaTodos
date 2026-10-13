package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.Map;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.PlazoCargaAtenciones;

public interface IPlazoCargaAtencionesGateway {

    // Crea el plazo del periodo (idInstructor, anio, periodo) si no existe, o lo
    // reemplaza (fechaLimite, idCoordinador, fechaAsignacion) si ya existia -- upsert.
    PlazoCargaAtenciones upsertPlazo(PlazoCargaAtenciones datos);

    // UNA sola consulta para todos los plazos del periodo (anio, periodo), indexada
    // por idInstructor -- evita N+1 al construir el listado de GET /plazos-carga.
    // Un instructor sin plazo asignado simplemente no aparece en el mapa.
    Map<String, PlazoCargaAtenciones> obtenerPlazosPorPeriodo(int anio, int periodo);

    // Numero de clases DISTINTAS con al menos una asistencia activa, agrupado por
    // el instructor del GRUPO (tbl_grupo.PERF_ID), para el periodo (anio, periodo)
    // dado. Un instructor sin ninguna clase con asistencia simplemente no aparece
    // en el mapa (el llamador debe tratar la ausencia como 0).
    Map<String, Integer> contarClasesConAsistenciaPorInstructor(int anio, int periodo);
}
