package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IInscripcionServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.InscripcionDto;

import java.util.List;
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

import java.sql.Timestamp;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InscripcionRestTest {

    @Mock
    private IInscripcionServicio servicio;

    @InjectMocks
    private InscripcionRest inscripcionRest;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String    ALUMNO_ID = "alum1";
    private static final String    CATEGORIA = "cat1";
    private static final String    CURSO     = "cur1";
    private static final int       ANIO      = 2026;
    private static final int       ITERABLE  = 1;
    private static final Timestamp AHORA     = Timestamp.from(Instant.now());
    private static final String    COORD     = "coord-1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(inscripcionRest)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();

        // Por defecto: Coordinador (exento de autorizacion por grupo) -- los
        // tests de rol Alumno/Instructor fijan su propio JWT cuando lo necesitan.
        autenticarComo(COORD, "Coordinador");
    }

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String perfId, String rol) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("perf_id", perfId)
                .claim("rol", rol)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority(rol))));
    }

    private Inscripcion inscripcionBase() {
        return new Inscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, AHORA, null, null);
    }

    @Test
    void postInscribir_retornaCreated() throws Exception {
        when(servicio.inscribir(any(Inscripcion.class), eq(COORD), eq("Coordinador"))).thenReturn(inscripcionBase());

        InscripcionDto dto = new InscripcionDto(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, null, null, null);

        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.alumnoId").value(ALUMNO_ID))
                .andExpect(jsonPath("$.categoria").value(CATEGORIA))
                .andExpect(jsonPath("$.curso").value(CURSO));
    }

    @Test
    void postInscribir_sinJwt_retornaUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        InscripcionDto dto = new InscripcionDto(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, null, null, null);

        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());

        verify(servicio, never()).inscribir(any(), any(), any());
    }

    // El id del Instructor/Alumno sale del token, no de un parametro del body:
    // un Alumno que manda en el body el id de OTRO alumno recibe 403, y el
    // servicio nunca llega a invocarse.
    @Test
    void postInscribir_alumnoConIdAjenoEnElBody_retornaForbiddenYNoLlamaAlServicio() throws Exception {
        autenticarComo(ALUMNO_ID, "Alumno");
        InscripcionDto dto = new InscripcionDto("otro-alumno", CATEGORIA, CURSO, ANIO, ITERABLE, null, null, null);

        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());

        verify(servicio, never()).inscribir(any(), any(), any());
    }

    @Test
    void postInscribir_instructor_retornaCreated() throws Exception {
        autenticarComo("INS001", "Instructor");
        when(servicio.inscribir(any(Inscripcion.class), eq("INS001"), eq("Instructor"))).thenReturn(inscripcionBase());

        InscripcionDto dto = new InscripcionDto(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, null, null, null);

        mockMvc.perform(post("/api/v2/inscripcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(servicio).inscribir(any(Inscripcion.class), eq("INS001"), eq("Instructor"));
    }

    @Test
    void getValidarInscripcion_activa_retornaTrue() throws Exception {
        when(servicio.validarInscripcion(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(true);

        mockMvc.perform(get("/api/v2/validarInscripcion")
                        .param("alumnoId", ALUMNO_ID)
                        .param("categoria", CATEGORIA)
                        .param("curso", CURSO)
                        .param("anio", String.valueOf(ANIO))
                        .param("iterable", String.valueOf(ITERABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getValidarInscripcion_inactiva_retornaFalse() throws Exception {
        when(servicio.validarInscripcion(anyString(), anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(false);

        mockMvc.perform(get("/api/v2/validarInscripcion")
                        .param("alumnoId", ALUMNO_ID)
                        .param("categoria", CATEGORIA)
                        .param("curso", CURSO)
                        .param("anio", String.valueOf(ANIO))
                        .param("iterable", String.valueOf(ITERABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void getListaEspera_retornaOk() throws Exception {
        List<InscripcionEnEspera> lista = List.of(
                new InscripcionEnEspera(ALUMNO_ID, "Juan Perez", "juan@unicauca.edu.co", AHORA));
        when(servicio.listarEnEspera(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(lista);

        mockMvc.perform(get("/api/v2/inscripcion/listaEspera")
                        .param("prmCategoria", CATEGORIA)
                        .param("prmCurso", CURSO)
                        .param("prmAnio", String.valueOf(ANIO))
                        .param("prmIterable", String.valueOf(ITERABLE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alumnoId").value(ALUMNO_ID))
                .andExpect(jsonPath("$[0].nombre").value("Juan Perez"))
                .andExpect(jsonPath("$[0].correo").value("juan@unicauca.edu.co"));
    }

    @Test
    void getTieneInscritos_conInscritos_retornaTrue() throws Exception {
        when(servicio.tieneInscritos(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(true);

        mockMvc.perform(get("/api/v2/inscripcion/tieneInscritos")
                        .param("prmCategoria", CATEGORIA)
                        .param("prmCurso", CURSO)
                        .param("prmAnio", String.valueOf(ANIO))
                        .param("prmIterable", String.valueOf(ITERABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getTieneInscritos_sinInscritos_retornaFalse() throws Exception {
        when(servicio.tieneInscritos(anyString(), anyString(), anyInt(), anyInt()))
                .thenReturn(false);

        mockMvc.perform(get("/api/v2/inscripcion/tieneInscritos")
                        .param("prmCategoria", CATEGORIA)
                        .param("prmCurso", CURSO)
                        .param("prmAnio", String.valueOf(ANIO))
                        .param("prmIterable", String.valueOf(ITERABLE)))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void putDesvincularInscripcion_retornaOk() throws Exception {
        Inscripcion desvinculada = new Inscripcion(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, AHORA, AHORA, null);
        when(servicio.desvincularInscripcion(anyString(), anyString(), anyString(), anyInt(), anyInt(), eq(COORD), eq("Coordinador")))
                .thenReturn(desvinculada);

        InscripcionDto dto = new InscripcionDto(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, null, null, null);

        mockMvc.perform(put("/api/v2/desvincularInscripcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alumnoId").value(ALUMNO_ID))
                .andExpect(jsonPath("$.categoria").value(CATEGORIA));
    }

    @Test
    void putDesvincularInscripcion_sinJwt_retornaUnauthorized() throws Exception {
        SecurityContextHolder.clearContext();
        InscripcionDto dto = new InscripcionDto(ALUMNO_ID, CATEGORIA, CURSO, ANIO, ITERABLE, null, null, null);

        mockMvc.perform(put("/api/v2/desvincularInscripcion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());

        verify(servicio, never()).desvincularInscripcion(any(), any(), any(), anyInt(), anyInt(), any(), any());
    }
}
