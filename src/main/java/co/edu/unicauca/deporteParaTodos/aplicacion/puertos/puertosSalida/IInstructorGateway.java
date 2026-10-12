package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.List;
import java.util.Optional;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;

public interface IInstructorGateway {
    public boolean existeInstructor(String instructorId);

    // SCRUM-78 (HE-13 HU-02): true solo si el instructor existe Y no esta eliminado
    // logicamente -- distingue "no existe" de "existe pero eliminado" en una sola
    // consulta (mismo criterio que ya usan GrupoGateway.insertarGrupo/actualizarGrupo
    // para validar el instructor asignado a un grupo).
    public boolean existeInstructorActivo(String instructorId);

    public List<Instructor> obtenerInstructores();

    public Instructor insertarInstructor(Instructor datosInstructor);

    public Optional<Instructor> obtenerInstructor(String instructorId);

    public Instructor eliminarInstructor(String instructorId);

    public Instructor registrarInstructor(Perfil perfil, String tipoAlumno);
}
