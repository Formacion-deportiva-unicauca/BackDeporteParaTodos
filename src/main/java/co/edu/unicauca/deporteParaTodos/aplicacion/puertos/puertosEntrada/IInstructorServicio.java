package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

public interface IInstructorServicio {

    public List<InstructorDto> obtenerInstructores();

    public Instructor insertarInstructor(Instructor datosInstructor);

    public InstructorDto obtenerInstructor(String instructorId);

    // SCRUM-79 (HE-14 HU-01): idCoordinador llega como parametro explicito (sale
    // del claim perf_id del JWT en el controller) -- el dominio no depende de
    // Spring Security. Se usa SOLO para el rastro de auditoria del correo.
    public Instructor actualizarInstructor(String instructorId, Instructor datosInstructor, String idCoordinador);

    public Instructor eliminarInstructor(String instructorId);

    public InstructorDto registrarInstructor(PerfilDto perfilDto);

}
