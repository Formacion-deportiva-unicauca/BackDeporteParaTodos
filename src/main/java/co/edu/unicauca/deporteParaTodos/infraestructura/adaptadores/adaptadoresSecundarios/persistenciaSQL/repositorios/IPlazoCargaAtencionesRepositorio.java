package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PlazoCargaAtencionesEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.PlazoCargaAtencionesId;

public interface IPlazoCargaAtencionesRepositorio
        extends CrudRepository<PlazoCargaAtencionesEntidad, PlazoCargaAtencionesId> {

    List<PlazoCargaAtencionesEntidad> findByAnioAndPeriodoAndEliminado(Integer anio, Integer periodo, Integer eliminado);

    /**
     * Numero de clases DISTINTAS con al menos una asistencia activa, agrupado por
     * el instructor del GRUPO (tbl_grupo.PERF_ID) -- NUNCA tbl_clase.PERF_ID, que
     * es un dato suelto que manda el cliente en cada clase y puede no coincidir
     * con el instructor real del grupo. Filtra META_ELIMINADO = 0 en asistencia,
     * clase y grupo; usa el anio/periodo del PROPIO grupo (GRP_ANIO/GRP_PERIODO)
     * contra el periodo vigente recibido por parametro.
     */
    @Query(value = """
        SELECT g.PERF_ID AS instructor, COUNT(DISTINCT cl.CLS_CODIGO) AS clases
        FROM tbl_grupo g
        INNER JOIN tbl_clase cl
            ON cl.CAT_TITULO = g.CAT_TITULO AND cl.CUR_NOMBRE = g.CUR_NOMBRE
           AND cl.GRP_ANIO = g.GRP_ANIO AND cl.GRP_ITERABLE = g.GRP_ITERABLE
        INNER JOIN tbl_asistencia a ON a.CLS_CODIGO = cl.CLS_CODIGO
        WHERE g.META_ELIMINADO = 0
          AND cl.META_ELIMINADO = 0
          AND a.META_ELIMINADO = 0
          AND g.PERF_ID IS NOT NULL
          AND g.GRP_ANIO = :anio
          AND g.GRP_PERIODO = :periodo
        GROUP BY g.PERF_ID
        """, nativeQuery = true)
    List<Object[]> contarClasesConAsistenciaPorInstructor(@Param("anio") int anio, @Param("periodo") int periodo);
}
