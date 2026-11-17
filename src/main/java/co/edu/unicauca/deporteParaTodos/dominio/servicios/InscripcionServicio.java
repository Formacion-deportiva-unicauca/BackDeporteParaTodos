package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInstructorGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CuposAgotadosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InscripcionFueraDeFechasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InscripcionesCerradasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Disponibilidad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoVentanaInscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionResumen;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.PublicoCursoNoCoincideExcepcion;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;

import java.util.ArrayList;
import java.util.List;

@Service
public class InscripcionServicio implements IInscripcionServicio {

    private static final Logger LOGGER = LoggerFactory.getLogger(InscripcionServicio.class);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final IInscripcionGateway gateway;
    private final ICursoGateway cursoGateway;
    private final IGrupoGateway grupoGateway;
    private final IHorarioGateway horarioGateway;
    private final IInstructorGateway instructorGateway;
    private final IAlumnoGateway alumnoGateway;
    private final AutorizacionGrupoServicio autorizacionGrupo;
    private final CruceHorariosServicio cruceHorarios;
    private final Clock clock;

    public InscripcionServicio(IInscripcionGateway gateway, ICursoGateway cursoGateway, IGrupoGateway grupoGateway,
            IHorarioGateway horarioGateway, IInstructorGateway instructorGateway, IAlumnoGateway alumnoGateway,
            AutorizacionGrupoServicio autorizacionGrupo, CruceHorariosServicio cruceHorarios, Clock clock) {
        this.gateway = gateway;
        this.cursoGateway = cursoGateway;
        this.grupoGateway = grupoGateway;
        this.horarioGateway = horarioGateway;
        this.instructorGateway = instructorGateway;
        this.alumnoGateway = alumnoGateway;
        this.autorizacionGrupo = autorizacionGrupo;
        this.cruceHorarios = cruceHorarios;
        this.clock = clock;
    }

    @Override
    // READ_COMMITTED: bajo el default de MySQL (REPEATABLE READ), el snapshot de la
    // transaccion se fija en la PRIMERA lectura no bloqueante (obtenerCurso, arriba),
    // antes de tomar el lock del grupo. El SELECT ... FOR UPDATE si bloquea de verdad
    // (serializa el acceso a la fila del grupo), pero el conteo de inscritos que viene
    // despues seguia leyendo ese snapshot congelado, no los commits de otros hilos que
    // ya esperaron y pasaron por el mismo lock -- permitiendo sobre-inscripcion real
    // bajo concurrencia real (ver InscripcionConcurrenciaIT). READ_COMMITTED hace que
    // cada SELECT tome su propio snapshot al ejecutarse.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Inscripcion inscribir(Inscripcion datos, String perfIdLlamador, String rolLlamador) {
        Curso curso = cursoGateway.obtenerCurso(datos.getCategoria(), datos.getCurso());
        if (curso == null) {
            throw new InscripcionesCerradasExcepcion(
                    "Las inscripciones para el curso " + datos.getCurso() + " estan cerradas");
        }
        // SCRUM-178: un curso INACTIVO se oculta del catalogo pero nada impedia inscribirse
        // igual llamando directo al endpoint -- confirmado contra MySQL real (InscripcionEstadoCursoIT).
        if (!EstadoCurso.ACTIVO.equals(curso.getEstadoCurso())) {
            throw new InscripcionesCerradasExcepcion(
                    "El curso " + datos.getCurso() + " no esta disponible para inscripciones");
        }

        // Publico del curso: el tipo de vinculo del alumno debe corresponder (los cursos para toda la comunidad no consultan
        // al alumno). Se valida antes de tomar el lock del grupo: no depende de cupos ni de fechas.
        PublicoCurso publico = curso.getPublico();
        if (publico != null && publico != PublicoCurso.MIXTO) {
            String tipoAlumno = alumnoGateway.obtenerAlumno(datos.getAlumnoId()).map(Alumno::getTipoAlumno).orElse(null);
            if (!publico.admite(tipoAlumno)) {
                throw new PublicoCursoNoCoincideExcepcion(publico == PublicoCurso.ESTUDIANTE
                        ? "El curso " + datos.getCurso() + " esta dirigido a estudiantes"
                        : "El curso " + datos.getCurso() + " esta dirigido a docentes, administrativos y contratistas");
            }
        }

        Grupo grupo = grupoGateway.obtenerGrupoConLock(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable());

        // Autorizacion por grupo: el Instructor solo puede inscribir alumnos en
        // el grupo que tiene a cargo. Reutiliza el Grupo ya obtenido con lock
        // arriba -- no se vuelve a consultar.
        autorizacionGrupo.verificarPropietario(grupo, perfIdLlamador, rolLlamador);

        // Inscripcion duplicada: quien ya esta INSCRITO (vigente, sin desvincular) en este grupo no se vuelve a
        // inscribir. Se rechaza ANTES de decidir el camino: no reactiva la fila ni cambia fechaInscripcion, no la
        // pasa a EN_ESPERA si el grupo esta lleno (el alumno ya ocupa un cupo) y no evalua el tope de cursos.
        // Se hace con el lock del grupo tomado, asi dos envios simultaneos no pasan los dos. EN_ESPERA y la
        // reinscripcion tras cancelar (fila desvinculada) conservan su flujo.
        if (gateway.existeInscripcion(datos.getAlumnoId(), datos.getCategoria(),
                datos.getCurso(), datos.getAnio(), datos.getIterable())) {
            Inscripcion previa = gateway.obtenerInscripcion(datos.getAlumnoId(), datos.getCategoria(),
                    datos.getCurso(), datos.getAnio(), datos.getIterable());
            if ("INSCRITO".equals(previa.getEstado()) && previa.getFechaDesvinculacion() == null) {
                throw new YaExisteElementoExcepcion("Ya estás inscrito en este grupo");
            }
            // Lo mismo para quien ya esta EN_ESPERA (vigente, no eliminada): con cupo libre esta rama lo pasaba a
            // INSCRITO saltandose la cola. Su lugar en la lista no se toca: ni estado, ni fechaInscripcion.
            // Si hay cupo y gente esperando, quien decide es la promocion (automatica o manual del Coordinador).
            if (gateway.existeEnEspera(datos.getAlumnoId(), datos.getCategoria(),
                    datos.getCurso(), datos.getAnio(), datos.getIterable())) {
                throw new YaExisteElementoExcepcion("Ya estás en la lista de espera de este grupo");
            }
        }

        // SCRUM-185: ventana de inscripcion del grupo (apertura/cierre), evaluada con
        // "hoy" en America/Bogota -- unica regla, la misma que usa el catalogo
        // (GrupoServicio/GrupoGateway). Solo aplica a inscripciones NUEVAS: no se toca
        // promoverManualmente() ni la promocion automatica desde la lista de espera.
        LocalDate hoy = LocalDate.now(clock);
        // Fase 3: el estado del grupo suma el cierre manual del Coordinador a la ventana de fechas.
        if (grupo.isInscripcionesCerradasManual()) {
            throw new InscripcionesCerradasExcepcion(
                    "Las inscripciones de este grupo estan cerradas");
        }
        EstadoVentanaInscripcion estadoVentana = grupo.estadoInscripcion(hoy);
        if (estadoVentana != EstadoVentanaInscripcion.ABIERTAS) {
            String mensaje = estadoVentana == EstadoVentanaInscripcion.NO_ABIERTAS
                    ? "Las inscripciones de este grupo abren el " + grupo.getFechaInscripcionApertura().format(FORMATO_FECHA)
                    : "Las inscripciones de este grupo cerraron el " + grupo.getFechaIncripcionCierre().format(FORMATO_FECHA);
            throw new InscripcionFueraDeFechasExcepcion(mensaje);
        }

        if (grupo.getCupos() == null) {
            throw new CuposAgotadosExcepcion("El grupo no tiene cupos configurados");
        }

        // Cruce de horarios: el alumno no puede estar inscrito en dos grupos cuyas franjas se solapan. Aplica a los dos caminos
        // (quedar INSCRITO y unirse a la lista) y va despues de los duplicados y de la ventana. Toma el lock del alumno
        // (despues del lock del grupo, ya tomado arriba) para que dos inscripciones simultaneas del mismo alumno no pasen las dos.
        cruceHorarios.validarSinCruce(grupo, datos.getAlumnoId());

        long inscritos = gateway.contarInscripcionesActivasGrupo(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable());

        // Orden de llegada: si hay alguien esperando (EN_ESPERA vigente, sin desvincular ni eliminado), quien llega NUEVO
        // (sin fila vigente: las filas INSCRITO y EN_ESPERA ya se rechazaron arriba; quien salio y vuelve cuenta como
        // nuevo) no entra directo aunque haya cupo: se une a la lista, al final, como si el grupo estuviera lleno. Un cupo
        // libre con cola solo ocurre en casos raros (ampliar cupos con el curso inactivo, o todos los de la cola con cruce);
        // la cola la resuelve la promocion automatica o la manual del Coordinador. Se decide con el lock del grupo tomado y
        // despues del cruce de horarios, asi dos llegadas simultaneas ven la misma cola. Sin cola nada cambia.
        boolean cupoLibre = inscritos < grupo.getCupos();
        long enCola = cupoLibre
                ? gateway.contarEnEsperaGrupo(datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable())
                : 0;
        if (cupoLibre && enCola > 0) {
            LOGGER.warn("cupo libre con cola de espera en el grupo {}/{}/{}/{}: en cola={}; la persona que llega pasa a la lista de espera",
                    datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable(), enCola);
        }

        if (cupoLibre && enCola == 0) {
            // Hay cupos y nadie esperando — camino INSCRITO
            if (gateway.existeInscripcion(datos.getAlumnoId(), datos.getCategoria(),
                    datos.getCurso(), datos.getAnio(), datos.getIterable())) {
                Inscripcion existente = gateway.obtenerInscripcion(
                        datos.getAlumnoId(), datos.getCategoria(),
                        datos.getCurso(), datos.getAnio(), datos.getIterable());
                existente.setFechaDesvinculacion(null);
                // fechaInscripcion = inicio de la activacion VIGENTE, no historial de
                // activaciones pasadas (tbl_inscripcion no guarda ese historial). Antes
                // esta rama era la UNICA de las 4 que reactivan una fila (esta, la rama
                // EN_ESPERA de abajo, y promoverPrimeroEnEspera/promoverInscripcion en
                // InscripcionGateway) que no la refrescaba.
                // Usa el Clock inyectado (America/Bogota), no Instant.now(): esElegibleParaAusencia()
                // en AsistenciaServicio reconvierte este Timestamp con toLocalDateTime(), que usa la
                // zona POR DEFECTO DE LA JVM (UTC en CI) -- con Instant.now() esa conversion puede caer
                // un dia distinto al "hoy" en Bogota usado para fechaClase, cerca de medianoche UTC.
                existente.setFechaInscripcion(Timestamp.valueOf(LocalDateTime.now(clock)));
                existente.setEstado("INSCRITO");
                return gateway.guardarInscripcion(existente);
            }
            datos.setFechaInscripcion(Timestamp.valueOf(LocalDateTime.now(clock)));
            datos.setFechaDesvinculacion(null); // viene del body sin validar; una fila nueva nunca nace desvinculada
            datos.setEstado("INSCRITO");
            return gateway.guardarInscripcion(datos);
        } else {
            // Sin cupos, o con cupo pero con cola — camino EN_ESPERA (al final: fechaInscripcion = ahora)
            if (gateway.existeEnEspera(datos.getAlumnoId(), datos.getCategoria(),
                    datos.getCurso(), datos.getAnio(), datos.getIterable())) {
                throw new YaExisteElementoExcepcion(
                        "El alumno ya está en la lista de espera para este grupo");
            }
            if (gateway.existeInscripcion(datos.getAlumnoId(), datos.getCategoria(),
                    datos.getCurso(), datos.getAnio(), datos.getIterable())) {
                Inscripcion existente = gateway.obtenerInscripcion(
                        datos.getAlumnoId(), datos.getCategoria(),
                        datos.getCurso(), datos.getAnio(), datos.getIterable());
                existente.setFechaDesvinculacion(null);
                existente.setFechaInscripcion(Timestamp.valueOf(LocalDateTime.now(clock)));
                existente.setEstado("EN_ESPERA");
                return gateway.guardarInscripcion(existente);
            }
            datos.setFechaInscripcion(Timestamp.valueOf(LocalDateTime.now(clock)));
            datos.setFechaDesvinculacion(null); // idem: ignorar lo que envie el cliente
            datos.setEstado("EN_ESPERA");
            return gateway.guardarInscripcion(datos);
        }
    }

    @Override
    public boolean validarInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable) {
        return gateway.existeInscripcionActiva(alumnoId, categoria, curso, anio, iterable);
    }

    @Override
    // READ_COMMITTED + lock del grupo ANTES de tocar la fila: la promocion de la lista valida el cruce de horarios de cada candidato
    // con lo ya confirmado, y no puede intercalarse con una inscripcion o promocion concurrente del mismo grupo.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Inscripcion desvincularInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable,
            String perfIdLlamador, String rolLlamador) {
        autorizacionGrupo.verificarPropietario(categoria, curso, anio, iterable, perfIdLlamador, rolLlamador);

        Grupo grupo = grupoGateway.obtenerGrupoConLock(categoria, curso, anio, iterable);

        if (!gateway.existeInscripcionSinDesvincular(alumnoId, categoria, curso, anio, iterable)) {
            throw new NoExisteExcepcion("La inscripcion a desvincular no existe");
        }
        Inscripcion desvinculada = gateway.desvincularInscripcion(alumnoId, categoria, curso, anio, iterable);
        // Solo promover si el desvinculado ocupaba realmente un cupo (INSCRITO).
        // Si estaba EN_ESPERA, no se libero ningun cupo y promover aqui sobre-inscribiria el grupo.
        if ("INSCRITO".equals(desvinculada.getEstado())) {
            cruceHorarios.promoverSiguientes(grupo, 1);
        }
        return desvinculada;
    }

    @Override
    // READ_COMMITTED: mismo problema que inscribir() -- existeEnEspera() (primera
    // lectura, no bloqueante) fija el snapshot REPEATABLE READ antes del lock del
    // grupo, y el conteo de cupos posterior seguia leyendo ese snapshot obsoleto.
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Inscripcion promoverManualmente(String alumnoId, String categoria, String curso, int anio, int iterable) {
        // SCRUM-178: mismo hueco que inscribir() -- promover a un alumno tampoco debia
        // permitirse si el curso esta INACTIVO, aunque haya cupo libre y espera legitima.
        Curso infoCurso = cursoGateway.obtenerCurso(categoria, curso);
        if (infoCurso == null || !EstadoCurso.ACTIVO.equals(infoCurso.getEstadoCurso())) {
            throw new InscripcionesCerradasExcepcion(
                    "El curso " + curso + " no esta disponible para inscripciones");
        }
        if (!gateway.existeEnEspera(alumnoId, categoria, curso, anio, iterable)) {
            throw new NoExisteExcepcion("El alumno no está en la lista de espera para este grupo");
        }
        // Mismo mecanismo de concurrencia que inscribir(): tomar el lock pesimista del
        // grupo ANTES de contar cupos, para que ninguna inscripcion/promocion concurrente
        // pueda colarse entre el conteo y la promocion.
        Grupo grupo = grupoGateway.obtenerGrupoConLock(categoria, curso, anio, iterable);
        if (grupo.getCupos() == null) {
            throw new CuposAgotadosExcepcion("El grupo no tiene cupos configurados");
        }
        long inscritos = gateway.contarInscripcionesActivasGrupo(categoria, curso, anio, iterable);
        if (inscritos >= grupo.getCupos()) {
            throw new CuposAgotadosExcepcion("No hay cupos disponibles para promover al alumno");
        }
        // Mismo cruce que al inscribir: no se promueve a quien quedaria inscrito en dos grupos que se solapan.
        cruceHorarios.validarSinCruce(grupo, alumnoId);
        return gateway.promoverInscripcion(alumnoId, categoria, curso, anio, iterable);
    }

    @Override
    public Disponibilidad obtenerDisponibilidad(String categoria, String curso, int anio, int iterable) {
        Grupo grupo = grupoGateway.obtenerGrupo(categoria, curso, anio, iterable);
        long inscritos = gateway.contarInscripcionesActivasGrupo(categoria, curso, anio, iterable);
        long enEspera = gateway.contarEnEsperaGrupo(categoria, curso, anio, iterable);
        int cuposTotales = grupo.getCupos() != null ? grupo.getCupos() : 0;
        return new Disponibilidad(cuposTotales, (int) Math.max(0, cuposTotales - inscritos), (int) enEspera);
    }

    @Override
    public boolean tieneInscritos(String categoria, String curso, int anio, int iterable) {
        return gateway.contarInscripcionesActivasGrupo(categoria, curso, anio, iterable) > 0;
    }

    @Override
    public List<InscripcionEnEspera> listarEnEspera(String categoria, String curso, int anio, int iterable) {
        return gateway.listarEnEspera(categoria, curso, anio, iterable);
    }

    @Override
    public List<InscripcionResumen> listarMisCursos(String alumnoId) {
        List<Inscripcion> inscripciones = gateway.obtenerInscripcionesAlumno(alumnoId);
        List<InscripcionResumen> resultado = new ArrayList<>();
        for (Inscripcion inscripcion : inscripciones) {
            // obtenerGrupo() es unfiltered por eliminado: un grupo borrado logicamente
            // (DELETE /grupo no desvincula inscritos) sigue resolviendose aqui, nunca
            // lanza NoExisteExcepcion -- por eso la inscripcion no se oculta, solo se
            // marca con grupoActivo=false.
            Grupo grupo = grupoGateway.obtenerGrupo(
                    inscripcion.getCategoria(), inscripcion.getCurso(), inscripcion.getAnio(), inscripcion.getIterable());
            List<Horario> horarios = horarioGateway.listarHorariosPorGrupo(
                    inscripcion.getCategoria(), inscripcion.getCurso(), inscripcion.getAnio(), inscripcion.getIterable());
            String nombreInstructor = null;
            if (grupo.getIdInstructor() != null) {
                nombreInstructor = instructorGateway.obtenerInstructor(grupo.getIdInstructor())
                        .map(Instructor::getPerfil)
                        .map(perfil -> perfil.getNombre())
                        .orElse(null);
            }
            boolean grupoActivo = grupo.getEliminado() == null || grupo.getEliminado() == 0;
            resultado.add(new InscripcionResumen(
                    inscripcion.getCategoria(), inscripcion.getCurso(), inscripcion.getAnio(), inscripcion.getIterable(),
                    grupo.getPeriodo(), inscripcion.getEstado(), nombreInstructor, grupoActivo, horarios,
                    grupo.getFechaFinalizacion()));
        }
        return resultado;
    }
}
