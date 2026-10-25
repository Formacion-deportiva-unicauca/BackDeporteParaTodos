package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Disponibilidad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionResumen;

import java.util.List;

public interface IInscripcionServicio {

    // perfIdLlamador/rolLlamador salen del JWT en el controller -- autorizacion
    // por grupo (AutorizacionGrupoServicio): el Instructor solo puede
    // inscribir/desvincular alumnos en el grupo que tiene a cargo.
    Inscripcion inscribir(Inscripcion datos, String perfIdLlamador, String rolLlamador);

    boolean validarInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable);

    Inscripcion desvincularInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable,
            String perfIdLlamador, String rolLlamador);

    Inscripcion promoverManualmente(String alumnoId, String categoria, String curso, int anio, int iterable);

    Disponibilidad obtenerDisponibilidad(String categoria, String curso, int anio, int iterable);

    boolean tieneInscritos(String categoria, String curso, int anio, int iterable);

    List<InscripcionEnEspera> listarEnEspera(String categoria, String curso, int anio, int iterable);

    List<InscripcionResumen> listarMisCursos(String alumnoId);
}
