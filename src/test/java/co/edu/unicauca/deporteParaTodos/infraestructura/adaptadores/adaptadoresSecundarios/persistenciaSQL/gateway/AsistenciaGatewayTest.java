package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AsistenciaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.AsistenciaId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IAsistenciaRepositorio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
}
