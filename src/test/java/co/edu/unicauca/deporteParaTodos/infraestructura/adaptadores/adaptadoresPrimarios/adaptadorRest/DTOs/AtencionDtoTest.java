package co.edu.unicauca.deporteParaTodos.infraestructura.adaptadores.adaptadoresPrimarios.adaptadorRest.DTOs;

import co.edu.unicauca.deporteParaTodos.dominio.modelo.Asistencia;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Faltas acumuladas (etapa 2 de 5): estaAtendido = (ASI_ESTADO='PRESENTE' AND
// META_ELIMINADO != 1) -- misma formula que AsistenciaMapper.toDto(), aqui
// duplicada en fabricarDeModelo() siguiendo la convencion de DTOs del
// proyecto (ver AGENTS.md). fabricarDeModelo() no tenia pruebas previas.
class AtencionDtoTest {

    private static final String PERF_ID = "perf123";
    private static final Integer CLASE_ID = 10;

    @Test
    void fabricarDeModelo_presenteSinEliminar_estaAtendidoTrue() {
        Asistencia modelo = new Asistencia(PERF_ID, CLASE_ID, 0);

        AtencionDto resultado = AtencionDto.fabricarDeModelo(modelo);

        assertEquals(PERF_ID, resultado.getIdPerfil());
        assertEquals(CLASE_ID, resultado.getIdClase());
        assertTrue(resultado.getEstaAtendido());
    }

    @Test
    void fabricarDeModelo_presenteEliminado_estaAtendidoFalse() {
        Asistencia modelo = new Asistencia(PERF_ID, CLASE_ID, 1);
        assertFalse(AtencionDto.fabricarDeModelo(modelo).getEstaAtendido());
    }

    @Test
    void fabricarDeModelo_ausenteSinEliminar_estaAtendidoFalse() {
        Asistencia modelo = new Asistencia(PERF_ID, CLASE_ID, 0, "AUSENTE");
        assertFalse(AtencionDto.fabricarDeModelo(modelo).getEstaAtendido());
    }
}
