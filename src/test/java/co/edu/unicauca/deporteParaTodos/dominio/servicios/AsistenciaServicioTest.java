package co.edu.unicauca.deporteParaTodos.dominio.servicios;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAsistenciaGateway;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.ListadoVacioExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoExisteExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;

@ExtendWith(MockitoExtension.class)
class AsistenciaServicioTest {

    @Mock
    private IAsistenciaGateway asistenciaGateway;

    @Mock
    private co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IAlumnoGateway alumnoGateway;

    @Mock
    private co.edu.unicauca.deporteParaTodos.aplicacion.puertos.puertosSalida.IClaseGateway claseGateway;

    @InjectMocks
    private AsistenciaServicio servicio;

    // SonarCloud javabugs:S2259: AsistenciaGateway.obtenerAsistencias() esta sin
    // implementar y retorna null (no una lista vacia) -- antes de este fix,
    // asistencias.isEmpty() reventaba con NullPointerException real en vez de
    // la ListadoVacioExcepcion de negocio esperada.
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

    @Test
    void eliminarAsistencia_exitoso_devuelve_asistencia_eliminada() {
        Asistencia activa = new Asistencia("perf1", 10, 0);
        Asistencia eliminada = new Asistencia("perf1", 10, 1);
        when(asistenciaGateway.existeAsistencia("perf1", 10)).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("perf1", 10)).thenReturn(Optional.of(activa));
        when(asistenciaGateway.eliminarAsistencia("perf1", 10L)).thenReturn(eliminada);

        Asistencia resultado = servicio.eliminarAsistencia("perf1", 10L);

        assertNotNull(resultado);
        assertEquals(1, resultado.getEliminado());
        verify(asistenciaGateway).eliminarAsistencia("perf1", 10L);
    }

    @Test
    void eliminarAsistencia_noExiste_lanzaNoExisteExcepcion() {
        when(asistenciaGateway.existeAsistencia("perf1", 10)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", 10L));
        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
    }

    @Test
    void eliminarAsistencia_yaEliminada_lanzaYaExisteElementoExcepcion() {
        Asistencia yaEliminada = new Asistencia("perf1", 10, 1);
        when(asistenciaGateway.existeAsistencia("perf1", 10)).thenReturn(true);
        when(asistenciaGateway.obtenerAsistencia("perf1", 10)).thenReturn(Optional.of(yaEliminada));

        assertThrows(YaExisteElementoExcepcion.class,
                () -> servicio.eliminarAsistencia("perf1", 10L));
        verify(asistenciaGateway, never()).eliminarAsistencia(any(), any());
    }

    // ── PASO 0 (SCRUM-80 bloque 2): caracterizacion de registrarAtencionesPorClase ──
    // Pruebas de caracterizacion del comportamiento ACTUAL (no de lo deseado) --
    // se escriben ANTES de tocar nada, para reportar con evidencia real.

    @Test
    void CARACTERIZACION_claseInexistente_lanzaNoExisteExcepcion() {
        when(claseGateway.existeClase(99)).thenReturn(false);

        assertThrows(NoExisteExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(), 99));
    }

    @Test
    void CARACTERIZACION_listaVacia_noLanzaErrorNiTocaGateways() {
        when(claseGateway.existeClase(10)).thenReturn(true);

        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(), 10));

        verify(asistenciaGateway, never()).InsertarAsistencia(any());
        verify(alumnoGateway, never()).existeAlumno(any());
    }

    @Test
    void CARACTERIZACION_registrarDosVecesAlMismoAlumnoActivo_noDuplicaNiLanzaError() {
        when(claseGateway.existeClase(10)).thenReturn(true);
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(asistenciaGateway.existeAsistencia("alum1", 10)).thenReturn(true);

        AtencionDto dto = new AtencionDto("alum1", 10, true);
        assertDoesNotThrow(() -> servicio.registrarAtencionesPorClase(List.of(dto), 10));

        verify(asistenciaGateway, never()).InsertarAsistencia(any());
    }

    // HALLAZGO del PASO 0 (no una suposicion): existeAsistencia() usa
    // repoAsistencia.existsById(...), que NO filtra META_ELIMINADO -- por lo
    // tanto, si ya existe una fila con META_ELIMINADO=1 (eliminada
    // logicamente), existeAsistencia() de todas formas retorna true. El guard
    // "if (!existeAsistencia(...)) insertar" entonces NUNCA reactiva esa fila:
    // no inserta (porque "existe"), y tampoco hay ninguna rama que la reactive.
    // El comportamiento ACTUAL es: la fila se queda en META_ELIMINADO=1 para
    // siempre, sin error y sin aviso. Esto contradice el requisito nuevo
    // ("si existe con META_ELIMINADO=1, se reactiva") -- hay que corregirlo.
    @Test
    void CARACTERIZACION_alumnoConAsistenciaEliminadaLogicamente_actualmenteNoSeReactiva() {
        when(claseGateway.existeClase(10)).thenReturn(true);
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        // existeAsistencia() no distingue eliminado=0 de eliminado=1 -- ambos dan true.
        when(asistenciaGateway.existeAsistencia("alum1", 10)).thenReturn(true);

        AtencionDto dto = new AtencionDto("alum1", 10, true);
        servicio.registrarAtencionesPorClase(List.of(dto), 10);

        // Comportamiento actual: NO reactiva (no llama InsertarAsistencia ni
        // ningun metodo de "reactivar"/actualizar eliminado=0).
        verify(asistenciaGateway, never()).InsertarAsistencia(any());
    }

    @Test
    void CARACTERIZACION_alumnoInexistente_lanzaNoExisteExcepcion() {
        when(claseGateway.existeClase(10)).thenReturn(true);
        when(alumnoGateway.existeAlumno("fantasma")).thenReturn(false);

        AtencionDto dto = new AtencionDto("fantasma", 10, true);
        assertThrows(NoExisteExcepcion.class,
                () -> servicio.registrarAtencionesPorClase(List.of(dto), 10));
    }

    @Test
    void CARACTERIZACION_estaAtendidoFalseYExiste_eliminaLogicamente() {
        when(claseGateway.existeClase(10)).thenReturn(true);
        when(alumnoGateway.existeAlumno("alum1")).thenReturn(true);
        when(asistenciaGateway.existeAsistencia("alum1", 10)).thenReturn(true);

        AtencionDto dto = new AtencionDto("alum1", 10, false);
        servicio.registrarAtencionesPorClase(List.of(dto), 10);

        verify(asistenciaGateway).eliminarAsitencia("alum1", 10);
    }
}
