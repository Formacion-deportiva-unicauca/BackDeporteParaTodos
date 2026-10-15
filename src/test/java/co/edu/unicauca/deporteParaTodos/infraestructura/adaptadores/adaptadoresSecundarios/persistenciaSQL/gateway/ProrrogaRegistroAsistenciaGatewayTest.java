package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.dao.DataIntegrityViolationException;

import co.edu.unicauca.deporteParaTodos.dominio.excepciones.YaExisteElementoExcepcion;
import co.edu.unicauca.deporteParaTodos.dominio.modelo.ProrrogaRegistroAsistencia;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ProrrogaRegistroAsistenciaEntidad;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.entidades.ids.ProrrogaRegistroAsistenciaId;
import co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresSecundarios.persistenciaSQL.repositorios.IProrrogaRegistroAsistenciaRepositorio;

// SCRUM-80 (rediseno, bloque 1): el IT (BD MySQL real) no corre en este
// entorno sin DB_USER/DB_PASSWORD -- este test cubre mapeo y upsert con el
// repositorio mockeado, mismo patron que PlazoCargaAtencionesGatewayTest.
@ExtendWith(MockitoExtension.class)
class ProrrogaRegistroAsistenciaGatewayTest {

    @Mock
    private IProrrogaRegistroAsistenciaRepositorio repoProrroga;

    @InjectMocks
    private ProrrogaRegistroAsistenciaGateway gateway;

    private static final String ID_INSTRUCTOR = "ins1";
    private static final int ANIO = 2026;
    private static final int MES = 1;

    private ProrrogaRegistroAsistencia datosNuevos(int dias, LocalDate fechaLimite) {
        ProrrogaRegistroAsistencia datos = new ProrrogaRegistroAsistencia();
        datos.setIdInstructor(ID_INSTRUCTOR);
        datos.setAnio(ANIO);
        datos.setMes(MES);
        datos.setDias(dias);
        datos.setFechaLimite(fechaLimite);
        datos.setIdCoordinador("coord1");
        datos.setFechaAsignacion(LocalDateTime.of(2026, 1, 10, 9, 0));
        return datos;
    }

    @Test
    void upsertProrroga_sinFilaExistente_creaUnaEntidadNuevaYLaGuarda() {
        when(repoProrroga.findById(any(ProrrogaRegistroAsistenciaId.class))).thenReturn(Optional.empty());
        when(repoProrroga.saveAndFlush(any(ProrrogaRegistroAsistenciaEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        ProrrogaRegistroAsistencia resultado = gateway.upsertProrroga(datosNuevos(10, LocalDate.of(2026, 2, 10)));

        assertEquals(ID_INSTRUCTOR, resultado.getIdInstructor());
        assertEquals(10, resultado.getDias());
        assertEquals(LocalDate.of(2026, 2, 10), resultado.getFechaLimite());
        assertEquals(0, resultado.getEliminado());
    }

    @Test
    void upsertProrroga_conFilaExistente_reutilizaLaMismaEntidadYLaReemplaza() {
        ProrrogaRegistroAsistenciaEntidad existente = new ProrrogaRegistroAsistenciaEntidad();
        existente.setIdInstructor(ID_INSTRUCTOR);
        existente.setAnio(ANIO);
        existente.setMes(MES);
        existente.setDias(5);
        existente.setFechaLimite(LocalDate.of(2026, 2, 5));
        when(repoProrroga.findById(any(ProrrogaRegistroAsistenciaId.class))).thenReturn(Optional.of(existente));
        when(repoProrroga.saveAndFlush(any(ProrrogaRegistroAsistenciaEntidad.class))).thenAnswer(inv -> inv.getArgument(0));

        ProrrogaRegistroAsistencia resultado = gateway.upsertProrroga(datosNuevos(10, LocalDate.of(2026, 2, 10)));

        assertEquals(10, resultado.getDias(), "el valor nuevo reemplaza al anterior");
        assertEquals(LocalDate.of(2026, 2, 10), resultado.getFechaLimite());
        verify(repoProrroga).saveAndFlush(existente);
    }

    @Test
    void upsertProrroga_violacionDeRestriccion_lanzaYaExisteElementoExcepcion() {
        when(repoProrroga.findById(any(ProrrogaRegistroAsistenciaId.class))).thenReturn(Optional.empty());
        when(repoProrroga.saveAndFlush(any(ProrrogaRegistroAsistenciaEntidad.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(YaExisteElementoExcepcion.class, () -> gateway.upsertProrroga(datosNuevos(10, LocalDate.of(2026, 2, 10))));
    }

    @Test
    void obtenerProrrogasPorMes_conFilas_lasIndexaPorIdInstructor() {
        ProrrogaRegistroAsistenciaEntidad entidad = new ProrrogaRegistroAsistenciaEntidad();
        entidad.setIdInstructor(ID_INSTRUCTOR);
        entidad.setAnio(ANIO);
        entidad.setMes(MES);
        entidad.setDias(10);
        entidad.setFechaLimite(LocalDate.of(2026, 2, 10));
        when(repoProrroga.findByAnioAndMesAndEliminado(ANIO, MES, 0)).thenReturn(List.of(entidad));

        Map<String, ProrrogaRegistroAsistencia> resultado = gateway.obtenerProrrogasPorMes(ANIO, MES);

        assertEquals(1, resultado.size());
        assertTrue(resultado.containsKey(ID_INSTRUCTOR));
        assertEquals(LocalDate.of(2026, 2, 10), resultado.get(ID_INSTRUCTOR).getFechaLimite());
    }

    @Test
    void obtenerProrrogasPorMes_sinFilas_retornaMapaVacio() {
        when(repoProrroga.findByAnioAndMesAndEliminado(ANIO, MES, 0)).thenReturn(List.of());

        assertTrue(gateway.obtenerProrrogasPorMes(ANIO, MES).isEmpty());
    }

    @Test
    void eliminarProrroga_delegaAlRepositorioConBorradoFisico() {
        gateway.eliminarProrroga(ID_INSTRUCTOR, ANIO, MES);

        verify(repoProrroga).deleteById(new ProrrogaRegistroAsistenciaId(ID_INSTRUCTOR, ANIO, MES));
    }

    @Test
    void eliminarProrroga_siNoExiste_noLanzaNada() {
        doThrow(org.springframework.dao.EmptyResultDataAccessException.class).when(repoProrroga)
                .deleteById(any(ProrrogaRegistroAsistenciaId.class));

        // deleteById de Spring Data lanza EmptyResultDataAccessException si la fila
        // no existe -- el gateway lo absorbe (no hace nada) porque, para este
        // endpoint, dias=0 sobre un instructor SIN prorroga simplemente no tiene
        // nada que borrar.
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> gateway.eliminarProrroga(ID_INSTRUCTOR, ANIO, MES));
    }
}
