package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AvisoRegistroDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.EstadoRegistroDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InstructorDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PlazoCargaDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ProrrogaDto;

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

    // SCRUM-80 (rediseno, bloque 1 de 3): lista las prorrogas de un mes
    // (anio, mes) -- solo los instructores que SI tienen una asignada.
    // mes futuro (relativo al Clock) -> ProrrogaMesFuturoExcepcion (422).
    public List<ProrrogaDto> obtenerProrrogasRegistro(int anio, int mes);

    // Atomica: valida TODOS los ids antes de guardar nada -- si alguno no es
    // instructor activo, NoExisteExcepcion (404) y no se guarda ni se elimina
    // ninguna fila. ids duplicados se deduplican (no es error). dias=0 elimina
    // la prorroga del instructor para ese mes (retorna dias=0/fechaLimite=null
    // en vez de omitirlo). idCoordinador llega como parametro explicito (claim
    // del JWT en el controller).
    public List<ProrrogaDto> asignarProrrogaRegistro(
            List<String> idsInstructores, int anio, int mes, int dias, String idCoordinador);

    // SCRUM-80 (rediseno, bloque 3 de 4): una fila por instructor ACTIVO con el
    // estado del registro de asistencia del mes (anio, mes). anio/mes null ->
    // mes actual del Clock. mes futuro -> MesFuturoExcepcion (422).
    public List<EstadoRegistroDto> obtenerEstadoRegistro(Integer anio, Integer mes);

    // Avisos del MES ACTUAL y del ANTERIOR para un instructor: solo cuando le
    // falta registrar algun grupo activo y el plazo esta por vencer (<=3 dias)
    // o ya vencio. Instructor inexistente/inactivo -> NoExisteExcepcion (404).
    public List<AvisoRegistroDto> obtenerAvisosRegistro(String idInstructor);

}
