package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IGrupoServicio;
import co.edu.unicauca.deporteParaTodos.deporteParaTodos;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechasGrupoInvalidasExcepcion;

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

    // Fechas invalidas (validadas en GrupoServicio) -> FechasGrupoInvalidasExcepcion
    // -> RestExceptionHandler la mapea a 422, con cuerpo JSON que incluye "mensaje".
    @Test
    void putGrupo_fechasInvalidas_retorna422ConMensajeDeError() throws Exception {
        when(servicio.actualizarGrupo(anyString(), anyString(), anyInt(), anyInt(), any()))
                .thenThrow(new FechasGrupoInvalidasExcepcion(
                        "La fecha de finalizacion (2026-01-01) no puede ser anterior a la fecha de creacion (2026-02-01)"));

        mockMvc.perform(put("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .contentType("application/json")
                .content(BODY_SIN_INSTRUCTOR)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").exists())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString(
                        "no puede ser anterior a la fecha de creacion")));
    }

    // Confirma que el mensaje de error ya NO lleva el prefijo generico de
    // InscripcionesCerradasExcepcion ("inscripciones ... cerradas") que se usaba antes
    // de crear FechasGrupoInvalidasExcepcion -- ese prefijo era semanticamente
    // incorrecto para un error de fechas de grupo.
    @Test
    void putGrupo_fechasInvalidas_elMensajeNoMencionaInscripcionesCerradas() throws Exception {
        when(servicio.actualizarGrupo(anyString(), anyString(), anyInt(), anyInt(), any()))
                .thenThrow(new FechasGrupoInvalidasExcepcion(
                        "La fecha de cierre de inscripciones (2026-01-01) no puede ser anterior "
                                + "a la fecha de apertura de inscripciones (2026-02-01)"));

        mockMvc.perform(put("/api/v2/grupo")
                .param("categoria", "cat1").param("curso", "cur1")
                .param("anio", "2026").param("iterable", "1")
                .contentType("application/json")
                .content(BODY_SIN_INSTRUCTOR)
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsStringIgnoringCase("inscripciones de este curso estan cerradas"))));
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

    // ── GET /api/v2/grupo/misGrupos — solo Instructor, identidad SIEMPRE del JWT ──

    @Test
    void misGrupos_rolInstructor_retorna200() throws Exception {
        when(servicio.obtenerMisGrupos("ins1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/grupo/misGrupos")
                .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                .andExpect(status().isOk());
    }

    @Test
    void misGrupos_rolAlumno_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/misGrupos")
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alum1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void misGrupos_rolCoordinador_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/misGrupos")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void misGrupos_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/misGrupos"))
                .andExpect(status().isUnauthorized());
    }

    // Diseno explicito: "Si falta el claim [perf_id], 401." Un JWT valido (rol
    // Instructor, firma correcta) pero sin el claim perf_id no debe llegar al servicio.
    @Test
    void misGrupos_jwtSinClaimPerfId_retorna401() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
        SecretKey key = new SecretKeySpec(keyBytes, "HmacSHA256");
        JWKSource<SecurityContext> source = new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(key).build()));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("test@unicauca.edu.co")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", "Instructor")
                .build();
        String jwtSinPerfId = new NimbusJwtEncoder(source)
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        mockMvc.perform(get("/api/v2/grupo/misGrupos")
                .header("Authorization", "Bearer " + jwtSinPerfId))
                .andExpect(status().isUnauthorized());

        verify(servicio, never()).obtenerMisGrupos(any());
    }

    // No se expone ningun parametro de instructorId: el id SIEMPRE sale del JWT
    // (claim perf_id), nunca de algo que el cliente pueda enviar.
    @Test
    void misGrupos_usaPerfIdDelJwt_ignorandoCualquierParametroDeCliente() throws Exception {
        when(servicio.obtenerMisGrupos("ins1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/grupo/misGrupos")
                .param("idInstructor", "ins2")
                .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                .andExpect(status().isOk());

        verify(servicio).obtenerMisGrupos("ins1");
        verify(servicio, never()).obtenerMisGrupos("ins2");
    }

    // ── GET /api/v2/grupo/instructor — SCRUM-78 (HE-13 HU-02): solo Coordinador,
    // idInstructor lo manda el cliente (a diferencia de misGrupos, que usa el JWT) ──

    @Test
    void gruposDeInstructor_rolCoordinador_retorna200() throws Exception {
        when(servicio.obtenerGruposDeInstructor("ins1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", "ins1")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isOk());
    }

    @Test
    void gruposDeInstructor_rolInstructor_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", "ins1")
                .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                .andExpect(status().isForbidden());

        verify(servicio, never()).obtenerGruposDeInstructor(any());
    }

    @Test
    void gruposDeInstructor_rolAlumno_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", "ins1")
                .header("Authorization", "Bearer " + buildJwt("Alumno", "alum1")))
                .andExpect(status().isForbidden());
    }

    @Test
    void gruposDeInstructor_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", "ins1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void gruposDeInstructor_idInexistente_retorna404() throws Exception {
        when(servicio.obtenerGruposDeInstructor("desconocido"))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion(
                        "No existe el instructor con el identificador desconocido"));

        mockMvc.perform(get("/api/v2/grupo/instructor")
                .param("idInstructor", "desconocido")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isNotFound());
    }

    // Parametro idInstructor faltante -- 400 habitual del proyecto (Spring responde
    // por defecto ante un @RequestParam requerido ausente, sin handler custom).
    @Test
    void gruposDeInstructor_sinParametroIdInstructor_retorna400() throws Exception {
        mockMvc.perform(get("/api/v2/grupo/instructor")
                .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                .andExpect(status().isBadRequest());

        verify(servicio, never()).obtenerGruposDeInstructor(any());
    }

    // Sonar java:S5145 (log forging): idInstructor es un parametro de query crudo
    // del cliente en GrupoRest.gruposDeInstructor() (PeticionLogger.log). A
    // diferencia de la variable de ruta de InstructoresRest, aqui no hay
    // re-codificacion de URL que esquivar: MockMvc .param(nombre, valor) entrega el
    // valor del parametro directamente al controller (asi llega un %0d%0a ya
    // decodificado por el servidor real). Medicion: se compara, con un
    // ListAppender de Logback en GrupoRest.class, el numero de lineas
    // (getFormattedMessage().lines()) del ultimo log contra una peticion de
    // control sin CRLF -- igual criterio que en InstructorRestSecurityTest.
    @Test
    void gruposDeInstructor_idInstructorConCRLF_noAumentaElNumeroDeLineasDelLog() throws Exception {
        when(servicio.obtenerGruposDeInstructor(any())).thenReturn(List.of());
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(GrupoRest.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
        try {
            mockMvc.perform(get("/api/v2/grupo/instructor")
                    .param("idInstructor", "ins1")
                    .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                    .andExpect(status().isOk());
            long lineasControl = appender.list.get(appender.list.size() - 1).getFormattedMessage().lines().count();

            mockMvc.perform(get("/api/v2/grupo/instructor")
                    .param("idInstructor", "ins1\r\nFAKE-LOG-LINE")
                    .header("Authorization", "Bearer " + buildJwt("Coordinador", "coord1")))
                    .andExpect(status().isOk());
            String mensajeAtaque = appender.list.get(appender.list.size() - 1).getFormattedMessage();

            assertEquals(lineasControl, mensajeAtaque.lines().count(),
                    "idInstructor con CRLF no puede agregar lineas al log frente a la peticion de control");
            assertTrue(mensajeAtaque.contains("ins1__FAKE-LOG-LINE"),
                    "el valor sanitizado (CR/LF -> _, uno por cada caracter) debe seguir siendo identificable");
        } finally {
            logbackLogger.detachAppender(appender);
        }
    }

    // Sonar java:S5145 (log forging): en misGrupos() idInstructor viene del claim
    // perf_id del JWT, no de un parametro HTTP -- se inyecta directo en el claim
    // (los claims de un JWT son valores JSON normales, un \r\n ahi es perfectamente
    // valido una vez decodificado). Mismo criterio de medicion que arriba.
    @Test
    void misGrupos_perfIdDelJwtConCRLF_noAumentaElNumeroDeLineasDelLog() throws Exception {
        when(servicio.obtenerMisGrupos(any())).thenReturn(List.of());
        Logger logbackLogger = (Logger) LoggerFactory.getLogger(GrupoRest.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logbackLogger.addAppender(appender);
        try {
            mockMvc.perform(get("/api/v2/grupo/misGrupos")
                    .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1")))
                    .andExpect(status().isOk());
            long lineasControl = appender.list.get(appender.list.size() - 1).getFormattedMessage().lines().count();

            mockMvc.perform(get("/api/v2/grupo/misGrupos")
                    .header("Authorization", "Bearer " + buildJwt("Instructor", "ins1\r\nFAKE-LOG-LINE")))
                    .andExpect(status().isOk());
            String mensajeAtaque = appender.list.get(appender.list.size() - 1).getFormattedMessage();

            assertEquals(lineasControl, mensajeAtaque.lines().count(),
                    "el claim perf_id con CRLF no puede agregar lineas al log frente a la peticion de control");
            assertTrue(mensajeAtaque.contains("ins1__FAKE-LOG-LINE"),
                    "el valor sanitizado (CR/LF -> _, uno por cada caracter) debe seguir siendo identificable");
        } finally {
            logbackLogger.detachAppender(appender);
        }
    }
}
