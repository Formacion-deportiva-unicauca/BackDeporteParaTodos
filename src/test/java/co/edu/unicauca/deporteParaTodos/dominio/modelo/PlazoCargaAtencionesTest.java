package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

// SCRUM-80 (HE-14 HU-02): el campo eliminado y el constructor con todos los
// argumentos solo se ejercitan, en el resto de la suite, a traves del Gateway
// real (PlazoCargaAtencionesGateway.mapearEntidadADominio) -- que requiere
// MySQL real y no corre aqui (ver PlazoCargaAtencionesIT). Este test cubre
// ambos directamente, sin depender de una base de datos.
class PlazoCargaAtencionesTest {

    @Test
    void constructorConTodosLosArgumentos_dejaCadaCampoAsignado() {
        LocalDate fechaLimite = LocalDate.of(2026, 12, 1);
        LocalDateTime fechaAsignacion = LocalDateTime.of(2026, 10, 12, 9, 30);

        PlazoCargaAtenciones plazo = new PlazoCargaAtenciones(
                "ins1", 2026, 2, fechaLimite, "coord1", fechaAsignacion, 0);

        assertEquals("ins1", plazo.getIdInstructor());
        assertEquals(2026, plazo.getAnio());
        assertEquals(2, plazo.getPeriodo());
        assertEquals(fechaLimite, plazo.getFechaLimite());
        assertEquals("coord1", plazo.getIdCoordinador());
        assertEquals(fechaAsignacion, plazo.getFechaAsignacion());
        assertEquals(0, plazo.getEliminado());
    }

    @Test
    void setEliminado_quedaDisponibleEnGetEliminado() {
        PlazoCargaAtenciones plazo = new PlazoCargaAtenciones();
        plazo.setEliminado(1);

        assertEquals(1, plazo.getEliminado());
    }
}
