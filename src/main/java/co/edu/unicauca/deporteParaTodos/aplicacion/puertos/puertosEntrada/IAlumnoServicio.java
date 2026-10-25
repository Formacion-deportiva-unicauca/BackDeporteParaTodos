package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AlumnoDto;

public interface IAlumnoServicio {
    public List<Alumno> obtenerAlumnos();

    public List<AlumnoDto> obtenerAlumnosGrupo(String categoria, String curso, Integer anio, Integer iterable);

    public Alumno insertAlumno(Alumno datosAlumno);

    public Alumno obtenerAlumno(String alumnoId);

    // perfIdLlamador/rolLlamador salen del JWT en el controller -- el
    // Instructor solo puede actualizar un alumno con inscripcion activa en un
    // grupo que tiene a cargo, y no puede modificar el correo (ver
    // AlumnoServicio para la decision documentada).
    public Alumno actualizarAlumno(String alumnoId, Alumno datosAlumno, String perfIdLlamador, String rolLlamador);

    public Alumno eliminarAlumno(String alumnoId);

}
