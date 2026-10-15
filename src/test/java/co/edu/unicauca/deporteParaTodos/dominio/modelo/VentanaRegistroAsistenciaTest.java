package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;

// SCRUM-80 (rediseño, bloque 1 de 3): regla de negocio pura (sin Spring) para
// decidir si un instructor puede registrar asistencia de una fecha dada.
// Plazo base = fin de mes de la fecha de la clase, inclusive, igual para
// todos. Prorroga = la asigna el Coordinador, por instructor y mes; limite
// efectivo = max(finDeMes, fechaAsignacion) + dias. El Coordinador no tiene
// limite de plazo, pero SI esta sujeto a "no fecha futura" (nadie la tiene).
class VentanaRegistroAsistenciaTest {

    // ── finDeMes ─────────────────────────────────────────────────────────

    @Test
    void finDeMes_mesRegular_retornaUltimoDia() {
        assertEquals(LocalDate.of(2026, 1, 31), VentanaRegistroAsistencia.finDeMes(2026, 1));
    }

    @Test
    void finDeMes_febreroBisiesto_retorna29() {
        // 2028 es bisiesto (divisible por 4, no por 100).
        assertEquals(LocalDate.of(2028, 2, 29), VentanaRegistroAsistencia.finDeMes(2028, 2));
    }

    @Test
    void finDeMes_febreroNoBisiesto_retorna28() {
        assertEquals(LocalDate.of(2026, 2, 28), VentanaRegistroAsistencia.finDeMes(2026, 2));
    }

    // ── calcularLimiteProrroga ───────────────────────────────────────────

    @Test
    void calcularLimiteProrroga_asignacionDentroDelMes_sumaDiasAlFinDeMes() {
        LocalDate finDeMes = LocalDate.of(2026, 1, 31);
        LocalDate fechaAsignacion = LocalDate.of(2026, 1, 10);
        assertEquals(LocalDate.of(2026, 2, 10),
                VentanaRegistroAsistencia.calcularLimiteProrroga(finDeMes, fechaAsignacion, 10));
    }

    // Reabrir un mes YA vencido: la asignacion es posterior al fin de mes ->
    // el limite se calcula desde la fecha de asignacion, no desde el fin de mes.
    @Test
    void calcularLimiteProrroga_reabrirMesVencido_usaFechaDeAsignacionComoBase() {
        LocalDate finDeMes = LocalDate.of(2026, 1, 31);
        LocalDate fechaAsignacion = LocalDate.of(2026, 3, 5);
        assertEquals(LocalDate.of(2026, 3, 15),
                VentanaRegistroAsistencia.calcularLimiteProrroga(finDeMes, fechaAsignacion, 10));
    }

    @Test
    void calcularLimiteProrroga_diasCero_limiteEsLaBaseMisma() {
        LocalDate finDeMes = LocalDate.of(2026, 1, 31);
        LocalDate fechaAsignacion = LocalDate.of(2026, 1, 10);
        assertEquals(finDeMes, VentanaRegistroAsistencia.calcularLimiteProrroga(finDeMes, fechaAsignacion, 0));
    }

    // ── validarRegistro ──────────────────────────────────────────────────

    @Test
    void validarRegistro_finDeMesInclusive_permiteSinProrroga() {
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31), false, null));
    }

    @Test
    void validarRegistro_hoyEsElLimite_permite() {
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 1, 31), false, null));
    }

    @Test
    void validarRegistro_hoyEsLimiteMasUnDia_lanzaVencido() {
        assertThrows(RegistroAsistenciaVencidoExcepcion.class, () -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 1), false, null));
    }

    @Test
    void validarRegistro_cambioDeAnioDiciembreAEnero_elLimiteSigueSiendoFinDeDiciembre() {
        // fechaClase en diciembre 2025; "hoy" ya en enero 2026 -> vencido sin prorroga.
        assertThrows(RegistroAsistenciaVencidoExcepcion.class, () -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2025, 12, 15), LocalDate.of(2026, 1, 1), false, null));
        // pero el 31 de diciembre todavia es valido (inclusive).
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2025, 12, 15), LocalDate.of(2025, 12, 31), false, null));
    }

    @Test
    void validarRegistro_febreroBisiesto_limiteEs29DeFebrero() {
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2028, 2, 10), LocalDate.of(2028, 2, 29), false, null));
        assertThrows(RegistroAsistenciaVencidoExcepcion.class, () -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2028, 2, 10), LocalDate.of(2028, 3, 1), false, null));
    }

    @Test
    void validarRegistro_mesAnteriorSinProrroga_lanzaVencido() {
        assertThrows(RegistroAsistenciaVencidoExcepcion.class, () -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 1), false, null));
    }

    @Test
    void validarRegistro_mesAnteriorConProrrogaVigente_permite() {
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 1), false, LocalDate.of(2026, 3, 15)));
    }

    // La prorroga con fecha ANTERIOR al fin de mes no debe acortar el plazo --
    // el limite efectivo es max(finDeMes, limiteProrroga), nunca el menor de los dos.
    @Test
    void validarRegistro_prorrogaAnteriorAlFinDeMes_seIgnoraYUsaFinDeMes() {
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 25), false, LocalDate.of(2026, 1, 20)));
    }

    @Test
    void validarRegistro_reabrirMesVencido_prorrogaConLimitePosteriorAHoy_permite() {
        // La prorroga fue calculada con calcularLimiteProrroga sobre una fecha de
        // asignacion posterior al fin de mes (reapertura) -- aqui solo se valida
        // que, dado ese limite ya calculado, el registro se permita.
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 1, 10), LocalDate.of(2026, 3, 10), false, LocalDate.of(2026, 3, 15)));
    }

    @Test
    void validarRegistro_fechaFutura_lanzaFechaClaseFuturaIndependienteDelRol() {
        assertThrows(FechaClaseFuturaExcepcion.class, () -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 10, 15), false, null));
        assertThrows(FechaClaseFuturaExcepcion.class, () -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 10, 15), true, null));
    }

    @Test
    void validarRegistro_coordinadorSinProrroga_noTieneLimiteDePlazo() {
        // Clase de hace varios meses, sin prorroga, pero quien registra es Coordinador.
        assertDoesNotThrow(() -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2025, 1, 10), LocalDate.of(2026, 10, 15), true, null));
    }

    @Test
    void validarRegistro_coordinadorConFechaFutura_igualSeRechaza() {
        assertThrows(FechaClaseFuturaExcepcion.class, () -> VentanaRegistroAsistencia.validarRegistro(
                LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 15), true, null));
    }
}
