package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;

// SCRUM-80 (rediseno, bloque 3 de 4): regla pura del aviso de registro
// pendiente (por vencer o vencido).
class EstadoAvisoRegistroTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 12);
    private static final LocalDate LIMITE = LocalDate.of(2026, 10, 31);

    @Test
    void calcular_sinGruposActivos_noGeneraAviso() {
        assertEquals(Optional.empty(), EstadoAvisoRegistro.calcular(0, 0, LIMITE, HOY));
    }

    @Test
    void calcular_todosRegistrados_noGeneraAviso() {
        assertEquals(Optional.empty(), EstadoAvisoRegistro.calcular(2, 2, LIMITE, HOY));
    }

    @Test
    void calcular_faltanRegistrosYQuedanMuchosDias_noGeneraAviso() {
        // 19 dias para el limite > 3 -> sin aviso.
        assertEquals(Optional.empty(), EstadoAvisoRegistro.calcular(2, 1, LIMITE, HOY));
    }

    @Test
    void calcular_faltanRegistrosYQuedanExactamente3Dias_generaProximoAVencer() {
        assertEquals(Optional.of(EstadoAvisoRegistro.PROXIMO_A_VENCER),
                EstadoAvisoRegistro.calcular(2, 1, LocalDate.of(2026, 10, 15), HOY));
    }

    @Test
    void calcular_faltanRegistrosYHoyEsElLimite_generaProximoAVencer() {
        // diasRestantes = 0 -> por vencer (todavia no vencido).
        assertEquals(Optional.of(EstadoAvisoRegistro.PROXIMO_A_VENCER),
                EstadoAvisoRegistro.calcular(2, 1, HOY, HOY));
    }

    @Test
    void calcular_faltanRegistrosYLimiteYaPaso_generaVencido() {
        Optional<EstadoAvisoRegistro> aviso =
                EstadoAvisoRegistro.calcular(2, 1, LocalDate.of(2026, 10, 11), HOY);
        assertTrue(aviso.isPresent());
        assertEquals(EstadoAvisoRegistro.VENCIDO, aviso.get());
    }

    @Test
    void calcular_limiteYaPaso_generaVencidoNoProximo() {
        assertEquals(Optional.of(EstadoAvisoRegistro.VENCIDO),
                EstadoAvisoRegistro.calcular(1, 0, LocalDate.of(2026, 10, 10), HOY));
    }
}
