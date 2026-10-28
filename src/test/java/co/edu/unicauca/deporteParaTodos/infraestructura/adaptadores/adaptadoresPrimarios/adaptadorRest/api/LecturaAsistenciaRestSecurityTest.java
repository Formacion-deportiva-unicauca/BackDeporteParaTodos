package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAlumnoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IClaseServicio;
import co.edu.unicauca.deporteParaTodos.deporteParaTodos;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Lectura de la pantalla de asistencia de un grupo: alumnos del grupo, clases y atenciones de una clase.
// El Administrador puede registrar asistencia (POST/PUT/DELETE) y por eso tambien debe poder LEER lo que esa pantalla
// necesita; el Alumno no. Solo se comprueba la autorizacion de Spring Security (los servicios son mocks).
@SpringBootTest(classes = deporteParaTodos.class, properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
@AutoConfigureMockMvc
class LecturaAsistenciaRestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IAlumnoServicio servicioAlumno;

    @MockBean
    private IClaseServicio servicioClase;

    @MockBean
    private IAsistenciaServicio servicioAsistencia;

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

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

    private MockHttpServletRequestBuilder alumnosGrupo() {
        return get("/api/v2/alumnosGrupo").param("categoria", "Futbol").param("curso", "Avanzado")
                .param("anio", "2024").param("iterable", "1");
    }

    private MockHttpServletRequestBuilder clasesGrupo() {
        return get("/api/v2/clasesGrupo").param("categoria", "Futbol").param("curso", "Avanzado")
                .param("anio", "2024").param("iterable", "1");
    }

    private MockHttpServletRequestBuilder atencionesPorClase() {
        return get("/api/v2/atencionesporclase").param("claseid", "50");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Administrador", "Instructor", "Coordinador"})
    void lecturasDeLaPantallaDeAsistencia_rolesAutorizados_retornan200(String rol) throws Exception {
        String token = "Bearer " + buildJwt(rol, "usr1");
        mockMvc.perform(alumnosGrupo().header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(clasesGrupo().header("Authorization", token)).andExpect(status().isOk());
        mockMvc.perform(atencionesPorClase().header("Authorization", token)).andExpect(status().isOk());
    }

    @Test
    void lecturasDeLaPantallaDeAsistencia_rolAlumno_retornan403() throws Exception {
        String token = "Bearer " + buildJwt("Alumno", "alum1");
        mockMvc.perform(alumnosGrupo().header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(clasesGrupo().header("Authorization", token)).andExpect(status().isForbidden());
        mockMvc.perform(atencionesPorClase().header("Authorization", token)).andExpect(status().isForbidden());
    }

    @Test
    void lecturasDeLaPantallaDeAsistencia_sinToken_retornan401() throws Exception {
        mockMvc.perform(alumnosGrupo()).andExpect(status().isUnauthorized());
        mockMvc.perform(clasesGrupo()).andExpect(status().isUnauthorized());
        mockMvc.perform(atencionesPorClase()).andExpect(status().isUnauthorized());
    }
}
