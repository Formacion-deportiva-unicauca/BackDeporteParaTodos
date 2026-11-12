package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.api;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosEntrada.IAutenticacionServicio;
import co.edu.unicauca.deporteParaTodos.dominio.servicios.TokenServicio;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.PerfilDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.controladorExcepciones.RestExceptionHandler;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TokenInterchangeRestTest {

    @Mock private JwtDecoder googleDecoder;
    @Mock private IAutenticacionServicio autenticacionServicio;
    @Mock private TokenServicio tokenServicio;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TokenInterchangeRest controller = new TokenInterchangeRest(googleDecoder, autenticacionServicio, tokenServicio);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new RestExceptionHandler())
                .build();
    }

    // --- Casos de rechazo 401 ---

    @Test
    void sinHeaderAuthorization_retorna401() throws Exception {
        // Bug corregido: required=false permite que el metodo se ejecute y devuelva 401
        // En vez de MissingRequestHeaderException (500 sin handler) que ocurria antes
        mockMvc.perform(post("/api/v2/auth/token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void headerSinPrefijoBEarer_retorna401() throws Exception {
        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenGoogleInvalido_retorna401() throws Exception {
        when(googleDecoder.decode("token-invalido")).thenThrow(new JwtException("firma invalida"));

        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailAusenteEnToken_retorna401() throws Exception {
        Jwt jwtSinEmail = Jwt.withTokenValue("google-token")
                .header("alg", "RS256")
                .claim("sub", "google-uid-sin-email")
                .build();
        when(googleDecoder.decode("google-token")).thenReturn(jwtSinEmail);

        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isUnauthorized());
    }

    // --- Correo no registrado: NoExisteExcepcion -> RestExceptionHandler -> 404 ---

    @Test
    void correoNoRegistrado_retorna404() throws Exception {
        Jwt googleJwt = Jwt.withTokenValue("google-token")
                .header("alg", "RS256")
                .claim("email", "desconocido@externo.com")
                .claim("email_verified", true)
                .build();
        when(googleDecoder.decode("google-token")).thenReturn(googleJwt);
        when(autenticacionServicio.login("desconocido@externo.com"))
                .thenThrow(new NoExisteExcepcion("desconocido@externo.com"));

        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigoError").value("GC-0003"))
                .andExpect(jsonPath("$.codigoHttp").value(404));
    }

    // --- email_verified ---

    @Test
    void emailVerificadoFalse_retorna401() throws Exception {
        Jwt googleJwt = Jwt.withTokenValue("google-token")
                .header("alg", "RS256")
                .claim("email", "alumno@unicauca.edu.co")
                .claim("email_verified", false)
                .build();
        when(googleDecoder.decode("google-token")).thenReturn(googleJwt);

        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isUnauthorized());
        verify(autenticacionServicio, never()).login(any());
    }

    @Test
    void emailVerificadoAusente_retorna401() throws Exception {
        Jwt googleJwt = Jwt.withTokenValue("google-token")
                .header("alg", "RS256")
                .claim("email", "alumno@unicauca.edu.co")
                .build();
        when(googleDecoder.decode("google-token")).thenReturn(googleJwt);

        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isUnauthorized());
        verify(autenticacionServicio, never()).login(any());
    }

    @Test
    void emailVerificadoComoStringTrue_continua() throws Exception {
        Jwt googleJwt = Jwt.withTokenValue("google-token")
                .header("alg", "RS256")
                .claim("email", "alumno@unicauca.edu.co")
                .claim("email_verified", "true")
                .build();
        when(googleDecoder.decode("google-token")).thenReturn(googleJwt);
        when(autenticacionServicio.login("alumno@unicauca.edu.co"))
                .thenThrow(new NoExisteExcepcion("alumno@unicauca.edu.co"));

        // Si llega a 404 (y no a 401) es porque email_verified="true" (String) si fue aceptado.
        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isNotFound());
    }

    // --- Happy path ---

    @Test
    void intercambio_exitoso_retorna200ConTokenYPerfil() throws Exception {
        Jwt googleJwt = Jwt.withTokenValue("google-token")
                .header("alg", "RS256")
                .claim("email", "alumno@unicauca.edu.co")
                .claim("email_verified", true)
                .issuedAt(Instant.now())
                .build();
        when(googleDecoder.decode("google-token")).thenReturn(googleJwt);

        PerfilDto perfil = new PerfilDto();
        perfil.setId("12345678");
        perfil.setCorreo("alumno@unicauca.edu.co");
        perfil.setRole("Estudiante");
        when(autenticacionServicio.login("alumno@unicauca.edu.co")).thenReturn(perfil);
        when(tokenServicio.emitirToken(any(PerfilDto.class))).thenReturn("mi-jwt-sistema");

        mockMvc.perform(post("/api/v2/auth/token")
                        .header("Authorization", "Bearer google-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mi-jwt-sistema"))
                .andExpect(jsonPath("$.perfil.correo").value("alumno@unicauca.edu.co"));
    }
}
