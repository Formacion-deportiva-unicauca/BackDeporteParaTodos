package co.edu.unicauca.deporteParaTodos.configurations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IReporteServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.OpcionesReporte;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ReporteMensual;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.ReporteRest;

/** Los reportes son solo del Coordinador: cualquier otro rol (y sin token) no llega al servicio. Mismo patron que RolAuthorizationTest. */
@WebMvcTest(controllers = ReporteRest.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://www.googleapis.com/oauth2/v3/certs"
})
class ReporteRolAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;
    @MockBean private IReporteServicio servicio;

    private String token(String rol) {
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("reporte-test@unicauca.edu.co").issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600)).claim("rol", rol).claim("perf_id", "reporte-test").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    private void mensualComo(String rol, int esperado) throws Exception {
        mockMvc.perform(get("/api/v2/reportes/mensual").param("anio", "2026").param("mes", "10")
                .header("Authorization", "Bearer " + token(rol))).andExpect(status().is(esperado));
    }

    @Test
    void mensual_conRolAlumno_retorna403_ySinTocarElServicio() throws Exception {
        mensualComo("Alumno", 403);
        verify(servicio, never()).mensual(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), anyBoolean());
    }

    @Test
    void mensual_conRolInstructor_retorna403() throws Exception {
        mensualComo("Instructor", 403);
    }

    @Test
    void mensual_conRolAdministrador_retorna403_soloElCoordinadorVeReportes() throws Exception {
        mensualComo("Administrador", 403);
    }

    @Test
    void mensual_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/v2/reportes/mensual").param("anio", "2026").param("mes", "10")).andExpect(status().isUnauthorized());
    }

    @Test
    void mensual_conRolCoordinador_retorna200ConElReporte() throws Exception {
        when(servicio.mensual(anyInt(), anyInt(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), anyBoolean()))
                .thenReturn(new ReporteMensual(2026, 10, null, List.of(), null, List.of(), null));

        mockMvc.perform(get("/api/v2/reportes/mensual").param("anio", "2026").param("mes", "10")
                .header("Authorization", "Bearer " + token("Coordinador")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.anio").value(2026)).andExpect(jsonPath("$.mes").value(10));
    }

    @Test
    void opciones_soloElCoordinador() throws Exception {
        when(servicio.opciones()).thenReturn(new OpcionesReporte("2026-10", "2026-11", List.of(), List.of(), List.of(), List.of(), List.of()));
        for (String rol : List.of("Alumno", "Instructor", "Administrador")) {
            mockMvc.perform(get("/api/v2/reportes/mensual/opciones").header("Authorization", "Bearer " + token(rol)))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/api/v2/reportes/mensual/opciones").header("Authorization", "Bearer " + token("Coordinador")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.primerMes").value("2026-10"));
    }

    @Test
    void mensual_pasaTodosLosFiltrosAlServicio() throws Exception {
        when(servicio.mensual(anyInt(), anyInt(), any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ReporteMensual(2026, 10, null, List.of(), null, List.of(), null));

        mockMvc.perform(get("/api/v2/reportes/mensual").param("anio", "2026").param("mes", "10").param("disciplina", "Voleibol")
                .param("modalidad", "Semillero").param("publico", "MIXTO").param("vinculo", "Docente,Contratista").param("genero", "F")
                .param("facultad", "Ingenieria").param("agruparFuncionarios", "true")
                .header("Authorization", "Bearer " + token("Coordinador"))).andExpect(status().isOk());

        verify(servicio).mensual(2026, 10, "Voleibol", "Semillero", "MIXTO", List.of("Docente", "Contratista"), "F", "Ingenieria", true);
    }
}
