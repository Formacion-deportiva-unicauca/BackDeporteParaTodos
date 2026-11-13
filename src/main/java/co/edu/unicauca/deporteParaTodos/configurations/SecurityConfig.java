package co.edu.unicauca.deporteParaTodos.configurations;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger DIAG_LOG = LoggerFactory.getLogger(SecurityConfig.class);

    // HS256 con clave < 32 bytes es criptograficamente debil (NIST SP 800-117 / RFC 2104
    // recomiendan clave >= tamano del hash). Verificado despues de decodificar Base64: lo que
    // importa es la entropia real, no la longitud del texto de app.jwt.secret.
    private static final int LONGITUD_MINIMA_CLAVE_JWT_BYTES = 32;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String googleJwksUri;

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // client_id de la app registrada en Google (publico, no es secreto). Sin valor por
    // defecto: si falta GOOGLE_CLIENT_ID, el arranque falla igual que con app.jwt.secret.
    @Value("${app.google.client-id}")
    private String googleClientId;

    // Clave HS256 ya decodificada y validada -- unica fuente de verdad, reutilizada por
    // systemJwtDecoder() y jwtEncoder() para no decodificar/validar dos veces.
    private SecretKey systemJwtKey;

    // Decodifica y valida app.jwt.secret una sola vez al arrancar. Si la clave resultante
    // tiene menos de 32 bytes, el arranque falla con un mensaje claro que NUNCA incluye el
    // valor de la clave (solo su longitud).
    @PostConstruct
    public void validarClaveJwt() {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(jwtSecret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "app.jwt.secret no es un valor Base64 valido. Genera uno nuevo con: "
                            + "openssl rand -base64 32");
        }
        if (keyBytes.length < LONGITUD_MINIMA_CLAVE_JWT_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret es demasiado corta: tiene " + keyBytes.length
                            + " bytes tras decodificar desde Base64, se requieren al menos "
                            + LONGITUD_MINIMA_CLAVE_JWT_BYTES
                            + ". Genera una nueva con: openssl rand -base64 32 (o mayor)");
        }
        systemJwtKey = new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // Chain 1 (Orden 1): solo /auth/token — recibe Google ID Token y lo decodifica manualmente
    // en el controller. BearerTokenAuthenticationFilter NO debe interceptar esta ruta porque
    // el token de Google no puede validarse con el decoder HS256 del sistema.
    @Bean
    @Order(1)
    public SecurityFilterChain tokenExchangeChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v2/auth/token")
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable) // NOSONAR: jwt-stateless-no-csrf-risk
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            // Sin oauth2ResourceServer: el controller decodifica el Google token manualmente
        return http.build();
    }

    // Chain 2 (Orden 2): todos los demas endpoints — valida dpt_token (HS256/JWT_SECRET).
    // SCRUM-156 Fase 1c: POST /api/v2/RegistroPerfilAlumno es publico (SCRUM-160), igual que el catalogo
    // de solo lectura GET /api/v2/facultades y /api/v2/programas que alimenta ese formulario.
    // Todo lo demas exige autenticacion con dpt_token valido.
    @Bean
    @Order(2)
    public SecurityFilterChain mainChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            // CSRF deshabilitado intencionalmente: API stateless con JWT en Authorization header.
            // El riesgo CSRF aplica solo a flujos basados en cookies; aqui no se usan cookies de sesion
            // (SessionCreationPolicy.STATELESS) y CORS esta restringido a origenes explicitamente permitidos.
            // Referencia: https://docs.spring.io/spring-security/reference/features/exploits/csrf.html
            .csrf(AbstractHttpConfigurer::disable) // NOSONAR: jwt-stateless-no-csrf-risk
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v2/RegistroPerfilAlumno").permitAll()
                // Catalogo de facultades y programas: lo necesita el formulario de registro, antes del dpt_token.
                .requestMatchers(HttpMethod.GET, "/api/v2/facultades", "/api/v2/programas").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt
                .decoder(systemJwtDecoder())
                .jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    // Decodifica tokens Google ID (RS256, valida firma via JWKS publico de Google).
    // Usado exclusivamente por TokenInterchangeRest para validar el token entrante de Google.
    // Ademas de la firma, valida (via setJwtValidator): exp/nbf (JwtValidators.createDefault(),
    // sin esto no se pierde nada de lo que ya habia) + aud (debe contener nuestro client_id) +
    // iss (debe ser Google, en cualquiera de sus dos formas) -- ver GoogleJwtClaimsValidator.
    @Bean("googleJwtDecoder")
    public JwtDecoder googleJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(googleJwksUri).build();
        OAuth2TokenValidator<Jwt> validadorPorDefecto = JwtValidators.createDefault();
        OAuth2TokenValidator<Jwt> validadorClaims = new GoogleJwtClaimsValidator(googleClientId);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validadorPorDefecto, validadorClaims));
        return decoder;
    }

    // Decodifica los JWT propios del sistema (HS256, firmados con JWT_SECRET).
    // Usado por mainChain para validar el dpt_token que emite TokenServicio.
    @Bean("systemJwtDecoder")
    public JwtDecoder systemJwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(systemJwtKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    // Mapea el claim "rol" del JWT propio del sistema a GrantedAuthority de Spring Security.
    // Converter completamente custom: lee jwt.getClaimAsString("rol") directamente,
    // evita cualquier split interno de JwtGrantedAuthoritiesConverter y registra [DIAG]
    // para confirmar en vivo el valor del claim y la autoridad resultante.
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String rol = jwt.getClaimAsString("rol");
            DIAG_LOG.info("[DIAG] JWT sub={} | claim 'rol'='{}' | authorities=[{}]",
                    jwt.getSubject(), rol, rol != null ? rol : "<null>");
            if (rol == null || rol.isBlank()) {
                return java.util.List.of();
            }
            return java.util.List.of(new SimpleGrantedAuthority(rol));
        });
        return converter;
    }

    // Firma los JWT propios del sistema con HMAC-SHA256
    @Bean
    public JwtEncoder jwtEncoder() {
        OctetSequenceKey jwk = new OctetSequenceKey.Builder(systemJwtKey).build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    // Unica fuente de verdad para CORS en toda la aplicacion.
    // webConfig.java fue eliminado: Spring Security intercepta requests antes que el
    // DispatcherServlet, por lo que el CorsFilter de seguridad debe tener su propia
    // CorsConfigurationSource -- delegar a WebMvcConfigurer.addCorsMappings() no es confiable.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        // JWT viaja en Authorization header, no en cookies — credentials no requeridas
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
