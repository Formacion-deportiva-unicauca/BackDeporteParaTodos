package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoVentanaInscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ErrorInternoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InsercionFallidaExepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class CursoServicio implements ICursoServicio {

    @Autowired
    private ICursoGateway cursoGateway;

    @Autowired
    private IAlumnoGateway alumnoGateway;

    @Autowired
    private IGrupoGateway grupoGateway;

    @Autowired
    private Clock clock;

    @Override
    public List<Curso> recuperarCursos() {
        return conEstadoInscripciones(cursoGateway.obtenerCursos());
    }

    @Override
    public List<Curso> cursosDeCategoria(String categoria) {
        return conEstadoInscripciones(cursoGateway.obtenerCursosDeCategoria(categoria));
    }

    @Override
    public List<Curso> todosLosCursosDeCategoria(String categoria) {
        return conEstadoInscripciones(cursoGateway.obtenerTodosCursosDeCategoria(categoria));
    }

    @Override
    public List<Curso> cursosDeCategoriaParaAlumno(String categoria, String alumnoId) {
        return visiblesParaAlumno(cursoGateway.obtenerCursosDeCategoria(categoria), alumnoId);
    }

    @Override
    public List<Curso> recuperarCursosParaAlumno(String alumnoId) {
        return visiblesParaAlumno(cursoGateway.obtenerCursos(), alumnoId);
    }

    // El alumno ve un curso si esta activo, va dirigido a su tipo de vinculo y algun grupo suyo tiene hoy las
    // inscripciones abiertas por fecha (misma regla EstadoVentanaInscripcion que usa inscribir()).
    private List<Curso> visiblesParaAlumno(List<Curso> cursos, String alumnoId) {
        String tipo = alumnoId == null ? null
                : alumnoGateway.obtenerAlumno(alumnoId).map(Alumno::getTipoAlumno).orElse(null);
        LocalDate hoy = LocalDate.now(clock);
        return cursos.stream()
                .filter(curso -> EstadoCurso.ACTIVO.equals(curso.getEstadoCurso()))
                .filter(curso -> Optional.ofNullable(curso.getPublico()).orElse(PublicoCurso.MIXTO).admite(tipo))
                .filter(curso -> tieneGrupoConInscripcionesAbiertas(curso, hoy))
                .map(curso -> conEstadoInscripciones(curso, hoy))
                .toList();
    }

    private boolean tieneGrupoConInscripcionesAbiertas(Curso curso, LocalDate hoy) {
        return estadoInscripcionesDe(curso, hoy) == EstadoInscripciones.ABIERTO;
    }

    // Fase 3: el estado de inscripciones del curso no se guarda ni se cambia a mano: se calcula con los grupos del curso
    // (misma regla que inscribir(): ventana de fechas + cierre manual). ABIERTO si algun grupo acepta inscripciones hoy,
    // CERRADO si ninguno, SIN_GRUPOS si aun no tiene grupos.
    private EstadoInscripciones estadoInscripcionesDe(Curso curso, LocalDate hoy) {
        List<Grupo> grupos = grupoGateway.obtenerGruposDeCurso(curso.getCategoriaCurso(), curso.getNombre());
        if (grupos == null || grupos.isEmpty()) {
            return EstadoInscripciones.SIN_GRUPOS;
        }
        return grupos.stream().anyMatch(grupo -> grupo.estadoInscripcion(hoy) == EstadoVentanaInscripcion.ABIERTAS)
                ? EstadoInscripciones.ABIERTO
                : EstadoInscripciones.CERRADO;
    }

    private Curso conEstadoInscripciones(Curso curso, LocalDate hoy) {
        if (curso != null) {
            curso.setEstadoInscripciones(estadoInscripcionesDe(curso, hoy));
        }
        return curso;
    }

    private Curso conEstadoInscripciones(Curso curso) {
        return curso == null ? null : conEstadoInscripciones(curso, LocalDate.now(clock));
    }

    private List<Curso> conEstadoInscripciones(List<Curso> cursos) {
        if (cursos == null || cursos.isEmpty()) {
            return cursos;
        }
        LocalDate hoy = LocalDate.now(clock);
        cursos.forEach(curso -> conEstadoInscripciones(curso, hoy));
        return cursos;
    }

    @Override
    public Curso obtenerCurso(String titulo, String nombre) {
        if (!cursoGateway.existeCurso(titulo, nombre)) {
            throw new NoExisteExcepcion();
        }
        Curso respuesta = cursoGateway.obtenerCurso(titulo, nombre);
        if (respuesta == null) {
            throw new ErrorInternoException();
        }
        return conEstadoInscripciones(respuesta);
    }

    @Override
    public Curso insertarCurso(Curso datosCurso) {
        if (cursoGateway.existeCurso(datosCurso.getCategoriaCurso(), datosCurso.getNombre())) {
            throw new YaExisteElementoExcepcion("el curso notado con categoria " + datosCurso.getCategoriaCurso() + " y nombre " + datosCurso.getNombre() + " ya se encuentra en el sistema");
        }
        Curso respuesta = cursoGateway.insertarCurso(datosCurso);
        if (respuesta == null) {
            throw new InsercionFallidaExepcion("Error en la insercion o conversion de retorno fallida, se ha respondido con nulo");
        }
        return conEstadoInscripciones(respuesta);
    }

    @Override
    public Curso actualizarCurso(String categoria, String curso, Curso datCurso) {
        if (!cursoGateway.existeCurso(categoria, curso)) {
            throw new NoExisteExcepcion("el curso notado no existe en el sistema");
        }
        Curso actualizado = cursoGateway.actualizarCurso(categoria, curso, datCurso);
        if (actualizado == null) {
            throw new InsercionFallidaExepcion("Error en la insercion o conversion de retorno fallida, se ha respondido con nulo");
        }
        return conEstadoInscripciones(actualizado);
    }

    @Override
    public Curso eliminarCurso(String categoria, String curso) {
        if (!cursoGateway.existeCurso(categoria, curso)) {
            throw new NoExisteExcepcion("El curso a eliminar no existe");
        }
        return cursoGateway.eliminarCurso(categoria, curso);
    }

    @Override
    public Curso cambiarEstadoCurso(String categoria, String nombreCurso, EstadoCurso estado) {
        if (!cursoGateway.existeCurso(categoria, nombreCurso)) {
            throw new NoExisteExcepcion("El curso al que se desea cambiar el estado no existe");
        }
        return conEstadoInscripciones(cursoGateway.cambiarEstadoCurso(categoria, nombreCurso, estado));
    }

    @Override
    public Curso eliminarCursoPermanente(String categoria, String nombreCurso) {
        if (!cursoGateway.existeCurso(categoria, nombreCurso)) {
            throw new NoExisteExcepcion("El curso a eliminar no existe");
        }
        // SonarCloud javabugs:S2259: existeCurso() y obtenerCurso() son 2 consultas
        // separadas -- si el curso desaparece justo entre ambas (TOCTOU), obtenerCurso()
        // retorna null y el contrato del metodo no garantiza lo contrario.
        Curso actual = cursoGateway.obtenerCurso(categoria, nombreCurso);
        if (actual == null) {
            throw new NoExisteExcepcion("El curso a eliminar no existe");
        }
        if (EstadoCurso.INACTIVO.equals(actual.getEstadoCurso())) {
            throw new YaExisteElementoExcepcion("El curso ya se encuentra eliminado");
        }
        return cursoGateway.eliminarCursoPermanente(categoria, nombreCurso);
    }
}
