package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// SCRUM-80 (HE-14 HU-02): funcion pura, sin Spring -- el estado del reporte de
// carga de atenciones de un instructor para el periodo vigente.
class EstadoReporteTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 12);

    @Test
    void calcular_sinFechaYSinClases_retornaSinPlazo() {
        assertEquals(EstadoReporte.SIN_PLAZO, EstadoReporte.calcular(null, 0, HOY));
    }

    @Test
    void calcular_diaLimiteExacto_esInclusivoYRetornaPendiente() {
        assertEquals(EstadoReporte.PENDIENTE, EstadoReporte.calcular(HOY, 0, HOY));
    }

    @Test
    void calcular_fechaLimiteFutura_retornaPendiente() {
        assertEquals(EstadoReporte.PENDIENTE, EstadoReporte.calcular(HOY.plusDays(1), 0, HOY));
    }

    @Test
    void calcular_fechaLimitePasada_retornaVencido() {
        assertEquals(EstadoReporte.VENCIDO, EstadoReporte.calcular(HOY.minusDays(1), 0, HOY));
    }

    @Test
    void calcular_conClasesYSinFecha_retornaGenerado() {
        assertEquals(EstadoReporte.GENERADO, EstadoReporte.calcular(null, 3, HOY));
    }

    @Test
    void calcular_conClasesYFechaVencida_retornaGeneradoIgual() {
        // GENERADO prevalece sobre VENCIDO: el instructor si reporto.
        assertEquals(EstadoReporte.GENERADO, EstadoReporte.calcular(HOY.minusDays(1), 1, HOY));
    }

    @Test
    void plazoVencido_sinFecha_esFalso() {
        assertFalse(EstadoReporte.plazoVencido(null, HOY));
    }

    @Test
    void plazoVencido_diaLimiteExacto_esFalso() {
        assertFalse(EstadoReporte.plazoVencido(HOY, HOY));
    }

    @Test
    void plazoVencido_fechaPasada_esVerdaderoAunConClasesGeneradoRetornado() {
        // plazoVencido es independiente del estado: GENERADO + plazo vencido -> true.
        assertTrue(EstadoReporte.plazoVencido(HOY.minusDays(1), HOY));
        assertEquals(EstadoReporte.GENERADO, EstadoReporte.calcular(HOY.minusDays(1), 2, HOY));
    }

    @Test
    void plazoVencido_fechaFutura_esFalso() {
        assertFalse(EstadoReporte.plazoVencido(HOY.plusDays(1), HOY));
    }
}
