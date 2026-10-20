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

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IGrupoGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.INovedadClaseGateway;
import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IProrrogaRegistroAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.FechaClaseFuturaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.JustificacionInvalidaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NovedadDuplicadaExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.RegistroAsistenciaVencidoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Grupo;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.NovedadClase;

@ExtendWith(MockitoExtension.class)
class NovedadClaseServicioTest {

    @Mock
    private INovedadClaseGateway novedadGateway;

    @Mock
    private IGrupoGateway grupoGateway;

    @Mock
    private IProrrogaRegistroAsistenciaGateway prorrogaGateway;

    @Mock
    private Clock clock;

    @InjectMocks
    private NovedadClaseServicio servicio;

    private static final String INS = "INS001";
    private static final String COORD = "coord-1";
    private static final LocalDate HOY_FIJO = LocalDate.of(2026, 10, 15); // jueves

    @BeforeEach
    void configurarClock() {
        lenient().when(clock.instant())
                .thenReturn(HOY_FIJO.atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("America/Bogota"));
    }

    private NovedadClase datosValidos() {
        NovedadClase d = new NovedadClase();
        d.setCategoria("Futbol");
        d.setCurso("Avanzado");
        d.setAnio(2026);
        d.setIterable(1);
        d.setFecha(HOY_FIJO);
        d.setMotivo("CLIMA");
        d.setDescripcion(null);
        return d;
    }

    private Grupo grupoConInstructor(String idInstructor) {
        Grupo g = new Grupo();
        g.setIdInstructor(idInstructor);
        g.setEliminado(0);
        return g;
    }

    private void sinProrroga() {
        lenient().when(prorrogaGateway.obtenerProrrogasPorMes(anyInt(), anyInt())).thenReturn(Map.of());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Registro válido
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_valido_instructorAsignado_exito() {
        NovedadClase datos = datosValidos();
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));
        when(novedadGateway.existeNovedadActiva("Futbol", "Avanzado", 2026, 1, HOY_FIJO)).thenReturn(false);
        // thenAnswer devuelve el mismo objeto que el servicio modificó (con idUsuarioCreador, fechaCreacion, etc.)
        when(novedadGateway.insertarNovedad(any(NovedadClase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NovedadClase resultado = servicio.registrarNovedad(datos, INS, "Instructor");

        assertNotNull(resultado);
        assertEquals(INS, resultado.getIdUsuarioCreador());
        assertEquals(0, resultado.getEliminado());
        assertEquals(HOY_FIJO, resultado.getFecha());
        verify(novedadGateway).insertarNovedad(any(NovedadClase.class));
    }

    @Test
    void registrarNovedad_valido_coordinadorSinLimiteDePlazo_exito() {
        NovedadClase datos = datosValidos();
        datos.setFecha(LocalDate.of(2026, 9, 10)); // mes pasado, fuera de plazo para instructor
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));
        when(novedadGateway.existeNovedadActiva("Futbol", "Avanzado", 2026, 1, datos.getFecha())).thenReturn(false);
        NovedadClase guardada = new NovedadClase();
        when(novedadGateway.insertarNovedad(any(NovedadClase.class))).thenReturn(guardada);

        assertDoesNotThrow(() -> servicio.registrarNovedad(datos, COORD, "Coordinador"));
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Motivo OTRO sin descripción
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_motivoOTRO_sinDescripcion_lanzaJustificacionInvalida() {
        NovedadClase datos = datosValidos();
        datos.setMotivo("OTRO");
        datos.setDescripcion(null);
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));
        sinProrroga();

        JustificacionInvalidaExcepcion ex = assertThrows(JustificacionInvalidaExcepcion.class,
                () -> servicio.registrarNovedad(datos, INS, "Instructor"));
        assertTrue(ex.getMessage().contains("descripci\u00f3n es obligatoria"));
        verify(novedadGateway, never()).insertarNovedad(any());
    }

    @Test
    void registrarNovedad_motivoOTRO_conDescripcion_permite() {
        NovedadClase datos = datosValidos();
        datos.setMotivo("OTRO");
        datos.setDescripcion("Cancha inundada por la lluvia");
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));
        when(novedadGateway.existeNovedadActiva("Futbol", "Avanzado", 2026, 1, HOY_FIJO)).thenReturn(false);
        NovedadClase guardada = new NovedadClase();
        when(novedadGateway.insertarNovedad(any(NovedadClase.class))).thenReturn(guardada);
        sinProrroga();

        assertDoesNotThrow(() -> servicio.registrarNovedad(datos, INS, "Instructor"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Fecha futura
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_fechaFutura_lanzaFechaClaseFutura() {
        NovedadClase datos = datosValidos();
        datos.setFecha(HOY_FIJO.plusDays(1));
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));

        assertThrows(FechaClaseFuturaExcepcion.class,
                () -> servicio.registrarNovedad(datos, INS, "Instructor"));
        verify(prorrogaGateway, never()).obtenerProrrogasPorMes(anyInt(), anyInt());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Ventana vencida
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_ventanaVencida_instructor_lanzaRegistroAsistenciaVencido() {
        NovedadClase datos = datosValidos();
        datos.setFecha(LocalDate.of(2026, 9, 10)); // mes pasado, sin prorroga
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));
        sinProrroga();

        assertThrows(RegistroAsistenciaVencidoExcepcion.class,
                () -> servicio.registrarNovedad(datos, INS, "Instructor"));
        verify(novedadGateway, never()).insertarNovedad(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Instructor no autorizado (no es el instructor del grupo)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_instructorNoEsElDelGrupo_lanzaNoExiste() {
        NovedadClase datos = datosValidos();
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor("OTRO_INS"));

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.registrarNovedad(datos, INS, "Instructor"));
        verify(novedadGateway, never()).insertarNovedad(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Novedad duplicada
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_novedadYaExistente_lanzaNovedadDuplicada() {
        NovedadClase datos = datosValidos();
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));
        when(novedadGateway.existeNovedadActiva("Futbol", "Avanzado", 2026, 1, HOY_FIJO)).thenReturn(true);
        sinProrroga();

        assertThrows(NovedadDuplicadaExcepcion.class,
                () -> servicio.registrarNovedad(datos, INS, "Instructor"));
        verify(novedadGateway, never()).insertarNovedad(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Consulta de novedades del grupo
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void obtenerNovedadesGrupo_retornaLista() {
        NovedadClase n = new NovedadClase();
        n.setCodigo(1L);
        n.setMotivo("CLIMA");
        when(novedadGateway.obtenerNovedadesPorGrupoFecha("Futbol", "Avanzado", 2026, 1, null))
                .thenReturn(List.of(n));

        List<NovedadClase> resultado = servicio.obtenerNovedadesGrupo("Futbol", "Avanzado", 2026, 1);

        assertEquals(1, resultado.size());
        assertEquals(1L, resultado.get(0).getCodigo());
        assertEquals("CLIMA", resultado.get(0).getMotivo());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Edición: conserva campos de creación, actualiza campos de modificación
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void actualizarNovedad_valido_actualizaYConservaCamposCreacion() {
        NovedadClase existente = new NovedadClase();
        existente.setCodigo(1L);
        existente.setIdUsuarioCreador(INS); // el instructor es el creador
        existente.setFechaCreacion(LocalDate.of(2026, 9, 1).atStartOfDay());
        existente.setVersion(0);
        existente.setEliminado(0);

        NovedadClase datos = new NovedadClase();
        datos.setCategoria("Futbol");
        datos.setCurso("Avanzado");
        datos.setAnio(2026);
        datos.setIterable(1);
        datos.setFecha(HOY_FIJO);
        datos.setMotivo("ESCENARIO");
        datos.setDescripcion("Cancha mojada");

        when(novedadGateway.obtenerNovedadPorId(1L)).thenReturn(Optional.of(existente));
        // thenAnswer devuelve el mismo objeto que el servicio modificó (con idUsuarioCreador conservado, version incrementada, etc.)
        when(novedadGateway.actualizarNovedad(any(NovedadClase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        NovedadClase resultado = servicio.actualizarNovedad(1L, datos, INS, "Instructor");

        assertEquals(INS, resultado.getIdUsuarioCreador());
        assertEquals(INS, resultado.getIdUsuarioModificador());
        assertNotNull(resultado.getFechaModificacion());
        assertEquals(1, resultado.getVersion());
        verify(novedadGateway).actualizarNovedad(any(NovedadClase.class));
    }

    @Test
    void actualizarNovedad_instructorNoEsCreador_lanzaNoExiste() {
        NovedadClase existente = new NovedadClase();
        existente.setCodigo(1L);
        existente.setIdUsuarioCreador("otro-creador");
        when(novedadGateway.obtenerNovedadPorId(1L)).thenReturn(Optional.of(existente));

        NovedadClase datos = new NovedadClase();
        datos.setCategoria("Futbol");
        datos.setCurso("Avanzado");
        datos.setAnio(2026);
        datos.setIterable(1);
        datos.setFecha(HOY_FIJO);
        datos.setMotivo("CLIMA");

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.actualizarNovedad(1L, datos, INS, "Instructor"));
        verify(novedadGateway, never()).actualizarNovedad(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Regresión SCRUM-80: ventana de registro respetada (no se tocan asistencias)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    void registrarNovedad_noTocaAsistenciasSoloRegistraNovedad() {
        NovedadClase datos = datosValidos();
        when(grupoGateway.obtenerGrupo("Futbol", "Avanzado", 2026, 1)).thenReturn(grupoConInstructor(INS));
        when(novedadGateway.existeNovedadActiva("Futbol", "Avanzado", 2026, 1, HOY_FIJO)).thenReturn(false);
        when(novedadGateway.insertarNovedad(any(NovedadClase.class))).thenReturn(new NovedadClase());

        servicio.registrarNovedad(datos, INS, "Instructor");

        // El servicio de novedad no inyecta ningún gateway de asistencia, por lo tanto
        // no puede modificar tbl_asistencia. Solo verifica que se insertó la novedad.
        verify(novedadGateway).insertarNovedad(any(NovedadClase.class));
    }
}
