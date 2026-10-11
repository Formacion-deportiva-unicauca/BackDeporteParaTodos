package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

// SCRUM-185: regla pura, unica fuente de verdad usada tanto por
// InscripcionServicio.inscribir() como por el catalogo de grupos disponibles.
class EstadoVentanaInscripcionTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    @Test
    void calcular_cierreAyer_retornaCerradas() {
        LocalDate cierre = HOY.minusDays(1);
        assertEquals(EstadoVentanaInscripcion.CERRADAS,
                EstadoVentanaInscripcion.calcular(null, cierre, HOY));
    }

    @Test
    void calcular_cierreHoy_retornaAbiertas() {
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(null, HOY, HOY));
    }

    @Test
    void calcular_cierreManana_retornaAbiertas() {
        LocalDate cierre = HOY.plusDays(1);
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(null, cierre, HOY));
    }

    @Test
    void calcular_aperturaManana_retornaNoAbiertas() {
        LocalDate apertura = HOY.plusDays(1);
        assertEquals(EstadoVentanaInscripcion.NO_ABIERTAS,
                EstadoVentanaInscripcion.calcular(apertura, null, HOY));
    }

    @Test
    void calcular_aperturaHoy_retornaAbiertas() {
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(HOY, null, HOY));
    }

    @Test
    void calcular_aperturaAyer_retornaAbiertas() {
        LocalDate apertura = HOY.minusDays(1);
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(apertura, null, HOY));
    }

    @Test
    void calcular_aperturaNull_abiertoDesdeSiempre() {
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(null, HOY.plusDays(10), HOY));
    }

    @Test
    void calcular_cierreNull_sinCierre() {
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(HOY.minusDays(10), null, HOY));
    }

    @Test
    void calcular_ambosNull_siempreAbiertas() {
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(null, null, HOY));
    }

    // 23:30 en Bogota (UTC-5) es ya 04:30 del dia siguiente en UTC. Si "hoy" se
    // calculara mal (con la zona UTC en vez de America/Bogota), un cierre fijado
    // para HOY se veria como "ayer" y cerraria antes de tiempo. LocalDate.now(Clock)
    // con un Clock de zona America/Bogota debe seguir viendo HOY como HOY.
    @Test
    void calcular_relojA23_30EnBogota_cierreHoySigueAbierto() {
        ZoneId bogota = ZoneId.of("America/Bogota");
        Instant instante2330Bogota = HOY.atTime(23, 30).atZone(bogota).toInstant();
        Clock clockBogota = Clock.fixed(instante2330Bogota, bogota);

        // Confirma la premisa: en UTC ese mismo instante ya es el dia siguiente.
        LocalDate fechaEnUtc = LocalDate.now(Clock.fixed(instante2330Bogota, ZoneOffset.UTC));
        assertEquals(HOY.plusDays(1), fechaEnUtc, "en UTC ese instante ya deberia ser el dia siguiente");

        LocalDate hoyBogota = LocalDate.now(clockBogota);
        assertEquals(HOY, hoyBogota, "con el Clock de Bogota, 'hoy' debe seguir siendo HOY");
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                EstadoVentanaInscripcion.calcular(null, HOY, hoyBogota));
    }
}
