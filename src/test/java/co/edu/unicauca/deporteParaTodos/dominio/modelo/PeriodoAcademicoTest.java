package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

// SCRUM-80 (HE-14 HU-02): reutiliza la MISMA regla de periodo que
// GrupoGateway.insertarGrupo() (mes >= 7 -> periodo 2, si no periodo 1), pero
// calculando "hoy" con el Clock inyectado (zona America/Bogota) en vez de
// LocalDate.now() sin zona -- sin cambiar el comportamiento de GrupoGateway.
class PeriodoAcademicoTest {

    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    @Test
    void vigente_mesDeEnero_esPeriodo1() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-15T12:00:00-05:00"), BOGOTA);
        PeriodoAcademico periodo = PeriodoAcademico.vigente(clock);
        assertEquals(2026, periodo.getAnio());
        assertEquals(1, periodo.getPeriodo());
    }

    @Test
    void vigente_mesJunio_esPeriodo1() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-15T12:00:00-05:00"), BOGOTA);
        assertEquals(1, PeriodoAcademico.vigente(clock).getPeriodo());
    }

    @Test
    void vigente_mesJulio_esPeriodo2() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-15T12:00:00-05:00"), BOGOTA);
        assertEquals(2, PeriodoAcademico.vigente(clock).getPeriodo());
    }

    @Test
    void vigente_mesDiciembre_esPeriodo2() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-15T12:00:00-05:00"), BOGOTA);
        assertEquals(2, PeriodoAcademico.vigente(clock).getPeriodo());
    }

    // 2026-06-30 23:30 en Bogota (UTC-5) es 2026-07-01T04:30:00Z en UTC. Si el
    // calculo usara el instante en UTC (o la zona del sistema) en vez de la zona
    // America/Bogota del propio Clock, "hoy" se correria a julio y el periodo
    // saldria 2 en vez de 1 -- este test fija el instante exacto de ese borde.
    @Test
    void vigente_fijadoA2330BogotaFinDeJunio_siguesiendoPeriodo1YAnio2026() {
        Instant instanteUtc = Instant.parse("2026-07-01T04:30:00Z");
        Clock clock = Clock.fixed(instanteUtc, BOGOTA);
        PeriodoAcademico periodo = PeriodoAcademico.vigente(clock);
        assertEquals(2026, periodo.getAnio());
        assertEquals(1, periodo.getPeriodo());
    }

    @Test
    void vigente_finDeAnioEnDiciembre_anioEsElDeHoyNoElSiguiente() {
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T23:30:00-05:00"), BOGOTA);
        PeriodoAcademico periodo = PeriodoAcademico.vigente(clock);
        assertEquals(2026, periodo.getAnio());
        assertEquals(2, periodo.getPeriodo());
    }
}
