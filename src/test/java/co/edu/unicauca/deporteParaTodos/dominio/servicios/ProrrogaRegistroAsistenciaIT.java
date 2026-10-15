package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;

/**
 * SCRUM-80 (rediseno, bloque 1 de 3): PUT/GET /api/v2/instructores/prorroga-registro
 * (test de integracion, BD MySQL real).
 *
 * NO requiere fixtures de Clase/Asistencia -- la prorroga en si no se cruza
 * todavia con el registro de atenciones (eso es el bloque 2). Fixtures: 3
 * instructores activos (A, B, C) + 1 instructor eliminado logicamente
 * (inactivo). "anio"/"mes" de cada caso se toman del mes VIGENTE segun el
 * Clock inyectado (America/Bogota) para que nunca se interpreten como "mes
 * futuro" sin importar cuando corra la build.
 *
 * Ejecutar este IT DOS veces seguidas debe dar el mismo resultado (limpieza
 * en @BeforeAll/@AfterAll + identificadores unicos por corrida). Una tercera
 * corrida con JAVA_TOOL_OPTIONS=-Duser.timezone=Pacific/Kiritimati prueba que
 * el resultado no cambia aunque la zona por defecto de la JVM sea una de las
 * mas adelantadas del mundo (UTC+14) -- porque "hoy" SIEMPRE sale del Clock
 * inyectado (America/Bogota), nunca de la zona del sistema.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class ProrrogaRegistroAsistenciaIT {

    private static final String PREFIJO_PERFIL = "prorrogait-%";
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @Autowired
    private IInstructorServicio instructorServicio;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private Clock clock;

    private String instructorA;
    private String instructorB;
    private String instructorC;
    private String instructorInactivo;
    private int anioVigente;
    private int mesVigente;

    @BeforeAll
    void crearFixtures() {
        limpiarRestosDeCorridasAnteriores();

        LocalDate hoy = LocalDate.now(clock);
        anioVigente = hoy.getYear();
        mesVigente = hoy.getMonthValue();

        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        instructorA = crearInstructor("a-" + sufijo);
        instructorB = crearInstructor("b-" + sufijo);
        instructorC = crearInstructor("c-" + sufijo);
        instructorInactivo = crearInstructor("inactivo-" + sufijo);
        instructorServicio.eliminarInstructor(instructorInactivo);
    }

    @AfterAll
    void limpiarAlTerminar() {
        limpiarRestosDeCorridasAnteriores();
    }

    // Orden de FK: prorroga -> instructor -> alumno -> perfil.
    private void limpiarRestosDeCorridasAnteriores() {
        jdbc.update("DELETE FROM tbl_prorroga_registro_asistencia WHERE PERF_ID LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_instructor WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_alumno WHERE perf_id LIKE ?", PREFIJO_PERFIL);
        jdbc.update("DELETE FROM tbl_perfil WHERE perf_id LIKE ?", PREFIJO_PERFIL);
    }

    private String crearInstructor(String sufijoId) {
        String id = "prorrogait-" + sufijoId;
        PerfilDto dto = new PerfilDto();
        dto.setId(id);
        dto.setNombre("Prof. Prueba " + sufijoId);
        dto.setCorreo(id + "@it.unicauca.edu.co");
        dto.setTipoId("CC");
        dto.setSexo("M");
        dto.setTipoAlumno("Docente");
        instructorServicio.registrarInstructor(dto);
        return id;
    }

    private String buildJwt(String rol, String perfId) {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(key).build()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(perfId + "@it.unicauca.edu.co")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", rol)
                .claim("perf_id", perfId)
                .build();
        return new NimbusJwtEncoder(source)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private long contarFilas(String idInstructor) {
        Long filas = jdbc.queryForObject(
                "SELECT COUNT(*) FROM tbl_prorroga_registro_asistencia WHERE PERF_ID = ? AND PRR_ANIO = ? AND PRR_MES = ?",
                Long.class, idInstructor, anioVigente, mesVigente);
        return filas == null ? 0 : filas;
    }

    @Test
    void asignarProrrogaRegistro_variosInstructores_seAsignanATodosYQuedanPersistidos() throws Exception {
        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorA + "\",\"" + instructorB + "\"],"
                        + "\"anio\":" + anioVigente + ",\"mes\":" + mesVigente + ",\"dias\":10}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        assertEquals(1, contarFilas(instructorA));
        assertEquals(1, contarFilas(instructorB));
    }

    @Test
    void asignarProrrogaRegistro_reasignarSobreElMismoInstructorYMes_reemplazaSinDuplicar() throws Exception {
        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorA + "\"],"
                        + "\"anio\":" + anioVigente + ",\"mes\":" + mesVigente + ",\"dias\":5}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dias").value(5));

        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorA + "\"],"
                        + "\"anio\":" + anioVigente + ",\"mes\":" + mesVigente + ",\"dias\":20}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dias").value(20));

        assertEquals(1, contarFilas(instructorA), "la reasignacion reemplaza, nunca duplica la fila");
    }

    @Test
    void asignarProrrogaRegistro_diasCero_eliminaLaFilaFisicamente() throws Exception {
        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorB + "\"],"
                        + "\"anio\":" + anioVigente + ",\"mes\":" + mesVigente + ",\"dias\":10}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());
        assertEquals(1, contarFilas(instructorB));

        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorB + "\"],"
                        + "\"anio\":" + anioVigente + ",\"mes\":" + mesVigente + ",\"dias\":0}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dias").value(0))
                .andExpect(jsonPath("$[0].fechaLimite").doesNotExist());

        assertEquals(0, contarFilas(instructorB), "dias=0 borra la fila fisicamente");
    }

    @Test
    void obtenerProrrogasRegistro_consultaPorMes_retornaSoloLosQueTienenProrroga() throws Exception {
        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorA + "\"],"
                        + "\"anio\":" + anioVigente + ",\"mes\":" + mesVigente + ",\"dias\":15}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v2/instructores/prorroga-registro")
                .param("anio", String.valueOf(anioVigente))
                .param("mes", String.valueOf(mesVigente))
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.idInstructor=='" + instructorA + "')].dias").value(15))
                .andExpect(jsonPath("$[?(@.idInstructor=='" + instructorC + "')]").isEmpty());
    }

    // 404 atomico: instructorC (nunca tuvo prorroga) + instructorInactivo en la
    // MISMA llamada -> debe fallar 404 y NO dejar ninguna fila parcial para C.
    @Test
    void asignarProrrogaRegistro_algunIdInactivo_esAtomicoYNoDejaFilasParciales() throws Exception {
        mockMvc.perform(put("/api/v2/instructores/prorroga-registro")
                .contentType("application/json")
                .content("{\"idsInstructores\":[\"" + instructorC + "\",\"" + instructorInactivo + "\"],"
                        + "\"anio\":" + anioVigente + ",\"mes\":" + mesVigente + ",\"dias\":10}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isNotFound());

        assertEquals(0, contarFilas(instructorC),
                "el 404 es atomico -- instructorC no debe quedar con una fila parcial");
    }
}
