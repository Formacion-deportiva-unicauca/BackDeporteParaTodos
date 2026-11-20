package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.INovedadClaseGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NovedadAsistenciaConflictoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;

@ExtendWith(MockitoExtension.class)
class ExclusionNovedadAsistenciaServicioTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 11, 20);

    @Mock private IGrupoGateway grupoGateway;
    @Mock private INovedadClaseGateway novedadGateway;
    @Mock private IClaseGateway claseGateway;
    @Mock private IAsistenciaGateway asistenciaGateway;

    private ExclusionNovedadAsistenciaServicio servicio() {
        return new ExclusionNovedadAsistenciaServicio(grupoGateway, novedadGateway, claseGateway, asistenciaGateway);
    }

    private Clase clase(LocalDateTime confirmadaEn) {
        Clase c = new Clase();
        c.setCodigo(7);
        c.setAsistenciaConfirmadaEn(confirmadaEn);
        return c;
    }

    // ── asistencia con novedad ──

    @Test
    void exigirSinNovedad_novedadActiva_lanzaConflictoConMensajeClaro_yTomaPrimeroElBloqueoDelGrupo() {
        when(novedadGateway.existeNovedadActiva("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(true);

        NovedadAsistenciaConflictoExcepcion ex = assertThrows(NovedadAsistenciaConflictoExcepcion.class,
                () -> servicio().exigirSinNovedad("Rec", "Ajedrez", 2026, 1, FECHA));

        assertEquals("Esta clase fue marcada como no realizada. No se puede registrar asistencia para esa fecha", ex.getMessage());
        InOrder orden = inOrder(grupoGateway, novedadGateway);
        orden.verify(grupoGateway).obtenerGrupoConLock("Rec", "Ajedrez", 2026, 1);
        orden.verify(novedadGateway).existeNovedadActiva("Rec", "Ajedrez", 2026, 1, FECHA);
    }

    @Test
    void exigirSinNovedad_sinNovedadActiva_permite() {
        // existeNovedadActiva solo cuenta META_ELIMINADO=0: una novedad cancelada o eliminada devuelve false (regla 5).
        when(novedadGateway.existeNovedadActiva("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(false);

        assertDoesNotThrow(() -> servicio().exigirSinNovedad("Rec", "Ajedrez", 2026, 1, FECHA));
    }

    // ── novedad con asistencia ──

    @Test
    void exigirSinAsistencia_sinClaseEnLaFecha_permite_yTomaElBloqueoDelGrupo() {
        when(claseGateway.existeClaseEnFecha("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(false);

        assertDoesNotThrow(() -> servicio().exigirSinAsistencia("Rec", "Ajedrez", 2026, 1, FECHA));
        verify(grupoGateway).obtenerGrupoConLock("Rec", "Ajedrez", 2026, 1);
    }

    @Test
    void exigirSinAsistencia_claseConfirmada_lanzaConflicto_aunqueTodosEstenAusentes() {
        when(claseGateway.existeClaseEnFecha("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(true);
        when(claseGateway.obtenerClaseEnFecha("Rec", "Ajedrez", 2026, 1, FECHA))
                .thenReturn(clase(LocalDateTime.of(2026, 11, 20, 20, 0)));

        NovedadAsistenciaConflictoExcepcion ex = assertThrows(NovedadAsistenciaConflictoExcepcion.class,
                () -> servicio().exigirSinAsistencia("Rec", "Ajedrez", 2026, 1, FECHA));

        assertEquals("Ya hay asistencia registrada para esta fecha. Corrige primero la asistencia antes de marcar la clase como no realizada",
                ex.getMessage());
    }

    @Test
    void exigirSinAsistencia_claseSinConfirmarPeroConUnaFilaActiva_lanzaConflicto() {
        when(claseGateway.existeClaseEnFecha("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(true);
        when(claseGateway.obtenerClaseEnFecha("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(clase(null));
        when(asistenciaGateway.obtenerAtencionesPorClase(7))
                .thenReturn(List.of(new Asistencia("alu-1", 7, 1), new Asistencia("alu-2", 7, 0)));

        assertThrows(NovedadAsistenciaConflictoExcepcion.class,
                () -> servicio().exigirSinAsistencia("Rec", "Ajedrez", 2026, 1, FECHA));
    }

    @Test
    void exigirSinAsistencia_claseSinConfirmarSoloConFilasEliminadas_permite() {
        when(claseGateway.existeClaseEnFecha("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(true);
        when(claseGateway.obtenerClaseEnFecha("Rec", "Ajedrez", 2026, 1, FECHA)).thenReturn(clase(null));
        when(asistenciaGateway.obtenerAtencionesPorClase(7)).thenReturn(List.of(new Asistencia("alu-1", 7, 1)));

        assertDoesNotThrow(() -> servicio().exigirSinAsistencia("Rec", "Ajedrez", 2026, 1, FECHA));
    }
}
