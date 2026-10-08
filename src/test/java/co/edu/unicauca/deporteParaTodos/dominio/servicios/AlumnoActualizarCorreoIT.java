package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IPerfilGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;

/**
 * PUT /api/v2/alumnos/{id} (SCRUM-182, BD MySQL real): la violacion de
 * UQ_PERFIL_CORREO debe traducirse a 409, no al 500 generico que daba antes.
 * Confirma tambien, contra la BD real (no un mock), que PerfilGateway.actualizarPerfil
 * usa saveAndFlush() correctamente -- es decir, que la excepcion de integridad se
 * dispara DENTRO del metodo y es capturable, no despues en el commit.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
class AlumnoActualizarCorreoIT {

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @Autowired
    private IPerfilGateway perfilGateway;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private String alumno1Id;
    private String alumno1CorreoOriginal;
    private String alumno2Correo;

    @BeforeAll
    void crearFixtures() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        alumno1Id = "alumno-it-correo-1-" + sufijo;
        String alumno2Id = "alumno-it-correo-2-" + sufijo;
        alumno1CorreoOriginal = alumno1Id + "@it.unicauca.edu.co";
        alumno2Correo = alumno2Id + "@it.unicauca.edu.co";

        crearAlumno(alumno1Id, alumno1CorreoOriginal);
        crearAlumno(alumno2Id, alumno2Correo);
    }

    private void crearAlumno(String id, String correo) {
        try {
            Perfil perfil = new Perfil();
            perfil.setId(id);
            perfil.setNombre("Alumno Prueba " + id);
            perfil.setCorreo(correo);
            perfil.setTipoId("CC");
            perfil.setSexo("M");
            perfil.setTipoAlumno("Estudiante");
            perfilGateway.registrarAlumno(perfil);
        } catch (YaExisteElementoExcepcion e) {
            // Ya existe de una corrida anterior; se reutiliza.
        }
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

    private String correoActualDe(String perfilId) {
        return jdbc.queryForObject(
                "SELECT PERF_CORREO FROM tbl_perfil WHERE PERF_ID = ?", String.class, perfilId);
    }

    // Captura el correo ANTES de este test (no asume el valor original de
    // @BeforeAll): otros tests de esta misma clase tambien actualizan a alumno1, y
    // JUnit no garantiza el orden de ejecucion entre ellos.
    @Test
    void actualizarAlumno_correoDeOtroAlumno_retorna409YNoModificaLaFila() throws Exception {
        String correoAntes = correoActualDe(alumno1Id);
        String body = "{\"nombre\":\"Alumno Prueba\",\"correo\":\"" + alumno2Correo + "\","
                + "\"tipoAlumno\":\"Estudiante\"}";

        mockMvc.perform(put("/api/v2/alumnos/" + alumno1Id)
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").exists());

        assertEquals(correoAntes, correoActualDe(alumno1Id),
                "la fila NO debe haber cambiado tras el 409");
    }

    @Test
    void actualizarAlumno_mismoCorreoPropio_retorna200() throws Exception {
        String correoPropio = correoActualDe(alumno1Id);
        String body = "{\"nombre\":\"Alumno Prueba Renombrado\",\"correo\":\"" + correoPropio + "\","
                + "\"tipoAlumno\":\"Estudiante\"}";

        mockMvc.perform(put("/api/v2/alumnos/" + alumno1Id)
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());

        assertEquals(correoPropio, correoActualDe(alumno1Id));
    }

    @Test
    void actualizarAlumno_correoLibre_retorna200YActualizaLaFila() throws Exception {
        String correoNuevo = "alumno-it-correo-nuevo-" + UUID.randomUUID().toString().substring(0, 8)
                + "@it.unicauca.edu.co";
        String body = "{\"nombre\":\"Alumno Prueba\",\"correo\":\"" + correoNuevo + "\","
                + "\"tipoAlumno\":\"Estudiante\"}";

        mockMvc.perform(put("/api/v2/alumnos/" + alumno1Id)
                .contentType("application/json")
                .content(body)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord-it")))
                .andExpect(status().isOk());

        assertEquals(correoNuevo, correoActualDe(alumno1Id));
    }
}
