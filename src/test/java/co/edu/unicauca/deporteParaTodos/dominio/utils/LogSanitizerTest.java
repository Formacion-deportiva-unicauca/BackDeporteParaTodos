package co.edu.unicauca.deporteParaTodos.dominio.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

// Sonar java:S5145 (log forging): LogSanitizer.sanitizar() es el unico punto
// usado por InstructorServicio y los controladores (GrupoRest, InstructoresRest)
// para limpiar CR/LF de cualquier valor del cliente antes de pasarlo a un logger.
class LogSanitizerTest {

    @Test
    void sanitizar_null_retornaNull() {
        assertNull(LogSanitizer.sanitizar(null));
    }

    @Test
    void sanitizar_sinSaltos_retornaIgual() {
        assertEquals("instructor-123", LogSanitizer.sanitizar("instructor-123"));
    }

    @Test
    void sanitizar_soloCR_reemplazaPorGuionBajo() {
        assertEquals("abc_def", LogSanitizer.sanitizar("abc\rdef"));
    }

    @Test
    void sanitizar_soloLF_reemplazaPorGuionBajo() {
        assertEquals("abc_def", LogSanitizer.sanitizar("abc\ndef"));
    }

    @Test
    void sanitizar_crlf_reemplazaAmbosCaracteres() {
        // CR y LF se reemplazan cada uno por su propio "_" -- CRLF produce "__".
        assertEquals("abc__def", LogSanitizer.sanitizar("abc\r\ndef"));
    }

    @Test
    void sanitizar_variosSaltosMezclados_reemplazaTodos() {
        assertEquals("a_b_c__d_", LogSanitizer.sanitizar("a\rb\nc\r\nd\n"));
    }
}
