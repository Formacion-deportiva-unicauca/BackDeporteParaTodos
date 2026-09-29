package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.sql.Timestamp;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CuposAgotadosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InscripcionesCerradasExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.LimiteCursosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Disponibilidad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;

import java.util.List;

@Service
public class InscripcionServicio implements IInscripcionServicio {

    private final IInscripcionGateway gateway;
    private final ICursoGateway cursoGateway;
    private final IGrupoGateway grupoGateway;

    @Value("${inscripciones.limite-cursos-alumno:3}")
    private int limiteCursosAlumno;

    public InscripcionServicio(IInscripcionGateway gateway, ICursoGateway cursoGateway, IGrupoGateway grupoGateway) {
        this.gateway = gateway;
        this.cursoGateway = cursoGateway;
        this.grupoGateway = grupoGateway;
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
    public Inscripcion inscribir(Inscripcion datos) {
        Curso curso = cursoGateway.obtenerCurso(datos.getCategoria(), datos.getCurso());
        if (curso == null || !EstadoInscripciones.ABIERTO.equals(curso.getEstadoInscripciones())) {
            throw new InscripcionesCerradasExcepcion(
                    "Las inscripciones para el curso " + datos.getCurso() + " estan cerradas");
        }
        // SCRUM-178: un curso INACTIVO se oculta del catalogo pero nada impedia inscribirse
        // igual llamando directo al endpoint -- confirmado contra MySQL real (InscripcionEstadoCursoIT).
        if (!EstadoCurso.ACTIVO.equals(curso.getEstadoCurso())) {
            throw new InscripcionesCerradasExcepcion(
                    "El curso " + datos.getCurso() + " no esta disponible para inscripciones");
        }

        Grupo grupo = grupoGateway.obtenerGrupoConLock(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable());
        if (grupo.getCupos() == null) {
            throw new CuposAgotadosExcepcion("El grupo no tiene cupos configurados");
        }

        long inscritos = gateway.contarInscripcionesActivasGrupo(
                datos.getCategoria(), datos.getCurso(), datos.getAnio(), datos.getIterable());

        if (inscritos < grupo.getCupos()) {
            // Hay cupos — camino INSCRITO
            long cursosActivos = gateway.contarCursosActivosAlumno(datos.getAlumnoId());
            if (cursosActivos >= limiteCursosAlumno) {
                throw new LimiteCursosExcepcion(
                        "El alumno ya esta inscrito en el maximo de " + limiteCursosAlumno + " cursos activos");
            }
            if (gateway.existeInscripcion(datos.getAlumnoId(), datos.getCategoria(),
                    datos.getCurso(), datos.getAnio(), datos.getIterable())) {
                Inscripcion existente = gateway.obtenerInscripcion(
                        datos.getAlumnoId(), datos.getCategoria(),
                        datos.getCurso(), datos.getAnio(), datos.getIterable());
                existente.setFechaDesvinculacion(null);
                existente.setEstado("INSCRITO");
                return gateway.guardarInscripcion(existente);
            }
            datos.setFechaInscripcion(Timestamp.from(Instant.now()));
            datos.setFechaDesvinculacion(null); // viene del body sin validar; una fila nueva nunca nace desvinculada
            datos.setEstado("INSCRITO");
            return gateway.guardarInscripcion(datos);
        } else {
            // Sin cupos — camino EN_ESPERA
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
                existente.setFechaInscripcion(Timestamp.from(Instant.now()));
                existente.setEstado("EN_ESPERA");
                return gateway.guardarInscripcion(existente);
            }
            datos.setFechaInscripcion(Timestamp.from(Instant.now()));
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
    @Transactional
    public Inscripcion desvincularInscripcion(String alumnoId, String categoria, String curso, int anio, int iterable) {
        if (!gateway.existeInscripcionSinDesvincular(alumnoId, categoria, curso, anio, iterable)) {
            throw new NoExisteExcepcion("La inscripcion a desvincular no existe");
        }
        Inscripcion desvinculada = gateway.desvincularInscripcion(alumnoId, categoria, curso, anio, iterable);
        // Solo promover si el desvinculado ocupaba realmente un cupo (INSCRITO).
        // Si estaba EN_ESPERA, no se libero ningun cupo y promover aqui sobre-inscribiria el grupo.
        if ("INSCRITO".equals(desvinculada.getEstado())) {
            gateway.promoverPrimeroEnEspera(categoria, curso, anio, iterable);
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
    public List<InscripcionEnEspera> listarEnEspera(String categoria, String curso, int anio, int iterable) {
        return gateway.listarEnEspera(categoria, curso, anio, iterable);
    }
}
