package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.time.LocalDate;
import java.util.List;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConsultaReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.FilaCrudaReporte;

/** Consultas de solo lectura del reporte mensual. */
public interface IReporteGateway {

    /** Inscripciones INSCRITO vigentes en algun momento del mes, por curso, vinculo y genero. */
    List<FilaCrudaReporte> inscritos(ConsultaReporte consulta);

    /** Atenciones (asistencia PRESENTE) del mes, por curso, vinculo y genero, con las que no tienen inscripcion en el grupo. */
    List<FilaCrudaReporte> atenciones(ConsultaReporte consulta);

    long personasDistintasInscritas(ConsultaReporte consulta);

    long personasDistintasAtendidas(ConsultaReporte consulta);

    /** Clases del mes (hasta hoy, no eliminadas, sin novedad) cuya asistencia aun no se confirmo. */
    long clasesSinConfirmar(ConsultaReporte consulta);

    LocalDate primeraFechaConDatos();

    LocalDate ultimaFechaConDatos();

    List<String> disciplinas();

    /** Titulos de categoria sin normalizar (sin las eliminadas ni las ignoradas). */
    List<String> modalidades();

    List<String> facultades();
}
