package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.sql.Timestamp;
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
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AccesoGrupoDenegadoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.AlumnoFueraDeGrupoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ConfirmacionSinAsistentesExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DiaFueraDeHorarioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Alumno;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Inscripcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Perfil;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ResultadoConfirmacionAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.ItemConfirmacionAsistenciaDto;
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
    private IInscripcionGateway inscripcionGateway;

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

    // Faltas acumuladas (etapa 4 de 5) -----------------------------------------

    private Clase claseConfirmada(LocalDate fecha) {
        Clase c = claseConFecha(fecha);
        c.setAsistenciaConfirmadaEn(LocalDateTime.of(2026, 10, 10, 8, 0));
        c.setAsistenciaConfirmadaPor("coord-anterior");
        return c;
    }

    private Alumno alumnoDe(String perfId) {
        Perfil perfil = new Perfil();
        perfil.setId(perfId);
        Alumno alumno = new Alumno();
        alumno.setPerfil(perfil);
        return alumno;
    }

    // Universo U por defecto para confirmarAsistencias(): 1 alumno, inscrito
    // antes de la fecha de la clase (elegible para ausencia).
    private void stubUniversoYElegibilidad(Clase clase, String alumnoId, LocalDate fechaInscripcion) {
        when(alumnoGateway.obtenerAlumnosGrupo(clase.getCategoria(), clase.getCurso(), clase.getAnio(), clase.getIterable()))
                .thenReturn(List.of(alumnoDe(alumnoId)));
        stubElegibilidad(clase, alumnoId, fechaInscripcion);
    }

    // Solo la inscripcion (fecha) de un alumnoId -- para tests que ya armaron
    // el universo (obtenerAlumnosGrupo) con varios alumnos por su cuenta.
    private void stubElegibilidad(Clase clase, String alumnoId, LocalDate fechaInscripcion) {
        lenient().when(inscripcionGateway.existeInscripcion(alumnoId, clase.getCategoria(), clase.getCurso(),
                clase.getAnio(), clase.getIterable())).thenReturn(true);
        Inscripcion inscripcion = new Inscripcion(alumnoId, clase.getCategoria(), clase.getCurso(),
                clase.getAnio(), clase.getIterable(), Timestamp.valueOf(fechaInscripcion.atStartOfDay()), null, "INSCRITO");
        lenient().when(inscripcionGateway.obtenerInscripcion(alumnoId, clase.getCategoria(), clase.getCurso(),
                clase.getAnio(), clase.getIterable())).thenReturn(inscripcion);
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

    // ──────────────────────────────────────────────────────────────────────────
    // Faltas acumuladas (etapa 4 de 5): confirmarAsistencias()
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void confirmarAsistencias_registroNormal_marcaPresenteYAusenteYConfirmaLaClase() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        when(alumnoGateway.obtenerAlumnosGrupo("Recreativo", "Natacion", 2026, 1))
                .thenReturn(List.of(alumnoDe("alum1"), alumnoDe("alum2")));
        stubElegibilidad(clase, "alum2", HOY_FIJO.minusDays(30));
        lenient().when(inscripcionGateway.existeInscripcion("alum1", "Recreativo", "Natacion", 2026, 1)).thenReturn(true);

        ItemConfirmacionAsistenciaDto item = new ItemConfirmacionAsistenciaDto("alum1", true);
        ResultadoConfirmacionAsistencia resultado = servicio.confirmarAsistencias(
                CLASE_ID, List.of(item), false, INS, "Instructor");

        verify(asistenciaGateway).marcarPresente("alum1", CLASE_ID);
        verify(asistenciaGateway).marcarAusente("alum2", CLASE_ID);
        verify(claseGateway).confirmarAsistencia(eq(CLASE_ID), any(LocalDateTime.class), eq(INS));
        assertEquals(1, resultado.getPresentes());
        assertEquals(1, resultado.getAusentes());
        assertEquals(CLASE_ID, resultado.getCodigoClase());
    }

    @Test
    void confirmarAsistencias_todosAusentesConBandera_permite() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        stubUniversoYElegibilidad(clase, "alum1", HOY_FIJO.minusDays(30));

        ResultadoConfirmacionAsistencia resultado = servicio.confirmarAsistencias(
                CLASE_ID, List.of(), true, INS, "Instructor");

        verify(asistenciaGateway).marcarAusente("alum1", CLASE_ID);
        assertEquals(0, resultado.getPresentes());
        assertEquals(1, resultado.getAusentes());
    }

    @Test
    void confirmarAsistencias_todosAusentesSinBandera_lanzaConfirmacionSinAsistentesYNoGuardaNada() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();

        assertThrows(ConfirmacionSinAsistentesExcepcion.class,
                () -> servicio.confirmarAsistencias(CLASE_ID, List.of(), false, INS, "Instructor"));

        verify(alumnoGateway, never()).obtenerAlumnosGrupo(any(), any(), anyInt(), anyInt());
        verify(asistenciaGateway, never()).marcarPresente(any(), anyInt());
        verify(asistenciaGateway, never()).marcarAusente(any(), anyInt());
        verify(claseGateway, never()).confirmarAsistencia(any(), any(), any());
    }

    @Test
    void confirmarAsistencias_idempotente_mismoResultadoAlRepetir() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        stubUniversoYElegibilidad(clase, "alum1", HOY_FIJO.minusDays(30));

        ItemConfirmacionAsistenciaDto item = new ItemConfirmacionAsistenciaDto("alum1", true);
        ResultadoConfirmacionAsistencia primera = servicio.confirmarAsistencias(
                CLASE_ID, List.of(item), false, INS, "Instructor");
        ResultadoConfirmacionAsistencia segunda = servicio.confirmarAsistencias(
                CLASE_ID, List.of(item), false, INS, "Instructor");

        assertEquals(primera.getPresentes(), segunda.getPresentes());
        assertEquals(primera.getAusentes(), segunda.getAusentes());
        verify(asistenciaGateway, times(2)).marcarPresente("alum1", CLASE_ID);
        // Los campos de auditoria SI se actualizan a proposito en cada corrida.
        verify(claseGateway, times(2)).confirmarAsistencia(eq(CLASE_ID), any(LocalDateTime.class), eq(INS));
    }

    @Test
    void confirmarAsistencias_reconfirmacionQueCorrige_segundaCorridaCambiaDePresenteAAusente() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        stubUniversoYElegibilidad(clase, "alum1", HOY_FIJO.minusDays(30));

        servicio.confirmarAsistencias(CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)),
                false, INS, "Instructor");
        verify(asistenciaGateway).marcarPresente("alum1", CLASE_ID);

        ResultadoConfirmacionAsistencia corregida = servicio.confirmarAsistencias(
                CLASE_ID, List.of(), true, INS, "Instructor");

        verify(asistenciaGateway).marcarAusente("alum1", CLASE_ID);
        assertEquals(0, corregida.getPresentes());
        assertEquals(1, corregida.getAusentes());
    }

    @Test
    void confirmarAsistencias_idPerfilFueraDeU_lanzaAlumnoFueraDeGrupoYNoGuardaNada() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        when(alumnoGateway.obtenerAlumnosGrupo("Recreativo", "Natacion", 2026, 1))
                .thenReturn(List.of(alumnoDe("alum1")));

        List<ItemConfirmacionAsistenciaDto> items = List.of(
                new ItemConfirmacionAsistenciaDto("alum1", true),
                new ItemConfirmacionAsistenciaDto("ajeno", true));

        assertThrows(AlumnoFueraDeGrupoExcepcion.class,
                () -> servicio.confirmarAsistencias(CLASE_ID, items, false, INS, "Instructor"));

        // Fallo parcial: NADA se guarda, ni siquiera para "alum1" que si era valido.
        verify(asistenciaGateway, never()).marcarPresente(any(), anyInt());
        verify(asistenciaGateway, never()).marcarAusente(any(), anyInt());
        verify(claseGateway, never()).confirmarAsistencia(any(), any(), any());
    }

    @Test
    void confirmarAsistencias_instructorDeOtroGrupo_lanzaAccesoGrupoDenegadoYNoLeeUniverso() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        doThrow(new AccesoGrupoDenegadoExcepcion("ajeno"))
                .when(autorizacionGrupo).verificarPropietario("Recreativo", "Natacion", 2026, 1, INS, "Instructor");

        assertThrows(AccesoGrupoDenegadoExcepcion.class, () -> servicio.confirmarAsistencias(
                CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)), false, INS, "Instructor"));

        verify(alumnoGateway, never()).obtenerAlumnosGrupo(any(), any(), anyInt(), anyInt());
    }

    @Test
    void confirmarAsistencias_instructorMesAnteriorSinProrroga_lanzaRegistroAsistenciaVencido() {
        Clase clase = claseConFecha(LocalDate.of(2026, 9, 10));
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();

        assertThrows(RegistroAsistenciaVencidoExcepcion.class, () -> servicio.confirmarAsistencias(
                CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)), false, INS, "Instructor"));

        verify(alumnoGateway, never()).obtenerAlumnosGrupo(any(), any(), anyInt(), anyInt());
    }

    @Test
    void confirmarAsistencias_instructorSinHorarioEseDia_lanzaDiaFueraDeHorario() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        lenient().when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt())).thenReturn(List.of());

        assertThrows(DiaFueraDeHorarioExcepcion.class, () -> servicio.confirmarAsistencias(
                CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)), false, INS, "Instructor"));

        verify(alumnoGateway, never()).obtenerAlumnosGrupo(any(), any(), anyInt(), anyInt());
    }

    @Test
    void confirmarAsistencias_coordinador_exentoDePlazoYDeHorario_permite() {
        Clase clase = claseConFecha(LocalDate.of(2026, 1, 1));
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        lenient().when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        stubUniversoYElegibilidad(clase, "alum1", LocalDate.of(2025, 12, 1));

        ResultadoConfirmacionAsistencia resultado = servicio.confirmarAsistencias(
                CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)), false, COORD, "Coordinador");

        assertEquals(1, resultado.getPresentes());
        verify(horarioGateway, never()).listarHorariosPorGrupo(any(), any(), anyInt(), anyInt());
    }

    @Test
    void confirmarAsistencias_claseInexistente_lanzaNoExisteExcepcion() {
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> servicio.confirmarAsistencias(
                CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)), false, INS, "Instructor"));

        verify(autorizacionGrupo, never()).verificarPropietario(any(), any(), any(), any(), any(), any());
    }

    @Test
    void confirmarAsistencias_claseEliminada_lanzaNoExisteExcepcion() {
        Clase clase = claseConFecha(HOY_FIJO);
        clase.setEliminado(1);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();

        assertThrows(NoExisteExcepcion.class, () -> servicio.confirmarAsistencias(
                CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)), false, INS, "Instructor"));

        verify(alumnoGateway, never()).obtenerAlumnosGrupo(any(), any(), anyInt(), anyInt());
    }

    // Regla 3 del ticket: PRESENTE se registra SIEMPRE, sea o no elegible --
    // un alumno inscrito DESPUES de la fecha de la clase puede ser marcado
    // presente igual (el Instructor lo decide), pero si no viene marcado NO
    // genera una fila AUSENTE (no era elegible) ni toca ninguna fila existente.
    @Test
    void confirmarAsistencias_alumnoInscritoDespuesDeLaClase_marcaPresenteSiElInstructorLoIndica() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        stubUniversoYElegibilidad(clase, "alum1", HOY_FIJO.plusDays(5));

        ResultadoConfirmacionAsistencia resultado = servicio.confirmarAsistencias(CLASE_ID,
                List.of(new ItemConfirmacionAsistenciaDto("alum1", true)), false, INS, "Instructor");

        verify(asistenciaGateway).marcarPresente("alum1", CLASE_ID);
        assertEquals(1, resultado.getPresentes());
    }

    @Test
    void confirmarAsistencias_alumnoInscritoDespuesSinMarcaDePresente_noCreaFilaAusente() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        stubUniversoYElegibilidad(clase, "alum1", HOY_FIJO.plusDays(5));
        when(asistenciaGateway.obtenerAsistencia("alum1", CLASE_ID)).thenReturn(Optional.empty());

        ResultadoConfirmacionAsistencia resultado = servicio.confirmarAsistencias(
                CLASE_ID, List.of(), true, INS, "Instructor");

        verify(asistenciaGateway, never()).marcarAusente(any(), anyInt());
        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
        assertEquals(0, resultado.getAusentes());
    }

    // Regla 5 del ticket: un alumno desvinculado (fuera de U) no aparece en el
    // universo -- sus filas, si existen, nunca se tocan desde este metodo.
    @Test
    void confirmarAsistencias_alumnoDesvinculado_noApareceEnUniversoNiSeTocaSuFila() {
        Clase clase = claseConFecha(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        // El desvinculado ("alum-viejo") no aparece en obtenerAlumnosGrupo.
        stubUniversoYElegibilidad(clase, "alum1", HOY_FIJO.minusDays(10));

        servicio.confirmarAsistencias(CLASE_ID, List.of(new ItemConfirmacionAsistenciaDto("alum1", true)),
                false, INS, "Instructor");

        verify(asistenciaGateway, never()).marcarPresente(eq("alum-viejo"), anyInt());
        verify(asistenciaGateway, never()).marcarAusente(eq("alum-viejo"), anyInt());
        verify(asistenciaGateway, never()).eliminarAsistencia(eq("alum-viejo"), any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Faltas acumuladas (etapa 4 de 5): registrarAtencionesPorClase() y
    // eliminarAsistencia() cuando la clase YA esta confirmada (regla 8)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarAtencionesPorClase_claseConfirmada_marcarTrue_forzaPresente() {
        Clase clase = claseConfirmada(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, true);
        servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway).marcarPresente("alum1", CLASE_ID);
        verify(asistenciaGateway, never()).InsertarAsistencia(any());
        verify(asistenciaGateway, never()).reactivarAsistencia(any(), anyInt());
    }

    @Test
    void registrarAtencionesPorClase_claseConfirmadaYAlumnoElegible_marcarFalse_marcaAusente() {
        Clase clase = claseConfirmada(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(inscripcionGateway.existeInscripcion("alum1", "Recreativo", "Natacion", 2026, 1)).thenReturn(true);
        Inscripcion inscripcion = new Inscripcion("alum1", "Recreativo", "Natacion", 2026, 1,
                Timestamp.valueOf(HOY_FIJO.minusDays(30).atStartOfDay()), null, "INSCRITO");
        when(inscripcionGateway.obtenerInscripcion("alum1", "Recreativo", "Natacion", 2026, 1)).thenReturn(inscripcion);

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, false);
        servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway).marcarAusente("alum1", CLASE_ID);
        verify(asistenciaGateway, never()).eliminarAsitencia(any(), anyInt());
    }

    @Test
    void registrarAtencionesPorClase_claseConfirmadaYAlumnoNoElegible_eliminaLogicamente() {
        Clase clase = claseConfirmada(HOY_FIJO);
        when(claseGateway.existeClase(CLASE_ID)).thenReturn(true);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(inscripcionGateway.existeInscripcion("alum1", "Recreativo", "Natacion", 2026, 1)).thenReturn(false);
        when(asistenciaGateway.obtenerAsistencia("alum1", CLASE_ID))
                .thenReturn(Optional.of(new Asistencia("alum1", CLASE_ID, 0)));

        AtencionDto dto = new AtencionDto("alum1", CLASE_ID, false);
        servicio.registrarAtencionesPorClase(List.of(dto), CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway).eliminarAsistencia("alum1", (long) CLASE_ID);
        verify(asistenciaGateway, never()).marcarAusente(any(), anyInt());
        verify(asistenciaGateway, never()).eliminarAsitencia(any(), anyInt());
    }

    @Test
    void eliminarAsistencia_claseConfirmadaYElegible_marcaAusenteEnVezDeEliminarLogicamente() {
        stubAsistenciaActiva("alum1");
        Clase clase = claseConfirmada(HOY_FIJO);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        when(inscripcionGateway.existeInscripcion("alum1", "Recreativo", "Natacion", 2026, 1)).thenReturn(true);
        Inscripcion inscripcion = new Inscripcion("alum1", "Recreativo", "Natacion", 2026, 1,
                Timestamp.valueOf(HOY_FIJO.minusDays(30).atStartOfDay()), null, "INSCRITO");
        when(inscripcionGateway.obtenerInscripcion("alum1", "Recreativo", "Natacion", 2026, 1)).thenReturn(inscripcion);

        servicio.eliminarAsistencia("alum1", (long) CLASE_ID, INS, "Instructor");

        verify(asistenciaGateway).marcarAusente("alum1", CLASE_ID);
        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
    }

    @Test
    void eliminarAsistencia_claseConfirmadaYNoElegible_eliminaLogicamente() {
        stubAsistenciaActiva("alum1");
        Clase clase = claseConfirmada(HOY_FIJO);
        when(claseGateway.obtenerClase(CLASE_ID)).thenReturn(clase);
        sinProrroga();
        when(inscripcionGateway.existeInscripcion("alum1", "Recreativo", "Natacion", 2026, 1)).thenReturn(false);
        Asistencia eliminada = new Asistencia("alum1", CLASE_ID, 1);
        when(asistenciaGateway.eliminarAsistencia("alum1", (long) CLASE_ID)).thenReturn(eliminada);

        Asistencia resultado = servicio.eliminarAsistencia("alum1", (long) CLASE_ID, INS, "Instructor");

        assertEquals(1, resultado.getEliminado());
        verify(asistenciaGateway).eliminarAsistencia("alum1", (long) CLASE_ID);
        verify(asistenciaGateway, never()).marcarAusente(any(), anyInt());
    }
}
