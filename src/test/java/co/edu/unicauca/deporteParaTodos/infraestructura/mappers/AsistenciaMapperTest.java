package co.edu.unicauca.deporteParaTodos.infraestructura.mappers;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.NoProcesableEntidadException;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs.AtencionDto;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.AsistenciaEntidad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AsistenciaMapperTest {

    private static final String PERF_ID = "perf123";
    private static final Integer CLASE_ID = 10;

    private AsistenciaEntidad entidadBase() {
        AsistenciaEntidad e = new AsistenciaEntidad();
        e.setPerfilId(PERF_ID);
        e.setClaseCodigo(CLASE_ID);
        e.setEliminado(0);
        e.setEstado("PRESENTE");
        return e;
    }

    private Asistencia modeloBase() {
        return new Asistencia(PERF_ID, CLASE_ID, 0);
    }

    // ── toDominio ─────────────────────────────────────────────────────────────

    @Test
    void toDominio_entidadValida_mapeaTodosCamposCorrectamente() {
        Asistencia resultado = AsistenciaMapper.toDominio(entidadBase());

        assertNotNull(resultado);
        assertEquals(PERF_ID, resultado.getIdPerfil());
        assertEquals(CLASE_ID, resultado.getClsCodigo());
        assertEquals(0, resultado.getEliminado());
        assertEquals("PRESENTE", resultado.getEstado());
    }

    @Test
    void toDominio_entidadNula_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> AsistenciaMapper.toDominio(null));
    }

    // ── toEntidad ─────────────────────────────────────────────────────────────

    @Test
    void toEntidad_modeloValido_mapeaTodosCamposCorrectamente() {
        AsistenciaEntidad resultado = AsistenciaMapper.toEntidad(modeloBase());

        assertNotNull(resultado);
        assertEquals(PERF_ID, resultado.getPerfilId());
        assertEquals(CLASE_ID, resultado.getClaseCodigo());
        assertEquals(0, resultado.getEliminado());
        assertEquals("PRESENTE", resultado.getEstado());
    }

    @Test
    void toEntidad_modeloNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> AsistenciaMapper.toEntidad(null));
    }

    // ── toDto — conversión Boolean ─────────────────────────────────────────────

    @Test
    void toDto_eliminadoCero_estaAtendidoTrue() {
        Asistencia modelo = new Asistencia(PERF_ID, CLASE_ID, 0);
        AtencionDto resultado = AsistenciaMapper.toDto(modelo);

        assertNotNull(resultado);
        assertEquals(PERF_ID, resultado.getIdPerfil());
        assertEquals(CLASE_ID, resultado.getIdClase());
        assertTrue(resultado.getEstaAtendido());
    }

    @Test
    void toDto_eliminadoUno_estaAtendidoFalse() {
        Asistencia modelo = new Asistencia(PERF_ID, CLASE_ID, 1);
        assertTrue(!AsistenciaMapper.toDto(modelo).getEstaAtendido());
    }

    @Test
    void toDto_eliminadoNulo_estaAtendidoTrue() {
        Asistencia modelo = new Asistencia(PERF_ID, CLASE_ID, null);
        assertTrue(AsistenciaMapper.toDto(modelo).getEstaAtendido());
    }

    // Faltas acumuladas (etapa 2 de 5): una fila AUSENTE no eliminada NO cuenta
    // como atendida -- antes de esta etapa esto no era posible (toda fila no
    // eliminada era implicitamente una presencia).
    @Test
    void toDto_estadoAusenteSinEliminar_estaAtendidoFalse() {
        Asistencia modelo = new Asistencia(PERF_ID, CLASE_ID, 0, "AUSENTE");
        assertFalse(AsistenciaMapper.toDto(modelo).getEstaAtendido());
    }

    @Test
    void toDto_modeloNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> AsistenciaMapper.toDto(null));
    }

    // ── fromDto ───────────────────────────────────────────────────────────────

    @Test
    void fromDto_estaAtendidoTrue_eliminadoCero() {
        AtencionDto dto = new AtencionDto(PERF_ID, CLASE_ID, true);
        Asistencia resultado = AsistenciaMapper.fromDto(dto);

        assertNotNull(resultado);
        assertEquals(PERF_ID, resultado.getIdPerfil());
        assertEquals(CLASE_ID, resultado.getClsCodigo());
        assertEquals(0, resultado.getEliminado());
    }

    @Test
    void fromDto_estaAtendidoFalse_eliminadoUno() {
        AtencionDto dto = new AtencionDto(PERF_ID, CLASE_ID, false);
        assertEquals(1, AsistenciaMapper.fromDto(dto).getEliminado());
    }

    @Test
    void fromDto_dtoNulo_lanzaNoProcesableEntidadException() {
        assertThrows(NoProcesableEntidadException.class, () -> AsistenciaMapper.fromDto(null));
    }
}
