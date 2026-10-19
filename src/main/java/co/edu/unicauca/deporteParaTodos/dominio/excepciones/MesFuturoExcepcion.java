package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

// SCRUM-80 (rediseno, bloque 3 de 4): consultar estado-registro de un mes
// futuro no tiene sentido (no hay datos que mostrar todavia) -> 422.
@Setter
@Getter
public class MesFuturoExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public MesFuturoExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.CONSULTA_MES_FUTURO.getLlaveMensaje();
        codigo = CodigoError.CONSULTA_MES_FUTURO.getCodigo();
    }
}
