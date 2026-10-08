package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAlumnoServicio;
import co.edu.unicauca.deporteParaTodos.deporteParaTodos;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
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

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PUT /api/v2/alumnos/{id} -- SCRUM-182: correo duplicado debe dar 409 con mensaje
// legible, no el 500 generico que daba antes la violacion de UQ_PERFIL_CORREO.
@SpringBootTest(classes = deporteParaTodos.class, properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
@AutoConfigureMockMvc
class AlumnoRestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IAlumnoServicio servicioAlumno;

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

    private static final String BODY = "{\"nombre\":\"Juan Perez\",\"correo\":\"correo-de-otro@unicauca.edu.co\","
            + "\"tipoAlumno\":\"Estudiante\"}";

    @Test
    void actualizarAlumno_correoYaUsado_retorna409ConMensajeLegible() throws Exception {
        when(servicioAlumno.actualizarAlumno(anyString(), any(Alumno.class)))
                .thenThrow(new YaExisteElementoExcepcion(
                        "El correo correo-de-otro@unicauca.edu.co ya esta en uso por otro perfil"));

        mockMvc.perform(put("/api/v2/alumnos/alum1")
                .contentType("application/json")
                .content(BODY)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString(
                        "ya esta en uso por otro perfil")));
    }

    @Test
    void actualizarAlumno_datosValidos_retorna200() throws Exception {
        Perfil perfil = new Perfil();
        perfil.setId("alum1");
        perfil.setNombre("Juan Perez");
        perfil.setCorreo("correo-de-otro@unicauca.edu.co");
        Alumno actualizado = new Alumno();
        actualizado.setTipoAlumno("Estudiante");
        actualizado.setPerfil(perfil);
        when(servicioAlumno.actualizarAlumno(anyString(), any(Alumno.class))).thenReturn(actualizado);

        mockMvc.perform(put("/api/v2/alumnos/alum1")
                .contentType("application/json")
                .content(BODY)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarAlumno_rolAlumno_retorna403() throws Exception {
        mockMvc.perform(put("/api/v2/alumnos/alum1")
                .contentType("application/json")
                .content(BODY)
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alum1")))
                .andExpect(status().isForbidden());
    }
}
