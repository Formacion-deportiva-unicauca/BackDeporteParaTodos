package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;

import java.util.List;

public interface IInscripcionGateway {

    boolean existeInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    boolean existeInscripcionSinDesvincular(String alumnoId, String categoria, String curso, int anio, int iterable);

    boolean existeInscripcionActiva(String alumnoId, String categoria, String curso, int anio, int iterable);

    boolean existeEnEspera(String alumnoId, String categoria, String curso, int anio, int iterable);

    Inscripcion obtenerInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    Inscripcion guardarInscripcion(Inscripcion inscripcion);

    Inscripcion desvincularInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    long contarInscripcionesActivasGrupo(String categoria, String curso, int anio, int iterable);

    long contarCursosActivosAlumno(String alumnoId);

    long contarEnEsperaGrupo(String categoria, String curso, int anio, int iterable);

    void promoverPrimeroEnEspera(String categoria, String curso, int anio, int iterable);

    Inscripcion promoverInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    List<InscripcionEnEspera> listarEnEspera(String categoria, String curso, int anio, int iterable);

    List<Inscripcion> obtenerInscripcionesAlumno(String alumnoId);
}
