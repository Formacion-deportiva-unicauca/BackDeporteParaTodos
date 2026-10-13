package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.ICursoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Curso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoCurso;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.EstadoInscripciones;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ErrorInternoException;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.InsercionFallidaExepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class CursoServicio implements ICursoServicio {

    @Autowired
    private ICursoGateway cursoGateway;

    @Override
    public List<Curso> recuperarCursos() {
        return cursoGateway.obtenerCursos();
    }

    @Override
    public List<Curso> cursosDeCategoria(String categoria) {
        return cursoGateway.obtenerCursosDeCategoria(categoria);
    }

    @Override
    public List<Curso> todosLosCursosDeCategoria(String categoria) {
        return cursoGateway.obtenerTodosCursosDeCategoria(categoria);
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
        return respuesta;
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
        return respuesta;
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
        return actualizado;
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
        return cursoGateway.cambiarEstadoCurso(categoria, nombreCurso, estado);
    }

    @Override
    public Curso cambiarEstadoInscripciones(String categoria, String nombreCurso, EstadoInscripciones estado) {
        if (!cursoGateway.existeCurso(categoria, nombreCurso)) {
            throw new NoExisteExcepcion("El curso al que se desea cambiar el estado de inscripciones no existe");
        }
        return cursoGateway.cambiarEstadoInscripciones(categoria, nombreCurso, estado);
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
