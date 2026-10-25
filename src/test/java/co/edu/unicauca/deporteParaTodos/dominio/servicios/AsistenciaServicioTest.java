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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AccesoGrupoDenegadoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DiaFueraDeHorarioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.VentanaRegistroDto;

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
    private IHorarioGateway horarioGateway;

    @Mock
    private AutorizacionGrupoServicio autorizacionGrupo;

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
        // SCRUM-80 (correccion del cliente): el dia-en-horario necesita
        // categoria/curso/anio/iterable para consultar el horario del grupo --
        // horarioGateway.listarHorariosPorGrupo(..., int, int) desempaqueta
        // anio/iterable, asi que no pueden quedar null (NPE en autoboxing).
        c.setCategoria("Recreativo");
        c.setCurso("Natacion");
        c.setAnio(2026);
        c.setIterable(1);
        return c;
    }

    private Horario franja(String dia, String inicio, String fin) {
        return new Horario(1, null, null, 0, 0, dia, inicio, fin, "Cancha", 0);
    }

    // SCRUM-80 (correccion del cliente, 15 oct 2026): HOY_FIJO (2026-10-15) es
    // jueves -- todos los llamantes de este helper son Instructor registrando
    // en HOY_FIJO o en el mes anterior sobre la misma fecha-de-prueba (tambien
    // jueves, ver ClaseServicioTest), asi que una franja JUEVES por defecto
    // les permite seguir probando ventana/plazo sin que la regla nueva de
    // dia-en-horario (no relacionada con lo que esos tests verifican) los
    // bloquee. Los tests que SI prueban la regla de dia sobreescriben este stub.
    private void sinProrroga() {
        lenient().when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        lenient().when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja("JUEVES", "14:00", "16:00")));
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
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(LocalDate.of(2026, 9, 10))); // jueves
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 20));
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(INS, prorroga));
        // Este test prueba la prorroga, no el dia-en-horario -- se le da una franja valida.
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja("JUEVES", "14:00", "16:00")));

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

    // ── SCRUM-80 (correccion del cliente, 15 oct 2026): dia-en-horario (Instructor) ──

    @Test
    void registrarAtencionesPorClase_instructorGrupoSinNingunHorario_lanzaDiaFueraDeHorario() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        DiaFueraDeHorarioExcepcion ex = assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));

        assertTrue(ex.getMessage().toLowerCase().contains("ningun horario"));
    }

    @Test
    void registrarAtencionesPorClase_instructorDiaSinFranjaPeroGrupoTieneOtrosDias_lanzaDiaFueraDeHorario() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO)); // jueves
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja("MARTES", "08:00", "10:00")));

        DiaFueraDeHorarioExcepcion ex = assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));

        assertTrue(ex.getMessage().contains("MARTES"));
    }

    @Test
    void registrarAtencionesPorClase_instructorDiaConFranjaValida_permite() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));
    }

    // Una clase fuera de horario creada por un Coordinador NO exime al Instructor
    // de la regla de dia-en-horario (punto 6 del ticket).
    @Test
    void registrarAtencionesPorClase_instructorClaseExistenteFueraDeHorario_siguebloqueado() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));
    }

    @Test
    void registrarAtencionesPorClase_coordinadorSinNingunHorario_noAplicaLaReglaDeDia() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, COORD, "Coordinador"));
        verify(horarioGateway, never()).listarHorariosPorGrupo(any(), any(), anyInt(), anyInt());
    }

    @Test
    void registrarAtencionesPorClase_administradorSinNingunHorario_noAplicaLaReglaDeDia() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, "admin-1", "Administrador"));
        verify(horarioGateway, never()).listarHorariosPorGrupo(any(), any(), anyInt(), anyInt());
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

    // Decision documentada (punto 2 del ticket): DELETE /asistencia NO aplica la
    // regla de dia-en-horario -- solo fecha futura y plazo, igual que antes. Un
    // Instructor dentro de plazo puede eliminar aunque el grupo no tenga (o ya
    // no tenga) horario asignado ese dia.
    @Test
    void eliminarAsistencia_instructorSinNingunHorario_noAplicaLaReglaDeDia() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(asistenciaGateway.eliminarAsistencia("perf1", (long) CLASE_ID))
                .thenReturn(new Asistencia("perf1", CLASE_ID, 1));

        assertDoesNotThrow(() -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));
        verify(horarioGateway, never()).listarHorariosPorGrupo(any(), any(), anyInt(), anyInt());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerVentanaRegistro: ventana de registro de "hoy" hacia atras
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerVentanaRegistro_instructorSinProrrogaMesAnterior_esPrimerDiaDelMesActual() {
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of());

        VentanaRegistroDto dto = servicio.obtenerVentanaRegistro(INS, "Instructor");

        assertEquals("2026-10-01", dto.getFechaMinima());
        assertEquals("2026-10-15", dto.getFechaMaxima());
        assertEquals("Instructor", dto.getRol());
    }

    @Test
    void obtenerVentanaRegistro_instructorConProrrogaVigenteDelMesAnterior_bajaAlPrimerDiaDelMesAnterior() {
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 20));
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(INS, prorroga));

        VentanaRegistroDto dto = servicio.obtenerVentanaRegistro(INS, "Instructor");

        assertEquals("2026-09-01", dto.getFechaMinima());
        assertEquals("2026-10-15", dto.getFechaMaxima());
    }

    @Test
    void obtenerVentanaRegistro_instructorConProrrogaVencidaDelMesAnterior_seQuedaEnElMesActual() {
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 10));
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(INS, prorroga));

        VentanaRegistroDto dto = servicio.obtenerVentanaRegistro(INS, "Instructor");

        assertEquals("2026-10-01", dto.getFechaMinima());
    }

    @Test
    void obtenerVentanaRegistro_coordinador_sinLimiteInferior() {
        VentanaRegistroDto dto = servicio.obtenerVentanaRegistro(COORD, "Coordinador");

        assertNull(dto.getFechaMinima());
        assertEquals("2026-10-15", dto.getFechaMaxima());
        assertEquals("Coordinador", dto.getRol());
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void obtenerVentanaRegistro_administrador_sinLimiteInferior() {
        VentanaRegistroDto dto = servicio.obtenerVentanaRegistro("admin-1", "Administrador");

        assertNull(dto.getFechaMinima());
        assertEquals("2026-10-15", dto.getFechaMaxima());
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Autorizacion por grupo (rol Instructor) -- registrarAtencionesPorClase / eliminarAsistencia
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarAtencionesPorClase_instructorDeOtroGrupo_lanzaAccesoGrupoDenegadoAntesDeValidarFecha() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        doThrow(new AccesoGrupoDenegadoExcepcion("no es su grupo"))
                .when(autorizacionGrupo).verificarPropietario("Recreativo", "Natacion", 2026, 1, INS, "Instructor");

        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor"));

        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
        verify(horarioGateway, never()).listarHorariosPorGrupo(any(), any(), anyInt(), anyInt());
        verify(alumnoGateway, never()).existeAlumno(any());
    }

    @Test
    void registrarAtencionesPorClase_llamaAutorizacionGrupoConLaClaveDeLaClase() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();

        servicio.registrarAtencionesPorClase(List.of(), CLASE_ID, INS, "Instructor");

        verify(autorizacionGrupo).verificarPropietario("Recreativo", "Natacion", 2026, 1, INS, "Instructor");
    }

    @Test
    void eliminarAsistencia_instructorDeOtroGrupo_lanzaAccesoGrupoDenegadoAntesDeValidarFecha() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        doThrow(new AccesoGrupoDenegadoExcepcion("no es su grupo"))
                .when(autorizacionGrupo).verificarPropietario("Recreativo", "Natacion", 2026, 1, INS, "Instructor");

        assertThrows(AccesoGrupoDenegadoExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));

        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void eliminarAsistencia_llamaAutorizacionGrupoConLaClaveDeLaClase() {
        stubAsistenciaActiva("perf1");
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(claseConFecha(HOY_FIJO));
        sinProrroga();
        when(asistenciaGateway.eliminarAsistencia("perf1", (long) CLASE_ID))
                .thenReturn(new Asistencia("perf1", CLASE_ID, 1));

        servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor");

        verify(autorizacionGrupo).verificarPropietario("Recreativo", "Natacion", 2026, 1, INS, "Instructor");
    }

    // Caso D del ticket: tras un 403, nada debe cambiar en la base de datos --
    // ya cubierto arriba con verify(..., never()), se deja explicito aqui para
    // la asistencia ya eliminada (no debe ni intentar reactivarla).
    @Test
    void eliminarAsistencia_yaEliminadaYInstructorDeOtroGrupo_noLlegaAVerificarAutorizacion() {
        Asistencia yaEliminada = new Asistencia("perf1", CLASE_ID, 1);
        when(asistenciaGateway.existeAsistencia("perf1", CLASE_ID)).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("perf1", CLASE_ID)).thenReturn(Optional.of(yaEliminada));

        assertThrows(YaExisteElementoExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", (long) CLASE_ID, INS, "Instructor"));

        verify(autorizacionGrupo, never()).verificarPropietario(any(), any(), any(), any(), any(), any());
    }
}
