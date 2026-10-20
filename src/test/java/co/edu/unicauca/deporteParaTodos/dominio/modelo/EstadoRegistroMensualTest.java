package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// SCRUM-80 (rediseno, bloque 3 de 4): regla pura del estado del registro de
// asistencia de un instructor para un mes calendario.
class EstadoRegistroMensualTest {

    // ── grupoActivoEnMes ──────────────────────────────────────────────────

    @Test
    void grupoActivoEnMes_fechasNull_estaActivoEnCualquierMes() {
        assertTrue(EstadoRegistroMensual.grupoActivoEnMes(null, null, 2026, 10));
        assertTrue(EstadoRegistroMensual.grupoActivoEnMes(null, null, 2025, 1));
    }

    @Test
    void grupoActivoEnMes_periodoSolapaElMes_estaActivo() {
        // Periodo: 2026-08-01 .. 2026-12-31 -> activo en octubre.
        assertTrue(EstadoRegistroMensual.grupoActivoEnMes(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 31), 2026, 10));
    }

    @Test
    void grupoActivoEnMes_limitesDelPeriodoInclusive_estaActivo() {
        // fechas exactamente en el primer y ultimo dia del mes -> activo (inclusive).
        assertTrue(EstadoRegistroMensual.grupoActivoEnMes(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 2026, 10));
        assertTrue(EstadoRegistroMensual.grupoActivoEnMes(
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31), 2026, 10));
    }

    @Test
    void grupoActivoEnMes_periodoTerminaAntesDelMes_noEstaActivo() {
        // Termina el 2026-09-30 -> no activo en octubre.
        assertFalse(EstadoRegistroMensual.grupoActivoEnMes(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30), 2026, 10));
    }

    @Test
    void grupoActivoEnMes_periodoEmpiezaDespuesDelMes_noEstaActivo() {
        // Empieza el 2026-11-01 -> no activo en octubre.
        assertFalse(EstadoRegistroMensual.grupoActivoEnMes(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 12, 31), 2026, 10));
    }

    @Test
    void grupoActivoEnMes_soloFechaCreacionNull_activoMientrasNoTermineAntesDelMes() {
        assertTrue(EstadoRegistroMensual.grupoActivoEnMes(null, LocalDate.of(2026, 12, 31), 2026, 10));
        assertFalse(EstadoRegistroMensual.grupoActivoEnMes(null, LocalDate.of(2026, 9, 30), 2026, 10));
    }

    @Test
    void grupoActivoEnMes_soloFechaFinalizacionNull_activoDesdeQueEmpiece() {
        assertTrue(EstadoRegistroMensual.grupoActivoEnMes(LocalDate.of(2026, 8, 1), null, 2026, 10));
        assertFalse(EstadoRegistroMensual.grupoActivoEnMes(LocalDate.of(2026, 11, 1), null, 2026, 10));
    }

    // ── claseDentroDelMes ─────────────────────────────────────────────────

    @Test
    void claseDentroDelMes_mismoAnioYMes_esVerdadero() {
        assertTrue(EstadoRegistroMensual.claseDentroDelMes(LocalDate.of(2026, 10, 15), 2026, 10));
    }

    @Test
    void claseDentroDelMes_otroMesUOtroAnioOANull_esFalso() {
        assertFalse(EstadoRegistroMensual.claseDentroDelMes(LocalDate.of(2026, 9, 15), 2026, 10));
        assertFalse(EstadoRegistroMensual.claseDentroDelMes(LocalDate.of(2025, 10, 15), 2026, 10));
        assertFalse(EstadoRegistroMensual.claseDentroDelMes(null, 2026, 10));
    }

    // ── fechaLimiteEfectiva ───────────────────────────────────────────────

    @Test
    void fechaLimiteEfectiva_sinProrroga_esElFinDeMes() {
        assertEquals(LocalDate.of(2026, 10, 31),
                EstadoRegistroMensual.fechaLimiteEfectiva(2026, 10, null));
    }

    @Test
    void fechaLimiteEfectiva_prorrogaPosteriorAlFinDeMes_extiendeElLimite() {
        assertEquals(LocalDate.of(2026, 11, 10),
                EstadoRegistroMensual.fechaLimiteEfectiva(2026, 10, LocalDate.of(2026, 11, 10)));
    }

    @Test
    void fechaLimiteEfectiva_prorrogaAnteriorAlFinDeMes_seIgnora() {
        assertEquals(LocalDate.of(2026, 10, 31),
                EstadoRegistroMensual.fechaLimiteEfectiva(2026, 10, LocalDate.of(2026, 10, 20)));
    }

    // ── calcular ──────────────────────────────────────────────────────────

    @Test
    void calcular_sinGruposActivos_retornaSinGrupos() {
        assertEquals(EstadoRegistroMensual.SIN_GRUPOS,
                EstadoRegistroMensual.calcular(0, 0, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 15)));
    }

    @Test
    void calcular_todosRegistrados_retornaCompleto() {
        assertEquals(EstadoRegistroMensual.COMPLETO,
                EstadoRegistroMensual.calcular(3, 3, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 15)));
        assertEquals(EstadoRegistroMensual.COMPLETO,
                EstadoRegistroMensual.calcular(3, 5, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 15)));
    }

    @Test
    void calcular_faltanRegistrosYHoyAntesDelLimite_retornaPendiente() {
        assertEquals(EstadoRegistroMensual.PENDIENTE,
                EstadoRegistroMensual.calcular(3, 2, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 15)));
    }

    @Test
    void calcular_faltanRegistrosYHoyEsElLimite_esInclusivoYPendiente() {
        assertEquals(EstadoRegistroMensual.PENDIENTE,
                EstadoRegistroMensual.calcular(3, 2, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 10, 31)));
    }

    @Test
    void calcular_faltanRegistrosYHoyDespuesDelLimite_retornaVencido() {
        assertEquals(EstadoRegistroMensual.VENCIDO,
                EstadoRegistroMensual.calcular(3, 2, LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1)));
    }
}
