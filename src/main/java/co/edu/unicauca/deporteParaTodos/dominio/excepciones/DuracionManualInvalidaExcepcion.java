package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DuracionManualInvalidaExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public DuracionManualInvalidaExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.DURACION_MANUAL_INVALIDA.getLlaveMensaje();
        codigo = CodigoError.DURACION_MANUAL_INVALIDA.getCodigo();
    }
}
