package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.INovedadClaseServicio;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.NovedadClaseDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.NovedadClaseRequestDto;

@ExtendWith(MockitoExtension.class)
class NovedadClaseRestTest {

    @Mock
    private INovedadClaseServicio servicio;

    @InjectMocks
    private NovedadClaseRest rest;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rest)
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
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private NovedadClaseRequestDto requestBase() {
        NovedadClaseRequestDto r = new NovedadClaseRequestDto();
        r.setCategoria("Futbol");
        r.setCurso("Avanzado");
        r.setAnio(2026);
        r.setIterable(1);
        r.setFecha(LocalDate.of(2026, 10, 15));
        r.setMotivo("CLIMA");
        r.setDescripcion(null);
        return r;
    }

    private NovedadClase dominioBase() {
        NovedadClase d = new NovedadClase();
        d.setCodigo(1L);
        d.setCategoria("Futbol");
        d.setCurso("Avanzado");
        d.setAnio(2026);
        d.setIterable(1);
        d.setFecha(LocalDate.of(2026, 10, 15));
        d.setMotivo("CLIMA");
        d.setDescripcion(null);
        d.setIdUsuarioCreador("INS001");
        d.setEliminado(0);
        return d;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /novedades-clase
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_valido_retornaCreated() throws Exception {
        NovedadClase dominio = dominioBase();
        when(servicio.registrarNovedad(any(), eq("INS001"), eq("Instructor"))).thenReturn(dominio);

        mockMvc.perform(post("/api/v2/novedades-clase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBase())))
                .andExpect(status().isCreated());
    }

    @Test
    void registrarNovedad_motivoOTRO_sinDescripcion_lanzaExcepcionDelServicio() throws Exception {
        NovedadClaseRequestDto req = requestBase();
        req.setMotivo("OTRO");
        req.setDescripcion(null);
        when(servicio.registrarNovedad(any(), anyString(), anyString()))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.JustificacionInvalidaExcepcion(
                        "La descripci\u00f3n es obligatoria"));

        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v2/novedades-clase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))));
        assertTrue(ex.getCause() instanceof co.edu.unicauca.deporteParaTodos.dominio.excepciones.JustificacionInvalidaExcepcion);
        verify(servicio).registrarNovedad(any(), eq("INS001"), eq("Instructor"));
    }

    @Test
    void registrarNovedad_novedadDuplicada_lanzaExcepcionDelServicio() throws Exception {
        when(servicio.registrarNovedad(any(), anyString(), anyString()))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.NovedadDuplicadaExcepcion(
                        "Ya existe una novedad activa para este grupo y fecha"));

        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v2/novedades-clase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBase()))));
        assertTrue(ex.getCause() instanceof co.edu.unicauca.deporteParaTodos.dominio.excepciones.NovedadDuplicadaExcepcion);
        verify(servicio).registrarNovedad(any(), eq("INS001"), eq("Instructor"));
    }

    @Test
    void registrarNovedad_fechaFutura_lanzaExcepcionDelServicio() throws Exception {
        NovedadClaseRequestDto req = requestBase();
        req.setFecha(LocalDate.of(2026, 10, 16)); // futura
        when(servicio.registrarNovedad(any(), anyString(), anyString()))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion(
                        "La fecha no puede ser futura"));

        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/v2/novedades-clase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req))));
        assertTrue(ex.getCause() instanceof co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion);
        verify(servicio).registrarNovedad(any(), eq("INS001"), eq("Instructor"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /novedades-clase
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerNovedadesGrupo_retornaListaConEstadoNoRealizada() throws Exception {
        NovedadClaseDto dto = new NovedadClaseDto();
        dto.setCodigo(1L);
        dto.setCategoria("Futbol");
        dto.setCurso("Avanzado");
        dto.setAnio(2026);
        dto.setIterable(1);
        dto.setFecha(LocalDate.of(2026, 10, 15));
        dto.setMotivo("CLIMA");
        dto.setEstado("NO_REALIZADA");

        when(servicio.obtenerNovedadesGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(List.of(new NovedadClase()));

        mockMvc.perform(get("/api/v2/novedades-clase")
                        .param("categoria", "Futbol")
                        .param("curso", "Avanzado")
                        .param("anio", "2026")
                        .param("iterable", "1"))
                .andExpect(status().isOk());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /novedades-clase/{codigo}
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void actualizarNovedad_valido_retornaOk() throws Exception {
        NovedadClase dominio = new NovedadClase();
        dominio.setCodigo(1L);
        dominio.setMotivo("ESCENARIO");
        when(servicio.actualizarNovedad(eq(1L), any(), eq("INS001"), eq("Instructor"))).thenReturn(dominio);

        mockMvc.perform(put("/api/v2/novedades-clase/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBase())))
                .andExpect(status().isOk());
    }

    @Test
    void actualizarNovedad_noExiste_lanzaExcepcionDelServicio() throws Exception {
        when(servicio.actualizarNovedad(eq(99L), any(), anyString(), anyString()))
                .thenThrow(new co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion(
                        "No existe la novedad"));

        Exception ex = assertThrows(Exception.class, () ->
                mockMvc.perform(put("/api/v2/novedades-clase/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBase()))));
        assertTrue(ex.getCause() instanceof co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion);
        verify(servicio).actualizarNovedad(eq(99L), any(), eq("INS001"), eq("Instructor"));
    }
}
