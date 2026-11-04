package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada;

import java.time.LocalDate;
import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ClaveGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OcupacionGrupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.PublicoCurso;
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

    // Fase 3: cierra (true) o reabre (false) a mano las inscripciones de un grupo.
    public Grupo cambiarCierreManual(String categoria, String curso, Integer anio, Integer iterable, boolean cerrada);
    public Grupo obtenerGrupo(String categoria, String curso, Integer anio, Integer iterable);
    public List<InstructorGrupoResumen> obtenerMisGrupos(String idInstructor);

    // SCRUM-78 (HE-13 HU-02): igual que obtenerMisGrupos(), pero el idInstructor lo
    // manda el cliente (Coordinador consultando a otro instructor) y por eso se
    // valida explicitamente -- lanza NoExisteExcepcion si no existe o esta
    // eliminado logicamente.
    public List<InstructorGrupoResumen> obtenerGruposDeInstructor(String idInstructor);

    // Fase 4: tablero del Coordinador. Los filtros null no filtran.
    public List<OcupacionGrupo> obtenerOcupacion(String categoria, String curso, PublicoCurso publico);

    // Fase 4: aplica un periodo de inscripciones (apertura opcional, cierre obligatorio) a los grupos indicados, todo o nada.
    public List<Grupo> aplicarPeriodo(LocalDate apertura, LocalDate cierre, List<ClaveGrupo> grupos);
}
