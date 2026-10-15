package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

// SCRUM-80 (rediseno, bloque 1): el constructor con todos los argumentos
// solo se ejercita, en el resto de la suite, via el Gateway real (mapeo
// Entidad->Dominio) o via setters -- este test lo cubre directamente.
class ProrrogaRegistroAsistenciaTest {

    @Test
    void constructorConTodosLosArgumentos_dejaCadaCampoAsignado() {
        LocalDate fechaLimite = LocalDate.of(2026, 11, 10);
        LocalDateTime fechaAsignacion = LocalDateTime.of(2026, 10, 12, 9, 0);

        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia(
                "ins1", 2026, 10, 10, fechaLimite, "coord1", fechaAsignacion, 0);

        assertEquals("ins1", prorroga.getIdInstructor());
        assertEquals(2026, prorroga.getAnio());
        assertEquals(10, prorroga.getMes());
        assertEquals(10, prorroga.getDias());
        assertEquals(fechaLimite, prorroga.getFechaLimite());
        assertEquals("coord1", prorroga.getIdCoordinador());
        assertEquals(fechaAsignacion, prorroga.getFechaAsignacion());
        assertEquals(0, prorroga.getEliminado());
    }
}
