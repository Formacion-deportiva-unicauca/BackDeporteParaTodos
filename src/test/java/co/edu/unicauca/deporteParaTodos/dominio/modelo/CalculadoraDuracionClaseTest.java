package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

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
}
