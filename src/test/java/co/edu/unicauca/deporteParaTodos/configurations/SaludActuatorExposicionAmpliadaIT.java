package co.edu.unicauca.deporteParaTodos.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

/**
 * Red de seguridad: aunque alguien exponga TODOS los endpoints de Actuator por error ("exposure.include=*"), la seguridad solo deja
 * pasar la salud. Ni siquiera una persona con un JWT valido (cualquier rol) puede leer /env, /beans, /heapdump, etc.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=" + SaludActuatorExposicionAmpliadaIT.SECRETO,
        "management.server.port=0",
        "management.endpoints.web.exposure.include=*"
})
class SaludActuatorExposicionAmpliadaIT {

    static final String SECRETO = "dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=";

    @LocalManagementPort
    private int puertoGestion;

    @Autowired
    private TestRestTemplate http;

    private ResponseEntity<String> get(String ruta, String jwt) {
        HttpHeaders cabeceras = new HttpHeaders();
        if (jwt != null) {
            cabeceras.setBearerAuth(jwt);
        }
        return http.exchange("http://127.0.0.1:" + puertoGestion + ruta, HttpMethod.GET, new HttpEntity<>(cabeceras), String.class);
    }

    private static String jwt(String rol) {
        SecretKey clave = new SecretKeySpec(Base64.getDecoder().decode(SECRETO), "HmacSHA256");
        NimbusJwtEncoder codificador = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(new OctetSequenceKey.Builder(clave).build())));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject("salud-it@it.unicauca.edu.co")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                .claim("rol", rol).claim("perf_id", "salud-it").build();
        return codificador.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Test
    void conTodoExpuesto_laSaludSigueRespondiendoSinToken() {
        ResponseEntity<String> r = get("/actuator/health", null);
        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("{\"status\":\"UP\"}", r.getBody());
    }

    @Test
    void conTodoExpuesto_nadaMasRespondeNiConJwtValidoDeCualquierRol() {
        for (String rol : List.of("Alumno", "Coordinador", "Administrador")) {
            String token = jwt(rol);
            for (String ruta : List.of("/actuator", "/actuator/env", "/actuator/beans", "/actuator/metrics", "/actuator/mappings",
                    "/actuator/configprops", "/actuator/threaddump", "/actuator/loggers")) {
                ResponseEntity<String> r = get(ruta, token);
                assertTrue(r.getStatusCode().value() == 403 || r.getStatusCode().value() == 401,
                        rol + " " + ruta + " -> " + r.getStatusCode());
            }
        }
    }
}
