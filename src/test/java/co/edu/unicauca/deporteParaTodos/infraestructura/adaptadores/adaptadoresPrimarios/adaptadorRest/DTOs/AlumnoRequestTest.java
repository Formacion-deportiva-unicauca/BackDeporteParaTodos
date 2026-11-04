package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlumnoRequestTest {

    private static ValidatorFactory fabrica;
    private static Validator validador;

    @BeforeAll
    static void crearValidador() {
        fabrica = Validation.buildDefaultValidatorFactory();
        validador = fabrica.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        fabrica.close();
    }

    private AlumnoRequest request(String tipo) {
        return new AlumnoRequest("Juan García", "juan@unicauca.edu.co", tipo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Estudiante", "Docente", "Administrativo", "Contratista"})
    void tipoAlumno_losCuatroTiposDeVinculo_sonValidos(String tipo) {
        assertTrue(validador.validate(request(tipo)).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Regular", "contratista", "Otro"})
    void tipoAlumno_valorDesconocido_esInvalido(String tipo) {
        assertFalse(validador.validate(request(tipo)).isEmpty());
    }

    @Test
    void tipoAlumno_vacio_esInvalido() {
        assertFalse(validador.validate(request("")).isEmpty());
    }
}
