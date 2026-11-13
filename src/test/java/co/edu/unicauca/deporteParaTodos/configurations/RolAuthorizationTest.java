package co.edu.unicauca.deporteParaTodos.configurations;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAsistenciaServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAutenticacionServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.ICategoriaCursoServicio;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IEstadisticaServicio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.AtencionRest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.AutenticacionRest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.CategoriaRest;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api.EstadisticasRest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CategoriaRest.class, AtencionRest.class, EstadisticasRest.class, AutenticacionRest.class})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://www.googleapis.com/oauth2/v3/certs"
})
class RolAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtEncoder jwtEncoder;

    @MockBean private ICategoriaCursoServicio servicioCategoria;
    @MockBean private IAsistenciaServicio servicioAsistencia;
    @MockBean private IEstadisticaServicio servEstadistica;
    @MockBean private IAutenticacionServicio servicioAutenticacion;

    // ── Tier: Solo Coordinador ────────────────────────────────────────────────

    @Test
    void postCategoria_conRolAlumno_retorna403() throws Exception {
        mockMvc.perform(post("/api/v2/categoria")
                        .header("Authorization", "Bearer " + generarTokenConRol("Alumno"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Test\",\"descripcion\":\"Test\",\"imagenId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postCategoria_conRolInstructor_retorna403() throws Exception {
        mockMvc.perform(post("/api/v2/categoria")
                        .header("Authorization", "Bearer " + generarTokenConRol("Instructor"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Test\",\"descripcion\":\"Test\",\"imagenId\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postCategoria_conRolCoordinador_noEsForbidden() throws Exception {
        // Body vacio -> 400 por Bean Validation, pero no 403: Spring Security deja pasar
        mockMvc.perform(post("/api/v2/categoria")
                        .header("Authorization", "Bearer " + generarTokenConRol("Coordinador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is(not(403)));
    }

    @Test
    void getEstadisticasCategorias_conRolAlumno_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/estadisticas/categorias")
                        .header("Authorization", "Bearer " + generarTokenConRol("Alumno"))
                        .param("inicio", "2025-01-01")
                        .param("fin", "2025-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getEstadisticasCategorias_conRolCoordinador_retorna200() throws Exception {
        when(servEstadistica.estadisticasCategorias(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/estadisticas/categorias")
                        .header("Authorization", "Bearer " + generarTokenConRol("Coordinador"))
                        .param("inicio", "2025-01-01")
                        .param("fin", "2025-12-31"))
                .andExpect(status().isOk());
    }

    // ── Tier: Instructor O Coordinador ────────────────────────────────────────

    @Test
    void postAtenciones_conRolAlumno_retorna403() throws Exception {
        mockMvc.perform(post("/api/v2/atenciones")
                        .header("Authorization", "Bearer " + generarTokenConRol("Alumno"))
                        .param("idClase", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    void postAtenciones_conRolInstructor_noEsForbidden() throws Exception {
        mockMvc.perform(post("/api/v2/atenciones")
                        .header("Authorization", "Bearer " + generarTokenConRol("Instructor"))
                        .param("idClase", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().is(not(403)));
    }

    @Test
    void postAtenciones_conRolCoordinador_noEsForbidden() throws Exception {
        mockMvc.perform(post("/api/v2/atenciones")
                        .header("Authorization", "Bearer " + generarTokenConRol("Coordinador"))
                        .param("idClase", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]"))
                .andExpect(status().is(not(403)));
    }

    // ── Tier: Cualquier autenticado ───────────────────────────────────────────

    @Test
    void getCategorias_conRolAlumno_retorna200() throws Exception {
        when(servicioCategoria.recuperarCategoriasCurso()).thenReturn(List.of());

        mockMvc.perform(get("/api/v2/categorias2")
                        .header("Authorization", "Bearer " + generarTokenConRol("Alumno")))
                .andExpect(status().isOk());
    }

    // ── Tier: GET /login -- Alumno solo su propio correo; staff, cualquiera ──

    @Test
    void getLogin_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/v2/login").param("email", "alguien@unicauca.edu.co"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLogin_rolDesconocido_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/login")
                        .param("email", "alguien@unicauca.edu.co")
                        .header("Authorization", "Bearer " + generarTokenConRol("Visitante")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLogin_alumnoConsultaSuPropioCorreo_retorna200() throws Exception {
        when(servicioAutenticacion.login("yo@unicauca.edu.co")).thenReturn(new PerfilDto());

        mockMvc.perform(get("/api/v2/login")
                        .param("email", "yo@unicauca.edu.co")
                        .header("Authorization", "Bearer " + generarTokenConRolYCorreo("Alumno", "yo@unicauca.edu.co")))
                .andExpect(status().isOk());
    }

    @Test
    void getLogin_alumnoConsultaSuPropioCorreoConOtrasMayusculas_retorna200() throws Exception {
        when(servicioAutenticacion.login("Yo@Unicauca.edu.co")).thenReturn(new PerfilDto());

        mockMvc.perform(get("/api/v2/login")
                        .param("email", "Yo@Unicauca.edu.co")
                        .header("Authorization", "Bearer " + generarTokenConRolYCorreo("Alumno", "yo@unicauca.edu.co")))
                .andExpect(status().isOk());
    }

    @Test
    void getLogin_alumnoConsultaOtroCorreo_retorna403() throws Exception {
        mockMvc.perform(get("/api/v2/login")
                        .param("email", "otro@unicauca.edu.co")
                        .header("Authorization", "Bearer " + generarTokenConRolYCorreo("Alumno", "yo@unicauca.edu.co")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLogin_coordinadorConsultaOtroCorreo_retorna200() throws Exception {
        when(servicioAutenticacion.login("otro@unicauca.edu.co")).thenReturn(new PerfilDto());

        mockMvc.perform(get("/api/v2/login")
                        .param("email", "otro@unicauca.edu.co")
                        .header("Authorization", "Bearer " + generarTokenConRolYCorreo("Coordinador", "coord@unicauca.edu.co")))
                .andExpect(status().isOk());
    }

    @Test
    void getLogin_instructorConsultaOtroCorreo_retorna200() throws Exception {
        when(servicioAutenticacion.login("otro@unicauca.edu.co")).thenReturn(new PerfilDto());

        mockMvc.perform(get("/api/v2/login")
                        .param("email", "otro@unicauca.edu.co")
                        .header("Authorization", "Bearer " + generarTokenConRolYCorreo("Instructor", "inst@unicauca.edu.co")))
                .andExpect(status().isOk());
    }

    @Test
    void getLogin_administradorConsultaOtroCorreo_retorna200() throws Exception {
        when(servicioAutenticacion.login("otro@unicauca.edu.co")).thenReturn(new PerfilDto());

        mockMvc.perform(get("/api/v2/login")
                        .param("email", "otro@unicauca.edu.co")
                        .header("Authorization", "Bearer " + generarTokenConRolYCorreo("Administrador", "admin@unicauca.edu.co")))
                .andExpect(status().isOk());
    }

    // ─────────────────────────────────────────────────────────────────────────

    private String generarTokenConRol(String rol) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("test-user@test.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", rol)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String generarTokenConRolYCorreo(String rol, String correo) {
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(correo)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("rol", rol)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
