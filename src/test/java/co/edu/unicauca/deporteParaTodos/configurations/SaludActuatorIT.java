package co.edu.unicauca.deporteParaTodos.configurations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Salud de la aplicacion para el HEALTHCHECK de la imagen (test de integracion, BD MySQL real, servidor real con puerto aleatorio).
 *
 * Solo el endpoint "health" responde, en el puerto de gestion (en produccion el 8083, en loopback y sin publicar); no lleva detalles
 * ni datos sensibles; el resto de Actuator no responde; y en el puerto de la API la salud sigue exigiendo JWT. El puerto de
 * gestion se pone en 0 solo en la prueba (aleatorio) para no chocar con otras ejecuciones.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.jwt.secret=dGVzdFNlY3JldEtleUZvckNJMTIzNDU2Nzg5MDEyMzQ1Njc4OTA=",
        "management.server.port=0"
})
class SaludActuatorIT {

    @LocalManagementPort
    private int puertoGestion;

    @LocalServerPort
    private int puertoApi;

    @Autowired
    private TestRestTemplate http;

    private ResponseEntity<String> get(int puerto, String ruta) {
        return http.getForEntity("http://127.0.0.1:" + puerto + ruta, String.class);
    }

    @Test
    void lasaludResponde200UpSinDetalles_yUsaUnPuertoDistintoAlDeLaApi() {
        assertNotEquals(puertoApi, puertoGestion, "la gestion va en su propio puerto");

        ResponseEntity<String> r = get(puertoGestion, "/actuator/health");

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertEquals("{\"status\":\"UP\"}", r.getBody(), "solo el estado: sin detalles ni componentes");
        assertFalse(r.getBody().contains("components") || r.getBody().contains("details"));
    }

    @Test
    void elDetalleDeLosComponentesNoSeExpone() {
        for (String ruta : List.of("/actuator/health/db", "/actuator/health/ping", "/actuator/health/diskSpace")) {
            ResponseEntity<String> r = get(puertoGestion, ruta);
            assertTrue(r.getStatusCode().value() == 404 || r.getStatusCode().value() == 401, ruta + " -> " + r.getStatusCode());
        }
    }

    @Test
    void ningunOtroEndpointDeActuatorRespondeEnElPuertoDeGestion() {
        for (String ruta : List.of("/actuator", "/actuator/env", "/actuator/beans", "/actuator/info", "/actuator/metrics",
                "/actuator/mappings", "/actuator/configprops", "/actuator/heapdump", "/actuator/loggers", "/actuator/shutdown",
                "/actuator/threaddump", "/actuator/httpexchanges")) {
            ResponseEntity<String> r = get(puertoGestion, ruta);
            assertFalse(r.getStatusCode().is2xxSuccessful(), ruta + " no debe responder con datos: " + r.getStatusCode());
        }
    }

    @Test
    void enElPuertoDeLaApiLaSaludSigueExigiendoJwt_yNingunActuatorSeExpone() {
        for (String ruta : List.of("/actuator", "/actuator/health", "/actuator/env", "/actuator/beans")) {
            ResponseEntity<String> r = get(puertoApi, ruta);
            assertEquals(HttpStatus.UNAUTHORIZED, r.getStatusCode(), ruta);
        }
    }

    @Test
    void lasRutasPublicasYProtegidasDeLaApiNoCambian() {
        // Protegida: sin token, 401.
        assertEquals(HttpStatus.UNAUTHORIZED, get(puertoApi, "/api/v2/cursos").getStatusCode());
        // Publicas (catalogo de registro): no piden token.
        assertEquals(HttpStatus.OK, get(puertoApi, "/api/v2/facultades").getStatusCode());
        // La API no responde en el puerto de gestion.
        assertFalse(get(puertoGestion, "/api/v2/facultades").getStatusCode().is2xxSuccessful());
    }
}
