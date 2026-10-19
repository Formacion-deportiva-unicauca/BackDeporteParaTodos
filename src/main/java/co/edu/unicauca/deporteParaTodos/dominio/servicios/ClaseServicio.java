package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IClaseServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DuracionManualInvalidaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.JustificacionInvalidaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.CalculadoraDuracionClase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.VentanaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ClaseServicio implements IClaseServicio {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClaseServicio.class);

    private static final int DURACION_MINIMA_MINUTOS = 1;
    private static final int DURACION_MAXIMA_MINUTOS = 12 * 60;
    private static final int JUSTIFICACION_MIN = 10;
    private static final int JUSTIFICACION_MAX = 500;

    @Autowired
    private IClaseGateway claseGateway;

    @Autowired
    private IHorarioGateway horarioGateway;

    @Autowired
    private IProrrogaRegistroAsistenciaGateway prorrogaGateway;

    @Autowired
    private Clock clock;

    @Override
    public List<Clase> obtenerClasesGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        List<Clase> listado = claseGateway.obtenerClasesGrupo(categoria, curso, anio, iterable);
        if (listado.isEmpty()) {
            throw new ListadoVacioExcepcion("No existen registros");
        }
        return listado;
    }

    @Override
    public boolean existeClaseEnFecha(String categoria, String curso, Integer anio, Integer iterable, LocalDate fecha) {
        return claseGateway.existeClaseEnFecha(categoria, curso, anio, iterable, fecha);
    }

    // SCRUM-80 (rediseno, bloque 2): perfIdLlamador/rolLlamador identifican a
    // QUIEN LLAMA (JWT en el controller), no al instructor asignado al grupo
    // -- la ventana de registro se evalua contra quien registra, no contra
    // datoClase.getIdInstructor().
    private LocalDate limiteProrrogaDe(String perfIdLlamador, LocalDate fecha) {
        ProrrogaRegistroAsistencia prorroga = prorrogaGateway
                .obtenerProrrogasPorMes(fecha.getYear(), fecha.getMonthValue())
                .get(perfIdLlamador);
        return prorroga != null ? prorroga.getFechaLimite() : null;
    }

    // Evita la consulta de prorroga cuando ni siquiera hace falta: si la fecha
    // ya es futura, VentanaRegistroAsistencia.validarRegistro() va a rechazar
    // por eso ANTES de mirar el plazo, sin importar el rol. Esta es solo una
    // lectura de optimizacion -- quien decide y lanza sigue siendo
    // VentanaRegistroAsistencia, una sola vez, nunca duplicado aqui.
    private LocalDate limiteProrrogaSiAplica(boolean esExentoDePlazo, String perfIdLlamador, LocalDate fecha, LocalDate hoy) {
        if (esExentoDePlazo || fecha.isAfter(hoy)) {
            return null;
        }
        return limiteProrrogaDe(perfIdLlamador, fecha);
    }

    @Override
    public Clase insertarClase(Clase datoClase, String perfIdLlamador, String rolLlamador) {
        boolean esInstructor = "Instructor".equals(rolLlamador);
        boolean esExentoDePlazo = !esInstructor;
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limiteProrroga = limiteProrrogaSiAplica(esExentoDePlazo, perfIdLlamador, datoClase.getFecha(), hoy);
        VentanaRegistroAsistencia.validarRegistro(datoClase.getFecha(), hoy, esExentoDePlazo, limiteProrroga);

        // SCRUM-80 (correccion del cliente, 15 oct 2026): el Instructor solo
        // puede registrar en dias con horario asignado -- se valida ANTES de
        // mirar si la clase ya existe (una clase fuera de horario creada por
        // un Coordinador no habilita al Instructor a registrar en esa fecha).
        // Coordinador/Administrador no estan sujetos a esta regla. Se
        // aprovecha esta misma consulta para el calculo de duracion de abajo
        // (nunca se vuelve a pedir el horario del grupo una segunda vez).
        List<Horario> franjasDelGrupo = null;
        if (esInstructor) {
            franjasDelGrupo = horarioGateway.listarHorariosPorGrupo(
                    datoClase.getCategoria(), datoClase.getCurso(), datoClase.getAnio(), datoClase.getIterable());
            CalculadoraDuracionClase.validarDiaConHorario(franjasDelGrupo, datoClase.getFecha());
        }

        // El Instructor registra asistencia alumno por alumno, con un click por alumno
        // en momentos distintos durante la misma clase -- cada click repite el flujo
        // completo (crear clase + registrar atencion). Si ya existe una clase de hoy
        // para este grupo, se reutiliza en vez de rechazar o crear una segunda (ver
        // ClaseFechaDuplicadaIT) -- SOLO despues de pasar fecha/plazo/dia arriba, y SIN
        // exigir justificacion de nuevo (ya quedo guardada en el primer click).
        if (claseGateway.existeClaseEnFecha(datoClase.getCategoria(), datoClase.getCurso(),
                datoClase.getAnio(), datoClase.getIterable(), datoClase.getFecha())) {
            return claseGateway.obtenerClaseEnFecha(datoClase.getCategoria(), datoClase.getCurso(),
                    datoClase.getAnio(), datoClase.getIterable(), datoClase.getFecha());
        }

        if (franjasDelGrupo == null) {
            // Coordinador/Administrador: no se valido dia arriba, se pide el horario aqui.
            franjasDelGrupo = horarioGateway.listarHorariosPorGrupo(
                    datoClase.getCategoria(), datoClase.getCurso(), datoClase.getAnio(), datoClase.getIterable());
        }
        String diaSemana = CalculadoraDuracionClase.nombreDia(datoClase.getFecha().getDayOfWeek());
        List<Horario> franjasDelDia = franjasDelGrupo.stream()
                .filter(h -> diaSemana.equalsIgnoreCase(h.getDia()))
                .toList();
        CalculadoraDuracionClase.Resultado duracion = CalculadoraDuracionClase.calcular(franjasDelDia);
        if (!duracion.getIdsFranjasIgnoradas().isEmpty()) {
            LOGGER.warn("franjas de horario invalidas (fin <= inicio) ignoradas para {}/{} el {}: ids={}",
                    datoClase.getCategoria(), datoClase.getCurso(), diaSemana, duracion.getIdsFranjasIgnoradas());
        }

        if (duracion.tieneFranjasValidas()) {
            // Hay horario -- se IGNORAN horas/minutos del cliente.
            datoClase.setHoras(duracion.getMinutos() / 60);
            datoClase.setMinutos(duracion.getMinutos() % 60);
        } else {
            // Sin horario ese dia -- justificacion obligatoria + duracion manual valida.
            validarJustificacion(datoClase.getJustificacion());
            validarDuracionManual(datoClase.getHoras(), datoClase.getMinutos());
        }

        return claseGateway.insertarClase(datoClase);
    }

    // Decision documentada: "sin espacios sobrantes" = el texto no puede tener
    // espacios al inicio/final (trim() debe dejarlo igual). La longitud se
    // valida sobre el texto tal cual llega (ya sin espacios sobrantes).
    private void validarJustificacion(String justificacion) {
        if (justificacion == null) {
            throw new JustificacionInvalidaExcepcion(
                    "El grupo no tiene horario asignado ese dia -- debe indicar una justificacion");
        }
        if (!justificacion.equals(justificacion.trim())) {
            throw new JustificacionInvalidaExcepcion(
                    "La justificacion no puede tener espacios al inicio o al final");
        }
        if (justificacion.length() < JUSTIFICACION_MIN || justificacion.length() > JUSTIFICACION_MAX) {
            throw new JustificacionInvalidaExcepcion(
                    "La justificacion debe tener entre " + JUSTIFICACION_MIN + " y " + JUSTIFICACION_MAX + " caracteres");
        }
    }

    private void validarDuracionManual(Integer horas, Integer minutos) {
        int totalMinutos = (horas != null ? horas : 0) * 60 + (minutos != null ? minutos : 0);
        if (totalMinutos < DURACION_MINIMA_MINUTOS || totalMinutos > DURACION_MAXIMA_MINUTOS) {
            throw new DuracionManualInvalidaExcepcion(
                    "La duracion de la clase debe ser mayor a 0 y menor o igual a 12 horas");
        }
    }

    @Override
    public Clase eliminarClase(int id, String perfIdLlamador, String rolLlamador) {
        if (!claseGateway.existeClase(id)) {
            throw new NoExisteExcepcion();
        }
        Clase actual = claseGateway.obtenerClase(id);

        boolean esExentoDePlazo = !"Instructor".equals(rolLlamador);
        LocalDate hoy = LocalDate.now(clock);
        LocalDate limiteProrroga = limiteProrrogaSiAplica(esExentoDePlazo, perfIdLlamador, actual.getFecha(), hoy);
        VentanaRegistroAsistencia.validarRegistro(actual.getFecha(), hoy, esExentoDePlazo, limiteProrroga);

        return claseGateway.eliminarClase(id);
    }
}
