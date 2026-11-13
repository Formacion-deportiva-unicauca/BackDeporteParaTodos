package co.edu.unicauca.deporteParaTodos.configurations;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

import static org.assertj.core.api.Assertions.assertThat;

// Valida que SecurityConfig rechace al arrancar una app.jwt.secret cuyos bytes
// decodificados (Base64) sean menores al minimo de 32 bytes para HS256, sin exponer
// el valor de la clave en el mensaje de error. ApplicationContextRunner arranca
// SecurityConfig de forma aislada (sin el resto del contexto de la aplicacion),
// que es suficiente porque la validacion vive en su @PostConstruct.
class JwtSecretValidationTest {

    private static final String PROPIEDADES_BASE_SECURITY_CONFIG =
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://www.googleapis.com/oauth2/v3/certs";

    // mainChain() usa MvcRequestMatcher (via .requestMatchers("/swagger-ui/**", ...)), que
    // exige un bean HandlerMappingIntrospector en el contexto -- normalmente lo aporta
    // WebMvcAutoConfiguration, ausente aqui porque ApplicationContextRunner no arranca
    // autoconfiguracion de Spring Boot. Se aporta manualmente, solo para que el contexto
    // pueda construirse; no participa en la validacion que este test verifica.
    @Configuration
    static class MvcIntrospectorConfig {
        @Bean
        public HandlerMappingIntrospector mvcHandlerMappingIntrospector() {
            return new HandlerMappingIntrospector();
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class, MvcIntrospectorConfig.class)
            .withPropertyValues(
                    PROPIEDADES_BASE_SECURITY_CONFIG,
                    "app.google.client-id=test-client-id");

    @Test
    void claveMenorA32BytesTrasDecodificar_fallaAlArrancar() {
        // "MTIzNDU2Nzg5MDEyMzQ1Ng==" decodifica a 16 bytes (< 32)
        contextRunner
                .withPropertyValues("app.jwt.secret=MTIzNDU2Nzg5MDEyMzQ1Ng==")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasRootCauseMessage(
                                    "app.jwt.secret es demasiado corta: tiene 16 bytes tras "
                                            + "decodificar desde Base64, se requieren al menos 32. "
                                            + "Genera una nueva con: openssl rand -base64 32 (o mayor)");
                });
    }

    @Test
    void claveDe32OMasBytesTrasDecodificar_arrancaSinErrores() {
        // Secreto de prueba usado en el resto de la suite: decodifica a 38 bytes (>= 32)
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void claveDeExactamente32Bytes_arrancaSinErrores() {
        // "MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=" decodifica a exactamente 32 bytes
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=MTIzNDU2Nzg5MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTI=")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
