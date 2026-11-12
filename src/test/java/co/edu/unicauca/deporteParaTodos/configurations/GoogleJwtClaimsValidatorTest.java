package co.edu.unicauca.deporteParaTodos.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleJwtClaimsValidatorTest {

    private static final String CLIENT_ID = "963344430854-4cvodrre2ssir1n7t1l2odpj5rakd4hj.apps.googleusercontent.com";

    private final GoogleJwtClaimsValidator validador = new GoogleJwtClaimsValidator(CLIENT_ID);

    private Jwt jwtConAudIss(Object aud, String iss) {
        Jwt.Builder builder = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "google-uid");
        if (aud != null) {
            builder.claim("aud", aud);
        }
        if (iss != null) {
            builder.claim("iss", iss);
        }
        return builder.build();
    }

    @Test
    void audCorrectoComoLista_issHttps_valida() {
        Jwt jwt = jwtConAudIss(List.of(CLIENT_ID), "https://accounts.google.com");
        assertTrue(validador.validate(jwt).hasErrors() == false);
    }

    @Test
    void audCorrectoComoStringUnico_issSinEsquema_valida() {
        Jwt jwt = jwtConAudIss(CLIENT_ID, "accounts.google.com");
        OAuth2TokenValidatorResult resultado = validador.validate(jwt);
        assertFalse(resultado.hasErrors(), "iss sin esquema (accounts.google.com) tambien debe aceptarse");
    }

    @Test
    void audAusente_falla() {
        Jwt jwt = jwtConAudIss(null, "https://accounts.google.com");
        assertTrue(validador.validate(jwt).hasErrors());
    }

    @Test
    void audDeOtraApp_falla() {
        Jwt jwt = jwtConAudIss(List.of("otra-app.apps.googleusercontent.com"), "https://accounts.google.com");
        assertTrue(validador.validate(jwt).hasErrors());
    }

    @Test
    void issIncorrecto_falla() {
        Jwt jwt = jwtConAudIss(List.of(CLIENT_ID), "https://evil.example.com");
        assertTrue(validador.validate(jwt).hasErrors());
    }

    @Test
    void issAusente_falla() {
        Jwt jwt = jwtConAudIss(List.of(CLIENT_ID), null);
        assertTrue(validador.validate(jwt).hasErrors());
    }

    // Verifica que combinar con JwtValidators.createDefault() (como hace SecurityConfig)
    // no pierde la validacion de exp/nbf ya existente.
    @Test
    void combinadoConValidadorPorDefecto_tokenExpirado_falla() {
        OAuth2TokenValidator<Jwt> combinado = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), validador);

        Jwt expirado = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "google-uid")
                .claim("aud", List.of(CLIENT_ID))
                .claim("iss", "https://accounts.google.com")
                .issuedAt(Instant.now().minusSeconds(7200))
                .expiresAt(Instant.now().minusSeconds(3600))
                .build();

        assertTrue(combinado.validate(expirado).hasErrors(),
                "un token con exp en el pasado debe seguir siendo rechazado");
    }

    @Test
    void combinadoConValidadorPorDefecto_tokenValido_pasa() {
        OAuth2TokenValidator<Jwt> combinado = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), validador);

        Jwt valido = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "google-uid")
                .claim("aud", List.of(CLIENT_ID))
                .claim("iss", "https://accounts.google.com")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        assertFalse(combinado.validate(valido).hasErrors());
    }
}
