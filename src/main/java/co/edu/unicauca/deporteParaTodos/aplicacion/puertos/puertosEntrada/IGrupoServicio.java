package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InstructorGrupoResumen;

public interface IGrupoServicio {
    public List<Grupo> obtenerTodosGrupos();
    public List<Grupo> obtenerGruposDisponibles();
    public List<Grupo> obtenerGruposDeCurso(String categoria, String curso);
    public List<Grupo> obtenerGruposInscripcionDisponible();
    public List<Grupo> obtenerGruposInstructor(String idInstructor);
    public Grupo insertarGrupo(Grupo datosGrupo);
    public Grupo obtenerGrupoPorId(String categoria, String curso, Integer anio, Integer iterable);
    public Grupo actualizarGrupo(String categoria, String curso, Integer anio, Integer iterable, Grupo datosGrupo);
    public Grupo eliminarGrupo(String categoria, String curso, Integer anio, Integer iterable);
    public Grupo obtenerGrupo(String categoria, String curso, Integer anio, Integer iterable);
    public List<InstructorGrupoResumen> obtenerMisGrupos(String idInstructor);
}
