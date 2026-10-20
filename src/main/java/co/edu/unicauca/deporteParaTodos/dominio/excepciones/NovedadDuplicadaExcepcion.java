package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

/**
 * Excepci\u00f3n lanzada al intentar registrar una novedad para un grupo y fecha
 * en los que ya existe una novedad activa (no eliminada l\u00f3gicamente).
 */
@Setter
@Getter
public class NovedadDuplicadaExcepcion extends RuntimeException {

    private final String llaveMensaje;
    private final String codigo;

    public NovedadDuplicadaExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.YA_EXISTE.getLlaveMensaje();
        codigo = CodigoError.YA_EXISTE.getCodigo();
    }
}
