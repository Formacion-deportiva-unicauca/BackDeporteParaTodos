package co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida;

import java.util.List;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;

public interface IGrupoGateway {
    
    /**
     * verifica la existencia de un curso en el sistema identificado con
     * @param categoria identificador de la categoria
     * @param curso identificador del curso
     * @param anio anio para identificar el grupo
     * @param secuencial secuencial para identificar el grupo
     * @return
     */
    public boolean existeGrupo(String categoria, String curso, Integer anio, Integer secuencial);
    
    /**
     * Obtiene todos los grupos del sistema, marcados como eliminados o no eliminados
     * @return listados de grupos
     */
    public List<Grupo> obtenerTodosGrupos();

    /***
     * Obtiene todos los grupos del sistema marcados como no eliminados
     * @return listados de grupos
     */
    public List<Grupo> obtenerGruposDisponibles();

    /****
     * Obtiene los grupos asociados a curso que esten disponibles
     * @param categoria identificador de la categoria
     * @param curso identificador del curso
     * @return listado de grupos
     */
    public List<Grupo> obtenerGruposDeCurso(String categoria, String curso);

    public List<Grupo> obtenerGruposInscripcionDisponible();

    /***
     * Obtener los grupos asociados a un instructor
     * @return listado de grupos
     */
    public List<Grupo> obtenerGruposInstructor(String idInstructor);

    /**
     * Agrega un grupo al sistema, el identificador de la imagen debe existir
     * @param datosGrupo datos del grupo
     * @return grupo insertado
     */
    public Grupo insertarGrupo(Grupo datosGrupo);

    /***
     * Obtiene un grupo identificado con
     * @param categoria identificador de la categoria
     * @param curso identificador del curso
     * @param anio año para identificar grupo
     * @param iterable secuencial para identificar grupo
     * @return grupo obtenido
     */
    public Grupo obtenerGrupoPorId(String categoria, String curso, Integer anio, Integer iterable);

    /***
     * Obtiene la informacion de un grupo identificado con
     * @param categoria identificador de la categoria
     * @param curso identificador del curso
     * @param anio anio para identificar el grupo
     * @param iterable secuencial para identificar el grupo
     * @param datosGrupo datos del grupo
     * @return grupo actualizado
     */
    public Grupo actualizarGrupo(String categoria, String curso, Integer anio, Integer iterable, Grupo datosGrupo);

    /***
     * Marca como elimnado un grupo en el sistema, identficado con
     * @param categoria identificador de la categoria,
     * @param curso identificador del curso
     * @param anio año para identificar el grupo
     * @param iterable secuencial para identificar el grupo
     * @return grupo marcado
     */
    public Grupo eliminarGrupo(String categoria, String curso, Integer anio, Integer iterable);

    /***
     * Confirma si el grupo identificado con los parametros dados ya esta marcado
     * como eliminado (META_ELIMINADO=1). Si el grupo no existe, retorna false --
     * ese caso lo cubre existeGrupo().
     */
    public boolean existeGrupoEliminado(String categoria, String curso, Integer anio, Integer iterable);


    /***
     * Recupera un curso del sistema identificado con los parametros
     * @param categoria
     * @param curso
     * @param anio
     * @param iterable
     * @return curso encontrado
     */
    public Grupo obtenerGrupo(String categoria, String curso, Integer anio, Integer iterable);

    /***
     * Obtiene un grupo con bloqueo pesimista (SELECT FOR UPDATE) para control de concurrencia en inscripciones
     */
    public Grupo obtenerGrupoConLock(String categoria, String curso, Integer anio, Integer iterable);
}