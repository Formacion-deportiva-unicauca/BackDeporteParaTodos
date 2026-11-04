package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * Registro de un perfil segun su tipo de vinculo (test de integracion, BD MySQL real):
 * POST /api/v2/RegistroPerfilAlumno, el login posterior y el catalogo publico de facultades y programas.
 *
 * Lo que solo se comprueba contra la base real: el CHECK de ALM_TIPO acepta 'Contratista', ALM_DEPENDENCIA existe,
 * y el INSERT nativo de tbl_intermedia_alumno_programa respeta la FK al alumno recien guardado.
 * El catalogo (facultades y programas) lo crea la propia prueba con el prefijo "RegPerfilIT-" para no depender de la
 * carga de programas de la migracion; todo lo que crea se borra al terminar.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class RegistroPerfilPorTipoIT {

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";
    private static final String PREFIJO_PERFIL = "regperfilit-";
    private static final String FACULTAD = "RegPerfilIT-Facultad";
    private static final String OTRA_FACULTAD = "RegPerfilIT-OtraFacultad";
    private static final String PROGRAMA = "RegPerfilIT-Programa";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void crearCatalogo() {
        limpiar();
        jdbc.update("INSERT INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, ?)", FACULTAD);
        jdbc.update("INSERT INTO tbl_facultad (META_ELIMINADO, FAC_NOMBRE) VALUES (0, ?)", OTRA_FACULTAD);
        jdbc.update("INSERT INTO tbl_programa (META_ELIMINADO, PRG_NOMBRE, FAC_NOMBRE) VALUES (0, ?, ?)", PROGRAMA, FACULTAD);
    }

    @AfterAll
    void limpiar() {
        jdbc.update("DELETE FROM tbl_intermedia_alumno_programa WHERE PERF_ID LIKE ?", PREFIJO_PERFIL + "%");
        jdbc.update("DELETE FROM tbl_alumno WHERE PERF_ID LIKE ?", PREFIJO_PERFIL + "%");
        jdbc.update("DELETE FROM tbl_perfil WHERE PERF_ID LIKE ?", PREFIJO_PERFIL + "%");
        jdbc.update("DELETE FROM tbl_programa WHERE PRG_NOMBRE LIKE ?", "RegPerfilIT-%");
        jdbc.update("DELETE FROM tbl_facultad WHERE FAC_NOMBRE LIKE ?", "RegPerfilIT-%");
    }

    // ── utilidades ────────────────────────────────────────────────────────────

    private String nuevoId() {
        return PREFIJO_PERFIL + UUID.randomUUID().toString().substring(0, 8);
    }

    private String registro(String id, String tipo, String codigo, String facultad, String programa, String dependencia)
            throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("id", id);
        cuerpo.put("nombre", "Persona " + id);
        cuerpo.put("correo", id + "@it.unicauca.edu.co");
        cuerpo.put("tipoId", "CC");
        cuerpo.put("sexo", "M");
        cuerpo.put("role", "");
        cuerpo.put("tipoAlumno", tipo);
        if (codigo != null) cuerpo.put("alumnoCodigo", codigo);
        if (facultad != null) cuerpo.put("facultad", facultad);
        if (programa != null) cuerpo.put("programa", programa);
        if (dependencia != null) cuerpo.put("dependencia", dependencia);
        return json.writeValueAsString(cuerpo);
    }

    private String codigoNuevo() {
        return "RP" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String columna(String columna, String id) {
        return jdbc.queryForObject("SELECT " + columna + " FROM tbl_alumno WHERE PERF_ID = ?", String.class, id);
    }

    private int programasDe(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_intermedia_alumno_programa WHERE PERF_ID = ?", Integer.class, id);
    }

    private int perfilesCon(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_perfil WHERE PERF_ID = ?", Integer.class, id);
    }

    private void registrarYEsperar201(String cuerpo) throws Exception {
        mockMvc.perform(post("/api/v2/RegistroPerfilAlumno").contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated());
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

    // ── registros validos ─────────────────────────────────────────────────────

    @Test
    void estudiante_conCodigoFacultadYPrograma_guardaCodigoYRelacionConElPrograma() throws Exception {
        String id = nuevoId();
        String codigo = codigoNuevo();

        registrarYEsperar201(registro(id, "Estudiante", codigo, FACULTAD, PROGRAMA, null));

        assertEquals("Estudiante", columna("ALM_TIPO", id));
        assertEquals(codigo, columna("ALM_CODIGO", id));
        assertNull(columna("ALM_DEPENDENCIA", id));
        assertEquals(1, programasDe(id), "La relacion alumno-programa debe guardarse (FK al alumno recien insertado)");
        assertEquals(PROGRAMA, jdbc.queryForObject(
                "SELECT PRG_NOMBRE FROM tbl_intermedia_alumno_programa WHERE PERF_ID = ?", String.class, id));
    }

    @Test
    void docente_conFacultadYPrograma_noGuardaCodigo() throws Exception {
        String id = nuevoId();

        registrarYEsperar201(registro(id, "Docente", codigoNuevo(), FACULTAD, PROGRAMA, null));

        assertEquals("Docente", columna("ALM_TIPO", id));
        assertNull(columna("ALM_CODIGO", id), "El Docente no tiene codigo: aunque llegue, se descarta");
        assertEquals(1, programasDe(id));
    }

    @Test
    void administrativo_conDependencia_guardaDependenciaSinProgramaNiCodigo() throws Exception {
        String id = nuevoId();

        registrarYEsperar201(registro(id, "Administrativo", codigoNuevo(), FACULTAD, PROGRAMA, "Oficina de Planeación"));

        assertEquals("Administrativo", columna("ALM_TIPO", id));
        assertEquals("Oficina de Planeación", columna("ALM_DEPENDENCIA", id));
        assertNull(columna("ALM_CODIGO", id));
        assertEquals(0, programasDe(id), "Administrativo no guarda facultad ni programa aunque lleguen");
    }

    @Test
    void contratista_esUnTipoAceptadoPorElCheckDeLaBaseDeDatos() throws Exception {
        String id = nuevoId();

        registrarYEsperar201(registro(id, "Contratista", null, null, null, "División de Sistemas"));

        assertEquals("Contratista", columna("ALM_TIPO", id));
        assertEquals("División de Sistemas", columna("ALM_DEPENDENCIA", id));
    }

    @Test
    void login_deUnDocenteRegistrado_devuelveTipoFacultadYPrograma() throws Exception {
        String id = nuevoId();
        registrarYEsperar201(registro(id, "Docente", null, FACULTAD, PROGRAMA, null));

        mockMvc.perform(get("/api/v2/login").param("email", id + "@it.unicauca.edu.co")
                        .header("Authorization", "Bearer " + buildJwt("Alumno", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("Alumno"))
                .andExpect(jsonPath("$.tipoAlumno").value("Docente"))
                .andExpect(jsonPath("$.facultad").value(FACULTAD))
                .andExpect(jsonPath("$.programa").value(PROGRAMA));
    }

    @Test
    void login_deUnContratistaRegistrado_devuelveLaDependencia() throws Exception {
        String id = nuevoId();
        registrarYEsperar201(registro(id, "Contratista", null, null, null, "Oficina Jurídica"));

        mockMvc.perform(get("/api/v2/login").param("email", id + "@it.unicauca.edu.co")
                        .header("Authorization", "Bearer " + buildJwt("Alumno", id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoAlumno").value("Contratista"))
                .andExpect(jsonPath("$.dependencia").value("Oficina Jurídica"));
    }

    // ── registros rechazados: 422 con codigo GC-0029 y nada queda guardado ────

    @Test
    void estudianteSinCodigo_retorna422_yNoGuardaNada() throws Exception {
        String id = nuevoId();

        mockMvc.perform(post("/api/v2/RegistroPerfilAlumno").contentType("application/json")
                        .content(registro(id, "Estudiante", null, FACULTAD, PROGRAMA, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0029"));

        assertEquals(0, perfilesCon(id));
    }

    @Test
    void administrativoSinDependencia_retorna422_yNoGuardaNada() throws Exception {
        String id = nuevoId();

        mockMvc.perform(post("/api/v2/RegistroPerfilAlumno").contentType("application/json")
                        .content(registro(id, "Administrativo", null, null, null, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0029"));

        assertEquals(0, perfilesCon(id));
    }

    @Test
    void programaDeOtraFacultad_retorna422_yNoGuardaNada() throws Exception {
        String id = nuevoId();

        mockMvc.perform(post("/api/v2/RegistroPerfilAlumno").contentType("application/json")
                        .content(registro(id, "Docente", null, OTRA_FACULTAD, PROGRAMA, null)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0029"));

        assertEquals(0, perfilesCon(id));
    }

    @Test
    void tipoDesconocido_retorna422() throws Exception {
        String id = nuevoId();

        mockMvc.perform(post("/api/v2/RegistroPerfilAlumno").contentType("application/json")
                        .content(registro(id, "Regular", null, null, null, null)))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(0, perfilesCon(id));
    }

    // ── catalogo publico ──────────────────────────────────────────────────────

    @Test
    void catalogo_facultadesYProgramas_sonPublicosYDevuelvenLoRegistrado() throws Exception {
        mockMvc.perform(get("/api/v2/facultades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre=='" + FACULTAD + "')]").isNotEmpty());

        mockMvc.perform(get("/api/v2/programas").param("facultad", FACULTAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value(PROGRAMA))
                .andExpect(jsonPath("$[0].facultad").value(FACULTAD));
    }

    @Test
    void catalogo_facultadSinProgramas_respondeListadoVacio() throws Exception {
        mockMvc.perform(get("/api/v2/programas").param("facultad", OTRA_FACULTAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoError").value("GC-0002"));
    }
}
