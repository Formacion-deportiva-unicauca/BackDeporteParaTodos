package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

// Fase 3: un grupo acepta inscripciones solo si su ventana de fechas esta abierta Y no fue cerrado a mano.
class GrupoTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    private Grupo grupo(LocalDate apertura, LocalDate cierre, boolean cerradoManual) {
        Grupo g = new Grupo();
        g.setFechaInscripcionApertura(apertura);
        g.setFechaIncripcionCierre(cierre);
        g.setInscripcionesCerradasManual(cerradoManual);
        return g;
    }

    @Test
    void sinCierreManual_siguePorFechas_abierto() {
        assertEquals(EstadoVentanaInscripcion.ABIERTAS,
                grupo(HOY.minusDays(5), HOY.plusDays(5), false).estadoInscripcion(HOY));
    }

    @Test
    void sinCierreManual_siguePorFechas_cerradoPorFecha() {
        assertEquals(EstadoVentanaInscripcion.CERRADAS,
                grupo(HOY.minusDays(10), HOY.minusDays(1), false).estadoInscripcion(HOY));
    }

    @Test
    void sinCierreManual_siguePorFechas_aunNoAbre() {
        assertEquals(EstadoVentanaInscripcion.NO_ABIERTAS,
                grupo(HOY.plusDays(1), HOY.plusDays(9), false).estadoInscripcion(HOY));
    }

    @Test
    void cierreManual_anulaUnaVentanaAbierta() {
        assertEquals(EstadoVentanaInscripcion.CERRADAS,
                grupo(HOY.minusDays(5), HOY.plusDays(5), true).estadoInscripcion(HOY));
    }

    @Test
    void porDefectoUnGrupoNoEstaCerradoAMano() {
        assertFalse(new Grupo().isInscripcionesCerradasManual());
    }
}
