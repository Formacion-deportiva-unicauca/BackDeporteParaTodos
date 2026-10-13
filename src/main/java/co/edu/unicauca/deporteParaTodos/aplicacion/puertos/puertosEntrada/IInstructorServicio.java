package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PlazoCargaDto;

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

    // SCRUM-80 (HE-14 HU-02): un elemento por instructor ACTIVO para el periodo
    // vigente (anio/periodo calculados con el Clock inyectado).
    public List<PlazoCargaDto> obtenerPlazosCarga();

    // idCoordinador llega como parametro explicito (sale del claim perf_id del
    // JWT en el controller), igual que actualizarInstructor(). El periodo lo
    // calcula el servidor con el Clock -- el cliente solo manda fechaLimite.
    public PlazoCargaDto asignarPlazoCarga(String instructorId, LocalDate fechaLimite, String idCoordinador);

}
