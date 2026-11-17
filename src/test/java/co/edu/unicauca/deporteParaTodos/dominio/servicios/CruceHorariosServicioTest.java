package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IHorarioGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IInscripcionGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.CruceHorarioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Horario;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.HorarioInscrito;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.InscripcionEnEspera;

@ExtendWith(MockitoExtension.class)
class CruceHorariosServicioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 6, 15);

    @Mock
    private IInscripcionGateway inscripcionGateway;
    @Mock
    private IHorarioGateway horarioGateway;
    @Mock
    private IAlumnoGateway alumnoGateway;

    private Clock clock;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(HOY.atStartOfDay(ZoneId.of("America/Bogota")).toInstant(), ZoneId.of("America/Bogota"));
        grupo = new Grupo();
        grupo.setCategoria("cat1");
        grupo.setCurso("cur1");
        grupo.setAnio(2026);
        grupo.setIterable(1);
        grupo.setPeriodo(1);
    }

    private CruceHorariosServicio servicio(boolean validar) {
        return new CruceHorariosServicio(inscripcionGateway, horarioGateway, alumnoGateway, clock, validar);
    }

    private static Horario horario(int id, String dia, String ini, String fin) {
        Horario h = new Horario();
        h.setId(id);
        h.setDia(dia);
        h.setHoraInicio(ini);
        h.setHoraFin(fin);
        return h;
    }

    private static HorarioInscrito inscrito(String curso, int iterable, String dia, String ini, String fin) {
        return new HorarioInscrito("cat1", curso, 2026, iterable, 1, 99, dia, ini, fin);
    }

    private void destinoConHorario(String dia, String ini, String fin) {
        when(horarioGateway.listarHorariosPorGrupo("cat1", "cur1", 2026, 1)).thenReturn(List.of(horario(1, dia, ini, fin)));
    }

    private void alumnoInscritoEn(String alumnoId, HorarioInscrito... franjas) {
        when(inscripcionGateway.listarHorariosInscritosVigentes(alumnoId, 2026, 1, HOY)).thenReturn(List.of(franjas));
    }

    private static InscripcionEnEspera enEspera(String alumnoId) {
        return new InscripcionEnEspera(alumnoId, "n", "c", Timestamp.from(Instant.now()));
    }

    // ── validarSinCruce ───────────────────────────────────────────────────────

    @Test
    void validarSinCruce_conCruce_lanzaCruceHorarioConElMensajeExacto_yBloqueoAlAlumnoAntesDeLeer() {
        destinoConHorario("MARTES", "10:00", "12:00");
        alumnoInscritoEn("a1", inscrito("Natación", 1, "MARTES", "11:00", "13:00"));

        CruceHorarioExcepcion ex = assertThrows(CruceHorarioExcepcion.class, () -> servicio(true).validarSinCruce(grupo, "a1"));

        assertEquals("Se cruza con Natación, grupo A, martes 11:00-13:00", ex.getMessage());
        InOrder orden = inOrder(alumnoGateway, inscripcionGateway);
        orden.verify(alumnoGateway).bloquearAlumno("a1");
        orden.verify(inscripcionGateway).listarHorariosInscritosVigentes("a1", 2026, 1, HOY);
    }

    @Test
    void validarSinCruce_sinCruce_noLanza() {
        destinoConHorario("MARTES", "10:00", "12:00");
        alumnoInscritoEn("a1", inscrito("Yoga", 1, "MARTES", "12:00", "13:00"), inscrito("Yoga", 2, "LUNES", "10:00", "12:00"));

        assertDoesNotThrow(() -> servicio(true).validarSinCruce(grupo, "a1"));
    }

    @Test
    void validarSinCruce_sinTope_unAlumnoConMuchasInscripcionesSinCruceEsValido() {
        destinoConHorario("MARTES", "10:00", "12:00");
        alumnoInscritoEn("a1", inscrito("A", 1, "LUNES", "10:00", "12:00"), inscrito("B", 1, "MIERCOLES", "10:00", "12:00"),
                inscrito("C", 1, "JUEVES", "10:00", "12:00"), inscrito("D", 1, "VIERNES", "10:00", "12:00"),
                inscrito("E", 1, "SABADO", "10:00", "12:00"));

        assertDoesNotThrow(() -> servicio(true).validarSinCruce(grupo, "a1"));
    }

    @Test
    void validarSinCruce_destinoSinHorarios_noBloqueaNiConsultaAlAlumno() {
        when(horarioGateway.listarHorariosPorGrupo("cat1", "cur1", 2026, 1)).thenReturn(List.of());

        assertDoesNotThrow(() -> servicio(true).validarSinCruce(grupo, "a1"));
        verifyNoInteractions(alumnoGateway, inscripcionGateway);
    }

    @Test
    void validarSinCruce_horasMalFormadas_noHay500_ySeIgnoran() {
        destinoConHorario("MARTES", "10:00", "12:00");
        alumnoInscritoEn("a1", inscrito("Yoga", 1, "MARTES", "8:", "doce"), inscrito("Yoga", 2, "MARTES", null, null));

        assertDoesNotThrow(() -> servicio(true).validarSinCruce(grupo, "a1"));
    }

    @Test
    void validarSinCruce_destinoConHorasMalFormadas_seIgnora() {
        when(horarioGateway.listarHorariosPorGrupo("cat1", "cur1", 2026, 1)).thenReturn(List.of(horario(5, "MARTES", "xx", "12:00")));

        assertDoesNotThrow(() -> servicio(true).validarSinCruce(grupo, "a1"));
        verifyNoInteractions(alumnoGateway);
    }

    @Test
    void validarSinCruce_interruptorApagado_noValidaNiBloquea() {
        assertDoesNotThrow(() -> servicio(false).validarSinCruce(grupo, "a1"));
        verifyNoInteractions(alumnoGateway, inscripcionGateway, horarioGateway);
    }

    // ── promoverSiguientes ────────────────────────────────────────────────────

    private void cola(String... alumnos) {
        when(inscripcionGateway.listarEnEspera("cat1", "cur1", 2026, 1))
                .thenReturn(java.util.Arrays.stream(alumnos).map(CruceHorariosServicioTest::enEspera).toList());
    }

    @Test
    void promoverSiguientes_saltaAlQueTendriaCruce_yPromueveAlSiguiente() {
        destinoConHorario("MARTES", "10:00", "12:00");
        cola("a1", "a2", "a3");
        alumnoInscritoEn("a1", inscrito("Yoga", 1, "MARTES", "11:00", "12:00"));   // cruce
        alumnoInscritoEn("a2");                                                    // libre
        alumnoInscritoEn("a3");

        int promovidos = servicio(true).promoverSiguientes(grupo, 1);

        assertEquals(1, promovidos);
        verify(inscripcionGateway).promoverInscripcion("a2", "cat1", "cur1", 2026, 1);
        verify(inscripcionGateway, never()).promoverInscripcion(eq("a1"), anyString(), anyString(), anyInt(), anyInt());
        verify(inscripcionGateway, never()).promoverInscripcion(eq("a3"), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void promoverSiguientes_todosConCruce_noPromueveANadie_yNoBloquea() {
        destinoConHorario("MARTES", "10:00", "12:00");
        cola("a1", "a2");
        alumnoInscritoEn("a1", inscrito("Yoga", 1, "MARTES", "11:00", "12:00"));
        alumnoInscritoEn("a2", inscrito("Zumba", 1, "MARTES", "10:00", "11:00"));

        assertEquals(0, servicio(true).promoverSiguientes(grupo, 2));

        verify(inscripcionGateway, never()).promoverInscripcion(any(), any(), any(), anyInt(), anyInt());
        verifyNoInteractions(alumnoGateway);
    }

    @Test
    void promoverSiguientes_variasPromociones_bloqueaLosAlumnosEnOrdenAscendenteDeId_noEnElOrdenDeLaCola() {
        destinoConHorario("MARTES", "10:00", "12:00");
        cola("zeta", "alfa", "mu");
        alumnoInscritoEn("zeta");
        alumnoInscritoEn("alfa");
        alumnoInscritoEn("mu");

        int promovidos = servicio(true).promoverSiguientes(grupo, 2);

        assertEquals(2, promovidos);
        InOrder orden = inOrder(alumnoGateway, inscripcionGateway);
        orden.verify(alumnoGateway).bloquearAlumno("alfa");
        orden.verify(alumnoGateway).bloquearAlumno("mu");
        orden.verify(alumnoGateway).bloquearAlumno("zeta");
        // Se promueve en el orden de la cola (zeta y alfa), con todos los locks ya tomados.
        orden.verify(inscripcionGateway).promoverInscripcion("zeta", "cat1", "cur1", 2026, 1);
        orden.verify(inscripcionGateway).promoverInscripcion("alfa", "cat1", "cur1", 2026, 1);
        verify(inscripcionGateway, never()).promoverInscripcion(eq("mu"), anyString(), anyString(), anyInt(), anyInt());
    }

    @Test
    void promoverSiguientes_destinoSinHorarios_promueveEnElOrdenDeLaCola_sinBloquear() {
        when(horarioGateway.listarHorariosPorGrupo("cat1", "cur1", 2026, 1)).thenReturn(List.of());
        cola("a1", "a2", "a3");

        assertEquals(2, servicio(true).promoverSiguientes(grupo, 2));

        verify(inscripcionGateway).promoverInscripcion("a1", "cat1", "cur1", 2026, 1);
        verify(inscripcionGateway).promoverInscripcion("a2", "cat1", "cur1", 2026, 1);
        verifyNoInteractions(alumnoGateway);
    }

    @Test
    void promoverSiguientes_interruptorApagado_promueveEnOrdenSinValidarNiBloquear() {
        cola("a1", "a2");

        assertEquals(1, servicio(false).promoverSiguientes(grupo, 1));

        verify(inscripcionGateway).promoverInscripcion("a1", "cat1", "cur1", 2026, 1);
        verifyNoInteractions(alumnoGateway, horarioGateway);
    }

    @Test
    void promoverSiguientes_colaVaciaOCantidadCero_noHaceNada() {
        when(inscripcionGateway.listarEnEspera("cat1", "cur1", 2026, 1)).thenReturn(List.of());
        assertEquals(0, servicio(true).promoverSiguientes(grupo, 3));
        assertEquals(0, servicio(true).promoverSiguientes(grupo, 0));
        assertEquals(0, servicio(true).promoverSiguientes(grupo, -2));
        verify(inscripcionGateway, never()).promoverInscripcion(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void promoverSiguientes_unCandidatoQueGanaUnCruceMientrasSeBloqueaSeRevalidaYSeSalta() {
        destinoConHorario("MARTES", "10:00", "12:00");
        cola("a1", "a2");
        // a1 estaba libre en la preseleccion pero, ya bloqueado, otra transaccion le dejo un cruce confirmado.
        when(inscripcionGateway.listarHorariosInscritosVigentes("a1", 2026, 1, HOY))
                .thenReturn(List.of())
                .thenReturn(List.of(inscrito("Yoga", 1, "MARTES", "10:00", "11:00")));
        alumnoInscritoEn("a2");

        assertEquals(1, servicio(true).promoverSiguientes(grupo, 1));

        verify(inscripcionGateway, never()).promoverInscripcion(eq("a1"), anyString(), anyString(), anyInt(), anyInt());
        verify(inscripcionGateway).promoverInscripcion("a2", "cat1", "cur1", 2026, 1);
    }
}
