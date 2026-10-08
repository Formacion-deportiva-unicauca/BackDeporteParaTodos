package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConteoInscripcionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
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
        return grupoGateway.obtenerGruposInscripcionDisponible();
    }

    public List<Grupo> obtenerGruposInstructor(String idInstructor) {
        return grupoGateway.obtenerGruposInstructor(idInstructor);
    }

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
        return grupoGateway.actualizarGrupo(categoria, curso, anio, iterable, datosGrupo);
    }

    // R1: fechaFinalizacion no puede ser anterior a fechaCreacion.
    // R2: fechaIncripcionCierre no puede ser anterior a fechaInscripcionApertura.
    // Si cualquiera de las dos fechas de una regla es null, esa regla no aplica.
    private void validarFechasGrupo(LocalDate fechaCreacion, LocalDate fechaFinalizacion,
            LocalDate fechaInscripcionApertura, LocalDate fechaIncripcionCierre) {
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

    public Grupo eliminarGrupo(String categoria, String curso, Integer anio, Integer iterable) {
        if (!grupoGateway.existeGrupo(categoria, curso, anio, iterable)) {
            throw new NoExisteExcepcion("el objetivo a eliminar no existe");
        }
        if (grupoGateway.existeGrupoEliminado(categoria, curso, anio, iterable)) {
            throw new YaExisteElementoExcepcion("El grupo ya se encuentra eliminado");
        }
        return grupoGateway.eliminarGrupo(categoria, curso, anio, iterable);
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
                    curso != null ? curso.getEstadoInscripciones() : null,
                    inscritos, enEspera, horarios));
        }
        return resultado;
    }

    private String claveGrupo(String categoria, String curso, int anio, int iterable) {
        return categoria + "|" + curso + "|" + anio + "|" + iterable;
    }
}
