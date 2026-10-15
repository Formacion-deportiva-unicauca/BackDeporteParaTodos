package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AtencionRestTest {

    @Mock
    private IAsistenciaServicio servicioAsistencia;

    @InjectMocks
    private AtencionRest atencionRest;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PERF_ID = "perf123";
    private static final int CLASE_ID = 10;

    // SCRUM-80 (rediseno, bloque 2): mismo patron que ClaseRestTest -- standaloneSetup
    // no corre los filtros reales de Spring Security, asi que se registra a mano el
    // resolver de @AuthenticationPrincipal y se fija un JWT (perf_id=INS001,
    // rol=Instructor) en el SecurityContext para simular al llamador real.
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(atencionRest)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("perf_id", "INS001")
                .claim("rol", "Instructor")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("Instructor"))));
    }

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void obtenerAtencionesPorClase_retornaOkConLista() throws Exception {
        AtencionDto dto = new AtencionDto(PERF_ID, CLASE_ID, true);
        when(servicioAsistencia.obtenerAtencionesPorClase(CLASE_ID)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v2/atencionesporclase")
                        .param("claseid", String.valueOf(CLASE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].idPerfil").value(PERF_ID))
                .andExpect(jsonPath("$[0].estaAtendido").value(true));
    }

    @Test
    void obtenerAtencionesPorClase_variosElementos_retornaListaCompleta() throws Exception {
        AtencionDto dto1 = new AtencionDto(PERF_ID, CLASE_ID, true);
        AtencionDto dto2 = new AtencionDto("perf999", CLASE_ID, true);
        when(servicioAsistencia.obtenerAtencionesPorClase(CLASE_ID)).thenReturn(List.of(dto1, dto2));

        mockMvc.perform(get("/api/v2/atencionesporclase")
                        .param("claseid", String.valueOf(CLASE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void registrarAtencionesPorClase_retornaOk() throws Exception {
        List<AtencionDto> atenciones = List.of(new AtencionDto(PERF_ID, CLASE_ID, true));
        doNothing().when(servicioAsistencia)
                .registrarAtencionesPorClase(anyList(), eq(CLASE_ID), eq("INS001"), eq("Instructor"));

        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(CLASE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atenciones)))
                .andExpect(status().isOk());

        verify(servicioAsistencia).registrarAtencionesPorClase(anyList(), eq(CLASE_ID), eq("INS001"), eq("Instructor"));
    }

    @Test
    void registrarAtencionesPorClase_sinJwt_retornaUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        List<AtencionDto> atenciones = List.of(new AtencionDto(PERF_ID, CLASE_ID, true));

        mockMvc.perform(post("/api/v2/atenciones")
                        .param("idClase", String.valueOf(CLASE_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(atenciones)))
                .andExpect(status().isUnauthorized());

        verify(servicioAsistencia, never()).registrarAtencionesPorClase(any(), any(), any(), any());
    }

    @Test
    void eliminarAsistencia_retornaOkConAtencionDtoCorrect() throws Exception {
        Asistencia asistenciaActiva = new Asistencia(PERF_ID, CLASE_ID, 0);
        when(servicioAsistencia.eliminarAsistencia(PERF_ID, (long) CLASE_ID, "INS001", "Instructor"))
                .thenReturn(asistenciaActiva);

        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", PERF_ID)
                        .param("prmClsCodigo", String.valueOf(CLASE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPerfil").value(PERF_ID))
                .andExpect(jsonPath("$.estaAtendido").value(true));
    }

    @Test
    void eliminarAsistencia_sinJwt_retornaUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(delete("/api/v2/asistencia")
                        .param("prmPerfId", PERF_ID)
                        .param("prmClsCodigo", String.valueOf(CLASE_ID)))
                .andExpect(status().isUnauthorized());

        verify(servicioAsistencia, never()).eliminarAsistencia(any(), any(), any(), any());
    }
}
