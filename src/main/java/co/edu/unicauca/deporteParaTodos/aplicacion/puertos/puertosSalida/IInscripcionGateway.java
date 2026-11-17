package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConteoInscripcionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.HorarioInscrito;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;

import java.time.LocalDate;
import java.util.List;

public interface IInscripcionGateway {

    boolean existeInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    boolean existeInscripcionSinDesvincular(String alumnoId, String categoria, String curso, int anio, int iterable);

    boolean existeInscripcionActiva(String alumnoId, String categoria, String curso, int anio, int iterable);

    boolean existeEnEspera(String alumnoId, String categoria, String curso, int anio, int iterable);

    Inscripcion obtenerInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    Inscripcion guardarInscripcion(Inscripcion inscripcion);

    Inscripcion desvincularInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    /** Desvincula todas las inscripciones activas (INSCRITO y EN_ESPERA) del grupo; retorna cuantas. */
    int desvincularInscripcionesActivasGrupo(String categoria, String curso, int anio, int iterable);

    long contarInscripcionesActivasGrupo(String categoria, String curso, int anio, int iterable);

    long contarEnEsperaGrupo(String categoria, String curso, int anio, int iterable);

    /**
     * Franjas de los grupos donde el alumno esta INSCRITO y vigente, del anio y periodo dados, de grupos no eliminados y no
     * terminados antes de "hoy". Una sola consulta (sin N+1).
     */
    List<HorarioInscrito> listarHorariosInscritosVigentes(String alumnoId, int anio, int periodo, LocalDate hoy);

    Inscripcion promoverInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    List<InscripcionEnEspera> listarEnEspera(String categoria, String curso, int anio, int iterable);

    List<Inscripcion> obtenerInscripcionesAlumno(String alumnoId);

    List<ConteoInscripcionGrupo> contarInscripcionesPorInstructor(String idInstructor);

    /** Fase 4: conteos de inscritos/en espera de todos los grupos (los grupos sin inscripciones no aparecen). */
    List<ConteoInscripcionGrupo> contarInscripcionesTodosLosGrupos();
}
