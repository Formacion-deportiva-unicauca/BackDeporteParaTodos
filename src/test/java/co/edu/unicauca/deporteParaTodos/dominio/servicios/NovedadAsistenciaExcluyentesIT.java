package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
 * Una clase no realizada (novedad activa) y la asistencia de la misma fecha se excluyen entre si (BD MySQL real, por HTTP con JWT de
 * Coordinador, que no tiene limite de plazo: las fechas son de 2020). Cada prueba usa su propia fecha dentro del mismo grupo.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {"app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="})
class NovedadAsistenciaExcluyentesIT {

    private static final String SECRETO = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final int ANIO = 2020;
    private static final String MSG_NO_REALIZADA = "Esta clase fue marcada como no realizada";
    private static final String MSG_YA_HAY_ASISTENCIA = "Ya hay asistencia registrada para esta fecha";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private java.time.Clock clock;

    private String s;
    private String categoria;
    private String curso;
    private String alumno;

    @BeforeAll
    void sembrar() {
        s = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        categoria = "ITNov" + s;
        curso = "NovIT-" + s;
        alumno = "nov-it-" + s;
        jdbc.update("INSERT INTO tbl_categoria_curso (META_ELIMINADO, CAT_TITULO, CAT_DESCRIPCION) VALUES (0, ?, 'IT')", categoria);
        jdbc.update("INSERT INTO tbl_curso (META_ELIMINADO, CUR_NOMBRE, DEPT_NOMBRE, CAT_TITULO, CUR_DESCRIPCION, CUR_PUBLICO) "
                + "VALUES (0, ?, 'Futbol', ?, 'IT', 'MIXTO')", curso, categoria);
        jdbc.update("INSERT INTO tbl_grupo (META_ELIMINADO, GRP_ANIO, GRP_ITERABLE, CUR_NOMBRE, CAT_TITULO, GRP_CUPOS, GRP_FECHACREACION) "
                + "VALUES (0, ?, 1, ?, ?, 30, '2020-01-01')", ANIO, curso, categoria);
        jdbc.update("INSERT INTO tbl_perfil (META_ELIMINADO, PERF_ID, PERF_NOMBRE, PERF_CORREO, PERF_TIPOID, PERF_SEXO) VALUES (0, ?, 'Alumno IT', ?, 'CC', 'M')",
                alumno, alumno + "@it.unicauca.edu.co");
        jdbc.update("INSERT INTO tbl_alumno (META_ELIMINADO, PERF_ID, ALM_TIPO) VALUES (0, ?, 'Estudiante')", alumno);
        jdbc.update("INSERT INTO tbl_inscripcion (META_ELIMINADO, INSCR_FECHAINSCRIPCION, INSCR_ESTADO, GRP_ANIO, GRP_ITERABLE, CUR_NOMBRE, CAT_TITULO, PERF_ID) "
                + "VALUES (0, '2020-01-02 08:00:00', 'INSCRITO', ?, 1, ?, ?, ?)", ANIO, curso, categoria, alumno);
    }

    // ── Regla 2 (y 4): asistencia con novedad activa -> 409 ───────────────────

    @Test
    void conNovedadActiva_noSePuedeCrearLaClase_niRegistrarAsistencia_niConfirmar() throws Exception {
        String fecha = "2020-05-04";
        novedad(fecha, "CLIMA").andExpect(status().isCreated());

        // El flujo del instructor empieza creando la clase del dia: no hay clase en una fecha no realizada.
        mockMvc.perform(post("/api/v2/claseGrupo").contentType("application/json").content(cuerpoClase(fecha))
                .header("Authorization", "Bearer " + jwt())).andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(MSG_NO_REALIZADA)));
        assertEquals(0, clasesEnFecha(fecha));

        // Dato ya inconsistente (clase existente de antes de esta regla): ni registrar ni confirmar -- tambien para el Coordinador.
        long clase = clase(fecha, false);
        mockMvc.perform(post("/api/v2/atenciones").param("idClase", String.valueOf(clase)).contentType("application/json")
                .content("[{\"idPerfil\":\"" + alumno + "\",\"idClase\":" + clase + ",\"estaAtendido\":true}]")
                .header("Authorization", "Bearer " + jwt())).andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(MSG_NO_REALIZADA)));
        mockMvc.perform(put("/api/v2/clase/" + clase + "/asistencia").contentType("application/json")
                .content("{\"asistencias\":[{\"idPerfil\":\"" + alumno + "\",\"estaAtendido\":true}]}")
                .header("Authorization", "Bearer " + jwt())).andExpect(status().isConflict());

        assertEquals(0, asistenciasActivas(clase));
        assertEquals(0, confirmadas(clase));
    }

    // ── Regla 1 y 3: novedad con asistencia -> 409 ────────────────────────────

    @Test
    void conAsistenciaRegistradaSinConfirmar_noSePuedeMarcarNoRealizada() throws Exception {
        String fecha = "2020-05-05";
        long clase = clase(fecha, false);
        asistencia(clase, "PRESENTE");

        novedad(fecha, "CLIMA").andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(MSG_YA_HAY_ASISTENCIA)));

        assertEquals(0, novedadesActivas(fecha));
        assertEquals(1, asistenciasActivas(clase));
    }

    @Test
    void conAsistenciaConfirmada_noSePuedeMarcarNoRealizada_aunqueTodosEstenAusentes() throws Exception {
        String fecha = "2020-05-06";
        long clase = clase(fecha, true);
        asistencia(clase, "AUSENTE");

        novedad(fecha, "OTRO").andExpect(status().isConflict());

        assertEquals(0, novedadesActivas(fecha));
    }

    @Test
    void claseSinAsistenciaAlguna_noImpideLaNovedad() throws Exception {
        String fecha = "2020-05-07";
        clase(fecha, false);

        novedad(fecha, "CLIMA").andExpect(status().isCreated());

        assertEquals(1, novedadesActivas(fecha));
    }

    // ── Novedad duplicada: 409 con mensaje claro (antes salia como error del servidor) ──

    @Test
    void novedadDuplicada_responde409ConMensajeClaro() throws Exception {
        String fecha = "2020-05-08";
        novedad(fecha, "CLIMA").andExpect(status().isCreated());

        novedad(fecha, "CLIMA").andExpect(status().isConflict())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ya existe una novedad activa para este grupo y fecha")));

        assertEquals(1, novedadesActivas(fecha));
    }

    // ── Regla 5: novedad cancelada o eliminada -> vuelve a permitirse la asistencia ──

    @Test
    void novedadEliminada_vuelveAPermitirLaAsistenciaDeLaFecha() throws Exception {
        String fecha = "2020-05-11";
        novedad(fecha, "CLIMA").andExpect(status().isCreated());
        long clase = clase(fecha, false);
        mockMvc.perform(post("/api/v2/atenciones").param("idClase", String.valueOf(clase)).contentType("application/json")
                .content(atencion(clase)).header("Authorization", "Bearer " + jwt())).andExpect(status().isConflict());

        jdbc.update("UPDATE tbl_novedad_clase SET META_ELIMINADO = 1 WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? AND NOV_FECHA = ?", categoria, curso, fecha);

        mockMvc.perform(post("/api/v2/atenciones").param("idClase", String.valueOf(clase)).contentType("application/json")
                .content(atencion(clase)).header("Authorization", "Bearer " + jwt())).andExpect(status().isOk());
        assertEquals(1, asistenciasActivas(clase));
    }

    // ── "Cursos a mi cargo" sabe si la clase de hoy fue marcada como no realizada ──

    @Test
    void misGrupos_traeNovedadHoy_soloCuandoLaHayActivaParaHoy() throws Exception {
        String instructor = "nov-it-ins-" + s;
        jdbc.update("INSERT INTO tbl_perfil (META_ELIMINADO, PERF_ID, PERF_NOMBRE, PERF_CORREO, PERF_TIPOID, PERF_SEXO) VALUES (0, ?, 'Instructor IT', ?, 'CC', 'M')",
                instructor, instructor + "@it.unicauca.edu.co");
        jdbc.update("INSERT INTO tbl_instructor (META_ELIMINADO, PERF_ID) VALUES (0, ?)", instructor);
        jdbc.update("UPDATE tbl_grupo SET PERF_ID = ? WHERE CAT_TITULO = ? AND CUR_NOMBRE = ?", instructor, categoria, curso);
        String hoy = java.time.LocalDate.now(clock).toString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v2/grupo/misGrupos")
                .header("Authorization", "Bearer " + jwt("Instructor", instructor))).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].novedadHoy").doesNotExist());

        // Hoy no se puede registrar la novedad por la ventana del Instructor; el Coordinador si puede (sin limite de plazo).
        novedadEn(hoy).andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v2/grupo/misGrupos")
                .header("Authorization", "Bearer " + jwt("Instructor", instructor))).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].novedadHoy.motivo").value("CLIMA"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$[0].novedadHoy.fecha").value(hoy));
    }

    // ── Carrera: una novedad y una asistencia del mismo grupo y fecha al mismo tiempo ──

    @Test
    void carrera_novedadContraAsistencia_gananUnaSolaVez_yNuncaQuedanLasDos() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int ronda = 0; ronda < 12; ronda++) {
                String fecha = String.format("2020-06-%02d", ronda + 1);
                long clase = clase(fecha, false);
                CountDownLatch salida = new CountDownLatch(1);
                Callable<Integer> conNovedad = () -> {
                    salida.await();
                    return novedad(fecha, "CLIMA").andReturn().getResponse().getStatus();
                };
                Callable<Integer> conAsistencia = () -> {
                    salida.await();
                    return mockMvc.perform(post("/api/v2/atenciones").param("idClase", String.valueOf(clase)).contentType("application/json")
                            .content(atencion(clase)).header("Authorization", "Bearer " + jwt())).andReturn().getResponse().getStatus();
                };
                Future<Integer> a = pool.submit(conNovedad);
                Future<Integer> b = pool.submit(conAsistencia);
                salida.countDown();
                int estadoNovedad = a.get();
                int estadoAsistencia = b.get();

                boolean novedadGano = estadoNovedad == 201 && estadoAsistencia == 409;
                boolean asistenciaGano = estadoNovedad == 409 && estadoAsistencia == 200;
                assertTrue(novedadGano || asistenciaGano, "ronda " + ronda + ": novedad=" + estadoNovedad + " asistencia=" + estadoAsistencia);
                assertTrue(novedadesActivas(fecha) + asistenciasActivas(clase) == 1,
                        "ronda " + ronda + ": quedaron las dos o ninguna (novedades=" + novedadesActivas(fecha)
                                + ", asistencias=" + asistenciasActivas(clase) + ")");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // ── Ayudas ────────────────────────────────────────────────────────────────

    private ResultActions novedadEn(String fecha) throws Exception {
        return novedad(fecha, "CLIMA");
    }

    private ResultActions novedad(String fecha, String motivo) throws Exception {
        String cuerpo = "{\"categoria\":\"" + categoria + "\",\"curso\":\"" + curso + "\",\"anio\":" + ANIO + ",\"iterable\":1,"
                + "\"fecha\":\"" + fecha + "\",\"motivo\":\"" + motivo + "\",\"descripcion\":\"prueba de integracion\"}";
        return mockMvc.perform(post("/api/v2/novedades-clase").contentType("application/json").content(cuerpo)
                .header("Authorization", "Bearer " + jwt()));
    }

    private String cuerpoClase(String fecha) {
        return "{\"idGrupoCategoria\":\"" + categoria + "\",\"idGrupoCurso\":\"" + curso + "\",\"idGrupoAnio\":" + ANIO
                + ",\"idGrupoIterable\":1,\"idInstructor\":\"x\",\"fecha\":\"" + fecha + "\",\"horas\":1,\"minutos\":0,"
                + "\"justificacion\":\"clase de prueba de integracion\",\"observacion\":\"IT\",\"eliminado\":0}";
    }

    private String atencion(long clase) {
        return "[{\"idPerfil\":\"" + alumno + "\",\"idClase\":" + clase + ",\"estaAtendido\":true}]";
    }

    private long clase(String fecha, boolean confirmada) {
        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.update(con -> {
            java.sql.PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tbl_clase (META_ELIMINADO, CLS_FECHA, CLS_DURACION_HORAS, CLS_DURACION_MINUTOS, GRP_ANIO, GRP_ITERABLE, CUR_NOMBRE, CAT_TITULO, CLS_ASISTENCIA_CONFIRMADA_EN) "
                            + "VALUES (0, ?, 1, 0, ?, 1, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            ps.setDate(1, java.sql.Date.valueOf(fecha));
            ps.setInt(2, ANIO);
            ps.setString(3, curso);
            ps.setString(4, categoria);
            ps.setTimestamp(5, confirmada ? Timestamp.valueOf(fecha + " 20:00:00") : null);
            return ps;
        }, llave);
        return llave.getKey().longValue();
    }

    private void asistencia(long clase, String estado) {
        jdbc.update("INSERT INTO tbl_asistencia (META_ELIMINADO, PERF_ID, CLS_CODIGO, ASI_ESTADO) VALUES (0, ?, ?, ?)", alumno, clase, estado);
    }

    private int clasesEnFecha(String fecha) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? AND CLS_FECHA = ? AND META_ELIMINADO = 0",
                Integer.class, categoria, curso, fecha);
    }

    private int asistenciasActivas(long clase) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_asistencia WHERE CLS_CODIGO = ? AND META_ELIMINADO = 0", Integer.class, clase);
    }

    private int confirmadas(long clase) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_clase WHERE CLS_CODIGO = ? AND CLS_ASISTENCIA_CONFIRMADA_EN IS NOT NULL", Integer.class, clase);
    }

    private int novedadesActivas(String fecha) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_novedad_clase WHERE CAT_TITULO = ? AND CUR_NOMBRE = ? AND NOV_FECHA = ? AND META_ELIMINADO = 0",
                Integer.class, categoria, curso, fecha);
    }

    private static String jwt() {
        return jwt("Coordinador", "novedad-it");
    }

    private static String jwt(String rol, String perfId) {
        SecretKey clave = new SecretKeySpec(Base64.getDecoder().decode(SECRETO), "HmacSHA256");
        NimbusJwtEncoder codificador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(clave).build())));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("novedad-it@it.unicauca.edu.co").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600)).claim("rol", rol).claim("perf_id", perfId).build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
