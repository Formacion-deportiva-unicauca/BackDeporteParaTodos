package co.edu.unicauca.deporteParaTodos.dominio.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

// SCRUM-80 (rediseno, bloque 3 de 4): aviso que se le muestra al Instructor
// cuando le falta registrar alguna clase de un mes y el plazo esta por vencer
// o ya vencio. "PROXIMO_A_VENCER" es una ADICION propia de esta implementacion
// (no pedida explicitamente): avisa desde DIAS_ANTICIPACION dias antes del
// limite; VENCIDO cuando el limite ya paso. Regla pura, sin Spring.
public enum EstadoAvisoRegistro {
    PROXIMO_A_VENCER,
    VENCIDO;

    public static final int DIAS_ANTICIPACION = 3;

    // Optional.empty() = sin aviso: no hay grupos activos, ya esta COMPLETO, o
    // faltan mas de DIAS_ANTICIPACION dias para el limite. Si falta registro y
    // hoy > fechaLimite -> VENCIDO; si falta y quedan 0..DIAS_ANTICIPACION -> PROXIMO_A_VENCER.
    public static Optional<EstadoAvisoRegistro> calcular(int gruposActivos, int gruposRegistrados,
            LocalDate fechaLimite, LocalDate hoy) {
        if (gruposActivos <= 0 || gruposRegistrados >= gruposActivos) {
            return Optional.empty();
        }
        if (hoy.isAfter(fechaLimite)) {
            return Optional.of(VENCIDO);
        }
        long diasRestantes = ChronoUnit.DAYS.between(hoy, fechaLimite);
        return diasRestantes <= DIAS_ANTICIPACION ? Optional.of(PROXIMO_A_VENCER) : Optional.empty();
    }
}
