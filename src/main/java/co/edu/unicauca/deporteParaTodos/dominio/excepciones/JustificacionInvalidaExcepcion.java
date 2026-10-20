package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class JustificacionInvalidaExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public JustificacionInvalidaExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.JUSTIFICACION_INVALIDA.getLlaveMensaje();
        codigo = CodigoError.JUSTIFICACION_INVALIDA.getCodigo();
    }
}
