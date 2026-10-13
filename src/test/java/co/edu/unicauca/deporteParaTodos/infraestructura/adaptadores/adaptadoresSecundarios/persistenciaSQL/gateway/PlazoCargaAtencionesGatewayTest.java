package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.PlazoCargaAtenciones;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.PlazoCargaAtencionesEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.PlazoCargaAtencionesId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IPlazoCargaAtencionesRepositorio;

// SCRUM-80 (HE-14 HU-02): el PlazoCargaAtencionesIT (BD MySQL real) no corre en
// este entorno sin DB_USER/DB_PASSWORD -- este test cubre la logica de mapeo y
// de upsert del Gateway con el repositorio mockeado, mismo patron que
// InstructorGatewayTest. Lo que SI necesita una BD real (el SQL nativo en si,
// las FK, el JOIN) queda sin probar aqui -- ver el IT.
@ExtendWith(MockitoExtension.class)
class PlazoCargaAtencionesGatewayTest {

    @Mock
    private IPlazoCargaAtencionesRepositorio repoPlazoCarga;

    @InjectMocks
    private PlazoCargaAtencionesGateway gateway;

    private static final String ID_INSTRUCTOR = "ins1";
    private static final int ANIO = 2026;
    private static final int PERIODO = 2;

    private PlazoCargaAtenciones datosNuevos(LocalDate fechaLimite) {
        PlazoCargaAtenciones datos = new PlazoCargaAtenciones();
        datos.setIdInstructor(ID_INSTRUCTOR);
        datos.setAnio(ANIO);
        datos.setPeriodo(PERIODO);
        datos.setFechaLimite(fechaLimite);
        datos.setIdCoordinador("coord1");
        datos.setFechaAsignacion(LocalDateTime.of(2026, 10, 12, 9, 0));
        return datos;
    }

    // ── upsertPlazo ──────────────────────────────────────────────────────────

    @Test
    void upsertPlazo_sinFilaExistente_creaUnaEntidadNuevaYLaGuarda() {
        when(repoPlazoCarga.findById(any(PlazoCargaAtencionesId.class))).thenReturn(Optional.empty());
        when(repoPlazoCarga.save(any(PlazoCargaAtencionesEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        PlazoCargaAtenciones resultado = gateway.upsertPlazo(datosNuevos(LocalDate.of(2026, 12, 1)));

        assertEquals(ID_INSTRUCTOR, resultado.getIdInstructor());
        assertEquals(ANIO, resultado.getAnio());
        assertEquals(PERIODO, resultado.getPeriodo());
        assertEquals(LocalDate.of(2026, 12, 1), resultado.getFechaLimite());
        assertEquals("coord1", resultado.getIdCoordinador());
        assertEquals(0, resultado.getEliminado(), "una fila nueva siempre queda activa");
    }

    @Test
    void upsertPlazo_conFilaExistente_reutilizaLaMismaEntidadYLaReemplaza() {
        PlazoCargaAtencionesEntidad existente = new PlazoCargaAtencionesEntidad();
        existente.setIdInstructor(ID_INSTRUCTOR);
        existente.setAnio(ANIO);
        existente.setPeriodo(PERIODO);
        existente.setFechaLimite(LocalDate.of(2026, 11, 1));
        existente.setIdCoordinador("coord-anterior");
        when(repoPlazoCarga.findById(any(PlazoCargaAtencionesId.class))).thenReturn(Optional.of(existente));
        when(repoPlazoCarga.save(any(PlazoCargaAtencionesEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        PlazoCargaAtenciones resultado = gateway.upsertPlazo(datosNuevos(LocalDate.of(2026, 12, 1)));

        assertEquals(LocalDate.of(2026, 12, 1), resultado.getFechaLimite(), "el valor nuevo reemplaza al anterior");
        assertEquals("coord1", resultado.getIdCoordinador());
        verify(repoPlazoCarga).save(existente);
    }

    // ── obtenerPlazosPorPeriodo ──────────────────────────────────────────────

    @Test
    void obtenerPlazosPorPeriodo_conFilas_lasIndexaPorIdInstructor() {
        PlazoCargaAtencionesEntidad entidad = new PlazoCargaAtencionesEntidad();
        entidad.setIdInstructor(ID_INSTRUCTOR);
        entidad.setAnio(ANIO);
        entidad.setPeriodo(PERIODO);
        entidad.setFechaLimite(LocalDate.of(2026, 12, 1));
        when(repoPlazoCarga.findByAnioAndPeriodoAndEliminado(ANIO, PERIODO, 0)).thenReturn(List.of(entidad));

        Map<String, PlazoCargaAtenciones> resultado = gateway.obtenerPlazosPorPeriodo(ANIO, PERIODO);

        assertEquals(1, resultado.size());
        assertTrue(resultado.containsKey(ID_INSTRUCTOR));
        assertEquals(LocalDate.of(2026, 12, 1), resultado.get(ID_INSTRUCTOR).getFechaLimite());
    }

    @Test
    void obtenerPlazosPorPeriodo_sinFilas_retornaMapaVacio() {
        when(repoPlazoCarga.findByAnioAndPeriodoAndEliminado(ANIO, PERIODO, 0)).thenReturn(List.of());

        assertTrue(gateway.obtenerPlazosPorPeriodo(ANIO, PERIODO).isEmpty());
    }

    // ── contarClasesConAsistenciaPorInstructor ──────────────────────────────

    @Test
    void contarClasesConAsistenciaPorInstructor_conFilas_lasConvierteAMapa() {
        Object[] fila1 = new Object[] {"ins1", 3L};
        Object[] fila2 = new Object[] {"ins2", 1L};
        when(repoPlazoCarga.contarClasesConAsistenciaPorInstructor(ANIO, PERIODO)).thenReturn(List.of(fila1, fila2));

        Map<String, Integer> resultado = gateway.contarClasesConAsistenciaPorInstructor(ANIO, PERIODO);

        assertEquals(2, resultado.size());
        assertEquals(3, resultado.get("ins1"));
        assertEquals(1, resultado.get("ins2"));
    }

    @Test
    void contarClasesConAsistenciaPorInstructor_sinFilas_retornaMapaVacio() {
        when(repoPlazoCarga.contarClasesConAsistenciaPorInstructor(ANIO, PERIODO)).thenReturn(List.of());

        assertTrue(gateway.contarClasesConAsistenciaPorInstructor(ANIO, PERIODO).isEmpty());
    }
}
