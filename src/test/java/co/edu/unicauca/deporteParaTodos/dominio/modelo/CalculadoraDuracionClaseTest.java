package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DiaFueraDeHorarioExcepcion;

// SCRUM-80 (rediseno, bloque 2 de 3): componente puro (sin Spring) que calcula
// la duracion de una clase a partir de las franjas de tbl_horario del dia de
// la semana correspondiente, y el mapeo DayOfWeek <-> valor guardado en
// HR_DIA. PASO 0 confirmo (grep sobre fixtures/IT reales) que HR_DIA se
// guarda SIN tildes ("MIERCOLES", no "MIÉRCOLES") -- se implementa asi para
// ser consistente con los datos reales, no con la ortografia formal.
class CalculadoraDuracionClaseTest {

    private Horario franja(Integer id, String inicio, String fin) {
        return new Horario(id, "cat", "cur", 2026, 1, "LUNES", inicio, fin, "Cancha", 0);
    }

    // ── nombreDia: mapeo para los 7 dias ────────────────────────────────────

    @Test
    void nombreDia_lunes() {
        assertEquals("LUNES", CalculadoraDuracionClase.nombreDia(DayOfWeek.MONDAY));
    }

    @Test
    void nombreDia_martes() {
        assertEquals("MARTES", CalculadoraDuracionClase.nombreDia(DayOfWeek.TUESDAY));
    }

    @Test
    void nombreDia_miercoles_sinTilde() {
        assertEquals("MIERCOLES", CalculadoraDuracionClase.nombreDia(DayOfWeek.WEDNESDAY));
    }

    @Test
    void nombreDia_jueves() {
        assertEquals("JUEVES", CalculadoraDuracionClase.nombreDia(DayOfWeek.THURSDAY));
    }

    @Test
    void nombreDia_viernes() {
        assertEquals("VIERNES", CalculadoraDuracionClase.nombreDia(DayOfWeek.FRIDAY));
    }

    @Test
    void nombreDia_sabado_sinTilde() {
        assertEquals("SABADO", CalculadoraDuracionClase.nombreDia(DayOfWeek.SATURDAY));
    }

    @Test
    void nombreDia_domingo() {
        assertEquals("DOMINGO", CalculadoraDuracionClase.nombreDia(DayOfWeek.SUNDAY));
    }

    @Test
    void nombreDia_deFecha_delegaEnElDiaDeLaSemanaDeLaFecha() {
        // 2026-10-14 es miercoles.
        assertEquals("MIERCOLES", CalculadoraDuracionClase.nombreDia(LocalDate.of(2026, 10, 14).getDayOfWeek()));
    }

    // ── calcular: duracion a partir de franjas ──────────────────────────────

    @Test
    void calcular_unaFranjaDeDosHoras_retorna120Minutos() {
        var resultado = CalculadoraDuracionClase.calcular(List.of(franja(1, "14:00", "16:00")));
        assertEquals(120, resultado.getMinutos());
        assertTrue(resultado.getIdsFranjasIgnoradas().isEmpty());
    }

    @Test
    void calcular_unaFranjaDeTresHoras_retorna180Minutos() {
        var resultado = CalculadoraDuracionClase.calcular(List.of(franja(1, "14:00", "17:00")));
        assertEquals(180, resultado.getMinutos());
    }

    @Test
    void calcular_variasFranjasElMismoDia_seSuman() {
        var resultado = CalculadoraDuracionClase.calcular(List.of(
                franja(1, "08:00", "10:00"),
                franja(2, "14:00", "16:00")));
        assertEquals(240, resultado.getMinutos());
        assertTrue(resultado.getIdsFranjasIgnoradas().isEmpty());
    }

    @Test
    void calcular_sinFranjas_retornaCero() {
        var resultado = CalculadoraDuracionClase.calcular(List.of());
        assertEquals(0, resultado.getMinutos());
        assertTrue(resultado.getIdsFranjasIgnoradas().isEmpty());
    }

    // Decision documentada: una franja con fin <= inicio NO es un error -- se
    // IGNORA (no cuenta minutos) y su id queda en el resultado para que el
    // llamador (ClaseServicio) la deje en el log como dato de diagnostico, sin
    // romper el registro de la clase completa por un dato de horario invalido
    // que esta fuera del control del instructor que registra hoy.
    @Test
    void calcular_franjaConFinIgualAInicio_seIgnoraYQuedaElIdRegistrado() {
        var resultado = CalculadoraDuracionClase.calcular(List.of(franja(7, "10:00", "10:00")));
        assertEquals(0, resultado.getMinutos());
        assertEquals(List.of(7), resultado.getIdsFranjasIgnoradas());
    }

    @Test
    void calcular_franjaConFinAnteriorAInicio_seIgnoraYQuedaElIdRegistrado() {
        var resultado = CalculadoraDuracionClase.calcular(List.of(franja(8, "10:00", "09:00")));
        assertEquals(0, resultado.getMinutos());
        assertEquals(List.of(8), resultado.getIdsFranjasIgnoradas());
    }

    @Test
    void calcular_mezclaDeFranjasValidasEInvalidas_sumaSoloLasValidasYRegistraLasIgnoradas() {
        var resultado = CalculadoraDuracionClase.calcular(List.of(
                franja(1, "08:00", "10:00"),
                franja(2, "10:00", "10:00"),
                franja(3, "14:00", "15:30")));
        assertEquals(210, resultado.getMinutos(), "120 (08-10) + 90 (14:00-15:30), la franja 2 no cuenta");
        assertEquals(List.of(2), resultado.getIdsFranjasIgnoradas());
    }

    @Test
    void calcular_tieneFranjas_esVerdaderoSoloConAlMenosUnaValida() {
        assertTrue(CalculadoraDuracionClase.calcular(List.of(franja(1, "08:00", "10:00"))).tieneFranjasValidas());
        assertTrue(!CalculadoraDuracionClase.calcular(List.of()).tieneFranjasValidas());
        assertTrue(!CalculadoraDuracionClase.calcular(List.of(franja(1, "10:00", "10:00"))).tieneFranjasValidas(),
                "una franja invalida no cuenta como franja -- equivale a 'sin horario ese dia'");
    }

    // ── SCRUM-80 (correccion del cliente, 15 oct 2026): dia-en-horario ──────

    private Horario franjaDia(Integer id, String dia, String inicio, String fin) {
        return new Horario(id, "cat", "cur", 2026, 1, dia, inicio, fin, "Cancha", 0);
    }

    @Test
    void hayFranjaValidaEnFecha_diaConFranjaValida_esVerdadero() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);
        List<Horario> franjas = List.of(franjaDia(1, "JUEVES", "14:00", "16:00"));

        assertTrue(CalculadoraDuracionClase.hayFranjaValidaEnFecha(franjas, jueves));
    }

    @Test
    void hayFranjaValidaEnFecha_diaSinNingunaFranja_esFalso() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);
        List<Horario> franjas = List.of(franjaDia(1, "MARTES", "14:00", "16:00"));

        assertFalse(CalculadoraDuracionClase.hayFranjaValidaEnFecha(franjas, jueves));
    }

    @Test
    void hayFranjaValidaEnFecha_grupoSinNingunaFranja_esFalso() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);

        assertFalse(CalculadoraDuracionClase.hayFranjaValidaEnFecha(List.of(), jueves));
    }

    @Test
    void hayFranjaValidaEnFecha_soloFranjaInvalidaEseDia_esFalso() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);
        List<Horario> franjas = List.of(franjaDia(1, "JUEVES", "10:00", "10:00"));

        assertFalse(CalculadoraDuracionClase.hayFranjaValidaEnFecha(franjas, jueves));
    }

    @Test
    void hayFranjaValidaEnFecha_variasFranjasElMismoDia_bastaConUnaValida() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);
        List<Horario> franjas = List.of(
                franjaDia(1, "JUEVES", "10:00", "10:00"), // invalida
                franjaDia(2, "JUEVES", "14:00", "16:00")); // valida

        assertTrue(CalculadoraDuracionClase.hayFranjaValidaEnFecha(franjas, jueves));
    }

    @Test
    void hayFranjaValidaEnFecha_paraLos7Dias_usaElNombreSinTildes() {
        List<Horario> franjas = List.of(
                franjaDia(1, "LUNES", "08:00", "09:00"),
                franjaDia(2, "MARTES", "08:00", "09:00"),
                franjaDia(3, "MIERCOLES", "08:00", "09:00"),
                franjaDia(4, "JUEVES", "08:00", "09:00"),
                franjaDia(5, "VIERNES", "08:00", "09:00"),
                franjaDia(6, "SABADO", "08:00", "09:00"),
                franjaDia(7, "DOMINGO", "08:00", "09:00"));
        // 2026-10-12 es lunes -- los 7 dias siguientes cubren la semana completa.
        LocalDate lunes = LocalDate.of(2026, 10, 12);
        for (int i = 0; i < 7; i++) {
            assertTrue(CalculadoraDuracionClase.hayFranjaValidaEnFecha(franjas, lunes.plusDays(i)),
                    "deberia haber franja valida para " + lunes.plusDays(i).getDayOfWeek());
        }
    }

    @Test
    void diasConFranjaValida_variosDias_retornaEnOrdenNaturalDeLaSemana() {
        List<Horario> franjas = List.of(
                franjaDia(1, "JUEVES", "14:00", "16:00"),
                franjaDia(2, "MARTES", "08:00", "10:00"));

        assertEquals(List.of("MARTES", "JUEVES"), CalculadoraDuracionClase.diasConFranjaValida(franjas));
    }

    @Test
    void diasConFranjaValida_sinNingunaFranja_retornaListaVacia() {
        assertTrue(CalculadoraDuracionClase.diasConFranjaValida(List.of()).isEmpty());
    }

    @Test
    void diasConFranjaValida_soloFranjasInvalidas_retornaListaVacia() {
        List<Horario> franjas = List.of(franjaDia(1, "JUEVES", "10:00", "10:00"));

        assertTrue(CalculadoraDuracionClase.diasConFranjaValida(franjas).isEmpty());
    }

    @Test
    void validarDiaConHorario_conFranjaValidaEseDia_noLanza() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);
        List<Horario> franjas = List.of(franjaDia(1, "JUEVES", "14:00", "16:00"));

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> CalculadoraDuracionClase.validarDiaConHorario(franjas, jueves));
    }

    @Test
    void validarDiaConHorario_grupoSinNingunHorario_lanzaConMensajeQueLoExplica() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);

        DiaFueraDeHorarioExcepcion ex = assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> CalculadoraDuracionClase.validarDiaConHorario(List.of(), jueves));

        assertTrue(ex.getMessage().toLowerCase().contains("ningun horario"));
        assertTrue(ex.getMessage().toLowerCase().contains("coordinador"));
    }

    @Test
    void validarDiaConHorario_grupoConHorarioOtrosDias_lanzaMencionandoLosDiasReales() {
        LocalDate jueves = LocalDate.of(2026, 10, 15);
        List<Horario> franjas = List.of(
                franjaDia(1, "MARTES", "08:00", "10:00"),
                franjaDia(2, "SABADO", "08:00", "10:00"));

        DiaFueraDeHorarioExcepcion ex = assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> CalculadoraDuracionClase.validarDiaConHorario(franjas, jueves));

        assertTrue(ex.getMessage().contains("MARTES"));
        assertTrue(ex.getMessage().contains("SABADO"));
        assertFalse(ex.getMessage().contains("JUEVES"));
    }

    // El mensaje nunca repite datos crudos del cliente (p.ej. la fecha enviada
    // en la peticion) -- solo los dias reales del horario del grupo.
    @Test
    void validarDiaConHorario_elMensajeNoEcoaLaFechaCrudaDeLaPeticion() {
        LocalDate fechaEnviadaPorElCliente = LocalDate.of(2026, 10, 15);

        DiaFueraDeHorarioExcepcion ex = assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> CalculadoraDuracionClase.validarDiaConHorario(List.of(), fechaEnviadaPorElCliente));

        assertFalse(ex.getMessage().contains(fechaEnviadaPorElCliente.toString()));
    }
}
