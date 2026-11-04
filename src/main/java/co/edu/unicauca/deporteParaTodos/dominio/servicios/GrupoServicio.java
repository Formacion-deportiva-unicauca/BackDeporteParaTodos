package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ClaveGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConteoInscripcionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NivelOcupacion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OcupacionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoVentanaInscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InstructorGrupoResumen;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechasGrupoInvalidasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class GrupoServicio implements IGrupoServicio {

    @Autowired
    private IGrupoGateway grupoGateway;

    @Autowired
    private IInscripcionGateway inscripcionGateway;

    @Autowired
    private ICursoGateway cursoGateway;

    @Autowired
    private IHorarioGateway horarioGateway;

    @Autowired
    private IInstructorGateway instructorGateway;

    @Autowired
    private Clock clock;

    public List<Grupo> obtenerTodosGrupos() {
        return grupoGateway.obtenerTodosGrupos();
    }

    public List<Grupo> obtenerGruposDisponibles() {
        return grupoGateway.obtenerGruposDisponibles();
    }

    public List<Grupo> obtenerGruposDeCurso(String categoria, String curso) {
        return grupoGateway.obtenerGruposDeCurso(categoria, curso);
    }

    public List<Grupo> obtenerGruposInscripcionDisponible() {
        return grupoGateway.obtenerGruposInscripcionDisponible(LocalDate.now(clock));
    }

    public List<Grupo> obtenerGruposInstructor(String idInstructor) {
        return grupoGateway.obtenerGruposInstructor(idInstructor);
    }

    // Fase 3: sin esta regla un grupo sin fecha de cierre quedaba abierto para siempre.
    private static final String MENSAJE_CIERRE_OBLIGATORIO =
            "La fecha de cierre de inscripciones es obligatoria";

    public Grupo insertarGrupo(Grupo datosGrupo) {
        if (datosGrupo != null) {
            validarFechasGrupo(datosGrupo.getFechaCreacion(), datosGrupo.getFechaFinalizacion(),
                    datosGrupo.getFechaInscripcionApertura(), datosGrupo.getFechaIncripcionCierre());
        }
        return grupoGateway.insertarGrupo(datosGrupo);
    }

    public Grupo obtenerGrupoPorId(String categoria, String curso, Integer anio, Integer iterable) {
        Grupo grupo = grupoGateway.obtenerGrupoPorId(categoria, curso, anio, iterable);
        if (grupo == null) {
            throw new NoExisteExcepcion("el objetivo no existe en el sistema");
        }
        return grupo;
    }

    // READ_COMMITTED: igual que inscribir()/promoverManualmente() -- el lock pesimista del grupo se toma antes de contar cupos y la
    // lectura de ocupacion no debe quedar atada a un snapshot anterior al lock.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Grupo actualizarGrupo(String categoria, String curso, Integer anio, Integer iterable, Grupo datosGrupo) {
        if (!grupoGateway.existeGrupo(categoria, curso, anio, iterable)) {
            throw new NoExisteExcepcion("no existe el objetivo a actualizar");
        }
        // La regla se evalua sobre los valores FINALES: lo que trae el body, combinado
        // con lo que ya tenia la fila para los campos que el body no envia (null).
        Grupo actual = grupoGateway.obtenerGrupo(categoria, curso, anio, iterable);
        LocalDate fechaCreacionFinal = datosGrupo.getFechaCreacion() != null
                ? datosGrupo.getFechaCreacion() : actual.getFechaCreacion();
        LocalDate fechaFinalizacionFinal = datosGrupo.getFechaFinalizacion() != null
                ? datosGrupo.getFechaFinalizacion() : actual.getFechaFinalizacion();
        LocalDate fechaAperturaFinal = datosGrupo.getFechaInscripcionApertura() != null
                ? datosGrupo.getFechaInscripcionApertura() : actual.getFechaInscripcionApertura();
        LocalDate fechaCierreFinal = datosGrupo.getFechaIncripcionCierre() != null
                ? datosGrupo.getFechaIncripcionCierre() : actual.getFechaIncripcionCierre();
        validarFechasGrupo(fechaCreacionFinal, fechaFinalizacionFinal, fechaAperturaFinal, fechaCierreFinal);
        Grupo actualizado = grupoGateway.actualizarGrupo(categoria, curso, anio, iterable, datosGrupo);
        promoverListaDeEsperaPorAmpliacionDeCupos(actual.getCupos(), actualizado.getCupos(), categoria, curso, anio, iterable);
        return actualizado;
    }

    // Al AMPLIAR los cupos, los cupos nuevos se ocupan automaticamente con quienes esperan, en el orden de la lista de espera
    // (mismo criterio que al liberarse un cupo por desvinculacion). Nunca se sobrepasa el cupo y, como en la promocion manual,
    // no se promueve si el curso no esta ACTIVO (SCRUM-178).
    private void promoverListaDeEsperaPorAmpliacionDeCupos(Integer cuposAntes, Integer cuposDespues,
            String categoria, String nombreCurso, Integer anio, Integer iterable) {
        if (cuposAntes == null || cuposDespues == null || cuposDespues <= cuposAntes) {
            return;
        }
        Curso infoCurso = cursoGateway.obtenerCurso(categoria, nombreCurso);
        if (infoCurso == null || !EstadoCurso.ACTIVO.equals(infoCurso.getEstadoCurso())) {
            return;
        }
        // Lock pesimista del grupo ANTES de contar: ninguna inscripcion o promocion concurrente puede colarse entre el conteo y la promocion.
        grupoGateway.obtenerGrupoConLock(categoria, nombreCurso, anio, iterable);
        long libres = cuposDespues - inscripcionGateway.contarInscripcionesActivasGrupo(categoria, nombreCurso, anio, iterable);
        long enEspera = inscripcionGateway.contarEnEsperaGrupo(categoria, nombreCurso, anio, iterable);
        for (long i = 0; i < Math.min(libres, enEspera); i++) {
            inscripcionGateway.promoverPrimeroEnEspera(categoria, nombreCurso, anio, iterable);
        }
    }

    // R1: fechaFinalizacion no puede ser anterior a fechaCreacion.
    // R2: fechaIncripcionCierre no puede ser anterior a fechaInscripcionApertura.
    // R3: fechaIncripcionCierre es obligatoria.
    // Si cualquiera de las dos fechas de R1 o R2 es null, esa regla no aplica.
    private void validarFechasGrupo(LocalDate fechaCreacion, LocalDate fechaFinalizacion,
            LocalDate fechaInscripcionApertura, LocalDate fechaIncripcionCierre) {
        if (fechaIncripcionCierre == null) {
            throw new FechasGrupoInvalidasExcepcion(MENSAJE_CIERRE_OBLIGATORIO);
        }
        if (fechaCreacion != null && fechaFinalizacion != null && fechaFinalizacion.isBefore(fechaCreacion)) {
            throw new FechasGrupoInvalidasExcepcion(
                    "La fecha de finalizacion (" + fechaFinalizacion
                            + ") no puede ser anterior a la fecha de creacion (" + fechaCreacion + ")");
        }
        if (fechaInscripcionApertura != null && fechaIncripcionCierre != null
                && fechaIncripcionCierre.isBefore(fechaInscripcionApertura)) {
            throw new FechasGrupoInvalidasExcepcion(
                    "La fecha de cierre de inscripciones (" + fechaIncripcionCierre
                            + ") no puede ser anterior a la fecha de apertura de inscripciones ("
                            + fechaInscripcionApertura + ")");
        }
    }

    // Fase 3: cierre manual de las inscripciones de un grupo. Cerrar siempre se puede; reabrir solo mientras la fecha de
    // cierre no haya pasado (si ya paso, el grupo seguiria cerrado por fecha: hay que ampliar la fecha primero).
    @Override
    public Grupo cambiarCierreManual(String categoria, String curso, Integer anio, Integer iterable, boolean cerrada) {
        if (!grupoGateway.existeGrupo(categoria, curso, anio, iterable)) {
            throw new NoExisteExcepcion("no existe el grupo al que se desea cambiar el estado de inscripciones");
        }
        if (!cerrada) {
            Grupo actual = grupoGateway.obtenerGrupo(categoria, curso, anio, iterable);
            LocalDate cierre = actual.getFechaIncripcionCierre();
            if (cierre != null && LocalDate.now(clock).isAfter(cierre)) {
                throw new FechasGrupoInvalidasExcepcion(
                        "No se pueden reabrir las inscripciones: la fecha de cierre (" + cierre
                                + ") ya paso. Amplie la fecha de cierre del grupo.");
            }
        }
        return grupoGateway.cambiarCierreManual(categoria, curso, anio, iterable, cerrada);
    }

    // Borrar el grupo cancela sus inscripciones activas (INSCRITO y EN_ESPERA) en la misma
    // transaccion: el alumno recupera su cupo de curso y no queda inscrito en un grupo inexistente.
    // No se promueve la lista de espera: el grupo ya no existe.
    @Transactional
    public Grupo eliminarGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        if (!grupoGateway.existeGrupo(categoria, curso, anio, iterable)) {
            throw new NoExisteExcepcion("el objetivo a eliminar no existe");
        }
        if (grupoGateway.existeGrupoEliminado(categoria, curso, anio, iterable)) {
            throw new YaExisteElementoExcepcion("El grupo ya se encuentra eliminado");
        }
        Grupo eliminado = grupoGateway.eliminarGrupo(categoria, curso, anio, iterable);
        inscripcionGateway.desvincularInscripcionesActivasGrupo(categoria, curso, anio, iterable);
        return eliminado;
    }

    @Override
    public Grupo obtenerGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        return grupoGateway.obtenerGrupo(categoria, curso, anio, iterable);
    }

    @Override
    public List<InstructorGrupoResumen> obtenerMisGrupos(String idInstructor) {
        List<Grupo> grupos = grupoGateway.obtenerGruposInstructor(idInstructor);
        if (grupos.isEmpty()) {
            return new ArrayList<>();
        }
        // Un solo query agrupado para los conteos de TODOS los grupos del instructor
        // (no uno por grupo); un grupo sin ninguna inscripcion no aparece aqui, se
        // trata como 0/0 al armar la fila.
        Map<String, ConteoInscripcionGrupo> conteosPorGrupo = new HashMap<>();
        for (ConteoInscripcionGrupo conteo : inscripcionGateway.contarInscripcionesPorInstructor(idInstructor)) {
            conteosPorGrupo.put(claveGrupo(conteo.getCategoria(), conteo.getCurso(), conteo.getAnio(), conteo.getIterable()), conteo);
        }

        LocalDate hoy = LocalDate.now(clock);
        List<InstructorGrupoResumen> resultado = new ArrayList<>();
        for (Grupo grupo : grupos) {
            Curso curso = cursoGateway.obtenerCurso(grupo.getCategoria(), grupo.getCurso());
            List<Horario> horarios = horarioGateway.listarHorariosPorGrupo(
                    grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable());
            ConteoInscripcionGrupo conteo = conteosPorGrupo.get(
                    claveGrupo(grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable()));
            int inscritos = conteo != null ? conteo.getInscritos() : 0;
            int enEspera = conteo != null ? conteo.getEnEspera() : 0;
            resultado.add(new InstructorGrupoResumen(
                    grupo.getCategoria(), grupo.getCurso(), grupo.getAnio(), grupo.getIterable(),
                    grupo.getPeriodo(), grupo.getCupos(),
                    curso != null ? curso.getEstadoCurso() : null,
                    // Fase 3: el estado es el del GRUPO (fechas + cierre manual), no el del curso.
                    grupo.estadoInscripcion(hoy) == EstadoVentanaInscripcion.ABIERTAS
                            ? EstadoInscripciones.ABIERTO : EstadoInscripciones.CERRADO,
                    inscritos, enEspera, horarios));
        }
        return resultado;
    }

    private String claveGrupo(String categoria, String curso, int anio, int iterable) {
        return categoria + "|" + curso + "|" + anio + "|" + iterable;
    }

    @Override
    public List<InstructorGrupoResumen> obtenerGruposDeInstructor(String idInstructor) {
        if (!instructorGateway.existeInstructorActivo(idInstructor)) {
            throw new NoExisteExcepcion("No existe el instructor con el identificador " + idInstructor);
        }
        return obtenerMisGrupos(idInstructor);
    }

    // ── Fase 4: tablero de ocupacion y periodo de inscripciones del Coordinador ──

    // Desde este porcentaje de cupos ocupados un grupo esta "por llenar".
    private static final int PORCENTAJE_POR_LLENAR = 80;

    @Override
    public List<OcupacionGrupo> obtenerOcupacion(String categoria, String curso, PublicoCurso publico) {
        Map<String, PublicoCurso> publicoPorCurso = new HashMap<>();
        for (Curso c : cursoGateway.obtenerCursos()) {
            publicoPorCurso.put(c.getCategoriaCurso() + "|" + c.getNombre(),
                    c.getPublico() != null ? c.getPublico() : PublicoCurso.MIXTO);
        }
        Map<String, ConteoInscripcionGrupo> conteos = new HashMap<>();
        for (ConteoInscripcionGrupo c : inscripcionGateway.contarInscripcionesTodosLosGrupos()) {
            conteos.put(claveGrupo(c.getCategoria(), c.getCurso(), c.getAnio(), c.getIterable()), c);
        }
        LocalDate hoy = LocalDate.now(clock);
        List<OcupacionGrupo> resultado = new ArrayList<>();
        for (Grupo g : grupoGateway.obtenerGruposDisponibles()) {
            PublicoCurso publicoGrupo = publicoPorCurso.getOrDefault(g.getCategoria() + "|" + g.getCurso(), PublicoCurso.MIXTO);
            if ((categoria != null && !categoria.equals(g.getCategoria()))
                    || (curso != null && !curso.equals(g.getCurso()))
                    || (publico != null && publico != publicoGrupo)) {
                continue;
            }
            ConteoInscripcionGrupo conteo = conteos.get(claveGrupo(g.getCategoria(), g.getCurso(), g.getAnio(), g.getIterable()));
            int inscritos = conteo != null ? conteo.getInscritos() : 0;
            int enEspera = conteo != null ? conteo.getEnEspera() : 0;
            resultado.add(new OcupacionGrupo(g.getCategoria(), g.getCurso(), g.getAnio(), g.getIterable(),
                    g.getPeriodo(), publicoGrupo, g.getCupos(), inscritos, enEspera,
                    nivelOcupacion(g.getCupos(), inscritos), g.estadoInscripcion(hoy),
                    g.isInscripcionesCerradasManual(), g.getFechaInscripcionApertura(), g.getFechaIncripcionCierre()));
        }
        return resultado;
    }

    private NivelOcupacion nivelOcupacion(Integer cupos, int inscritos) {
        if (cupos == null) {
            return NivelOcupacion.CON_CUPO;
        }
        if (inscritos >= cupos) {
            return NivelOcupacion.LLENO;
        }
        return inscritos * 100 >= cupos * PORCENTAJE_POR_LLENAR ? NivelOcupacion.POR_LLENAR : NivelOcupacion.CON_CUPO;
    }

    // Todo o nada: primero se valida el periodo y que existan TODOS los grupos, y solo entonces se escribe. No reabre los
    // grupos cerrados a mano (eso se hace con cambiarCierreManual).
    @Override
    @Transactional
    public List<Grupo> aplicarPeriodo(LocalDate apertura, LocalDate cierre, List<ClaveGrupo> grupos) {
        if (cierre == null) {
            throw new FechasGrupoInvalidasExcepcion(MENSAJE_CIERRE_OBLIGATORIO);
        }
        if (apertura != null && cierre.isBefore(apertura)) {
            throw new FechasGrupoInvalidasExcepcion("La fecha de cierre de inscripciones (" + cierre
                    + ") no puede ser anterior a la fecha de apertura de inscripciones (" + apertura + ")");
        }
        if (cierre.isBefore(LocalDate.now(clock))) {
            throw new FechasGrupoInvalidasExcepcion("La fecha de cierre de inscripciones (" + cierre
                    + ") ya paso: elija una fecha de hoy en adelante");
        }
        if (grupos == null || grupos.isEmpty()) {
            throw new FechasGrupoInvalidasExcepcion("Debe seleccionar al menos un grupo");
        }
        for (ClaveGrupo c : grupos) {
            if (!grupoGateway.existeGrupo(c.getCategoria(), c.getCurso(), c.getAnio(), c.getIterable())) {
                throw new NoExisteExcepcion("no existe el grupo " + c.getCategoria() + "/" + c.getCurso() + "/"
                        + c.getAnio() + "/" + c.getIterable());
            }
            if (apertura == null) {
                Grupo actual = grupoGateway.obtenerGrupo(c.getCategoria(), c.getCurso(), c.getAnio(), c.getIterable());
                if (actual != null && actual.getFechaInscripcionApertura() != null
                        && cierre.isBefore(actual.getFechaInscripcionApertura())) {
                    throw new FechasGrupoInvalidasExcepcion("La fecha de cierre (" + cierre
                            + ") es anterior a la apertura actual del grupo " + c.getCurso() + " " + c.getAnio()
                            + "-" + c.getIterable() + " (" + actual.getFechaInscripcionApertura() + ")");
                }
            }
        }
        List<Grupo> actualizados = new ArrayList<>();
        for (ClaveGrupo c : grupos) {
            actualizados.add(grupoGateway.actualizarVentanaInscripcion(
                    c.getCategoria(), c.getCurso(), c.getAnio(), c.getIterable(), apertura, cierre));
        }
        return actualizados;
    }
}
