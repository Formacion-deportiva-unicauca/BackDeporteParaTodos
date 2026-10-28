package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AsistenciaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.AsistenciaId;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;


public interface IAsistenciaRepositorio extends CrudRepository<AsistenciaEntidad,AsistenciaId>{
    List<AsistenciaEntidad> findByClaseCodigo(Integer claseCodigo);
    Boolean existsByClaseCodigoAndPerfilId(Integer claseCodigo, String perfilId);

    /**
     * Faltas acumuladas (etapa 5 de 5): UNA consulta agregada (GROUP BY
     * perf_id) para todo el grupo -- sin N+1. No filtra por inscripcion
     * actual: un alumno desvinculado conserva sus faltas aqui (solo deja de
     * aparecer en la lista de alumnosGrupo, que las combina por fuera).
     * Cuenta AUSENTE activa, de una clase activa y YA CONFIRMADA, no futura,
     * y sin una novedad activa ese mismo grupo+fecha (la novedad excluye la
     * falta, no bloquea la confirmacion -- ver AsistenciaServicio).
     */
    @Query(value = """
        SELECT asis.perf_id, COUNT(*) AS faltas
        FROM tbl_asistencia asis
        INNER JOIN tbl_clase cl ON asis.cls_codigo = cl.cls_codigo
        WHERE cl.cat_titulo = :categoria
          AND cl.cur_nombre = :curso
          AND cl.grp_anio = :anio
          AND cl.grp_iterable = :iterable
          AND cl.meta_eliminado = 0
          AND asis.meta_eliminado = 0
          AND asis.asi_estado = 'AUSENTE'
          AND cl.cls_asistencia_confirmada_en IS NOT NULL
          AND cl.cls_fecha <= :hoy
          AND NOT EXISTS (
              SELECT 1 FROM tbl_novedad_clase nov
              WHERE nov.cat_titulo = cl.cat_titulo
                AND nov.cur_nombre = cl.cur_nombre
                AND nov.grp_anio = cl.grp_anio
                AND nov.grp_iterable = cl.grp_iterable
                AND nov.nov_fecha = cl.cls_fecha
                AND nov.meta_eliminado = 0
          )
        GROUP BY asis.perf_id
        """, nativeQuery = true)
    List<Object[]> contarFaltasPorGrupo(
            @Param("categoria") String categoria,
            @Param("curso") String curso,
            @Param("anio") Integer anio,
            @Param("iterable") Integer iterable,
            @Param("hoy") LocalDate hoy);

    /**
     * Retorna las estadisticas entre dos fechas para las categorias existentes que posean cursos
     * @param fechaInicio
     * @param fechaFin
     * @return retorna un objeto que posee CAT_TITULO, CLASES, HORAS, MINUTOS, DURACION_TOTAL_MINUTOS
     */
    @Query(value = """
        select cc.cat_titulo, 
            coalesce( count(cl.cat_titulo), 0) as clases,
            coalesce (sum(cl.cls_duracion_horas),0) as horas,
            coalesce (sum(cl.cls_duracion_minutos), 0) as minutos,
            (coalesce( sum(cl.cls_duracion_horas),0)*60)
                +coalesce(sum(cl.cls_duracion_minutos),0) as duracion_total_minutos
            from tbl_categoria_curso cc
            right join tbl_curso c on c.cat_titulo=cc.cat_titulo
            right join tbl_grupo g on c.cat_titulo = g.cat_titulo and c.cur_nombre = g.cur_nombre
            left join tbl_clase cl on g.cat_titulo = cl.cat_titulo
                                        and g.cur_nombre = cl.cur_nombre
                                        and g.grp_anio = cl.grp_anio
                                        and g.grp_iterable = cl.grp_iterable
                                        and cl.cls_fecha between :fechaInicio and :fechaFin
                                        and cl.meta_eliminado = 0
            where (:categoria is null or c.cat_titulo = :categoria)
            group by cc.cat_titulo
            """,nativeQuery = true)
    List<Object[]> estadisticasCategorias(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("categoria") String categoria);
    /***
     * Obtiene las estadisticas de todos los cursos entre 2 fechas
     * @param fechaInicio
     * @param fechaFin
     * @return lista de objetos con los campos: CAT_TITULO, CUR_NOMBRE, CLASES, HORAS, MINUTOS, DURACION_TOTAL_MINUTOS
     */
    @Query(value = """
        select 
            c.cat_titulo,
            c.cur_nombre,
            coalesce( count(cl.cat_titulo), 0) as clases,
            coalesce (sum(cl.cls_duracion_horas),0) as horas,
            coalesce (sum(cl.cls_duracion_minutos), 0) as minutos,
            (coalesce( sum(cl.cls_duracion_horas),0)*60)
                +coalesce(sum(cl.cls_duracion_minutos),0) as duracion_total_minutos
            from tbl_curso c
            right join tbl_grupo g on c.cat_titulo = g.cat_titulo and c.cur_nombre = g.cur_nombre
            left join tbl_clase cl on g.cat_titulo = cl.cat_titulo
                                        and g.cur_nombre = cl.cur_nombre
                                        and g.grp_anio = cl.grp_anio
                                        and g.grp_iterable = cl.grp_iterable
                                        and cl.cls_fecha between :fechaInicio and :fechaFin
                                        and cl.meta_eliminado = 0
            where
                (:categoria is null or c.cat_titulo = :categoria) and
                (:curso is null or c.cur_nombre = :curso)
            group by c.cat_titulo, c.cur_nombre
            """,nativeQuery = true)
    List<Object[]> estadisticasCursos(
            @Param("fechaInicio") LocalDate fechaInicio,
            @Param("fechaFin") LocalDate fechaFin,
            @Param("categoria") String categoria,   // ← agregar @Param
            @Param("curso") String curso);

    /**
     * Retorna las estadisticas de los grupos entre 2 fechas
     * @param fechaInicio requerido
     * @param fechaFin requerido
     * @param categoria nullable
     * @param curso nullable
     * @param anio nullable
     * @param iterable nullable
     * @return retorna un objeto que posee los campos CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE, CLASES, HORAS, MINUTOS, DURACION_TOTAL_MINUTOS
     */
    @Query(value = """
        select 
            g.cat_titulo,
            g.cur_nombre,
            g.grp_anio,
            g.grp_iterable,
            coalesce( count(cl.cat_titulo), 0) as clases,
            coalesce (sum(cl.cls_duracion_horas),0) as horas,
            coalesce (sum(cl.cls_duracion_minutos), 0) as minutos,
            (coalesce( sum(cl.cls_duracion_horas),0)*60)
                +coalesce(sum(cl.cls_duracion_minutos),0) as duracion_total_minutos
            from tbl_grupo g
            left join tbl_clase cl on g.cat_titulo = cl.cat_titulo
                                        and g.cur_nombre = cl.cur_nombre
                                        and g.grp_anio = cl.grp_anio
                                        and g.grp_iterable = cl.grp_iterable
                                        and cl.cls_fecha between :fechaInicio and :fechaFin
                                        and cl.meta_eliminado = 0
            where
                (:categoria is null or g.cat_titulo = :categoria) and 
                (:curso is null or g.cur_nombre = :curso) and 
                (:anio is null or g.grp_anio = :anio) and 
                (:iterable is null or g.grp_iterable = :iterable)
            group by g.cat_titulo,
            g.cur_nombre,
            g.grp_anio,
            g.grp_iterable
            """,nativeQuery = true)
    List<Object[]>  estadisticasGrupos(
        @Param("fechaInicio") LocalDate fechaInicio, 
        @Param("fechaFin") LocalDate fechaFin, 
        @Param("categoria") String categoria, 
        @Param("curso") String curso, 
        @Param("anio") Integer anio, 
        @Param("iterable") Integer iterable);

    /**
     * Retorna el numero de asistencias y sumatoria de horas de un alumno entre 2 fechas
     * @param fechaInicio
     * @param fechaFin
     * @param alumno identificador del alumno
     * @return
     */
    @Query(value = """
        SELECT  
            p.perf_nombre,
            asis.perf_id,
            COALESCE(COUNT(*), 0) AS clases,
            COALESCE(SUM(cl.cls_duracion_horas), 0) AS horas,
            COALESCE(SUM(cl.cls_duracion_minutos), 0) AS minutos,
            (COALESCE(SUM(cl.cls_duracion_horas), 0) * 60) + COALESCE(SUM(cl.cls_duracion_minutos), 0) AS duracion_total_minutos
        FROM  
            tbl_asistencia asis
            INNER JOIN tbl_clase cl ON asis.cls_codigo = cl.cls_codigo
            INNER JOIN tbl_perfil p ON asis.perf_id = p.perf_id
        WHERE
            (asis.perf_id = :alumno OR :alumno IS NULL)
            AND cl.cls_fecha BETWEEN :fechaInicio AND :fechaFin
            AND asis.meta_eliminado = 0
            AND cl.meta_eliminado = 0
            AND asis.asi_estado = 'PRESENTE'
        GROUP BY
            asis.perf_id, p.perf_nombre""", 
        nativeQuery = true)
    List<Object[]> estadisticasAlumno(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin, @Param("alumno") String alumno);

    /**
     * Estadistcas para instructor entre 2 fechas
     * @param fechaInicio
     * @param fechaFin
     * @param instructor si es null, obtendra de todos los instructores
     * @return
     */
    @Query(value = """
            select 
            p.perf_nombre,
            cl.perf_id,
            coalesce( count(*), 0) as clases,
            coalesce (sum(cl.cls_duracion_horas),0) as horas,
            coalesce (sum(cl.cls_duracion_minutos), 0) as minutos,
            (coalesce( sum(cl.cls_duracion_horas),0)*60)
                +coalesce(sum(cl.cls_duracion_minutos),0) as duracion_total_minutos
        from 
            tbl_clase cl inner join tbl_perfil p on cl.perf_id = p.perf_id
        where
            (cl.perf_id = :instructor or :instructor is null) and
            cl.cls_fecha BETWEEN :fechaInicio
                            AND :fechaFin and
            cl.meta_eliminado = 0
        group by p.perf_nombre, cl.perf_id
            """, nativeQuery = true)
            List<Object[]> estadisticasInstructor(@Param("fechaInicio") LocalDate fechaInicio, @Param("fechaFin") LocalDate fechaFin, @Param("instructor") String instructor);
}
