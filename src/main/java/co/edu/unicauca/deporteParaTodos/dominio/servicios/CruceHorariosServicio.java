package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CruceHorarioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CruceHorarios;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CruceHorarios.Cruce;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CruceHorarios.Franja;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.HorarioInscrito;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import jakarta.annotation.PostConstruct;

/**
 * Un alumno puede inscribirse en todos los cursos que desee siempre que sus horarios NO se crucen (sin tope de cursos).
 *
 * Concurrencia: el lock del grupo no basta (dos inscripciones del mismo alumno en grupos distintos toman locks distintos y
 * ninguna ve a la otra). Por eso, antes de leer las franjas del alumno se toma el lock de su fila. ORDEN DE ADQUISICION en
 * todo el sistema (inscribir, promocion manual, desvincular, ampliacion de cupos): primero el lock del grupo, despues el(los)
 * lock(s) de alumno, y si son varios, en orden ascendente de id. Asi ninguna transaccion espera un grupo teniendo un alumno
 * y no puede formarse un ciclo. Quien llama debe llevar una transaccion READ_COMMITTED (cada lectura ve lo ya confirmado).
 *
 * El interruptor inscripciones.validar-cruce-horarios (variable de entorno INSCRIPCIONES_VALIDAR_CRUCE_HORARIOS) lo apaga: sin
 * validacion ni lock de alumno, las promociones vuelven a tomar a la cola en orden.
 */
@Service
public class CruceHorariosServicio {

    private static final Logger LOGGER = LoggerFactory.getLogger(CruceHorariosServicio.class);

    private final IInscripcionGateway inscripcionGateway;
    private final IHorarioGateway horarioGateway;
    private final IAlumnoGateway alumnoGateway;
    private final Clock clock;
    private final boolean validarCruce;

    public CruceHorariosServicio(IInscripcionGateway inscripcionGateway, IHorarioGateway horarioGateway,
            IAlumnoGateway alumnoGateway, Clock clock,
            @Value("${inscripciones.validar-cruce-horarios:true}") boolean validarCruce) {
        this.inscripcionGateway = inscripcionGateway;
        this.horarioGateway = horarioGateway;
        this.alumnoGateway = alumnoGateway;
        this.clock = clock;
        this.validarCruce = validarCruce;
    }

    @PostConstruct
    void avisarSiEstaApagado() {
        if (!validarCruce) {
            LOGGER.warn("inscripciones.validar-cruce-horarios=false: la validacion de cruce de horarios y el lock por alumno "
                    + "estan DESACTIVADOS; un alumno puede inscribirse en grupos que se cruzan.");
        }
    }

    /**
     * Rechaza (CruceHorarioExcepcion, 422) si el horario del grupo se cruza con el de otro grupo donde el alumno esta INSCRITO.
     * Debe llamarse con el lock del grupo ya tomado y dentro de una transaccion READ_COMMITTED.
     */
    public void validarSinCruce(Grupo destino, String alumnoId) {
        if (!validarCruce) {
            return;
        }
        List<Franja> franjasDestino = franjasDestino(destino);
        if (franjasDestino.isEmpty()) {
            return; // sin franjas validas no puede haber cruce (ni carrera que serializar)
        }
        alumnoGateway.bloquearAlumno(alumnoId);
        Optional<Cruce> cruce = buscarCruce(destino, franjasDestino, alumnoId);
        if (cruce.isPresent()) {
            throw new CruceHorarioExcepcion(cruce.get().detalle());
        }
    }

    /**
     * Promueve de la lista de espera a lo sumo "cantidad" alumnos, en orden de la cola, saltando a quien tendria un cruce (se
     * queda en espera). Si ninguno es promovible, no promueve a nadie: el cupo queda libre. Debe llamarse con el lock del grupo
     * tomado y en READ_COMMITTED. Retorna cuantos se promovieron.
     *
     * Orden de locks: se eligen los candidatos sin cruce SIN bloquear, se bloquean sus filas de alumno en orden ascendente de
     * id (el mismo orden global en todas las transacciones, aunque las colas de dos grupos esten en orden distinto), y se
     * revalida cada uno ya bloqueado antes de promoverlo. Solo se promueve dentro del conjunto bloqueado.
     */
    public int promoverSiguientes(Grupo grupo, int cantidad) {
        if (cantidad <= 0) {
            return 0;
        }
        List<InscripcionEnEspera> cola = inscripcionGateway.listarEnEspera(
                grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable());
        if (cola.isEmpty()) {
            return 0;
        }
        List<Franja> franjasDestino = validarCruce ? franjasDestino(grupo) : List.of();
        if (franjasDestino.isEmpty()) {
            return promoverEnOrden(grupo, cola, cantidad); // sin validacion posible: orden de la cola, como siempre
        }

        List<InscripcionEnEspera> elegibles = new ArrayList<>();
        for (InscripcionEnEspera candidato : cola) {
            if (buscarCruce(grupo, franjasDestino, candidato.getAlumnoId()).isPresent()) {
                LOGGER.info("Promocion omitida por cruce de horario: alumno={}, grupo={}/{}/{}/{}", candidato.getAlumnoId(),
                        grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable());
            } else {
                elegibles.add(candidato);
            }
        }
        elegibles.stream().map(InscripcionEnEspera::getAlumnoId).sorted(Comparator.naturalOrder())
                .forEach(alumnoGateway::bloquearAlumno);

        int promovidos = 0;
        for (InscripcionEnEspera candidato : elegibles) {
            if (promovidos >= cantidad) {
                break;
            }
            if (buscarCruce(grupo, franjasDestino, candidato.getAlumnoId()).isPresent()) {
                LOGGER.info("Promocion omitida por cruce de horario (confirmado con el alumno bloqueado): alumno={}, grupo={}/{}/{}/{}",
                        candidato.getAlumnoId(), grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable());
                continue;
            }
            inscripcionGateway.promoverInscripcion(candidato.getAlumnoId(), grupo.getCategoria(), grupo.getCurso(),
                    grupo.getAnio(), grupo.getIterable());
            promovidos++;
        }
        if (promovidos == 0) {
            LOGGER.info("Ningun alumno de la lista de espera es promovible sin cruce de horario; el cupo queda libre: grupo={}/{}/{}/{}",
                    grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable());
        }
        return promovidos;
    }

    private int promoverEnOrden(Grupo grupo, List<InscripcionEnEspera> cola, int cantidad) {
        int promovidos = 0;
        for (InscripcionEnEspera candidato : cola) {
            if (promovidos >= cantidad) {
                break;
            }
            inscripcionGateway.promoverInscripcion(candidato.getAlumnoId(), grupo.getCategoria(), grupo.getCurso(),
                    grupo.getAnio(), grupo.getIterable());
            promovidos++;
        }
        return promovidos;
    }

    private Optional<Cruce> buscarCruce(Grupo destino, List<Franja> franjasDestino, String alumnoId) {
        List<HorarioInscrito> inscritos = inscripcionGateway.listarHorariosInscritosVigentes(
                alumnoId, destino.getAnio(), destino.getPeriodo(), LocalDate.now(clock));
        for (HorarioInscrito h : inscritos) {
            if (CruceHorarios.normalizar(h.dia(), h.horaInicio(), h.horaFin()).isEmpty()) {
                LOGGER.warn("Franja de horario invalida ignorada en la validacion de cruce: horarioId={}", h.horarioId());
            }
        }
        return CruceHorarios.primerCruce(destino, franjasDestino, inscritos);
    }

    private List<Franja> franjasDestino(Grupo destino) {
        List<Horario> horarios = horarioGateway.listarHorariosPorGrupo(
                destino.getCategoria(), destino.getCurso(), destino.getAnio(), destino.getIterable());
        for (Horario h : horarios) {
            if (CruceHorarios.normalizar(h.getDia(), h.getHoraInicio(), h.getHoraFin()).isEmpty()) {
                LOGGER.warn("Franja de horario invalida ignorada en la validacion de cruce: horarioId={}", h.getId());
            }
        }
        return CruceHorarios.franjasValidas(horarios);
    }
}
