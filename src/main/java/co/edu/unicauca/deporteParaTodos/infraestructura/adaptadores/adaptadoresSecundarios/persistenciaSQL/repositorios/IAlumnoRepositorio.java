package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AlumnoEntidad;

public interface IAlumnoRepositorio extends CrudRepository<AlumnoEntidad,String>{

     @Query(value = """
        SELECT 
            alm.meta_eliminado AS eliminadoestado,
            alm.perf_id AS id,
            alm.alm_codigo AS codigo,
            alm.alm_tipo AS tipo,
            perf.perf_nombre AS nombre,
            perf.perf_correo AS correo,
            perf.perf_sexo AS sexo,
            perf.perf_tipoid AS tipoid,
            perf.perf_imagen AS imagen
        FROM tbl_alumno alm
        INNER JOIN tbl_inscripcion ins ON alm.perf_id = ins.perf_id
        INNER JOIN tbl_perfil perf ON perf.perf_id = alm.perf_id
        WHERE alm.meta_eliminado = :eliminado
          AND ins.cat_titulo = :categoria
          AND ins.cur_nombre = :curso
          AND ins.grp_anio = :anio
          AND ins.grp_iterable = :iterable
          AND ins.meta_eliminado = 0
          AND ins.inscr_estado = 'INSCRITO'
          AND ins.inscr_fechadesvinculacion IS NULL
        """, nativeQuery = true)
    List<Object[]> buscarAlumnosGrupoRaw(
        @Param("categoria") String categoria,
        @Param("curso") String curso,
        @Param("anio") Integer anio,
        @Param("iterable") Integer iterable,
        @Param("eliminado") Integer eliminado
    );

    @Query(value = """
        SELECT 
            alm.meta_eliminado AS eliminadoestado,
            alm.perf_id AS id,
            alm.alm_codigo AS codigo,
            alm.alm_tipo AS tipo,
            perf.perf_nombre AS nombre,
            perf.perf_correo AS correo,
            perf.perf_sexo AS sexo,
            perf.perf_tipoid AS tipoid,
            perf.perf_imagen AS imagen
        FROM tbl_alumno alm
        INNER JOIN tbl_perfil perf ON perf.perf_id = alm.perf_id
        WHERE alm.perf_id = :alumnoId
        """, nativeQuery = true)
    Object[] buscarAlumnoPorIdRaw(@Param("alumnoId") String alumnoId);

    @Query(value = """
        SELECT fac.fac_nombre
        FROM tbl_alumno alm
        INNER JOIN tbl_intermedia_alumno_programa intermedia
            ON intermedia.perf_id = alm.perf_id
        INNER JOIN tbl_programa programa
            ON programa.prg_nombre = intermedia.prg_nombre
        INNER JOIN tbl_facultad fac
            ON fac.fac_nombre = programa.fac_nombre
        WHERE alm.perf_id = :perfilId
        LIMIT 1
        """, nativeQuery = true)
    String obtenerFacultadPorPerfilId(@Param("perfilId") String perfilId);

    @Query(value = """
        SELECT intermedia.prg_nombre
        FROM tbl_intermedia_alumno_programa intermedia
        WHERE intermedia.perf_id = :perfilId
          AND intermedia.meta_eliminado = 0
        LIMIT 1
        """, nativeQuery = true)
    String obtenerProgramaPorPerfilId(@Param("perfilId") String perfilId);

    @Transactional
    @Modifying(flushAutomatically = true)
    @Query(value = """
        INSERT INTO tbl_intermedia_alumno_programa (META_ELIMINADO, PERF_ID, PRG_NOMBRE)
        VALUES (0, :perfilId, :programa)
        """, nativeQuery = true)
    int insertarAlumnoPrograma(@Param("perfilId") String perfilId, @Param("programa") String programa);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE tbl_alumno
        SET alm_tipo = :tipo
        WHERE perf_id = :alumnoId
        """, nativeQuery = true)
    int actualizarTipoAlumno(@Param("alumnoId") String alumnoId, @Param("tipo") String tipo);

    @Transactional
    @Modifying
    @Query(value = """
        UPDATE tbl_alumno
        SET meta_eliminado = 1
        WHERE perf_id = :alumnoId
        """, nativeQuery = true)
    int eliminarAlumnoLogico(@Param("alumnoId") String alumnoId);
}
