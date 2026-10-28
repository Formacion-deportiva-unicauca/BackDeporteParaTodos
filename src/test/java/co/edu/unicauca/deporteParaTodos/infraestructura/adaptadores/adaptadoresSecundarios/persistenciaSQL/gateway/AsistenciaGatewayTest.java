package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AsistenciaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.AsistenciaId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAsistenciaRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenciaGatewayTest {

    @Mock
    private IAsistenciaRepositorio repoAsistencia;

    @InjectMocks
    private AsistenciaGateway asistenciaGateway;

    private static final String PERF_ID = "perf123";
    private static final Integer CLASE_ID = 10;

    private AsistenciaEntidad entidadActiva() {
        AsistenciaEntidad e = new AsistenciaEntidad();
        e.setPerfilId(PERF_ID);
        e.setClaseCodigo(CLASE_ID);
        e.setEliminado(0);
        return e;
    }

    private Asistencia modeloBase() {
        return new Asistencia(PERF_ID, CLASE_ID, 0);
    }

    // --- existeAsistencia ---

    @Test
    void existeAsistencia_existe_retornaTrue() {
        when(repoAsistencia.existsById(any(AsistenciaId.class))).thenReturn(true);
        assertTrue(asistenciaGateway.existeAsistencia(PERF_ID, CLASE_ID));
    }

    @Test
    void existeAsistencia_noExiste_retornaFalse() {
        when(repoAsistencia.existsById(any(AsistenciaId.class))).thenReturn(false);
        assertFalse(asistenciaGateway.existeAsistencia(PERF_ID, CLASE_ID));
    }

    // --- obtenerAtencionesPorClase ---

    @Test
    void obtenerAtencionesPorClase_retornaListaMapeada() {
        when(repoAsistencia.findByClaseCodigo(CLASE_ID)).thenReturn(List.of(entidadActiva()));

        List<Asistencia> resultado = asistenciaGateway.obtenerAtencionesPorClase(CLASE_ID);

        assertEquals(1, resultado.size());
        assertEquals(PERF_ID, resultado.get(0).getIdPerfil());
    }

    @Test
    void obtenerAtencionesPorClase_listaVacia_retornaVacia() {
        when(repoAsistencia.findByClaseCodigo(CLASE_ID)).thenReturn(List.of());
        assertTrue(asistenciaGateway.obtenerAtencionesPorClase(CLASE_ID).isEmpty());
    }

    // --- obtenerAsistencia ---

    @Test
    void obtenerAsistencia_encontrado_retornaOptionalPresente() {
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.of(entidadActiva()));

        Optional<Asistencia> resultado = asistenciaGateway.obtenerAsistencia(PERF_ID, CLASE_ID);

        assertTrue(resultado.isPresent());
        assertEquals(PERF_ID, resultado.get().getIdPerfil());
    }

    @Test
    void obtenerAsistencia_noEncontrado_retornaOptionalVacio() {
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.empty());
        assertTrue(asistenciaGateway.obtenerAsistencia(PERF_ID, CLASE_ID).isEmpty());
    }

    // --- InsertarAsistencia ---

    @Test
    void insertarAsistencia_guardaYRetornaMapeado() {
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenReturn(entidadActiva());

        Asistencia resultado = asistenciaGateway.InsertarAsistencia(modeloBase());

        assertNotNull(resultado);
        assertEquals(PERF_ID, resultado.getIdPerfil());
        verify(repoAsistencia).save(any(AsistenciaEntidad.class));
    }

    // Faltas acumuladas (etapa 2 de 5): ASI_ESTADO es NOT NULL sin DEFAULT a
    // nivel de entidad JPA -- si no se fija explicitamente antes de save(), la
    // insercion viola la restriccion NOT NULL.
    @Test
    void insertarAsistencia_fijaEstadoDelModeloEnLaEntidad() {
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenReturn(entidadActiva());

        asistenciaGateway.InsertarAsistencia(modeloBase());

        ArgumentCaptor<AsistenciaEntidad> guardada = ArgumentCaptor.forClass(AsistenciaEntidad.class);
        verify(repoAsistencia).save(guardada.capture());
        assertEquals("PRESENTE", guardada.getValue().getEstado());
    }

    // --- eliminarAsitencia ---

    @Test
    void eliminarAsitencia_encontrado_eliminaYRetorna() {
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.of(entidadActiva()));
        doNothing().when(repoAsistencia).delete(any(AsistenciaEntidad.class));

        Asistencia resultado = asistenciaGateway.eliminarAsitencia(PERF_ID, CLASE_ID);

        assertNotNull(resultado);
        assertEquals(PERF_ID, resultado.getIdPerfil());
        verify(repoAsistencia).delete(any(AsistenciaEntidad.class));
    }

    @Test
    void eliminarAsitencia_noEncontrado_lanzaNoExisteExcepcion() {
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.empty());
        assertThrows(NoExisteExcepcion.class, () -> asistenciaGateway.eliminarAsitencia(PERF_ID, CLASE_ID));
    }

    // --- eliminarAsistencia (lógico) ---

    @Test
    void eliminarAsistencia_encontrado_marcaEliminadoYRetorna() {
        AsistenciaEntidad entidadEliminada = entidadActiva();
        entidadEliminada.setEliminado(1);
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.of(entidadActiva()));
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenReturn(entidadEliminada);

        Asistencia resultado = asistenciaGateway.eliminarAsistencia(PERF_ID, (long) CLASE_ID);

        assertNotNull(resultado);
        assertEquals(1, resultado.getEliminado());
    }

    // --- reactivarAsistencia (SCRUM-80, rediseno bloque 2) ---
    // PASO 0 (caracterizacion, AsistenciaServicioTest): existeAsistencia() usa
    // existsById() y NO distingue META_ELIMINADO -- una fila eliminada
    // logicamente nunca se reactivaba. Este metodo nuevo la reactiva (eliminado=0).

    @Test
    void reactivarAsistencia_encontrada_marcaEliminado0YRetorna() {
        AsistenciaEntidad entidadEliminada = entidadActiva();
        entidadEliminada.setEliminado(1);
        AsistenciaEntidad entidadReactivada = entidadActiva();
        entidadReactivada.setEliminado(0);
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.of(entidadEliminada));
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenReturn(entidadReactivada);

        Asistencia resultado = asistenciaGateway.reactivarAsistencia(PERF_ID, CLASE_ID);

        assertNotNull(resultado);
        assertEquals(0, resultado.getEliminado());
    }

    @Test
    void reactivarAsistencia_noEncontrada_lanzaNoExisteExcepcion() {
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.empty());

        assertThrows(NoExisteExcepcion.class, () -> asistenciaGateway.reactivarAsistencia(PERF_ID, CLASE_ID));
    }

    // --- marcarPresente / marcarAusente (Faltas acumuladas, etapa 4 de 5) ---

    @Test
    void marcarPresente_filaNoExiste_laCrea() {
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.empty());
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaGateway.marcarPresente(PERF_ID, CLASE_ID);

        assertEquals("PRESENTE", resultado.getEstado());
        assertEquals(0, resultado.getEliminado());
        ArgumentCaptor<AsistenciaEntidad> guardada = ArgumentCaptor.forClass(AsistenciaEntidad.class);
        verify(repoAsistencia).save(guardada.capture());
        assertEquals(PERF_ID, guardada.getValue().getPerfilId());
        assertEquals(CLASE_ID, guardada.getValue().getClaseCodigo());
    }

    @Test
    void marcarPresente_filaExisteEliminada_laReactivaYLaFijaPresente() {
        AsistenciaEntidad existente = entidadActiva();
        existente.setEliminado(1);
        existente.setEstado("AUSENTE");
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.of(existente));
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaGateway.marcarPresente(PERF_ID, CLASE_ID);

        assertEquals("PRESENTE", resultado.getEstado());
        assertEquals(0, resultado.getEliminado());
    }

    @Test
    void marcarAusente_filaNoExiste_laCreaSinEliminar() {
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.empty());
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaGateway.marcarAusente(PERF_ID, CLASE_ID);

        assertEquals("AUSENTE", resultado.getEstado());
        assertEquals(0, resultado.getEliminado());
    }

    @Test
    void marcarAusente_filaExistePresenteYEliminada_laReactivaYLaFijaAusente() {
        AsistenciaEntidad existente = entidadActiva();
        existente.setEliminado(1);
        existente.setEstado("PRESENTE");
        when(repoAsistencia.findById(any(AsistenciaId.class))).thenReturn(Optional.of(existente));
        when(repoAsistencia.save(any(AsistenciaEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        Asistencia resultado = asistenciaGateway.marcarAusente(PERF_ID, CLASE_ID);

        assertEquals("AUSENTE", resultado.getEstado());
        assertEquals(0, resultado.getEliminado());
    }

    // --- contarFaltasPorGrupo (Faltas acumuladas, etapa 5 de 5) ---

    @Test
    void contarFaltasPorGrupo_mapeaFilasAPerfIdYCantidad() {
        LocalDate hoy = LocalDate.of(2026, 10, 26);
        when(repoAsistencia.contarFaltasPorGrupo("cat1", "cur1", 2026, 1, hoy))
                .thenReturn(List.of(new Object[] {"alum1", 2L}, new Object[] {"alum2", 5L}));

        Map<String, Integer> resultado = asistenciaGateway.contarFaltasPorGrupo("cat1", "cur1", 2026, 1, hoy);

        assertEquals(2, resultado.get("alum1"));
        assertEquals(5, resultado.get("alum2"));
    }

    @Test
    void contarFaltasPorGrupo_sinFilas_retornaMapaVacio() {
        LocalDate hoy = LocalDate.of(2026, 10, 26);
        when(repoAsistencia.contarFaltasPorGrupo("cat1", "cur1", 2026, 1, hoy)).thenReturn(List.of());

        Map<String, Integer> resultado = asistenciaGateway.contarFaltasPorGrupo("cat1", "cur1", 2026, 1, hoy);

        assertTrue(resultado.isEmpty());
    }
}
