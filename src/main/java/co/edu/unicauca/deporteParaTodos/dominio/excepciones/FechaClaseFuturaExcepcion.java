package co.edu.unicauca.deporteParaTodos.dominio.excepciones;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FechaClaseFuturaExcepcion extends RuntimeException {
    private final String llaveMensaje;
    private final String codigo;

    public FechaClaseFuturaExcepcion(final String mensaje) {
        super(mensaje);
        llaveMensaje = CodigoError.FECHA_CLASE_FUTURA.getLlaveMensaje();
        codigo = CodigoError.FECHA_CLASE_FUTURA.getCodigo();
    }
}
