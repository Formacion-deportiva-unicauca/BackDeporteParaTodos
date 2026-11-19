package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * GET /api/v2/reportes/mensual (test de integracion, BD MySQL real, por HTTP con JWT).
 *
 * Los datos se siembran directo con SQL en marzo de 2020 y bajo una modalidad unica por corrida, asi las cifras esperadas son
 * exactas aunque la BD tenga datos de otras pruebas. Resultados conocidos de antemano (mes = 2020-03, curso A mixto de Futbol,
 * curso B para estudiantes de Natacion):
 *
 *   Inscritos del mes: A = 2 hombres (E1, AD1) y 2 mujeres (E2, D1); B = 1 hombre (E1)  -> 5 inscripciones, 4 personas.
 *       (C1 en lista de espera, X inscrito en abril y W desvinculado en febrero NO cuentan; AD1 se desvinculo el 10 de marzo: si cuenta.)
 *   Atenciones del mes: A = 6 H + 3 M; B = 1 H  -> 10 atenciones, 6 personas distintas.
 *       (ausentes, clases de febrero y abril, asistencia o clase eliminada, grupo eliminado, categorias ignoradas NO cuentan.)
 *   Calidad: 1 clase sin confirmar (la del 16; la del 23 tiene novedad), 2 atenciones sin inscripcion (C1 en espera y Z sin fila).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {"app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="})
class ReporteMensualIT {

    private static final String SECRETO = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final int ANIO = 2020;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;

    private String s;                 // sufijo unico de la corrida
    private String categoria;         // titulo guardado (minuscula, con espacio al final)
    private String modalidad;         // ya normalizada
    private String cursoA;
    private String cursoB;
    private String facultad1;
    private String facultad2;

    @BeforeAll
    void sembrar() {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        categoria = "itmod" + s + " ";
        modalidad = "Itmod" + s;
        cursoA = "RepIT-A-" + s;
        cursoB = "RepIT-B-" + s;
        facultad1 = "RepIT-Fac1-" + s;
        facultad2 = "RepIT-Fac2-" + s;
        String categoriaEliminada = "itelim" + s;

        jdbc.update("INSERT INTO tbl_categoria_curso (META_ELIMINADO, CAT_TITULO, CAT_DESCRIPCION) VALUES (0, ?, 'IT')", categoria);
        jdbc.update("INSERT INTO tbl_categoria_curso (META_ELIMINADO, CAT_TITULO, CAT_DESCRIPCION) VALUES (1, ?, 'IT')", categoriaEliminada);
        jdbc.update("INSERT IGNORE INTO tbl_categoria_curso (META_ELIMINADO, CAT_TITULO, CAT_DESCRIPCION) VALUES (0, 'Representativa institucional', 'IT')");
        curso(cursoA, "Futbol", categoria, "MIXTO");
        curso(cursoB, "Natacion", categoria, "ESTUDIANTE");
        String cursoEliminado = "RepIT-E-" + s;
        String cursoIgnorado = "RepIT-I-" + s;
        curso(cursoEliminado, "Futbol", categoriaEliminada, "MIXTO");
        curso(cursoIgnorado, "Futbol", "Representativa institucional", "MIXTO");

        grupo(categoria, cursoA, 1, 0);
        grupo(categoria, cursoA, 2, 1);   // grupo ELIMINADO
        grupo(categoria, cursoB, 1, 0);
        grupo(categoriaEliminada, cursoEliminado, 1, 0);
        grupo("Representativa institucional", cursoIgnorado, 1, 0);

        jdbc.update("INSERT INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, ?)", facultad1);
        jdbc.update("INSERT INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, ?)", facultad2);
        jdbc.update("INSERT INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, ?, ?)", "RepIT-Prog1-" + s, facultad1);
        jdbc.update("INSERT INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, ?, ?)", "RepIT-Prog2-" + s, facultad2);

        persona("e1", "M", "Estudiante", "RepIT-Prog1-" + s);
        persona("e2", "F", "Estudiante", "RepIT-Prog1-" + s);
        persona("d1", "F", "Docente", "RepIT-Prog2-" + s);
        persona("ad1", "M", "Administrativo", null);
        persona("c1", "M", "Contratista", null);
        persona("z", "M", "Estudiante", null);
        persona("x", "F", "Estudiante", null);
        persona("w", "M", "Estudiante", null);

        // Inscripciones
        inscripcion("e1", categoria, cursoA, 1, "INSCRITO", "2020-02-01 08:00:00", null);
        inscripcion("e2", categoria, cursoA, 1, "INSCRITO", "2020-02-01 08:00:00", null);
        inscripcion("d1", categoria, cursoA, 1, "INSCRITO", "2020-02-01 08:00:00", null);
        inscripcion("ad1", categoria, cursoA, 1, "INSCRITO", "2020-01-10 08:00:00", "2020-03-10 10:00:00");
        inscripcion("c1", categoria, cursoA, 1, "EN_ESPERA", "2020-02-05 08:00:00", null);
        inscripcion("x", categoria, cursoA, 1, "INSCRITO", "2020-04-02 08:00:00", null);
        inscripcion("w", categoria, cursoA, 1, "INSCRITO", "2020-01-05 08:00:00", "2020-02-20 08:00:00");
        inscripcion("e1", categoria, cursoB, 1, "INSCRITO", "2020-02-15 08:00:00", null);

        // Clases y asistencia del curso A, grupo 1
        long feb29 = clase(categoria, cursoA, 1, "2020-02-29", 0, true);
        asistencia("d1", feb29, "PRESENTE", 0);
        long c0302 = clase(categoria, cursoA, 1, "2020-03-02", 0, true);
        asistencia("e1", c0302, "PRESENTE", 0);
        asistencia("e2", c0302, "PRESENTE", 0);
        asistencia("d1", c0302, "AUSENTE", 0);
        asistencia("ad1", c0302, "PRESENTE", 0);
        asistencia("c1", c0302, "PRESENTE", 0);
        long c0309 = clase(categoria, cursoA, 1, "2020-03-09", 0, true);
        asistencia("e1", c0309, "PRESENTE", 0);
        asistencia("e2", c0309, "AUSENTE", 0);
        asistencia("d1", c0309, "PRESENTE", 0);
        asistencia("z", c0309, "PRESENTE", 0);
        long c0310 = clase(categoria, cursoA, 1, "2020-03-10", 1, true);   // clase ELIMINADA
        asistencia("e1", c0310, "PRESENTE", 0);
        long c0316 = clase(categoria, cursoA, 1, "2020-03-16", 0, false);  // sin confirmar
        asistencia("e1", c0316, "PRESENTE", 0);
        asistencia("e2", c0316, "PRESENTE", 1);                            // asistencia ELIMINADA
        long c0323 = clase(categoria, cursoA, 1, "2020-03-23", 0, false);  // sin confirmar, pero con novedad (no se dicto)
        jdbc.update("INSERT INTO tbl_novedad_clase (CAT_TITULO, CUR_NOMBRE, GRP_ANIO, GRP_ITERABLE, NOV_FECHA, NOV_MOTIVO, NOV_USUARIO_CREADOR, NOV_FECHA_CREACION, META_ELIMINADO) "
                + "VALUES (?, ?, 2020, 1, '2020-03-23', 'CLIMA', 'it', NOW(), 0)", categoria, cursoA);
        long c0331 = clase(categoria, cursoA, 1, "2020-03-31", 0, true);
        asistencia("d1", c0331, "PRESENTE", 0);
        long c0401 = clase(categoria, cursoA, 1, "2020-04-01", 0, true);
        asistencia("d1", c0401, "PRESENTE", 0);
        // Grupo eliminado, categoria eliminada y categoria ignorada: no cuentan
        asistencia("e1", clase(categoria, cursoA, 2, "2020-03-05", 0, true), "PRESENTE", 0);
        asistencia("e1", clase(categoriaEliminada, cursoEliminado, 1, "2020-03-05", 0, true), "PRESENTE", 0);
        asistencia("e1", clase("Representativa institucional", cursoIgnorado, 1, "2020-03-05", 0, true), "PRESENTE", 0);
        // Curso B
        asistencia("e1", clase(categoria, cursoB, 1, "2020-03-03", 0, true), "PRESENTE", 0);
    }

    // ── Cifras exactas ────────────────────────────────────────────────────────

    @Test
    void mensual_cifrasExactas_porCurso_totales_vinculo_calidad() throws Exception {
        consultar("").andExpect(status().isOk())
                .andExpect(jsonPath("$.anio").value(2020)).andExpect(jsonPath("$.mes").value(3))
                .andExpect(jsonPath("$.filas.length()").value(2))
                .andExpect(jsonPath("$.filas[0].modalidad").value(modalidad))
                .andExpect(jsonPath("$.filas[0].curso").value(cursoA))
                .andExpect(jsonPath("$.filas[0].disciplina").value("Futbol"))
                .andExpect(jsonPath("$.filas[0].publico").value("MIXTO"))
                .andExpect(jsonPath("$.filas[0].inscritosHombres").value(2))
                .andExpect(jsonPath("$.filas[0].inscritosMujeres").value(2))
                .andExpect(jsonPath("$.filas[0].inscritosTotal").value(4))
                .andExpect(jsonPath("$.filas[0].atencionesHombres").value(6))
                .andExpect(jsonPath("$.filas[0].atencionesMujeres").value(3))
                .andExpect(jsonPath("$.filas[0].atencionesTotal").value(9))
                .andExpect(jsonPath("$.filas[1].curso").value(cursoB))
                .andExpect(jsonPath("$.filas[1].publico").value("ESTUDIANTE"))
                .andExpect(jsonPath("$.filas[1].inscritosHombres").value(1))
                .andExpect(jsonPath("$.filas[1].inscritosMujeres").value(0))
                .andExpect(jsonPath("$.filas[1].atencionesHombres").value(1))
                .andExpect(jsonPath("$.filas[1].atencionesMujeres").value(0))
                // Conciliacion: totales = suma de filas
                .andExpect(jsonPath("$.totales.inscritosTotal").value(5))
                .andExpect(jsonPath("$.totales.inscritosHombres").value(3))
                .andExpect(jsonPath("$.totales.inscritosMujeres").value(2))
                .andExpect(jsonPath("$.totales.atencionesTotal").value(10))
                .andExpect(jsonPath("$.totales.atencionesHombres").value(7))
                .andExpect(jsonPath("$.totales.atencionesMujeres").value(3))
                // Personas distintas: consulta separada (E1 cuenta una vez aunque este en dos cursos y asista 4 veces)
                .andExpect(jsonPath("$.totales.personasDistintasInscritas").value(4))
                .andExpect(jsonPath("$.totales.personasDistintasAtendidas").value(6))
                // Por vinculo
                .andExpect(jsonPath("$.porVinculo[0].vinculo").value("Estudiante"))
                .andExpect(jsonPath("$.porVinculo[0].inscritos").value(3))
                .andExpect(jsonPath("$.porVinculo[0].atenciones").value(6))
                .andExpect(jsonPath("$.porVinculo[1].vinculo").value("Docente"))
                .andExpect(jsonPath("$.porVinculo[1].inscritos").value(1))
                .andExpect(jsonPath("$.porVinculo[1].atenciones").value(2))
                .andExpect(jsonPath("$.porVinculo[2].vinculo").value("Administrativo"))
                .andExpect(jsonPath("$.porVinculo[2].inscritos").value(1))
                .andExpect(jsonPath("$.porVinculo[2].atenciones").value(1))
                .andExpect(jsonPath("$.porVinculo[3].vinculo").value("Contratista"))
                .andExpect(jsonPath("$.porVinculo[3].inscritos").value(0))
                .andExpect(jsonPath("$.porVinculo[3].atenciones").value(1))
                // Calidad
                .andExpect(jsonPath("$.calidad.clasesSinConfirmar").value(1))
                .andExpect(jsonPath("$.calidad.atencionesSinInscripcion").value(2));
    }

    @Test
    void mensual_lasModalidadesSeNormalizan_yElFiltroNoDistingueMayusculasNiEspacios() throws Exception {
        for (String escrito : new String[] {modalidad, modalidad.toUpperCase(), "  " + modalidad.toLowerCase() + " "}) {
            conParametro("modalidad", escrito)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totales.atencionesTotal").value(10))
                    .andExpect(jsonPath("$.filtros.modalidad").value(modalidad));
        }
    }

    // ── Filtros combinables ───────────────────────────────────────────────────

    @Test
    void filtroGenero_F() throws Exception {
        consultar("&genero=F").andExpect(jsonPath("$.totales.atencionesTotal").value(3))
                .andExpect(jsonPath("$.totales.atencionesHombres").value(0))
                .andExpect(jsonPath("$.totales.inscritosTotal").value(2));
    }

    @Test
    void filtroVinculoFuncionarios_esDocenteMasAdministrativoMasContratista() throws Exception {
        consultar("&vinculo=Funcionarios").andExpect(jsonPath("$.totales.atencionesTotal").value(4))
                .andExpect(jsonPath("$.totales.inscritosTotal").value(2))
                .andExpect(jsonPath("$.porVinculo.length()").value(3));
    }

    @Test
    void agruparFuncionarios_unePorVinculo_ySinCambiarLosTotales() throws Exception {
        consultar("&agruparFuncionarios=true")
                .andExpect(jsonPath("$.porVinculo.length()").value(2))
                .andExpect(jsonPath("$.porVinculo[1].vinculo").value("Funcionarios"))
                .andExpect(jsonPath("$.porVinculo[1].atenciones").value(4))
                .andExpect(jsonPath("$.porVinculo[1].inscritos").value(2))
                .andExpect(jsonPath("$.totales.atencionesTotal").value(10));
    }

    @Test
    void filtroFacultad_dejaFueraAdministrativosYContratistas_yNoDuplicaPersonas() throws Exception {
        consultar("&facultad=" + java.net.URLEncoder.encode(facultad1, java.nio.charset.StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.totales.atencionesTotal").value(5))      // E1 x4 + E2 x1
                .andExpect(jsonPath("$.totales.inscritosTotal").value(3))       // E1 en A y B + E2
                .andExpect(jsonPath("$.porVinculo[2].atenciones").value(0))
                .andExpect(jsonPath("$.porVinculo[3].atenciones").value(0));
        consultar("&facultad=" + java.net.URLEncoder.encode(facultad2, java.nio.charset.StandardCharsets.UTF_8))
                .andExpect(jsonPath("$.totales.atencionesTotal").value(2));     // solo D1
    }

    @Test
    void filtroPublicoYDisciplina_seleccionanElCurso() throws Exception {
        consultar("&publico=ESTUDIANTE").andExpect(jsonPath("$.filas.length()").value(1))
                .andExpect(jsonPath("$.filas[0].curso").value(cursoB)).andExpect(jsonPath("$.totales.atencionesTotal").value(1));
        consultar("&publico=MIXTO").andExpect(jsonPath("$.filas.length()").value(1))
                .andExpect(jsonPath("$.filas[0].curso").value(cursoA));
        consultar("&disciplina=Natacion").andExpect(jsonPath("$.filas.length()").value(1))
                .andExpect(jsonPath("$.filas[0].curso").value(cursoB));
    }

    @Test
    void filtrosCombinados() throws Exception {
        consultar("&publico=MIXTO&genero=M&vinculo=Estudiante,Administrativo")
                .andExpect(jsonPath("$.totales.atencionesTotal").value(5))   // E1 x3 + Z + AD1 (curso A, hombres)
                .andExpect(jsonPath("$.totales.inscritosTotal").value(2));   // E1 y AD1 en A
    }

    // ── Casos borde ───────────────────────────────────────────────────────────

    @Test
    void mesSinDatos_devuelveCeros_noError() throws Exception {
        consulta("&anio=2019&mes=1").andExpect(status().isOk())
                .andExpect(jsonPath("$.filas.length()").value(0))
                .andExpect(jsonPath("$.totales.atencionesTotal").value(0))
                .andExpect(jsonPath("$.totales.inscritosTotal").value(0))
                .andExpect(jsonPath("$.totales.personasDistintasAtendidas").value(0))
                .andExpect(jsonPath("$.calidad.clasesSinConfirmar").value(0))
                .andExpect(jsonPath("$.porVinculo.length()").value(4));
    }

    @Test
    void categoriaIgnoradaYCategoriaEliminada_noAparecenNiConFiltroExplicito() throws Exception {
        conParametro("modalidad", "Representativa institucional")
                .andExpect(jsonPath("$.filas.length()").value(0)).andExpect(jsonPath("$.totales.atencionesTotal").value(0));
        conParametro("modalidad", "itelim" + s).andExpect(jsonPath("$.filas.length()").value(0));
        // Y sin filtro de modalidad (marzo de 2020) esas dos categorias no aparecen entre las filas: lo cubre sinFiltroDeModalidad_concilia.
    }

    @Test
    void sinFiltroDeModalidad_concilia_yTraeNuestrosCursos() throws Exception {
        String json = mockMvc.perform(get("/api/v2/reportes/mensual?anio=2020&mes=3").header("Authorization", "Bearer " + jwt("Coordinador")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode r = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        long inscritos = 0;
        long atenciones = 0;
        boolean estaA = false;
        for (com.fasterxml.jackson.databind.JsonNode f : r.get("filas")) {
            inscritos += f.get("inscritosTotal").asLong();
            atenciones += f.get("atencionesTotal").asLong();
            estaA |= cursoA.equals(f.get("curso").asText());
        }
        assertTrue(estaA);
        assertEquals(inscritos, r.get("totales").get("inscritosTotal").asLong());
        assertEquals(atenciones, r.get("totales").get("atencionesTotal").asLong());
        long porVinculoAtenciones = 0;
        for (com.fasterxml.jackson.databind.JsonNode v : r.get("porVinculo")) {
            porVinculoAtenciones += v.get("atenciones").asLong();
        }
        assertEquals(atenciones, porVinculoAtenciones);
        assertFalse(json.contains("Representativa") || json.contains("itelim"));
    }

    @Test
    void opciones_primerMesConDatos_modalidadesNormalizadas_yFacultades() throws Exception {
        String json = mockMvc.perform(get("/api/v2/reportes/mensual/opciones").header("Authorization", "Bearer " + jwt("Coordinador")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        com.fasterxml.jackson.databind.JsonNode o = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json);
        assertTrue(o.get("primerMes").asText().compareTo("2020-01") <= 0, "primer mes con datos: " + o.get("primerMes"));
        assertTrue(json.contains("\"" + modalidad + "\""));
        assertFalse(json.contains("Representativa") || json.contains("Itelim") || json.contains("itelim"));
        assertTrue(json.contains(facultad1));
        assertTrue(json.contains("Futbol"));
        assertTrue(json.contains("\"Estudiante\"") && json.contains("\"MIXTO\""));
    }

    // ── Seguridad por HTTP ────────────────────────────────────────────────────

    @Test
    void soloElCoordinador() throws Exception {
        for (String rol : new String[] {"Alumno", "Instructor", "Administrador"}) {
            mockMvc.perform(get("/api/v2/reportes/mensual?anio=2020&mes=3").header("Authorization", "Bearer " + jwt(rol)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v2/reportes/mensual?anio=2020&mes=3")).andExpect(status().isUnauthorized());
    }

    @Test
    void parametrosInvalidos_422() throws Exception {
        mockMvc.perform(get("/api/v2/reportes/mensual?anio=2020&mes=13").header("Authorization", "Bearer " + jwt("Coordinador")))
                .andExpect(status().isUnprocessableEntity());
        mockMvc.perform(get("/api/v2/reportes/mensual?anio=2020&mes=3&genero=X").header("Authorization", "Bearer " + jwt("Coordinador")))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── Ayudas ────────────────────────────────────────────────────────────────

    /** Marzo de 2020 con la modalidad unica de esta corrida mas los filtros extra (cadena de consulta). */
    private ResultActions consultar(String extra) throws Exception {
        return consulta("&modalidad=" + modalidad + extra);
    }

    /** Marzo de 2020 con UN parametro enviado tal cual (con espacios), via .param(): evita el doble codificado de la URL. */
    private ResultActions conParametro(String nombre, String valor) throws Exception {
        return mockMvc.perform(get("/api/v2/reportes/mensual").param("anio", "2020").param("mes", "3").param(nombre, valor)
                .header("Authorization", "Bearer " + jwt("Coordinador")));
    }

    private ResultActions consulta(String extra) throws Exception {
        String base = extra.contains("anio=") ? "/api/v2/reportes/mensual?" + extra.substring(1) : "/api/v2/reportes/mensual?anio=2020&mes=3" + extra;
        return mockMvc.perform(get(base).header("Authorization", "Bearer " + jwt("Coordinador")));
    }

    private void curso(String nombre, String deporte, String cat, String publico) {
        jdbc.update("INSERT INTO tbl_curso (META_ELIMINADO, CUR_NOMBRE, DEPT_NOMBRE, CAT_TITULO, CUR_DESCRIPCION, CUR_PUBLICO) VALUES (0, ?, ?, ?, 'IT', ?)",
                nombre, deporte, cat, publico);
    }

    private void grupo(String cat, String curso, int iterable, int eliminado) {
        jdbc.update("INSERT INTO tbl_grupo (META_ELIMINADO, GRP_ANIO, GRP_ITERABLE, CUR_NOMBRE, CAT_TITULO, GRP_CUPOS, GRP_FECHACREACION) "
                + "VALUES (?, ?, ?, ?, ?, 30, '2020-01-01')", eliminado, ANIO, iterable, curso, cat);
    }

    private void persona(String clave, String sexo, String vinculo, String programa) {
        String id = "rep-it-" + s + "-" + clave;
        jdbc.update("INSERT INTO tbl_perfil (META_ELIMINADO, PERF_ID, PERF_NOMBRE, PERF_CORREO, PERF_TIPOID, PERF_SEXO) VALUES (0, ?, ?, ?, 'CC', ?)",
                id, "Persona " + clave, id + "@it.unicauca.edu.co", sexo);
        jdbc.update("INSERT INTO tbl_alumno (META_ELIMINADO, PERF_ID, ALM_TIPO) VALUES (0, ?, ?)", id, vinculo);
        if (programa != null) {
            jdbc.update("INSERT INTO tbl_intermedia_alumno_programa (META_ELIMINADO, PERF_ID, PRG_NOMBRE) VALUES (0, ?, ?)", id, programa);
        }
    }

    private void inscripcion(String persona, String cat, String curso, int iterable, String estado, String fecha, String desvinculacion) {
        jdbc.update("INSERT INTO tbl_inscripcion (META_ELIMINADO, INSCR_FECHAINSCRIPCION, INSCR_FECHADESVINCULACION, INSCR_ESTADO, GRP_ANIO, GRP_ITERABLE, CUR_NOMBRE, CAT_TITULO, PERF_ID) "
                + "VALUES (0, ?, ?, ?, ?, ?, ?, ?, ?)", Timestamp.valueOf(fecha), desvinculacion == null ? null : Timestamp.valueOf(desvinculacion),
                estado, ANIO, iterable, curso, cat, "rep-it-" + s + "-" + persona);
    }

    private long clase(String cat, String curso, int iterable, String fecha, int eliminado, boolean confirmada) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tbl_clase (META_ELIMINADO, CLS_FECHA, CLS_DURACION_HORAS, CLS_DURACION_MINUTOS, GRP_ANIO, GRP_ITERABLE, CUR_NOMBRE, CAT_TITULO, CLS_ASISTENCIA_CONFIRMADA_EN) "
                            + "VALUES (?, ?, 1, 30, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, eliminado);
            ps.setDate(2, java.sql.Date.valueOf(fecha));
            ps.setInt(3, ANIO);
            ps.setInt(4, iterable);
            ps.setString(5, curso);
            ps.setString(6, cat);
            ps.setTimestamp(7, confirmada ? Timestamp.valueOf(fecha + " 20:00:00") : null);
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }

    private void asistencia(String persona, long clase, String estado, int eliminado) {
        jdbc.update("INSERT INTO tbl_asistencia (META_ELIMINADO, PERF_ID, CLS_CODIGO, ASI_ESTADO) VALUES (?, ?, ?, ?)",
                eliminado, "rep-it-" + s + "-" + persona, clase, estado);
    }

    private static String jwt(String rol) {
        SecretKey clave = new SecretKeySpec(Base64.getDecoder().decode(SECRETO), "HmacSHA256");
        NimbusJwtEncoder codificador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(clave).build())));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("reporte-it@it.unicauca.edu.co").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600)).claim("rol", rol).claim("perf_id", "reporte-it").build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
