package co.edu.unicauca.deporteParaTodos.configurations;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAutenticacionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IDeporteServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IFacultadServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IProgramaServicio;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DatosInvalidosExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Facultad;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Programa;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.FacultadRest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.ProgramaRest;
import co.edu.unicauca.deporteParaTodos.dominio.servicios.TokenServicio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.AutenticacionRest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.DeporteRest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.TokenInterchangeRest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {DeporteRest.class, AutenticacionRest.class, TokenInterchangeRest.class,
        FacultadRest.class, ProgramaRest.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://www.googleapis.com/oauth2/v3/certs"
})
class SecurityFilterChainTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockBean
    private IDeporteServicio servicioDeporte;

    @MockBean
    private IAutenticacionServicio servicioAutenticacion;

    @MockBean
    private TokenServicio tokenServicio;

    @MockBean
    private IFacultadServicio servicioFacultad;

    @MockBean
    private IProgramaServicio servicioPrograma;

    // ── Test 1: endpoint protegido sin token → 401 de Spring Security ────────

    @Test
    void getEndpointProtegido_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/v2/deportes"))
                .andExpect(status().isUnauthorized());
    }

    // ── Test 2: endpoint protegido con dpt_token válido → Spring Security lo deja pasar ──

    @Test
    void getEndpointProtegido_conDptTokenValido_retorna200() throws Exception {
        when(servicioDeporte.listaDeportes()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/deportes")
                        .header("Authorization", "Bearer " + generarTokenValido()))
                .andExpect(status().isOk());
    }

    // ── Test 3: POST /api/v2/RegistroPerfilAlumno sin token → NO 401 (endpoint público) ──

    @Test
    void postRegistroAlumno_sinToken_noEsBloqueadoPorSecurity() throws Exception {
        // Security permite el request (permitAll); la validacion del body devuelve 400,
        // pero nunca 401 — eso confirma que el matcher de permitAll funciona correctamente.
        mockMvc.perform(post("/api/v2/RegistroPerfilAlumno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(not(401)));
    }

    // ── Registro con datos que no corresponden al tipo de vínculo: 422 con código GC-0029 (antes era 500) ──

    @Test
    void postRegistroAlumno_datosInvalidosParaElTipo_retorna422ConSuMensaje() throws Exception {
        when(servicioAutenticacion.registrarAlumno(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new DatosInvalidosExcepcion("La dependencia es obligatoria para administrativos y contratistas"));

        mockMvc.perform(post("/api/v2/RegistroPerfilAlumno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"1\",\"nombre\":\"Ana\",\"correo\":\"ana@unicauca.edu.co\","
                                + "\"tipoId\":\"CC\",\"sexo\":\"F\",\"tipoAlumno\":\"Contratista\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigoError").value("GC-0029"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("La dependencia es obligatoria")));
    }

    // ── Catálogo de facultades y programas: público, lo necesita el formulario de registro ──
    // (quien se registra todavía no tiene dpt_token). Solo expone nombres de catálogo.

    @Test
    void getFacultades_sinToken_noEsBloqueadoPorSecurity() throws Exception {
        when(servicioFacultad.obtenerFacultades()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/facultades"))
                .andExpect(status().isOk());
    }

    @Test
    void getProgramasDeUnaFacultad_sinToken_retornaNombreYFacultad() throws Exception {
        when(servicioPrograma.obtenerProgramasPorFacultad("Facultad de Ingeniería Civil"))
                .thenReturn(List.of(new Programa("Ingeniería Civil", new Facultad("Facultad de Ingeniería Civil"))));

        mockMvc.perform(get("/api/v2/programas").param("facultad", "Facultad de Ingeniería Civil"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Ingeniería Civil"))
                .andExpect(jsonPath("$[0].facultad").value("Facultad de Ingeniería Civil"));
    }

    @Test
    void getProgramas_sinParametroFacultad_retorna400NoEs401() throws Exception {
        mockMvc.perform(get("/api/v2/programas"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otrosGetDelCatalogo_siguenProtegidos() throws Exception {
        mockMvc.perform(get("/api/v2/deportes"))
                .andExpect(status().isUnauthorized());
    }

    // ── Test 4: POST /api/v2/auth/token sin Google token → 401 del CONTROLLER, no de Security ──

    @Test
    void postAuthToken_sinGoogleToken_retorna401DelController_noDeSpringSecurityFilter() throws Exception {
        // tokenExchangeChain usa permitAll() — Spring Security no bloquea la ruta.
        // El 401 lo devuelve TokenInterchangeRest al detectar ausencia de Authorization header.
        // El 401 de Spring Security incluiría WWW-Authenticate; el del controller no lo incluye.
        mockMvc.perform(post("/api/v2/auth/token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String generarTokenValido() {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
