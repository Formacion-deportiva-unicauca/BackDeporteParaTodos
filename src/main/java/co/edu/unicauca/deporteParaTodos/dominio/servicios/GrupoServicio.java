package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;

@Service
public class GrupoServicio implements IGrupoServicio {

    @Autowired
    private IGrupoGateway grupoGateway;

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
        return grupoGateway.actualizarGrupo(categoria, curso, anio, iterable, datosGrupo);
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
}
