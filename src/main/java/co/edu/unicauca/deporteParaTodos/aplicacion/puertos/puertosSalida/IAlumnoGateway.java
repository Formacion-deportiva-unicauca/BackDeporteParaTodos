package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.List;
import java.util.Optional;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;

public interface IAlumnoGateway {
    /**
     * Toma el lock pesimista de la fila del alumno (dura hasta el fin de la transaccion). Orden de adquisicion en todo el
     * sistema: primero el lock del grupo, despues el del alumno; si hay varios alumnos, en orden ascendente de id.
     * Retorna false si el alumno no existe (no hay nada que bloquear).
     */
    public boolean bloquearAlumno(String alumnoId);

    public boolean existeAlumno(String alumnoId);

    public List<Alumno> obtenerAlumnos();

    public List<Alumno> obtenerAlumnosGrupo(String categoria, String curso, Integer anio, Integer iterable);

    public Alumno insertAlumno(Alumno datosAlumno);

    public Optional<Alumno> obtenerAlumno(String alumnoId);

    public Alumno actualizarAlumno(String alumnoId, Alumno datosAlumno);

    public Alumno eliminarAlumno(String alumnoId);
}
