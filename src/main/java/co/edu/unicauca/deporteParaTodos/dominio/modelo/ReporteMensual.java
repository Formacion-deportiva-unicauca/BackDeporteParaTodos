package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.util.List;

/**
 * Reporte mensual de estadisticas (solo cifras agregadas, sin datos personales).
 *
 * Atencion = una asistencia PRESENTE de una persona a una sesion (clase) del mes. Inscritos = inscripciones INSCRITO (sin lista de
 * espera) vigentes en algun momento del mes. Las personas distintas NO son aditivas: se calculan con una consulta aparte.
 */
public record ReporteMensual(int anio, int mes, Filtros filtros, List<Fila> filas, Totales totales,
        List<PorVinculo> porVinculo, Calidad calidad) {

    /** Filtros aplicados (ya normalizados). */
    public record Filtros(String disciplina, String modalidad, String publico, List<String> vinculos, String genero,
            String facultad, boolean agruparFuncionarios) {
    }

    /** Una fila por curso (con su modalidad ya normalizada). */
    public record Fila(String modalidad, String curso, String disciplina, String publico,
            long inscritosHombres, long inscritosMujeres, long inscritosTotal,
            long atencionesHombres, long atencionesMujeres, long atencionesTotal) {
    }

    /** Totales: la suma exacta de las filas, mas las personas distintas (consulta separada). */
    public record Totales(long inscritosHombres, long inscritosMujeres, long inscritosTotal,
            long atencionesHombres, long atencionesMujeres, long atencionesTotal,
            long personasDistintasInscritas, long personasDistintasAtendidas) {
    }

    /** Distribucion por vinculo (o "Funcionarios" cuando se agrupan Docente, Administrativo y Contratista). */
    public record PorVinculo(String vinculo, long inscritos, long atenciones) {
    }

    /** Calidad de datos del mes: clases sin confirmar y atenciones de personas sin inscripcion INSCRITO en ese grupo. */
    public record Calidad(long clasesSinConfirmar, long atencionesSinInscripcion) {
    }
}
