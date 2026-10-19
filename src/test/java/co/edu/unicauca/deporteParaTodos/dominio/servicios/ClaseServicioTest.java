package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DiaFueraDeHorarioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.DuracionManualInvalidaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.JustificacionInvalidaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Clase;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClaseServicioTest {

    @Mock
    private IClaseGateway claseGateway;

    @Mock
    private IHorarioGateway horarioGateway;

    @Mock
    private IProrrogaRegistroAsistenciaGateway prorrogaGateway;

    @Mock
    private Clock clock;

    @InjectMocks
    private ClaseServicio claseServicio;

    private static final String  CATEGORIA = "Recreativo";
    private static final String  CURSO     = "Natacion";
    private static final Integer ANIO      = 2025;
    private static final Integer ITERABLE  = 1;
    private static final String  INS       = "INS001";
    private static final String  COORD     = "coord-1";

    // "hoy" = 2026-10-15, jueves -- mes actual = (2026, 10).
    private static final LocalDate HOY_FIJO = LocalDate.of(2026, 10, 15);

    @BeforeEach
    void configurarClockPorDefecto() {
        lenient().when(clock.instant()).thenReturn(HOY_FIJO.atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));
    }

    private Clase claseBase() {
        Clase c = new Clase();
        c.setCodigo(1);
        c.setCategoria(CATEGORIA);
        c.setCurso(CURSO);
        c.setAnio(ANIO);
        c.setIterable(ITERABLE);
        c.setIdInstructor(INS);
        c.setFecha(HOY_FIJO);
        c.setHoras(1);
        c.setMinutos(30);
        c.setEliminado(0);
        return c;
    }

    private Horario franja(Integer id, String dia, String inicio, String fin) {
        return new Horario(id, CATEGORIA, CURSO, ANIO, ITERABLE, dia, inicio, fin, "Cancha", 0);
    }

    // Por defecto: sin franjas, sin prorroga -- cada test agrega lo que necesite.
    private void sinFranjasNiProrroga() {
        lenient().when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // obtenerClasesGrupo / existeClaseEnFecha (sin cambios de firma)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerClasesGrupo_exitoso_retornaListado() {
        when(claseGateway.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of(claseBase(), claseBase()));

        List<Clase> resultado = claseServicio.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE);

        assertEquals(2, resultado.size());
    }

    @Test
    void obtenerClasesGrupo_vacio_lanzaListadoVacioExcepcion() {
        when(claseGateway.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(Collections.emptyList());

        assertThrows(ListadoVacioExcepcion.class,
                () -> claseServicio.obtenerClasesGrupo(CATEGORIA, CURSO, ANIO, ITERABLE));
    }

    @Test
    void existeClaseEnFecha_delegaAlGateway() {
        LocalDate fecha = LocalDate.of(2026, 1, 1);
        when(claseGateway.existeClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, fecha)).thenReturn(true);

        assertTrue(claseServicio.existeClaseEnFecha(CATEGORIA, CURSO, ANIO, ITERABLE, fecha));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // insertarClase -- matriz rol x estado (fecha/plazo)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void insertarClase_instructorMesActual_permiteYCalculaHorasDesdeHorario() {
        sinFranjasNiProrroga();
        Clase datos = claseBase(); // fecha = HOY_FIJO, jueves
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(horarioGateway.listarHorariosPorGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of(franja(1, "JUEVES", "14:00", "16:00")));
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        Clase resultado = claseServicio.insertarClase(datos, INS, "Instructor");

        assertEquals(2, resultado.getHoras());
        assertEquals(0, resultado.getMinutos());
    }

    @Test
    void insertarClase_instructorMesAnteriorSinProrroga_lanzaRegistroAsistenciaVencidoSinGuardar() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setFecha(LocalDate.of(2026, 9, 10)); // mes anterior, sin prorroga

        assertThrows(RegistroAsistenciaVencidoExcepcion.class,
                () -> claseServicio.insertarClase(datos, INS, "Instructor"));

        verify(claseGateway, never()).insertarClase(any());
    }

    @Test
    void insertarClase_instructorMesAnteriorConProrrogaVigente_permite() {
        Clase datos = claseBase();
        datos.setFecha(LocalDate.of(2026, 9, 10)); // tambien jueves (35 dias = 5 semanas antes de HOY_FIJO)
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 20)); // posterior a HOY_FIJO
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(INS, prorroga));
        // SCRUM-80 (correccion del cliente): el Instructor ahora tambien necesita
        // un dia con horario para pasar -- este test prueba la prorroga, no el
        // dia, asi que se le da una franja valida ese jueves.
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja(1, "JUEVES", "14:00", "16:00")));
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, INS, "Instructor"));
    }

    @Test
    void insertarClase_fechaFutura_instructor_lanzaFechaClaseFuturaSinGuardar() {
        Clase datos = claseBase();
        datos.setFecha(HOY_FIJO.plusDays(1));

        assertThrows(FechaClaseFuturaExcepcion.class,
                () -> claseServicio.insertarClase(datos, INS, "Instructor"));

        verify(claseGateway, never()).insertarClase(any());
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void insertarClase_fechaFutura_coordinador_igualLanzaFechaClaseFutura() {
        Clase datos = claseBase();
        datos.setFecha(HOY_FIJO.plusDays(1));

        assertThrows(FechaClaseFuturaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));

        verify(claseGateway, never()).insertarClase(any());
    }

    @Test
    void insertarClase_coordinadorMesAnteriorSinProrroga_noTieneLimiteDePlazo() {
        Clase datos = claseBase();
        datos.setFecha(LocalDate.of(2026, 9, 10));
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        datos.setJustificacion("clase dictada por el coordinador, sin horario ese dia");
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    @Test
    void insertarClase_administradorMesAnteriorSinProrroga_noTieneLimiteDePlazo() {
        Clase datos = claseBase();
        datos.setFecha(LocalDate.of(2026, 9, 10));
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        datos.setJustificacion("clase dictada por el administrador, sin horario ese dia");
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, "admin-1", "Administrador"));
    }

    // ── sin franja ese dia: justificacion y duracion manual ─────────────────
    // SCRUM-80 (correccion del cliente, 15 oct 2026): el Instructor ya NO puede
    // llegar a esta rama -- si el grupo no tiene horario ese dia, se le bloquea
    // con DiaFueraDeHorarioExcepcion ANTES de evaluar justificacion/duracion
    // (ver mas abajo). Esta rama sigue existiendo solo para Coordinador y
    // Administrador, que no estan sujetos a la regla de dia-en-horario (rol que
    // antes era INS/"Instructor" ahora es COORD/"Coordinador" en estos tests).

    @Test
    void insertarClase_sinFranjaConJustificacionYDuracionValidas_permite() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setHoras(2);
        datos.setMinutos(0);
        datos.setJustificacion("instalacion en mantenimiento, clase dictada en otro espacio");
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        Clase resultado = claseServicio.insertarClase(datos, COORD, "Coordinador");

        assertEquals(2, resultado.getHoras());
        assertEquals(0, resultado.getMinutos());
    }

    @Test
    void insertarClase_sinFranjaSinJustificacion_lanzaJustificacionInvalida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion(null);

        assertThrows(JustificacionInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
        verify(claseGateway, never()).insertarClase(any());
    }

    @Test
    void insertarClase_sinFranjaJustificacionSoloEspacios_lanzaJustificacionInvalida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("                     ");

        assertThrows(JustificacionInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_sinFranjaJustificacionConEspaciosSobrantes_lanzaJustificacionInvalida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("  justificacion con espacios al inicio y al final  ");

        assertThrows(JustificacionInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_sinFranjaJustificacion9Caracteres_lanzaJustificacionInvalida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("123456789"); // 9 caracteres, el minimo es 10

        assertThrows(JustificacionInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_sinFranjaJustificacion10Caracteres_esValida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("1234567890"); // exactamente 10, el minimo
        datos.setHoras(1);
        datos.setMinutos(0);
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_sinFranjaJustificacion501Caracteres_lanzaJustificacionInvalida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("a".repeat(501));

        assertThrows(JustificacionInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_sinFranjaJustificacion500Caracteres_esValida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("a".repeat(500));
        datos.setHoras(1);
        datos.setMinutos(0);
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_sinFranjaDuracionManualCero_lanzaDuracionManualInvalida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("justificacion valida de mas de diez caracteres");
        datos.setHoras(0);
        datos.setMinutos(0);

        assertThrows(DuracionManualInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
        verify(claseGateway, never()).insertarClase(any());
    }

    @Test
    void insertarClase_sinFranjaDuracionManualMasDe12Horas_lanzaDuracionManualInvalida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("justificacion valida de mas de diez caracteres");
        datos.setHoras(12);
        datos.setMinutos(1); // 12h01 > 12h

        assertThrows(DuracionManualInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_sinFranjaDuracionManualExactamente12Horas_esValida() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("justificacion valida de mas de diez caracteres");
        datos.setHoras(12);
        datos.setMinutos(0);
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    // Decision documentada en CalculadoraDuracionClaseTest: una franja invalida
    // (fin <= inicio) no cuenta como franja -- equivale a "sin horario ese dia".
    @Test
    void insertarClase_soloFranjaInvalidaEseDia_equivaleASinHorario_exigeJustificacion() {
        Clase datos = claseBase();
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja(1, "JUEVES", "10:00", "10:00")));
        datos.setJustificacion(null);

        assertThrows(JustificacionInvalidaExcepcion.class,
                () -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    // ── SCRUM-80 (correccion del cliente, 15 oct 2026): dia-en-horario (Instructor) ──

    @Test
    void insertarClase_instructorGrupoSinNingunHorario_lanzaDiaFueraDeHorarioMencionandoQueNoHayHorario() {
        Clase datos = claseBase(); // fecha = HOY_FIJO, jueves
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt())).thenReturn(List.of());
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());

        DiaFueraDeHorarioExcepcion ex = assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> claseServicio.insertarClase(datos, INS, "Instructor"));

        assertTrue(ex.getMessage().toLowerCase().contains("ningun horario"));
        verify(claseGateway, never()).existeClaseEnFecha(any(), any(), any(), any(), any());
        verify(claseGateway, never()).insertarClase(any());
    }

    @Test
    void insertarClase_instructorDiaSinFranjaPeroGrupoTieneOtrosDias_lanzaDiaFueraDeHorarioMencionandoLosDias() {
        Clase datos = claseBase(); // fecha = HOY_FIJO, jueves -- el grupo solo tiene horario martes
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja(1, "MARTES", "08:00", "10:00")));
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());

        DiaFueraDeHorarioExcepcion ex = assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> claseServicio.insertarClase(datos, INS, "Instructor"));

        assertTrue(ex.getMessage().contains("MARTES"));
        verify(claseGateway, never()).existeClaseEnFecha(any(), any(), any(), any(), any());
    }

    @Test
    void insertarClase_instructorSoloFranjaInvalidaEseDia_lanzaDiaFueraDeHorario() {
        Clase datos = claseBase(); // fecha = HOY_FIJO, jueves
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja(1, "JUEVES", "10:00", "10:00"))); // fin == inicio -- invalida
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());

        assertThrows(DiaFueraDeHorarioExcepcion.class,
                () -> claseServicio.insertarClase(datos, INS, "Instructor"));
    }

    @Test
    void insertarClase_instructorDiaConFranjaValida_noLanzaDiaFueraDeHorario() {
        sinFranjasNiProrroga();
        Clase datos = claseBase(); // fecha = HOY_FIJO, jueves
        when(horarioGateway.listarHorariosPorGrupo(CATEGORIA, CURSO, ANIO, ITERABLE))
                .thenReturn(List.of(franja(1, "JUEVES", "14:00", "16:00")));
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, INS, "Instructor"));
    }

    @Test
    void insertarClase_coordinadorDiaSinNingunHorario_noAplicaLaReglaDeDia() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("clase dictada por el coordinador, sin horario ese dia");
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, COORD, "Coordinador"));
    }

    @Test
    void insertarClase_administradorDiaSinNingunHorario_noAplicaLaReglaDeDia() {
        sinFranjasNiProrroga();
        Clase datos = claseBase();
        datos.setJustificacion("clase dictada por el administrador, sin horario ese dia");
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> claseServicio.insertarClase(datos, "admin-1", "Administrador"));
    }

    // Orden de validaciones (punto 3 del ticket): fecha futura y plazo se
    // evaluan ANTES del dia-en-horario -- ya cubierto por los tests existentes
    // de fecha futura (sin horario stubbeado, ni siquiera se llega a mirarlo) y
    // de "mes anterior sin prorroga" (sinFranjasNiProrroga + lanza
    // RegistroAsistenciaVencidoExcepcion, no DiaFueraDeHorarioExcepcion).

    @Test
    void insertarClase_conFranja_ignoraHorasYMinutosEnviadosPorElCliente() {
        Clase datos = claseBase();
        datos.setHoras(99); // el cliente manda cualquier cosa -- debe ignorarse
        datos.setMinutos(59);
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja(1, "JUEVES", "08:00", "11:00"))); // 3h
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        Clase resultado = claseServicio.insertarClase(datos, INS, "Instructor");

        assertEquals(3, resultado.getHoras());
        assertEquals(0, resultado.getMinutos());
    }

    @Test
    void insertarClase_variasFranjasElMismoDia_sumanLaDuracion() {
        Clase datos = claseBase();
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(
                        franja(1, "JUEVES", "08:00", "10:00"),
                        franja(2, "JUEVES", "14:00", "16:00")));
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(false);
        when(claseGateway.insertarClase(any(Clase.class))).thenAnswer(inv -> inv.getArgument(0));

        Clase resultado = claseServicio.insertarClase(datos, INS, "Instructor");

        assertEquals(4, resultado.getHoras());
        assertEquals(0, resultado.getMinutos());
    }

    // ── Reutilizar clase existente (ClaseFechaDuplicadaIT) respetando el plazo ──

    @Test
    void insertarClase_yaExisteClaseEnFecha_reutilizaSinExigirJustificacionDeNuevo() {
        Clase datos = claseBase();
        datos.setJustificacion(null); // el segundo click no manda justificacion -- no debe hacer falta
        Clase existente = claseBase();
        existente.setCodigo(99);
        existente.setJustificacion("justificacion que quedo guardada en el primer click");
        // SCRUM-80 (correccion del cliente): el dia-en-horario SI se consulta
        // ahora para el Instructor (antes de mirar si la clase ya existe), asi
        // que horarioGateway se stubbea con una franja valida para HOY_FIJO
        // (jueves) -- pero solo UNA vez: la reutilizacion retorna antes de
        // volver a pedirlo para el calculo de duracion.
        when(horarioGateway.listarHorariosPorGrupo(any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(franja(1, "JUEVES", "14:00", "16:00")));
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(claseGateway.existeClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(true);
        when(claseGateway.obtenerClaseEnFecha(any(), any(), any(), any(), any())).thenReturn(existente);

        Clase resultado = claseServicio.insertarClase(datos, INS, "Instructor");

        assertEquals(99, resultado.getCodigo());
        verify(claseGateway, never()).insertarClase(any());
        verify(horarioGateway, times(1)).listarHorariosPorGrupo(any(), any(), anyInt(), anyInt());
    }

    @Test
    void insertarClase_yaExisteClaseEnFecha_peroFueraDePlazo_lanzaExcepcionSinReutilizar() {
        Clase datos = claseBase();
        datos.setFecha(LocalDate.of(2026, 9, 10)); // mes anterior, sin prorroga
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of());

        assertThrows(RegistroAsistenciaVencidoExcepcion.class,
                () -> claseServicio.insertarClase(datos, INS, "Instructor"));

        verify(claseGateway, never()).existeClaseEnFecha(any(), any(), any(), any(), any());
        verify(claseGateway, never()).obtenerClaseEnFecha(any(), any(), any(), any(), any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // eliminarClase -- misma matriz rol x estado, sobre la fecha de la clase existente
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void eliminarClase_instructorMesActual_permiteYDelegaAlGateway() {
        when(claseGateway.existeClase(1)).thenReturn(true);
        when(claseGateway.obtenerClase(1)).thenReturn(claseBase()); // fecha = HOY_FIJO
        when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
        when(claseGateway.eliminarClase(1)).thenReturn(claseBase());

        Clase resultado = claseServicio.eliminarClase(1, INS, "Instructor");

        assertNotNull(resultado);
        verify(claseGateway).eliminarClase(1);
    }

    @Test
    void eliminarClase_noExiste_sigueLanzandoNoExisteExcepcionAntesDeValidarFecha() {
        when(claseGateway.existeClase(99)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class, () -> claseServicio.eliminarClase(99, INS, "Instructor"));

        verify(claseGateway, never()).eliminarClase(anyInt());
        verify(claseGateway, never()).obtenerClase(anyInt());
    }

    @Test
    void eliminarClase_instructorMesAnteriorSinProrroga_lanzaRegistroAsistenciaVencidoSinEliminar() {
        Clase existente = claseBase();
        existente.setFecha(LocalDate.of(2026, 9, 10));
        when(claseGateway.existeClase(1)).thenReturn(true);
        when(claseGateway.obtenerClase(1)).thenReturn(existente);
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of());

        assertThrows(RegistroAsistenciaVencidoExcepcion.class,
                () -> claseServicio.eliminarClase(1, INS, "Instructor"));

        verify(claseGateway, never()).eliminarClase(anyInt());
    }

    @Test
    void eliminarClase_instructorMesAnteriorConProrrogaVigente_permite() {
        Clase existente = claseBase();
        existente.setFecha(LocalDate.of(2026, 9, 10));
        ProrrogaRegistroAsistencia prorroga = new ProrrogaRegistroAsistencia();
        prorroga.setFechaLimite(LocalDate.of(2026, 10, 20));
        when(claseGateway.existeClase(1)).thenReturn(true);
        when(claseGateway.obtenerClase(1)).thenReturn(existente);
        when(prorrogaGateway.obtenerProrrogasPorMes(2026, 9)).thenReturn(Map.of(INS, prorroga));
        when(claseGateway.eliminarClase(1)).thenReturn(existente);

        assertDoesNotThrow(() -> claseServicio.eliminarClase(1, INS, "Instructor"));
    }

    @Test
    void eliminarClase_coordinadorMesAnteriorSinProrroga_noTieneLimiteDePlazo() {
        Clase existente = claseBase();
        existente.setFecha(LocalDate.of(2026, 9, 10));
        when(claseGateway.existeClase(1)).thenReturn(true);
        when(claseGateway.obtenerClase(1)).thenReturn(existente);
        when(claseGateway.eliminarClase(1)).thenReturn(existente);

        assertDoesNotThrow(() -> claseServicio.eliminarClase(1, COORD, "Coordinador"));
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }
}
