package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.Map;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;

public interface IProrrogaRegistroAsistenciaGateway {

    // UNA sola consulta para todas las prorrogas del mes (anio, mes), indexada
    // por idInstructor -- evita N+1 en el listado de GET /prorroga-registro.
    Map<String, ProrrogaRegistroAsistencia> obtenerProrrogasPorMes(int anio, int mes);

    // Crea la prorroga de (idInstructor, anio, mes) si no existe, o la
    // REEMPLAZA (dias, fechaLimite, idCoordinador, fechaAsignacion) si ya
    // existia -- upsert.
    ProrrogaRegistroAsistencia upsertProrroga(ProrrogaRegistroAsistencia datos);

    // dias=0 elimina la prorroga -- borrado FISICO (es configuracion, no
    // historial). No hace nada si no existia ninguna fila.
    void eliminarProrroga(String idInstructor, int anio, int mes);
}
