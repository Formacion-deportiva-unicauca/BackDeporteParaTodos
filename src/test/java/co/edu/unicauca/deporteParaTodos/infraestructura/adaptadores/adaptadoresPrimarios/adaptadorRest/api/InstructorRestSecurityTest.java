package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInstructorServicio;
import co.edu.unicauca.deporteParaTodos.deporteParaTodos;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Instructor;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
// @Import explicito: con `classes = deporteParaTodos.class` dado, Spring Boot NO
// auto-detecta @TestConfiguration anidadas (esa auto-deteccion solo ocurre cuando
// se omite `classes`) -- sin el @Import, FirewallDePruebaConfig nunca se activaba
// (confirmado: el 400 persistia igual con y sin el bean definido).
@SpringBootTest(classes = deporteParaTodos.class, properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA="
})
@AutoConfigureMockMvc
@Import(InstructorRestSecurityTest.FirewallDePruebaConfig.class)
class InstructorRestSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IInstructorServicio servicio;

    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    // El StrictHttpFirewall POR DEFECTO de Spring Security ya rechaza (400, antes
    // de llegar al controller) cualquier URL cuyo path decodificado contenga CR/LF
    // -- confirmado al correr el test de abajo contra el firewall por defecto. Esa
    // es una capa de defensa aparte, independiente de si el CODIGO sanitiza. Aqui
    // se desactiva SOLO esa verificacion especifica (setAllowUrlEncodedCarriageReturn/
    // LineFeed) para poder probar, con una peticion HTTP real (JWT real, filtros de
    // Security activos, decodificado real de Spring MVC del path variable), que el
    // CONTROLADOR tambien sanitiza por su cuenta -- defensa en profundidad, no
    // depender de una sola capa.
    @TestConfiguration
    static class FirewallDePruebaConfig {
        @Bean
        public HttpFirewall httpFirewall() {
            StrictHttpFirewall firewall = new StrictHttpFirewall();
            firewall.setAllowUrlEncodedCarriageReturn(true);
            firewall.setAllowUrlEncodedLineFeed(true);
            return firewall;
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

    // Sonar java:S5145 (log forging): el id de la ruta se interpola crudo en
    // InstructoresRest.actualizarInstructor() (PeticionLogger.log). %0d%0a en la
    // URL es como un atacante real tendria que mandar CR/LF (un caracter de salto
    // de linea crudo no es un URI valido) -- Spring decodifica el path variable
    // a \r\n reales antes de bindearlo. Medicion: se cuenta
    // getFormattedMessage().lines() del ULTIMO evento de log capturado con un
    // ListAppender de Logback pegado a InstructoresRest.class, y se compara contra
    // una peticion de control sin CRLF -- PeticionLogger.log ya produce un bloque
    // "=====" de varias lineas por diseno, asi que no sirve buscar "\n" a secas;
    // lo que no puede cambiar es el NUMERO de lineas entre control y ataque.
    @Test
    void actualizarInstructor_idConCRLFEnLaRuta_noAumentaElNumeroDeLineasDelLog() throws Exception {
        when(servicio.actualizarInstructor(any(), any(), any())).thenReturn(instructorActualizadoDeEjemplo());
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(InstructoresRest.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
        try {
            mockMvc.perform(put("/api/v2/instructor/ins1")
                    .contentType("application/json")
                    .content(BODY_VALIDO)
                    .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                    .andExpect(status().isOk());
            long lineasControl = appender.list.get(appender.list.size() - 1).getFormattedMessage().lines().count();

            // MockMvcRequestBuilders.put(String) trata el String como plantilla y
            // RE-codifica cualquier "%" literal (produciria %250d%250a, que el
            // servidor decodifica UNA vez a los caracteres literales "%0d%0a", no a
            // \r\n) -- confirmado viendo el Request URI real con andDo(print()).
            // Pasar un java.net.URI ya construido evita esa re-codificacion: Spring
            // decodifica %0d%0a a \r\n reales al bindear el path variable, igual que
            // haria un servidor real con la peticion de un atacante.
            java.net.URI uriConCrlf = new java.net.URI("/api/v2/instructor/ins1%0d%0aFAKE-LOG-LINE");
            mockMvc.perform(put(uriConCrlf)
                    .contentType("application/json")
                    .content(BODY_VALIDO)
                    .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                    .andExpect(status().isOk());
            String mensajeAtaque = appender.list.get(appender.list.size() - 1).getFormattedMessage();
            long lineasAtaque = mensajeAtaque.lines().count();

            assertEquals(lineasControl, lineasAtaque,
                    "el id con %0d%0a no puede agregar lineas al log frente a la peticion de control");
            assertTrue(mensajeAtaque.contains("ins1__FAKE-LOG-LINE"),
                    "el id sanitizado (CR/LF -> _) debe seguir siendo identificable en el log");
        } finally {
            logbackLogger.detachAppender(appender);
        }
    }
}
