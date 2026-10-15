package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;

@ExtendWith(MockitoExtension.class)
class AsistenciaServicioTest {

    @Mock
    private IAsistenciaGateway asistenciaGateway;

    @Mock
    private IAlumnoGateway alumnoGateway;

    @Mock
    private IClaseGateway claseGateway;

    @Mock
    private IProrrogaRegistroAsistenciaGateway prorrogaGateway;

    @Mock
    private Clock clock;

    @InjectMocks
    private AsistenciaServicio servicio;

    private static final String INS   = "INS001";
    private static final String COORD = "coord-1";
    private static final int CLASE_ID = 10;

    // "hoy" = 2026-10-15, jueves -- mes actual = (2026, 10). Mismo HOY_FIJO que
    // ClaseServicioTest para que el mismo escenario (mes anterior/con o sin
    // prorroga) sea comparable entre ambos servicios.
    private static final LocalDate HOY_FIJO = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void configurarClockPorDefecto() {
        lenient().when(clock.instant()).thenReturn(HOY_FIJO.atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));
    }

    private Clase claseConFecha(LocalDate fecha) {
        Clase c = new Clase();
        c.setCodigo(CLASE_ID);
        c.setFecha(fecha);
        return c;
    }

    private void sinProrroga() {
        lenient().when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerAsistencias / obtenerAtencionesPorClase (sin cambios de firma)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerAsistencias_gatewayRetornaNull_lanzaListadoVacioExcepcionSinNpe() {
        when(asistenciaGateway.obtenerAsistencias()).thenReturn(null);

        assertThrows(ListadoVacioExcepcion.class, () -> servicio.obtenerAsistencias());
    }

    @Test
    void obtenerAsistencias_listaVacia_lanzaListadoVacioExcepcion() {
        when(asistenciaGateway.obtenerAsistencias()).thenReturn(List.of());

        assertThrows(ListadoVacioExcepcion.class, () -> servicio.obtenerAsistencias());
    }

    @Test
    void obtenerAsistencias_conDatos_retornaLaLista() {
        Asistencia asistencia = new Asistencia("perf1", 10, 0);
        when(asistenciaGateway.obtenerAsistencias()).thenReturn(List.of(asistencia));

        List<Asistencia> resultado = servicio.obtenerAsistencias();

        assertEquals(1, resultado.size());
    }

    @Test
    void obtenerAtencionesPorClase_exitoso_retornaListaDtos() {
        Asistencia asistencia = new Asistencia("perf1", 10, 0);
        when(asistenciaGateway.obtenerAtencionesPorClase(10)).thenReturn(List.of(asistencia));

        List<AtencionDto> resultado = servicio.obtenerAtencionesPorClase(10);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("perf1", resultado.get(0).getIdPerfil());
    }

    @Test
    void obtenerAtencionesPorClase_listaVacia_lanzaListadoVacioExcepcion() {
        when(asistenciaGateway.obtenerAtencionesPorClase(10)).thenReturn(List.of());

        assertThrows(ListadoVacioExcepcion.class,
                () -> servicio.obtenerAtencionesPorClase(10));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // registrarAtencionesPorClase: ventana de registro (rol x estado)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarAtencionesPorClase_claseInexistente_lanzaNoExisteExcepcionAntesDeValidarVentana() {
        when(claseGateway.existeClase(99)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), 99, INS, "Instructor"));

        verify(claseGateway, never()).obtenerClase(anyInt());
    }

    @Test
    void registrarAtencionesPorClase_instructorMesActual_permiteYNoLanza() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));
    }

    @Test
    void registrarAtencionesPorClase_instructorMesAnteriorSinProrroga_lanzaRegistroAsistenciaVencidoSinTocarAlumnos() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));
        sinProrroga();
        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, true);

        assertThrows(RegistroAsistenciaVencidoExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor"));

        verify(alumnoGateway, never()).existeAlumno(any());
    }

    @Test
    void registrarAtencionesPorClase_instructorMesAnteriorConProrrogaVigente_permite() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 20));
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(INS, prorroga));

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));
    }

    @Test
    void registrarAtencionesPorClase_fechaFutura_instructor_lanzaFechaClaseFutura() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO.plusDays(1)));

        assertThrows(FechaClaseFuturaExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));

        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void registrarAtencionesPorClase_fechaFutura_coordinador_igualLanzaFechaClaseFutura() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO.plusDays(1)));

        assertThrows(FechaClaseFuturaExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, COORD, "Coordinador"));
    }

    @Test
    void registrarAtencionesPorClase_coordinadorMesAnteriorSinProrroga_noTieneLimiteDePlazo() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, COORD, "Coordinador"));
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void registrarAtencionesPorClase_administradorMesAnteriorSinProrroga_noTieneLimiteDePlazo() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, "admin-1", "Administrador"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // registrarAtencionesPorClase: alta/baja/reactivacion por alumno (dentro de ventana)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarAtencionesPorClase_listaVacia_noLanzaErrorNiTocaAlumnos() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));

        verify(asistenciaGateway, never()).InsertarAsistencia(any());
        verify(alumnoGateway, never()).existeAlumno(any());
    }

    @Test
    void registrarAtencionesPorClase_alumnoNuevo_insertaAsistencia() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("alum1", CLASE_ID)).thenReturn(Optional.empty());

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, true);
        servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway).InsertarAsistencia(any());
    }

    @Test
    void registrarAtencionesPorClase_alumnoYaActivo_noDuplicaNiLanzaError() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("alum1", CLASE_ID))
                .thenReturn(Optional.of(new Asistencia("alum1", CLASE_ID, 0)));

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, true);
        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor"));

        verify(asistenciaGateway, never()).InsertarAsistencia(any());
        verify(asistenciaGateway, never()).reactivarAsistencia(any(), anyInt());
    }

    // Fix de esta misma iteracion (ver AsistenciaServicio.registrarAtencionesPorClase):
    // una fila con META_ELIMINADO=1 ahora SI se reactiva en vez de quedar huerfana.
    @Test
    void registrarAtencionesPorClase_alumnoConAsistenciaEliminadaLogicamente_laReactiva() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("alum1", CLASE_ID))
                .thenReturn(Optional.of(new Asistencia("alum1", CLASE_ID, 1)));

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, true);
        servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway).reactivarAsistencia("alum1", CLASE_ID);
        verify(asistenciaGateway, never()).InsertarAsistencia(any());
    }

    @Test
    void registrarAtencionesPorClase_alumnoInexistente_lanzaNoExisteExcepcion() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        when(alumnoGateway.existeAlumno("fantasma")).thenReturn(false);

        AtencionDto dto = new AtencionDto("fantasma", CLASE_ID, true);
        assertThrows(NoExisteExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor"));
    }

    @Test
    void registrarAtencionesPorClase_estaAtendidoFalseYExisteActiva_eliminaLogicamente() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("alum1", CLASE_ID))
                .thenReturn(Optional.of(new Asistencia("alum1", CLASE_ID, 0)));

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, false);
        servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway).eliminarAsitencia("alum1", CLASE_ID);
    }

    @Test
    void registrarAtencionesPorClase_estaAtendidoFalseYYaEstabaEliminada_noHaceNada() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("alum1", CLASE_ID))
                .thenReturn(Optional.of(new Asistencia("alum1", CLASE_ID, 1)));

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, false);
        servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway, never()).eliminarAsitencia(any(), anyInt());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarAsistencia: estado previo (no existe / ya eliminada) antes de la ventana
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void eliminarAsistencia_noExiste_lanzaNoExisteExcepcionSinValidarVentana() {
        when(asistenciaGateway.existeAsistencia("perf1", CLASE_ID)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));

        verify(claseGateway, never()).obtenerClase(anyInt());
        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
    }

    @Test
    void eliminarAsistencia_yaEliminada_lanzaYaExisteElementoExcepcionSinValidarVentana() {
        Asistencia yaEliminada = new Asistencia("perf1", CLASE_ID, 1);
        when(asistenciaGateway.existeAsistencia("perf1", CLASE_ID)).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("perf1", CLASE_ID)).thenReturn(Optional.of(yaEliminada));

        assertThrows(YaExisteElementoExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));

        verify(claseGateway, never()).obtenerClase(anyInt());
        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarAsistencia: ventana de registro (rol x estado)
    // ──────────────────────────────────────────────────────────────────────────

    private void stubAsistenciaActiva(String perfId) {
        Asistencia activa = new Asistencia(perfId, CLASE_ID, 0);
        when(asistenciaGateway.existeAsistencia(perfId, CLASE_ID)).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia(perfId, CLASE_ID)).thenReturn(Optional.of(activa));
    }

    @Test
    void eliminarAsistencia_instructorMesActual_permiteYDelegaAlGateway() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        Asistencia eliminada = new Asistencia("perf1", CLASE_ID, 1);
        when(asistenciaGateway.eliminarAsistencia("perf1", (long) CLASE_ID)).thenReturn(eliminada);

        Asistencia resultado = servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor");

        assertNotNull(resultado);
        assertEquals(1, resultado.getEliminado());
        verify(asistenciaGateway).eliminarAsistencia("perf1", (long) CLASE_ID);
    }

    @Test
    void eliminarAsistencia_instructorMesAnteriorSinProrroga_lanzaRegistroAsistenciaVencidoSinEliminar() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));
        sinProrroga();

        assertThrows(RegistroAsistenciaVencidoExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));

        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
    }

    @Test
    void eliminarAsistencia_instructorMesAnteriorConProrrogaVigente_permite() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 20));
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(INS, prorroga));
        when(asistenciaGateway.eliminarAsistencia("perf1", (long) CLASE_ID))
                .thenReturn(new Asistencia("perf1", CLASE_ID, 1));

        assertDoesNotThrow(() -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));
    }

    @Test
    void eliminarAsistencia_fechaFutura_instructor_lanzaFechaClaseFutura() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO.plusDays(1)));

        assertThrows(FechaClaseFuturaExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));

        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
    }

    @Test
    void eliminarAsistencia_coordinadorMesAnteriorSinProrroga_noTieneLimiteDePlazo() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));
        when(asistenciaGateway.eliminarAsistencia("perf1", (long) CLASE_ID))
                .thenReturn(new Asistencia("perf1", CLASE_ID, 1));

        assertDoesNotThrow(() -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, COORD, "Coordinador"));
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void eliminarAsistencia_administradorMesAnteriorSinProrroga_noTieneLimiteDePlazo() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10)));
        when(asistenciaGateway.eliminarAsistencia("perf1", (long) CLASE_ID))
                .thenReturn(new Asistencia("perf1", CLASE_ID, 1));

        assertDoesNotThrow(() -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, "admin-1", "Administrador"));
    }
}
