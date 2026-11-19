package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IReporteGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ConsultaReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.FilaCrudaReporte;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/**
 * Consultas SQL nativas de SOLO LECTURA del reporte mensual. El SQL se arma unicamente con las constantes de esta clase (nunca con
 * texto del usuario): todos los valores viajan como parametros enlazados.
 *
 * Reglas: grupos eliminados fuera; cursos y categorias eliminados fuera; la categoria "Representativa institucional" se ignora;
 * la atencion es asistencia PRESENTE con META_ELIMINADO=0 en la asistencia y en la clase; el inscrito es INSCRITO (no lista de espera)
 * vigente en algun momento del mes. La facultad de una persona sale de su programa (EXISTS: una persona con varios programas no se duplica).
 */
@Service
public class ReporteGateway implements IReporteGateway {


    private static final String UNIONES_GRUPO_INSCRIPCION = """
            JOIN tbl_grupo g ON g.CAT_TITULO = i.CAT_TITULO AND g.CUR_NOMBRE = i.CUR_NOMBRE
                            AND g.GRP_ANIO = i.GRP_ANIO AND g.GRP_ITERABLE = i.GRP_ITERABLE
            JOIN tbl_curso c ON c.CAT_TITULO = g.CAT_TITULO AND c.CUR_NOMBRE = g.CUR_NOMBRE
            JOIN tbl_categoria_curso cc ON cc.CAT_TITULO = c.CAT_TITULO
            """;

    private static final String UNIONES_GRUPO_CLASE = """
            JOIN tbl_grupo g ON g.CAT_TITULO = cl.CAT_TITULO AND g.CUR_NOMBRE = cl.CUR_NOMBRE
                            AND g.GRP_ANIO = cl.GRP_ANIO AND g.GRP_ITERABLE = cl.GRP_ITERABLE
            JOIN tbl_curso c ON c.CAT_TITULO = g.CAT_TITULO AND c.CUR_NOMBRE = g.CUR_NOMBRE
            JOIN tbl_categoria_curso cc ON cc.CAT_TITULO = c.CAT_TITULO
            """;

    private static final String FILTRO_CURSO = """
              AND COALESCE(g.META_ELIMINADO, 0) = 0
              AND c.META_ELIMINADO = 0
              AND cc.META_ELIMINADO = 0
              AND LOWER(TRIM(cc.CAT_TITULO)) NOT IN ('representativa institucional')
              AND (:disciplina IS NULL OR c.DEPT_NOMBRE = :disciplina)
              AND (:modalidad IS NULL OR LOWER(TRIM(cc.CAT_TITULO)) = :modalidad)
              AND (:publico IS NULL OR c.CUR_PUBLICO = :publico)
            """;

    private static final String FILTRO_PERSONA = """
              AND al.ALM_TIPO IN (:vinculos)
              AND (:genero IS NULL OR p.PERF_SEXO = :genero)
              AND (:facultad IS NULL OR EXISTS (
                    SELECT 1 FROM tbl_intermedia_alumno_programa ip
                    JOIN tbl_programa pr ON pr.PRG_NOMBRE = ip.PRG_NOMBRE
                    WHERE ip.PERF_ID = al.PERF_ID AND ip.META_ELIMINADO = 0 AND pr.FAC_NOMBRE = :facultad))
            """;

    private static final String DESDE_INSCRIPCIONES = """
            FROM tbl_inscripcion i
            JOIN tbl_alumno al ON al.PERF_ID = i.PERF_ID
            JOIN tbl_perfil p ON p.PERF_ID = i.PERF_ID
            """ + UNIONES_GRUPO_INSCRIPCION + """
            WHERE i.META_ELIMINADO = 0 AND i.INSCR_ESTADO = 'INSCRITO'
              AND i.INSCR_FECHAINSCRIPCION < :finExclusivo
              AND (i.INSCR_FECHADESVINCULACION IS NULL OR i.INSCR_FECHADESVINCULACION >= :inicio)
            """ + FILTRO_CURSO + FILTRO_PERSONA;

    private static final String DESDE_ATENCIONES = """
            FROM tbl_clase cl
            JOIN tbl_asistencia a ON a.CLS_CODIGO = cl.CLS_CODIGO
            JOIN tbl_alumno al ON al.PERF_ID = a.PERF_ID
            JOIN tbl_perfil p ON p.PERF_ID = a.PERF_ID
            """ + UNIONES_GRUPO_CLASE + """
            WHERE cl.CLS_FECHA >= :inicio AND cl.CLS_FECHA < :finExclusivo AND cl.META_ELIMINADO = 0
              AND a.META_ELIMINADO = 0 AND a.ASI_ESTADO = 'PRESENTE'
            """ + FILTRO_CURSO + FILTRO_PERSONA;

    private static final String COLUMNAS = """
            SELECT cc.CAT_TITULO, c.CUR_NOMBRE, c.DEPT_NOMBRE, c.CUR_PUBLICO, al.ALM_TIPO, p.PERF_SEXO, COUNT(*)""";

    private static final String AGRUPAR = """
            GROUP BY cc.CAT_TITULO, c.CUR_NOMBRE, c.DEPT_NOMBRE, c.CUR_PUBLICO, al.ALM_TIPO, p.PERF_SEXO
            """;

    /** Atencion sin inscripcion: la persona no tiene ninguna inscripcion INSCRITO (no eliminada) en el grupo de la clase. */
    private static final String SIN_INSCRIPCION = """
            , SUM(CASE WHEN NOT EXISTS (
                    SELECT 1 FROM tbl_inscripcion ii
                    WHERE ii.PERF_ID = a.PERF_ID AND ii.CAT_TITULO = cl.CAT_TITULO AND ii.CUR_NOMBRE = cl.CUR_NOMBRE
                      AND ii.GRP_ANIO = cl.GRP_ANIO AND ii.GRP_ITERABLE = cl.GRP_ITERABLE
                      AND ii.META_ELIMINADO = 0 AND ii.INSCR_ESTADO = 'INSCRITO') THEN 1 ELSE 0 END) AS sin_inscripcion
            """;

    private static final String CLASES_SIN_CONFIRMAR = """
            SELECT COUNT(*) FROM tbl_clase cl
            """ + UNIONES_GRUPO_CLASE + """
            WHERE cl.CLS_FECHA >= :inicio AND cl.CLS_FECHA < :finExclusivo AND cl.CLS_FECHA <= :hoy
              AND cl.META_ELIMINADO = 0 AND cl.CLS_ASISTENCIA_CONFIRMADA_EN IS NULL
              AND NOT EXISTS (
                    SELECT 1 FROM tbl_novedad_clase nov
                    WHERE nov.CAT_TITULO = cl.CAT_TITULO AND nov.CUR_NOMBRE = cl.CUR_NOMBRE AND nov.GRP_ANIO = cl.GRP_ANIO
                      AND nov.GRP_ITERABLE = cl.GRP_ITERABLE AND nov.NOV_FECHA = cl.CLS_FECHA AND nov.META_ELIMINADO = 0)
            """ + FILTRO_CURSO;

    private static final String FECHAS_ATENCION_MIN = """
            SELECT MIN(cl.CLS_FECHA) FROM tbl_clase cl JOIN tbl_asistencia a ON a.CLS_CODIGO = cl.CLS_CODIGO
            WHERE cl.META_ELIMINADO = 0 AND a.META_ELIMINADO = 0 AND a.ASI_ESTADO = 'PRESENTE'""";
    private static final String FECHAS_ATENCION_MAX = """
            SELECT MAX(cl.CLS_FECHA) FROM tbl_clase cl JOIN tbl_asistencia a ON a.CLS_CODIGO = cl.CLS_CODIGO
            WHERE cl.META_ELIMINADO = 0 AND a.META_ELIMINADO = 0 AND a.ASI_ESTADO = 'PRESENTE'""";

    /** Consultas completas: constantes de compilacion, sin ningun texto que venga de la peticion. */
    private static final String SQL_INSCRITOS = COLUMNAS + ", 0 AS sin_inscripcion " + DESDE_INSCRIPCIONES + AGRUPAR;
    private static final String SQL_ATENCIONES = COLUMNAS + SIN_INSCRIPCION + DESDE_ATENCIONES + AGRUPAR;
    private static final String SQL_PERSONAS_INSCRITAS = "SELECT COUNT(DISTINCT i.PERF_ID) " + DESDE_INSCRIPCIONES;
    private static final String SQL_PERSONAS_ATENDIDAS = "SELECT COUNT(DISTINCT a.PERF_ID) " + DESDE_ATENCIONES;
    private static final String SQL_PRIMERA_INSCRIPCION =
            "SELECT MIN(INSCR_FECHAINSCRIPCION) FROM tbl_inscripcion WHERE META_ELIMINADO = 0 AND INSCR_ESTADO = 'INSCRITO'";
    private static final String SQL_PRIMERA_ATENCION = FECHAS_ATENCION_MIN;
    private static final String SQL_ULTIMA_ATENCION = FECHAS_ATENCION_MAX;
    private static final String SQL_MODALIDADES = "SELECT CAT_TITULO FROM tbl_categoria_curso WHERE META_ELIMINADO = 0 "
            + "AND LOWER(TRIM(CAT_TITULO)) NOT IN ('representativa institucional')";

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional(readOnly = true)
    public List<FilaCrudaReporte> inscritos(ConsultaReporte q) {
        Query query = em.createNativeQuery(SQL_INSCRITOS);
        enlazarInscripciones(query, q);
        return filas(query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FilaCrudaReporte> atenciones(ConsultaReporte q) {
        Query query = em.createNativeQuery(SQL_ATENCIONES);
        enlazarAtenciones(query, q);
        return filas(query);
    }

    @Override
    @Transactional(readOnly = true)
    public long personasDistintasInscritas(ConsultaReporte q) {
        Query query = em.createNativeQuery(SQL_PERSONAS_INSCRITAS);
        enlazarInscripciones(query, q);
        return numero(query.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    public long personasDistintasAtendidas(ConsultaReporte q) {
        Query query = em.createNativeQuery(SQL_PERSONAS_ATENDIDAS);
        enlazarAtenciones(query, q);
        return numero(query.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    public long clasesSinConfirmar(ConsultaReporte q) {
        Query query = em.createNativeQuery(CLASES_SIN_CONFIRMAR);
        query.setParameter("inicio", q.inicio());
        query.setParameter("finExclusivo", q.finExclusivo());
        query.setParameter("hoy", q.hoy());
        enlazarCurso(query, q);
        return numero(query.getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate primeraFechaConDatos() {
        LocalDate asistencia = fecha(em.createNativeQuery(SQL_PRIMERA_ATENCION).getSingleResult());
        LocalDate inscripcion = fecha(em.createNativeQuery(SQL_PRIMERA_INSCRIPCION).getSingleResult());
        if (asistencia == null) {
            return inscripcion;
        }
        return inscripcion != null && inscripcion.isBefore(asistencia) ? inscripcion : asistencia;
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate ultimaFechaConDatos() {
        return fecha(em.createNativeQuery(SQL_ULTIMA_ATENCION).getSingleResult());
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<String> disciplinas() {
        return em.createNativeQuery("SELECT DEPT_NOMBRE FROM tbl_deporte WHERE META_ELIMINADO = 0 ORDER BY DEPT_NOMBRE").getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<String> modalidades() {
        return em.createNativeQuery(SQL_MODALIDADES).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    @SuppressWarnings("unchecked")
    public List<String> facultades() {
        return em.createNativeQuery("SELECT FAC_NOMBRE FROM tbl_facultad WHERE META_ELIMINADO = 0 AND FAC_NOMBRE IS NOT NULL ORDER BY FAC_NOMBRE")
                .getResultList();
    }

    private static void enlazarInscripciones(Query query, ConsultaReporte q) {
        query.setParameter("inicio", LocalDateTime.of(q.inicio(), java.time.LocalTime.MIDNIGHT));
        query.setParameter("finExclusivo", LocalDateTime.of(q.finExclusivo(), java.time.LocalTime.MIDNIGHT));
        enlazarCurso(query, q);
        enlazarPersona(query, q);
    }

    private static void enlazarAtenciones(Query query, ConsultaReporte q) {
        query.setParameter("inicio", q.inicio());
        query.setParameter("finExclusivo", q.finExclusivo());
        enlazarCurso(query, q);
        enlazarPersona(query, q);
    }

    private static void enlazarCurso(Query query, ConsultaReporte q) {
        query.setParameter("disciplina", q.disciplina());
        query.setParameter("modalidad", q.modalidad());
        query.setParameter("publico", q.publico());
    }

    private static void enlazarPersona(Query query, ConsultaReporte q) {
        query.setParameter("vinculos", q.vinculos());
        query.setParameter("genero", q.genero());
        query.setParameter("facultad", q.facultad());
    }

    @SuppressWarnings("unchecked")
    private static List<FilaCrudaReporte> filas(Query query) {
        List<Object[]> crudas = query.getResultList();
        return crudas.stream().map(o -> new FilaCrudaReporte((String) o[0], (String) o[1], (String) o[2], (String) o[3],
                (String) o[4], (String) o[5], numero(o[6]), numero(o[7]))).toList();
    }

    private static long numero(Object valor) {
        return valor == null ? 0L : ((Number) valor).longValue();
    }

    private static LocalDate fecha(Object valor) {
        if (valor == null) {
            return null;
        }
        if (valor instanceof java.sql.Timestamp t) {
            return t.toLocalDateTime().toLocalDate();
        }
        if (valor instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (valor instanceof LocalDateTime dt) {
            return dt.toLocalDate();
        }
        return (LocalDate) valor;
    }
}
