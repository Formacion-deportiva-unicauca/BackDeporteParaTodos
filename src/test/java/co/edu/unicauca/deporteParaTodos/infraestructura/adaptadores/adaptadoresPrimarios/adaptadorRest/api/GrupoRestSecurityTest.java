package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.deporteParaTodos;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
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
import java.time.LocalDate;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// PUT /api/v2/grupo -- solo Coordinador puede actualizar un grupo (@PreAuthorize).
@SpringBootTest(classes = deporteParaTodos.class, properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
@AutoConfigureMockMvc
class GrupoRestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IGrupoServicio servicio;

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

    // idInstructor null a proposito: es el caso que motiva este endpoint (quitar instructor).
    private static final String BODY_SIN_INSTRUCTOR =
            "{\"categoria\":\"cat1\",\"curso\":\"cur1\",\"imagenGrupo\":1,\"cupos\":10,"
                    + "\"idInstructor\":null,\"fechaCreacion\":\"2026-01-01\"}";

    @Test
    void putGrupo_rolAlumno_retorna403() throws Exception {
        mockMvc.perform(put("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .contentType("application/json")
                .content(BODY_SIN_INSTRUCTOR)
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alum1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void putGrupo_rolInstructor_retorna403() throws Exception {
        mockMvc.perform(put("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .contentType("application/json")
                .content(BODY_SIN_INSTRUCTOR)
                .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void putGrupo_rolCoordinador_retorna200() throws Exception {
        Grupo actualizado = new Grupo();
        actualizado.setCategoria("cat1");
        actualizado.setCurso("cur1");
        actualizado.setAnio(2026);
        actualizado.setIterable(1);
        actualizado.setImagenGrupo(1);
        actualizado.setCupos(10);
        actualizado.setIdInstructor(null);
        actualizado.setFechaCreacion(LocalDate.now());
        actualizado.setPeriodo(1);
        when(servicio.actualizarGrupo(anyString(), anyString(), anyInt(), anyInt(), any()))
                .thenReturn(actualizado);

        mockMvc.perform(put("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .contentType("application/json")
                .content(BODY_SIN_INSTRUCTOR)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isOk());
    }

    // DELETE /api/v2/grupo -- solo Coordinador puede eliminar un grupo (@PreAuthorize).

    @Test
    void deleteGrupo_rolAlumno_retorna403() throws Exception {
        mockMvc.perform(delete("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alum1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteGrupo_rolInstructor_retorna403() throws Exception {
        mockMvc.perform(delete("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteGrupo_rolCoordinador_retorna200() throws Exception {
        Grupo eliminado = new Grupo();
        eliminado.setCategoria("cat1");
        eliminado.setCurso("cur1");
        eliminado.setAnio(2026);
        eliminado.setIterable(1);
        when(servicio.eliminarGrupo(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(eliminado);

        mockMvc.perform(delete("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isOk());
    }
}
