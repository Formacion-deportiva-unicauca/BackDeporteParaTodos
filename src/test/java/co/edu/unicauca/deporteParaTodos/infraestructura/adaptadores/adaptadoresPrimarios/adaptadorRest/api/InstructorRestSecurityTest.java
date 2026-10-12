package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.deporteParaTodos;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DELETE /api/v2/instructor -- solo Coordinador puede eliminar un instructor (@PreAuthorize).
@SpringBootTest(classes = deporteParaTodos.class, properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
@AutoConfigureMockMvc
class InstructorRestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IInstructorServicio servicio;

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

    @Test
    void deleteInstructor_rolAlumno_retorna403() throws Exception {
        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", "ins1")
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alum1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteInstructor_rolInstructor_retorna403() throws Exception {
        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", "ins1")
                .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteInstructor_rolCoordinador_retorna200() throws Exception {
        Perfil perfil = new Perfil();
        perfil.setId("ins1");
        perfil.setNombre("Prof. Prueba");
        perfil.setCorreo("prueba@unicauca.edu.co");
        perfil.setSexo("M");
        Instructor eliminado = new Instructor();
        eliminado.setInst_codigo("ins1");
        eliminado.setPerfil(perfil);
        eliminado.setEliminado(1);
        when(servicio.eliminarInstructor(anyString())).thenReturn(eliminado);

        mockMvc.perform(delete("/api/v2/instructor")
                .param("instructorId", "ins1")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isOk());
    }

    // ── PUT /api/v2/instructor/{id} — SCRUM-79 (HE-14 HU-01) ─────────────────

    private static final String BODY_VALIDO =
            "{\"nombre\":\"Prof. Nuevo\",\"correo\":\"nuevo@unicauca.edu.co\"}";

    private Instructor instructorActualizadoDeEjemplo() {
        Perfil perfil = new Perfil();
        perfil.setId("ins1");
        perfil.setNombre("Prof. Nuevo");
        perfil.setCorreo("nuevo@unicauca.edu.co");
        perfil.setSexo("M");
        Instructor actualizado = new Instructor();
        actualizado.setInst_codigo("ins1");
        actualizado.setPerfil(perfil);
        actualizado.setEliminado(0);
        return actualizado;
    }

    @Test
    void actualizarInstructor_rolCoordinador_retorna200() throws Exception {
        when(servicio.actualizarInstructor(eq("ins1"), any(Instructor.class), eq("coord1")))
                .thenReturn(instructorActualizadoDeEjemplo());

        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content(BODY_VALIDO)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ins1"))
                .andExpect(jsonPath("$.nombre").value("Prof. Nuevo"))
                .andExpect(jsonPath("$.correo").value("nuevo@unicauca.edu.co"));
    }

    @Test
    void actualizarInstructor_rolInstructor_retorna403() throws Exception {
        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content(BODY_VALIDO)
                .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                .andExpect(status().isForbidden());

        verify(servicio, never()).actualizarInstructor(any(), any(), any());
    }

    @Test
    void actualizarInstructor_rolAlumno_retorna403() throws Exception {
        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content(BODY_VALIDO)
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alum1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void actualizarInstructor_sinToken_retorna401() throws Exception {
        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content(BODY_VALIDO))
                .andExpect(status().isUnauthorized());
    }

    // Diseno explicito (patron misCursos/misGrupos): "Si falta el claim [perf_id],
    // 401." Un JWT valido (rol Coordinador, firma correcta) pero sin perf_id no
    // debe llegar al servicio -- el idCoordinador para el log de auditoria no
    // puede faltar silenciosamente.
    @Test
    void actualizarInstructor_coordinadorSinClaimPerfId_retorna401() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(key).build()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("test@unicauca.edu.co")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", "Coordinador")
                .build();
        String jwtSinPerfId = new NimbusJwtEncoder(source)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content(BODY_VALIDO)
                .header("Authorization", "Bearer " + jwtSinPerfId))
                .andExpect(status().isUnauthorized());

        verify(servicio, never()).actualizarInstructor(any(), any(), any());
    }

    @Test
    void actualizarInstructor_idInexistente_retorna404() throws Exception {
        when(servicio.actualizarInstructor(eq("desconocido"), any(Instructor.class), eq("coord1")))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion(
                        "No existe el instructor con el identificador desconocido"));

        mockMvc.perform(put("/api/v2/instructor/desconocido")
                .contentType("application/json")
                .content(BODY_VALIDO)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isNotFound());
    }

    @Test
    void actualizarInstructor_nombreVacio_retorna400() throws Exception {
        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content("{\"nombre\":\"\",\"correo\":\"nuevo@unicauca.edu.co\"}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isBadRequest());

        verify(servicio, never()).actualizarInstructor(any(), any(), any());
    }

    @Test
    void actualizarInstructor_correoMalFormado_retorna400() throws Exception {
        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content("{\"nombre\":\"Prof. Nuevo\",\"correo\":\"no-es-un-correo\"}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizarInstructor_correoDuplicado_retorna409ConMensajeLegible() throws Exception {
        when(servicio.actualizarInstructor(eq("ins1"), any(Instructor.class), eq("coord1")))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion(
                        "El correo nuevo@unicauca.edu.co ya esta en uso por otro perfil"));

        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content(BODY_VALIDO)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        org.hamcrest.Matchers.containsString("ya esta en uso por otro perfil")));
    }

    // Cuerpo con campos extra (p.ej. "id" distinto del de la ruta): InstructorRequest
    // no declara esos campos, asi que Jackson los ignora -- no pueden cambiar nada.
    // Se verifica que el servicio recibe el id de la RUTA, nunca el del cuerpo.
    @Test
    void actualizarInstructor_cuerpoConCamposExtra_losIgnora() throws Exception {
        when(servicio.actualizarInstructor(eq("ins1"), any(Instructor.class), eq("coord1")))
                .thenReturn(instructorActualizadoDeEjemplo());

        mockMvc.perform(put("/api/v2/instructor/ins1")
                .contentType("application/json")
                .content("{\"id\":\"otro-id\",\"nombre\":\"Prof. Nuevo\",\"correo\":\"nuevo@unicauca.edu.co\","
                        + "\"sexo\":\"F\",\"rol\":\"Alumno\"}")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isOk());

        verify(servicio).actualizarInstructor(eq("ins1"), any(Instructor.class), eq("coord1"));
        verify(servicio, never()).actualizarInstructor(eq("otro-id"), any(), any());
    }
}
